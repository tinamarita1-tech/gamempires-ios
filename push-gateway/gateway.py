#!/usr/bin/env python3
"""
GamEmpires push gateway: a small Nextcloud push proxy (push-v2) that delivers through Firebase Cloud Messaging.

    Nextcloud (talk.gamempires.com) --POST /notifications--> this gateway --FCM HTTP v1--> GamEmpires Messenger

Endpoints (served under https://talk.gamempires.com/push-gateway/ by Caddy):
    POST   /devices        phone registers {pushToken, deviceIdentifier, deviceIdentifierSignature, userPublicKey}
    DELETE /devices        phone unregisters {deviceIdentifier, deviceIdentifierSignature, userPublicKey}
    POST   /notifications  Nextcloud delivers notifications[] (each a JSON string)
    POST   /session        app claims "this phone is my one mobile session" (Nextcloud login checked);
                           the phone that had the session before gets a sign-out push
    POST   /session-check  app asks "am I still the active phone for my account?"
    GET    /health         simple status check

Security: every device registration is signed by the user's Nextcloud key, and every notification is signed
with the same key; the gateway checks both, so only our Nextcloud can push to a registered phone. Message
content is encrypted for the phone; the gateway and Google cannot read it.

Config (environment, see gamempires-push.service):
    PUSH_GATEWAY_BIND          default 127.0.0.1
    PUSH_GATEWAY_PORT          default 8790
    PUSH_GATEWAY_DB            default /var/lib/gamempires-push/devices.db
    FIREBASE_SERVICE_ACCOUNT   default /etc/gamempires-push/service-account.json
    PUSH_GATEWAY_DRY_RUN       "1" = log instead of sending to Firebase (testing)
    NEXTCLOUD_URL              default https://talk.gamempires.com (used to check app logins)

One mobile session per account: the app sends its own Nextcloud app login (Authorization header) only so
the gateway can ask Nextcloud who the user is. It is never stored or logged.
"""
import base64
import hashlib
import json
import logging
import os
import sqlite3
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, utils

BIND = os.environ.get("PUSH_GATEWAY_BIND", "127.0.0.1")
PORT = int(os.environ.get("PUSH_GATEWAY_PORT", "8790"))
DB_PATH = os.environ.get("PUSH_GATEWAY_DB", "/var/lib/gamempires-push/devices.db")
SERVICE_ACCOUNT = os.environ.get("FIREBASE_SERVICE_ACCOUNT", "/etc/gamempires-push/service-account.json")
DRY_RUN = os.environ.get("PUSH_GATEWAY_DRY_RUN") == "1"
MAX_BODY = 1024 * 1024
FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
NEXTCLOUD_URL = os.environ.get("NEXTCLOUD_URL", "https://talk.gamempires.com").rstrip("/")

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("push-gateway")


# ---------------------------------------------------------------- storage
class DeviceStore:
    def __init__(self, path):
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        self.db = sqlite3.connect(path, check_same_thread=False)
        self.lock = threading.Lock()
        with self.lock:
            self.db.execute("""CREATE TABLE IF NOT EXISTS devices (
                device_identifier TEXT PRIMARY KEY,
                push_token TEXT NOT NULL,
                user_public_key TEXT NOT NULL,
                updated INTEGER NOT NULL)""")
            self.db.execute("""CREATE TABLE IF NOT EXISTS sessions (
                user_id TEXT PRIMARY KEY,
                install_id TEXT NOT NULL,
                push_token TEXT NOT NULL,
                updated INTEGER NOT NULL)""")
            self.db.commit()

    def session(self, user_id):
        with self.lock:
            return self.db.execute("SELECT install_id, push_token FROM sessions WHERE user_id=?", (user_id,)).fetchone()

    def put_session(self, user_id, install_id, push_token):
        with self.lock:
            self.db.execute("INSERT OR REPLACE INTO sessions VALUES (?,?,?,?)",
                            (user_id, install_id, push_token, int(time.time())))
            self.db.commit()

    def get(self, device_identifier):
        with self.lock:
            row = self.db.execute("SELECT push_token, user_public_key FROM devices WHERE device_identifier=?",
                                  (device_identifier,)).fetchone()
        return row

    def put(self, device_identifier, push_token, user_public_key):
        with self.lock:
            self.db.execute("INSERT OR REPLACE INTO devices VALUES (?,?,?,?)",
                            (device_identifier, push_token, user_public_key, int(time.time())))
            self.db.commit()

    def delete(self, device_identifier):
        with self.lock:
            self.db.execute("DELETE FROM devices WHERE device_identifier=?", (device_identifier,))
            self.db.commit()

    def count(self):
        with self.lock:
            return self.db.execute("SELECT COUNT(*) FROM devices").fetchone()[0]


# ---------------------------------------------------------------- crypto
def normalize_pem(pem):
    """Same key, same text: unify line endings and trailing whitespace before comparing/storing."""
    return "\n".join(line.strip() for line in str(pem).strip().replace("\r\n", "\n").split("\n"))


def load_public_key(pem):
    return serialization.load_pem_public_key(pem.encode())


def verify_sha512(public_key_pem, data: bytes, signature_b64: str) -> bool:
    """openssl_verify(data, base64_decode(sig), key, OPENSSL_ALGO_SHA512) equivalent."""
    try:
        key = load_public_key(public_key_pem)
        key.verify(base64.b64decode(signature_b64), data, padding.PKCS1v15(), hashes.SHA512())
        return True
    except (InvalidSignature, ValueError, TypeError):
        return False


def verify_device_identifier(public_key_pem, device_identifier: str, signature_b64: str) -> bool:
    """
    Nextcloud signs json([cloudId, tokenId]) with SHA-512 and returns
    deviceIdentifier = base64(sha512(that json)). So the identifier IS the signed digest:
    verify the signature against it as a pre-hashed SHA-512 value (same as the official proxy).
    """
    try:
        digest = base64.b64decode(device_identifier)
        if len(digest) != 64:
            return False
        key = load_public_key(public_key_pem)
        key.verify(base64.b64decode(signature_b64), digest, padding.PKCS1v15(), utils.Prehashed(hashes.SHA512()))
        return True
    except (InvalidSignature, ValueError, TypeError):
        return False


# ---------------------------------------------------------------- Firebase
class Fcm:
    def __init__(self, service_account_path):
        self.lock = threading.Lock()
        self.token = None
        self.token_expiry = 0
        self.account = None
        if DRY_RUN:
            log.warning("DRY RUN: notifications are logged, not sent to Firebase")
            return
        with open(service_account_path) as f:
            self.account = json.load(f)
        self.private_key = serialization.load_pem_private_key(self.account["private_key"].encode(), password=None)
        self.project_id = self.account["project_id"]
        log.info("Firebase project: %s", self.project_id)

    def _access_token(self):
        with self.lock:
            if self.token and time.time() < self.token_expiry - 300:
                return self.token
            now = int(time.time())
            b64 = lambda d: base64.urlsafe_b64encode(json.dumps(d, separators=(",", ":")).encode()).rstrip(b"=")
            header = b64({"alg": "RS256", "typ": "JWT"})
            claims = b64({"iss": self.account["client_email"], "scope": FCM_SCOPE,
                          "aud": self.account["token_uri"], "iat": now, "exp": now + 3600})
            signing_input = header + b"." + claims
            sig = self.private_key.sign(signing_input, padding.PKCS1v15(), hashes.SHA256())
            assertion = (signing_input + b"." + base64.urlsafe_b64encode(sig).rstrip(b"=")).decode()
            body = urllib.parse.urlencode({"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                                           "assertion": assertion}).encode()
            with urllib.request.urlopen(urllib.request.Request(self.account["token_uri"], data=body), timeout=15) as r:
                data = json.load(r)
            self.token = data["access_token"]
            self.token_expiry = now + int(data.get("expires_in", 3600))
            return self.token

    def send(self, push_token, data, high_priority, ttl_seconds):
        """Returns 'ok', 'unknown' (token no longer valid) or 'failed'."""
        message = {"message": {
            "token": push_token,
            # Data-only: the app decrypts and builds the notification itself.
            "data": data,
            "android": {"priority": "HIGH" if high_priority else "NORMAL", "ttl": f"{ttl_seconds}s"},
        }}
        if DRY_RUN:
            log.info("DRY RUN send to %s...: %s", push_token[:12], json.dumps(message)[:300])
            return "ok"
        url = f"https://fcm.googleapis.com/v1/projects/{self.project_id}/messages:send"
        req = urllib.request.Request(url, data=json.dumps(message).encode(), method="POST", headers={
            "Authorization": "Bearer " + self._access_token(), "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=15):
                return "ok"
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")[:500]
            if e.code == 404 or "UNREGISTERED" in detail or (e.code == 400 and "registration token" in detail.lower()):
                log.info("FCM token no longer valid (%s)", e.code)
                return "unknown"
            if e.code == 401:
                with self.lock:
                    self.token = None
            log.warning("FCM error %s: %s", e.code, detail)
            return "failed"
        except Exception as e:  # network problems
            log.warning("FCM request failed: %s", e)
            return "failed"


# ---------------------------------------------------------------- Nextcloud login check
_user_cache = {}
_user_cache_lock = threading.Lock()


def nextcloud_user(auth_header):
    """Returns the Nextcloud user id for this app login, or None. Positive answers are cached for 2 minutes."""
    if not auth_header or not auth_header.startswith("Basic ") or len(auth_header) > 1024:
        return None
    key = hashlib.sha256(auth_header.encode()).hexdigest()
    now = time.time()
    with _user_cache_lock:
        hit = _user_cache.get(key)
        if hit and hit[1] > now:
            return hit[0]
    req = urllib.request.Request(NEXTCLOUD_URL + "/ocs/v2.php/cloud/user?format=json", headers={
        "Authorization": auth_header, "OCS-APIRequest": "true", "Accept": "application/json",
        "User-Agent": "GamEmpiresPushGateway/1.1"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            user_id = json.load(r)["ocs"]["data"]["id"]
    except Exception as e:
        log.info("Session login check failed: %s", getattr(e, "code", type(e).__name__))
        return None
    with _user_cache_lock:
        if len(_user_cache) > 5000:
            _user_cache.clear()
        _user_cache[key] = (user_id, now + 120)
    return user_id


# ---------------------------------------------------------------- HTTP
def parse_body(handler):
    """Form (PHP style, incl. notifications[0]=...) or JSON. Returns dict of str -> str|list."""
    length = int(handler.headers.get("Content-Length") or 0)
    if length > MAX_BODY:
        raise ValueError("body too large")
    raw = handler.rfile.read(length) if length else b""
    query = urllib.parse.urlsplit(handler.path).query
    ctype = (handler.headers.get("Content-Type") or "").lower()
    if "application/json" in ctype and raw:
        data = json.loads(raw)
        return {k: v for k, v in data.items()}
    result = {}
    for source in (query, raw.decode("utf-8", errors="replace")):
        for key, value in urllib.parse.parse_qsl(source, keep_blank_values=True):
            if "[" in key and key.endswith("]"):
                result.setdefault(key.split("[", 1)[0], []).append(value)
            else:
                result[key] = value
    return result


class Handler(BaseHTTPRequestHandler):
    server_version = "GamEmpiresPush/1.0"
    store: DeviceStore = None
    fcm: Fcm = None

    def log_message(self, fmt, *args):
        log.info("%s %s", self.address_string(), fmt % args)

    def reply(self, code, payload):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def route(self):
        return "/" + urllib.parse.urlsplit(self.path).path.strip("/").split("/")[-1]

    def do_GET(self):
        if self.route() == "/health":
            return self.reply(200, {"status": "ok", "devices": self.store.count(), "dryRun": DRY_RUN})
        self.reply(404, {"error": "not found"})

    def do_POST(self):
        try:
            body = parse_body(self)
        except Exception:
            return self.reply(400, {"error": "bad request"})
        if self.route() == "/devices":
            return self.register(body)
        if self.route() == "/notifications":
            return self.notifications(body)
        if self.route() in ("/session", "/session-check"):
            return self.session(body, claim=self.route() == "/session")
        self.reply(404, {"error": "not found"})

    def session(self, body, claim):
        install_id = str(body.get("installId", "")).strip()
        if not (16 <= len(install_id) <= 64):
            return self.reply(400, {"error": "malformed"})
        user_id = nextcloud_user(self.headers.get("Authorization", ""))
        if user_id is None:
            return self.reply(401, {"error": "login not accepted"})
        current = self.store.session(user_id)
        if not claim:
            return self.reply(200, {"claimed": current is not None,
                                    "active": current is not None and current[0] == install_id})
        push_token = str(body.get("pushToken", "")).strip()[:4096]
        self.store.put_session(user_id, install_id, push_token)
        if current is not None and current[0] != install_id:
            log.info("Mobile session moved to a new phone; signing out the previous one")
            if current[1]:
                self.fcm.send(current[1], {"gamempires": "logout"}, high_priority=True, ttl_seconds=7 * 24 * 3600)
        self.reply(200, {"claimed": True, "active": True})

    def do_DELETE(self):
        try:
            body = parse_body(self)
        except Exception:
            return self.reply(400, {"error": "bad request"})
        if self.route() != "/devices":
            return self.reply(404, {"error": "not found"})
        ident = str(body.get("deviceIdentifier", ""))
        sig = str(body.get("deviceIdentifierSignature", ""))
        key = normalize_pem(body.get("userPublicKey", ""))
        row = self.store.get(ident)
        if not ident or not sig or not key:
            return self.reply(400, {"error": "missing fields"})
        if row is None or row[1] != key or not verify_device_identifier(key, ident, sig):
            return self.reply(403, {"error": "forbidden"})
        self.store.delete(ident)
        log.info("Device unregistered")
        self.reply(200, {"success": True})

    def register(self, body):
        token = str(body.get("pushToken", "")).strip()
        ident = str(body.get("deviceIdentifier", "")).strip()
        sig = str(body.get("deviceIdentifierSignature", "")).strip()
        key = normalize_pem(body.get("userPublicKey", ""))
        if not (20 <= len(token) <= 4096) or not (1 <= len(ident) <= 128) or not sig or "BEGIN PUBLIC KEY" not in key:
            log.warning("Registration rejected: malformed fields (token %d chars, id %d chars, key ok %s)",
                        len(token), len(ident), "BEGIN PUBLIC KEY" in key)
            return self.reply(400, {"error": "malformed"})
        if not verify_device_identifier(key, ident, sig):
            log.warning("Registration rejected: device signature mismatch")
            return self.reply(400, {"error": "signature mismatch"})
        existing = self.store.get(ident)
        if existing is not None and existing[1] != key:
            return self.reply(403, {"error": "identifier owned by another key"})
        self.store.put(ident, token, key)
        log.info("Device registered (%d total)", self.store.count())
        self.reply(200, {"success": True})

    def notifications(self, body):
        items = body.get("notifications", [])
        if isinstance(items, str):
            items = [items]
        success, failed, unknown = 0, 0, []
        for item in items:
            try:
                n = json.loads(item) if isinstance(item, str) else item
                ident = n["deviceIdentifier"]
                row = self.store.get(ident)
                if row is None:
                    unknown.append(ident)
                    continue
                push_token, user_key = row
                if hashlib.sha512(push_token.encode()).hexdigest() != n.get("pushTokenHash"):
                    unknown.append(ident)  # token changed on the phone; it will re-register
                    continue
                if not verify_sha512(user_key, base64.b64decode(n["subject"]), n["signature"]):
                    log.warning("Rejected notification with bad signature")
                    failed += 1
                    continue
                call = n.get("type") == "voip"
                result = self.fcm.send(push_token, {
                    "subject": n["subject"], "signature": n["signature"], "type": n.get("type", "alert"),
                }, high_priority=(n.get("priority") == "high" or call), ttl_seconds=45 if call else 4 * 24 * 3600)
                if result == "ok":
                    success += 1
                elif result == "unknown":
                    self.store.delete(ident)
                    unknown.append(ident)
                else:
                    failed += 1
            except Exception as e:
                log.warning("Bad notification entry: %s", e)
                failed += 1
        self.reply(200, {"success": success, "failed": failed, "unknown": unknown})


def main():
    Handler.store = DeviceStore(DB_PATH)
    Handler.fcm = Fcm(SERVICE_ACCOUNT)
    server = ThreadingHTTPServer((BIND, PORT), Handler)
    log.info("GamEmpires push gateway listening on %s:%d (%d devices)", BIND, PORT, Handler.store.count())
    server.serve_forever()


if __name__ == "__main__":
    main()

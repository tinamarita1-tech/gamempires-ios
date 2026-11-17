# GamEmpires push gateway — setup on the "talk" VM

What it does: Nextcloud sends each Talk notification (encrypted for the phone) to this gateway; the gateway
checks the signature and forwards it to Firebase, which wakes GamEmpires Messenger even when it is closed.
Nothing needs to change in Nextcloud itself — each phone tells Nextcloud to use
`https://talk.gamempires.com/push-gateway` when it registers.

Time needed: about 20 minutes. Where: SSH to the talk VM (192.168.10.151) as an admin user.

## 1. Get the Firebase server key (once)

1. Firebase console → project **gamempires-messenger** → ⚙ Project settings → **Service accounts**.
2. **Generate new private key** → a `.json` file downloads.
3. This file is a password for sending pushes. Do **not** put it in the app, the project folder, chat or email.
   Copy it straight to the VM (example from the Windows PC):
   ```
   scp gamempires-messenger-xxxx.json sagher@192.168.10.151:/tmp/service-account.json
   ```
4. Also check: Firebase console → Project settings → **Cloud Messaging** → "Firebase Cloud Messaging API (V1)" shows **Enabled**.

## 2. Install the gateway

```bash
sudo apt update && sudo apt install -y python3 python3-cryptography
sudo mkdir -p /opt/gamempires-push /etc/gamempires-push
sudo mv /tmp/service-account.json /etc/gamempires-push/service-account.json
sudo chown root:root /etc/gamempires-push/service-account.json
sudo chmod 600 /etc/gamempires-push/service-account.json
# copy gateway.py and gamempires-push.service from D:\AndroidBuilds - Copy\push-gateway\ to /tmp first (scp), then:
sudo cp /tmp/gateway.py /opt/gamempires-push/gateway.py
sudo cp /tmp/gamempires-push.service /etc/systemd/system/gamempires-push.service
sudo systemctl daemon-reload
sudo systemctl enable --now gamempires-push
sudo systemctl status gamempires-push --no-pager
curl -s http://127.0.0.1:8790/health        # -> {"status": "ok", "devices": 0, "dryRun": false}
```

The log should show `Firebase project: gamempires-messenger`. Logs: `journalctl -u gamempires-push -f`

## 3. Publish it through Caddy at /push-gateway

First check how Caddy runs:
```bash
systemctl is-active caddy          # "active"  -> Caddy runs on the VM itself (case A)
sudo docker ps --format '{{.Names}}' | grep -i caddy   # a name here -> Caddy runs in Docker (case B)
```

Add this inside the existing `talk.gamempires.com { ... }` site block, **above** the existing `reverse_proxy` line:

**Case A — Caddy on the VM** (`/etc/caddy/Caddyfile`):
```
handle_path /push-gateway/* {
    reverse_proxy 127.0.0.1:8790
}
```
then `sudo systemctl reload caddy`.

**Case B — Caddy in Docker:** the container cannot reach the VM's 127.0.0.1.
1. In `/etc/systemd/system/gamempires-push.service` change `PUSH_GATEWAY_BIND=127.0.0.1` to `PUSH_GATEWAY_BIND=172.17.0.1`
   (the Docker bridge address; check with `ip -4 addr show docker0`), then
   `sudo systemctl daemon-reload && sudo systemctl restart gamempires-push`.
2. Use `reverse_proxy 172.17.0.1:8790` in the `handle_path` block above (in the Caddyfile the container mounts),
   then reload Caddy: `sudo docker exec <caddy-container> caddy reload --config /etc/caddy/Caddyfile`.
   If the Caddy container uses `network_mode: host`, keep 127.0.0.1 instead.

## 4. Check both sides can reach it

```bash
curl -s https://talk.gamempires.com/push-gateway/health
sudo docker exec nextcloud-aio-nextcloud curl -s https://talk.gamempires.com/push-gateway/health
```
Both must print `{"status": "ok", ...}`. The second line is Nextcloud itself; if it fails, the Nextcloud
container cannot resolve/reach talk.gamempires.com (fix DNS/hairpin NAT on OPNsense for that name).

## 5. Test with a phone

1. Install the new app build, sign in, allow notifications (and on Android 14+: Settings gear →
   "Allow call screen on lock screen").
2. `curl -s http://127.0.0.1:8790/health` → `devices` should now be 1 (one per signed-in phone).
3. Lock the phone. From a PC browser, send that user a direct message → notification within seconds.
4. From the PC, start an audio call to that user → phone rings with Answer / Decline.
5. Watch `journalctl -u gamempires-push -f` while testing: each push logs a line; `FCM error` lines show the reason.

## Maintenance

- Backups: the device list is in `/var/lib/private/gamempires-push/devices.db`; it rebuilds itself when phones
  re-open the app, so it does not need backing up. The service-account key does (keep a copy offline).
- If the Firebase key leaks: Firebase console → Service accounts → delete that key, generate a new one,
  replace `/etc/gamempires-push/service-account.json`, `sudo systemctl restart gamempires-push`.
- Update: replace `/opt/gamempires-push/gateway.py`, then `sudo systemctl restart gamempires-push`.

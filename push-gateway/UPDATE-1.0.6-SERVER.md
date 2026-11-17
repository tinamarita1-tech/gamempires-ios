# Server steps for GamEmpires Messenger 1.0.6

Two jobs on the **talk VM** (user `talkadmin`):
1. Update the push gateway, so that only one phone can use an account at a time.
2. Turn on two-step login in Nextcloud.

Phones running 1.0.6 keep working before you do this. Until the gateway is updated, the one-phone rule just isn't enforced yet.

---

## 1. Update the push gateway (about 2 minutes)

From the Windows PC, in PowerShell, copy the three files:

```powershell
cd "D:\AndroidBuilds - Copy\push-gateway"
scp gateway.py gamempires-push.service install.sh talkadmin@talk.gamempires.com:~/push-gateway/
```
If `~/push-gateway` doesn't exist on the VM yet, run `mkdir -p ~/push-gateway` there first.

On the VM:

```bash
cd ~/push-gateway
sudo bash install.sh            # keeps the existing Firebase key, restarts the service
curl -s https://talk.gamempires.com/push-gateway/health
```

You should see `"status": "ok"`. The Firebase key stays where it is (`/etc/gamempires-push`). Don't copy it anywhere.

**How it works:** when someone logs in on a phone, the app tells the gateway "this phone is now the account's phone". The gateway checks the login with Nextcloud. If another phone had that account, the gateway sends it a sign-out push. That phone then deletes its own login and shows "Signed out: your account was signed in on another phone." A phone that was switched off signs out the next time it's opened.

Desktop and web logins are not affected. The rule is only for phones.

---

## 2. Two-step login (2FA) in Nextcloud

The app logs in through the Nextcloud login page, so any two-step method you enable here appears there automatically. No app change is needed.

All commands run on the talk VM:

```bash
OCC="sudo docker exec --user www-data nextcloud-aio-nextcloud php occ"

# Authenticator app codes (Google Authenticator, Microsoft Authenticator, Aegis…) – recommended
$OCC app:enable twofactor_totp

# Approve a new login from a device that is already signed in (e.g. the PC app) – optional
$OCC app:install twofactor_nextcloud_notification || $OCC app:enable twofactor_nextcloud_notification

# Backup codes (built in) – lets a user get in if they lose their phone
$OCC app:enable twofactor_backupcodes
```

Then each user (start with your own `sagher` admin account):
1. Open https://talk.gamempires.com and go to **avatar → Personal settings → Security**.
2. Turn on **TOTP (Authenticator app)** and scan the QR code with an authenticator app.
3. Click **Generate backup codes** and keep them somewhere safe.

### Make it required for everyone (only after the admin is set up)

```bash
$OCC twofactorauth:enforce --on                 # everyone
# or only for one group:
$OCC twofactorauth:enforce --on --group staff
$OCC twofactorauth:state sagher                 # check a user
```

If a user loses their phone: `$OCC twofactorauth:disable <user> totp`, then they set it up again.

### Email codes? Not recommended

The "Two-Factor Email" app is a third-party app. After Nextcloud upgrades it has broken and locked users out. If you still want it, you also need outgoing email set up first (Administration settings → Basic settings → Email server, e.g. Gmail SMTP with a Google app password). Enable it only for testing first, and never enforce email-only 2FA.

---

## Fingerprint (in the app, no server work)

In the app: **Profile → ⚙ Settings → Security → Fingerprint lock**. The app then asks for the fingerprint (or the phone's screen lock) every time it's opened, and again after 30 seconds in the background. Incoming calls still ring and can be answered while the app is locked.

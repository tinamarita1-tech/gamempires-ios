# GamEmpires Messenger – Do's and Don'ts (what worked / what didn't)

Lessons from building push notifications, calls, UI fixes and the push gateway for
`com.gamempires.messenger` (project copy: `D:\AndroidBuilds - Copy`).

---

## 1. Project & build workflow

### DO (worked)
- **Work only in `D:\AndroidBuilds - Copy`.** The original `D:\AndroidBuilds` stays untouched so you can compare the two and keep one.
- **Make a backup folder before every big change** (`_backup-before-firebase`, `_backup-before-ring-rework`, …). Rolling back is just a copy.
- **Bump `versionCode` / `versionName` every build** and name the APK with the version (`GamEmpires-Messenger-1.0.4.apk`). Without this, nobody could tell which build was on the phone.
- **Check the version on the phone before testing** (profile dialog → Settings → About, or Android App info). The 1.0.2 screenshot is what showed the old APK was still installed.
- **If a build fails strangely after an interruption** (Hilt "Could not get element … GeneratedInjector"), do **Build → Clean Project**, then **Assemble** again.
- **Keep at least 15–20 GB free on C:.** Gradle, AVD images and caches (`.gradle`, `.android`, `.lmstudio`) grow quickly.

### DON'T (didn't work)
- **Don't install APKs from the old project or from shared folders.** That's how 1.0.2 kept coming back with all the old bugs.
- **Don't build while C: is nearly full.** That produced a corrupt APK and froze Android Studio.
- **Don't believe an 11-second "BUILD SUCCESSFUL" means the code changed.** It was an up-to-date build of old output. Check the APK's timestamp and version.
- **Don't kill Studio or Gradle in the middle of a build** unless you also run a Clean afterwards.

---

## 2. Firebase / push notifications

### DO (worked)
- **Use Firebase (FCM)** so users, including the CEO, only need the messenger app. NextPush/UnifiedPush was rejected because it needs a second app.
- **Generate `res/values/firebase_config.xml` from `google-services.json`** instead of using the google-services Gradle plugin. Simpler and works with AGP 9.
- **Send data-only FCM messages with high priority** for calls, so the app's own code decides how to ring.
- **Use the Talk-style User-Agent** (`Mozilla/5.0 (Android) Nextcloud-Talk vX`) so Nextcloud treats the device as a Talk client and sends call pushes.
- **Register with Nextcloud first, then with the gateway**, and **unregister before logout**.
- **Keep the private key encrypted** (Android KeyStore) in DataStore.

### DON'T (didn't work)
- **Don't put the Firebase service-account key in the app, the project or a chat.** It lives only at `/etc/gamempires-push/service-account.json` on the talk VM, root-only.
- **Don't name it `service-account.json.json`.** Windows hides extensions, so check the real name.
- **Don't verify the device signature as a normal SHA512withRSA.** Nextcloud signs the JSON *before* hashing, so the gateway must verify with **Prehashed(SHA512)** over the decoded identifier. This was the cause of the gateway's `400` error.
- **Don't compare PEM keys with whitespace.** A trailing newline broke unregister. Normalize the PEM first.

---

## 3. Push gateway on the talk VM

### DO (worked)
- **Run the gateway as a systemd service** (`gamempires-push.service`, DynamicUser, LoadCredential) on `127.0.0.1:8790`.
- **Expose it through the existing Caddy** with `handle_path /push-gateway/*`, keeping Nextcloud on `handle → localhost:11000`. Back up the Caddyfile first (`/etc/caddy/Caddyfile.bak-push`).
- **Test with `/push-gateway/health`** from outside *and* from inside the `nextcloud-aio-nextcloud` container.
- **Use `DRY_RUN` first** to check the pipeline before sending real FCM messages.
- **Read the gateway log while testing.** It proved that pushes arrive instantly, so the remaining problems were in the app and on the phone.

### DON'T (didn't work)
- **Don't guess SSH usernames.** The VM user is `talkadmin`. A failed login isn't a hack. Check with `qm guest exec` from Proxmox if unsure.
- **Don't restart the VM during working hours** for "System restart required". Do it after hours.

---

## 4. Incoming calls (lock screen, ringing, decline)

### DO (worked / current approach in 1.0.4)
- **Ring immediately** when a call push arrives, then check in the background whether the call is still live. If it has ended, stop ringing and show "Missed call".
- **Use a tagged call notification** (`notify(roomToken, nid, …)`) with full-screen intent, Answer/Decline actions, `FLAG_INSISTENT` and a 45-second timeout.
- **Stop the ringtone only on Answer, Decline or a confirmed call end.**
- **On Decline, really end the call on the server** (`declineDirectCall`: join, `DELETE call?all=true`, leave). Otherwise the Windows client keeps showing the call.
- **Show over the lock screen** with `setShowWhenLocked` / `setTurnScreenOn` for call launches only, and reset them after the call.
- **Ask for the full-screen-intent permission** on Android 14+ (Settings → Notifications).
- **Use "Share call diagnostics"** to see exactly what happened (push time, priority, HTTP codes, liveness result).

### DON'T (didn't work)
- **Don't delay ringing to check liveness first** (the old 5-second check). The phone rang late or for just a moment.
- **Don't cancel the ringing notification when a "delete" push arrives** unless the server confirms the call ended. Talk sends deletes early.
- **Don't cancel call alerts when the activity opens.** That silenced the ringtone while the call was still going.
- **Don't treat a failed liveness check (no network on a locked phone) as "call ended".** Only `live == false` counts.

- **DO (1.0.5, confirmed working on the Xiaomi on 2026-11-14) wake the screen yourself** with a short `SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP` (WAKE_LOCK permission) when a call rings. Xiaomi often blocks the full-screen intent for sideloaded apps, so the notification alone may ring with the screen still dark.
- **DON'T rely only on `setFullScreenIntent`** to light the screen.

### Xiaomi / MIUI (required on the phone)
- **DO** turn on **Autostart**.
- **DO** set Battery saver to **No restrictions**.
- **DO** allow **Show on lock screen**, **Display pop-up windows while running in background** and **Full-screen notifications**.
- **DO** lock the app in Recents (padlock).
- **DON'T** expect calls to work on Xiaomi with default settings. MIUI kills the app process and cuts network while the phone is locked.

---

## 5. UI fixes

### DO (worked)
- **Keyboard opening on launch:** `windowSoftInputMode="adjustResize|stateHidden"` **plus** stop the search field from taking focus (`focusProperties { canFocus = searchActive }`), and enable it only when the user taps it.
- **Smoother chat transition:** wrap the list and detail panes in `AnimatedPane`, key the list state per chat, use a `reverseLayout` LazyColumn with `contentType`.
- **Settings:** rename "Appearance" to **Settings**, with sections (Appearance / Notifications / About), and put the gear top-right in the profile (presence) dialog.
- **Judge smoothness on a release-speed build** (the `staging` build type). Debug builds are always slower.
- **Icon:** make the gold vivid and crisp, size the logo at about 47% of the canvas, and provide all mipmap densities plus monochrome and a separate notification icon.
- **Restart the phone after changing the icon.** The launcher caches the old one.

### DON'T (didn't work)
- **Don't rely only on `stateHidden` or a focusable root.** The search field still grabbed focus and opened the keyboard.
- **Don't judge animation smoothness on the debug APK.**
- **Don't keep the dull, soft icon version.** Muted gold looked flat on the home screen.

---

## 6. Communication / process

### DO
- Tell the plan and make backups before changing things.
- Give one clear test procedure (lock phone → call from PC for 20 s → hang up → share diagnostics).
- Verify what is actually installed before debugging "nothing changed".

### DON'T
- Don't assume a fix works until it's tested on the real locked Xiaomi phone.
- Don't stack several untested changes. Test each build version separately.

---

## 7. Version 1.0.6 (startup logo, files, one phone per account, fingerprint, 2FA)

### DO
- **Show an in-app logo screen** (`StartupSplash`, about 1.4 s) on a normal start, and set the Android 12+ splash attributes (`values-v31/themes.xml`). On Android 12+ the old `windowBackground` logo barely shows.
- **Make every upload unique** (time + random key) so several files picked at once never share an ID or a file name.
- **Drop the local "uploading" bubble once the server copy arrives** (`dropSettledUploads`). Otherwise one file shows up as two bubbles.
- **Queue picked files** (multi-select for gallery and files) and send them with Send or Enter, one after another so they keep their order.
- **Enforce one phone per account through the push gateway** (`/session`, `/session-check`): the new login claims the session, the old phone gets a sign-out push, and the app also checks on every start.
- **Use Nextcloud's own two-step login** (TOTP plus backup codes). The app logs in through the web login page, so 2FA works there with no app change.
- **Make the fingerprint lock optional** (Settings → Security), with the phone's screen lock as fallback. Never lock out incoming calls.

### DON'T
- Don't keep a temp message *and* the server message for the same file.
- Don't upload a multi-select batch in parallel. The order gets mixed up.
- Don't rely on the "Two-Factor Email" app. It has broken after Nextcloud upgrades and locked users out.
- Don't enforce 2FA for everyone before the admin account has TOTP and backup codes.
- Don't store or log the app login in the gateway. It's used only to ask Nextcloud who the user is.

---

## Still open
- A background service so an ongoing call survives the app leaving the screen.
- A signed release build with the keystore (keep the keystore outside the project, backed up).
- A VM restart after hours.
- Nextcloud two-step sign-in (TOTP + backup codes): deferred on 2026-11-14 at the owner's request; set it up only if users ask for it (steps in push-gateway/UPDATE-1.0.6-SERVER.md, section 2).

---

## 8. Release signing (1.0.7, 2026-11-14)

- 1.0.7 is the first build signed with the **GamEmpires release key** (CN=Abdul Aziz, O=GamEmpires, C=PK, valid until 2076).
  Certificate SHA-256: `25:BA:64:6A:4E:87:71:99:E0:03:FE:8B:83:64:5E:2F:9B:C9:C1:72:8A:E0:47:F3:77:74:98:C5:81:7C:32:C0`
- **DO** build every future version with Build → Generate Signed App Bundle or APK → APK → release, using the same key file. Raise versionCode every time.
- **DO** keep the key file and its password backed up in two places, outside the project folder.
- **DO** keep `app/release/mapping.txt` for each release. It turns crash logs from the optimized build back into readable code.
- **DON'T** hand out test-signed (debug/staging) APKs to users. Phones with one installed must uninstall once before installing a release-signed version.
- **DON'T** ever create a new key for this app. Phones can only update to versions signed with the same key.

## 9. Calls in the background (1.0.8)

- **DO** run a foreground service (type `microphone`, "Ongoing call" notification with Hang up) while a call is connecting or active. Start it from the open app only.
- **DO** give the ongoing-call notification its own channel, so clearing the ringing alerts can't remove it.
- **DON'T** end a live call when the activity stops (the old `setForeground(false)` → `endCall()`).
- **TODO:** register the package name `com.gamempires.messenger` with Android developer verification (Android Developer Console, using the release key) before Google enforces it globally (2027).

## 10. Release builds and native libraries (1.0.9)

- **DON'T** ship a release build whose calls were only tested on a debug build. R8 (the release optimizer) removed `org.jni_zero.JniZero`, which WebRTC's native library looks up by name, so every call aborted the app ("JNI DETECTED ERROR ... java_class == null"). 1.0.7 and 1.0.8 had this bug; they're moved to `_withdrawn-do-not-share`.
- **DO** keep `org.jni_zero.**`, native methods and `@CalledByNative` members in `proguard-rules.pro`. Test a call on the release/staging build before every rollout.
- **DON'T** call `stopService()` on a service started with `startForegroundService()` before it reached `startForeground()`. Android kills the app (`ForegroundServiceDidNotStartInTimeException`). Send a stop action and let the service stop itself.
- 1.0.9: a call tested on a release-optimized build on the Redmi Note 9 Pro stayed connected through Home and screen lock, with no crashes (2026-11-14).
- Confirmed 2026-11-14: Hang up on the "Ongoing call" notification ends the call on the PC too.
- One-phone-per-account gateway update: skipped on 2026-11-14 at the owner's request. The app keeps working without it (its session check just gets no answer). Steps stay in push-gateway/UPDATE-1.0.6-SERVER.md, section 1, if it's ever wanted.

## 11. In-app login (1.0.10)

- Login screen has username + password fields. The app trades them once for a Nextcloud app password (`GET /ocs/v2.php/core/getapppassword`, Basic auth) and stores **only** the app password. The typed password is never saved, not even in the screen's saved state.
- "Log in with browser instead" (Login Flow v2) stays as a fallback. It's required if Nextcloud two-step login is ever turned on, because getapppassword refuses accounts with 2FA.
- The session shows in Nextcloud → Personal settings → Security → Devices & sessions as "GamEmpires Messenger (Android <model>)". Logging out deletes it.

## 12. Chat history reset on the server (2026-11-14)

- All Talk messages, call logs, reactions, notifications and chat shares were deleted directly in PostgreSQL (one transaction, after a Proxmox snapshot `before-chat-reset` of VM 103). Conversations and members were kept.
- **DON'T** leave a conversation with zero messages. Talk then answers HTTP 304, and both the web page and the app hang on "loading". Each room got one system line, "Administration cleared the history" (`history_cleared`, actor guests/cli).
- **DO** write timestamps as `now() AT TIME ZONE 'UTC'`. Plain `now()` stored Pakistan time, and chats showed "Tomorrow".
- **TODO (next app version):** treat HTTP 304 on chat history as "no messages", not an error.
- **TODO:** delete the `before-chat-reset` snapshot once all is fine (thin pool is overcommitted).

## 13. IT Help ticket bot (2026-11-14)
- Files: it-help-bot\ (bot.py, install.sh, it-help-bot.service, INSTALL.md). Runs on talk VM as systemd `it-help-bot`, 127.0.0.1:8791, Caddy route /it-help-bot/. Talk bot id 1.
- ONE room only: "IT Help" (j2dh8viz, everyone). Summaries/reports/!open/!stats/!show/new-ticket alerts go ONLY to sagher's "Note to self" (vwdiwj4e = DESK_ROOM_TOKEN). Never post summaries in IT Help.
- DO: commands start with `!` (`/` opens Talk's link picker). IT: !open !take N !note N text !done N text !cat N X !reopen N !show N !stats [YYYY-MM] !report YYYY-MM. Users: !mine.
- DO: `allow_local_remote_servers=true` in Nextcloud, otherwise Talk silently never delivers webhooks to the bot (curl test looked fine but bypasses that rule).
- DON'T: treat "help/hi/thanks" or < 12 chars as tickets (handled). IT staff (IT_STAFF=sagher) never open tickets.
- Update: scp bot.py to VM, `sudo install -m 644 ~/bot.py /opt/it-help-bot/bot.py && sudo systemctl restart it-help-bot`; verify with grep that the new code is really there.
- DO: type IT commands in sagher's Note to self (no ! needed there) so nothing shows in IT Help; bot never reacts to commands in IT Help.
- Clear test tickets: stop service, rm /var/lib/private/it-help-bot/tickets.db, start.
- Optional: NC_USER/NC_APP_PASSWORD in /etc/it-help-bot/config.env -> team names + monthly CSV in IT/Reports.

## 14. Gaming look (1.0.11, 2026-11-16)
- DO make mockups first and build only after the final mockup is approved (canvas "GamEmpires Chat – new look", boards "Android – …").
- Shared look pieces live in `ui/components/GamingKit.kt` (colours, shapes, background, hex icon). Change them there, not per screen.
- Custom shapes need the screen density: write them as `object : Shape { createOutline(size, layoutDirection, density) }`. `GenericShape` only gets the layout direction, not the density.
- Group pictures come from `/ocs/v2.php/apps/spreed/api/v1/room/<token>/avatar` (needs the app's login, so it goes through the app's own OkHttp client in Coil).
- Home tab status = latest non-system message in the "Announcements" group (matched by name). If that group is ever renamed, update `ChatListViewModel` (statusMessage).
- Backup before this change: `_backup-before-gaming-look\` (whole `app/src` and `app/build.gradle.kts`).

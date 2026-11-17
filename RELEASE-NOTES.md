# GamEmpires Messenger 1.0.1 — release-preparation copy

Folder: `D:\AndroidBuilds - Copy` (the original `D:\AndroidBuilds` was not changed).
Original versions of every edited file are in `_original-backup\` (release prep) and `_backup-before-firebase\` (before the notification work).

## What changed (vs. the original project)

| Area | Change | Why |
| --- | --- | --- |
| Backup | `allowBackup="false"`; backup and data-extraction rules exclude everything | Nextcloud login, tokens and chat cache never go into Google/ADB backups or phone-to-phone transfer |
| Unused libraries | Removed Google Play services location, CameraX (4 libs), Accompanist permissions | No code used them; smaller app, no Google Play dependency for now |
| Versions | `adaptive-navigation3` 1.3.+ → 1.3.0, `lifecycle-viewmodel-navigation3` 2.11.+ → 2.11.0 | Same versions as the working build, but repeatable |
| App version | versionCode 2, versionName 1.0.1 | Installs as an update over 1.0.0 builds signed with the same key |
| Release size | Release build ships only arm64-v8a + armeabi-v7a (emulator x86 libs dropped); R8 now shrinks the whole app, not just AndroidX/Kotlin | Expected ~25–35 MB instead of 136 MB (estimate until you build) |
| Shrink safety | Keep rules added for WebRTC (JNI), call engine, Moshi reflection, Retrofit, enums; line numbers kept for crash reports | So calls, chat and downloads keep working after shrinking |
| Logging | Network request logging only in debug builds | Release builds don't print server URLs to logcat |
| Signing | Release is signed only when a keystore is configured; otherwise use the Studio wizard | Release build no longer fails when no keystore properties exist |
| Credits | Settings (gear) dialog → "Open-source licences" + app version line; text in `app/src/main/assets/open_source_licenses.txt` | Licence notices required for distribution (WebRTC, Apache, BSD, MIT) |

Debug builds are unchanged: still all ABIs (emulator works), logging still on.

## Background notifications and incoming calls (Firebase) — added 14 Nov 2026

How it works: Nextcloud → GamEmpires push gateway (`https://talk.gamempires.com/push-gateway`, on the talk VM) →
Firebase → app. Message content is encrypted for each phone; the gateway and Google cannot read it.
Only the GamEmpires Messenger app is needed on the phone (no NextPush/ntfy).

| Part | What it does |
| --- | --- |
| `app/src/main/res/values/firebase_config.xml` | Firebase settings generated from `app/google-services.json` (no Google Services Gradle plugin needed — avoids AGP 9 plugin issues) |
| `push/PushRegistrar` | On every app start / new Firebase token: registers the phone with Nextcloud (push-v2) and with the gateway. On logout: unregisters before the app password is deleted |
| `push/PushStore` + `PushCrypto` | Phone RSA-2048 key (private key encrypted with the Android Keystore); exact Nextcloud key format; signature check (SHA-512) + decrypt (RSA-OAEP) — tested against OpenSSL, as Nextcloud uses |
| `push/PushMessagingService` | Receives push, rejects anything not signed by our Nextcloud, shows message notifications (with message text fetched from Nextcloud), rings for live one-to-one calls, shows missed/group calls as normal notifications, removes notifications Nextcloud marks as read |
| `push/PushNotifier` | Channels "Messages" and "Incoming calls" (ringtone, vibration, lock screen), call alert with **Answer** / **Decline**, 45 s ring timeout |
| `MainActivity` / `AudioCallViewModel` | Tapping a message opens that chat; Answer opens the app over the lock screen and connects; the lock-screen permission is removed again after the call |
| Settings (gear) | "Notification settings" and, on Android 14+, "Allow call screen on lock screen" |
| `push-gateway/` | The server gateway (`gateway.py`), systemd service and **SETUP.md** for the talk VM |

Order to switch it on:
1. Server: follow `push-gateway\SETUP.md` (Firebase service-account key → install gateway → Caddy route → health checks).
2. App: build and install this version, sign in, allow notifications.
3. Test (SETUP.md step 5): locked phone gets messages; a PC call rings the phone; Answer connects; Decline stops ringing.

Limits of this version (honest):
- The call itself still runs only while the app is on screen. Answering from the lock screen opens the app and
  works; leaving the app during a call still ends it (an ongoing-call service is the next step).
- Group calls are shown as a notification, not rung (the app only answers one-to-one calls).
- Needs Google Play services on the phone (normal on all standard Android phones).
- Phones that force-stop apps or use aggressive battery savers (some Xiaomi/Oppo/Vivo/Huawei) may delay
  pushes: set the app's battery usage to "Unrestricted" on those phones.
- Not compiled here — first build is in Android Studio. Unit test `PushCryptoTest` covers the crypto.

## Build the signed release APK (Android Studio)

1. File → Open → `D:\AndroidBuilds - Copy`. Let Gradle sync finish.
2. Build → Generate Signed App Bundle / APK → **APK** → Next.
3. First time: **Create new…** keystore. Save it OUTSIDE the project, e.g. `D:\Keystores\gamempires-messenger.jks`, alias `gamempires`, validity 25+ years.
   **Back up the .jks file and both passwords in two places. Losing them = no future updates for installed phones.**
4. Choose **release** → Create. Output: `app\release\app-release.apk`.
5. Because 1.0.0 was signed with the debug key, phones with the old test APK must **uninstall it once** before installing this release. All later releases update normally.

Optional (to build signed from the command line later), add to `C:\Users\<you>\.gradle\gradle.properties` — never inside the project:
```
MYAPP_RELEASE_STORE_FILE=D:/Keystores/gamempires-messenger.jks
MYAPP_RELEASE_STORE_PASSWORD=...
MYAPP_RELEASE_KEY_ALIAS=gamempires
MYAPP_RELEASE_KEY_PASSWORD=...
```

## Test checklist for the release APK (before giving it to staff)

- Login, chat list, open chat, send/receive messages
- Presence/status change
- Download an attachment, open it
- Light/Dark/System theme
- One-to-one audio call phone ↔ PC: both directions audible, hang-up clean
- Settings → Open-source licences opens and scrolls
- APK size check (expected ~25–35 MB)

If anything crashes only in release: send the logcat stack trace — it is usually a missing keep rule (one-line fix in `app/proguard-rules.pro`).

## Not verified / still open

- Not compiled here (no Android SDK on this machine) — first build happens in your Android Studio.
- WebRTC native third-party notices are linked, not bundled per exact version; confirm against version 150.7871.01 before wider/public distribution.
- Background notifications / incoming calls when the app is closed (Firebase work) — unchanged, still to do.
- The four old debug APKs (identical size, 136 MB each) were copied along; delete them from this folder if not needed.

## 1.0.11 — Gaming look (16 Nov 2026)

Same look as the Windows app (approved mockups on the "GamEmpires Chat – new look" canvas). Backup of the source before this change: `_backup-before-gaming-look\`.

| Area | Change |
| --- | --- |
| Theme | New dark and light colours; Orbitron (titles) and Rajdhani (names) fonts in `res/font`, credited in the open-source licences |
| Background | `ui/components/GamingKit.kt` → `GamingBackground`: deep base, amber glow top right, pink glow bottom left, dim RGB light lines with glow, vignette, faint GE crown watermark (`res/drawable-nodpi/ge_crown_watermark.png`) |
| Chat list | Slim angled search "Search or start a chat" (people matching the search are listed under the chats to start a chat); filter and new-chat buttons removed; tabs always visible with icons: **Home** (opens on start), All, Direct, Groups, Unread; Favorites heading removed (pinned chats stay on top); "// CHATS" heading; tighter rows, Rajdhani names, slanted unread counters, amber bar on the open chat |
| Pictures | Rounded-square tiles for everyone; groups now load their real picture (team emblems) from the server |
| Home tab | `ui/home/GamingHomeTab.kt`: "// PLAYER ONE / HELLO, NAME", **STATUS** card = latest message in the Announcements group (only CEO and HR post there) with the poster's name (tap opens Announcements), Events, Message reminders |
| Open chat | Same background; top bar with emblem, name in Orbitron, amber icons and line; angled bubbles (mine in an amber frame, others with an amber edge); angled message box and send button |
| App icon | Same see-through crown + GE as the Windows app, on a dark base with a soft amber glow (all mipmap sizes, adaptive + monochrome for themed icons). Old icons in `_backup-before-gaming-look\res-launcher-icons\` |
| Version | versionCode 12, versionName 1.0.11 |

## 1.0.12 — fixes after phone test (16 Nov 2026)

| Area | Change |
| --- | --- |
| Home status | If the last item in Announcements is a system note (e.g. "picture changed"), the card now looks back and shows the latest real post (`ChatRepository.latestUserMessage`) |
| Own picture | Profile picture link carries a version from the display name, so the phone stops showing the old cached "S" (from `sagher`) |
| App icon | Same as the Windows app: large see-through crown + GE with its glow, no tile behind it (1.0.11 tile icons kept in `_backup-before-gaming-look\res-launcher-icons-1.0.11-tile\`) |
| Version | versionCode 13, versionName 1.0.12 |

## 1.0.13 — swipe between tabs (16 Nov 2026)

| Area | Change |
| --- | --- |
| Tabs | Swipe between tabs with a real page swiper (Compose HorizontalPager): the next tab's page sits beside the current one, follows the finger and snaps smoothly; the tab highlight moves with the finger; tapping a tab slides to it. Each tab has its own list (`ChatListUiState.chatsByTab`). Pull-to-refresh and list scrolling unchanged |
| Own picture | Initials of the display name (like Windows) unless an uploaded photo exists (`X-NC-IsCustomAvatar`) |
| Version | versionCode 14, versionName 1.0.13 |

# GamEmpires Messenger — final handoff

Date: 14 November 2026 (Asia/Karachi)

Status: work stopped at the user's request. Foreground app fixes are delivered. Background notification/call work remains unfinished.

## Delivered and confirmed

- Online presence corrected; user confirmed it works.
- Attachment download handling and progress updated. User confirmed downloads work, a completion tick appears, and downloaded files can be opened.
- Light/dark/system appearance settings added; light-theme logo contrast corrected.
- Video-call button removed.
- Native one-to-one audio calling added inside the Android app, including calls to a Talk user on a PC browser.
- Native crash during call negotiation corrected. User confirmed clear audio in both directions.
- Mobile hang-up cleanup corrected so the PC leaves the direct call and the phone does not falsely ring back. User confirmed this is fixed.

## Latest delivered APK

[GamEmpiresChat-audio-fixed.apk](GamEmpiresChat-audio-fixed.apk)

Application ID: `com.gamempires.messenger`

Nextcloud server: `https://talk.gamempires.com`

This APK supports foreground calling. It must not be presented as supporting reliable locked-screen/background incoming calls yet.

## Validation already performed

- Last recorded full JVM suite: 33 tests passing.
- Native codec/SDP regression: 2 device tests passing before the later hang-up-only changes.
- Live mobile-to-PC call: user confirmed two-way audio.
- Live direct-call hang-up: HTTP 200 recorded; user confirmed cleanup and false-callback correction.

These are previously recorded results, not fresh test runs made for this handoff. See [audio investigation](docs/superpowers/specs/2026-11-14-audio-native-crash.md).

## Notification trial and final decision

The user installed and enabled UnifiedPush Provider 2.6.2 on Nextcloud and connected NextPush on the test phone. UP-Example received a local test notification. A subsequent PC-originated request returned `201 Created` and `{"success":true}`; the user confirmed receiving the notification after following the lock-phone test instructions. This proves that basic test delivery worked, not long-idle reliability, encrypted messenger delivery, or incoming-call integration.

The first PC attempts never sent a request because the endpoint variable was empty in a different PowerShell session. Using one session resolved that test setup issue.

**Final choice: Firebase/FCM.** Users, including the CEO, must only need the messenger app, without installing a separate notification receiver. The NextPush approach tested requires a distributor app on each phone and was therefore dropped. Firebase delivery assumes Google Play services are available on the target Android phones.

The desired path is:

`Nextcloud → compatible push gateway → Firebase Cloud Messaging → messenger app`

Keep Nextcloud as the chat and call backend. Firebase supplies notification delivery, not call audio or file storage.

## Work still required

1. Create/select a Firebase project and register Android application `com.gamempires.messenger`.
2. Add the project's `google-services.json` client configuration to the Android app and implement FCM token/message handling.
3. Configure a Nextcloud Notifications push-v2 compatible gateway able to send through that same Firebase project. Keep server service-account private credentials on the server, never in the APK.
4. Implement device key storage, push-v2 registration, signature verification and decryption, token refresh, account isolation, and logout unregistration.
5. Implement Android incoming-call notifications and explicit Answer/Decline actions, reject stale/duplicate alerts, and check live server call state before ringing.
6. Implement the required ongoing-call service and revise the foreground-only call lifecycle so supported calls can continue when the app backgrounds or the phone locks.
7. Test on the actual phones: locked screen, long idle/Doze, reboot, Wi-Fi/mobile data, caller cancellation, decline, answer, two-way audio, hang-up, logout and notification permission denial. Treat Android force-stop separately from normal background operation.

**No Firebase SDK/configuration, messenger push registration, compatible gateway deployment, or background-call implementation has been completed in this preparation phase.** The local preparation delivered here consists of documentation and the generic UnifiedPush delivery trial.

## Optional test cleanup

NextPush and UP-Example may be removed from the test phone, and UnifiedPush Provider disabled on Nextcloud if nothing else uses it. This cleanup was suggested but has not been verified or performed by the agent. Keep Nextcloud's normal Notifications app enabled.

## Resuming work

Start with Firebase project/client configuration and gateway selection. Preserve the working foreground audio/download behavior. Do not repeat the NextPush trial unless the user changes the single-app requirement.

Existing documents:

- [Notification trial notes](docs/push-notification-test.md)
- [Native audio scope](docs/superpowers/specs/2026-11-14-native-audio-calls.md)
- [Audio crash and hang-up investigation](docs/superpowers/specs/2026-11-14-audio-native-crash.md)

Reference documentation:

- [Firebase Android setup](https://firebase.google.com/docs/android/setup)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Nextcloud Notifications push-v2](https://github.com/nextcloud/notifications/blob/master/docs/push-v2.md)

# Native audio calls with background ringing

Status: foreground one-to-one implementation added; background ringing deferred by user until Firebase/push setup is available. See the foreground audio plan for delivered scope and remaining two-device verification.

## Required behavior

GamEmpires must make and receive audio calls inside the Android app. Incoming calls must ring when the app is backgrounded or the phone is locked. The user requested removal of the video button. Existing chat, downloads, and presence must continue working.

## Current code

The phone button is now connected to a native WebRTC audio coordinator. Native microphone permission, HPB signaling and foreground incoming-call detection are present. Push registration and Firebase configuration are still deferred. There is no background call foreground service in the temporary foreground-only version; calls end when the app stops being visible.

## Proposed implementation

Use native WebRTC audio, the authenticated Nextcloud Talk room/call/signaling APIs, and server-advertised STUN/TURN configuration. Discover internal versus external signaling rather than hardcode a particular VM deployment. Keep microphone and media transport in a call-scoped service, independent of the chat screen lifecycle. Implement one active call with mute, speaker/earpiece, hang up, answer, and decline.

For locked/background incoming calls use device push delivery compatible with this custom app's application ID, Android CallStyle notifications and supported incoming-call UI. Answering is an explicit user action before microphone capture starts. The microphone service must meet the target SDK's foreground-service and runtime permission rules. Do not replace push delivery with constant polling, a hidden web page, or a permanently open microphone.

Push registration follows Nextcloud Notifications push-v2: device keypair and signed device registration, encrypted payload validation, configured proxy delivery, token refresh and logout cleanup. A Firebase Android configuration alone is insufficient: the delivery proxy must be able to send to that Firebase project. Existing official Nextcloud app credentials must not be copied into a custom application.

## External information/setup needed

- Confirm whether a Firebase/FCM project and compatible push-delivery proxy exist for com.gamempires.messenger; provide the Android client configuration if they do. Keep server service-account private credentials on the push server, never in the APK.
- Inspect the Talk administration settings and authenticated signaling/settings response to determine the supported signaling mode and TURN deployment. Do not request or record long-lived admin passwords in project files.
- Two Android test devices/accounts are needed for release validation, including a mobile-data caller and a locked receiver.

## Acceptance checks

Call connects bidirectionally on LAN and across Wi-Fi/mobile-data, including TURN relay; decline, timeout, busy and hangup clean up resources; permission denial gives a usable message; answering a locked/background call works; logout unregisters push and ends the session; no web browser is launched by native calls. Test process termination, stale/duplicate push, token refresh, Bluetooth routing, audio interruption, and battery restrictions. Unit tests alone do not prove media or background delivery works.

## References

- https://nextcloud-talk.readthedocs.io/en/latest/internal-signaling/
- https://nextcloud-talk.readthedocs.io/en/latest/call/
- https://github.com/nextcloud/notifications/blob/master/docs/push-v2.md

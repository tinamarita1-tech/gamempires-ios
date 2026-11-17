# Foreground audio calls

## Scope and deployment

User deferred Firebase and background ringing. Implement one-to-one audio calls while both applications are open, using native WebRTC and the existing HPB backend. No external call webpage, embedded webpage, camera stream, new server shared secret, or Firebase setup. Moving the app to the background ends the call in this first version.

Server evidence: admin screenshots show HPB 2.1.1~docker, hello-v2, mcu, STUN/TURN configuration, and call E2EE disabled. Read-only secure WebSocket probe confirmed welcome on wss://talk.gamempires.com/standalone-signaling/spreed. This proves signaling endpoint reachability, not authenticated call or media success.

## Files and responsibilities

- `calls/TalkCallTransport.kt`: call-scoped cookies, authenticated OCS calls, cancelable HTTP.
- `calls/CallProtocol.kt`: audio-only flags and incoming-call eligibility.
- `calls/NativeAudioEngine.kt`: native microphone, audio focus/routing, audio-only publisher/subscriber peer connections, SDP and trickle ICE.
- `calls/AudioCallViewModel.kt`: foreground incoming-call polling, explicit answer/decline, runtime permissions, authenticated HPB hello/room handshake, publisher/subscriber signaling, one active call, timeout and cleanup.
- `calls/AudioCallHost.kt`: Compose call dialog and activity lifecycle binding.
- `MainActivity.kt` / `ChatDetailScreen.kt`: replace browser call action with in-app coordinator.
- `AndroidManifest.xml`: microphone and audio settings permissions. No camera permission.
- `app/build.gradle.kts`: pin native WebRTC dependency.

## Implementation checks

1. Write and run failing regressions for remote active direct-call detection, suppression of dismissed/self/group calls, and flags excluding video.
2. Add HTTP/session transport, native audio engine, signaling coordinator and native call UI.
3. Build and run the complete test suite, then independently review protocol and resource lifetime.
4. Ship a test APK with explicit two-device validation requirement. Background/push behavior remains deferred.

## Required phone validation

Install the same build on two phones and use separate Nextcloud accounts. Keep both apps open (receiver may be on the chat list). Start a direct audio call; receiver should ring after the next foreground check (about three seconds plus server delay). Answer and allow microphone access. Verify speech in both directions, mute/unmute, earpiece/speaker, hangup, no-answer timeout, rejection, denied permissions, repeat calls, logout, screen lock/background ending the call, Wi-Fi/mobile-data and TURN-only connectivity. Check server/session failure messages. Unit tests and successful WebSocket welcome are insufficient to claim end-to-end media is verified.

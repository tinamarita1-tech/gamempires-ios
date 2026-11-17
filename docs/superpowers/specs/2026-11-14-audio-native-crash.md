# Audio call native crash investigation

## Observed failure

Reproduced on a USB-connected Redmi Note 9 Pro, Android 12, ARM64, while calling a Talk web client. Publisher ICE connected successfully. When the HPB delivered the remote participant's offer, WebRTC's worker thread aborted with SIGABRT and `front() called on an empty vector` in `libjingle_peerconnection_so.so` (build ID `8939406b3b9fa259`). This is a native abort and cannot be recovered by Kotlin exception guards.

## Cause and change

The production PeerConnectionFactory registered an audio device module but no video encoder/decoder factories. The on-device regression confirmed that its video receiver codec list was empty. An audio-only client can still receive an audio/video SDP offer from the browser/HPB. The m150 video receive negotiation path dereferences `mapped_codecs.front()` without an empty check:
https://github.com/webrtc-sdk/webrtc/blob/m150_release/media/engine/webrtc_video_engine.cc#L2806

The factory now registers software video encoder and decoder factories so media negotiation has valid codec mappings. The client still creates no camera capturer or video source and requests no camera permission. After applying a remote offer, incoming video tracks are disabled and video transceivers are set inactive before producing the answer. SDP media sections and BUNDLE mids are preserved.

## Validation

- Original on-device capability regression failed because the production factory's video codecs were empty.
- Corrected capability regression passes on the same phone.
- Full on-device SDP exchange passes: create an audio/video offer, set remote description, disable receiving video, create/apply the answer, verify audio remains enabled and video is inactive, apply answer to the offering peer.
- Device test report: 2 tests, 0 failures, 0 errors.
- Full JVM suite: 32 tests, 0 failures, 0 errors.
- Corrected app installed for live phone-to-PC media retest. User confirmed clear audio in both directions. Logs confirmed publisher and receiver ICE connected and calls reached ACTIVE without the former abort.

## Hang-up follow-up

The user also observed that ending the mobile call left the web participant connected, followed by false incoming ringing. Logs showed the cleanup DELETE returning 404. HPB `bye` was sent before the HTTP hang-up: the HPB leave callback deletes the PHP session, racing the HTTP request that requires that session. Direct-call teardown now sends DELETE call with `all=true` before releasing the PHP/WebSocket session. Only successfully joined one-to-one calls use this behavior; local audio stops immediately, network cleanup is bounded, and new call startup waits for the prior cleanup. Normal direct-room users are owners and may end the call for both parties.

Primary source chain:
- https://github.com/nextcloud/spreed/blob/main/lib/Controller/CallController.php#L557
- https://github.com/nextcloud/spreed/blob/main/lib/Service/ParticipantService.php#L1123

The direct/group hang-up regression passes; full JVM suite is now 33 tests. Final live hang-up request returned HTTP 200 with all=true. User confirmed HR's PC also leaves and the false callback is fixed. Firebase/background ringing remains deferred.

The earlier safeguards for stale-call ringing and unhandled signaling exceptions remain; neither was sufficient to prevent this native codec abort.

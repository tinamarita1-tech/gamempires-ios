# Notification delivery trial

Decision (2026-11-14): Firebase/FCM is the production priority. UnifiedPush is an optional experimental alternative, not a replacement commitment.

Final outcome: the generic PC-to-phone delivery trial succeeded (HTTP 201 and user-confirmed receipt). The user subsequently dropped this approach because every phone would need an additional distributor app. Firebase/FCM is the chosen direction. The proposed integration steps below are historical trial notes, not implemented work. See [final handoff](../FINAL-HANDOFF-2026-11-14.md).

## Current state

The messenger supports foreground audio calls. It does not yet register for push notifications. Installing a distributor alone will not make it ring in the background. No push gateway or Firebase configuration has been connected or verified.

## Experimental route

Use a test account and device with NextPush (Nextcloud server app plus Android distributor). Confirm that the installed Nextcloud version supports the server app before installation. A Nextcloud Notifications push-v2 compatible gateway with UnifiedPush support and a messenger client integration are also required; NextPush is not that gateway by itself.

First prove a generic test notification reaches the distributor with the phone locked. Only then integrate registration and encrypted notification handling into a separate messenger test build. Do not change or uninstall the working messenger to test generic distributor delivery.

The client must verify signatures before decrypting notification payloads, reject stale or duplicate call alerts, fetch current call state before ringing, refresh registrations, and unregister on logout. A push must never automatically answer or enable the microphone.

Background call handling also requires Android incoming-call notifications and an appropriate ongoing-call service; the current foreground lifecycle cannot simply be reused unchanged.

## Acceptance checks

- Deliver on office Wi-Fi and mobile data with screen locked and app in background.
- Repeat after idle/Doze and device reboot; record delay and failures.
- Check battery restrictions and distributor availability on the actual phone.
- Ensure a caller hanging up before delivery does not produce a stale ring.
- Check duplicate alerts, decline, answer, two-way audio, hang-up and logout.
- Distinguish Android force-stop from normal background/process termination: do not promise delivery to a force-stopped app.

Record device/Android version, network, send/receive times and result without logging tokens, keys or notification contents. Passing generic delivery does not mean messenger incoming calls are implemented.

## Firebase remains primary

Later configure the Firebase Android app for `com.gamempires.messenger`, add its client configuration and configure a compatible push gateway for the same project. Keep service-account credentials on the server. Reuse notification validation and call handling, with only one active delivery provider per installation to avoid duplicate rings.

## References

- https://unifiedpush.org/users/distributors/
- https://github.com/nextcloud/notifications/blob/master/docs/push-v2.md
- https://firebase.google.com/docs/cloud-messaging

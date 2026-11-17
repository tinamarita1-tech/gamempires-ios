# GamEmpires Messenger — dependency and asset licence audit

Date: 14 November 2026. Scope: local Android project and latest delivered debug APK; intended internal office distribution.

## Conclusion

No confirmed copying conflict with another messenger was identified in this review. This is not a clean legal clearance: there is a confirmed third-party-notice packaging gap, and original source/asset ownership remains undocumented. Internal distribution does not automatically remove licence obligations.

No product code or APK was changed by this audit. Findings below remain to be addressed.

## Evidence examined

- Application manifest, Gradle declarations/version catalogue, Kotlin/XML source and bundled resource inventory.
- Cached `releaseRuntimeClasspath` dependency graph resolved successfully offline on this date.
- 224 unique coordinates from that graph, including BOMs/constraints and platform metadata: this is not a count of 224 libraries actually shipped. All had cached POMs and licence declarations, directly or through cached parent POMs.
- Embedded notice entry names in cached AAR/JAR archives and in `GamEmpiresChat-audio-fixed.apk`.
- Visual inspection of launcher PNG (gold GE on dark background) and placeholder PNG (generic grey shapes); launcher vector and Compose logo source; typography uses `FontFamily.Default`. No bundled TTF/OTF files found in the application resource inventory.
- Source scans for copyright/licence markers, third-party messenger names and source-reference URLs. No third-party messenger copyright header or copied-code attribution was located. Absence of a header is not proof of original authorship.

Audited APK SHA-256: `4F83F96BAB8AF05306F78523844649677A6CA13E1C8E565E8976810DFCAF4AF6`.

The current release graph is not a byte-for-byte SBOM of the delivered debug APK. Dynamic dependency declarations exist (`1.3.+`, `2.11.+`); freeze resolved versions before a final distribution audit.

## Findings

### 1. Confirmed: incomplete notice distribution, especially WebRTC

The application has no complete third-party licence bundle or user-facing credits facility. The inspected APK has ten notice/licence entries, mainly AndroidX and a Jakarta injection NOTICE; that NOTICE does not cover all other libraries. The cached WebRTC `150.7871.01` AAR contains no licence/NOTICE entries, and no separate WebRTC notice material was found in app resources or this distribution.

The WebRTC Android wrapper repository has an MIT licence, while the exact cached AAR POM declares BSD-3-Clause. Upstream WebRTC requires binary distributions to reproduce its copyright, conditions and disclaimer in accompanying materials. The wrapper's MIT label is not a replacement for native WebRTC and bundled-component notices. Its maintainer publishes an extensive native-component notice file.

Action: collect notices for the exact distributed native build, preserving component attribution and full terms, and distribute them with the APK or within the app. Confirm the maintainer notice bundle matches version `150.7871.01`; do not treat a floating `main` bundle as verified version-specific evidence. Also collect Apache/MIT/BSD notices for all shipped dependencies. A credits screen is a practical delivery method, not universally a mandatory licence format.

Sources: [WebRTC licence](https://webrtc.googlesource.com/src/+/refs/heads/main/LICENSE), [wrapper MIT licence](https://github.com/webrtc-sdk/android/blob/main/LICENSE), [maintainer native notices](https://github.com/webrtc-sdk/android/blob/main/Licenses/WEBRTC.md), [Apache 2.0 redistribution terms](https://www.apache.org/licenses/LICENSE-2.0).

### 2. Dependency metadata: mostly permissive terms, with Google SDK exceptions

Major declared families:

| Family | Declared licence/terms and review result |
| --- | --- |
| AndroidX, Compose/Material, Kotlin/coroutines, Hilt/Dagger, Retrofit/OkHttp/Moshi, Coil, Accompanist | Predominantly Apache 2.0 in cached POMs; preserve applicable licence and NOTICE materials. |
| WebRTC native SDK | BSD-3-Clause POM; additional wrapper/native third-party notices require review as above. |
| Datastore embedded protobuf | BSD-3-Clause declaration. |
| Checker qualifiers | MIT declaration. |
| Google Play services base/basement/location/tasks | Android SDK licence declaration; not blanket Apache-licensed dependencies. Review applicable Google SDK terms and embedded open-source notices. |

No GPL/AGPL declaration was detected in the parsed POM licence fields. This is a metadata result, not proof that every native transitive component was exhaustively cleared. In particular, the long WebRTC native component notice set has not been mapped to the actual binary's compile options.

Google's official guidance makes developers responsible for appropriately supplying applicable open-source notices: [Google Play services notices](https://developers.google.com/android/guides/opensource).

Location and CameraX libraries are declared, but no application Kotlin references to their APIs were found by the targeted scan. Consider removing unused dependencies in a separate change to reduce distribution scope. They were not removed during this audit.

### 3. Source provenance cannot be established from this workspace

No root licence, contributor agreement or source-origin ledger was found. No `.git` history was present for authorship tracing. The project's source resembles a custom Compose client speaking Nextcloud HTTP/WebSocket APIs; a local scan cannot establish whether earlier portions were copied, commissioned, generated, or independently authored.

Using a server API is not by itself evidence that the Android client incorporates that server's source. This audit did not compare all local source against Nextcloud Talk Android or every other messenger. If any upstream client implementation was copied, that project's actual licence and derivative-work obligations must be checked before asserting the app can remain proprietary.

Action: retain the original project/source provenance and permissions from its creator. Check any known copied snippets against their exact sources. No blanket licence was added to code whose ownership is unknown.

### 4. Assets and branding: no obvious third-party messenger mark found; rights unverified

Reviewed launcher and logo use GE/GamEmpires branding. No obvious WhatsApp, Telegram, Signal or Nextcloud logo was found in the inspected app branding. Fonts use system defaults, and Material icons are consumed through declared libraries. The placeholder PNG has no origin/licence record. The PNG icon, vector artwork, company name and logo also lack a documented rights record.

Action: confirm company ownership or permission for GamEmpires artwork and the original placeholder; retain creation/source records. If provenance is unavailable, replace the uncertain asset with newly authored artwork. Visual inspection is not a trademark search, image-similarity search, or proof of ownership.

## Remaining clearance work

1. Build an exact shipped-artifact inventory, excluding unused platform/BOM metadata, and pin dynamic versions.
2. Obtain and package exact-version licence and NOTICE texts, including WebRTC native dependencies and Google SDK open-source materials.
3. Confirm source and artwork ownership with the original project creator/company records.
4. Re-inspect the final release APK/AAB and accompanying notice materials after changes. The current working APK has not been rebuilt by this audit.

This review covers copyright/licence indicators and packaging evidence. It does not certify patents, codec royalties, trademarks, app-store policies, Firebase compliance, security, or legal compliance in every jurisdiction.

## Reproducible local evidence

- [Dependency graph](diagnostics/license-release-dependencies.txt)
- [POM licence inventory](diagnostics/license-inventory.csv)
- [Cached archive notice inventory](diagnostics/embedded-notice-inventory.json)
- [Delivered APK notice entries](diagnostics/apk-notice-entries.txt)
- [Inventory script](diagnostics/audit_licenses.py)

The script reads cached metadata without downloading dependencies, follows cached parent-POM licence declarations, and inventories notice entry names. It does not perform code-similarity analysis or determine actual native component inclusion.

---
name: mockup-first-rule
description: Enforce mandatory mockup presentation and explicit user approval before modifying code or compiling APKs.
always_on: true
---

# Mockup-First Workflow Rule

1. Whenever any design change, UI styling, theme, screen, or layout update is requested:
   - **DO NOT** modify Compose code or build APKs directly.
   - **FIRST**: Generate a visual, interactive HTML mockup with Light/Dark mode switcher.
   - Show the mockup link/artifact to the user.
2. **WAIT** for the user's explicit review and approval.
3. **ONLY AFTER APPROVAL**: Update Jetpack Compose code to match the approved mockup exactly, then build the APK.
4. **NEVER TOUCH BACKEND**: Maintain strict separation from Nextcloud OCS, WebDAV, Room, WebRTC, and push logic.

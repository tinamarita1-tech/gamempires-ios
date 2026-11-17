# Project Rules & Workflow Instructions: GamEmpires Messenger

## MANDATORY WORKFLOW: MOCKUP FIRST, APK ONLY AFTER APPROVAL

### Rule 1: Always Show Mockup First (MOCKUP FIRST PRINCIPLE)
* **Never jump straight to code changes or APK building** when the user asks for design, UI, color, theme, screen, or layout modifications.
* **Always generate a visual, interactive HTML mockup first** (e.g. `mockup.html`) with Tailwind CSS and theme controls (Light/Dark toggle, responsive container).
* Show the mockup to the user for review.

### Rule 2: Wait for User Approval Before Touching Code
* Do NOT edit Jetpack Compose files (`ui/`, `theme/`, etc.) or Android resources until the user reviews and **explicitly approves** the mockup.
* If the user requests tweaks to the mockup, revise the mockup and show it again until the user says it is approved.

### Rule 3: Compile APK Only After Explicit Approval
* Only after the user confirms and approves the mockup design:
  1. Implement the exact approved design tokens and components into Jetpack Compose.
  2. Run Gradle assemble commands (`assembleStaging` / `assembleDebug`).
  3. Deliver the compiled APK to the user.

### Rule 4: Strict UI Layer Boundaries (Zero Backend Alterations)
* Modify ONLY the UI layer (`ui/`, `res/values/colors.xml`, etc.).
* NEVER touch or alter backend logic, Nextcloud Talk OCS APIs, WebDAV sync, Room database schemas/daos, WebRTC/AudioCall logic, or push notification handlers.

### Rule 5: Zero Unauthorized Designing
* Strictly implement the approved design.
* Do not invent unsolicited gradients, sci-fi glows, extra watermarks, or unrequested styling changes.

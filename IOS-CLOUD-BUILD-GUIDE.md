# GamEmpires Messenger — iOS Cloud Build Guide (Apple iPhone)

Yeh guide aapko batati hai ke bina Mac kharide, apne Windows PC se iPhone ki **`.ipa` file** kaise build aur download karni hai.

---

## 1. Project Structure (Is PC par ban chuka hai)
Aapka iOS code **`iosApp/`** folder mein completely ready hai:
* **`iosApp/GamEmpiresMessengerApp.swift`**: Main SwiftUI app entry.
* **`iosApp/Theme/AppColors.swift`**: Exact Corporate Blue (`#2563EB`) aur Light/Dark tokens.
* **`iosApp/Views/`**:
  * `ChatListView.swift`: Corporate GE badge, pill search bar, All/Direct/Teams/Unread tabs.
  * `ChatDetailView.swift`: Corporate Blue outgoing bubbles, green audio call icon (`#10B981`).
  * `HomeTabView.swift`: 24pt rounded announcement, upcoming events, and reminder cards.
  * `PresenceSelectorSheet.swift`: Available / Meeting / Focus / Away picker.
* **`iosApp/GamEmpires.xcodeproj`**: Standard Xcode project configuration.
* **`.github/workflows/ios-build.yml`**: GitHub Actions automated cloud compilation.

---

## 2. Cloud Build Run Karne Ka Tareeqa (1-Time Setup)

### Step 1: GitHub par Repository banayein
1. [github.com](https://github.com) par jayein aur **New Repository** banayein (e.g. `gamempires-messenger`).
2. Is repository ko Private ya Public rakh sakte hain (dono par GitHub Actions free chalta hai).

### Step 2: Code ko GitHub par Push karein
Apne terminal mein yeh commands run karein:
```bash
git init
git add .
git commit -m "Add iOS app with Corporate UI and GitHub Actions workflow"
git branch -M main
git remote add origin https://github.com/<YOUR_USERNAME>/<REPO_NAME>.git
git push -u origin main
```

### Step 3: Cloud Build automatically start hoga
1. Apne GitHub repository ke **"Actions"** tab par jayein.
2. Wahan **"Build iOS App"** workflow run hona shuru ho jayega.
3. Yeh GitHub ke cloud macOS server par Xcode run karke app compile karega (~2-3 minute lagenge).

### Step 4: .IPA Download karein
* Build complete hone par summary page par **`GamEmpires-iOS-IPA`** ka button aayega.
* Us par click karein aur aapki **`GamEmpires-iOS.ipa`** file download ho jayegi!

---

## 3. iPhone par Install Karne Ka Tareeqa

iPhone par custom `.ipa` install karne ke liye yeh free tools sab se aasan hain:
1. **Sideloadly (Recommended & Free on Windows)**:
   * PC par [sideloadly.io](https://sideloadly.io) download karein.
   * iPhone ko USB cable se PC ke sath connect karein.
   * `GamEmpires-iOS.ipa` ko Sideloadly mein drag & drop karein aur **Start** dabayein.
   * App direct aapke iPhone ki screen par aa jayegi!
2. **AltStore**:
   * Wireless installation ke liye use hota hai.

# Wherez Android (Deep)

Android app where the user chooses their own level of location privacy.  
Converted from the original iOS `wherez` app.

---

## Project Structure

```
wherez-android/
├── android/          ← Android Studio project (open this folder in Android Studio)
│   ├── app/
│   │   └── src/main/java/com/example/deep/
│   │       ├── MainActivity.kt       ← Map, location, contacts, API calls
│   │       └── SettingsActivity.kt   ← Privacy level picker
│   └── ...
└── wherez-server/    ← Node.js backend (run separately in VS Code / terminal)
    ├── app.js
    ├── package.json
    └── routes/
```

---

## Running the Android App

1. Open **Android Studio**
2. Click **File → Open** and select the `android/` folder
3. Wait for Gradle sync to finish
4. Click the green **▶ Run** button

---

## Running the Node.js Server

Open `wherez-server/` in VS Code, then in the terminal:

```bash
npm install
node app.js
```

Server runs on **http://localhost:3001**

Test it: open `http://localhost:3001/?id=test` in your browser.  
You should see: `{"id":"test","msg":"I am at Hawaii!!!"}`

---

## Connecting the App to Your Local Server

In `android/app/src/main/java/com/example/deep/MainActivity.kt`, find:

```kotlin
private const val WHEREZ_SERVER_URL = "http://76.103.100.81:3001"
```

Change to:
- **Android Emulator**: `http://10.0.2.2:3001`
- **Real Android phone on same WiFi**: `http://YOUR_PC_IP:3001`  
  (find your PC's IP with: Start → CMD → `ipconfig` → look for IPv4 Address)

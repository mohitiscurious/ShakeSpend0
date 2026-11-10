# ShakeSpend – Android starter project

An offline-first expense tracker prototype using Kotlin, Jetpack Compose, Room and the Android accelerometer.

## Open the project
1. Install Android Studio (latest stable).
2. Open this folder as a project.
3. Let Gradle sync.
4. Connect an Android phone with USB debugging enabled, or use an emulator.
5. Run the `app` configuration.

## Current prototype features
- Local Room database (no account, server or bank connection).
- Manual expense entry.
- Monthly total and recent-expense list.
- User-controlled shake-detection foreground service.
- Shake detection posts a notification; tapping it opens the quick-entry screen.

## Important Android limitations and publishing work
- Background sensor monitoring is implemented as a user-started `specialUse` foreground service. Android and Google Play may require justification/review for this foreground-service type. Do not assume Play approval.
- Android/OEM battery management may stop or throttle long-running services. Test on multiple devices and explain the persistent notification clearly.
- The service must be enabled by the user while the app is visible. This prototype does not auto-start after reboot.
- Notification permission is requested on Android 13+.
- Before publishing: implement proper service state restoration, reboot behavior if permitted, CSV/PDF export, editing/deleting, monthly date-boundary tests, privacy policy, app icon, screenshots, accessibility checks, crash testing, signing key, and Play Console declarations.
- This is source code for a development prototype, not a verified release build or a Play Store-approved app.

## Privacy
Expense records are stored in the app's private Room database on the device. The prototype does not transmit expense data.

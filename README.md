# NepTune - A social network for samples

NepTune is an Android app and backend that makes capturing, editing, and sharing short audio samples 
simple and social. It combines a lightweight sound-design toolset (resampling without pitch 
distortion, filters, reverb, fades) with social features (sharing, subscriptions, favorites, stats).

This project has been done for the CS-311 course at EPFL given by George Candea

## Install the app

You can download the last apk release [here](https://github.com/NepTune-SwEnt/NepTune/releases)

## Project overview

NepTune helps creators capture real-world sound, apply basic audio processing, and share results with a community. The Android app (in `app/`) includes audio capture and editing features and integrates with Firebase for auth, storage and server-side processing. Cloud functions (in `functions/`) perform heavier tasks such as server-side preview generation and FFmpeg-based processing.

Key features
- Record audio from the device microphone
- Basic sound design: resampling (without pitch shift), filters, reverb, fade in/out
- Export as WAV or as a portable project file
- Social features: share samples, follow users, favorites, stats
- Server-side sample processing & preview generation (Firebase Functions + FFmpeg)

## Screenshots

![Capture screen](docs/images/capture.png)
![Edit screen](docs/images/editor.png)

## Build the project
### Setup
1. Install JDK 11 and Android Studio.
2. Configure local Android SDK in Android Studio and set `local.properties` if needed (Android Studio usually writes this automatically):
   - local.properties (example):
     ```
     sdk.dir=/path/to/Android/Sdk
     ```
3. Open the project in Android Studio by selecting the repository root or `settings.gradle.kts`.
4. Gradle will sync dependencies. If Android Studio asks to upgrade Gradle plugin or Kotlin plugin, prefer the versions defined by the project unless you have a specific reason.
5. Ensure `app/google-services.json` is present (this repo already contains one at `app/google-services.json`). If you need to use a different Firebase project, replace that file with one from the Firebase console.
6. Native libraries: `app/libs/` contains `TarsosDSPKit-release.aar` which is included by the Gradle build. No extra steps required unless you replace the AAR.

### Firebase emulator

1. Ensure you have Node.js 22 installed (the functions `package.json` sets `engines.node` to 22).
2. Install Firebase CLI (`npm install -g firebase-tools`) and log in: `firebase login`.
3. Navigate to the functions folder and install dependencies:
    ```
    cd functions
    npm install
    ```
4. Run the emulator: `firebase emulators:start`

### Build, run and test commands (quick reference)

Android (from repo root):
- Build debug APK:
  `./gradlew :app:assembleDebug`

- Build release APK (ensure signing config / keystore is available):
  `./gradlew :app:assembleRelease`

- Run unit tests (JVM/Robolectric):
  `./gradlew :app:testDebugUnitTest`

- Run connected instrumentation tests (device/emulator required):
  `./gradlew :app:connectedDebugAndroidTest`

- Generate combined JaCoCo report (unit + android tests; configured in `app/build.gradle.kts`):
  `./gradlew :app:jacocoTestReport`

## CI and release notes

- Keep secrets out of the repository. Configure CI secrets for:
  - Firebase token (`FIREBASE_TOKEN`) or use service account auth
  - Keystore file and passwords (or upload the keystore as a protected CI artifact)
- Example automated steps for CI (GitHub Actions):
  1. Checkout
  2. Set up JDK 11 and Android SDK
  3. Cache Gradle & Node modules
  4. Build `./gradlew assembleDebug` and run `:app:testDebugUnitTest`
  5. Optionally run `npm --prefix functions ci && npm --prefix functions run build` to validate functions
  6. Deploy functions only from protected branches or tags using `firebase deploy --only functions` with a `FIREBASE_TOKEN` stored in secrets

## Contributing & PR process

- We use `pull_request_template.md` in the repository root to standardize PRs. Please follow it when opening PRs.
- Branching model: create feature branches from `main` (or your current default branch), open a PR and request reviews.

## Troubleshooting

- Gradle sync errors:
  - Run `./gradlew --refresh-dependencies` and ensure Android SDK versions are installed.
  - Check `local.properties` contains a valid `sdk.dir`.
- Emulator / device issues:
  - Use `adb devices` to confirm your device is visible.
  - For audio capture testing consider using a hardware device rather than emulator (some emulators have limited audio input support).
- Cloud functions failing locally:
  - Ensure Node 22 is active (use nvm to switch). Check `node -v`.
  - If FFmpeg operations fail, confirm `ffmpeg-static` binary is compatible with your environment; logs are printed in the emulator output.
- Tests OOM on CI:
  - The project config increases test JVM heap; in CI you can increase runner memory or adjust Gradle test JVM args.

Security & secrets
- Never commit service account JSONs, keystores, or passwords.
- Use CI provider secrets or `~/.gradle/gradle.properties` for local secrets.

Contact / Maintainers
- See the repository owners and the project team for questions. Use issues for bug reports and feature requests.

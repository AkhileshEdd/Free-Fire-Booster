# Ember — Game Companion

An ad-free native Android preparation and diagnostics toolkit for Free Fire and Free Fire MAX. Built for a one-time paid Google Play download. Working product name: **Ember**; repository: **Free-Fire-Booster**.

## What is implemented

- Charcoal, sage and amber native interface with five tabs, responsive scrolling and system-bar/keyboard insets.
- Device readiness checks using actual battery level/temperature, internet validation and Battery Saver status. Available RAM, storage and display refresh readings are labeled accurately.
- Targeted detection and launching of Free Fire / Free Fire MAX; missing-game and unavailable-settings handling.
- Optional, explicitly confirmed 12-sample TCP connection tests to Cloudflare or Google, with median, adjacent-sample variation, failures and a graph. Tests cancel when leaving the screen/app.
- Three saved preparation profiles per game, profile-specific notes and resettable checklists. Recommendations are manually applied in-game, not written to game files.
- Persistent manual session timer, start/end battery snapshots, subjective session ratings, notes, 200-record history and CSV export using Android's document picker.
- Android settings shortcuts, privacy details and local data deletion.
- No ads, analytics, billing SDK, VPN, root, accessibility automation, overlay or background service. No runtime libraries beyond Android itself.

This is a gaming companion, not an FPS unlocker, RAM cleaner or network accelerator. Session duration includes time away from the game until the user explicitly ends it. Device readings are snapshots, not continuous in-game telemetry.

## Build

Requirements: JDK 17, **Gradle 8.13**, Android SDK platform 36 and build tools 35.0.0. Android Studio can open the root project; configure a local Gradle 8.13 installation. Minimum Android 8 (API 26), target API 36, Android Gradle Plugin 8.11.1, Java 17.

```sh
# With Gradle 8.13 installed (the repository does not bundle a wrapper binary):
gradle wrapper --gradle-version 8.13
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
# With a connected emulator/device:
./gradlew connectedDebugAndroidTest
```

GitHub Actions installs the toolchain, builds a debug APK and unsigned release AAB, runs unit tests/lint and an API 35 emulator smoke test. Download `ember-build-and-reports` from a successful workflow run. Debug APKs are for testing; the unsigned AAB cannot be uploaded as a production release until signed with your upload key.

The smaller `bash scripts/check-core.sh` check needs a JDK but no Android SDK. See [validation](docs/VALIDATION.md) for actual verification status and outstanding device tests.

## Paid distribution

Set the app to paid in Play Console; no in-app purchase is needed to unlock features. All implemented features are available. A **₹79–₹149 introductory experiment** is a positioning hypothesis, not validated willingness to pay. Do not position this first version as a high-priced performance accelerator. Competitors' public download counts do not establish paid conversion or current revenue.

Before publishing: choose the final app ID/name, sign the build, host the privacy policy, complete Play Console declarations and required testing, and verify on physical phones. See [release checklist](docs/RELEASE.md), [market research](docs/MARKET_RESEARCH.md), [store copy](docs/STORE_LISTING.md), and [privacy policy draft](docs/PRIVACY.md).

Free Fire and Free Fire MAX are trademarks of their owners. Ember is independent and not affiliated with Garena. No Garena artwork or logos are included.

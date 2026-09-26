# Release checklist

This source build is not a published or approved Play Store product.

1. Confirm final branding, application ID (`in.akhilesh.ember`) and developer identity before the first upload. Verify name availability. The application ID cannot later change for the same listing.
2. Build with JDK 17 / Gradle 8.13 / platform 36. Resolve unit, lint, build and device-test failures in Actions. Test both debug and release builds.
3. Test on real low-memory Android phones and Android 14–16 devices with Free Fire / MAX installed. Check cutouts, gesture and three-button navigation, 200% font size, landscape, offline mode, unavailable games/settings, network timeout/cancellation, app recreation, screen lock, process death, session resume and CSV export to local/cloud document providers.
4. Confirm that session timer semantics and manually applied profiles are understood by prospective buyers. Check the quality and usefulness of recommendations on physical devices; none are marketed as device-tested presets.
5. Generate an upload keystore securely. In Android Studio use Build → Generate Signed Bundle / APK → Android App Bundle. Choose release. Keep passwords and keys outside Git and back them up securely. Enroll in Play App Signing. CI produces an UNSIGNED release AAB.
6. Host the finalized privacy policy on a stable public URL; fill in effective date/support contact. Complete the Data safety questionnaire from actual behavior, including optional third-party network tests. No ads declaration; no billing permission because payment is at download.
7. Set paid pricing in Play Console before first publication. Check current rules about free-to-paid conversion and regional pricing. ₹79–₹149 is only an experiment. Do not claim premium pricing is validated by this research.
8. Complete content rating, target audience, app-access, contact information, country availability and required verification/testing for your developer account. Follow the current Console gates; requirements vary by account.
9. Create real device screenshots and a 512×512 Play icon / 1024×500 feature graphic. The repository contains a launcher vector, not a complete Play media kit. Do not use game trademarks as your app's identity.
10. Upload the signed AAB to internal testing first. Inspect pre-launch reports, test purchases through the appropriate Play process, then stage production rollout after feedback. Publication and approval are not guaranteed.

## Permissions audit

Only INTERNET and ACCESS_NETWORK_STATE. Targeted package queries for `com.dts.freefireth` and `com.dts.freefiremax`. No QUERY_ALL_PACKAGES, overlay, accessibility, VPN, usage access, broad storage, background service or notification policy access.

## Known scope boundaries

- No overlay or in-game FPS / continuous thermal telemetry.
- No automatic changes to DND, brightness, game settings or system power policies.
- No relay servers / network acceleration.
- No automatic game-exit detection. Users explicitly end sessions.
- Network results are in-memory; rotating/closing the activity clears them. Saved profiles and sessions persist.
- English UI only in this first version.
- No licensing backend. Paid access is distributed through Google Play; debug APK is unrestricted for testing.

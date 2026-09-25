# AA Mirror Probe v0.4

Purpose: determine whether a Galaxy phone can expose a custom full-screen Activity to Android Auto/Uconnect without an external dongle, and separately verify Android MediaProjection permission.

Important result from Google's current Android Auto documentation: as of September 2026, direct full-screen parked Activities are supported on Android Auto, but the only currently supported parked-app category is games. Android Auto exits parked apps when vehicle motion is detected. This probe therefore uses the legitimate CAR_LAUNCHER parked-app route and does not attempt to disguise itself as another app category.

## Test
1. Build/install the debug APK on an Android 15+ phone.
2. Enable Android Auto developer mode if needed for local testing.
3. Connect to Uconnect using Android Auto.
4. While parked, look for "AA Mirror Probe".
5. Open it. If the AA MIRROR PROBE screen appears on Uconnect, Test 1 passes.
6. Tap REQUEST PHONE SCREEN CAPTURE on the phone/head unit as available and grant the Android screen-capture prompt. If status changes, Test 2 passes.

## v0.4 test
1. Open AA Mirror Probe on the phone.
2. Tap START SCREEN CAPTURE and approve Android's warning.
3. Connect Android Auto and, while parked, open AA Mirror Probe on Uconnect.
4. The car Activity polls frames produced by the phone-side foreground capture service.
5. The Android Auto Activity uses the required landscape declaration so Uconnect recognizes it. This restores the proven v0.2 launcher behavior while retaining a higher version number for installation over v0.3.

This is still a capability probe. It uses Android's normal MediaProjection consent and Android Auto's parked-app route; it does not bypass motion restrictions.

# Brannen Voice Control

An Android accessibility controller for Bobby's Galaxy S24 Ultra. This work reuses the existing phone-control project and keeps its application ID so test builds can update the installed prototype.

## v0.8 production pass

- Native control screen with separate master-control and background-listening switches.
- Android speech recognition, preferring on-device recognition when Android reports it available. No OpenAI API key or paid model service is used. If on-device recognition isn't available, Android's selected speech service may use remote processing.
- Navigation and device commands: Home, Back, Recents, notifications, quick settings, volume, scrolling, and swipes.
- Launch installed apps by spoken launcher name, with aliases for common apps.
- Voice Access-style numbered targets (`show numbers`, `tap 4`) and a 3×3 screen grid (`show grid`, `tap grid 2 3`). Labels disappear after 15 seconds.
- Tap or long-press visible text, type into the focused text field, and read accessible screen text aloud.
- Confirmation gate before recognized high-impact controls such as Send, Delete, Pay, Purchase, Submit, Call, or Transfer.
- Voice control is opt-in, foreground-notification visible, and refuses to act while the master switch is off.

## Install and first run

1. Install the debug APK from the `BrannenVoiceControl-v0.8.0-Android` GitHub Actions artifact.
2. Open Brannen Voice Control and enable its Accessibility Service in Android Settings.
3. Return to the app, turn on `PHONE CONTROL`, then turn on `BACKGROUND LISTENING` and grant microphone permission.
4. Test with `show numbers`, `tap 1`, `open Gallery`, `scroll down`, or `what's on screen?`. If a button looks like a send/delete/payment action, say `confirm` to proceed or `cancel` to drop it.
5. Stop listening with the app switch or the persistent notification action. Turning `PHONE CONTROL` off also stops the listener.

## Current boundaries

This is the first clone-production build, not a claim of feature parity with Google's Voice Access. Android's recognizer returns one utterance at a time, so the foreground service restarts recognition after each phrase. Android documents `SpeechRecognizer` as unsuitable for continuous recognition because it can consume battery and bandwidth; this prototype is therefore not a custom always-hotword engine. Some apps hide text from accessibility services; those screens cannot be read, tapped by label, or typed into reliably. Coordinate gestures, visible-node labeling, and app launch also need device testing on the S24 Ultra. The Android user must grant the accessibility and microphone permissions; the app cannot enable those permissions itself.

Accessibility permission is used only to carry out the user's explicit commands. It is not enabled automatically. The master switch defaults off and is checked again for every action.

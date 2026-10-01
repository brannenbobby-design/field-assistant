# Phone Control Prototype

Proof-of-concept Android accessibility control core for Bobby's Galaxy S24 Ultra.

Control is OFF by default. Commands are rejected unless the in-app master switch is ON. Android Accessibility permission must be manually granted by the phone owner. This prototype has no OpenAI API dependency or API key.

Initial commands: HOME, BACK, OPEN_APP, TAP_TEXT.

Test: install the APK; enable Phone Control Prototype under Android Accessibility settings; return to the app and turn PHONE CONTROL ON; test the buttons; then turn it OFF and verify commands stop.

Next milestone: after local controls are verified on the S24, add a narrow authenticated bridge/MCP command surface.

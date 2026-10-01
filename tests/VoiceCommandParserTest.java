package com.brannenservices.phonecontrol;

public final class VoiceCommandParserTest {
    public static void main(String[] args) {
        expect("go home", "HOME", null);
        expect("Open Gallery", "OPEN_NAMED_APP", "gallery");
        expect("show me numbers", "SHOW_NUMBERS", null);
        expect("tap number 14", "TAP_NUMBER", "14");
        expect("tap grid 2 3", "TAP_GRID", "2,3");
        expect("tap top left", "TAP_REGION", "top_left");
        expect("tap center", "TAP_REGION", "center");
        expect("tap bottom right", "TAP_REGION", "bottom_right");
        expect("tap search bar", "FOCUS_FIELD", "search");
        expect("tap text field", "FOCUS_FIELD", "text");
        expect("tap Continue", "TAP_TEXT", "continue");
        expect("confirm action", "CONFIRM", null);
        expect("cancel", "CANCEL", null);
        expect("scroll down", "SWIPE_UP", null);
        expect("type Bobby’s test message", "TYPE_TEXT", "Bobby’s test message");
        if (VoiceCommandParser.parse("random unrelated sentence") != null) throw new AssertionError("unknown phrase must not trigger an action");
        if (!VoiceCommandParser.parse("read this screen").readScreen) throw new AssertionError("screen reading phrase not recognized");
        if (!VoiceCommandParser.parse("stop listening").stopListening) throw new AssertionError("stop phrase not recognized");
        System.out.println("VoiceCommandParserTest: 18 checks passed");
    }

    private static void expect(String phrase, String action, String argument) {
        VoiceCommandParser.Result result = VoiceCommandParser.parse(phrase);
        if (result == null || !action.equals(result.action) || (argument == null ? result.argument != null : !argument.equals(result.argument))) {
            throw new AssertionError(phrase + " => " + (result == null ? "null" : result.action + "/" + result.argument));
        }
    }
}

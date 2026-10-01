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
        expect("scroll down", "SCROLL", "down");
        expect("type Bobby’s test message", "TYPE_TEXT", "Bobby’s test message");
        expect("tap second Continue", "TAP_TEXT_ORDINAL", "2|continue");
        expect("find Submit", "FIND_TEXT", "submit");
        expect("tap it", "TAP_HIGHLIGHTED", null);
        expect("show commands", "SHOW_HELP", null);
        expect("repeat that", "REPEAT_LAST", null);
        expect("undo", "UNDO_LAST", null);
        expect("back 2 screens", "BACK_MULTIPLE", "2");
        expect("scroll down a little", "SCROLL", "down_small");
        expect("scroll halfway up", "SCROLL", "up_half");
        expect("scroll to bottom", "SCROLL", "bottom");
        expect("clear text", "EDIT_TEXT", "clear");
        expect("select all", "EDIT_TEXT", "select_all");
        expect("copy", "EDIT_TEXT", "copy");
        expect("paste", "EDIT_TEXT", "paste");
        expect("press enter", "KEY_ACTION", "enter");
        expect("send it", "KEY_ACTION", "send");
        expect("hit search", "SMART_SEARCH", null);
        expect("search", "SMART_SEARCH", null);
        expect("go", "KEY_ACTION", "go");
        expect("done", "KEY_ACTION", "done");
        expect("next", "KEY_ACTION", "next");
        expect("help", "SHOW_HELP", null);
        expect("go left", "SCROLL", "left");
        expect("go right", "SCROLL", "right");
        expect("go up", "SCROLL", "up");
        expect("go down", "SCROLL", "down");
        expect("zoom in", "ZOOM", "in");
        expect("zoom out", "ZOOM", "out");
        if (VoiceCommandParser.parse("random unrelated sentence") != null) throw new AssertionError("unknown phrase must not trigger an action");
        if (!VoiceCommandParser.parse("read this screen").readScreen) throw new AssertionError("screen reading phrase not recognized");
        if (!VoiceCommandParser.parse("stop listening").stopListening) throw new AssertionError("stop phrase not recognized");
        System.out.println("VoiceCommandParserTest: 46 checks passed");
    }

    private static void expect(String phrase, String action, String argument) {
        VoiceCommandParser.Result result = VoiceCommandParser.parse(phrase);
        if (result == null || !action.equals(result.action) || (argument == null ? result.argument != null : !argument.equals(result.argument))) {
            throw new AssertionError(phrase + " => " + (result == null ? "null" : result.action + "/" + result.argument));
        }
    }
}

package com.brannenservices.phonecontrol;

import java.util.Locale;

/** Small, deterministic parser for hands-free commands; no cloud model or API key. */
final class VoiceCommandParser {
    static final class Result {
        final String action;
        final String argument;
        final boolean readScreen;
        final boolean stopListening;
        Result(String action, String argument, boolean readScreen, boolean stopListening) {
            this.action = action; this.argument = argument; this.readScreen = readScreen; this.stopListening = stopListening;
        }
        static Result action(String action, String argument) { return new Result(action, argument, false, false); }
        static Result read() { return new Result(null, null, true, false); }
        static Result stop() { return new Result(null, null, false, true); }
    }

    private VoiceCommandParser() { }

    static Result parse(String phrase) {
        if (phrase == null) return null;
        String value = phrase.toLowerCase(Locale.US).trim().replaceAll("[.!?,]", " ").replaceAll("\\s+", " ");
        if (value.isEmpty()) return null;
        if (matches(value, "stop listening", "turn off listening", "voice control off")) return Result.stop();
        if (matches(value, "confirm", "confirm action", "go ahead")) return Result.action("CONFIRM", null);
        if (matches(value, "cancel", "cancel action", "never mind", "never mind cancel")) return Result.action("CANCEL", null);
        if (matches(value, "home", "go home", "take me home", "go to home screen", "open home screen")) return Result.action("HOME", null);
        if (matches(value, "back", "go back", "go backwards")) return Result.action("BACK", null);
        if (matches(value, "recents", "recent apps", "open recents", "app switcher", "switch apps")) return Result.action("RECENTS", null);
        if (matches(value, "notifications", "open notifications", "show notifications")) return Result.action("NOTIFICATIONS", null);
        if (matches(value, "quick settings", "open quick settings", "show quick settings")) return Result.action("QUICK_SETTINGS", null);
        if (matches(value, "settings", "open settings")) return Result.action("OPEN_NAMED_APP", "settings");
        if (matches(value, "show numbers", "show me numbers", "show number", "show labels", "number items", "label items")) return Result.action("SHOW_NUMBERS", null);
        if (matches(value, "show grid", "show me the grid", "show grid numbers", "grid mode", "display grid")) return Result.action("SHOW_GRID", null);
        if (matches(value, "hide numbers", "hide grid", "hide labels", "clear overlay")) return Result.action("HIDE_OVERLAYS", null);
        if (matches(value, "what is on screen", "what's on screen", "read screen", "read this screen", "describe screen")) return Result.read();
        if (matches(value, "scroll down", "swipe up", "move down")) return Result.action("SWIPE_UP", null);
        if (matches(value, "scroll up", "swipe down", "move up")) return Result.action("SWIPE_DOWN", null);
        if (matches(value, "swipe left", "move left")) return Result.action("SWIPE_LEFT", null);
        if (matches(value, "swipe right", "move right")) return Result.action("SWIPE_RIGHT", null);
        if (matches(value, "volume up", "turn volume up", "louder")) return Result.action("VOLUME_UP", null);
        if (matches(value, "volume down", "turn volume down", "quieter")) return Result.action("VOLUME_DOWN", null);
        if (value.matches("(?:tap|click|press)(?: number)? \\d+")) return Result.action("TAP_NUMBER", lastNumber(value));
        if (value.matches("(?:long press|press and hold|hold)(?: on)? .+")) return Result.action("LONG_PRESS_TEXT", value.replaceFirst("^(long press|press and hold|hold)( on)? ", "").replaceFirst("^the ", "").trim());
        java.util.regex.Matcher grid = java.util.regex.Pattern.compile("^(?:tap|click|press) grid (\\d+) ?[, ] ?(\\d+)$").matcher(value);
        if (grid.matches()) return Result.action("TAP_GRID", grid.group(1) + "," + grid.group(2));
        if (value.startsWith("tap ") || value.startsWith("click ") || value.startsWith("press ")) {
            String target = value.replaceFirst("^(tap|click|press) ", "").replaceFirst("^the ", "").replaceFirst("^(button|link) called ", "").trim();
            if (!target.isEmpty()) return Result.action("TAP_TEXT", target);
        }
        if (value.startsWith("type ") && value.length() > 5) return Result.action("TYPE_TEXT", phrase.trim().substring(5).trim());
        if (value.startsWith("open ") && value.length() > 5) return Result.action("OPEN_NAMED_APP", value.substring(5).trim());
        if (value.startsWith("switch to ") && value.length() > 10) return Result.action("OPEN_NAMED_APP", value.substring(10).trim());
        return null;
    }

    private static String lastNumber(String value) { return value.replaceAll(".*?(\\d+)$", "$1"); }
    private static boolean matches(String value, String... candidates) {
        for (String candidate : candidates) if (value.equals(candidate)) return true;
        return false;
    }
}

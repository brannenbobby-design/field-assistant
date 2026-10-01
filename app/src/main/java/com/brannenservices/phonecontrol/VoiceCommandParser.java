package com.brannenservices.phonecontrol;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic local parser for hands-free commands; no cloud model or API key. */
final class VoiceCommandParser {
    static final class Result {
        final String action;
        final String argument;
        final boolean readScreen;
        final boolean stopListening;
        Result(String action, String argument, boolean readScreen, boolean stopListening) {
            this.action=action; this.argument=argument; this.readScreen=readScreen; this.stopListening=stopListening;
        }
        static Result action(String action,String argument){ return new Result(action,argument,false,false); }
        static Result read(){ return new Result(null,null,true,false); }
        static Result stop(){ return new Result(null,null,false,true); }
    }

    private VoiceCommandParser(){}

    static Result parse(String phrase){
        if(phrase==null) return null;
        String original=phrase.trim();
        String value=original.toLowerCase(Locale.US).replaceAll("[.!?,]"," ").replaceAll("\\s+"," ").trim();
        if(value.isEmpty()) return null;

        if(matches(value,"stop listening","turn off listening","voice control off")) return Result.stop();
        if(matches(value,"confirm","confirm action","go ahead","yes do it")) return Result.action("CONFIRM",null);
        if(matches(value,"cancel","cancel action","never mind","never mind cancel")) return Result.action("CANCEL",null);
        if(matches(value,"help","show help","show commands","what can i say","what can you do")) return Result.action("SHOW_HELP",null);
        if(matches(value,"repeat","repeat that","do that again","again")) return Result.action("REPEAT_LAST",null);
        if(matches(value,"undo","undo that")) return Result.action("UNDO_LAST",null);
        if(matches(value,"tap it","click it","press it")) return Result.action("TAP_HIGHLIGHTED",null);

        Matcher back=Pattern.compile("^(?:go )?back (?:by )?(\\d+)(?: screens?)?$").matcher(value);
        if(back.matches()) return Result.action("BACK_MULTIPLE",back.group(1));
        if(matches(value,"home","go home","take me home","go to home screen","open home screen")) return Result.action("HOME",null);
        if(matches(value,"back","go back","go backwards")) return Result.action("BACK",null);
        if(matches(value,"recents","recent apps","open recents","app switcher","switch apps")) return Result.action("RECENTS",null);
        if(matches(value,"notifications","open notifications","show notifications")) return Result.action("NOTIFICATIONS",null);
        if(matches(value,"quick settings","open quick settings","show quick settings")) return Result.action("QUICK_SETTINGS",null);
        if(matches(value,"settings","open settings")) return Result.action("OPEN_NAMED_APP","settings");
        if(matches(value,"show numbers","show me numbers","show number","show labels","number items","label items")) return Result.action("SHOW_NUMBERS",null);
        if(matches(value,"show grid","show me the grid","show grid numbers","grid mode","display grid")) return Result.action("SHOW_GRID",null);
        if(matches(value,"hide numbers","hide grid","hide labels","clear overlay","hide help")) return Result.action("HIDE_OVERLAYS",null);
        if(matches(value,"what is on screen","what's on screen","read screen","read this screen","describe screen")) return Result.read();

        if(matches(value,"scroll down","swipe up","move down","go down")) return Result.action("SCROLL","down");
        if(matches(value,"scroll up","swipe down","move up","go up")) return Result.action("SCROLL","up");
        if(matches(value,"scroll left","swipe left","move left","go left")) return Result.action("SCROLL","left");
        if(matches(value,"scroll right","swipe right","move right","go right")) return Result.action("SCROLL","right");
        if(matches(value,"scroll down a little","scroll a little down")) return Result.action("SCROLL","down_small");
        if(matches(value,"scroll up a little","scroll a little up")) return Result.action("SCROLL","up_small");
        if(matches(value,"scroll down halfway","scroll halfway down","page down")) return Result.action("SCROLL","down_half");
        if(matches(value,"scroll up halfway","scroll halfway up","page up")) return Result.action("SCROLL","up_half");
        if(matches(value,"scroll to top","go to top","top of page")) return Result.action("SCROLL","top");
        if(matches(value,"scroll to bottom","go to bottom","bottom of page")) return Result.action("SCROLL","bottom");

        if(matches(value,"zoom in","magnify","increase zoom")) return Result.action("ZOOM","in");
        if(matches(value,"zoom out","reduce zoom","decrease zoom")) return Result.action("ZOOM","out");

        if(matches(value,"volume up","turn volume up","louder")) return Result.action("VOLUME_UP",null);
        if(matches(value,"volume down","turn volume down","quieter")) return Result.action("VOLUME_DOWN",null);

        String[][] regions={
            {"top_left","tap top left","press top left","click top left"},
            {"top_center","tap top center","tap top middle","press top center","click top center"},
            {"top_right","tap top right","press top right","click top right"},
            {"center_left","tap left","tap center left","tap middle left","press left","click left"},
            {"center","tap center","tap middle","press center","click center"},
            {"center_right","tap right","tap center right","tap middle right","press right","click right"},
            {"bottom_left","tap bottom left","press bottom left","click bottom left"},
            {"bottom_center","tap bottom center","tap bottom middle","press bottom center","click bottom center"},
            {"bottom_right","tap bottom right","press bottom right","click bottom right"}
        };
        for(String[] group:regions){
            String[] candidates=new String[group.length-1]; System.arraycopy(group,1,candidates,0,candidates.length);
            if(matches(value,candidates)) return Result.action("TAP_REGION",group[0]);
        }

        if(matches(value,"tap search bar","tap search box","select search bar","select search box","open search bar","focus search bar","hit search","hit the search bar","press search bar","click search bar"))
            return Result.action("FOCUS_FIELD","search");
        if(matches(value,"tap text bar","tap text box","tap text field","select text bar","select text box","select text field","focus text field","hit text box","press message box","tap message box"))
            return Result.action("FOCUS_FIELD","text");

        if(matches(value,"delete that","delete last","backspace")) return Result.action("EDIT_TEXT","delete");
        if(matches(value,"clear text","clear field","clear that","erase all")) return Result.action("EDIT_TEXT","clear");
        if(matches(value,"select all","select all text")) return Result.action("EDIT_TEXT","select_all");
        if(matches(value,"copy","copy that","copy text")) return Result.action("EDIT_TEXT","copy");
        if(matches(value,"paste","paste that","paste text")) return Result.action("EDIT_TEXT","paste");
        if(matches(value,"enter","press enter","hit enter","submit with enter")) return Result.action("KEY_ACTION","enter");
        if(matches(value,"send","send it","hit send","press send")) return Result.action("KEY_ACTION","send");

        if(value.matches("(?:tap|click|press)(?: number)? \\d+")) return Result.action("TAP_NUMBER",lastNumber(value));
        Matcher ordinal=Pattern.compile("^(?:tap|click|press) (first|second|third|fourth|fifth) (.+)$").matcher(value);
        if(ordinal.matches()) return Result.action("TAP_TEXT_ORDINAL",ordinalIndex(ordinal.group(1))+"|"+ordinal.group(2).replaceFirst("^the ","").trim());

        Matcher grid=Pattern.compile("^(?:tap|click|press) grid (\\d+) ?[, ] ?(\\d+)$").matcher(value);
        if(grid.matches()) return Result.action("TAP_GRID",grid.group(1)+","+grid.group(2));

        if(value.matches("(?:long press|press and hold|hold)(?: on)? .+"))
            return Result.action("LONG_PRESS_TEXT",value.replaceFirst("^(long press|press and hold|hold)( on)? ","").replaceFirst("^the ","").trim());

        if(value.startsWith("find ") && value.length()>5) return Result.action("FIND_TEXT",value.substring(5).trim());

        if(value.startsWith("tap ")||value.startsWith("click ")||value.startsWith("press ")||value.startsWith("hit ")){
            String target=value.replaceFirst("^(tap|click|press|hit) ","").replaceFirst("^the ","").replaceFirst("^(button|link) called ","").trim();
            if(!target.isEmpty()) return Result.action("TAP_TEXT",target);
        }

        if(value.startsWith("type ")&&original.length()>5) return Result.action("TYPE_TEXT",original.substring(original.toLowerCase(Locale.US).indexOf("type ")+5).trim());
        if(value.startsWith("open ")&&value.length()>5) return Result.action("OPEN_NAMED_APP",value.substring(5).trim());
        if(value.startsWith("switch to ")&&value.length()>10) return Result.action("OPEN_NAMED_APP",value.substring(10).trim());
        return null;
    }

    private static int ordinalIndex(String v){
        switch(v){case "first":return 1;case "second":return 2;case "third":return 3;case "fourth":return 4;case "fifth":return 5;default:return 1;}
    }
    private static String lastNumber(String value){ return value.replaceAll(".*?(\\d+)$","$1"); }
    private static boolean matches(String value,String... candidates){
        for(String candidate:candidates) if(value.equals(candidate)) return true;
        return false;
    }
}

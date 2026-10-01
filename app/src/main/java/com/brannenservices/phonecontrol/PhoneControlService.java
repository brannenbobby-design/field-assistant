package com.brannenservices.phonecontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Accessibility layer: every action is gated by the user's master switch. */
public class PhoneControlService extends AccessibilityService {
    private static PhoneControlService self;
    private static final Map<String, String> APP_ALIASES = new HashMap<>();
    static {
        APP_ALIASES.put("gallery", "com.sec.android.gallery3d");
        APP_ALIASES.put("photos", "com.google.android.apps.photos");
        APP_ALIASES.put("square", "com.squareup");
        APP_ALIASES.put("facebook", "com.facebook.katana");
        APP_ALIASES.put("messenger", "com.facebook.orca");
        APP_ALIASES.put("youtube", "com.google.android.youtube");
        APP_ALIASES.put("maps", "com.google.android.apps.maps");
        APP_ALIASES.put("chrome", "com.android.chrome");
        APP_ALIASES.put("settings", "com.android.settings");
        APP_ALIASES.put("camera", "com.sec.android.app.camera");
    }

    private static final class NumberedTarget {
        final Rect bounds;
        final String label;
        NumberedTarget(Rect bounds, String label) { this.bounds = bounds; this.label = label; }
    }
    private static Runnable pendingConfirmation;
    private static String lastCommand;
    private static String lastArgument;
    private static String undoCommand;
    private static String undoArgument;
    private Rect highlightedTarget;
    private String previousTextValue;
    private final Map<Integer, NumberedTarget> numberedTargets = new LinkedHashMap<>();
    private final List<TextView> labels = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private Runnable hideTask;

    public static PhoneControlService instance() { return self; }
    public static boolean confirmationPending() { return pendingConfirmation != null; }
    @Override protected void onServiceConnected() { self = this; windowManager = getSystemService(WindowManager.class); }
    @Override public void onDestroy() { hideOverlays(); if (self == this) { self = null; pendingConfirmation = null; } super.onDestroy(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { hideOverlays(); }

    public static boolean runCommand(String command, String arg) {
        PhoneControlService service = self;
        if (service == null || !ControlPrefs.enabled(service) || command == null) return false;

        if ("REPEAT_LAST".equals(command)) {
            if (lastCommand == null) return false;
            return runCommand(lastCommand, lastArgument);
        }
        if ("UNDO_LAST".equals(command)) {
            if (undoCommand == null) return false;
            String c = undoCommand, a = undoArgument;
            undoCommand = null; undoArgument = null;
            return runCommandInternal(service, c, a);
        }

        boolean done = runCommandInternal(service, command, arg);
        if (done && isRepeatable(command)) {
            lastCommand = command;
            lastArgument = arg;
            if ("TYPE_TEXT".equals(command) || "EDIT_TEXT".equals(command)) {
                undoCommand = "EDIT_TEXT";
                undoArgument = "undo";
            } else if ("BACK".equals(command)) {
                undoCommand = null; undoArgument = null;
            }
        }
        return done;
    }

    private static boolean isRepeatable(String command) {
        return !("CONFIRM".equals(command) || "CANCEL".equals(command) || "SHOW_HELP".equals(command)
                || "SHOW_NUMBERS".equals(command) || "SHOW_GRID".equals(command) || "HIDE_OVERLAYS".equals(command)
                || "FIND_TEXT".equals(command) || "TAP_HIGHLIGHTED".equals(command));
    }

    private static boolean runCommandInternal(PhoneControlService service, String command, String arg) {
        switch (command) {
            case "HOME": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_HOME);
            case "BACK": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_BACK);
            case "BACK_MULTIPLE": return service.backMultiple(parseInt(arg, 1));
            case "RECENTS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_RECENTS);
            case "NOTIFICATIONS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
            case "QUICK_SETTINGS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
            case "OPEN_NAMED_APP": return service.openNamedApp(arg);
            case "TAP_TEXT": return service.tapText(arg, false);
            case "TAP_TEXT_ORDINAL": return service.tapTextOrdinal(arg);
            case "FIND_TEXT": return service.findText(arg);
            case "TAP_HIGHLIGHTED": return service.tapHighlighted();
            case "TAP_REGION": return service.tapRegion(arg);
            case "FOCUS_FIELD": return service.focusField(arg);
            case "LONG_PRESS_TEXT": return service.tapText(arg, true);
            case "TYPE_TEXT": return service.typeText(arg);
            case "EDIT_TEXT": return service.editText(arg);
            case "KEY_ACTION": return service.keyAction(arg);
            case "CONFIRM": return service.confirmPendingAction();
            case "CANCEL": pendingConfirmation = null; return true;
            case "SHOW_NUMBERS": return service.showNumbers();
            case "TAP_NUMBER": return service.tapNumber(parseInt(arg, -1));
            case "SHOW_GRID": return service.showGrid();
            case "SHOW_HELP": return service.showHelp();
            case "TAP_GRID": return service.tapGrid(arg);
            case "HIDE_OVERLAYS": service.hideOverlays(); return true;
            case "SCROLL": return service.scroll(arg);
            case "ZOOM": return service.zoom(arg);
            case "SWIPE_UP": return service.scroll("down");
            case "SWIPE_DOWN": return service.scroll("up");
            case "SWIPE_LEFT": return service.scroll("left");
            case "SWIPE_RIGHT": return service.scroll("right");
            case "VOLUME_UP": return service.volume(true);
            case "VOLUME_DOWN": return service.volume(false);
            default: return false;
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private boolean openNamedApp(String spokenName) {
        if (spokenName == null) return false;
        String name = spokenName.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ");
        String packageName = APP_ALIASES.get(name);
        if (packageName != null && openPackage(packageName)) return true;
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (android.content.pm.ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            CharSequence label = info.loadLabel(pm);
            if (label == null) continue;
            String app = label.toString().toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ");
            if (app.equals(name) || app.startsWith(name + " ") || name.startsWith(app + " ")) {
                Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { startActivity(launch); hideOverlays(); return true; } catch (Exception ignored) { }
            }
        }
        return false;
    }

    private boolean openPackage(String packageName) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(launch); hideOverlays(); return true; } catch (Exception ignored) { return false; }
    }

    private static final class MatchTarget {
        final AccessibilityNodeInfo node;
        final Rect bounds;
        final String label;
        final int score;
        MatchTarget(AccessibilityNodeInfo node, Rect bounds, String label, int score) {
            this.node=node; this.bounds=bounds; this.label=label; this.score=score;
        }
    }

    private boolean tapText(String text, boolean longClick) {
        return tapTextInternal(text, longClick, false, 0);
    }

    private boolean tapTextOrdinal(String encoded) {
        if (encoded == null || !encoded.contains("|")) return false;
        String[] pieces = encoded.split("\\|", 2);
        return tapTextInternal(pieces[1], false, false, parseInt(pieces[0], 1));
    }

    private List<MatchTarget> findMatches(String text) {
        List<MatchTarget> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return out;
        String wanted = normalize(text);
        List<AccessibilityNodeInfo> all = new ArrayList<>();
        collectVisible(root, all, 0);
        for (AccessibilityNodeInfo node : all) {
            int score = matchScore(wanted, normalize(searchableLabel(node)));
            if (score < 70) continue;
            AccessibilityNodeInfo target = clickableTarget(node);
            if (target == null) continue;
            Rect bounds = new Rect();
            target.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) continue;
            boolean duplicate = false;
            for (MatchTarget existing : out) if (existing.bounds.equals(bounds)) { duplicate = true; break; }
            if (!duplicate) out.add(new MatchTarget(target, new Rect(bounds), nodeLabel(target), score));
        }
        java.util.Collections.sort(out, (a,b) -> {
            int s = Integer.compare(b.score, a.score);
            if (s != 0) return s;
            int y = Integer.compare(a.bounds.top, b.bounds.top);
            return y != 0 ? y : Integer.compare(a.bounds.left, b.bounds.left);
        });
        return out;
    }

    private boolean tapTextInternal(String text, boolean longClick, boolean confirmed, int ordinal) {
        List<MatchTarget> matches = findMatches(text);
        if (matches.isEmpty()) return false;
        int bestScore = matches.get(0).score;
        List<MatchTarget> best = new ArrayList<>();
        for (MatchTarget m : matches) if (m.score == bestScore) best.add(m);

        if (ordinal <= 0 && best.size() > 1) {
            return showMatchingNumbers(best);
        }
        int index = ordinal > 0 ? ordinal - 1 : 0;
        if (index < 0 || index >= best.size()) return false;
        MatchTarget chosen = best.get(index);
        if (!confirmed && isRiskyLabel(chosen.label)) {
            final String requestedText=text;
            final int requestedOrdinal=ordinal;
            requestConfirmation("tap " + chosen.label, () -> tapTextInternal(requestedText,longClick,true,requestedOrdinal));
            return true;
        }
        boolean done=chosen.node.performAction(longClick ? AccessibilityNodeInfo.ACTION_LONG_CLICK : AccessibilityNodeInfo.ACTION_CLICK);
        if(!done) done=swipeTap(chosen.bounds.centerX(),chosen.bounds.centerY());
        if(done) hideOverlays();
        return done;
    }

    private boolean showMatchingNumbers(List<MatchTarget> matches) {
        hideOverlays();
        numberedTargets.clear();
        int i=1;
        for(MatchTarget m:matches) {
            if(i>20) break;
            numberedTargets.put(i,new NumberedTarget(new Rect(m.bounds),m.label));
            addLabel(i,m.bounds.centerX(),m.bounds.centerY());
            i++;
        }
        scheduleOverlayDismiss();
        return i>1;
    }

    private boolean findText(String text) {
        List<MatchTarget> matches=findMatches(text);
        if(matches.isEmpty()) return false;
        MatchTarget m=matches.get(0);
        highlightedTarget=new Rect(m.bounds);
        showHighlight(m.bounds, m.label.isEmpty()?text:m.label);
        return true;
    }

    private boolean tapHighlighted() {
        if(highlightedTarget==null) return false;
        Rect r=new Rect(highlightedTarget);
        highlightedTarget=null;
        hideOverlays();
        return swipeTap(r.centerX(),r.centerY());
    }

    private void collectVisible(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 25 || out.size() >= 500) return;
        if (node.isVisibleToUser()) out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collectVisible(node.getChild(i), out, depth + 1);
    }

    private AccessibilityNodeInfo clickableTarget(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        int hops = 0;
        while (current != null && hops++ < 6) {
            if (current.isVisibleToUser() && (current.isClickable() || current.isFocusable())) return current;
            current = current.getParent();
        }
        return null;
    }

    private String searchableLabel(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder value = new StringBuilder(nodeLabel(node));
        if (node.getHintText() != null) value.append(' ').append(node.getHintText());
        String viewId = node.getViewIdResourceName();
        if (viewId != null) value.append(' ').append(viewId.replace('_', ' ').replace('/', ' '));
        CharSequence pane = node.getPaneTitle();
        if (pane != null) value.append(' ').append(pane);
        return value.toString().trim();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ");
    }

    private int matchScore(String wanted, String candidate) {
        if (wanted.isEmpty() || candidate.isEmpty()) return 0;
        if (candidate.equals(wanted)) return 100;
        if (candidate.startsWith(wanted + " ") || candidate.endsWith(" " + wanted)) return 90;
        if (candidate.contains(" " + wanted + " ") || candidate.contains(wanted)) return 80;
        String[] words = wanted.split(" ");
        int hits = 0;
        for (String word : words) if (!word.isEmpty() && candidate.matches(".*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*")) hits++;
        if (hits == words.length && hits > 0) return 70 + Math.min(hits, 9);
        return 0;
    }

    private boolean tapRegion(String region) {
        if (region == null) return false;
        float x, y;
        switch (region) {
            case "top_left": x = .17f; y = .17f; break;
            case "top_center": x = .50f; y = .17f; break;
            case "top_right": x = .83f; y = .17f; break;
            case "center_left": x = .17f; y = .50f; break;
            case "center": x = .50f; y = .50f; break;
            case "center_right": x = .83f; y = .50f; break;
            case "bottom_left": x = .17f; y = .83f; break;
            case "bottom_center": x = .50f; y = .83f; break;
            case "bottom_right": x = .83f; y = .83f; break;
            default: return false;
        }
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int px = (int)(metrics.widthPixels * x);
        int py = (int)(metrics.heightPixels * y);
        String label = labelAt(px, py);
        if (isRiskyLabel(label)) {
            final int fx = px, fy = py;
            requestConfirmation("tap " + label, () -> swipeTap(fx, fy));
            return true;
        }
        hideOverlays();
        return swipeTap(px, py);
    }

    private boolean focusField(String kind) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> all = new ArrayList<>();
        collectVisible(root, all, 0);
        AccessibilityNodeInfo best = null;
        int bestScore = Integer.MIN_VALUE;
        boolean search = "search".equals(kind);
        for (AccessibilityNodeInfo node : all) {
            boolean editable = node.isEditable();
            String cls = node.getClassName() == null ? "" : node.getClassName().toString().toLowerCase(Locale.US);
            if (!editable && !cls.contains("edittext") && !node.isFocusable()) continue;
            String label = normalize(searchableLabel(node));
            int score = 0;
            if (editable) score += 40;
            if (cls.contains("edittext")) score += 30;
            if (node.isFocusable()) score += 10;
            if (search) {
                if (label.contains("search")) score += 100;
                else score -= 20;
            } else {
                if (label.contains("message") || label.contains("comment") || label.contains("text") || label.contains("type") || label.contains("reply")) score += 45;
                if (label.contains("search")) score -= 60;
            }
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty()) continue;
            if (score > bestScore) { bestScore = score; best = node; }
        }
        if (best == null) return false;
        boolean done = best.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (!done) done = best.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!done) {
            Rect bounds = new Rect();
            best.getBoundsInScreen(bounds);
            done = !bounds.isEmpty() && swipeTap(bounds.centerX(), bounds.centerY());
        }
        return done;
    }

    private AccessibilityNodeInfo focusedEditable() {
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null) return null;
        AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(focused!=null && (focused.isEditable() || focused.isFocusable())) return focused;
        List<AccessibilityNodeInfo> all=new ArrayList<>();
        collectVisible(root,all,0);
        for(AccessibilityNodeInfo node:all) if(node.isFocused() && node.isEditable()) return node;
        return null;
    }

    private boolean typeText(String value) {
        if(value==null || value.isEmpty()) return false;
        AccessibilityNodeInfo focused=focusedEditable();
        if(focused==null) return false;
        CharSequence current=focused.getText();
        String prefix=current==null ? "" : current.toString();
        previousTextValue=prefix;
        android.os.Bundle args=new android.os.Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prefix + value);
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
    }

    private boolean editText(String action) {
        AccessibilityNodeInfo focused=focusedEditable();
        if(focused==null) return false;
        CharSequence currentCs=focused.getText();
        String current=currentCs==null ? "" : currentCs.toString();
        android.os.Bundle args=new android.os.Bundle();
        if(!"undo".equals(action)) previousTextValue=current;
        switch(action==null?"":action) {
            case "delete":
                if(current.isEmpty()) return true;
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,current.substring(0,current.length()-1));
                return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
            case "clear":
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"");
                return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
            case "select_all":
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,0);
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,current.length());
                return focused.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION,args);
            case "copy": return focused.performAction(AccessibilityNodeInfo.ACTION_COPY);
            case "paste": return focused.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            case "undo":
                if(previousTextValue==null) return false;
                String restore=previousTextValue;
                previousTextValue=current;
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,restore);
                return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
            default: return false;
        }
    }

    private boolean keyAction(String action) {
        if("send".equals(action)) return tapText("send",false);
        AccessibilityNodeInfo focused=focusedEditable();
        if(focused==null) return false;
        if(android.os.Build.VERSION.SDK_INT>=30) return focused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
        return false;
    }

    private void requestConfirmation(String description, Runnable action) {
        pendingConfirmation = action;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> pendingConfirmation = null, 15000);
        VoiceControlService.announceConfirmation(description);
    }

    private boolean confirmPendingAction() {
        Runnable action = pendingConfirmation;
        if (action == null) return false;
        pendingConfirmation = null;
        handler.removeCallbacksAndMessages(null);
        action.run();
        return true;
    }

    private boolean isRiskyLabel(String label) {
        if (label == null) return false;
        String value = label.toLowerCase(Locale.US);
        return value.matches(".*\\b(send|delete|remove|purchase|buy|pay|submit|post|publish|call|block|report|transfer|unsubscribe|place order|save changes)\\b.*");
    }

    private String nodeLabel(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder value = new StringBuilder();
        if (node.getText() != null) value.append(node.getText()).append(' ');
        if (node.getContentDescription() != null) value.append(node.getContentDescription());
        return value.toString().trim();
    }

    private String labelAt(int x, int y) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return labelAt(root, x, y, 0);
    }

    private String labelAt(AccessibilityNodeInfo node, int x, int y, int depth) {
        if (node == null || depth > 20 || !node.isVisibleToUser()) return "";
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        String own = nodeLabel(node);
        if ((node.isClickable() || node.isFocusable()) && bounds.contains(x, y) && isRiskyLabel(own)) return own;
        for (int i = 0; i < node.getChildCount(); i++) {
            String child = labelAt(node.getChild(i), x, y, depth + 1);
            if (!child.isEmpty()) return child;
        }
        return "";
    }

    private boolean showNumbers() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        hideOverlays();
        numberedTargets.clear();
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collectClickable(root, nodes, 0);
        Rect bounds = new Rect();
        int index = 1;
        for (AccessibilityNodeInfo node : nodes) {
            if (index > 50) break;
            node.getBoundsInScreen(bounds);
            if (bounds.isEmpty() || bounds.width() < dp(12) || bounds.height() < dp(12)) continue;
            numberedTargets.put(index, new NumberedTarget(new Rect(bounds), nodeLabel(node)));
            addLabel(index, bounds.centerX(), bounds.centerY());
            index++;
        }
        scheduleOverlayDismiss();
        return index > 1;
    }

    private void collectClickable(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 20 || out.size() >= 100) return;
        if (node.isVisibleToUser() && (node.isClickable() || node.isFocusable())) out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) collectClickable(node.getChild(i), out, depth + 1);
    }

    private boolean tapNumber(int number) {
        NumberedTarget item = numberedTargets.get(number);
        if (item == null) return false;
        final Rect target = new Rect(item.bounds);
        String label = item.label.isEmpty() ? labelAt(target.centerX(), target.centerY()) : item.label;
        if (isRiskyLabel(label)) {
            requestConfirmation("tap " + label, () -> swipeTap(target.centerX(), target.centerY()));
            hideOverlays();
            return true;
        }
        boolean result = swipeTap(target.centerX(), target.centerY());
        hideOverlays();
        return result;
    }

    private boolean showGrid() {
        hideOverlays();
        numberedTargets.clear();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int cols = 3, rows = 3;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int number = row * cols + col + 1;
                int x = (int) (metrics.widthPixels * (col + .5f) / cols);
                int y = (int) (metrics.heightPixels * (row + .5f) / rows);
                numberedTargets.put(number, new NumberedTarget(new Rect(x - dp(22), y - dp(22), x + dp(22), y + dp(22)), ""));
                addLabel(number, x, y);
            }
        }
        scheduleOverlayDismiss();
        return true;
    }

    private boolean tapGrid(String coordinates) {
        if (coordinates == null) return false;
        String[] pieces = coordinates.trim().split("[ ,]+", 2);
        if (pieces.length != 2) return false;
        int row = parseInt(pieces[0], -1), col = parseInt(pieces[1], -1);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (row < 1 || row > 3 || col < 1 || col > 3) return false;
        int x = (int) (metrics.widthPixels * (col - .5f) / 3);
        int y = (int) (metrics.heightPixels * (row - .5f) / 3);
        String label = labelAt(x, y);
        if (isRiskyLabel(label)) {
            hideOverlays();
            requestConfirmation("tap " + label, () -> swipeTap(x, y));
            return true;
        }
        hideOverlays();
        return swipeTap(x, y);
    }

    private void addLabel(int number, int x, int y) {
        if (windowManager == null) return;
        TextView label = new TextView(this);
        label.setText(String.valueOf(number));
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        label.setBackgroundColor(Color.rgb(22, 91, 174));
        int size = dp(32);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = Math.max(0, x - size / 2);
        params.y = Math.max(0, y - size / 2);
        try { windowManager.addView(label, params); labels.add(label); } catch (Exception ignored) { }
    }

    private void scheduleOverlayDismiss() {
        hideTask = this::hideOverlays;
        handler.postDelayed(hideTask, 15000);
    }

    private void hideOverlays() {
        if (hideTask != null) handler.removeCallbacks(hideTask);
        hideTask = null;
        if (windowManager != null) for (TextView label : labels) try { windowManager.removeView(label); } catch (Exception ignored) { }
        labels.clear();
        numberedTargets.clear();
    }

    public static String readScreen() {
        PhoneControlService service = self;
        if (service == null || !ControlPrefs.enabled(service)) return "";
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return "";
        StringBuilder output = new StringBuilder();
        service.collectText(root, output, 0);
        return output.toString();
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder output, int depth) {
        if (node == null || depth > 20 || output.length() > 1600) return;
        if (node.isVisibleToUser()) {
            appendText(output, node.getText());
            appendText(output, node.getContentDescription());
        }
        for (int i = 0; i < node.getChildCount(); i++) collectText(node.getChild(i), output, depth + 1);
    }

    private void appendText(StringBuilder output, CharSequence text) {
        if (text == null) return;
        String value = text.toString().trim();
        if (value.isEmpty() || output.indexOf(value) >= 0 || output.length() + value.length() > 1600) return;
        if (output.length() > 0) output.append(". ");
        output.append(value);
    }

    private boolean swipe(float sx, float sy, float ex, float ey, long duration) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Path path = new Path(); path.moveTo(metrics.widthPixels * sx, metrics.heightPixels * sy); path.lineTo(metrics.widthPixels * ex, metrics.heightPixels * ey);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, duration)).build();
        return dispatchGesture(gesture, null, null);
    }

    private boolean swipeTap(int x, int y) {
        Path path = new Path(); path.moveTo(x, y); path.lineTo(x + 1, y + 1);
        return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, 90)).build(), null, null);
    }

    private boolean backMultiple(int count) {
        final int times=Math.max(1,Math.min(count,5));
        boolean first=performGlobalAction(GLOBAL_ACTION_BACK);
        for(int i=1;i<times;i++) {
            final int delay=i*220;
            handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK),delay);
        }
        return first;
    }

    private boolean scroll(String direction) {
        hideOverlays();
        if(direction==null) return false;
        switch(direction) {
            case "down": return swipe(.50f,.78f,.50f,.27f,420);
            case "up": return swipe(.50f,.27f,.50f,.78f,420);
            case "left": return swipe(.82f,.50f,.18f,.50f,420);
            case "right": return swipe(.18f,.50f,.82f,.50f,420);
            case "down_small": return swipe(.50f,.64f,.50f,.42f,280);
            case "up_small": return swipe(.50f,.42f,.50f,.64f,280);
            case "down_half": return swipe(.50f,.72f,.50f,.38f,340);
            case "up_half": return swipe(.50f,.38f,.50f,.72f,340);
            case "top": return multiSwipe(false);
            case "bottom": return multiSwipe(true);
            default: return false;
        }
    }

    private boolean multiSwipe(boolean down) {
        boolean first=down ? swipe(.50f,.82f,.50f,.18f,300) : swipe(.50f,.18f,.50f,.82f,300);
        for(int i=1;i<6;i++) {
            final int delay=i*340;
            handler.postDelayed(() -> {
                if(down) swipe(.50f,.82f,.50f,.18f,300);
                else swipe(.50f,.18f,.50f,.82f,300);
            },delay);
        }
        return first;
    }

    private boolean zoom(String direction) {
        if (direction == null || android.os.Build.VERSION.SDK_INT < 24) return false;
        try {
            android.accessibilityservice.AccessibilityService.MagnificationController controller = getMagnificationController();
            float current = controller.getScale();
            float target = current;
            if ("in".equals(direction)) target = Math.min(8.0f, current + 0.10f);
            else if ("out".equals(direction)) target = Math.max(1.0f, current - 0.10f);
            else return false;
            if (Math.abs(target - current) < 0.001f) return true;
            return controller.setScale(target, true);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean showHelp() {
        hideOverlays();
        if(windowManager==null) return false;
        TextView help=new TextView(this);
        help.setText("VOICE COMMANDS\n\n" +
                "HELP: Help • Show commands • Hide help\n" +
                "TAP: Tap <word> • Find <word> • Tap it • Tap first/second/third <word>\n" +
                "TARGETING: Show numbers • Tap number <#> • Show grid • Tap grid <row> <col>\n" +
                "SCREEN AREAS: Tap top left/center/right • Tap left/center/right • Tap bottom left/center/right\n" +
                "FIELDS: Tap search bar • Tap text field • Type <text>\n" +
                "EDIT: Delete that • Clear text • Select all • Copy • Paste • Enter • Send • Undo\n" +
                "MOVE: Scroll up/down/left/right • Go up/down/left/right • Scroll a little • Scroll halfway • Scroll to top/bottom\n" +
                "ZOOM: Zoom in (+10%) • Zoom out (-10%)\n" +
                "PHONE: Home • Back • Back <#> screens • Recents • Notifications • Quick settings • Volume up/down\n" +
                "OTHER: Repeat that • Open <app> • Switch to <app> • Read screen • Stop listening");
        help.setTextColor(Color.WHITE);
        help.setTextSize(14);
        help.setPadding(dp(20),dp(20),dp(20),dp(20));
        help.setBackgroundColor(Color.argb(235,25,25,25));
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(560)),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.CENTER;
        try { windowManager.addView(help,p); labels.add(help); scheduleOverlayDismiss(); return true; }
        catch(Exception ignored){ return false; }
    }

    private void showHighlight(Rect bounds,String labelText) {
        hideOverlays();
        highlightedTarget=new Rect(bounds);
        if(windowManager==null) return;
        TextView label=new TextView(this);
        label.setText("  " + labelText + "  ");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        label.setBackgroundColor(Color.argb(220,20,110,220));
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
                Math.max(bounds.width(),dp(90)),Math.max(bounds.height(),dp(38)),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.x=Math.max(0,bounds.left);
        p.y=Math.max(0,bounds.top);
        try { windowManager.addView(label,p); labels.add(label); scheduleOverlayDismiss(); } catch(Exception ignored){}
    }

    private boolean volume(boolean up) {
        android.media.AudioManager audio = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) return false;
        audio.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                up ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI);
        return true;
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}

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
        if (service == null || !ControlPrefs.enabled(service)) return false;
        if (command == null) return false;
        switch (command) {
            case "HOME": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_HOME);
            case "BACK": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_BACK);
            case "RECENTS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_RECENTS);
            case "NOTIFICATIONS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
            case "QUICK_SETTINGS": service.hideOverlays(); return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
            case "OPEN_NAMED_APP": return service.openNamedApp(arg);
            case "TAP_TEXT": return service.tapText(arg, false);
            case "LONG_PRESS_TEXT": return service.tapText(arg, true);
            case "TYPE_TEXT": return service.typeText(arg);
            case "CONFIRM": return service.confirmPendingAction();
            case "CANCEL": pendingConfirmation = null; return true;
            case "SHOW_NUMBERS": return service.showNumbers();
            case "TAP_NUMBER": return service.tapNumber(parseInt(arg, -1));
            case "SHOW_GRID": return service.showGrid();
            case "TAP_GRID": return service.tapGrid(arg);
            case "HIDE_OVERLAYS": service.hideOverlays(); return true;
            case "SWIPE_UP": service.hideOverlays(); return service.swipe(.50f,.78f,.50f,.27f,420);
            case "SWIPE_DOWN": service.hideOverlays(); return service.swipe(.50f,.27f,.50f,.78f,420);
            case "SWIPE_LEFT": service.hideOverlays(); return service.swipe(.82f,.50f,.18f,.50f,420);
            case "SWIPE_RIGHT": service.hideOverlays(); return service.swipe(.18f,.50f,.82f,.50f,420);
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

    private boolean tapText(String text, boolean longClick) {
        return tapTextInternal(text, longClick, false);
    }

    private boolean tapTextInternal(String text, boolean longClick, boolean confirmed) {
        if (text == null || text.trim().isEmpty()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(text.trim());
        if (matches == null) return false;
        for (AccessibilityNodeInfo node : matches) {
            AccessibilityNodeInfo target = node;
            while (target != null) {
                if (target.isVisibleToUser() && target.isClickable()) {
                    if (!confirmed && isRiskyLabel(nodeLabel(target))) {
                        final String requestedText = text;
                        requestConfirmation("tap " + nodeLabel(target), () -> tapTextInternal(requestedText, longClick, true));
                        return true;
                    }
                    boolean done = target.performAction(longClick ? AccessibilityNodeInfo.ACTION_LONG_CLICK : AccessibilityNodeInfo.ACTION_CLICK);
                    if (done) hideOverlays();
                    return done;
                }
                target = target.getParent();
            }
        }
        return false;
    }

    private boolean typeText(String value) {
        if (value == null || value.isEmpty()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null) return false;
        android.os.Bundle args = new android.os.Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
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

    private boolean volume(boolean up) {
        android.media.AudioManager audio = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) return false;
        audio.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                up ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI);
        return true;
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}

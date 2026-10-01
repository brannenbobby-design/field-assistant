package com.brannenservices.phonecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Main setup and control screen for Brannen Voice Control. */
public class MainActivity extends Activity {
    private static final int MIC_REQUEST = 41;
    private static final int NOTIFICATION_REQUEST = 42;
    private static final int INK = Color.rgb(25, 35, 45);
    private static final int MUTED = Color.rgb(91, 106, 120);
    private static final int BLUE = Color.rgb(28, 91, 170);
    private TextView status;
    private TextView latest;
    private Switch masterSwitch;
    private Switch voiceSwitch;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(242, 246, 250));
        getWindow().setNavigationBarColor(Color.rgb(242, 246, 250));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(242, 246, 250));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(25), dp(20), dp(32));
        scroll.addView(root);

        TextView eyebrow = text("BRANNEN SERVICES  •  HANDS-FREE CONTROL", 12, MUTED, true);
        root.addView(eyebrow);
        TextView title = text("Voice Control", 31, INK, true);
        title.setPadding(0, dp(7), 0, dp(4));
        root.addView(title);
        TextView intro = text("Use natural commands to navigate your Galaxy and control the screen in any app.", 16, MUTED, false);
        root.addView(intro);

        LinearLayout card = card(root);
        TextView cardTitle = text("Control status", 18, INK, true);
        card.addView(cardTitle);
        masterSwitch = new Switch(this);
        masterSwitch.setText("PHONE CONTROL");
        masterSwitch.setTextColor(INK);
        masterSwitch.setTextSize(16);
        masterSwitch.setChecked(ControlPrefs.enabled(this));
        masterSwitch.setOnCheckedChangeListener((button, checked) -> {
            ControlPrefs.setEnabled(this, checked);
            if (!checked) stopVoice();
            refreshStatus();
        });
        card.addView(masterSwitch, matchWrap());

        voiceSwitch = new Switch(this);
        voiceSwitch.setText("BACKGROUND LISTENING");
        voiceSwitch.setTextColor(INK);
        voiceSwitch.setTextSize(15);
        voiceSwitch.setChecked(VoiceControlService.isRunning());
        voiceSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked) requestStartVoice(); else stopVoice();
        });
        card.addView(voiceSwitch, matchWrap());
        status = text("", 14, MUTED, false);
        status.setPadding(0, dp(8), 0, 0);
        card.addView(status);

        LinearLayout heardCard = card(root);
        heardCard.addView(text("Last command", 16, INK, true));
        latest = text("Nothing heard yet", 15, MUTED, false);
        latest.setPadding(0, dp(7), 0, 0);
        heardCard.addView(latest);

        LinearLayout actions = card(root);
        actions.addView(text("Set up your phone", 18, INK, true));
        addButton(actions, "Enable Accessibility Service", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton(actions, "Test Home command", v -> PhoneControlService.runCommand("HOME", null));
        addButton(actions, "Test Back command", v -> PhoneControlService.runCommand("BACK", null));

        LinearLayout guide = card(root);
        guide.addView(text("Try saying", 18, INK, true));
        TextView examples = text("“Open Gallery”  •  “Show numbers”  •  “Tap 4”\n“Scroll down”  •  “Go home”  •  “What’s on screen?”\n“Type Bobby’s message”  •  “Show grid”", 15, MUTED, false);
        examples.setPadding(0, dp(9), 0, 0);
        guide.addView(examples);
        TextView note = text("Listening uses Android’s speech service. A persistent notification appears while the microphone is active. Phone Control must be switched on before a command can act.", 13, MUTED, false);
        note.setPadding(dp(3), dp(5), dp(3), 0);
        root.addView(note);

        setContentView(scroll);
        refreshStatus();
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(17), dp(15), dp(17), dp(15));
        box.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        parent.addView(box, lp);
        return box;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(BLUE);
        button.setOnClickListener(listener);
        parent.addView(button, matchWrap());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void requestStartVoice() {
        if (!ControlPrefs.enabled(this)) {
            voiceSwitch.setChecked(false);
            masterSwitch.setChecked(true);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(0).getBoolean("notification_prompted", false)) {
            getPreferences(0).edit().putBoolean("notification_prompted", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
            return;
        }
        requestMicrophonePermission();
    }

    private void requestMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
            return;
        }
        startVoice();
    }

    private void startVoice() {
        if (!ControlPrefs.enabled(this)) ControlPrefs.setEnabled(this, true);
        Intent intent = new Intent(this, VoiceControlService.class).setAction(VoiceControlService.ACTION_START);
        startForegroundService(intent);
        refreshStatus();
    }

    private void stopVoice() {
        Intent intent = new Intent(this, VoiceControlService.class).setAction(VoiceControlService.ACTION_STOP);
        startService(intent);
        refreshStatus();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == MIC_REQUEST) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) startVoice();
            else if (voiceSwitch != null) voiceSwitch.setChecked(false);
        } else if (request == NOTIFICATION_REQUEST) {
            requestMicrophonePermission();
        }
    }

    private void refreshStatus() {
        if (status == null) return;
        boolean enabled = ControlPrefs.enabled(this);
        status.setText("Accessibility: " + (PhoneControlService.instance() != null ? "connected" : "needs setup")
                + "\nVoice listener: " + (VoiceControlService.isRunning() ? "listening" : "off")
                + "\nMaster control: " + (enabled ? "on" : "off"));
        if (latest != null) {
            String heard = VoiceControlService.lastHeard(this);
            latest.setText(heard.isEmpty() ? "Nothing heard yet" : heard);
        }
        if (voiceSwitch != null && voiceSwitch.isChecked() != VoiceControlService.isRunning()) {
            voiceSwitch.setOnCheckedChangeListener(null);
            voiceSwitch.setChecked(VoiceControlService.isRunning());
            voiceSwitch.setOnCheckedChangeListener((button, checked) -> { if (checked) requestStartVoice(); else stopVoice(); });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }
}

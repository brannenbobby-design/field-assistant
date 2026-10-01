package com.brannenservices.phonecontrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup;

public class MainActivity extends Activity {
  private TextView status;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(48,48,48,48);
    TextView title=new TextView(this); title.setText("Phone Control Prototype"); title.setTextSize(28); title.setTextColor(Color.BLACK); root.addView(title);
    TextView info=new TextView(this); info.setText("\nLocal Android control core. No OpenAI API key.\nMaster switch must be ON before commands execute.\n"); info.setTextSize(16); root.addView(info);
    Switch master=new Switch(this); master.setText("PHONE CONTROL"); master.setChecked(ControlPrefs.enabled(this));
    master.setOnCheckedChangeListener((v,on)->{ControlPrefs.setEnabled(this,on); updateStatus();}); root.addView(master);
    Button access=button("Open Accessibility Settings"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(access);
    Button home=button("TEST: Home"); home.setOnClickListener(v->PhoneControlService.runCommand("HOME",null)); root.addView(home);
    Button back=button("TEST: Back"); back.setOnClickListener(v->PhoneControlService.runCommand("BACK",null)); root.addView(back);
    Button settings=button("TEST: Open Android Settings"); settings.setOnClickListener(v->PhoneControlService.runCommand("OPEN_APP","com.android.settings")); root.addView(settings);
    EditText text=new EditText(this); text.setHint("Visible button/text to tap"); root.addView(text);
    Button tap=button("TEST: Tap matching text"); tap.setOnClickListener(v->PhoneControlService.runCommand("TAP_TEXT",text.getText().toString())); root.addView(tap);
    status=new TextView(this); status.setTextSize(15); status.setPadding(0,30,0,0); root.addView(status);
    setContentView(root); updateStatus();
  }
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));return b;}
  private void updateStatus(){status.setText("Master: "+(ControlPrefs.enabled(this)?"ON":"OFF")+"\nAccessibility service: "+(PhoneControlService.instance()!=null?"CONNECTED":"NOT CONNECTED"));}
  @Override protected void onResume(){super.onResume(); if(status!=null) updateStatus();}
}
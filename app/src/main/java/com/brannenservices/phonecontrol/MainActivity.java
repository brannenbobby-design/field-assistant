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
  ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(48,48,48,48); scroll.addView(root);
  TextView title=new TextView(this); title.setText("Phone Control Prototype v0.2"); title.setTextSize(27); title.setTextColor(Color.BLACK); root.addView(title);
  TextView info=new TextView(this); info.setText("\nLocal control core — no OpenAI API key.\nMaster switch must be ON.\n"); info.setTextSize(16); root.addView(info);
  Switch master=new Switch(this); master.setText("PHONE CONTROL"); master.setChecked(ControlPrefs.enabled(this)); master.setOnCheckedChangeListener((v,on)->{ControlPrefs.setEnabled(this,on);updateStatus();}); root.addView(master);
  add(root,"Open Accessibility Settings",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  add(root,"TEST: Home",v->PhoneControlService.runCommand("HOME",null)); add(root,"TEST: Back",v->PhoneControlService.runCommand("BACK",null));
  add(root,"TEST: Recents",v->PhoneControlService.runCommand("RECENTS",null)); add(root,"TEST: Notifications",v->PhoneControlService.runCommand("NOTIFICATIONS",null));
  add(root,"TEST: Open Android Settings",v->PhoneControlService.runCommand("OPEN_APP","com.android.settings"));
  TextView gestures=new TextView(this); gestures.setText("\nGesture tests"); gestures.setTextSize(19); root.addView(gestures);
  add(root,"Swipe Up",v->PhoneControlService.runCommand("SWIPE_UP",null)); add(root,"Swipe Down",v->PhoneControlService.runCommand("SWIPE_DOWN",null));
  add(root,"Swipe Left",v->PhoneControlService.runCommand("SWIPE_LEFT",null)); add(root,"Swipe Right",v->PhoneControlService.runCommand("SWIPE_RIGHT",null));
  EditText text=new EditText(this); text.setHint("Visible button/text to tap"); root.addView(text); add(root,"TEST: Tap matching text",v->PhoneControlService.runCommand("TAP_TEXT",text.getText().toString()));
  status=new TextView(this); status.setTextSize(15); status.setPadding(0,30,0,40); root.addView(status); setContentView(scroll); updateStatus();
 }
 private void add(LinearLayout r,String s,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));b.setOnClickListener(l);r.addView(b);}
 private void updateStatus(){status.setText("Master: "+(ControlPrefs.enabled(this)?"ON":"OFF")+"\nAccessibility service: "+(PhoneControlService.instance()!=null?"CONNECTED":"NOT CONNECTED"));}
 @Override protected void onResume(){super.onResume();if(status!=null)updateStatus();}
}
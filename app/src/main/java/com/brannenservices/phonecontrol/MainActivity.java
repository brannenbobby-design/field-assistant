package com.brannenservices.phonecontrol;
import android.Manifest; import android.app.Activity; import android.content.*; import android.content.pm.PackageManager; import android.os.Bundle; import android.provider.Settings; import android.widget.*; import android.graphics.Color; import android.view.ViewGroup;
public class MainActivity extends Activity {
 private static final int MIC_REQUEST=41; private TextView status; private Switch voice;
 @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(48,48,48,48);scroll.addView(root);
  TextView title=new TextView(this);title.setText("Phone Control Prototype v0.5");title.setTextSize(27);title.setTextColor(Color.BLACK);root.addView(title);
  TextView info=new TextView(this);info.setText("\nBackground voice control. Voice stays active while other apps are open.\n");info.setTextSize(16);root.addView(info);
  Switch master=new Switch(this);master.setText("PHONE CONTROL");master.setChecked(ControlPrefs.enabled(this));master.setOnCheckedChangeListener((v,on)->{ControlPrefs.setEnabled(this,on);updateStatus();});root.addView(master);
  voice=new Switch(this);voice.setText("VOICE CONTROL (BACKGROUND)");voice.setOnCheckedChangeListener((v,on)->{if(on)requestStartVoice();else stopVoice();});root.addView(voice);
  TextView commands=new TextView(this);commands.setText("Say: home, back, recents, notifications, settings, scroll up/down, swipe left/right");commands.setPadding(0,15,0,15);root.addView(commands);
  add(root,"Open Accessibility Settings",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  add(root,"TEST: Home",v->PhoneControlService.runCommand("HOME",null));add(root,"TEST: Back",v->PhoneControlService.runCommand("BACK",null));add(root,"Swipe Left",v->PhoneControlService.runCommand("SWIPE_LEFT",null));add(root,"Swipe Right",v->PhoneControlService.runCommand("SWIPE_RIGHT",null));
  status=new TextView(this);status.setTextSize(15);status.setPadding(0,30,0,40);root.addView(status);setContentView(scroll);updateStatus();}
 private void requestStartVoice(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}startVoice();}
 private void startVoice(){Intent i=new Intent(this,VoiceControlService.class).setAction(VoiceControlService.ACTION_START);startForegroundService(i);updateStatus();}
 private void stopVoice(){Intent i=new Intent(this,VoiceControlService.class).setAction(VoiceControlService.ACTION_STOP);startService(i);updateStatus();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==MIC_REQUEST){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startVoice();else voice.setChecked(false);}}
 private void add(LinearLayout r,String s,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));b.setOnClickListener(l);r.addView(b);}
 private void updateStatus(){if(status!=null)status.setText("Master: "+(ControlPrefs.enabled(this)?"ON":"OFF")+"\nAccessibility service: "+(PhoneControlService.instance()!=null?"CONNECTED":"NOT CONNECTED")+"\nBackground voice uses the persistent Phone Control notification.");}
 @Override protected void onResume(){super.onResume();if(status!=null)updateStatus();}
}

package com.brannenservices.phonecontrol;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
 private static final int MIC_REQUEST=41;
 private TextView status,heard;
 private Switch voice;
 private SpeechRecognizer recognizer;
 private Intent speechIntent;
 private boolean listening=false, restarting=false;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(48,48,48,48); scroll.addView(root);
  TextView title=new TextView(this); title.setText("Phone Control Prototype v0.4"); title.setTextSize(27); title.setTextColor(Color.BLACK); root.addView(title);
  TextView info=new TextView(this); info.setText("\nHands-free local voice control. No OpenAI API key.\nPHONE CONTROL must be ON.\n"); info.setTextSize(16); root.addView(info);
  Switch master=new Switch(this); master.setText("PHONE CONTROL"); master.setChecked(ControlPrefs.enabled(this)); master.setOnCheckedChangeListener((v,on)->{ControlPrefs.setEnabled(this,on);updateStatus();}); root.addView(master);
  voice=new Switch(this); voice.setText("VOICE CONTROL"); voice.setOnCheckedChangeListener((v,on)->{ if(on) startVoice(); else stopVoice(); }); root.addView(voice);
  heard=new TextView(this); heard.setText("Say: home, back, recents, notifications, settings, scroll up/down, swipe left/right"); heard.setPadding(0,15,0,15); root.addView(heard);
  add(root,"Open Accessibility Settings",v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  add(root,"TEST: Home",v->PhoneControlService.runCommand("HOME",null)); add(root,"TEST: Back",v->PhoneControlService.runCommand("BACK",null));
  add(root,"TEST: Recents",v->PhoneControlService.runCommand("RECENTS",null)); add(root,"TEST: Notifications",v->PhoneControlService.runCommand("NOTIFICATIONS",null));
  add(root,"TEST: Open Android Settings",v->PhoneControlService.runCommand("OPEN_APP","com.android.settings"));
  TextView gestures=new TextView(this); gestures.setText("\nGesture tests"); gestures.setTextSize(19); root.addView(gestures);
  add(root,"Swipe Up",v->PhoneControlService.runCommand("SWIPE_UP",null)); add(root,"Swipe Down",v->PhoneControlService.runCommand("SWIPE_DOWN",null));
  add(root,"Swipe Left",v->PhoneControlService.runCommand("SWIPE_LEFT",null)); add(root,"Swipe Right",v->PhoneControlService.runCommand("SWIPE_RIGHT",null));
  EditText text=new EditText(this); text.setHint("Visible button/text to tap"); root.addView(text); add(root,"TEST: Tap matching text",v->PhoneControlService.runCommand("TAP_TEXT",text.getText().toString()));
  status=new TextView(this); status.setTextSize(15); status.setPadding(0,30,0,40); root.addView(status); setContentView(scroll); setupSpeech(); updateStatus();
 }
 private void setupSpeech(){
  if(!SpeechRecognizer.isRecognitionAvailable(this)){heard.setText("Speech recognition is not available on this phone.");return;}
  recognizer=SpeechRecognizer.createSpeechRecognizer(this);
  speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
  speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
  recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){listening=true;updateStatus();}
   public void onBeginningOfSpeech(){} public void onRmsChanged(float r){} public void onBufferReceived(byte[] b){}
   public void onEndOfSpeech(){listening=false;updateStatus();}
   public void onError(int e){listening=false;if(voice!=null&&voice.isChecked()) restartVoice();}
   public void onResults(Bundle b){listening=false;ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()) handleVoice(r.get(0));if(voice!=null&&voice.isChecked()) restartVoice();}
   public void onPartialResults(Bundle b){} public void onEvent(int t,Bundle b){}
  });
 }
 private void handleVoice(String raw){
  String s=raw.toLowerCase(Locale.US).trim(); heard.setText("Heard: "+raw);
  String cmd=null,arg=null;
  if(s.equals("home")||s.equals("go home"))cmd="HOME";
  else if(s.equals("back")||s.equals("go back"))cmd="BACK";
  else if(s.equals("recents")||s.equals("recent apps")||s.equals("open recents"))cmd="RECENTS";
  else if(s.equals("notifications")||s.equals("open notifications"))cmd="NOTIFICATIONS";
  else if(s.equals("settings")||s.equals("open settings")){cmd="OPEN_APP";arg="com.android.settings";}
  else if(s.equals("scroll down")||s.equals("swipe up"))cmd="SWIPE_UP";
  else if(s.equals("scroll up")||s.equals("swipe down"))cmd="SWIPE_DOWN";
  else if(s.equals("swipe left"))cmd="SWIPE_LEFT";
  else if(s.equals("swipe right"))cmd="SWIPE_RIGHT";
  if(cmd!=null)PhoneControlService.runCommand(cmd,arg); else heard.setText("Heard: "+raw+"\nNo exact command matched.");
 }
 private void startVoice(){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}
  if(recognizer!=null&&!listening){try{recognizer.startListening(speechIntent);}catch(Exception e){heard.setText("Voice start failed: "+e.getMessage());}}
 }
 private void restartVoice(){if(restarting)return;restarting=true;heard.postDelayed(()->{restarting=false;if(voice!=null&&voice.isChecked())startVoice();},450);}
 private void stopVoice(){if(recognizer!=null){try{recognizer.cancel();}catch(Exception ignored){}}listening=false;updateStatus();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==MIC_REQUEST){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){voice.setChecked(true);startVoice();}else{voice.setChecked(false);heard.setText("Microphone permission is required for voice control.");}}}
 private void add(LinearLayout r,String s,android.view.View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));b.setOnClickListener(l);r.addView(b);}
 private void updateStatus(){if(status!=null)status.setText("Master: "+(ControlPrefs.enabled(this)?"ON":"OFF")+"\nAccessibility service: "+(PhoneControlService.instance()!=null?"CONNECTED":"NOT CONNECTED")+"\nVoice: "+(listening?"LISTENING":"IDLE"));}
 @Override protected void onResume(){super.onResume();if(status!=null)updateStatus();}
 @Override protected void onDestroy(){stopVoice();if(recognizer!=null)recognizer.destroy();super.onDestroy();}
}

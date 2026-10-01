package com.brannenservices.phonecontrol;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.*;
import java.util.*;

public class VoiceControlService extends Service {
 public static final String ACTION_START="com.brannenservices.phonecontrol.START_VOICE";
 public static final String ACTION_STOP="com.brannenservices.phonecontrol.STOP_VOICE";
 private static final String CHANNEL="voice_control";
 private SpeechRecognizer recognizer; private Intent speechIntent;
 private boolean active=false,restarting=false;

 @Override public void onCreate(){super.onCreate();createChannel();setupSpeech();}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&ACTION_STOP.equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  active=true; startForeground(7,notification("Listening for phone-control commands")); startListening(); return START_STICKY;
 }
 private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Voice Control",NotificationManager.IMPORTANCE_LOW);c.setDescription("Keeps hands-free phone control listening while other apps are open.");getSystemService(NotificationManager.class).createNotificationChannel(c);}}
 private Notification notification(String text){
  Intent open=new Intent(this,MainActivity.class); PendingIntent p=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Intent stop=new Intent(this,VoiceControlService.class).setAction(ACTION_STOP); PendingIntent sp=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Phone Control voice active").setContentText(text).setOngoing(true).setContentIntent(p).addAction(new Notification.Action.Builder(null,"Stop",sp).build()).build();
 }
 private void setupSpeech(){
  recognizer=SpeechRecognizer.createSpeechRecognizer(this); speechIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()); speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
  recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float r){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){}
   public void onError(int e){if(active)restart();}
   public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty())handle(r.get(0));if(active)restart();}
   public void onPartialResults(Bundle b){} public void onEvent(int t,Bundle b){}
  });
 }
 private void handle(String raw){
  if(!ControlPrefs.enabled(this))return; String s=raw.toLowerCase(Locale.US).trim(); String c=null,a=null;
  if(s.equals("home")||s.equals("go home"))c="HOME"; else if(s.equals("back")||s.equals("go back"))c="BACK";
  else if(s.equals("recents")||s.equals("recent apps")||s.equals("open recents"))c="RECENTS";
  else if(s.equals("notifications")||s.equals("open notifications"))c="NOTIFICATIONS";
  else if(s.equals("settings")||s.equals("open settings")){c="OPEN_APP";a="com.android.settings";}\n  else if(s.startsWith("open ")&&s.length()>5){c="OPEN_NAMED_APP";a=s.substring(5).trim();}
  else if(s.equals("scroll down")||s.equals("swipe up"))c="SWIPE_UP"; else if(s.equals("scroll up")||s.equals("swipe down"))c="SWIPE_DOWN";
  else if(s.equals("swipe left"))c="SWIPE_LEFT"; else if(s.equals("swipe right"))c="SWIPE_RIGHT";
  if(c!=null)PhoneControlService.runCommand(c,a);
 }
 private void startListening(){if(!active||recognizer==null)return;try{recognizer.startListening(speechIntent);}catch(Exception e){restart();}}
 private void restart(){if(restarting||!active)return;restarting=true;new Handler(Looper.getMainLooper()).postDelayed(()->{restarting=false;startListening();},350);}
 @Override public void onDestroy(){active=false;if(recognizer!=null){try{recognizer.cancel();}catch(Exception ignored){}recognizer.destroy();}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}

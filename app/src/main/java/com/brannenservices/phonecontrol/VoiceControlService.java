package com.brannenservices.phonecontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.Locale;

/** Explicitly enabled, notification-visible speech listener using Android's speech engine. */
public class VoiceControlService extends Service {
    public static final String ACTION_START = "com.brannenservices.phonecontrol.START_VOICE";
    public static final String ACTION_STOP = "com.brannenservices.phonecontrol.STOP_VOICE";
    private static final String CHANNEL = "voice_control";
    private static final String PREFS = "voice_status";
    private static final String LAST_HEARD = "last_heard";
    private static volatile boolean running;
    private static volatile VoiceControlService current;

    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active;
    private boolean restarting;
    private boolean readingScreen;
    private int retryDelayMs = 350;
    private TextToSpeech tts;
    private long lastRecognizerActivity=0L;
    private final Runnable watchdog=new Runnable(){
        @Override public void run(){
            if(active && !readingScreen){
                long now=android.os.SystemClock.elapsedRealtime();
                if(lastRecognizerActivity>0 && now-lastRecognizerActivity>20000){
                    try { if(recognizer!=null) recognizer.cancel(); } catch(Exception ignored){}
                    restarting=false;
                    startListening();
                }
            }
            if(active) handler.postDelayed(this,10000);
        }
    };

    public static boolean isRunning() { return running; }
    public static void announceConfirmation(String description) {
        VoiceControlService service = current;
        if (service != null) service.updateNotification("Confirm action: " + description + ". Say confirm or cancel.");
    }
    public static String lastHeard(android.content.Context context) {
        return context.getSharedPreferences(PREFS, 0).getString(LAST_HEARD, "");
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        setupSpeech();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            active = false;
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ControlPrefs.enabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        active = true;
        running = true;
        current = this;
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, notification("Listening for commands"), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(7, notification("Listening for commands"));
        lastRecognizerActivity=android.os.SystemClock.elapsedRealtime();
        startListening();
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog,10000);
        return START_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Voice Control", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible notification while hands-free voice control is listening.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification notification(String message) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stopIntent = new Intent(this, VoiceControlService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Brannen Voice Control")
                .setContentText(message)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, "Stop listening", stop).build())
                .build();
    }

    private void updateNotification(String message) {
        getSystemService(NotificationManager.class).notify(7, notification(message));
    }

    private void setupSpeech() {
        try {
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            } else if (SpeechRecognizer.isRecognitionAvailable(this)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            }
        } catch (Exception ignored) {
            try { recognizer = SpeechRecognizer.createSpeechRecognizer(this); } catch (Exception ignoredAgain) { recognizer = null; }
        }
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        if (recognizer == null) return;
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { lastRecognizerActivity=android.os.SystemClock.elapsedRealtime(); }
            @Override public void onBeginningOfSpeech() { lastRecognizerActivity=android.os.SystemClock.elapsedRealtime(); }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { lastRecognizerActivity=android.os.SystemClock.elapsedRealtime(); }
            @Override public void onError(int error) { lastRecognizerActivity=android.os.SystemClock.elapsedRealtime(); scheduleRestart(); }
            @Override public void onResults(Bundle results) {
                lastRecognizerActivity=android.os.SystemClock.elapsedRealtime();
                ArrayList<String> candidates = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (candidates != null && !candidates.isEmpty()) {
                    String chosen=candidates.get(0);
                    for(String candidate:candidates){
                        if(VoiceCommandParser.parse(candidate)!=null){ chosen=candidate; break; }
                    }
                    handle(chosen);
                }
                retryDelayMs = 350;
                scheduleRestart();
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void handle(String raw) {
        if (raw == null || !active) return;
        getSharedPreferences(PREFS, 0).edit().putString(LAST_HEARD, "“" + raw.trim() + "”").apply();
        if (!ControlPrefs.enabled(this)) { updateNotification("Phone Control is off"); return; }
        VoiceCommandParser.Result result = VoiceCommandParser.parse(raw);
        if (result == null) { updateNotification("Heard: " + raw.trim()); return; }
        if (result.stopListening) {
            updateNotification("Voice control stopped by command");
            active = false;
            running = false;
            stopSelf();
            return;
        }
        if (result.readScreen) {
            readScreenAloud();
            return;
        }
        boolean done = PhoneControlService.runCommand(result.action, result.argument);
        if (PhoneControlService.confirmationPending()) updateNotification("Action held. Say confirm or cancel.");
        else if ("SHOW_NUMBERS".equals(result.action) && !done) updateNotification("No numbered controls found on this screen");
        else if ("OPEN_NAMED_APP".equals(result.action) && !done) updateNotification("Couldn't find app: " + result.argument);
        else if (("TAP_TEXT".equals(result.action) || "LONG_PRESS_TEXT".equals(result.action)) && !done) updateNotification("Couldn't find: " + result.argument);
        else if ("FOCUS_FIELD".equals(result.action) && !done) updateNotification("Couldn't find a " + result.argument + " field");
        else if ("TAP_REGION".equals(result.action) && !done) updateNotification("Couldn't tap that screen region");
        else if ("TYPE_TEXT".equals(result.action) && !done) updateNotification("No text field is selected");
        else updateNotification("Command: " + result.action.toLowerCase(Locale.US).replace('_', ' '));
    }

    private void readScreenAloud() {
        String screen = PhoneControlService.readScreen();
        if (screen.isEmpty()) screen = "I can't read text on this screen.";
        readingScreen = true;
        if (recognizer != null) try { recognizer.cancel(); } catch (Exception ignored) { }
        final String text = screen;
        if (tts == null) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());
                    speak(text);
                } else { readingScreen = false; scheduleRestart(); }
            });
        } else speak(text);
        updateNotification("Reading screen aloud");
    }

    private void speak(String text) {
        if (tts == null) return;
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { readingScreen = false; scheduleRestart(); }
            @Override public void onError(String id) { readingScreen = false; scheduleRestart(); }
        });
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "screen_read");
    }

    private void startListening() {
        if (!active) return;
        if (recognizer == null) {
            updateNotification("Speech recognition isn't available on this phone");
            scheduleRestart();
            return;
        }
        try { restarting = false; lastRecognizerActivity=android.os.SystemClock.elapsedRealtime(); recognizer.startListening(speechIntent); }
        catch (Exception ignored) { scheduleRestart(); }
    }

    private void scheduleRestart() {
        if (restarting || !active || readingScreen) return;
        restarting = true;
        int delay = retryDelayMs;
        retryDelayMs = Math.min(retryDelayMs * 2, 5000);
        handler.postDelayed(() -> { restarting = false; startListening(); }, delay);
    }

    @Override public void onDestroy() {
        active = false;
        running = false;
        handler.removeCallbacks(watchdog);
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) { }
            recognizer.destroy();
        }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (current == this) current = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}

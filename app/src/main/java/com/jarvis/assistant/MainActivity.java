package com.jarvis.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView statusText;
    private ImageView micButton;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        micButton = findViewById(R.id.micButton);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.ENGLISH);
            }
        });

        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception ignored) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("Listening..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override
            public void onError(int error) {
                statusText.setText("Press mic and try again");
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handleVoiceCommand(matches.get(0));
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        micButton.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-IN");

        speechRecognizer.startListening(intent);
    }

    private void handleVoiceCommand(String rawCmd) {
        statusText.setText("\"" + rawCmd + "\"");
        String cmd = rawCmd.toLowerCase().trim();

        if (cmd.contains("song") || cmd.contains("play") || cmd.contains("youtube") || cmd.contains("baja") || cmd.contains("gana")) {
            String songName = cmd.replaceAll("(?i)(open youtube and play|open youtube|youtube open karke|play|song|baja do|baja|gana|karo)", "").trim();
            if (songName.isEmpty()) {
                songName = "295";
            }

            speakOut("Playing " + songName + " on YouTube, sir");

            Intent ytIntent = new Intent(Intent.ACTION_SEARCH);
            ytIntent.setPackage("com.google.android.youtube");
            ytIntent.putExtra("query", songName);
            ytIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(ytIntent);
            } catch (Exception e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(songName)));
                webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            }
            return;
        }

        if (cmd.contains("call") || cmd.contains("dial") || cmd.contains("phone")) {
            String name = cmd.replaceAll("(?i)(call to|call lagao|call karo|call|dial)", "").trim();
            speakOut("Calling " + (name.isEmpty() ? "selected contact" : name) + ", sir");

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            String digits = name.replaceAll("[^0-9+]", "");
            if (!digits.isEmpty()) {
                callIntent.setData(Uri.parse("tel:" + digits));
            }
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(callIntent);
            } catch (Exception ignored) {}
            return;
        }

        if (cmd.contains("flashlight") || cmd.contains("torch")) {
            if (cmd.contains("on") || cmd.contains("chalu") || cmd.contains("jalao")) {
                toggleFlashlight(true);
                speakOut("Flashlight turned on, sir");
            } else if (cmd.contains("off") || cmd.contains("band")) {
                toggleFlashlight(false);
                speakOut("Flashlight turned off, sir");
            }
            return;
        }

        if (cmd.contains("time") || cmd.contains("samay")) {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            speakOut("The current time is " + currentTime + ", sir");
            return;
        }

        speakOut("Searching for " + rawCmd);
        Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(rawCmd)));
        searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(searchIntent);
    }

    private void toggleFlashlight(boolean state) {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, state);
                isFlashOn = state;
            }
        } catch (Exception ignored) {}
    }

    private void speakOut(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

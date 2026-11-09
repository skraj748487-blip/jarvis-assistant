package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private TextView statusText;
    private ImageView micButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        micButton = findViewById(R.id.mic_btn);

        // TTS Engine Init
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
                speakOut("जार्विस ऑनलाइन है, सर। क्या हुक्म है?");
            }
        });

        // Speech Recognizer Init
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("सुन रहा हूँ..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { statusText.setText("प्रोसेसिंग..."); }
            @Override public void onError(int error) { statusText.setText("आवाज़ समझ नहीं आई, दोबारा टैप करें।"); }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handleVoiceCommand(matches.get(0).toLowerCase());
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        speechRecognizer.startListening(intent);
    }

    private void handleVoiceCommand(String cmd) {
        statusText.setText("\"" + cmd + "\"");

        // 1. YouTube & Music
        if (cmd.contains("gana") || cmd.contains("song") || cmd.contains("bajao") || cmd.contains("youtube")) {
            String query = cmd.replaceAll("(gana|song|bajao|chalao|play|youtube|kholo|open)", "").trim();
            speakOut("यूट्यूब चला रहा हूँ, सर।");
            Intent ytIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
            startActivity(ytIntent);
            return;
        }

        // 2. Camera Trigger
        if (cmd.contains("camera") || cmd.contains("photo")) {
            speakOut("कैमरा खोल दिया गया है, सर।");
            Intent camIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            startActivity(camIntent);
            return;
        }

        // 3. Siri-Style Universal App Launcher (Queries phone launcher directly)
        String targetApp = cmd.replaceAll("(kholo|open karo|open|chalao|app)", "").trim();
        boolean found = launchAnyAppByName(targetApp);

        if (!found) {
            speakOut(targetApp + " ऐप फ़ोन में नहीं मिला, सर।");
        }
    }

    private boolean launchAnyAppByName(String appName) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();
            if (label.contains(appName) || appName.contains(label)) {
                Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                if (launchIntent != null) {
                    speakOut(label + " खोल रहा हूँ, सर।");
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return true;
                }
            }
        }
        return false;
    }

    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        super.onDestroy();
    }
}

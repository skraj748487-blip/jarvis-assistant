package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

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

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
                speakOut("जार्विस तैयार है, सर।");
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("सुन रहा हूँ..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { statusText.setText("प्रोसेस कर रहा हूँ..."); }
            @Override public void onError(int error) { statusText.setText("आवाज़ समझ नहीं आई, दोबारा टैप करें।"); }

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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        speechRecognizer.startListening(intent);
    }

    private void handleVoiceCommand(String rawCmd) {
        statusText.setText("\"" + rawCmd + "\"");
        String cmd = rawCmd.toLowerCase().trim();

        // 1. हेलो जार्विस
        if (cmd.contains("हेलो") || cmd.contains("hello") || cmd.contains("हाय") || cmd.contains("hi") || cmd.equals("जार्विस") || cmd.equals("jarvis")) {
            speakOut("नमस्ते साहिल सर, बताइए क्या काम करना है?");
            return;
        }

        // 2. गैलरी / फ़ोटोज़ (Direct Media Intent + Package fallback)
        if (cmd.contains("गैलरी") || cmd.contains("gallery") || cmd.contains("photos") || cmd.contains("फोटो") || cmd.contains("फ़ोटो")) {
            speakOut("गैलरी खोल रहा हूँ, सर।");
            Intent galleryIntent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isIntentAvailable(galleryIntent)) {
                startActivity(galleryIntent);
            } else {
                launchPackageOrUrl("com.google.android.apps.photos", null);
            }
            return;
        }

        // 3. फ़ोन डायलर / कॉल
        if (cmd.contains("फोन") || cmd.contains("फ़ोन") || cmd.contains("phone") || cmd.contains("कॉल") || cmd.contains("call") || cmd.contains("डायलर")) {
            speakOut("फ़ोन डायलर खोल रहा हूँ, सर।");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
            return;
        }

        // 4. सेटिंग्स
        if (cmd.contains("सेटिंग") || cmd.contains("सेटिंग्स") || cmd.contains("setting") || cmd.contains("settings")) {
            speakOut("सेटिंग्स खोल रहा हूँ, सर।");
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            return;
        }

        // 5. कैमरा
        if (cmd.contains("कैमरा") || cmd.contains("camera")) {
            speakOut("कैमरा खोल रहा हूँ, सर।");
            Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cameraIntent);
            return;
        }

        // 6. YouTube
        if (cmd.contains("युटुब") || cmd.contains("यूट्यूब") || cmd.contains("youtube") || cmd.contains("gana") || cmd.contains("गाने")) {
            speakOut("यूट्यूब खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.google.android.youtube", "https://www.youtube.com");
            return;
        }

        // 7. WhatsApp
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            speakOut("व्हाट्सएप खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.whatsapp", null);
            return;
        }

        // 8. Instagram
        if (cmd.contains("इंस्टाग्राम") || cmd.contains("instagram") || cmd.contains("insta")) {
            speakOut("इंस्टाग्राम खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.instagram.android", null);
            return;
        }

        // 9. क्रोम ब्राउज़र
        if (cmd.contains("क्रोम") || cmd.contains("chrome") || cmd.contains("browser") || cmd.contains("ब्राउज़र")) {
            speakOut("गूगल क्रोम खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.android.chrome", "https://www.google.com");
            return;
        }

        // 10. फ़ोन में मौजूद बाकी सभी ऐप्स की यूनिवर्सल सर्च (Full Package Access)
        String cleaned = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|open|kholo|chalao|app|ऐप|दिखाओ)", "").trim();
        boolean found = launchAnyAppByName(cleaned);

        if (!found) {
            speakOut(rawCmd + " समझ नहीं आया, सर।");
        }
    }

    private boolean isIntentAvailable(Intent intent) {
        PackageManager pm = getPackageManager();
        return !pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }

    private void launchPackageOrUrl(String packageName, String fallbackUrl) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (fallbackUrl != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        } else {
            speakOut("यह ऐप इस फ़ोन में इंस्टॉल नहीं है, सर।");
        }
    }

    private boolean launchAnyAppByName(String query) {
        if (query.isEmpty()) return false;
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : packages) {
            String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();
            String pkg = appInfo.packageName.toLowerCase();

            if (label.contains(query) || pkg.contains(query)) {
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
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        super.onDestroy();
    }
}

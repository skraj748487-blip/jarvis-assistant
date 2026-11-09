package com.jarvis.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private TextView statusText;
    private ImageView micButton;
    private boolean isFlashOn = false;

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

        // 1. सवाल-जवाब (Smart Conversation)
        if (cmd.contains("हेलो") || cmd.contains("hello") || cmd.contains("हाय") || cmd.equals("जार्विस")) {
            speakOut("नमस्ते साहिल सर, बताइए क्या हुक्म है?");
            return;
        }
        if (cmd.contains("नाम क्या है") || cmd.contains("who are you") || cmd.contains("कौन हो")) {
            speakOut("मैं जार्विस हूँ, आपका पर्सनल वॉइस असिस्टेंट।");
            return;
        }
        if (cmd.contains("टाइम") || cmd.contains("समय") || cmd.contains("कितने बजे")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            speakOut("अभी समय हुआ है " + time);
            return;
        }
        if (cmd.contains("तारीख") || cmd.contains("डेट") || cmd.contains("date")) {
            String date = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
            speakOut("आज की तारीख है " + date);
            return;
        }

        // 2. फ़ाइल मैनेजर (Files / My Files / File Manager)
        if (cmd.contains("फाइल") || cmd.contains("फ़ाइल") || cmd.contains("file")) {
            speakOut("फ़ाइल मैनेजर खोल रहा हूँ, सर।");
            if (!launchPackageIfExists("com.google.android.documentsui")) {
                if (!launchPackageIfExists("com.google.android.apps.nbu.files")) {
                    if (!launchPackageIfExists("com.oneplus.filemanager")) {
                        launchAnyAppByName("file");
                    }
                }
            }
            return;
        }

        // 3. टॉर्च / फ़्लैशलाइट
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश") || cmd.contains("torch") || cmd.contains("flashlight")) {
            if (cmd.contains("बंद") || cmd.contains("off")) {
                toggleFlashlight(false);
                speakOut("टॉर्च बंद कर दी गई है।");
            } else {
                toggleFlashlight(true);
                speakOut("टॉर्च चालू कर दी गई है।");
            }
            return;
        }

        // 4. गैलरी / फोटो
        if (cmd.contains("गैलरी") || cmd.contains("gallery") || cmd.contains("photos") || cmd.contains("फोटो") || cmd.contains("फ़ोटो")) {
            speakOut("गैलरी खोल रहा हूँ, सर।");
            Intent galleryIntent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(galleryIntent);
            return;
        }

        // 5. फ़ोन डायलर
        if (cmd.contains("फोन") || cmd.contains("फ़ोन") || cmd.contains("phone") || cmd.contains("कॉल") || cmd.contains("call") || cmd.contains("डायलर")) {
            speakOut("फ़ोन डायलर खोल रहा हूँ, सर।");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
            return;
        }

        // 6. अलार्म / घड़ी
        if (cmd.contains("अलार्म") || cmd.contains("घड़ी") || cmd.contains("clock") || cmd.contains("alarm")) {
            speakOut("घड़ी खोल रहा हूँ, सर।");
            Intent clockIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(clockIntent);
            return;
        }

        // 7. कैलकुलेटर
        if (cmd.contains("कैलकुलेटर") || cmd.contains("calculator") || cmd.contains("हिसाब")) {
            speakOut("कैलकुलेटर खोल रहा हूँ, सर।");
            launchAnyAppByName("calculator");
            return;
        }

        // 8. सेटिंग्स
        if (cmd.contains("सेटिंग") || cmd.contains("setting")) {
            speakOut("सेटिंग्स खोल रहा हूँ, सर।");
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            return;
        }

        // 9. सोशल ऐप्स
        if (cmd.contains("युटुब") || cmd.contains("यूट्यूब") || cmd.contains("youtube") || cmd.contains("gana") || cmd.contains("गाने")) {
            speakOut("यूट्यूब खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.google.android.youtube", "https://www.youtube.com");
            return;
        }
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            speakOut("व्हाट्सएप खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.whatsapp", null);
            return;
        }
        if (cmd.contains("इंस्टाग्राम") || cmd.contains("instagram")) {
            speakOut("इंस्टाग्राम खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.instagram.android", null);
            return;
        }
        if (cmd.contains("कैमरा") || cmd.contains("camera")) {
            speakOut("कैमरा खोल रहा हूँ, सर।");
            Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cameraIntent);
            return;
        }
        if (cmd.contains("क्रोम") || cmd.contains("chrome")) {
            speakOut("गूगल क्रोम खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.android.chrome", "https://www.google.com");
            return;
        }

        // 10. बाकी कोई भी ऐप जो फ़ोन में हो
        String cleaned = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|open|kholo|chalao|app|ऐप|दिखाओ)", "").trim();
        boolean found = launchAnyAppByName(cleaned);

        if (!found) {
            speakOut(rawCmd + " समझ नहीं आया, सर।");
        }
    }

    private void toggleFlashlight(boolean status) {
        try {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, status);
            isFlashOn = status;
        } catch (Exception ignored) {}
    }

    private boolean launchPackageIfExists(String packageName) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void launchPackageOrUrl(String packageName, String fallbackUrl) {
        if (!launchPackageIfExists(packageName) && fallbackUrl != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
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

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private TextView statusText;
    private ImageView micButton;
    private static final Map<String, String> HINDI_APP_MAP = new HashMap<>();

    static {
        HINDI_APP_MAP.put("फाइल", "file");
        HINDI_APP_MAP.put("फ़ाइल", "file");
        HINDI_APP_MAP.put("कैलकुलेटर", "calculator");
        HINDI_APP_MAP.put("कैलेंडर", "calendar");
        HINDI_APP_MAP.put("घड़ी", "clock");
        HINDI_APP_MAP.put("अलार्म", "clock");
        HINDI_APP_MAP.put("मैसेज", "message");
        HINDI_APP_MAP.put("संदेश", "message");
        HINDI_APP_MAP.put("कांटेक्ट", "contact");
        HINDI_APP_MAP.put("कॉन्टैक्ट", "contact");
        HINDI_APP_MAP.put("फोन", "phone");
        HINDI_APP_MAP.put("फ़ोन", "phone");
        HINDI_APP_MAP.put("कॉल", "phone");
        HINDI_APP_MAP.put("प्ले स्टोर", "play store");
        HINDI_APP_MAP.put("मैप", "maps");
        HINDI_APP_MAP.put("नक्शा", "maps");
        HINDI_APP_MAP.put("गाना", "music");
        HINDI_APP_MAP.put("म्यूजिक", "music");
        HINDI_APP_MAP.put("रिकॉर्डर", "recorder");
        HINDI_APP_MAP.put("नोट्स", "notes");
    }

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

        // 1. बातचीत और परिचय
        if (cmd.contains("हेलो") || cmd.contains("hello") || cmd.contains("हाय") || cmd.equals("जार्विस")) {
            speakOut("नमस्ते साहिल सर, बताइए क्या हुक्म है?");
            return;
        }
        if (cmd.contains("नाम क्या है") || cmd.contains("कौन हो") || cmd.contains("who are you")) {
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

        // 2. टॉर्च कंट्रोल
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश") || cmd.contains("torch") || cmd.contains("flashlight")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            toggleFlashlight(turnOn);
            speakOut(turnOn ? "टॉर्च चालू कर दी गई है।" : "टॉर्च बंद कर दी गई है।");
            return;
        }

        // 3. फ़ोन डायलर
        if (cmd.contains("फोन") || cmd.contains("फ़ोन") || cmd.contains("phone") || cmd.contains("डायलर") || cmd.contains("कॉल")) {
            speakOut("फ़ोन डायलर खोल रहा हूँ, सर।");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
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

        // 5. सेटिंग्स
        if (cmd.contains("सेटिंग") || cmd.contains("setting")) {
            speakOut("सेटिंग्स खोल रहा हूँ, सर।");
            Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            return;
        }

        // 6. कैमरा
        if (cmd.contains("कैमरा") || cmd.contains("camera")) {
            speakOut("कैमरा खोल रहा हूँ, सर।");
            Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cameraIntent);
            return;
        }

        // 7. यूट्यूब
        if (cmd.contains("युटुब") || cmd.contains("यूट्यूब") || cmd.contains("youtube")) {
            speakOut("यूट्यूब खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.google.android.youtube", "https://www.youtube.com");
            return;
        }

        // 8. व्हाट्सएप
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            speakOut("व्हाट्सएप खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.whatsapp", null);
            return;
        }

        // 9. इंस्टाग्राम
        if (cmd.contains("इंस्टाग्राम") || cmd.contains("instagram")) {
            speakOut("इंस्टाग्राम खोल रहा हूँ, सर।");
            launchPackageOrUrl("com.instagram.android", null);
            return;
        }

        // 10. पूरे मोबाइल के हर ऐप की डीप सर्च (100% Launchable Apps Scanner)
        String query = cleanQuery(cmd);
        String translatedQuery = translateHindiApp(query);

        boolean opened = searchAndLaunchApp(translatedQuery);
        if (!opened && !query.equals(translatedQuery)) {
            opened = searchAndLaunchApp(query);
        }

        // 11. अगर कोई ऐप नहीं है, तो सीधे Google पर सर्च करें (Assistant Feature)
        if (!opened) {
            speakOut(rawCmd + " गूगल पर खोज रहा हूँ, सर।");
            Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(rawCmd)));
            searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(searchIntent);
        }
    }

    private String cleanQuery(String input) {
        return input.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|लगाओ|open|kholo|chalao|app|ऐप|दिखाओ|चालू)", "").trim();
    }

    private String translateHindiApp(String query) {
        for (Map.Entry<String, String> entry : HINDI_APP_MAP.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return query;
    }

    private boolean searchAndLaunchApp(String query) {
        if (query.isEmpty()) return false;
        PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : appList) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            String pkg = info.activityInfo.packageName.toLowerCase();

            if (label.contains(query) || pkg.contains(query) || query.contains(label)) {
                Intent launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
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

    private void toggleFlashlight(boolean status) {
        try {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, status);
        } catch (Exception ignored) {}
    }

    private void launchPackageOrUrl(String packageName, String fallbackUrl) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (fallbackUrl != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
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

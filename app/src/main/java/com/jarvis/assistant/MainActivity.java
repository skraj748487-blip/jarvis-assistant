package com.jarvis.assistant;

import android.app.Activity;
import android.app.SearchManager;
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

import java.net.URLEncoder;
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

    private static final Map<String, String> DIRECT_APP_MAP = new HashMap<>();

    static {
        // Daily UPI, Social & Tools
        DIRECT_APP_MAP.put("phonepe", "com.phonepe.app");
        DIRECT_APP_MAP.put("paytm", "net.one97.paytm");
        DIRECT_APP_MAP.put("gpay", "com.google.android.apps.nbu.paisa.user");
        DIRECT_APP_MAP.put("googlepay", "com.google.android.apps.nbu.paisa.user");
        DIRECT_APP_MAP.put("whatsapp", "com.whatsapp");
        DIRECT_APP_MAP.put("instagram", "com.instagram.android");
        DIRECT_APP_MAP.put("facebook", "com.facebook.katana");
        DIRECT_APP_MAP.put("youtube", "com.google.android.youtube");
        DIRECT_APP_MAP.put("snapchat", "com.snapchat.android");
        DIRECT_APP_MAP.put("telegram", "org.telegram.messenger");
        DIRECT_APP_MAP.put("termux", "com.termux");
        DIRECT_APP_MAP.put("flipkart", "com.flipkart.android");
        DIRECT_APP_MAP.put("amazon", "in.amazon.mShop.android.shopping");
        DIRECT_APP_MAP.put("zomato", "com.application.zomato");
        DIRECT_APP_MAP.put("swiggy", "in.swiggy.android");
        DIRECT_APP_MAP.put("mxplayer", "com.mxtech.videoplayer.ad");
        DIRECT_APP_MAP.put("whereismytrain", "com.whereismytrain.android");
        DIRECT_APP_MAP.put("maps", "com.google.android.apps.maps");
        DIRECT_APP_MAP.put("chrome", "com.android.chrome");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        micButton = findViewById(R.id.mic_btn);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                speakOut("Jarvis online, sir.");
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("Listening..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { statusText.setText("Processing..."); }
            @Override public void onError(int error) { statusText.setText("Could not hear, tap mic again."); }

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
        speechRecognizer.startListening(intent);
    }

    private void handleVoiceCommand(String rawCmd) {
        statusText.setText("\"" + rawCmd + "\"");
        String cmd = rawCmd.toLowerCase().trim();

        // 1. YouTube Song / Music Auto-Play
        if (cmd.contains("song") || cmd.contains("play") || cmd.contains("gana") || cmd.contains("baja") || cmd.contains("chalao") || cmd.contains("गाना") || cmd.contains("बजाओ") || cmd.contains("चलाओ")) {
            String query = cmd.replaceAll("(youtube par|youtube me|youtube open karke|youtube|play song|play|song|gana bajao|gana chalao|gana|bajao|chalao|kholo|युटुब|यूट्यूब|गाना बजाओ|गाना चलाओ|गाना|बजाओ|चलाओ)", "").trim();
            if (query.isEmpty()) query = "trending songs";

            speakOut("Playing " + query + " on YouTube, sir.");
            try {
                Intent ytIntent = new Intent(Intent.ACTION_SEARCH);
                ytIntent.setPackage("com.google.android.youtube");
                ytIntent.putExtra("query", query);
                ytIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ytIntent);
            } catch (Exception e) {
                Intent webYt = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query)));
                webYt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webYt);
            }
            return;
        }

        // 2. Direct Calling (English + Hindi voice support)
        if (cmd.contains("call") || cmd.contains("phone") || cmd.contains("dial") || cmd.contains("कॉल") || cmd.contains("फोन")) {
            String target = cmd.replaceAll("(call to|call|make a call to|phone lagao|phone karo|dial|ke paas call karo|ko call lagao|ko call karo|कॉल लगाओ|कॉल करो|कॉल|फोन लगाओ|फोन करो|फोन|ke paas|ko)", "").trim();
            speakOut("Calling " + (target.isEmpty() ? "dialer" : target) + ", sir.");

            String digitsOnly = target.replaceAll("[^0-9+]", "");
            Intent callIntent;
            if (!digitsOnly.isEmpty()) {
                callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + digitsOnly));
            } else {
                callIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/"));
            }
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(callIntent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_DIAL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }

        // 3. WhatsApp Direct Chat & Message
        if (cmd.contains("whatsapp") || cmd.contains("व्हाट्सएप")) {
            String clean = cmd.replaceAll("(whatsapp par|whatsapp me|whatsapp open karke|whatsapp kholo|whatsapp|व्हाट्सएप पर|व्हाट्सएप)", "").trim();
            String msg = clean.replaceAll("(message bhejo|message karo|message send karo|send message|message|bhejo|karo|send)", "").trim();
            if (msg.isEmpty()) msg = "Hello";

            speakOut("Opening WhatsApp, sir.");
            Intent waIntent = new Intent(Intent.ACTION_SEND);
            waIntent.setType("text/plain");
            waIntent.setPackage("com.whatsapp");
            waIntent.putExtra(Intent.EXTRA_TEXT, msg);
            waIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(waIntent);
            } catch (Exception e) {
                launchByPackage("com.whatsapp");
            }
            return;
        }

        // 4. Torch / Flashlight
        if (cmd.contains("torch") || cmd.contains("flashlight") || cmd.contains("light") || cmd.contains("टॉर्च")) {
            boolean turnOn = !cmd.contains("off") && !cmd.contains("band") && !cmd.contains("बंद");
            setFlashlight(turnOn);
            speakOut(turnOn ? "Torch turned on." : "Torch turned off.");
            return;
        }

        // 5. Camera & Gallery
        if (cmd.contains("camera") || cmd.contains("photo") || cmd.contains("कैमरा")) {
            speakOut("Opening camera, sir.");
            startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (cmd.contains("gallery") || cmd.contains("photos") || cmd.contains("गैलरी")) {
            speakOut("Opening gallery, sir.");
            startActivity(new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 6. Direct Named Apps
        for (Map.Entry<String, String> entry : DIRECT_APP_MAP.entrySet()) {
            if (cmd.contains(entry.getKey())) {
                speakOut("Opening " + entry.getKey() + ", sir.");
                if (launchByPackage(entry.getValue())) return;
            }
        }

        // 7. Universal System Scan
        String cleanQuery = cmd.replaceAll("(open|launch|kholo|chalao|start|app)", "").trim();
        if (launchUniversalApp(cleanQuery)) return;

        speakOut(rawCmd + " command not recognized, sir.");
    }

    private boolean launchByPackage(String pkg) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private boolean launchUniversalApp(String query) {
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
                    speakOut("Opening " + label + ", sir.");
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return true;
                }
            }
        }
        return false;
    }

    private void setFlashlight(boolean enable) {
        try {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, enable);
        } catch (Exception ignored) {}
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

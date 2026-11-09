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
import java.util.Map;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private TextView statusText;
    private ImageView micButton;

    private static final Map<String, String> DIRECT_APP_MAP = new HashMap<>();

    static {
        DIRECT_APP_MAP.put("phonepe", "com.phonepe.app");
        DIRECT_APP_MAP.put("paytm", "net.one97.paytm");
        DIRECT_APP_MAP.put("gpay", "com.google.android.apps.nbu.paisa.user");
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
            @Override public void onError(int error) { statusText.setText("आवाज़ समझ नहीं आई, दोबारा दबाएँ।"); }

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

        // 1. YouTube Song - Clean Uri Search
        if (cmd.contains("गाना") || cmd.contains("song") || cmd.contains("बजाओ") || cmd.contains("चलाओ") || cmd.contains("play")) {
            String songName = cmd.replaceAll("(युटुब ओपन करके|यूट्यूब ओपन करके|युटुब खोल के|यूट्यूब खोल के|यूट्यूब पर|यूट्यूब में|युटुब|यूट्यूब|youtube|गाना बजाओ|गाना चलाओ|गाना लगाओ|गाना|बजाओ|चलाओ|लगाओ|ओपन करके|खोल के|ओपन करो|खोलो|play|song)", "").trim();
            if (songName.isEmpty()) songName = "295";

            speakOut(songName + " बजा रहा हूँ, सर।");

            Intent ytIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(songName)));
            ytIntent.setPackage("com.google.android.youtube");
            ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(ytIntent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(songName))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }

        // 2. Call
        if (cmd.contains("कॉल") || cmd.contains("call") || cmd.contains("फोन लगाओ") || cmd.contains("फोन करो")) {
            String name = cmd.replaceAll("(के पास कॉल करो|के पास कॉल लगाओ|को कॉल लगाओ|को कॉल करो|पर कॉल लगाओ|पर कॉल करो|कॉल लगाओ|कॉल करो|कॉल|फोन लगाओ|फोन करो|call|फोन)", "").trim();
            speakOut((name.isEmpty() ? "कॉल" : name + " को कॉल") + " मिला रहा हूँ, सर।");

            Intent callIntent = new Intent(Intent.ACTION_DIAL);
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

        // 3. WhatsApp
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            String msg = cmd.replaceAll("(व्हाट्सएप खोल के|व्हाट्सएप पर|व्हाट्सएप में|व्हाट्सएप|whatsapp|मैसेज करो|मैसेज भेजो|मैसेज|भेजो|करो|लिखो)", "").trim();
            speakOut("व्हाट्सएप खोल रहा हूँ, सर।");

            if (!msg.isEmpty() && !msg.equals("ओपन") && !msg.equals("खोलो")) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.setPackage("com.whatsapp");
                sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(sendIntent);
                    return;
                } catch (Exception ignored) {}
            }
            launchByPackage("com.whatsapp");
            return;
        }

        // 4. Torch
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश") || cmd.contains("torch")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            setFlashlight(turnOn);
            speakOut(turnOn ? "टॉर्च चालू कर दी।" : "टॉर्च बंद कर दी।");
            return;
        }

        // 5. Camera & Gallery
        if (cmd.contains("कैमरा") || cmd.contains("camera")) {
            speakOut("कैमरा खोल रहा हूँ।");
            startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (cmd.contains("गैलरी") || cmd.contains("फोटो") || cmd.contains("gallery")) {
            speakOut("गैलरी खोल रहा हूँ।");
            startActivity(new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 6. Alarm
        if (cmd.contains("अलार्म") || cmd.contains("घड़ी") || cmd.contains("clock")) {
            speakOut("घड़ी खोल रहा हूँ।");
            startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 7. Time & Date
        if (cmd.contains("टाइम") || cmd.contains("समय") || cmd.contains("कितने बजे")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            speakOut("अभी समय हुआ है " + time);
            return;
        }
        if (cmd.contains("तारीख") || cmd.contains("डेट")) {
            String date = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
            speakOut("आज की तारीख है " + date);
            return;
        }

        // 8. Direct Apps (PhonePe, Paytm, Insta, FB, etc.)
        if (cmd.contains("फोन पे") || cmd.contains("phonepe")) { speakOut("फ़ोन पे खोल रहा हूँ।"); launchByPackage("com.phonepe.app"); return; }
        if (cmd.contains("पेटीएम") || cmd.contains("paytm")) { speakOut("पेटीएम खोल रहा हूँ।"); launchByPackage("net.one97.paytm"); return; }
        if (cmd.contains("गूगल पे") || cmd.contains("gpay")) { speakOut("गूगल पे खोल रहा हूँ।"); launchByPackage("com.google.android.apps.nbu.paisa.user"); return; }
        if (cmd.contains("इंस्टाग्राम") || cmd.contains("instagram")) { speakOut("इंस्टाग्राम खोल रहा हूँ।"); launchByPackage("com.instagram.android"); return; }
        if (cmd.contains("फेसबुक") || cmd.contains("facebook")) { speakOut("फेसबुक खोल रहा हूँ।"); launchByPackage("com.facebook.katana"); return; }
        if (cmd.contains("यूट्यूब") || cmd.contains("youtube") || cmd.contains("युटुब")) { speakOut("यूट्यूब खोल रहा हूँ।"); launchByPackage("com.google.android.youtube"); return; }
        if (cmd.contains("टर्मक्स") || cmd.contains("termux")) { speakOut("टर्मक्स खोल रहा हूँ।"); launchByPackage("com.termux"); return; }

        // 9. Phone ke baaki sabhi apps
        String clean = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|लगाओ|open|kholo|chalao|app|ऐप|दिखाओ|चालू)", "").trim();
        if (launchUniversalApp(clean)) return;

        speakOut(rawCmd + " समझ नहीं आया, सर।");
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
                    speakOut(label + " खोल रहा हूँ, सर।");
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

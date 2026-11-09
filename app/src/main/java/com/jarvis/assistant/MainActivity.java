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

    private static final Map<String, String> DIRECT_APP_MAP = new HashMap<>();
    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // Finance & UPI
        DIRECT_APP_MAP.put("phonepe", "com.phonepe.app");
        DIRECT_APP_MAP.put("gpay", "com.google.android.apps.nbu.paisa.user");
        DIRECT_APP_MAP.put("paytm", "net.one97.paytm");
        DIRECT_APP_MAP.put("jiofinance", "com.jio.myjio");
        DIRECT_APP_MAP.put("snapmint", "com.snapmint.app");
        DIRECT_APP_MAP.put("razorpay", "com.razorpay.payments.app");
        DIRECT_APP_MAP.put("khatabook", "com.kyip.khatabook");
        DIRECT_APP_MAP.put("mpokket", "com.mpokket.app");

        // Entertainment
        DIRECT_APP_MAP.put("mxplayer", "com.mxtech.videoplayer.ad");
        DIRECT_APP_MAP.put("youtube", "com.google.android.youtube");
        DIRECT_APP_MAP.put("playit", "videoplayer.videodownloader.downloader");
        DIRECT_APP_MAP.put("vidmate", "com.nemo.vidmate");
        DIRECT_APP_MAP.put("crex", "cricket.scores.live.line");
        DIRECT_APP_MAP.put("jiohotstar", "in.startv.hotstar");
        DIRECT_APP_MAP.put("hotstar", "in.startv.hotstar");
        DIRECT_APP_MAP.put("jiosaavn", "com.jio.media.jiobeats");
        DIRECT_APP_MAP.put("sonyliv", "com.sonyliv");
        DIRECT_APP_MAP.put("ytmusic", "com.google.android.apps.youtube.music");
        DIRECT_APP_MAP.put("googletv", "com.google.android.videos");
        DIRECT_APP_MAP.put("ytstudio", "com.google.android.apps.youtube.creator");

        // Travel
        DIRECT_APP_MAP.put("whereismytrain", "com.whereismytrain.android");
        DIRECT_APP_MAP.put("confirmtkt", "com.confirmtkt.lite");
        DIRECT_APP_MAP.put("maps", "com.google.android.apps.maps");
        DIRECT_APP_MAP.put("railone", "com.cris.cfind");
        DIRECT_APP_MAP.put("flightradar24", "com.flightradar24free");
        DIRECT_APP_MAP.put("irctc", "cris.org.in.prs.ima");
        DIRECT_APP_MAP.put("railmadad", "com.cris.railmadad");
        DIRECT_APP_MAP.put("uber", "com.ubercab");
        DIRECT_APP_MAP.put("rapido", "com.rapido.passenger");

        // Shopping & Food
        DIRECT_APP_MAP.put("flipkart", "com.flipkart.android");
        DIRECT_APP_MAP.put("amazon", "in.amazon.mShop.android.shopping");
        DIRECT_APP_MAP.put("blinkit", "com.grofers.customerapp");
        DIRECT_APP_MAP.put("zomato", "com.application.zomato");
        DIRECT_APP_MAP.put("bigbasket", "com.bigbasket.mobileapp");
        DIRECT_APP_MAP.put("swiggy", "in.swiggy.android");
        DIRECT_APP_MAP.put("olx", "com.olx.southasia");
        DIRECT_APP_MAP.put("cashify", "in.cashify");

        // Social & Messaging
        DIRECT_APP_MAP.put("whatsapp", "com.whatsapp");
        DIRECT_APP_MAP.put("instagram", "com.instagram.android");
        DIRECT_APP_MAP.put("facebook", "com.facebook.katana");
        DIRECT_APP_MAP.put("telegram", "org.telegram.messenger");
        DIRECT_APP_MAP.put("snapchat", "com.snapchat.android");
        DIRECT_APP_MAP.put("gmail", "com.google.android.gm");
        DIRECT_APP_MAP.put("x", "com.twitter.android");
        DIRECT_APP_MAP.put("twitter", "com.twitter.android");
        DIRECT_APP_MAP.put("discord", "com.discord");
        DIRECT_APP_MAP.put("imo", "com.imo.android.imoim");
        DIRECT_APP_MAP.put("messenger", "com.facebook.orca");

        // Tools & Daily Apps
        DIRECT_APP_MAP.put("termux", "com.termux");
        DIRECT_APP_MAP.put("vn", "com.frontrow.vlog");
        DIRECT_APP_MAP.put("gemini", "com.google.android.apps.bard");
        DIRECT_APP_MAP.put("chatgpt", "com.openai.chatgpt");
        DIRECT_APP_MAP.put("playstore", "com.android.vending");
        DIRECT_APP_MAP.put("chrome", "com.android.chrome");
        DIRECT_APP_MAP.put("myjio", "com.jio.myjio");
        DIRECT_APP_MAP.put("calculator", "com.google.android.calculator");
        DIRECT_APP_MAP.put("clock", "com.google.android.deskclock");
        DIRECT_APP_MAP.put("zapya", "com.dewmobile.kuaiya.play");
        DIRECT_APP_MAP.put("zarchiver", "ru.zdevs.zarchiver");

        // Aliases (Hindi & Phonetic mapping)
        ALIAS_MAP.put("फोन पे", "phonepe");
        ALIAS_MAP.put("फोनपे", "phonepe");
        ALIAS_MAP.put("पेटीएम", "paytm");
        ALIAS_MAP.put("गूगल पे", "gpay");
        ALIAS_MAP.put("गूगलपे", "gpay");
        ALIAS_MAP.put("जीपे", "gpay");
        ALIAS_MAP.put("व्हाट्सएप", "whatsapp");
        ALIAS_MAP.put("इंस्टाग्राम", "instagram");
        ALIAS_MAP.put("इंस्टा", "instagram");
        ALIAS_MAP.put("फेसबुक", "facebook");
        ALIAS_MAP.put("यूट्यूब", "youtube");
        ALIAS_MAP.put("युटुब", "youtube");
        ALIAS_MAP.put("हॉटस्टार", "jiohotstar");
        ALIAS_MAP.put("जियो हॉटस्टार", "jiohotstar");
        ALIAS_MAP.put("जियो सावन", "jiosaavn");
        ALIAS_MAP.put("सोनी लिव", "sonyliv");
        ALIAS_MAP.put("एमएक्स प्लेयर", "mxplayer");
        ALIAS_MAP.put("एमएक्स", "mxplayer");
        ALIAS_MAP.put("अमेजन", "amazon");
        ALIAS_MAP.put("अमेज़न", "amazon");
        ALIAS_MAP.put("फ्लिपकार्ट", "flipkart");
        ALIAS_MAP.put("जोमैटो", "zomato");
        ALIAS_MAP.put("ज़ोमैटो", "zomato");
        ALIAS_MAP.put("स्विगी", "swiggy");
        ALIAS_MAP.put("ब्लिंकिट", "blinkit");
        ALIAS_MAP.put("रैपिडो", "rapido");
        ALIAS_MAP.put("उबर", "uber");
        ALIAS_MAP.put("ट्रेन", "whereismytrain");
        ALIAS_MAP.put("कन्फर्म टिकट", "confirmtkt");
        ALIAS_MAP.put("मैप", "maps");
        ALIAS_MAP.put("नक्शा", "maps");
        ALIAS_MAP.put("टेलीग्राम", "telegram");
        ALIAS_MAP.put("स्नैपचैट", "snapchat");
        ALIAS_MAP.put("टर्मक्स", "termux");
        ALIAS_MAP.put("कैलकुलेटर", "calculator");
        ALIAS_MAP.put("घड़ी", "clock");
        ALIAS_MAP.put("अलार्म", "clock");
        ALIAS_MAP.put("कैमरा", "camera");
        ALIAS_MAP.put("गैलरी", "gallery");
        ALIAS_MAP.put("फोटो", "gallery");
        ALIAS_MAP.put("फ़ाइल", "file");
        ALIAS_MAP.put("फाइल", "file");
        ALIAS_MAP.put("मैसेज", "message");
        ALIAS_MAP.put("कांटेक्ट", "contact");
        ALIAS_MAP.put("सेटिंग", "setting");
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
            @Override public void onEndOfSpeech() { statusText.setText("खोल रहा हूँ..."); }
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

        // 1. बातचीत
        if (cmd.contains("हेलो") || cmd.contains("hello") || cmd.contains("हाय") || cmd.equals("जार्विस")) {
            speakOut("नमस्ते साहिल सर, बताइए क्या खोलना है?");
            return;
        }
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

        // 2. टॉर्च
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश") || cmd.contains("torch") || cmd.contains("flashlight")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            setFlashlight(turnOn);
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

        // 4. कैमरा
        if (cmd.contains("कैमरा") || cmd.contains("camera")) {
            speakOut("कैमरा खोल रहा हूँ, सर।");
            Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cameraIntent);
            return;
        }

        // 5. गैलरी / फोटो
        if (cmd.contains("गैलरी") || cmd.contains("gallery") || cmd.contains("photos") || cmd.contains("फोटो") || cmd.contains("फ़ोटो")) {
            speakOut("गैलरी खोल रहा हूँ, सर।");
            Intent galleryIntent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(galleryIntent);
            return;
        }

        // 6. अलार्म / घड़ी
        if (cmd.contains("अलार्म") || cmd.contains("घड़ी") || cmd.contains("clock") || cmd.contains("alarm")) {
            speakOut("घड़ी खोल रहा हूँ, सर।");
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alarmIntent);
            return;
        }

        // 7. मैसेज
        if (cmd.contains("मैसेज") || cmd.contains("संदेश") || cmd.contains("message") || cmd.contains("sms")) {
            speakOut("मैसेज खोल रहा हूँ, सर।");
            Intent msgIntent = new Intent(Intent.ACTION_MAIN);
            msgIntent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            msgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(msgIntent);
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

        // 9. डायरेक्ट मैप और अलियास सर्च
        for (Map.Entry<String, String> entry : ALIAS_MAP.entrySet()) {
            if (cmd.contains(entry.getKey())) {
                String targetKey = entry.getValue();
                if (DIRECT_APP_MAP.containsKey(targetKey)) {
                    speakOut(entry.getKey() + " खोल रहा हूँ, सर।");
                    if (launchByPackage(DIRECT_APP_MAP.get(targetKey))) return;
                }
            }
        }

        for (Map.Entry<String, String> entry : DIRECT_APP_MAP.entrySet()) {
            if (cmd.contains(entry.getKey())) {
                speakOut(entry.getKey() + " खोल रहा हूँ, सर।");
                if (launchByPackage(entry.getValue())) return;
            }
        }

        // 10. पूरे फ़ोन की यूनिवर्सल सिस्टम ऐप सर्च (100% Launchable Apps)
        String cleaned = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|लगाओ|open|kholo|chalao|app|ऐप|दिखाओ|चालू|दिखाइए)", "").trim();
        if (launchUniversalApp(cleaned)) {
            return;
        }

        speakOut(rawCmd + " नाम का ऐप इस फ़ोन में नहीं मिला, सर।");
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

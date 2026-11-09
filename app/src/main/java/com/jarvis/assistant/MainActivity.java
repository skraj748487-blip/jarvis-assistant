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
    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // Finance & UPI
        DIRECT_APP_MAP.put("phonepe", "com.phonepe.app");
        DIRECT_APP_MAP.put("gpay", "com.google.android.apps.nbu.paisa.user");
        DIRECT_APP_MAP.put("paytm", "net.one97.paytm");
        DIRECT_APP_MAP.put("jiofinance", "com.jio.myjio");
        DIRECT_APP_MAP.put("snapmint", "com.snapmint.app");
        DIRECT_APP_MAP.put("khatabook", "com.kyip.khatabook");

        // Entertainment & Media
        DIRECT_APP_MAP.put("mxplayer", "com.mxtech.videoplayer.ad");
        DIRECT_APP_MAP.put("youtube", "com.google.android.youtube");
        DIRECT_APP_MAP.put("playit", "videoplayer.videodownloader.downloader");
        DIRECT_APP_MAP.put("vidmate", "com.nemo.vidmate");
        DIRECT_APP_MAP.put("hotstar", "in.startv.hotstar");
        DIRECT_APP_MAP.put("jiosaavn", "com.jio.media.jiobeats");
        DIRECT_APP_MAP.put("sonyliv", "com.sonyliv");
        DIRECT_APP_MAP.put("ytmusic", "com.google.android.apps.youtube.music");

        // Social & Travel
        DIRECT_APP_MAP.put("whatsapp", "com.whatsapp");
        DIRECT_APP_MAP.put("instagram", "com.instagram.android");
        DIRECT_APP_MAP.put("facebook", "com.facebook.katana");
        DIRECT_APP_MAP.put("telegram", "org.telegram.messenger");
        DIRECT_APP_MAP.put("snapchat", "com.snapchat.android");
        DIRECT_APP_MAP.put("termux", "com.termux");
        DIRECT_APP_MAP.put("whereismytrain", "com.whereismytrain.android");
        DIRECT_APP_MAP.put("maps", "com.google.android.apps.maps");

        // Aliases
        ALIAS_MAP.put("फोन पे", "phonepe");
        ALIAS_MAP.put("पेटीएम", "paytm");
        ALIAS_MAP.put("गूगल पे", "gpay");
        ALIAS_MAP.put("व्हाट्सएप", "whatsapp");
        ALIAS_MAP.put("इंस्टाग्राम", "instagram");
        ALIAS_MAP.put("फेसबुक", "facebook");
        ALIAS_MAP.put("यूट्यूब", "youtube");
        ALIAS_MAP.put("युटुब", "youtube");
        ALIAS_MAP.put("हॉटस्टार", "hotstar");
        ALIAS_MAP.put("ट्रेन", "whereismytrain");
        ALIAS_MAP.put("मैप", "maps");
        ALIAS_MAP.put("टर्मक्स", "termux");
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
            @Override public void onEndOfSpeech() { statusText.setText("काम कर रहा हूँ..."); }
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

        // 1. YouTube Song - डायरेक्ट सर्च और प्ले इंटेंट
        if (cmd.contains("गाना") || cmd.contains("song") || cmd.contains("बजाओ") || cmd.contains("चलाओ") || cmd.contains("play")) {
            String song = cmd.replaceAll("(युटुब ओपन करके|यूट्यूब ओपन करके|युटुब खोल के|यूट्यूब खोल के|यूट्यूब पर|यूट्यूब में|युटुब|यूट्यूब|youtube|गाना बजाओ|गाना चलाओ|गाना लगाओ|गाना|बजाओ|चलाओ|लगाओ|ओपन करके|खोल के|ओपन करो|खोलो|play|song)", "").trim();
            if (song.isEmpty()) song = "new song";
            speakOut(song + " बजा रहा हूँ, सर।");

            try {
                Intent ytIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(song, "UTF-8")));
                ytIntent.setPackage("com.google.android.youtube");
                ytIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ytIntent);
            } catch (Exception e) {
                Intent webYt = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(song)));
                webYt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webYt);
            }
            return;
        }

        // 2. कॉल (Android Direct Search Intent - रात वाला अचूक सिस्टम)
        if (cmd.contains("कॉल") || cmd.contains("फोन लगाओ") || cmd.contains("call") || cmd.contains("फोन करो")) {
            String name = cmd.replaceAll("(के पास कॉल करो|के पास कॉल लगाओ|को कॉल लगाओ|को कॉल करो|पर कॉल लगाओ|पर कॉल करो|कॉल लगाओ|कॉल करो|कॉल|फोन लगाओ|फोन करो|call|फोन)", "").trim();
            speakOut((name.isEmpty() ? "कॉल" : name + " को कॉल") + " लगा रहा हूँ, सर।");

            // सिस्टम डायलर जो नाम को तुरंत पहचान कर कॉल स्क्रीन पर ले जाता है
            Intent callIntent = new Intent(Intent.ACTION_VIEW);
            callIntent.setData(Uri.parse("tel:" + (name.replaceAll("[^0-9+]", "").isEmpty() ? "" : name.replaceAll("[^0-9+]", ""))));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(callIntent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_DIAL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }

        // 3. WhatsApp (Direct WhatsApp Launcher)
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

        // 4. टॉर्च / कैमरा / गैलरी / अलार्म
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            setFlashlight(turnOn);
            speakOut(turnOn ? "टॉर्च चालू कर दी।" : "टॉर्च बंद कर दी।");
            return;
        }
        if (cmd.contains("कैमरा")) {
            speakOut("कैमरा चालू कर रहा हूँ।");
            startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (cmd.contains("गैलरी") || cmd.contains("फोटो")) {
            speakOut("गैलरी खोल रहा हूँ।");
            startActivity(new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (cmd.contains("अलार्म") || cmd.contains("घड़ी")) {
            speakOut("घड़ी खोल रहा हूँ।");
            startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 5. डायरेक्ट ऐप्स
        for (Map.Entry<String, String> entry : ALIAS_MAP.entrySet()) {
            if (cmd.contains(entry.getKey())) {
                speakOut(entry.getKey() + " खोल रहा हूँ, सर।");
                launchByPackage(DIRECT_APP_MAP.get(entry.getValue()));
                return;
            }
        }

        // 6. फ़ोन के बाकी सभी ऐप्स
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

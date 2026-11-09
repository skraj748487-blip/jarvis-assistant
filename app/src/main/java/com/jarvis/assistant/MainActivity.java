package com.jarvis.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
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
        DIRECT_APP_MAP.put("phonepe", "com.phonepe.app");
        DIRECT_APP_MAP.put("gpay", "com.google.android.apps.nbu.paisa.user");
        DIRECT_APP_MAP.put("paytm", "net.one97.paytm");
        DIRECT_APP_MAP.put("whatsapp", "com.whatsapp");
        DIRECT_APP_MAP.put("instagram", "com.instagram.android");
        DIRECT_APP_MAP.put("facebook", "com.facebook.katana");
        DIRECT_APP_MAP.put("youtube", "com.google.android.youtube");
        DIRECT_APP_MAP.put("termux", "com.termux");

        ALIAS_MAP.put("फोन पे", "phonepe");
        ALIAS_MAP.put("पेटीएम", "paytm");
        ALIAS_MAP.put("गूगल पे", "gpay");
        ALIAS_MAP.put("व्हाट्सएप", "whatsapp");
        ALIAS_MAP.put("इंस्टाग्राम", "instagram");
        ALIAS_MAP.put("फेसबुक", "facebook");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        micButton = findViewById(R.id.mic_btn);

        // सभी जरूरी परमिशन मांगें
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.RECORD_AUDIO
            }, 101);
        }

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

        // 1. YouTube Auto Song Play (295, अरिजीत, सिद्धू आदि)
        if (cmd.contains("गाना") || cmd.contains("song") || cmd.contains("बजाओ") || cmd.contains("चलाओ") || cmd.contains("play")) {
            String song = cmd.replaceAll("(युटुब ओपन करके|यूट्यूब ओपन करके|युटुब खोल के|यूट्यूब खोल के|यूट्यूब पर|यूट्यूब में|युटुब|यूट्यूब|youtube|गाना बजाओ|गाना चलाओ|गाना लगाओ|गाना|बजाओ|चलाओ|लगाओ|ओपन करके|खोल के|ओपन करो|खोलो|play|song)", "").trim();
            if (song.isEmpty()) song = "trending song";

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

        // 2. Call by Name ("तौसीफ आलम के पास कॉल करो", "आलम को कॉल लगाओ")
        if (cmd.contains("कॉल") || cmd.contains("फोन लगाओ") || cmd.contains("call") || cmd.contains("फोन करो")) {
            String name = cmd.replaceAll("(के पास कॉल करो|के पास कॉल लगाओ|को कॉल लगाओ|को कॉल करो|पर कॉल लगाओ|पर कॉल करो|कॉल लगाओ|कॉल करो|कॉल|फोन लगाओ|फोन करो|call|फोन)", "").trim();
            if (name.contains("के पास")) {
                name = name.replace("के पास", "").trim();
            }
            if (name.contains("को")) {
                name = name.replace("को", "").trim();
            }
            makeCall(name);
            return;
        }

        // 3. WhatsApp Message
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            if (cmd.contains("मैसेज") || cmd.contains("भेजो") || cmd.contains("लिखो") || cmd.contains("करो")) {
                sendWhatsApp(cmd);
                return;
            }
            speakOut("व्हाट्सएप खोल रहा हूँ, सर।");
            launchByPackage("com.whatsapp");
            return;
        }

        // 4. टॉर्च
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            setFlashlight(turnOn);
            speakOut(turnOn ? "टॉर्च चालू कर दी।" : "टॉर्च बंद कर दी।");
            return;
        }

        // 5. कैमरा
        if (cmd.contains("कैमरा")) {
            speakOut("कैमरा चालू कर रहा हूँ।");
            startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 6. डायरेक्ट ऐप्स
        for (Map.Entry<String, String> entry : ALIAS_MAP.entrySet()) {
            if (cmd.contains(entry.getKey())) {
                speakOut(entry.getKey() + " खोल रहा हूँ, सर।");
                launchByPackage(DIRECT_APP_MAP.get(entry.getValue()));
                return;
            }
        }

        // 7. यूनिवर्सल ऐप्स
        String clean = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|लगाओ|open|kholo|chalao|app|ऐप|दिखाओ|चालू)", "").trim();
        if (launchUniversalApp(clean)) return;

        speakOut(rawCmd + " समझ नहीं आया, सर।");
    }

    private void makeCall(String targetName) {
        if (targetName.isEmpty()) {
            startActivity(new Intent(Intent.ACTION_DIAL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        String number = getContactNumber(targetName);
        if (number != null && !number.isEmpty()) {
            speakOut(targetName + " को कॉल लगा रहा हूँ, सर।");
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } else {
            speakOut(targetName + " का नंबर नहीं मिला, डायलर खोल रहा हूँ।");
            startActivity(new Intent(Intent.ACTION_DIAL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    private void sendWhatsApp(String cmd) {
        String clean = cmd.replaceAll("(व्हाट्सएप खोल के|व्हाट्सएप पर|व्हाट्सएप में|व्हाट्सएप|whatsapp)", "").trim();
        String name = "";
        String msg = "नमस्ते";

        if (clean.contains("के पास")) {
            int idx = clean.indexOf("के पास");
            name = clean.substring(0, idx).trim();
            msg = clean.substring(idx + 6).replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        } else if (clean.contains("को")) {
            int idx = clean.indexOf("को");
            name = clean.substring(0, idx).trim();
            msg = clean.substring(idx + 2).replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        } else {
            msg = clean.replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        }

        if (msg.isEmpty()) msg = "नमस्ते";

        String number = getContactNumber(name);
        if (number != null && !number.isEmpty()) {
            speakOut(name + " के व्हाट्सएप पर मैसेज भेज रहा हूँ।");
            String cleanNum = number.replaceAll("[^0-9]", "");
            if (cleanNum.length() == 10) cleanNum = "91" + cleanNum;

            Intent wa = new Intent(Intent.ACTION_VIEW);
            wa.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanNum + "&text=" + Uri.encode(msg)));
            wa.setPackage("com.whatsapp");
            wa.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(wa);
            } catch (Exception e) {
                launchGeneralWhatsApp(msg);
            }
        } else {
            speakOut(name + " का नंबर नहीं मिला, व्हाट्सएप खोल रहा हूँ।");
            launchGeneralWhatsApp(msg);
        }
    }

    private void launchGeneralWhatsApp(String msg) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sendIntent);
    }

    private String getContactNumber(String searchName) {
        if (searchName == null || searchName.isEmpty()) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            String query = searchName.toLowerCase().trim();
            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String num = cursor.getString(1);
                    if (name != null) {
                        String lowerName = name.toLowerCase();
                        if (lowerName.contains(query) || query.contains(lowerName)) {
                            cursor.close();
                            return num;
                        }
                    }
                }
                cursor.close();
            }

            // अगर पूरा नाम न मिले तो नाम के अलग-अलग शब्दों (जैसे सिर्फ "तौसीफ" या सिर्फ "आलम") से मिलाएँ
            String[] words = query.split("\\s+");
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String num = cursor.getString(1);
                    if (name != null) {
                        String lowerName = name.toLowerCase();
                        for (String w : words) {
                            if (w.length() >= 3 && lowerName.contains(w)) {
                                cursor.close();
                                return num;
                            }
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {}
        return null;
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

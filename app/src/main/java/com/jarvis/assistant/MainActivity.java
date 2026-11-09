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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URLEncoder;
import java.util.ArrayList;
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
        DIRECT_APP_MAP.put("termux", "com.termux");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        micButton = findViewById(R.id.mic_btn);

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

        // 1. CALL ACTION (तौसीफ आलम को कॉल करो, कॉल लगाओ)
        if (cmd.contains("कॉल") || cmd.contains("call") || cmd.contains("फोन लगाओ") || cmd.contains("फोन करो")) {
            String name = cmd.replaceAll("(के पास कॉल करो|के पास कॉल लगाओ|को कॉल लगाओ|को कॉल करो|पर कॉल लगाओ|पर कॉल करो|कॉल लगाओ|कॉल करो|कॉल|फोन लगाओ|फोन करो|call|फोन)", "").trim();
            if (name.contains("के पास")) name = name.replace("के पास", "").trim();
            if (name.contains("को")) name = name.replace("को", "").trim();

            makeDirectCall(name);
            return;
        }

        // 2. YOUTUBE SONG ACTION (295 गाना बजाओ, गाना चलाओ)
        if (cmd.contains("गाना") || cmd.contains("song") || cmd.contains("बजाओ") || cmd.contains("चलाओ") || cmd.contains("play")) {
            String song = cmd.replaceAll("(युटुब ओपन करके|यूट्यूब ओपन करके|युटुब खोल के|यूट्यूब खोल के|यूट्यूब पर|यूट्यूब में|युटुब|यूट्यूब|youtube|गाना बजाओ|गाना चलाओ|गाना लगाओ|गाना|बजाओ|चलाओ|लगाओ|ओपन करके|खोल के|ओपन करो|खोलो|play|song)", "").trim();
            if (song.isEmpty()) song = "295";

            speakOut(song + " बजा रहा हूँ, सर।");
            Intent ytIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(song)));
            ytIntent.setPackage("com.google.android.youtube");
            ytIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(ytIntent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(song))).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }

        // 3. WHATSAPP ACTION (व्हाट्सएप पर मैसेज करो)
        if (cmd.contains("व्हाट्सएप") || cmd.contains("whatsapp")) {
            sendWhatsAppMsg(cmd);
            return;
        }

        // 4. Torch / Camera / Gallery
        if (cmd.contains("टॉर्च") || cmd.contains("फ्लैश") || cmd.contains("torch")) {
            boolean turnOn = !cmd.contains("बंद") && !cmd.contains("off");
            setFlashlight(turnOn);
            speakOut(turnOn ? "टॉर्च चालू कर दी।" : "टॉर्च बंद कर दी।");
            return;
        }
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
        if (cmd.contains("अलार्म") || cmd.contains("घड़ी") || cmd.contains("clock")) {
            speakOut("घड़ी खोल रहा हूँ।");
            startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 5. Direct Daily Apps
        if (cmd.contains("फोन पे") || cmd.contains("phonepe")) { speakOut("फ़ोन पे खोल रहा हूँ।"); launchByPackage("com.phonepe.app"); return; }
        if (cmd.contains("पेटीएम") || cmd.contains("paytm")) { speakOut("पेटीएम खोल रहा हूँ।"); launchByPackage("net.one97.paytm"); return; }
        if (cmd.contains("गूगल पे") || cmd.contains("gpay")) { speakOut("गूगल पे खोल रहा हूँ।"); launchByPackage("com.google.android.apps.nbu.paisa.user"); return; }
        if (cmd.contains("इंस्टाग्राम") || cmd.contains("instagram")) { speakOut("इंस्टाग्राम खोल रहा हूँ।"); launchByPackage("com.instagram.android"); return; }
        if (cmd.contains("फेसबुक") || cmd.contains("facebook")) { speakOut("फेसबुक खोल रहा हूँ।"); launchByPackage("com.facebook.katana"); return; }
        if (cmd.contains("यूट्यूब") || cmd.contains("youtube") || cmd.contains("युटुब")) { speakOut("यूट्यूब खोल रहा हूँ।"); launchByPackage("com.google.android.youtube"); return; }
        if (cmd.contains("टर्मक्स") || cmd.contains("termux")) { speakOut("टर्मक्स खोल रहा हूँ।"); launchByPackage("com.termux"); return; }

        // 6. Universal Apps
        String clean = cmd.replaceAll("(ओपन|खोलो|चलाओ|खोलें|करो|लगाओ|open|kholo|chalao|app|ऐप|दिखाओ|चालू)", "").trim();
        if (launchUniversalApp(clean)) return;

        speakOut(rawCmd + " समझ नहीं आया, सर।");
    }

    private void makeDirectCall(String targetName) {
        String number = findContactNumber(targetName);
        if (number != null && !number.isEmpty()) {
            speakOut(targetName + " को कॉल लगा रहा हूँ।");
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } else {
            speakOut(targetName + " का नंबर खोजने के लिए डायलर खोल रहा हूँ।");
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialIntent);
        }
    }

    private void sendWhatsAppMsg(String cmd) {
        String clean = cmd.replaceAll("(व्हाट्सएप खोल के|व्हाट्सएप पर|व्हाट्सएप में|व्हाट्सएप|whatsapp)", "").trim();
        String name = "";
        String msg = "नमस्ते";

        if (clean.contains("को")) {
            int idx = clean.indexOf("को");
            name = clean.substring(0, idx).trim();
            msg = clean.substring(idx + 2).replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        } else if (clean.contains("के पास")) {
            int idx = clean.indexOf("के पास");
            name = clean.substring(0, idx).trim();
            msg = clean.substring(idx + 6).replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        } else {
            msg = clean.replaceAll("(मैसेज करो|मैसेज भेजो|भेजो|करो|लिखो|मैसेज)", "").trim();
        }

        if (msg.isEmpty()) msg = "नमस्ते";

        String number = findContactNumber(name);
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
                launchWhatsAppFallback(msg);
            }
        } else {
            speakOut("व्हाट्सएप खोल रहा हूँ।");
            launchWhatsAppFallback(msg);
        }
    }

    private void launchWhatsAppFallback(String msg) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(sendIntent);
        } catch (Exception e) {
            launchByPackage("com.whatsapp");
        }
    }

    private String findContactNumber(String searchName) {
        if (searchName == null || searchName.trim().isEmpty()) return null;
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
                        String lower = name.toLowerCase();
                        if (lower.contains(query) || query.contains(lower)) {
                            cursor.close();
                            return num;
                        }
                    }
                }
                cursor.close();
            }

            // Word match fallback
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
                        String lower = name.toLowerCase();
                        for (String w : words) {
                            if (w.length() >= 3 && lower.contains(w)) {
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

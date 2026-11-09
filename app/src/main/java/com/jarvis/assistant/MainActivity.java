package com.jarvis.assistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URLEncoder;
import java.util.ArrayList;
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
            @Override public void onError(int error) { statusText.setText("आवाज़ समझ नहीं आई, दोबारा दबाएँ।"); }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handleYouTubeOnly(matches.get(0));
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

    private void handleYouTubeOnly(String rawCmd) {
        statusText.setText("\"" + rawCmd + "\"");
        String cmd = rawCmd.toLowerCase().trim();

        // Agar YouTube ya Gana bola gaya hai
        if (cmd.contains("गाना") || cmd.contains("song") || cmd.contains("बजाओ") || cmd.contains("चलाओ") || cmd.contains("युटुब") || cmd.contains("यूट्यूब") || cmd.contains("youtube") || cmd.contains("play")) {
            
            // Gaane ka naam alag nikalna
            String songName = cmd.replaceAll("(युटुब ओपन करके|यूट्यूब ओपन करके|युटुब खोल के|यूट्यूब खोल के|यूट्यूब पर|यूट्यूब में|युटुब|यूट्यूब|youtube|गाना बजाओ|गाना चलाओ|गाना लगाओ|गाना|बजाओ|चलाओ|लगाओ|ओपन करके|खोल के|ओपन करो|खोलो|play|song)", "").trim();
            
            if (songName.isEmpty()) {
                songName = "trending song";
            }

            speakOut(songName + " बजा रहा हूँ, सर।");

            // Direct YouTube Intent Jo 100% open karega
            try {
                Intent ytIntent = new Intent(Intent.ACTION_VIEW);
                ytIntent.setData(Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(songName, "UTF-8")));
                ytIntent.setPackage("com.google.android.youtube");
                ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ytIntent);
            } catch (Exception e) {
                // Agar app me problem ho to direct browser/YouTube open kare
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + URLEncoder.encode(songName, "UTF-8")));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            }
            return;
        }

        speakOut("सर, अभी सिर्फ यूट्यूब टेस्ट कर रहे हैं। कोई गाना बोलिए।");
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

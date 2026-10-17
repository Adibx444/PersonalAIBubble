package com.adib.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    LinearLayout chatLayout;
    EditText input;
    SpeechRecognizer speechRecognizer;

    int bg = Color.rgb(18, 23, 35);
    int panel = Color.rgb(31, 39, 55);
    int accent = Color.rgb(92, 120, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 16);
        root.setBackgroundColor(bg);

        TextView title = new TextView(this);
        title.setText("Personal AI Assistant");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 6);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Bangla & English Voice Assistant");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 20);
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(4, 8, 4, 8);
        scroll.addView(chatLayout);

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        addMessage("Assistant",
                "Hello! Voice button-e chap diye bolo: YouTube kholo ba shomoy bolo.");

        TextView quickTitle = new TextView(this);
        quickTitle.setText("Quick Commands");
        quickTitle.setTextColor(Color.WHITE);
        quickTitle.setTextSize(15);
        quickTitle.setPadding(0, 12, 0, 8);
        root.addView(quickTitle);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        addQuickButton(quickRow, "Help");
        addQuickButton(quickRow, "Time");
        addQuickButton(quickRow, "YouTube");
        root.addView(quickRow);

        LinearLayout sendRow = new LinearLayout(this);
        sendRow.setOrientation(LinearLayout.HORIZONTAL);
        sendRow.setPadding(0, 12, 0, 0);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Type a command...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setPadding(14, 8, 14, 8);
        input.setBackgroundColor(panel);

        sendRow.addView(input, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button send = new Button(this);
        send.setText("Send");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(accent));
        send.setOnClickListener(v -> sendCommand());
        sendRow.addView(send);

        Button mic = new Button(this);
        mic.setText("🎙️");
        mic.setTextSize(18);
        mic.setOnClickListener(v -> startVoiceInput());
        sendRow.addView(mic);

        root.addView(sendRow);
        setContentView(root);
    }

    void addQuickButton(LinearLayout row, String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setOnClickListener(v -> {
            input.setText(text);
            sendCommand();
        });
        row.addView(button, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }

    void addMessage(String sender, String message) {
        TextView text = new TextView(this);
        text.setText(sender + ":\n" + message);
        text.setTextColor(Color.WHITE);
        text.setTextSize(15);
        text.setPadding(14, 12, 14, 12);
        text.setBackgroundColor(panel);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);
        chatLayout.addView(text, params);
    }

    void startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addMessage("Assistant",
                    "Speech recognition ei device-e available nei.");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                addMessage("Assistant", "Shunchi... Ekhon bolo.");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                addMessage("Assistant",
                        "Kotha bujhte parini. Abar microphone chap diye bolo.");
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    input.setText(matches.get(0));
                    sendCommand();
                }

                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                    speechRecognizer = null;
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Command bolo / Speak a command");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            addMessage("Assistant", "Voice input start kora jayni.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else if (requestCode == 101) {
            Toast.makeText(this,
                    "Voice command-er jonno microphone permission lagbe.",
                    Toast.LENGTH_LONG).show();
        }
    }

    void sendCommand() {
        String command = input.getText().toString().trim();

        if (command.isEmpty()) {
            Toast.makeText(this, "Command lekho ba bolo",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        addMessage("You", command);
        input.setText("");

        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.equals("help") || lower.contains("সাহায্য")) {
            addMessage("Assistant",
                    "Test command: Help, Time, YouTube. " +
                    "Bolo: YouTube kholo ba shomoy bolo.");
        } else if (lower.equals("time")
                || lower.contains("সময়")
                || lower.contains("সময়")
                || lower.contains("shomoy")
                || lower.contains("somoy")
                || lower.contains("কটা বাজে")) {
            addMessage("Assistant", "Current time: " +
                    new java.text.SimpleDateFormat("hh:mm a",
                    Locale.getDefault()).format(new java.util.Date()));
        } else if (lower.contains("youtube")
                || lower.contains("ইউটিউب")
                || lower.contains("ইউটিউব")) {
            openYouTube();
        } else if (lower.contains("facebook")
                || lower.contains("ফেসবুক")) {
            openApp("com.facebook.katana", "https://www.facebook.com", "Facebook");
        } else if (lower.contains("whatsapp")
                || lower.contains("হোয়াটসঅ্যাপ")
                || lower.contains("হোয়াটসঅ্যাপ")
                || lower.contains("হোয়াটস অ্যাপ")) {
            openApp("com.whatsapp", "https://www.whatsapp.com", "WhatsApp");
        } else {
            addMessage("Assistant",
                    "Command peyechi, kintu ei command-er kaj ekhono jog hoyni.");
        }
    }

    void openApp(String packageName, String url, String appName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                addMessage("Assistant", appName + " open kora hocche.");
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                addMessage("Assistant", "Browser-e " + appName + " khola hocche.");
            }
        } catch (Exception e) {
            addMessage("Assistant", appName + " khola jayni.");
        }
    }

    void openYouTube() {
        try {
            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage("com.google.android.youtube");

            if (intent != null) {
                startActivity(intent);
                addMessage("Assistant", "YouTube open kora hocche.");
            } else {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com"));
                startActivity(intent);
                addMessage("Assistant", "Browser-e YouTube khola hocche.");
            }
        } catch (Exception e) {
            addMessage("Assistant", "YouTube khola jayni.");
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }
}

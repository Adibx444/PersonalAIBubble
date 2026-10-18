package com.adib.assistant;

import android.app.Service;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BubbleService extends Service {

    private WindowManager wm;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private SpeechRecognizer alwaysRecognizer;
    private boolean alwaysListening = false;
    private boolean restartingListening = false;

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow Display over other apps first.",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    private int windowType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams makeParams(int width, int height) {
        return new WindowManager.LayoutParams(
                width, height, windowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    private void createBubble() {
        bubble = new TextView(this);
        bubble.setText("AI");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(17);
        bubble.setTypeface(null, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(Color.rgb(92, 120, 255));
        bubble.setElevation(dp(8));

        bubbleParams = makeParams(dp(58), dp(58));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(20);
        bubbleParams.y = dp(180);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;
            int ix, iy;
            boolean moved;
            boolean longPressed;
            final android.os.Handler handler =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            final Runnable longPressAction = new Runnable() {
                @Override
                public void run() {
                    longPressed = true;
                    togglePanel();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX();
                        sy = e.getRawY();
                        ix = bubbleParams.x;
                        iy = bubbleParams.y;
                        moved = false;
                        longPressed = false;
                        handler.postDelayed(longPressAction,
                                android.view.ViewConfiguration.getLongPressTimeout());
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - sx;
                        float dy = e.getRawY() - sy;
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) {
                            moved = true;
                            handler.removeCallbacks(longPressAction);
                        }
                        bubbleParams.x = ix + (int) dx;
                        bubbleParams.y = iy + (int) dy;
                        try {
                            wm.updateViewLayout(bubble, bubbleParams);
                        } catch (Exception ignored) {}
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPressAction);
                        if (e.getAction() == MotionEvent.ACTION_UP
                                && !moved && !longPressed) {
                            toggleListening();
                        }
                        return true;
                }
                return true;
            }
        });

        try {
            wm.addView(bubble, bubbleParams);
        } catch (Exception e) {
            Toast.makeText(this, "Bubble start failed.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }


    private void toggleListening() {
        if (alwaysListening) stopAlwaysListening();
        else startAlwaysListening();
    }

    private void startAlwaysListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Open the app and grant microphone permission first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition unavailable.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        alwaysListening = true;
        bubble.setText("ON");
        bubble.setBackgroundColor(Color.rgb(30, 170, 75));
        Toast.makeText(this, "Voice listening ON.", Toast.LENGTH_SHORT).show();
        startNextListening();
    }

    private void stopAlwaysListening() {
        alwaysListening = false;
        restartingListening = false;
        if (alwaysRecognizer != null) {
            try {
                alwaysRecognizer.cancel();
                alwaysRecognizer.destroy();
            } catch (Exception ignored) {}
            alwaysRecognizer = null;
        }
        if (bubble != null) {
            bubble.setText("AI");
            bubble.setBackgroundColor(Color.rgb(92, 120, 255));
        }
    }

    private void startNextListening() {
        if (!alwaysListening || restartingListening) return;
        restartingListening = true;

        if (alwaysRecognizer != null) {
            try { alwaysRecognizer.destroy(); } catch (Exception ignored) {}
        }
        alwaysRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final SpeechRecognizer current = alwaysRecognizer;

        current.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                restartingListening = false;
            }

            @Override public void onResults(Bundle results) {
                restartingListening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (alwaysListening && matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0).trim();
                    if (!spoken.isEmpty()) {
                        Intent open = new Intent(BubbleService.this, MainActivity.class);
                        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        open.putExtra("bubble_command", spoken);
                        startActivity(open);
                    }
                }
                scheduleNextListening();
            }

            @Override public void onError(int error) {
                restartingListening = false;
                scheduleNextListening();
            }
            @Override public void onEndOfSpeech() {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        try {
            current.startListening(speechIntent);
        } catch (Exception e) {
            restartingListening = false;
            scheduleNextListening();
        }
    }

    private void scheduleNextListening() {
        if (!alwaysListening || bubble == null) return;
        bubble.postDelayed(() -> {
            if (alwaysListening) startNextListening();
        }, 900);
    }

    private void togglePanel() {
        if (panel != null) closePanel();
        else showPanel();
    }


    private void showPanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackgroundColor(Color.rgb(24, 30, 45));
        box.setElevation(dp(12));

        TextView title = new TextView(this);
        title.setText("Personal AI Mini Chat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setText("Personal AI Mini Chat  —  Drag here");
        title.setPadding(dp(4), dp(8), dp(4), dp(12));
        title.setBackgroundColor(Color.rgb(38, 48, 68));
        box.addView(title);

        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX;
            private int startY;
            private float downX;
            private float downY;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (panelParams == null || panel == null) return false;

                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = panelParams.x;
                        startY = panelParams.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        panelParams.x = startX
                                + (int) (event.getRawX() - downX);
                        panelParams.y = startY
                                + (int) (event.getRawY() - downY);
                        try {
                            wm.updateViewLayout(panel, panelParams);
                        } catch (Exception ignored) {}
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return true;
            }
        });

        TextView hint = new TextView(this);
        hint.setText("Try: Help, Time, YouTube, Facebook, WhatsApp");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(12);
        hint.setPadding(0, dp(6), 0, dp(8));
        box.addView(hint);

        EditText command = new EditText(this);
        command.setSingleLine(true);
        command.setHint("Type a command...");
        command.setTextColor(Color.WHITE);
        command.setHintTextColor(Color.LTGRAY);
        box.addView(command);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button send = new Button(this);
        send.setText("Send");
        row.addView(send, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button voice = new Button(this);
        voice.setText("Voice");
        row.addView(voice, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button close = new Button(this);
        close.setText("Close");
        row.addView(close, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        box.addView(row);
        panel = box;

        panelParams = makeParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        panelParams.width = dp(320);
        panelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        panelParams.y = dp(100);
        panelParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        panelParams.softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        send.setOnClickListener(v -> {
            String text = command.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Type a command first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra("bubble_command", text);
            startActivity(open);
            closePanel();
        });

        voice.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Open the app and grant microphone permission first.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this,
                        "Speech recognition is not available on this device.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            SpeechRecognizer recognizer =
                    SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Toast.makeText(BubbleService.this,
                            "Listening... Speak now.",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        command.setText(matches.get(0));
                        command.setSelection(command.length());
                    } else {
                        Toast.makeText(BubbleService.this,
                                "No speech recognized. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                    recognizer.destroy();
                }

                @Override
                public void onError(int error) {
                    Toast.makeText(BubbleService.this,
                            "Voice error code: " + error,
                            Toast.LENGTH_LONG).show();
                    recognizer.destroy();
                }

                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            Intent speechIntent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT, "বাংলায় কথা বলুন");

            try {
                recognizer.startListening(speechIntent);
            } catch (Exception e) {
                recognizer.destroy();
                Toast.makeText(BubbleService.this,
                        "Could not start voice recognition.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        close.setOnClickListener(v -> closePanel());

        try {
            wm.addView(panel, panelParams);
        } catch (Exception e) {
            panel = null;
            Toast.makeText(this, "Mini Chat could not open.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void closePanel() {
        if (wm != null && panel != null) {
            try {
                wm.removeView(panel);
            } catch (Exception ignored) {}
        }
        panel = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopAlwaysListening();
        closePanel();
        if (wm != null && bubble != null) {
            try {
                wm.removeView(bubble);
            } catch (Exception ignored) {}
        }
        bubble = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

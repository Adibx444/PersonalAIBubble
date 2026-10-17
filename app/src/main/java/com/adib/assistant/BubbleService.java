package com.adib.assistant;

import android.app.Service;
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

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX();
                        sy = e.getRawY();
                        ix = bubbleParams.x;
                        iy = bubbleParams.y;
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - sx;
                        float dy = e.getRawY() - sy;
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) {
                            moved = true;
                        }
                        bubbleParams.x = ix + (int) dx;
                        bubbleParams.y = iy + (int) dy;
                        try {
                            wm.updateViewLayout(bubble, bubbleParams);
                        } catch (Exception ignored) {}
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });

        try {
            wm.addView(bubble, bubbleParams);
        } catch (Exception e) {
            Toast.makeText(this, "Bubble start failed.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    private void togglePanel() {
        if (panel != null) {
            closePanel();
        } else {
            showPanel();
        }
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
        box.addView(title);

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
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra("bubble_voice", true);
            startActivity(open);
            closePanel();
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

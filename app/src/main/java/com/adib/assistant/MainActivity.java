package com.adib.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    LinearLayout chatLayout;
    EditText input;

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
        subtitle.setText("Your personal assistant");
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

        addMessage("Assistant", "Hello! Ami tomar assistant. Nicher box-e command lekho.");

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
        send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        send.setOnClickListener(v -> sendCommand());

        sendRow.addView(send);
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

    void sendCommand() {
        String command = input.getText().toString().trim();

        if (command.isEmpty()) {
            Toast.makeText(this, "Command lekho", Toast.LENGTH_SHORT).show();
            return;
        }

        addMessage("You", command);
        input.setText("");

        String lower = command.toLowerCase();

        if (lower.equals("help") || lower.equals("সাহায্য")) {
            addMessage("Assistant",
                    "Ekhon test command ache: Help, Time, YouTube. " +
                    "Onno command-er feature pore jog korbo.");
        } else if (lower.equals("time") || lower.equals("সময়")
                || lower.equals("সময়")) {
            addMessage("Assistant",
                    "Current time: " +
                    new java.text.SimpleDateFormat("hh:mm a",
                    java.util.Locale.getDefault()).format(new java.util.Date()));
        } else if (lower.equals("youtube")) {
            try {
                android.content.Intent intent =
                        getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                if (intent != null) {
                    startActivity(intent);
                    addMessage("Assistant", "YouTube open kora hocche.");
                } else {
                    intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com"));
                    startActivity(intent);
                    addMessage("Assistant", "Browser-e YouTube khola hocche.");
                }
            } catch (Exception e) {
                addMessage("Assistant", "YouTube khola jayni.");
            }
        } else {
            addMessage("Assistant",
                    "Command peyechi, kintu ekhono ei command-er kaj jog kora hoyni.");
        }
    }
}

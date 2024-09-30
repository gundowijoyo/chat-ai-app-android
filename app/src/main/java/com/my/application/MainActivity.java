package com.my.application;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout; 
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import android.view.Gravity;
import android.text.TextUtils;
import android.content.ClipData;
import android.content.ClipboardManager;
import androidx.core.content.ContextCompat;   
import android.widget.Toast; 
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;

public class MainActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText chatInput;
    private ScrollView chatScrollView;
    private boolean isWaitingForResponse = false;
   
    @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SharedPreferences prefs = getSharedPreferences("intro_prefs", MODE_PRIVATE);
    boolean isFirstRun = prefs.getBoolean("is_first_run", true);

    if (isFirstRun) {
        setContentView(R.layout.activity_intro);
        Button nextButton = findViewById(R.id.intro_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_first_run", false);
                editor.apply();
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                finish();
            }
        });
    } else {
        setContentView(R.layout.activity_main);

        chatInput = findViewById(R.id.chat_input);
        ImageButton sendButton = findViewById(R.id.send_button);
        chatScrollView = findViewById(R.id.chat_scroll_view);
        chatContainer = findViewById(R.id.chat_container);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = chatInput.getText().toString();

                if (!message.isEmpty()) {
                    if (isWaitingForResponse) {
                        showAlert3D(MainActivity.this, "Anda tidak bisa mengirim pesan baru sebelum AI merespons.");
                    } else {
                        addMessageToChat(message, false);
                        chatInput.setText("");
                        isWaitingForResponse = true;
                        new SendMessageToAI().execute(message);
                        
                    }
                }
            }
        });
    }
	
}

private SpannableString formatCodeBlocks(String message) {
    SpannableString spannableString = new SpannableString(message);
    int startIndex = message.indexOf("```");
    while (startIndex != -1) {
        int endIndex = message.indexOf("```", startIndex + 3);
        if (endIndex != -1) {
            // Calculate the start of the actual code (after the opening ``` and any text on the same line)
            int lineEndIndex = message.indexOf("\n", startIndex); // Find the end of the line where ``` occurs
            if (lineEndIndex == -1 || lineEndIndex > endIndex) {
                lineEndIndex = startIndex + 3; // Default if no new line or it's within the same code block
            }

            // Extract the actual code block
            String codeBlock = message.substring(lineEndIndex, endIndex);

            // Create a new string without the ``` delimiters and the unwanted text on the same line
            String newMessage = message.substring(0, startIndex) +
                    codeBlock + message.substring(endIndex + 3);

            // Update the spannableString with the new message
            spannableString = new SpannableString(newMessage);

            // Calculate the new start and end of the code block in the updated string
            int newStart = startIndex;
            int newEnd = newStart + codeBlock.length();

            // Apply background color (black) and text color (warm green) to the code block
            spannableString.setSpan(new BackgroundColorSpan(Color.BLACK),
                    newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(Color.rgb(0, 255, 127)), // Warm green color
                    newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Optionally, change font to monospace for the code block
            spannableString.setSpan(new TypefaceSpan("monospace"),
                    newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Look for the next code block
            startIndex = newMessage.indexOf("```", newEnd);
        } else {
            break;
        }
    }
    return spannableString;
}



private void addMessageToChat(String message, boolean isAIResponse) {
    TextView chatBubble = new TextView(this);

    // Check if the message contains code blocks and format them
    SpannableString formattedMessage = formatCodeBlocks(message);
    chatBubble.setText(formattedMessage);
    
    chatBubble.setPadding(16, 16, 16, 16);
    chatBubble.setTextColor(getResources().getColor(android.R.color.black));

    if (isAIResponse) {
        chatBubble.setBackgroundResource(R.drawable.ai_chat_bubble_background);
    } else {
        chatBubble.setBackgroundResource(R.drawable.chat_bubble_background);
    }

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );

    if (isAIResponse) {
        params.setMargins(20, 20, 50, 10);
        params.gravity = Gravity.START;
    } else {
        params.setMargins(50, 20, 20, 10);
        params.gravity = Gravity.END;
    }

    chatBubble.setLayoutParams(params);
    chatContainer.addView(chatBubble);

    // Add copy button if it's an AI response
    if (isAIResponse) {
        chatBubble.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AI Response", message);
                clipboard.setPrimaryClip(clip);

                // Show a simple alert dialog with "Copied!" message
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Text Copied")
                        .setMessage("The text has been copied to clipboard.")
                        .setPositiveButton("OK", null)
                        .show();

                return true;
            }
        });
    }

    chatScrollView.post(new Runnable() {
        @Override
        public void run() {
            chatScrollView.fullScroll(View.FOCUS_DOWN);
        }
    });
}



    private void showAlert3D(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class SendMessageToAI extends AsyncTask<String, Void, String> {
    @Override protected void onPreExecute() {
        super.onPreExecute();
        addMessageToChat("Ai sedang mengetik! kamu harus sabar yaaa 😊..", true); // Menambahkan pesan "AI sedang mengetik..." sebelum permintaan
    }

    @Override  protected String doInBackground(String... params) {
        String userInput = params[0];
        String apiUrl = "https://gundowijoyo.my.id/api/gpt/?text=" + userInput;
        
        String aiResponse = "";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            Scanner scanner = new Scanner(inputStream);
            scanner.useDelimiter("\\A");

            if (scanner.hasNext()) {
                String responseString = scanner.next();
                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.getString("status").equals("success")) {
                    aiResponse = jsonResponse.getString("data");
                } else {
                    aiResponse = "Error: " + jsonResponse.getString("message");
                }
            }

            scanner.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            aiResponse = "Error: Failed to get response from server.";
        }

        return aiResponse;
    }

    @Override   protected void onPostExecute(String aiResponse) { 
        // Hapus pesan "AI sedang mengetik..." jika ada
        if (chatContainer != null && chatContainer.getChildCount() > 0) {
            for (int i = chatContainer.getChildCount() - 1; i >= 0; i--) {
                View view = chatContainer.getChildAt(i);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    if (textView.getText().toString().equals("Ai sedang mengetik! kamu harus sabar yaaa 😊..")) {
                        chatContainer.removeViewAt(i);
                        break;
                    }
                }
            }
        }

        addMessageToChat(aiResponse, true); 
        isWaitingForResponse = false;
    }
}
}
 
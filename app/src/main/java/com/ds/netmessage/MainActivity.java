package com.ds.netmessage;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private EditText messageInput;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<Message> messages; // List to hold messages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageInput = findViewById(R.id.messageInput);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        ImageButton sendButton = findViewById(R.id.sendButton);

        // Initialize messages list and RecyclerView
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Connect to the server
        new Thread(() -> {
            try {
                socket = new Socket("192.168.1.7", 1574); // Replace with server's IP
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Receive messages from the server
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    runOnUiThread(() -> {
                        Message receivedMessage = new Message(finalMessage, false); // Received message
                        messages.add(receivedMessage); // Add to messages list
                        chatAdapter.notifyItemInserted(messages.size() - 1); // Notify adapter
                        chatRecyclerView.scrollToPosition(messages.size() - 1); // Scroll to the latest message
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

        // Send message to the server
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString();
            if (!message.isEmpty()) {
                new Thread(() -> {
                    try {
                        out.println(message); // Send message to the server
                        Message sentMessage = new Message(message, true); // Sent message
                        runOnUiThread(() -> {
                            messages.add(sentMessage); // Add to messages list
                            chatAdapter.notifyItemInserted(messages.size() - 1); // Notify adapter
                            chatRecyclerView.scrollToPosition(messages.size() - 1); // Scroll to the latest message
                            messageInput.setText(""); // Clear input field
                        });
                    } catch (Exception e) {
                        Log.e("MainActivity", "Failed to send message: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to send message: An unexpected error occurred", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close resources
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

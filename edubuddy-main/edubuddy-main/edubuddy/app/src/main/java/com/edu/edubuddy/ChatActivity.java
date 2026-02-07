package com.edu.edubuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private String groupName;
    private EditText messageField;
    private Button sendButton, imageButton, studyRequestButton, callVideoButton;
    private ListView chatListView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ArrayList<Map<String, String>> chatMessages;
    private ChatAdapter chatAdapter;
    private CollectionReference messagesRef;
    private ProgressDialog progressDialog;
    private static final String IMGBB_API_KEY = "c04851f51baab8c4aa5aaed7254ba462"; // Replace with your ImgBB API key

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadImageToImgBB(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        groupName = getIntent().getStringExtra("groupName");
        if (TextUtils.isEmpty(groupName)) {
            Toast.makeText(this, "Group not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set the group name in the header
        TextView groupNameTextView = findViewById(R.id.groupName);
        groupNameTextView.setText(groupName);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        messagesRef = db.collection("groups").document(groupName.trim()).collection("messages");

        messageField = findViewById(R.id.messageField);
        sendButton = findViewById(R.id.sendButton);
        imageButton = findViewById(R.id.imageButton);
        studyRequestButton = findViewById(R.id.studyRequestButton);
        callVideoButton = findViewById(R.id.callVideoButton);

        callVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create and show the dialog for custom link input
                showCustomMeetingLinkDialog();
            }
        });

        Button homeButton = findViewById(R.id.homeButton);
        Button profileButton = findViewById(R.id.profileButton);

// Set click listener for the Home button
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to HomeActivity
                Intent intent = new Intent(ChatActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

// Set click listener for the Profile button
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to ProfileActivity
                Intent intent = new Intent(ChatActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });


        chatListView = findViewById(R.id.chatListView);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("sending...");

        sendButton.setOnClickListener(v -> sendMessage());
        imageButton.setOnClickListener(v -> pickImage());
        studyRequestButton.setOnClickListener(v -> showStudyRequestDialog());

        loadMessages();
    }

    private void showCustomMeetingLinkDialog() {
        // Create a dialog using the custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom_link, null);
        builder.setView(dialogView);

        // Find the EditText and Buttons in the custom layout
        EditText customLinkInput = dialogView.findViewById(R.id.customLinkInput);
        Button sendLinkButton = dialogView.findViewById(R.id.sendLinkButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set up the Send Link button
        sendLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String customLink = customLinkInput.getText().toString().trim();
                if (TextUtils.isEmpty(customLink)) {
                    Toast.makeText(ChatActivity.this, "Please enter a valid link", Toast.LENGTH_SHORT).show();
                } else {
                    sendMeetingLinkToChat(customLink); // Send the custom link to the chat
                }
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Set up the Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void sendMeetingLinkToChat(String customLink) {
        if (currentUser == null) return;

        // Create a message with text and the custom meeting link
        String messageText = "📅 Join my meeting: " + customLink;

        // Create a map with the message data
        Map<String, Object> message = new HashMap<>();
        message.put("text", messageText);
        message.put("sender", currentUser.getEmail());
        message.put("timestamp", System.currentTimeMillis());
        message.put("type", "link");  // Mark it as a link message

        // Add the message to the Firestore collection
        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(ChatActivity.this, "Meeting link sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send meeting link", Toast.LENGTH_SHORT).show();
                });
    }



    private void sendMessage() {
        String messageText = messageField.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || currentUser == null) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("text", messageText);
        message.put("sender", currentUser.getEmail());
        message.put("timestamp", System.currentTimeMillis());
        message.put("type", "text");

        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> messageField.setText(""))
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }

    private void pickImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImageToImgBB(Uri imageUri) {
        progressDialog.show();
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int n;
                while ((n = inputStream.read(buffer)) > 0) {
                    baos.write(buffer, 0, n);
                }
                String base64Image = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new FormBody.Builder()
                        .add("key", IMGBB_API_KEY)
                        .add("image", base64Image)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JSONObject json = new JSONObject(responseBody);
                    String imageUrl = json.getJSONObject("data").getString("url");

                    runOnUiThread(() -> sendImageMessage(imageUrl));
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            } finally {
                progressDialog.dismiss();
            }
        }).start();
    }

    private void sendImageMessage(String imageUrl) {
        if (currentUser == null) return;

        Map<String, Object> message = new HashMap<>();
        message.put("imageUrl", imageUrl);
        message.put("sender", currentUser.getEmail());
        message.put("timestamp", System.currentTimeMillis());
        message.put("type", "image");

        messagesRef.add(message)
                .addOnSuccessListener(doc -> Toast.makeText(ChatActivity.this, "Image sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send image", Toast.LENGTH_SHORT).show());
    }

    private void showStudyRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Study Topic");

        final EditText topicInput = new EditText(this);
        builder.setView(topicInput);

        builder.setPositiveButton("Send Request", (dialog, which) -> {
            String topic = topicInput.getText().toString().trim();
            if (!TextUtils.isEmpty(topic)) {
                sendStudyRequest(topic);
            } else {
                Toast.makeText(this, "Topic cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendStudyRequest(String topic) {
        if (currentUser == null) return;

        String studyRequestMessage = "📚 I want to learn " + topic + ". Can anyone teach me?";

        Map<String, Object> requestMessage = new HashMap<>();
        requestMessage.put("text", studyRequestMessage);
        requestMessage.put("sender", currentUser.getEmail());
        requestMessage.put("timestamp", System.currentTimeMillis());
        requestMessage.put("type", "study_request");
        requestMessage.put("topic", topic);  // Store the topic

        messagesRef.add(requestMessage)
                .addOnSuccessListener(documentReference -> Toast.makeText(ChatActivity.this, "Study Request Sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send study request", Toast.LENGTH_SHORT).show());
    }

    private void loadMessages() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    chatMessages.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, String> message = new HashMap<>();
                        message.put("sender", doc.getString("sender"));
                        String type = doc.getString("type");

                        if ("study_request".equals(type)) {
                            message.put("type", "study_request");
                            message.put("text", doc.getString("text"));
                            message.put("topic", doc.getString("topic"));
                        } else if ("image".equals(type)) {
                            message.put("type", "image");
                            message.put("imageUrl", doc.getString("imageUrl"));
                        } else {
                            message.put("type", "text");
                            message.put("text", doc.getString("text"));
                        }

                        chatMessages.add(message);
                    }
                    chatAdapter.notifyDataSetChanged();
                });
    }
}
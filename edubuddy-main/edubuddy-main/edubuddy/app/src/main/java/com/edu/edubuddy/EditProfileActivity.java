package com.edu.edubuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editAge, editCollege, editUsername;
    private Button saveButton;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        editName = findViewById(R.id.editName);
        editAge = findViewById(R.id.editAge);
        editCollege = findViewById(R.id.editCollege);
        editUsername = findViewById(R.id.editUsername);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userId = querySnapshot.getDocuments().get(0).getId();
                        Map<String, Object> userData = querySnapshot.getDocuments().get(0).getData();

                        // Fetch existing values from Firestore
                        String existingName = userData.get("name") != null ? userData.get("name").toString() : "";
                        String existingAge = userData.get("age") != null ? userData.get("age").toString() : "";
                        String existingCollege = userData.get("college") != null ? userData.get("college").toString() : "";
                        String existingUsername = userData.get("username") != null ? userData.get("username").toString() : "";

                        // Get new values entered by the user
                        String newName = editName.getText().toString().trim();
                        String newAge = editAge.getText().toString().trim();
                        String newCollege = editCollege.getText().toString().trim();
                        String newUsername = editUsername.getText().toString().trim();

                        Map<String, Object> updates = new HashMap<>();

                        // Update only if a new value is entered and different from existing value
                        if (!newName.isEmpty() && !newName.equals(existingName)) {
                            updates.put("name", newName);
                        }
                        if (!newAge.isEmpty() && !newAge.equals(existingAge)) {
                            updates.put("age", newAge);
                        }
                        if (!newCollege.isEmpty() && !newCollege.equals(existingCollege)) {
                            updates.put("college", newCollege);
                        }
                        if (!newUsername.isEmpty() && !newUsername.equals(existingUsername)) {
                            updates.put("username", newUsername);
                        }

                        if (!updates.isEmpty()) {
                            // Only update if there are changes
                            db.collection("users").document(userId).update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("updated", true);
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Update Failed!", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "No changes detected!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show());
    }
}

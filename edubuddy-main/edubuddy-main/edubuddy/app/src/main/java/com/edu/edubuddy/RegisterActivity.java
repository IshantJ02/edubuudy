package com.edu.edubuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameField, ageField, collegeField, usernameField, emailField, passwordField;
    private Button registerButton, backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference realtimeDbRef; // Realtime Database Reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth, Firestore & Realtime Database
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        realtimeDbRef = FirebaseDatabase.getInstance().getReference("users"); // Root reference in Realtime DB

        // Bind UI elements
        nameField = findViewById(R.id.nameField);
        ageField = findViewById(R.id.ageField);
        collegeField = findViewById(R.id.collegeField);
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);

        // Set button click listeners
        registerButton.setOnClickListener(v -> registerUser());
        backButton.setOnClickListener(v -> goBack());
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String age = ageField.getText().toString().trim();
        String college = collegeField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Check if fields are empty
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(college) ||
                TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password length
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("RegisterActivity", "User registered successfully: " + user.getUid());
                            saveUserData(user.getUid(), name, age, college, username, email);
                        }
                    } else {
                        if (task.getException() != null) {
                            String errorMessage = task.getException().getMessage();
                            Log.e("RegisterActivity", "Registration failed: " + errorMessage);
                            Toast.makeText(this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserData(String userId, String name, String age, String college, String username, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("age", age);
        userMap.put("college", college);
        userMap.put("username", username);
        userMap.put("email", email);

        // Save user details in Firestore
        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d("RegisterActivity", "User data saved in Firestore"))
                .addOnFailureListener(e -> Log.e("RegisterActivity", "Error saving Firestore data: " + e.getMessage()));

        // Save user details in Realtime Database
        realtimeDbRef.child(userId).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegisterActivity", "User data saved in Realtime Database");
                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    redirectToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Error saving Realtime Database: " + e.getMessage());
                    Toast.makeText(RegisterActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToMainActivity() {
        Log.d("RegisterActivity", "Redirecting to MainActivity...");
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void goBack() {
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
    }
}

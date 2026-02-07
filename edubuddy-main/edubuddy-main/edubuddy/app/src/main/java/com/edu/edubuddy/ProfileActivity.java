package com.edu.edubuddy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String IMGBB_API_KEY = "c04851f51baab8c4aa5aaed7254ba462";

    private ImageView profileImage;
    private TextView nameText, ageText, collegeText, usernameText, emailText;
    private Button uploadImageButton, editProfileButton, logoutButton, homeButton;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId;
    private Uri imageUri;
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    profileImage.setImageURI(imageUri);
                    uploadImageToImgBB();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        profileImage = findViewById(R.id.profileImage);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        logoutButton = findViewById(R.id.logoutButton);
        homeButton = findViewById(R.id.homeButton);
        nameText = findViewById(R.id.nameText);
        ageText = findViewById(R.id.ageText);
        collegeText = findViewById(R.id.collegeText);
        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.emailText);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");

        checkUserAuthentication();

        uploadImageButton.setOnClickListener(v -> openGallery());
        editProfileButton.setOnClickListener(v -> openEditProfile());
        logoutButton.setOnClickListener(v -> logoutUser());
        homeButton.setOnClickListener(v -> goToHome());
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void checkUserAuthentication() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            loadUserProfile();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loadUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nameText.setText("Name: " + snapshot.child("name").getValue(String.class));
                    ageText.setText("Age: " + snapshot.child("age").getValue(String.class));
                    collegeText.setText("College/School: " + snapshot.child("college").getValue(String.class));
                    usernameText.setText("Username: " + snapshot.child("username").getValue(String.class));
                    emailText.setText("Email: " + snapshot.child("email").getValue(String.class));

                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(profileImageUrl).into(profileImage);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageToImgBB() {
        if (imageUri == null) return;
        progressDialog.show();

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                String base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP);

                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build();

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
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String imageUrl = jsonResponse.getJSONObject("data").getString("url");
                    runOnUiThread(() -> updateProfileImage(imageUrl));
                } else {
                    runOnUiThread(() -> showToast("Upload failed! Server error"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Upload failed!"));
                Log.e(TAG, "Error uploading image", e);
            } finally {
                progressDialog.dismiss();
            }
        }).start();
    }

    private void updateProfileImage(String imageUrl) {
        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Profile updated!");
                        Glide.with(this).load(imageUrl).into(profileImage);
                    } else {
                        showToast("Error updating profile!");
                    }
                });
    }

    private void openEditProfile() {
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Optional, only if you want to close ProfileActivity
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

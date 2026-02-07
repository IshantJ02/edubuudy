package com.edu.edubuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private EditText groupNameField;
    private Button createGroupButton;
    private ListView groupListView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<String> groupList;
    private ArrayList<String> filteredGroupList; // List to store filtered groups
    private GroupListAdapter adapter; // Custom adapter for displaying groups
    private SearchView searchGroupField; // Search view for searching groups

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI elements
        Button homeButton = findViewById(R.id.homeButton);
        Button profileButton = findViewById(R.id.profileButton);
        searchGroupField = findViewById(R.id.searchGroupField); // SearchView for groups

        // Set click listeners for the buttons
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        groupNameField = findViewById(R.id.groupNameField);
        createGroupButton = findViewById(R.id.createGroupButton);
        groupListView = findViewById(R.id.groupListView);

        groupList = new ArrayList<>();
        filteredGroupList = new ArrayList<>();
        adapter = new GroupListAdapter(this, filteredGroupList); // Use filtered list for the adapter
        groupListView.setAdapter(adapter);

        // Load groups from Firestore
        loadGroups();

        // Set click listener for creating a new group
        createGroupButton.setOnClickListener(v -> createGroup());

        // Set item click listener for group list
        groupListView.setOnItemClickListener((parent, view, position, id) -> {
            String groupName = filteredGroupList.get(position); // Use filtered list for selection
            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });

        // Set up the SearchView listener to filter groups
        searchGroupField.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterGroups(newText);
                return true;
            }
        });
    }

    private void createGroup() {
        String groupName = groupNameField.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (TextUtils.isEmpty(groupName) || user == null) {
            Toast.makeText(this, "Enter a valid group name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create group data
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", groupName);
        groupData.put("createdBy", user.getUid());

        // Store in Firestore
        db.collection("groups").add(groupData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(HomeActivity.this, "Group Created!", Toast.LENGTH_SHORT).show();
                    groupNameField.setText(""); // Clear input
                    loadGroups(); // Reload groups
                })
                .addOnFailureListener(e ->
                        Toast.makeText(HomeActivity.this, "Failed to create group", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadGroups() {
        db.collection("groups").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                groupList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String groupName = document.getString("groupName");
                    if (groupName != null) {
                        groupList.add(groupName);
                    }
                }
                // Initially, display all groups
                filteredGroupList.clear();
                filteredGroupList.addAll(groupList);
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Method to filter groups based on search query
    private void filterGroups(String query) {
        filteredGroupList.clear();

        if (query.isEmpty()) {
            filteredGroupList.addAll(groupList);
        } else {
            for (String group : groupList) {
                if (group.toLowerCase().contains(query.toLowerCase())) {
                    filteredGroupList.add(group);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}

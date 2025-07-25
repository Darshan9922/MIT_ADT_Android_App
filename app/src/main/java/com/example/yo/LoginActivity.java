package com.example.yo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Spinner;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.app.ProgressDialog;

public class LoginActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private EditText emailEditText;
    private EditText phoneEditText;
    private AutoCompleteTextView roleDropdown;
    private Button loginButton;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private String[] roles = {"student", "parent"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check Firebase initialization
        if (FirebaseDatabase.getInstance() == null) {
            Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Test database connection
        databaseReference.child("test").setValue("Connection test")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Firebase connection successful", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Firebase connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        roleDropdown = findViewById(R.id.roleDropdown);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Set up role dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        roleDropdown.setAdapter(adapter);
        roleDropdown.setFocusable(false);
        roleDropdown.setOnClickListener(v -> roleDropdown.showDropDown());

        // Set up login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String selectedRole = roleDropdown.getText().toString().trim();

            // Log input values
            Log.d("LoginActivity", "Attempting login with:");
            Log.d("LoginActivity", "Email: " + email);
            Log.d("LoginActivity", "Phone: " + phone);
            Log.d("LoginActivity", "Role: " + selectedRole);

            // Validate inputs
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, phone, selectedRole);
        });
    }

    private void loginUser(String email, String phone, String role) {
        // Test Firebase connection
        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("test");
        testRef.setValue("test").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("LoginActivity", "Firebase connection successful");
                // Continue with login
                proceedWithLogin(email, phone, role);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("LoginActivity", "Firebase connection failed: " + e.getMessage());
                Toast.makeText(LoginActivity.this, "Failed to connect to Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedWithLogin(String email, String phone, String role) {
        // Show progress dialog
        progressDialog.show();
        
        // Log the input values
        Log.d("LoginActivity", "Input values - Email: " + email + ", Phone: " + phone + ", Role: " + role);
        
        // Get Firebase reference
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        // Query for user with matching email
        usersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("LoginActivity", "Firebase query completed. Snapshot exists: " + snapshot.exists());
                    
                    if (!snapshot.exists()) {
                        Log.e("LoginActivity", "No user found with email: " + email);
                        Toast.makeText(LoginActivity.this, "No user found with this email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d("LoginActivity", "Found matching users: " + snapshot.getChildrenCount());
                    
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        try {
                            String dbEmail = userSnapshot.child("email").getValue(String.class);
                            String dbPhone = userSnapshot.child("phone").getValue(String.class);
                            String dbRole = userSnapshot.child("role").getValue(String.class);
                            String studentId = userSnapshot.child("studentId").getValue(String.class);
                            String linkedStudentId = userSnapshot.child("linkedStudentId").getValue(String.class);
                            
                            Log.d("LoginActivity", "Database values - Email: " + dbEmail + ", Phone: " + dbPhone + ", Role: " + dbRole);
                            Log.d("LoginActivity", "Student ID: " + studentId + ", Linked Student ID: " + linkedStudentId);
                            
                            // Check if credentials match
                            boolean emailMatch = email.equals(dbEmail);
                            boolean phoneMatch = dbPhone == null || phone.equals(dbPhone); // Allow null phone numbers
                            boolean roleMatch = role.equals(dbRole);
                            
                            Log.d("LoginActivity", "Email match: " + emailMatch + ", Phone match: " + phoneMatch + ", Role match: " + roleMatch);
                            
                            if (emailMatch && phoneMatch && roleMatch) {
                                Log.d("LoginActivity", "Credentials match successfully");
                                
                                // Check for null phone number
                                if (dbPhone == null) {
                                    Log.w("LoginActivity", "Warning: Phone number is null in database");
                                }
                                
                                if (role.equals("student")) {
                                    if (studentId != null && !studentId.isEmpty()) {
                                        Intent intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                                        intent.putExtra("studentId", studentId);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e("LoginActivity", "Student ID not found for student");
                                        Toast.makeText(LoginActivity.this, "Student ID not found", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (role.equals("parent")) {
                                    if (linkedStudentId != null && !linkedStudentId.isEmpty()) {
                                        Intent intent = new Intent(LoginActivity.this, ParentDashboardActivity.class);
                                        intent.putExtra("linkedStudentId", linkedStudentId);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Log.e("LoginActivity", "Linked student ID not found for parent");
                                        Toast.makeText(LoginActivity.this, "Linked student ID not found", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("LoginActivity", "Invalid role: " + role);
                                    Toast.makeText(LoginActivity.this, "Invalid role selected", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("LoginActivity", "Credentials do not match");
                                Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Error processing user data: " + e.getMessage());
                            Toast.makeText(LoginActivity.this, "Error processing user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    // Hide progress dialog
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("LoginActivity", "Database error: " + error.getMessage());
                    Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            });
    }
}

package com.example.yo;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;
import android.content.Intent;

public class StudentDashboardActivity extends AppCompatActivity {
    private String studentId;
    private TextView studentName, studentDept, studentYear, studentRoll, studentCgpa;
    private TextView welcomeText, attendancePercent, tuitionFee, examFee, dueFee, backlogCount, backlogSubjects;
    private LinearLayout marksList, cgpaList;
    private CircularProgressIndicator attendanceProgress;
    private ProgressBar loadingSpinner;
    private Button retryButton;
    private FloatingActionButton refreshFab;
    private DatabaseReference databaseReference;

    private void initializeUIElements() {
        studentName = findViewById(R.id.studentName);
        studentDept = findViewById(R.id.studentDept);
        studentYear = findViewById(R.id.studentYear);
        studentRoll = findViewById(R.id.studentRoll);
        studentCgpa = findViewById(R.id.studentCgpa);
        welcomeText = findViewById(R.id.welcomeText);
        attendancePercent = findViewById(R.id.attendancePercent);
        tuitionFee = findViewById(R.id.tuitionFee);
        examFee = findViewById(R.id.examFee);
        dueFee = findViewById(R.id.dueFee);
        backlogCount = findViewById(R.id.backlogCount);
        backlogSubjects = findViewById(R.id.backlogSubjects);
        marksList = findViewById(R.id.marksList);
        cgpaList = findViewById(R.id.cgpaList);
        attendanceProgress = findViewById(R.id.attendanceProgress);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        retryButton = findViewById(R.id.retryButton);
        refreshFab = findViewById(R.id.refreshFab);
        Button btnLoginAgain = findViewById(R.id.btnLoginAgain);
        Button btnExit = findViewById(R.id.btnExit);

        // Set up login again button click listener
        btnLoginAgain.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Set up exit button click listener
        btnExit.setOnClickListener(v -> {
            finishAffinity(); // Close all activities
            System.exit(0); // Exit the app
        });
        
        // Set up retry button click listener
        retryButton.setOnClickListener(v -> {
            loadingSpinner.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            loadStudentData();
        });

        // Set up refresh FAB click listener
        refreshFab.setOnClickListener(v -> {
            loadingSpinner.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            loadStudentData();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Check Firebase initialization
        try {
            if (FirebaseDatabase.getInstance() == null) {
                Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Initialize Firebase and UI elements
            databaseReference = FirebaseDatabase.getInstance().getReference();
            initializeUIElements();

            // Get studentId from intent and validate
            studentId = getIntent().getStringExtra("studentId");
            if (studentId == null || studentId.isEmpty()) {
                Toast.makeText(this, "Invalid student ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Show loading spinner
            loadingSpinner.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            refreshFab.setVisibility(View.GONE);

            // Load student data
            loadStudentData();
        } catch (Exception e) {
            Log.e("StudentDashboard", "Firebase initialization error: " + e.getMessage());
            Toast.makeText(this, "Error initializing Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

        // Get studentId from intent and validate
        studentId = getIntent().getStringExtra("studentId");
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Invalid student ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading spinner
        loadingSpinner.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        refreshFab.setVisibility(View.GONE);

        // Load student data
        loadStudentData();
    }

    private void loadStudentData() {
        // First check if the student exists
        Log.d("StudentDashboard", "Loading student data for ID: " + studentId);
        databaseReference.child("students").child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                if (!studentSnapshot.exists()) {
                    Toast.makeText(StudentDashboardActivity.this, "Student data not found", Toast.LENGTH_SHORT).show();
                    loadingSpinner.setVisibility(View.GONE);
                    retryButton.setVisibility(View.VISIBLE);
                    refreshFab.setVisibility(View.GONE);
                    return;
                }

                try {
                    // Get basic student info
                    Object nameObj = studentSnapshot.child("name").getValue();
                    Object deptObj = studentSnapshot.child("department").getValue();
                    Object yearObj = studentSnapshot.child("year").getValue();
                    Object rollObj = studentSnapshot.child("roll_no").getValue();
                    Object cgpaObj = studentSnapshot.child("cgpa").getValue();
                    
                    String name = nameObj != null ? String.valueOf(nameObj) : "--";
                    String dept = deptObj != null ? String.valueOf(deptObj) : "--";
                    String year = yearObj != null ? String.valueOf(yearObj) : "--";
                    String roll = rollObj != null ? String.valueOf(rollObj) : "--";
                    String cgpa = cgpaObj != null ? String.valueOf(cgpaObj) : "--";

                    // Update basic info
                    studentName.setText(name);
                    studentDept.setText(dept);
                    studentYear.setText(year);
                    studentRoll.setText(roll);
                    studentCgpa.setText(cgpa);
                    welcomeText.setText("Welcome, " + name + "!");

                    // Load fees
                    DataSnapshot feesSnapshot = studentSnapshot.child("fees");
                    if (feesSnapshot.exists()) {
                        Object tuitionObj = feesSnapshot.child("tuition_fees").getValue();
                        Object examObj = feesSnapshot.child("exam_fees").getValue();
                        Object dueObj = feesSnapshot.child("due_amount").getValue();
                        
                        String tuition = tuitionObj != null ? String.valueOf(tuitionObj) : "--";
                        String exam = examObj != null ? String.valueOf(examObj) : "--";
                        String due = dueObj != null ? String.valueOf(dueObj) : "--";
                        
                        tuitionFee.setText("Tuition: " + tuition);
                        examFee.setText("Exam: " + exam);
                        dueFee.setText("Due: " + due);
                    } else {
                        tuitionFee.setText("Tuition: --");
                        examFee.setText("Exam: --");
                        dueFee.setText("Due: --");
                    }

                    // Load backlogs
                    DataSnapshot backlogsSnapshot = studentSnapshot.child("backlogs");
                    if (backlogsSnapshot.exists()) {
                        int count = 0;
                        StringBuilder subjects = new StringBuilder();
                        for (DataSnapshot subj : backlogsSnapshot.getChildren()) {
                            String subjectName = subj.getKey();
                            String status = subj.getValue(String.class);
                            if (status != null && !"cleared".equalsIgnoreCase(status)) {
                                count++;
                                if (subjects.length() > 0) subjects.append("\n");
                                subjects.append(subjectName);
                            }
                        }
                        backlogCount.setText(String.valueOf(count));
                        backlogSubjects.setText(subjects.toString());
                    } else {
                        backlogCount.setText("0");
                        backlogSubjects.setText("No backlogs");
                    }

                    // Load attendance
                    DataSnapshot attendanceSnapshot = studentSnapshot.child("attendance");
                    if (attendanceSnapshot.exists()) {
                        int totalAttended = 0;
                        int totalClasses = 0;
                        for (DataSnapshot subj : attendanceSnapshot.getChildren()) {
                            Integer attended = subj.child("attended").getValue(Integer.class);
                            Integer total = subj.child("total").getValue(Integer.class);
                            if (attended != null && total != null) {
                                totalAttended += attended;
                                totalClasses += total;
                            }
                        }
                        if (totalClasses > 0) {
                            float attendance = ((float) totalAttended / totalClasses) * 100;
                            attendanceProgress.setProgress((int) attendance);
                            attendancePercent.setText("Attendance: " + String.format("%.1f%%", attendance));
                        } else {
                            attendanceProgress.setProgress(0);
                            attendancePercent.setText("Attendance: --");
                        }
                    } else {
                        attendanceProgress.setProgress(0);
                        attendancePercent.setText("Attendance: --");
                    }

                    // Load marks
                    DataSnapshot marksSnapshot = studentSnapshot.child("marks");
                    if (marksSnapshot.exists()) {
                        marksList.removeAllViews();
                        for (DataSnapshot subj : marksSnapshot.getChildren()) {
                            String subject = subj.getKey();
                            Object internalObj = subj.child("internal").getValue();
                            Object externalObj = subj.child("external").getValue();
                            Object averageObj = subj.child("average").getValue();
                            
                            String internal = internalObj != null ? String.valueOf(internalObj) : "--";
                            String external = externalObj != null ? String.valueOf(externalObj) : "--";
                            String average = averageObj != null ? String.valueOf(averageObj) : "--";
                            
                            TextView marksView = new TextView(StudentDashboardActivity.this);
                            marksView.setText(subject + ": Internal: " + internal + ", External: " + external + ", Avg: " + average);
                            marksList.addView(marksView);
                        }
                    }

                    // Load CGPA by semester
                    DataSnapshot cgpaSnapshot = studentSnapshot.child("cgpa_history");
                    if (cgpaSnapshot.exists()) {
                        cgpaList.removeAllViews();
                        for (DataSnapshot sem : cgpaSnapshot.getChildren()) {
                            String semester = sem.getKey();
                            Object semesterCgpaObj = sem.getValue();
                            String semesterCgpa = semesterCgpaObj != null ? String.valueOf(semesterCgpaObj) : "--";
                            
                            TextView cgpaView = new TextView(StudentDashboardActivity.this);
                            cgpaView.setText(semester + ": " + semesterCgpa);
                            cgpaList.addView(cgpaView);
                        }
                    } else {
                        cgpaList.removeAllViews();
                        TextView cgpaView = new TextView(StudentDashboardActivity.this);
                        cgpaView.setText("No CGPA history available");
                        cgpaList.addView(cgpaView);
                    }

                    // Hide loading and show refresh
                    loadingSpinner.setVisibility(View.GONE);
                    retryButton.setVisibility(View.GONE);
                    refreshFab.setVisibility(View.VISIBLE);

                } catch (Exception e) {
                    Log.e("StudentDashboard", "Error loading student data: " + e.getMessage());
                    Toast.makeText(StudentDashboardActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingSpinner.setVisibility(View.GONE);
                    retryButton.setVisibility(View.VISIBLE);
                    refreshFab.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Firebase error: " + error.getMessage());
                Toast.makeText(StudentDashboardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingSpinner.setVisibility(View.GONE);
                retryButton.setVisibility(View.VISIBLE);
                refreshFab.setVisibility(View.GONE);
            }
        });

    }
}

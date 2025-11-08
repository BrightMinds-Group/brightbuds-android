package com.example.brightbuds_app.activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.Progress;
import com.example.brightbuds_app.services.PDFReportService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
public class ReportGenerationActivity extends AppCompatActivity {

    private TextView txtReportStatus;
    private ProgressDialog progressDialog;

    // Firebase services
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Report configuration
    private String filterChildId;
    private String parentName = "Parent User";
    private String userRole = "parent";
    private long startTimestamp = 0;
    private long endTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_generation);

        initializeUIComponents();

        FirebaseUser user = verifyUserAuthentication();
        if (user == null) return;

        loadUserProfileAndGenerateReport(user);
    }

    /** Initialize UI and optional child filter */
    private void initializeUIComponents() {
        txtReportStatus = findViewById(R.id.txtReportStatus);

        filterChildId = getIntent().getStringExtra("childId");
        if (filterChildId != null) {
            Log.d("ReportGen", "📋 Generating report for child ID: " + filterChildId);
        }
    }

    /** Check Firebase authentication */
    private FirebaseUser verifyUserAuthentication() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return null;
        }
        return user;
    }

    /** Load Firestore user profile and detect role */
    private void loadUserProfileAndGenerateReport(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        processUserProfile(doc, user);
                    } else {
                        handleMissingUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> handleUserProfileLoadError(e, user));
    }

    /** Identify role and route to correct report type */
    private void processUserProfile(DocumentSnapshot doc, FirebaseUser user) {
        String roleField = doc.getString("role");
        String typeField = doc.getString("type");

        setDisplayName(doc, user);

        if ("admin".equalsIgnoreCase(roleField) || "admin".equalsIgnoreCase(typeField)) {
            userRole = "admin";
            Log.d("ReportGen", "👑 ADMIN DETECTED — generating system-wide report");
            generateAdminReport();
        } else {
            userRole = "parent";
            Log.d("ReportGen", "👪 PARENT DETECTED — generating family report");
            loadAndBuildParentReport(user.getUid(), user.getEmail());
        }
    }

    /** Get a friendly display name */
    private void setDisplayName(DocumentSnapshot doc, FirebaseUser user) {
        String name = doc.getString("name");
        if (name != null && !name.trim().isEmpty()) {
            parentName = name.trim();
        } else if (user.getEmail() != null) {
            parentName = user.getEmail().split("@")[0];
        }
    }

    private void handleMissingUserProfile(FirebaseUser user) {
        Log.w("ReportGen", "⚠️ User profile missing, defaulting to parent");
        parentName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Parent User";
        loadAndBuildParentReport(user.getUid(), user.getEmail());
    }

    private void handleUserProfileLoadError(Exception e, FirebaseUser user) {
        Log.e("ReportGen", "❌ Failed to load user profile", e);
        parentName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Parent User";
        loadAndBuildParentReport(user.getUid(), user.getEmail());
    }

    // ADMIN REPORT

    private void generateAdminReport() {
        showProgressDialog("Generating system-wide admin report...");

        // Pull data from correct Firestore collection
        Query progressQuery = buildProgressQueryWithDateRange(db.collection("child_progress"));

        progressQuery.get().addOnSuccessListener(progressSnap -> {
            List<Progress> progressList = extractProgressRecords(progressSnap);
            if (progressList.isEmpty()) {
                handleEmptyProgressData();
                return;
            }
            loadChildAndParentDataForAdminReport(progressList);
        }).addOnFailureListener(this::handleProgressDataLoadError);
    }

    /** Load child & parent data for full admin overview */
    private void loadChildAndParentDataForAdminReport(List<Progress> progressList) {
        db.collection("child_profiles").get().addOnSuccessListener(childrenSnap -> {
            Map<String, String> childNames = extractChildNames(childrenSnap);

            db.collection("users").get().addOnSuccessListener(usersSnap -> {
                Map<String, String> parentNames = extractParentNames(usersSnap);
                generateAdminPDFReport(progressList, childNames, parentNames);
            }).addOnFailureListener(this::handleParentDataLoadError);

        }).addOnFailureListener(this::handleChildDataLoadError);
    }

    private void generateAdminPDFReport(List<Progress> progressList,
                                        Map<String, String> childNames,
                                        Map<String, String> parentNames) {
        PDFReportService pdfService = new PDFReportService(this);
        pdfService.setDateRange(startTimestamp, endTimestamp);

        pdfService.generateAdminProgressReport(
                progressList,
                computeAverageScore(progressList),
                computeTotalPlays(progressList),
                parentName + " (Admin)",
                childNames,
                parentNames,
                new PDFReportService.PDFCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        handleReportSuccess("✅ Admin report saved at:\n" + filePath,
                                "Admin report generated successfully!");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        handleReportFailure("❌ Failed to generate admin report: " + e.getMessage(), e);
                    }
                });
    }

    // PARENT REPORT

    /** Load all children for current parent */
    private void loadAndBuildParentReport(String userId, String email) {
        showProgressDialog("Generating family progress report...");

        db.collection("child_profiles")
                .whereEqualTo("parentId", userId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(childrenSnap -> {
                    if (childrenSnap.isEmpty()) {
                        handleNoChildrenFound();
                        return;
                    }
                    processChildrenDataForParentReport(childrenSnap, userId, email);
                })
                .addOnFailureListener(this::handleChildrenLoadError);
    }

    /** Collect child IDs and names for the report */
    private void processChildrenDataForParentReport(QuerySnapshot childrenSnap, String userId, String email) {
        Map<String, String> childNames = new HashMap<>();
        List<String> childIds = new ArrayList<>();

        for (var doc : childrenSnap.getDocuments()) {
            childNames.put(doc.getId(), doc.getString("name"));
            childIds.add(doc.getId());
        }

        Log.d("ReportGen", "👨‍👩‍👧 Found " + childIds.size() + " children for parent " + parentName);
        loadProgressDataForParentReport(userId, email, childNames);
    }

    /** Fetch all progress data for this parent's children */
    private void loadProgressDataForParentReport(String userId, String email, Map<String, String> childNames) {
        Query progressQuery = buildParentProgressQuery(userId, email);

        progressQuery.get().addOnSuccessListener(progressSnap -> {
            List<Progress> progressList = extractProgressRecords(progressSnap);

            if (progressList.isEmpty()) {
                handleNoProgressDataFound();
                return;
            }

            generateParentPDFReport(progressList, childNames);
        }).addOnFailureListener(this::handleProgressLoadError);
    }

    private void generateParentPDFReport(List<Progress> progressList, Map<String, String> childNames) {
        PDFReportService pdfService = new PDFReportService(this);
        pdfService.setDateRange(startTimestamp, endTimestamp);

        pdfService.generateProgressReport(
                progressList,
                computeAverageScore(progressList),
                computeTotalPlays(progressList),
                parentName,
                childNames,
                new PDFReportService.PDFCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        handleReportSuccess("✅ Family report saved at:\n" + filePath,
                                "Family report generated successfully!");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        handleReportFailure("❌ Parent report generation failed: " + e.getMessage(), e);
                    }
                });
    }

    // QUERY UTILITIES

    private Query buildProgressQueryWithDateRange(Query baseQuery) {
        if (startTimestamp > 0 && endTimestamp > 0) {
            return baseQuery
                    .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                    .whereLessThanOrEqualTo("timestamp", endTimestamp);
        }
        return baseQuery;
    }

    /** Query parent progress from the correct collection */
    private Query buildParentProgressQuery(String userId, String email) {
        Query baseQuery = db.collection("child_progress")
                .whereEqualTo("parentId", userId);

        if (email != null && !email.isEmpty()) {
            return db.collection("child_progress")
                    .whereIn("parentId", Arrays.asList(userId, email));
        }

        return baseQuery;
    }

    // DATA EXTRACTION UTILITIES

    private List<Progress> extractProgressRecords(QuerySnapshot snapshot) {
        return snapshot.getDocuments()
                .stream()
                .map(doc -> doc.toObject(Progress.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, String> extractChildNames(QuerySnapshot snapshot) {
        Map<String, String> childNames = new HashMap<>();
        for (var doc : snapshot.getDocuments()) {
            childNames.put(doc.getId(), doc.getString("name"));
        }
        return childNames;
    }

    private Map<String, String> extractParentNames(QuerySnapshot snapshot) {
        Map<String, String> parentNames = new HashMap<>();
        for (var doc : snapshot.getDocuments()) {
            String name = doc.getString("name");
            if (name != null && !name.trim().isEmpty()) {
                parentNames.put(doc.getId(), name.trim());
            }
        }
        return parentNames;
    }

    // UTILITIES & UI HANDLERS

    /** Count total module plays */
    private int computeTotalPlays(List<Progress> progressList) {
        if (progressList == null || progressList.isEmpty()) return 0;
        return progressList.size();
    }

    private double computeAverageScore(List<Progress> progressList) {
        if (progressList == null || progressList.isEmpty()) return 0.0;
        double total = 0;
        for (Progress p : progressList) total += p.getScore();
        return total / progressList.size();
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void handleReportSuccess(String statusText, String toastMessage) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            txtReportStatus.setText(statusText);
            Toast.makeText(ReportGenerationActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void handleReportFailure(String errorText, Exception e) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            txtReportStatus.setText(errorText);
            Log.e("ReportGen", "PDF generation error", e);
        });
    }

    private void handleEmptyProgressData() {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ No progress data found.");
    }

    private void handleNoChildrenFound() {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ No child profiles found for this parent.");
    }

    private void handleNoProgressDataFound() {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ No learning progress found for your children.");
    }

    private void handleProgressDataLoadError(Exception e) {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ Failed to load progress: " + e.getMessage());
    }

    private void handleChildrenLoadError(Exception e) {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ Failed to load child profiles: " + e.getMessage());
    }

    private void handleChildDataLoadError(Exception e) {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ Failed to load child data: " + e.getMessage());
    }

    private void handleParentDataLoadError(Exception e) {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ Failed to load parent data: " + e.getMessage());
    }

    private void handleProgressLoadError(Exception e) {
        progressDialog.dismiss();
        txtReportStatus.setText("⚠️ Failed to load progress data: " + e.getMessage());
    }

    // DATE PICKER FOR REPORT RANGE
    private void showDateRangePicker() {
        final Calendar startCal = Calendar.getInstance();
        final Calendar endCal = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            startCal.set(year, month, day, 0, 0, 0);
            startTimestamp = startCal.getTimeInMillis();
            showEndDatePicker(endCal);
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH));

        startPicker.setTitle("Select Start Date");
        startPicker.show();
    }

    private void showEndDatePicker(Calendar endCal) {
        DatePickerDialog endPicker = new DatePickerDialog(this, (v2, y2, m2, d2) -> {
            endCal.set(y2, m2, d2, 23, 59, 59);
            endTimestamp = endCal.getTimeInMillis();
            regenerateReportWithDateRange();
        }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH));

        endPicker.setTitle("Select End Date");
        endPicker.show();
    }

    private void regenerateReportWithDateRange() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            if ("admin".equalsIgnoreCase(userRole)) generateAdminReport();
            else loadAndBuildParentReport(user.getUid(), user.getEmail());
        }
    }
}
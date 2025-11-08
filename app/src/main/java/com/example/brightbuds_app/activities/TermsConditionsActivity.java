package com.example.brightbuds_app.activities;

import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.brightbuds_app.R;

/**
 * TermsConditionsActivity:
 * Displays the Terms & Conditions for the BrightBuds app.
 * The content is loaded from strings.xml and supports HTML formatting.
 * Accessible via the Parent Profile screen under "Account Settings".
 */
public class TermsConditionsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView txtTermsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        initializeViews();
        setupListeners();
        loadTermsContent();
    }

    /** Initialize UI components */
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        txtTermsContent = findViewById(R.id.txtTermsContent);
    }

    /** Handle button click actions */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish()); // Closes the activity
    }

    /** Load and render HTML Terms & Conditions text */
    private void loadTermsContent() {
        String termsHtml = getString(R.string.terms_and_conditions_content);

        // Renders the HTML with proper formatting (headings, bold, lists)
        txtTermsContent.setText(Html.fromHtml(termsHtml, Html.FROM_HTML_MODE_LEGACY));
    }
}

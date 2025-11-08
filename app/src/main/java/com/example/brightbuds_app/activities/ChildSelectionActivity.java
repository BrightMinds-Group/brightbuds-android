package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.services.ChildProfileService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * ChildSelectionActivity
 * Allows child users to select their profile
 */
public class ChildSelectionActivity extends AppCompatActivity {

    private ChildProfileService childService;
    private LinearLayout childrenContainer;
    private LinearLayout loadingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        childService = new ChildProfileService();
        childrenContainer = findViewById(R.id.childrenContainer);
        loadingLayout = findViewById(R.id.loadingLayout);

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadChildren();
    }

    private void loadChildren() {
        String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        childService.getChildrenForCurrentParent(new DataCallbacks.ChildrenListCallback() {
            @Override
            public void onSuccess(List<ChildProfile> children) {
                loadingLayout.setVisibility(View.GONE);

                if (children.isEmpty()) {
                    showEmptyState();
                } else {
                    for (ChildProfile child : children) {
                        addChildCard(child);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                loadingLayout.setVisibility(View.GONE);
                showErrorState();
            }
        });
    }

    private void addChildCard(ChildProfile child) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_child_selection_card, childrenContainer, false);

        TextView txtChildName = card.findViewById(R.id.txtChildName);
        TextView txtChildAge = card.findViewById(R.id.txtChildAge);

    // ✅ Show decrypted display name
        txtChildName.setText(child.getDisplayName());
        txtChildAge.setText(child.getAge() + " years old");


        // Load avatar if available
        if (child.getAvatarUrl() != null && !child.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(child.getAvatarUrl())
                    .placeholder(R.drawable.ic_child_placeholder)
                    .error(R.drawable.ic_child_placeholder)
                    .circleCrop()
                    .into(card.<ImageView>findViewById(R.id.imgChildAvatar));
        }

        card.setOnClickListener(v -> startChildDashboard(child));
        childrenContainer.addView(card);
    }

    private void startChildDashboard(ChildProfile child) {
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("childId", child.getChildId());
        intent.putExtra("childName", child.getName());
        startActivity(intent);
        finish();
    }

    private void showEmptyState() {
        View emptyView = LayoutInflater.from(this)
                .inflate(R.layout.item_empty_children, childrenContainer, false);
        childrenContainer.addView(emptyView);

        TextView emptyText = emptyView.findViewById(R.id.txtEmpty);
        emptyText.setText("No child profiles found. Please ask a parent to add your profile.");
    }

    private void showErrorState() {
        View errorView = LayoutInflater.from(this)
                .inflate(R.layout.item_error_children, childrenContainer, false);
        childrenContainer.addView(errorView);

        errorView.findViewById(R.id.btnRetry).setOnClickListener(v -> {
            childrenContainer.removeAllViews();
            loadingLayout.setVisibility(View.VISIBLE);
            loadChildren();
        });
    }
}
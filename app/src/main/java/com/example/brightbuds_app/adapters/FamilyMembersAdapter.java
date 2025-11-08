package com.example.brightbuds_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.FamilyMember;

import java.io.File;
import java.util.List;

/**
 * FamilyMembersAdapter
 * --------------------
 * Adapter for displaying local and default family members in a grid layout.
 *
 * ✅ November 2025 Update:
 *   - Added support for loading local images via member.getLocalPath().
 *   - Retains backward compatibility for imageUrl (if ever reintroduced).
 *   - Defaults to drawable placeholders if no image found.
 */
public class FamilyMembersAdapter extends RecyclerView.Adapter<FamilyMembersAdapter.ViewHolder> {

    private final List<FamilyMember> familyMembers;
    private final OnFamilyMemberClickListener listener;

    /** Listener interface for click events */
    public interface OnFamilyMemberClickListener {
        void onFamilyMemberClick(FamilyMember member);
    }

    public FamilyMembersAdapter(List<FamilyMember> familyMembers, OnFamilyMemberClickListener listener) {
        this.familyMembers = familyMembers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = familyMembers.get(position);

        // --- Bind basic info ---
        holder.textName.setText(member.getName());
        holder.textRelationship.setText(member.getRelationship());

        // --- NEW LOGIC: Load localPath first, fallback to URL, then default drawable ---
        if (member.getLocalPath() != null && !member.getLocalPath().isEmpty()) {
            File localFile = new File(member.getLocalPath());
            if (localFile.exists()) {
                // ✅ Load image directly from device storage (COPPA-safe)
                Glide.with(holder.itemView.getContext())
                        .load(localFile)
                        .placeholder(getDefaultImageResource(member.getRelationship()))
                        .error(getDefaultImageResource(member.getRelationship()))
                        .into(holder.imagePhoto);
            } else {
                // File missing locally, fallback to default icon
                holder.imagePhoto.setImageResource(getDefaultImageResource(member.getRelationship()));
            }
        } else if (member.getImageUrl() != null && !member.getImageUrl().isEmpty()) {
            // ✅ Load from Firebase (kept for backward compatibility)
            Glide.with(holder.itemView.getContext())
                    .load(member.getImageUrl())
                    .placeholder(getDefaultImageResource(member.getRelationship()))
                    .error(getDefaultImageResource(member.getRelationship()))
                    .into(holder.imagePhoto);
        } else {
            // ✅ Use built-in default drawable
            holder.imagePhoto.setImageResource(getDefaultImageResource(member.getRelationship()));
        }

        // --- Handle tap events ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFamilyMemberClick(member);
        });
    }

    /**
     * Returns appropriate placeholder icon for each relationship.
     */
    private int getDefaultImageResource(String relationship) {
        if (relationship == null) return R.drawable.default_family_member;

        switch (relationship.toLowerCase()) {
            case "mother":
            case "mom":
                return R.drawable.default_mom;
            case "father":
            case "dad":
                return R.drawable.default_dad;
            case "grandmother":
            case "grandma":
                return R.drawable.default_grandma;
            case "grandfather":
            case "grandpa":
                return R.drawable.default_grandpa;
            case "sister":
                return R.drawable.default_sister;
            case "brother":
                return R.drawable.default_brother;
            default:
                return R.drawable.default_family_member;
        }
    }

    @Override
    public int getItemCount() {
        return familyMembers.size();
    }

    // ----------------------------------------------------------------------
    // 🔹 ViewHolder
    // ----------------------------------------------------------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePhoto;
        TextView textName, textRelationship;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imageFamilyMember);
            textName = itemView.findViewById(R.id.textFamilyMemberName);
            textRelationship = itemView.findViewById(R.id.textFamilyMemberRelationship);
        }
    }
}

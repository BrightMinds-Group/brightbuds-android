package com.example.brightbuds_app.models;

import com.google.firebase.firestore.PropertyName;
import androidx.annotation.NonNull;

public class Module {

    private String id;
    private String title;
    private String description;
    private String type;
    private String icon;
    private String storagePath;
    private Boolean isActive;
    private int order;
    private int points;

    // Required empty constructor for Firestore
    public Module() {}

    // Optional: Convenience constructor
    public Module(String id, String title, String description, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.isActive = true;
        this.order = 0;
        this.points = 0;
    }

    // Firestore document ID
    @PropertyName("id")
    public String getId() { return id; }

    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    // Title
    @PropertyName("title")
    public String getTitle() { return title; }

    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    // Description
    @PropertyName("description")
    public String getDescription() { return description; }

    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    // Type (e.g., "game", "video", "quiz")
    @PropertyName("type")
    public String getType() { return type; }

    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    // Icon (drawable resource name or URL)
    @PropertyName("icon")
    public String getIcon() { return icon; }

    @PropertyName("icon")
    public void setIcon(String icon) { this.icon = icon; }

    // Storage path (Firebase Storage reference)
    @PropertyName("storagePath")
    public String getStoragePath() { return storagePath; }

    @PropertyName("storagePath")
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    // Active status with safe defaults
    @PropertyName("isActive")
    public Boolean getIsActive() { return isActive != null ? isActive : true; }

    @PropertyName("isActive")
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Convenience method
    public boolean isActive() { return isActive != null ? isActive : true; }

    // Display order
    @PropertyName("order")
    public int getOrder() { return order; }

    @PropertyName("order")
    public void setOrder(int order) { this.order = order; }

    // Points/reward value
    @PropertyName("points")
    public int getPoints() { return points; }

    @PropertyName("points")
    public void setPoints(int points) { this.points = points; }

    // Improved toString with more details
    @NonNull
    @Override
    public String toString() {
        return "Module{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", active=" + isActive() +
                ", order=" + order +
                ", points=" + points +
                '}';
    }
}
package com.example.brightbuds_app.models;

/**
 * FamilyMember model
 * -------------------
 * Represents a single family member entry in the "My Family" module.
 *
 * ✅ November 2025 Update:
 *  - Supports local-only storage via `localPath`.
 *  - Retains backward compatibility with older Firestore documents (imageUrl).
 *  - Fully documented for maintainability.
 */

public class FamilyMember {

    private String name;
    private String relationship;
    private String imageUrl;
    private String localPath;
    private String parentId;
    private long createdAt;


    // Empty constructor for Firestore deserialization
    public FamilyMember() {}

    /**
     * Constructor for default members (uses built-in drawable resources)
     * Note: These entries are placeholders with no localPath.
     */
    public FamilyMember(String name, String relationship, int imageResource, String imageUrl) {
        this.name = name;
        this.relationship = relationship;
        this.imageUrl = imageUrl;
        this.localPath = null;
    }

    /**
     * Constructor for locally stored members.
     * @param name         The family member’s name.
     * @param relationship Their relationship (e.g., "father", "sibling").
     * @param localPath    Absolute path to the local image on device.
     */
    public FamilyMember(String name, String relationship, String localPath) {
        this.name = name;
        this.relationship = relationship;
        this.localPath = localPath;
    }

    // Getters & Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Utility

    @Override
    public String toString() {
        return "FamilyMember{" +
                "name='" + name + '\'' +
                ", relationship='" + relationship + '\'' +
                ", localPath='" + localPath + '\'' +
                ", parentId='" + parentId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

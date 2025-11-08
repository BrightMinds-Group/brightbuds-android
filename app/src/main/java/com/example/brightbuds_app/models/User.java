package com.example.brightbuds_app.models;

/*
 Model class for a User.
 This structure will be stored in the "users" collection in Firestore.
 */
public class User {
    private String uid;       // The unique ID from Firebase Authentication.
    private String name;
    private String email;
    private String userType;  // e.g., "parent" or "admin"

    // A public, no-argument constructor is required by Firestore
    public User() {
    }

    // Constructor to easily create a new user object
    public User(String uid, String name, String email, String userType) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.userType = userType;
    }

    // Getters and Setters for all fields
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}

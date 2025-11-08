package com.example.brightbuds_app.services;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Handles user authentication and data management with Firebase.
public class UserService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";

    public UserService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /*
     Registers a new parent user.
     1. Creates an account in Firebase Authentication.
     2. If successful, creates a corresponding user document in Firestore.
     * @param callback Returns a success or failure message.
     */
    public void registerParent(String name, String email, String password, DataCallbacks.GenericCallback callback) {
        // Step 1: Create the user in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();

                        // Step 2: Create a User object to save in Firestore
                        User newUser = new User(uid, name, email, "parent");

                        // Step 3: Save the User object to the "users" collection with the UID as the document ID
                        db.collection(USERS_COLLECTION).document(uid)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> callback.onSuccess("Registration successful!"))
                                .addOnFailureListener(e -> callback.onFailure(e));
                    } else {
                        callback.onFailure(new Exception("Firebase user is null after creation."));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }
}

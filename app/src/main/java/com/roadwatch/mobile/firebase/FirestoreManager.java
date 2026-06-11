package com.roadwatch.mobile.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private final FirebaseFirestore db;

    public interface ProfileCallback {
        void onSuccess(Map<String, Object> profile);
        void onFailure(String error);
    }

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveUserProfile(String uid, String fullName, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("email", email);
        db.collection("users").document(uid).set(data)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "User profile saved: " + uid))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to save user profile: " + e.getMessage()));
    }

    public void getUserProfile(String uid, ProfileCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot.getData());
                    } else {
                        callback.onFailure("Profile not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}

package com.roadwatch.mobile.auth;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private final FirebaseAuth auth;

    public FirebaseAuthManager() {
        this.auth = FirebaseAuth.getInstance();
    }

    public void signUp(String fullName, String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure("User creation succeeded but user is null");
                        }
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Sign up failed";
                        callback.onFailure(msg);
                    }
                });
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        callback.onFailure(msg);
                    }
                });
    }

    public void logout() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
}

package com.example.todolist.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final int RC_SIGN_IN = 9001;
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_IS_SIGNED_IN = "is_signed_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    
    private static AuthManager instance;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Context context;
    private SharedPreferences prefs;
    
    private AuthManager() {
        // Private constructor for singleton
    }
    
    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("788880723589-i0uq5id6g4ibgbsc2j1ie9tsbudo18vj.apps.googleusercontent.com")
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    public void signIn(Activity activity, AuthCallback callback) {
        if (mGoogleSignInClient == null) {
            initialize(activity);
        }
        
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
        
        // Store callback for result handling
        this.pendingCallback = callback;
    }
    
    private AuthCallback pendingCallback;
    
    public void handleSignInResult(Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account, callback);
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            if (callback != null) {
                callback.onError("Google sign in failed: " + e.getMessage());
            }
        }
    }
    
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct, AuthCallback callback) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserInfo(user.getEmail(), user.getDisplayName());
                                if (callback != null) {
                                    callback.onSuccess(user.getEmail());
                                }
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (callback != null) {
                                callback.onError("Authentication failed: " + task.getException().getMessage());
                            }
                        }
                    }
                });
    }
    
    public void signOut(AuthCallback callback) {
        // Sign out Firebase
        mAuth.signOut();
        
        // Sign out Google
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                clearUserInfo();
                if (callback != null) {
                    callback.onSuccess("Signed out successfully");
                }
            });
        } else {
            clearUserInfo();
            if (callback != null) {
                callback.onSuccess("Signed out successfully");
            }
        }
    }
    
    public boolean isSignedIn() {
        FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
        boolean firebaseSignedIn = currentUser != null;
        boolean prefsSignedIn = prefs != null ? prefs.getBoolean(KEY_IS_SIGNED_IN, false) : false;
        return firebaseSignedIn || prefsSignedIn;
    }
    
    public String getCurrentUserEmail() {
        FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
        if (currentUser != null) {
            return currentUser.getEmail();
        }
        return prefs != null ? prefs.getString(KEY_USER_EMAIL, null) : null;
    }
    
    public String getCurrentUserName() {
        FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
        if (currentUser != null) {
            return currentUser.getDisplayName();
        }
        return prefs != null ? prefs.getString(KEY_USER_NAME, null) : null;
    }
    
    private void saveUserInfo(String email, String name) {
        if (prefs != null) {
            prefs.edit()
                    .putBoolean(KEY_IS_SIGNED_IN, true)
                    .putString(KEY_USER_EMAIL, email)
                    .putString(KEY_USER_NAME, name)
                    .apply();
        }
    }
    
    private void clearUserInfo() {
        if (prefs != null) {
            prefs.edit()
                    .putBoolean(KEY_IS_SIGNED_IN, false)
                    .remove(KEY_USER_EMAIL)
                    .remove(KEY_USER_NAME)
                    .putBoolean(KEY_SYNC_ENABLED, false)
                    .apply();
        }
    }
    
    // Sync management methods
    public boolean isSyncEnabled() {
        return prefs != null ? prefs.getBoolean(KEY_SYNC_ENABLED, false) : false;
    }
    
    public void setSyncEnabled(boolean enabled) {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply();
        }
    }
    
    public boolean shouldSyncToFirebase() {
        return isSignedIn() && isSyncEnabled();
    }
    
    public interface AuthCallback {
        void onSuccess(String email);
        void onError(String error);
    }
}

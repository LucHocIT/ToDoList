package com.example.todolist.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.todolist.R;
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
    
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private AuthListener authListener;
    
    public interface AuthListener {
        void onAuthSuccess(FirebaseUser user);
        void onAuthError(String error);
        void onSignOut();
    }
    
    public AuthManager(Context context, AuthListener listener) {
        this.authListener = listener;
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    public void signInWithGoogle(Activity activity) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            Log.d(TAG, "Google sign in successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            if (authListener != null) {
                authListener.onAuthError("Google sign in failed: " + e.getMessage());
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (authListener != null) {
                                authListener.onAuthSuccess(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (authListener != null) {
                                authListener.onAuthError("Authentication failed");
                            }
                        }
                    }
                });
    }
    
    public void signOut() {
        // Firebase sign out
        mAuth.signOut();
        
        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "User signed out");
                if (authListener != null) {
                    authListener.onSignOut();
                }
            }
        });
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    public boolean isSignedIn() {
        return getCurrentUser() != null;
    }
    
    public static int getSignInRequestCode() {
        return RC_SIGN_IN;
    }
}

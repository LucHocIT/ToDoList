package com.example.todolist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Giảm xuống 2 giây
    private static final int ANIMATION_DELAY = 100;
    
    private View circleOuter, circleMiddle, circleInner;
    private com.google.android.material.imageview.ShapeableImageView appLogo;
    private View logoShadow;
    private TextView appName, appTagline, loadingText;
    private View loadingDot1, loadingDot2, loadingDot3;
    private View loadingContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize views
        initViews();
        
        // Start animations
        startAnimations();
        
        // Navigate to MainActivity after splash duration
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
    
    private void initViews() {
        circleOuter = findViewById(R.id.circle_outer);
        circleMiddle = findViewById(R.id.circle_middle);
        circleInner = findViewById(R.id.circle_inner);
        appLogo = findViewById(R.id.app_logo);
        logoShadow = findViewById(R.id.logo_shadow);
        appName = findViewById(R.id.app_name);
        appTagline = findViewById(R.id.app_tagline);
        loadingContainer = findViewById(R.id.loading_container);
        loadingDot1 = findViewById(R.id.loading_dot1);
        loadingDot2 = findViewById(R.id.loading_dot2);
        loadingDot3 = findViewById(R.id.loading_dot3);
        loadingText = findViewById(R.id.loading_text);
    }
    
    private void startAnimations() {
        // Animate circles with ripple effect - faster
        animateCircle(circleOuter, 0, 1.0f);
        animateCircle(circleMiddle, 100, 1.0f);
        animateCircle(circleInner, 200, 1.0f);
        
        // Animate logo with bounce effect
        new Handler().postDelayed(() -> animateLogo(), 150);
        
        // Animate shadow
        new Handler().postDelayed(() -> animateShadow(), 150);
        
        // Animate text
        new Handler().postDelayed(() -> animateText(appName), 450);
        new Handler().postDelayed(() -> animateText(appTagline), 600);
        
        // Animate loading elements
        new Handler().postDelayed(() -> {
            animateFadeIn(loadingContainer);
            startDotsAnimation();
        }, 1000);
    }
    
    private void startDotsAnimation() {
        // Animate dot 1
        animateDot(loadingDot1, 0);
        // Animate dot 2
        animateDot(loadingDot2, 200);
        // Animate dot 3
        animateDot(loadingDot3, 400);
    }
    
    private void animateDot(View dot, long delay) {
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1.0f, 1.5f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(dot, "alpha", 1.0f, 0.3f, 1.0f);
        
        scaleY.setDuration(600);
        alpha.setDuration(600);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);
        
        scaleY.start();
        alpha.start();
    }
    
    private void animateCircle(View circle, long delay, float targetAlpha) {
        AnimatorSet animatorSet = new AnimatorSet();
        
        // Scale animation - faster
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(circle, "scaleX", 0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(circle, "scaleY", 0f, 1.1f, 1.0f);
        scaleX.setDuration(500);
        scaleY.setDuration(500);
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());
        
        // Alpha animation
        ObjectAnimator alpha = ObjectAnimator.ofFloat(circle, "alpha", 0f, targetAlpha);
        alpha.setDuration(400);
        
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setStartDelay(delay);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // No pulse animation for simpler look
            }
        });
        animatorSet.start();
    }
    
    private void animateLogo() {
        AnimatorSet animatorSet = new AnimatorSet();
        
        // Scale with bounce - faster
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(appLogo, "scaleX", 0f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(appLogo, "scaleY", 0f, 1.1f, 1.0f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());
        
        // Rotation for dynamic effect
        ObjectAnimator rotation = ObjectAnimator.ofFloat(appLogo, "rotation", -5f, 0f);
        rotation.setDuration(600);
        rotation.setInterpolator(new DecelerateInterpolator());
        
        // Alpha
        ObjectAnimator alpha = ObjectAnimator.ofFloat(appLogo, "alpha", 0f, 1.0f);
        alpha.setDuration(500);
        
        animatorSet.playTogether(scaleX, scaleY, rotation, alpha);
        animatorSet.start();
    }
    
    private void animateShadow() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoShadow, "alpha", 0f, 1.0f);
        alpha.setDuration(800);
        alpha.setInterpolator(new DecelerateInterpolator());
        alpha.start();
    }
    
    private void animateText(TextView textView) {
        AnimatorSet animatorSet = new AnimatorSet();
        
        // Translate from bottom
        ObjectAnimator translateY = ObjectAnimator.ofFloat(textView, "translationY", 30f, 0f);
        translateY.setDuration(600);
        translateY.setInterpolator(new DecelerateInterpolator());
        
        // Alpha
        ObjectAnimator alpha = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1.0f);
        alpha.setDuration(800);
        
        animatorSet.playTogether(translateY, alpha);
        animatorSet.start();
    }
    
    private void animateFadeIn(View view) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f);
        alpha.setDuration(600);
        alpha.setInterpolator(new DecelerateInterpolator());
        alpha.start();
    }
    
    @Override
    public void onBackPressed() {
        // Disable back button on splash screen
        // Do nothing
    }
}

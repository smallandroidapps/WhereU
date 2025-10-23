package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.whereu.whereu.R;
import android.view.View;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate the logo
        CardView logo = findViewById(R.id.logo_container);
        AnimationSet logoAnimation = new AnimationSet(true);
        logoAnimation.addAnimation(new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        logoAnimation.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        logoAnimation.setDuration(1500);
        logo.startAnimation(logoAnimation);

        // Animate the app name
        TextView appName = findViewById(R.id.app_name);
        Animation appNameAnimation = new AlphaAnimation(0.0f, 1.0f);
        appNameAnimation.setDuration(1500);
        appNameAnimation.setStartOffset(500);
        appName.startAnimation(appNameAnimation);

        // Animate loading dots
        final View dot1 = findViewById(R.id.dot1);
        final View dot2 = findViewById(R.id.dot2);
        final View dot3 = findViewById(R.id.dot3);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count % 3 == 0) {
                    dot1.setAlpha(1.0f);
                    dot2.setAlpha(0.4f);
                    dot3.setAlpha(0.4f);
                } else if (count % 3 == 1) {
                    dot1.setAlpha(0.4f);
                    dot2.setAlpha(1.0f);
                    dot3.setAlpha(0.4f);
                } else {
                    dot1.setAlpha(0.4f);
                    dot2.setAlpha(0.4f);
                    dot3.setAlpha(1.0f);
                }
                count++;
                handler.postDelayed(this, 300);
            }
        }, 0);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashActivity.this, SignInActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
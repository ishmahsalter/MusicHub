package com.ishmah.musichub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.ishmah.musichub.R;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int progress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        // Animate progress bar
        handler.post(progressRunnable);
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            progress += 2;
            progressBar.setProgress(progress);

            if (progress < 100) {
                handler.postDelayed(this, 50);
            } else {
                // Progress selesai → pindah ke MainActivity
                handler.postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }, 300);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
    }
}
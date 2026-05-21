package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.UserProfileDao;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.EditText;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private EditText etUsername, etBio;
    private Switch swNotifications;
    private TextView chipThemePurple, chipThemeDark, chipThemeGold;
    private UserProfileDao userProfileDao;
    private String selectedPhotoUri = null;
    private String selectedTheme = "purple";

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    Glide.with(this).load(uri).into(ivAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyThemeFromPrefs();
        setContentView(R.layout.activity_edit_profile);

        userProfileDao = new UserProfileDao(this);

        ivAvatar = findViewById(R.id.iv_avatar);
        etUsername = findViewById(R.id.et_username);
        etBio = findViewById(R.id.et_bio);
        swNotifications = findViewById(R.id.sw_notifications);
        chipThemePurple = findViewById(R.id.chip_theme_purple);
        chipThemeDark = findViewById(R.id.chip_theme_dark);
        chipThemeGold = findViewById(R.id.chip_theme_gold);

        loadProfile();
        setupThemeChips();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_gallery).setOnClickListener(v ->
                galleryLauncher.launch("image/*"));

        findViewById(R.id.btn_delete_photo).setOnClickListener(v -> {
            selectedPhotoUri = null;
            ivAvatar.setImageResource(R.drawable.ic_profile);
        });

        ((Button) findViewById(R.id.btn_save)).setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        Map<String, String> profile = userProfileDao.getProfile();
        if (profile != null) {
            String username = profile.get("username");
            String bio = profile.get("bio");
            String photoUri = profile.get("photo_uri");
            String theme = profile.get("theme");
            String notif = profile.get("notif_enabled");

            if (username != null) etUsername.setText(username);
            if (bio != null) etBio.setText(bio);
            if (photoUri != null && !photoUri.isEmpty()) {
                selectedPhotoUri = photoUri;
                Glide.with(this).load(Uri.parse(photoUri))
                        .placeholder(R.drawable.ic_profile)
                        .into(ivAvatar);
            }
            if (theme != null) {
                selectedTheme = theme;
                updateThemeChipUI();
            }
            if (notif != null) swNotifications.setChecked("1".equals(notif));
        }
    }

    private void setupThemeChips() {
        chipThemePurple.setOnClickListener(v -> selectTheme("purple"));
        chipThemeDark.setOnClickListener(v -> selectTheme("dark"));
        chipThemeGold.setOnClickListener(v -> selectTheme("gold"));
    }

    private void selectTheme(String theme) {
        selectedTheme = theme;
        updateThemeChipUI();
    }

    private void updateThemeChipUI() {
        chipThemePurple.setBackgroundResource(
                "purple".equals(selectedTheme) ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemePurple.setTextColor(getResources().getColor(
                "purple".equals(selectedTheme) ? R.color.purple_soft : R.color.text_muted));

        chipThemeDark.setBackgroundResource(
                "dark".equals(selectedTheme) ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemeDark.setTextColor(getResources().getColor(
                "dark".equals(selectedTheme) ? R.color.purple_soft : R.color.text_muted));

        chipThemeGold.setBackgroundResource(
                "gold".equals(selectedTheme) ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemeGold.setTextColor(getResources().getColor(
                "gold".equals(selectedTheme) ? R.color.purple_soft : R.color.text_muted));
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        boolean notifEnabled = swNotifications.isChecked();

        if (username.isEmpty()) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        getSharedPreferences("musichub_prefs", MODE_PRIVATE)
                .edit()
                .putString("theme", "dark".equals(selectedTheme) ? "dark" : "light")
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                "dark".equals(selectedTheme)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);

        userProfileDao.saveProfile(
                username, bio,
                selectedPhotoUri != null ? selectedPhotoUri : "",
                selectedTheme, notifEnabled,
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }));
    }

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("musichub_prefs", MODE_PRIVATE);
        String theme = prefs.getString("theme", "dark");
        AppCompatDelegate.setDefaultNightMode(
                "light".equals(theme)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES);
    }
}

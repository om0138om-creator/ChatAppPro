package com.chatapp.pro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText usernameInput, bioInput;
    private Button saveButton;
    private ProgressBar progressBar;
    private Uri selectedImageUri;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = getIntent().getStringExtra("user_id");

        profileImage = findViewById(R.id.profile_image);
        usernameInput = findViewById(R.id.username_input);
        bioInput = findViewById(R.id.bio_input);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);

        profileImage.setOnClickListener(v -> pickImage());
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }

    private void saveProfile() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال اسم المستخدم", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        // أولاً: رفع الصورة إلى Cloudinary إذا تم اختيارها
        if (selectedImageUri != null) {
            CloudinaryManager.getInstance().uploadImage(selectedImageUri, new CloudinaryManager.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    updateSupabaseProfile(username, imageUrl);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "فشل رفع الصورة", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            updateSupabaseProfile(username, "");
        }
    }

    private void updateSupabaseProfile(String username, String imageUrl) {
        SupabaseManager.getInstance(this).createUserProfile(userId, "", username, bioInput.getText().toString(), imageUrl, new SupabaseManager.DatabaseCallback() {
            @Override
            public void onSuccess(org.json.JSONObject result) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "حدث خطأ في حفظ البيانات", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

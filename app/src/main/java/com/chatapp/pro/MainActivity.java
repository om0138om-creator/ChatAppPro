package com.chatapp.pro;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  CHAT APP PRO - MainActivity.java
 *  الشاشة الرئيسية للتطبيق - تحتوي على نظام المصادقة وقائمة المحادثات
 * ═══════════════════════════════════════════════════════════════════
 * 
 * هذا الملف يحتوي على:
 * 1. شاشة الترحيب (Splash Screen)
 * 2. شاشة التحقق من رقم الهاتف (Phone Verification)
 * 3. شاشة إدخال كود OTP
 * 4. شاشة إعداد الملف الشخصي (Profile Setup)
 * 5. الشاشة الرئيسية - قائمة المحادثات (Chats List)
 * 6. نظام البحث عن المستخدمين
 * 7. نظام الإشعارات
 * 
 * التقنيات المستخدمة:
 * - Supabase للقاعدة البيانات والتخزين
 * - Telegram Bot للمصادقة
 * - RecyclerView لعرض المحادثات
 * - SharedPreferences للتخزين المحلي
 * - Threading للعمليات الخلفية
 * 
 * @author ChatApp Pro Team
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    // ═══════════════════════════════════════════════════════════════
    // المتغيرات العامة والثوابت
    // ═══════════════════════════════════════════════════════════════
    
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String PREFS_NAME = "ChatAppPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PHONE = "phone_number";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PROFILE_PIC = "profile_pic";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    // Telegram Bot Configuration
    private static final String TELEGRAM_BOT_USERNAME = "YourBotUsername"; // ضع اسم البوت هنا
    private static final String TELEGRAM_BOT_LINK = "https://t.me/" + TELEGRAM_BOT_USERNAME;
    
    // Views - شاشة التحقق من الهاتف
    private RelativeLayout verificationLayout;
    private TextInputLayout phoneInputLayout;
    private TextInputEditText phoneEditText;
    private Button openTelegramButton;
    private Button verifyButton;
    private ProgressBar verificationProgressBar;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private ImageView headerIcon;
    
    // Views - شاشة إدخال OTP
    private RelativeLayout otpLayout;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button confirmOtpButton;
    private Button resendOtpButton;
    private ProgressBar otpProgressBar;
    private TextView otpTimer;
    private TextView otpPhoneDisplay;
    
    // Views - شاشة إعداد الملف الشخصي
    private RelativeLayout profileSetupLayout;
    private CircleImageView profileImageView;
    private Button uploadPhotoButton;
    private TextInputLayout usernameInputLayout;
    private TextInputEditText usernameEditText;
    private TextInputLayout bioInputLayout;
    private TextInputEditText bioEditText;
    private Button completeProfileButton;
    private ProgressBar profileProgressBar;
    private Uri selectedImageUri;
    
    // Views - الشاشة الرئيسية (قائمة المحادثات)
    private RelativeLayout mainLayout;
    private RecyclerView chatsRecyclerView;
    private FloatingActionButton newChatFab;
    private TextView noChatsTextView;
    private ProgressBar mainProgressBar;
    private EditText searchEditText;
    private ImageView searchIcon;
    private CircleImageView userProfileImage;
    private TextView userNameText;
    
    // Data & Adapters
    private ChatsAdapter chatsAdapter;
    private List<ChatItem> chatsList;
    private SupabaseManager supabaseManager;
    private SharedPreferences sharedPreferences;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // User Data
    private String currentUserId;
    private String currentPhone;
    private String currentUsername;
    private String currentProfilePic;
    
    // State Variables
    private boolean isOtpTimerRunning = false;
    private int otpTimerSeconds = 120; // 2 دقيقة
    
    // ═══════════════════════════════════════════════════════════════
    // دورة حياة النشاط (Activity Lifecycle)
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // تهيئة الأدوات الأساسية
        initializeCore();
        
        // تهيئة جميع الواجهات
        initializeAllViews();
        
        // التحقق من حالة تسجيل الدخول
        checkLoginStatus();
        
        // طلب الأذونات الضرورية
        requestNecessaryPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // تحديث قائمة المحادثات إذا كان المستخدم مسجل دخول
        if (isUserLoggedIn()) {
            refreshChatsList();
            startRealtimeListeners();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // إيقاف المستمعين في الوقت الفعلي لتوفير الموارد
        stopRealtimeListeners();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // تنظيف الموارد
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // التهيئة الأساسية (Core Initialization)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تهيئة المكونات الأساسية للتطبيق
     */
    private void initializeCore() {
        // تهيئة Supabase Manager
        supabaseManager = SupabaseManager.getInstance(this);
        
        // تهيئة SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // تهيئة ExecutorService للعمليات الخلفية
        executorService = Executors.newFixedThreadPool(4);
        
        // تهيئة Handler للواجهة الرئيسية
        mainHandler = new Handler(Looper.getMainLooper());
        
        // تهيئة قائمة المحادثات
        chatsList = new ArrayList<>();
    }
    
    /**
     * تهيئة جميع عناصر الواجهة
     */
    private void initializeAllViews() {
        // ══════════════════════════════════════════════════════════
        // تهيئة شاشة التحقق من الهاتف
        // ══════════════════════════════════════════════════════════
        verificationLayout = findViewById(R.id.verificationLayout);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);
        phoneEditText = findViewById(R.id.phoneEditText);
        openTelegramButton = findViewById(R.id.openTelegramButton);
        verifyButton = findViewById(R.id.verifyButton);
        verificationProgressBar = findViewById(R.id.verificationProgressBar);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        headerIcon = findViewById(R.id.headerIcon);
        
        // ══════════════════════════════════════════════════════════
        // تهيئة شاشة OTP
        // ══════════════════════════════════════════════════════════
        otpLayout = findViewById(R.id.otpLayout);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        confirmOtpButton = findViewById(R.id.confirmOtpButton);
        resendOtpButton = findViewById(R.id.resendOtpButton);
        otpProgressBar = findViewById(R.id.otpProgressBar);
        otpTimer = findViewById(R.id.otpTimer);
        otpPhoneDisplay = findViewById(R.id.otpPhoneDisplay);
        
        // ══════════════════════════════════════════════════════════
        // تهيئة شاشة إعداد الملف الشخصي
        // ══════════════════════════════════════════════════════════
        profileSetupLayout = findViewById(R.id.profileSetupLayout);
        profileImageView = findViewById(R.id.profileImageView);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        bioInputLayout = findViewById(R.id.bioInputLayout);
        bioEditText = findViewById(R.id.bioEditText);
        completeProfileButton = findViewById(R.id.completeProfileButton);
        profileProgressBar = findViewById(R.id.profileProgressBar);
        
        // ══════════════════════════════════════════════════════════
        // تهيئة الشاشة الرئيسية
        // ══════════════════════════════════════════════════════════
        mainLayout = findViewById(R.id.mainLayout);
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        newChatFab = findViewById(R.id.newChatFab);
        noChatsTextView = findViewById(R.id.noChatsTextView);
        mainProgressBar = findViewById(R.id.mainProgressBar);
        searchEditText = findViewById(R.id.searchEditText);
        searchIcon = findViewById(R.id.searchIcon);
        userProfileImage = findViewById(R.id.userProfileImage);
        userNameText = findViewById(R.id.userNameText);
        
        // إعداد RecyclerView
        setupChatsRecyclerView();
        
        // إعداد مستمعي الأحداث
        setupEventListeners();
        
        // إعداد OTP Input Auto Focus
        setupOtpAutoFocus();
    }
    
    /**
     * إعداد RecyclerView لعرض المحادثات
     */
    private void setupChatsRecyclerView() {
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsAdapter = new ChatsAdapter(chatsList, this);
        chatsRecyclerView.setAdapter(chatsAdapter);
        
        // إضافة Item Decoration للمسافات
        int spacing = getResources().getDimensionPixelSize(R.dimen.chat_item_spacing);
        chatsRecyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
    }
    
    /**
     * إعداد جميع مستمعي الأحداث
     */
    private void setupEventListeners() {
        // ══════════════════════════════════════════════════════════
        // شاشة التحقق من الهاتف
        // ══════════════════════════════════════════════════════════
        
        // زر فتح تليجرام للحصول على OTP
        openTelegramButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString().trim();
            if (validatePhoneNumber(phone)) {
                openTelegramBot();
            }
        });
        
        // زر التحقق من OTP
        verifyButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString().trim();
            if (validatePhoneNumber(phone)) {
                currentPhone = phone;
                showOtpScreen();
            }
        });
        
        // مراقبة إدخال رقم الهاتف
        phoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phoneInputLayout.setError(null);
                updateVerifyButtonState();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // ══════════════════════════════════════════════════════════
        // شاشة OTP
        // ══════════════════════════════════════════════════════════
        
        // زر تأكيد OTP
        confirmOtpButton.setOnClickListener(v -> {
            String otpCode = getEnteredOtp();
            if (otpCode.length() == 6) {
                verifyOtpCode(otpCode);
            } else {
                showToast("الرجاء إدخال رمز التحقق كامل");
            }
        });
        
        // زر إعادة إرسال OTP
        resendOtpButton.setOnClickListener(v -> {
            if (!isOtpTimerRunning) {
                resendOtp();
            }
        });
        
        // ══════════════════════════════════════════════════════════
        // شاشة إعداد الملف الشخصي
        // ══════════════════════════════════════════════════════════
        
        // زر رفع الصورة الشخصية
        uploadPhotoButton.setOnClickListener(v -> openImagePicker());
        profileImageView.setOnClickListener(v -> openImagePicker());
        
        // زر إكمال إعداد الملف الشخصي
        completeProfileButton.setOnClickListener(v -> completeProfileSetup());
        
        // مراقبة إدخال اسم المستخدم
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameInputLayout.setError(null);
                updateCompleteProfileButtonState();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // ══════════════════════════════════════════════════════════
        // الشاشة الرئيسية
        // ══════════════════════════════════════════════════════════
        
        // زر محادثة جديدة
        newChatFab.setOnClickListener(v -> showNewChatDialog());
        
        // البحث في المحادثات
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChats(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // النقر على الصورة الشخصية للمستخدم
        userProfileImage.setOnClickListener(v -> openUserProfile());
    }
    
    /**
     * إعداد نظام التركيز التلقائي لحقول OTP
     */
    private void setupOtpAutoFocus() {
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};
        
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        // الانتقال للحقل التالي
                        otpFields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        // الرجوع للحقل السابق
                        otpFields[index - 1].requestFocus();
                    }
                    
                    // تحديث حالة زر التأكيد
                    updateConfirmOtpButtonState();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // نظام المصادقة والتحقق (Authentication System)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * التحقق من حالة تسجيل الدخول
     */
    private void checkLoginStatus() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        
        if (isLoggedIn) {
            // المستخدم مسجل دخول - تحميل البيانات وعرض الشاشة الرئيسية
            loadUserData();
            showMainScreen();
        } else {
            // المستخدم غير مسجل دخول - عرض شاشة التحقق
            showVerificationScreen();
        }
    }
    
    /**
     * التحقق من صحة رقم الهاتف
     */
    private boolean validatePhoneNumber(String phone) {
        if (phone.isEmpty()) {
            phoneInputLayout.setError("الرجاء إدخال رقم الهاتف");
            return false;
        }
        
        // التحقق من التنسيق - يجب أن يبدأ بـ + ويحتوي على أرقام فقط
        if (!phone.startsWith("+")) {
            phoneInputLayout.setError("رقم الهاتف يجب أن يبدأ بـ + متبوعاً برمز الدولة");
            return false;
        }
        
        String digitsOnly = phone.substring(1).replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
            phoneInputLayout.setError("رقم الهاتف غير صحيح");
            return false;
        }
        
        phoneInputLayout.setError(null);
        return true;
    }
    
    /**
     * فتح بوت تليجرام
     */
    private void openTelegramBot() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_BOT_LINK));
            intent.setPackage("org.telegram.messenger");
            startActivity(intent);
            showToast("افتح البوت واضغط على زر 'مشاركة رقم الهاتف' للحصول على رمز التحقق");
        } catch (Exception e) {
            // تليجرام غير مثبت - فتح في المتصفح
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_BOT_LINK));
            startActivity(intent);
        }
    }
    
    /**
     * التحقق من رمز OTP
     */
    private void verifyOtpCode(String otpCode) {
        showOtpProgress(true);
        
        executorService.execute(() -> {
            supabaseManager.verifyOTP(currentPhone, otpCode, new SupabaseManager.VerificationCallback() {
                @Override
                public void onSuccess(String userId, boolean isNewUser) {
                    mainHandler.post(() -> {
                        showOtpProgress(false);
                        currentUserId = userId;
                        
                        if (isNewUser) {
                            // مستخدم جديد - الانتقال لإعداد الملف الشخصي
                            showProfileSetupScreen();
                        } else {
                            // مستخدم موجود - تسجيل الدخول مباشرة
                            completeLogin();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        showOtpProgress(false);
                        showToast("رمز التحقق غير صحيح أو منتهي الصلاحية");
                        clearOtpFields();
                    });
                }
            });
        });
    }
    
    /**
     * إعادة إرسال OTP
     */
    private void resendOtp() {
        showToast("الرجاء فتح بوت تليجرام مرة أخرى للحصول على رمز جديد");
        openTelegramBot();
        startOtpTimer();
    }
    
    /**
     * إكمال إعداد الملف الشخصي
     */
    private void completeProfileSetup() {
        String username = usernameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();
        
        if (username.isEmpty()) {
            usernameInputLayout.setError("الرجاء إدخال اسم المستخدم");
            return;
        }
        
        if (username.length() < 3) {
            usernameInputLayout.setError("اسم المستخدم يجب أن يكون 3 أحرف على الأقل");
            return;
        }
        
        showProfileProgress(true);
        
        // رفع الصورة الشخصية إذا تم اختيارها
        if (selectedImageUri != null) {
            uploadProfilePhoto(username, bio);
        } else {
            saveUserProfile(username, bio, null);
        }
    }
    
    /**
     * رفع الصورة الشخصية
     */
    private void uploadProfilePhoto(String username, String bio) {
        executorService.execute(() -> {
            supabaseManager.uploadProfileImage(currentUserId, selectedImageUri, new SupabaseManager.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    mainHandler.post(() -> saveUserProfile(username, bio, imageUrl));
                }
                
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        showProfileProgress(false);
                        showToast("حدث خطأ في رفع الصورة، حاول مرة أخرى");
                    });
                }
                
                @Override
                public void onProgress(int progress) {
                    // يمكن عرض تقدم الرفع هنا
                }
            });
        });
    }
    
    /**
     * حفظ بيانات الملف الشخصي
     */
    private void saveUserProfile(String username, String bio, String profilePicUrl) {
        executorService.execute(() -> {
            supabaseManager.createUserProfile(currentUserId, currentPhone, username, bio, profilePicUrl, 
                new SupabaseManager.DatabaseCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        mainHandler.post(() -> {
                            showProfileProgress(false);
                            currentUsername = username;
                            currentProfilePic = profilePicUrl;
                            completeLogin();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            showProfileProgress(false);
                            showToast("حدث خطأ في حفظ البيانات");
                        });
                    }
                });
        });
    }
    
    /**
     * إكمال عملية تسجيل الدخول
     */
    private void completeLogin() {
        // حفظ بيانات المستخدم في SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, currentUserId);
        editor.putString(KEY_PHONE, currentPhone);
        editor.putString(KEY_USERNAME, currentUsername);
        editor.putString(KEY_PROFILE_PIC, currentProfilePic);
        editor.apply();
        
        // عرض الشاشة الرئيسية
        showMainScreen();
        showToast("مرحباً " + currentUsername + "!");
        
        // تحميل المحادثات
        loadChats();
    }
    
    /**
     * تحميل بيانات المستخدم من SharedPreferences
     */
    private void loadUserData() {
        currentUserId = sharedPreferences.getString(KEY_USER_ID, "");
        currentPhone = sharedPreferences.getString(KEY_PHONE, "");
        currentUsername = sharedPreferences.getString(KEY_USERNAME, "");
        currentProfilePic = sharedPreferences.getString(KEY_PROFILE_PIC, "");
        
        // تحديث واجهة المستخدم
        userNameText.setText(currentUsername);
        if (currentProfilePic != null && !currentProfilePic.isEmpty()) {
            Picasso.get()
                .load(currentProfilePic)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(userProfileImage);
        }
    }
    
    /**
     * التحقق من تسجيل الدخول
     */
    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * تسجيل الخروج
     */
    private void logout() {
        new AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل أنت متأكد من تسجيل الخروج؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                // مسح البيانات المحفوظة
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                
                // إيقاف المستمعين
                stopRealtimeListeners();
                
                // العودة لشاشة التحقق
                showVerificationScreen();
                showToast("تم تسجيل الخروج بنجاح");
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // إدارة المحادثات (Chats Management)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تحميل قائمة المحادثات
     */
    private void loadChats() {
        showMainProgress(true);
        
        executorService.execute(() -> {
            supabaseManager.getUserChats(currentUserId, new SupabaseManager.ChatsCallback() {
                @Override
                public void onSuccess(List<ChatItem> chats) {
                    mainHandler.post(() -> {
                        showMainProgress(false);
                        chatsList.clear();
                        chatsList.addAll(chats);
                        chatsAdapter.notifyDataSetChanged();
                        
                        // عرض رسالة إذا لم توجد محادثات
                        if (chatsList.isEmpty()) {
                            noChatsTextView.setVisibility(View.VISIBLE);
                            chatsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noChatsTextView.setVisibility(View.GONE);
                            chatsRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        showMainProgress(false);
                        showToast("حدث خطأ في تحميل المحادثات");
                    });
                }
            });
        });
    }
    
    /**
     * تحديث قائمة المحادثات
     */
    private void refreshChatsList() {
        loadChats();
    }
    
    /**
     * البحث في المحادثات
     */
    private void filterChats(String query) {
        if (query.isEmpty()) {
            chatsAdapter.filter("");
        } else {
            chatsAdapter.filter(query);
        }
    }
    
    /**
     * عرض نافذة محادثة جديدة
     */
    private void showNewChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_chat, null);
        builder.setView(dialogView);
        
        EditText searchUserInput = dialogView.findViewById(R.id.searchUserInput);
        RecyclerView usersRecyclerView = dialogView.findViewById(R.id.usersRecyclerView);
        ProgressBar searchProgress = dialogView.findViewById(R.id.searchProgress);
        TextView noResultsText = dialogView.findViewById(R.id.noResultsText);
        
        List<UserItem> usersList = new ArrayList<>();
        UsersAdapter usersAdapter = new UsersAdapter(usersList, this);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(usersAdapter);
        
        AlertDialog dialog = builder.create();
        
        // البحث عن المستخدمين
        searchUserInput.addTextChangedListener(new TextWatcher() {
            private Handler searchHandler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= 2) {
                        searchUsers(query, usersList, usersAdapter, searchProgress, noResultsText);
                    } else {
                        usersList.clear();
                        usersAdapter.notifyDataSetChanged();
                        noResultsText.setVisibility(View.VISIBLE);
                        noResultsText.setText("ابحث عن مستخدم بالاسم أو رقم الهاتف");
                    }
                };
                
                searchHandler.postDelayed(searchRunnable, 500); // تأخير 500ms
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // عند النقر على مستخدم
        usersAdapter.setOnUserClickListener(user -> {
            dialog.dismiss();
            openChatWithUser(user);
        });
        
        dialog.show();
    }
    
    /**
     * البحث عن المستخدمين
     */
    private void searchUsers(String query, List<UserItem> usersList, UsersAdapter adapter, 
                           ProgressBar progress, TextView noResults) {
        progress.setVisibility(View.VISIBLE);
        noResults.setVisibility(View.GONE);
        
        executorService.execute(() -> {
            supabaseManager.searchUsers(query, currentUserId, new SupabaseManager.UsersCallback() {
                @Override
                public void onSuccess(List<UserItem> users) {
                    mainHandler.post(() -> {
                        progress.setVisibility(View.GONE);
                        usersList.clear();
                        usersList.addAll(users);
                        adapter.notifyDataSetChanged();
                        
                        if (users.isEmpty()) {
                            noResults.setVisibility(View.VISIBLE);
                            noResults.setText("لم يتم العثور على نتائج");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        progress.setVisibility(View.GONE);
                        noResults.setVisibility(View.VISIBLE);
                        noResults.setText("حدث خطأ في البحث");
                    });
                }
            });
        });
    }
    
    /**
     * فتح محادثة مع مستخدم
     */
    private void openChatWithUser(UserItem user) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("user_id", user.getUserId());
        intent.putExtra("username", user.getUsername());
        intent.putExtra("profile_pic", user.getProfilePic());
        intent.putExtra("bio", user.getBio());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * فتح الملف الشخصي للمستخدم الحالي
     */
    private void openUserProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // المستمعين في الوقت الفعلي (Realtime Listeners)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * بدء الاستماع للتحديثات في الوقت الفعلي
     */
    private void startRealtimeListeners() {
        supabaseManager.subscribeToNewMessages(currentUserId, new SupabaseManager.MessageListener() {
            @Override
            public void onNewMessage(MessageItem message) {
                mainHandler.post(() -> {
                    // تحديث قائمة المحادثات
                    updateChatItem(message);
                    
                    // عرض إشعار إذا كان التطبيق في المقدمة
                    showNewMessageNotification(message);
                });
            }
            
            @Override
            public void onError(String error) {
                // معالجة الأخطاء
            }
        });
    }
    
    /**
     * إيقاف المستمعين
     */
    private void stopRealtimeListeners() {
        supabaseManager.unsubscribeFromMessages();
    }
    
    /**
     * تحديث عنصر محادثة في القائمة
     */
    private void updateChatItem(MessageItem message) {
        // البحث عن المحادثة في القائمة
        for (int i = 0; i < chatsList.size(); i++) {
            ChatItem chat = chatsList.get(i);
            if (chat.getUserId().equals(message.getSenderId()) || 
                chat.getUserId().equals(message.getReceiverId())) {
                // تحديث آخر رسالة
                chat.setLastMessage(message.getContent());
                chat.setLastMessageTime(message.getTimestamp());
                chat.setUnreadCount(chat.getUnreadCount() + 1);
                
                // نقل المحادثة للأعلى
                chatsList.remove(i);
                chatsList.add(0, chat);
                chatsAdapter.notifyDataSetChanged();
                return;
            }
        }
        
        // إذا لم توجد المحادثة، تحديث القائمة
        refreshChatsList();
    }
    
    /**
     * عرض إشعار برسالة جديدة
     */
    private void showNewMessageNotification(MessageItem message) {
        // يمكن تنفيذ نظام إشعارات متقدم هنا
        // للتبسيط، نستخدم Snackbar
        Snackbar.make(mainLayout, "رسالة جديدة من " + message.getSenderName(), 
                     Snackbar.LENGTH_SHORT)
            .setAction("فتح", v -> {
                // فتح المحادثة
            })
            .show();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // إدارة الواجهات (UI Management)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * عرض شاشة التحقق من الهاتف
     */
    private void showVerificationScreen() {
        verificationLayout.setVisibility(View.VISIBLE);
        otpLayout.setVisibility(View.GONE);
        profileSetupLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.GONE);
        
        // تطبيق animation
        fadeIn(verificationLayout);
    }
    
    /**
     * عرض شاشة OTP
     */
    private void showOtpScreen() {
        verificationLayout.setVisibility(View.GONE);
        otpLayout.setVisibility(View.VISIBLE);
        profileSetupLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.GONE);
        
        // عرض رقم الهاتف
        otpPhoneDisplay.setText("تم إرسال رمز التحقق إلى\n" + currentPhone);
        
        // بدء مؤقت OTP
        startOtpTimer();
        
        // تطبيق animation
        fadeIn(otpLayout);
    }
    
    /**
     * عرض شاشة إعداد الملف الشخصي
     */
    private void showProfileSetupScreen() {
        verificationLayout.setVisibility(View.GONE);
        otpLayout.setVisibility(View.GONE);
        profileSetupLayout.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
        
        // تطبيق animation
        fadeIn(profileSetupLayout);
    }
    
    /**
     * عرض الشاشة الرئيسية
     */
    private void showMainScreen() {
        verificationLayout.setVisibility(View.GONE);
        otpLayout.setVisibility(View.GONE);
        profileSetupLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
        
        // تطبيق animation
        fadeIn(mainLayout);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // مساعدات الواجهة (UI Helpers)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تحديث حالة زر التحقق
     */
    private void updateVerifyButtonState() {
        String phone = phoneEditText.getText().toString().trim();
        verifyButton.setEnabled(!phone.isEmpty() && phone.startsWith("+"));
    }
    
    /**
     * تحديث حالة زر تأكيد OTP
     */
    private void updateConfirmOtpButtonState() {
        String otp = getEnteredOtp();
        confirmOtpButton.setEnabled(otp.length() == 6);
    }
    
    /**
     * تحديث حالة زر إكمال الملف الشخصي
     */
    private void updateCompleteProfileButtonState() {
        String username = usernameEditText.getText().toString().trim();
        completeProfileButton.setEnabled(username.length() >= 3);
    }
    
    /**
     * الحصول على رمز OTP المُدخل
     */
    private String getEnteredOtp() {
        return otp1.getText().toString() +
               otp2.getText().toString() +
               otp3.getText().toString() +
               otp4.getText().toString() +
               otp5.getText().toString() +
               otp6.getText().toString();
    }
    
    /**
     * مسح حقول OTP
     */
    private void clearOtpFields() {
        otp1.setText("");
        otp2.setText("");
        otp3.setText("");
        otp4.setText("");
        otp5.setText("");
        otp6.setText("");
        otp1.requestFocus();
    }
    
    /**
     * بدء مؤقت OTP
     */
    private void startOtpTimer() {
        isOtpTimerRunning = true;
        otpTimerSeconds = 120;
        resendOtpButton.setEnabled(false);
        
        Handler timerHandler = new Handler(Looper.getMainLooper());
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (otpTimerSeconds > 0) {
                    int minutes = otpTimerSeconds / 60;
                    int seconds = otpTimerSeconds % 60;
                    otpTimer.setText(String.format(Locale.getDefault(), 
                                    "إعادة الإرسال بعد %d:%02d", minutes, seconds));
                    otpTimerSeconds--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    isOtpTimerRunning = false;
                    otpTimer.setText("يمكنك الآن إعادة طلب الرمز");
                    resendOtpButton.setEnabled(true);
                }
            }
        });
    }
    
    /**
     * عرض/إخفاء تقدم التحقق
     */
    private void showVerificationProgress(boolean show) {
        verificationProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        openTelegramButton.setEnabled(!show);
        verifyButton.setEnabled(!show);
        phoneEditText.setEnabled(!show);
    }
    
    /**
     * عرض/إخفاء تقدم OTP
     */
    private void showOtpProgress(boolean show) {
        otpProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        confirmOtpButton.setEnabled(!show);
        resendOtpButton.setEnabled(!show && !isOtpTimerRunning);
    }
    
    /**
     * عرض/إخفاء تقدم الملف الشخصي
     */
    private void showProfileProgress(boolean show) {
        profileProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        uploadPhotoButton.setEnabled(!show);
        usernameEditText.setEnabled(!show);
        bioEditText.setEnabled(!show);
        completeProfileButton.setEnabled(!show);
    }
    
    /**
     * عرض/إخفاء تقدم الشاشة الرئيسية
     */
    private void showMainProgress(boolean show) {
        mainProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * تطبيق تأثير Fade In
     */
    private void fadeIn(View view) {
        view.setAlpha(0f);
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    /**
     * عرض رسالة Toast
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // معالجة اختيار الصور (Image Picker)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * فتح محدد الصور
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // الأذونات (Permissions)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * طلب الأذونات الضرورية
     */
    private void requestNecessaryPermissions() {
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        };
        
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                showToast("بعض الأذونات مطلوبة لعمل التطبيق بشكل صحيح");
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Adapter للمحادثات (Chats Adapter)
    // ═══════════════════════════════════════════════════════════════
    
    private class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {
        private List<ChatItem> chatsList;
        private List<ChatItem> chatsListFull;
        private Activity activity;
        
        public ChatsAdapter(List<ChatItem> chatsList, Activity activity) {
            this.chatsList = chatsList;
            this.chatsListFull = new ArrayList<>(chatsList);
            this.activity = activity;
        }
        
        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatItem chat = chatsList.get(position);
            
            holder.usernameText.setText(chat.getUsername());
            holder.lastMessageText.setText(chat.getLastMessage());
            holder.timeText.setText(formatTime(chat.getLastMessageTime()));
            
            // عرض عدد الرسائل غير المقروءة
            if (chat.getUnreadCount() > 0) {
                holder.unreadBadge.setVisibility(View.VISIBLE);
                holder.unreadBadge.setText(String.valueOf(chat.getUnreadCount()));
            } else {
                holder.unreadBadge.setVisibility(View.GONE);
            }
            
            // تحميل الصورة الشخصية
            if (chat.getProfilePic() != null && !chat.getProfilePic().isEmpty()) {
                Picasso.get()
                    .load(chat.getProfilePic())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.default_avatar);
            }
            
            // النقر على المحادثة
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(activity, ChatActivity.class);
                intent.putExtra("user_id", chat.getUserId());
                intent.putExtra("username", chat.getUsername());
                intent.putExtra("profile_pic", chat.getProfilePic());
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        
        @Override
        public int getItemCount() {
            return chatsList.size();
        }
        
        public void filter(String query) {
            chatsList.clear();
            if (query.isEmpty()) {
                chatsList.addAll(chatsListFull);
            } else {
                String lowerQuery = query.toLowerCase();
                for (ChatItem chat : chatsListFull) {
                    if (chat.getUsername().toLowerCase().contains(lowerQuery) ||
                        chat.getLastMessage().toLowerCase().contains(lowerQuery)) {
                        chatsList.add(chat);
                    }
                }
            }
            notifyDataSetChanged();
        }
        
        class ChatViewHolder extends RecyclerView.ViewHolder {
            CircleImageView profileImage;
            TextView usernameText;
            TextView lastMessageText;
            TextView timeText;
            TextView unreadBadge;
            
            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                usernameText = itemView.findViewById(R.id.usernameText);
                lastMessageText = itemView.findViewById(R.id.lastMessageText);
                timeText = itemView.findViewById(R.id.timeText);
                unreadBadge = itemView.findViewById(R.id.unreadBadge);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Adapter للمستخدمين (Users Adapter)
    // ═══════════════════════════════════════════════════════════════
    
    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
        private List<UserItem> usersList;
        private Activity activity;
        private OnUserClickListener listener;
        
        public UsersAdapter(List<UserItem> usersList, Activity activity) {
            this.usersList = usersList;
            this.activity = activity;
        }
        
        public void setOnUserClickListener(OnUserClickListener listener) {
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserItem user = usersList.get(position);
            
            holder.usernameText.setText(user.getUsername());
            holder.bioText.setText(user.getBio());
            
            // تحميل الصورة الشخصية
            if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
                Picasso.get()
                    .load(user.getProfilePic())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.default_avatar);
            }
            
            // النقر على المستخدم
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return usersList.size();
        }
        
        class UserViewHolder extends RecyclerView.ViewHolder {
            CircleImageView profileImage;
            TextView usernameText;
            TextView bioText;
            
            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                usernameText = itemView.findViewById(R.id.usernameText);
                bioText = itemView.findViewById(R.id.bioText);
            }
        }
    }
    
    interface OnUserClickListener {
        void onUserClick(UserItem user);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Item Decoration للمسافات
    // ═══════════════════════════════════════════════════════════════
    
    private class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spacing;
        
        public SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }
        
        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                  @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = spacing;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // مساعدات عامة (General Helpers)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تنسيق الوقت لعرضه في قائمة المحادثات
     */
    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // أقل من دقيقة
        if (diff < 60000) {
            return "الآن";
        }
        
        // أقل من ساعة
        if (diff < 3600000) {
            int minutes = (int) (diff / 60000);
            return minutes + " د";
        }
        
        // أقل من يوم
        if (diff < 86400000) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        
        // أقل من أسبوع
        if (diff < 604800000) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        
        // أكثر من أسبوع
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

// ═══════════════════════════════════════════════════════════════════
// نماذج البيانات (Data Models)
// ═══════════════════════════════════════════════════════════════════

/**
 * نموذج عنصر المحادثة
 */
class ChatItem {
    private String chatId;
    private String userId;
    private String username;
    private String profilePic;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    
    public ChatItem(String chatId, String userId, String username, String profilePic,
                   String lastMessage, long lastMessageTime, int unreadCount) {
        this.chatId = chatId;
        this.userId = userId;
        this.username = username;
        this.profilePic = profilePic;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }
    
    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }
    
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    
    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}

/**
 * نموذج عنصر المستخدم
 */
class UserItem {
    private String userId;
    private String username;
    private String phone;
    private String profilePic;
    private String bio;
    private boolean isOnline;
    private long lastSeen;
    
    public UserItem(String userId, String username, String phone, String profilePic, 
                   String bio, boolean isOnline, long lastSeen) {
        this.userId = userId;
        this.username = username;
        this.phone = phone;
        this.profilePic = profilePic;
        this.bio = bio;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}

/**
 * نموذج الرسالة
 */
class MessageItem {
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String content;
    private String type; // text, image, video, audio, file
    private String mediaUrl;
    private long timestamp;
    private boolean isRead;
    private boolean isSent;
    private boolean isDelivered;
    
    public MessageItem(String messageId, String senderId, String senderName, String receiverId,
                      String content, String type, String mediaUrl, long timestamp,
                      boolean isRead, boolean isSent, boolean isDelivered) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
        this.mediaUrl = mediaUrl;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.isSent = isSent;
        this.isDelivered = isDelivered;
    }
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }
    
    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }
}
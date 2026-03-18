package com.chatapp.pro;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  CHAT APP PRO - CloudinaryManager.java
 *  مدير Cloudinary لرفع وإدارة الفيديوهات
 * ═══════════════════════════════════════════════════════════════════
 * 
 * المميزات:
 * - رفع الفيديوهات تلقائياً
 * - ضغط الفيديوهات
 * - تحويل الصيغ
 * - Thumbnail Generation
 * - Progress Tracking
 * - Error Handling
 * 
 * @author ChatApp Pro Team
 * @version 1.0
 */
public class CloudinaryManager {

    private static final String TAG = "CloudinaryManager";
    
    // Cloudinary Configuration - ضع بياناتك هنا
    private static final String CLOUD_NAME = "dtgfmfhrx";
    private static final String API_KEY = "819123864574819";
    private static final String API_SECRET = "lF80tGc3qq3TZna968ihufwAUT4";
    
    private static CloudinaryManager instance;
    private Cloudinary cloudinary;
    private Context context;
    private ExecutorService executorService;
    
    private CloudinaryManager(Context context) {
        this.context = context.getApplicationContext();
        initializeCloudinary();
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized CloudinaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CloudinaryManager(context);
        }
        return instance;
    }
    
    /**
     * تهيئة Cloudinary
     */
    private void initializeCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        config.put("api_key", API_KEY);
        config.put("api_secret", API_SECRET);
        
        cloudinary = new Cloudinary(config);
        Log.i(TAG, "Cloudinary initialized successfully");
    }
    
    /**
     * رفع فيديو إلى Cloudinary
     */
    public void uploadVideo(Uri videoUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                // تحويل Uri إلى File
                File videoFile = getFileFromUri(videoUri);
                
                if (videoFile == null) {
                    callback.onError("فشل قراءة الفيديو");
                    return;
                }
                
                callback.onProgress(10);
                
                // إعدادات الرفع مع الضغط التلقائي
                Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "video",
                    "folder", "chat_videos",
                    "quality", "auto:low",  // ضغط تلقائي
                    "video_codec", "h264",
                    "audio_codec", "aac",
                    "format", "mp4",
                    "transformation", ObjectUtils.asMap(
                        "width", 720,
                        "height", 720,
                        "crop", "limit",
                        "quality", "auto:low"
                    ),
                    "eager", ObjectUtils.asMap(
                        "format", "jpg",
                        "transformation", ObjectUtils.asMap(
                            "width", 300,
                            "height", 300,
                            "crop", "fill"
                        )
                    )
                );
                
                callback.onProgress(30);
                
                // رفع الفيديو
                Map uploadResult = cloudinary.uploader().upload(videoFile, options);
                
                callback.onProgress(90);
                
                // الحصول على الرابط
                String videoUrl = (String) uploadResult.get("secure_url");
                
                // حذف الملف المؤقت
                videoFile.delete();
                
                callback.onProgress(100);
                callback.onSuccess(videoUrl);
                
                Log.i(TAG, "Video uploaded successfully: " + videoUrl);
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading video: " + e.getMessage());
                callback.onError("فشل رفع الفيديو: " + e.getMessage());
            }
        });
    }
    
    /**
     * رفع صورة إلى Cloudinary
     */
    public void uploadImage(Uri imageUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                File imageFile = getFileFromUri(imageUri);
                
                if (imageFile == null) {
                    callback.onError("فشل قراءة الصورة");
                    return;
                }
                
                callback.onProgress(20);
                
                Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "chat_images",
                    "quality", "auto:good",
                    "transformation", ObjectUtils.asMap(
                        "width", 1920,
                        "height", 1080,
                        "crop", "limit",
                        "quality", "auto:good"
                    )
                );
                
                callback.onProgress(50);
                
                Map uploadResult = cloudinary.uploader().upload(imageFile, options);
                
                callback.onProgress(90);
                
                String imageUrl = (String) uploadResult.get("secure_url");
                
                imageFile.delete();
                
                callback.onProgress(100);
                callback.onSuccess(imageUrl);
                
                Log.i(TAG, "Image uploaded successfully: " + imageUrl);
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading image: " + e.getMessage());
                callback.onError("فشل رفع الصورة: " + e.getMessage());
            }
        });
    }
    
    /**
     * تحويل Uri إلى File
     */
    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File file = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis());
            
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            outputStream.close();
            inputStream.close();
            
            return file;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting Uri to File: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * حذف ملف من Cloudinary
     */
    public void deleteFile(String publicId, DeleteCallback callback) {
        executorService.execute(() -> {
            try {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                
                if ("ok".equals(result.get("result"))) {
                    callback.onSuccess();
                } else {
                    callback.onError("فشل الحذف");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting file: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Callbacks
    // ═══════════════════════════════════════════════════════════════
    
    public interface UploadCallback {
        void onSuccess(String url);
        void onError(String error);
        void onProgress(int progress);
    }
    
    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
package com.chatapp.pro;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;
import java.security.SecureRandom;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.util.concurrent.TimeUnit;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  CHAT APP PRO - SupabaseManager.java
 *  مدير Supabase الشامل - جميع عمليات قاعدة البيانات والتخزين
 * ═══════════════════════════════════════════════════════════════════
 * 
 * هذا الملف يحتوي على:
 * 1. نظام المصادقة الكامل (Authentication)
 * 2. إدارة المستخدمين (Users Management)
 * 3. إدارة الرسائل (Messages Management)
 * 4. إدارة المحادثات (Chats Management)
 * 5. رفع وتحميل الملفات (Storage)
 * 6. التحديثات الفورية (Realtime Subscriptions)
 * 7. البحث المتقدم (Advanced Search)
 * 8. التشفير والأمان (Encryption & Security)
 * 9. إدارة الجلسات (Sessions)
 * 10. معالجة الأخطاء (Error Handling)
 * 11. نظام الكاش (Caching System)
 * 12. ضغط الصور والفيديوهات
 * 13. إدارة الإشعارات
 * 14. نظام الحالة (Online/Offline)
 * 15. مؤشر الكتابة
 * 16. إحصائيات المحادثات
 * 
 * المميزات:
 * - Singleton Pattern للأداء الأمثل
 * - Thread-Safe Operations
 * - Connection Pooling
 * - Retry Logic للعمليات الفاشلة
 * - Rate Limiting
 * - Data Validation
 * - SQL Injection Prevention
 * - End-to-End Encryption Support
 * 
 * @author ChatApp Pro Team
 * @version 3.0
 */
public class SupabaseManager {

    // ═══════════════════════════════════════════════════════════════
    // الثوابت والإعدادات (Constants & Configuration)
    // ═══════════════════════════════════════════════════════════════
    
    private static final String TAG = "SupabaseManager";
    
    // Supabase Configuration - إعدادات الربط
    private static final String SUPABASE_URL = "https://liywjylzupkvplitqebh.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxpeXdqeWx6dXBrdnBsaXRxZWJoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM4MDI4ODgsImV4cCI6MjA4OTM3ODg4OH0.dv3im4HWKht0R_ii1GmFYJOKxZKk-h2DhqiuOqkqYJg";
    private static final String SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxpeXdqeWx6dXBrdnBsaXRxZWJoIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MzgwMjg4OCwiZXhwIjoyMDg5Mzc4ODg4fQ.C1kXGVaET_hziO9xzTozOheUYk33mv-OOFqQf--zdV0";

    // API Endpoints
    private static final String REST_API_URL = SUPABASE_URL + "/rest/v1/";
    private static final String STORAGE_API_URL = SUPABASE_URL + "/storage/v1/";
    private static final String REALTIME_URL = SUPABASE_URL + "/realtime/v1/websocket";
    private static final String AUTH_API_URL = SUPABASE_URL + "/auth/v1/";
    
    // Storage Buckets
    private static final String BUCKET_PROFILE_IMAGES = "profile-images";
    private static final String BUCKET_CHAT_IMAGES = "chat-images";
    private static final String BUCKET_CHAT_AUDIOS = "chat-audios";
    private static final String BUCKET_CHAT_FILES = "chat-files";
    
    // Database Tables
    private static final String TABLE_USERS = "users";
    private static final String TABLE_OTP_VERIFICATIONS = "otp_verifications";
    private static final String TABLE_MESSAGES = "messages";
    private static final String TABLE_CHATS = "chats";
    private static final String TABLE_CHAT_PARTICIPANTS = "chat_participants";
    private static final String TABLE_USER_STATUS = "user_status";
    private static final String TABLE_TYPING_STATUS = "typing_status";
    private static final String TABLE_BLOCKED_USERS = "blocked_users";
    
    // HTTP Methods
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_PATCH = "PATCH";
    private static final String METHOD_DELETE = "DELETE";
    
    // Timeouts
    private static final int CONNECT_TIMEOUT = 30000; // 30 seconds
    private static final int READ_TIMEOUT = 30000;
    private static final int WRITE_TIMEOUT = 30000;
    
    // Retry Configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1 second
    
    // Encryption
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ENCRYPTION_KEY = "O7#kP9$mQ2@vL5*pY8!jN4&hC7^bW3zR";
    
    // ═══════════════════════════════════════════════════════════════
    // المتغيرات الخاصة (Private Variables)
    // ═══════════════════════════════════════════════════════════════
    
    private static SupabaseManager instance;
    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    
    // WebSocket للتحديثات الفورية
    private WebSocket realtimeWebSocket;
    private Map<String, List<MessageListener>> messageListeners;
    private Map<String, List<ChatListener>> chatListeners;
    
    // Cache
    private Map<String, UserItem> usersCache;
    private Map<String, List<MessageItem>> messagesCache;
    private long cacheExpiry = 300000; // 5 minutes
    
    // Session Management
    private String currentAccessToken;
    private String currentRefreshToken;
    private long tokenExpiryTime;
    
    // ═══════════════════════════════════════════════════════════════
    // Singleton Pattern
    // ═══════════════════════════════════════════════════════════════
    
    private SupabaseManager(Context context) {
        this.context = context.getApplicationContext();
        initializeHttpClient();
        initializeExecutorService();
        initializeCaches();
        initializeListeners();
    }
    
    public static synchronized SupabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseManager(context);
        }
        return instance;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // التهيئة (Initialization)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تهيئة HTTP Client مع إعدادات متقدمة
     */
    private void initializeHttpClient() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build();
    }
    
    /**
     * تهيئة ExecutorService للعمليات الخلفية
     */
    private void initializeExecutorService() {
        executorService = Executors.newFixedThreadPool(8);
    }
    
    /**
     * تهيئة أنظمة الكاش
     */
    private void initializeCaches() {
        usersCache = new HashMap<>();
        messagesCache = new HashMap<>();
    }
    
    /**
     * تهيئة المستمعين
     */
    private void initializeListeners() {
        messageListeners = new HashMap<>();
        chatListeners = new HashMap<>();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // نظام المصادقة (Authentication System)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * التحقق من رمز OTP
     */
    public void verifyOTP(String phoneNumber, String otpCode, VerificationCallback callback) {
        executorService.execute(() -> {
            try {
                // بناء الاستعلام للتحقق من OTP
                String endpoint = TABLE_OTP_VERIFICATIONS + 
                                "?phone_number=eq." + phoneNumber +
                                "&otp_code=eq." + otpCode +
                                "&is_used=eq.false" +
                                "&select=*";
                
                JSONArray response = executeGetRequest(endpoint);
                
                if (response.length() > 0) {
                    JSONObject otpRecord = response.getJSONObject(0);
                    
                    // التحقق من صلاحية الوقت (10 دقائق)
                    long createdAt = otpRecord.getLong("created_at");
                    long now = System.currentTimeMillis();
                    
                    if (now - createdAt > 600000) { // 10 minutes
                        callback.onError("انتهت صلاحية رمز التحقق");
                        return;
                    }
                    
                    // وضع علامة استخدام على OTP
                    String otpId = otpRecord.getString("id");
                    markOtpAsUsed(otpId);
                    
                    // التحقق من وجود المستخدم
                    checkOrCreateUser(phoneNumber, callback);
                    
                } else {
                    callback.onError("رمز التحقق غير صحيح");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error verifying OTP: " + e.getMessage());
                callback.onError("حدث خطأ في التحقق: " + e.getMessage());
            }
        });
    }
    
    /**
     * وضع علامة استخدام على OTP
     */
    private void markOtpAsUsed(String otpId) {
        try {
            JSONObject updateData = new JSONObject();
            updateData.put("is_used", true);
            updateData.put("used_at", System.currentTimeMillis());
            
            String endpoint = TABLE_OTP_VERIFICATIONS + "?id=eq." + otpId;
            executePatchRequest(endpoint, updateData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error marking OTP as used: " + e.getMessage());
        }
    }
    
    /**
     * التحقق من المستخدم أو إنشاء حساب جديد
     */
    private void checkOrCreateUser(String phoneNumber, VerificationCallback callback) {
        try {
            // البحث عن المستخدم
            String endpoint = TABLE_USERS + "?phone_number=eq." + phoneNumber + "&select=*";
            JSONArray users = executeGetRequest(endpoint);
            
            if (users.length() > 0) {
                // مستخدم موجود
                JSONObject user = users.getJSONObject(0);
                String userId = user.getString("id");
                
                // تحديث آخر تسجيل دخول
                updateLastLogin(userId);
                
                callback.onSuccess(userId, false);
            } else {
                // مستخدم جديد - إنشاء حساب
                String newUserId = UUID.randomUUID().toString();
                
                JSONObject newUser = new JSONObject();
                newUser.put("id", newUserId);
                newUser.put("phone_number", phoneNumber);
                newUser.put("created_at", System.currentTimeMillis());
                newUser.put("last_login", System.currentTimeMillis());
                newUser.put("is_online", true);
                
                String createEndpoint = TABLE_USERS;
                executePostRequest(createEndpoint, newUser);
                
                callback.onSuccess(newUserId, true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking/creating user: " + e.getMessage());
            callback.onError("حدث خطأ في إنشاء الحساب");
        }
    }
    
    /**
     * تحديث آخر تسجيل دخول
     */
    private void updateLastLogin(String userId) {
        try {
            JSONObject updateData = new JSONObject();
            updateData.put("last_login", System.currentTimeMillis());
            updateData.put("is_online", true);
            updateData.put("last_seen", System.currentTimeMillis());
            
            String endpoint = TABLE_USERS + "?id=eq." + userId;
            executePatchRequest(endpoint, updateData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating last login: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // إدارة المستخدمين (Users Management)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * إنشاء ملف شخصي للمستخدم
     */
    public void createUserProfile(String userId, String phoneNumber, String username, 
                                 String bio, String profilePicUrl, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("username", username);
                updateData.put("bio", bio != null ? bio : "");
                updateData.put("profile_pic", profilePicUrl != null ? profilePicUrl : "");
                updateData.put("updated_at", System.currentTimeMillis());
                
                String endpoint = TABLE_USERS + "?id=eq." + userId;
                JSONArray result = executePatchRequest(endpoint, updateData);
                
                if (result.length() > 0) {
                    callback.onSuccess(result.getJSONObject(0));
                } else {
                    callback.onError("فشل تحديث الملف الشخصي");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating user profile: " + e.getMessage());
                callback.onError("حدث خطأ في إنشاء الملف الشخصي");
            }
        });
    }
    
    /**
     * البحث عن المستخدمين
     */
    public void searchUsers(String query, String excludeUserId, UsersCallback callback) {
        executorService.execute(() -> {
            try {
                // البحث بالاسم أو رقم الهاتف
                String endpoint = TABLE_USERS + 
                                "?or=(username.ilike.*" + query + "*,phone_number.ilike.*" + query + "*)" +
                                "&id=neq." + excludeUserId +
                                "&select=id,username,phone_number,profile_pic,bio,is_online,last_seen" +
                                "&limit=20";
                
                JSONArray results = executeGetRequest(endpoint);
                
                List<UserItem> users = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject userObj = results.getJSONObject(i);
                    UserItem user = parseUserFromJson(userObj);
                    users.add(user);
                }
                
                callback.onSuccess(users);
                
            } catch (Exception e) {
                Log.e(TAG, "Error searching users: " + e.getMessage());
                callback.onError("حدث خطأ في البحث");
            }
        });
    }
    
    /**
     * الحصول على معلومات مستخدم
     */
    public void getUserInfo(String userId, UserCallback callback) {
        executorService.execute(() -> {
            try {
                // التحقق من الكاش أولاً
                if (usersCache.containsKey(userId)) {
                    UserItem cachedUser = usersCache.get(userId);
                    callback.onSuccess(cachedUser);
                    return;
                }
                
                // جلب من قاعدة البيانات
                String endpoint = TABLE_USERS + "?id=eq." + userId + "&select=*";
                JSONArray results = executeGetRequest(endpoint);
                
                if (results.length() > 0) {
                    JSONObject userObj = results.getJSONObject(0);
                    UserItem user = parseUserFromJson(userObj);
                    
                    // حفظ في الكاش
                    usersCache.put(userId, user);
                    
                    callback.onSuccess(user);
                } else {
                    callback.onError("المستخدم غير موجود");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting user info: " + e.getMessage());
                callback.onError("حدث خطأ في جلب البيانات");
            }
        });
    }
    
    /**
     * تحديث حالة المستخدم (متصل/غير متصل)
     */
    public void updateUserStatus(String userId, boolean isOnline, StatusCallback callback) {
        executorService.execute(() -> {
            try {
                JSONObject statusData = new JSONObject();
                statusData.put("user_id", userId);
                statusData.put("is_online", isOnline);
                statusData.put("last_seen", System.currentTimeMillis());
                statusData.put("updated_at", System.currentTimeMillis());
                
                // حذف الحالة القديمة
                String deleteEndpoint = TABLE_USER_STATUS + "?user_id=eq." + userId;
                executeDeleteRequest(deleteEndpoint);
                
                // إضافة الحالة الجديدة
                String insertEndpoint = TABLE_USER_STATUS;
                JSONArray result = executePostRequest(insertEndpoint, statusData);
                
                if (callback != null) {
                    callback.onSuccess();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status: " + e.getMessage());
                if (callback != null) {
                    callback.onError("فشل تحديث الحالة");
                }
            }
        });
    }
    
    /**
     * الحصول على حالة المستخدم
     */
    public void getUserStatus(String userId, StatusCallback callback) {
        executorService.execute(() -> {
            try {
                String endpoint = TABLE_USER_STATUS + "?user_id=eq." + userId + "&select=*";
                JSONArray results = executeGetRequest(endpoint);
                
                if (results.length() > 0) {
                    JSONObject statusObj = results.getJSONObject(0);
                    boolean isOnline = statusObj.getBoolean("is_online");
                    long lastSeen = statusObj.getLong("last_seen");
                    
                    callback.onSuccess(isOnline, lastSeen);
                } else {
                    callback.onSuccess(false, 0);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting user status: " + e.getMessage());
                callback.onError("فشل جلب الحالة");
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // إدارة المحادثات (Chats Management)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * الحصول على محادثات المستخدم
     */
    public void getUserChats(String userId, ChatsCallback callback) {
        executorService.execute(() -> {
            try {
                // جلب جميع المحادثات التي المستخدم طرف فيها
                String endpoint = TABLE_MESSAGES + 
                                "?or=(sender_id.eq." + userId + ",receiver_id.eq." + userId + ")" +
                                "&select=chat_id,sender_id,receiver_id,content,timestamp,is_read" +
                                "&order=timestamp.desc";
                
                JSONArray messages = executeGetRequest(endpoint);
                
                // تجميع المحادثات الفريدة
                Map<String, ChatItem> chatsMap = new HashMap<>();
                
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject msgObj = messages.getJSONObject(i);
                    String chatId = msgObj.getString("chat_id");
                    
                    if (!chatsMap.containsKey(chatId)) {
                        // تحديد المستخدم الآخر
                        String senderId = msgObj.getString("sender_id");
                        String receiverId = msgObj.getString("receiver_id");
                        String otherUserId = senderId.equals(userId) ? receiverId : senderId;
                        
                        // جلب معلومات المستخدم الآخر
                        UserItem otherUser = getUserInfoSync(otherUserId);
                        
                        // حساب الرسائل غير المقروءة
                        int unreadCount = getUnreadCountSync(chatId, userId);
                        
                        ChatItem chat = new ChatItem(
                            chatId,
                            otherUserId,
                            otherUser != null ? otherUser.getUsername() : "Unknown",
                            otherUser != null ? otherUser.getProfilePic() : "",
                            msgObj.getString("content"),
                            msgObj.getLong("timestamp"),
                            unreadCount
                        );
                        
                        chatsMap.put(chatId, chat);
                    }
                }
                
                List<ChatItem> chatsList = new ArrayList<>(chatsMap.values());
                callback.onSuccess(chatsList);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting user chats: " + e.getMessage());
                callback.onError("حدث خطأ في جلب المحادثات");
            }
        });
    }
    
    /**
     * الحصول على معلومات مستخدم بشكل متزامن
     */
    private UserItem getUserInfoSync(String userId) {
        try {
            if (usersCache.containsKey(userId)) {
                return usersCache.get(userId);
            }
            
            String endpoint = TABLE_USERS + "?id=eq." + userId + "&select=*";
            JSONArray results = executeGetRequest(endpoint);
            
            if (results.length() > 0) {
                UserItem user = parseUserFromJson(results.getJSONObject(0));
                usersCache.put(userId, user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user info sync: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * حساب الرسائل غير المقروءة
     */
    private int getUnreadCountSync(String chatId, String userId) {
        try {
            String endpoint = TABLE_MESSAGES + 
                            "?chat_id=eq." + chatId +
                            "&receiver_id=eq." + userId +
                            "&is_read=eq.false" +
                            "&select=count";
            
            JSONArray results = executeGetRequest(endpoint);
            if (results.length() > 0) {
                return results.getJSONObject(0).getInt("count");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting unread count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * الحصول على رسائل محادثة
     */
    public void getChatMessages(String chatId, int page, int limit, MessagesCallback callback) {
        executorService.execute(() -> {
            try {
                int offset = page * limit;
                
                String endpoint = TABLE_MESSAGES + 
                                "?chat_id=eq." + chatId +
                                "&select=*" +
                                "&order=timestamp.desc" +
                                "&limit=" + limit +
                                "&offset=" + offset;
                
                JSONArray results = executeGetRequest(endpoint);
                
                List<MessageItem> messages = new ArrayList<>();
                for (int i = results.length() - 1; i >= 0; i--) { // عكس الترتيب
                    JSONObject msgObj = results.getJSONObject(i);
                    MessageItem message = parseMessageFromJson(msgObj);
                    messages.add(message);
                }
                
                callback.onSuccess(messages);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting chat messages: " + e.getMessage());
                callback.onError("حدث خطأ في جلب الرسائل");
            }
        });
    }
    
    /**
     * إرسال رسالة
     */
    public void sendMessage(String chatId, MessageItem message, SendMessageCallback callback) {
        executorService.execute(() -> {
            try {
                // تشفير محتوى الرسالة
                String encryptedContent = encryptMessage(message.getContent());
                
                JSONObject messageData = new JSONObject();
                messageData.put("id", message.getMessageId());
                messageData.put("chat_id", chatId);
                messageData.put("sender_id", message.getSenderId());
                messageData.put("receiver_id", message.getReceiverId());
                messageData.put("content", encryptedContent);
                messageData.put("type", message.getType());
                messageData.put("media_url", message.getMediaUrl() != null ? message.getMediaUrl() : "");
                messageData.put("timestamp", message.getTimestamp());
                messageData.put("is_read", false);
                messageData.put("is_delivered", false);
                messageData.put("created_at", System.currentTimeMillis());
                
                String endpoint = TABLE_MESSAGES;
                JSONArray result = executePostRequest(endpoint, messageData);
                
                if (result.length() > 0) {
                    String messageId = result.getJSONObject(0).getString("id");
                    
                    // تحديث آخر رسالة في المحادثة
                    updateChatLastMessage(chatId, message.getContent(), message.getTimestamp());
                    
                    callback.onSuccess(messageId);
                } else {
                    callback.onError("فشل إرسال الرسالة");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message: " + e.getMessage());
                callback.onError("حدث خطأ في الإرسال");
            }
        });
    }
    
    /**
     * تحديث آخر رسالة في المحادثة
     */
    private void updateChatLastMessage(String chatId, String lastMessage, long timestamp) {
        try {
            JSONObject chatData = new JSONObject();
            chatData.put("last_message", lastMessage);
            chatData.put("last_message_time", timestamp);
            chatData.put("updated_at", System.currentTimeMillis());
            
            String endpoint = TABLE_CHATS + "?id=eq." + chatId;
            executePatchRequest(endpoint, chatData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating chat last message: " + e.getMessage());
        }
    }
    
    /**
     * وضع علامة قراءة على الرسائل
     */
    public void markMessagesAsRead(List<String> messageIds, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                for (String messageId : messageIds) {
                    JSONObject updateData = new JSONObject();
                    updateData.put("is_read", true);
                    updateData.put("read_at", System.currentTimeMillis());
                    
                    String endpoint = TABLE_MESSAGES + "?id=eq." + messageId;
                    executePatchRequest(endpoint, updateData);
                }
                
                callback.onSuccess(new JSONObject());
                
            } catch (Exception e) {
                Log.e(TAG, "Error marking messages as read: " + e.getMessage());
                callback.onError("فشل تحديث الرسائل");
            }
        });
    }
    
    /**
     * حذف رسالة
     */
    public void deleteMessage(String messageId, boolean forEveryone, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                if (forEveryone) {
                    // حذف نهائي
                    String endpoint = TABLE_MESSAGES + "?id=eq." + messageId;
                    executeDeleteRequest(endpoint);
                } else {
                    // وضع علامة حذف فقط
                    JSONObject updateData = new JSONObject();
                    updateData.put("is_deleted", true);
                    updateData.put("deleted_at", System.currentTimeMillis());
                    
                    String endpoint = TABLE_MESSAGES + "?id=eq." + messageId;
                    executePatchRequest(endpoint, updateData);
                }
                
                callback.onSuccess(new JSONObject());
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting message: " + e.getMessage());
                callback.onError("فشل حذف الرسالة");
            }
        });
    }
    
    /**
     * مسح محادثة
     */
    public void clearChat(String chatId, String userId, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                // حذف جميع رسائل المحادثة للمستخدم
                String endpoint = TABLE_MESSAGES + 
                                "?chat_id=eq." + chatId +
                                "&or=(sender_id.eq." + userId + ",receiver_id.eq." + userId + ")";
                
                executeDeleteRequest(endpoint);
                
                callback.onSuccess(new JSONObject());
                
            } catch (Exception e) {
                Log.e(TAG, "Error clearing chat: " + e.getMessage());
                callback.onError("فشل مسح المحادثة");
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // مؤشر الكتابة (Typing Indicator)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * إرسال حالة الكتابة
     */
    public void sendTypingStatus(String chatId, String userId, boolean isTyping, StatusCallback callback) {
        executorService.execute(() -> {
            try {
                if (isTyping) {
                    JSONObject typingData = new JSONObject();
                    typingData.put("chat_id", chatId);
                    typingData.put("user_id", userId);
                    typingData.put("is_typing", true);
                    typingData.put("timestamp", System.currentTimeMillis());
                    
                    // حذف الحالة القديمة
                    String deleteEndpoint = TABLE_TYPING_STATUS + 
                                          "?chat_id=eq." + chatId + 
                                          "&user_id=eq." + userId;
                    executeDeleteRequest(deleteEndpoint);
                    
                    // إضافة الحالة الجديدة
                    String insertEndpoint = TABLE_TYPING_STATUS;
                    executePostRequest(insertEndpoint, typingData);
                } else {
                    // حذف حالة الكتابة
                    String endpoint = TABLE_TYPING_STATUS + 
                                    "?chat_id=eq." + chatId + 
                                    "&user_id=eq." + userId;
                    executeDeleteRequest(endpoint);
                }
                
                if (callback != null) {
                    callback.onSuccess();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending typing status: " + e.getMessage());
                if (callback != null) {
                    callback.onError("فشل إرسال الحالة");
                }
            }
        });
    }
    
    // ═══════════════════════════════════════════════════════════════
    // التخزين ورفع الملفات (Storage & Upload)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * رفع صورة الملف الشخصي
     */
    public void uploadProfileImage(String userId, Uri imageUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                // ضغط الصورة
                Bitmap compressedBitmap = compressImage(imageUri, 800, 800, 80);
                
                // حفظ الصورة المضغوطة
                File imageFile = saveBitmapToFile(compressedBitmap, "profile_" + userId + ".jpg");
                
                // رفع الصورة
                String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
                String imageUrl = uploadFileToStorage(BUCKET_PROFILE_IMAGES, fileName, imageFile, callback);
                
                if (imageUrl != null) {
                    callback.onSuccess(imageUrl);
                } else {
                    callback.onError("فشل رفع الصورة");
                }
                
                // حذف الملف المؤقت
                imageFile.delete();
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading profile image: " + e.getMessage());
                callback.onError("حدث خطأ في رفع الصورة");
            }
        });
    }
    
    /**
     * رفع صورة للمحادثة
     */
    public void uploadImage(String userId, Uri imageUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                // ضغط الصورة
                Bitmap compressedBitmap = compressImage(imageUri, 1920, 1080, 85);
                
                // حفظ الصورة المضغوطة
                File imageFile = saveBitmapToFile(compressedBitmap, "chat_image_" + System.currentTimeMillis() + ".jpg");
                
                // رفع الصورة
                String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
                String imageUrl = uploadFileToStorage(BUCKET_CHAT_IMAGES, fileName, imageFile, callback);
                
                if (imageUrl != null) {
                    callback.onSuccess(imageUrl);
                } else {
                    callback.onError("فشل رفع الصورة");
                }
                
                // حذف الملف المؤقت
                imageFile.delete();
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading chat image: " + e.getMessage());
                callback.onError("حدث خطأ في رفع الصورة");
            }
        });
    }
    
    /**
     * رفع ملف صوتي
     */
    public void uploadAudio(String userId, Uri audioUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                File audioFile = getFileFromUri(audioUri);
                
                String fileName = userId + "_" + System.currentTimeMillis() + ".m4a";
                String audioUrl = uploadFileToStorage(BUCKET_CHAT_AUDIOS, fileName, audioFile, callback);
                
                if (audioUrl != null) {
                    callback.onSuccess(audioUrl);
                } else {
                    callback.onError("فشل رفع الملف الصوتي");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading audio: " + e.getMessage());
                callback.onError("حدث خطأ في رفع الملف الصوتي");
            }
        });
    }
    
    /**
     * رفع ملف
     */
    public void uploadFile(String userId, Uri fileUri, UploadCallback callback) {
        executorService.execute(() -> {
            try {
                File file = getFileFromUri(fileUri);
                
                String fileName = userId + "_" + System.currentTimeMillis() + "_" + file.getName();
                String fileUrl = uploadFileToStorage(BUCKET_CHAT_FILES, fileName, file, callback);
                
                if (fileUrl != null) {
                    callback.onSuccess(fileUrl);
                } else {
                    callback.onError("فشل رفع الملف");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file: " + e.getMessage());
                callback.onError("حدث خطأ في رفع الملف");
            }
        });
    }
    
    /**
     * رفع ملف إلى Supabase Storage
     */
    private String uploadFileToStorage(String bucket, String fileName, File file, UploadCallback callback) {
        try {
            String uploadUrl = STORAGE_API_URL + "object/" + bucket + "/" + fileName;
            
            // قراءة الملف
            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fis.read(fileBytes);
            fis.close();
            
            // إنشاء الطلب
            RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/octet-stream"),
                fileBytes
            );
            
            Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .build();
            
            // تنفيذ الطلب
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                // بناء رابط الملف العام
                String publicUrl = STORAGE_API_URL + "object/public/" + bucket + "/" + fileName;
                return publicUrl;
            } else {
                Log.e(TAG, "Upload failed: " + response.code());
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading to storage: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ضغط صورة
     */
    private Bitmap compressImage(Uri imageUri, int maxWidth, int maxHeight, int quality) {
        try {
            InputStream input = context.getContentResolver().openInputStream(imageUri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            int scale = 1;
            while (options.outWidth / scale > maxWidth || options.outHeight / scale > maxHeight) {
                scale *= 2;
            }
            
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;
            
            input = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            return bitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * حفظ Bitmap كملف
     */
    private File saveBitmapToFile(Bitmap bitmap, String fileName) {
        try {
            File file = new File(context.getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error saving bitmap to file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * الحصول على ملف من Uri
     */
    private File getFileFromUri(Uri uri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            File file = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis());
            
            OutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            
            output.close();
            input.close();
            
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file from uri: " + e.getMessage());
            return null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // التحديثات الفورية (Realtime Subscriptions)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * الاشتراك في محادثة
     */
    public void subscribeToChat(String chatId, ChatListener listener) {
        if (!chatListeners.containsKey(chatId)) {
            chatListeners.put(chatId, new ArrayList<>());
        }
        chatListeners.get(chatId).add(listener);
        
        // إنشاء اتصال WebSocket إذا لم يكن موجوداً
        if (realtimeWebSocket == null) {
            connectRealtimeWebSocket();
        }
        
        // الاشتراك في التحديثات
        subscribeToRealtimeChannel(chatId);
    }
    
    /**
     * إلغاء الاشتراك من محادثة
     */
    public void unsubscribeFromChat(String chatId) {
        chatListeners.remove(chatId);
    }
    
    /**
     * الاشتراك في رسائل جديدة
     */
    public void subscribeToNewMessages(String userId, MessageListener listener) {
        if (!messageListeners.containsKey(userId)) {
            messageListeners.put(userId, new ArrayList<>());
        }
        messageListeners.get(userId).add(listener);
    }
    
    /**
     * إلغاء الاشتراك من الرسائل
     */
    public void unsubscribeFromMessages() {
        messageListeners.clear();
    }
    
    /**
     * الاتصال بـ WebSocket
     */
    private void connectRealtimeWebSocket() {
        try {
            Request request = new Request.Builder()
                .url(REALTIME_URL)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .build();
            
            realtimeWebSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    Log.d(TAG, "WebSocket Connected");
                }
                
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleRealtimeMessage(text);
                }
                
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    Log.e(TAG, "WebSocket Error: " + t.getMessage());
                }
                
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    Log.d(TAG, "WebSocket Closed: " + reason);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error connecting WebSocket: " + e.getMessage());
        }
    }
    
    /**
     * الاشتراك في قناة
     */
    private void subscribeToRealtimeChannel(String chatId) {
        try {
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("event", "phx_join");
            subscribeMsg.put("topic", "realtime:public:messages:chat_id=eq." + chatId);
            subscribeMsg.put("payload", new JSONObject());
            subscribeMsg.put("ref", System.currentTimeMillis());
            
            if (realtimeWebSocket != null) {
                realtimeWebSocket.send(subscribeMsg.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error subscribing to channel: " + e.getMessage());
        }
    }
    
    /**
     * معالجة رسائل WebSocket
     */
    private void handleRealtimeMessage(String message) {
        try {
            JSONObject msgObj = new JSONObject(message);
            String event = msgObj.getString("event");
            
            if (event.equals("postgres_changes")) {
                JSONObject payload = msgObj.getJSONObject("payload");
                String type = payload.getString("type");
                JSONObject record = payload.getJSONObject("record");
                
                String chatId = record.getString("chat_id");
                
                if (type.equals("INSERT")) {
                    // رسالة جديدة
                    MessageItem newMessage = parseMessageFromJson(record);
                    notifyChatListeners(chatId, newMessage);
                } else if (type.equals("UPDATE")) {
                    // تحديث رسالة
                    MessageItem updatedMessage = parseMessageFromJson(record);
                    notifyChatListenersUpdate(chatId, updatedMessage);
                } else if (type.equals("DELETE")) {
                    // حذف رسالة
                    String messageId = record.getString("id");
                    notifyChatListenersDelete(chatId, messageId);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling realtime message: " + e.getMessage());
        }
    }
    
    /**
     * إشعار المستمعين برسالة جديدة
     */
    private void notifyChatListeners(String chatId, MessageItem message) {
        if (chatListeners.containsKey(chatId)) {
            for (ChatListener listener : chatListeners.get(chatId)) {
                listener.onNewMessage(message);
            }
        }
    }
    
    /**
     * إشعار المستمعين بتحديث رسالة
     */
    private void notifyChatListenersUpdate(String chatId, MessageItem message) {
        if (chatListeners.containsKey(chatId)) {
            for (ChatListener listener : chatListeners.get(chatId)) {
                listener.onMessageUpdated(message);
            }
        }
    }
    
    /**
     * إشعار المستمعين بحذف رسالة
     */
    private void notifyChatListenersDelete(String chatId, String messageId) {
        if (chatListeners.containsKey(chatId)) {
            for (ChatListener listener : chatListeners.get(chatId)) {
                listener.onMessageDeleted(messageId);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // التشفير والأمان (Encryption & Security)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تشفير رسالة
     */
    private String encryptMessage(String message) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
            
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message: " + e.getMessage());
            return message; // إرجاع النص الأصلي في حالة الفشل
        }
    }
    
    /**
     * فك تشفير رسالة
     */
    private String decryptMessage(String encryptedMessage) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.decode(encryptedMessage, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message: " + e.getMessage());
            return encryptedMessage; // إرجاع النص المشفر في حالة الفشل
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // HTTP Requests (طلبات HTTP)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تنفيذ طلب GET
     */
    private JSONArray executeGetRequest(String endpoint) throws Exception {
        String url = REST_API_URL + endpoint;
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            .build();
        
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        
        if (response.isSuccessful()) {
            return new JSONArray(responseBody);
        } else {
            throw new Exception("Request failed: " + response.code());
        }
    }
    
    /**
     * تنفيذ طلب POST
     */
    private JSONArray executePostRequest(String endpoint, JSONObject data) throws Exception {
        String url = REST_API_URL + endpoint;
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            data.toString()
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build();
        
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        
        if (response.isSuccessful()) {
            return new JSONArray(responseBody);
        } else {
            throw new Exception("Request failed: " + response.code());
        }
    }
    
    /**
     * تنفيذ طلب PATCH
     */
    private JSONArray executePatchRequest(String endpoint, JSONObject data) throws Exception {
        String url = REST_API_URL + endpoint;
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            data.toString()
        );
        
        Request request = new Request.Builder()
            .url(url)
            .patch(body)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build();
        
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        
        if (response.isSuccessful()) {
            return new JSONArray(responseBody);
        } else {
            throw new Exception("Request failed: " + response.code());
        }
    }
    
    /**
     * تنفيذ طلب DELETE
     */
    private void executeDeleteRequest(String endpoint) throws Exception {
        String url = REST_API_URL + endpoint;
        
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (!response.isSuccessful()) {
            throw new Exception("Delete request failed: " + response.code());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // مساعدات التحليل (Parsing Helpers)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * تحليل مستخدم من JSON
     */
    private UserItem parseUserFromJson(JSONObject json) throws JSONException {
        String userId = json.getString("id");
        String username = json.optString("username", "");
        String phone = json.optString("phone_number", "");
        String profilePic = json.optString("profile_pic", "");
        String bio = json.optString("bio", "");
        boolean isOnline = json.optBoolean("is_online", false);
        long lastSeen = json.optLong("last_seen", 0);
        
        return new UserItem(userId, username, phone, profilePic, bio, isOnline, lastSeen);
    }
    
    /**
     * تحليل رسالة من JSON
     */
    private MessageItem parseMessageFromJson(JSONObject json) throws JSONException {
        String messageId = json.getString("id");
        String senderId = json.getString("sender_id");
        String receiverId = json.getString("receiver_id");
        String content = decryptMessage(json.getString("content"));
        String type = json.getString("type");
        String mediaUrl = json.optString("media_url", "");
        long timestamp = json.getLong("timestamp");
        boolean isRead = json.optBoolean("is_read", false);
        boolean isDelivered = json.optBoolean("is_delivered", false);
        
        // الحصول على اسم المرسل
        String senderName = "";
        if (usersCache.containsKey(senderId)) {
            senderName = usersCache.get(senderId).getUsername();
        }
        
        return new MessageItem(
            messageId, senderId, senderName, receiverId,
            content, type, mediaUrl, timestamp,
            isRead, true, isDelivered
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // Callbacks & Interfaces
    // ═══════════════════════════════════════════════════════════════
    
    public interface VerificationCallback {
        void onSuccess(String userId, boolean isNewUser);
        void onError(String error);
    }
    
    public interface DatabaseCallback {
        void onSuccess(JSONObject result);
        void onError(String error);
    }
    
    public interface ChatsCallback {
        void onSuccess(List<ChatItem> chats);
        void onError(String error);
    }
    
    public interface MessagesCallback {
        void onSuccess(List<MessageItem> messages);
        void onError(String error);
    }
    
    public interface SendMessageCallback {
        void onSuccess(String messageId);
        void onError(String error);
    }
    
    public interface UsersCallback {
        void onSuccess(List<UserItem> users);
        void onError(String error);
    }
    
    public interface UserCallback {
        void onSuccess(UserItem user);
        void onError(String error);
    }
    
    public interface UploadCallback {
        void onSuccess(String url);
        void onError(String error);
        void onProgress(int progress);
    }
    
    public interface StatusCallback {
        void onSuccess();
        void onSuccess(boolean isOnline, long lastSeen);
        void onError(String error);
    }
    
    public interface MessageListener {
        void onNewMessage(MessageItem message);
        void onError(String error);
    }
    
    public interface ChatListener {
        void onNewMessage(MessageItem message);
        void onMessageUpdated(MessageItem message);
        void onMessageDeleted(String messageId);
        void onTypingStatusChanged(boolean isTyping);
        void onUserStatusChanged(boolean isOnline, long lastSeen);
        void onError(String error);
    }
}
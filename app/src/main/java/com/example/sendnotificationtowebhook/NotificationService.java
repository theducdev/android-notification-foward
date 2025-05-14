package com.example.sendnotificationtowebhook;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    private static final String MESSENGER_PACKAGE = "com.facebook.orca";
    private static final String ZALO_PACKAGE = "com.zing.zalo";
    private static final String PREFS_NAME = "NotificationSettings";
    private static final String WEBHOOK_URL_KEY = "webhook_url";
    private static final String KEYWORDS_KEY = "keywords";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "NotificationServiceChannel";
    private BroadcastReceiver actionReceiver;
    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WebhookManager webhookManager;
    private boolean isPaused = false;
    private Set<String> processedNotifications = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        webhookManager = new WebhookManager();
        createNotificationChannel();
        startForeground(Constants.NOTIFICATION_ID, createNotification());
        
        actionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.ACTION_STOP_SERVICE.equals(intent.getAction())) {
                    Log.d(TAG, "Stopping service...");
                    stopSelf();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_STOP_SERVICE);
        filter.addAction(Constants.ACTION_START_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, filter);
        
        Log.d(TAG, "NotificationService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (actionReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver);
        }
        Log.d(TAG, "NotificationService destroyed");
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                Constants.CHANNEL_ID,
                "Notification Listener Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the notification listener service running");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle("Notification Listener Active")
            .setContentText("Monitoring notifications...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "NotificationListener connected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (isPaused) {
            Log.d(TAG, "Service is paused, ignoring notification");
            return;
        }
        String packageName = sbn.getPackageName();
        
        // Chỉ xử lý thông báo từ Messenger và Zalo
        if (!packageName.equals(Constants.MESSENGER_PACKAGE) && 
            !packageName.equals(Constants.ZALO_PACKAGE)) {
            return;
        }

        // Tạo unique key cho notification
        String notificationKey = createNotificationKey(sbn);
        
        // Kiểm tra xem notification đã được xử lý chưa
        if (processedNotifications.contains(notificationKey)) {
            Log.d(TAG, "Notification already processed: " + notificationKey);
            return;
        }

        // Thêm vào danh sách đã xử lý
        processedNotifications.add(notificationKey);
        
        // Giới hạn kích thước của set để tránh memory leak
        if (processedNotifications.size() > 100) {
            // Xóa các entry cũ nhất
            Iterator<String> iterator = processedNotifications.iterator();
            for (int i = 0; i < 50 && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }

        Notification notification = sbn.getNotification();
        String title = "";
        String text = "";
        String sender = "";
        String content = "";

        if (packageName.equals(ZALO_PACKAGE)) {
            // Xử lý đặc biệt cho Zalo
            if (notification.extras != null) {
                // Log tất cả các key có trong extras để debug
                for (String key : notification.extras.keySet()) {
                    Object value = notification.extras.get(key);
                    Log.d(TAG, "Zalo notification extra - Key: " + key + ", Value: " + value);
                }

                // Lấy thông tin từ các trường khác nhau của notification
                CharSequence titleSequence = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                CharSequence textSequence = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
                CharSequence bigTextSequence = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                CharSequence summarySequence = notification.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                CharSequence subTextSequence = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                
                // Xử lý title (thường là tên người gửi)
                if (titleSequence != null) {
                    sender = titleSequence.toString();
                    // Loại bỏ phần "(X tin nhắn)" từ title
                    if (sender.contains("(")) {
                        sender = sender.substring(0, sender.lastIndexOf("(")).trim();
                    }
                    title = sender;
                }

                // Xử lý nội dung tin nhắn
                if (bigTextSequence != null) {
                    // Ưu tiên sử dụng big text vì nó thường chứa toàn bộ nội dung
                    content = bigTextSequence.toString();
                } else if (textSequence != null) {
                    content = textSequence.toString();
                }

                // Log để debug
                Log.d(TAG, "Zalo notification parsed - Sender: " + sender + ", Content: " + content);
            }
        } else {
            // Xử lý thông thường cho Messenger
            title = notification.extras.getString(Notification.EXTRA_TITLE);
            text = notification.extras.getString(Notification.EXTRA_TEXT);
            content = text;
        }

        if ((packageName.equals(ZALO_PACKAGE) && !sender.isEmpty() && !content.isEmpty()) ||
            (packageName.equals(MESSENGER_PACKAGE) && title != null && text != null)) {
            
            if (shouldIgnoreNotification(title)) {
                Log.d(TAG, "Ignoring notification due to keyword filter");
                return;
            }

            NotificationItem item;
            if (packageName.equals(ZALO_PACKAGE)) {
                item = new NotificationItem(title, content, packageName);
            } else {
                item = new NotificationItem(title, text, packageName);
            }
            
            sendToWebhook(item);
            sendBroadcastToActivity(item);
        }
    }

    private String createNotificationKey(StatusBarNotification sbn) {
        return sbn.getPackageName() + "|" + 
               sbn.getPostTime() + "|" + 
               sbn.getNotification().extras.getString(Notification.EXTRA_TITLE) + "|" +
               sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
    }

    private boolean shouldIgnoreNotification(String title) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String keywords = settings.getString(KEYWORDS_KEY, "");
        
        if (keywords.isEmpty()) {
            return false;
        }

        String[] keywordArray = keywords.split(",");
        for (String keyword : keywordArray) {
            keyword = keyword.trim().toLowerCase();
            if (!keyword.isEmpty() && title.toLowerCase().contains(keyword)) {
                Log.d(TAG, "Found ignore keyword: " + keyword + " in title: " + title);
                return true;
            }
        }
        return false;
    }

    private void sendToWebhook(final NotificationItem item) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String webhookUrl = settings.getString(WEBHOOK_URL_KEY, "");
        
        if (webhookUrl.isEmpty()) {
            Log.e(TAG, "Webhook URL is empty");
            return;
        }

        webhookManager.sendToWebhook(webhookUrl, item, new WebhookManager.WebhookCallback() {
            @Override
            public void onSuccess() {
                item.setSendSuccess(true);
                updateNotificationStatus(item);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Webhook error: " + error);
                handleWebhookFailure(item);
            }
        });
    }

    private void handleWebhookFailure(NotificationItem item) {
        item.incrementRetryCount();
        Log.d(TAG, "Handling webhook failure, retry count: " + item.getRetryCount());
        
        if (item.getRetryCount() < 3) {
            Log.d(TAG, "Scheduling retry in 2 seconds");
            mainHandler.postDelayed(() -> sendToWebhook(item), 2000);
        } else {
            Log.d(TAG, "Max retries reached, marking as failed");
            item.setSendSuccess(false);
            updateNotificationStatus(item);
        }
    }

    private void updateNotificationStatus(NotificationItem item) {
        Log.d(TAG, "Updating notification status: success=" + item.isSendSuccess());
        sendBroadcastToActivity(item);
    }

    private void sendBroadcastToActivity(NotificationItem item) {
        Intent intent = new Intent(Constants.ACTION_NOTIFICATION_RECEIVED);
        intent.putExtra(Constants.EXTRA_NOTIFICATION, item);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Broadcast sent to activity");
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
        Log.d(TAG, "Service paused state changed to: " + paused);
    }
} 
package com.example.sendnotificationtowebhook;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NotificationAdapter adapter;
    private EditText webhookUrlInput;
    private EditText keywordInput;
    private Button toggleButton;
    private boolean isServiceRunning = false;
    private WebhookManager webhookManager;
    private RecyclerView recyclerView;

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                NotificationItem item = (NotificationItem) intent.getSerializableExtra(Constants.EXTRA_NOTIFICATION);
                if (item != null) {
                    runOnUiThread(() -> {
                        adapter.addNotification(item);
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webhookManager = new WebhookManager();

        // Initialize views
        webhookUrlInput = findViewById(R.id.webhookUrlInput);
        keywordInput = findViewById(R.id.keywordInput);
        Button submitButton = findViewById(R.id.submitButton);
        Button testButton = findViewById(R.id.testButton);
        toggleButton = findViewById(R.id.toggleButton);
        recyclerView = findViewById(R.id.notificationsRecyclerView);

        // Load saved settings
        loadSettings();
        loadServiceState();

        // Setup RecyclerView
        adapter = new NotificationAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Thêm animation
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(500);  // Thời gian animation khi thêm item
        animator.setRemoveDuration(500);  // Thời gian animation khi xóa item
        recyclerView.setItemAnimator(animator);

        // Setup buttons
        submitButton.setOnClickListener(v -> saveSettings());
        testButton.setOnClickListener(v -> testWebhook());
        toggleButton.setOnClickListener(v -> toggleService());

        // Register broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            notificationReceiver,
            new IntentFilter(Constants.ACTION_NOTIFICATION_RECEIVED)
        );

        // Check permissions
        checkAndRequestPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    private void saveSettings() {
        String webhookUrl = webhookUrlInput.getText().toString().trim();
        String keywords = keywordInput.getText().toString().trim();

        if (webhookUrl.isEmpty()) {
            webhookUrlInput.setError("Please enter webhook URL");
            return;
        }

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Constants.WEBHOOK_URL_KEY, webhookUrl);
        editor.putString(Constants.KEYWORDS_KEY, keywords);
        editor.apply();

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String savedWebhookUrl = settings.getString(Constants.WEBHOOK_URL_KEY, "");
        String savedKeywords = settings.getString(Constants.KEYWORDS_KEY, "");

        webhookUrlInput.setText(savedWebhookUrl);
        keywordInput.setText(savedKeywords);
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private void requestNotificationAccess() {
        Intent intent = new Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
        Toast.makeText(this, "Please enable notification access",
                Toast.LENGTH_LONG).show();
    }

    private void checkAndRequestPermissions() {
        // Check notification listener permission
        if (!isNotificationServiceEnabled()) {
            requestNotificationAccess();
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        Constants.PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void testWebhook() {
        String webhookUrl = webhookUrlInput.getText().toString().trim();
        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "Please enter webhook URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationItem testItem = new NotificationItem(
            "Test Notification",
            "This is a test notification",
            "com.example.sendnotificationtowebhook"
        );

        // Show loading
        Toast.makeText(this, "Sending test notification...", Toast.LENGTH_SHORT).show();

        webhookManager.sendToWebhook(webhookUrl, testItem, new WebhookManager.WebhookCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Test notification sent successfully!", Toast.LENGTH_SHORT).show();
                    testItem.setSendSuccess(true);
                    adapter.addNotification(testItem);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to send: " + error, Toast.LENGTH_LONG).show();
                    testItem.setSendSuccess(false);
                    adapter.addNotification(testItem);
                });
            }
        });
    }

    private void loadServiceState() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        isServiceRunning = settings.getBoolean(Constants.PREF_SERVICE_RUNNING, true);
        updateToggleButton();
    }

    private void toggleService() {
        isServiceRunning = !isServiceRunning;
        
        if (!isServiceRunning) {
            // Dừng service
            Intent intent = new Intent(Constants.ACTION_STOP_SERVICE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            
            // Vô hiệu hóa NotificationListenerService
            toggleNotificationListenerService(false);
        } else {
            // Kích hoạt NotificationListenerService
            toggleNotificationListenerService(true);
        }

        // Lưu trạng thái
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        settings.edit().putBoolean(Constants.PREF_SERVICE_RUNNING, isServiceRunning).apply();

        updateToggleButton();
        
        Toast.makeText(this, 
            isServiceRunning ? "Service started" : "Service stopped", 
            Toast.LENGTH_SHORT).show();
    }

    private void toggleNotificationListenerService(boolean enable) {
        PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName(this, NotificationService.class);
        
        if (enable) {
            // Kích hoạt service
            pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            
            // Yêu cầu quyền nếu chưa có
            if (!isNotificationServiceEnabled()) {
                requestNotificationAccess();
            }
        } else {
            // Vô hiệu hóa service
            pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        }
    }

    private void updateToggleButton() {
        toggleButton.setText(isServiceRunning ? "Stop Service" : "Start Service");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra trạng thái service khi activity được mở lại
        loadServiceState();
    }
}
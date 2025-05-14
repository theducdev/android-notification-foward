package com.example.sendnotificationtowebhook;

public class Constants {
    // Service Actions
    public static final String ACTION_STOP_SERVICE = "com.example.sendnotificationtowebhook.STOP_SERVICE";
    public static final String ACTION_START_SERVICE = "com.example.sendnotificationtowebhook.START_SERVICE";
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.example.sendnotificationtowebhook.NOTIFICATION_RECEIVED";

    // Intent Extras
    public static final String EXTRA_NOTIFICATION = "notification_item";

    // Permissions
    public static final int PERMISSION_REQUEST_CODE = 123;

    // Preferences
    public static final String PREFS_NAME = "NotificationSettings";
    public static final String PREF_SERVICE_RUNNING = "service_running";
    public static final String WEBHOOK_URL_KEY = "webhook_url";
    public static final String KEYWORDS_KEY = "keywords";

    // Notification
    public static final String CHANNEL_ID = "NotificationServiceChannel";
    public static final int NOTIFICATION_ID = 1;

    // Package Names
    public static final String MESSENGER_PACKAGE = "com.facebook.orca";
    public static final String ZALO_PACKAGE = "com.zing.zalo";
} 
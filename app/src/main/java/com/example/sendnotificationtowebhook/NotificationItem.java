package com.example.sendnotificationtowebhook;

import java.io.Serializable;

public class NotificationItem implements Serializable {
    private String title;
    private String content;
    private String packageName;
    private long timestamp;
    private boolean sendSuccess;
    private int retryCount;

    public NotificationItem(String title, String content, String packageName) {
        this.title = title;
        this.content = content;
        this.packageName = packageName;
        this.timestamp = System.currentTimeMillis();
        this.sendSuccess = false;
        this.retryCount = 0;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getPackageName() { return packageName; }
    public long getTimestamp() { return timestamp; }
    public boolean isSendSuccess() { return sendSuccess; }
    public void setSendSuccess(boolean sendSuccess) { this.sendSuccess = sendSuccess; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
} 
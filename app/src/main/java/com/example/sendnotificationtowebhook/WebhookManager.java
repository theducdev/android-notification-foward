package com.example.sendnotificationtowebhook;

import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class WebhookManager {
    private static final String TAG = "WebhookManager";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    public interface WebhookCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void sendToWebhook(String webhookUrl, NotificationItem item, WebhookCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("title", item.getTitle());
            json.put("content", item.getContent());
            json.put("packageName", item.getPackageName());
            json.put("timestamp", item.getTimestamp());

            String jsonStr = json.toString();
            Log.d(TAG, "Sending JSON to webhook: " + jsonStr);

            RequestBody body = RequestBody.create(jsonStr, JSON);
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Webhook request failed", e);
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d(TAG, "Webhook response: " + response.code() + ", body: " + responseBody);
                    
                    if (!response.isSuccessful()) {
                        callback.onFailure("HTTP " + response.code() + ": " + responseBody);
                    } else {
                        callback.onSuccess();
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating webhook request", e);
            callback.onFailure(e.getMessage());
        }
    }
} 
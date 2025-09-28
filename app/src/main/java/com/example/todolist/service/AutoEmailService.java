package com.example.todolist.service;

import android.content.Context;
import android.util.Log;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AutoEmailService {
    private static final String TAG = "AutoEmailService";
    private static AutoEmailService instance;
    private Context context;
    // Firebase Functions URL
    private static final String FIREBASE_FUNCTION_URL = "https://us-central1-todolist-b2f4a.cloudfunctions.net/sendTaskInvitationEmail";

    public interface EmailSendCallback {
        void onEmailSent(String message);
        void onError(String error);
    }

    private AutoEmailService() {
    }

    public static AutoEmailService getInstance() {
        if (instance == null) {
            instance = new AutoEmailService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
    }

    public void sendTaskInvitationEmail(String recipientEmail, String recipientName, 
                                       String taskTitle, String inviterName, 
                                       String taskId, String shareId, EmailSendCallback callback) {
        
        Log.d(TAG, "Sending email invitation via Firebase Function to: " + recipientEmail);
        String inviteUrl = generateInviteUrl(taskId, shareId, recipientEmail);
        new SendEmailTask(callback).execute(recipientEmail, recipientName, taskTitle, inviterName, inviteUrl);
    }
    
    private static class SendEmailTask extends AsyncTask<String, Void, String> {
        private EmailSendCallback callback;
        private Exception exception;
        
        public SendEmailTask(EmailSendCallback callback) {
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(String... params) {
            try {
                String recipientEmail = params[0];
                String recipientName = params[1];
                String taskTitle = params[2];
                String inviterName = params[3];
                String inviteUrl = params[4];
                
                // Tạo JSON data cho Firebase Function
                JSONObject emailData = new JSONObject();
                emailData.put("recipientEmail", recipientEmail);
                emailData.put("recipientName", recipientName);
                emailData.put("taskTitle", taskTitle);
                emailData.put("inviterName", inviterName);
                emailData.put("inviteUrl", inviteUrl);
                
                Log.d(TAG, "Firebase Function request data: " + emailData.toString());
                URL url = new URL(FIREBASE_FUNCTION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = emailData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Firebase Function response code: " + responseCode);
                
                if (responseCode == 200) {
                    return "Email đã được gửi thành công đến " + recipientEmail;
                } else {
                    throw new Exception("Firebase Function returned error code: " + responseCode);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending email via Firebase Function", e);
                this.exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                if (result != null) {
                    callback.onEmailSent(result);
                } else {
                    String errorMsg = "Lỗi gửi email qua Firebase Function";
                    if (exception != null) {
                        errorMsg += ": " + exception.getMessage();
                    }
                    callback.onError(errorMsg);
                }
            }
        }
    }

    private String generateInviteUrl(String taskId, String shareId, String recipientEmail) {
        return "todolist://invite?taskId=" + taskId + "&shareId=" + shareId + "&email=" + recipientEmail;
    }
}
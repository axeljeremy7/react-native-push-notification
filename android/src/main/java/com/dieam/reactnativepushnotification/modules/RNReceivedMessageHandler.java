package com.dieam.reactnativepushnotification.modules;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.util.Log;
import android.net.Uri;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.dieam.reactnativepushnotification.helpers.ApplicationBadgeHelper;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import org.json.JSONObject;

import java.util.Map;
import java.util.List;
import java.security.SecureRandom;
import java.util.Set;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

public class RNReceivedMessageHandler {
    private FirebaseMessagingService mFirebaseMessagingService;

    public RNReceivedMessageHandler(@NonNull FirebaseMessagingService service) {
        this.mFirebaseMessagingService = service;
    }

    public void handleReceivedMessage(RemoteMessage message) {
        Log.d(LOG_TAG, "handleReceivedMessage()");
        Log.d(LOG_TAG, "handleReceivedMessage() message getTo => " + message.getTo());
        Log.d(LOG_TAG, "handleReceivedMessage() message getFrom => " + message.getFrom());
        Log.d(LOG_TAG, "handleReceivedMessage() message getCollapseKey => " + message.getCollapseKey());
        Log.d(LOG_TAG, "handleReceivedMessage() message getMessageId => " + message.getMessageId());
        Log.d(LOG_TAG, "handleReceivedMessage() message getSenderId => " + message.getSenderId());
        Log.d(LOG_TAG, "handleReceivedMessage() message getMessageType => " + message.getMessageType());
        String from = message.getFrom();
        RemoteMessage.Notification remoteNotification = message.getNotification();
        final Bundle bundle = new Bundle();
        // Putting it from remoteNotification first so it can be overridden if message
        // data has it
        if (remoteNotification != null) {
            Log.d(LOG_TAG, "remoteNotification: " + remoteNotification.toString());
            Log.d(LOG_TAG, "remoteNotification body: " + remoteNotification.getBody());
            Log.d(LOG_TAG, "remoteNotification title: " + remoteNotification.getTitle());
            Log.d(LOG_TAG, "remoteNotification channelId: " + remoteNotification.getChannelId());
            // ^ It's null when message is from GCM
            RNPushNotificationConfig config = new RNPushNotificationConfig(mFirebaseMessagingService.getApplication());

            String title = getLocalizedString(remoteNotification.getTitle(), remoteNotification.getTitleLocalizationKey(), remoteNotification.getTitleLocalizationArgs());
            String body = getLocalizedString(remoteNotification.getBody(), remoteNotification.getBodyLocalizationKey(), remoteNotification.getBodyLocalizationArgs());

            bundle.putString("title", title);
            bundle.putString("message", body);
            bundle.putString("sound", remoteNotification.getSound());
            bundle.putString("color", remoteNotification.getColor());
            bundle.putString("tag", remoteNotification.getTag());

            if (remoteNotification.getChannelId() != null) {
                bundle.putString("channelId", remoteNotification.getChannelId());
            } else {
                bundle.putString("channelId", config.getNotificationDefaultChannelId());
            }

            Integer visibilty = remoteNotification.getVisibility();
            String visibilityString = "private";

            if (visibilty != null) {
                switch (visibilty) {
                    case NotificationCompat.VISIBILITY_PUBLIC:
                        visibilityString = "public";
                        break;
                    case NotificationCompat.VISIBILITY_SECRET:
                        visibilityString = "secret";
                        break;
                }
            }

            bundle.putString("visibility", visibilityString);

            Integer priority = remoteNotification.getNotificationPriority();
            String priorityString = "high";

            if (priority != null) {
                switch (priority) {
                    case NotificationCompat.PRIORITY_MAX:
                        priorityString = "max";
                        break;
                    case NotificationCompat.PRIORITY_LOW:
                        priorityString = "low";
                        break;
                    case NotificationCompat.PRIORITY_MIN:
                        priorityString = "min";
                        break;
                    case NotificationCompat.PRIORITY_DEFAULT:
                        priorityString = "default";
                        break;
                }
            }

            bundle.putString("priority", priorityString);

            Uri uri = remoteNotification.getImageUrl();

            if (uri != null) {
                String imageUrl = uri.toString();

                bundle.putString("bigPictureUrl", imageUrl);
                bundle.putString("largeIconUrl", imageUrl);
            }
        } else {
            Log.d(LOG_TAG, "remoteNotification is null: ");
        }

        Bundle dataBundle = new Bundle();
        Map<String, String> notificationData = message.getData();

        for (Map.Entry<String, String> entry : notificationData.entrySet()) {
            Log.d(LOG_TAG, "Notification Data getKey: " + entry.getKey());
            Log.d(LOG_TAG, "Notification Data getValue: " + entry.getValue());
            dataBundle.putString(entry.getKey(), entry.getValue());
            if (entry.getKey().equalsIgnoreCase("twi_body")) {
                dataBundle.putString("message", entry.getValue());
                bundle.putString("message", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("channel_title")) {
                dataBundle.putString("title", entry.getValue());
                bundle.putString("title", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("twi_sound")) {
//                bundle.putString("sound", entry.getValue());
                dataBundle.putString("sound", "default");
                bundle.putString("sound", "default");
//                bundle.putString("color", remoteNotification.getColor());
            } else if (entry.getKey().equalsIgnoreCase("channel_id")) {
                dataBundle.putString("tag", entry.getValue());
                dataBundle.putString("channelId", entry.getValue());
                bundle.putString("tag", entry.getValue());
                bundle.putString("channelId", entry.getValue());
                bundle.putString("channelSid", entry.getValue());

//                bundle.putString("group", entry.getValue());
                dataBundle.putString("channelSid", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("message_index")) {
                dataBundle.putString("id", entry.getValue());
                bundle.putString("id", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("message_id")) {
                dataBundle.putString("messageSid", entry.getValue());
                bundle.putString("messageSid", entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("author")) {
                bundle.putString("author", entry.getValue());
            }
        }
        bundle.putString("visibility", "public");
        bundle.putString("priority", "high");
//        bundle.putBoolean("groupSummary", true);
        bundle.putParcelable("data", dataBundle);
        Log.d(LOG_TAG, "bundle: " + bundle);
        bundle.putInt("group", 0);
//        Log.v(LOG_TAG, "onMessageReceived: " + bundle);

        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                final ReactInstanceManager mReactInstanceManager = ((ReactApplication) mFirebaseMessagingService.getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notification
                if (context != null) {
                    Log.d(LOG_TAG, "context is not null");
                    handleRemotePushNotification((ReactApplicationContext) context, bundle);
                } else {
                    Log.d(LOG_TAG, "context is null");
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            mReactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
    }

    private void handleRemotePushNotification(ReactApplicationContext context, Bundle bundle) {
        Log.d(LOG_TAG, "handleRemotePushNotification()");
        boolean showNotification = true;
        String identity = "";
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("dsp", Context.MODE_PRIVATE);
            Map<String, String> map = (Map<String, String>) sharedPreferences.getAll();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Log.d(LOG_TAG, "SharedPreferences => Key = " + entry.getKey() + ", Value = " + entry.getValue());
            }
            Log.d(LOG_TAG, "activeChannel : " + map.get("activeChannel"));
            Log.d(LOG_TAG, "channelSid : " + bundle.getString("channelSid"));
            if (map.containsKey("activeChannel") && (map.get("activeChannel") != null) && map.get("activeChannel").equalsIgnoreCase(bundle.getString("channelSid"))) {
                showNotification = false;
            }
            if (map.containsKey("identity") && (map.get("identity") != null)) {
                identity = map.get("identity");
            }
            String message = bundle.getString("message");
            Log.d(LOG_TAG, "message : " + message);
            if (message.contains("system:")) {
                message = message.replace("system: ", "workplace_bot: ");
                Log.d(LOG_TAG, "message : " + message);
                bundle.putString("message", message);
            }

            if (bundle.getString("title") != null && bundle.getString("title").contains(identity) && bundle.getString("author") != null) {
                String title = bundle.getString("author");
                message = message.replace(title + ": ", "");
                if (title.contains("workplace_bot")) {
                    title = "workplace_bot";
                }
                if(title.contains("system")){
                    title = "workplace_bot";
                    message = message.replace(title + ": ", "");
                }
                Log.d(LOG_TAG, "title : " + title);
                Log.d(LOG_TAG, "message : " + message);
                bundle.putString("title", title);
                bundle.putString("message", message);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "getSharedPreferences: " + e.getMessage());
        }
        Log.d(LOG_TAG, "bundle: " + bundle);
        // If notification ID is not provided by the user for push notification, generate one at random
        if (bundle.getString("id") == null) {
            Log.d(LOG_TAG, "bundle.getString(id) is null");
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        } else {
            Log.d(LOG_TAG, "bundle.getString(id) is not null");
        }

        Application applicationContext = (Application) context.getApplicationContext();
        RNPushNotificationConfig config = new RNPushNotificationConfig(mFirebaseMessagingService.getApplication());
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);
        boolean isForeground = pushNotificationHelper.isApplicationInForeground();
        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
        bundle.putBoolean("foreground", isForeground);
        bundle.putBoolean("userInteraction", false);
        Log.d(LOG_TAG, "bundle => " + bundle);
        try {
            if (showNotification) {
                Log.v(LOG_TAG, "notifyNotification()");
                jsDelivery.notifyNotification(bundle);
                // If contentAvailable is set to true, then send out a remote fetch event
                if (bundle.getString("contentAvailable", "false").equalsIgnoreCase("true")) {
                    Log.d(LOG_TAG, "notifyRemoteFetch()");
                    jsDelivery.notifyRemoteFetch(bundle);
                }
                if (config.getNotificationForeground() || !isForeground) {
                    Log.d(LOG_TAG, "sendToNotificationCentre()");
                    pushNotificationHelper.sendToNotificationCentre(bundle);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Fail sending notification: " + e.getMessage());
        }
    }

    private String getLocalizedString(String text, String locKey, String[] locArgs) {
        if (text != null) {
            return text;
        }
//        Log.d(LOG_TAG, "getLocalizedString text: " + text);
        Context context = mFirebaseMessagingService.getApplicationContext();
        String packageName = context.getPackageName();

        String result = null;

        if (locKey != null) {
            int id = context.getResources().getIdentifier(locKey, "string", packageName);
            if (id != 0) {
                if (locArgs != null) {
                    result = context.getResources().getString(id, (Object[]) locArgs);
                } else {
                    result = context.getResources().getString(id);
                }
            }
        }
//        Log.d(LOG_TAG, "getLocalizedString result: " + result);
        return result;
    }
}

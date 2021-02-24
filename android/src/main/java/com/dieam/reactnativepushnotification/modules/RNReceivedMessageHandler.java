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
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getTo => " + message.getTo());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getFrom => " + message.getFrom());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getCollapseKey => " + message.getCollapseKey());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getMessageId => " + message.getMessageId());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getSenderId => " + message.getSenderId());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getMessageType => " + message.getMessageType());
        Log.d(LOG_TAG, "handleReceivedMessage()  message.getNotification => " + message.getNotification());

        String from = message.getFrom();
        RemoteMessage.Notification remoteNotification = message.getNotification();
        final Bundle bundle = new Bundle();
        // Putting it from remoteNotification first so it can be overridden if message
        // data has it
        if (remoteNotification != null) {
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
            dataBundle.putString(entry.getKey(), entry.getValue());
        }
        Log.d(LOG_TAG, "twi_body: " + dataBundle.getString("twi_body"));
        Log.d(LOG_TAG, "channel_title: " + dataBundle.getString("channel_title"));
        Log.d(LOG_TAG, "channel_sid: " + dataBundle.getString("channel_sid"));
        Log.d(LOG_TAG, "message_index: " + dataBundle.getString("message_index"));
        Log.d(LOG_TAG, "messageId: " + message.getMessageId());

        if (dataBundle.getString("twi_body") != null) {
            bundle.putString("message", dataBundle.getString("twi_body"));
            Log.d(LOG_TAG, "message: " + bundle.getString("message"));
        }
        if (dataBundle.getString("channel_title") != null) {
            bundle.putString("title", dataBundle.getString("channel_title"));
            Log.d(LOG_TAG, "message: " + bundle.getString("title"));
        }
        if (dataBundle.getString("message_index") != null) {
            bundle.putString("number", dataBundle.getString("message_index"));
            Log.d(LOG_TAG, "number: " + bundle.getString("number"));
        }
        if (dataBundle.getString("channel_sid") != null) {
            //channelId
            bundle.putString("channelId", dataBundle.getString("channel_sid"));
            bundle.putString("tag", dataBundle.getString("channel_sid"));
            Log.d(LOG_TAG, "channelId: " + bundle.getString("channelId"));
        }
        if (message.getMessageId() != null) {
            bundle.putString("messageId", message.getMessageId());
        }
        bundle.putString("priority", "high");
        bundle.putString("visibility", "public");
        bundle.putString("sound", "default");
        bundle.putParcelable("data", dataBundle);

        Log.d(LOG_TAG, "bundle => " + bundle);
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
        // If notification ID is not provided by the user for push notification, generate one at random
        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Application applicationContext = (Application) context.getApplicationContext();

        RNPushNotificationConfig config = new RNPushNotificationConfig(mFirebaseMessagingService.getApplication());
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);

        boolean isForeground = pushNotificationHelper.isApplicationInForeground();

        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
        bundle.putBoolean("foreground", isForeground);
        bundle.putBoolean("userInteraction", false);
        Log.d(LOG_TAG, "bundle => " + bundle);
        jsDelivery.notifyNotification(bundle);
        // If contentAvailable is set to true, then send out a remote fetch event
        if (bundle.getString("contentAvailable", "false").equalsIgnoreCase("true")) {
            jsDelivery.notifyRemoteFetch(bundle);
        }

        if (config.getNotificationForeground() || !isForeground) {
            Log.d(LOG_TAG, "sendNotification: " + bundle);

            pushNotificationHelper.sendToNotificationCentre(bundle);
        }
    }

    private String getLocalizedString(String text, String locKey, String[] locArgs) {
        if (text != null) {
            return text;
        }
        Log.d(LOG_TAG, "getLocalizedString text: " + text);
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

package com.dieam.reactnativepushnotification.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.facebook.common.logging.FLog;

import me.leolin.shortcutbadger.Badger;
import me.leolin.shortcutbadger.ShortcutBadger;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

/**
 * Helper for setting application launcher icon badge counts.
 * This is a wrapper around {@link ShortcutBadger}:
 */
public class ApplicationBadgeHelper {

    public static final ApplicationBadgeHelper INSTANCE = new ApplicationBadgeHelper();

    private static final String LOG_TAG = "ApplicationBadgeHelper";

    private Boolean applyAutomaticBadger;
    private ComponentName componentName;

    private ApplicationBadgeHelper() {
    }

    public void setApplicationIconBadgeNumber(Context context, int number) {
        Log.d(LOG_TAG, "setApplicationIconBadgeNumber()");
        if (null == componentName) {
            componentName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent();
        }
        tryAutomaticBadge(context, number);
    }

    private void tryAutomaticBadge(Context context, int number) {
        Log.d(LOG_TAG, "tryAutomaticBadge()");
        if (null == applyAutomaticBadger) {
            applyAutomaticBadger = ShortcutBadger.applyCount(context, number);
            if (applyAutomaticBadger) {
                FLog.i(LOG_TAG, "First attempt to use automatic badger succeeded; permanently enabling method.");
            } else {
                FLog.i(LOG_TAG, "First attempt to use automatic badger failed; permanently disabling method.");
            }
            return;
        } else if (!applyAutomaticBadger) {
            return;
        }
        ShortcutBadger.applyCount(context, number);
    }
}

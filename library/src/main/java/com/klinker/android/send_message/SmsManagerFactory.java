package com.klinker.android.send_message;

import android.os.Build;
import android.telephony.SmsManager;

public class SmsManagerFactory {

    // val smsManager = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
    //     @Suppress("DEPRECATION")
    //             SmsManager.getDefault()
    // } else {
    //     context.getSystemService(SmsManager::class.java)
    // }

    public static SmsManager createSmsManager(Settings settings) {
        return createSmsManager(settings.getSubscriptionId());
    }

    public static SmsManager createSmsManager(int subscriptionId) {
        if (subscriptionId != Settings.DEFAULT_SUBSCRIPTION_ID &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SmsManager manager = null;

            try {
                if (Build.VERSION.SDK_INT >= 31) {
                    throw new RuntimeException("Must implemented");
//                  Context.getSystemService(SmsManager.class).createForSubscriptionId(subscriptionId);
                } else {
                    manager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (manager != null) {
                return manager;
            } else {
                return SmsManager.getDefault();
            }
        } else {
            return SmsManager.getDefault();
        }
    }
}

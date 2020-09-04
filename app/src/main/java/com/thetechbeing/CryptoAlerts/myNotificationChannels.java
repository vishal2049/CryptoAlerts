package com.thetechbeing.CryptoAlerts;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class myNotificationChannels extends Application {
    public static final String CHANNEL_ID_1 = "channel1";

    @Override
    public void onCreate() {
        super.onCreate();

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_ID_1,"alert channel", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("This channel is for Alerts.");
//            channel1.setVibrationPattern(new long[]{1000, 1000, 1000});
            channel1.enableVibration(true);

            NotificationManager manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel1);
        }

    }
}

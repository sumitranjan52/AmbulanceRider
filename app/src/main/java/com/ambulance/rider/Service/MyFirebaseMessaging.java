package com.ambulance.rider.Service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.ambulance.rider.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by sumit on 26-Jan-18.
 */

public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        if (remoteMessage.getNotification().getBody().equals("The driver is within 25 m of range")){

            showArrivedNotification(remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());

        }
        if(remoteMessage.getNotification().getTitle().equals("Notice!")){

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String msg = remoteMessage.getNotification().getBody();
                    Toast.makeText(MyFirebaseMessaging.this, msg, Toast.LENGTH_SHORT).show();
                }
            });

        }

    }

    private void showArrivedNotification(final String title, String body) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyFirebaseMessaging.this, title, Toast.LENGTH_SHORT).show();
                }
            });

        }else{

            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(),0,new Intent(),PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getBaseContext());

            notificationBuilder.setAutoCancel(true)
                    .setColor(Color.RED)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setDefaults(android.app.Notification.DEFAULT_LIGHTS| android.app.Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getBaseContext().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(1,notificationBuilder.build());

        }

    }

}

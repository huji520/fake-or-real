package com.fakeorreal.fakeorreal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        FakeOrRealApp app = (FakeOrRealApp)getApplication();
        app.setFirebaseToken(s);
    }

    private static final String CHANNEL_ID = "my_channel";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        Intent intent = CreateIntent(data);
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        createNotificationChannel(manager);
        Random random = new Random();
        int notificationId = random.nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT);
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.app_icon);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(data.get("title"))
                .setContentText(data.get("msg"))
                .setSmallIcon(R.drawable.notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(icon)
                .build();
        manager.notify(notificationId, notification);
    }

    private void createNotificationChannel(NotificationManagerCompat manager) {
        String name = "Challenges";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Allow other users to challenge you with specific images");
        manager.createNotificationChannel(channel);
    }

    private Intent CreateIntent(Map<String, String> message) {
        Intent intent;
        if (message.get("title").equals(getResources().getString(R.string.challenge_notification_title))) {
            intent = new Intent(this, PendingChallengeActivity.class);
            intent.putExtra(GameActivity.ID_KEY, message.get("imgId"));
            intent.putExtra(GameActivity.IS_REAL_KEY, Boolean.parseBoolean(message.get("isReal")));
            intent.putExtra(PendingChallengeActivity.CHALLENGER_NAME_KEY, message.get("challengerName"));
            intent.putExtra(PendingChallengeActivity.CHALLENGER_TOKEN_KEY, message.get("challengerToken"));
        }
        else {
            FakeOrRealApp app = (FakeOrRealApp)getApplication();
            User user = app.getUser();
            Class nextActivity = user == null ? LoginActivity.class : MainActivity.class;
            intent = new Intent(this, nextActivity);
            int score = Integer.parseInt(message.get("challengeScore"));
            if (score > 0) {
                user.update_totalScore(score);
                Map<String, Object> map = new HashMap<>();
                map.put(MainActivity.FIREBASE_DOCUMENT_TOTAL_SCORE, String.valueOf(user.get_records().get_totalScore()));
                FirebaseFirestore firestore = app.getFireStoreInstance();
                firestore.collection(MainActivity.FIREBASE_COLLECTION_USERS).document(user.get_id()).set(map, SetOptions.merge());
            }
        }
        return intent;
    }

}

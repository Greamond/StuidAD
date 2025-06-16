package com.example.stuid.classes;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.stuid.R;
import com.example.stuid.activity.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "project_notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");

            switch (type) {
                case "task_assignment":
                    showTaskNotification(remoteMessage.getData());
                    break;
                case "task_assignment_remove":
                    showTaskRemoveNotification(remoteMessage.getData());
                    break;
                case "project_invite":
                    showProjectNotification(remoteMessage.getData());
                    break;
                case "project_removal":
                    showProjectRemovalNotification(remoteMessage.getData());
                    break;
                default:
                    break;
            }
        }
    }

    private void showProjectRemovalNotification(Map<String, String> data) {
        String title = "Исключение из проекта";
        String body = String.format("%s исключил(а) вас из проекта '%s'",
                data.get("actionBy"),
                data.get("projectName"));
        showNotification(title, body);
    }

    private void showTaskRemoveNotification(Map<String, String> data) {
        String title = "Снятие с задачи";
        String body = String.format("%s снял(а) вас с задачи '%s'",
                data.get("assigner"),
                data.get("taskName"));
        showNotification(title, body);
    }

    private void showTaskNotification(Map<String, String> data) {
        String title = "Новая задача";
        String body = String.format("%s назначил(а) вас ответственным за задачу '%s'",
                data.get("assigner"), data.get("taskName"));

        showNotification(title, body);
    }

    private void showProjectNotification(Map<String, String> data) {
        // Формируем заголовок и текст уведомления
        String title = "Приглашение в проект";
        String body = String.format("%s пригласил(а) вас в проект \"%s\"",
                data.get("inviterName"), data.get("projectName"));

        showNotification(title, body);
    }



    /**
     * Показывает системное уведомление
     */
    private void showNotification(String title, String message) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stuid)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.notify((int) System.currentTimeMillis(), builder.build());
            } else {
                Log.d("FCM", "Разрешение на уведомления не получено");
            }
        } else {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Создает канал уведомлений для Android 8+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Проекты";
            String description = "Уведомления о проектах и задачах";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
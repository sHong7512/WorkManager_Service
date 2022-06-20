package com.shong.workmanager_service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotiMaker {
    private val NOTIFICATION_CHANNEL_ID = "Vibrate_Channel"

    fun getVibeNotiBuilder(context: Context): NotificationCompat.Builder{
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val packageName = context.packageName
        val packageManager = context.packageManager
        val notificationIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                putExtra("", "")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        builder.setContentText("문자 메세지가 도착했습니다.")
        builder.setContentIntent(pendingIntent)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setAutoCancel(true)
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)

        // Oreo API 26이상에서는 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            val channelName = "noti_Vibrate"
            val description = "AndroidO"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance)
            channel.description = description

            // 노티피케이션 채널을 시스템에 등록
            notificationManager.createNotificationChannel(channel)
        } else builder.setSmallIcon(R.mipmap.ic_launcher)

        return builder
    }
}
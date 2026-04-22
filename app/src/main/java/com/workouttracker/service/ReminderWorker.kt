package com.workouttracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.workouttracker.MainActivity

class ReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        postNotification()
        return Result.success()
    }

    private fun postNotification() {
        val channelId = "workout_reminder"
        val nm = applicationContext.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Workout Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Daily workout reminder" }
        nm.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Time to train!")
            .setContentText("Your daily workout is waiting for you.")
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(3001, notification)
    }
}

package com.shakespend.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeDetectionService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lastTrigger = 0L
    private var shakeCount = 0
    private var lastAboveThreshold = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(SERVICE_NOTIFICATION_ID, serviceNotification())
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x*x + y*y + z*z)
        val now = System.currentTimeMillis()

        // Require repeated acceleration peaks and apply a cooldown to reduce false triggers.
        if (magnitude > 23f && now - lastAboveThreshold > 120) {
            shakeCount++
            lastAboveThreshold = now
        }
        if (now - lastAboveThreshold > 650) shakeCount = 0

        if (shakeCount >= 2 && now - lastTrigger > 5000) {
            lastTrigger = now
            shakeCount = 0
            showExpensePrompt()
        }
    }

    private fun showExpensePrompt() {
        val quickAdd = Intent(this, QuickAddActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 42, quickAdd,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("ShakeSpend detected a shake")
            .setContentText("Tap to record your expense.")
            .setContentIntent(pending)
            .addAction(android.R.drawable.ic_input_add, "Add expense", pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(EXPENSE_NOTIFICATION_ID, notification)
    }

    private fun serviceNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("ShakeSpend is listening")
        .setContentText("Shake detection is enabled. Tap Stop in the app to turn it off.")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Shake detection", NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.shakespend.app.STOP"
        const val CHANNEL_ID = "shakespend_detection"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val EXPENSE_NOTIFICATION_ID = 1002
    }
}

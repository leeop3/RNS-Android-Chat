package com.example.rnschat
import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class RnsService : Service() {
    val btService by lazy { BluetoothService(this) }
    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        val channel = NotificationChannel("rns", "RNS", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(1, NotificationCompat.Builder(this, "rns").setContentTitle("RNS Active").setSmallIcon(android.R.drawable.stat_notify_chat).build())
    }
    override fun onBind(intent: Intent) = Binder()
}
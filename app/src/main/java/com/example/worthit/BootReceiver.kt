package com.example.worthit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting MonitorService")
            val serviceIntent = Intent(context, MonitorService::class.java)
            context.startService(serviceIntent)
        }
    }
}

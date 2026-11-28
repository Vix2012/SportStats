package com.example.sportstats

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object PowerGuard {
    fun enCharge(context: Context): Boolean {
        val etat = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val branche = etat?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return branche != 0
    }
}
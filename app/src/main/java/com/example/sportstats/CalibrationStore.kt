package com.example.sportstats

import android.content.Context
import androidx.compose.ui.geometry.Offset

object CalibrationStore {

    private const val NOM = "calibration"

    fun sauver(context: Context, coins: List<Offset>) {
        val editeur = context.getSharedPreferences(NOM, Context.MODE_PRIVATE).edit()
        coins.forEachIndexed { i, c ->
            editeur.putFloat("x$i", c.x)
            editeur.putFloat("y$i", c.y)
        }
        editeur.putBoolean("present", coins.size == 4)
        editeur.apply()
    }

    fun charger(context: Context): List<Offset>? {
        val p = context.getSharedPreferences(NOM, Context.MODE_PRIVATE)
        if (!p.getBoolean("present", false)) return null
        return List(4) { i -> Offset(p.getFloat("x$i", 0f), p.getFloat("y$i", 0f)) }
    }
}

package com.morgunbaen.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Android getur tekið heimildir af appi sem er ekki opnað í nokkra daga.
 * Vekjari á virkum dögum er ónotaður yfir helgi — nákvæmlega þröskuldurinn.
 * Rétt stilling er „Remove permissions if app is unused" á app-síðunni.
 */
object OemBatteryGuide {

    fun open(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}

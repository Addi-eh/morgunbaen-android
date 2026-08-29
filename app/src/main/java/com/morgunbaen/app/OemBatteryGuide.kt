package com.morgunbaen.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Leiðbeiningar um bakgrunnssvefn framleiðenda.
 *
 * Samsung, Xiaomi/HyperOS, Huawei og Oppo/OnePlus drepa öpp sem
 * enginn opnar í þrjá daga. Vekjari á virkum dögum er ónotaður yfir
 * helgi — nákvæmlega þröskuldurinn. Ekkert opinbert API slekkur á
 * þessu; notandinn verður að gera það sjálfur. Intentin hér eru
 * óskjalfest og geta brugðist; þá opnast app-upplýsingar.
 */
object OemBatteryGuide {

    enum class Kind { SAMSUNG, XIAOMI, HUAWEI, OPPO }

    fun kind(): Kind? {
        val haystack = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL)
            .joinToString(" ")
            .lowercase()
        fun hit(vararg names: String) = names.any { haystack.contains(it) }
        return when {
            hit("samsung") -> Kind.SAMSUNG
            hit("xiaomi", "redmi", "poco", "blackshark") -> Kind.XIAOMI
            hit("huawei", "honor") -> Kind.HUAWEI
            hit("oppo", "oneplus", "realme", "oplus") -> Kind.OPPO
            else -> null
        }
    }

    fun titleRes(kind: Kind): Int = when (kind) {
        Kind.SAMSUNG -> R.string.oem_samsung_title
        Kind.XIAOMI -> R.string.oem_xiaomi_title
        Kind.HUAWEI -> R.string.oem_huawei_title
        Kind.OPPO -> R.string.oem_oppo_title
    }

    fun bodyRes(kind: Kind): Int = when (kind) {
        Kind.SAMSUNG -> R.string.oem_samsung_body
        Kind.XIAOMI -> R.string.oem_xiaomi_body
        Kind.HUAWEI -> R.string.oem_huawei_body
        Kind.OPPO -> R.string.oem_oppo_body
    }

    fun open(context: Context, kind: Kind) {
        val intents = when (kind) {
            Kind.SAMSUNG -> listOf(
                component(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
            Kind.XIAOMI -> listOf(
                Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST"),
                component("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
            )
            Kind.HUAWEI -> listOf(
                component(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ),
                component(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
                )
            )
            Kind.OPPO -> listOf(
                component(
                    "com.oplus.battery",
                    "com.oplus.powermanager.fuelgaue.PowerControlActivity"
                ),
                component(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
            )
        }
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // næsta óskjalfesta leið
            }
        }
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun component(packageName: String, className: String): Intent =
        Intent().setClassName(packageName, className)
}

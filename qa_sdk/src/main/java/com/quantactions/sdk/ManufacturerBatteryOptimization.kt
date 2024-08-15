/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */
@file:Suppress("SpellCheckingInspection")

package com.quantactions.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.Keep

/**
 * @suppress
 * */
@Keep
object ManufacturerBatteryOptimization {

    private val POWERMANAGER_INTENTS = listOf(
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity"
            )
        ).setData(
            Uri.parse("mobilemanager://function/entry/AutoStart")
        ),
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.MainActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.dewav.dwappmanager",
                "com.dewav.dwappmanager.memory.SmartClearupWhiteList"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.htc.pitroad",
                "com.htc.pitroad.landingpage.activity.LandingPageActivity"
            )
        ),
//        Intent().setComponent(
//            ComponentName(
//                "com.huawei.systemmanager",
//                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
//            )
//        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
//        Intent().setComponent(
//            ComponentName(
//                "com.oppo.safe",
//                "com.oppo.safe.permission.startup.StartupAppListActivity"
//            )
//        ),
//        Intent().setComponent(
//            ComponentName(
//                "com.samsung.android.lool",
//                "com.samsung.android.sm.battery.ui.BatteryActivity"
//            )
//        ),
//        Intent().setComponent(
//            ComponentName(
//                "com.samsung.android.lool",
//                "com.samsung.android.sm.ui.battery.BatteryActivity"
//            )
//        ),
        Intent().setComponent(
            ComponentName(
                "com.transsion.phonemanager",
                "com.itel.autobootmanager.activity.AutoBootMgrActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity"
            )
        ).setData(
            Uri.parse("mobilemanager://function/entry/AutoStart")
        )
    )

    internal fun getAvailableIntents(context: Context): Intent {
        val availableIntents = mutableListOf<Intent>()
        for (intent in POWERMANAGER_INTENTS) {
            if (context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ) != null
            ) {
                availableIntents.add(intent)
            }
        }
        if (availableIntents.isEmpty()) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            availableIntents.add(intent)
        }
        return availableIntents.first()
    }

    // This should be left to do to the user of the SDK, for customization and internationalisation
//    fun showAddToWhitelistDialog(context: Context) {
//        if (getAvailableIntents(context).size > 0) {
//            if (sharedPreferences == null) {
//                sharedPreferences = context.getSharedPreferences(MANUFACTURER, Context.MODE_PRIVATE)
//            }
//            if (!sharedPreferences!!.getBoolean(PROTECTED, false)) {
//                val dialog = AlertDialog.Builder(context)
//                    .setTitle("ATTENTION PLEASE")
//                    .setMessage(
//                        """Dear ${Build.MANUFACTURER} user, you need to add ${
//                            getApplicationName(
//                                context
//                            )
//                        } in your Task Killer's whitelist for ${getApplicationName(context)} to work properly.
//Click OK to proceed..."""
//                    )
//                    .setPositiveButton("OK") { dialog, which ->
//                        for (intent in getAvailableIntents(context)) {
//                            context.startActivity(intent)
//                        }
//                        sharedPreferences!!.edit().putBoolean(PROTECTED, true).apply()
//                    }
//                    .setCancelable(false)
//                    .create()
//                dialog.show()
//            }
//        }
//    }
}
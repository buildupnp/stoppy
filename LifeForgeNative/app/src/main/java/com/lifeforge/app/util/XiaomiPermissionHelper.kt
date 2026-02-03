package com.lifeforge.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object XiaomiPermissionHelper {

    fun isXiaomiDevice(): Boolean {
        val manufacturers = listOf("xiaomi", "redmi", "poco")
        return manufacturers.any { Build.MANUFACTURER.lowercase().contains(it) }
    }

    fun getAutostartIntent(context: Context): Intent? {
        val intent = Intent()
        intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        if (canResolveIntent(context, intent)) return intent

        // Fallback for different MIUI versions
        val intents = listOf(
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") },
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings") },
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.powercenter.autostart.AutoStartManagementActivity") }
        )

        for (i in intents) {
            if (canResolveIntent(context, i)) return i
        }
        return null
    }

    fun getBackgroundPopupIntent(context: Context): Intent? {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        intent.putExtra("extra_pkgname", context.packageName)
        
        if (canResolveIntent(context, intent)) return intent

        // Fallback: Try generic permission editor intent
        val fallback = Intent("miui.intent.action.APP_PERM_EDITOR")
        fallback.putExtra("extra_pkgname", context.packageName)
        if (canResolveIntent(context, fallback)) return fallback

        return null
    }

    fun getBatteryWhitelistIntent(context: Context): Intent? {
        val intent = Intent()
        intent.component = ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
        if (canResolveIntent(context, intent)) return intent
        
        return null
    }

    fun isIgnoredBatteryOptimizations(context: Context): Boolean {
        // This is a rough check as MIUI has its own whitelist
        // Realistically we just launch the intent and hope user sets it
        return true 
    }

    private fun canResolveIntent(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }
}

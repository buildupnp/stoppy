package com.lifeforge.app.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun getTodayUsage(packageNames: List<String>): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageMap = mutableMapOf<String, Long>()
        
        try {
            // queryUsageStats usually returns a list. aggregate is better.
            val stats = usageStatsManager.queryAndAggregateUsageStats(
                startTime,
                endTime
            )
            
            packageNames.forEach { pkg ->
                val usage = stats[pkg]
                if (usage != null) {
                    usageMap[pkg] = usage.totalTimeInForeground
                } else {
                    usageMap[pkg] = 0L
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return usageMap
    }
    
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}

package com.lifeforge.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lifeforge.app.data.local.database.dao.ActivityDao
import com.lifeforge.app.data.local.database.dao.AppLockDao
import com.lifeforge.app.data.local.database.dao.CoinDao
import com.lifeforge.app.data.local.database.dao.FeatureDao
import com.lifeforge.app.data.local.database.entities.Achievement
import com.lifeforge.app.data.local.database.entities.ActivitySession
import com.lifeforge.app.data.local.database.entities.AppUnlock
import com.lifeforge.app.data.local.database.entities.CoinTransaction
import com.lifeforge.app.data.local.database.entities.ManagedAppEntity
import com.lifeforge.app.data.local.database.entities.UserPreferences
import com.lifeforge.app.data.local.database.entities.WeeklyChallenge

@Database(
    entities = [
        ActivitySession::class,
        CoinTransaction::class,
        ManagedAppEntity::class,
        AppUnlock::class,
        Achievement::class,
        WeeklyChallenge::class,
        UserPreferences::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun coinDao(): CoinDao
    abstract fun appLockDao(): AppLockDao
    abstract fun featureDao(): FeatureDao
}


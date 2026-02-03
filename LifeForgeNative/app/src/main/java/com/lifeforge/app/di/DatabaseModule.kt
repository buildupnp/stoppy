package com.lifeforge.app.di

import android.content.Context
import androidx.room.Room
import com.lifeforge.app.data.local.database.AppDatabase
import com.lifeforge.app.data.local.database.dao.ActivityDao
import com.lifeforge.app.data.local.database.dao.AppLockDao
import com.lifeforge.app.data.local.database.dao.CoinDao
import com.lifeforge.app.data.local.database.dao.FeatureDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lifeforge_database"
        )
            .fallbackToDestructiveMigration() // For development - handles schema changes
            .build()
    }
    
    @Provides
    fun provideActivityDao(database: AppDatabase): ActivityDao {
        return database.activityDao()
    }
    
    @Provides
    fun provideCoinDao(database: AppDatabase): CoinDao {
        return database.coinDao()
    }
    
    @Provides
    fun provideAppLockDao(database: AppDatabase): AppLockDao {
        return database.appLockDao()
    }
    
    @Provides
    fun provideFeatureDao(database: AppDatabase): FeatureDao {
        return database.featureDao()
    }
}


package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SiteEntity::class,
        PostEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        CustomerEntity::class,
        PluginEntity::class,
        CouponEntity::class,
        NotificationItemEntity::class,
        NotificationSettingsEntity::class,
        DashboardWidgetEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun postDao(): PostDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun pluginDao(): PluginDao
    abstract fun couponDao(): CouponDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao
    abstract fun dashboardWidgetDao(): DashboardWidgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add isDemo column to sites table with default of 0 (false)
                db.execSQL("ALTER TABLE sites ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                // Safely drop the deprecated water_telemetry table
                db.execSQL("DROP TABLE IF EXISTS water_telemetry")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wpmobile_hub_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    exportSchema = false
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wpmobile_hub_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

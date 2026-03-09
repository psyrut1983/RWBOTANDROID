package com.rwbot.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewEntity

@Database(
    entities = [ReviewEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}

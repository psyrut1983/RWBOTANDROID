package com.rwbot.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewEntity

/** Миграция: добавление колонки артикула продавца. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reviews ADD COLUMN supplierArticle TEXT")
    }
}

@Database(
    entities = [ReviewEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}

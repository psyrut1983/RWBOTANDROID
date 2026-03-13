package com.rwbot.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rwbot.android.data.local.dao.ReviewArchiveDao
import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewArchiveEntity
import com.rwbot.android.data.local.entity.ReviewEntity

/** Миграция: добавление колонки артикула продавца. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reviews ADD COLUMN supplierArticle TEXT")
    }
}

/** Миграция: таблица архива отзывов для RAG. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS review_archive (
                id TEXT PRIMARY KEY NOT NULL,
                reviewText TEXT NOT NULL,
                responseText TEXT,
                embedding BLOB,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

@Database(
    entities = [ReviewEntity::class, ReviewArchiveEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun reviewArchiveDao(): ReviewArchiveDao
}

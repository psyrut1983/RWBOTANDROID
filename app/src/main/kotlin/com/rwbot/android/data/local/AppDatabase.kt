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
                rating INTEGER NOT NULL DEFAULT 0,
                responseText TEXT,
                embedding BLOB,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/** Миграция: добавляем оценку (rating) в архив RAG. */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE review_archive ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [ReviewEntity::class, ReviewArchiveEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun reviewArchiveDao(): ReviewArchiveDao
}

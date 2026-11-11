package com.f7developer.tapeback.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CassetteEntity::class, RecordingEntity::class, RecordingTagEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TapeDatabase : RoomDatabase() {
    abstract fun cassetteDao(): CassetteDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        const val NAME = "tapeback.db"

        fun create(context: Context): TapeDatabase =
            Room.databaseBuilder(context.applicationContext, TapeDatabase::class.java, NAME)
                .addCallback(ForeignKeysOn)
                .build()

        /** Make ON DELETE CASCADE effective on every connection. */
        private object ForeignKeysOn : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
    }
}

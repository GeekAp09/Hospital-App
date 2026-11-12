package com.mobile360.healthcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class, DoctorEntity::class, SlotEntity::class, AppointmentEntity::class,
        PrescriptionEntity::class, MedicationEntity::class, TimelineEntity::class, NoteEntity::class,
        MessageEntity::class, AvailabilityEntity::class, FeedbackEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CareDao
}

// CR-001: adds the feedback table and keeps all existing data
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `feedback` (`id` TEXT NOT NULL, `ownerId` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, `message` TEXT NOT NULL, `createdMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_feedback_ownerId` ON `feedback` (`ownerId`)")
    }
}

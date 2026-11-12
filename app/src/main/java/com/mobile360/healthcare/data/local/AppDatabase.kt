package com.mobile360.healthcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// CR-001: FeedbackEntity added, version bumped (destructive migration is enabled)
@Database(
    entities = [
        AccountEntity::class, DoctorEntity::class, SlotEntity::class, AppointmentEntity::class,
        PrescriptionEntity::class, MedicationEntity::class, TimelineEntity::class, NoteEntity::class,
        MessageEntity::class, AvailabilityEntity::class, FeedbackEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CareDao
}

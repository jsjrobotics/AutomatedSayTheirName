package com.spookyrobotics.automatedsaytheirnames.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PoliceVictimEntry::class], version = 2)
abstract class DatabaseWrapper : RoomDatabase() {
    abstract fun contentDao(): OfflineContentDao
}
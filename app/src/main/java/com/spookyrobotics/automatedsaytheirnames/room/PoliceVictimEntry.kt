package com.spookyrobotics.automatedsaytheirnames.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_BIRTHDATE
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_NAME
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_DEATHDATE
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_PRIMARY_KEY
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_DETAILS
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManifestTableConstants.COLUMN_LOCATION

@Entity(tableName = ManifestTableConstants.NAME)
data class PoliceVictimEntry(
        @ColumnInfo(name = COLUMN_BIRTHDATE) val birthDate: Long,
        @ColumnInfo(name = COLUMN_DEATHDATE) val deathDate: Long,
        @ColumnInfo(name = COLUMN_LOCATION) val location: String,
        @ColumnInfo(name = COLUMN_DETAILS) val details: String,
        @ColumnInfo(name = COLUMN_NAME) val name: String,
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = COLUMN_PRIMARY_KEY) val primaryKey: Int = 0
)

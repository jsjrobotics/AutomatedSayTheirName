package com.spookyrobotics.automatedsaytheirnames.room

object OfflineDatabaseConstants {
    const val DATABASE_FILENAME = "offline_database"

    object ManifestTableConstants {
        const val NAME = "police_victim_entries"
        const val COLUMN_NAME = "name"
        const val COLUMN_BIRTHDATE = "birthdate"
        const val COLUMN_DEATHDATE = "deathdate"

        const val COLUMN_LOCATION = "location"
        const val COLUMN_DETAILS = "details"
        const val COLUMN_PRIMARY_KEY = "primary_key"
    }
    object ManfiestTableOperations {
        const val QUERY_ALL_ENTRIES = "SELECT * from ${ManifestTableConstants.NAME} ORDER BY ${ManifestTableConstants.COLUMN_DEATHDATE}"
        const val DELETE_ALL_ENTRIES = "DELETE FROM ${ManifestTableConstants.NAME}"

    }




}
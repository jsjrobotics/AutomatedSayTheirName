package com.spookyrobotics.automatedsaytheirnames.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManfiestTableOperations.DELETE_ALL_ENTRIES
import com.spookyrobotics.automatedsaytheirnames.room.OfflineDatabaseConstants.ManfiestTableOperations.QUERY_ALL_ENTRIES
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineContentDao {
    @Query(QUERY_ALL_ENTRIES)
    suspend fun getAll(): List<PoliceVictimEntry>

    @Insert
    suspend fun insertEntry(entry: PoliceVictimEntry)

    @Update
    suspend fun updateEntry(entry: PoliceVictimEntry)

    @Query(DELETE_ALL_ENTRIES)
    suspend fun deleteAll()

    @Query(QUERY_ALL_ENTRIES)
    fun getAllObservable(): Flow<List<PoliceVictimEntry>>
}

package com.example.dataguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dataguard.data.model.DailySnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySnapshotDao {

    @Query("SELECT * FROM daily_snapshots ORDER BY date DESC")
    fun getAllSnapshots(): Flow<List<DailySnapshot>>

    @Query("SELECT * FROM daily_snapshots WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSnapshotsBetween(startDate: String, endDate: String): Flow<List<DailySnapshot>>

    @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
    suspend fun getSnapshotByDate(date: String): DailySnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(snapshot: DailySnapshot)

    @Query("DELETE FROM daily_snapshots WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("SELECT COUNT(*) FROM daily_snapshots")
    suspend fun getSnapshotCount(): Int
}

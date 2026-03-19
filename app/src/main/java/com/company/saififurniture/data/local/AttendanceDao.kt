package com.saififurnitures.app.data.local

import androidx.room.*

@Dao
interface AttendanceDao {

    @Insert
    suspend fun insert(attendance: AttendanceEntity): Long

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE isSynced = 0 AND userId = :userId ORDER BY timestamp ASC")
    suspend fun getUnsyncedAttendance(userId: String): List<AttendanceEntity>

    @Query("SELECT COUNT(*) FROM attendance WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: String): Int

    @Query("UPDATE attendance SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE attendance SET isSynced = 1, address = :address WHERE id = :id")
    suspend fun markSyncedWithAddress(id: Long, address: String)

    @Query("UPDATE attendance SET isSynced = 1, address = :address, selfieUrl = :selfieUrl WHERE id = :id")
    suspend fun markSyncedWithDetails(id: Long, address: String, selfieUrl: String)

    // Today only
    @Query("""
        SELECT * FROM attendance
        WHERE userId = :userId
        AND DATE(timestamp/1000, 'unixepoch') = DATE('now')
        ORDER BY timestamp ASC
    """)
    suspend fun getTodayAttendance(userId: String): List<AttendanceEntity>

    // Date range — startDate and endDate in format 'yyyy-MM-dd'
    @Query("""
        SELECT * FROM attendance
        WHERE userId = :userId
        AND DATE(timestamp/1000, 'unixepoch') BETWEEN :startDate AND :endDate
        ORDER BY timestamp ASC
    """)
    suspend fun getAttendanceByRange(userId: String, startDate: String, endDate: String): List<AttendanceEntity>

    // Open session — check-in with no matching check-out today
    @Query("""
        SELECT * FROM attendance
        WHERE userId = :userId
        AND type = 'check_in'
        AND DATE(timestamp/1000, 'unixepoch') = DATE('now')
        AND sessionId NOT IN (
            SELECT sessionId FROM attendance
            WHERE type = 'check_out' AND userId = :userId
        )
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getOpenSession(userId: String): AttendanceEntity?

    @Query("DELETE FROM attendance WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
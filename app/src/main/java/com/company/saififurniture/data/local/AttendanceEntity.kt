package com.saififurnitures.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["isSynced", "userId"], name = "idx_att_synced"),
        Index(value = ["userId"],             name = "idx_att_user"),
        Index(value = ["sessionId"],          name = "idx_att_session")
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val employeeId: String = "",
    val type: String = "check_in",
    val sessionId: String = "",
    val selfiePath: String = "",      // local file path
    val selfieUrl: String = "",       // Cloudinary URL after upload
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val timestamp: Long = 0L,
    val isSynced: Boolean = false
)
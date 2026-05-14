package com.aushadh.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val timestamp: Long,
    val fileType: String, // "PDF" or "IMAGE"
    val tags: String,
    val doctorName: String
)

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE tags LIKE '%' || :query || '%' OR doctorName LIKE '%' || :query || '%'")
    fun searchRecords(query: String): Flow<List<MedicalRecord>>

    @Insert
    suspend fun insert(record: MedicalRecord)

    @Update
    suspend fun update(record: MedicalRecord)

    @Delete
    suspend fun delete(record: MedicalRecord)
}

@Database(entities = [MedicalRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalRecordDao(): MedicalRecordDao
}

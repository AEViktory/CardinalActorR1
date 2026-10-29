package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.ExecutionEventEntity
import com.example.core.database.entity.MissionEntity
import com.example.core.database.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)

    @Update
    suspend fun updateMission(mission: MissionEntity)

    @Query("SELECT * FROM missions ORDER BY createdAt DESC LIMIT 50")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun getMissionById(id: String): MissionEntity?

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun deleteMission(id: String)
}

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<StepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: StepEntity)

    @Query("SELECT * FROM mission_steps WHERE missionId = :missionId ORDER BY stepIndex ASC")
    suspend fun getStepsForMission(missionId: String): List<StepEntity>

    @Query("SELECT * FROM mission_steps WHERE missionId = :missionId ORDER BY stepIndex ASC")
    fun getStepsForMissionFlow(missionId: String): Flow<List<StepEntity>>
}

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ExecutionEventEntity)

    @Query("SELECT * FROM execution_events WHERE missionId = :missionId ORDER BY timestamp ASC")
    suspend fun getEventsForMission(missionId: String): List<ExecutionEventEntity>

    @Query("SELECT * FROM execution_events WHERE missionId = :missionId ORDER BY timestamp ASC")
    fun getEventsForMissionFlow(missionId: String): Flow<List<ExecutionEventEntity>>
}

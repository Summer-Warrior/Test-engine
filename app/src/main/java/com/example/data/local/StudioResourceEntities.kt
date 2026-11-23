package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

enum class ResourceCategory(val id: String, val label: String, val shortBadge: String) {
  SFX("sfx", "Sound Effects", "SFX"),
  BGM("bgm", "Background Music", "BGM"),
  MAP_TILES("map_tiles", "Map Tiles", "TILE"),
}

@Entity(tableName = "studio_resources")
data class StudioResourceEntity(
  @androidx.room.PrimaryKey val id: String,
  val name: String,
  val categoryId: String,
  val formatTag: String,
  val consoleChip: String,
  val durationOrGrid: String,
  val payloadSpec: String,
  val accentHex: String,
  val frequencyHz: Int = 440,
  val tileTypeIndex: Int = -1,
  val assignedProjectIdsCsv: String = "",
  val description: String,
  val isBuiltIn: Boolean = true,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
) {
  fun isAssignedToProject(projectId: String?): Boolean {
    if (projectId.isNullOrBlank()) return false
    return assignedProjectIdsCsv.split(",")
      .map { it.trim() }
      .any { it == projectId }
  }

  fun assignedProjectCount(): Int {
    if (assignedProjectIdsCsv.isBlank()) return 0
    return assignedProjectIdsCsv.split(",").count { it.isNotBlank() }
  }
}

@Entity(tableName = "encyclopedia_entries")
data class EncyclopediaEntryEntity(
  @androidx.room.PrimaryKey val id: String,
  val title: String,
  val domainCategory: String, // "Species Biology", "Competitive Stats & Moves", "Locations & Items", "Mechanics & Anime"
  val sourceOrigin: String,   // "Bulbapedia", "PokemonDB", "Serebii.net", "All 3 Canon Sources"
  val sourceUrl: String,
  val dexRangeOrTag: String,
  val summary: String,
  val structuredData: String,
  val lastUpdatedEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "encyclopedia_sources")
data class EncyclopediaSourceStatusEntity(
  @androidx.room.PrimaryKey val sourceId: String,
  val displayName: String,
  val baseUrl: String,
  val lastSyncEpochMs: Long,
  val httpStatusCode: Int,
  val liveHeadline: String,
  val entryCount: Int,
)

@Dao
interface StudioResourceDao {
  @Query("SELECT * FROM studio_resources ORDER BY isBuiltIn ASC, updatedAtEpochMs DESC")
  fun observeAllResources(): Flow<List<StudioResourceEntity>>

  @Query("SELECT * FROM studio_resources ORDER BY id ASC")
  suspend fun getAllSnapshot(): List<StudioResourceEntity>

  @Query("SELECT * FROM studio_resources WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): StudioResourceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(items: List<StudioResourceEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertResource(item: StudioResourceEntity)

  @Query("DELETE FROM studio_resources WHERE id = :id AND isBuiltIn = 0")
  suspend fun deleteCustomResource(id: String)
}

@Dao
interface EncyclopediaDao {
  @Query("SELECT * FROM encyclopedia_entries ORDER BY id ASC")
  fun observeAllEntries(): Flow<List<EncyclopediaEntryEntity>>

  @Query("SELECT * FROM encyclopedia_sources ORDER BY displayName ASC")
  fun observeSourceStatuses(): Flow<List<EncyclopediaSourceStatusEntity>>

  @Query("SELECT COUNT(*) FROM encyclopedia_entries")
  suspend fun countEntries(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertEntries(entries: List<EncyclopediaEntryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSourceStatus(status: EncyclopediaSourceStatusEntity)
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "hardware_profiles")
data class HardwareProfileEntity(
  @PrimaryKey val id: String,
  val name: String,
  val manufacturer: String,
  val year: Int,
  val generation: String,
  val category: String,
  val gpuChip: String,
  val audioChip: String,
  val videoModes: String,
  val primaryDomain: String,
  val honoredSignature: String,
  val brokenLimitation: String,
  val graphicsJson: String,
  val audioJson: String,
)

@Entity(tableName = "saved_sprites")
data class SavedSpriteEntity(
  @PrimaryKey val id: String,
  val projectId: String = "",
  val name: String,
  val spriteSeedId: Int,
  val paletteJson: String,
  val framesRleJson: String,
  val frameCount: Int,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "saved_maps")
data class SavedMapEntity(
  @PrimaryKey val mapKey: String, // "$projectId:$regionId"
  val projectId: String,
  val regionId: String,
  val regionName: String,
  val gridSize: Int = 8,
  val tilesCsv: String,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "studio_items")
data class StudioItemEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String, // POKE_BALL, MEDICINE, HOLD_ITEM, EVOLUTION, BERRY, TM, GIMMICK_CATALYST, KEY_ITEM
  val description: String,
  val effectTag: String,
  val effectPower: Float = 1.0f,
  val quantity: Int = 1,
  val accentHex: String = "#00F5D4",
  val regionOrigin: String = "All Regions",
)

@Entity(tableName = "atlas_progress")
data class AtlasProgressEntity(
  @PrimaryKey val dexNumber: Int,
  val seen: Boolean = false,
  val battled: Boolean = false,
  val captured: Boolean = false,
  val hardwareHarmonized: Boolean = false,
  val harmonizedHardwareId: String = "",
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "campaign_state")
data class CampaignStateEntity(
  @PrimaryKey val projectId: String,
  val activeRegionId: String = "kanto",
  val playerX: Int = 2,
  val playerY: Int = 5,
  val partyDexIdsCsv: String = "25,6,94,448,658,384",
  val heldItemsMapCsv: String = "25:life_orb,6:choice_specs,94:focus_sash,448:choice_band,658:choice_scarf,384:leftovers",
  val completedRegionIdsCsv: String = "",
  val harmonicShardsCsv: String = "",
  val activeHardwareIdsCsv: String = "",
  val disciplineModeToken: String = "SHOVEL_KNIGHT_CURATED",
  val extraPrettyMask: Int = 0,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Dao
interface StudioPersistenceDao {
  // Hardware profiles
  @Query("SELECT * FROM hardware_profiles ORDER BY year ASC, name ASC")
  fun observeHardwareProfiles(): Flow<List<HardwareProfileEntity>>

  @Query("SELECT COUNT(*) FROM hardware_profiles")
  suspend fun countHardwareProfiles(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHardwareProfiles(items: List<HardwareProfileEntity>)

  // Saved sprites
  @Query("SELECT * FROM saved_sprites ORDER BY updatedAtEpochMs DESC")
  fun observeAllSavedSprites(): Flow<List<SavedSpriteEntity>>

  @Query("SELECT * FROM saved_sprites WHERE id = :id LIMIT 1")
  suspend fun getSavedSpriteById(id: String): SavedSpriteEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedSprite(sprite: SavedSpriteEntity)

  @Query("DELETE FROM saved_sprites WHERE id = :id")
  suspend fun deleteSavedSprite(id: String)

  // Saved maps
  @Query("SELECT * FROM saved_maps WHERE projectId = :projectId ORDER BY regionId ASC")
  fun observeMapsForProject(projectId: String): Flow<List<SavedMapEntity>>

  @Query("SELECT * FROM saved_maps WHERE mapKey = :mapKey LIMIT 1")
  suspend fun getSavedMap(mapKey: String): SavedMapEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedMap(map: SavedMapEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedMaps(maps: List<SavedMapEntity>)

  // Studio items
  @Query("SELECT * FROM studio_items ORDER BY category ASC, name ASC")
  fun observeAllItems(): Flow<List<StudioItemEntity>>

  @Query("SELECT COUNT(*) FROM studio_items")
  suspend fun countItems(): Int

  @Query("SELECT * FROM studio_items WHERE id = :id LIMIT 1")
  suspend fun getItemById(id: String): StudioItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItems(items: List<StudioItemEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItem(item: StudioItemEntity)

  // Atlas progress
  @Query("SELECT * FROM atlas_progress ORDER BY dexNumber ASC")
  fun observeAtlasProgress(): Flow<List<AtlasProgressEntity>>

  @Query("SELECT * FROM atlas_progress WHERE dexNumber = :dexNumber LIMIT 1")
  suspend fun getAtlasProgress(dexNumber: Int): AtlasProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAtlasProgress(entry: AtlasProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAtlasProgressList(entries: List<AtlasProgressEntity>)

  // Campaign state
  @Query("SELECT * FROM campaign_state WHERE projectId = :projectId LIMIT 1")
  fun observeCampaignState(projectId: String): Flow<CampaignStateEntity?>

  @Query("SELECT * FROM campaign_state WHERE projectId = :projectId LIMIT 1")
  suspend fun getCampaignState(projectId: String): CampaignStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCampaignState(state: CampaignStateEntity)
}

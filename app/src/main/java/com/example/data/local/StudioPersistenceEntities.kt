package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "fan_game_projects")
data class FanGameProjectEntity(
  @PrimaryKey val id: String,
  val title: String,
  val regionName: String,
  val synopsis: String,
  val consoleMatrixPreset: String,
  val activeChipsCsv: String,
  val pixel3dBlendRatio: Long,
  val dexCount: Long,
  val mapCount: Long,
  val versionTag: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
) {
  fun toDomain(): FanGameProject = FanGameProject(
    id = id,
    title = title,
    regionName = regionName,
    synopsis = synopsis,
    consoleMatrixPreset = consoleMatrixPreset,
    activeChipsCsv = activeChipsCsv,
    pixel3dBlendRatio = pixel3dBlendRatio,
    dexCount = dexCount,
    mapCount = mapCount,
    versionTag = versionTag,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
  )

  companion object {
    fun fromDomain(project: FanGameProject): FanGameProjectEntity = FanGameProjectEntity(
      id = project.id,
      title = project.title,
      regionName = project.regionName,
      synopsis = project.synopsis,
      consoleMatrixPreset = project.consoleMatrixPreset,
      activeChipsCsv = project.activeChipsCsv,
      pixel3dBlendRatio = project.pixel3dBlendRatio,
      dexCount = project.dexCount,
      mapCount = project.mapCount,
      versionTag = project.versionTag,
      createdAtMs = project.createdAtMs,
      updatedAtMs = project.updatedAtMs,
    )
  }
}

@Entity(tableName = "custom_dex_entries")
data class CustomDexRoomEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val dexNumber: Long,
  val name: String,
  val category: String,
  val primaryType: String,
  val secondaryType: String,
  val abilityName: String,
  val signatureMove: String,
  val loreDescription: String,
  val baseHp: Long,
  val baseAttack: Long,
  val baseDefense: Long,
  val baseSpAtk: Long,
  val baseSpDef: Long,
  val baseSpeed: Long,
  val baseSpriteId: Long,
  val fusionStylePreset: String,
  val enhancementChip: String,
  val primaryHex: String,
  val secondaryHex: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
) {
  fun toDomain(): CustomDexEntry = CustomDexEntry(
    id = id,
    projectId = projectId,
    dexNumber = dexNumber,
    name = name,
    category = category,
    primaryType = primaryType,
    secondaryType = secondaryType,
    abilityName = abilityName,
    signatureMove = signatureMove,
    loreDescription = loreDescription,
    baseHp = baseHp,
    baseAttack = baseAttack,
    baseDefense = baseDefense,
    baseSpAtk = baseSpAtk,
    baseSpDef = baseSpDef,
    baseSpeed = baseSpeed,
    baseSpriteId = baseSpriteId,
    fusionStylePreset = fusionStylePreset,
    enhancementChip = enhancementChip,
    primaryHex = primaryHex,
    secondaryHex = secondaryHex,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
  )

  companion object {
    fun fromDomain(entry: CustomDexEntry): CustomDexRoomEntity = CustomDexRoomEntity(
      id = entry.id,
      projectId = entry.projectId,
      dexNumber = entry.dexNumber,
      name = entry.name,
      category = entry.category,
      primaryType = entry.primaryType,
      secondaryType = entry.secondaryType,
      abilityName = entry.abilityName,
      signatureMove = entry.signatureMove,
      loreDescription = entry.loreDescription,
      baseHp = entry.baseHp,
      baseAttack = entry.baseAttack,
      baseDefense = entry.baseDefense,
      baseSpAtk = entry.baseSpAtk,
      baseSpDef = entry.baseSpDef,
      baseSpeed = entry.baseSpeed,
      baseSpriteId = entry.baseSpriteId,
      fusionStylePreset = entry.fusionStylePreset,
      enhancementChip = entry.enhancementChip,
      primaryHex = entry.primaryHex,
      secondaryHex = entry.secondaryHex,
      createdAtMs = entry.createdAtMs,
      updatedAtMs = entry.updatedAtMs,
    )
  }
}

@Entity(tableName = "custom_studio_assets")
data class CustomAssetRoomEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val name: String,
  val category: String,
  val consoleLineage: String,
  val chipAddon: String,
  val spriteSeedId: Long,
  val shaderFormula: String,
  val createdAtMs: Long,
  val updatedAtMs: Long,
) {
  fun toDomain(): CustomStudioAsset = CustomStudioAsset(
    id = id,
    projectId = projectId,
    name = name,
    category = category,
    consoleLineage = consoleLineage,
    chipAddon = chipAddon,
    spriteSeedId = spriteSeedId,
    shaderFormula = shaderFormula,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
  )

  companion object {
    fun fromDomain(asset: CustomStudioAsset): CustomAssetRoomEntity = CustomAssetRoomEntity(
      id = asset.id,
      projectId = asset.projectId,
      name = asset.name,
      category = asset.category,
      consoleLineage = asset.consoleLineage,
      chipAddon = asset.chipAddon,
      spriteSeedId = asset.spriteSeedId,
      shaderFormula = asset.shaderFormula,
      createdAtMs = asset.createdAtMs,
      updatedAtMs = asset.updatedAtMs,
    )
  }
}

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
  val gridSize: Int = 16,
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
  val playerX: Int = 3,
  val playerY: Int = 13,
  val partyDexIdsCsv: String = "25,6,94,448,658,384",
  val heldItemsMapCsv: String = "25:life_orb,6:choice_specs,94:focus_sash,448:choice_band,658:choice_scarf,384:leftovers",
  val completedRegionIdsCsv: String = "",
  val harmonicShardsCsv: String = "",
  val activeHardwareIdsCsv: String = "",
  val disciplineModeToken: String = "SHOVEL_KNIGHT_CURATED",
  val extraPrettyMask: Int = 0,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
  val partyLevelsCsv: String = "25:50,6:50,94:50,448:50,658:50,384:50",
  val taughtMovesCsv: String = "",
)

@Dao
interface FanGameProjectDao {
  @Query("SELECT * FROM fan_game_projects ORDER BY updatedAtMs DESC")
  fun observeAllProjects(): Flow<List<FanGameProjectEntity>>

  @Query("SELECT * FROM fan_game_projects ORDER BY updatedAtMs DESC")
  suspend fun getAllProjectsSnapshot(): List<FanGameProjectEntity>

  @Query("SELECT * FROM fan_game_projects WHERE id = :projectId LIMIT 1")
  suspend fun getProjectById(projectId: String): FanGameProjectEntity?

  @Query("SELECT COUNT(*) FROM fan_game_projects")
  suspend fun countProjects(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProject(project: FanGameProjectEntity)

  @Query("DELETE FROM fan_game_projects WHERE id = :projectId")
  suspend fun deleteProjectById(projectId: String)
}

@Dao
interface CustomDexDao {
  @Query("SELECT * FROM custom_dex_entries WHERE projectId = :projectId ORDER BY dexNumber ASC")
  fun observeDexEntries(projectId: String): Flow<List<CustomDexRoomEntity>>

  @Query("SELECT * FROM custom_dex_entries WHERE projectId = :projectId ORDER BY dexNumber ASC")
  suspend fun getDexEntriesSnapshot(projectId: String): List<CustomDexRoomEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDexEntry(entry: CustomDexRoomEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDexEntries(entries: List<CustomDexRoomEntity>)

  @Query("DELETE FROM custom_dex_entries WHERE id = :entryId")
  suspend fun deleteDexEntry(entryId: String)

  @Query("DELETE FROM custom_dex_entries WHERE projectId = :projectId")
  suspend fun deleteDexEntriesForProject(projectId: String)
}

@Dao
interface CustomAssetDao {
  @Query("SELECT * FROM custom_studio_assets WHERE projectId = :projectId ORDER BY updatedAtMs DESC")
  fun observeAssets(projectId: String): Flow<List<CustomAssetRoomEntity>>

  @Query("SELECT * FROM custom_studio_assets WHERE projectId = :projectId ORDER BY updatedAtMs DESC")
  suspend fun getAssetsSnapshot(projectId: String): List<CustomAssetRoomEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAsset(asset: CustomAssetRoomEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAssets(assets: List<CustomAssetRoomEntity>)

  @Query("DELETE FROM custom_studio_assets WHERE id = :assetId")
  suspend fun deleteAsset(assetId: String)

  @Query("DELETE FROM custom_studio_assets WHERE projectId = :projectId")
  suspend fun deleteAssetsForProject(projectId: String)
}

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

  @Query("SELECT * FROM saved_sprites WHERE projectId = :projectId ORDER BY updatedAtEpochMs DESC")
  suspend fun getSavedSpritesForProjectSnapshot(projectId: String): List<SavedSpriteEntity>

  @Query("SELECT * FROM saved_sprites ORDER BY updatedAtEpochMs DESC")
  suspend fun getAllSavedSpritesSnapshot(): List<SavedSpriteEntity>

  @Query("SELECT * FROM saved_sprites WHERE id = :id LIMIT 1")
  suspend fun getSavedSpriteById(id: String): SavedSpriteEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedSprite(sprite: SavedSpriteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedSprites(sprites: List<SavedSpriteEntity>)

  @Query("DELETE FROM saved_sprites WHERE id = :id")
  suspend fun deleteSavedSprite(id: String)

  @Query("DELETE FROM saved_sprites WHERE projectId = :projectId")
  suspend fun deleteSavedSpritesForProject(projectId: String)

  // Saved maps
  @Query("SELECT * FROM saved_maps WHERE projectId = :projectId ORDER BY regionId ASC")
  fun observeMapsForProject(projectId: String): Flow<List<SavedMapEntity>>

  @Query("SELECT * FROM saved_maps WHERE projectId = :projectId ORDER BY regionId ASC")
  suspend fun getMapsForProjectSnapshot(projectId: String): List<SavedMapEntity>

  @Query("SELECT * FROM saved_maps WHERE mapKey = :mapKey LIMIT 1")
  suspend fun getSavedMap(mapKey: String): SavedMapEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedMap(map: SavedMapEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedMaps(maps: List<SavedMapEntity>)

  @Query("DELETE FROM saved_maps WHERE projectId = :projectId")
  suspend fun deleteMapsForProject(projectId: String)

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

  @Query("DELETE FROM campaign_state WHERE projectId = :projectId")
  suspend fun deleteCampaignState(projectId: String)
}

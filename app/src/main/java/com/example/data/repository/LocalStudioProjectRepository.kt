package com.example.data.repository

import com.example.data.local.CampaignStateEntity
import com.example.data.local.CustomAssetDao
import com.example.data.local.CustomAssetRoomEntity
import com.example.data.local.CustomDexDao
import com.example.data.local.CustomDexRoomEntity
import com.example.data.local.CustomTrainerDao
import com.example.data.local.CustomTrainerEntity
import com.example.data.local.FanGameProjectDao
import com.example.data.local.FanGameProjectEntity
import com.example.data.local.SavedMapEntity
import com.example.data.local.SavedSpriteEntity
import com.example.data.local.SideModeRecordEntity
import com.example.data.local.StudioPersistenceDao
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import com.example.engine.MasterCanonTemplateCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 100% Offline Single-Source-of-Truth Studio Project Repository backed exclusively by Room v6 DAOs.
 * Eliminates all network/auth gates and provides full JSON project export/import via SAF.
 */
class LocalStudioProjectRepository(
  private val projectDao: FanGameProjectDao,
  private val customDexDao: CustomDexDao,
  private val customAssetDao: CustomAssetDao,
  private val studioPersistenceDao: StudioPersistenceDao,
  private val customTrainerDao: CustomTrainerDao? = null,
) {

  fun observeProjects(): Flow<List<FanGameProject>> =
    projectDao.observeAllProjects().map { list -> list.map { it.toDomain() } }

  fun observeCustomDex(projectId: String): Flow<List<CustomDexEntry>> =
    customDexDao.observeDexEntries(projectId).map { list -> list.map { it.toDomain() } }

  fun observeStudioAssets(projectId: String): Flow<List<CustomStudioAsset>> =
    customAssetDao.observeAssets(projectId).map { list -> list.map { it.toDomain() } }

  /**
   * Ensures the default "Pokémon: Prism Resonance" 22-Region All-Hardware Master Project
   * exists on first launch so the workspace boots immediately into an active playable project.
   */
  suspend fun ensureDefaultMasterProjectSeeded(): FanGameProject = withContext(Dispatchers.IO) {
    val existing = projectDao.getAllProjectsSnapshot()
    if (existing.isNotEmpty()) {
      return@withContext existing.first().toDomain()
    }
    val now = System.currentTimeMillis()
    val template = MasterCanonTemplateCatalog.createBaseProjectTemplate().copy(
      id = DEFAULT_MASTER_PROJECT_ID,
      createdAtMs = now,
      updatedAtMs = now,
    )
    projectDao.upsertProject(FanGameProjectEntity.fromDomain(template))
    seedDefaultMapsForProject(template.id)
    template
  }

  private suspend fun seedDefaultMapsForProject(projectId: String) {
    val now = System.currentTimeMillis()
    val existingMaps = studioPersistenceDao.getMapsForProjectSnapshot(projectId).associateBy { it.regionId }
    val mapsToInsert = MasterCanonTemplateCatalog.worldChapters.mapNotNull { chapter ->
      if (existingMaps.containsKey(chapter.id)) return@mapNotNull null
      SavedMapEntity(
        mapKey = "$projectId:${chapter.id}",
        projectId = projectId,
        regionId = chapter.id,
        regionName = chapter.regionName,
        gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE,
        tilesCsv = chapter.defaultMapTiles.joinToString(","),
        updatedAtEpochMs = now,
      )
    }
    if (mapsToInsert.isNotEmpty()) {
      studioPersistenceDao.upsertSavedMaps(mapsToInsert)
    }
  }

  suspend fun createProject(project: FanGameProject): FanGameProject = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val id = project.id.ifBlank { "proj_${UUID.randomUUID().toString().take(10)}" }
    val sanitized = project.copy(
      id = id,
      title = project.title.trim().take(80).ifEmpty { "Untitled Prism Project" },
      regionName = project.regionName.trim().take(80).ifEmpty { "22-Region Multi-Source Nexus" },
      synopsis = project.synopsis.trim().take(1000),
      consoleMatrixPreset = project.consoleMatrixPreset.trim().take(120),
      activeChipsCsv = project.activeChipsCsv.trim().take(2000),
      pixel3dBlendRatio = project.pixel3dBlendRatio.coerceIn(0L, 100L),
      dexCount = project.dexCount.coerceIn(1L, 5000L),
      mapCount = project.mapCount.coerceIn(1L, 100L),
      versionTag = project.versionTag.trim().take(32).ifEmpty { "v5.0.0-PRISM" },
      createdAtMs = now,
      updatedAtMs = now,
    )
    projectDao.upsertProject(FanGameProjectEntity.fromDomain(sanitized))
    seedDefaultMapsForProject(sanitized.id)
    sanitized
  }

  suspend fun updateProject(project: FanGameProject): FanGameProject = withContext(Dispatchers.IO) {
    val updated = project.copy(
      title = project.title.trim().take(80),
      regionName = project.regionName.trim().take(80),
      synopsis = project.synopsis.trim().take(1000),
      consoleMatrixPreset = project.consoleMatrixPreset.trim().take(120),
      activeChipsCsv = project.activeChipsCsv.trim().take(2000),
      pixel3dBlendRatio = project.pixel3dBlendRatio.coerceIn(0L, 100L),
      updatedAtMs = System.currentTimeMillis(),
    )
    projectDao.upsertProject(FanGameProjectEntity.fromDomain(updated))
    updated
  }

  suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
    projectDao.deleteProjectById(projectId)
    customDexDao.deleteDexEntriesForProject(projectId)
    customAssetDao.deleteAssetsForProject(projectId)
    studioPersistenceDao.deleteMapsForProject(projectId)
    studioPersistenceDao.deleteSavedSpritesForProject(projectId)
    studioPersistenceDao.deleteCampaignState(projectId)
    studioPersistenceDao.deleteSideModeRecord(projectId)
    customTrainerDao?.deleteTrainersForProject(projectId)
    // Never leave studio with zero projects
    if (projectDao.countProjects() == 0) {
      ensureDefaultMasterProjectSeeded()
    }
  }

  suspend fun addCustomDexEntry(entry: CustomDexEntry): CustomDexEntry = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val id = entry.id.ifBlank { "dex_${UUID.randomUUID().toString().take(10)}" }
    val sanitized = entry.copy(
      id = id,
      name = entry.name.trim().take(60).ifEmpty { "Prismon" },
      category = entry.category.trim().take(60),
      primaryType = entry.primaryType.trim().take(24),
      secondaryType = entry.secondaryType.trim().take(24),
      abilityName = entry.abilityName.trim().take(60),
      signatureMove = entry.signatureMove.trim().take(60),
      loreDescription = entry.loreDescription.trim().take(500),
      baseHp = entry.baseHp.coerceIn(1L, 255L),
      baseAttack = entry.baseAttack.coerceIn(1L, 255L),
      baseDefense = entry.baseDefense.coerceIn(1L, 255L),
      baseSpAtk = entry.baseSpAtk.coerceIn(1L, 255L),
      baseSpDef = entry.baseSpDef.coerceIn(1L, 255L),
      baseSpeed = entry.baseSpeed.coerceIn(1L, 255L),
      baseSpriteId = entry.baseSpriteId.coerceIn(1L, 1025L),
      createdAtMs = now,
      updatedAtMs = now,
    )
    customDexDao.upsertDexEntry(CustomDexRoomEntity.fromDomain(sanitized))
    sanitized
  }

  suspend fun deleteCustomDexEntry(entryId: String) = withContext(Dispatchers.IO) {
    customDexDao.deleteDexEntry(entryId)
  }

  suspend fun addStudioAsset(asset: CustomStudioAsset): CustomStudioAsset = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val id = asset.id.ifBlank { "asset_${UUID.randomUUID().toString().take(10)}" }
    val sanitized = asset.copy(
      id = id,
      name = asset.name.trim().take(80).ifEmpty { "Studio Asset" },
      category = asset.category.trim().take(40),
      consoleLineage = asset.consoleLineage.trim().take(120),
      chipAddon = asset.chipAddon.trim().take(120),
      spriteSeedId = asset.spriteSeedId.coerceIn(1L, 1025L),
      shaderFormula = asset.shaderFormula.trim().take(200),
      createdAtMs = now,
      updatedAtMs = now,
    )
    customAssetDao.upsertAsset(CustomAssetRoomEntity.fromDomain(sanitized))
    sanitized
  }

  suspend fun deleteStudioAsset(assetId: String) = withContext(Dispatchers.IO) {
    customAssetDao.deleteAsset(assetId)
  }

  /**
   * Serializes a complete PokéFusion Studio v5.0 project bundle into portable JSON for SAF Export:
   * - project metadata & console matrix preset
   * - campaign state (active hardware IDs, discipline mode, Extra Pretty mask, party, levels, taught TMs, shards)
   * - custom Dex entries
   * - custom Studio assets
   * - saved 16x16 pixel sprites
   * - all 22 editable 16x16 region maps
   * - custom Trainers & Protagonist avatar profiles (Room v5)
   */
  suspend fun exportProjectToJson(projectId: String): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val projectEntity = projectDao.getProjectById(projectId)
        ?: error("Project '$projectId' not found in local Room database")
      val campaign = studioPersistenceDao.getCampaignState(projectId)
      val dexEntries = customDexDao.getDexEntriesSnapshot(projectId)
      val studioAssets = customAssetDao.getAssetsSnapshot(projectId)
      val savedSprites = studioPersistenceDao.getSavedSpritesForProjectSnapshot(projectId)
      val savedMaps = studioPersistenceDao.getMapsForProjectSnapshot(projectId)
      val customTrainers = customTrainerDao?.getTrainersForProjectSnapshot(projectId).orEmpty()
      val sideModeRecord = studioPersistenceDao.getSideModeRecord(projectId)

      val root = JSONObject().apply {
        put("schemaVersion", 6)
        put("format", "pokefusion_studio_project_v6")
        put("exportedAtMs", System.currentTimeMillis())

        put(
          "project",
          JSONObject().apply {
            put("id", projectEntity.id)
            put("title", projectEntity.title)
            put("regionName", projectEntity.regionName)
            put("synopsis", projectEntity.synopsis)
            put("consoleMatrixPreset", projectEntity.consoleMatrixPreset)
            put("activeChipsCsv", projectEntity.activeChipsCsv)
            put("pixel3dBlendRatio", projectEntity.pixel3dBlendRatio)
            put("dexCount", projectEntity.dexCount)
            put("mapCount", projectEntity.mapCount)
            put("versionTag", projectEntity.versionTag)
            put("createdAtMs", projectEntity.createdAtMs)
            put("updatedAtMs", projectEntity.updatedAtMs)
          }
        )

        if (campaign != null) {
          put(
            "campaignState",
            JSONObject().apply {
              put("activeRegionId", campaign.activeRegionId)
              put("playerX", campaign.playerX)
              put("playerY", campaign.playerY)
              put("partyDexIdsCsv", campaign.partyDexIdsCsv)
              put("heldItemsMapCsv", campaign.heldItemsMapCsv)
              put("completedRegionIdsCsv", campaign.completedRegionIdsCsv)
              put("harmonicShardsCsv", campaign.harmonicShardsCsv)
              put("activeHardwareIdsCsv", campaign.activeHardwareIdsCsv)
              put("disciplineModeToken", campaign.disciplineModeToken)
              put("extraPrettyMask", campaign.extraPrettyMask)
              put("partyLevelsCsv", campaign.partyLevelsCsv)
              put("taughtMovesCsv", campaign.taughtMovesCsv)
            }
          )
        }

        val dexArray = JSONArray()
        dexEntries.forEach { d ->
          dexArray.put(
            JSONObject().apply {
              put("id", d.id)
              put("dexNumber", d.dexNumber)
              put("name", d.name)
              put("category", d.category)
              put("primaryType", d.primaryType)
              put("secondaryType", d.secondaryType)
              put("abilityName", d.abilityName)
              put("signatureMove", d.signatureMove)
              put("loreDescription", d.loreDescription)
              put("baseHp", d.baseHp)
              put("baseAttack", d.baseAttack)
              put("baseDefense", d.baseDefense)
              put("baseSpAtk", d.baseSpAtk)
              put("baseSpDef", d.baseSpDef)
              put("baseSpeed", d.baseSpeed)
              put("baseSpriteId", d.baseSpriteId)
              put("fusionStylePreset", d.fusionStylePreset)
              put("enhancementChip", d.enhancementChip)
              put("primaryHex", d.primaryHex)
              put("secondaryHex", d.secondaryHex)
            }
          )
        }
        put("customDexEntries", dexArray)

        val assetsArray = JSONArray()
        studioAssets.forEach { a ->
          assetsArray.put(
            JSONObject().apply {
              put("id", a.id)
              put("name", a.name)
              put("category", a.category)
              put("consoleLineage", a.consoleLineage)
              put("chipAddon", a.chipAddon)
              put("spriteSeedId", a.spriteSeedId)
              put("shaderFormula", a.shaderFormula)
            }
          )
        }
        put("customStudioAssets", assetsArray)

        val spritesArray = JSONArray()
        savedSprites.forEach { s ->
          spritesArray.put(
            JSONObject().apply {
              put("id", s.id)
              put("name", s.name)
              put("spriteSeedId", s.spriteSeedId)
              put("paletteJson", s.paletteJson)
              put("framesRleJson", s.framesRleJson)
              put("frameCount", s.frameCount)
              put("updatedAtEpochMs", s.updatedAtEpochMs)
            }
          )
        }
        put("savedSprites", spritesArray)

        val mapsArray = JSONArray()
        savedMaps.forEach { m ->
          mapsArray.put(
            JSONObject().apply {
              put("regionId", m.regionId)
              put("regionName", m.regionName)
              put("gridSize", m.gridSize)
              put("tilesCsv", m.tilesCsv)
            }
          )
        }
        put("savedMaps", mapsArray)

        val trainersArray = JSONArray()
        customTrainers.forEach { t ->
          trainersArray.put(
            JSONObject().apply {
              put("id", t.id)
              put("name", t.name)
              put("title", t.title)
              put("roleCategory", t.roleCategory)
              put("regionId", t.regionId)
              put("teamDexIdsCsv", t.teamDexIdsCsv)
              put("skinHex", t.skinHex)
              put("hairHex", t.hairHex)
              put("outfitPrimaryHex", t.outfitPrimaryHex)
              put("outfitSecondaryHex", t.outfitSecondaryHex)
              put("accentHex", t.accentHex)
              put("headgearToken", t.headgearToken)
              put("outfitStyleToken", t.outfitStyleToken)
              put("hairStyleIndex", t.hairStyleIndex)
              put("companionPoseIndex", t.companionPoseIndex)
              put("introQuote", t.introQuote)
              put("defeatQuote", t.defeatQuote)
              put("isPlayerAvatar", t.isPlayerAvatar)
              put("updatedAtEpochMs", t.updatedAtEpochMs)
            }
          )
        }
        put("customTrainers", trainersArray)

        if (sideModeRecord != null) {
          put(
            "sideModeRecords",
            JSONObject().apply {
              put("battleTowerBestStreak", sideModeRecord.battleTowerBestStreak)
              put("battleTowerCurrentStreak", sideModeRecord.battleTowerCurrentStreak)
              put("battlePoints", sideModeRecord.battlePoints)
              put("contestRibbonsWon", sideModeRecord.contestRibbonsWon)
              put("contestBestScore", sideModeRecord.contestBestScore)
              put("bestSnapScore", sideModeRecord.bestSnapScore)
              put("bestSnapStarRating", sideModeRecord.bestSnapStarRating)
              put("bestSnapCaption", sideModeRecord.bestSnapCaption)
              put("deepestRiftFloor", sideModeRecord.deepestRiftFloor)
              put("riftGuardiansDefeated", sideModeRecord.riftGuardiansDefeated)
              put("rangerCapturesCompleted", sideModeRecord.rangerCapturesCompleted)
              put("campMealsServed", sideModeRecord.campMealsServed)
              put("updatedAtEpochMs", sideModeRecord.updatedAtEpochMs)
            }
          )
        }
      }
      root.toString(2)
    }
  }

  /**
   * Imports a PokéFusion Studio v5.0 (or backward-compatible v4.0) JSON project bundle into Room SQLite,
   * assigning a fresh project ID on conflict so existing user work is never overwritten.
   */
  suspend fun importProjectFromJson(jsonText: String): Result<FanGameProject> = withContext(Dispatchers.IO) {
    runCatching {
      val root = JSONObject(jsonText)
      val projJson = root.getJSONObject("project")
      val now = System.currentTimeMillis()
      val requestedId = projJson.optString("id").trim()
      val targetProjectId = if (requestedId.isNotEmpty() && projectDao.getProjectById(requestedId) == null) {
        requestedId
      } else {
        "proj_imp_${UUID.randomUUID().toString().take(8)}"
      }

      val importedProject = FanGameProject(
        id = targetProjectId,
        title = projJson.optString("title", "Imported Prism Project").trim().take(80),
        regionName = projJson.optString("regionName", "22-Region Multi-Source Nexus").trim().take(80),
        synopsis = projJson.optString("synopsis", "").trim().take(1000),
        consoleMatrixPreset = projJson.optString("consoleMatrixPreset", "Ultimate Target Style — All 156 Hardware"),
        activeChipsCsv = projJson.optString("activeChipsCsv", "SHOVEL_KNIGHT_CURATED|ALL_ON_RECORD"),
        pixel3dBlendRatio = projJson.optLong("pixel3dBlendRatio", 65L).coerceIn(0L, 100L),
        dexCount = projJson.optLong("dexCount", 1025L).coerceIn(1L, 5000L),
        mapCount = projJson.optLong("mapCount", 22L).coerceIn(1L, 100L),
        versionTag = projJson.optString("versionTag", "v5.0.0-PRISM"),
        createdAtMs = projJson.optLong("createdAtMs", now),
        updatedAtMs = now,
      )
      projectDao.upsertProject(FanGameProjectEntity.fromDomain(importedProject))

      val campJson = root.optJSONObject("campaignState")
      if (campJson != null) {
        studioPersistenceDao.upsertCampaignState(
          CampaignStateEntity(
            projectId = targetProjectId,
            activeRegionId = campJson.optString("activeRegionId", "kanto"),
            playerX = campJson.optInt("playerX", 3),
            playerY = campJson.optInt("playerY", 13),
            partyDexIdsCsv = campJson.optString("partyDexIdsCsv", "25,6,94,448,658,384"),
            heldItemsMapCsv = campJson.optString("heldItemsMapCsv", ""),
            completedRegionIdsCsv = campJson.optString("completedRegionIdsCsv", ""),
            harmonicShardsCsv = campJson.optString("harmonicShardsCsv", ""),
            activeHardwareIdsCsv = campJson.optString("activeHardwareIdsCsv", ""),
            disciplineModeToken = campJson.optString("disciplineModeToken", "SHOVEL_KNIGHT_CURATED"),
            extraPrettyMask = campJson.optInt("extraPrettyMask", 0),
            updatedAtEpochMs = now,
            partyLevelsCsv = campJson.optString("partyLevelsCsv", "25:50,6:50,94:50,448:50,658:50,384:50"),
            taughtMovesCsv = campJson.optString("taughtMovesCsv", ""),
          )
        )
      }

      val dexArr = root.optJSONArray("customDexEntries")
      if (dexArr != null && dexArr.length() > 0) {
        val entries = (0 until dexArr.length()).map { idx ->
          val o = dexArr.getJSONObject(idx)
          CustomDexRoomEntity(
            id = "dex_${UUID.randomUUID().toString().take(10)}",
            projectId = targetProjectId,
            dexNumber = o.optLong("dexNumber", 1026L + idx),
            name = o.optString("name", "Imported Species"),
            category = o.optString("category", "Fusion Species"),
            primaryType = o.optString("primaryType", "Dragon"),
            secondaryType = o.optString("secondaryType", ""),
            abilityName = o.optString("abilityName", "Adaptability"),
            signatureMove = o.optString("signatureMove", "Tera Blast"),
            loreDescription = o.optString("loreDescription", ""),
            baseHp = o.optLong("baseHp", 85L).coerceIn(1L, 255L),
            baseAttack = o.optLong("baseAttack", 90L).coerceIn(1L, 255L),
            baseDefense = o.optLong("baseDefense", 85L).coerceIn(1L, 255L),
            baseSpAtk = o.optLong("baseSpAtk", 95L).coerceIn(1L, 255L),
            baseSpDef = o.optLong("baseSpDef", 85L).coerceIn(1L, 255L),
            baseSpeed = o.optLong("baseSpeed", 90L).coerceIn(1L, 255L),
            baseSpriteId = o.optLong("baseSpriteId", 384L).coerceIn(1L, 1025L),
            fusionStylePreset = o.optString("fusionStylePreset", "Omni-Console Pixel + 3D Cel Voxel"),
            enhancementChip = o.optString("enhancementChip", "Super FX2 (GSU-2)"),
            primaryHex = o.optString("primaryHex", "#00F5D4"),
            secondaryHex = o.optString("secondaryHex", "#FF2A85"),
            createdAtMs = now,
            updatedAtMs = now,
          )
        }
        customDexDao.upsertDexEntries(entries)
      }

      val assetsArr = root.optJSONArray("customStudioAssets")
      if (assetsArr != null && assetsArr.length() > 0) {
        val assets = (0 until assetsArr.length()).map { idx ->
          val o = assetsArr.getJSONObject(idx)
          CustomAssetRoomEntity(
            id = "asset_${UUID.randomUUID().toString().take(10)}",
            projectId = targetProjectId,
            name = o.optString("name", "Imported Asset"),
            category = o.optString("category", "Sprites"),
            consoleLineage = o.optString("consoleLineage", "GBC + GBA + DS + Switch HD-2D"),
            chipAddon = o.optString("chipAddon", "Super FX2 + Voxelizer-FX"),
            spriteSeedId = o.optLong("spriteSeedId", 25L).coerceIn(1L, 1025L),
            shaderFormula = o.optString("shaderFormula", "BLEND:65|VOXEL:70|CRT:30|BLOOM:85"),
            createdAtMs = now,
            updatedAtMs = now,
          )
        }
        customAssetDao.upsertAssets(assets)
      }

      val spritesArr = root.optJSONArray("savedSprites")
      if (spritesArr != null && spritesArr.length() > 0) {
        val sprites = (0 until spritesArr.length()).map { idx ->
          val o = spritesArr.getJSONObject(idx)
          SavedSpriteEntity(
            id = "sprite_${UUID.randomUUID().toString().take(8)}",
            projectId = targetProjectId,
            name = o.optString("name", "Imported Sprite"),
            spriteSeedId = o.optInt("spriteSeedId", 25),
            paletteJson = o.optString("paletteJson", "[]"),
            framesRleJson = o.optString("framesRleJson", "[]"),
            frameCount = o.optInt("frameCount", 2).coerceAtLeast(1),
            updatedAtEpochMs = now,
          )
        }
        studioPersistenceDao.upsertSavedSprites(sprites)
      }

      val mapsArr = root.optJSONArray("savedMaps")
      if (mapsArr != null && mapsArr.length() > 0) {
        val maps = (0 until mapsArr.length()).map { idx ->
          val o = mapsArr.getJSONObject(idx)
          val regionId = o.optString("regionId", "kanto")
          SavedMapEntity(
            mapKey = "$targetProjectId:$regionId",
            projectId = targetProjectId,
            regionId = regionId,
            regionName = o.optString("regionName", regionId),
            gridSize = o.optInt("gridSize", MasterCanonTemplateCatalog.MAP_GRID_SIZE),
            tilesCsv = o.optString("tilesCsv", ""),
            updatedAtEpochMs = now,
          )
        }
        studioPersistenceDao.upsertSavedMaps(maps)
      }
      seedDefaultMapsForProject(targetProjectId)

      // Room v5 custom_trainers import (gracefully skipped for backward-compatible v4 JSON bundles)
      val trainersArr = root.optJSONArray("customTrainers")
      if (customTrainerDao != null && trainersArr != null && trainersArr.length() > 0) {
        val trainers = (0 until trainersArr.length()).map { idx ->
          val o = trainersArr.getJSONObject(idx)
          val baseId = o.optString("id", "trainer_${idx + 1}").trim().ifEmpty { "trainer_${idx + 1}" }
          CustomTrainerEntity(
            id = "${targetProjectId}_$baseId",
            projectId = targetProjectId,
            name = o.optString("name", "Imported Trainer").trim().take(60),
            title = o.optString("title", "Prism Explorer").trim().take(60),
            roleCategory = o.optString("roleCategory", o.optString("roleCategoryToken", "PROTAGONIST")),
            regionId = o.optString("regionId", "kanto"),
            teamDexIdsCsv = o.optString("teamDexIdsCsv", "25,6,94,448,658,384"),
            skinHex = o.optString("skinHex", "#F6D0B1"),
            hairHex = o.optString("hairHex", "#1E293B"),
            outfitPrimaryHex = o.optString("outfitPrimaryHex", "#00E5FF"),
            outfitSecondaryHex = o.optString("outfitSecondaryHex", "#0F172A"),
            accentHex = o.optString("accentHex", "#FFB300"),
            headgearToken = o.optString("headgearToken", "CAP"),
            outfitStyleToken = o.optString("outfitStyleToken", "ADVENTURER_JACKET"),
            hairStyleIndex = o.optInt("hairStyleIndex", 0).coerceIn(0, 5),
            companionPoseIndex = o.optInt("companionPoseIndex", 0).coerceIn(0, 2),
            introQuote = o.optString("introQuote", "Let's test our Resonance bond!"),
            defeatQuote = o.optString("defeatQuote", "What a match!"),
            isPlayerAvatar = o.optBoolean("isPlayerAvatar", false),
            updatedAtEpochMs = now,
          )
        }
        customTrainerDao.upsertCustomTrainers(trainers)
      }

      // Room v6 side_mode_records import (gracefully skipped for backward-compatible v4/v5 JSON bundles)
      val sideJson = root.optJSONObject("sideModeRecords")
      if (sideJson != null) {
        studioPersistenceDao.upsertSideModeRecord(
          SideModeRecordEntity(
            projectId = targetProjectId,
            battleTowerBestStreak = sideJson.optInt("battleTowerBestStreak", 0).coerceAtLeast(0),
            battleTowerCurrentStreak = sideJson.optInt("battleTowerCurrentStreak", 0).coerceAtLeast(0),
            battlePoints = sideJson.optInt("battlePoints", 0).coerceAtLeast(0),
            contestRibbonsWon = sideJson.optInt("contestRibbonsWon", 0).coerceAtLeast(0),
            contestBestScore = sideJson.optInt("contestBestScore", 0).coerceAtLeast(0),
            bestSnapScore = sideJson.optInt("bestSnapScore", 0).coerceAtLeast(0),
            bestSnapStarRating = sideJson.optInt("bestSnapStarRating", 0).coerceIn(0, 4),
            bestSnapCaption = sideJson.optString("bestSnapCaption", "No Photosphere Shots Yet").take(120),
            deepestRiftFloor = sideJson.optInt("deepestRiftFloor", 0).coerceIn(0, 5),
            riftGuardiansDefeated = sideJson.optInt("riftGuardiansDefeated", 0).coerceAtLeast(0),
            rangerCapturesCompleted = sideJson.optInt("rangerCapturesCompleted", 0).coerceAtLeast(0),
            campMealsServed = sideJson.optInt("campMealsServed", 0).coerceAtLeast(0),
            updatedAtEpochMs = now,
          )
        )
      }

      importedProject
    }
  }

  companion object {
    const val DEFAULT_MASTER_PROJECT_ID = "prism_resonance_master_v4"
  }
}

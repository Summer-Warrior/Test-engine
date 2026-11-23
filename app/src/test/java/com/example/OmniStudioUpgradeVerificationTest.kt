package com.example

import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AtlasProgressEntity
import com.example.data.local.CampaignStateEntity
import com.example.data.local.EncyclopediaSeedCatalog
import com.example.data.local.NationalAtlasIndex
import com.example.data.local.NativePokemonDatabase
import com.example.data.local.ResourceCategory
import com.example.data.local.SavedMapEntity
import com.example.data.local.SavedSpriteEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.data.repository.LocalStudioProjectRepository
import com.example.engine.AudioConvergenceSolver
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTurnField
import com.example.engine.BgmThemeId
import com.example.engine.CanonBattleResolver
import com.example.engine.CombatantSnapshot
import com.example.engine.GameFeelFrameState
import com.example.engine.HardwareConvergenceSolver
import com.example.engine.HardwareRuntimePresetCatalog
import com.example.engine.HardwareRuntimeSection
import com.example.engine.ImpactWeight
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.MoveCategory
import com.example.engine.MoveDirection
import com.example.engine.OmniHardwareAudioEngine
import com.example.engine.PokemonType
import com.example.engine.SpritePixelMatrixEngine
import com.example.engine.StatusCondition
import com.example.engine.StudioSfxType
import com.example.engine.TerrainCondition
import com.example.engine.UniversalGameFeelEngine
import com.example.engine.UniversalHardwareRegistry
import com.example.engine.WeatherCondition
import com.example.ui.StudioViewModelHelpers
import com.example.ui.components.AutoTileBitmaskSolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OmniStudioUpgradeVerificationTest {

  @Test
  fun `hardware registry loads 156 platforms builds 4-section runtime presets and formats readable badges`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val platforms = UniversalHardwareRegistry.loadFromAssets(context)
    assertTrue("Expected >= 150 platforms, got ${platforms.size}", platforms.size >= 150)
    assertEquals(platforms.size, platforms.map { it.id }.distinct().size)

    val allIds = platforms.map { it.id }.toSet()
    val token = UniversalHardwareRegistry.encodeSelectionToken(
      activeIds = allIds,
      allCatalogIds = allIds,
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
    )
    assertEquals("SHOVEL_KNIGHT_CURATED|ALL_ON_RECORD", token)

    val (decodedMode, decodedIds) = UniversalHardwareRegistry.decodeSelectionToken(token, allIds)
    assertEquals(MasterDisciplineMode.SHOVEL_KNIGHT_CURATED, decodedMode)
    assertEquals(allIds.size, decodedIds.size)

    // Readable hardware summary never exposes raw pipe tokens
    val (headline, badges) = UniversalHardwareRegistry.formatReadableHardwareSummary(token, platforms)
    assertFalse(headline.contains("|"))
    assertFalse(headline.contains("ALL_ON_RECORD"))
    assertTrue(badges.isNotEmpty())
    assertTrue(badges.none { it.contains("|") || it.contains("ALL_ON_RECORD") })

    // Game Test 4-section Runtime Preset Catalog & 8-step Hardware Tour
    val ultimatePresets = HardwareRuntimePresetCatalog.buildUltimatePresets(platforms)
    val curatedCombos = HardwareRuntimePresetCatalog.buildCuratedCombos(platforms)
    val categoryEraPresets = HardwareRuntimePresetCatalog.buildCategoryAndEraPresets(platforms)
    val singlePuristPresets = platforms.map { HardwareRuntimePresetCatalog.buildSingleSystemPuristPreset(it) }
    assertTrue(ultimatePresets.any { it.section == HardwareRuntimeSection.ULTIMATE && it.id == HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED })
    assertTrue(ultimatePresets.any { it.section == HardwareRuntimeSection.ULTIMATE && it.id == HardwareRuntimePresetCatalog.MODE_ULTIMATE_MAXIMALIST })
    assertTrue(curatedCombos.size >= 8)
    assertTrue(categoryEraPresets.size >= 8)
    assertEquals(platforms.size, singlePuristPresets.size)

    val tour = HardwareRuntimePresetCatalog.buildHardwareTourSequence(platforms)
    assertEquals(8, tour.size)
    assertEquals(HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED, tour.last().id)

    val curatedVisuals = HardwareConvergenceSolver.solveVisuals(
      activePlatforms = platforms,
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      extraPretty = ExtraVisualAudioSettings.ALL_DISABLED,
    )
    val maxPrettyVisuals = HardwareConvergenceSolver.solveVisuals(
      activePlatforms = platforms,
      disciplineMode = MasterDisciplineMode.MAXIMALIST_PRETTY,
      extraPretty = ExtraVisualAudioSettings.ALL_ENABLED,
    )

    assertEquals(platforms.size, curatedVisuals.activeCount)
    assertTrue(curatedVisuals.outlineWeight > 0.5f)
    assertEquals(8, curatedVisuals.voxelSliceCount)
    assertEquals(14, maxPrettyVisuals.voxelSliceCount)
    assertEquals(60, maxPrettyVisuals.characterStepFps)
    assertTrue(maxPrettyVisuals.waterCausticsEnabled)
    assertTrue(maxPrettyVisuals.volumetricRaysEnabled)
    assertTrue(maxPrettyVisuals.crtCurvatureEnabled)

    val audioParams = AudioConvergenceSolver.solveAudio(
      activePlatforms = platforms,
      disciplineMode = MasterDisciplineMode.MAXIMALIST_PRETTY,
      extraPretty = ExtraVisualAudioSettings.ALL_ENABLED,
    )
    assertTrue(audioParams.extraLushChoirReverb)
    assertTrue(audioParams.pulseLeadGain > 0.4f)
    assertTrue(audioParams.fmSlapBassGain > 0.3f)
  }

  @Test
  fun `all 14 regions have unique 16x16 custom maps valid spawns landmarks 3 POIs and zero Bulbasaur fallback`() {
    val chapters = MasterCanonTemplateCatalog.worldChapters
    assertEquals(14, chapters.size)

    val uniqueTileHashes = mutableSetOf<Int>()
    chapters.forEach { chapter ->
      assertEquals("Region ${chapter.id} must be 16x16 (256 tiles)", 256, chapter.defaultMapTiles.size)
      assertTrue("Region ${chapter.id} tiles must be in 0..11", chapter.defaultMapTiles.all { it in 0..11 })
      assertTrue("Region ${chapter.id} must have Tall Grass (1)", chapter.defaultMapTiles.contains(1))
      assertTrue("Region ${chapter.id} must have Route Road (3)", chapter.defaultMapTiles.contains(3))
      assertTrue("Region ${chapter.id} must have Prism Sanctuary (5)", chapter.defaultMapTiles.contains(5))
      assertTrue("Region ${chapter.id} must have Healing Plaza (8)", chapter.defaultMapTiles.contains(8))
      assertTrue("Region ${chapter.id} must have Warp Gate (11)", chapter.defaultMapTiles.contains(11))

      val spawnIdx = chapter.spawnRow * MasterCanonTemplateCatalog.MAP_GRID_SIZE + chapter.spawnCol
      val spawnTile = chapter.defaultMapTiles[spawnIdx]
      assertTrue(
        "Region ${chapter.id} spawn (${chapter.spawnCol},${chapter.spawnRow}) tile=$spawnTile must be passable",
        UniversalGameFeelEngine.isTilePassable(spawnTile),
      )

      assertEquals("Region ${chapter.id} must have 3 NPCs/POIs", 3, chapter.pointsOfInterest.size)
      chapter.pointsOfInterest.forEach { poi ->
        assertTrue(poi.col in 0 until 16 && poi.row in 0 until 16)
        assertTrue(poi.name.isNotBlank() && poi.dialogue.isNotBlank())
      }

      // Verify encounter pool species synthesize cleanly with exact dex numbers (NEVER falling back to #1 Bulbasaur)
      chapter.encounterPoolDexIds.forEach { dex ->
        val synth = StudioViewModelHelpers.synthesizePokemonFromAtlas(dex)
        assertEquals(dex, synth.dexNumber)
        if (dex != 1) {
          assertNotEquals("Bulbasaur", synth.name)
        }
      }

      uniqueTileHashes += chapter.defaultMapTiles.hashCode()
    }
    assertEquals("All 14 region maps must have unique custom layouts", 14, uniqueTileHashes.size)

    // Verify Room Resource Manager map tiles use aligned terrain indices (1, 2, 3, 5)
    val mapTileResources = EncyclopediaSeedCatalog.builtInResources.filter { it.categoryId == ResourceCategory.MAP_TILES.id }
    assertTrue(mapTileResources.isNotEmpty())
    assertTrue(mapTileResources.all { it.tileTypeIndex in setOf(1, 2, 3, 5) })
  }

  @Test
  fun `battle engine fixes tera move types priority enemy move levitate wonder guard weather terrain hold items TMs and evolution`() {
    val charizardTeraDragon = CombatantSnapshot(
      dexNumber = 6,
      name = "Charizard",
      level = 50,
      primaryType = PokemonType.FIRE,
      secondaryType = PokemonType.FLYING,
      baseHp = 78, baseAtk = 84, baseDef = 78, baseSpA = 109, baseSpD = 85, baseSpe = 100,
      abilityName = "Blaze",
      gimmickMode = BattleGimmickMode.TERASTALLIZATION,
      teraType = PokemonType.DRAGON,
    )
    val target = CombatantSnapshot(
      dexNumber = 143,
      name = "Snorlax",
      level = 50,
      primaryType = PokemonType.NORMAL,
      secondaryType = null,
      baseHp = 160, baseAtk = 110, baseDef = 65, baseSpA = 65, baseSpD = 110, baseSpe = 30,
      abilityName = "Thick Fat",
    )

    // 1. Regular Fire move keeps Fire type + original 1.5x STAB under Tera Dragon
    val flamethrower = BattleMoveSpec("Flamethrower", PokemonType.FIRE, MoveCategory.SPECIAL, 90)
    val fireRes = CanonBattleResolver.resolveTurn(charizardTeraDragon, target, flamethrower, BattleTurnField())
    assertEquals(PokemonType.FIRE, fireRes.resolvedMoveType)
    assertEquals(1.5f, fireRes.stabMultiplier)

    // 1b. Enemy higher-priority move (+1) outspeeds faster player using priority 0
    val enemyPriorityRes = CanonBattleResolver.resolveTurn(
      attacker = charizardTeraDragon,
      defender = target,
      move = flamethrower,
      field = BattleTurnField(),
      defenderMovePriority = 1,
    )
    assertFalse("Enemy +1 priority move must act before faster attacker using priority 0", enemyPriorityRes.attackerActsFirst)

    // 2. Tera Blast shifts to Dragon and gets 1.5x Tera STAB; Tera Fire gets 2.0x STAB
    val teraBlast = BattleMoveSpec("Tera Blast", PokemonType.NORMAL, MoveCategory.SPECIAL, 80, isTeraBlast = true)
    val tbRes = CanonBattleResolver.resolveTurn(charizardTeraDragon, target, teraBlast, BattleTurnField())
    assertEquals(PokemonType.DRAGON, tbRes.resolvedMoveType)
    assertEquals(1.5f, tbRes.stabMultiplier)

    val charizardTeraFire = charizardTeraDragon.copy(teraType = PokemonType.FIRE)
    val doubleStabRes = CanonBattleResolver.resolveTurn(charizardTeraFire, target, flamethrower, BattleTurnField())
    assertEquals(2.0f, doubleStabRes.stabMultiplier)

    // 3. Levitate Ground immunity & Gravity override
    val gengarLevitate = CombatantSnapshot(
      dexNumber = 94, name = "Gengar", level = 50,
      primaryType = PokemonType.GHOST, secondaryType = PokemonType.POISON,
      baseHp = 60, baseAtk = 65, baseDef = 60, baseSpA = 130, baseSpD = 75, baseSpe = 110,
      abilityName = "Levitate",
    )
    val eq = BattleMoveSpec("Earthquake", PokemonType.GROUND, MoveCategory.PHYSICAL, 100)
    val immuneRes = CanonBattleResolver.resolveTurn(target, gengarLevitate, eq, BattleTurnField())
    assertEquals(0f, immuneRes.typeEffectiveness)
    assertEquals(0, immuneRes.maxDamage)

    val gravityRes = CanonBattleResolver.resolveTurn(target, gengarLevitate, eq, BattleTurnField(isGravityActive = true))
    assertEquals(2.0f, gravityRes.typeEffectiveness)
    assertTrue(gravityRes.maxDamage > 0)

    // 4. Wonder Guard Shedinja blocks non-SE and takes SE
    val shedinja = CombatantSnapshot(
      dexNumber = 292, name = "Shedinja", level = 50,
      primaryType = PokemonType.BUG, secondaryType = PokemonType.GHOST,
      baseHp = 1, baseAtk = 90, baseDef = 45, baseSpA = 30, baseSpD = 30, baseSpe = 40,
      abilityName = "Wonder Guard",
    )
    val thunderbolt = BattleMoveSpec("Thunderbolt", PokemonType.ELECTRIC, MoveCategory.SPECIAL, 90)
    assertEquals(0, CanonBattleResolver.resolveTurn(target, shedinja, thunderbolt, BattleTurnField()).maxDamage)
    assertTrue(CanonBattleResolver.resolveTurn(target, shedinja, flamethrower, BattleTurnField()).maxDamage > 0)

    // 5. Sandstorm +1.5x Rock Sp.Def & Snow +1.5x Ice Def
    val tyranitar = CombatantSnapshot(
      dexNumber = 248, name = "Tyranitar", level = 50,
      primaryType = PokemonType.ROCK, secondaryType = PokemonType.DARK,
      baseHp = 100, baseAtk = 134, baseDef = 110, baseSpA = 95, baseSpD = 100, baseSpe = 61,
      abilityName = "Sand Stream",
    )
    val clearSpRes = CanonBattleResolver.resolveTurn(target, tyranitar, thunderbolt, BattleTurnField(weather = WeatherCondition.CLEAR))
    val sandSpRes = CanonBattleResolver.resolveTurn(target, tyranitar, thunderbolt, BattleTurnField(weather = WeatherCondition.SANDSTORM))
    assertTrue(sandSpRes.maxDamage < clearSpRes.maxDamage)

    val glalie = CombatantSnapshot(
      dexNumber = 362, name = "Glalie", level = 50,
      primaryType = PokemonType.ICE, secondaryType = null,
      baseHp = 80, baseAtk = 80, baseDef = 80, baseSpA = 80, baseSpD = 80, baseSpe = 80,
      abilityName = "Ice Body",
    )
    val clearPhys = CanonBattleResolver.resolveTurn(target, glalie, eq, BattleTurnField(weather = WeatherCondition.CLEAR))
    val snowPhys = CanonBattleResolver.resolveTurn(target, glalie, eq, BattleTurnField(weather = WeatherCondition.SNOW))
    assertTrue(snowPhys.maxDamage < clearPhys.maxDamage)

    // 6. Misty Terrain 0.5x Dragon mitigation & grounded Electric Terrain 1.3x boost
    val dragonPulse = BattleMoveSpec("Dragon Pulse", PokemonType.DRAGON, MoveCategory.SPECIAL, 85)
    val normalDragon = CanonBattleResolver.resolveTurn(charizardTeraDragon, target, dragonPulse, BattleTurnField())
    val mistyDragon = CanonBattleResolver.resolveTurn(charizardTeraDragon, target, dragonPulse, BattleTurnField(terrain = TerrainCondition.MISTY_TERRAIN))
    assertTrue(mistyDragon.maxDamage < normalDragon.maxDamage)

    val airborneCharizard = charizardTeraDragon.copy(gimmickMode = BattleGimmickMode.NONE, teraType = null)
    val flyingElecNormal = CanonBattleResolver.resolveTurn(airborneCharizard, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.NONE))
    val flyingElecTerrain = CanonBattleResolver.resolveTurn(airborneCharizard, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.ELECTRIC_TERRAIN))
    assertEquals(flyingElecNormal.maxDamage, flyingElecTerrain.maxDamage)

    val teraGroundedElecNormal = CanonBattleResolver.resolveTurn(charizardTeraDragon, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.NONE))
    val teraGroundedElecTerrain = CanonBattleResolver.resolveTurn(charizardTeraDragon, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.ELECTRIC_TERRAIN))
    assertTrue(teraGroundedElecTerrain.maxDamage > teraGroundedElecNormal.maxDamage)

    // 7. Hold Items: Life Orb recoil & Focus Sash survival at full HP
    val lifeOrbAttacker = target.copy(heldItemId = "life_orb")
    val loRes = CanonBattleResolver.resolveTurn(lifeOrbAttacker, glalie, eq, BattleTurnField())
    assertTrue(loRes.lifeOrbRecoil > 0)
    assertTrue(loRes.maxDamage > clearPhys.maxDamage)

    val sashDefender = shedinja.copy(dexNumber = 25, name = "Pikachu", baseHp = 40, primaryType = PokemonType.NORMAL, secondaryType = null, abilityName = "Run Away", heldItemId = "focus_sash", currentHpPercent = 100)
    val superAttacker = target.copy(level = 100, baseAtk = 180)
    val sashRes = CanonBattleResolver.resolveTurn(superAttacker, sashDefender, eq, BattleTurnField())
    assertTrue(sashRes.focusSashTriggered)
    assertEquals(sashDefender.maxHp() - 1, sashRes.maxDamage)

    // 8. TM teaching & Evolution stone helpers (verified against real StudioItemCatalog entries)
    val tmCatalogItem = StudioItemCatalog.defaultItems.first { it.id == "tm_thunderbolt" }
    val tmSpec = StudioViewModelHelpers.parseTmEffectTag(tmCatalogItem.effectTag)
    assertNotNull(tmSpec)
    assertEquals("Thunderbolt", tmSpec!!.name)
    assertEquals(PokemonType.ELECTRIC, tmSpec.type)
    assertEquals(90, tmSpec.basePower)

    val pikachuEntity = StudioViewModelHelpers.synthesizePokemonFromAtlas(25)
    val movesWithTm = StudioViewModelHelpers.buildMovesetForPokemon(
      entity = pikachuEntity,
      teraType = PokemonType.ELECTRIC,
      taughtSlot4Move = tmSpec,
    )
    assertEquals(4, movesWithTm.size)
    assertEquals("Thunderbolt", movesWithTm[3].name)

    val thunderStone = StudioItemCatalog.defaultItems.first { it.id == "evo_thunder_stone" }
    val fireStone = StudioItemCatalog.defaultItems.first { it.id == "evo_fire_stone" }
    assertEquals(26, StudioViewModelHelpers.resolveStoneEvolutionTarget(25, thunderStone.effectTag.removePrefix("EVO:")))
    assertEquals(136, StudioViewModelHelpers.resolveStoneEvolutionTarget(133, fireStone.effectTag.removePrefix("EVO:")))

    // 9. Proportional Dynamax HP scaling never full-heals
    assertEquals(150, CanonBattleResolver.scaleHpProportionally(currentHp = 75, oldMaxHp = 150, newMaxHp = 300))
    assertEquals(75, CanonBattleResolver.scaleHpProportionally(currentHp = 150, oldMaxHp = 300, newMaxHp = 150))
    assertEquals(0, CanonBattleResolver.scaleHpProportionally(currentHp = 0, oldMaxHp = 150, newMaxHp = 300))

    // 10. Capture calculation (Master Ball 100% & Quick Ball Turn 1 advantage)
    val masterCap = CanonBattleResolver.calculateCaptureResult(
      ballId = "ball_master", ballName = "Master Ball",
      targetCurrentHp = 300, targetMaxHp = 300, targetBaseStatTotal = 680,
      targetPrimaryType = PokemonType.DRAGON, targetSecondaryType = PokemonType.FLYING,
      targetStatus = StatusCondition.HEALTHY, turnNumber = 1,
      isWaterSector = false, isRiftOrShadow = true, alreadyCaptured = false, deterministicRoll01 = 0.99f,
    )
    assertTrue(masterCap.captured)
    assertEquals(1.0f, masterCap.catchProbability)
  }

  @Test
  fun `auto tile solver 16x16 game feel engine and 2-pass seamless DSP audio pass invariants`() {
    // AutoTile 4-neighbor bitmask (N=1, E=2, S=4, W=8) on 16x16 grid
    val grid = MutableList(256) { 0 }
    grid[2 * 16 + 2] = 2 // center water
    grid[1 * 16 + 2] = 2 // North water
    grid[2 * 16 + 3] = 2 // East water
    val mask = AutoTileBitmaskSolver.computeMask(grid, 16, col = 2, row = 2)
    assertEquals(AutoTileBitmaskSolver.BIT_NORTH or AutoTileBitmaskSolver.BIT_EAST, mask)
    assertTrue(AutoTileBitmaskSolver.variantIndex(2, 2) in 0..3)

    // UniversalGameFeelEngine: non-stacking maxOf hitstop, mosaic dissolve, input buffer, 16x16 corner correction
    var feel = GameFeelFrameState()
    feel = UniversalGameFeelEngine.triggerImpact(feel, ImpactWeight.HEAVY_OR_CRIT, targetIsEnemy = true)
    assertEquals(95f, feel.hitstopRemainingMs)
    feel = UniversalGameFeelEngine.triggerImpact(feel, ImpactWeight.NORMAL, targetIsEnemy = true)
    assertEquals(95f, feel.hitstopRemainingMs)

    feel = UniversalGameFeelEngine.triggerBattleEntryMosaic(feel)
    assertEquals(10f, feel.mosaicPixelFactor)
    feel = UniversalGameFeelEngine.stepFrame(feel, 40f)
    assertTrue(feel.mosaicPixelFactor < 10f)

    // Passable vs solid tile rules for all 12 canonical terrain types
    assertTrue(UniversalGameFeelEngine.isTilePassable(0)) // Meadow / Plaza
    assertTrue(UniversalGameFeelEngine.isTilePassable(1)) // Tall Wild Grass
    assertFalse(UniversalGameFeelEngine.isTilePassable(2)) // Deep Water
    assertTrue(UniversalGameFeelEngine.isTilePassable(3)) // Route Road
    assertFalse(UniversalGameFeelEngine.isTilePassable(4)) // Mountain Ridge
    assertTrue(UniversalGameFeelEngine.isTilePassable(5)) // Prism Sanctuary
    assertFalse(UniversalGameFeelEngine.isTilePassable(6)) // Tree Canopy
    assertTrue(UniversalGameFeelEngine.isTilePassable(7)) // Bridge / Span
    assertTrue(UniversalGameFeelEngine.isTilePassable(8)) // Healing Plaza
    assertTrue(UniversalGameFeelEngine.isTilePassable(9)) // Wild Biome
    assertFalse(UniversalGameFeelEngine.isTilePassable(10)) // Landmark Building
    assertTrue(UniversalGameFeelEngine.isTilePassable(11)) // Region Warp Gate

    val overworld = MutableList(256) { 0 }
    overworld[1 * 16 + 2] = 2 // blocked water directly north of (2, 2)
    val (nx, ny) = UniversalGameFeelEngine.resolveStepWithCornerCorrection(2, 2, MoveDirection.UP, 16, overworld)
    assertEquals(3, nx)
    assertEquals(1, ny)

    // Pure-Kotlin 44.1kHz Stereo 2-pass warmed seamless DSP Audio
    val audioParams = AudioConvergenceSolver.solveAudio(
      activePlatforms = emptyList(),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      extraPretty = ExtraVisualAudioSettings(),
    )
    val bgmPcm = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.PRISM_TITLE, audioParams)
    assertTrue(bgmPcm.size > 44100)
    assertTrue(bgmPcm.any { it.toInt() != 0 })
    assertTrue(bgmPcm.all { it.toInt() in -32767..32767 })

    val sfxPcm = OmniHardwareAudioEngine.renderSfxPcm(StudioSfxType.HIT_HEAVY_CRIT, audioParams)
    assertTrue(sfxPcm.size > 1024)
    assertTrue(sfxPcm.any { it.toInt() != 0 })

    val cryPcm = OmniHardwareAudioEngine.renderPokemonCryPcm(25, "Electric", "", 35, 55, 90, audioParams)
    assertTrue(cryPcm.size > 10000)
    assertTrue(cryPcm.any { it.toInt() != 0 })
  }

  @Test
  fun `offline room v4 repository auto-seeds Prism Resonance and roundtrips full JSON project export import`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db = Room.inMemoryDatabaseBuilder(context, NativePokemonDatabase::class.java)
        .allowMainThreadQueries()
        .build()
      try {
        val repo = LocalStudioProjectRepository(
          projectDao = db.fanGameProjectDao(),
          customDexDao = db.customDexDao(),
          customAssetDao = db.customAssetDao(),
          studioPersistenceDao = db.studioPersistenceDao(),
        )
        val dao = db.studioPersistenceDao()

        // 1. Auto-seed "Pokémon: Prism Resonance" when empty
        val seeded = repo.ensureDefaultMasterProjectSeeded()
        assertTrue(seeded.title.contains("Prism Resonance"))
        assertEquals(14L, seeded.mapCount)
        assertEquals(1025L, seeded.dexCount)
        val allProjects = repo.observeProjects().first()
        assertEquals(1, allProjects.size)

        // 2. Add custom dex, custom asset, saved sprite, and saved 16x16 map
        repo.addCustomDexEntry(
          CustomDexEntry(
            projectId = seeded.id,
            name = "Prismwyrm",
            dexNumber = 1026L,
            primaryType = "Dragon",
            secondaryType = "Electric",
            abilityName = "Harmonic Core",
            signatureMove = "Prism Surge",
            baseSpriteId = 384L,
          )
        )
        repo.addStudioAsset(
          CustomStudioAsset(
            projectId = seeded.id,
            name = "Prism Sprite Pack",
            category = "Sprites",
            consoleLineage = "All 156 Hardware",
            chipAddon = "Shovel Knight+",
            spriteSeedId = 384L,
          )
        )

        val palette = SpritePixelMatrixEngine.buildInitialPalette("#00F5D4", "#FF2A85")
        val frame0 = SpritePixelMatrixEngine.generateSeededFrame(384, 0)
        val spriteEntity = SavedSpriteEntity(
          id = "test_sprite_384",
          projectId = seeded.id,
          name = "Test Rayquaza Rig",
          spriteSeedId = 384,
          paletteJson = SpritePixelMatrixEngine.serializePaletteJson(palette),
          framesRleJson = SpritePixelMatrixEngine.serializeFramesRleJson(listOf(frame0)),
          frameCount = 1,
        )
        dao.upsertSavedSprite(spriteEntity)
        val bmp = SpritePixelMatrixEngine.renderFrameToBitmap(frame0, palette, scale = 2)
        assertEquals(32, bmp.width)
        assertTrue((0 until 32).any { y -> (0 until 32).any { x -> Color.alpha(bmp.getPixel(x, y)) == 255 } })

        dao.upsertSavedMap(
          SavedMapEntity(
            mapKey = "${seeded.id}:kanto",
            projectId = seeded.id,
            regionId = "kanto",
            regionName = "Kanto",
            tilesCsv = MasterCanonTemplateCatalog.worldChapters.first().defaultMapTiles.joinToString(","),
          )
        )

        dao.upsertItems(StudioItemCatalog.defaultItems)
        assertEquals(StudioItemCatalog.defaultItems.size, dao.countItems())

        dao.upsertAtlasProgress(AtlasProgressEntity(dexNumber = 25, seen = true, battled = true, captured = true))
        assertTrue(dao.getAtlasProgress(25)?.captured == true)

        val mask = StudioViewModelHelpers.encodeExtraPrettyMask(ExtraVisualAudioSettings.ALL_ENABLED)
        assertEquals(ExtraVisualAudioSettings.ALL_ENABLED, StudioViewModelHelpers.decodeExtraPrettyMask(mask))

        dao.upsertCampaignState(
          CampaignStateEntity(
            projectId = seeded.id,
            activeRegionId = "johto",
            playerX = 4,
            playerY = 12,
            completedRegionIdsCsv = "kanto",
            harmonicShardsCsv = "shard_kanto",
            extraPrettyMask = mask,
          )
        )

        // 3. Export full project bundle to JSON & re-import as a new project in Room v4
        val exportedJson = repo.exportProjectToJson(seeded.id).getOrThrow()
        assertTrue(exportedJson.contains("Prism Resonance"))
        assertTrue(exportedJson.contains("Test Rayquaza Rig"))
        assertTrue(exportedJson.contains("Prismwyrm"))

        val importedProject = repo.importProjectFromJson(exportedJson).getOrThrow()
        assertNotEquals(seeded.id, importedProject.id)
        assertTrue(importedProject.title.contains("Prism Resonance"))
        assertEquals(2, repo.observeProjects().first().size)
        val importedDex = repo.observeCustomDex(importedProject.id).first()
        assertTrue(importedDex.any { it.name == "Prismwyrm" })
        val importedMap = dao.getSavedMap("${importedProject.id}:kanto")
        assertNotNull(importedMap)
        val importedCampaign = dao.observeCampaignState(importedProject.id).first()
        assertEquals("johto", importedCampaign?.activeRegionId)

        // 4. Full #0001-#1025 National Atlas Index check
        val fullIndex = NationalAtlasIndex.buildCompleteIndex()
        assertEquals(1025, fullIndex.size)
        assertEquals("Bulbasaur", fullIndex.first().name)
        assertEquals("Pecharunt", fullIndex.last().name)
      } finally {
        db.close()
      }
    }
  }
}

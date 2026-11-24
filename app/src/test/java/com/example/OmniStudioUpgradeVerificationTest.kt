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
import com.example.engine.TimeOfDayCycle
import com.example.engine.TrainerArchetypeCatalog
import com.example.engine.TrainerCategory
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
  fun `all 22 fusion regions have unique 16x16 maps 22 distinct biomes passable spawns landmarks 3 sub-zones 57 trainer types and 100 percent 1025 dex coverage`() {
    val chapters = MasterCanonTemplateCatalog.worldChapters
    assertEquals(22, chapters.size)

    val chapterIds = chapters.map { it.id }.toSet()
    assertEquals(22, chapterIds.size)
    assertEquals("All 22 regions must have distinct RegionBiomeStyle palettes", 22, chapters.map { it.biomeStyle }.toSet().size)

    val uniqueTileHashes = mutableSetOf<Int>()
    val unionDexAcrossRegions = mutableSetOf<Int>()

    chapters.forEach { chapter ->
      assertEquals("Region ${chapter.id} must be 16x16 (256 tiles)", 256, chapter.defaultMapTiles.size)
      assertTrue("Region ${chapter.id} tiles must be in 0..11", chapter.defaultMapTiles.all { it in 0..11 })
      assertTrue("Region ${chapter.id} must have Tall Grass (1)", chapter.defaultMapTiles.contains(1))
      assertTrue("Region ${chapter.id} must have Route Road (3)", chapter.defaultMapTiles.contains(3))
      assertTrue("Region ${chapter.id} must have Prism Sanctuary (5)", chapter.defaultMapTiles.contains(5))
      assertTrue("Region ${chapter.id} must have Healing Plaza (8)", chapter.defaultMapTiles.contains(8))
      assertTrue("Region ${chapter.id} must have Landmark Tile (10)", chapter.defaultMapTiles.contains(10))
      assertTrue("Region ${chapter.id} must have Warp Gate (11)", chapter.defaultMapTiles.contains(11))

      val spawnIdx = chapter.spawnRow * MasterCanonTemplateCatalog.MAP_GRID_SIZE + chapter.spawnCol
      val spawnTile = chapter.defaultMapTiles[spawnIdx]
      assertTrue(
        "Region ${chapter.id} spawn (${chapter.spawnCol},${chapter.spawnRow}) tile=$spawnTile must be passable",
        UniversalGameFeelEngine.isTilePassable(spawnTile),
      )

      // Multi-zone layer & landmark silhouette & connected warp portals
      assertEquals("Region ${chapter.id} must have 3 RegionSubZones", 3, chapter.subZones.size)
      chapter.subZones.forEach { zone ->
        assertTrue(zone.name.isNotBlank())
        assertTrue(zone.sourceTag.isNotBlank())
        assertTrue(zone.col in 0..15 && zone.row in 0..15)
      }
      assertTrue("Region ${chapter.id} fusionSourcesLineage must be non-blank", chapter.fusionSourcesLineage.isNotBlank())
      assertTrue("Region ${chapter.id} must link to valid connected regions", chapter.connectedRegionIds.isNotEmpty() && chapter.connectedRegionIds.all { it in chapterIds })
      assertTrue("Region ${chapter.id} must define regional trainer classes", chapter.regionalTrainerClassIds.isNotEmpty())
      chapter.regionalTrainerClassIds.forEach { trId ->
        assertNotNull("Trainer ID $trId in ${chapter.id} must exist in TrainerArchetypeCatalog", TrainerArchetypeCatalog.findById(trId))
      }

      assertEquals("Region ${chapter.id} must have 3 NPCs/POIs", 3, chapter.pointsOfInterest.size)
      chapter.pointsOfInterest.forEach { poi ->
        assertTrue(poi.col in 0 until 16 && poi.row in 0 until 16)
        assertTrue(poi.name.isNotBlank() && poi.dialogue.isNotBlank())
        poi.trainerArchetypeId?.let { trId ->
          assertNotNull("POI trainerArchetypeId $trId in ${chapter.id} must resolve", TrainerArchetypeCatalog.findById(trId))
        }
      }

      // Verify encounter pool species synthesize cleanly with exact dex numbers (NEVER falling back to #1 Bulbasaur)
      chapter.encounterPoolDexIds.forEach { dex ->
        val synth = StudioViewModelHelpers.synthesizePokemonFromAtlas(dex)
        assertEquals(dex, synth.dexNumber)
        if (dex != 1) {
          assertNotEquals("Bulbasaur", synth.name)
        }
      }

      val regionalPool = NationalAtlasIndex.dexPoolForRegion(chapter.id)
      assertTrue("Region ${chapter.id} must have regional Pokédex pool", regionalPool.isNotEmpty())
      unionDexAcrossRegions += regionalPool
      uniqueTileHashes += chapter.defaultMapTiles.hashCode()
    }
    assertEquals("All 22 region maps must have unique custom layouts", 22, uniqueTileHashes.size)
    assertEquals("All 1,025 Pokémon (#0001..#1025) must be reachable across the 22 fusion regions", 1025, unionDexAcrossRegions.size)
    assertTrue((1..1025).all { it in unionDexAcrossRegions })

    // Verify All Trainer Types catalog (Route/Field, Specialists, League/Frontier, Cross-Media, Factions)
    val allTrainers = TrainerArchetypeCatalog.allTrainerTypes
    assertTrue("Expected >= 50 Trainer Types, got ${allTrainers.size}", allTrainers.size >= 50)
    assertEquals(allTrainers.size, allTrainers.map { it.id }.distinct().size)
    TrainerCategory.entries.forEach { cat ->
      assertTrue("TrainerCategory $cat must have entries", TrainerArchetypeCatalog.filterByCategory(cat).isNotEmpty())
    }
    allTrainers.forEach { tr ->
      val dex = TrainerArchetypeCatalog.resolveEnemyDexForTrainer(tr, "kanto", seedNonce = 3)
      assertTrue("Resolved trainer dex $dex out of bounds", dex in 1..1025)
    }

    // Verify Day/Dusk/Night cycle & clean tile legend label lookup (regression test for Pair string bug)
    assertEquals(listOf(TimeOfDayCycle.DAY, TimeOfDayCycle.DUSK, TimeOfDayCycle.NIGHT), TimeOfDayCycle.entries)
    val label0 = MasterCanonTemplateCatalog.tileLegendLabels.firstOrNull { it.first == 0 }?.second
    assertEquals("Meadow / Plaza", label0)
    assertFalse(label0!!.startsWith("("))

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
  fun `offline room v5 repository auto-seeds Prism Resonance and roundtrips full JSON project export import`() {
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
          customTrainerDao = db.customTrainerDao(),
        )
        val dao = db.studioPersistenceDao()

        // 1. Auto-seed "Pokémon: Prism Resonance" when empty
        val seeded = repo.ensureDefaultMasterProjectSeeded()
        assertTrue(seeded.title.contains("Prism Resonance"))
        assertEquals(22L, seeded.mapCount)
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

        // 2b. Save a custom protagonist Trainer before exporting so v5 JSON includes customTrainers
        val trainerDao = db.customTrainerDao()
        val customHero = com.example.engine.TrainerCatalogAndRigEngine.defaultProtagonist.copy(
          name = "Sora Prime",
          title = "Prism Champion",
          appearance = com.example.engine.TrainerCatalogAndRigEngine.defaultProtagonist.appearance.copy(
            headgear = com.example.engine.TrainerHeadgear.VISOR,
            outfitStyle = com.example.engine.TrainerOutfitStyle.CHAMPION_CAPE,
            outfitPrimaryHex = "#8B5CF6",
            companionPoseIndex = 2,
          ),
        )
        trainerDao.upsertCustomTrainer(
          com.example.data.local.CustomTrainerEntity.fromTrainerSpec(customHero, projectId = seeded.id, isPlayerAvatar = true)
        )

        // 2c. Save Room v6 SideModeRecordEntity & verify MIGRATION_5_6 versioning
        assertEquals(5, NativePokemonDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, NativePokemonDatabase.MIGRATION_5_6.endVersion)
        dao.upsertSideModeRecord(
          com.example.data.local.SideModeRecordEntity(
            projectId = seeded.id,
            battleTowerBestStreak = 14,
            battleTowerCurrentStreak = 7,
            battlePoints = 48,
            contestRibbonsWon = 3,
            contestBestScore = 36,
            bestSnapScore = 4320,
            bestSnapStarRating = 4,
            bestSnapCaption = "Pikachu (4★ Diamond • Illumina Aura ★)",
            deepestRiftFloor = 5,
            riftGuardiansDefeated = 2,
            rangerCapturesCompleted = 5,
            campMealsServed = 9,
          )
        )

        // 3. Export full project bundle to Room v6 JSON & re-import as a new project
        val exportedJson = repo.exportProjectToJson(seeded.id).getOrThrow()
        assertTrue(exportedJson.contains("pokefusion_studio_project_v6"))
        assertTrue(exportedJson.contains("sideModeRecords"))
        assertTrue(exportedJson.contains("Prism Resonance"))
        assertTrue(exportedJson.contains("Test Rayquaza Rig"))
        assertTrue(exportedJson.contains("Prismwyrm"))
        assertTrue(exportedJson.contains("Sora Prime"))

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
        val importedTrainers = trainerDao.getTrainersForProjectSnapshot(importedProject.id)
        assertEquals(1, importedTrainers.size)
        assertEquals("Sora Prime", importedTrainers.first().name)
        assertEquals(2, importedTrainers.first().companionPoseIndex)
        val importedSideRecord = dao.getSideModeRecord(importedProject.id)
        assertNotNull(importedSideRecord)
        assertEquals(14, importedSideRecord!!.battleTowerBestStreak)
        assertEquals(48, importedSideRecord.battlePoints)
        assertEquals(3, importedSideRecord.contestRibbonsWon)
        assertEquals(4320, importedSideRecord.bestSnapScore)
        assertEquals(5, importedSideRecord.deepestRiftFloor)
        assertEquals(5, importedSideRecord.rangerCapturesCompleted)

        // 3b. Backward-compatible v4/v5 JSON import (without customTrainers / sideModeRecords keys) succeeds cleanly
        val legacyV4Json = org.json.JSONObject(exportedJson).apply {
          put("schemaVersion", 4)
          put("format", "pokefusion_studio_project_v4")
          remove("customTrainers")
          remove("sideModeRecords")
        }.toString()
        val legacyImported = repo.importProjectFromJson(legacyV4Json).getOrThrow()
        assertEquals(3, repo.observeProjects().first().size)
        assertTrue(trainerDao.getTrainersForProjectSnapshot(legacyImported.id).isEmpty())
        assertEquals(null, dao.getSideModeRecord(legacyImported.id))

        // 4. Full #0001-#1025 Offline National Atlas Index + Unified Naming + ProceduralCreatureRigEngine Bitmap check
        val fullIndex = NationalAtlasIndex.initializeFromAssets(context)
        assertEquals(1025, fullIndex.size)
        assertEquals(1025, fullIndex.map { it.name }.toSet().size)
        assertTrue(fullIndex.none { it.name.startsWith("Species #") })
        assertTrue(fullIndex.all { it.epithet.isNotBlank() && it.baseStatTotal in 150..900 })
        assertTrue(fullIndex.sumOf { it.availableForms.size } > 1100)
        // Unified primary naming: Pikachu #25 matches across Atlas and built-in catalog, preserving Prism alias underneath
        val pikaAtlas = NationalAtlasIndex.peekSpecies(25)
        assertEquals("Pikachu", pikaAtlas.name)
        assertTrue("Expected non-blank Prism alias for Pikachu", pikaAtlas.prismAlias.isNotBlank())
        assertEquals("Bulbasaur", NationalAtlasIndex.peekSpecies(1).name)
        assertEquals("Rayquaza", NationalAtlasIndex.peekSpecies(384).name)

        // Verify Android ARGB_8888 transparent-backed Bitmaps render cleanly offline (2D + 3D Cel-Voxel)
        listOf(1, 6, 25, 94, 150, 249, 384, 448, 658, 1007, 1025).forEach { dex ->
          val bmp2d = com.example.engine.ProceduralCreatureRigEngine.renderSpriteBitmap32(dex, scale = 2)
          assertEquals(64, bmp2d.width)
          assertEquals(64, bmp2d.height)
          assertEquals(0, Color.alpha(bmp2d.getPixel(0, 0))) // Transparent corner background
          assertTrue((0 until 64).any { y -> (0 until 64).any { x -> Color.alpha(bmp2d.getPixel(x, y)) == 255 } })

          val bmp3d = com.example.engine.ProceduralCreatureRigEngine.renderCelVoxelModelBitmap(dex, scale = 2)
          assertEquals(64, bmp3d.width)
          assertEquals(64, bmp3d.height)
          assertTrue((0 until 64).any { y -> (0 until 64).any { x -> Color.alpha(bmp3d.getPixel(x, y)) == 255 } })
        }

        // 5. Room v5 custom_trainers CRUD + Trainer 2D Sprite / 3D Cel-Voxel / Cached Voxel Slices / 40x40 Portrait check
        val restoredHero = trainerDao.getSavedProtagonist()
        assertNotNull(restoredHero)
        assertEquals("VISOR", restoredHero!!.headgearToken)
        assertEquals("CHAMPION_CAPE", restoredHero.outfitStyleToken)

        val tBmp2d = com.example.engine.TrainerCatalogAndRigEngine.renderTrainerSpriteBitmap(
          customHero, com.example.engine.TrainerPose.CAPSULE_THROW, scale = 2,
        )
        assertEquals(64, tBmp2d.width)
        assertEquals(0, Color.alpha(tBmp2d.getPixel(0, 0)))
        val tBmp3d = com.example.engine.TrainerCatalogAndRigEngine.renderTrainerCelVoxelBitmap(customHero, scale = 2)
        assertEquals(64, tBmp3d.width)
        val tPortrait = com.example.engine.TrainerCatalogAndRigEngine.renderTrainerPortraitBitmap(customHero, scale = 2)
        assertEquals(80, tPortrait.width)

        val slicesA = com.example.engine.TrainerCatalogAndRigEngine.renderTrainerVoxelSlices(customHero, sliceCount = 7, scale = 2)
        val slicesB = com.example.engine.TrainerCatalogAndRigEngine.renderTrainerVoxelSlices(customHero, sliceCount = 7, scale = 2)
        assertEquals(7, slicesA.size)
        assertTrue("Voxel slices should hit bitmapCache cleanly", slicesA.first() === slicesB.first())
      } finally {
        db.close()
      }
    }
  }

  @Test
  fun `64-step regional and battle soundtracks dynamic layers ambient beds and new SFX pass Robolectric PCM checks`() {
    val audioParams = AudioConvergenceSolver.solveAudio(
      activePlatforms = emptyList(),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      extraPretty = ExtraVisualAudioSettings(),
    )

    // 1. Verify all 16 themes have 64-step compositions, unique IDs, non-zero stereo PCM, and safe amplitude bounds
    assertEquals(16, BgmThemeId.entries.size)
    assertEquals(16, BgmThemeId.entries.map { it.id }.distinct().size)
    BgmThemeId.entries.forEach { theme ->
      assertEquals("Theme ${theme.id} must have 64 steps", 64, theme.stepCount)
      val pcm = OmniHardwareAudioEngine.renderBgmLoopPcm(theme, audioParams)
      assertTrue("Theme ${theme.id} PCM too short: ${pcm.size}", pcm.size >= 64 * 1024 * 2)
      assertEquals("Theme ${theme.id} PCM must be even stereo frames", 0, pcm.size % 2)
      var hasNonZeroL = false
      var hasNonZeroR = false
      var hasStereoDiff = false
      for (i in pcm.indices step 2) {
        val l = pcm[i].toInt()
        val r = pcm[i + 1].toInt()
        assertTrue("Left sample out of safe range: $l", l in -32767..32767)
        assertTrue("Right sample out of safe range: $r", r in -32767..32767)
        if (l != 0) hasNonZeroL = true
        if (r != 0) hasNonZeroR = true
        if (l != r) hasStereoDiff = true
      }
      assertTrue("Theme ${theme.id} left channel silent", hasNonZeroL)
      assertTrue("Theme ${theme.id} right channel silent", hasNonZeroR)
      assertTrue("Theme ${theme.id} lacks stereo separation", hasStereoDiff)
    }

    // 2. Verify Dynamic Battle Audio Layers: Low-HP Urgency Pulse & Boss Final Ace Climax (+10% BPM & octave lift)
    val baseBossPcm = OmniHardwareAudioEngine.renderBgmLoopPcm(
      theme = BgmThemeId.CHAMPION_CONVERGENCE,
      params = audioParams,
      dynamicState = com.example.engine.DynamicBattleAudioState(),
    )
    val lowHpPcm = OmniHardwareAudioEngine.renderBgmLoopPcm(
      theme = BgmThemeId.CHAMPION_CONVERGENCE,
      params = audioParams,
      dynamicState = com.example.engine.DynamicBattleAudioState(lowHpUrgency = true),
    )
    val climaxPcm = OmniHardwareAudioEngine.renderBgmLoopPcm(
      theme = BgmThemeId.CHAMPION_CONVERGENCE,
      params = audioParams,
      dynamicState = com.example.engine.DynamicBattleAudioState(bossFinalAceClimax = true),
    )
    assertTrue("Low-HP urgency should slightly tighten tempo", lowHpPcm.size < baseBossPcm.size)
    assertTrue("Final Ace climax (+10% BPM) must render a faster loop", climaxPcm.size < lowHpPcm.size)
    assertTrue(lowHpPcm.all { it.toInt() in -32767..32767 })
    assertTrue(climaxPcm.all { it.toInt() in -32767..32767 })

    // 3. Verify all 4 Region Ambient Beds blend safely through tanh() soft-limiter
    val dryRoute = OmniHardwareAudioEngine.renderBgmLoopPcm(
      theme = BgmThemeId.HOENN_ORANGE_BRASS,
      params = audioParams,
      dynamicState = com.example.engine.DynamicBattleAudioState(ambientBed = com.example.engine.AmbientBedType.NONE),
    )
    listOf(
      com.example.engine.AmbientBedType.OCEAN_SURF,
      com.example.engine.AmbientBedType.MEADOW_WIND,
      com.example.engine.AmbientBedType.SNOW_SHIMMER,
      com.example.engine.AmbientBedType.CRYSTAL_HUM,
    ).forEach { bed ->
      val wet = OmniHardwareAudioEngine.renderBgmLoopPcm(
        theme = BgmThemeId.HOENN_ORANGE_BRASS,
        params = audioParams,
        dynamicState = com.example.engine.DynamicBattleAudioState(ambientBed = bed, ambientBedEnabled = true),
      )
      assertEquals(dryRoute.size, wet.size)
      assertTrue("Ambient bed $bed had no audible mix effect", !dryRoute.contentEquals(wet))
      assertTrue("Ambient bed $bed clipped", wet.all { it.toInt() in -32767..32767 })
    }

    // 4. Verify all StudioSfxType entries including Trainer '!' Alert, Door/Warp Swoosh, and Evolution Chime
    assertTrue(StudioSfxType.TRAINER_EYE_CONTACT in StudioSfxType.entries)
    assertTrue(StudioSfxType.DOOR_WARP_SWOOSH in StudioSfxType.entries)
    assertTrue(StudioSfxType.EVOLUTION_CHIME in StudioSfxType.entries)
    StudioSfxType.entries.forEach { sfx ->
      val pcm = OmniHardwareAudioEngine.renderSfxPcm(sfx, audioParams)
      assertTrue("SFX ${sfx.id} too short", pcm.size >= 1024)
      assertEquals(0, pcm.size % 2)
      assertTrue("SFX ${sfx.id} silent", pcm.any { it.toInt() != 0 })
      assertTrue("SFX ${sfx.id} out of safe bounds", pcm.all { it.toInt() in -32767..32767 })
    }

    // 5. Verify all 22 chapters map directly to exact era-matched 64-step themes & ambient beds
    MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
      assertEquals("Chapter ${ch.id} BGM mismatch", ch.defaultBgmThemeId, ch.resolveBgmTheme().id)
      assertTrue("Chapter ${ch.id} missing ambient bed", ch.resolveAmbientBed() != com.example.engine.AmbientBedType.NONE)
    }

    // 6. Verify 6-slot LRU BGM PCM cache caps memory cleanly and returns cached PCM without re-synth
    assertTrue("BGM PCM LRU cache must stay capped at <= 6 entries", OmniHardwareAudioEngine.bgmPcmCacheSize() <= 6)
    val cachedFirst = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.PRISM_TITLE, audioParams)
    val cachedSecond = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.PRISM_TITLE, audioParams)
    assertTrue("Repeated BGM render should return cached ShortArray instance", cachedFirst === cachedSecond)
  }
}

package com.example

import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AtlasProgressEntity
import com.example.data.local.CampaignStateEntity
import com.example.data.local.NationalAtlasIndex
import com.example.data.local.NativePokemonDatabase
import com.example.data.local.SavedMapEntity
import com.example.data.local.SavedSpriteEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.engine.AudioConvergenceSolver
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTurnField
import com.example.engine.BgmThemeId
import com.example.engine.CanonBattleResolver
import com.example.engine.CombatantSnapshot
import com.example.engine.GameFeelFrameState
import com.example.engine.HardwareConvergenceSolver
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
  fun `hardware registry loads 85 plus platforms and peak convergence avoids mud`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val platforms = UniversalHardwareRegistry.loadFromAssets(context)
    assertTrue("Expected >= 85 platforms, got ${platforms.size}", platforms.size >= 85)
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
  fun `battle engine fixes tera move types levitate wonder guard weather terrain hold items dynamax hp and capture`() {
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

    val groundedRes = CanonBattleResolver.resolveTurn(target, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.ELECTRIC_TERRAIN))
    val normalElec = CanonBattleResolver.resolveTurn(target, glalie, thunderbolt, BattleTurnField(terrain = TerrainCondition.NONE))
    assertTrue(groundedRes.maxDamage > normalElec.maxDamage)
    // Non-Tera Flying attacker is airborne and does NOT get grounded Electric Terrain boost;
    // Tera Dragon Charizard loses Flying type so it becomes grounded and DOES get 1.3x boost!
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

    // 8. Proportional Dynamax HP scaling never full-heals
    assertEquals(150, CanonBattleResolver.scaleHpProportionally(currentHp = 75, oldMaxHp = 150, newMaxHp = 300))
    assertEquals(75, CanonBattleResolver.scaleHpProportionally(currentHp = 150, oldMaxHp = 300, newMaxHp = 150))
    assertEquals(0, CanonBattleResolver.scaleHpProportionally(currentHp = 0, oldMaxHp = 150, newMaxHp = 300))

    // 9. Seeded fair speed tie
    assertTrue(CanonBattleResolver.resolveTurnOrder(0, 0, 100, 100, false, speedTieSeed = 0))
    assertFalse(CanonBattleResolver.resolveTurnOrder(0, 0, 100, 100, false, speedTieSeed = 1))

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

    val quickT1 = CanonBattleResolver.calculateCaptureResult(
      ballId = "ball_quick", ballName = "Quick Ball",
      targetCurrentHp = 100, targetMaxHp = 100, targetBaseStatTotal = 500,
      targetPrimaryType = PokemonType.WATER, targetSecondaryType = null,
      targetStatus = StatusCondition.HEALTHY, turnNumber = 1,
      isWaterSector = false, isRiftOrShadow = false, alreadyCaptured = false, deterministicRoll01 = 0.5f,
    )
    val quickT5 = CanonBattleResolver.calculateCaptureResult(
      ballId = "ball_quick", ballName = "Quick Ball",
      targetCurrentHp = 100, targetMaxHp = 100, targetBaseStatTotal = 500,
      targetPrimaryType = PokemonType.WATER, targetSecondaryType = null,
      targetStatus = StatusCondition.HEALTHY, turnNumber = 5,
      isWaterSector = false, isRiftOrShadow = false, alreadyCaptured = false, deterministicRoll01 = 0.5f,
    )
    assertTrue(quickT1.catchProbability > quickT5.catchProbability)
  }

  @Test
  fun `auto tile solver game feel engine and pure kotlin DSP audio pass invariants`() {
    // AutoTile 4-neighbor bitmask (N=1, E=2, S=4, W=8)
    val grid = MutableList(64) { 0 }
    grid[2 * 8 + 2] = 2 // center water
    grid[1 * 8 + 2] = 2 // North water
    grid[2 * 8 + 3] = 2 // East water
    val mask = AutoTileBitmaskSolver.computeMask(grid, 8, col = 2, row = 2)
    assertEquals(AutoTileBitmaskSolver.BIT_NORTH or AutoTileBitmaskSolver.BIT_EAST, mask)
    assertTrue(AutoTileBitmaskSolver.variantIndex(2, 2) in 0..3)

    // UniversalGameFeelEngine: non-stacking maxOf hitstop, mosaic dissolve, input buffer, corner correction
    var feel = GameFeelFrameState()
    feel = UniversalGameFeelEngine.triggerImpact(feel, ImpactWeight.HEAVY_OR_CRIT, targetIsEnemy = true)
    assertEquals(95f, feel.hitstopRemainingMs)
    // Triggering a lighter impact must NOT stack or shrink hitstop
    feel = UniversalGameFeelEngine.triggerImpact(feel, ImpactWeight.NORMAL, targetIsEnemy = true)
    assertEquals(95f, feel.hitstopRemainingMs)

    feel = UniversalGameFeelEngine.triggerBattleEntryMosaic(feel)
    assertEquals(10f, feel.mosaicPixelFactor)
    feel = UniversalGameFeelEngine.stepFrame(feel, 160f.coerceAtMost(50f))
    assertTrue(feel.mosaicPixelFactor < 10f)

    feel = UniversalGameFeelEngine.bufferMovementInput(feel, MoveDirection.RIGHT)
    assertEquals(MoveDirection.RIGHT, feel.bufferedDirection)
    assertEquals(130f, feel.bufferedInputRemainingMs)

    // Corner correction: moving UP into Water(2) at (2, 1) nudges to passable (3, 1)
    val overworld = MutableList(64) { 0 }
    overworld[1 * 8 + 2] = 2 // blocked water directly north of (2, 2)
    val (nx, ny) = UniversalGameFeelEngine.resolveStepWithCornerCorrection(2, 2, MoveDirection.UP, 8, overworld)
    assertEquals(3, nx)
    assertEquals(1, ny)

    // Pure-Kotlin 44.1kHz Stereo DSP Audio: non-empty, non-silent, strictly bounded [-32767, 32767]
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
  fun `room v3 persistence sprite bitmap rendering and 1025 national atlas pass roundtrip`() {
    runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, NativePokemonDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.studioPersistenceDao()

      // 1. Saved sprites roundtrip + Bitmap rendering
      val palette = SpritePixelMatrixEngine.buildInitialPalette("#00F5D4", "#FF2A85")
      val frame0 = SpritePixelMatrixEngine.generateSeededFrame(384, 0)
      val spriteEntity = SavedSpriteEntity(
        id = "test_sprite_384",
        projectId = "proj_test",
        name = "Test Rayquaza Rig",
        spriteSeedId = 384,
        paletteJson = SpritePixelMatrixEngine.serializePaletteJson(palette),
        framesRleJson = SpritePixelMatrixEngine.serializeFramesRleJson(listOf(frame0)),
        frameCount = 1,
      )
      dao.upsertSavedSprite(spriteEntity)
      val loadedSprite = dao.getSavedSpriteById("test_sprite_384")
      assertNotNull(loadedSprite)
      val decodedFrames = SpritePixelMatrixEngine.deserializeFramesRleJson(loadedSprite!!.framesRleJson)
      val decodedPalette = SpritePixelMatrixEngine.deserializePaletteJson(loadedSprite.paletteJson)
      assertEquals(frame0, decodedFrames.first())
      val bmp = SpritePixelMatrixEngine.renderFrameToBitmap(decodedFrames.first(), decodedPalette, scale = 2)
      assertEquals(32, bmp.width)
      assertEquals(32, bmp.height)
      val hasOpaquePixel = (0 until 32).any { y -> (0 until 32).any { x -> Color.alpha(bmp.getPixel(x, y)) == 255 } }
      assertTrue(hasOpaquePixel)

      // 2. Saved maps + Studio items + Atlas progress + Campaign state roundtrip
      dao.upsertSavedMap(
        SavedMapEntity(
          mapKey = "proj_test:kanto",
          projectId = "proj_test",
          regionId = "kanto",
          regionName = "Kanto",
          tilesCsv = MasterCanonTemplateCatalog.worldChapters.first().defaultMapTiles.joinToString(","),
        )
      )
      assertNotNull(dao.getSavedMap("proj_test:kanto"))

      dao.upsertItems(StudioItemCatalog.defaultItems)
      assertEquals(StudioItemCatalog.defaultItems.size, dao.countItems())

      dao.upsertAtlasProgress(AtlasProgressEntity(dexNumber = 25, seen = true, battled = true, captured = true))
      assertTrue(dao.getAtlasProgress(25)?.captured == true)

      val mask = StudioViewModelHelpers.encodeExtraPrettyMask(ExtraVisualAudioSettings.ALL_ENABLED)
      assertEquals(ExtraVisualAudioSettings.ALL_ENABLED, StudioViewModelHelpers.decodeExtraPrettyMask(mask))

      dao.upsertCampaignState(
        CampaignStateEntity(
          projectId = "proj_test",
          activeRegionId = "johto",
          playerX = 4,
          playerY = 3,
          completedRegionIdsCsv = "kanto",
          harmonicShardsCsv = "shard_kanto",
          extraPrettyMask = mask,
        )
      )
      val savedCampaign = dao.observeCampaignState("proj_test").first()
      assertEquals("johto", savedCampaign?.activeRegionId)
      assertEquals(mask, savedCampaign?.extraPrettyMask)

      // 3. Full #0001-#1025 National Atlas Index check
      val fullIndex = NationalAtlasIndex.buildCompleteIndex()
      assertEquals(NationalAtlasIndex.TOTAL_NATIONAL_SPECIES, fullIndex.size)
      assertEquals(1025, fullIndex.size)
      assertEquals("Bulbasaur", fullIndex.first().name)
      assertEquals("Pecharunt", fullIndex.last().name)
    } finally {
      db.close()
    }
    }
  }
}

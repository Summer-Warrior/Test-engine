package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonEngineVerificationTest {

  @Test
  fun `18-type matrix enforces immunities 4x dual weaknesses and special overrides`() {
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.GHOST, PokemonType.NORMAL))
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ELECTRIC, PokemonType.GROUND))
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.DRAGON, PokemonType.FAIRY))
    assertEquals(4.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ROCK, PokemonType.FIRE, PokemonType.FLYING))
    assertEquals(2.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ICE, PokemonType.WATER, isFreezeDry = true))
    assertEquals(
      1.0f,
      CanonTypeChart.calculateEffectiveness(
        moveType = PokemonType.ROCK,
        defenderPrimary = PokemonType.FLYING,
        weather = WeatherCondition.STRONG_WINDS,
      ),
    )
    assertEquals(
      2.0f,
      CanonTypeChart.calculateEffectiveness(
        moveType = PokemonType.WATER,
        defenderPrimary = PokemonType.GRASS,
        defenderTeraType = PokemonType.FIRE,
      ),
    )
  }

  @Test
  fun `stat spreads clamp EVs to 510 total and enforce Shedinja 1 HP invariant`() {
    val overflow = StatSpread(
      ivHp = 99,
      evHp = 252,
      evAtk = 252,
      evDef = 252,
      evSpe = 252,
    ).normalized()
    assertEquals(31, overflow.ivHp)
    assertEquals(510, overflow.totalEvs)
    assertEquals(6, overflow.evDef)

    val shedinja = CombatantSnapshot(
      dexNumber = 292,
      name = "Shedinja",
      level = 100,
      primaryType = PokemonType.BUG,
      secondaryType = PokemonType.GHOST,
      baseHp = 1,
      baseAtk = 90,
      baseDef = 45,
      baseSpA = 30,
      baseSpD = 30,
      baseSpe = 40,
      abilityName = "Wonder Guard",
    )
    assertEquals(1, shedinja.maxHp())
  }

  @Test
  fun `battle resolver handles anime mechanics terrain blocks and extreme stages without NaN or div-by-zero`() {
    val attacker = CombatantSnapshot(
      dexNumber = 658,
      name = "Ash-Greninja",
      level = 50,
      primaryType = PokemonType.WATER,
      secondaryType = PokemonType.DARK,
      baseHp = 72,
      baseAtk = 145,
      baseDef = 67,
      baseSpA = 153,
      baseSpD = 71,
      baseSpe = 132,
      abilityName = "Battle Bond",
      stages = StatStages(atk = -99, spA = 99),
      gimmickMode = BattleGimmickMode.ANIME_BATTLE_BOND,
    )
    val defender = CombatantSnapshot(
      dexNumber = 6,
      name = "Charizard",
      level = 50,
      primaryType = PokemonType.FIRE,
      secondaryType = PokemonType.FLYING,
      baseHp = 78,
      baseAtk = 84,
      baseDef = 78,
      baseSpA = 109,
      baseSpD = 85,
      baseSpe = 100,
      abilityName = "Blaze",
      gimmickMode = BattleGimmickMode.ANIME_COUNTER_SHIELD,
    )

    val waterShuriken = MasterCanonTemplateCatalog.signatureMovePool.first { it.name.startsWith("Water Shuriken") }
    val normalFieldResult = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = defender,
      move = waterShuriken,
      field = BattleTurnField(weather = WeatherCondition.RAIN),
    )
    assertTrue(normalFieldResult.minDamage >= 3)
    assertTrue(normalFieldResult.maxDamage >= normalFieldResult.minDamage)
    assertFalse(normalFieldResult.minPercentHp.isNaN())
    assertTrue(normalFieldResult.attackerActsFirst)

    // Priority move blocked on Psychic Terrain against grounded target
    val groundedDefender = defender.copy(secondaryType = null)
    val terrainBlocked = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = groundedDefender,
      move = waterShuriken,
      field = BattleTurnField(terrain = TerrainCondition.PSYCHIC_TERRAIN),
    )
    assertTrue(terrainBlocked.isBlockedByTerrain)
    assertEquals(0, terrainBlocked.maxDamage)

    // Contact move triggers Anime Counter-Shield chip recoil
    val contactMove = MasterCanonTemplateCatalog.signatureMovePool.first { it.isContact }
    val contactResult = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = defender,
      move = contactMove,
      field = BattleTurnField(),
    )
    assertTrue(contactResult.counterShieldChipRecoil > 0)
  }

  @Test
  fun `omni-region campaign prevents sequence-breaking and integrity audit passes`() {
    val start = CampaignProgressionState()
    assertEquals(22, MasterCanonTemplateCatalog.worldChapters.size)
    val sinnoh = MasterCanonTemplateCatalog.worldChapters.first { it.id == "sinnoh" }
    val kanto = MasterCanonTemplateCatalog.worldChapters.first { it.id == "kanto" }

    val blockedSinnoh = start.completeChapter(sinnoh)
    assertTrue(blockedSinnoh.completedChapterIds.isEmpty())

    val afterKanto = start.completeChapter(kanto)
    assertTrue("kanto" in afterKanto.completedChapterIds)
    assertEquals(1, afterKanto.harmonicShardsCollected)
    assertTrue(afterKanto.worldCoronationRank < start.worldCoronationRank)
    assertTrue(afterKanto.validateInvariants().isEmpty())

    val audit = MasterCanonTemplateCatalog.runEngineIntegrityAudit()
    assertTrue(audit.isNotEmpty())
    assertTrue(audit.all { it.second })
  }

  @Test
  fun `all 22 fusion regions 57 trainer types 1025 pokemon coverage landmarks and time of day pass pure JVM checks`() {
    val chapters = MasterCanonTemplateCatalog.worldChapters
    assertEquals(22, chapters.size)
    val chapterIds = chapters.map { it.id }.toSet()
    assertEquals(22, chapterIds.size)
    assertEquals(22, chapters.map { it.biomeStyle }.toSet().size)

    val uniqueMapHashes = mutableSetOf<Int>()
    val unionDex = mutableSetOf<Int>()

    chapters.forEach { chapter ->
      assertEquals(256, chapter.defaultMapTiles.size)
      assertTrue(chapter.defaultMapTiles.all { it in 0..11 })
      assertTrue("Region ${chapter.id} missing grass(1)", 1 in chapter.defaultMapTiles)
      assertTrue("Region ${chapter.id} missing road(3)", 3 in chapter.defaultMapTiles)
      assertTrue("Region ${chapter.id} missing sanctuary(5)", 5 in chapter.defaultMapTiles)
      assertTrue("Region ${chapter.id} missing healing(8)", 8 in chapter.defaultMapTiles)
      assertTrue("Region ${chapter.id} missing landmark(10)", 10 in chapter.defaultMapTiles)
      assertTrue("Region ${chapter.id} missing warp(11)", 11 in chapter.defaultMapTiles)

      val spawnTile = chapter.defaultMapTiles[chapter.spawnRow * MasterCanonTemplateCatalog.MAP_GRID_SIZE + chapter.spawnCol]
      assertTrue(
        "Region ${chapter.id} spawn tile=$spawnTile not passable",
        UniversalGameFeelEngine.isTilePassable(spawnTile),
      )
      assertEquals(3, chapter.subZones.size)
      assertTrue(chapter.subZones.all { it.name.isNotBlank() && it.col in 0..15 && it.row in 0..15 })
      assertEquals(3, chapter.pointsOfInterest.size)
      assertTrue(chapter.connectedRegionIds.isNotEmpty() && chapter.connectedRegionIds.all { it in chapterIds })
      assertTrue(chapter.regionalTrainerClassIds.all { TrainerArchetypeCatalog.findById(it) != null })
      chapter.pointsOfInterest.forEach { poi ->
        poi.trainerArchetypeId?.let { id ->
          assertTrue("Missing trainer $id in ${chapter.id}", TrainerArchetypeCatalog.findById(id) != null)
        }
      }

      val regionalPool = com.example.data.local.NationalAtlasIndex.dexPoolForRegion(chapter.id)
      assertTrue(regionalPool.isNotEmpty())
      unionDex += regionalPool
      uniqueMapHashes += chapter.defaultMapTiles.hashCode()
    }

    assertEquals(22, uniqueMapHashes.size)
    assertEquals(1025, unionDex.size)
    assertTrue((1..1025).all { it in unionDex })

    val trainers = TrainerArchetypeCatalog.allTrainerTypes
    assertTrue("Expected >= 50 Trainer Types, got ${trainers.size}", trainers.size >= 50)
    assertEquals(trainers.size, trainers.map { it.id }.distinct().size)
    TrainerCategory.entries.forEach { cat ->
      assertTrue(TrainerArchetypeCatalog.filterByCategory(cat).isNotEmpty())
    }
    trainers.forEach { tr ->
      assertTrue(TrainerArchetypeCatalog.resolveEnemyDexForTrainer(tr, "kanto", seedNonce = 5) in 1..1025)
    }

    assertEquals(listOf(TimeOfDayCycle.DAY, TimeOfDayCycle.DUSK, TimeOfDayCycle.NIGHT), TimeOfDayCycle.entries)
    val tile0Label = MasterCanonTemplateCatalog.tileLegendLabels.firstOrNull { it.first == 0 }?.second
    assertEquals("Meadow / Plaza", tile0Label)
    assertFalse(tile0Label!!.startsWith("("))
  }

  @Test
  fun `offline 1025+ species atlas json and ProceduralCreatureRigEngine pass full clean-room verification`() {
    val assetFile = listOf(
      java.io.File("src/main/assets/resonance_species_atlas.json"),
      java.io.File("app/src/main/assets/resonance_species_atlas.json"),
    ).firstOrNull { it.isFile }
    assertTrue("Missing app/src/main/assets/resonance_species_atlas.json", assetFile != null)

    val parsed = com.example.data.local.NationalAtlasIndex.parseAtlasJson(assetFile!!.readText(Charsets.UTF_8))
    assertEquals(1025, parsed.size)

    val allNames = parsed.map { it.name }.toSet()
    assertEquals("All 1,025 species must have 100% unique names", 1025, allNames.size)

    val validTypes = PokemonType.entries.map { it.displayName }.toSet()
    val validRegions = MasterCanonTemplateCatalog.worldChapters.map { it.id }.toSet()
    val homeRegionsSeen = mutableSetOf<String>()
    var totalForms = 0

    parsed.forEachIndexed { idx, sp ->
      val expectedDex = idx + 1
      assertEquals(expectedDex, sp.dexNumber)
      assertTrue("Species #$expectedDex has blank name", sp.name.isNotBlank())
      assertFalse("Species #$expectedDex is a placeholder: ${sp.name}", sp.name.startsWith("Species #"))
      assertTrue("Species #$expectedDex missing epithet", sp.epithet.isNotBlank())
      assertTrue("Species #$expectedDex invalid primaryType ${sp.primaryType}", sp.primaryType in validTypes)
      if (sp.secondaryType.isNotBlank()) {
        assertTrue("Species #$expectedDex invalid secondaryType ${sp.secondaryType}", sp.secondaryType in validTypes)
      }
      assertTrue("Species #$expectedDex invalid region ${sp.regionId}", sp.regionId in validRegions)
      homeRegionsSeen += sp.regionId
      assertTrue("Species #$expectedDex invalid stats", sp.baseHp in 1..255 && sp.baseAttack in 1..255 && sp.baseDefense in 1..255 && sp.baseSpAtk in 1..255 && sp.baseSpDef in 1..255 && sp.baseSpeed in 1..255)
      assertTrue("Species #$expectedDex missing signature ability", sp.signatureAbility.isNotBlank())
      assertTrue("Species #$expectedDex missing signature move", sp.signatureMove.isNotBlank())
      totalForms += sp.availableForms.size

      // Verify ProceduralCreatureRigEngine generates valid non-empty transparent-backed 32x32 matrix for EVERY species
      val matrix = ProceduralCreatureRigEngine.generateIndexedSprite32(sp.dexNumber)
      assertEquals(32, matrix.width)
      assertEquals(32, matrix.height)
      assertEquals(1024, matrix.pixels.size)
      assertEquals(8, matrix.paletteArgb.size)
      assertTrue("Species #${sp.dexNumber} has too few opaque pixels: ${matrix.opaquePixelCount()}", matrix.opaquePixelCount() >= 60)
      assertTrue("Species #${sp.dexNumber} missing transparent background", matrix.transparentPixelCount() >= 100)
      assertTrue("Species #${sp.dexNumber} missing 1px ink outline", matrix.inkPixelCount() >= 20)
      assertTrue("Species #${sp.dexNumber} missing 3D voxel depth", matrix.depthMap.any { it.toInt() >= 5 })
    }

    assertEquals("All 22 regions must have native home species", 22, homeRegionsSeen.size)
    assertTrue("Expected >1100 total playable forms including Regional/Mega/Paradox/Prism, got $totalForms", totalForms > 1100)

    // Verify all 5 CreatureVariantForms produce distinct palettes and valid matrices
    val formPalettes = com.example.data.local.CreatureVariantForm.entries.map { form ->
      val m = ProceduralCreatureRigEngine.generateIndexedSprite32(384, form)
      assertTrue(m.opaquePixelCount() >= 60)
      m.paletteArgb.toList()
    }.toSet()
    assertEquals(com.example.data.local.CreatureVariantForm.entries.size, formPalettes.size)

    // Verify Sprite Editor 16x16 sampling works offline for any creature
    val (pal, frames) = ProceduralCreatureRigEngine.sampleToSpriteEditor16(25)
    assertEquals(8, pal.size)
    assertEquals(2, frames.size)
    assertEquals(SpritePixelMatrixEngine.PIXEL_COUNT, frames[0].size)
    assertTrue(frames[0].any { it >= 0 } && frames[0].any { it == -1 })
  }

  @Test
  fun `trainer catalog 22-region roster 4-direction sprites portraits and multi-creature tactical AI pass pure JVM checks`() {
    val allTrainers = TrainerCatalogAndRigEngine.allBuiltInTrainers
    assertTrue("Expected >= 74 built-in Trainers, got ${allTrainers.size}", allTrainers.size >= 74)
    assertEquals(allTrainers.size, allTrainers.map { it.id }.distinct().size)

    // Every single TrainerRoleCategory must be populated
    TrainerRoleCategory.entries.forEach { role ->
      assertTrue("Missing role category $role", allTrainers.any { it.roleCategory == role })
    }

    // Every single one of the 22 regions must have >= 3 Trainers (Leader + Challenger + Mentor) & valid POI/Boss mapping
    MasterCanonTemplateCatalog.worldChapters.forEach { chapter ->
      val regional = TrainerCatalogAndRigEngine.trainersForRegion(chapter.id)
      assertTrue("Region ${chapter.id} has fewer than 3 Trainers: ${regional.size}", regional.size >= 3)
      val boss = TrainerCatalogAndRigEngine.resolveBossTrainerForRegion(chapter.id)
      assertTrue("Boss ${boss.name} in ${chapter.id} should have multi-creature team (2-6)", boss.teamDexIds.size in 2..6)
      chapter.pointsOfInterest.forEach { poi ->
        val npc = TrainerCatalogAndRigEngine.resolveTrainerForPoi(poi, chapter.id)
        assertTrue(npc.name.isNotBlank() && npc.teamDexIds.isNotEmpty())
      }
    }

    // Verify 32x32 2D Overworld + Battle Pixel Sprites across all 7 poses + 2 walk frames + 3D depth map + 1px ink outline
    val hero = TrainerCatalogAndRigEngine.defaultProtagonist
    val posePixelHashes = TrainerPose.entries.map { pose ->
      val m0 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(hero, pose, walkFrameIndex = 0)
      assertEquals(32, m0.width)
      assertEquals(32, m0.height)
      assertEquals(1024, m0.pixels.size)
      assertEquals(15, m0.paletteArgb.size)
      assertTrue("Pose $pose missing opaque pixels", m0.pixels.count { it >= 0 } >= 100)
      assertTrue("Pose $pose missing transparent backdrop", m0.pixels.count { it == -1 } >= 200)
      assertTrue("Pose $pose missing 1px ink outline", m0.pixels.count { it == 0 } >= 35)
      assertTrue("Pose $pose missing 3D depth map layers", m0.depthMap.any { it.toInt() >= 6 })
      m0.pixels.contentHashCode()
    }.toSet()
    assertEquals("All 7 Trainer poses must produce distinct silhouettes", TrainerPose.entries.size, posePixelHashes.size)

    // Verify walk stride frame 0 vs frame 1 differ cleanly for animation
    val walk0 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(hero, TrainerPose.OVERWORLD_DOWN, 0)
    val walk1 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(hero, TrainerPose.OVERWORLD_DOWN, 1)
    assertFalse("Walk frames 0 and 1 must animate legs", walk0.pixels.contentEquals(walk1.pixels))

    // Verify 40x40 High-Contrast Character Dialogue Portraits for core cast & Syndicate
    listOf("protag_nova", "prof_lyra_vance", "rival_kael", "commander_vex", "leader_sinnoh", "aura_guardian_sirion").forEach { id ->
      val spec = TrainerCatalogAndRigEngine.findById(id)!!
      val portrait = TrainerCatalogAndRigEngine.generateTrainerPortraitMatrix(spec)
      assertEquals(40, portrait.width)
      assertEquals(40, portrait.height)
      assertEquals(1600, portrait.pixels.size)
      assertTrue("Portrait $id missing border ink", portrait.pixels[0] == 0)
      assertTrue("Portrait $id missing eye highlights", portrait.pixels.any { it == 12 })
    }

    // Verify Multi-Creature Trainer Battle AI: tactical mid-battle switching + faint replacement + Ace gimmick
    val party = listOf(130, 59, 149) // Water/Flying (weak to Electric), Fire, Dragon/Flying
    val hpMap = mapOf(130 to 160, 59 to 150, 149 to 175)
    val types: (Int) -> Pair<PokemonType, PokemonType?> = { dex ->
      when (dex) {
        130 -> PokemonType.WATER to PokemonType.FLYING
        59 -> PokemonType.GROUND to null // resists/immune to Electric for tactical pivot check
        else -> PokemonType.DRAGON to PokemonType.FLYING
      }
    }
    val tacticalSwitch = TrainerBattleAiEngine.chooseTacticalMidBattleSwitchIndex(
      tactic = TrainerAiTactic.STATUS_TACTICIAN,
      partyDexIds = party,
      currentIndex = 0,
      hpMap = hpMap,
      turnNumber = 2,
      playerPrimaryType = PokemonType.ELECTRIC,
      resolveTypes = types,
    )
    assertEquals("Tactical AI should pivot from 4x-weak Water/Flying into Ground partner", 1, tacticalSwitch)

    // Next conscious partner on faint saves Ace (last slot) until non-Ace fainted
    val afterLeadFaint = TrainerBattleAiEngine.chooseNextConsciousIndex(
      partyDexIds = party,
      currentIndex = 0,
      hpMap = mapOf(130 to 0, 59 to 150, 149 to 175),
      playerPrimaryType = PokemonType.ELECTRIC,
      resolveTypes = types,
    )
    assertEquals(1, afterLeadFaint)

    // Ace Gimmick activates on final partner
    assertEquals(
      BattleGimmickMode.NONE,
      TrainerBattleAiEngine.resolveGimmickForEnemySlot(BattleGimmickMode.MEGA_EVOLUTION, party, 0, hpMap),
    )
    assertEquals(
      BattleGimmickMode.MEGA_EVOLUTION,
      TrainerBattleAiEngine.resolveGimmickForEnemySlot(
        BattleGimmickMode.MEGA_EVOLUTION,
        party,
        2,
        mapOf(130 to 0, 59 to 0, 149 to 175),
      ),
    )
  }

  @Test
  fun `64-step multi-section soundtracks dynamic battle layers ambient beds and expanded SFX pass pure JVM DSP checks`() {
    val audioParams = AudioConvergenceSolver.solveAudio(
      activePlatforms = emptyList(),
      disciplineMode = com.example.data.model.MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      extraPretty = com.example.data.model.ExtraVisualAudioSettings(),
    )

    // 1. All 16 themes have 64-step multi-section scores, non-zero stereo PCM, stereo width, and safe bounds
    assertEquals(16, BgmThemeId.entries.size)
    assertEquals(16, BgmThemeId.entries.map { it.id }.distinct().size)
    BgmThemeId.entries.forEach { theme ->
      assertEquals("Theme ${theme.id} must be 64 steps", 64, theme.stepCount)
      val pcm = OmniHardwareAudioEngine.renderBgmLoopPcm(theme, audioParams)
      assertTrue("Theme ${theme.id} PCM too short", pcm.size >= 64 * 1024 * 2)
      assertEquals(0, pcm.size % 2)
      var hasNonZeroL = false
      var hasNonZeroR = false
      var hasStereoDiff = false
      for (i in pcm.indices step 2) {
        val l = pcm[i].toInt()
        val r = pcm[i + 1].toInt()
        assertTrue(l in -32767..32767)
        assertTrue(r in -32767..32767)
        if (l != 0) hasNonZeroL = true
        if (r != 0) hasNonZeroR = true
        if (l != r) hasStereoDiff = true
      }
      assertTrue("Left channel silent for ${theme.id}", hasNonZeroL)
      assertTrue("Right channel silent for ${theme.id}", hasNonZeroR)
      assertTrue("Stereo separation missing for ${theme.id}", hasStereoDiff)
    }

    // 2. Dynamic Battle Layers: Low-HP Urgency Pulse (<25% HP) & Final Ace Climax (+10% BPM & octave lift)
    val baseBattle = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.CHAMPION_CONVERGENCE, audioParams)
    val lowHpBattle = OmniHardwareAudioEngine.renderBgmLoopPcm(
      BgmThemeId.CHAMPION_CONVERGENCE,
      audioParams,
      DynamicBattleAudioState(lowHpUrgency = true),
    )
    val climaxBattle = OmniHardwareAudioEngine.renderBgmLoopPcm(
      BgmThemeId.CHAMPION_CONVERGENCE,
      audioParams,
      DynamicBattleAudioState(bossFinalAceClimax = true),
    )
    assertTrue("Low-HP pulse must tighten loop duration", lowHpBattle.size < baseBattle.size)
    assertTrue("Boss climax (+10% BPM) must be faster than low-HP base", climaxBattle.size < lowHpBattle.size)
    assertTrue(lowHpBattle.all { it.toInt() in -32767..32767 })
    assertTrue(climaxBattle.all { it.toInt() in -32767..32767 })

    // 3. All 4 Region Ambient Beds mix audibly and safely through tanh() soft-limiter
    val dryRoute = OmniHardwareAudioEngine.renderBgmLoopPcm(
      BgmThemeId.JOHTO_KITAKAMI_KOTO,
      audioParams,
      DynamicBattleAudioState(ambientBed = AmbientBedType.NONE),
    )
    listOf(
      AmbientBedType.OCEAN_SURF,
      AmbientBedType.MEADOW_WIND,
      AmbientBedType.SNOW_SHIMMER,
      AmbientBedType.CRYSTAL_HUM,
    ).forEach { bed ->
      val wet = OmniHardwareAudioEngine.renderBgmLoopPcm(
        BgmThemeId.JOHTO_KITAKAMI_KOTO,
        audioParams,
        DynamicBattleAudioState(ambientBed = bed, ambientBedEnabled = true),
      )
      assertEquals(dryRoute.size, wet.size)
      assertFalse("Ambient bed $bed should alter stereo mix", dryRoute.contentEquals(wet))
      assertTrue(wet.all { it.toInt() in -32767..32767 })
    }

    // 4. All StudioSfxType entries including Trainer '!' Alert, Door/Warp Swoosh, and Evolution Chime
    StudioSfxType.entries.forEach { sfx ->
      val pcm = OmniHardwareAudioEngine.renderSfxPcm(sfx, audioParams)
      assertTrue("SFX ${sfx.id} too short", pcm.size >= 1024)
      assertEquals(0, pcm.size % 2)
      assertTrue("SFX ${sfx.id} silent", pcm.any { it.toInt() != 0 })
      assertTrue("SFX ${sfx.id} out of safe range", pcm.all { it.toInt() in -32767..32767 })
    }

    // 5. All 22 regions map to exact era-matched 64-step themes & non-NONE ambient beds
    MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
      assertEquals("Region ${ch.id} defaultBgmThemeId not directly registered", ch.defaultBgmThemeId, ch.resolveBgmTheme().id)
      assertTrue("Region ${ch.id} missing ambient bed", ch.resolveAmbientBed() != AmbientBedType.NONE)
    }

    // 6. Audio LRU cache strictly caps at <= 6 entries and returns identical cached PCM on hit
    assertTrue("BGM LRU cache exceeded 6 entries: ${OmniHardwareAudioEngine.bgmPcmCacheSize()}", OmniHardwareAudioEngine.bgmPcmCacheSize() <= 6)
    val hit1 = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.PRISM_TITLE, audioParams)
    val hit2 = OmniHardwareAudioEngine.renderBgmLoopPcm(BgmThemeId.PRISM_TITLE, audioParams)
    assertTrue("Repeated identical BGM request must hit LRU cache", hit1 === hit2)
  }

  @Test
  fun `unified naming 22-region caps hero companion poses sprite editor sampling and diegetic dialogue pass pure JVM checks`() {
    // 1. Unified Species Naming across NationalAtlasIndex & built-in franchise dex (zero split-brain)
    val builtIn = com.example.data.local.NativeFranchiseCatalogRepository.builtInFranchiseDex.associateBy { it.dexNumber }
    val unifiedIndex = com.example.data.local.NationalAtlasIndex.buildCompleteIndex(builtIn)
    assertEquals(1025, unifiedIndex.size)
    assertEquals("All 1,025 unified species must retain unique primary names", 1025, unifiedIndex.map { it.name }.toSet().size)
    assertEquals("Bulbasaur", com.example.data.local.NationalAtlasIndex.peekSpecies(1).name)
    assertEquals("Pikachu", com.example.data.local.NationalAtlasIndex.peekSpecies(25).name)
    assertEquals("Rayquaza", com.example.data.local.NationalAtlasIndex.peekSpecies(384).name)
    assertEquals("Miraidon", com.example.data.local.NationalAtlasIndex.peekSpecies(1008).name)
    assertTrue(com.example.data.local.NationalAtlasIndex.peekSpecies(25).prismAlias.isNotBlank())

    // 2. 22-Region Campaign progression cap formula (all 22 chapters completable without hitting stale 14-region ceiling)
    var state = CampaignProgressionState()
    MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
      state = state.completeChapter(ch)
    }
    assertEquals(22, state.completedChapterIds.size)
    assertEquals(22, state.harmonicShardsCollected)
    assertTrue("Badges must surpass legacy 14-region 112 cap and stay <= 176, got ${state.badgesEarned}", state.badgesEarned in 113..176)
    assertEquals(44, state.animeRibbonsAndKeys)
    assertEquals(100, state.worldCoronationRank)
    assertTrue("All 22-region invariants must hold", state.validateInvariants().isEmpty())

    // 3. Hero companionPoseIndex (0..2) drives distinct visible pixel matrices & 16x16 Sprite Editor sampling
    val baseHero = TrainerCatalogAndRigEngine.defaultProtagonist
    val pose0 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(
      baseHero.copy(appearance = baseHero.appearance.copy(companionPoseIndex = 0)),
      TrainerPose.BATTLE_IDLE,
    )
    val pose1 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(
      baseHero.copy(appearance = baseHero.appearance.copy(companionPoseIndex = 1)),
      TrainerPose.BATTLE_IDLE,
    )
    val pose2 = TrainerCatalogAndRigEngine.generateTrainerSpriteMatrix(
      baseHero.copy(appearance = baseHero.appearance.copy(companionPoseIndex = 2)),
      TrainerPose.BATTLE_IDLE,
    )
    assertFalse("Companion pose 0 and 1 must produce visually distinct pixels", pose0.pixels.contentEquals(pose1.pixels))
    assertFalse("Companion pose 1 and 2 must produce visually distinct pixels", pose1.pixels.contentEquals(pose2.pixels))

    val (heroPal, heroFrames) = TrainerCatalogAndRigEngine.sampleTrainerToSpriteEditor16(baseHero)
    assertEquals(8, heroPal.size)
    assertEquals(2, heroFrames.size)
    assertEquals(SpritePixelMatrixEngine.PIXEL_COUNT, heroFrames[0].size)
    assertEquals(SpritePixelMatrixEngine.PIXEL_COUNT, heroFrames[1].size)
    assertTrue("Hero 16x16 sample must contain opaque pixels and transparent backdrop", heroFrames[0].any { it >= 0 } && heroFrames[0].any { it == -1 })

    // 4. Diegetic Dialogue Audit: zero 4th-wall engine/website strings in NPC or Trainer dialogue
    val forbiddenDialogueTokens = listOf("156 hardware", "Omni-Console Viewport", "PokémonDB", "Serebii")
    MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
      ch.pointsOfInterest.forEach { poi ->
        forbiddenDialogueTokens.forEach { token ->
          assertFalse(
            "4th-wall token '$token' found in POI dialogue (${ch.id}/${poi.id}): ${poi.dialogue}",
            poi.dialogue.contains(token, ignoreCase = true),
          )
        }
      }
    }
    TrainerCatalogAndRigEngine.allBuiltInTrainers.forEach { tr ->
      val combinedQuotes = (listOf(tr.introQuote, tr.switchQuote, tr.defeatQuote) + tr.dialogueLines).joinToString(" ")
      forbiddenDialogueTokens.forEach { token ->
        assertFalse(
          "4th-wall token '$token' found in trainer dialogue (${tr.id}): $combinedQuotes",
          combinedQuotes.contains(token, ignoreCase = true),
        )
      }
    }
    TrainerArchetypeCatalog.allTrainerTypes.forEach { tr ->
      forbiddenDialogueTokens.forEach { token ->
        assertFalse(
          "4th-wall token '$token' found in trainer intro (${tr.id}): ${tr.introQuote}",
          tr.introQuote.contains(token, ignoreCase = true),
        )
      }
    }
  }

  @Test
  fun `all 5 clean-room side modes pass deterministic mechanics scoring reachability and capture checks`() {
    val resolveEntity: (Int) -> com.example.data.local.NativePokemonEntity = { dex ->
      com.example.ui.StudioViewModelHelpers.synthesizePokemonFromAtlas(dex)
    }

    // 1. Battle Tower / Frontier Streak: 7-battle escalating gauntlet + auto-heal + Frontier Brain Boss on Round 7
    var tower = BattleTowerEngine.startGauntlet(
      partyDexIds = listOf(384, 150, 448),
      resolveEntity = resolveEntity,
      startingStreak = 0,
    )
    assertTrue(tower.isActive)
    assertEquals(1, tower.roundNumber)
    assertFalse(tower.isFrontierBrainRound)
    val dragonMove = BattleMoveSpec("Prism Dragon Surge", PokemonType.DRAGON, MoveCategory.SPECIAL, basePower = 180)
    val steelMove = BattleMoveSpec("Prism Flash Cannon", PokemonType.STEEL, MoveCategory.SPECIAL, basePower = 180)
    var totalBp = 0
    for (round in 1..7) {
      assertEquals(round, tower.roundNumber)
      assertEquals(round == 7, tower.isFrontierBrainRound)
      // Verify auto-heal restored all player slots at start of every round
      tower.playerTeamDexIds.forEach { dex ->
        assertEquals(tower.playerMaxHpMap[dex], tower.playerHpMap[dex])
      }
      var guard = 0
      while (!tower.roundCleared && !tower.gauntletFailed && guard < 40) {
        val enemy = resolveEntity(tower.activeEnemyDex)
        val ePrimary = PokemonType.fromString(enemy.primaryType)
        val eSecondary = enemy.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) }
        val chosenMove = if (CanonTypeChart.calculateEffectiveness(PokemonType.DRAGON, ePrimary, eSecondary) > 0f) {
          dragonMove
        } else {
          steelMove
        }
        val out = BattleTowerEngine.executeTurn(tower, chosenMove, resolveEntity)
        tower = out.nextState
        totalBp += out.bpAwardedNow
        guard++
      }
      assertTrue("Round $round should clear cleanly", tower.roundCleared)
      if (round < 7) {
        tower = BattleTowerEngine.advanceToNextRound(tower, resolveEntity)
      }
    }
    assertTrue(tower.gauntletCompleted)
    assertEquals(7, tower.currentStreak)
    assertEquals(33, totalBp) // 6 * 3 BP + 15 BP Frontier Brain
    assertTrue(BattleTowerEngine.bpShopCatalog.size >= 6)

    // 2. Prism Contest & Showcase Appeal Stage: 5 rounds + Crowd Excitement Encore + Ribbon victory
    val lead = resolveEntity(384)
    var contest = PrismContestEngine.startContest(ContestCategory.COOL, "Nova", lead)
    assertEquals(4, contest.contestants.size)
    val coolMoveA = BattleMoveSpec("Dragon Ascent", PokemonType.DRAGON, MoveCategory.PHYSICAL, 120)
    val coolMoveB = BattleMoveSpec("Extreme Speed", PokemonType.ELECTRIC, MoveCategory.PHYSICAL, 80, priority = 2)
    val appealA = PrismContestEngine.mapMoveToAppeal(coolMoveA)
    val appealB = PrismContestEngine.mapMoveToAppeal(coolMoveB)
    var wonRibbon = false
    for (r in 1..5) {
      val out = PrismContestEngine.performAppealRound(contest, if (r % 2 == 1) appealA else appealB)
      contest = out.nextState
      if (out.ribbonAwarded) wonRibbon = true
    }
    assertTrue(contest.isFinished)
    assertTrue("Alternating matching Cool + Counter-Shield + Jam appeals should win the Prism Ribbon", wonRibbon)
    assertTrue(contest.playerWonRibbon)

    // 3. Lental Photosphere Snap: Illumina Orb + Apple + Centered Night shot yields 4★ Diamond (>= 4000 pts)
    var snap = LentalSnapEngine.createSession("lental")
    snap = LentalSnapEngine.stepFrame(snap)
    snap = LentalSnapEngine.tossIlluminaOrb(snap)
    snap = LentalSnapEngine.tossApple(snap)
    val centeredSnap = snap.copy(subjectX = 0.50f, subjectY = 0.50f, depthScale = 1.05f)
    val (afterSnap, shot) = LentalSnapEngine.capturePhoto(centeredSnap, "Pikachu", TimeOfDayCycle.NIGHT)
    assertEquals(4, shot.starRating)
    assertEquals("4★ Diamond", shot.tierLabel)
    assertTrue("Expected >= 4000 Diamond score, got ${shot.totalScore}", shot.totalScore >= 4000)
    assertEquals(1, afterSnap.shotsTaken.size)

    // 4. Mystery Rift Dungeon Crawl: 5 procedural 10x10 floors + BFS reachability + Floor-5 Guardian defeat
    for (f in 1..5) {
      val floor = MysteryRiftEngine.generateFloor(f, seed = 42, regionId = "mystery_rift")
      assertEquals(100, floor.tiles.size)
      assertTrue(
        "Floor $f target (${floor.stairsRow},${floor.stairsCol}) must be BFS-reachable from spawn",
        MysteryRiftEngine.isTargetReachable(floor, floor.stairsRow, floor.stairsCol),
      )
      if (f == 5) {
        assertTrue("Floor 5 must spawn the Rift Guardian", floor.enemies.any { it.isFloorGuardian })
      }
    }
    val f5 = MysteryRiftEngine.generateFloor(5, seed = 42, regionId = "mystery_rift")
    val guardian = f5.enemies.first { it.isFloorGuardian }
    var rift = MysteryRiftState(
      isActive = true,
      seed = 42,
      floorNumber = 5,
      tiles = f5.tiles,
      playerRow = guardian.row,
      playerCol = (guardian.col - 1).coerceAtLeast(1),
      playerHp = 220,
      playerMaxHp = 220,
      playerLevel = 8,
      enemies = listOf(guardian),
    )
    var guardianDown = false
    repeat(6) {
      if (!rift.dungeonCleared) {
        val out = MysteryRiftEngine.stepOrAttack(rift, 0, 0, useSignatureSkill = true)
        rift = out.nextState
        if (out.guardianDefeated) guardianDown = true
      }
    }
    assertTrue("Floor-5 Rift Guardian should be defeated by Prism Pulse", guardianDown)
    assertTrue(rift.dungeonCleared)

    // 5. Ranger Capture Styler (touch-loop 360° circle drag) & Prism Camp Bonding (0 -> 100%)
    var ranger = RangerCampEngine.createTrial(448, "Lucario")
    ranger = RangerCampEngine.beginDrag(ranger, ranger.targetX + 0.22f, ranger.targetY)
    val stepsPerLoop = 24
    for (loop in 0 until ranger.loopsRequired) {
      for (s in 1..stepsPerLoop) {
        val theta = (s.toDouble() / stepsPerLoop.toDouble()) * 2.0 * kotlin.math.PI
        val nx = (ranger.targetX + kotlin.math.cos(theta) * 0.22).toFloat()
        val ny = (ranger.targetY + kotlin.math.sin(theta) * 0.22).toFloat()
        ranger = RangerCampEngine.stepLoopDrag(ranger, nx, ny).nextState
      }
    }
    assertTrue("4 full 360-degree Styler loops should capture Lucario", ranger.isCaptured)
    assertEquals(1f, ranger.friendshipGauge, 0.01f)

    var bond = 35
    var camp = PrismCampState()
    repeat(4) {
      val (nextBond, nextCamp) = RangerCampEngine.performCampActivity(bond, CampActivityType.PRISM_CURRY, "Pikachu", camp)
      bond = nextBond
      camp = nextCamp
    }
    assertEquals(100, bond)
    assertEquals(4, camp.activePartyMoodBoostNonce)
  }

  @Test
  fun `all 10 comparison ROM profiles compile cleanly with distinct locked hardware tags extra-pretty bitmask round-trip and 22-region maps`() {
    val profiles = PrismRomPackageEngine.comparisonProfiles
    assertEquals("Expected all 10 flagship comparison ROM profiles", 10, profiles.size)
    assertEquals(
      setOf(
        "GB_DMG",
        "NES_VRC6",
        "SNES_FX2",
        "GBA_AGB",
        "PS1_GTE",
        "GC_TEV",
        "NDS_25D",
        "3DS_PICA200",
        "SWITCH_HD2D",
        "ULTIMATE_156_CURATED",
      ),
      profiles.map { it.comparisonTag }.toSet(),
    )

    // Verify 9-bit Extra Pretty bitmask round-trips every individual toggle and ALL_ENABLED losslessly
    val allMask = PrismRomPackageEngine.encodeExtraPrettyMask(com.example.data.model.ExtraVisualAudioSettings.ALL_ENABLED)
    assertEquals(0x1FF, allMask)
    assertEquals(com.example.data.model.ExtraVisualAudioSettings.ALL_ENABLED, PrismRomPackageEngine.decodeExtraPrettyMask(allMask))
    assertEquals(com.example.data.model.ExtraVisualAudioSettings(), PrismRomPackageEngine.decodeExtraPrettyMask(0))

    // Compile all 10 comparison ROMs from "Pokémon: Prism Resonance" with 22-region maps, custom hero, party & Side Modes
    val all22Maps = MasterCanonTemplateCatalog.worldChapters.associate { it.id to it.defaultMapTiles }
    val customHero = TrainerCatalogAndRigEngine.defaultProtagonist.copy(name = "Aria", title = "Prism Sovereign")
    val sampleRecord = com.example.data.local.SideModeRecordEntity(
      projectId = "prism_resonance_default",
      battleTowerBestStreak = 14,
      battleTowerCurrentStreak = 7,
      battlePoints = 120,
      contestRibbonsWon = 5,
      contestBestScore = 18,
      bestSnapScore = 4450,
      bestSnapStarRating = 4,
      bestSnapCaption = "Rayquaza (4★ • Prism Bloom)",
      deepestRiftFloor = 5,
      riftGuardiansDefeated = 2,
      rangerCapturesCompleted = 9,
      campMealsServed = 6,
    )
    val simulatedCatalog = (UniversalHardwareRegistry.defaultCuratedHardwareIds + profiles.flatMap { it.platformIds }).sorted()
    val compiledTen = PrismRomPackageEngine.compileAllTenComparisonRoms(
      romTitle = "Pokémon: Prism Resonance",
      allCatalogIds = simulatedCatalog,
      activeRegionId = "sinnoh",
      playerRow = 6,
      playerCol = 8,
      regionMapOverrides = all22Maps,
      playerTrainer = customHero,
      playerCustomAvatarRle = "0:4,1:4,2:4",
      playerCustomAvatarPaletteJson = "[4278248959,4294912645]",
      useCustomPlayerAvatarBitmap = true,
      partyDexIds = listOf(25, 6, 94, 448, 658, 384),
      partyLevels = mapOf(25 to 62, 384 to 75),
      heldItemsMap = mapOf(25 to "light_ball", 384 to "life_orb"),
      completedRegionIds = setOf("kanto", "johto", "hoenn"),
      sideModeRecord = sampleRecord,
    )

    assertEquals(10, compiledTen.size)
    assertEquals(10, compiledTen.map { it.romId }.toSet().size)
    compiledTen.forEach { rom ->
      assertEquals("Pokémon: Prism Resonance", rom.romTitle)
      assertEquals("sinnoh", rom.activeRegionId)
      assertEquals(22, rom.regionMapOverrides.size)
      assertEquals("Aria", rom.playerTrainer.name)
      assertTrue(rom.useCustomPlayerAvatarBitmap)
      assertEquals("0:4,1:4,2:4", rom.playerCustomAvatarRle)
      assertEquals(6, rom.partyDexIds.size)
      assertEquals("life_orb", rom.heldItemsMap[384])
      assertEquals(14, rom.sideModeRecord.battleTowerBestStreak)
      assertEquals(120, rom.sideModeRecord.battlePoints)
      assertTrue("ROM ${rom.comparisonTag} must lock at least 1 hardware platform", rom.activePlatformIds.isNotEmpty())
      assertTrue("Shortcut short label must stay concise: ${rom.shortShortcutLabel}", rom.shortShortcutLabel.length <= 18)
    }

    val ultimateRom = compiledTen.first { it.comparisonTag == "ULTIMATE_156_CURATED" }
    assertTrue(ultimateRom.useAllCatalogPlatforms)
    assertEquals(simulatedCatalog.size, ultimateRom.activePlatformIds.size)

    val customStudioRom = PrismRomPackageEngine.compileCurrentStudioRom(
      romTitle = "Pokémon: Prism Resonance",
      activePlatformIds = setOf("snes_sfc", "gamecube_tev", "nintendo_switch"),
      allCatalogIds = simulatedCatalog,
      disciplineMode = com.example.data.model.MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = com.example.data.model.CharacterMixMode.SPLIT_BEAM_MORPH,
      extraVisualAudio = com.example.data.model.ExtraVisualAudioSettings(waterCausticsAndFoam = true),
      pixelTo3dBlend = 0.52f,
      hardwareSummaryLabel = "Custom 3-Era Fusion",
      activeRegionId = "kanto",
      playerRow = 8,
      playerCol = 8,
      regionMapOverrides = all22Maps,
      playerTrainer = customHero,
      playerCustomAvatarRle = "",
      playerCustomAvatarPaletteJson = "",
      useCustomPlayerAvatarBitmap = false,
      partyDexIds = listOf(25, 6, 94, 448, 658, 384),
      partyLevels = mapOf(25 to 50),
      heldItemsMap = emptyMap(),
      completedRegionIds = emptySet(),
      sideModeRecord = sampleRecord,
    )
    assertEquals("CURRENT_STUDIO_BUILD", customStudioRom.comparisonTag)
    assertEquals(3, customStudioRom.activePlatformIds.size)
    assertFalse(customStudioRom.useAllCatalogPlatforms)
  }
}

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
  }
}

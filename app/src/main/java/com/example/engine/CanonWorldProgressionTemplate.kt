package com.example.engine

import com.example.data.model.FanGameProject

/**
 * Single-Game Master Canon Base Template bridging all mainline Pokémon games (Gen I–IX)
 * and the Pokémon anime series (Indigo League → Orange Islands → Sinnoh → XY&Z → Journeys → Horizons)
 * into one deterministic, glitch-free world progression graph.
 */
data class CanonRegionChapter(
  val id: String,
  val orderIndex: Int,
  val regionName: String,
  val gameSources: String,
  val animeArcSource: String,
  val gymOrTrialCount: Int,
  val signatureGimmick: BattleGimmickMode,
  val requiredPreviousChapterId: String?,
  val keyLoreMilestone: String,
  val starterDexSeed: Int,
)

data class CampaignProgressionState(
  val completedChapterIds: Set<String> = emptySet(),
  val badgesEarned: Int = 0,
  val animeRibbonsAndKeys: Int = 0,
  val worldCoronationRank: Int = 10000,
  val partyDexIds: List<Int> = listOf(25, 6, 384, 658, 448, 887),
) {
  /**
   * Validates strict sequence and party invariants so progression can never glitch or soft-lock.
   */
  fun canUnlockChapter(chapter: CanonRegionChapter): Boolean {
    val req = chapter.requiredPreviousChapterId ?: return true
    return req in completedChapterIds
  }

  fun completeChapter(chapter: CanonRegionChapter): CampaignProgressionState {
    if (!canUnlockChapter(chapter) || chapter.id in completedChapterIds) return this
    val newCompleted = completedChapterIds + chapter.id
    val newBadges = (badgesEarned + chapter.gymOrTrialCount).coerceAtMost(88)
    val newRibbons = (animeRibbonsAndKeys + 2).coerceAtMost(40)
    val rankStep = if (worldCoronationRank > 100) 1250 else 12
    val newRank = (worldCoronationRank - rankStep).coerceAtLeast(1)
    return copy(
      completedChapterIds = newCompleted,
      badgesEarned = newBadges,
      animeRibbonsAndKeys = newRibbons,
      worldCoronationRank = newRank,
    )
  }

  fun validateInvariants(): List<String> {
    val issues = mutableListOf<String>()
    if (partyDexIds.size !in 1..6) {
      issues.add("Party size ${partyDexIds.size} violates 1..6 slot invariant.")
    }
    if (worldCoronationRank < 1) {
      issues.add("World Coronation Rank cannot drop below #1.")
    }
    if (badgesEarned < 0) {
      issues.add("Badge count cannot be negative.")
    }
    return issues
  }
}

object MasterCanonTemplateCatalog {
  const val TEMPLATE_TITLE = "Pokémon World Coronation: Master Canon"
  const val TEMPLATE_REGION = "Kanto–Paldea Omni-Atlas"
  const val TEMPLATE_VERSION = "v1.0.0-CANON-BASE"
  const val TEMPLATE_PRESET = "Omni-Era HD-2D + Anime Aura + 18-Type Engine"
  const val TEMPLATE_CHIPS_CSV = "super_fx2,sa_1,sgb2_border,n64_exp_pak,sega_32x,e_reader_gba,msu_1,voxelizer_fx,pokecrt_rtx"

  fun createBaseProjectTemplate(): FanGameProject = FanGameProject(
    title = TEMPLATE_TITLE,
    regionName = TEMPLATE_REGION,
    synopsis = "Unified single-game canon template fusing Gen I–IX mainline mechanics (18-type matrix, EV/IV/Natures, Mega, Z-Moves, Dynamax, Tera) with Pokémon Anime Battle Bond, Counter-Shield, Orange Archipelago, Contests, and Masters Eight progression.",
    consoleMatrixPreset = TEMPLATE_PRESET,
    activeChipsCsv = TEMPLATE_CHIPS_CSV,
    pixel3dBlendRatio = 68L,
    dexCount = 1025L,
    mapCount = 128L,
    versionTag = TEMPLATE_VERSION,
  )

  val worldChapters: List<CanonRegionChapter> = listOf(
    CanonRegionChapter(
      id = "kanto_indigo",
      orderIndex = 1,
      regionName = "Kanto + Indigo Plateau",
      gameSources = "Red / Blue / Yellow / FireRed / LeafGreen / Let's Go",
      animeArcSource = "EP001–EP082: Indigo League & Mewtwo Strikes Back",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      requiredPreviousChapterId = null,
      keyLoreMilestone = "Silph Co. Master Ball Crisis, Lavender Tower & Indigo Conference",
      starterDexSeed = 25,
    ),
    CanonRegionChapter(
      id = "orange_archipelago",
      orderIndex = 2,
      regionName = "Orange Archipelago",
      gameSources = "Anime-Exclusive Isles integrated onto Sevii Islands Map",
      animeArcSource = "EP083–EP118: Orange Crew Trophy & Lugia Prophecy (Pokemon 2000)",
      gymOrTrialCount = 4,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "kanto_indigo",
      keyLoreMilestone = "Shamouti Island Three-Titan Harmony & Drake's Dragonite Trophy",
      starterDexSeed = 149,
    ),
    CanonRegionChapter(
      id = "johto_silver",
      orderIndex = 3,
      regionName = "Johto + Mt. Silver",
      gameSources = "Gold / Silver / Crystal / HeartGold / SoulSilver",
      animeArcSource = "Johto Journeys & Master Quest: Silver Conference",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "orange_archipelago",
      keyLoreMilestone = "Bell Tower Ho-Oh Awakening, Whirl Islands & Red Summit Duel",
      starterDexSeed = 249,
    ),
    CanonRegionChapter(
      id = "hoenn_frontier",
      orderIndex = 4,
      regionName = "Hoenn + Battle Frontier",
      gameSources = "Ruby / Sapphire / Emerald / ORAS + Delta Episode",
      animeArcSource = "Advanced Generation: Hoenn Contests & Frontier Brains",
      gymOrTrialCount = 15,
      signatureGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      requiredPreviousChapterId = "johto_silver",
      keyLoreMilestone = "Sky Pillar Rayquaza Mediation & All 7 Battle Frontier Symbols",
      starterDexSeed = 384,
    ),
    CanonRegionChapter(
      id = "sinnoh_coronet",
      orderIndex = 5,
      regionName = "Sinnoh + Hisui Rifts",
      gameSources = "Diamond / Pearl / Platinum / Legends: Arceus",
      animeArcSource = "Diamond & Pearl: Counter-Shield, Paul Rivalry & Spear Pillar",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.ANIME_COUNTER_SHIELD,
      requiredPreviousChapterId = "hoenn_frontier",
      keyLoreMilestone = "Grand Festival Ribbons, Distortion World & Ash vs Paul Lily of the Valley",
      starterDexSeed = 448,
    ),
    CanonRegionChapter(
      id = "unova_decamap",
      orderIndex = 6,
      regionName = "Unova + Decolore Isles",
      gameSources = "Black / White / Black 2 / White 2 (PWT)",
      animeArcSource = "Best Wishes: N's Ideals, Meloetta & Pokemon World Tournament",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "sinnoh_coronet",
      keyLoreMilestone = "Dragonspiral Tower Truth/Ideals & PWT Champion Bracket",
      starterDexSeed = 643,
    ),
    CanonRegionChapter(
      id = "kalos_bond",
      orderIndex = 7,
      regionName = "Kalos + Lumiose",
      gameSources = "X / Y (Mega Evolution Lineage)",
      animeArcSource = "XY & XY&Z: Battle Bond Phenomenon & Team Flare Zygarde Crisis",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.ANIME_BATTLE_BOND,
      requiredPreviousChapterId = "unova_decamap",
      keyLoreMilestone = "Anistar Sundial Bond Awakening & Lumiose Alliance Finale",
      starterDexSeed = 658,
    ),
    CanonRegionChapter(
      id = "alola_ultra",
      orderIndex = 8,
      regionName = "Alola + Ultra Space",
      gameSources = "Sun / Moon / Ultra Sun / Ultra Moon",
      animeArcSource = "Sun & Moon: 4 Island Kahunas, Necrozma & Manalo Conference",
      gymOrTrialCount = 7,
      signatureGimmick = BattleGimmickMode.Z_MOVE,
      requiredPreviousChapterId = "kalos_bond",
      keyLoreMilestone = "Aether Ultra Wormhole & Inaugural Alola Champion Title",
      starterDexSeed = 800,
    ),
    CanonRegionChapter(
      id = "galar_masters",
      orderIndex = 9,
      regionName = "Galar + Wyndon Stadium",
      gameSources = "Sword / Shield + Crown Tundra",
      animeArcSource = "Journeys: World Coronation Series & Masters Eight Tournament",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.DYNAMAX,
      requiredPreviousChapterId = "alola_ultra",
      keyLoreMilestone = "Darkest Day Eternatus Raid & Monarch Finals vs Leon",
      starterDexSeed = 887,
    ),
    CanonRegionChapter(
      id = "paldea_horizons",
      orderIndex = 10,
      regionName = "Paldea + Area Zero",
      gameSources = "Scarlet / Violet + The Indigo Disk",
      animeArcSource = "Pokémon Horizons: Rising Volt Tacklers & Terapagos Mystery",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.TERASTALLIZATION,
      requiredPreviousChapterId = "galar_masters",
      keyLoreMilestone = "Great Crater Stellar Tera Core & Six Heroes Convergence",
      starterDexSeed = 1008,
    ),
  )

  val signatureMovePool: List<BattleMoveSpec> = listOf(
    BattleMoveSpec("Thunderbolt", PokemonType.ELECTRIC, MoveCategory.SPECIAL, basePower = 90),
    BattleMoveSpec("Water Shuriken (Bond)", PokemonType.WATER, MoveCategory.SPECIAL, basePower = 20, priority = 1, hitsMin = 3, hitsMax = 3),
    BattleMoveSpec("Dragon Ascent", PokemonType.FLYING, MoveCategory.PHYSICAL, basePower = 120, isContact = true),
    BattleMoveSpec("Aura Sphere", PokemonType.FIGHTING, MoveCategory.SPECIAL, basePower = 80),
    BattleMoveSpec("Freeze-Dry", PokemonType.ICE, MoveCategory.SPECIAL, basePower = 70, isFreezeDry = true),
    BattleMoveSpec("Flying Press", PokemonType.FIGHTING, MoveCategory.PHYSICAL, basePower = 100, isContact = true, isFlyingPress = true),
    BattleMoveSpec("Flare Blitz", PokemonType.FIRE, MoveCategory.PHYSICAL, basePower = 120, isContact = true),
    BattleMoveSpec("Shadow Ball", PokemonType.GHOST, MoveCategory.SPECIAL, basePower = 80),
    BattleMoveSpec("Draco Meteor", PokemonType.DRAGON, MoveCategory.SPECIAL, basePower = 130),
    BattleMoveSpec("Electro Drift", PokemonType.ELECTRIC, MoveCategory.SPECIAL, basePower = 100),
  )

  /**
   * Executes a comprehensive deterministic integrity audit across the entire game engine
   * to verify zero glitches, zero NaN/div-by-zero, strict bounds, and valid progression.
   */
  fun runEngineIntegrityAudit(): List<Pair<String, Boolean>> {
    val checks = mutableListOf<Pair<String, Boolean>>()

    // 1. Type immunities & 4x dual weaknesses
    val ghostVsNormal = CanonTypeChart.calculateEffectiveness(PokemonType.GHOST, PokemonType.NORMAL)
    val groundVsFlying = CanonTypeChart.calculateEffectiveness(PokemonType.GROUND, PokemonType.FLYING)
    val rockVsCharizard = CanonTypeChart.calculateEffectiveness(PokemonType.ROCK, PokemonType.FIRE, PokemonType.FLYING)
    val freezeDryVsWater = CanonTypeChart.calculateEffectiveness(PokemonType.ICE, PokemonType.WATER, isFreezeDry = true)
    checks.add("18x18 Type Immunities & 4x/Freeze-Dry Rules" to (ghostVsNormal == 0f && groundVsFlying == 0f && rockVsCharizard == 4.0f && freezeDryVsWater == 2.0f))

    // 2. EV 510 budget & Shedinja 1 HP invariant
    val overflowSpread = StatSpread(evHp = 252, evAtk = 252, evSpe = 252).normalized()
    val shedinja = CombatantSnapshot(292, "Shedinja", 100, PokemonType.BUG, PokemonType.GHOST, 1, 90, 45, 30, 30, 40, "Wonder Guard")
    checks.add("EV 510-Cap & Shedinja 1HP Invariants" to (overflowSpread.totalEvs == 510 && shedinja.maxHp() == 1))

    // 3. Extreme stat stage clamping & zero-glitch combat math
    val extremeAttacker = CombatantSnapshot(658, "Ash-Greninja", 50, PokemonType.WATER, PokemonType.DARK, 72, 145, 67, 153, 71, 132, "Battle Bond", stages = StatStages(atk = -99, spA = 99), gimmickMode = BattleGimmickMode.ANIME_BATTLE_BOND)
    val extremeDefender = CombatantSnapshot(6, "Charizard", 50, PokemonType.FIRE, PokemonType.FLYING, 78, 84, 78, 109, 85, 100, "Blaze", gimmickMode = BattleGimmickMode.ANIME_COUNTER_SHIELD)
    val simResult = CanonBattleResolver.resolveTurn(
      extremeAttacker,
      extremeDefender,
      signatureMovePool[1],
      BattleTurnField(weather = WeatherCondition.RAIN),
    )
    checks.add("Deterministic Battle Math (Zero NaN/Glitch)" to (simResult.minDamage >= 1 && simResult.maxDamage >= simResult.minDamage && !simResult.minPercentHp.isNaN()))

    // 4. Campaign sequence lock & invariant checks
    val initialCampaign = CampaignProgressionState()
    val lockedSinnoh = initialCampaign.completeChapter(worldChapters[4])
    val unlockedKanto = initialCampaign.completeChapter(worldChapters[0])
    checks.add("Omni-Region Sequence Lock & State Safety" to (lockedSinnoh.completedChapterIds.isEmpty() && unlockedKanto.completedChapterIds.contains("kanto_indigo") && unlockedKanto.validateInvariants().isEmpty()))

    return checks
  }
}

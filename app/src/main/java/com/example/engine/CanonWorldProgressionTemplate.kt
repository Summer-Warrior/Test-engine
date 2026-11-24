package com.example.engine

import com.example.data.model.FanGameProject

/**
 * 22 distinct regional biome palettes (12 mainline-era inspirations + 10 anime/manga/spin-off inspirations).
 */
enum class RegionBiomeStyle(val label: String) {
  TEMPERATE_MEADOW("Temperate Meadow & Grove"),
  AUTUMN_SHRINE("Autumn Maple Shrine"),
  VOLCANIC_SEA("Volcanic Coast & Sky Pillar"),
  ALPINE_SNOW("Alpine Snow & Coronet Peaks"),
  ANCIENT_HISUI("Ancient Primeval Moor"),
  NEON_METROPOLIS("Neon Skyline & Bridges"),
  GRAND_BOULEVARD("Kalos Canal Boulevard"),
  SUNLIT_ISLAND("Volcanic Sun-Ring Archipelago"),
  STADIUM_HIGHLANDS("Galar Wild Highlands"),
  LANTERN_TERRACE("Kitakami Crystal Terrace"),
  CRYSTAL_CRATER("Area Zero Prism Crater"),
  APEX_TERRARIUM("Blueberry 4-Biome Terrarium"),
  SEVII_STAR_ISLES("Sevii Seven-Star Reef & Ember Isle"),
  ORANGE_CORAL_TRIALS("Orange Tropical Coral Trial Atolls"),
  DESERT_CANYON("Orre Desert Wastes & Relic Canyon"),
  RANGER_HIGHLANDS("Fiore & Almia Ranger Uplands"),
  FEUDAL_KINGDOMS("Ransei Feudal Aurora Terraces"),
  FERRUM_ARENA("Ferrum Synergy Arena Coast"),
  LENTAL_PHOTOSPHERE("Lental Illumina Photosphere Atolls"),
  PASIO_CONVERGENCE("Pasio Artificial Convergence Ring"),
  AEOS_STADIUM("Aeos Remoat Energy Stadium Isle"),
  MYSTERY_ULTRA_RIFT("Mystery Rift & Ultra-Distortion Void"),
}

/**
 * Atmospheric lighting cycle rendered live across AutoTileMapCanvas and MultiHardwareBattleStageBackdrop.
 */
enum class TimeOfDayCycle(val id: String, val label: String) {
  DAY("day", "Daylight"),
  DUSK("dusk", "Golden Dusk"),
  NIGHT("night", "Starlit Night"),
}

/**
 * Signature inspired architectural / environmental silhouettes drawn procedurally on Landmark (10) & Sanctuary (5) tiles.
 */
enum class LandmarkSilhouette(val label: String) {
  INDIGO_SPIRE("Indigo Prism Spire"),
  PAGODA_TOWER("Tiered Bell Pagoda"),
  VOLCANIC_RIDGE("Volcanic Sky Caldera"),
  SNOW_PEAKS("Alpine Coronet Peaks"),
  ANCIENT_SHRINE("Primeval Rift Arch"),
  NEON_BRIDGE("Neon Suspension Pylon"),
  PRISM_TOWER("Lumiose Prism Tower"),
  CORAL_REEF("Coral Reef & Sun Altar"),
  ARENA_DOME("Galar/Aeos Stadium Dome"),
  LANTERN_TEMPLE("Festival Lantern Temple"),
  CRYSTAL_SPIRE("Terastal Crystal Spire"),
  DESERT_MESA("Orre Sandstone Mesa"),
  RANGER_TOWER("Ranger Styler Relay"),
  FEUDAL_CASTLE("Ransei Feudal Keep"),
  ILLUMINA_BLOOM("Lental Illumina Bloom"),
  DIMENSIONAL_RIFT("Ultra-Distortion Vortex"),
}

enum class SubZoneKind(val badge: String) {
  HUB_TOWN("HUB TOWN"),
  WILD_BIOME("WILD BIOME"),
  PRISM_SANCTUARY("SANCTUARY"),
}

data class RegionSubZone(
  val id: String,
  val name: String,
  val kind: SubZoneKind,
  val col: Int,
  val row: Int,
  val sourceTag: String,
)

enum class PoiInteractionKind {
  PROFESSOR_GUIDE,
  RIVAL_OR_CHALLENGER,
  HEALER_OR_LORE,
}

data class RegionPointOfInterest(
  val id: String,
  val name: String,
  val roleBadge: String,
  val col: Int,
  val row: Int,
  val interactionKind: PoiInteractionKind,
  val dialogue: String,
  val accentHex: String = "#00F5D4",
  val battleDexSeed: Int? = null,
  val trainerArchetypeId: String? = null,
)

/**
 * Default Master Game Project: "Pokémon: Prism Resonance" v5.0
 * 100% original clean-room multi-source fusion world spanning ALL 22 playable regions inspired by
 * mainline games, anime arcs, manga/adventures, and spin-offs—with custom 16x16 maps, distinct
 * biomes, signature landmark silhouettes, 3 regional sub-zones, 3+ Trainer/NPC POIs, and full
 * #0001-#1025 Pokémon habitat coverage.
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
  val defaultMapTiles: List<Int>,
  val encounterPoolDexIds: List<Int>,
  val defaultBgmThemeId: String,
  val harmonicShardId: String,
  val storyLogEntry: String,
  val weatherPreset: WeatherCondition = WeatherCondition.CLEAR,
  val regionAccentHex: String = "#00F5D4",
  val spawnCol: Int = 3,
  val spawnRow: Int = 13,
  val biomeStyle: RegionBiomeStyle = RegionBiomeStyle.TEMPERATE_MEADOW,
  val landmarkSilhouette: LandmarkSilhouette = LandmarkSilhouette.INDIGO_SPIRE,
  val fusionSourcesLineage: String = "Mainline Games + Anime + Manga + Spin-Offs + Bulbapedia/PokémonDB/Serebii",
  val subZones: List<RegionSubZone> = emptyList(),
  val regionalTrainerClassIds: List<String> = emptyList(),
  val connectedRegionIds: List<String> = emptyList(),
  val pointsOfInterest: List<RegionPointOfInterest> = emptyList(),
  val atlasX: Float = 0.5f,
  val atlasY: Float = 0.5f,
) {
  fun resolveBgmTheme(): BgmThemeId = BgmThemeId.fromId(defaultBgmThemeId)

  fun resolveAmbientBed(): AmbientBedType = when (biomeStyle) {
    RegionBiomeStyle.VOLCANIC_SEA,
    RegionBiomeStyle.SUNLIT_ISLAND,
    RegionBiomeStyle.APEX_TERRARIUM,
    RegionBiomeStyle.SEVII_STAR_ISLES,
    RegionBiomeStyle.ORANGE_CORAL_TRIALS,
    RegionBiomeStyle.FERRUM_ARENA,
    RegionBiomeStyle.LENTAL_PHOTOSPHERE,
    RegionBiomeStyle.PASIO_CONVERGENCE -> AmbientBedType.OCEAN_SURF

    RegionBiomeStyle.ALPINE_SNOW,
    RegionBiomeStyle.ANCIENT_HISUI -> AmbientBedType.SNOW_SHIMMER

    RegionBiomeStyle.NEON_METROPOLIS,
    RegionBiomeStyle.LANTERN_TERRACE,
    RegionBiomeStyle.CRYSTAL_CRATER,
    RegionBiomeStyle.AEOS_STADIUM,
    RegionBiomeStyle.MYSTERY_ULTRA_RIFT -> AmbientBedType.CRYSTAL_HUM

    else -> AmbientBedType.MEADOW_WIND
  }
}

data class CampaignProgressionState(
  val completedChapterIds: Set<String> = emptySet(),
  val badgesEarned: Int = 0,
  val animeRibbonsAndKeys: Int = 0,
  val worldCoronationRank: Int = 10000,
  val harmonicShardsCollected: Int = 0,
  val partyDexIds: List<Int> = listOf(25, 6, 94, 448, 658, 384),
) {
  fun canUnlockChapter(chapter: CanonRegionChapter): Boolean {
    val req = chapter.requiredPreviousChapterId ?: return true
    return req in completedChapterIds
  }

  fun completeChapter(chapter: CanonRegionChapter): CampaignProgressionState {
    if (!canUnlockChapter(chapter) || chapter.id in completedChapterIds) return this
    val newCompleted = completedChapterIds + chapter.id
    val newBadges = (badgesEarned + chapter.gymOrTrialCount).coerceAtMost(176)
    val newRibbons = (animeRibbonsAndKeys + 2).coerceAtMost(66)
    val rankStep = if (worldCoronationRank > 100) 450 else 8
    val newRank = (worldCoronationRank - rankStep).coerceAtLeast(1)
    return copy(
      completedChapterIds = newCompleted,
      badgesEarned = newBadges,
      animeRibbonsAndKeys = newRibbons,
      worldCoronationRank = newRank,
      harmonicShardsCollected = newCompleted.size,
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
  const val MAP_GRID_SIZE = 16
  const val MAP_TILE_COUNT = MAP_GRID_SIZE * MAP_GRID_SIZE
  const val TEMPLATE_TITLE = "Pokémon: Prism Resonance"
  const val TEMPLATE_REGION = "22-Region Multi-Source Fusion Nexus"
  const val TEMPLATE_VERSION = "v5.0.0-PRISM"
  const val TEMPLATE_PRESET = "Ultimate Target Style — All 156 Hardware"
  const val TEMPLATE_CHIPS_CSV = "SHOVEL_KNIGHT_CURATED|ALL_ON_RECORD"

  /**
   * 12-Tile Universal Map Legend (used consistently by World Map Editor, AutoTileMapCanvas,
   * Resource Manager, and Game Test 16x16 Overworld):
   * 0 (.) = Meadow / Town Ground (walkable)
   * 1 (g) = Tall Wild Grass (walkable, triggers wild encounter)
   * 2 (~) = Deep Water / Coast (blocked)
   * 3 (=) = Route Road / Cobblestone Path (walkable)
   * 4 (^) = Mountain / Volcanic Ridge (blocked)
   * 5 (S) = Prism Sanctuary Boss / Gym Arena (walkable, launches boss/shard trial)
   * 6 (T) = Regional Tree Canopy / Hedge (blocked, biome-styled)
   * 7 (B) = Bridge / Causeway over Water (walkable)
   * 8 (H) = Healing Lab / Prism Center Plaza (walkable, heals party to 100%)
   * 9 (w) = Regional Wild Biome Floor (walkable, secondary encounters: sand/snow/crystal/ruin)
   * 10 (L) = Landmark Building / Tower Facade (blocked, renders signature silhouette)
   * 11 (W) = Region Warp Gate / Port (walkable, quick-travel to connected regions)
   */
  val tileLegendLabels: List<Pair<Int, String>> = listOf(
    0 to "Meadow / Plaza",
    1 to "Tall Wild Grass",
    2 to "Deep Water",
    3 to "Route Road",
    4 to "Mountain Ridge",
    5 to "Prism Sanctuary",
    6 to "Tree Canopy",
    7 to "Bridge / Span",
    8 to "Healing Plaza",
    9 to "Wild Biome",
    10 to "Landmark Bldg",
    11 to "Region Warp",
  )

  internal fun parseMap16x16(rows: List<String>): List<Int> {
    require(rows.size == MAP_GRID_SIZE) { "Expected $MAP_GRID_SIZE rows, got ${rows.size}" }
    val out = ArrayList<Int>(MAP_TILE_COUNT)
    for (row in rows) {
      require(row.length == MAP_GRID_SIZE) { "Expected $MAP_GRID_SIZE cols, got ${row.length} in '$row'" }
      for (ch in row) {
        out.add(
          when (ch) {
            '.' -> 0
            'g' -> 1
            '~' -> 2
            '=' -> 3
            '^' -> 4
            'S' -> 5
            'T' -> 6
            'B' -> 7
            'H' -> 8
            'w' -> 9
            'L' -> 10
            'W' -> 11
            else -> 0
          }
        )
      }
    }
    return out
  }

  fun createBaseProjectTemplate(): FanGameProject = FanGameProject(
    title = TEMPLATE_TITLE,
    regionName = TEMPLATE_REGION,
    synopsis = "Original 22-region multi-source fusion epic 'The Harmonic Fracture': explore 22 fully drawn non-1:1 regions fusing mainline eras, anime arcs, manga/adventures, and spin-off realms with all 57 Trainer Types, all 1,025 Pokémon, signature landmark silhouettes, and Day/Dusk/Night lighting.",
    consoleMatrixPreset = TEMPLATE_PRESET,
    activeChipsCsv = TEMPLATE_CHIPS_CSV,
    pixel3dBlendRatio = 65L,
    dexCount = 1025L,
    mapCount = 22L,
    versionTag = TEMPLATE_VERSION,
  )

  val worldChapters: List<CanonRegionChapter> by lazy {
    MainlineRegionChaptersCatalog.chapters + CrossMediaRegionChaptersCatalog.chapters
  }

  fun findRegionById(regionId: String): CanonRegionChapter =
    worldChapters.firstOrNull { it.id == regionId } ?: worldChapters.first()

  val signatureMovePool: List<BattleMoveSpec> = listOf(
    BattleMoveSpec("Thunderbolt", PokemonType.ELECTRIC, MoveCategory.SPECIAL, basePower = 90),
    BattleMoveSpec("Tera Blast", PokemonType.NORMAL, MoveCategory.SPECIAL, basePower = 80, isTeraBlast = true),
    BattleMoveSpec("Water Shuriken (Bond)", PokemonType.WATER, MoveCategory.SPECIAL, basePower = 20, priority = 1, hitsMin = 3, hitsMax = 3),
    BattleMoveSpec("Earthquake", PokemonType.GROUND, MoveCategory.PHYSICAL, basePower = 100),
    BattleMoveSpec("Dragon Ascent", PokemonType.FLYING, MoveCategory.PHYSICAL, basePower = 120, isContact = true),
    BattleMoveSpec("Aura Sphere", PokemonType.FIGHTING, MoveCategory.SPECIAL, basePower = 80),
    BattleMoveSpec("Freeze-Dry", PokemonType.ICE, MoveCategory.SPECIAL, basePower = 70, isFreezeDry = true),
    BattleMoveSpec("Flying Press", PokemonType.FIGHTING, MoveCategory.PHYSICAL, basePower = 100, isContact = true, isFlyingPress = true),
    BattleMoveSpec("Flare Blitz", PokemonType.FIRE, MoveCategory.PHYSICAL, basePower = 120, isContact = true),
    BattleMoveSpec("Shadow Ball", PokemonType.GHOST, MoveCategory.SPECIAL, basePower = 80),
    BattleMoveSpec("Draco Meteor", PokemonType.DRAGON, MoveCategory.SPECIAL, basePower = 130),
    BattleMoveSpec("Electro Drift", PokemonType.ELECTRIC, MoveCategory.SPECIAL, basePower = 100),
  )

  fun runEngineIntegrityAudit(): List<Pair<String, Boolean>> {
    val checks = mutableListOf<Pair<String, Boolean>>()

    // 1. Type immunities, 4x, Freeze-Dry, Levitate, Wonder Guard
    val ghostVsNormal = CanonTypeChart.calculateEffectiveness(PokemonType.GHOST, PokemonType.NORMAL)
    val groundVsFlying = CanonTypeChart.calculateEffectiveness(PokemonType.GROUND, PokemonType.FLYING)
    val rockVsCharizard = CanonTypeChart.calculateEffectiveness(PokemonType.ROCK, PokemonType.FIRE, PokemonType.FLYING)
    val freezeDryVsWater = CanonTypeChart.calculateEffectiveness(PokemonType.ICE, PokemonType.WATER, isFreezeDry = true)
    checks.add("18x18 Type Immunities & 4x/Freeze-Dry Rules" to (ghostVsNormal == 0f && groundVsFlying == 0f && rockVsCharizard == 4.0f && freezeDryVsWater == 2.0f))

    // 2. EV 510 budget & Shedinja 1 HP + Wonder Guard invariant
    val overflowSpread = StatSpread(evHp = 252, evAtk = 252, evSpe = 252).normalized()
    val shedinja = CombatantSnapshot(292, "Shedinja", 100, PokemonType.BUG, PokemonType.GHOST, 1, 90, 45, 30, 30, 40, "Wonder Guard")
    val pikachu = CombatantSnapshot(25, "Pikachu", 50, PokemonType.ELECTRIC, null, 35, 55, 40, 50, 50, 90, "Static")
    val wonderBlocked = CanonBattleResolver.resolveTurn(pikachu, shedinja, signatureMovePool[0], BattleTurnField())
    checks.add("EV 510-Cap & Shedinja Wonder Guard Invariants" to (overflowSpread.totalEvs == 510 && shedinja.maxHp() == 1 && wonderBlocked.maxDamage == 0))

    // 3. Terastallization Tera Blast vs Regular Move & Levitate
    val teraAttacker = CombatantSnapshot(
      dexNumber = 6, name = "Charizard", level = 50,
      primaryType = PokemonType.FIRE, secondaryType = PokemonType.FLYING,
      baseHp = 78, baseAtk = 84, baseDef = 78, baseSpA = 109, baseSpD = 85, baseSpe = 100,
      abilityName = "Blaze", gimmickMode = BattleGimmickMode.TERASTALLIZATION, teraType = PokemonType.DRAGON,
    )
    val regularFire = CanonBattleResolver.resolveTurn(teraAttacker, pikachu, signatureMovePool[8], BattleTurnField())
    val teraBlast = CanonBattleResolver.resolveTurn(teraAttacker, pikachu, signatureMovePool[1], BattleTurnField())
    checks.add("Tera Preserves Move Types & Shifts Tera Blast" to (regularFire.resolvedMoveType == PokemonType.FIRE && teraBlast.resolvedMoveType == PokemonType.DRAGON))

    // 4. All 22 Playable Multi-Source Fusion 16x16 Regions + Distinct Biomes + Sub-Zones + Sequence Locks
    val initialCampaign = CampaignProgressionState()
    val lockedSinnoh = initialCampaign.completeChapter(worldChapters.first { it.id == "sinnoh" })
    val unlockedKanto = initialCampaign.completeChapter(worldChapters.first { it.id == "kanto" })
    val allMapsValid = worldChapters.all { ch ->
      ch.defaultMapTiles.size == MAP_TILE_COUNT &&
        ch.defaultMapTiles.all { it in 0..11 } &&
        ch.defaultMapTiles.contains(1) &&
        ch.defaultMapTiles.contains(3) &&
        ch.defaultMapTiles.contains(5) &&
        ch.defaultMapTiles.contains(8) &&
        ch.defaultMapTiles.contains(11) &&
        ch.subZones.size >= 3 &&
        ch.pointsOfInterest.size >= 3 &&
        UniversalGameFeelEngine.isTilePassable(ch.defaultMapTiles[ch.spawnRow * MAP_GRID_SIZE + ch.spawnCol])
    }
    val uniqueHashes = worldChapters.map { it.defaultMapTiles.hashCode() }.distinct().size
    val uniqueBiomes = worldChapters.map { it.biomeStyle }.distinct().size
    checks.add(
      "22 Full Fusion 16x16 Maps, Biomes & Sequence Locks" to (
        worldChapters.size == 22 &&
          uniqueHashes == 22 &&
          uniqueBiomes == 22 &&
          allMapsValid &&
          lockedSinnoh.completedChapterIds.isEmpty() &&
          unlockedKanto.completedChapterIds.contains("kanto") &&
          unlockedKanto.validateInvariants().isEmpty()
        )
    )

    // 5. 64-Step Clean-Room Soundtrack & Region-Matched Ambient Beds across all 22 regions
    val allThemes64 = BgmThemeId.entries.all { it.stepCount == BgmScore64.STEPS }
    val allChaptersHaveSoundtrack = worldChapters.all { ch ->
      ch.resolveBgmTheme().id == ch.defaultBgmThemeId && ch.resolveAmbientBed() != AmbientBedType.NONE
    }
    checks.add(
      "64-Step Era Soundtrack & Region Ambient Beds (22/22)" to (allThemes64 && allChaptersHaveSoundtrack)
    )

    return checks
  }
}

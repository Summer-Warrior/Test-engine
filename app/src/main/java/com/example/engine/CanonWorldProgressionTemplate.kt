package com.example.engine

import com.example.data.model.FanGameProject

enum class RegionBiomeStyle(val label: String) {
  TEMPERATE_MEADOW("Temperate Meadow & Grove"),
  TROPICAL_ATOLL("Tropical Atoll & Reef"),
  AUTUMN_SHRINE("Autumn Maple Shrine"),
  VOLCANIC_SEA("Volcanic Coast & Sky Pillar"),
  ALPINE_SNOW("Alpine Snow & Coronet Peaks"),
  ANCIENT_HISUI("Ancient Primeval Moor"),
  NEON_METROPOLIS("Neon Skyline & Bridges"),
  DESERT_CANYON("Orre Canyon & Relic Oasis"),
  GRAND_BOULEVARD("Kalos Canal Boulevard"),
  SUNLIT_ISLAND("Volcanic Sun-Ring Archipelago"),
  STADIUM_HIGHLANDS("Galar Wild Highlands"),
  LANTERN_TERRACE("Kitakami Crystal Terrace"),
  CRYSTAL_CRATER("Area Zero Prism Crater"),
  APEX_TERRARIUM("Blueberry 4-Biome Terrarium"),
}

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
)

/**
 * Default Master Game Project: "Pokémon: Prism Resonance"
 * 100% original story ("The Harmonic Fracture", Professor Lyra Vance, Rival Kael, Team Monochrome,
 * Prism Loom, 14 Harmonic Shards) spanning ALL 14 playable regions with custom source-inspired
 * 16x16 (256-tile) maps across 12 distinct terrain/landmark tiles, 3 interactive NPCs/POIs per
 * region, connected region warp gates, regional encounters, and BGM theme routing.
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
  val connectedRegionIds: List<String> = emptyList(),
  val pointsOfInterest: List<RegionPointOfInterest> = emptyList(),
)

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
    val newBadges = (badgesEarned + chapter.gymOrTrialCount).coerceAtMost(112)
    val newRibbons = (animeRibbonsAndKeys + 2).coerceAtMost(42)
    val rankStep = if (worldCoronationRank > 100) 740 else 12
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
  const val TEMPLATE_REGION = "14-Region Harmonic Nexus"
  const val TEMPLATE_VERSION = "v4.0.0-PRISM"
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
   * 10 (L) = Landmark Building / Tower Facade (blocked, readable via Interact)
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

  private fun parseMap16x16(rows: List<String>): List<Int> {
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
    synopsis = "Original 14-region epic 'The Harmonic Fracture': join Professor Lyra Vance and rival Kael to weave the Prism Loom across 14 full 16x16 playable regions, recover 14 Harmonic Shards, and stop Team Monochrome from flattening every world era into silence.",
    consoleMatrixPreset = TEMPLATE_PRESET,
    activeChipsCsv = TEMPLATE_CHIPS_CSV,
    pixel3dBlendRatio = 65L,
    dexCount = 1025L,
    mapCount = 14L,
    versionTag = TEMPLATE_VERSION,
  )

  val worldChapters: List<CanonRegionChapter> = listOf(
    // 1. KANTO — Custom Indigo & Viridian Valley inspired layout
    CanonRegionChapter(
      id = "kanto",
      orderIndex = 1,
      regionName = "1. Kanto — Indigo Resonance",
      gameSources = "8-Bit DMG / SGB2 / GBA Viridian-Indigo Lineage",
      animeArcSource = "Prologue: Professor Lyra Vance's Prism Loom Lab",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      requiredPreviousChapterId = null,
      keyLoreMilestone = "Stop Team Monochrome's static siphon at Indigo Spire and recover Harmonic Shard I",
      starterDexSeed = 25,
      spawnCol = 3,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.TEMPERATE_MEADOW,
      connectedRegionIds = listOf("sevii_orange", "johto"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^^..SS..^^^^^",
          "^TT..==SS==..TT^",
          "^T.gg=....=gg.T^",
          "TT.gg=.LL.=gg.TT",
          "T..==.====.==..T",
          "T.w=..g..g..=w.T",
          "~~~~~~B~~B~~~~~~",
          "~~~~~~B~~B~~~~~~",
          "T.gg..=..=..gg.T",
          "T.gg===..===gg.W",
          "T..===....===..T",
          "TT.=..H..H..=.TT",
          "T..=.LL..LL.=..T",
          "T..===....===..T",
          "TT.ww.====.gg.TT",
          "TTTTTTTTTTTTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(1, 4, 7, 25, 94, 133, 150),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_01_kanto",
      storyLogEntry = "Prof. Lyra Vance activates the Prism Loom Compass as Kanto's tall grass shimmers with 8-bit static.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#EF4444",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "kanto_lyra",
          name = "Prof. Lyra Vance",
          roleBadge = "PRISM LAB",
          col = 6,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Welcome to Kanto's Indigo Valley! Step onto the glowing Healing Plaza (H) anytime, cross the twin bridges north, and purify Indigo Spire (S) for Harmonic Shard I.",
          accentHex = "#00F5D4",
        ),
        RegionPointOfInterest(
          id = "kanto_kael",
          name = "Rival Kael",
          roleBadge = "RIVAL DUEL",
          col = 7,
          row = 5,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Kael: 'Bet my Charizard reaches Indigo Spire before your squad! Let's test our Mega Resonance right by the river bridge!'",
          accentHex = "#FF2A85",
          battleDexSeed = 6,
        ),
        RegionPointOfInterest(
          id = "kanto_scout",
          name = "Indigo Ranger Mina",
          roleBadge = "WARP GUIDE",
          col = 14,
          row = 9,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "The eastern Warp Gate (W) sails straight to the Sevii & Orange Archipelago and Johto once you're ready to chart new regions.",
          accentHex = "#FFD166",
        ),
      ),
    ),

    // 2. SEVII & ORANGE ARCHIPELAGO — Custom Twin Atoll & Coral Causeway layout
    CanonRegionChapter(
      id = "sevii_orange",
      orderIndex = 2,
      regionName = "2. Sevii & Orange Archipelago",
      gameSources = "GBA Archipelago Sea Routes & Tidal Atolls",
      animeArcSource = "Act I: Kael's Coastal Skiff Duel",
      gymOrTrialCount = 4,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "kanto",
      keyLoreMilestone = "Calm the Three-Current Shrine across the Emerald Atolls for Harmonic Shard II",
      starterDexSeed = 149,
      spawnCol = 3,
      spawnRow = 12,
      biomeStyle = RegionBiomeStyle.TROPICAL_ATOLL,
      connectedRegionIds = listOf("kanto", "johto"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "~~~~~~~SS~~~~~~~",
          "~~~~~wwSSww~~~~~",
          "~~~~T.g==g.T~~~~",
          "~~~~~.====.~~~~~",
          "~~~~~~~BB~~~~~~~",
          "~T.gg..BB..gg.T~",
          "~.ww==BBBB==ww.~",
          "~.=..=.LL.=..=.W",
          "~.=gg=....=gg=.~",
          "~~====~~~~====~~",
          "~~~BB~~~~~~BB~~~",
          "~T.==..H...==.T~",
          "~..===.LL.===..~",
          "~.ww.======.ww.~",
          "~~TT..gggg..TT~~",
          "~~~~~~~~~~~~~~~~",
        )
      ),
      encounterPoolDexIds = listOf(7, 130, 131, 144, 145, 146, 149),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_02_sevii",
      storyLogEntry = "Rival Kael challenges your surf formation across sunlit tidewaters glittering with GameCube caustics.",
      weatherPreset = WeatherCondition.RAIN,
      regionAccentHex = "#00B4D8",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "sevii_captain",
          name = "Captain Briney-Echo",
          roleBadge = "HARBOR MASTER",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Tidal causeways link all four coral atolls! Rest at the Harbor Healing Plaza (H) before braving the Three-Current Shrine north.",
          accentHex = "#38BDF8",
        ),
        RegionPointOfInterest(
          id = "sevii_kael",
          name = "Rival Kael",
          roleBadge = "SKIFF DUEL",
          col = 8,
          row = 6,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Kael: 'Rain boosts Water strikes by 1.5x out here! Try breaking through my Dragonite's tidal defense!'",
          accentHex = "#FF2A85",
          battleDexSeed = 149,
        ),
        RegionPointOfInterest(
          id = "sevii_lore",
          name = "Atoll Scholar Nori",
          roleBadge = "TIDE LORE",
          col = 14,
          row = 7,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Shimmering sand bars (w) hide rare sea encounters like Lapras and Gyarados. Step onto the Warp Dock (W) to sail to Johto.",
          accentHex = "#00F5D4",
        ),
      ),
    ),

    // 3. JOHTO — Custom Autumn Pagoda & Canal Ring layout
    CanonRegionChapter(
      id = "johto",
      orderIndex = 3,
      regionName = "3. Johto — Bell Tower Echo",
      gameSources = "GBC Hi-Color / NDS SoulSilver 2.5D",
      animeArcSource = "Act II: Golden Leaf Bell Chimes",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "sevii_orange",
      keyLoreMilestone = "Restore the GBC chromatic bell chimes atop Tin Spire to claim Harmonic Shard III",
      starterDexSeed = 249,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.AUTUMN_SHRINE,
      connectedRegionIds = listOf("sevii_orange", "hoenn", "kanto"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "TTTTTT.SS.TTTTTT",
          "TT.ww.=SS=.ww.TT",
          "T.gg..====..gg.T",
          "T.gg.LL==LL.gg.T",
          "T..===....===..T",
          "TT.=..~~~~..=.TT",
          "T..=.~wwww~.=..T",
          "T.===BwwwwB===.W",
          "T..=.~wwww~.=..T",
          "TT.=..~~~~..=.TT",
          "T.gg===..===gg.T",
          "T.gg..=HH=..gg.T",
          "T..LL.====.LL..T",
          "TT....====....TT",
          "TTT.ww....ww.TTT",
          "TTTTTTTTTTTTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(152, 155, 158, 196, 197, 212, 248, 249, 250),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_03_johto",
      storyLogEntry = "Autumn maple leaves swirl through Ecruteak Plaza as Team Monochrome attempts to mute the Bell Tower.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#FFD166",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "johto_sage",
          name = "Bell Sage Eiji",
          roleBadge = "SHRINE KEEPER",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Golden maple bridges surround the central Mirror Courtyard. Cross the twin bridges to ring the Tin Spire (S) for Harmonic Shard III.",
          accentHex = "#FFD166",
        ),
        RegionPointOfInterest(
          id = "johto_warden",
          name = "Monochrome Cantor",
          roleBadge = "RIFT DUEL",
          col = 8,
          row = 4,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Monochrome Cantor: 'Without color or chime, Johto falls silent! Face my Tyranitar!'",
          accentHex = "#A855F7",
          battleDexSeed = 248,
        ),
        RegionPointOfInterest(
          id = "johto_kimono",
          name = "Dancer Haruka",
          roleBadge = "HARMONY",
          col = 14,
          row = 7,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Evolution Stones in your Bag can awaken Eevee's Johto evolutions Espeon & Umbreon right in the field!",
          accentHex = "#F472B6",
        ),
      ),
    ),

    // 4. HOENN — Custom Volcanic Ridge & Emerald Sea Pillar layout
    CanonRegionChapter(
      id = "hoenn",
      orderIndex = 4,
      regionName = "4. Hoenn — Sky Pillar Currents",
      gameSources = "GBA 32-Bit Trumpet Era / Mode-7 Weather Basin",
      animeArcSource = "Act III: Brass Horizons & Frontier Trial",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      requiredPreviousChapterId = "johto",
      keyLoreMilestone = "Balance Sun, Rain, and Delta Winds at Sky Pillar Summit for Harmonic Shard IV",
      starterDexSeed = 384,
      spawnCol = 3,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.VOLCANIC_SEA,
      connectedRegionIds = listOf("johto", "sinnoh"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^~~.SS.~~^^^^",
          "^^^~..=SS=..~^^^",
          "^^.ww.====.gg.^^",
          "^..ww==..==gg..~",
          "^.^==..LL..==~~W",
          "^..=..gggg..=BB~",
          "^^.===....===~~^",
          "~~~BB======BB~~~",
          "~~~BB..ww..BB~~~",
          "T..===....===.T~",
          "T.gg.=HHHH=.gg.~",
          "T.gg.=.LL.=.ww.~",
          "TT...======...TT",
          "T..===....===..T",
          "TT.gg.wwww.gg.TT",
          "~~~~~~^^^^~~~~~~",
        )
      ),
      encounterPoolDexIds = listOf(252, 255, 258, 257, 282, 292, 376, 384),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_04_hoenn",
      storyLogEntry = "GBA brass trumpets echo above the emerald sea as Rayquaza's winds part the Monochrome storm.",
      weatherPreset = WeatherCondition.STRONG_WINDS,
      regionAccentHex = "#10B981",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "hoenn_weather",
          name = "Dr. Solis (Weather Lab)",
          roleBadge = "CLIMATE LAB",
          col = 8,
          row = 5,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Strong Winds swirl across Hoenn's Sky Pillar, neutralizing Flying weaknesses! Watch for volcanic ash meadows (w) on the west ridge.",
          accentHex = "#10B981",
        ),
        RegionPointOfInterest(
          id = "hoenn_ace",
          name = "Frontier Ace Steven-Echo",
          roleBadge = "MEGA TRIAL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Ace Challenger: 'Steel and Mega Resonance are forged under pressure—let's clash on the central sea-bridge!'",
          accentHex = "#38BDF8",
          battleDexSeed = 376,
        ),
        RegionPointOfInterest(
          id = "hoenn_healer",
          name = "Sootopolis Attendant",
          roleBadge = "HEAL BAY",
          col = 8,
          row = 11,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Step over the emerald Healing Plaza (H) to restore every party member's HP and status before ascending Sky Pillar!",
          accentHex = "#00F5D4",
        ),
      ),
    ),

    // 5. SINNOH — Custom Mt. Coronet Peaks & Snow Lake layout
    CanonRegionChapter(
      id = "sinnoh",
      orderIndex = 5,
      regionName = "5. Sinnoh — Spear Pillar Crest",
      gameSources = "NDS 2.5D Diorama / Platinum Snowscape",
      animeArcSource = "Act IV: Counter-Shield Mastery on Mt. Coronet",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.ANIME_COUNTER_SHIELD,
      requiredPreviousChapterId = "hoenn",
      keyLoreMilestone = "Anchor the Space-Time obelisks atop Mt. Coronet to recover Harmonic Shard V",
      starterDexSeed = 448,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.ALPINE_SNOW,
      connectedRegionIds = listOf("hoenn", "hisui"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^^.SSSS.^^^^^",
          "^^^^.=....=.^^^^",
          "^^^.w=.LL.=w.^^^",
          "^^.ww======ww.^^",
          "^..gg..==..gg..^",
          "T..====..====..T",
          "T.w=..~~~~..=w.T",
          "T..=.~~..~~.=..W",
          "TT.=~BB..BB~=.TT",
          "T..===....===..T",
          "T.gg.=H..H=.gg.T",
          "T.gg.=.LL.=.gg.T",
          "TT...======...TT",
          "T..w.======.w..T",
          "TTT.ww....ww.TTT",
          "^^^^^^^^^^^^^^^^",
        )
      ),
      encounterPoolDexIds = listOf(387, 390, 393, 445, 448, 483, 484, 487),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_05_sinnoh",
      storyLogEntry = "Snowfall drifts across Mt. Coronet's 2.5D stone ledges; Ice-type defenses surge in the crisp cold.",
      weatherPreset = WeatherCondition.SNOW,
      regionAccentHex = "#6366F1",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "sinnoh_scholar",
          name = "Historian Rowan-Echo",
          roleBadge = "MYTH ARCHIVE",
          col = 8,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Mt. Coronet's alpine snowfall fortifies Ice-types by 1.5x Defense. Activate Counter-Shield in battle to punish physical rushers!",
          accentHex = "#6366F1",
        ),
        RegionPointOfInterest(
          id = "sinnoh_champion",
          name = "Champion Echo",
          roleBadge = "CORONET DUEL",
          col = 7,
          row = 4,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Champion Echo: 'Show me the bond you've built across five regions—my Garchomp awaits atop the ridge!'",
          accentHex = "#FF2A85",
          battleDexSeed = 445,
        ),
        RegionPointOfInterest(
          id = "sinnoh_rift",
          name = "Chrono Scout Rei",
          roleBadge = "HISUI WARP",
          col = 14,
          row = 7,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "A shimmering Space-Time Warp Gate (W) east of the glacial lake opens directly into Ancient Hisui!",
          accentHex = "#F59E0B",
        ),
      ),
    ),

    // 6. HISUI — Custom Primeval Moorland & Cedar River layout
    CanonRegionChapter(
      id = "hisui",
      orderIndex = 6,
      regionName = "6. Hisui Rift — Ancient Epoch",
      gameSources = "Switch Watercolor Cel-3D Space-Time Rift",
      animeArcSource = "Act V: Through the Chrono-Loom Tear",
      gymOrTrialCount = 5,
      signatureGimmick = BattleGimmickMode.ANIME_COUNTER_SHIELD,
      requiredPreviousChapterId = "sinnoh",
      keyLoreMilestone = "Soothe the Ancient Noble Rift Guardian and weave Harmonic Shard VI",
      starterDexSeed = 900,
      spawnCol = 3,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.ANCIENT_HISUI,
      connectedRegionIds = listOf("sinnoh", "unova"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^TT.SS.TT^^^^",
          "^^^T.w=SS=w.T^^^",
          "^^.gg.====.gg.^^",
          "^T.gg..==..gg.T^",
          "T..ww.====.ww..T",
          "T.===.~~~~.===.T",
          "T.=..~....~..=.W",
          "T.=ggB.LL.Bgg=.T",
          "T.=..~....~..=.T",
          "TT===.~~~~.===TT",
          "T..ww..==..ww..T",
          "T.gg..=HH=..gg.T",
          "T..LL.====.LL..T",
          "TT.===....===.TT",
          "TTT.ww.gg.ww.TTT",
          "TTTTTT^^^^TTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(157, 503, 724, 900, 901, 905, 493),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_06_hisui",
      storyLogEntry = "A prism rift opens into ancient Hisui's wild meadows, where noble silhouettes glow with amber rims.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#F59E0B",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "hisui_warden",
          name = "Survey Warden Akari",
          roleBadge = "BASE CAMP",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Ancient Hisui's wild moors (w) and cedar groves haven't seen modern cities yet. Soothe the Noble Guardian at the northern Shrine (S)!",
          accentHex = "#F59E0B",
        ),
        RegionPointOfInterest(
          id = "hisui_noble",
          name = "Noble Warden Lian",
          roleBadge = "NOBLE TRIAL",
          col = 7,
          row = 4,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Warden: 'Kleavor's stone axes split boulders—prove your Counter-Shield timing in an ancient duel!'",
          accentHex = "#EF4444",
          battleDexSeed = 900,
        ),
        RegionPointOfInterest(
          id = "hisui_gate",
          name = "Chrono Weaver",
          roleBadge = "RIFT GATE",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "The Rift Gate (W) threads forward across centuries directly to Unova's neon skyline.",
          accentHex = "#00F5D4",
        ),
      ),
    ),

    // 7. UNOVA — Custom Twin Sky-Bridges & Neon Plaza layout
    CanonRegionChapter(
      id = "unova",
      orderIndex = 7,
      regionName = "7. Unova — Dragonspiral Neon",
      gameSources = "DSi 60fps Dynamic Sprite Camera & Skylines",
      animeArcSource = "Act VI: Truth vs Monochrome Silence",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.NONE,
      requiredPreviousChapterId = "hisui",
      keyLoreMilestone = "Rekindle the Twin Dragons' harmonic resonance at Dragonspiral Tower for Shard VII",
      starterDexSeed = 643,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.NEON_METROPOLIS,
      connectedRegionIds = listOf("hisui", "orre"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "LLLL..SSSS..LLLL",
          "LL.==.=SS=.==.LL",
          "L..=gg====gg=..L",
          "T..=ww....ww=..T",
          "T.=====..=====.T",
          "~~~~BB~~~~BB~~~~",
          "~~~~BB~~~~BB~~~~",
          "T.=====..=====.W",
          "T.gg..=LL=..gg.T",
          "T.gg..====..gg.T",
          "L..===HHHH===..L",
          "LL.=..wwww..=.LL",
          "LL.=LL....LL=.LL",
          "L..==========..L",
          "TT.gg..==..gg.TT",
          "LLLLLL====LLLLLL",
        )
      ),
      encounterPoolDexIds = listOf(495, 498, 501, 571, 609, 635, 637, 643, 644),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_07_unova",
      storyLogEntry = "Castelia's steel bridges hum with energetic basslines while Kael tracks a Team Monochrome relay.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#F8FAFC",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "unova_lyra",
          name = "Prof. Lyra Vance",
          roleBadge = "METRO RELAY",
          col = 7,
          row = 11,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Unova's twin steel bridges carry 60fps DSi sprite energy straight into Dragonspiral Sanctuary (S). Watch out for Monochrome drones!",
          accentHex = "#00F5D4",
        ),
        RegionPointOfInterest(
          id = "unova_kael",
          name = "Rival Kael",
          roleBadge = "SKYLINE DUEL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Kael: 'I cornered a Monochrome relay on the Sky-Bridge—spar with my Hydreigon to charge our Prism gauge!'",
          accentHex = "#FF2A85",
          battleDexSeed = 635,
        ),
        RegionPointOfInterest(
          id = "unova_metro",
          name = "Subway Conductor",
          roleBadge = "WARP LINE",
          col = 14,
          row = 7,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Express Warp Gate (W) connects Unova Harbor directly to Orre's desert canyons!",
          accentHex = "#FFD166",
        ),
      ),
    ),

    // 8. ORRE WASTES — Custom Red Canyon & Relic Oasis layout
    CanonRegionChapter(
      id = "orre",
      orderIndex = 8,
      regionName = "8. Orre Wastes — Relic Sanctuary",
      gameSources = "GameCube Flipper TEV Shadow-Aura Desert",
      animeArcSource = "Act VII: Purifying the Shadow-Gray Vortex",
      gymOrTrialCount = 4,
      signatureGimmick = BattleGimmickMode.ANIME_BATTLE_BOND,
      requiredPreviousChapterId = "unova",
      keyLoreMilestone = "Cleanse the Relic Forest Sanctuary and claim Harmonic Shard VIII",
      starterDexSeed = 197,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.DESERT_CANYON,
      connectedRegionIds = listOf("unova", "kalos"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^^T.SS.T^^^^^",
          "^^^^T.=SS=.T^^^^",
          "^^^.gg====gg.^^^",
          "^^.ww..==..ww.^^",
          "^.===.~~~~.===.^",
          "^.w=.~~..~~.=w.^",
          "^..=~BB..BB~=..W",
          "^^.===....===.^^",
          "^..ww.=LL=.ww..^",
          "^.gg..====..gg.^",
          "^^.=..HHHH..=.^^",
          "^^^=.LL..LL.=^^^",
          "^^.===....===.^^",
          "^.www======www.^",
          "^^^..ww..ww..^^^",
          "^^^^^^^^^^^^^^^^",
        )
      ),
      encounterPoolDexIds = listOf(196, 197, 248, 330, 376, 249, 250),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_08_orre",
      storyLogEntry = "Desert winds whip across Orre's canyons; Rock-type Sp.Def rises by 1.5x beneath the swirling Sandstorm.",
      weatherPreset = WeatherCondition.SANDSTORM,
      regionAccentHex = "#A855F7",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "orre_keeper",
          name = "Relic Keeper Eagun-Echo",
          roleBadge = "OASIS CAMP",
          col = 7,
          row = 11,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "The green Relic Oasis heals weary partners amidst Orre's red canyons. Use Battle Bond aura to purify the northern Shadow Vortex (S)!",
          accentHex = "#A855F7",
        ),
        RegionPointOfInterest(
          id = "orre_cipher",
          name = "Monochrome Admin",
          roleBadge = "SHADOW DUEL",
          col = 7,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Monochrome Admin: 'Our Shadow TEV siphon drains every vivid palette! Face Shadow-aura Umbreon!'",
          accentHex = "#FF2A85",
          battleDexSeed = 197,
        ),
        RegionPointOfInterest(
          id = "orre_scout",
          name = "Outskirt Mechanic",
          roleBadge = "WARP SKIFF",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Orre's desert Sandstorm grants +1.5x Sp.Def to Rock-types. Take the eastern Warp Gate (W) when bound for Kalos.",
          accentHex = "#FFD166",
        ),
      ),
    ),

    // 9. KALOS — Custom Radial Canal Boulevard & Prism Spire layout
    CanonRegionChapter(
      id = "kalos",
      orderIndex = 9,
      regionName = "9. Kalos — Prism Tower Spire",
      gameSources = "3DS PICA200 Toon-Ink Outlines & Mega Resonance",
      animeArcSource = "Act VIII: Lumiose Sundial Convergence",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.ANIME_BATTLE_BOND,
      requiredPreviousChapterId = "orre",
      keyLoreMilestone = "Synchronize the Anistar Sundial and Lumiose Spire to earn Harmonic Shard IX",
      starterDexSeed = 658,
      spawnCol = 7,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.GRAND_BOULEVARD,
      connectedRegionIds = listOf("orre", "alola"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "TTTT..====..TTTT",
          "TT.gg.=SS=.gg.TT",
          "T..ww.=SS=.ww..T",
          "T.====~~~~====.T",
          "T.=..~BBBB~..=.T",
          "=.=LL~....~LL=.=",
          "=.=..B.gg.B..=.W",
          "=.====.gg.====.=",
          "=.=..B.ww.B..=.=",
          "=.=LL~....~LL=.=",
          "T.=..~BBBB~..=.T",
          "T.====~~~~====.T",
          "T.gg..=HH=..gg.T",
          "TT....====....TT",
          "TTT.ww====ww.TTT",
          "TTTTTTTTTTTTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(650, 653, 656, 658, 681, 700, 716, 717, 718),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_09_kalos",
      storyLogEntry = "Kalos boulevards shine under crisp PICA200 ink outlines as Battle Bond aura lights the plaza.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#EC4899",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "kalos_inventor",
          name = "Prism Architect Clemont-Echo",
          roleBadge = "SPIRE TECH",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Four stone canal bridges converge on Kalos's manicured inner garden! Follow the northern avenue straight to Prism Tower (S).",
          accentHex = "#EC4899",
        ),
        RegionPointOfInterest(
          id = "kalos_ace",
          name = "Bond Master Alain-Echo",
          roleBadge = "BOND DUEL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Bond Master: 'At 75%+ Bond Gauge, Battle Bond surges move power by 1.30x! Test your Greninja against mine!'",
          accentHex = "#00B4D8",
          battleDexSeed = 658,
        ),
        RegionPointOfInterest(
          id = "kalos_cafe",
          name = "Boulevard Concierge",
          roleBadge = "ALOLA GATE",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Our eastern Warp Gate (W) beams you directly to Alola's sunlit archipelago.",
          accentHex = "#FFD166",
        ),
      ),
    ),

    // 10. ALOLA — Custom Volcanic Sun-Ring Crescent Island layout
    CanonRegionChapter(
      id = "alola",
      orderIndex = 10,
      regionName = "10. Alola — Lanakila Sun-Ring",
      gameSources = "New 3DS Tropical Rim-Light & Z-Power Islands",
      animeArcSource = "Act IX: Four-Island Kahuna Harmony",
      gymOrTrialCount = 7,
      signatureGimmick = BattleGimmickMode.Z_MOVE,
      requiredPreviousChapterId = "kalos",
      keyLoreMilestone = "Channel the Four Island Guardians' Z-Light to unlock Harmonic Shard X",
      starterDexSeed = 778,
      spawnCol = 4,
      spawnRow = 12,
      biomeStyle = RegionBiomeStyle.SUNLIT_ISLAND,
      connectedRegionIds = listOf("kalos", "galar"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "~~~~~~^SS^~~~~~~",
          "~~~~^^=SS=^^~~~~",
          "~~~T.g====g.T~~~",
          "~~.ww==..==ww.~~",
          "~~.=..~BB~..=.~~",
          "~T.=gg~~~~gg=.T~",
          "~.===.BBBB.===.W",
          "~.=ww.=LL=.ww=.~",
          "~.=..======..=.~",
          "~~=gg..==..gg=~~",
          "~~.===HHHH===.~~",
          "~~T.=..LL..=.T~~",
          "~~~.========.~~~",
          "~~~~.ww..ww.~~~~",
          "~~~~~TTggTT~~~~~",
          "~~~~~~~~~~~~~~~~",
        )
      ),
      encounterPoolDexIds = listOf(722, 725, 728, 778, 791, 792, 800),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_10_alola",
      storyLogEntry = "Tropical Harsh Sunlight pours over golden beaches, amplifying Fire moves and Z-Crystal radiance.",
      weatherPreset = WeatherCondition.HARSH_SUNLIGHT,
      regionAccentHex = "#FACC15",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "alola_kahuna",
          name = "Island Kahuna Hala-Echo",
          roleBadge = "Z-TRIAL",
          col = 7,
          row = 11,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Alola! Harsh Sunlight boosts Fire moves by 1.5x. Trigger Z-Power Burst (1.75x) atop Lanakila Sun-Ring (S) to earn Shard X!",
          accentHex = "#FACC15",
        ),
        RegionPointOfInterest(
          id = "alola_challenger",
          name = "Trial Captain Kiawe-Echo",
          roleBadge = "Z-DUEL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Trial Captain: 'Mimikyu's Disguise and sunlit radiance are no joke—show me your Z-Sync!'",
          accentHex = "#FF5722",
          battleDexSeed = 778,
        ),
        RegionPointOfInterest(
          id = "alola_ferry",
          name = "Mantine Surf Guide",
          roleBadge = "GALAR WARP",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Catch the eastern Warp Current (W) straight to Galar's Hammerlocke Highlands!",
          accentHex = "#00B4D8",
        ),
      ),
    ),

    // 11. GALAR — Custom Wild Area & Castle Viaduct layout
    CanonRegionChapter(
      id = "galar",
      orderIndex = 11,
      regionName = "11. Galar — Hammerlocke Arena",
      gameSources = "Switch Stadium Spotlights & Wild Area",
      animeArcSource = "Act X: Monarch Stadium Showdown",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.DYNAMAX,
      requiredPreviousChapterId = "alola",
      keyLoreMilestone = "Defuse Team Monochrome's Power-Spot dampener at Hammerlocke for Harmonic Shard XI",
      starterDexSeed = 887,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.STADIUM_HIGHLANDS,
      connectedRegionIds = listOf("alola", "kitakami"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^^.SSSS.^^^^^",
          "^^^LL.=SS=.LL^^^",
          "^^.===....===.^^",
          "^.gg.=wwww=.gg.^",
          "^.gg.==..==.gg.^",
          "T..====..====..T",
          "~~~BB~~==~~BB~~~",
          "~~~BB..LL..BB~~W",
          "T..===....===..T",
          "T.ww.=gggg=.ww.T",
          "T.gg.=HHHH=.gg.T",
          "TT...=....=...TT",
          "T.LL.======.LL.T",
          "T....======....T",
          "TT.ww.gggg.ww.TT",
          "TTTTTT^^^^TTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(810, 813, 816, 823, 887, 888, 889, 890),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_11_galar",
      storyLogEntry = "Stadium spotlights sweep across Hammerlocke as Dynamax energy surges cleanly without HP glitches.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#FF2A85",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "galar_commentator",
          name = "Stadium Director Rose-Echo",
          roleBadge = "ARENA DESK",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Dynamax doubles Max HP while preserving your current HP percentage proportionally—head north over the moat bridges to Hammerlocke Arena (S)!",
          accentHex = "#FF2A85",
        ),
        RegionPointOfInterest(
          id = "galar_monarch",
          name = "Monarch Ace Leon-Echo",
          roleBadge = "MONARCH DUEL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Monarch Ace: 'Let's have a champion-time clash! My Dragapult flies faster than a 16-bit blast processor!'",
          accentHex = "#2DD4BF",
          battleDexSeed = 887,
        ),
        RegionPointOfInterest(
          id = "galar_taxi",
          name = "Corviknight Warp Pilot",
          roleBadge = "KITAKAMI WARP",
          col = 14,
          row = 7,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Step onto the eastern Warp Pad (W) for a scenic flight to Kitakami's Lantern Terraces.",
          accentHex = "#FFD166",
        ),
      ),
    ),

    // 12. KITAKAMI — Custom Autumn Terraces & Crystal Pool layout
    CanonRegionChapter(
      id = "kitakami",
      orderIndex = 12,
      regionName = "12. Kitakami — Crystal Pool Valley",
      gameSources = "Switch Lantern-Lit Autumn Terraces",
      animeArcSource = "Act XI: Festival of Masks & Shimmering Pools",
      gymOrTrialCount = 4,
      signatureGimmick = BattleGimmickMode.TERASTALLIZATION,
      requiredPreviousChapterId = "galar",
      keyLoreMilestone = "Illuminate the Crystal Pool terraces and awaken Harmonic Shard XII",
      starterDexSeed = 1017,
      spawnCol = 4,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.LANTERN_TERRACE,
      connectedRegionIds = listOf("galar", "paldea"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "TTTTT~~SS~~TTTTT",
          "TTT.w~=SS=~w.TTT",
          "TT.gg~====~gg.TT",
          "T..===BBBB===..T",
          "T.ww.~~~~~~.ww.T",
          "T.=..~wwww~..=.T",
          "T.=ggB....Bgg=.W",
          "T.=..~.LL.~..=.T",
          "TT====~~~~====TT",
          "T.gg..====..gg.T",
          "T.gg.=HHHH=.gg.T",
          "T..LL=....=LL..T",
          "TT...======...TT",
          "T..===....===..T",
          "TTT.ww.gg.ww.TTT",
          "TTTTTTTTTTTTTTTT",
        )
      ),
      encounterPoolDexIds = listOf(38, 130, 149, 1017, 1025),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_12_kitakami",
      storyLogEntry = "Lantern reflections ripple across Kitakami's Crystal Pool while Ogerpon guards the twelfth shard.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#2DD4BF",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "kitakami_caretaker",
          name = "Mask Artisan Yukito",
          roleBadge = "FESTIVAL HALL",
          col = 7,
          row = 11,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Lantern bridges span the Crystal Pool up to Oni Crest (S). Terastallize to double STAB to 2.0x while keeping your original moves' types!",
          accentHex = "#2DD4BF",
        ),
        RegionPointOfInterest(
          id = "kitakami_rival",
          name = "Rival Kael",
          roleBadge = "MASK DUEL",
          col = 8,
          row = 6,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Kael: 'Ogerpon's masks shimmer like crystal pixels—let's spar beside the Crystal Pool!'",
          accentHex = "#FF2A85",
          battleDexSeed = 1017,
        ),
        RegionPointOfInterest(
          id = "kitakami_gate",
          name = "Mossui Guide",
          roleBadge = "PALDEA WARP",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "The eastern Warp Gate (W) descends straight toward Paldea's Area Zero Prism Core.",
          accentHex = "#38BDF8",
        ),
      ),
    ),

    // 13. PALDEA — Custom Concentric Crater Rim & Crystal Core layout
    CanonRegionChapter(
      id = "paldea",
      orderIndex = 13,
      regionName = "13. Paldea — Area Zero Prism Core",
      gameSources = "Switch Open-World PBR + Terastal Crystal Cavern",
      animeArcSource = "Act XII: Descent into the Crystalline Crater",
      gymOrTrialCount = 8,
      signatureGimmick = BattleGimmickMode.TERASTALLIZATION,
      requiredPreviousChapterId = "kitakami",
      keyLoreMilestone = "Stabilize the Area Zero Prism Reactor and collect Harmonic Shard XIII",
      starterDexSeed = 1008,
      spawnCol = 7,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.CRYSTAL_CRATER,
      connectedRegionIds = listOf("kitakami", "blueberry_summit"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^^w.SS.w^^^^^",
          "^^^^ww=SS=ww^^^^",
          "^^^.=..==..=.^^^",
          "^^.g=wwwwww=g.^^",
          "^.gg==~~~~==gg.^",
          "^.ww=~BBBB~=ww.^",
          "^.=..B.LL.B..=.W",
          "^.=ggB....Bgg=.^",
          "^.=..~BBBB~..=.^",
          "^.ww==~~~~==ww.^",
          "^^.gg.=HH=.gg.^^",
          "^^^.=LL..LL=.^^^",
          "^^^.========.^^^",
          "^^^^.ww==ww.^^^^",
          "^^^^^..gg..^^^^^",
          "^^^^^^^^^^^^^^^^",
        )
      ),
      encounterPoolDexIds = listOf(906, 909, 912, 937, 959, 983, 987, 992, 1007, 1008),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_13_paldea",
      storyLogEntry = "Tera crystals refract every console palette in Area Zero; Tera Blast adapts cleanly to your Tera Type.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#38BDF8",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "paldea_lyra",
          name = "Prof. Lyra Vance",
          roleBadge = "ZERO LAB",
          col = 7,
          row = 11,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "Area Zero's crystalline floor (w) resonates with every console era! Stabilize the Prism Reactor (S) at the crater's northern shelf.",
          accentHex = "#38BDF8",
        ),
        RegionPointOfInterest(
          id = "paldea_paradox",
          name = "Paradox Guardian",
          roleBadge = "TERA TRIAL",
          col = 8,
          row = 7,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Paradox Resonance: 'Miraidon's Hadron Engine pulses at 60fps—channel your Tera Blast!'",
          accentHex = "#A855F7",
          battleDexSeed = 1008,
        ),
        RegionPointOfInterest(
          id = "paldea_gate",
          name = "Crater Lift Operator",
          roleBadge = "SUMMIT WARP",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "The eastern Warp Gate (W) ascends to the final 14th region: Blueberry Dome & Convergence Summit!",
          accentHex = "#00F5D4",
        ),
      ),
    ),

    // 14. BLUEBERRY DOME & CONVERGENCE SUMMIT — Custom 4-Biome Terrarium Finale
    CanonRegionChapter(
      id = "blueberry_summit",
      orderIndex = 14,
      regionName = "14. Blueberry Dome & Convergence Summit",
      gameSources = "Switch 2 / Omni-Hardware Apex Convergence Finale",
      animeArcSource = "Finale: Team Monochrome Zenith & Prism Loom Restoration",
      gymOrTrialCount = 4,
      signatureGimmick = BattleGimmickMode.TERASTALLIZATION,
      requiredPreviousChapterId = "paldea",
      keyLoreMilestone = "Unite all 14 Harmonic Shards at Convergence Summit and restore full multi-era color & sound!",
      starterDexSeed = 1024,
      spawnCol = 7,
      spawnRow = 13,
      biomeStyle = RegionBiomeStyle.APEX_TERRARIUM,
      connectedRegionIds = listOf("paldea", "kanto"),
      defaultMapTiles = parseMap16x16(
        listOf(
          "^^^^~.SSSS.~TTTT",
          "^^^~..=SS=..~TTT",
          "^^.gg.====.gg.TT",
          "~.ww===..===ww.T",
          "~~.=..~BB~..=.TT",
          "~~.=ggBBBBgg=.T~",
          "~~~===....===~~W",
          "==BBBB.LL.BBBB==",
          "~~~===....===~~~",
          "^..=wwBBBBww=..^",
          "^^.=..~BB~..=.^^",
          "^^.gg.=HH=.gg.^^",
          "^^^.LL====LL.^^^",
          "^^^^..====..^^^^",
          "^^^^^.wwww.^^^^^",
          "^^^^^^^^^^^^^^^^",
        )
      ),
      encounterPoolDexIds = listOf(1018, 1024, 1007, 1008, 384, 150, 493),
      defaultBgmThemeId = "bgm_champion_convergence",
      harmonicShardId = "shard_14_blueberry",
      storyLogEntry = "All four biomes of the Blueberry Dome fuse at the Convergence Summit for the grand Prism Loom finale!",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#00F5D4",
      pointsOfInterest = listOf(
        RegionPointOfInterest(
          id = "blueberry_lyra",
          name = "Prof. Lyra Vance",
          roleBadge = "PRISM LOOM",
          col = 7,
          row = 12,
          interactionKind = PoiInteractionKind.PROFESSOR_GUIDE,
          dialogue = "All 156 hardware eras converge inside the Blueberry Terrarium! Unite the 14th Harmonic Shard at the northern Apex Sanctuary (S)!",
          accentHex = "#00F5D4",
        ),
        RegionPointOfInterest(
          id = "blueberry_zenith",
          name = "Monochrome Zenith Vex",
          roleBadge = "FINAL BOSS",
          col = 8,
          row = 8,
          interactionKind = PoiInteractionKind.RIVAL_OR_CHALLENGER,
          dialogue = "Commander Vex: 'One muted palette shall rule all 156 machines—unless your Terapagos & Prism Loom break my silence!'",
          accentHex = "#FF2A85",
          battleDexSeed = 1024,
        ),
        RegionPointOfInterest(
          id = "blueberry_warp",
          name = "Convergence Navigator",
          roleBadge = "WORLD WARP",
          col = 14,
          row = 6,
          interactionKind = PoiInteractionKind.HEALER_OR_LORE,
          dialogue = "Step onto the Convergence Warp Gate (W) to travel instantly back to Kanto, Paldea, or any of the 14 regions!",
          accentHex = "#FFD166",
        ),
      ),
    ),
  )

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

    // 4. All 14 Playable 16x16 Regions + Landmarks + Sequence Lock
    val initialCampaign = CampaignProgressionState()
    val lockedSinnoh = initialCampaign.completeChapter(worldChapters[4])
    val unlockedKanto = initialCampaign.completeChapter(worldChapters[0])
    val allMapsValid = worldChapters.all { ch ->
      ch.defaultMapTiles.size == MAP_TILE_COUNT &&
        ch.defaultMapTiles.all { it in 0..11 } &&
        ch.defaultMapTiles.contains(1) &&
        ch.defaultMapTiles.contains(5) &&
        ch.defaultMapTiles.contains(8) &&
        ch.defaultMapTiles.contains(11) &&
        UniversalGameFeelEngine.isTilePassable(ch.defaultMapTiles[ch.spawnRow * MAP_GRID_SIZE + ch.spawnCol])
    }
    checks.add("14 Full 16x16 Custom Maps & Sequence Locks" to (worldChapters.size == 14 && allMapsValid && lockedSinnoh.completedChapterIds.isEmpty() && unlockedKanto.completedChapterIds.contains("kanto") && unlockedKanto.validateInvariants().isEmpty()))

    return checks
  }
}

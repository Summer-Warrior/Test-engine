package com.example.engine

import com.example.data.model.FanGameProject

/**
 * Default Master Game Project: "Pokémon: Prism Resonance"
 * 100% original story ("The Harmonic Fracture", Professor Lyra Vance, Rival Kael, Team Monochrome,
 * Prism Loom, 14 Harmonic Shards) spanning ALL 14 playable regions with editable 8x8 sector maps,
 * regional wild encounters, BGM theme routing, and deterministic progression invariants.
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
  const val TEMPLATE_TITLE = "Pokémon: Prism Resonance"
  const val TEMPLATE_REGION = "14-Region Harmonic Nexus"
  const val TEMPLATE_VERSION = "v3.0.0-PRISM"
  const val TEMPLATE_PRESET = "Shovel Knight+ Omni-Era Fusion"
  const val TEMPLATE_CHIPS_CSV = "SHOVEL_KNIGHT_CURATED|super_fx2,sa_1,sgb2_border,n64_exp_pak,sega_32x,msu_1,voxelizer_fx,pokecrt_rtx"

  fun createBaseProjectTemplate(): FanGameProject = FanGameProject(
    title = TEMPLATE_TITLE,
    regionName = TEMPLATE_REGION,
    synopsis = "Original 14-region epic 'The Harmonic Fracture': join Professor Lyra Vance and rival Kael to weave the Prism Loom across 14 playable regions, recover 14 Harmonic Shards, and stop Team Monochrome from flattening every world era into silence.",
    consoleMatrixPreset = TEMPLATE_PRESET,
    activeChipsCsv = TEMPLATE_CHIPS_CSV,
    pixel3dBlendRatio = 65L,
    dexCount = 1025L,
    mapCount = 14L,
    versionTag = TEMPLATE_VERSION,
  )

  // Tile Legend: 0=Grass/Plains, 1=Tall Grass (Encounters), 2=Water/Coast, 3=Road/Path, 4=Volcanic/Rift, 5=Prism Sanctuary/Arena
  val worldChapters: List<CanonRegionChapter> = listOf(
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
      defaultMapTiles = listOf(
        5, 5, 3, 0, 0, 1, 1, 2,
        5, 0, 3, 0, 1, 1, 2, 2,
        0, 0, 3, 3, 3, 0, 0, 2,
        1, 1, 0, 0, 3, 0, 1, 1,
        1, 1, 0, 3, 3, 0, 1, 1,
        0, 0, 3, 3, 0, 0, 0, 0,
        0, 3, 3, 0, 0, 4, 4, 0,
        3, 3, 0, 0, 0, 4, 5, 0,
      ),
      encounterPoolDexIds = listOf(1, 4, 7, 25, 94, 133, 150),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_01_kanto",
      storyLogEntry = "Prof. Lyra Vance activates the Prism Loom Compass as Kanto's tall grass shimmers with 8-bit static.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#EF4444",
    ),
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
      defaultMapTiles = listOf(
        2, 2, 2, 5, 5, 2, 2, 2,
        2, 2, 0, 3, 0, 2, 1, 2,
        2, 1, 1, 3, 0, 0, 1, 2,
        2, 0, 3, 3, 3, 3, 0, 2,
        2, 0, 3, 2, 2, 3, 1, 2,
        2, 1, 3, 3, 0, 3, 0, 2,
        2, 2, 0, 0, 1, 3, 2, 2,
        2, 2, 2, 2, 2, 3, 2, 2,
      ),
      encounterPoolDexIds = listOf(7, 130, 131, 144, 145, 146, 149),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_02_sevii",
      storyLogEntry = "Rival Kael challenges your surf formation across sunlit tidewaters glittering with GameCube caustics.",
      weatherPreset = WeatherCondition.RAIN,
      regionAccentHex = "#00B4D8",
    ),
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
      defaultMapTiles = listOf(
        0, 5, 5, 5, 0, 1, 1, 0,
        0, 0, 3, 0, 0, 1, 1, 0,
        1, 1, 3, 3, 3, 0, 2, 2,
        1, 0, 0, 0, 3, 0, 2, 2,
        0, 0, 3, 3, 3, 0, 0, 0,
        0, 1, 3, 0, 1, 1, 0, 0,
        0, 1, 3, 3, 3, 0, 4, 0,
        0, 0, 0, 0, 3, 3, 0, 0,
      ),
      encounterPoolDexIds = listOf(152, 155, 158, 196, 197, 212, 248, 249, 250),
      defaultBgmThemeId = "bgm_kanto_johto_route",
      harmonicShardId = "shard_03_johto",
      storyLogEntry = "Autumn maple leaves swirl through Ecruteak Plaza as Team Monochrome attempts to mute the Bell Tower.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#FFD166",
    ),
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
      defaultMapTiles = listOf(
        2, 2, 0, 5, 5, 0, 2, 2,
        2, 1, 0, 3, 3, 0, 1, 2,
        0, 1, 1, 3, 0, 1, 1, 0,
        0, 0, 3, 3, 3, 3, 0, 0,
        4, 0, 3, 2, 2, 3, 0, 4,
        4, 4, 3, 3, 3, 3, 4, 4,
        0, 1, 0, 3, 0, 1, 0, 0,
        2, 2, 3, 3, 3, 2, 2, 2,
      ),
      encounterPoolDexIds = listOf(252, 255, 258, 257, 282, 292, 376, 384),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_04_hoenn",
      storyLogEntry = "GBA brass trumpets echo above the emerald sea as Rayquaza's winds part the Monochrome storm.",
      weatherPreset = WeatherCondition.STRONG_WINDS,
      regionAccentHex = "#10B981",
    ),
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
      defaultMapTiles = listOf(
        4, 5, 5, 5, 5, 5, 4, 4,
        0, 0, 3, 3, 3, 0, 0, 4,
        1, 0, 3, 0, 3, 0, 1, 0,
        1, 1, 3, 0, 3, 1, 1, 0,
        0, 0, 3, 3, 3, 0, 2, 2,
        0, 1, 0, 3, 0, 0, 2, 2,
        0, 1, 1, 3, 3, 0, 0, 0,
        0, 0, 0, 0, 3, 3, 0, 0,
      ),
      encounterPoolDexIds = listOf(387, 390, 393, 445, 448, 483, 484, 487),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_05_sinnoh",
      storyLogEntry = "Snowfall drifts across Mt. Coronet's 2.5D stone ledges; Ice-type defenses surge in the crisp cold.",
      weatherPreset = WeatherCondition.SNOW,
      regionAccentHex = "#6366F1",
    ),
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
      defaultMapTiles = listOf(
        4, 4, 1, 5, 5, 1, 4, 4,
        4, 1, 1, 3, 3, 1, 1, 4,
        1, 1, 0, 3, 0, 0, 1, 0,
        0, 0, 3, 3, 3, 0, 2, 2,
        0, 1, 3, 4, 3, 2, 2, 0,
        1, 1, 3, 3, 3, 0, 0, 0,
        0, 0, 0, 3, 1, 1, 0, 0,
        0, 0, 3, 3, 0, 0, 0, 0,
      ),
      encounterPoolDexIds = listOf(157, 503, 724, 900, 901, 905, 493),
      defaultBgmThemeId = "bgm_hoenn_sinnoh_route",
      harmonicShardId = "shard_06_hisui",
      storyLogEntry = "A prism rift opens into ancient Hisui's wild meadows, where noble silhouettes glow with amber rims.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#F59E0B",
    ),
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
      defaultMapTiles = listOf(
        3, 3, 5, 5, 5, 5, 3, 3,
        3, 0, 3, 0, 0, 3, 0, 3,
        0, 1, 3, 3, 3, 3, 1, 0,
        0, 1, 1, 3, 3, 1, 1, 0,
        2, 2, 3, 3, 3, 3, 2, 2,
        0, 0, 3, 0, 0, 3, 0, 0,
        1, 1, 3, 3, 3, 3, 1, 1,
        0, 0, 0, 3, 3, 0, 0, 0,
      ),
      encounterPoolDexIds = listOf(495, 498, 501, 571, 609, 635, 637, 643, 644),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_07_unova",
      storyLogEntry = "Castelia's steel bridges hum with energetic basslines while Kael tracks a Team Monochrome relay.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#F8FAFC",
    ),
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
      defaultMapTiles = listOf(
        4, 4, 4, 5, 5, 4, 4, 4,
        4, 0, 0, 3, 3, 0, 0, 4,
        4, 1, 3, 3, 3, 3, 1, 4,
        0, 1, 3, 0, 0, 3, 1, 0,
        0, 0, 3, 2, 2, 3, 0, 0,
        4, 0, 3, 3, 3, 3, 0, 4,
        4, 4, 1, 3, 3, 1, 4, 4,
        4, 4, 4, 3, 3, 4, 4, 4,
      ),
      encounterPoolDexIds = listOf(196, 197, 248, 330, 376, 249, 250),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_08_orre",
      storyLogEntry = "Desert winds whip across Orre's canyons; Rock-type Sp.Def rises by 1.5x beneath the swirling Sandstorm.",
      weatherPreset = WeatherCondition.SANDSTORM,
      regionAccentHex = "#A855F7",
    ),
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
      defaultMapTiles = listOf(
        0, 0, 3, 5, 5, 3, 0, 0,
        0, 1, 3, 5, 5, 3, 1, 0,
        3, 3, 3, 3, 3, 3, 3, 3,
        1, 0, 3, 2, 2, 3, 0, 1,
        1, 0, 3, 2, 2, 3, 0, 1,
        3, 3, 3, 3, 3, 3, 3, 3,
        0, 1, 0, 3, 3, 0, 1, 0,
        0, 0, 0, 3, 3, 0, 0, 0,
      ),
      encounterPoolDexIds = listOf(650, 653, 656, 658, 681, 700, 716, 717, 718),
      defaultBgmThemeId = "bgm_unova_kalos_route",
      harmonicShardId = "shard_09_kalos",
      storyLogEntry = "Kalos boulevards shine under crisp PICA200 ink outlines as Battle Bond aura lights the plaza.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#EC4899",
    ),
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
      defaultMapTiles = listOf(
        2, 2, 2, 5, 5, 2, 2, 2,
        2, 1, 0, 3, 3, 0, 1, 2,
        2, 1, 1, 3, 0, 1, 1, 2,
        0, 0, 3, 3, 3, 3, 0, 0,
        0, 4, 3, 0, 0, 3, 4, 0,
        2, 0, 3, 1, 1, 3, 0, 2,
        2, 2, 3, 3, 3, 3, 2, 2,
        2, 2, 2, 3, 3, 2, 2, 2,
      ),
      encounterPoolDexIds = listOf(722, 725, 728, 778, 791, 792, 800),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_10_alola",
      storyLogEntry = "Tropical Harsh Sunlight pours over golden beaches, amplifying Fire moves and Z-Crystal radiance.",
      weatherPreset = WeatherCondition.HARSH_SUNLIGHT,
      regionAccentHex = "#FACC15",
    ),
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
      defaultMapTiles = listOf(
        3, 5, 5, 5, 5, 5, 5, 3,
        3, 0, 0, 3, 3, 0, 0, 3,
        1, 1, 0, 3, 3, 0, 1, 1,
        1, 1, 3, 3, 3, 3, 1, 1,
        0, 0, 3, 4, 4, 3, 0, 0,
        2, 0, 3, 3, 3, 3, 0, 2,
        2, 1, 0, 3, 3, 0, 1, 2,
        0, 0, 0, 3, 3, 0, 0, 0,
      ),
      encounterPoolDexIds = listOf(810, 813, 816, 823, 887, 888, 889, 890),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_11_galar",
      storyLogEntry = "Stadium spotlights sweep across Hammerlocke as Dynamax energy surges cleanly without HP glitches.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#FF2A85",
    ),
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
      defaultMapTiles = listOf(
        0, 1, 2, 5, 5, 2, 1, 0,
        1, 1, 2, 2, 2, 2, 1, 1,
        0, 0, 3, 3, 3, 3, 0, 0,
        0, 1, 3, 0, 0, 3, 1, 0,
        1, 1, 3, 1, 1, 3, 1, 1,
        0, 0, 3, 3, 3, 3, 0, 0,
        0, 2, 2, 3, 3, 2, 2, 0,
        0, 0, 0, 3, 3, 0, 0, 0,
      ),
      encounterPoolDexIds = listOf(38, 130, 149, 1017, 1025),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_12_kitakami",
      storyLogEntry = "Lantern reflections ripple across Kitakami's Crystal Pool while Ogerpon guards the twelfth shard.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#2DD4BF",
    ),
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
      defaultMapTiles = listOf(
        5, 4, 5, 5, 5, 5, 4, 5,
        4, 0, 3, 3, 3, 3, 0, 4,
        1, 0, 3, 5, 5, 3, 0, 1,
        1, 1, 3, 3, 3, 3, 1, 1,
        0, 0, 3, 2, 2, 3, 0, 0,
        1, 0, 3, 3, 3, 3, 0, 1,
        4, 1, 0, 3, 3, 0, 1, 4,
        4, 4, 0, 3, 3, 0, 4, 4,
      ),
      encounterPoolDexIds = listOf(906, 909, 912, 937, 959, 983, 987, 992, 1007, 1008),
      defaultBgmThemeId = "bgm_alola_galar_paldea_route",
      harmonicShardId = "shard_13_paldea",
      storyLogEntry = "Tera crystals refract every console palette in Area Zero; Tera Blast adapts cleanly to your Tera Type.",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#38BDF8",
    ),
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
      defaultMapTiles = listOf(
        5, 5, 5, 5, 5, 5, 5, 5,
        5, 2, 3, 3, 3, 3, 4, 5,
        2, 2, 3, 5, 5, 3, 4, 4,
        1, 1, 3, 3, 3, 3, 1, 1,
        0, 0, 3, 5, 5, 3, 0, 0,
        1, 1, 3, 3, 3, 3, 1, 1,
        2, 0, 0, 3, 3, 0, 0, 4,
        5, 0, 3, 3, 3, 3, 0, 5,
      ),
      encounterPoolDexIds = listOf(1018, 1024, 1007, 1008, 384, 150, 493),
      defaultBgmThemeId = "bgm_champion_convergence",
      harmonicShardId = "shard_14_blueberry",
      storyLogEntry = "All four biomes of the Blueberry Dome fuse at the Convergence Summit for the grand Prism Loom finale!",
      weatherPreset = WeatherCondition.CLEAR,
      regionAccentHex = "#00F5D4",
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

    // 4. All 14 Playable Regions + Sequence Lock
    val initialCampaign = CampaignProgressionState()
    val lockedSinnoh = initialCampaign.completeChapter(worldChapters[4])
    val unlockedKanto = initialCampaign.completeChapter(worldChapters[0])
    checks.add("14-Region Prism Resonance Sequence & Maps" to (worldChapters.size == 14 && lockedSinnoh.completedChapterIds.isEmpty() && unlockedKanto.completedChapterIds.contains("kanto") && unlockedKanto.validateInvariants().isEmpty()))

    return checks
  }
}

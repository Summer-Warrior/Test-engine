package com.example.engine

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Clean-room, 100% offline Trainer & Character System for "Pokémon: Prism Resonance".
 *
 * Provides:
 * - Rich source-inspired original Trainer roster spanning ALL 22 regions:
 *   Playable Protagonists, Professors & Researchers, Rivals, Regional Gym Leaders /
 *   Trial Captains / Wardens / Frontier Brains / Champions, Regional Challengers,
 *   Mentors/NPCs, Team Monochrome Syndicate (Grunts, Admins, Commander Vex),
 *   and Spin-off Rangers & Aura Guardians.
 * - Pure-JVM deterministic 32x32 2D Overworld & Battle Pixel Sprite matrix generator
 *   (4-direction walk frames + battle idle + capsule-throw + victory) with crisp 1px
 *   hue-shifted ink outlines and per-pixel 3D depth maps.
 * - Pure-JVM deterministic 40x40 high-contrast GBA/DS-anime bust Dialogue Portrait generator.
 * - Android Bitmap + multi-slice 3D Cel-Voxel character model renderer with LRU cache.
 * - Pure-JVM Tactical Multi-Creature Trainer Battle AI (2-6 creatures, smart switching, Ace gimmick).
 */

enum class TrainerRoleCategory(val label: String, val badge: String) {
  PROTAGONIST("Protagonist", "HERO"),
  PROFESSOR_MENTOR("Professor & Mentor", "PROF"),
  RIVAL("Rival", "RIVAL"),
  GYM_LEADER_WARDEN("Leader & Warden", "LEADER"),
  CHAMPION_FRONTIER("Champion & Frontier", "CHAMP"),
  VILLAIN_SYNDICATE("Team Monochrome", "VEX"),
  RANGER_GUARDIAN("Ranger & Guardian", "RANGER"),
}

enum class TrainerHeadgear(val label: String) {
  CAP("Trainer Cap"),
  VISOR("Prism Visor"),
  BANDANA("Ace Bandana"),
  CROWN("Champion Crown"),
  HOOD("Stealth Hood"),
  GLASSES("Research Glasses"),
  FEDORA("Sleuth Hat"),
  RANGER_HELM("Ranger Crest"),
  NONE("Bare Hair"),
}

enum class TrainerOutfitStyle(val label: String) {
  ADVENTURER_JACKET("Adventurer Jacket"),
  LAB_COAT("Research Lab Coat"),
  RIVAL_TRENCH("Tactical Trench"),
  GYM_ATHLETIC("Arena Tracksuit"),
  CHAMPION_CAPE("Sovereign Cape"),
  SYNDICATE_UNIFORM("Monochrome Uniform"),
  RANGER_SUIT("Ranger Field Suit"),
  KIMONO_WARDEN("Warden Haori"),
}

enum class TrainerPose(val label: String) {
  OVERWORLD_DOWN("Walk Down"),
  OVERWORLD_UP("Walk Up"),
  OVERWORLD_LEFT("Walk Left"),
  OVERWORLD_RIGHT("Walk Right"),
  BATTLE_IDLE("Battle Stance"),
  CAPSULE_THROW("Throw Capsule"),
  VICTORY("Victory Pose"),
}

data class TrainerAppearanceConfig(
  val skinHex: String = "#F6C8A5",
  val hairHex: String = "#1E293B",
  val outfitPrimaryHex: String = "#EF4444",
  val outfitSecondaryHex: String = "#F8FAFC",
  val accentHex: String = "#06B6D4",
  val headgear: TrainerHeadgear = TrainerHeadgear.CAP,
  val outfitStyle: TrainerOutfitStyle = TrainerOutfitStyle.ADVENTURER_JACKET,
  val hairStyleIndex: Int = 0, // 0..5
  val companionPoseIndex: Int = 0, // 0..2
)

data class TrainerCharacterSpec(
  val id: String,
  val name: String,
  val title: String,
  val roleCategory: TrainerRoleCategory,
  val regionId: String,
  val sourceInspiration: String,
  val teamDexIds: List<Int>,
  val aiTactic: TrainerAiTactic = TrainerAiTactic.BALANCED_ALLROUNDER,
  val preferredGimmick: BattleGimmickMode = BattleGimmickMode.NONE,
  val preferredWeather: WeatherCondition = WeatherCondition.CLEAR,
  val introQuote: String,
  val switchQuote: String,
  val defeatQuote: String,
  val dialogueLines: List<String>,
  val appearance: TrainerAppearanceConfig,
  val linkedPoiArchetypeId: String? = null,
)

/**
 * Pure-JVM 32x32 sprite matrix + 3D depth slice map.
 * Palette indices:
 *  -1 = transparent
 *   0 = hue-shifted ink outline
 *   1 = skin light, 2 = skin shadow
 *   3 = hair light, 4 = hair shadow
 *   5 = outfit primary light, 6 = outfit primary shadow
 *   7 = outfit secondary light, 8 = outfit secondary shadow
 *   9 = accent bright, 10 = accent glow / specular
 *  11 = boots / trousers dark, 12 = eye white / pure specular
 *  13 = capsule crimson top, 14 = capsule prism core
 */
data class TrainerSpriteMatrix(
  val width: Int,
  val height: Int,
  val pixels: IntArray,
  val depthMap: ByteArray,
  val paletteArgb: LongArray,
)

data class TrainerPortraitMatrix(
  val width: Int,
  val height: Int,
  val pixels: IntArray,
  val paletteArgb: LongArray,
)

object TrainerCatalogAndRigEngine {

  const val SPRITE_SIZE = 32
  const val PORTRAIT_SIZE = 40

  // Curated outfit color presets for the Protagonist Customizer
  val outfitPalettePresets: List<Pair<String, Triple<String, String, String>>> = listOf(
    "Prism Crimson" to Triple("#EF4444", "#F8FAFC", "#06B6D4"),
    "Cobalt Ace" to Triple("#2563EB", "#E2E8F0", "#FACC15"),
    "Emerald Ranger" to Triple("#10B981", "#1E293B", "#34D399"),
    "Amethyst Rift" to Triple("#8B5CF6", "#F1F5F9", "#38BDF8"),
    "Solaris Gold" to Triple("#F59E0B", "#1E1B4B", "#FB7185"),
    "Obsidian Tactician" to Triple("#334155", "#E2E8F0", "#F43F5E"),
  )

  val skinTonePresets: List<Pair<String, String>> = listOf(
    "Porcelain" to "#FDE2CC",
    "Warm Peach" to "#F6C8A5",
    "Golden Tan" to "#D99B6C",
    "Bronze" to "#A96E44",
    "Deep Umber" to "#6E4226",
  )

  val hairColorPresets: List<Pair<String, String>> = listOf(
    "Raven" to "#1E293B",
    "Crimson" to "#DC2626",
    "Amber" to "#D97706",
    "Silver" to "#CBD5E1",
    "Cobalt" to "#2563EB",
    "Emerald" to "#059669",
    "Violet" to "#7C3AED",
  )

  // 1. Core Playable Protagonists (All customizable)
  val playableProtagonists: List<TrainerCharacterSpec> = listOf(
    TrainerCharacterSpec(
      id = "protag_nova",
      name = "Nova",
      title = "Prism Resonance Ace",
      roleCategory = TrainerRoleCategory.PROTAGONIST,
      regionId = "kanto",
      sourceInspiration = "Mainline + Anime Hero",
      teamDexIds = listOf(25, 6, 94, 448, 658, 384),
      aiTactic = TrainerAiTactic.GIMMICK_ACE,
      preferredGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      introQuote = "Our Prism Resonance is synchronized—let's push past every limit!",
      switchQuote = "Switching formation! Show them our bond!",
      defeatQuote = "What a thrilling match—we'll train even harder for the next summit!",
      dialogueLines = listOf("Ready to chart all 22 fusion regions!", "Every partner's bond sharpens the Prism."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#F6C8A5", hairHex = "#1E293B", outfitPrimaryHex = "#EF4444",
        outfitSecondaryHex = "#F8FAFC", accentHex = "#06B6D4",
        headgear = TrainerHeadgear.CAP, outfitStyle = TrainerOutfitStyle.ADVENTURER_JACKET,
        hairStyleIndex = 0, companionPoseIndex = 0,
      ),
    ),
    TrainerCharacterSpec(
      id = "protag_kairo",
      name = "Kairo",
      title = "Dexholder Tactician",
      roleCategory = TrainerRoleCategory.PROTAGONIST,
      regionId = "johto",
      sourceInspiration = "Manga Adventures Hero",
      teamDexIds = listOf(25, 196, 212, 248, 658, 384),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.TERASTALLIZATION,
      introQuote = "I've analyzed every matchup across the National Atlas!",
      switchQuote = "Counter-pivot! Reading your tempo!",
      defeatQuote = "Impressive adaptation—noted in my tactical Dex.",
      dialogueLines = listOf("Strategy turns type charts into symphonies."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#FDE2CC", hairHex = "#2563EB", outfitPrimaryHex = "#2563EB",
        outfitSecondaryHex = "#F1F5F9", accentHex = "#FACC15",
        headgear = TrainerHeadgear.VISOR, outfitStyle = TrainerOutfitStyle.RIVAL_TRENCH,
        hairStyleIndex = 2, companionPoseIndex = 1,
      ),
    ),
    TrainerCharacterSpec(
      id = "protag_sora",
      name = "Sora",
      title = "Horizon Dreamer",
      roleCategory = TrainerRoleCategory.PROTAGONIST,
      regionId = "paldea",
      sourceInspiration = "Anime Horizon Explorer",
      teamDexIds = listOf(906, 25, 448, 937, 1007),
      aiTactic = TrainerAiTactic.AGGRESSIVE_SWEEPER,
      preferredGimmick = BattleGimmickMode.TERASTALLIZATION,
      introQuote = "Across sky, sea, and starlight—let our partners shine!",
      switchQuote = "Tag in! Keep the rhythm soaring!",
      defeatQuote = "That spark was unforgettable!",
      dialogueLines = listOf("Let's discover what lies beyond the horizon."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#D99B6C", hairHex = "#7C3AED", outfitPrimaryHex = "#0EA5E9",
        outfitSecondaryHex = "#FEF3C7", accentHex = "#F43F5E",
        headgear = TrainerHeadgear.BANDANA, outfitStyle = TrainerOutfitStyle.ADVENTURER_JACKET,
        hairStyleIndex = 3, companionPoseIndex = 2,
      ),
    ),
    TrainerCharacterSpec(
      id = "protag_talia",
      name = "Talia",
      title = "Field Ranger Scout",
      roleCategory = TrainerRoleCategory.PROTAGONIST,
      regionId = "fiore_almia",
      sourceInspiration = "Spin-Off Ranger Hero",
      teamDexIds = listOf(311, 448, 398, 282, 490),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.Z_MOVE,
      introQuote = "Capture Styler calibrated—protecting every wild habitat!",
      switchQuote = "Relay assist! Hold the defensive ring!",
      defeatQuote = "Harmony restored—outstanding teamwork!",
      dialogueLines = listOf("Wild spirits trust Trainers who listen."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#A96E44", hairHex = "#D97706", outfitPrimaryHex = "#10B981",
        outfitSecondaryHex = "#1E293B", accentHex = "#FACC15",
        headgear = TrainerHeadgear.RANGER_HELM, outfitStyle = TrainerOutfitStyle.RANGER_SUIT,
        hairStyleIndex = 4, companionPoseIndex = 0,
      ),
    ),
  )

  // 2. Core Story Cast (Professors, Rivals, Team Monochrome Syndicate, Spin-Off Guardians)
  val coreStoryTrainers: List<TrainerCharacterSpec> = listOf(
    TrainerCharacterSpec(
      id = "prof_lyra_vance",
      name = "Prof. Lyra Vance",
      title = "Prism Resonance Architect",
      roleCategory = TrainerRoleCategory.PROFESSOR_MENTOR,
      regionId = "kanto",
      sourceInspiration = "Mainline Research Lead",
      teamDexIds = listOf(133, 479, 801),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.TERASTALLIZATION,
      introQuote = "Let's test the Prism Loom telemetry in a live calibration match!",
      switchQuote = "Rotating specimens for harmonic cross-verification!",
      defeatQuote = "Remarkable data! Your Prism Resonance frequency is off the charts!",
      dialogueLines = listOf(
        "The Harmonic Fracture linked all 22 regions across games, anime, and manga.",
        "Heal anytime at a Plaza and inspect Trainers in the Omni-Console Viewport!",
      ),
      appearance = TrainerAppearanceConfig(
        skinHex = "#FDE2CC", hairHex = "#0EA5E9", outfitPrimaryHex = "#F8FAFC",
        outfitSecondaryHex = "#0F172A", accentHex = "#06B6D4",
        headgear = TrainerHeadgear.GLASSES, outfitStyle = TrainerOutfitStyle.LAB_COAT,
        hairStyleIndex = 1,
      ),
    ),
    TrainerCharacterSpec(
      id = "rival_kael",
      name = "Rival Kael",
      title = "Crimson Prism Rival",
      roleCategory = TrainerRoleCategory.RIVAL,
      regionId = "kanto",
      sourceInspiration = "Games + Manga Rival",
      teamDexIds = listOf(135, 59, 130, 149),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      introQuote = "You're already at the next Sanctuary? Don't think I'll let you pass untested!",
      switchQuote = "Saw that move coming a mile away—switching in my counter!",
      defeatQuote = "Tch... alright, you've got real resonance. I won't lose at the Summit!",
      dialogueLines = listOf(
        "Smell ya later? Nah—meet me at the Blueberry Prism Summit!",
        "My squad covers every elemental blind spot.",
      ),
      appearance = TrainerAppearanceConfig(
        skinHex = "#F6C8A5", hairHex = "#B45309", outfitPrimaryHex = "#1E293B",
        outfitSecondaryHex = "#DC2626", accentHex = "#F59E0B",
        headgear = TrainerHeadgear.NONE, outfitStyle = TrainerOutfitStyle.RIVAL_TRENCH,
        hairStyleIndex = 2,
      ),
      linkedPoiArchetypeId = "rival",
    ),
    TrainerCharacterSpec(
      id = "rival_verity",
      name = "Rival Verity",
      title = "Starlight Manga Strategist",
      roleCategory = TrainerRoleCategory.RIVAL,
      regionId = "kalos",
      sourceInspiration = "Manga + Anime Rival",
      teamDexIds = listOf(658, 700, 282, 448),
      aiTactic = TrainerAiTactic.AGGRESSIVE_SWEEPER,
      preferredGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      introQuote = "Elegance and precision decide every championship clash!",
      switchQuote = "A graceful pivot changes the entire tempo!",
      defeatQuote = "Bravo! Your bond outperformed my purest calculations!",
      dialogueLines = listOf("Battle choreography is an art form."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#FDE2CC", hairHex = "#EC4899", outfitPrimaryHex = "#6366F1",
        outfitSecondaryHex = "#F8FAFC", accentHex = "#F472B6",
        headgear = TrainerHeadgear.FEDORA, outfitStyle = TrainerOutfitStyle.RIVAL_TRENCH,
        hairStyleIndex = 3,
      ),
    ),
    TrainerCharacterSpec(
      id = "monochrome_grunt",
      name = "Monochrome Operative",
      title = "Team Monochrome Grunt",
      roleCategory = TrainerRoleCategory.VILLAIN_SYNDICATE,
      regionId = "unova",
      sourceInspiration = "Villain Syndicate Archetype",
      teamDexIds = listOf(510, 569, 625),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.NONE,
      introQuote = "Color is chaos! Surrender your Harmonic Shard to Team Monochrome!",
      switchQuote = "Deploy backup unit! Drain the spectrum!",
      defeatQuote = "Gah! Commander Vex will hear about your interference!",
      dialogueLines = listOf("All 22 regions will soon run in pure grayscale silence."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#F6C8A5", hairHex = "#475569", outfitPrimaryHex = "#1E293B",
        outfitSecondaryHex = "#E2E8F0", accentHex = "#94A3B8",
        headgear = TrainerHeadgear.VISOR, outfitStyle = TrainerOutfitStyle.SYNDICATE_UNIFORM,
        hairStyleIndex = 0,
      ),
      linkedPoiArchetypeId = "team_grunt",
    ),
    TrainerCharacterSpec(
      id = "monochrome_admin_noctis",
      name = "Admin Noctis",
      title = "Monochrome Null-Division Lead",
      roleCategory = TrainerRoleCategory.VILLAIN_SYNDICATE,
      regionId = "orre",
      sourceInspiration = "Villain Admin + Shadow Lab",
      teamDexIds = listOf(461, 635, 778, 861),
      aiTactic = TrainerAiTactic.WEATHER_SETTER,
      preferredGimmick = BattleGimmickMode.DYNAMAX,
      preferredWeather = WeatherCondition.SANDSTORM,
      introQuote = "Your vibrant resonance is an anomaly. Allow me to zero it out.",
      switchQuote = "Null-protocol engaged—deploying counter-asset!",
      defeatQuote = "Fascinating... even muted frequencies refuse to break.",
      dialogueLines = listOf("Commander Vex's Zero-Prism Loom nears completion."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#D99B6C", hairHex = "#E2E8F0", outfitPrimaryHex = "#0F172A",
        outfitSecondaryHex = "#94A3B8", accentHex = "#A855F7",
        headgear = TrainerHeadgear.HOOD, outfitStyle = TrainerOutfitStyle.SYNDICATE_UNIFORM,
        hairStyleIndex = 5,
      ),
    ),
    TrainerCharacterSpec(
      id = "monochrome_admin_mireille",
      name = "Admin Mireille",
      title = "Monochrome Prism-Siphon Chief",
      roleCategory = TrainerRoleCategory.VILLAIN_SYNDICATE,
      regionId = "alola",
      sourceInspiration = "Ultra-Rift Scientist Admin",
      teamDexIds = listOf(793, 797, 748, 800),
      aiTactic = TrainerAiTactic.STATUS_TACTICIAN,
      preferredGimmick = BattleGimmickMode.Z_MOVE,
      introQuote = "Every crystal in the Prism Resonance belongs in our containment vault!",
      switchQuote = "Adjusting containment lattice—switching specimens!",
      defeatQuote = "Impossible! The resonance spike shattered our dampener!",
      dialogueLines = listOf("Ultra space and the Prism share the exact same wavelength."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#FDE2CC", hairHex = "#F8FAFC", outfitPrimaryHex = "#334155",
        outfitSecondaryHex = "#F1F5F9", accentHex = "#EC4899",
        headgear = TrainerHeadgear.GLASSES, outfitStyle = TrainerOutfitStyle.SYNDICATE_UNIFORM,
        hairStyleIndex = 1,
      ),
    ),
    TrainerCharacterSpec(
      id = "commander_vex",
      name = "Commander Vex",
      title = "Team Monochrome Zenith Director",
      roleCategory = TrainerRoleCategory.VILLAIN_SYNDICATE,
      regionId = "mystery_rift",
      sourceInspiration = "Final Syndicate Boss",
      teamDexIds = listOf(487, 717, 800, 890, 1024, 493),
      aiTactic = TrainerAiTactic.GIMMICK_ACE,
      preferredGimmick = BattleGimmickMode.TERASTALLIZATION,
      preferredWeather = WeatherCondition.HARSH_SUNLIGHT,
      introQuote = "I have fused every timeline's shadow into one absolute order. Witness the Monochrome Zenith!",
      switchQuote = "Reality bends at my command—face my next sovereign!",
      defeatQuote = "Color... chaos... yet your Resonance binds all 22 worlds in harmony. I concede.",
      dialogueLines = listOf("Only at the Mystery Rift can the Harmonic Fracture be sealed—or rewritten."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#E2C2A8", hairHex = "#F8FAFC", outfitPrimaryHex = "#090D16",
        outfitSecondaryHex = "#CBD5E1", accentHex = "#C084FC",
        headgear = TrainerHeadgear.CROWN, outfitStyle = TrainerOutfitStyle.CHAMPION_CAPE,
        hairStyleIndex = 5,
      ),
      linkedPoiArchetypeId = "villain_boss",
    ),
    TrainerCharacterSpec(
      id = "aura_guardian_sirion",
      name = "Sirion of Rota",
      title = "Ancient Aura Guardian",
      roleCategory = TrainerRoleCategory.RANGER_GUARDIAN,
      regionId = "fiore_almia",
      sourceInspiration = "Anime Film Aura Knight",
      teamDexIds = listOf(448, 151, 282, 475),
      aiTactic = TrainerAiTactic.AGGRESSIVE_SWEEPER,
      preferredGimmick = BattleGimmickMode.MEGA_EVOLUTION,
      introQuote = "Feel the pulse of Aura across the Tree of Beginning—show me your heart!",
      switchQuote = "Aura flows unbroken—tagging in my sworn partner!",
      defeatQuote = "Your Aura burns clear and true, worthy of legend.",
      dialogueLines = listOf("Aura connects Trainer and partner beyond distance or time."),
      appearance = TrainerAppearanceConfig(
        skinHex = "#F6C8A5", hairHex = "#1E3A8A", outfitPrimaryHex = "#1D4ED8",
        outfitSecondaryHex = "#E0F2FE", accentHex = "#38BDF8",
        headgear = TrainerHeadgear.FEDORA, outfitStyle = TrainerOutfitStyle.CHAMPION_CAPE,
        hairStyleIndex = 2,
      ),
    ),
  )

  private data class RegionalTrioSeed(
    val regionId: String,
    val regionLabel: String,
    val leaderName: String,
    val leaderTitle: String,
    val leaderTeam: List<Int>,
    val leaderGimmick: BattleGimmickMode,
    val leaderWeather: WeatherCondition,
    val leaderOutfit: TrainerOutfitStyle,
    val leaderHeadgear: TrainerHeadgear,
    val leaderPrimaryHex: String,
    val leaderAccentHex: String,
    val challengerName: String,
    val challengerTitle: String,
    val challengerTeam: List<Int>,
    val challengerTactic: TrainerAiTactic,
    val mentorName: String,
    val mentorTitle: String,
    val mentorTeam: List<Int>,
    val sourceTag: String,
  )

  private val regionalTrioSeeds: List<RegionalTrioSeed> = listOf(
    RegionalTrioSeed(
      "kanto", "Kanto", "Apex Master Red-Echo", "Indigo Summit Champion",
      listOf(25, 6, 3, 9, 143, 150), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.ADVENTURER_JACKET, TrainerHeadgear.CAP, "#DC2626", "#FACC15",
      "Ace Challenger Blue-Echo", "Viridian Tactical Leader", listOf(18, 65, 130, 59), TrainerAiTactic.STATUS_TACTICIAN,
      "Elder Botanist Oak-Echo", "Pallet Research Mentor", listOf(1, 4, 7), "Mainline Gen 1 + Manga",
    ),
    RegionalTrioSeed(
      "johto", "Johto", "Dragon Sovereign Lance-Echo", "Bell Tower Champion",
      listOf(130, 142, 149, 230, 248), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.RAIN,
      TrainerOutfitStyle.CHAMPION_CAPE, TrainerHeadgear.CROWN, "#991B1B", "#F59E0B",
      "Ghost Mystic Morty-Echo", "Ecruteak Shrine Leader", listOf(94, 200, 250), TrainerAiTactic.STATUS_TACTICIAN,
      "Apricorn Artisan Kurt-Echo", "Azalea Ballwright Mentor", listOf(152, 181, 212), "Mainline Gen 2 + Anime",
    ),
    RegionalTrioSeed(
      "hoenn", "Hoenn", "Steel Sovereign Steven-Echo", "Sky Pillar Champion",
      listOf(227, 306, 348, 376, 384), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.SANDSTORM,
      TrainerOutfitStyle.RIVAL_TRENCH, TrainerHeadgear.NONE, "#334155", "#38BDF8",
      "Contest Ace May-Echo", "Emerald Horizon Rival", listOf(257, 267, 282, 330), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Field Ecologist Birch-Echo", "Littleroot Survey Mentor", listOf(252, 255, 258), "Mainline Gen 3 + ORAS",
    ),
    RegionalTrioSeed(
      "sinnoh", "Sinnoh", "Mythos Champion Cynthia-Echo", "Spear Pillar Sovereign",
      listOf(442, 407, 350, 448, 445, 483), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.SANDSTORM,
      TrainerOutfitStyle.RIVAL_TRENCH, TrainerHeadgear.NONE, "#0F172A", "#FACC15",
      "Frontier Tower Palmer-Echo", "Battle Tower Tycoon", listOf(464, 149, 488), TrainerAiTactic.GIMMICK_ACE,
      "Evolution Scholar Rowan-Echo", "Sandgem Mythos Mentor", listOf(387, 390, 393), "Mainline Gen 4 + Anime",
    ),
    RegionalTrioSeed(
      "hisui", "Hisui Rift", "Noble Warden Adaman-Echo", "Diamond Clan Warden",
      listOf(470, 899, 901, 905, 493), BattleGimmickMode.NONE, WeatherCondition.SNOW,
      TrainerOutfitStyle.KIMONO_WARDEN, TrainerHeadgear.BANDANA, "#0284C7", "#E0F2FE",
      "Pearl Warden Irida-Echo", "Glacier Clan Duelist", listOf(471, 900, 902), TrainerAiTactic.WEATHER_SETTER,
      "Survey Captain Laventon-Echo", "Jubilife Galaxy Mentor", listOf(722, 155, 501), "Legends Hisui",
    ),
    RegionalTrioSeed(
      "unova", "Unova", "Harmonic King N-Echo", "Plasma Truth Ideologist",
      listOf(571, 567, 601, 643, 644), BattleGimmickMode.NONE, WeatherCondition.CLEAR,
      TrainerOutfitStyle.ADVENTURER_JACKET, TrainerHeadgear.CAP, "#10B981", "#F8FAFC",
      "Subway Boss Ingo-Echo", "Gear Station Strategist", listOf(609, 530, 612), TrainerAiTactic.STATUS_TACTICIAN,
      "Origin Scholar Juniper-Echo", "Nuvema Field Mentor", listOf(495, 498, 501), "Mainline Gen 5 + Manga",
    ),
    RegionalTrioSeed(
      "kalos", "Kalos", "Grand Actress Diantha-Echo", "Lumiose Prism Champion",
      listOf(701, 697, 699, 706, 282, 716), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.CHAMPION_CAPE, TrainerHeadgear.CROWN, "#F8FAFC", "#F43F5E",
      "Mega Ace Alain-Echo", "Prism Tower Rival", listOf(6, 376, 461, 658), TrainerAiTactic.GIMMICK_ACE,
      "Mega Scholar Sycamore-Echo", "Lumiose Lab Mentor", listOf(650, 653, 656), "Mainline Gen 6 + XYZ Anime",
    ),
    RegionalTrioSeed(
      "alola", "Alola", "Masked Royal Kukui-Echo", "Alola League Founder",
      listOf(727, 745, 730, 724, 785), BattleGimmickMode.Z_MOVE, WeatherCondition.HARSH_SUNLIGHT,
      TrainerOutfitStyle.GYM_ATHLETIC, TrainerHeadgear.VISOR, "#F59E0B", "#06B6D4",
      "Island Kahuna Hala-Echo", "Melemele Grand Trial", listOf(740, 297, 760), TrainerAiTactic.BALANCED_ALLROUNDER,
      "Ultra Scholar Burnet-Echo", "Dimensional Reef Mentor", listOf(722, 725, 728), "Mainline Gen 7 + Anime",
    ),
    RegionalTrioSeed(
      "galar", "Galar", "Coronation Master Leon-Echo", "Wyndon Monarch Champion",
      listOf(887, 812, 815, 818, 6, 888), BattleGimmickMode.DYNAMAX, WeatherCondition.CLEAR,
      TrainerOutfitStyle.CHAMPION_CAPE, TrainerHeadgear.CAP, "#7C3AED", "#FACC15",
      "Dragon Rival Raihan-Echo", "Hammerlocke Weather Ace", listOf(884, 330, 248, 844), TrainerAiTactic.WEATHER_SETTER,
      "Dynamax Scholar Magnolia-Echo", "Wedgehurst Lab Mentor", listOf(810, 813, 816), "Mainline Gen 8 + Journeys",
    ),
    RegionalTrioSeed(
      "kitakami", "Kitakami", "Mask Challenger Kieran-Echo", "Oni Mountain Rival",
      listOf(1019, 1018, 1011, 727, 1017), BattleGimmickMode.TERASTALLIZATION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.KIMONO_WARDEN, TrainerHeadgear.BANDANA, "#4F46E5", "#34D399",
      "Festival Keeper Carmine-Echo", "Mossui Lantern Duelist", listOf(1013, 262, 38), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Folklore Elder Briar-Echo", "Crystal Pool Mentor", listOf(1012, 1017), "Teal Mask",
    ),
    RegionalTrioSeed(
      "paldea", "Paldea", "Academy Champion Nemona-Echo", "Mesagoza Battle Prodigy",
      listOf(923, 908, 911, 914, 987, 1007), BattleGimmickMode.TERASTALLIZATION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.GYM_ATHLETIC, TrainerHeadgear.NONE, "#F97316", "#22C55E",
      "Zero Specialist Arven-Echo", "Titan Trail Challenger", listOf(943, 934, 952), TrainerAiTactic.BALANCED_ALLROUNDER,
      "Academy Director Clavell-Echo", "Naranja Research Mentor", listOf(906, 909, 912), "Mainline Gen 9",
    ),
    RegionalTrioSeed(
      "blueberry_summit", "Blueberry Terrarium", "BB Elite Dragon Drayton-Echo", "Terrarium League Ace",
      listOf(1018, 330, 612, 149, 1024), BattleGimmickMode.TERASTALLIZATION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.GYM_ATHLETIC, TrainerHeadgear.VISOR, "#0EA5E9", "#FACC15",
      "BB Fairy Tactician Lacey-Echo", "Savanna Biome Challenger", listOf(547, 530, 869), TrainerAiTactic.STATUS_TACTICIAN,
      "League Curator Cyrano-Echo", "Terarium Core Mentor", listOf(984, 990, 1024), "Indigo Disk Finale",
    ),
    RegionalTrioSeed(
      "sevii_isles", "Sevii Isles", "Ember Keeper Solaris", "Mt. Ember Flame Guardian",
      listOf(146, 59, 126, 219, 386), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.HARSH_SUNLIGHT,
      TrainerOutfitStyle.ADVENTURER_JACKET, TrainerHeadgear.BANDANA, "#EA580C", "#FDE047",
      "Seagallop Ace Brine", "Seven-Star Sea Challenger", listOf(131, 230, 121), TrainerAiTactic.WEATHER_SETTER,
      "Archivist Celio-Echo", "One Island Network Mentor", listOf(133, 137, 251), "FRLG Sevii Isles",
    ),
    RegionalTrioSeed(
      "orange_archipelago", "Orange Archipelago", "Orange Champion Drake-Echo", "Pummelo Trophy Master",
      listOf(149, 94, 103, 134, 125), BattleGimmickMode.Z_MOVE, WeatherCondition.RAIN,
      TrainerOutfitStyle.ADVENTURER_JACKET, TrainerHeadgear.BANDANA, "#F97316", "#06B6D4",
      "Coral Gym Captain Rudy-Echo", "Trovita Rhythm Challenger", listOf(125, 103, 121), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Marine Botanist Ivy-Echo", "Valencia Coral Mentor", listOf(45, 116, 131), "Anime Orange Islands",
    ),
    RegionalTrioSeed(
      "orre", "Orre Desert Wastes", "Cipher Commander Greevil-Echo", "Citadark Shadow Tactician",
      listOf(249, 144, 145, 146, 248), BattleGimmickMode.DYNAMAX, WeatherCondition.SANDSTORM,
      TrainerOutfitStyle.SYNDICATE_UNIFORM, TrainerHeadgear.FEDORA, "#7C2D12", "#A855F7",
      "Snagem Renegade Wes-Echo", "Phenac Outlaw Duelist", listOf(196, 197, 330, 244), TrainerAiTactic.STATUS_TACTICIAN,
      "Relic Elder Eagun-Echo", "Agate Purification Mentor", listOf(25, 251), "Colosseum & XD",
    ),
    RegionalTrioSeed(
      "fiore_almia", "Fiore & Almia", "Top Ranger Solana-Echo", "Ranger Union Commander",
      listOf(311, 448, 398, 485, 490), BattleGimmickMode.Z_MOVE, WeatherCondition.CLEAR,
      TrainerOutfitStyle.RANGER_SUIT, TrainerHeadgear.RANGER_HELM, "#DC2626", "#38BDF8",
      "Almia Ace Kellyn-Echo", "Vientown Styler Duelist", listOf(312, 445, 475), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Union Chief Hastings-Echo", "Capture Styler Mentor", listOf(133, 492), "Pokémon Ranger",
    ),
    RegionalTrioSeed(
      "ransei", "Ransei Kingdoms", "Aurora Warlord Nobunaga-Echo", "Dragnor Perfect Link",
      listOf(384, 635, 612, 644, 373), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.KIMONO_WARDEN, TrainerHeadgear.CROWN, "#1E1B4B", "#F59E0B",
      "Ignis Strategist Hideyoshi-Echo", "Flame Banner Duelist", listOf(392, 643, 637), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Aurora Tactician Oichi-Echo", "Harmony Keep Mentor", listOf(133, 39), "Pokémon Conquest",
    ),
    RegionalTrioSeed(
      "ferrum", "Ferrum Arena Isle", "Ferrum Ace Nia-Echo", "Chroma League Synergy Master",
      listOf(448, 461, 257, 681, 150), BattleGimmickMode.MEGA_EVOLUTION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.GYM_ATHLETIC, TrainerHeadgear.VISOR, "#E11D48", "#38BDF8",
      "Iron Striker Travis-Echo", "Tellur Dojo Brawler", listOf(68, 445, 701), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Arena Master Anne-Echo", "Synergy Stone Mentor", listOf(25, 282), "Pokkén Tournament",
    ),
    RegionalTrioSeed(
      "lental", "Lental Photosphere", "Prof. Mirror-Echo", "Illumina Phenomenon Chief",
      listOf(350, 764, 637, 250, 385), BattleGimmickMode.TERASTALLIZATION, WeatherCondition.RAIN,
      TrainerOutfitStyle.LAB_COAT, TrainerHeadgear.GLASSES, "#0D9488", "#FDE047",
      "Photosphere Artist Todd-Echo", "4-Star Lens Challenger", listOf(25, 131, 448), TrainerAiTactic.STATUS_TACTICIAN,
      "NEO-ONE Pilot Rita-Echo", "Florio Atoll Mentor", listOf(810, 722), "New Pokémon Snap",
    ),
    RegionalTrioSeed(
      "pasio", "Pasio Convergence", "Sync-Pair Prince Lear-Echo", "PML Tournament Host",
      listOf(720, 398, 553, 448, 384), BattleGimmickMode.Z_MOVE, WeatherCondition.CLEAR,
      TrainerOutfitStyle.CHAMPION_CAPE, TrainerHeadgear.VISOR, "#F59E0B", "#EC4899",
      "Sync Rival Paulo-Echo", "Centra Summit Challenger", listOf(745, 720, 445), TrainerAiTactic.GIMMICK_ACE,
      "Sync Coordinator Bellis-Echo", "Pasio Sync Lab Mentor", listOf(133, 25), "Pokémon Masters EX",
    ),
    RegionalTrioSeed(
      "aeos", "Aeos Stadium Island", "Unite Captain Erbie-Echo", "Aeos Energy Champion",
      listOf(658, 815, 448, 887, 888), BattleGimmickMode.DYNAMAX, WeatherCondition.CLEAR,
      TrainerOutfitStyle.GYM_ATHLETIC, TrainerHeadgear.CAP, "#7C3AED", "#F97316",
      "Lane Carry Zera-Echo", "Sky Ruins Striker", listOf(807, 6, 94), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Aeos Researcher Phorus-Echo", "Stadium Goal Mentor", listOf(25, 143), "Pokémon UNITE",
    ),
    RegionalTrioSeed(
      "mystery_rift", "Mystery Rift", "Time Explorer Grovyle-Echo", "Temporal Spire Guardian",
      listOf(254, 483, 487, 491, 493), BattleGimmickMode.TERASTALLIZATION, WeatherCondition.CLEAR,
      TrainerOutfitStyle.RANGER_SUIT, TrainerHeadgear.HOOD, "#059669", "#C084FC",
      "Guild Master Wiggly-Echo", "Treasure Town Challenger", listOf(40, 441, 131), TrainerAiTactic.AGGRESSIVE_SWEEPER,
      "Dimension Chronicler Celebi-Echo", "Time Gear Mentor", listOf(251, 385), "Mystery Dungeon + Ultra Rift",
    ),
  )

  // Generate 3 distinct Trainers per region (66 regional Trainers) + 4 Protagonists + 8 Core Cast = 78 total!
  val regionalTrainers: List<TrainerCharacterSpec> by lazy {
    regionalTrioSeeds.flatMapIndexed { idx, seed ->
      val skinTone = skinTonePresets[idx % skinTonePresets.size].second
      val hairTone = hairColorPresets[(idx * 3 + 1) % hairColorPresets.size].second
      val challengerSkin = skinTonePresets[(idx + 2) % skinTonePresets.size].second
      val challengerHair = hairColorPresets[(idx * 2 + 4) % hairColorPresets.size].second
      val mentorSkin = skinTonePresets[(idx + 4) % skinTonePresets.size].second
      listOf(
        // 1. Signature Regional Leader / Champion / Warden
        TrainerCharacterSpec(
          id = "leader_${seed.regionId}",
          name = seed.leaderName,
          title = seed.leaderTitle,
          roleCategory = if (idx % 2 == 0) TrainerRoleCategory.CHAMPION_FRONTIER else TrainerRoleCategory.GYM_LEADER_WARDEN,
          regionId = seed.regionId,
          sourceInspiration = seed.sourceTag,
          teamDexIds = seed.leaderTeam,
          aiTactic = TrainerAiTactic.GIMMICK_ACE,
          preferredGimmick = seed.leaderGimmick,
          preferredWeather = seed.leaderWeather,
          introQuote = "Welcome to ${seed.regionLabel}'s Sanctuary! Face my ${seed.leaderTeam.size}-partner sovereign team!",
          switchQuote = "Excellent strike—now meet the next pillar of ${seed.regionLabel}!",
          defeatQuote = "Magnificent! You've earned ${seed.regionLabel}'s Harmonic Resonance Shard!",
          dialogueLines = listOf(
            "${seed.leaderTitle} guarding ${seed.regionLabel}'s Prism Sanctuary.",
            "My team draws strength from ${seed.sourceTag} tactics.",
          ),
          appearance = TrainerAppearanceConfig(
            skinHex = skinTone,
            hairHex = hairTone,
            outfitPrimaryHex = seed.leaderPrimaryHex,
            outfitSecondaryHex = "#F8FAFC",
            accentHex = seed.leaderAccentHex,
            headgear = seed.leaderHeadgear,
            outfitStyle = seed.leaderOutfit,
            hairStyleIndex = idx % 6,
          ),
        ),
        // 2. Regional Challenger / Rival
        TrainerCharacterSpec(
          id = "challenger_${seed.regionId}",
          name = seed.challengerName,
          title = seed.challengerTitle,
          roleCategory = TrainerRoleCategory.RIVAL,
          regionId = seed.regionId,
          sourceInspiration = seed.sourceTag,
          teamDexIds = seed.challengerTeam,
          aiTactic = seed.challengerTactic,
          preferredGimmick = if (idx % 3 == 0) seed.leaderGimmick else BattleGimmickMode.NONE,
          preferredWeather = seed.leaderWeather,
          introQuote = "Hold it! Nobody crosses ${seed.regionLabel} without testing their squad against me!",
          switchQuote = "Time to shift gears—out you go!",
          defeatQuote = "Whoa, your partners fought in total sync! Good luck at the Sanctuary!",
          dialogueLines = listOf(
            "Training every day across ${seed.regionLabel}'s wild biomes!",
          ),
          appearance = TrainerAppearanceConfig(
            skinHex = challengerSkin,
            hairHex = challengerHair,
            outfitPrimaryHex = seed.leaderAccentHex,
            outfitSecondaryHex = "#1E293B",
            accentHex = seed.leaderPrimaryHex,
            headgear = if (idx % 2 == 0) TrainerHeadgear.CAP else TrainerHeadgear.BANDANA,
            outfitStyle = TrainerOutfitStyle.ADVENTURER_JACKET,
            hairStyleIndex = (idx + 2) % 6,
          ),
        ),
        // 3. Regional Mentor / Professor / NPC
        TrainerCharacterSpec(
          id = "mentor_${seed.regionId}",
          name = seed.mentorName,
          title = seed.mentorTitle,
          roleCategory = TrainerRoleCategory.PROFESSOR_MENTOR,
          regionId = seed.regionId,
          sourceInspiration = seed.sourceTag,
          teamDexIds = seed.mentorTeam,
          aiTactic = TrainerAiTactic.BALANCED_ALLROUNDER,
          preferredGimmick = BattleGimmickMode.NONE,
          introQuote = "Let's share a friendly exhibition match to study ${seed.regionLabel}'s ecology!",
          switchQuote = "Let's observe another species synergy!",
          defeatQuote = "Splendid teamwork! May ${seed.regionLabel} guide your journey.",
          dialogueLines = listOf(
            "Rest up and check the local landmarks across ${seed.regionLabel}.",
            "Regional encounters here reflect ${seed.sourceTag} lore.",
          ),
          appearance = TrainerAppearanceConfig(
            skinHex = mentorSkin,
            hairHex = "#CBD5E1",
            outfitPrimaryHex = "#E2E8F0",
            outfitSecondaryHex = seed.leaderPrimaryHex,
            accentHex = seed.leaderAccentHex,
            headgear = if (idx % 2 == 0) TrainerHeadgear.GLASSES else TrainerHeadgear.NONE,
            outfitStyle = TrainerOutfitStyle.LAB_COAT,
            hairStyleIndex = (idx + 4) % 6,
          ),
        ),
      )
    }
  }

  val allBuiltInTrainers: List<TrainerCharacterSpec> by lazy {
    playableProtagonists + coreStoryTrainers + regionalTrainers
  }

  val defaultProtagonist: TrainerCharacterSpec
    get() = playableProtagonists.first()

  fun findById(trainerId: String?): TrainerCharacterSpec? =
    trainerId?.let { id -> allBuiltInTrainers.firstOrNull { it.id == id } }

  fun trainersForRegion(regionId: String): List<TrainerCharacterSpec> =
    allBuiltInTrainers.filter { it.regionId == regionId }

  fun resolveBossTrainerForRegion(regionId: String): TrainerCharacterSpec {
    if (regionId == "mystery_rift") {
      return findById("commander_vex") ?: regionalTrainers.last()
    }
    return allBuiltInTrainers.firstOrNull { it.id == "leader_$regionId" }
      ?: allBuiltInTrainers.firstOrNull { it.regionId == regionId }
      ?: coreStoryTrainers.first()
  }

  /**
   * Maps any map RegionPointOfInterest deterministically to a rich TrainerCharacterSpec
   * so every overworld NPC renders with an authentic 2D pixel sprite, 40x40 portrait,
   * and multi-creature themed battle team.
   */
  fun resolveTrainerForPoi(poi: RegionPointOfInterest, regionId: String): TrainerCharacterSpec {
    val nameLower = poi.name.lowercase()
    if (nameLower.contains("lyra")) return findById("prof_lyra_vance")!!
    if (nameLower.contains("vex") || nameLower.contains("monochrome")) return findById("commander_vex")!!
    if (nameLower.contains("kael")) return findById("rival_kael")!!

    val exactRegional = allBuiltInTrainers.firstOrNull { spec ->
      spec.regionId == regionId && (
        spec.name.equals(poi.name, ignoreCase = true) ||
          poi.name.contains(spec.name.substringAfterLast(" "), ignoreCase = true)
        )
    }
    if (exactRegional != null) return exactRegional

    return when (poi.interactionKind) {
      PoiInteractionKind.RIVAL_OR_CHALLENGER -> {
        findById("challenger_$regionId") ?: findById("leader_$regionId") ?: findById("rival_kael")!!
      }
      PoiInteractionKind.HEALER_OR_LORE -> {
        findById("mentor_$regionId") ?: findById("prof_lyra_vance")!!
      }
      PoiInteractionKind.PROFESSOR_GUIDE -> {
        val base = findById("prof_lyra_vance") ?: findById("mentor_$regionId")!!
        if (poi.name.contains("Lyra", ignoreCase = true)) {
          base
        } else {
          base.copy(
            id = "guide_${poi.id}",
            name = poi.name,
            title = poi.roleBadge,
            roleCategory = TrainerRoleCategory.PROFESSOR_MENTOR,
            appearance = base.appearance.copy(
              headgear = TrainerHeadgear.GLASSES,
              outfitStyle = TrainerOutfitStyle.LAB_COAT,
              outfitPrimaryHex = "#F8FAFC",
              accentHex = "#06B6D4",
            ),
          )
        }
      }
    }
  }

  /**
   * Maps any TrainerArchetype (from the 57-class roster) to a full TrainerCharacterSpec
   * with a themed 2-5 creature team and matching visual appearance.
   */
  fun resolveTrainerForArchetype(archetype: TrainerArchetype, regionId: String, seedNonce: Int = 0): TrainerCharacterSpec {
    val linked = allBuiltInTrainers.firstOrNull { it.linkedPoiArchetypeId == archetype.id }
    if (linked != null) return linked

    val hash = abs(archetype.id.hashCode() + regionId.hashCode())
    val teamSize = when {
      archetype.levelOffset >= 7 -> 5
      archetype.levelOffset >= 4 -> 4
      archetype.levelOffset >= 2 -> 3
      else -> 2
    }
    val team = (0 until teamSize).map { slot ->
      TrainerArchetypeCatalog.resolveEnemyDexForTrainer(
        archetype = archetype,
        regionId = regionId,
        seedNonce = seedNonce + slot * 7,
      )
    }.distinct().ifEmpty { listOf(25, 94) }

    val roleCat = when (archetype.category) {
      TrainerCategory.LEAGUE_FRONTIER -> TrainerRoleCategory.CHAMPION_FRONTIER
      TrainerCategory.FACTION -> TrainerRoleCategory.VILLAIN_SYNDICATE
      TrainerCategory.CROSS_MEDIA -> TrainerRoleCategory.RANGER_GUARDIAN
      TrainerCategory.SPECIALIST -> TrainerRoleCategory.GYM_LEADER_WARDEN
      TrainerCategory.ROUTE_FIELD -> TrainerRoleCategory.RIVAL
    }
    val outfitStyle = when (roleCat) {
      TrainerRoleCategory.CHAMPION_FRONTIER -> TrainerOutfitStyle.CHAMPION_CAPE
      TrainerRoleCategory.RIVAL -> TrainerOutfitStyle.RIVAL_TRENCH
      TrainerRoleCategory.VILLAIN_SYNDICATE -> TrainerOutfitStyle.SYNDICATE_UNIFORM
      TrainerRoleCategory.RANGER_GUARDIAN -> TrainerOutfitStyle.RANGER_SUIT
      else -> TrainerOutfitStyle.entries[hash % TrainerOutfitStyle.entries.size]
    }
    val headgear = when (roleCat) {
      TrainerRoleCategory.CHAMPION_FRONTIER -> TrainerHeadgear.CROWN
      TrainerRoleCategory.VILLAIN_SYNDICATE -> TrainerHeadgear.VISOR
      TrainerRoleCategory.RANGER_GUARDIAN -> TrainerHeadgear.RANGER_HELM
      else -> TrainerHeadgear.entries[hash % TrainerHeadgear.entries.size]
    }
    val paletteChoice = outfitPalettePresets[hash % outfitPalettePresets.size].second
    return TrainerCharacterSpec(
      id = "arch_${archetype.id}_$regionId",
      name = archetype.className,
      title = "${archetype.sourceLineage} • ${archetype.aiTactic.label}",
      roleCategory = roleCat,
      regionId = regionId,
      sourceInspiration = archetype.sourceLineage,
      teamDexIds = team,
      aiTactic = archetype.aiTactic,
      preferredGimmick = archetype.preferredGimmick,
      preferredWeather = archetype.preferredWeather,
      introQuote = archetype.introQuote,
      switchQuote = "${archetype.className}: Adjusting formation—sending out my next partner!",
      defeatQuote = archetype.defeatQuote,
      dialogueLines = listOf(archetype.introQuote),
      appearance = TrainerAppearanceConfig(
        skinHex = skinTonePresets[hash % skinTonePresets.size].second,
        hairHex = hairColorPresets[(hash / 3) % hairColorPresets.size].second,
        outfitPrimaryHex = paletteChoice.first,
        outfitSecondaryHex = paletteChoice.second,
        accentHex = paletteChoice.third,
        headgear = headgear,
        outfitStyle = outfitStyle,
        hairStyleIndex = hash % 6,
      ),
      linkedPoiArchetypeId = archetype.id,
    )
  }

  // =========================================================================
  // PURE-JVM COLOR MATH (ZERO android.graphics.Color CALLS)
  // =========================================================================

  private fun parseHexArgb(hex: String, fallback: Long): Long {
    val clean = hex.trim().removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return fallback
    return if (clean.length == 6) (0xFF000000L or parsed) else parsed
  }

  private fun shadeArgb(argb: Long, factor: Float, blueShift: Int = 0): Long {
    val a = (argb ushr 24) and 0xFFL
    val r = (((argb ushr 16) and 0xFFL) * factor).roundToInt().coerceIn(0, 255)
    val g = (((argb ushr 8) and 0xFFL) * factor).roundToInt().coerceIn(0, 255)
    val b = ((((argb and 0xFFL) * factor).roundToInt()) + blueShift).coerceIn(0, 255)
    return (a shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
  }

  private fun buildTrainerPalette(appearance: TrainerAppearanceConfig): LongArray {
    val skin = parseHexArgb(appearance.skinHex, 0xFFF6C8A5L)
    val hair = parseHexArgb(appearance.hairHex, 0xFF1E293BL)
    val primary = parseHexArgb(appearance.outfitPrimaryHex, 0xFFEF4444L)
    val secondary = parseHexArgb(appearance.outfitSecondaryHex, 0xFFF8FAFCL)
    val accent = parseHexArgb(appearance.accentHex, 0xFF06B6D4L)
    val ink = shadeArgb(primary, 0.16f, blueShift = 18) // Rich hue-shifted dark ink (never muddy grey)
    return longArrayOf(
      ink,                                  // 0: 1px crisp ink outline
      skin,                                 // 1: skin light
      shadeArgb(skin, 0.78f, blueShift = 4), // 2: skin shadow
      hair,                                 // 3: hair light
      shadeArgb(hair, 0.65f, blueShift = 10), // 4: hair shadow
      primary,                              // 5: outfit primary light
      shadeArgb(primary, 0.68f, blueShift = 10), // 6: outfit primary shadow
      secondary,                            // 7: outfit secondary light
      shadeArgb(secondary, 0.78f, blueShift = 8), // 8: outfit secondary shadow
      accent,                               // 9: accent bright
      shadeArgb(accent, 1.24f, blueShift = 10), // 10: accent glow
      0xFF1E293BL,                          // 11: trousers / boots
      0xFFFFFFFFL,                          // 12: eye white / specular
      0xFFEF4444L,                          // 13: Prism Capsule crimson shell
      0xFF38BDF8L,                          // 14: Prism Capsule glowing core
    )
  }

  // =========================================================================
  // PURE-JVM 32x32 2D OVERWORLD & BATTLE PIXEL SPRITE + 3D DEPTH GENERATOR
  // =========================================================================

  fun poseForDirection(direction: MoveDirection): TrainerPose = when (direction) {
    MoveDirection.DOWN -> TrainerPose.OVERWORLD_DOWN
    MoveDirection.UP -> TrainerPose.OVERWORLD_UP
    MoveDirection.LEFT -> TrainerPose.OVERWORLD_LEFT
    MoveDirection.RIGHT -> TrainerPose.OVERWORLD_RIGHT
  }

  /**
   * Generates a crisp 32x32 indexed pixel sprite + 3D depth map for any TrainerCharacterSpec,
   * supporting all 4 overworld directions with 2-frame walk stride animation, Battle Idle,
   * Capsule-Throw pose, and Victory pose — with crisp 1px hue-shifted ink outlines.
   */
  fun generateTrainerSpriteMatrix(
    spec: TrainerCharacterSpec,
    pose: TrainerPose = TrainerPose.BATTLE_IDLE,
    walkFrameIndex: Int = 0,
  ): TrainerSpriteMatrix {
    val w = SPRITE_SIZE
    val h = SPRITE_SIZE
    val pixels = IntArray(w * h) { -1 }
    val depth = ByteArray(w * h) { 0 }
    val palette = buildTrainerPalette(spec.appearance)
    val app = spec.appearance
    val stride = if (walkFrameIndex % 2 == 0) 1 else -1

    fun put(x: Int, y: Int, idx: Int, d: Int) {
      if (x in 1 until (w - 1) && y in 1 until (h - 1)) {
        val pos = y * w + x
        pixels[pos] = idx
        if (d > depth[pos].toInt()) depth[pos] = d.coerceIn(1, 7).toByte()
      }
    }

    fun fillRect(x0: Int, y0: Int, rw: Int, rh: Int, idx: Int, d: Int) {
      for (yy in y0 until (y0 + rh)) {
        for (xx in x0 until (x0 + rw)) {
          put(xx, yy, idx, d)
        }
      }
    }

    val isFacingLeft = pose == TrainerPose.OVERWORLD_LEFT
    val isFacingRight = pose == TrainerPose.OVERWORLD_RIGHT
    val isSide = isFacingLeft || isFacingRight
    val isBack = pose == TrainerPose.OVERWORLD_UP
    val isBattle = pose == TrainerPose.BATTLE_IDLE || pose == TrainerPose.CAPSULE_THROW || pose == TrainerPose.VICTORY

    // 1. Optional Flowing Cape / Haori Backdrop (behind torso)
    if (app.outfitStyle == TrainerOutfitStyle.CHAMPION_CAPE || app.outfitStyle == TrainerOutfitStyle.KIMONO_WARDEN) {
      val capeColor = if (app.outfitStyle == TrainerOutfitStyle.CHAMPION_CAPE) 5 else 6
      val capeShift = if (pose == TrainerPose.CAPSULE_THROW) -2 else 0
      fillRect(9 + capeShift, 13, 14, 13, capeColor, 2)
      fillRect(8 + capeShift, 18, 16, 9, 6, 2)
      fillRect(10 + capeShift, 24, 12, 3, 9, 3) // golden trim hem
    }

    // 2. Legs & Boots (animated stride for walk / dynamic stance for battle)
    val leftLegY = when {
      isBattle -> 22
      stride > 0 -> 22
      else -> 21
    }
    val rightLegY = when {
      isBattle -> 22
      stride < 0 -> 22
      else -> 21
    }
    if (isSide) {
      val frontOffset = if (stride > 0) 2 else -2
      fillRect(14 + frontOffset, 22, 4, 6, 11, 4)
      fillRect(14 - frontOffset, 22, 4, 5, 6, 3)
      fillRect(13 + frontOffset, 27, 5, 2, 9, 4) // boot accent
      fillRect(13 - frontOffset, 26, 5, 2, 11, 3)
    } else {
      val stanceSpread = if (isBattle) 1 else 0
      fillRect(11 - stanceSpread, leftLegY, 4, 6, 11, 4)
      fillRect(17 + stanceSpread, rightLegY, 4, 6, 11, 4)
      // Boots with accent stripe
      fillRect(10 - stanceSpread, leftLegY + 5, 5, 2, 6, 5)
      fillRect(17 + stanceSpread, rightLegY + 5, 5, 2, 6, 5)
      put(12 - stanceSpread, leftLegY + 5, 9, 5)
      put(19 + stanceSpread, rightLegY + 5, 9, 5)
    }

    // 3. Torso / Outfit Silhouette
    val torsoX = if (isSide) 12 else 10
    val torsoW = if (isSide) 8 else 12
    fillRect(torsoX, 14, torsoW, 9, 5, 5)
    // Shade right half of torso for crisp cel depth
    fillRect(torsoX + torsoW - 3, 14, 3, 9, 6, 4)

    when (app.outfitStyle) {
      TrainerOutfitStyle.LAB_COAT -> {
        fillRect(torsoX, 14, torsoW, 11, 7, 5)
        fillRect(torsoX + torsoW - 2, 14, 2, 11, 8, 4)
        if (!isBack) fillRect(14, 15, 4, 8, 5, 6) // inner shirt
      }
      TrainerOutfitStyle.RIVAL_TRENCH -> {
        fillRect(torsoX - 1, 14, torsoW + 2, 10, 5, 5)
        if (!isBack) {
          fillRect(13, 14, 6, 3, 7, 6) // high collar
          fillRect(15, 16, 2, 7, 9, 6) // accent tie/zip
        }
      }
      TrainerOutfitStyle.SYNDICATE_UNIFORM -> {
        fillRect(torsoX, 14, torsoW, 9, 5, 5)
        fillRect(torsoX, 17, torsoW, 2, 7, 6) // monochrome belt stripe
        put(15, 16, 9, 7); put(16, 16, 9, 7) // Vex diamond insignia
      }
      TrainerOutfitStyle.RANGER_SUIT -> {
        fillRect(torsoX, 14, torsoW, 9, 5, 5)
        fillRect(torsoX + 1, 15, torsoW - 2, 3, 7, 6) // shoulder harness
        fillRect(torsoX, 20, torsoW, 2, 9, 6) // styler belt
      }
      TrainerOutfitStyle.GYM_ATHLETIC -> {
        fillRect(torsoX, 14, torsoW, 9, 5, 5)
        fillRect(torsoX + 2, 15, torsoW - 4, 7, 7, 6)
        fillRect(14, 17, 4, 3, 9, 7) // jersey number block
      }
      TrainerOutfitStyle.CHAMPION_CAPE -> {
        fillRect(torsoX, 14, torsoW, 9, 7, 5)
        fillRect(torsoX + 1, 14, torsoW - 2, 2, 9, 7) // gold mantle clasp
        fillRect(14, 16, 4, 6, 5, 6)
      }
      TrainerOutfitStyle.KIMONO_WARDEN -> {
        fillRect(torsoX - 1, 14, torsoW + 2, 10, 5, 5)
        fillRect(torsoX, 19, torsoW, 2, 9, 6) // obi sash
        if (!isBack) {
          put(14, 15, 7, 6); put(15, 16, 7, 6); put(16, 17, 7, 6)
        }
      }
      TrainerOutfitStyle.ADVENTURER_JACKET -> {
        fillRect(torsoX, 14, torsoW, 9, 5, 5)
        if (!isBack) {
          fillRect(14, 15, 4, 7, 7, 6) // inner tee
          fillRect(torsoX, 21, torsoW, 2, 9, 6) // belt
        } else {
          // Adventurer backpack on back view
          fillRect(12, 15, 8, 7, 9, 6)
          fillRect(13, 16, 6, 2, 10, 7)
        }
      }
    }

    // 4. Arms + Capsule Throw / Victory Pose
    when (pose) {
      TrainerPose.CAPSULE_THROW -> {
        // Left arm braced back
        fillRect(7, 15, 3, 5, 6, 4)
        fillRect(6, 19, 3, 2, 1, 5)
        // Right arm thrust forward holding glowing Prism Capsule
        fillRect(21, 13, 5, 3, 5, 6)
        fillRect(25, 12, 3, 3, 1, 6)
        // Prism Capsule (4x4 orb with crimson top + cyan core + white glint)
        fillRect(26, 9, 4, 2, 13, 7)
        fillRect(26, 11, 4, 2, 12, 7)
        put(27, 10, 14, 7); put(28, 10, 14, 7)
        put(27, 9, 12, 7)
      }
      TrainerPose.VICTORY -> {
        // Both arms raised high in triumph
        fillRect(7, 10, 3, 6, 5, 5)
        fillRect(7, 8, 3, 2, 1, 6)
        fillRect(22, 10, 3, 6, 5, 5)
        fillRect(22, 8, 3, 2, 1, 6)
        put(23, 7, 10, 7) // sparkle star above fist
      }
      TrainerPose.BATTLE_IDLE -> {
        fillRect(8, 15, 2, 6, 5, 5)
        fillRect(8, 20, 2, 2, 1, 5)
        fillRect(22, 14, 3, 5, 5, 6)
        fillRect(23, 18, 2, 2, 1, 6)
        // Capsule clipped on hand
        put(24, 18, 13, 7); put(24, 19, 12, 7)
      }
      else -> {
        if (isSide) {
          val armSwing = if (stride > 0) 1 else -1
          fillRect(14 + armSwing, 15, 3, 6, 6, 6)
          fillRect(14 + armSwing, 20, 3, 2, 1, 6)
        } else {
          fillRect(8, 15 + (if (stride > 0) 0 else 1), 2, 6, 5, 5)
          fillRect(8, 20 + (if (stride > 0) 0 else 1), 2, 2, 1, 5)
          fillRect(22, 15 + (if (stride < 0) 0 else 1), 2, 6, 5, 5)
          fillRect(22, 20 + (if (stride < 0) 0 else 1), 2, 2, 1, 5)
        }
      }
    }

    // 5. Head, Face, Hair & Expressions
    val headX = if (isSide) 12 else 11
    val headW = if (isSide) 8 else 10
    fillRect(headX, 6, headW, 8, 1, 6)
    // Jaw / cheek shadow
    fillRect(headX, 12, headW, 2, 2, 5)

    if (isBack) {
      // Full back hair coverage
      fillRect(headX - 1, 5, headW + 2, 9, 3, 6)
      fillRect(headX, 11, headW, 3, 4, 5)
    } else {
      // Hair crown & stylized bangs (6 hair silhouettes)
      fillRect(headX - 1, 4, headW + 2, 4, 3, 6)
      fillRect(headX - 1, 6, 2, 6, 4, 6)
      fillRect(headX + headW - 1, 6, 2, 6, 4, 6)
      when (app.hairStyleIndex % 6) {
        0 -> { // Spiky hero tufts
          put(11, 3, 3, 6); put(15, 3, 3, 6); put(19, 3, 3, 6)
          put(13, 7, 3, 7); put(17, 7, 3, 7)
        }
        1 -> { // Long flowing side locks
          fillRect(headX - 2, 7, 2, 9, 3, 5)
          fillRect(headX + headW, 7, 2, 9, 4, 5)
        }
        2 -> { // Swept rival bangs
          fillRect(headX, 6, 4, 3, 3, 7)
          put(headX + headW, 5, 3, 6); put(headX + headW + 1, 6, 4, 6)
        }
        3 -> { // Twin pony / starlight volume
          fillRect(headX - 3, 6, 2, 6, 3, 5)
          fillRect(headX + headW + 1, 6, 2, 6, 3, 5)
        }
        4 -> { // Ranger ponytail high crest
          fillRect(14, 3, 4, 2, 3, 6)
          fillRect(headX + headW, 5, 2, 7, 4, 5)
        }
        else -> { // Sovereign long silver/zenith mane
          fillRect(headX - 2, 5, 2, 11, 3, 5)
          fillRect(headX + headW, 5, 2, 11, 4, 5)
        }
      }

      // Expressive Eyes with white specular catchlight
      if (isSide) {
        val eyeX = if (isFacingLeft) headX + 1 else headX + headW - 3
        put(eyeX, 9, 0, 7); put(eyeX, 10, 0, 7)
        put(eyeX + (if (isFacingLeft) 1 else -1), 9, 12, 7)
      } else {
        // Left eye
        put(13, 9, 12, 7); put(13, 10, 0, 7); put(14, 9, 0, 7); put(14, 10, 0, 7)
        // Right eye
        put(17, 9, 12, 7); put(17, 10, 0, 7); put(18, 9, 0, 7); put(18, 10, 0, 7)
      }
    }

    // 6. Headgear Silhouette (Cap, Visor, Bandana, Crown, Hood, Glasses, Fedora, Ranger Helm)
    when (app.headgear) {
      TrainerHeadgear.CAP -> {
        fillRect(headX - 1, 3, headW + 2, 4, 5, 7)
        fillRect(headX + 2, 4, headW - 4, 2, 7, 7) // white front emblem
        val brimX = if (isFacingLeft) headX - 3 else headX - 1
        fillRect(brimX, 6, headW + 4, 1, 6, 7)
      }
      TrainerHeadgear.VISOR -> {
        fillRect(headX - 1, 4, headW + 2, 2, 5, 7)
        if (!isBack) {
          fillRect(headX, 8, headW, 2, 9, 7)
          put(headX + 2, 8, 10, 7); put(headX + headW - 2, 8, 10, 7)
        }
      }
      TrainerHeadgear.BANDANA -> {
        fillRect(headX - 1, 5, headW + 2, 2, 9, 7)
        put(headX + headW + 1, 6, 9, 6); put(headX + headW + 2, 7, 10, 6) // trailing knot
      }
      TrainerHeadgear.CROWN -> {
        fillRect(headX, 3, headW, 2, 9, 7)
        put(headX + 1, 2, 10, 7); put(15, 2, 10, 7); put(16, 2, 10, 7); put(headX + headW - 2, 2, 10, 7)
      }
      TrainerHeadgear.HOOD -> {
        fillRect(headX - 2, 3, headW + 4, 3, 5, 7)
        fillRect(headX - 2, 6, 2, 8, 6, 7)
        fillRect(headX + headW, 6, 2, 8, 6, 7)
      }
      TrainerHeadgear.GLASSES -> {
        if (!isBack && !isSide) {
          fillRect(12, 9, 3, 2, 9, 7)
          fillRect(17, 9, 3, 2, 9, 7)
          put(13, 9, 12, 7); put(18, 9, 12, 7)
          put(15, 9, 0, 7); put(16, 9, 0, 7)
        }
      }
      TrainerHeadgear.FEDORA -> {
        fillRect(headX - 3, 5, headW + 6, 2, 6, 7)
        fillRect(headX, 2, headW, 3, 5, 7)
        fillRect(headX, 4, headW, 1, 9, 7) // ribbon band
      }
      TrainerHeadgear.RANGER_HELM -> {
        fillRect(headX - 1, 3, headW + 2, 3, 5, 7)
        put(15, 2, 9, 7); put(16, 2, 9, 7) // ranger crest antenna
        fillRect(headX + 1, 5, headW - 2, 1, 10, 7)
      }
      TrainerHeadgear.NONE -> Unit
    }

    // 7. Flip horizontally for pure Left facing if needed so left/right are true mirrored profiles
    if (isFacingLeft) {
      for (y in 0 until h) {
        for (x in 0 until (w / 2)) {
          val lPos = y * w + x
          val rPos = y * w + (w - 1 - x)
          val tmpP = pixels[lPos]; pixels[lPos] = pixels[rPos]; pixels[rPos] = tmpP
          val tmpD = depth[lPos]; depth[lPos] = depth[rPos]; depth[rPos] = tmpD
        }
      }
    }

    // 8. Automatic Crisp 1px Hue-Shifted Ink Outline Pass (Shovel Knight+ Standard)
    val outlined = pixels.copyOf()
    val outlinedDepth = depth.copyOf()
    for (y in 1 until (h - 1)) {
      for (x in 1 until (w - 1)) {
        val pos = y * w + x
        if (pixels[pos] == -1) {
          val hasNeighbor = pixels[(y - 1) * w + x] >= 0 ||
            pixels[(y + 1) * w + x] >= 0 ||
            pixels[y * w + (x - 1)] >= 0 ||
            pixels[y * w + (x + 1)] >= 0
          if (hasNeighbor) {
            outlined[pos] = 0 // crisp 1px dark ink outline
            outlinedDepth[pos] = 2
          }
        }
      }
    }

    return TrainerSpriteMatrix(
      width = w,
      height = h,
      pixels = outlined,
      depthMap = outlinedDepth,
      paletteArgb = palette,
    )
  }

  // =========================================================================
  // PURE-JVM 40x40 HIGH-CONTRAST ANIME / GBA-DS DIALOGUE PORTRAIT GENERATOR
  // =========================================================================

  fun generateTrainerPortraitMatrix(spec: TrainerCharacterSpec): TrainerPortraitMatrix {
    val w = PORTRAIT_SIZE
    val h = PORTRAIT_SIZE
    val pixels = IntArray(w * h) { -1 }
    val basePalette = buildTrainerPalette(spec.appearance)
    // Add 3 backdrop gradient tones at indices 15..17
    val primary = parseHexArgb(spec.appearance.outfitPrimaryHex, 0xFFEF4444L)
    val accent = parseHexArgb(spec.appearance.accentHex, 0xFF06B6D4L)
    val palette = LongArray(18) { i ->
      when (i) {
        in 0..14 -> basePalette[i]
        15 -> shadeArgb(primary, 0.25f, blueShift = 28) // deep portrait backdrop
        16 -> shadeArgb(accent, 0.36f, blueShift = 22)  // diagonal speed-line stripe
        else -> shadeArgb(accent, 0.72f, blueShift = 18) // bright rim highlight
      }
    }

    fun put(x: Int, y: Int, idx: Int) {
      if (x in 0 until w && y in 0 until h) pixels[y * w + x] = idx
    }

    fun fillRect(x0: Int, y0: Int, rw: Int, rh: Int, idx: Int) {
      for (yy in y0 until (y0 + rh)) {
        for (xx in x0 until (x0 + rw)) put(xx, yy, idx)
      }
    }

    // 1. Dynamic diagonal GBA/DS anime portrait backdrop
    for (y in 0 until h) {
      for (x in 0 until w) {
        val diag = (x + y) % 12
        pixels[y * w + x] = when {
          diag in 0..1 -> 16
          y < 4 || y > h - 4 -> 15
          else -> 15
        }
      }
    }
    // Diagonal rim light slash behind head
    for (y in 4 until 32) {
      val sx = (32 - y).coerceIn(4, 34)
      put(sx, y, 17); put(sx + 1, y, 17)
    }

    val app = spec.appearance

    // 2. Back hair & Cape/Collar behind neck
    if (app.outfitStyle == TrainerOutfitStyle.CHAMPION_CAPE || app.outfitStyle == TrainerOutfitStyle.RIVAL_TRENCH) {
      fillRect(6, 27, 28, 13, 6)
      fillRect(9, 25, 22, 4, 5)
    }
    fillRect(10, 8, 20, 20, 4)

    // 3. Shoulders, Jacket / Lab Coat & Neck
    fillRect(6, 30, 28, 10, if (app.outfitStyle == TrainerOutfitStyle.LAB_COAT) 7 else 5)
    fillRect(24, 30, 10, 10, if (app.outfitStyle == TrainerOutfitStyle.LAB_COAT) 8 else 6)
    fillRect(15, 31, 10, 9, 7) // inner chest panel
    fillRect(18, 32, 4, 8, 9)  // tie / scarf / clasp accent
    fillRect(16, 25, 8, 6, 2)  // neck shadow
    fillRect(17, 25, 6, 4, 1)  // neck lit

    // 4. Sculpted Face Bust (oval jaw + warm skin shading)
    fillRect(12, 11, 16, 14, 1)
    fillRect(13, 24, 14, 2, 1)
    fillRect(15, 26, 10, 1, 2)
    // Right cheek cel shadow
    fillRect(25, 13, 3, 11, 2)
    fillRect(12, 12, 16, 2, 2) // bang cast shadow

    // 5. Expressive Anime/DS Eyes (iris colored with accent, crisp specular catchlight)
    // Left Eye (x=14..17, y=16..19)
    fillRect(14, 16, 4, 1, 0) // upper lash
    fillRect(14, 17, 4, 3, 12) // sclera
    fillRect(15, 17, 2, 3, 9)  // iris
    put(16, 18, 0)             // pupil
    put(15, 17, 12)            // specular catchlight
    // Right Eye (x=22..25, y=16..19)
    fillRect(22, 16, 4, 1, 0)
    fillRect(22, 17, 4, 3, 12)
    fillRect(23, 17, 2, 3, 9)
    put(24, 18, 0)
    put(23, 17, 12)
    // Eyebrows & Confident Smile / Determined Mouth
    fillRect(14, 14, 4, 1, 4)
    fillRect(22, 14, 4, 1, 4)
    put(20, 20, 2) // nose shadow
    if (spec.roleCategory == TrainerRoleCategory.VILLAIN_SYNDICATE) {
      fillRect(17, 23, 6, 1, 0) // stern smirk
      put(23, 22, 0)
    } else {
      fillRect(18, 23, 4, 1, 0) // confident smile
      put(17, 22, 2); put(22, 22, 2)
    }

    // 6. Stylized Hair Bangs & Side Locks
    fillRect(10, 7, 20, 6, 3)
    fillRect(9, 10, 3, 14, 3)
    fillRect(28, 10, 3, 14, 4)
    // Bang spikes across forehead
    fillRect(12, 11, 3, 3, 3)
    fillRect(17, 11, 4, 4, 3)
    fillRect(24, 11, 3, 3, 4)
    if (app.hairStyleIndex % 2 == 1) {
      fillRect(8, 14, 3, 16, 3)
      fillRect(29, 14, 3, 16, 4)
    }

    // 7. Portrait Headgear Detail
    when (app.headgear) {
      TrainerHeadgear.CAP -> {
        fillRect(9, 4, 22, 7, 5)
        fillRect(15, 6, 10, 3, 7)
        fillRect(7, 10, 26, 2, 6)
      }
      TrainerHeadgear.VISOR -> {
        fillRect(11, 15, 18, 5, 9)
        fillRect(13, 16, 6, 2, 10)
        fillRect(23, 16, 4, 1, 12)
      }
      TrainerHeadgear.BANDANA -> {
        fillRect(10, 9, 20, 3, 9)
        fillRect(18, 9, 4, 3, 10)
      }
      TrainerHeadgear.CROWN -> {
        fillRect(12, 4, 16, 4, 9)
        fillRect(13, 2, 3, 2, 10); fillRect(18, 1, 4, 3, 10); fillRect(24, 2, 3, 2, 10)
      }
      TrainerHeadgear.HOOD -> {
        fillRect(7, 4, 26, 6, 5)
        fillRect(7, 9, 4, 22, 6)
        fillRect(29, 9, 4, 22, 6)
      }
      TrainerHeadgear.GLASSES -> {
        fillRect(13, 16, 6, 4, 9); fillRect(14, 17, 4, 2, 12); put(16, 18, 0)
        fillRect(21, 16, 6, 4, 9); fillRect(22, 17, 4, 2, 12); put(24, 18, 0)
        fillRect(19, 17, 2, 1, 0)
      }
      TrainerHeadgear.FEDORA -> {
        fillRect(5, 9, 30, 2, 6)
        fillRect(11, 3, 18, 6, 5)
        fillRect(11, 7, 18, 2, 9)
      }
      TrainerHeadgear.RANGER_HELM -> {
        fillRect(9, 4, 22, 6, 5)
        fillRect(18, 2, 4, 3, 10)
        fillRect(12, 9, 16, 2, 9)
      }
      TrainerHeadgear.NONE -> Unit
    }

    // 8. Crisp 1px Ink Frame Border around Portrait
    for (x in 0 until w) {
      put(x, 0, 0); put(x, h - 1, 0)
    }
    for (y in 0 until h) {
      put(0, y, 0); put(w - 1, y, 0)
    }

    return TrainerPortraitMatrix(w, h, pixels, palette)
  }

  // =========================================================================
  // ANDROID BITMAP & 3D CEL-VOXEL CHARACTER MODEL RENDERERS (LRU CACHED)
  // =========================================================================

  private val bitmapCache = object : LinkedHashMap<String, Bitmap>(96, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 128
  }

  private fun appearanceKey(spec: TrainerCharacterSpec): String = with(spec.appearance) {
    "${spec.id}|$skinHex|$hairHex|$outfitPrimaryHex|$outfitSecondaryHex|$accentHex|${headgear.name}|${outfitStyle.name}|$hairStyleIndex"
  }

  fun renderTrainerSpriteBitmap(
    spec: TrainerCharacterSpec,
    pose: TrainerPose = TrainerPose.BATTLE_IDLE,
    walkFrameIndex: Int = 0,
    scale: Int = 4,
  ): Bitmap {
    val key = "spr|${appearanceKey(spec)}|${pose.name}|${walkFrameIndex % 2}|$scale"
    synchronized(bitmapCache) { bitmapCache[key]?.let { return it } }

    val matrix = generateTrainerSpriteMatrix(spec, pose, walkFrameIndex)
    val outW = matrix.width * scale
    val outH = matrix.height * scale
    val outPixels = IntArray(outW * outH)
    for (y in 0 until matrix.height) {
      for (x in 0 until matrix.width) {
        val idx = matrix.pixels[y * matrix.width + x]
        if (idx >= 0) {
          val argb = matrix.paletteArgb[idx].toInt()
          for (sy in 0 until scale) {
            val rowOff = (y * scale + sy) * outW + x * scale
            for (sx in 0 until scale) {
              outPixels[rowOff + sx] = argb
            }
          }
        }
      }
    }
    val bmp = Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
    synchronized(bitmapCache) { bitmapCache[key] = bmp }
    return bmp
  }

  /**
   * Renders a multi-slice extruded 3D Cel-Voxel Character Model from the Trainer's depth map,
   * with directional 3D rim lighting, cel-shaded depth extrusion, and crisp 1px silhouette outline.
   */
  fun renderTrainerCelVoxelBitmap(
    spec: TrainerCharacterSpec,
    pose: TrainerPose = TrainerPose.BATTLE_IDLE,
    yawDegrees: Float = 22f,
    scale: Int = 4,
  ): Bitmap {
    val quantYaw = ((yawDegrees / 5f).roundToInt() * 5)
    val key = "vox|${appearanceKey(spec)}|${pose.name}|$quantYaw|$scale"
    synchronized(bitmapCache) { bitmapCache[key]?.let { return it } }

    val matrix = generateTrainerSpriteMatrix(spec, pose, 0)
    val outW = matrix.width * scale
    val outH = matrix.height * scale
    val outPixels = IntArray(outW * outH)
    val rad = Math.toRadians(quantYaw.toDouble())
    val dxPerLayer = sin(rad).toFloat() * 1.15f
    val dyPerLayer = -0.35f * cos(rad).toFloat()
    val maxSlices = 7

    // Painter's algorithm: draw back-to-front voxel extrusion layers
    for (slice in 0 until maxSlices) {
      val depthRatio = slice.toFloat() / (maxSlices - 1).coerceAtLeast(1)
      val shadeFactor = 0.62f + depthRatio * 0.44f
      val offsetX = ((slice - maxSlices / 2f) * dxPerLayer * scale).roundToInt()
      val offsetY = ((slice - maxSlices / 2f) * dyPerLayer * scale).roundToInt()

      for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
          val pos = y * matrix.width + x
          val idx = matrix.pixels[pos]
          val d = matrix.depthMap[pos].toInt()
          if (idx >= 0 && slice <= d) {
            val baseArgb = matrix.paletteArgb[idx]
            // Crisp specular rim on frontmost face
            val litArgb = if (slice == d && idx != 0) {
              shadeArgb(baseArgb, (shadeFactor * 1.08f).coerceAtMost(1.25f)).toInt()
            } else if (idx == 0) {
              baseArgb.toInt()
            } else {
              shadeArgb(baseArgb, shadeFactor, blueShift = ((1f - depthRatio) * 14f).roundToInt()).toInt()
            }
            for (sy in 0 until scale) {
              val py = y * scale + sy + offsetY
              if (py in 0 until outH) {
                val rowOff = py * outW
                for (sx in 0 until scale) {
                  val px = x * scale + sx + offsetX
                  if (px in 0 until outW) {
                    outPixels[rowOff + px] = litArgb
                  }
                }
              }
            }
          }
        }
      }
    }

    val bmp = Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
    synchronized(bitmapCache) { bitmapCache[key] = bmp }
    return bmp
  }

  /**
   * Produces stacked multi-slice depth Bitmaps for true 3D Voxelizer-FX parallax rotation in Compose.
   */
  fun renderTrainerVoxelSlices(
    spec: TrainerCharacterSpec,
    pose: TrainerPose = TrainerPose.BATTLE_IDLE,
    sliceCount: Int = 7,
    scale: Int = 3,
  ): List<Bitmap> {
    val matrix = generateTrainerSpriteMatrix(spec, pose, 0)
    val outW = matrix.width * scale
    val outH = matrix.height * scale
    return (0 until sliceCount).map { slice ->
      val shade = 0.68f + (slice.toFloat() / (sliceCount - 1).coerceAtLeast(1)) * 0.36f
      val outPixels = IntArray(outW * outH)
      for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
          val pos = y * matrix.width + x
          val idx = matrix.pixels[pos]
          val d = matrix.depthMap[pos].toInt()
          if (idx >= 0 && slice <= d) {
            val argb = if (idx == 0) matrix.paletteArgb[0].toInt() else shadeArgb(matrix.paletteArgb[idx], shade).toInt()
            for (sy in 0 until scale) {
              val rowOff = (y * scale + sy) * outW + x * scale
              for (sx in 0 until scale) outPixels[rowOff + sx] = argb
            }
          }
        }
      }
      Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
    }
  }

  fun renderTrainerPortraitBitmap(spec: TrainerCharacterSpec, scale: Int = 4): Bitmap {
    val key = "port|${appearanceKey(spec)}|$scale"
    synchronized(bitmapCache) { bitmapCache[key]?.let { return it } }

    val matrix = generateTrainerPortraitMatrix(spec)
    val outW = matrix.width * scale
    val outH = matrix.height * scale
    val outPixels = IntArray(outW * outH)
    for (y in 0 until matrix.height) {
      for (x in 0 until matrix.width) {
        val idx = matrix.pixels[y * matrix.width + x]
        if (idx >= 0) {
          val argb = matrix.paletteArgb[idx].toInt()
          for (sy in 0 until scale) {
            val rowOff = (y * scale + sy) * outW + x * scale
            for (sx in 0 until scale) outPixels[rowOff + sx] = argb
          }
        }
      }
    }
    val bmp = Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
    synchronized(bitmapCache) { bitmapCache[key] = bmp }
    return bmp
  }
}

/**
 * Pure-JVM Tactical Multi-Creature Trainer Battle AI Engine.
 * Drives intelligent switching for 2-6 creature Trainer parties and Ace Gimmick activation.
 */
object TrainerBattleAiEngine {

  /**
   * Returns the next conscious party index when the current enemy creature faints,
   * preferring the creature with the strongest defensive matchup against the player's lead type.
   */
  fun chooseNextConsciousIndex(
    partyDexIds: List<Int>,
    currentIndex: Int,
    hpMap: Map<Int, Int>,
    playerPrimaryType: PokemonType,
    resolveTypes: (Int) -> Pair<PokemonType, PokemonType?>,
  ): Int? {
    val candidates = partyDexIds.indices.filter { idx ->
      if (idx == currentIndex) return@filter false
      val dex = partyDexIds[idx]
      (hpMap[dex] ?: 1) > 0
    }
    if (candidates.isEmpty()) return null
    // Keep the final slot (Trainer's Ace) for last when other non-Ace partners remain
    val nonAce = candidates.filter { it != partyDexIds.lastIndex }
    val pool = nonAce.ifEmpty { candidates }
    return pool.minByOrNull { idx ->
      val (t1, t2) = resolveTypes(partyDexIds[idx])
      CanonTypeChart.calculateEffectiveness(playerPrimaryType, t1, t2)
    }
  }

  /**
   * Evaluates whether a tactical AI Trainer should proactively switch mid-battle
   * when their active creature is weak (2x+) to the player's primary type and a healthy
   * bench partner resists (<= 0.5x) it.
   */
  fun chooseTacticalMidBattleSwitchIndex(
    tactic: TrainerAiTactic?,
    partyDexIds: List<Int>,
    currentIndex: Int,
    hpMap: Map<Int, Int>,
    turnNumber: Int,
    playerPrimaryType: PokemonType,
    resolveTypes: (Int) -> Pair<PokemonType, PokemonType?>,
  ): Int? {
    if (partyDexIds.size <= 1) return null
    val supportsSwitching = tactic == TrainerAiTactic.STATUS_TACTICIAN ||
      tactic == TrainerAiTactic.GIMMICK_ACE ||
      tactic == TrainerAiTactic.WEATHER_SETTER
    if (!supportsSwitching || turnNumber % 2 != 0) return null

    val currentDex = partyDexIds.getOrNull(currentIndex) ?: return null
    val (curT1, curT2) = resolveTypes(currentDex)
    val currentWeakness = CanonTypeChart.calculateEffectiveness(playerPrimaryType, curT1, curT2)
    if (currentWeakness < 2.0f) return null

    return partyDexIds.indices.firstOrNull { idx ->
      if (idx == currentIndex) return@firstOrNull false
      val dex = partyDexIds[idx]
      if ((hpMap[dex] ?: 1) <= 0) return@firstOrNull false
      val (candT1, candT2) = resolveTypes(dex)
      CanonTypeChart.calculateEffectiveness(playerPrimaryType, candT1, candT2) <= 0.5f
    }
  }

  /**
   * Activates a Trainer's signature Battle Gimmick (Mega / Z-Move / Dynamax / Terastallization)
   * on their Ace (last remaining conscious partner) or immediately for single-Ace boss battles.
   */
  fun resolveGimmickForEnemySlot(
    preferredGimmick: BattleGimmickMode,
    partyDexIds: List<Int>,
    activeIndex: Int,
    hpMap: Map<Int, Int>,
  ): BattleGimmickMode {
    if (preferredGimmick == BattleGimmickMode.NONE) return BattleGimmickMode.NONE
    val remainingCount = partyDexIds.count { dex -> (hpMap[dex] ?: 1) > 0 }
    return if (activeIndex == partyDexIds.lastIndex || remainingCount <= 1) {
      preferredGimmick
    } else {
      BattleGimmickMode.NONE
    }
  }
}

package com.example.data.local

import android.content.Context
import java.io.File

/**
 * 7 modular creature anatomy archetypes used by ProceduralCreatureRigEngine to
 * synthesize deterministic 32x32 2D pixel sprites and multi-slice 3D cel-voxel rigs offline.
 */
enum class CreatureAnatomyClass(val code: Int, val label: String) {
  QUADRUPED(0, "Quadruped"),
  AVIAN_WINGED(1, "Avian / Winged"),
  SERPENTINE_DRACONIC(2, "Serpentine / Draconic"),
  BIPEDAL_FIGHTER(3, "Bipedal Fighter"),
  AMORPHOUS_SPECTRAL(4, "Amorphous / Spectral"),
  CRYSTALLINE_MECHA(5, "Crystalline / Mecha"),
  AQUATIC(6, "Aquatic"),
  ;

  companion object {
    fun fromCode(code: Int): CreatureAnatomyClass =
      entries.firstOrNull { it.code == code } ?: QUADRUPED
  }
}

/**
 * Alternate resonance forms supported offline for regional, Mega-Resonance,
 * Paradox-Echo, and Prism-Awakened creature transformations.
 */
enum class CreatureVariantForm(
  val token: String,
  val displayLabel: String,
  val shortTag: String,
  val statMultiplier: Float,
  val hueShiftDegrees: Float,
) {
  STANDARD("STANDARD", "Standard", "Base", 1.0f, 0f),
  REGIONAL("REGIONAL", "Regional Variant", "Regional", 1.04f, 48f),
  MEGA_RESONANCE("MEGA_RESONANCE", "Mega-Resonance", "Mega", 1.18f, -26f),
  PARADOX_ECHO("PARADOX_ECHO", "Paradox-Echo", "Paradox", 1.14f, 132f),
  PRISM_AWAKENED("PRISM_AWAKENED", "Prism-Awakened", "Prism", 1.22f, 210f),
  ;

  companion object {
    fun fromToken(token: String?): CreatureVariantForm =
      entries.firstOrNull { it.token.equals(token?.trim(), ignoreCase = true) } ?: STANDARD
  }
}

/**
 * Complete offline clean-room species entry for the #0001-#1025 Resonance Atlas Dex.
 * Zero external sprite URLs required — all 2D pixel sprites and 3D cel-voxel models
 * render deterministically offline via ProceduralCreatureRigEngine.
 */
data class AtlasSpeciesIndexItem(
  val dexNumber: Int,
  val name: String,
  val epithet: String,
  val generation: String,
  val regionId: String,
  val regionName: String,
  val primaryType: String,
  val secondaryType: String,
  val resonanceRoleTier: String,
  val primaryHex: String,
  val secondaryHex: String,
  val anatomy: CreatureAnatomyClass,
  val familyId: Int,
  val evolvesToDex: Int?,
  val baseHp: Int,
  val baseAttack: Int,
  val baseDefense: Int,
  val baseSpAtk: Int,
  val baseSpDef: Int,
  val baseSpeed: Int,
  val signatureAbility: String,
  val signatureMove: String,
  val availableForms: List<CreatureVariantForm> = listOf(CreatureVariantForm.STANDARD),
) {
  val baseStatTotal: Int
    get() = baseHp + baseAttack + baseDefense + baseSpAtk + baseSpDef + baseSpeed

  fun displayNameForForm(form: CreatureVariantForm): String = when (form) {
    CreatureVariantForm.STANDARD -> name
    CreatureVariantForm.REGIONAL -> "$regionName $name"
    CreatureVariantForm.MEGA_RESONANCE -> "Mega $name"
    CreatureVariantForm.PARADOX_ECHO -> "Paradox $name"
    CreatureVariantForm.PRISM_AWAKENED -> "Prism $name"
  }

  fun statTotalForForm(form: CreatureVariantForm): Int =
    (baseStatTotal * form.statMultiplier).toInt()
}

object NationalAtlasIndex {
  const val TOTAL_NATIONAL_SPECIES = 1025
  private const val ASSET_FILE_NAME = "resonance_species_atlas.json"

  private val regionDisplayMap: Map<String, Pair<String, String>> = mapOf(
    "kanto" to ("Kanto" to "Gen I"),
    "johto" to ("Johto" to "Gen II"),
    "hoenn" to ("Hoenn" to "Gen III"),
    "sinnoh" to ("Sinnoh" to "Gen IV"),
    "hisui" to ("Hisui Rift" to "Gen VIII"),
    "unova" to ("Unova" to "Gen V"),
    "kalos" to ("Kalos" to "Gen VI"),
    "alola" to ("Alola" to "Gen VII"),
    "galar" to ("Galar" to "Gen VIII"),
    "kitakami" to ("Kitakami" to "Gen IX"),
    "paldea" to ("Paldea" to "Gen IX"),
    "blueberry_summit" to ("Blueberry Dome" to "Gen IX"),
    "sevii_isles" to ("Sevii Isles" to "Gen I"),
    "orange_archipelago" to ("Orange Archipelago" to "Gen I"),
    "orre" to ("Orre Wastes" to "Gen II"),
    "fiore_almia" to ("Fiore & Almia" to "Gen III"),
    "ransei" to ("Ransei Kingdoms" to "Gen V"),
    "ferrum" to ("Ferrum Arena" to "Gen IV"),
    "lental" to ("Lental Atolls" to "Gen VII"),
    "pasio" to ("Pasio Isle" to "Gen VIII"),
    "aeos" to ("Aeos Island" to "Gen VIII"),
    "mystery_rift" to ("Mystery Rift" to "Gen IX"),
  )

  private val starterIds = setOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9, 25, 133,
    152, 153, 154, 155, 156, 157, 158, 159, 160,
    252, 253, 254, 255, 256, 257, 258, 259, 260,
    387, 388, 389, 390, 391, 392, 393, 394, 395,
    495, 496, 497, 498, 499, 500, 501, 502, 503,
    650, 651, 652, 653, 654, 655, 656, 657, 658,
    722, 723, 724, 725, 726, 727, 728, 729, 730,
    810, 811, 812, 813, 814, 815, 816, 817, 818,
    906, 907, 908, 909, 910, 911, 912, 913, 914,
  )

  private val aceIds = setOf(
    59, 65, 68, 94, 130, 131, 143, 149,
    196, 197, 208, 212, 214, 229, 230, 248,
    282, 292, 306, 330, 350, 359, 373, 376,
    405, 445, 448, 461, 468, 475, 479,
    530, 553, 571, 609, 612, 635, 637,
    681, 700, 706, 715,
    745, 758, 768, 778, 784,
    823, 849, 861, 887, 900, 901,
    937, 959, 979, 983, 998, 1018,
  )

  private val paradoxAndUltraIds = (793..799).toSet() + (803..806).toSet() + (984..995).toSet() + setOf(
    1005, 1006, 1009, 1010, 1020, 1021, 1022, 1023,
  )

  private val legendaryAndMythicalIds = setOf(
    144, 145, 146, 150, 151,
    243, 244, 245, 249, 250, 251,
    377, 378, 379, 380, 381, 382, 383, 384, 385, 386,
    480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493,
    494, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649,
    716, 717, 718, 719, 720, 721,
    785, 786, 787, 788, 789, 790, 791, 792, 800, 801, 802, 807, 808, 809,
    888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 905,
    1001, 1002, 1003, 1004, 1007, 1008, 1014, 1015, 1016, 1017, 1024, 1025,
  )

  private val allTypes = listOf(
    "Grass", "Fire", "Water", "Electric", "Psychic", "Dragon",
    "Ghost", "Dark", "Steel", "Fairy", "Ice", "Fighting",
    "Ground", "Rock", "Bug", "Poison", "Flying", "Normal",
  )

  private val typePrefixes: Map<String, List<String>> = mapOf(
    "Grass" to listOf("Verdi", "Bramble", "Moss", "Sylva", "Fern", "Thorn", "Petal", "Clover", "Grove", "Sprout", "Lichen", "Willow", "Cedar", "Briar", "Bloom", "Canopy"),
    "Fire" to listOf("Cinder", "Pyro", "Ember", "Solar", "Blaze", "Soot", "Magma", "Ignis", "Scorch", "Flare", "Ashen", "Hearth", "Caldera", "Spark", "Kindle", "Brim"),
    "Water" to listOf("Tide", "Aqua", "Coral", "Dew", "Ripple", "Surf", "Brine", "Brook", "Foam", "Abyss", "Lagoon", "Spray", "Kelp", "Nautil", "Cascade", "Mist"),
    "Electric" to listOf("Volt", "Spark", "Arc", "Tesla", "Zap", "Pulse", "Jolt", "Surge", "Amp", "Static", "Blitz", "Ohm", "Ion", "Dynamo", "Flash", "Storm"),
    "Psychic" to listOf("Psy", "Astra", "Lumen", "Oracle", "Mind", "Espe", "Nova", "Aura", "Vesper", "Runic", "Dream", "Mirage", "Glyph", "Seer", "Nebula", "Prism"),
    "Dragon" to listOf("Draco", "Wyrm", "Skye", "Aether", "Scale", "Talon", "Zephyr", "Vortex", "Drake", "Apex", "Saur", "Krak", "Rift", "Hydra", "Strat", "Myth"),
    "Ghost" to listOf("Wisp", "Shade", "Gloom", "Specter", "Phantom", "Veil", "Haunt", "Dusk", "Wraith", "Eerie", "Hollow", "Umbra", "Shroud", "Hex", "Spirit", "Nether"),
    "Dark" to listOf("Nox", "Omen", "Obsid", "Shadow", "Grim", "Sable", "Eclipse", "Kuro", "Vex", "Dread", "Prowl", "Bane", "Night", "Murk", "Dusk", "Velvet"),
    "Steel" to listOf("Ferro", "Chrome", "Iron", "Aegis", "Alloy", "Forge", "Titan", "Cobalt", "Anvil", "Quark", "Plate", "Gear", "Rivet", "Kinet", "Silver", "Valk"),
    "Fairy" to listOf("Fae", "Pixi", "Gleam", "Ribbon", "Sylph", "Glimmer", "Star", "Charm", "Petal", "Lull", "Twink", "Dewy", "Nymph", "Gossam", "Velv", "Halo"),
    "Ice" to listOf("Cryo", "Frost", "Glaci", "Rime", "Hail", "Snow", "Flurry", "Boreal", "Sleet", "Chill", "Tundra", "Icicle", "Polar", "Shiver", "Crystal", "Alba"),
    "Fighting" to listOf("Brawl", "Valor", "Karat", "Fist", "Strike", "Vigor", "Grapple", "Champ", "Guard", "Blitz", "Mach", "Knuck", "Tackle", "Martial", "Stance", "Valkyr"),
    "Ground" to listOf("Terra", "Dune", "Silt", "Quake", "Loam", "Pebble", "Mesa", "Canyon", "Mud", "Clay", "Burrow", "Shale", "Tecton", "Grit", "Rumble", "Sand"),
    "Rock" to listOf("Granite", "Basalt", "Crag", "Boulder", "Quartz", "Onyx", "Flint", "Slate", "Spire", "Cairn", "Obsid", "Geode", "Pillar", "Rubble", "Fossil", "Stone"),
    "Bug" to listOf("Chitin", "Mantis", "Beetle", "Hive", "Silk", "Nectar", "Thorax", "Buzz", "Scarab", "Gnat", "Wasp", "Moth", "Larva", "Carap", "Swarm", "Pincer"),
    "Poison" to listOf("Venom", "Toxin", "Spore", "Sludge", "Blight", "Miasma", "Viper", "Acida", "Noxis", "Fume", "Scorp", "Ooze", "Plague", "Taint", "Pustul", "Hemlock"),
    "Flying" to listOf("Gale", "Aero", "Plume", "Talon", "Swoop", "Gust", "Zephy", "Cirrus", "Swift", "Feather", "Skylark", "Condor", "Soar", "Breeze", "Kestrel", "Pinion"),
    "Normal" to listOf("Fluff", "Puff", "Nimble", "Whisk", "Scamp", "Plush", "Cuddle", "Brisk", "Peb", "Snout", "Trot", "Burr", "Cozy", "Bound", "Spry", "Velvet"),
  )

  private val stageSuffixes = listOf(
    listOf("ling", "kit", "pup", "mote", "fin", "pip", "cub", "let", "bit", "bud", "nib", "wisp", "tot", "imp", "bean", "fry"),
    listOf("claw", "wing", "mane", "tail", "crest", "horn", "fang", "fin", "pelt", "shell", "spark", "thorn", "veil", "step", "beak", "hide"),
    listOf("wyrm", "titan", "saur", "goliath", "drake", "warden", "lord", "geist", "colossus", "sovereign", "hydra", "sentinel", "reaver", "striker", "monarch", "beast"),
  )

  private val typeAbilities: Map<String, List<String>> = mapOf(
    "Grass" to listOf("Verdant Surge", "Chlorophyll Bloom", "Leaf Guard", "Canopy Veil"),
    "Fire" to listOf("Solar Blaze", "Flash Ember", "Flame Body", "Hearth Core"),
    "Water" to listOf("Tidal Surge", "Swift Swim", "Water Veil", "Torrential Flow"),
    "Electric" to listOf("Volt Reflex", "Static Surge", "Lightning Rod", "Storm Conduit"),
    "Psychic" to listOf("Astral Mind", "Magic Bounce", "Synchronize", "Prismatic Lens"),
    "Dragon" to listOf("Scale Resonance", "Multiscale", "Air Lock", "Apex Presence"),
    "Ghost" to listOf("Cursed Veil", "Infiltrator", "Shadow Float", "Spectral Ward"),
    "Dark" to listOf("Eclipse Veil", "Intimidate", "Night Prowl", "Moxie Surge"),
    "Steel" to listOf("Mirror Armor", "Clear Body", "Sturdy Core", "Forged Plate"),
    "Fairy" to listOf("Pixilate", "Misty Veil", "Serene Grace", "Starlight Aura"),
    "Ice" to listOf("Frost Body", "Snow Cloak", "Thermal Guard", "Boreal Shell"),
    "Fighting" to listOf("Iron Fist", "Inner Focus", "Guts Surge", "No Guard"),
    "Ground" to listOf("Sand Stream", "Earth Force", "Tectonic Grip", "Dune Veil"),
    "Rock" to listOf("Rock Head", "Solid Core", "Sand Veil", "Monolith Guard"),
    "Bug" to listOf("Swarm Pulse", "Compound Eyes", "Tinted Lens", "Sharpness"),
    "Poison" to listOf("Venom Touch", "Corrosion", "Miasma Veil", "Liquid Ooze"),
    "Flying" to listOf("Gale Wings", "Keen Eye", "Aerilate", "Sky Stride"),
    "Normal" to listOf("Adaptability", "Technician", "Scrappy", "Harmonic Sync"),
  )

  private val typeMoves: Map<String, List<String>> = mapOf(
    "Grass" to listOf("Petal Tempest", "Verdant Lance", "Briar Whip", "Solar Canopy"),
    "Fire" to listOf("Flare Burst", "Inferno Dive", "Caldera Wave", "Ember Vortex"),
    "Water" to listOf("Hydro Surge", "Riptide Pulse", "Abyssal Crash", "Foam Jet"),
    "Electric" to listOf("Volt Overdrive", "Arc Discharge", "Thunder Nova", "Plasma Spark"),
    "Psychic" to listOf("Astral Pulse", "Psionic Shatter", "Prism Ray", "Dream Rift"),
    "Dragon" to listOf("Draco Resonance", "Sky Rush", "Scale Tempest", "Wyrm Fang"),
    "Ghost" to listOf("Spectral Shift", "Shadow Surge", "Phantom Wail", "Soul Lantern"),
    "Dark" to listOf("Eclipse Slash", "Night Daze", "Void Crunch", "Sable Ambush"),
    "Steel" to listOf("Chrome Impact", "Meteor Slam", "Alloy Edge", "Bastion Crash"),
    "Fairy" to listOf("Prism Gleam", "Moonlight Rift", "Starlight Ribbon", "Sylph Kiss"),
    "Ice" to listOf("Glacial Lance", "Aurora Beam", "Boreal Storm", "Rime Shatter"),
    "Fighting" to listOf("Aura Strike", "Dynamic Barrage", "Valor Kick", "Breaker Palm"),
    "Ground" to listOf("Tectonic Rift", "Dune Quake", "Sandstone Crash", "Canyon Surge"),
    "Rock" to listOf("Granite Crash", "Obsidian Edge", "Meteor Boulder", "Spire Crush"),
    "Bug" to listOf("Chitin Swarm", "Pincer Slash", "Nectar Drain", "Silk Tempest"),
    "Poison" to listOf("Venom Nova", "Miasma Chain", "Spore Cloud", "Acid Fang"),
    "Flying" to listOf("Gale Dive", "Sky Slash", "Zephyr Gust", "Horizon Plunge"),
    "Normal" to listOf("Hyper Resonance", "Swift Strike", "Sonic Dash", "Prism Slam"),
  )

  @Volatile
  private var loadedAtlasSpecies: List<AtlasSpeciesIndexItem>? = null

  private val fallbackAtlasSpecies: List<AtlasSpeciesIndexItem> by lazy {
    // Try loading bundled JSON directly from local filesystem in JVM unit tests if available
    val localCandidates = listOf(
      File("app/src/main/assets/$ASSET_FILE_NAME"),
      File("src/main/assets/$ASSET_FILE_NAME"),
    )
    for (file in localCandidates) {
      if (file.isFile) {
        val parsed = runCatching { parseAtlasJson(file.readText(Charsets.UTF_8)) }.getOrNull()
        if (parsed != null && parsed.size == TOTAL_NATIONAL_SPECIES) {
          return@lazy parsed
        }
      }
    }
    buildDeterministicFallbackCatalog()
  }

  /**
   * Loads the compact offline 1,025-species (+ alternate forms) atlas manifest from Android assets.
   * Safe to call multiple times; caches parsed species in memory.
   */
  fun initializeFromAssets(context: Context): List<AtlasSpeciesIndexItem> {
    loadedAtlasSpecies?.let { return it }
    val parsed = runCatching {
      context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
        parseAtlasJson(reader.readText())
      }
    }.getOrNull()
    if (parsed != null && parsed.size == TOTAL_NATIONAL_SPECIES) {
      loadedAtlasSpecies = parsed
      return parsed
    }
    return fallbackAtlasSpecies
  }

  /**
   * Parses raw `resonance_species_atlas.json` text into strongly typed `AtlasSpeciesIndexItem` rows
   * using a pure-Kotlin JSON reader (zero Android org.json stub issues in pure JVM unit tests).
   */
  @Suppress("UNCHECKED_CAST")
  fun parseAtlasJson(rawJson: String): List<AtlasSpeciesIndexItem> {
    val root = MinimalJsonParser(rawJson).parseValue() as? Map<String, Any?> ?: return emptyList()
    val arr = root["species"] as? List<Any?> ?: return emptyList()
    val result = ArrayList<AtlasSpeciesIndexItem>(arr.size)
    for (entry in arr) {
      val obj = entry as? Map<String, Any?> ?: continue
      val dex = (obj["d"] as? Number)?.toInt() ?: continue
      val regId = obj["r"] as? String ?: "kanto"
      val (regName, defaultGen) = regionDisplayMap[regId] ?: ("Kanto" to "Gen I")
      val gen = (obj["g"] as? String)?.takeIf { it.isNotBlank() } ?: defaultGen
      val t1 = obj["t1"] as? String ?: "Normal"
      val t2 = obj["t2"] as? String ?: ""
      val stList = (obj["st"] as? List<Any?>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
      val forms = buildList {
        add(CreatureVariantForm.STANDARD)
        val fmList = obj["fm"] as? List<Any?>
        fmList?.forEach { rawForm ->
          val form = CreatureVariantForm.fromToken(rawForm as? String)
          if (form != CreatureVariantForm.STANDARD && form !in this) {
            add(form)
          }
        }
      }
      result.add(
        AtlasSpeciesIndexItem(
          dexNumber = dex,
          name = obj["n"] as? String ?: "Resonant$dex",
          epithet = obj["e"] as? String ?: "Resonance Kin",
          generation = gen,
          regionId = regId,
          regionName = regName,
          primaryType = t1,
          secondaryType = t2,
          resonanceRoleTier = obj["rt"] as? String ?: roleTierForDex(dex),
          primaryHex = (obj["c1"] as? String)?.ifBlank { hexForType(t1) } ?: hexForType(t1),
          secondaryHex = (obj["c2"] as? String)?.ifBlank { secondaryHexForType(t1, t2) } ?: secondaryHexForType(t1, t2),
          anatomy = CreatureAnatomyClass.fromCode((obj["an"] as? Number)?.toInt() ?: 0),
          familyId = (obj["fam"] as? Number)?.toInt() ?: dex,
          evolvesToDex = (obj["evo"] as? Number)?.toInt(),
          baseHp = stList.getOrElse(0) { 75 },
          baseAttack = stList.getOrElse(1) { 80 },
          baseDefense = stList.getOrElse(2) { 75 },
          baseSpAtk = stList.getOrElse(3) { 80 },
          baseSpDef = stList.getOrElse(4) { 75 },
          baseSpeed = stList.getOrElse(5) { 78 },
          signatureAbility = (obj["ab"] as? String)?.ifBlank { "Harmonic Sync" } ?: "Harmonic Sync",
          signatureMove = (obj["mv"] as? String)?.ifBlank { "Resonance Strike" } ?: "Resonance Strike",
          availableForms = forms,
        )
      )
    }
    return result.sortedBy { it.dexNumber }
  }

  private class MinimalJsonParser(private val src: String) {
    private var pos = 0

    fun parseValue(): Any? {
      skipWs()
      if (pos >= src.length) return null
      return when (src[pos]) {
        '{' -> parseObject()
        '[' -> parseArray()
        '"' -> parseString()
        't' -> { pos += 4; true }
        'f' -> { pos += 5; false }
        'n' -> { pos += 4; null }
        else -> parseNumber()
      }
    }

    private fun parseObject(): Map<String, Any?> {
      pos++ // '{'
      val map = LinkedHashMap<String, Any?>()
      skipWs()
      if (pos < src.length && src[pos] == '}') { pos++; return map }
      while (pos < src.length) {
        skipWs()
        val key = parseString()
        skipWs()
        if (pos < src.length && src[pos] == ':') pos++
        val value = parseValue()
        map[key] = value
        skipWs()
        if (pos < src.length && src[pos] == ',') {
          pos++
        } else {
          if (pos < src.length && src[pos] == '}') pos++
          break
        }
      }
      return map
    }

    private fun parseArray(): List<Any?> {
      pos++ // '['
      val list = ArrayList<Any?>()
      skipWs()
      if (pos < src.length && src[pos] == ']') { pos++; return list }
      while (pos < src.length) {
        list.add(parseValue())
        skipWs()
        if (pos < src.length && src[pos] == ',') {
          pos++
        } else {
          if (pos < src.length && src[pos] == ']') pos++
          break
        }
      }
      return list
    }

    private fun parseString(): String {
      pos++ // '"'
      val sb = StringBuilder()
      while (pos < src.length) {
        val c = src[pos++]
        if (c == '"') break
        if (c == '\\' && pos < src.length) {
          when (val esc = src[pos++]) {
            '"', '\\', '/' -> sb.append(esc)
            'n' -> sb.append('\n')
            'r' -> sb.append('\r')
            't' -> sb.append('\t')
            'u' -> if (pos + 4 <= src.length) {
              val hex = src.substring(pos, pos + 4)
              pos += 4
              hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
            }
            else -> sb.append(esc)
          }
        } else {
          sb.append(c)
        }
      }
      return sb.toString()
    }

    private fun parseNumber(): Number {
      val start = pos
      if (pos < src.length && src[pos] == '-') pos++
      var isFloat = false
      while (pos < src.length) {
        val c = src[pos]
        if (c in '0'..'9') {
          pos++
        } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
          isFloat = true
          pos++
        } else {
          break
        }
      }
      val token = src.substring(start, pos)
      return if (isFloat) token.toDoubleOrNull() ?: 0.0 else token.toLongOrNull() ?: 0L
    }

    private fun skipWs() {
      while (pos < src.length && src[pos] <= ' ') pos++
    }
  }

  fun baseCatalog(): List<AtlasSpeciesIndexItem> = loadedAtlasSpecies ?: fallbackAtlasSpecies

  fun totalPlayableFormsCount(): Int = baseCatalog().sumOf { it.availableForms.size }

  fun regionForDex(dexNumber: Int): Triple<String, String, String> {
    val item = peekSpecies(dexNumber)
    return Triple(item.generation, item.regionId, item.regionName)
  }

  fun roleTierForDex(dexNumber: Int): String = when {
    dexNumber in starterIds -> "Starter"
    dexNumber in legendaryAndMythicalIds -> "Legendary / Mythical"
    dexNumber in paradoxAndUltraIds -> "Paradox / Ultra"
    dexNumber in aceIds -> "Regional Ace"
    else -> "Wild Resonance"
  }

  fun hexForType(typeName: String): String = when (typeName.lowercase()) {
    "fire" -> "#FF5722"
    "water" -> "#00B4D8"
    "grass" -> "#10B981"
    "electric" -> "#FFD166"
    "psychic" -> "#FF2A85"
    "dragon" -> "#6366F1"
    "ghost" -> "#A855F7"
    "dark" -> "#475569"
    "steel" -> "#94A3B8"
    "fairy" -> "#F472B6"
    "ice" -> "#38BDF8"
    "fighting" -> "#EF4444"
    "ground" -> "#D97706"
    "rock" -> "#B45309"
    "bug" -> "#84CC16"
    "poison" -> "#C084FC"
    "flying" -> "#60A5FA"
    else -> "#00F5D4"
  }

  fun secondaryHexForType(primaryType: String, secondaryType: String): String {
    if (secondaryType.isNotBlank()) return hexForType(secondaryType)
    return when (primaryType.lowercase()) {
      "fire" -> "#FACC15"
      "water" -> "#38BDF8"
      "grass" -> "#84CC16"
      "electric" -> "#FF7A00"
      "psychic" -> "#00F5D4"
      "dragon" -> "#F43F5E"
      "ghost" -> "#38BDF8"
      "dark" -> "#A855F7"
      "steel" -> "#00F5D4"
      "fairy" -> "#FFD166"
      "ice" -> "#818CF8"
      "fighting" -> "#F59E0B"
      "ground" -> "#FDE047"
      "rock" -> "#F97316"
      "bug" -> "#10B981"
      "poison" -> "#EC4899"
      "flying" -> "#F8FAFC"
      else -> "#FF2A85"
    }
  }

  /** Fast O(1) offline lookup for any #0001-#1025 species. */
  fun peekSpecies(dexNumber: Int): AtlasSpeciesIndexItem {
    val catalog = baseCatalog()
    val safe = dexNumber.coerceIn(1, catalog.size)
    return catalog[safe - 1]
  }

  /** Returns all species belonging to the same evolutionary family. */
  fun evolutionFamilyFor(dexNumber: Int): List<AtlasSpeciesIndexItem> {
    val target = peekSpecies(dexNumber)
    return baseCatalog().filter { it.familyId == target.familyId }.sortedBy { it.dexNumber }
  }

  /**
   * Returns the complete multi-source habitat pool for each of the 22 playable fusion regions.
   * The union across all 22 regions covers 100% of the National Index (#0001-#1025).
   */
  fun dexPoolForRegion(regionId: String): List<Int> {
    val nativeHomeIds = baseCatalog().filter { it.regionId == regionId }.map { it.dexNumber }
    val fusionBonus = when (regionId) {
      "kanto" -> (1..151).toList()
      "johto" -> (152..251).toList()
      "hoenn" -> (252..386).toList()
      "sinnoh" -> (387..493).toList()
      "hisui" -> (899..905).toList() + listOf(155, 157, 215, 234, 448, 483, 484, 487, 491, 493, 501, 503, 571, 722, 724)
      "unova" -> (494..649).toList()
      "kalos" -> (650..721).toList()
      "alola" -> (722..809).toList()
      "galar" -> (810..898).toList()
      "kitakami" -> (1011..1017).toList() + listOf(25, 37, 58, 196, 197, 254, 350, 448, 609, 778, 901, 908, 937)
      "paldea" -> (906..1010).toList()
      "blueberry_summit" -> (1018..1025).toList() + (1..9).toList() + listOf(
        152, 155, 158, 252, 255, 258, 387, 390, 393, 495, 498, 501, 650, 653, 656, 722, 725, 728, 810, 813, 816, 906, 909, 912,
      )
      "sevii_isles" -> (131..151).toList() + (201..251).toList() + listOf(25, 38, 59, 130, 386)
      "orange_archipelago" -> listOf(6, 7, 9, 25, 95, 116, 121, 130, 131, 143, 149, 175, 183, 249, 350, 728, 730, 785) + (116..136).toList()
      "orre" -> listOf(196, 197, 243, 244, 245, 248, 249, 282, 330, 373, 376, 384, 445, 571, 635) + (196..251).toList()
      "fiore_almia" -> listOf(25, 133, 252, 254, 311, 312, 380, 381, 448, 488, 490, 491, 492) + (311..386).toList()
      "ransei" -> listOf(38, 133, 134, 135, 136, 196, 197, 212, 376, 384, 445, 448, 493, 571, 609, 635, 637, 644, 681, 983) + (494..540).toList()
      "ferrum" -> listOf(6, 25, 68, 94, 150, 212, 245, 257, 282, 392, 445, 448, 461, 491, 609, 681, 724, 937) + (440..493).toList()
      "lental" -> listOf(1, 25, 131, 149, 249, 250, 282, 350, 384, 492, 637, 716, 722, 730, 778, 823, 887) + (722..792).toList()
      "pasio" -> listOf(3, 6, 9, 25, 149, 150, 248, 250, 257, 376, 384, 445, 448, 637, 643, 644, 658, 700, 791, 792, 888, 908, 1007, 1008) + (1..1025 step 13).toList()
      "aeos" -> listOf(3, 6, 9, 25, 94, 131, 143, 149, 150, 212, 248, 282, 376, 384, 445, 448, 658, 681, 700, 724, 778, 815, 887, 888, 908, 937, 959, 1008) + (810..882).toList()
      "mystery_rift" -> (793..809).toList() + (984..1010).toList() + listOf(150, 151, 251, 384, 385, 386, 483, 484, 487, 491, 493, 646, 718, 890, 1024, 1025)
      else -> (1..TOTAL_NATIONAL_SPECIES).toList()
    }
    return (nativeHomeIds + fusionBonus).distinct()
  }

  /**
   * Builds the full #0001-#1025 offline catalog instantly in memory.
   * Preserves user-modified Room edits (custom palettes/stats/names) when `isUserModified = true`.
   */
  fun buildCompleteIndex(hydratedByDex: Map<Int, NativePokemonEntity> = emptyMap()): List<AtlasSpeciesIndexItem> {
    val catalog = baseCatalog()
    if (hydratedByDex.isEmpty()) return catalog
    return catalog.map { base ->
      val hydrated = hydratedByDex[base.dexNumber]
      if (hydrated != null && hydrated.isUserModified) {
        base.copy(
          name = hydrated.name.ifBlank { base.name },
          primaryType = hydrated.primaryType.ifBlank { base.primaryType },
          secondaryType = hydrated.secondaryType,
          primaryHex = hydrated.primaryHex.ifBlank { base.primaryHex },
          secondaryHex = hydrated.secondaryHex.ifBlank { base.secondaryHex },
          baseHp = hydrated.baseHp,
          baseAttack = hydrated.baseAttack,
          baseDefense = hydrated.baseDefense,
          baseSpAtk = hydrated.baseSpAtk,
          baseSpDef = hydrated.baseSpDef,
          baseSpeed = hydrated.baseSpeed,
          signatureAbility = hydrated.abilityPrimary.ifBlank { base.signatureAbility },
          signatureMove = hydrated.signatureMove.ifBlank { base.signatureMove },
        )
      } else {
        base
      }
    }
  }

  private fun fallbackRegionForDex(dex: Int): Triple<String, String, String> = when (dex) {
    in 1..115 -> Triple("kanto", "Kanto", "Gen I")
    in 116..136 -> Triple("orange_archipelago", "Orange Archipelago", "Gen I")
    in 137..151 -> Triple("sevii_isles", "Sevii Isles", "Gen I")
    in 152..220 -> Triple("johto", "Johto", "Gen II")
    in 221..251 -> Triple("orre", "Orre Wastes", "Gen II")
    in 252..345 -> Triple("hoenn", "Hoenn", "Gen III")
    in 346..386 -> Triple("fiore_almia", "Fiore & Almia", "Gen III")
    in 387..470 -> Triple("sinnoh", "Sinnoh", "Gen IV")
    in 471..493 -> Triple("ferrum", "Ferrum Arena", "Gen IV")
    in 494..599 -> Triple("unova", "Unova", "Gen V")
    in 600..649 -> Triple("ransei", "Ransei Kingdoms", "Gen V")
    in 650..721 -> Triple("kalos", "Kalos", "Gen VI")
    in 722..775 -> Triple("alola", "Alola", "Gen VII")
    in 776..792 -> Triple("lental", "Lental Atolls", "Gen VII")
    in 793..809 -> Triple("mystery_rift", "Mystery Rift", "Gen VII")
    in 810..864 -> Triple("galar", "Galar", "Gen VIII")
    in 865..882 -> Triple("aeos", "Aeos Island", "Gen VIII")
    in 883..898 -> Triple("pasio", "Pasio Isle", "Gen VIII")
    in 899..905 -> Triple("hisui", "Hisui Rift", "Gen VIII")
    in 906..983 -> Triple("paldea", "Paldea", "Gen IX")
    in 984..1010 -> Triple("mystery_rift", "Mystery Rift", "Gen IX")
    in 1011..1017 -> Triple("kitakami", "Kitakami", "Gen IX")
    else -> Triple("blueberry_summit", "Blueberry Dome", "Gen IX")
  }

  /**
   * Pure-Kotlin deterministic fallback catalog (used only if asset JSON is absent).
   * Guarantees 1,025 unique, non-placeholder entries across all 22 regions.
   */
  private fun buildDeterministicFallbackCatalog(): List<AtlasSpeciesIndexItem> {
    val usedNames = HashSet<String>(TOTAL_NATIONAL_SPECIES * 2)
    val result = ArrayList<AtlasSpeciesIndexItem>(TOTAL_NATIONAL_SPECIES)
    for (dex in 1..TOTAL_NATIONAL_SPECIES) {
      val (regId, regName, gen) = fallbackRegionForDex(dex)
      val role = roleTierForDex(dex)
      val familyId = ((dex - 1) / 3) + 1
      val stage = (dex - 1) % 3
      val evolvesTo = if (role != "Legendary / Mythical" && role != "Paradox / Ultra" && stage < 2 && dex < TOTAL_NATIONAL_SPECIES) dex + 1 else null
      val t1 = allTypes[(familyId * 7 + dex * 3) % allTypes.size]
      val t2Cand = if (familyId % 3 != 0 && stage > 0) allTypes[(familyId * 11 + dex * 5) % allTypes.size] else ""
      val t2 = if (t2Cand == t1) "" else t2Cand
      val anatomy = CreatureAnatomyClass.fromCode((familyId * 5 + dex) % CreatureAnatomyClass.entries.size)
      val prefixes = typePrefixes[t1] ?: listOf("Prism")
      val suffixes = stageSuffixes[stage.coerceIn(0, 2)]

      var attempt = 0
      var uniqueName: String
      while (true) {
        val p = prefixes[(familyId * 5 + dex + attempt * 3) % prefixes.size]
        val s = suffixes[(familyId * 3 + stage * 5 + attempt) % suffixes.size]
        val cand = if (attempt < 16) {
          if (p.last().lowercaseChar() == s.first().lowercaseChar()) p + s.drop(1) else p + s
        } else {
          val alt = (typePrefixes[allTypes[(dex + attempt) % allTypes.size]] ?: listOf("Aether"))[(attempt + dex) % 16]
          alt.take(4) + p.lowercase().take(3) + s
        }
        if (usedNames.add(cand)) {
          uniqueName = cand
          break
        }
        attempt++
      }

      val mult = when (role) {
        "Legendary / Mythical" -> 1.32f
        "Paradox / Ultra" -> 1.22f
        "Regional Ace" -> 1.14f
        "Starter" -> if (stage == 0) 0.72f else if (stage == 1) 0.90f else 1.12f
        else -> if (stage == 0) 0.68f else if (stage == 1) 0.88f else 1.06f
      }
      fun stat(base: Int, salt: Int): Int =
        ((base * mult).toInt() + ((dex * 13 + salt) % 9) - 4).coerceIn(30, 180)

      val forms = buildList {
        add(CreatureVariantForm.STANDARD)
        when {
          role == "Legendary / Mythical" -> add(CreatureVariantForm.PRISM_AWAKENED)
          role == "Paradox / Ultra" -> add(CreatureVariantForm.PARADOX_ECHO)
          role == "Regional Ace" && dex % 2 == 0 -> add(CreatureVariantForm.MEGA_RESONANCE)
          dex % 29 == 0 -> add(CreatureVariantForm.REGIONAL)
        }
      }

      result.add(
        AtlasSpeciesIndexItem(
          dexNumber = dex,
          name = uniqueName,
          epithet = "$t1 ${anatomy.label.substringBefore(" /")} Kin",
          generation = gen,
          regionId = regId,
          regionName = regName,
          primaryType = t1,
          secondaryType = t2,
          resonanceRoleTier = role,
          primaryHex = hexForType(t1),
          secondaryHex = secondaryHexForType(t1, t2),
          anatomy = anatomy,
          familyId = familyId,
          evolvesToDex = evolvesTo,
          baseHp = stat(80, 1),
          baseAttack = stat(84, 2),
          baseDefense = stat(78, 3),
          baseSpAtk = stat(84, 4),
          baseSpDef = stat(78, 5),
          baseSpeed = stat(82, 6),
          signatureAbility = (typeAbilities[t1] ?: listOf("Harmonic Sync"))[dex % 4],
          signatureMove = (typeMoves[t1] ?: listOf("Resonance Strike"))[dex % 4],
          availableForms = forms,
        )
      )
    }
    return result
  }
}

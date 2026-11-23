package com.example.ui

import com.example.data.local.AtlasSpeciesIndexItem
import com.example.data.local.HardwareProfileEntity
import com.example.data.local.NationalAtlasIndex
import com.example.data.local.NativePokemonEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ExtraVisualAudioSettings
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.CombatantSnapshot
import com.example.engine.HardwarePlatformSpec
import com.example.engine.MoveCategory
import com.example.engine.PokemonType
import com.example.engine.StatStages
import com.example.engine.StatusCondition
import org.json.JSONObject

object StudioViewModelHelpers {
  private val atlasIndexByDex: Map<Int, AtlasSpeciesIndexItem> by lazy {
    NationalAtlasIndex.buildCompleteIndex().associateBy { it.dexNumber }
  }

  fun parsePokemonType(raw: String): PokemonType =
    PokemonType.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PokemonType.NORMAL

  /**
   * Clean-room synthesis of any #0001-#1025 National Dex species from NationalAtlasIndex so
   * `findPokemonByDex(dexNumber)` NEVER falls back to Bulbasaur for uncached encounters.
   */
  fun synthesizePokemonFromAtlas(dexNumber: Int): NativePokemonEntity {
    val safeDex = dexNumber.coerceIn(1, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES)
    val item = atlasIndexByDex[safeDex]
    val (gen, _, regionName) = NationalAtlasIndex.regionForDex(safeDex)
    val name = item?.name ?: "Species #${safeDex.toString().padStart(4, '0')}"
    val type1 = item?.primaryType ?: "Normal"
    val type2 = item?.secondaryType.orEmpty()
    val roleTier = item?.resonanceRoleTier ?: NationalAtlasIndex.roleTierForDex(safeDex)
    val primaryHex = item?.primaryHex ?: NationalAtlasIndex.hexForType(type1)
    val secondaryHex = if (type2.isNotBlank()) NationalAtlasIndex.hexForType(type2) else "#00F5D4"

    val stats = when (roleTier) {
      "Legendary / Mythical" -> intArrayOf(102, 114, 96, 118, 98, 102)
      "Paradox / Ultra" -> intArrayOf(94, 112, 92, 108, 90, 102)
      "Regional Ace" -> intArrayOf(88, 104, 86, 100, 86, 96)
      "Starter" -> intArrayOf(80, 88, 82, 92, 84, 86)
      else -> intArrayOf(74, 82, 74, 82, 74, 78)
    }
    val hp = stats[0]
    val atk = stats[1]
    val def = stats[2]
    val spa = stats[3]
    val spd = stats[4]
    val spe = stats[5]

    val signatureMove = when (parsePokemonType(type1)) {
      PokemonType.FIRE -> "Flare Burst"
      PokemonType.WATER -> "Hydro Surge"
      PokemonType.GRASS -> "Verdant Lance"
      PokemonType.ELECTRIC -> "Volt Overdrive"
      PokemonType.PSYCHIC -> "Astral Pulse"
      PokemonType.DRAGON -> "Draco Resonance"
      PokemonType.GHOST -> "Spectral Shift"
      PokemonType.DARK -> "Eclipse Slash"
      PokemonType.STEEL -> "Chrome Impact"
      PokemonType.FAIRY -> "Prism Gleam"
      PokemonType.ICE -> "Glacial Lance"
      PokemonType.FIGHTING -> "Aura Strike"
      PokemonType.GROUND -> "Tectonic Rift"
      PokemonType.ROCK -> "Granite Crash"
      PokemonType.BUG -> "Chitin Swarm"
      PokemonType.POISON -> "Venom Nova"
      PokemonType.FLYING -> "Gale Dive"
      PokemonType.NORMAL -> "Hyper Resonance"
    }

    return NativePokemonEntity(
      dexNumber = safeDex,
      name = name,
      generation = item?.generation ?: gen,
      region = item?.regionName ?: regionName,
      primaryType = type1,
      secondaryType = type2,
      consoleOriginSystem = "Omni-Era Resonance Atlas ($gen)",
      recommendedChipAddon = "All 156 Hardware Unified",
      abilityPrimary = "Harmonic Sync",
      abilityHidden = "Prism Guard",
      signatureMove = signatureMove,
      loreSummary = "$name ($roleTier) from $regionName, synthesized cleanly from the Resonance Atlas Dex.",
      baseHp = hp,
      baseAttack = atk,
      baseDefense = def,
      baseSpAtk = spa,
      baseSpDef = spd,
      baseSpeed = spe,
      primaryHex = primaryHex,
      secondaryHex = secondaryHex,
      isUserModified = false,
    )
  }

  fun buildCombatantFromEntity(
    entity: NativePokemonEntity,
    level: Int = 50,
    gimmick: BattleGimmickMode = BattleGimmickMode.NONE,
    teraType: PokemonType? = null,
    status: StatusCondition = StatusCondition.HEALTHY,
    heldItemId: String = "",
    stages: StatStages = StatStages(),
    bondGauge: Int = 100,
  ): CombatantSnapshot = CombatantSnapshot(
    dexNumber = entity.dexNumber,
    name = entity.name,
    level = level.coerceIn(1, 100),
    primaryType = parsePokemonType(entity.primaryType),
    secondaryType = entity.secondaryType.takeIf { it.isNotBlank() }?.let { parsePokemonType(it) },
    baseHp = entity.baseHp,
    baseAtk = entity.baseAttack,
    baseDef = entity.baseDefense,
    baseSpA = entity.baseSpAtk,
    baseSpD = entity.baseSpDef,
    baseSpe = entity.baseSpeed,
    abilityName = entity.abilityPrimary,
    stages = stages.clamped(),
    status = status,
    gimmickMode = gimmick,
    teraType = teraType,
    bondGauge = bondGauge.coerceIn(0, 100),
    heldItemId = heldItemId,
  )

  fun buildMovesetForPokemon(
    entity: NativePokemonEntity,
    teraType: PokemonType,
    taughtSlot4Move: BattleMoveSpec? = null,
  ): List<BattleMoveSpec> {
    val primary = parsePokemonType(entity.primaryType)
    val secondary = entity.secondaryType.takeIf { it.isNotBlank() }?.let { parsePokemonType(it) } ?: PokemonType.NORMAL
    val prefersSpecial = entity.baseSpAtk >= entity.baseAttack
    val mainCat = if (prefersSpecial) MoveCategory.SPECIAL else MoveCategory.PHYSICAL
    val altCat = if (prefersSpecial) MoveCategory.PHYSICAL else MoveCategory.SPECIAL

    val defaultSlot4 = BattleMoveSpec(
      name = when (primary) {
        PokemonType.ELECTRIC -> "Earth Power"
        PokemonType.FIRE -> "Solar Flare"
        PokemonType.WATER -> "Freeze-Dry"
        PokemonType.GRASS -> "Leaf Storm"
        else -> "Aura Sphere"
      },
      type = when (primary) {
        PokemonType.ELECTRIC -> PokemonType.GROUND
        PokemonType.FIRE -> PokemonType.GRASS
        PokemonType.WATER -> PokemonType.ICE
        PokemonType.GRASS -> PokemonType.GRASS
        else -> PokemonType.FIGHTING
      },
      category = MoveCategory.SPECIAL,
      basePower = 85,
      isFreezeDry = primary == PokemonType.WATER,
    )

    return listOf(
      BattleMoveSpec(
        name = entity.signatureMove.ifBlank { "Resonance Strike" },
        type = primary,
        category = mainCat,
        basePower = 95,
        accuracy = 100,
        isContact = mainCat == MoveCategory.PHYSICAL,
        highCritRatio = true,
      ),
      BattleMoveSpec(
        name = if (secondary != PokemonType.NORMAL) "${secondary.displayName} Pulse" else "Quick Dash",
        type = secondary,
        category = altCat,
        basePower = 75,
        priority = if (secondary == PokemonType.NORMAL) 1 else 0,
        isContact = altCat == MoveCategory.PHYSICAL,
      ),
      BattleMoveSpec(
        name = "Tera Blast",
        type = PokemonType.NORMAL,
        category = mainCat,
        basePower = 80,
        isTeraBlast = true,
      ),
      taughtSlot4Move ?: defaultSlot4,
    )
  }

  /**
   * Parses a StudioItemCatalog TM effectTag ("TM:Thunderbolt|Electric|90") into a real playable BattleMoveSpec.
   */
  fun parseTmEffectTag(effectTag: String): BattleMoveSpec? {
    if (!effectTag.startsWith("TM:")) return null
    val parts = effectTag.removePrefix("TM:").split("|")
    if (parts.size < 3) return null
    val name = parts[0].trim().ifEmpty { return null }
    val type = parsePokemonType(parts[1])
    val power = parts[2].trim().toIntOrNull()?.coerceIn(40, 150) ?: 85
    val isPhysical = name.equals("Earthquake", ignoreCase = true) || type in setOf(
      PokemonType.GROUND, PokemonType.ROCK, PokemonType.BUG,
    )
    return BattleMoveSpec(
      name = name,
      type = type,
      category = if (isPhysical) MoveCategory.PHYSICAL else MoveCategory.SPECIAL,
      basePower = power,
      accuracy = 100,
      isTeraBlast = name.equals("Tera Blast", ignoreCase = true),
    )
  }

  /**
   * Resolves classic Evolution Stone evolutions for iconic species in-place (returning target Dex ID),
   * or null when a species is already fully evolved (which then triggers Resonance Evolution Surge).
   */
  fun resolveStoneEvolutionTarget(currentDex: Int, evoType: String): Int? {
    val norm = evoType.trim().lowercase()
    return when (currentDex) {
      25 -> if (norm == "electric") 26 else null // Pikachu -> Raichu
      133 -> when (norm) { // Eevee branches
        "water" -> 134 // Vaporeon
        "electric" -> 135 // Jolteon
        "fire" -> 136 // Flareon
        "psychic" -> 196 // Espeon
        "ghost" -> 197 // Umbreon
        "fairy" -> 700 // Sylveon
        else -> null
      }
      1, 2 -> if (norm == "grass") 3 else null
      4, 5 -> if (norm == "fire") 6 else null
      7, 8 -> if (norm == "water") 9 else null
      37 -> if (norm == "fire") 38 else null
      58 -> if (norm == "fire") 59 else null
      129 -> if (norm == "water") 130 else null
      147, 148 -> if (norm == "dragon") 149 else null
      152, 153 -> if (norm == "grass") 154 else null
      155, 156 -> if (norm == "fire") 157 else null
      158, 159 -> if (norm == "water") 160 else null
      252, 253 -> if (norm == "grass") 254 else null
      255, 256 -> if (norm == "fire") 257 else null
      258, 259 -> if (norm == "water") 260 else null
      387, 388 -> if (norm == "grass") 389 else null
      390, 391 -> if (norm == "fire") 392 else null
      393, 394 -> if (norm == "water") 395 else null
      495, 496 -> if (norm == "grass") 497 else null
      498, 499 -> if (norm == "fire") 500 else null
      501, 502 -> if (norm == "water") 503 else null
      650, 651 -> if (norm == "grass") 652 else null
      653, 654 -> if (norm == "fire") 655 else null
      656, 657 -> if (norm == "water") 658 else null
      722, 723 -> if (norm == "grass") 724 else null
      725, 726 -> if (norm == "fire") 727 else null
      728, 729 -> if (norm == "water") 730 else null
      810, 811 -> if (norm == "grass") 812 else null
      813, 814 -> if (norm == "fire") 815 else null
      816, 817 -> if (norm == "water") 818 else null
      906, 907 -> if (norm == "grass") 908 else null
      909, 910 -> if (norm == "fire") 911 else null
      912, 913 -> if (norm == "water") 914 else null
      else -> null
    }
  }

  fun encodeExtraPrettyMask(s: ExtraVisualAudioSettings): Int {
    var mask = 0
    if (s.dynamicWeatherParticles) mask = mask or (1 shl 0)
    if (s.waterCausticsAndFoam) mask = mask or (1 shl 1)
    if (s.hd2dVolumetricRaysAndBokeh) mask = mask or (1 shl 2)
    if (s.ultraVoxelSlices) mask = mask or (1 shl 3)
    if (s.uncappedBloomAndRimFlares) mask = mask or (1 shl 4)
    if (s.analogCrtPhosphorAndCurvature) mask = mask or (1 shl 5)
    if (s.fluid60FpsCharacterMotion) mask = mask or (1 shl 6)
    if (s.foregroundParallaxFraming) mask = mask or (1 shl 7)
    if (s.extraLushAudioReverbAndChoir) mask = mask or (1 shl 8)
    return mask
  }

  fun decodeExtraPrettyMask(mask: Int): ExtraVisualAudioSettings = ExtraVisualAudioSettings(
    dynamicWeatherParticles = (mask and (1 shl 0)) != 0,
    waterCausticsAndFoam = (mask and (1 shl 1)) != 0,
    hd2dVolumetricRaysAndBokeh = (mask and (1 shl 2)) != 0,
    ultraVoxelSlices = (mask and (1 shl 3)) != 0,
    uncappedBloomAndRimFlares = (mask and (1 shl 4)) != 0,
    analogCrtPhosphorAndCurvature = (mask and (1 shl 5)) != 0,
    fluid60FpsCharacterMotion = (mask and (1 shl 6)) != 0,
    foregroundParallaxFraming = (mask and (1 shl 7)) != 0,
    extraLushAudioReverbAndChoir = (mask and (1 shl 8)) != 0,
  )

  fun toHardwareProfileEntity(spec: HardwarePlatformSpec): HardwareProfileEntity {
    val g = spec.graphicsTraits
    val a = spec.audioTraits
    val gJson = JSONObject().apply {
      put("virtualPixelGrid", g.virtualPixelGrid.toDouble())
      put("outlineWeight", g.outlineWeight.toDouble())
      put("celBands", g.celBands.toDouble())
      put("bayerDither", g.bayerDither.toDouble())
      put("affineWarp", g.affineWarp.toDouble())
      put("voxelDepth", g.voxelDepth.toDouble())
      put("vertexSnap", g.vertexSnap.toDouble())
      put("bloom", g.bloom.toDouble())
      put("rimLight", g.rimLight.toDouble())
      put("crtScanline", g.crtScanline.toDouble())
      put("dotMatrixLcd", g.dotMatrixLcd.toDouble())
      put("parallaxLayers", g.parallaxLayers.toDouble())
    }.toString()
    val aJson = JSONObject().apply {
      put("pulseDutyWeight", a.pulseDutyWeight.toDouble())
      put("triangleBassWeight", a.triangleBassWeight.toDouble())
      put("fmSlapWeight", a.fmSlapWeight.toDouble())
      put("sidArpWeight", a.sidArpWeight.toDouble())
      put("spcEchoWeight", a.spcEchoWeight.toDouble())
      put("orchestralPadWeight", a.orchestralPadWeight.toDouble())
      put("lfsrNoiseWeight", a.lfsrNoiseWeight.toDouble())
      put("bitcrushDepth", a.bitcrushDepth.toDouble())
      put("stereoWidth", a.stereoWidth.toDouble())
    }.toString()
    return HardwareProfileEntity(
      id = spec.id,
      name = spec.name,
      manufacturer = spec.manufacturer,
      year = spec.year,
      generation = spec.generation,
      category = spec.category,
      gpuChip = spec.gpuChip,
      audioChip = spec.audioChip,
      videoModes = spec.videoModes,
      primaryDomain = spec.primaryDomain.name,
      honoredSignature = spec.honoredSignature,
      brokenLimitation = spec.brokenLimitation,
      graphicsJson = gJson,
      audioJson = aJson,
    )
  }

  fun parseHeldItemsCsv(csv: String): Map<Int, String> {
    if (csv.isBlank()) return emptyMap()
    return csv.split(",").mapNotNull { entry ->
      val parts = entry.split(":")
      val dex = parts.getOrNull(0)?.trim()?.toIntOrNull()
      val itemId = parts.getOrNull(1)?.trim()
      if (dex != null && !itemId.isNullOrBlank()) dex to itemId else null
    }.toMap()
  }

  fun encodeHeldItemsCsv(map: Map<Int, String>): String =
    map.entries.filter { it.value.isNotBlank() }.joinToString(",") { "${it.key}:${it.value}" }

  fun parsePartyLevelsCsv(csv: String): Map<Int, Int> {
    if (csv.isBlank()) return emptyMap()
    return csv.split(",").mapNotNull { entry ->
      val parts = entry.split(":")
      val dex = parts.getOrNull(0)?.trim()?.toIntOrNull()
      val lvl = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(1, 100)
      if (dex != null && lvl != null) dex to lvl else null
    }.toMap()
  }

  fun encodePartyLevelsCsv(map: Map<Int, Int>): String =
    map.entries.joinToString(",") { "${it.key}:${it.value.coerceIn(1, 100)}" }

  fun parseTaughtMovesCsv(csv: String): Map<Int, BattleMoveSpec> {
    if (csv.isBlank()) return emptyMap()
    return csv.split(";").mapNotNull { entry ->
      val colon = entry.indexOf(':')
      if (colon <= 0) return@mapNotNull null
      val dex = entry.substring(0, colon).trim().toIntOrNull() ?: return@mapNotNull null
      val parts = entry.substring(colon + 1).split("|")
      if (parts.size < 4) return@mapNotNull null
      val name = parts[0].trim().ifEmpty { return@mapNotNull null }
      val type = parsePokemonType(parts[1])
      val power = parts[2].trim().toIntOrNull()?.coerceIn(40, 150) ?: 85
      val cat = MoveCategory.entries.firstOrNull { it.name.equals(parts[3].trim(), ignoreCase = true) }
        ?: MoveCategory.SPECIAL
      dex to BattleMoveSpec(
        name = name,
        type = type,
        category = cat,
        basePower = power,
        isTeraBlast = name.equals("Tera Blast", ignoreCase = true),
      )
    }.toMap()
  }

  fun encodeTaughtMovesCsv(map: Map<Int, BattleMoveSpec>): String =
    map.entries.joinToString(";") { (dex, move) ->
      "$dex:${move.name}|${move.type.name}|${move.basePower}|${move.category.name}"
    }

  val builtInSidebarAssets: List<BuiltInAssetItem> = listOf(
    BuiltInAssetItem(
      id = "spr_rayquaza_omni",
      name = "Rayquaza Sky-Pillar Omni-Sprite",
      category = AssetCategory.SPRITES,
      consoleLineage = "GBA Emerald + Gen V Skeletal + Switch Bloom",
      chipAddon = "Super FX2 + Voxelizer-FX",
      spriteSeedId = 384,
      shaderFormula = "BLEND:65|VOXEL:75|CRT:25",
    ),
    BuiltInAssetItem(
      id = "spr_charizard_sgb",
      name = "Charizard SGB2 + Gen V Wing Rig",
      category = AssetCategory.SPRITES,
      consoleLineage = "DMG/SGB2 + NDS B2W2 + HD-2D Point Light",
      chipAddon = "Super Game Boy 2 + PokeCRT-RTX",
      spriteSeedId = 6,
      shaderFormula = "BLEND:55|VOXEL:68|CRT:35",
    ),
    BuiltInAssetItem(
      id = "spr_pikachu_yellow",
      name = "Pikachu Yellow PPU + Cel-Outline",
      category = AssetCategory.SPRITES,
      consoleLineage = "GBC Hi-Color + 3DS PICA200 Toon",
      chipAddon = "MSU-1 PCM + SA-1",
      spriteSeedId = 25,
      shaderFormula = "BLEND:50|VOXEL:60|CRT:30",
    ),
    BuiltInAssetItem(
      id = "spr_gengar_shadow",
      name = "Shadow Gengar XD001 Chromatic Sprite",
      category = AssetCategory.SPRITES,
      consoleLineage = "GBC Crystal Dither + GameCube Orre TEV",
      chipAddon = "GBA e-Reader + Dolphin TEV",
      spriteSeedId = 94,
      shaderFormula = "BLEND:70|VOXEL:80|CRT:40",
    ),
    BuiltInAssetItem(
      id = "mod_mewtwo_cx4",
      name = "Mewtwo Stadium + CX4 Wireframe Mesh",
      category = AssetCategory.MODELS_3D,
      consoleLineage = "N64 Stadium + Capcom CX4 + Pokkén PBR",
      chipAddon = "Capcom CX4 + N64 8MB Expansion Pak",
      spriteSeedId = 150,
      shaderFormula = "BLEND:88|VOXEL:90|CRT:15",
    ),
    BuiltInAssetItem(
      id = "mod_lugia_xd",
      name = "XD001 Shadow Lugia Volumetric Rig",
      category = AssetCategory.MODELS_3D,
      consoleLineage = "GameCube Flipper TEV + NDS SoulSilver 2.5D",
      chipAddon = "Sega 32X + GameCube TEV",
      spriteSeedId = 249,
      shaderFormula = "BLEND:82|VOXEL:85|CRT:20",
    ),
    BuiltInAssetItem(
      id = "mod_miraidon_matrix",
      name = "Miraidon Hadron Voxel-Polygon Core",
      category = AssetCategory.MODELS_3D,
      consoleLineage = "Switch PBR + 8-Bit LED Dot-Matrix Visor",
      chipAddon = "Super FX2 + PokeCRT-RTX",
      spriteSeedId = 1008,
      shaderFormula = "BLEND:85|VOXEL:92|CRT:28",
    ),
    BuiltInAssetItem(
      id = "tex_gbc_dither_pbr",
      name = "GBC Ordered Dither + PBR Normal Atlas",
      category = AssetCategory.TEXTURES,
      consoleLineage = "GBC Crystal 4-Tone + Switch Metallic Roughness",
      chipAddon = "Super Game Boy 2 + Voxelizer-FX",
      spriteSeedId = 133,
      shaderFormula = "DITHER:4x4|PBR_NORMAL:1.0",
    ),
    BuiltInAssetItem(
      id = "tex_mode7_sky",
      name = "SNES Mode-7 + Hoenn Cloud Panorama",
      category = AssetCategory.TEXTURES,
      consoleLineage = "SNES PPU Mode-7 + GBA Emerald Affine",
      chipAddon = "Super FX2 + SA-1",
      spriteSeedId = 384,
      shaderFormula = "AFFINE:0.75|PARALLAX:6",
    ),
    BuiltInAssetItem(
      id = "tex_terastal_facet",
      name = "Paldea Terastal Prism Facet Shader",
      category = AssetCategory.TEXTURES,
      consoleLineage = "Switch PBR + Capcom CX4 Wireframe",
      chipAddon = "Capcom CX4 + PokeCRT-RTX",
      spriteSeedId = 1007,
      shaderFormula = "CRYSTAL:0.85|RIM:0.90",
    ),
    BuiltInAssetItem(
      id = "aud_msu1_orchestral",
      name = "MSU-1 Orchestral + NES VRC6 Synth Bus",
      category = AssetCategory.AUDIO_FX,
      consoleLineage = "NES VRC6 + SPC700 + Sega YM2612 + MSU-1",
      chipAddon = "MSU-1 PCM + YM2612",
      spriteSeedId = 25,
      shaderFormula = "AUDIO_44K_STEREO_6LANE",
    ),
    BuiltInAssetItem(
      id = "shd_shovel_knight_plus",
      name = "Shovel Knight+ Omni-Era Discipline Shader",
      category = AssetCategory.CHIPS_SHADERS,
      consoleLineage = "All 156 Hardware Platforms Unified",
      chipAddon = "AGSL Single-Pass Ink + Cel + Rim",
      spriteSeedId = 448,
      shaderFormula = "SHOVEL_KNIGHT_CURATED",
    ),
    BuiltInAssetItem(
      id = "scr_prism_battle_core",
      name = "Prism Resonance Clean-Room Battle Core",
      category = AssetCategory.MAPS,
      consoleLineage = "Gen III-IX + Hold Items + Tera + Weather",
      chipAddon = "SA-1 + Deterministic Seeded RNG",
      spriteSeedId = 658,
      shaderFormula = "CLEAN_ROOM_CANON_ENGINE",
    ),
  )
}

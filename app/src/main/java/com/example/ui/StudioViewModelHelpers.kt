package com.example.ui

import com.example.data.local.HardwareProfileEntity
import com.example.data.local.NativePokemonEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ExtraVisualAudioSettings
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.CombatantSnapshot
import com.example.engine.HardwarePlatformSpec
import com.example.engine.MoveCategory
import com.example.engine.PokemonType
import com.example.engine.StatusCondition
import org.json.JSONObject

object StudioViewModelHelpers {
  fun parsePokemonType(raw: String): PokemonType =
    PokemonType.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PokemonType.NORMAL

  fun buildCombatantFromEntity(
    entity: NativePokemonEntity,
    level: Int = 50,
    gimmick: BattleGimmickMode = BattleGimmickMode.NONE,
    teraType: PokemonType? = null,
    status: StatusCondition = StatusCondition.HEALTHY,
    heldItemId: String = "",
  ): CombatantSnapshot = CombatantSnapshot(
    dexNumber = entity.dexNumber,
    name = entity.name,
    level = level,
    primaryType = parsePokemonType(entity.primaryType),
    secondaryType = entity.secondaryType.takeIf { it.isNotBlank() }?.let { parsePokemonType(it) },
    baseHp = entity.baseHp,
    baseAtk = entity.baseAttack,
    baseDef = entity.baseDefense,
    baseSpA = entity.baseSpAtk,
    baseSpD = entity.baseSpDef,
    baseSpe = entity.baseSpeed,
    abilityName = entity.abilityPrimary,
    status = status,
    gimmickMode = gimmick,
    teraType = teraType,
    heldItemId = heldItemId,
  )

  fun buildMovesetForPokemon(entity: NativePokemonEntity, teraType: PokemonType): List<BattleMoveSpec> {
    val primary = parsePokemonType(entity.primaryType)
    val secondary = entity.secondaryType.takeIf { it.isNotBlank() }?.let { parsePokemonType(it) } ?: PokemonType.NORMAL
    val prefersSpecial = entity.baseSpAtk >= entity.baseAttack
    val mainCat = if (prefersSpecial) MoveCategory.SPECIAL else MoveCategory.PHYSICAL
    val altCat = if (prefersSpecial) MoveCategory.PHYSICAL else MoveCategory.SPECIAL

    return listOf(
      BattleMoveSpec(
        name = entity.signatureMove.ifBlank { "Resonance Strike" },
        type = primary,
        category = mainCat,
        basePower = 95,
        accuracy = 100,
        highCritRatio = true,
      ),
      BattleMoveSpec(
        name = if (secondary != PokemonType.NORMAL) "${secondary.displayName} Pulse" else "Quick Dash",
        type = secondary,
        category = altCat,
        basePower = 75,
        priority = if (secondary == PokemonType.NORMAL) 1 else 0,
      ),
      BattleMoveSpec(
        name = "Tera Blast",
        type = PokemonType.NORMAL,
        category = mainCat,
        basePower = 80,
        isTeraBlast = true,
      ),
      BattleMoveSpec(
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
      ),
    )
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
      consoleLineage = "All 87 Hardware Platforms Unified",
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

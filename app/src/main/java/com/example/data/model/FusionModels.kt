package com.example.data.model

data class FanGameProject(
  val id: String = "",
  val title: String = "",
  val regionName: String = "",
  val synopsis: String = "",
  val consoleMatrixPreset: String = "Ultimate Target Style — All 156 Hardware",
  val activeChipsCsv: String = "SHOVEL_KNIGHT_CURATED|ALL_ON_RECORD",
  val pixel3dBlendRatio: Long = 65L,
  val dexCount: Long = 1025L,
  val mapCount: Long = 22L,
  val versionTag: String = "v5.0.0-PRISM",
  val createdAtMs: Long = System.currentTimeMillis(),
  val updatedAtMs: Long = System.currentTimeMillis(),
)

data class CustomDexEntry(
  val id: String = "",
  val projectId: String = "",
  val dexNumber: Long = 1026L,
  val name: String = "",
  val category: String = "Fusion Species",
  val primaryType: String = "Dragon",
  val secondaryType: String = "Electric",
  val abilityName: String = "Polygon Overclock",
  val signatureMove: String = "Super FX Hyper Beam",
  val loreDescription: String = "",
  val baseHp: Long = 90L,
  val baseAttack: Long = 105L,
  val baseDefense: Long = 85L,
  val baseSpAtk: Long = 115L,
  val baseSpDef: Long = 90L,
  val baseSpeed: Long = 100L,
  val baseSpriteId: Long = 384L,
  val fusionStylePreset: String = "Omni-Console Pixel + 3D Cel Voxel",
  val enhancementChip: String = "Super FX2 (GSU-2)",
  val primaryHex: String = "#00F5D4",
  val secondaryHex: String = "#FF2A85",
  val createdAtMs: Long = System.currentTimeMillis(),
  val updatedAtMs: Long = System.currentTimeMillis(),
) {
  val baseStatTotal: Int
    get() = (baseHp + baseAttack + baseDefense + baseSpAtk + baseSpDef + baseSpeed).toInt()
}

data class CustomStudioAsset(
  val id: String = "",
  val projectId: String = "",
  val name: String = "",
  val category: String = AssetCategory.SPRITES.label,
  val consoleLineage: String = "GBC + GBA + DS + Switch HD-2D",
  val chipAddon: String = "Super FX2 + Voxelizer-FX",
  val spriteSeedId: Long = 25L,
  val shaderFormula: String = "BLEND:65|VOXEL:70|CRT:30|BLOOM:85",
  val createdAtMs: Long = System.currentTimeMillis(),
  val updatedAtMs: Long = System.currentTimeMillis(),
)

enum class AssetCategory(val id: String, val label: String, val badge: String) {
  SPRITES("sprites", "Sprites", "2D+HD"),
  MODELS_3D("models_3d", "3D Models", "VOX+POLY"),
  TRAINERS("trainers", "Trainers", "2D+3D"),
  TEXTURES("textures", "Textures", "PBR+TEX"),
  TILESETS("tilesets", "Tilesets", "MODE-7"),
  CHIPS_SHADERS("chips", "Chips & Shaders", "ASIC+FX"),
  MAPS("maps", "World Maps", "DIORAMA"),
  AUDIO_FX("audio", "Audio & Chips", "MSU-1"),
}

enum class MasterDisciplineMode(val token: String, val label: String, val subtitle: String) {
  SHOVEL_KNIGHT_CURATED(
    token = "SHOVEL_KNIGHT_CURATED",
    label = "Shovel Knight+ Curated",
    subtitle = "Crisp shared pixel grid, 1px #0E1125 ink outline, clean cel ramps, 12-15fps stepped keyframes",
  ),
  MAXIMALIST_PRETTY(
    token = "MAXIMALIST_PRETTY",
    label = "Maximalist Pretty",
    subtitle = "Unleashes extra voxels, volumetric rays, caustics, 60fps fluid character motion & lush reverb",
  ),
  RAW_HARDWARE_PURIST(
    token = "RAW_HARDWARE_PURIST",
    label = "Raw Hardware Purist",
    subtitle = "Strict authenticity to selected hardware traits: DMG dot matrix, GBC dither, PS1 affine snap",
  ),
}

enum class CharacterMixMode(val id: String, val label: String, val shortTag: String) {
  SPLIT_BEAM_MORPH("split_beam", "Split-Beam 2D⇄3D", "2D⇄3D"),
  VOXELIZER_FX_3D("voxelizer_fx", "Voxelizer-FX 3D", "VOXEL"),
  HD2D_CRISP_SPRITE("hd2d_sprite", "HD-2D Crisp Sprite", "HD-2D"),
  PIXEL_LOCKED_3D("pixel_locked_3d", "Pixel-Locked PICA200+Switch", "CEL-3D"),
}

data class ExtraVisualAudioSettings(
  val ultraVoxelSlices: Boolean = false,
  val dynamicWeatherParticles: Boolean = false,
  val waterCausticsAndFoam: Boolean = false,
  val hd2dVolumetricRaysAndBokeh: Boolean = false,
  val foregroundParallaxFraming: Boolean = false,
  val fluid60FpsCharacterMotion: Boolean = false,
  val uncappedBloomAndRimFlares: Boolean = false,
  val analogCrtPhosphorAndCurvature: Boolean = false,
  val extraLushAudioReverbAndChoir: Boolean = false,
) {
  val enabledCount: Int
    get() = listOf(
      ultraVoxelSlices,
      dynamicWeatherParticles,
      waterCausticsAndFoam,
      hd2dVolumetricRaysAndBokeh,
      foregroundParallaxFraming,
      fluid60FpsCharacterMotion,
      uncappedBloomAndRimFlares,
      analogCrtPhosphorAndCurvature,
      extraLushAudioReverbAndChoir,
    ).count { it }

  companion object {
    val ALL_ENABLED = ExtraVisualAudioSettings(
      ultraVoxelSlices = true,
      dynamicWeatherParticles = true,
      waterCausticsAndFoam = true,
      hd2dVolumetricRaysAndBokeh = true,
      foregroundParallaxFraming = true,
      fluid60FpsCharacterMotion = true,
      uncappedBloomAndRimFlares = true,
      analogCrtPhosphorAndCurvature = true,
      extraLushAudioReverbAndChoir = true,
    )
    val ALL_DISABLED = ExtraVisualAudioSettings()
  }
}

data class ConsoleChipAddon(
  val id: String,
  val name: String,
  val systemOrigin: String,
  val isFanMade: Boolean,
  val effectSummary: String,
  val accentHex: String,
)

data class ConsoleEraLayer(
  val id: String,
  val eraName: String,
  val hardwareCode: String,
  val visualContribution: String,
  val defaultWeight: Float,
)

data class ConsoleFusionConfig(
  val presetName: String = "Ultimate Target Style — All 156 Hardware",
  val pixelTo3dBlend: Float = 0.65f,
  val voxelExtrusionDepth: Float = 0.72f,
  val crtScanlineIntensity: Float = 0.24f,
  val hd2dVolumetricBloom: Float = 0.68f,
  val mode7PerspectiveTilt: Float = 0.45f,
  val celOutlineWeight: Float = 0.70f,
  val sgbPaletteWarmth: Float = 0.50f,
  val disciplineMode: MasterDisciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
  val characterMixMode: CharacterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
  val extraPretty: ExtraVisualAudioSettings = ExtraVisualAudioSettings(),
  val activeChipIds: Set<String> = setOf(
    "super_fx2",
    "sa_1",
    "sega_32x",
    "sgb2_border",
    "n64_rcp",
    "msu_1",
    "voxelizer_fx",
    "pokecrt_rtx",
  ),
)

object ConsoleHardwareCatalog {
  val officialAndFanChips = listOf(
    ConsoleChipAddon(
      id = "super_fx2",
      name = "Super FX2 (GSU-2 21MHz)",
      systemOrigin = "SNES / Super Famicom Cartridge Co-Processor",
      isFanMade = false,
      effectSummary = "Hardware 3D polygon wireframe overlay, affine sprite scaling & texture morphing",
      accentHex = "#00F5D4",
    ),
    ConsoleChipAddon(
      id = "sa_1",
      name = "Nintendo SA-1 Accelerator",
      systemOrigin = "SNES 10.74MHz 65C816 Co-Processor",
      isFanMade = false,
      effectSummary = "High-density character-to-bitmap DMA & multi-layer parallax sprite banking",
      accentHex = "#FFD166",
    ),
    ConsoleChipAddon(
      id = "cx4",
      name = "Capcom CX4 Trigonometric DSP",
      systemOrigin = "SNES 20MHz Vector Math Chip",
      isFanMade = false,
      effectSummary = "Real-time 3D wireframe mesh cage and rotational sprite vector matrices",
      accentHex = "#FF2A85",
    ),
    ConsoleChipAddon(
      id = "sgb2_border",
      name = "Super Game Boy 2 + ICD2-R",
      systemOrigin = "GB / SNES Hybrid Enhancement Adapter",
      isFanMade = false,
      effectSummary = "Multi-palette SGB attribute shading with custom ornamental arcade borders",
      accentHex = "#A855F7",
    ),
    ConsoleChipAddon(
      id = "sega_32x",
      name = "Sega 32X Dual SH-2 RISC + VDP",
      systemOrigin = "Genesis / Mega Drive 32-Bit Add-On",
      isFanMade = false,
      effectSummary = "32,768-color Gouraud-shaded 3D polygon synthesis fused with 16-bit sprites",
      accentHex = "#38BDF8",
    ),
    ConsoleChipAddon(
      id = "n64_rcp",
      name = "N64 RCP + 8MB Expansion Pak",
      systemOrigin = "Nintendo 64 Reality Coprocessor",
      isFanMade = false,
      effectSummary = "Z-buffered trilinear Stadium-grade 3D vertex lighting & specular rim glow",
      accentHex = "#10B981",
    ),
    ConsoleChipAddon(
      id = "gba_e_reader",
      name = "GBA e-Reader + GameCube Link",
      systemOrigin = "32-Bit AGB + Dolphin TEV Bridge",
      isFanMade = false,
      effectSummary = "Unlocks Colosseum/XD TEV shadow aura shaders on 32-bit handheld sprites",
      accentHex = "#F97316",
    ),
    ConsoleChipAddon(
      id = "msu_1",
      name = "MSU-1 Media Enhancement Chip",
      systemOrigin = "Fan-Made FPGA Cartridge Co-Processor",
      isFanMade = true,
      effectSummary = "4GB address bus for 44.1kHz orchestral PCM audio & full-motion shader backgrounds",
      accentHex = "#EC4899",
    ),
    ConsoleChipAddon(
      id = "voxelizer_fx",
      name = "Voxelizer-FX FPGA Core",
      systemOrigin = "Fan-Made 2D-to-3D Isometric Voxel Synthesizer",
      isFanMade = true,
      effectSummary = "Extrudes 2D sprite pixels into bevelled 3D sub-voxels with normal-mapped depth",
      accentHex = "#2DD4BF",
    ),
    ConsoleChipAddon(
      id = "pokecrt_rtx",
      name = "PokeCRT-RTX Hybrid Shader ASIC",
      systemOrigin = "Fan-Made HD-2D + Trinitron Aperture Grille Engine",
      isFanMade = true,
      effectSummary = "Combines sub-pixel PVM phosphor bloom with Octopath-style bokeh & volumetric rays",
      accentHex = "#FACC15",
    ),
  )

  val consoleEraLayers = listOf(
    ConsoleEraLayer(
      id = "gb_gbc",
      eraName = "8-Bit Handheld Core (DMG / SGB2 / GBC)",
      hardwareCode = "Sharp LR35902 + PPU Hi-Color",
      visualContribution = "Crisp 1px silhouette ink lines & high-contrast 4-to-56 color dithered shading",
      defaultWeight = 0.85f,
    ),
    ConsoleEraLayer(
      id = "snes_gba",
      eraName = "16/32-Bit Sprite Golden Age (SNES / GBA)",
      hardwareCode = "S-PPU2 Mode-7 + ARM7TDMI",
      visualContribution = "Vibrant 15-bit RGB palettes, Mode-7 affine ground planes & expressive keyframe poses",
      defaultWeight = 0.95f,
    ),
    ConsoleEraLayer(
      id = "ds_ps1",
      eraName = "Hybrid 2.5D Dio-Polygon Era (NDS / PS1 / Saturn)",
      hardwareCode = "ARM946E-S + Geometry Engine",
      visualContribution = "3D low-poly architectural terrain combined with animated 60fps 2D sprite billboards",
      defaultWeight = 0.88f,
    ),
    ConsoleEraLayer(
      id = "n64_gc",
      eraName = "Console 3D Arena Era (N64 Stadium / GameCube TEV)",
      hardwareCode = "Reality Coprocessor + Flipper TEV",
      visualContribution = "Dynamic vertex rim lighting, elemental particle Halos & cel-metallic shaders",
      defaultWeight = 0.80f,
    ),
    ConsoleEraLayer(
      id = "3ds_switch",
      eraName = "Modern Cel-3D & HD-2D Era (3DS / Switch)",
      hardwareCode = "PICA200 + Maxwell PBR HD-2D",
      visualContribution = "Sub-surface toon shading, depth-of-field tilt-shift, and volumetric god-rays",
      defaultWeight = 0.92f,
    ),
  )
}

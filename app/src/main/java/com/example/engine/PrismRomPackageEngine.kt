package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.PrismRomPlayerActivity
import com.example.data.local.SideModeRecordEntity
import com.example.data.model.CharacterMixMode
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Pure-Kotlin declarative specification for one of the 10 built-in hardware comparison ROM editions
 * of "Pokémon: Prism Resonance" (plus CURRENT_STUDIO_BUILD for any custom Studio combo).
 */
data class PrismRomProfileSpec(
  val comparisonTag: String,
  val shortChipLabel: String,
  val displayName: String,
  val editionSubtitle: String,
  val hardwareBadgeSummary: String,
  val accentHex: String,
  val platformIds: List<String>,
  val useAllCatalogPlatforms: Boolean = false,
  val disciplineMode: MasterDisciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
  val characterMixMode: CharacterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
  val pixelTo3dBlend: Float = 0.50f,
  val extraVisualAudio: ExtraVisualAudioSettings = ExtraVisualAudioSettings(),
)

/**
 * Portable, self-contained `.prismrom` declarative cartridge snapshot.
 * Zero executable code (100% Play Policy compliant pure JSON data).
 */
data class PrismRomCartridge(
  val schemaVersion: Int = PrismRomPackageEngine.SCHEMA_VERSION,
  val romId: String,
  val romTitle: String,
  val comparisonTag: String,
  val editionSubtitle: String,
  val hardwareBadgeSummary: String,
  val accentHex: String,
  val activePlatformIds: List<String>,
  val useAllCatalogPlatforms: Boolean = false,
  val disciplineModeToken: String,
  val characterMixModeName: String,
  val extraPrettyMask: Int,
  val pixelTo3dBlend: Float,
  val activeRegionId: String,
  val playerRow: Int,
  val playerCol: Int,
  val regionMapOverrides: Map<String, List<Int>>,
  val playerTrainer: TrainerCharacterSpec,
  val playerCustomAvatarRle: String = "",
  val playerCustomAvatarPaletteJson: String = "",
  val useCustomPlayerAvatarBitmap: Boolean = false,
  val partyDexIds: List<Int>,
  val partyLevels: Map<Int, Int>,
  val heldItemsMap: Map<Int, String>,
  val completedRegionIds: List<String>,
  val sideModeRecord: SideModeRecordEntity,
  val compiledAtEpochMs: Long = System.currentTimeMillis(),
) {
  val disciplineMode: MasterDisciplineMode
    get() = MasterDisciplineMode.entries.firstOrNull { it.token == disciplineModeToken }
      ?: MasterDisciplineMode.SHOVEL_KNIGHT_CURATED

  val characterMixMode: CharacterMixMode
    get() = runCatching { CharacterMixMode.valueOf(characterMixModeName) }
      .getOrDefault(CharacterMixMode.SPLIT_BEAM_MORPH)

  val extraVisualAudio: ExtraVisualAudioSettings
    get() = PrismRomPackageEngine.decodeExtraPrettyMask(extraPrettyMask)

  val shortShortcutLabel: String
    get() {
      val chip = PrismRomPackageEngine.comparisonProfiles
        .firstOrNull { it.comparisonTag == comparisonTag }
        ?.shortChipLabel
        ?: comparisonTag.take(8)
      return "Prism: $chip".take(18)
    }

  val longShortcutLabel: String
    get() = "$romTitle ($editionSubtitle)".take(48)
}

enum class PinShortcutOutcome {
  PIN_REQUESTED,
  NOT_SUPPORTED_ON_LAUNCHER,
  FAILED,
}

/**
 * Clean-room `.prismrom` Comparison Cartridge Compiler, Lossless Serializer, Internal Library Vault,
 * Procedural Cartridge Icon Generator, and Android Home-Screen Shortcut Installer.
 */
object PrismRomPackageEngine {
  const val SCHEMA_VERSION = 1
  const val ROM_EXTENSION = ".prismrom"
  const val ROM_DIR_NAME = "prism_roms"
  const val EXTRA_ROM_ID = "com.example.extra.PRISM_ROM_ID"
  const val EXTRA_ROM_PATH = "com.example.extra.PRISM_ROM_PATH"

  private const val BIT_ULTRA_VOXEL = 1 shl 0
  private const val BIT_WEATHER = 1 shl 1
  private const val BIT_WATER = 1 shl 2
  private const val BIT_HD2D_RAYS = 1 shl 3
  private const val BIT_PARALLAX = 1 shl 4
  private const val BIT_FLUID_60 = 1 shl 5
  private const val BIT_BLOOM = 1 shl 6
  private const val BIT_CRT = 1 shl 7
  private const val BIT_LUSH_AUDIO = 1 shl 8

  /**
   * All 10 flagship hardware comparison profiles spanning 8-bit DMG through Ultimate 156 Hardware.
   */
  val comparisonProfiles: List<PrismRomProfileSpec> = listOf(
    PrismRomProfileSpec(
      comparisonTag = "GB_DMG",
      shortChipLabel = "GB DMG",
      displayName = "Game Boy DMG Classic",
      editionSubtitle = "GB DMG-01 DOT-MATRIX EDITION",
      hardwareBadgeSummary = "Game Boy DMG + Super Game Boy 2 Border",
      accentHex = "#8BAC0F",
      platformIds = listOf("game_boy_dmg", "sgb2_border"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.HD2D_CRISP_SPRITE,
      pixelTo3dBlend = 0.06f,
      extraVisualAudio = ExtraVisualAudioSettings(),
    ),
    PrismRomProfileSpec(
      comparisonTag = "NES_VRC6",
      shortChipLabel = "NES VRC6",
      displayName = "NES / Famicom VRC6",
      editionSubtitle = "NES VRC6 CHIPTUNE EDITION",
      hardwareBadgeSummary = "NES PPU + Konami VRC6 Expansion Synth",
      accentHex = "#EF4444",
      platformIds = listOf("nes_famicom", "nes_vrc6_fds"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.HD2D_CRISP_SPRITE,
      pixelTo3dBlend = 0.10f,
      extraVisualAudio = ExtraVisualAudioSettings(analogCrtPhosphorAndCurvature = true),
    ),
    PrismRomProfileSpec(
      comparisonTag = "SNES_FX2",
      shortChipLabel = "SNES FX2",
      displayName = "SNES Super FX2 + SA-1",
      editionSubtitle = "SNES SUPER FX2 EDITION",
      hardwareBadgeSummary = "SNES + Super FX2 + SA-1 + MSU-1 Orchestral",
      accentHex = "#8B5CF6",
      platformIds = listOf("snes_sfc", "super_fx2", "sa_1", "msu_1"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
      pixelTo3dBlend = 0.36f,
      extraVisualAudio = ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
        extraLushAudioReverbAndChoir = true,
      ),
    ),
    PrismRomProfileSpec(
      comparisonTag = "GBA_AGB",
      shortChipLabel = "GBA AGB",
      displayName = "Game Boy Advance AGB",
      editionSubtitle = "GBA 32-BIT PIXEL EDITION",
      hardwareBadgeSummary = "GBA AGB-001 + SP AGS-101 Bright TFT",
      accentHex = "#38BDF8",
      platformIds = listOf("gba_agb", "gba_sp_micro"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.HD2D_CRISP_SPRITE,
      pixelTo3dBlend = 0.18f,
      extraVisualAudio = ExtraVisualAudioSettings(waterCausticsAndFoam = true),
    ),
    PrismRomProfileSpec(
      comparisonTag = "PS1_GTE",
      shortChipLabel = "PS1 GTE",
      displayName = "PlayStation 1 GTE 3D",
      editionSubtitle = "PS1 GTE AFFINE 3D EDITION",
      hardwareBadgeSummary = "PlayStation 1 GTE Vertex-Snap 3D + SPU",
      accentHex = "#94A3B8",
      platformIds = listOf("ps1_gte"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.VOXELIZER_FX_3D,
      pixelTo3dBlend = 0.78f,
      extraVisualAudio = ExtraVisualAudioSettings(analogCrtPhosphorAndCurvature = true),
    ),
    PrismRomProfileSpec(
      comparisonTag = "GC_TEV",
      shortChipLabel = "GC TEV",
      displayName = "GameCube Flipper TEV",
      editionSubtitle = "GAMECUBE TEV CEL-3D EDITION",
      hardwareBadgeSummary = "GameCube Flipper TEV Pipeline + 60fps",
      accentHex = "#6366F1",
      platformIds = listOf("gamecube_tev"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.PIXEL_LOCKED_3D,
      pixelTo3dBlend = 0.86f,
      extraVisualAudio = ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
        fluid60FpsCharacterMotion = true,
      ),
    ),
    PrismRomProfileSpec(
      comparisonTag = "NDS_25D",
      shortChipLabel = "NDS 2.5D",
      displayName = "Nintendo DS / DSi 2.5D",
      editionSubtitle = "NDS 2.5D HYBRID BRIDGE EDITION",
      hardwareBadgeSummary = "Nintendo DS/DSi 2D Sprites on 3D Diorama",
      accentHex = "#14B8A6",
      platformIds = listOf("nintendo_ds_dsi"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
      pixelTo3dBlend = 0.48f,
      extraVisualAudio = ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
      ),
    ),
    PrismRomProfileSpec(
      comparisonTag = "3DS_PICA200",
      shortChipLabel = "3DS PICA",
      displayName = "Nintendo 3DS PICA200",
      editionSubtitle = "3DS PICA200 CEL-OUTLINE EDITION",
      hardwareBadgeSummary = "Nintendo 3DS PICA200 Toon-Ink 3D Shaders",
      accentHex = "#EC4899",
      platformIds = listOf("nintendo_3ds_pica200"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.PIXEL_LOCKED_3D,
      pixelTo3dBlend = 0.82f,
      extraVisualAudio = ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
        uncappedBloomAndRimFlares = true,
      ),
    ),
    PrismRomProfileSpec(
      comparisonTag = "SWITCH_HD2D",
      shortChipLabel = "SWITCH HD",
      displayName = "Nintendo Switch HD-2D",
      editionSubtitle = "SWITCH HD-2D VOXEL EDITION",
      hardwareBadgeSummary = "Switch Tegra + Voxelizer-FX + PokeCRT-RTX",
      accentHex = "#F97316",
      platformIds = listOf("nintendo_switch", "voxelizer_fx", "pokecrt_rtx"),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
      pixelTo3dBlend = 0.56f,
      extraVisualAudio = ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
        hd2dVolumetricRaysAndBokeh = true,
        foregroundParallaxFraming = true,
        fluid60FpsCharacterMotion = true,
      ),
    ),
    PrismRomProfileSpec(
      comparisonTag = "ULTIMATE_156_CURATED",
      shortChipLabel = "ALL 156",
      displayName = "Ultimate 156 Hardware",
      editionSubtitle = "ULTIMATE 156-HARDWARE CURATED EDITION",
      hardwareBadgeSummary = "All 156 Gaming Systems • Shovel Knight+ Curated",
      accentHex = "#FACC15",
      platformIds = emptyList(),
      useAllCatalogPlatforms = true,
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      characterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
      pixelTo3dBlend = 0.50f,
      extraVisualAudio = ExtraVisualAudioSettings.ALL_ENABLED,
    ),
  )

  fun encodeExtraPrettyMask(settings: ExtraVisualAudioSettings): Int {
    var mask = 0
    if (settings.ultraVoxelSlices) mask = mask or BIT_ULTRA_VOXEL
    if (settings.dynamicWeatherParticles) mask = mask or BIT_WEATHER
    if (settings.waterCausticsAndFoam) mask = mask or BIT_WATER
    if (settings.hd2dVolumetricRaysAndBokeh) mask = mask or BIT_HD2D_RAYS
    if (settings.foregroundParallaxFraming) mask = mask or BIT_PARALLAX
    if (settings.fluid60FpsCharacterMotion) mask = mask or BIT_FLUID_60
    if (settings.uncappedBloomAndRimFlares) mask = mask or BIT_BLOOM
    if (settings.analogCrtPhosphorAndCurvature) mask = mask or BIT_CRT
    if (settings.extraLushAudioReverbAndChoir) mask = mask or BIT_LUSH_AUDIO
    return mask
  }

  fun decodeExtraPrettyMask(mask: Int): ExtraVisualAudioSettings = ExtraVisualAudioSettings(
    ultraVoxelSlices = (mask and BIT_ULTRA_VOXEL) != 0,
    dynamicWeatherParticles = (mask and BIT_WEATHER) != 0,
    waterCausticsAndFoam = (mask and BIT_WATER) != 0,
    hd2dVolumetricRaysAndBokeh = (mask and BIT_HD2D_RAYS) != 0,
    foregroundParallaxFraming = (mask and BIT_PARALLAX) != 0,
    fluid60FpsCharacterMotion = (mask and BIT_FLUID_60) != 0,
    uncappedBloomAndRimFlares = (mask and BIT_BLOOM) != 0,
    analogCrtPhosphorAndCurvature = (mask and BIT_CRT) != 0,
    extraLushAudioReverbAndChoir = (mask and BIT_LUSH_AUDIO) != 0,
  )

  /**
   * Compiles a single comparison cartridge from the live game snapshot and chosen hardware profile.
   */
  fun compileComparisonRom(
    profile: PrismRomProfileSpec,
    romTitle: String,
    allCatalogIds: List<String>,
    activeRegionId: String,
    playerRow: Int,
    playerCol: Int,
    regionMapOverrides: Map<String, List<Int>>,
    playerTrainer: TrainerCharacterSpec,
    playerCustomAvatarRle: String,
    playerCustomAvatarPaletteJson: String,
    useCustomPlayerAvatarBitmap: Boolean,
    partyDexIds: List<Int>,
    partyLevels: Map<Int, Int>,
    heldItemsMap: Map<Int, String>,
    completedRegionIds: Set<String>,
    sideModeRecord: SideModeRecordEntity,
    compiledAtEpochMs: Long = System.currentTimeMillis(),
  ): PrismRomCartridge {
    val resolvedPlatformIds = if (profile.useAllCatalogPlatforms) {
      allCatalogIds.ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds.sorted() }
    } else {
      profile.platformIds
    }
    val slug = sanitizeRomSlug("${romTitle}_${profile.comparisonTag}")
    return PrismRomCartridge(
      schemaVersion = SCHEMA_VERSION,
      romId = slug,
      romTitle = romTitle.trim().ifBlank { "Pokémon: Prism Resonance" },
      comparisonTag = profile.comparisonTag,
      editionSubtitle = profile.editionSubtitle,
      hardwareBadgeSummary = profile.hardwareBadgeSummary,
      accentHex = profile.accentHex,
      activePlatformIds = resolvedPlatformIds,
      useAllCatalogPlatforms = profile.useAllCatalogPlatforms,
      disciplineModeToken = profile.disciplineMode.token,
      characterMixModeName = profile.characterMixMode.name,
      extraPrettyMask = encodeExtraPrettyMask(profile.extraVisualAudio),
      pixelTo3dBlend = profile.pixelTo3dBlend,
      activeRegionId = activeRegionId,
      playerRow = playerRow,
      playerCol = playerCol,
      regionMapOverrides = regionMapOverrides,
      playerTrainer = playerTrainer,
      playerCustomAvatarRle = playerCustomAvatarRle,
      playerCustomAvatarPaletteJson = playerCustomAvatarPaletteJson,
      useCustomPlayerAvatarBitmap = useCustomPlayerAvatarBitmap,
      partyDexIds = partyDexIds.ifEmpty { listOf(25, 6, 94, 448, 658, 384) },
      partyLevels = partyLevels,
      heldItemsMap = heldItemsMap,
      completedRegionIds = completedRegionIds.sorted(),
      sideModeRecord = sideModeRecord,
      compiledAtEpochMs = compiledAtEpochMs,
    )
  }

  /**
   * 1-Tap Batch Compiler: generates all 10 hardware comparison ROM cartridges cleanly.
   */
  fun compileAllTenComparisonRoms(
    romTitle: String,
    allCatalogIds: List<String>,
    activeRegionId: String,
    playerRow: Int,
    playerCol: Int,
    regionMapOverrides: Map<String, List<Int>>,
    playerTrainer: TrainerCharacterSpec,
    playerCustomAvatarRle: String,
    playerCustomAvatarPaletteJson: String,
    useCustomPlayerAvatarBitmap: Boolean,
    partyDexIds: List<Int>,
    partyLevels: Map<Int, Int>,
    heldItemsMap: Map<Int, String>,
    completedRegionIds: Set<String>,
    sideModeRecord: SideModeRecordEntity,
    compiledAtEpochMs: Long = System.currentTimeMillis(),
  ): List<PrismRomCartridge> = comparisonProfiles.map { profile ->
    compileComparisonRom(
      profile = profile,
      romTitle = romTitle,
      allCatalogIds = allCatalogIds,
      activeRegionId = activeRegionId,
      playerRow = playerRow,
      playerCol = playerCol,
      regionMapOverrides = regionMapOverrides,
      playerTrainer = playerTrainer,
      playerCustomAvatarRle = playerCustomAvatarRle,
      playerCustomAvatarPaletteJson = playerCustomAvatarPaletteJson,
      useCustomPlayerAvatarBitmap = useCustomPlayerAvatarBitmap,
      partyDexIds = partyDexIds,
      partyLevels = partyLevels,
      heldItemsMap = heldItemsMap,
      completedRegionIds = completedRegionIds,
      sideModeRecord = sideModeRecord,
      compiledAtEpochMs = compiledAtEpochMs,
    )
  }

  /**
   * Compiles the user's currently active custom Studio hardware combination into a `.prismrom`.
   */
  fun compileCurrentStudioRom(
    romTitle: String,
    activePlatformIds: Set<String>,
    allCatalogIds: List<String>,
    disciplineMode: MasterDisciplineMode,
    characterMixMode: CharacterMixMode,
    extraVisualAudio: ExtraVisualAudioSettings,
    pixelTo3dBlend: Float,
    hardwareSummaryLabel: String,
    activeRegionId: String,
    playerRow: Int,
    playerCol: Int,
    regionMapOverrides: Map<String, List<Int>>,
    playerTrainer: TrainerCharacterSpec,
    playerCustomAvatarRle: String,
    playerCustomAvatarPaletteJson: String,
    useCustomPlayerAvatarBitmap: Boolean,
    partyDexIds: List<Int>,
    partyLevels: Map<Int, Int>,
    heldItemsMap: Map<Int, String>,
    completedRegionIds: Set<String>,
    sideModeRecord: SideModeRecordEntity,
    compiledAtEpochMs: Long = System.currentTimeMillis(),
  ): PrismRomCartridge {
    val usesAll = allCatalogIds.isNotEmpty() && activePlatformIds.size >= allCatalogIds.size
    val profile = PrismRomProfileSpec(
      comparisonTag = "CURRENT_STUDIO_BUILD",
      shortChipLabel = "CUSTOM",
      displayName = "Current Studio Custom Build",
      editionSubtitle = "CUSTOM STUDIO FUSION EDITION",
      hardwareBadgeSummary = hardwareSummaryLabel.ifBlank { "${activePlatformIds.size} Hardware Active • ${disciplineMode.label}" },
      accentHex = "#00E5FF",
      platformIds = activePlatformIds.sorted(),
      useAllCatalogPlatforms = usesAll,
      disciplineMode = disciplineMode,
      characterMixMode = characterMixMode,
      pixelTo3dBlend = pixelTo3dBlend,
      extraVisualAudio = extraVisualAudio,
    )
    return compileComparisonRom(
      profile = profile,
      romTitle = romTitle,
      allCatalogIds = allCatalogIds,
      activeRegionId = activeRegionId,
      playerRow = playerRow,
      playerCol = playerCol,
      regionMapOverrides = regionMapOverrides,
      playerTrainer = playerTrainer,
      playerCustomAvatarRle = playerCustomAvatarRle,
      playerCustomAvatarPaletteJson = playerCustomAvatarPaletteJson,
      useCustomPlayerAvatarBitmap = useCustomPlayerAvatarBitmap,
      partyDexIds = partyDexIds,
      partyLevels = partyLevels,
      heldItemsMap = heldItemsMap,
      completedRegionIds = completedRegionIds,
      sideModeRecord = sideModeRecord,
      compiledAtEpochMs = compiledAtEpochMs,
    )
  }

  /**
   * Lossless `.prismrom` JSON serializer.
   */
  fun serializeRom(cartridge: PrismRomCartridge): String {
    val root = JSONObject()
    root.put("format", "PRISM_ROM_CARTRIDGE")
    root.put("schemaVersion", cartridge.schemaVersion)
    root.put("romId", cartridge.romId)
    root.put("romTitle", cartridge.romTitle)
    root.put("comparisonTag", cartridge.comparisonTag)
    root.put("editionSubtitle", cartridge.editionSubtitle)
    root.put("hardwareBadgeSummary", cartridge.hardwareBadgeSummary)
    root.put("accentHex", cartridge.accentHex)
    root.put("useAllCatalogPlatforms", cartridge.useAllCatalogPlatforms)
    root.put(
      "activePlatformIds",
      JSONArray().apply {
        val listToWrite = if (cartridge.useAllCatalogPlatforms) {
          listOf(UniversalHardwareRegistry.TOKEN_ALL_ON_RECORD)
        } else {
          cartridge.activePlatformIds
        }
        listToWrite.forEach { put(it) }
      },
    )
    root.put("disciplineModeToken", cartridge.disciplineModeToken)
    root.put("characterMixModeName", cartridge.characterMixModeName)
    root.put("extraPrettyMask", cartridge.extraPrettyMask)
    root.put("pixelTo3dBlend", cartridge.pixelTo3dBlend.toDouble())
    root.put("activeRegionId", cartridge.activeRegionId)
    root.put("playerRow", cartridge.playerRow)
    root.put("playerCol", cartridge.playerCol)
    root.put("compiledAtEpochMs", cartridge.compiledAtEpochMs)

    // 22-Region Custom Map Overrides
    val mapsObj = JSONObject()
    cartridge.regionMapOverrides.forEach { (regionId, tiles) ->
      mapsObj.put(regionId, tiles.joinToString(","))
    }
    root.put("regionMapOverrides", mapsObj)

    // Player Protagonist Spec + Optional Custom 16x16 Hero Avatar
    root.put("playerTrainer", serializeTrainerJson(cartridge.playerTrainer))
    root.put("playerCustomAvatarRle", cartridge.playerCustomAvatarRle)
    root.put("playerCustomAvatarPaletteJson", cartridge.playerCustomAvatarPaletteJson)
    root.put("useCustomPlayerAvatarBitmap", cartridge.useCustomPlayerAvatarBitmap)

    // Party, Levels, Held Items & Completed Regions
    root.put("partyDexIds", JSONArray().apply { cartridge.partyDexIds.forEach { put(it) } })
    val levelsObj = JSONObject()
    cartridge.partyLevels.forEach { (dex, lvl) -> levelsObj.put(dex.toString(), lvl) }
    root.put("partyLevels", levelsObj)
    val heldObj = JSONObject()
    cartridge.heldItemsMap.forEach { (dex, itemId) -> heldObj.put(dex.toString(), itemId) }
    root.put("heldItemsMap", heldObj)
    root.put("completedRegionIds", JSONArray().apply { cartridge.completedRegionIds.forEach { put(it) } })

    // Side Modes Record Snapshot
    val sm = cartridge.sideModeRecord
    root.put(
      "sideModeRecord",
      JSONObject().apply {
        put("projectId", sm.projectId)
        put("battleTowerBestStreak", sm.battleTowerBestStreak)
        put("battleTowerCurrentStreak", sm.battleTowerCurrentStreak)
        put("battlePoints", sm.battlePoints)
        put("contestRibbonsWon", sm.contestRibbonsWon)
        put("contestBestScore", sm.contestBestScore)
        put("bestSnapScore", sm.bestSnapScore)
        put("bestSnapStarRating", sm.bestSnapStarRating)
        put("bestSnapCaption", sm.bestSnapCaption)
        put("deepestRiftFloor", sm.deepestRiftFloor)
        put("riftGuardiansDefeated", sm.riftGuardiansDefeated)
        put("rangerCapturesCompleted", sm.rangerCapturesCompleted)
        put("campMealsServed", sm.campMealsServed)
      },
    )

    return root.toString(2)
  }

  /**
   * Lossless `.prismrom` JSON parser with full validation.
   */
  fun parseRom(rawJson: String): Result<PrismRomCartridge> = runCatching {
    val root = JSONObject(rawJson)
    val format = root.optString("format", "")
    require(format == "PRISM_ROM_CARTRIDGE" || root.has("romId")) {
      "File is not a valid .prismrom cartridge."
    }
    val romId = root.getString("romId").trim().ifBlank { error("Missing romId") }
    val romTitle = root.optString("romTitle", "Pokémon: Prism Resonance")
    val comparisonTag = root.optString("comparisonTag", "CURRENT_STUDIO_BUILD")
    val editionSubtitle = root.optString("editionSubtitle", "STANDALONE EDITION")
    val hardwareBadgeSummary = root.optString("hardwareBadgeSummary", comparisonTag)
    val accentHex = root.optString("accentHex", "#00E5FF")
    val useAll = root.optBoolean("useAllCatalogPlatforms", false)

    val platArr = root.optJSONArray("activePlatformIds") ?: JSONArray()
    val platformIds = buildList {
      for (i in 0 until platArr.length()) {
        val id = platArr.optString(i, "").trim()
        if (id.isNotEmpty() && id != UniversalHardwareRegistry.TOKEN_ALL_ON_RECORD) {
          add(id)
        }
      }
    }
    val effectiveUseAll = useAll || (platArr.length() == 1 && platArr.optString(0) == UniversalHardwareRegistry.TOKEN_ALL_ON_RECORD)

    val mapsObj = root.optJSONObject("regionMapOverrides")
    val regionMaps = linkedMapOf<String, List<Int>>()
    if (mapsObj != null) {
      val keys = mapsObj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val csv = mapsObj.optString(key, "")
        val parsedTiles = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (parsedTiles.isNotEmpty()) {
          regionMaps[key] = parsedTiles
        }
      }
    }

    val trainerObj = root.optJSONObject("playerTrainer")
    val trainer = if (trainerObj != null) {
      parseTrainerJson(trainerObj)
    } else {
      TrainerCatalogAndRigEngine.defaultProtagonist
    }

    val partyArr = root.optJSONArray("partyDexIds") ?: JSONArray()
    val party = buildList {
      for (i in 0 until partyArr.length()) {
        val dex = partyArr.optInt(i, -1)
        if (dex > 0) add(dex)
      }
    }.ifEmpty { listOf(25, 6, 94, 448, 658, 384) }

    val levelsMap = linkedMapOf<Int, Int>()
    root.optJSONObject("partyLevels")?.let { obj ->
      val keys = obj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        k.toIntOrNull()?.let { dex -> levelsMap[dex] = obj.optInt(k, 50) }
      }
    }

    val heldMap = linkedMapOf<Int, String>()
    root.optJSONObject("heldItemsMap")?.let { obj ->
      val keys = obj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val itemId = obj.optString(k, "").trim()
        val dex = k.toIntOrNull()
        if (dex != null && itemId.isNotEmpty()) heldMap[dex] = itemId
      }
    }

    val completedArr = root.optJSONArray("completedRegionIds") ?: JSONArray()
    val completedIds = buildList {
      for (i in 0 until completedArr.length()) {
        val id = completedArr.optString(i, "").trim()
        if (id.isNotEmpty()) add(id)
      }
    }

    val smObj = root.optJSONObject("sideModeRecord")
    val sideRecord = if (smObj != null) {
      SideModeRecordEntity(
        projectId = smObj.optString("projectId", romId),
        battleTowerBestStreak = smObj.optInt("battleTowerBestStreak", 0),
        battleTowerCurrentStreak = smObj.optInt("battleTowerCurrentStreak", 0),
        battlePoints = smObj.optInt("battlePoints", smObj.optInt("battleTowerCurrentBp", 0)),
        contestRibbonsWon = smObj.optInt("contestRibbonsWon", 0),
        contestBestScore = smObj.optInt("contestBestScore", 0),
        bestSnapScore = smObj.optInt("bestSnapScore", 0),
        bestSnapStarRating = smObj.optInt("bestSnapStarRating", 0),
        bestSnapCaption = smObj.optString("bestSnapCaption", ""),
        deepestRiftFloor = smObj.optInt("deepestRiftFloor", 0),
        riftGuardiansDefeated = smObj.optInt("riftGuardiansDefeated", 0),
        rangerCapturesCompleted = smObj.optInt("rangerCapturesCompleted", 0),
        campMealsServed = smObj.optInt("campMealsServed", 0),
      )
    } else {
      SideModeRecordEntity(projectId = romId)
    }

    PrismRomCartridge(
      schemaVersion = root.optInt("schemaVersion", SCHEMA_VERSION),
      romId = romId,
      romTitle = romTitle,
      comparisonTag = comparisonTag,
      editionSubtitle = editionSubtitle,
      hardwareBadgeSummary = hardwareBadgeSummary,
      accentHex = accentHex,
      activePlatformIds = platformIds,
      useAllCatalogPlatforms = effectiveUseAll,
      disciplineModeToken = root.optString("disciplineModeToken", MasterDisciplineMode.SHOVEL_KNIGHT_CURATED.token),
      characterMixModeName = root.optString("characterMixModeName", CharacterMixMode.SPLIT_BEAM_MORPH.name),
      extraPrettyMask = root.optInt("extraPrettyMask", 0),
      pixelTo3dBlend = root.optDouble("pixelTo3dBlend", 0.5).toFloat().coerceIn(0f, 1f),
      activeRegionId = root.optString("activeRegionId", MasterCanonTemplateCatalog.worldChapters.first().id),
      playerRow = root.optInt("playerRow", MasterCanonTemplateCatalog.worldChapters.first().spawnRow),
      playerCol = root.optInt("playerCol", MasterCanonTemplateCatalog.worldChapters.first().spawnCol),
      regionMapOverrides = regionMaps,
      playerTrainer = trainer,
      playerCustomAvatarRle = root.optString("playerCustomAvatarRle", ""),
      playerCustomAvatarPaletteJson = root.optString("playerCustomAvatarPaletteJson", ""),
      useCustomPlayerAvatarBitmap = root.optBoolean("useCustomPlayerAvatarBitmap", false),
      partyDexIds = party,
      partyLevels = levelsMap,
      heldItemsMap = heldMap,
      completedRegionIds = completedIds,
      sideModeRecord = sideRecord,
      compiledAtEpochMs = root.optLong("compiledAtEpochMs", System.currentTimeMillis()),
    )
  }

  private fun serializeTrainerJson(spec: TrainerCharacterSpec): JSONObject {
    val app = spec.appearance
    return JSONObject().apply {
      put("id", spec.id)
      put("name", spec.name)
      put("title", spec.title)
      put("roleCategory", spec.roleCategory.name)
      put("regionId", spec.regionId)
      put("sourceInspiration", spec.sourceInspiration)
      put("teamDexIds", JSONArray().apply { spec.teamDexIds.forEach { put(it) } })
      put("aiTactic", spec.aiTactic.name)
      put("preferredGimmick", spec.preferredGimmick.name)
      put("preferredWeather", spec.preferredWeather.name)
      put("introQuote", spec.introQuote)
      put("switchQuote", spec.switchQuote)
      put("defeatQuote", spec.defeatQuote)
      put("dialogueLines", JSONArray().apply { spec.dialogueLines.forEach { put(it) } })
      put("linkedPoiArchetypeId", spec.linkedPoiArchetypeId ?: JSONObject.NULL)
      put(
        "appearance",
        JSONObject().apply {
          put("skinHex", app.skinHex)
          put("hairHex", app.hairHex)
          put("hairStyleIndex", app.hairStyleIndex)
          put("headgear", app.headgear.name)
          put("outfitStyle", app.outfitStyle.name)
          put("outfitPrimaryHex", app.outfitPrimaryHex)
          put("outfitSecondaryHex", app.outfitSecondaryHex)
          put("accentHex", app.accentHex)
          put("companionPoseIndex", app.companionPoseIndex)
        },
      )
    }
  }

  private fun parseTrainerJson(obj: JSONObject): TrainerCharacterSpec {
    val fallback = TrainerCatalogAndRigEngine.defaultProtagonist
    val appObj = obj.optJSONObject("appearance")
    val app = if (appObj != null) {
      TrainerAppearanceConfig(
        skinHex = appObj.optString("skinHex", fallback.appearance.skinHex),
        hairHex = appObj.optString("hairHex", fallback.appearance.hairHex),
        hairStyleIndex = appObj.optInt("hairStyleIndex", fallback.appearance.hairStyleIndex),
        headgear = runCatching { TrainerHeadgear.valueOf(appObj.optString("headgear")) }
          .getOrDefault(fallback.appearance.headgear),
        outfitStyle = runCatching { TrainerOutfitStyle.valueOf(appObj.optString("outfitStyle")) }
          .getOrDefault(fallback.appearance.outfitStyle),
        outfitPrimaryHex = appObj.optString("outfitPrimaryHex", fallback.appearance.outfitPrimaryHex),
        outfitSecondaryHex = appObj.optString("outfitSecondaryHex", fallback.appearance.outfitSecondaryHex),
        accentHex = appObj.optString("accentHex", fallback.appearance.accentHex),
        companionPoseIndex = appObj.optInt("companionPoseIndex", fallback.appearance.companionPoseIndex),
      )
    } else {
      fallback.appearance
    }
    val teamArr = obj.optJSONArray("teamDexIds") ?: JSONArray()
    val team = buildList {
      for (i in 0 until teamArr.length()) {
        val dex = teamArr.optInt(i, -1)
        if (dex > 0) add(dex)
      }
    }.ifEmpty { fallback.teamDexIds }
    val dialogueArr = obj.optJSONArray("dialogueLines")
    val dialogue = if (dialogueArr != null) {
      buildList {
        for (i in 0 until dialogueArr.length()) {
          val line = dialogueArr.optString(i, "")
          if (line.isNotBlank()) add(line)
        }
      }.ifEmpty { fallback.dialogueLines }
    } else {
      fallback.dialogueLines
    }
    return TrainerCharacterSpec(
      id = obj.optString("id", fallback.id),
      name = obj.optString("name", fallback.name),
      title = obj.optString("title", fallback.title),
      roleCategory = runCatching { TrainerRoleCategory.valueOf(obj.optString("roleCategory")) }
        .getOrDefault(TrainerRoleCategory.PROTAGONIST),
      regionId = obj.optString("regionId", fallback.regionId),
      sourceInspiration = obj.optString("sourceInspiration", fallback.sourceInspiration),
      teamDexIds = team,
      aiTactic = runCatching { TrainerAiTactic.valueOf(obj.optString("aiTactic")) }
        .getOrDefault(fallback.aiTactic),
      preferredGimmick = runCatching { BattleGimmickMode.valueOf(obj.optString("preferredGimmick")) }
        .getOrDefault(fallback.preferredGimmick),
      preferredWeather = runCatching { WeatherCondition.valueOf(obj.optString("preferredWeather")) }
        .getOrDefault(fallback.preferredWeather),
      introQuote = obj.optString("introQuote", fallback.introQuote),
      switchQuote = obj.optString("switchQuote", fallback.switchQuote),
      defeatQuote = obj.optString("defeatQuote", fallback.defeatQuote),
      dialogueLines = dialogue,
      appearance = app,
      linkedPoiArchetypeId = obj.optString("linkedPoiArchetypeId", "").ifBlank { null },
    )
  }

  // ===============================================================================================
  // INTERNAL `.prismrom` CARTRIDGE STORAGE (filesDir/prism_roms/<romId>.prismrom)
  // ===============================================================================================

  fun getRomLibraryDir(context: Context): File = File(context.filesDir, ROM_DIR_NAME).apply {
    if (!exists()) mkdirs()
  }

  fun romFileForId(context: Context, romId: String): File =
    File(getRomLibraryDir(context), "${sanitizeRomSlug(romId)}$ROM_EXTENSION")

  fun saveRomToLibrary(context: Context, cartridge: PrismRomCartridge): File {
    val file = romFileForId(context, cartridge.romId)
    file.writeText(serializeRom(cartridge), Charsets.UTF_8)
    return file
  }

  fun listCompiledRoms(context: Context): List<PrismRomCartridge> {
    val dir = getRomLibraryDir(context)
    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(ROM_EXTENSION) } ?: return emptyList()
    val profileOrder = comparisonProfiles.mapIndexed { idx, p -> p.comparisonTag to idx }.toMap()
    return files.mapNotNull { file ->
      runCatching { parseRom(file.readText(Charsets.UTF_8)).getOrNull() }.getOrNull()
    }.sortedWith(
      compareBy<PrismRomCartridge> { profileOrder[it.comparisonTag] ?: 100 }
        .thenByDescending { it.compiledAtEpochMs },
    )
  }

  fun loadRomById(context: Context, romId: String): PrismRomCartridge? {
    val file = romFileForId(context, romId)
    if (!file.exists()) return null
    return runCatching { parseRom(file.readText(Charsets.UTF_8)).getOrNull() }.getOrNull()
  }

  fun loadRomFromPath(path: String): PrismRomCartridge? {
    val file = File(path)
    if (!file.exists()) return null
    return runCatching { parseRom(file.readText(Charsets.UTF_8)).getOrNull() }.getOrNull()
  }

  fun loadRomFromUri(context: Context, uri: Uri): Result<PrismRomCartridge> = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
      stream.bufferedReader(Charsets.UTF_8).readText()
    } ?: error("Unable to open .prismrom content stream.")
    parseRom(raw).getOrThrow()
  }

  fun deleteRomFromLibrary(context: Context, romId: String): Boolean {
    val file = romFileForId(context, romId)
    return file.exists() && file.delete()
  }

  // ===============================================================================================
  // STANDALONE LAUNCH INTENT & PROCEDURAL HOME-SCREEN SHORTCUT ICON INSTALLER
  // ===============================================================================================

  /**
   * Builds a multi-document standalone launch Intent so every comparison ROM opens as its own
   * isolated Recent Apps task window inside [PrismRomPlayerActivity].
   */
  fun buildLaunchIntent(
    context: Context,
    romId: String,
    romPath: String? = null,
  ): Intent = Intent(context, PrismRomPlayerActivity::class.java).apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse("prismrom://cartridge/${sanitizeRomSlug(romId)}")
    putExtra(EXTRA_ROM_ID, romId)
    if (!romPath.isNullOrBlank()) {
      putExtra(EXTRA_ROM_PATH, romPath)
    }
    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
  }

  /**
   * Installs a color-coded Home-Screen shortcut icon for a compiled comparison ROM using Android's
   * native [ShortcutManagerCompat] (zero extra permissions, zero dynamic code loading).
   */
  fun requestPinRomHomeShortcut(
    context: Context,
    cartridge: PrismRomCartridge,
  ): PinShortcutOutcome {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
      return PinShortcutOutcome.NOT_SUPPORTED_ON_LAUNCHER
    }
    return runCatching {
      val romFile = saveRomToLibrary(context, cartridge)
      val iconBitmap = renderCartridgeIconBitmap(cartridge, sizePx = 192)
      val launchIntent = buildLaunchIntent(
        context = context,
        romId = cartridge.romId,
        romPath = romFile.absolutePath,
      )
      val shortcut = ShortcutInfoCompat.Builder(context, "prismrom_${sanitizeRomSlug(cartridge.romId)}")
        .setShortLabel(cartridge.shortShortcutLabel)
        .setLongLabel(cartridge.longShortcutLabel)
        .setIcon(IconCompat.createWithBitmap(iconBitmap))
        .setIntent(launchIntent)
        .build()
      val accepted = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
      if (accepted) PinShortcutOutcome.PIN_REQUESTED else PinShortcutOutcome.NOT_SUPPORTED_ON_LAUNCHER
    }.getOrDefault(PinShortcutOutcome.FAILED)
  }

  /**
   * Procedurally renders a crisp, color-coded retro-modern `.prismrom` cartridge badge bitmap
   * for both the in-app Comparison ROM Exporter cards and Android Home Screen shortcut icons.
   */
  fun renderCartridgeIconBitmap(
    cartridge: PrismRomCartridge,
    sizePx: Int = 192,
  ): Bitmap {
    val size = sizePx.coerceIn(64, 512)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val accent = runCatching { Color.parseColor(cartridge.accentHex) }.getOrDefault(Color.parseColor("#00E5FF"))
    val shellDark = Color.parseColor("#11182B")
    val shellRim = Color.parseColor("#243356")
    val labelBg = Color.parseColor("#0A1020")
    val goldPin = Color.parseColor("#FACC15")

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val s = size.toFloat()

    // Outer adaptive backdrop rounded square
    paint.color = Color.parseColor("#070C18")
    canvas.drawRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, paint)

    // Cartridge body silhouette with notched upper-right corner
    val bodyRect = RectF(s * 0.14f, s * 0.08f, s * 0.86f, s * 0.92f)
    paint.color = shellDark
    canvas.drawRoundRect(bodyRect, s * 0.08f, s * 0.08f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = s * 0.028f
    paint.color = accent
    canvas.drawRoundRect(bodyRect, s * 0.08f, s * 0.08f, paint)
    paint.style = Paint.Style.FILL

    // Molded grip grooves along cartridge top
    paint.color = shellRim
    for (i in 0..2) {
      val y = s * (0.135f + i * 0.032f)
      canvas.drawRoundRect(RectF(s * 0.22f, y, s * 0.78f, y + s * 0.015f), s * 0.01f, s * 0.01f, paint)
    }

    // Center cartridge sticker bay
    val labelRect = RectF(s * 0.20f, s * 0.25f, s * 0.80f, s * 0.78f)
    paint.color = labelBg
    canvas.drawRoundRect(labelRect, s * 0.045f, s * 0.045f, paint)

    // Top accent stripe inside sticker
    paint.color = accent
    canvas.drawRoundRect(RectF(s * 0.20f, s * 0.25f, s * 0.80f, s * 0.37f), s * 0.045f, s * 0.045f, paint)

    // Glowing Prism Diamond Emblem
    val cx = s * 0.50f
    val cy = s * 0.50f
    val r = s * 0.105f
    val diamond = Path().apply {
      moveTo(cx, cy - r)
      lineTo(cx + r * 0.82f, cy)
      lineTo(cx, cy + r)
      lineTo(cx - r * 0.82f, cy)
      close()
    }
    paint.color = accent
    canvas.drawPath(diamond, paint)
    val innerDiamond = Path().apply {
      moveTo(cx, cy - r * 0.55f)
      lineTo(cx + r * 0.45f, cy)
      lineTo(cx, cy + r * 0.55f)
      lineTo(cx - r * 0.45f, cy)
      close()
    }
    paint.color = Color.WHITE
    canvas.drawPath(innerDiamond, paint)

    // Comparison Tag Text (e.g., "GB", "SNES", "PS1", "156")
    val tagBadge = comparisonProfiles
      .firstOrNull { it.comparisonTag == cartridge.comparisonTag }
      ?.shortChipLabel
      ?: "CUSTOM"
    paint.color = Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    paint.textSize = s * 0.11f
    canvas.drawText(tagBadge.take(9), cx, s * 0.725f, paint)

    // Header micro text
    paint.color = Color.parseColor("#08101E")
    paint.textSize = s * 0.068f
    canvas.drawText("PRISM ROM", cx, s * 0.335f, paint)

    // Gold edge connector pins at bottom
    paint.color = goldPin
    val pinCount = 7
    val startX = s * 0.26f
    val stepX = (s * 0.48f) / (pinCount - 1).coerceAtLeast(1)
    for (p in 0 until pinCount) {
      val px = startX + p * stepX
      canvas.drawRoundRect(
        RectF(px - s * 0.016f, s * 0.825f, px + s * 0.016f, s * 0.895f),
        s * 0.008f,
        s * 0.008f,
        paint,
      )
    }

    return bmp
  }

  fun sanitizeRomSlug(raw: String): String = raw
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .ifBlank { "prism_resonance_rom" }
    .take(64)
}

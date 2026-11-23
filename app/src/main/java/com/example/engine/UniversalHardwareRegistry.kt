package com.example.engine

import android.content.Context
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import org.json.JSONArray
import kotlin.math.roundToInt

enum class HardwarePrimaryDomain(val label: String) {
  SPRITE_2D("2D Sprite Engine"),
  MODEL_3D("3D Polygon/Model"),
  HYBRID_25D_BRIDGE("2.5D Hybrid Bridge"),
  DISPLAY_OUTPUT("Display / Output"),
}

data class GraphicsTraitVector(
  val virtualPixelGrid: Float, // 64f..208f
  val outlineWeight: Float,    // 0f..1f
  val celBands: Float,         // 2f..6f
  val bayerDither: Float,      // 0f..1f
  val affineWarp: Float,       // 0f..1f
  val voxelDepth: Float,       // 0f..1f
  val vertexSnap: Float,       // 0f..1f
  val bloom: Float,            // 0f..1f
  val rimLight: Float,         // 0f..1f
  val crtScanline: Float,      // 0f..1f
  val dotMatrixLcd: Float,     // 0f..1f
  val parallaxLayers: Float,   // 1f..8f
)

data class AudioTraitVector(
  val pulseDutyWeight: Float,     // 0f..1f
  val triangleBassWeight: Float,  // 0f..1f
  val fmSlapWeight: Float,        // 0f..1f
  val sidArpWeight: Float,        // 0f..1f
  val spcEchoWeight: Float,       // 0f..1f
  val orchestralPadWeight: Float, // 0f..1f
  val lfsrNoiseWeight: Float,     // 0f..1f
  val bitcrushDepth: Float,       // 0f..1f
  val stereoWidth: Float,         // 0f..1f
)

data class HardwarePlatformSpec(
  val id: String,
  val name: String,
  val manufacturer: String,
  val year: Int,
  val generation: String,
  val category: String,
  val gpuChip: String,
  val audioChip: String,
  val videoModes: String,
  val primaryDomain: HardwarePrimaryDomain,
  val honoredSignature: String,
  val brokenLimitation: String,
  val graphicsTraits: GraphicsTraitVector,
  val audioTraits: AudioTraitVector,
)

data class ConvergedVisualParams(
  val activeCount: Int,
  val disciplineMode: MasterDisciplineMode,
  val virtualPixelGrid: Float,
  val outlineWeight: Float,
  val celBands: Int,
  val bayerDither: Float,
  val affineWarp: Float,
  val voxelDepth: Float,
  val voxelSliceCount: Int,
  val vertexSnap: Float,
  val bloom: Float,
  val rimLight: Float,
  val crtScanline: Float,
  val dotMatrixLcd: Float,
  val parallaxLayers: Int,
  val characterStepFps: Int,
  val weatherParticlesEnabled: Boolean,
  val waterCausticsEnabled: Boolean,
  val volumetricRaysEnabled: Boolean,
  val foregroundParallaxEnabled: Boolean,
  val crtCurvatureEnabled: Boolean,
  val signatureSummary: String,
)

data class ConvergedAudioParams(
  val activeCount: Int,
  val pulseLeadGain: Float,
  val triangleBassGain: Float,
  val fmSlapBassGain: Float,
  val sidArpGain: Float,
  val spcEchoFeedback: Float,
  val orchestralPadGain: Float,
  val lfsrPercussionGain: Float,
  val bitcrushDepth: Float,
  val stereoWidth: Float,
  val extraLushChoirReverb: Boolean,
  val audioSignatureLabel: String,
)

/**
 * Loads all 85+ gaming hardware profiles from assets/hardware_registry.json and solves
 * unified visual and audio parameters without signal muddying using peak-preserving
 * weighted normalization (65% peak + 35% active mean).
 */
object UniversalHardwareRegistry {
  const val ASSET_FILE = "hardware_registry.json"
  const val TOKEN_ALL_ON_RECORD = "ALL_ON_RECORD"

  val defaultCuratedHardwareIds: Set<String> = setOf(
    "nes_famicom",
    "game_boy_color",
    "snes_sfc",
    "super_fx2",
    "sa_1",
    "sega_genesis",
    "sega_32x",
    "sgb2_border",
    "gba_agb",
    "n64_rcp",
    "gamecube_tev",
    "nintendo_ds_dsi",
    "nintendo_3ds_pica200",
    "nintendo_switch",
    "msu_1",
    "voxelizer_fx",
    "pokecrt_rtx",
  )

  fun loadFromAssets(context: Context): List<HardwarePlatformSpec> {
    val json = runCatching {
      context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
    }.getOrNull() ?: return fallbackMinimalCatalog()
    return parseJson(json)
  }

  fun parseJson(jsonText: String): List<HardwarePlatformSpec> {
    val array = JSONArray(jsonText)
    val result = ArrayList<HardwarePlatformSpec>(array.length())
    for (i in 0 until array.length()) {
      val obj = array.getJSONObject(i)
      val gfx = obj.getJSONObject("graphicsTraits")
      val aud = obj.getJSONObject("audioTraits")
      val domain = runCatching {
        HardwarePrimaryDomain.valueOf(obj.getString("primaryDomain"))
      }.getOrDefault(HardwarePrimaryDomain.SPRITE_2D)

      result.add(
        HardwarePlatformSpec(
          id = obj.getString("id"),
          name = obj.getString("name"),
          manufacturer = obj.getString("manufacturer"),
          year = obj.getInt("year"),
          generation = obj.getString("generation"),
          category = obj.getString("category"),
          gpuChip = obj.getString("gpuChip"),
          audioChip = obj.getString("audioChip"),
          videoModes = obj.getString("videoModes"),
          primaryDomain = domain,
          honoredSignature = obj.getString("honoredSignature"),
          brokenLimitation = obj.getString("brokenLimitation"),
          graphicsTraits = GraphicsTraitVector(
            virtualPixelGrid = gfx.getDouble("virtualPixelGrid").toFloat(),
            outlineWeight = gfx.getDouble("outlineWeight").toFloat(),
            celBands = gfx.getDouble("celBands").toFloat(),
            bayerDither = gfx.getDouble("bayerDither").toFloat(),
            affineWarp = gfx.getDouble("affineWarp").toFloat(),
            voxelDepth = gfx.getDouble("voxelDepth").toFloat(),
            vertexSnap = gfx.getDouble("vertexSnap").toFloat(),
            bloom = gfx.getDouble("bloom").toFloat(),
            rimLight = gfx.getDouble("rimLight").toFloat(),
            crtScanline = gfx.getDouble("crtScanline").toFloat(),
            dotMatrixLcd = gfx.getDouble("dotMatrixLcd").toFloat(),
            parallaxLayers = gfx.getDouble("parallaxLayers").toFloat(),
          ),
          audioTraits = AudioTraitVector(
            pulseDutyWeight = aud.getDouble("pulseDutyWeight").toFloat(),
            triangleBassWeight = aud.getDouble("triangleBassWeight").toFloat(),
            fmSlapWeight = aud.getDouble("fmSlapWeight").toFloat(),
            sidArpWeight = aud.getDouble("sidArpWeight").toFloat(),
            spcEchoWeight = aud.getDouble("spcEchoWeight").toFloat(),
            orchestralPadWeight = aud.getDouble("orchestralPadWeight").toFloat(),
            lfsrNoiseWeight = aud.getDouble("lfsrNoiseWeight").toFloat(),
            bitcrushDepth = aud.getDouble("bitcrushDepth").toFloat(),
            stereoWidth = aud.getDouble("stereoWidth").toFloat(),
          ),
        )
      )
    }
    return result
  }

  /**
   * Compact Firestore token representation so selecting all 85+ platforms never truncates
   * or exceeds Firestore document limits. Full checklists live in Room SQLite.
   */
  fun encodeSelectionToken(
    activeIds: Set<String>,
    allCatalogIds: Set<String>,
    disciplineMode: MasterDisciplineMode,
  ): String {
    if (allCatalogIds.isNotEmpty() && activeIds.size >= allCatalogIds.size) {
      return "${disciplineMode.token}|$TOKEN_ALL_ON_RECORD"
    }
    if (activeIds == defaultCuratedHardwareIds && disciplineMode == MasterDisciplineMode.SHOVEL_KNIGHT_CURATED) {
      return MasterDisciplineMode.SHOVEL_KNIGHT_CURATED.token
    }
    val sorted = activeIds.sorted().joinToString(",")
    return "${disciplineMode.token}|$sorted".take(1950)
  }

  fun decodeSelectionToken(
    token: String,
    allCatalogIds: Set<String>,
  ): Pair<MasterDisciplineMode, Set<String>> {
    val clean = token.trim()
    if (clean.isEmpty()) return MasterDisciplineMode.SHOVEL_KNIGHT_CURATED to defaultCuratedHardwareIds
    val parts = clean.split("|", limit = 2)
    val mode = MasterDisciplineMode.entries.firstOrNull { it.token == parts[0] }
      ?: MasterDisciplineMode.SHOVEL_KNIGHT_CURATED
    val payload = parts.getOrNull(1) ?: parts[0]
    val ids = when {
      payload == TOKEN_ALL_ON_RECORD -> allCatalogIds.ifEmpty { defaultCuratedHardwareIds }
      payload == MasterDisciplineMode.SHOVEL_KNIGHT_CURATED.token -> defaultCuratedHardwareIds
      payload == MasterDisciplineMode.MAXIMALIST_PRETTY.token -> allCatalogIds.ifEmpty { defaultCuratedHardwareIds }
      else -> payload.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        .ifEmpty { defaultCuratedHardwareIds }
    }
    return mode to ids
  }

  private fun fallbackMinimalCatalog(): List<HardwarePlatformSpec> = listOf(
    HardwarePlatformSpec(
      id = "snes_sfc",
      name = "SNES / Super Famicom",
      manufacturer = "Nintendo",
      year = 1990,
      generation = "Gen 4",
      category = "Console",
      gpuChip = "S-PPU1/2",
      audioChip = "Sony SPC700",
      videoModes = "256x224 Mode-7",
      primaryDomain = HardwarePrimaryDomain.SPRITE_2D,
      honoredSignature = "15-bit painterly ramps and Mode-7 horizon tilt",
      brokenLimitation = "3.58MHz CPU slowdown",
      graphicsTraits = GraphicsTraitVector(144f, 0.84f, 5f, 0.26f, 0.75f, 0.35f, 0f, 0.45f, 0.60f, 0.28f, 0f, 6f),
      audioTraits = AudioTraitVector(0.62f, 0.70f, 0.30f, 0.50f, 0.96f, 0.82f, 0.55f, 0.10f, 0.88f),
    ),
  )
}

/**
 * Peak-preserving convergence solver (65% peak + 35% active mean) so stacking 85+
 * platforms produces a bold, crisp unified art pipeline instead of gray average mud.
 */
object HardwareConvergenceSolver {
  private fun blendPeakAndMean(values: List<Float>, fallback: Float): Float {
    if (values.isEmpty()) return fallback
    val peak = values.max()
    val mean = values.average().toFloat()
    return 0.65f * peak + 0.35f * mean
  }

  fun solveVisuals(
    activePlatforms: List<HardwarePlatformSpec>,
    disciplineMode: MasterDisciplineMode,
    extraPretty: ExtraVisualAudioSettings,
    userSliderOutline: Float = 0.70f,
    userSliderBloom: Float = 0.68f,
    userSliderScanline: Float = 0.24f,
    userSliderVoxel: Float = 0.72f,
    userSliderAffine: Float = 0.45f,
  ): ConvergedVisualParams {
    val active = activePlatforms.ifEmpty { return defaultVisualParams(disciplineMode, extraPretty) }

    val rawGrid = blendPeakAndMean(active.map { it.graphicsTraits.virtualPixelGrid }, 160f)
    val rawOutline = blendPeakAndMean(active.map { it.graphicsTraits.outlineWeight }, 0.84f)
    val rawCel = blendPeakAndMean(active.map { it.graphicsTraits.celBands }, 5f)
    val rawDither = blendPeakAndMean(active.map { it.graphicsTraits.bayerDither }, 0.24f)
    val rawAffine = blendPeakAndMean(active.map { it.graphicsTraits.affineWarp }, 0.40f)
    val rawVoxel = blendPeakAndMean(active.map { it.graphicsTraits.voxelDepth }, 0.60f)
    val rawVertexSnap = blendPeakAndMean(active.map { it.graphicsTraits.vertexSnap }, 0.10f)
    val rawBloom = blendPeakAndMean(active.map { it.graphicsTraits.bloom }, 0.52f)
    val rawRim = blendPeakAndMean(active.map { it.graphicsTraits.rimLight }, 0.70f)
    val rawCrt = blendPeakAndMean(active.map { it.graphicsTraits.crtScanline }, 0.24f)
    val rawLcd = blendPeakAndMean(active.map { it.graphicsTraits.dotMatrixLcd }, 0.10f)
    val rawParallax = blendPeakAndMean(active.map { it.graphicsTraits.parallaxLayers }, 6f)

    // Combine converged hardware traits with interactive studio sliders
    var pixelGrid = rawGrid
    var outline = (rawOutline * 0.65f + userSliderOutline * 0.35f).coerceIn(0f, 1f)
    var celBands = rawCel.roundToInt().coerceIn(2, 6)
    var dither = rawDither.coerceIn(0f, 1f)
    var affine = (rawAffine * 0.55f + userSliderAffine * 0.45f).coerceIn(0f, 1f)
    var voxel = (rawVoxel * 0.55f + userSliderVoxel * 0.45f).coerceIn(0f, 1f)
    var vertexSnap = rawVertexSnap.coerceIn(0f, 1f)
    var bloom = (rawBloom * 0.55f + userSliderBloom * 0.45f).coerceIn(0f, 1f)
    var rim = rawRim.coerceIn(0f, 1f)
    var crt = (rawCrt * 0.50f + userSliderScanline * 0.50f).coerceIn(0f, 1f)
    var lcd = rawLcd.coerceIn(0f, 1f)
    var parallax = rawParallax.roundToInt().coerceIn(1, 8)

    // Apply Master Discipline curation rules
    when (disciplineMode) {
      MasterDisciplineMode.SHOVEL_KNIGHT_CURATED -> {
        // Shared crisp virtual pixel grid, clean 1px ink outline, restrained CRT/bloom so silhouette pops
        pixelGrid = pixelGrid.coerceIn(128f, 176f)
        outline = outline.coerceAtLeast(0.76f)
        celBands = celBands.coerceIn(3, 5)
        bloom = if (extraPretty.uncappedBloomAndRimFlares) bloom.coerceAtLeast(0.75f) else bloom.coerceAtMost(0.58f)
        crt = if (extraPretty.analogCrtPhosphorAndCurvature) crt.coerceAtLeast(0.42f) else crt.coerceAtMost(0.32f)
        lcd = lcd.coerceAtMost(0.28f)
        vertexSnap = vertexSnap.coerceAtMost(0.22f)
      }
      MasterDisciplineMode.MAXIMALIST_PRETTY -> {
        pixelGrid = pixelGrid.coerceIn(160f, 208f)
        outline = outline.coerceAtLeast(0.72f)
        bloom = (bloom * 1.25f).coerceIn(0.68f, 1.0f)
        rim = (rim * 1.18f).coerceIn(0.75f, 1.0f)
        voxel = (voxel * 1.15f).coerceIn(0.65f, 1.0f)
        parallax = (parallax + 1).coerceAtMost(8)
      }
      MasterDisciplineMode.RAW_HARDWARE_PURIST -> {
        // Unfiltered fidelity to active platforms (authentic DMG dot-matrix, GBC dither, PS1 vertex snap)
        pixelGrid = rawGrid.coerceIn(64f, 208f)
        outline = rawOutline.coerceIn(0f, 1f)
        dither = rawDither.coerceIn(0f, 1f)
        vertexSnap = rawVertexSnap.coerceIn(0f, 1f)
        crt = rawCrt.coerceIn(0f, 1f)
        lcd = rawLcd.coerceIn(0f, 1f)
      }
    }

    // Apply explicit Extra Pretty opt-in overrides
    val voxelSlices = if (extraPretty.ultraVoxelSlices || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY) 14 else 8
    val stepFps = if (extraPretty.fluid60FpsCharacterMotion || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY) 60 else 14
    if (extraPretty.uncappedBloomAndRimFlares) {
      bloom = (bloom + 0.28f).coerceIn(0f, 1f)
      rim = (rim + 0.22f).coerceIn(0f, 1f)
    }
    if (extraPretty.analogCrtPhosphorAndCurvature) {
      crt = (crt + 0.30f).coerceIn(0.35f, 1f)
    }

    val topNames = active.take(3).joinToString(" + ") { it.name.substringBefore(" (") }
    val summary = if (active.size > 3) "$topNames + ${active.size - 3} more" else topNames

    return ConvergedVisualParams(
      activeCount = active.size,
      disciplineMode = disciplineMode,
      virtualPixelGrid = pixelGrid,
      outlineWeight = outline,
      celBands = celBands,
      bayerDither = dither,
      affineWarp = affine,
      voxelDepth = voxel,
      voxelSliceCount = voxelSlices,
      vertexSnap = vertexSnap,
      bloom = bloom,
      rimLight = rim,
      crtScanline = crt,
      dotMatrixLcd = lcd,
      parallaxLayers = parallax,
      characterStepFps = stepFps,
      weatherParticlesEnabled = extraPretty.dynamicWeatherParticles || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY,
      waterCausticsEnabled = extraPretty.waterCausticsAndFoam || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY,
      volumetricRaysEnabled = extraPretty.hd2dVolumetricRaysAndBokeh || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY,
      foregroundParallaxEnabled = extraPretty.foregroundParallaxFraming || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY,
      crtCurvatureEnabled = extraPretty.analogCrtPhosphorAndCurvature,
      signatureSummary = summary,
    )
  }

  private fun defaultVisualParams(
    disciplineMode: MasterDisciplineMode,
    extraPretty: ExtraVisualAudioSettings,
  ): ConvergedVisualParams = ConvergedVisualParams(
    activeCount = 1,
    disciplineMode = disciplineMode,
    virtualPixelGrid = 160f,
    outlineWeight = 0.85f,
    celBands = 4,
    bayerDither = 0.25f,
    affineWarp = 0.45f,
    voxelDepth = 0.72f,
    voxelSliceCount = if (extraPretty.ultraVoxelSlices) 14 else 8,
    vertexSnap = 0.08f,
    bloom = 0.52f,
    rimLight = 0.72f,
    crtScanline = 0.24f,
    dotMatrixLcd = 0.10f,
    parallaxLayers = 6,
    characterStepFps = if (extraPretty.fluid60FpsCharacterMotion) 60 else 14,
    weatherParticlesEnabled = extraPretty.dynamicWeatherParticles,
    waterCausticsEnabled = extraPretty.waterCausticsAndFoam,
    volumetricRaysEnabled = extraPretty.hd2dVolumetricRaysAndBokeh,
    foregroundParallaxEnabled = extraPretty.foregroundParallaxFraming,
    crtCurvatureEnabled = extraPretty.analogCrtPhosphorAndCurvature,
    signatureSummary = "Shovel Knight+ Curated Baseline",
  )
}

object AudioConvergenceSolver {
  private fun blendPeakAndMean(values: List<Float>, fallback: Float): Float {
    if (values.isEmpty()) return fallback
    return 0.65f * values.max() + 0.35f * values.average().toFloat()
  }

  fun solveAudio(
    activePlatforms: List<HardwarePlatformSpec>,
    disciplineMode: MasterDisciplineMode,
    extraPretty: ExtraVisualAudioSettings,
  ): ConvergedAudioParams {
    val active = activePlatforms.ifEmpty {
      return ConvergedAudioParams(
        activeCount = 1,
        pulseLeadGain = 0.82f,
        triangleBassGain = 0.80f,
        fmSlapBassGain = 0.68f,
        sidArpGain = 0.56f,
        spcEchoFeedback = 0.58f,
        orchestralPadGain = 0.62f,
        lfsrPercussionGain = 0.72f,
        bitcrushDepth = 0.08f,
        stereoWidth = 0.84f,
        extraLushChoirReverb = extraPretty.extraLushAudioReverbAndChoir,
        audioSignatureLabel = "NES+YM2612+SPC700+MSU-1",
      )
    }

    val pulse = blendPeakAndMean(active.map { it.audioTraits.pulseDutyWeight }, 0.75f).coerceIn(0.1f, 1f)
    val tri = blendPeakAndMean(active.map { it.audioTraits.triangleBassWeight }, 0.75f).coerceIn(0.15f, 1f)
    val fm = blendPeakAndMean(active.map { it.audioTraits.fmSlapWeight }, 0.65f).coerceIn(0f, 1f)
    val sid = blendPeakAndMean(active.map { it.audioTraits.sidArpWeight }, 0.55f).coerceIn(0f, 1f)
    var echo = blendPeakAndMean(active.map { it.audioTraits.spcEchoWeight }, 0.60f).coerceIn(0f, 0.92f)
    var pad = blendPeakAndMean(active.map { it.audioTraits.orchestralPadWeight }, 0.62f).coerceIn(0f, 1f)
    val lfsr = blendPeakAndMean(active.map { it.audioTraits.lfsrNoiseWeight }, 0.70f).coerceIn(0.1f, 1f)
    var bitcrush = blendPeakAndMean(active.map { it.audioTraits.bitcrushDepth }, 0.12f).coerceIn(0f, 0.65f)
    val width = blendPeakAndMean(active.map { it.audioTraits.stereoWidth }, 0.82f).coerceIn(0.15f, 1f)

    val lush = extraPretty.extraLushAudioReverbAndChoir || disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY
    when (disciplineMode) {
      MasterDisciplineMode.SHOVEL_KNIGHT_CURATED -> {
        bitcrush = bitcrush.coerceAtMost(0.18f)
        echo = if (lush) echo.coerceAtLeast(0.72f) else echo.coerceAtMost(0.62f)
      }
      MasterDisciplineMode.MAXIMALIST_PRETTY -> {
        bitcrush = bitcrush.coerceAtMost(0.08f)
        echo = (echo + 0.18f).coerceAtMost(0.88f)
        pad = (pad + 0.20f).coerceAtMost(1.0f)
      }
      MasterDisciplineMode.RAW_HARDWARE_PURIST -> {
        // Preserve authentic raw chip grit and stereo field
      }
    }

    val dominantChips = buildList {
      if (pulse >= 0.72f) add("2A03/VRC6")
      if (fm >= 0.68f) add("YM2612")
      if (sid >= 0.62f) add("SID")
      if (echo >= 0.68f) add("SPC700/SPU")
      if (pad >= 0.72f) add("MSU-1")
    }.ifEmpty { listOf("Omni-DSP") }.joinToString(" + ")

    return ConvergedAudioParams(
      activeCount = active.size,
      pulseLeadGain = pulse,
      triangleBassGain = tri,
      fmSlapBassGain = fm,
      sidArpGain = sid,
      spcEchoFeedback = echo,
      orchestralPadGain = pad,
      lfsrPercussionGain = lfsr,
      bitcrushDepth = bitcrush,
      stereoWidth = width,
      extraLushChoirReverb = lush,
      audioSignatureLabel = dominantChips,
    )
  }
}

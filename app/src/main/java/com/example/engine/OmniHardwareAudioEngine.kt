package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 16 Original 64-Step Multi-Section Soundtrack Themes spanning all 22 Fusion Regions,
 * Hub Towns, Wild/Trainer/Rival/Gym/Syndicate/Champion Battles, and Victory Fanfare.
 */
enum class BgmThemeId(
  val id: String,
  val displayName: String,
  val bpm: Double,
  val rootMidi: Int,
  val palette: SoundPaletteStyle,
  val energetic: Boolean,
  val isBattleTheme: Boolean,
  internal val score: BgmScore64,
) {
  PRISM_TITLE(
    id = "bgm_prism_title",
    displayName = "Prism Resonance Title",
    bpm = 124.0,
    rootMidi = 62,
    palette = SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.prismTitle,
  ),
  HARMONIC_LAB(
    id = "bgm_harmonic_lab",
    displayName = "Harmonic Lab & Town",
    bpm = 104.0,
    rootMidi = 60,
    palette = SoundPaletteStyle.JAZZ_PIANO_ALPINE,
    energetic = false,
    isBattleTheme = false,
    score = OmniSoundtrackScores.harmonicLab,
  ),
  KANTO_SEVII_CHIPTUNE(
    id = "bgm_kanto_sevii_chiptune",
    displayName = "Kanto/Sevii Chiptune Adventure",
    bpm = 134.0,
    rootMidi = 64,
    palette = SoundPaletteStyle.CHIPTUNE_ADVENTURE,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.kantoSeviiChiptune,
  ),
  JOHTO_KITAKAMI_KOTO(
    id = "bgm_johto_kitakami_koto",
    displayName = "Johto/Kitakami Bell & Koto",
    bpm = 118.0,
    rootMidi = 62,
    palette = SoundPaletteStyle.PENTATONIC_BELL_KOTO,
    energetic = false,
    isBattleTheme = false,
    score = OmniSoundtrackScores.johtoKitakamiKoto,
  ),
  HOENN_ORANGE_BRASS(
    id = "bgm_hoenn_orange_brass",
    displayName = "Hoenn/Orange Brass Horizon",
    bpm = 138.0,
    rootMidi = 65,
    palette = SoundPaletteStyle.HEROIC_BRASS_TRUMPET,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.hoennOrangeBrass,
  ),
  SINNOH_HISUI_JAZZ(
    id = "bgm_sinnoh_hisui_jazz",
    displayName = "Sinnoh/Hisui Jazz & Alpine",
    bpm = 116.0,
    rootMidi = 63,
    palette = SoundPaletteStyle.JAZZ_PIANO_ALPINE,
    energetic = false,
    isBattleTheme = false,
    score = OmniSoundtrackScores.sinnohHisuiJazz,
  ),
  UNOVA_ORRE_FUNK(
    id = "bgm_unova_orre_funk",
    displayName = "Unova/Orre Neon Slap Funk",
    bpm = 130.0,
    rootMidi = 67,
    palette = SoundPaletteStyle.NEON_SLAP_FUNK,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.unovaOrreFunk,
  ),
  KALOS_GALAR_WALTZ(
    id = "bgm_kalos_galar_waltz",
    displayName = "Kalos/Galar Strings & Stadium",
    bpm = 122.0,
    rootMidi = 69,
    palette = SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.kalosGalarWaltz,
  ),
  ALOLA_PALDEA_TROPICAL(
    id = "bgm_alola_paldea_tropical",
    displayName = "Alola/Paldea Marimba & Guitar",
    bpm = 128.0,
    rootMidi = 67,
    palette = SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.alolaPaldeaTropical,
  ),
  WILD_ENCOUNTER(
    id = "bgm_wild_encounter",
    displayName = "Wild Encounter",
    bpm = 156.0,
    rootMidi = 64,
    palette = SoundPaletteStyle.BATTLE_CHIPTUNE_ROCK,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.wildEncounter,
  ),
  TRAINER_DUEL(
    id = "bgm_trainer_duel",
    displayName = "Trainer Eye-Contact Duel",
    bpm = 152.0,
    rootMidi = 66,
    palette = SoundPaletteStyle.HEROIC_BRASS_TRUMPET,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.trainerDuel,
  ),
  RIVAL_KAEL(
    id = "bgm_rival_kael",
    displayName = "Rival Kael Showdown",
    bpm = 162.0,
    rootMidi = 67,
    palette = SoundPaletteStyle.NEON_SLAP_FUNK,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.rivalKael,
  ),
  GYM_TRIAL(
    id = "bgm_gym_trial",
    displayName = "Gym / Trial Climax",
    bpm = 158.0,
    rootMidi = 69,
    palette = SoundPaletteStyle.BATTLE_CHIPTUNE_ROCK,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.gymTrial,
  ),
  TEAM_MONOCHROME(
    id = "bgm_team_monochrome",
    displayName = "Team Monochrome Rift",
    bpm = 152.0,
    rootMidi = 61,
    palette = SoundPaletteStyle.MONOCHROME_RIFT_SYNTH,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.teamMonochrome,
  ),
  CHAMPION_CONVERGENCE(
    id = "bgm_champion_convergence",
    displayName = "Champion Convergence Finale",
    bpm = 168.0,
    rootMidi = 64,
    palette = SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX,
    energetic = true,
    isBattleTheme = true,
    score = OmniSoundtrackScores.championConvergence,
  ),
  VICTORY_FANFARE(
    id = "bgm_victory_fanfare",
    displayName = "Victory & Shard Fanfare",
    bpm = 140.0,
    rootMidi = 67,
    palette = SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX,
    energetic = true,
    isBattleTheme = false,
    score = OmniSoundtrackScores.victoryFanfare,
  ),
  ;

  val stepCount: Int
    get() = score.melody64.size

  val melodyOffsets: IntArray
    get() = score.melody64

  companion object {
    fun fromId(id: String): BgmThemeId =
      entries.firstOrNull { it.id == id } ?: when (id) {
        "bgm_kanto_johto_route" -> KANTO_SEVII_CHIPTUNE
        "bgm_hoenn_sinnoh_route", "bgm_hoenn_sinnoh_frontier" -> HOENN_ORANGE_BRASS
        "bgm_unova_kalos_route", "bgm_unova_kalos_modern" -> UNOVA_ORRE_FUNK
        "bgm_alola_galar_paldea_route", "bgm_alola_galar_paldea" -> ALOLA_PALDEA_TROPICAL
        "bgm_prism_sanctuary_boss" -> TEAM_MONOCHROME
        else -> PRISM_TITLE
      }
  }
}

enum class StudioSfxType(val id: String, val label: String) {
  UI_HOVER("sfx_ui_hover", "UI Hover"),
  UI_CONFIRM("sfx_ui_confirm", "UI Confirm"),
  UI_CANCEL("sfx_ui_cancel", "UI Cancel"),
  STEP_GRASS("sfx_step_grass", "Step (Grass)"),
  STEP_STONE("sfx_step_stone", "Step (Road)"),
  STEP_CRYSTAL("sfx_step_crystal", "Step (Crystal)"),
  GRASS_RUSTLE("sfx_grass_rustle", "Tall Grass Rustle"),
  ENCOUNTER_STING("sfx_encounter_sting", "Encounter Sting"),
  TRAINER_EYE_CONTACT("sfx_trainer_alert", "Trainer '!' Alert"),
  DOOR_WARP_SWOOSH("sfx_door_warp", "Door / Warp Swoosh"),
  EVOLUTION_CHIME("sfx_evolution_chime", "Evolution Chime"),
  POKEBALL_THROW("sfx_ball_throw", "Poké Ball Throw"),
  POKEBALL_SHAKE("sfx_ball_shake", "Poké Ball Shake"),
  CAPTURE_SPARKLE("sfx_capture_sparkle", "Capture Sparkle"),
  HIT_LIGHT("sfx_hit_light", "Hit (Normal)"),
  HIT_HEAVY_CRIT("sfx_hit_heavy", "Hit (Super/Crit)"),
  HIT_RESISTED("sfx_hit_resisted", "Hit (Resisted)"),
  STAT_BOOST("sfx_stat_boost", "Stat Boost"),
  STAT_DROP("sfx_stat_drop", "Stat Drop"),
  HEAL_CHIME("sfx_heal_chime", "Heal Chime"),
  ITEM_USE("sfx_item_use", "Item Use"),
  LEVEL_UP("sfx_level_up", "Level Up Arpeggio"),
}

/**
 * 100% Clean-Room Pure-Kotlin Multi-Hardware Audio & Procedural Foley Synthesizer.
 *
 * - 44.1kHz Stereo 16-bit AudioTrack DSP (zero external audio files or third-party deps)
 * - 64-Step Multi-Section Compositions (Intro -> Section A -> Section B -> Bridge -> Turnaround)
 * - Era-Matched Sound Palettes (Chiptune Pulse/Triangle, Pentatonic Bell/Koto, Heroic Brass/Snare,
 *   Jazz Rhodes Piano/Alpine Synth, Neon YM2612 Slap Funk, Orchestral Strings/Accordion/Stadium,
 *   Tropical Marimba/Guitar, Battle Synth Rock, Monochrome Rift Industrial FM, Symphonic Convergence)
 * - Dynamic Battle Audio Layers: Low-HP Urgency Pulse (<25% HP) + Final Ace Climax (+10% BPM & +12st octave lift)
 * - Optional Region-Matched Procedural Ambient Beds (Ocean Surf, Meadow Wind, Snow Shimmer, Crystal Hum)
 * - 2-pass echo warmup + 128-frame cosine seam crossfade + master tanh() soft-limiter (zero clicks, NaN, or clipping)
 */
class OmniHardwareAudioEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val bgmMutex = Mutex()
  private val trackLock = Any()

  @Volatile private var activeBgmTrack: AudioTrack? = null
  private var activeBgmJob: Job? = null

  @Volatile private var activeBgmThemeId: String? = null
  @Volatile private var activePlayingSignature: BgmCacheKey? = null

  @Volatile var isMuted: Boolean = false
    private set

  @Volatile var masterVolume: Float = 0.72f
    private set

  @Volatile var activeAudioParams: ConvergedAudioParams = AudioConvergenceSolver.solveAudio(
    activePlatforms = emptyList(),
    disciplineMode = com.example.data.model.MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
    extraPretty = com.example.data.model.ExtraVisualAudioSettings(),
  )

  @Volatile var activeDynamicState: DynamicBattleAudioState = DynamicBattleAudioState()
    private set

  fun setMuted(muted: Boolean) {
    val wasMuted = isMuted
    isMuted = muted
    applyTrackVolume()
    if (wasMuted && !muted) {
      val themeId = activeBgmThemeId
      if (themeId != null && activeBgmTrack == null) {
        playBgm(BgmThemeId.fromId(themeId), activeDynamicState)
      }
    }
  }

  fun setMasterVolume(volume: Float) {
    masterVolume = volume.coerceIn(0f, 1f)
    applyTrackVolume()
  }

  fun updateAudioParams(params: ConvergedAudioParams) {
    val changed = params != activeAudioParams
    activeAudioParams = params
    val currentTheme = activeBgmThemeId
    if (changed && currentTheme != null && !isMuted) {
      playBgm(BgmThemeId.fromId(currentTheme), activeDynamicState)
    }
  }

  fun updateDynamicAudioState(state: DynamicBattleAudioState) {
    val changed = state != activeDynamicState
    activeDynamicState = state
    val currentTheme = activeBgmThemeId
    if (changed && currentTheme != null && !isMuted) {
      playBgm(BgmThemeId.fromId(currentTheme), state)
    }
  }

  private fun applyTrackVolume() {
    val gain = if (isMuted) 0f else masterVolume
    synchronized(trackLock) {
      runCatching { activeBgmTrack?.setVolume(gain) }
    }
  }

  fun playBgm(
    theme: BgmThemeId,
    dynamicState: DynamicBattleAudioState = activeDynamicState,
  ) {
    val paramsSnapshot = activeAudioParams
    val dynamicSnapshot = dynamicState
    val signature = BgmCacheKey(theme, paramsSnapshot, dynamicSnapshot)
    activeBgmThemeId = theme.id
    activeDynamicState = dynamicSnapshot

    // Skip redundant AudioTrack restart when the exact same BGM signature is already playing
    val alreadyPlayingSame = synchronized(trackLock) {
      activePlayingSignature == signature && activeBgmTrack != null && !isMuted
    }
    if (alreadyPlayingSame) {
      applyTrackVolume()
      return
    }

    activeBgmJob?.cancel()
    activeBgmJob = scope.launch {
      val pcm = renderBgmLoopPcm(theme, paramsSnapshot, dynamicSnapshot)
      bgmMutex.withLock {
        stopBgmTrackLocked()
        if (isMuted) return@withLock
        val frameCount = pcm.size / 2
        runCatching {
          val track = AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

          track.write(pcm, 0, pcm.size)
          track.setLoopPoints(0, frameCount, -1)
          track.setVolume(if (isMuted) 0f else masterVolume)
          track.play()
          synchronized(trackLock) {
            activeBgmTrack = track
            activePlayingSignature = signature
          }
        }
      }
    }
  }

  fun stopBgm() {
    activeBgmJob?.cancel()
    activeBgmJob = null
    activeBgmThemeId = null
    activePlayingSignature = null
    scope.launch {
      bgmMutex.withLock {
        stopBgmTrackLocked()
      }
    }
  }

  fun stopBgmImmediate() {
    activeBgmJob?.cancel()
    activeBgmJob = null
    activePlayingSignature = null
    stopBgmTrackLocked()
  }

  private fun stopBgmTrackLocked() {
    val track = synchronized(trackLock) {
      val current = activeBgmTrack
      activeBgmTrack = null
      activePlayingSignature = null
      current
    }
    if (track != null) {
      runCatching { track.stop() }
      runCatching { track.release() }
    }
  }

  fun playSfx(type: StudioSfxType) {
    if (isMuted || masterVolume <= 0.01f) return
    val paramsSnapshot = activeAudioParams
    val vol = masterVolume
    scope.launch {
      val pcm = renderSfxPcm(type, paramsSnapshot)
      playOneShotStereoPcm(pcm, vol)
    }
  }

  fun playPokemonCry(
    dexNumber: Int,
    primaryType: String,
    secondaryType: String = "",
    baseHp: Int = 75,
    baseAtk: Int = 85,
    baseSpe: Int = 90,
  ) {
    if (isMuted || masterVolume <= 0.01f) return
    val paramsSnapshot = activeAudioParams
    val vol = masterVolume
    scope.launch {
      val pcm = renderPokemonCryPcm(
        dexNumber = dexNumber,
        primaryType = primaryType,
        secondaryType = secondaryType,
        baseHp = baseHp,
        baseAtk = baseAtk,
        baseSpe = baseSpe,
        params = paramsSnapshot,
      )
      playOneShotStereoPcm(pcm, vol)
    }
  }

  private suspend fun playOneShotStereoPcm(pcm: ShortArray, volume: Float) {
    if (pcm.isEmpty()) return
    val durationMs = ((pcm.size / 2) * 1000L / SAMPLE_RATE).coerceAtLeast(40L)
    var track: AudioTrack? = null
    try {
      track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setBufferSizeInBytes(pcm.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(pcm, 0, pcm.size)
      track.setVolume(volume.coerceIn(0f, 1f))
      track.play()
      delay(durationMs + 35L)
    } catch (_: Exception) {
      // Gracefully ignore audio HAL unavailability in unit/Robolectric environments
    } finally {
      withContext(NonCancellable) {
        runCatching { track?.stop() }
        runCatching { track?.release() }
      }
    }
  }

  fun releaseAll() {
    stopBgmImmediate()
    scope.cancel()
  }

  data class BgmCacheKey(
    val theme: BgmThemeId,
    val params: ConvergedAudioParams,
    val dynamicState: DynamicBattleAudioState,
  )

  companion object {
    const val SAMPLE_RATE = 44100
    const val MAX_BGM_CACHE_ENTRIES = 6
    private const val TWO_PI = 2.0 * PI
    private const val INV_SAMPLE_RATE = 1.0 / SAMPLE_RATE

    // Thread-safe LRU cache holding up to 6 synthesized 64-step stereo PCM loops
    private val bgmPcmCache = object : LinkedHashMap<BgmCacheKey, ShortArray>(8, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<BgmCacheKey, ShortArray>?): Boolean =
        size > MAX_BGM_CACHE_ENTRIES
    }

    fun bgmPcmCacheSize(): Int = synchronized(bgmPcmCache) { bgmPcmCache.size }

    fun clearBgmPcmCache() {
      synchronized(bgmPcmCache) { bgmPcmCache.clear() }
    }

    // Precomputed MIDI frequency table (0..127) for fast allocation-free inner loop
    private val MIDI_HZ_TABLE = DoubleArray(128) { midi ->
      440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    // Chord triad/seventh intervals indexed by chordType (0=Maj, 1=Min, 2=Dom7, 3=Min7, 4=Sus4, 5=Dim/Phrygian)
    private val CHORD_INTERVALS = arrayOf(
      intArrayOf(0, 4, 7, 12),
      intArrayOf(0, 3, 7, 12),
      intArrayOf(0, 4, 7, 10),
      intArrayOf(0, 3, 7, 10),
      intArrayOf(0, 5, 7, 12),
      intArrayOf(0, 1, 6, 7),
    )

    private fun midiToHz(midi: Int): Double = MIDI_HZ_TABLE[midi.coerceIn(0, 127)]

    private fun quantizeBitcrush(sample: Double, depth: Float): Double {
      if (depth <= 0.02f) return sample
      val steps = (32.0 - depth * 22.0).coerceIn(6.0, 32.0)
      return floor(sample * steps + 0.5) / steps
    }

    private fun sanitize(value: Double): Double = if (value.isFinite()) value else 0.0

    /**
     * Synthesizes (or retrieves from the thread-safe 6-slot LRU cache) a click-free, seamless
     * 64-step multi-section stereo PCM loop (44.1kHz, 16-bit) across 6 frequency-separated stereo
     * lanes + dynamic battle layers (Low-HP urgency pulse & Boss Final Ace climax tempo/octave lift)
     * + optional region-matched ambient bed, routed through cross-fed SPC700/SPU stereo feedback
     * echo and a NaN/clipping-safe master tanh() soft limiter.
     */
    fun renderBgmLoopPcm(
      theme: BgmThemeId,
      params: ConvergedAudioParams,
      dynamicState: DynamicBattleAudioState = DynamicBattleAudioState(),
    ): ShortArray {
      val cacheKey = BgmCacheKey(theme, params, dynamicState)
      synchronized(bgmPcmCache) {
        bgmPcmCache[cacheKey]?.let { return it }
      }
      val score = theme.score
      val steps = score.melody64.size
      val climaxActive = dynamicState.bossFinalAceClimax
      val lowHpActive = dynamicState.lowHpUrgency
      val effectiveBpm = when {
        climaxActive -> theme.bpm * 1.10
        lowHpActive -> theme.bpm * 1.03
        else -> theme.bpm
      }
      val stepDurationSec = 60.0 / (effectiveBpm * 2.0) // 8th-note grid
      val samplesPerStep = (SAMPLE_RATE * stepDurationSec).toInt().coerceAtLeast(1024)
      val totalFrames = steps * samplesPerStep

      val pass2L = DoubleArray(totalFrames)
      val pass2R = DoubleArray(totalFrames)
      val out = ShortArray(totalFrames * 2)

      // Cross-fed SPC700/PS1 SPU stereo echo delay lines (~180ms L, ~240ms R)
      val delayLSize = (SAMPLE_RATE * 0.18).toInt()
      val delayRSize = (SAMPLE_RATE * 0.24).toInt()
      val delayL = DoubleArray(delayLSize)
      val delayR = DoubleArray(delayRSize)
      var dIdxL = 0
      var dIdxR = 0

      var lfsr = 0xACE1
      var ambLfsr = 0x5B37
      var ambLpL = 0.0
      var ambLpR = 0.0
      var ambHarmPhaseA = 0.0
      var ambHarmPhaseB = 0.0

      var leadPhase = 0.0
      var leadModPhase = 0.0
      var climaxOctPhase = 0.0
      var harmPhase = 0.0
      var bassPhase = 0.0
      var fmModPhase = 0.0
      var arpPhase = 0.0
      var padPhaseA = 0.0
      var padPhaseB = 0.0
      var padPhaseC = 0.0
      var choirPhase = 0.0
      var lowHpPhase = 0.0

      val echoFb = (params.spcEchoFeedback * 0.40).coerceIn(0.0, 0.44)
      val echoMix = if (params.extraLushChoirReverb) 0.38 else 0.26
      val width = params.stereoWidth.coerceIn(0.15f, 1.0f).toDouble()
      val palette = theme.palette

      val activeAmbient = if (dynamicState.ambientBedEnabled && !theme.isBattleTheme) {
        dynamicState.ambientBed
      } else {
        AmbientBedType.NONE
      }
      val lowHpPeriodFrames = (SAMPLE_RATE * 0.52).toInt().coerceAtLeast(1024)
      val invTotalFrames = 1.0 / totalFrames.toDouble()
      val invSamplesPerStep = 1.0 / samplesPerStep.toDouble()

      val triGain = params.triangleBassGain.toDouble()
      val slapGain = params.fmSlapBassGain.toDouble()
      val leadGain = params.pulseLeadGain.toDouble()
      val arpGain = params.sidArpGain.toDouble()
      val padGain = params.orchestralPadGain.toDouble()
      val percGain = params.lfsrPercussionGain.toDouble() * (if (climaxActive) 1.18 else 1.0)
      val bitcrush = params.bitcrushDepth
      val lushChoir = params.extraLushChoirReverb

      for (pass in 0 until 2) {
        val writeOutput = (pass == 1)
        for (frame in 0 until totalFrames) {
          val stepIdx = frame / samplesPerStep
          val frameInStep = frame - stepIdx * samplesPerStep
          val stepPos = frameInStep * invSamplesPerStep
          val tStepSec = frameInStep * INV_SAMPLE_RATE
          val songProgress = frame * invTotalFrames

          val barIdx = (stepIdx ushr 2) and 15
          val chordRoot = score.chordRoots16[barIdx]
          val chordIntervals = CHORD_INTERVALS[score.chordTypes16[barIdx].coerceIn(0, CHORD_INTERVALS.lastIndex)]

          // Section energy shaping across the 64-step form:
          // Intro (0..7) = 0.88, Sec A (8..23) = 0.96, Sec B (24..39) = 1.08, Bridge (40..55) = 1.02, Turnaround (56..63) = 1.05
          val sectionLift = when {
            stepIdx < 8 -> 0.88
            stepIdx < 24 -> 0.96
            stepIdx < 40 -> 1.08
            stepIdx < 56 -> 1.02
            else -> 1.05
          }

          // -----------------------------------------------------------------
          // 1. MONO BASS LANE (Era-shaped: Walking Triangle / YM2612 Slap / Acoustic / Sub)
          // -----------------------------------------------------------------
          val bassOff = score.bass64[stepIdx]
          var bassMono = 0.0
          if (bassOff != BgmScore64.REST) {
            val bassHz = midiToHz(theme.rootMidi - 24 + bassOff).coerceIn(55.0, 260.0)
            bassPhase += bassHz * INV_SAMPLE_RATE
            if (bassPhase >= 1.0) bassPhase -= floor(bassPhase)
            fmModPhase += (bassHz * 2.0) * INV_SAMPLE_RATE
            if (fmModPhase >= 1.0) fmModPhase -= floor(fmModPhase)

            val triRaw = 4.0 * abs(bassPhase - 0.5) - 1.0
            val nesTri4Bit = floor(triRaw * 7.5) / 7.5
            val fmEnv = exp(-tStepSec * 15.0)
            val slapMod = if (palette == SoundPaletteStyle.NEON_SLAP_FUNK) 3.4 else 2.2
            val ym2612Slap = sin(TWO_PI * bassPhase + slapMod * fmEnv * sin(TWO_PI * fmModPhase)) * exp(-tStepSec * 5.2)
            val speakerWarmth = 0.28 * sin(TWO_PI * 2.0 * bassPhase) + 0.14 * sin(TWO_PI * 3.0 * bassPhase)

            bassMono = when (palette) {
              SoundPaletteStyle.CHIPTUNE_ADVENTURE ->
                (nesTri4Bit * triGain * 0.30 + speakerWarmth * 0.12) * (1.0 - stepPos * 0.16)
              SoundPaletteStyle.NEON_SLAP_FUNK ->
                (ym2612Slap * slapGain * 0.34 + nesTri4Bit * triGain * 0.12 + speakerWarmth * 0.14) * (1.0 - stepPos * 0.12)
              SoundPaletteStyle.JAZZ_PIANO_ALPINE, SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR ->
                (sin(TWO_PI * bassPhase) * 0.26 + nesTri4Bit * triGain * 0.14 + speakerWarmth * 0.10) * exp(-tStepSec * 3.6)
              SoundPaletteStyle.MONOCHROME_RIFT_SYNTH ->
                (tanh((2.0 * bassPhase - 1.0) * 1.8) * 0.22 + ym2612Slap * slapGain * 0.20) * (1.0 - stepPos * 0.14)
              else ->
                (nesTri4Bit * triGain * 0.22 + ym2612Slap * slapGain * 0.20 + speakerWarmth * 0.13) * (1.0 - stepPos * 0.18)
            }
          }

          // -----------------------------------------------------------------
          // 2. HEROIC LEAD + COUNTER-HARMONY LANE (Era-Specific Timbre)
          // -----------------------------------------------------------------
          val melOff = score.melody64[stepIdx]
          var leadSig = 0.0
          if (melOff != BgmScore64.REST) {
            val leadMidi = theme.rootMidi + melOff
            val vibDepth = when (palette) {
              SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ,
              SoundPaletteStyle.HEROIC_BRASS_TRUMPET,
              SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX -> 0.0045
              SoundPaletteStyle.JAZZ_PIANO_ALPINE -> 0.0022
              else -> 0.0018
            }
            val vibrato = 1.0 + vibDepth * sin(TWO_PI * 5.8 * tStepSec) * (1.0 - exp(-tStepSec * 9.0))
            val leadHz = midiToHz(leadMidi) * vibrato
            leadPhase += leadHz * INV_SAMPLE_RATE
            if (leadPhase >= 1.0) leadPhase -= floor(leadPhase)

            val modRatio = when (palette) {
              SoundPaletteStyle.PENTATONIC_BELL_KOTO -> 2.756 // Metallic bronze shrine bell ratio
              SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> 4.0 // Woody percussive marimba overtone
              SoundPaletteStyle.JAZZ_PIANO_ALPINE -> 3.0 // Electric piano Rhodes bell tine
              SoundPaletteStyle.MONOCHROME_RIFT_SYNTH -> 1.414 // Inharmonic dark rift grit
              else -> 1.0
            }
            leadModPhase += (leadHz * modRatio) * INV_SAMPLE_RATE
            if (leadModPhase >= 1.0) leadModPhase -= floor(leadModPhase)

            val rawLead = when (palette) {
              SoundPaletteStyle.CHIPTUNE_ADVENTURE -> {
                val p25 = if (leadPhase < 0.25) 0.85 else -0.85
                val p50 = if (leadPhase < 0.50) 0.45 else -0.45
                (p25 * 0.72 + p50 * 0.28) * (1.0 - exp(-tStepSec * 80.0)) * exp(-tStepSec * 3.4)
              }
              SoundPaletteStyle.PENTATONIC_BELL_KOTO -> {
                // FM Bronze Bell chime + sharp plucked Koto string
                val bellIdx = 2.6 * exp(-tStepSec * 7.5)
                val bell = sin(TWO_PI * leadPhase + bellIdx * sin(TWO_PI * leadModPhase)) * exp(-tStepSec * 4.4)
                val kotoPluck = ((2.0 * leadPhase - 1.0) * 0.45 + sin(TWO_PI * leadPhase) * 0.55) * exp(-tStepSec * 11.5)
                (bell * 0.62 + kotoPluck * 0.55) * (1.0 - exp(-tStepSec * 120.0))
              }
              SoundPaletteStyle.HEROIC_BRASS_TRUMPET -> {
                // Bright GBA/FM Trumpet with characteristic brass bloom
                val brassBloom = 1.0 - exp(-tStepSec * 22.0)
                val fmBrass = sin(TWO_PI * leadPhase + (1.75 * brassBloom + 0.55) * sin(TWO_PI * leadPhase))
                val sawEdge = (2.0 * leadPhase - 1.0) * 0.30
                (fmBrass * 0.74 + sawEdge) * brassBloom * exp(-tStepSec * 2.4)
              }
              SoundPaletteStyle.JAZZ_PIANO_ALPINE -> {
                // Warm Rhodes Electric Piano (bell tine + warm fundamental)
                val tine = sin(TWO_PI * leadPhase + 1.8 * exp(-tStepSec * 18.0) * sin(TWO_PI * leadModPhase))
                val warmBody = sin(TWO_PI * leadPhase) + 0.22 * sin(TWO_PI * 2.0 * leadPhase)
                (tine * 0.52 + warmBody * 0.48) * (1.0 - exp(-tStepSec * 95.0)) * exp(-tStepSec * 4.2)
              }
              SoundPaletteStyle.NEON_SLAP_FUNK -> {
                val p20 = if (leadPhase < 0.20) 0.82 else -0.82
                val fmStab = sin(TWO_PI * leadPhase + 1.4 * exp(-tStepSec * 9.0) * sin(TWO_PI * leadPhase))
                (p20 * 0.45 + fmStab * 0.55) * (1.0 - exp(-tStepSec * 85.0)) * exp(-tStepSec * 4.6)
              }
              SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ -> {
                // Expressive Accordion reed + Violin harmonic blend
                val reed = sin(TWO_PI * leadPhase) * 0.55 +
                  sin(TWO_PI * 2.0 * leadPhase) * 0.28 +
                  sin(TWO_PI * 3.0 * leadPhase) * 0.17
                val sawSoft = (2.0 * leadPhase - 1.0) * 0.22
                (reed + sawSoft) * (1.0 - exp(-tStepSec * 24.0)) * exp(-tStepSec * 2.1)
              }
              SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> {
                // Percussive FM Marimba Mallet + crisp wood resonance
                val mallet = sin(TWO_PI * leadPhase + 2.2 * exp(-tStepSec * 36.0) * sin(TWO_PI * leadModPhase))
                val woodBody = sin(TWO_PI * leadPhase) * exp(-tStepSec * 8.2)
                (mallet * 0.48 + woodBody * 0.65) * (1.0 - exp(-tStepSec * 140.0)) * exp(-tStepSec * 6.5)
              }
              SoundPaletteStyle.MONOCHROME_RIFT_SYNTH -> {
                val fmGrit = sin(TWO_PI * leadPhase + 2.6 * sin(TWO_PI * leadModPhase))
                val vrc6Saw = 2.0 * leadPhase - 1.0
                (fmGrit * 0.58 + vrc6Saw * 0.42) * (1.0 - exp(-tStepSec * 70.0)) * exp(-tStepSec * 3.2)
              }
              SoundPaletteStyle.BATTLE_CHIPTUNE_ROCK -> {
                val p25 = if (leadPhase < 0.25) 0.85 else -0.85
                val saw = 2.0 * leadPhase - 1.0
                val brass = sin(TWO_PI * leadPhase + 1.1 * sin(TWO_PI * leadPhase))
                (p25 * 0.46 + saw * 0.30 + brass * 0.24) * (1.0 - exp(-tStepSec * 85.0)) * exp(-tStepSec * 3.3)
              }
              SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX -> {
                val p25 = if (leadPhase < 0.25) 0.80 else -0.80
                val symBrass = sin(TWO_PI * leadPhase + 1.5 * sin(TWO_PI * leadPhase)) * (1.0 - exp(-tStepSec * 28.0))
                val vrc6Saw = 2.0 * leadPhase - 1.0
                (p25 * 0.34 + symBrass * 0.44 + vrc6Saw * 0.22) * (1.0 - exp(-tStepSec * 65.0)) * exp(-tStepSec * 2.7)
              }
            }

            leadSig = rawLead * leadGain * 0.26 * sectionLift

            // DYNAMIC LAYER: Boss Final Ace Climax (+12 semitones octave lift!)
            if (climaxActive) {
              val climaxHz = midiToHz(leadMidi + 12)
              climaxOctPhase += climaxHz * INV_SAMPLE_RATE
              if (climaxOctPhase >= 1.0) climaxOctPhase -= floor(climaxOctPhase)
              val octPulse = if (climaxOctPhase < 0.25) 0.78 else -0.78
              val octBrass = sin(TWO_PI * climaxOctPhase + 1.1 * sin(TWO_PI * climaxOctPhase))
              val climaxBoost = if (stepIdx in 24..55) 0.16 else 0.11
              leadSig += (octPulse * 0.55 + octBrass * 0.45) * (1.0 - exp(-tStepSec * 75.0)) * exp(-tStepSec * 3.5) * climaxBoost
            }
          }

          // Secondary Counter-Harmony Voice (panned gently opposite the arp for lush stereo depth)
          val harmOff = score.harmony64[stepIdx]
          var harmL = 0.0
          var harmR = 0.0
          if (harmOff != BgmScore64.REST) {
            val harmHz = midiToHz(theme.rootMidi + harmOff)
            harmPhase += harmHz * INV_SAMPLE_RATE
            if (harmPhase >= 1.0) harmPhase -= floor(harmPhase)
            val harmWave = when (palette) {
              SoundPaletteStyle.CHIPTUNE_ADVENTURE -> if (harmPhase < 0.125) 0.65 else -0.65
              SoundPaletteStyle.PENTATONIC_BELL_KOTO -> sin(TWO_PI * harmPhase) * exp(-tStepSec * 8.0)
              SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> ((2.0 * harmPhase - 1.0) * 0.4 + sin(TWO_PI * harmPhase) * 0.6) * exp(-tStepSec * 7.5)
              else -> sin(TWO_PI * harmPhase) * 0.65 + (2.0 * harmPhase - 1.0) * 0.25
            }
            val harmSig = harmWave * (1.0 - exp(-tStepSec * 55.0)) * exp(-tStepSec * 3.8) * leadGain * 0.11 * sectionLift
            harmL = harmSig * (0.5 + 0.25 * width)
            harmR = harmSig * (0.5 - 0.25 * width)
          }

          // -----------------------------------------------------------------
          // 3. PANNED ARPEGGIO / PLUCKED CHORD STRUM LANE
          // -----------------------------------------------------------------
          val arpSubCount = if (palette == SoundPaletteStyle.JAZZ_PIANO_ALPINE || palette == SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR) 4 else 3
          val arpSubIdx = ((stepPos * arpSubCount).toInt()) % chordIntervals.size
          val arpOctShift = if (palette == SoundPaletteStyle.JAZZ_PIANO_ALPINE) 0 else -12
          val arpMidi = theme.rootMidi + arpOctShift + chordRoot + chordIntervals[arpSubIdx]
          val arpHz = midiToHz(arpMidi)
          arpPhase += arpHz * INV_SAMPLE_RATE
          if (arpPhase >= 1.0) arpPhase -= floor(arpPhase)
          val arpSubFrac = (stepPos * arpSubCount) - floor(stepPos * arpSubCount)

          val arpWave = when (palette) {
            SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> {
              // Rhythmic plucked acoustic guitar chord strum
              (sin(TWO_PI * arpPhase) * 0.6 + (2.0 * arpPhase - 1.0) * 0.4) * exp(-arpSubFrac * 4.5)
            }
            SoundPaletteStyle.JAZZ_PIANO_ALPINE -> {
              // Sparkling crystalline alpine synth arp
              (sin(TWO_PI * arpPhase) + 0.4 * sin(TWO_PI * 2.0 * arpPhase)) * exp(-arpSubFrac * 3.8)
            }
            SoundPaletteStyle.PENTATONIC_BELL_KOTO -> {
              // Soft koto arpeggio pluck
              sin(TWO_PI * arpPhase + 0.8 * exp(-arpSubFrac * 5.0) * sin(TWO_PI * 2.0 * arpPhase)) * exp(-arpSubFrac * 5.2)
            }
            else -> {
              (if (arpPhase < 0.35) 0.72 else -0.72) * exp(-arpSubFrac * 3.4)
            }
          }
          val arpSig = arpWave * arpGain * 0.13 * sectionLift
          val arpPan = if ((stepIdx + arpSubIdx) % 2 == 0) 0.62 * width else -0.62 * width
          val arpL = arpSig * (0.5 - arpPan * 0.5)
          val arpR = arpSig * (0.5 + arpPan * 0.5)

          // -----------------------------------------------------------------
          // 4. WIDE STEREO PAD / STRINGS / STADIUM CHANT (+ Extra Pretty Choir)
          // -----------------------------------------------------------------
          val padRootMidi = theme.rootMidi - 12 + chordRoot
          val padHz = midiToHz(padRootMidi)
          val padFifthHz = midiToHz(padRootMidi + chordIntervals[1])
          padPhaseA += (padHz * 0.9965) * INV_SAMPLE_RATE
          if (padPhaseA >= 1.0) padPhaseA -= floor(padPhaseA)
          padPhaseB += (padHz * 1.0035) * INV_SAMPLE_RATE
          if (padPhaseB >= 1.0) padPhaseB -= floor(padPhaseB)
          padPhaseC += padFifthHz * INV_SAMPLE_RATE
          if (padPhaseC >= 1.0) padPhaseC -= floor(padPhaseC)

          val padMult = when (palette) {
            SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ -> 0.15
            SoundPaletteStyle.CHAMPION_SYMPHONIC_CLIMAX -> 0.14
            SoundPaletteStyle.CHIPTUNE_ADVENTURE -> 0.06
            else -> 0.10
          }
          val stadiumPulse = if (palette == SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ && stepIdx in 24..39) {
            0.75 + 0.35 * sin(PI * stepPos)
          } else {
            1.0
          }
          val padThird = sin(TWO_PI * padPhaseC) * 0.42
          val padL = (sin(TWO_PI * padPhaseA) + 0.32 * sin(TWO_PI * 2.0 * padPhaseA) + padThird) * padGain * padMult * stadiumPulse
          val padR = (sin(TWO_PI * padPhaseB) + 0.32 * sin(TWO_PI * 2.0 * padPhaseB) + padThird) * padGain * padMult * stadiumPulse

          var choirL = 0.0
          var choirR = 0.0
          if (lushChoir || (palette == SoundPaletteStyle.ORCHESTRAL_ACCORDION_WALTZ && stepIdx in 24..39)) {
            choirPhase += (padHz * 2.0) * INV_SAMPLE_RATE
            if (choirPhase >= 1.0) choirPhase -= floor(choirPhase)
            val choirLevel = if (lushChoir) 0.065 else 0.045
            val shimmer = sin(TWO_PI * choirPhase) * choirLevel
            val lfo = sin(TWO_PI * songProgress * 4.0)
            choirL = shimmer * (0.7 + 0.3 * lfo)
            choirR = shimmer * (0.7 - 0.3 * lfo)
          }

          // -----------------------------------------------------------------
          // 5. 16-BIT LFSR PERCUSSION LANE (Era-Tailored Drum Kits)
          // -----------------------------------------------------------------
          var perc = 0.0
          val bit = ((lfsr ushr 0) xor (lfsr ushr 2) xor (lfsr ushr 3) xor (lfsr ushr 5)) and 1
          lfsr = (lfsr ushr 1) or (bit shl 15)
          val noise = if ((lfsr and 1) == 1) 1.0 else -1.0

          val stepInBar = stepIdx and 7
          val isKick = when (palette) {
            SoundPaletteStyle.NEON_SLAP_FUNK -> stepInBar == 0 || stepInBar == 3 || stepInBar == 4
            SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> stepInBar == 0 || stepInBar == 3 || stepInBar == 6
            else -> (stepIdx and 3) == 0
          }
          val isSnare = when (palette) {
            SoundPaletteStyle.HEROIC_BRASS_TRUMPET -> (stepIdx and 3) == 2 || (stepIdx >= 56 && stepIdx % 2 == 1)
            SoundPaletteStyle.NEON_SLAP_FUNK -> stepInBar == 2 || stepInBar == 6 || stepInBar == 7
            else -> (stepIdx and 3) == 2
          }

          if (theme.energetic || climaxActive || (stepIdx and 1) == 0) {
            if (isKick && tStepSec < 0.095) {
              val kickFreq = 148.0 * exp(-tStepSec * 27.0) + 46.0
              val kickAmp = if (palette == SoundPaletteStyle.PENTATONIC_BELL_KOTO) 0.18 else 0.27
              perc += sin(TWO_PI * kickFreq * tStepSec) * exp(-tStepSec * 25.0) * kickAmp
            } else if (isSnare && tStepSec < 0.085) {
              val snareAmp = when (palette) {
                SoundPaletteStyle.HEROIC_BRASS_TRUMPET -> 0.23
                SoundPaletteStyle.PENTATONIC_BELL_KOTO -> 0.11
                SoundPaletteStyle.JAZZ_PIANO_ALPINE -> 0.10
                else -> 0.19
              }
              perc += noise * exp(-tStepSec * 34.0) * snareAmp
            } else if (tStepSec < 0.030) {
              val hatAmp = when (palette) {
                SoundPaletteStyle.NEON_SLAP_FUNK -> 0.11
                SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR -> 0.09
                SoundPaletteStyle.JAZZ_PIANO_ALPINE -> 0.06
                else -> 0.075
              }
              perc += noise * exp(-tStepSec * 82.0) * hatAmp
            }
            // Extra syncopated 16th hi-hat / shaker on off-half of step for Funk & Tropical
            if ((palette == SoundPaletteStyle.NEON_SLAP_FUNK || palette == SoundPaletteStyle.TROPICAL_MARIMBA_GUITAR) &&
              stepPos >= 0.5 && stepPos < 0.65
            ) {
              val tHalf = (stepPos - 0.5) * stepDurationSec
              perc += noise * exp(-tHalf * 95.0) * 0.07
            }
            perc *= percGain
          }

          // -----------------------------------------------------------------
          // 5b. DYNAMIC LAYER: LOW-HP URGENCY PULSE (<25% HP)
          // -----------------------------------------------------------------
          var lowHpSig = 0.0
          if (lowHpActive) {
            val pulsePos = (frame % lowHpPeriodFrames).toDouble() / lowHpPeriodFrames.toDouble()
            val beaconHz = if (pulsePos < 0.22) 880.0 else if (pulsePos in 0.26..0.48) 659.25 else 0.0
            if (beaconHz > 0.0) {
              lowHpPhase += beaconHz * INV_SAMPLE_RATE
              if (lowHpPhase >= 1.0) lowHpPhase -= floor(lowHpPhase)
              val localP = if (pulsePos < 0.22) pulsePos / 0.22 else (pulsePos - 0.26) / 0.22
              val env = sin(PI * localP)
              val softSquare = sin(TWO_PI * lowHpPhase) * 0.72 + (if (lowHpPhase < 0.5) 0.28 else -0.28)
              lowHpSig = softSquare * env * 0.13
            }
          }

          // -----------------------------------------------------------------
          // 5c. OPTIONAL SUBTLE REGION-MATCHED AMBIENT BED (Pre-Limiter)
          // -----------------------------------------------------------------
          var ambL = 0.0
          var ambR = 0.0
          if (activeAmbient != AmbientBedType.NONE) {
            val aBit = ((ambLfsr ushr 0) xor (ambLfsr ushr 2) xor (ambLfsr ushr 3) xor (ambLfsr ushr 5)) and 1
            ambLfsr = (ambLfsr ushr 1) or (aBit shl 15)
            val aNoise = if ((ambLfsr and 1) == 1) 1.0 else -1.0

            when (activeAmbient) {
              AmbientBedType.OCEAN_SURF -> {
                ambLpL += 0.032 * (aNoise - ambLpL)
                ambLpR += 0.028 * (-aNoise - ambLpR)
                val waveL = 0.55 + 0.45 * sin(TWO_PI * songProgress * 2.0)
                val waveR = 0.55 + 0.45 * sin(TWO_PI * songProgress * 2.0 + 1.1)
                ambL = ambLpL * waveL * 0.16
                ambR = ambLpR * waveR * 0.16
              }
              AmbientBedType.MEADOW_WIND -> {
                ambLpL += 0.055 * (aNoise - ambLpL)
                ambLpR += 0.048 * (aNoise - ambLpR)
                ambHarmPhaseA += 392.0 * INV_SAMPLE_RATE
                if (ambHarmPhaseA >= 1.0) ambHarmPhaseA -= 1.0
                val gust = 0.5 + 0.5 * sin(TWO_PI * songProgress * 3.0)
                ambL = (ambLpL * 0.10 + sin(TWO_PI * ambHarmPhaseA) * 0.018) * gust
                ambR = (ambLpR * 0.10 - sin(TWO_PI * ambHarmPhaseA) * 0.018) * (1.0 - gust * 0.35)
              }
              AmbientBedType.SNOW_SHIMMER -> {
                ambLpL += 0.08 * (aNoise - ambLpL)
                val frostAir = (aNoise - ambLpL) * 0.025
                ambHarmPhaseA += 1760.0 * INV_SAMPLE_RATE
                if (ambHarmPhaseA >= 1.0) ambHarmPhaseA -= 1.0
                ambHarmPhaseB += 2637.02 * INV_SAMPLE_RATE
                if (ambHarmPhaseB >= 1.0) ambHarmPhaseB -= 1.0
                val sparkleL = 0.5 + 0.5 * sin(TWO_PI * songProgress * 8.0)
                val sparkleR = 0.5 + 0.5 * sin(TWO_PI * songProgress * 8.0 + PI * 0.5)
                ambL = frostAir + sin(TWO_PI * ambHarmPhaseA) * 0.024 * sparkleL
                ambR = frostAir + sin(TWO_PI * ambHarmPhaseB) * 0.024 * sparkleR
              }
              AmbientBedType.CRYSTAL_HUM -> {
                val humRoot = midiToHz(theme.rootMidi - 12)
                ambHarmPhaseA += humRoot * INV_SAMPLE_RATE
                if (ambHarmPhaseA >= 1.0) ambHarmPhaseA -= floor(ambHarmPhaseA)
                ambHarmPhaseB += (humRoot * 1.498) * INV_SAMPLE_RATE
                if (ambHarmPhaseB >= 1.0) ambHarmPhaseB -= floor(ambHarmPhaseB)
                val breathe = 0.65 + 0.35 * sin(TWO_PI * songProgress * 2.0)
                ambL = (sin(TWO_PI * ambHarmPhaseA) * 0.036 + sin(TWO_PI * ambHarmPhaseB) * 0.022) * breathe
                ambR = (sin(TWO_PI * ambHarmPhaseA + 0.7) * 0.036 + sin(TWO_PI * ambHarmPhaseB - 0.5) * 0.022) * breathe
              }
              AmbientBedType.NONE -> Unit
            }
          }

          // Cross-fed SPC700 / PS1 SPU Stereo Echo
          val dryL = leadSig * 0.88 + harmL + arpL + padL + choirL
          val dryR = leadSig * 0.88 + harmR + arpR + padR + choirR
          val tapL = delayL[dIdxL]
          val tapR = delayR[dIdxR]
          delayL[dIdxL] = dryL + tapR * echoFb
          delayR[dIdxR] = dryR + tapL * echoFb
          dIdxL++
          if (dIdxL >= delayLSize) dIdxL = 0
          dIdxR++
          if (dIdxR >= delayRSize) dIdxR = 0

          var mixL = bassMono + dryL + perc + lowHpSig + ambL + tapL * echoMix
          var mixR = bassMono + dryR + perc + lowHpSig + ambR + tapR * echoMix

          if (bitcrush > 0.02f) {
            mixL = quantizeBitcrush(mixL, bitcrush)
            mixR = quantizeBitcrush(mixR, bitcrush)
          }

          if (writeOutput) {
            pass2L[frame] = sanitize(mixL)
            pass2R[frame] = sanitize(mixR)
          }
        }
      }

      // 128-frame cosine micro-crossfade across loop seam: eliminates any discontinuity/click on loop wrap
      val xfadeFrames = 128.coerceAtMost(totalFrames / 8)
      for (i in 0 until xfadeFrames) {
        val alpha = 0.5 - 0.5 * cos(PI * i.toDouble() / xfadeFrames)
        val tailIdx = totalFrames - xfadeFrames + i
        pass2L[tailIdx] = pass2L[tailIdx] * (1.0 - alpha) + pass2L[i] * alpha
        pass2R[tailIdx] = pass2R[tailIdx] * (1.0 - alpha) + pass2R[i] * alpha
      }

      // 6. Soft-limiting master bus via tanh() — guaranteed zero clipping, zero NaN
      for (frame in 0 until totalFrames) {
        val limL = tanh(sanitize(pass2L[frame]) * 1.12)
        val limR = tanh(sanitize(pass2R[frame]) * 1.12)
        out[frame * 2] = (limL * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        out[frame * 2 + 1] = (limR * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
      }
      synchronized(bgmPcmCache) {
        bgmPcmCache[cacheKey] = out
      }
      return out
    }

    /**
     * Synthesizes sharp-transient procedural SFX for UI, footsteps, Trainer "!" eye-contact alert,
     * door/warp portal swoosh, evolution chime sequence, capture, and battle impact.
     */
    fun renderSfxPcm(type: StudioSfxType, params: ConvergedAudioParams): ShortArray {
      val durationSec = when (type) {
        StudioSfxType.UI_HOVER -> 0.055
        StudioSfxType.UI_CONFIRM -> 0.13
        StudioSfxType.UI_CANCEL -> 0.12
        StudioSfxType.STEP_GRASS, StudioSfxType.STEP_STONE, StudioSfxType.STEP_CRYSTAL -> 0.07
        StudioSfxType.GRASS_RUSTLE -> 0.15
        StudioSfxType.ENCOUNTER_STING -> 0.42
        StudioSfxType.TRAINER_EYE_CONTACT -> 0.34
        StudioSfxType.DOOR_WARP_SWOOSH -> 0.42
        StudioSfxType.EVOLUTION_CHIME -> 0.92
        StudioSfxType.POKEBALL_THROW -> 0.24
        StudioSfxType.POKEBALL_SHAKE -> 0.16
        StudioSfxType.CAPTURE_SPARKLE -> 0.50
        StudioSfxType.HIT_LIGHT -> 0.15
        StudioSfxType.HIT_HEAVY_CRIT -> 0.28
        StudioSfxType.HIT_RESISTED -> 0.12
        StudioSfxType.STAT_BOOST, StudioSfxType.STAT_DROP -> 0.26
        StudioSfxType.HEAL_CHIME -> 0.36
        StudioSfxType.ITEM_USE -> 0.22
        StudioSfxType.LEVEL_UP -> 0.48
      }
      val frames = (SAMPLE_RATE * durationSec).toInt().coerceAtLeast(512)
      val out = ShortArray(frames * 2)
      var lfsr = 0xBEEF
      var phase = 0.0
      var phase2 = 0.0
      var lpNoise = 0.0

      for (i in 0 until frames) {
        val t = i * INV_SAMPLE_RATE
        val p = i.toDouble() / frames
        val bit = ((lfsr ushr 0) xor (lfsr ushr 2) xor (lfsr ushr 3) xor (lfsr ushr 5)) and 1
        lfsr = (lfsr ushr 1) or (bit shl 15)
        val noise = if ((lfsr and 1) == 1) 1.0 else -1.0
        lpNoise += (0.04 + 0.14 * sin(PI * p)) * (noise - lpNoise)

        var panOffset = 0.0
        val sample = when (type) {
          StudioSfxType.UI_HOVER -> {
            val hz = 780.0 + p * 180.0
            phase += hz * INV_SAMPLE_RATE
            (if ((phase % 1.0) < 0.25) 0.6 else -0.6) * exp(-t * 38.0) * 0.35
          }
          StudioSfxType.UI_CONFIRM -> {
            val hz = if (p < 0.45) 659.25 else 987.77
            phase += hz * INV_SAMPLE_RATE
            (if ((phase % 1.0) < 0.25) 0.7 else -0.7) * exp(-t * 14.0) * 0.45
          }
          StudioSfxType.UI_CANCEL -> {
            val hz = 493.88 * (1.0 - p * 0.35)
            phase += hz * INV_SAMPLE_RATE
            (2.0 * (phase % 1.0) - 1.0) * exp(-t * 18.0) * 0.42
          }
          StudioSfxType.STEP_GRASS -> noise * exp(-t * 42.0) * 0.26
          StudioSfxType.STEP_STONE -> {
            val hz = 210.0 * exp(-t * 25.0) + 90.0
            phase += hz * INV_SAMPLE_RATE
            (sin(TWO_PI * phase) * 0.6 + noise * 0.4) * exp(-t * 48.0) * 0.38
          }
          StudioSfxType.STEP_CRYSTAL -> {
            val hz = 1174.66 + 220.0 * p
            phase += hz * INV_SAMPLE_RATE
            sin(TWO_PI * phase) * exp(-t * 32.0) * 0.42
          }
          StudioSfxType.GRASS_RUSTLE -> noise * sin(PI * p) * (0.4 + 0.6 * abs(sin(p * 18.0))) * 0.30
          StudioSfxType.ENCOUNTER_STING -> {
            val notes = doubleArrayOf(587.33, 698.46, 880.0, 1174.66, 987.77, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz * INV_SAMPLE_RATE
            ((if ((phase % 1.0) < 0.25) 0.8 else -0.8) + 0.3 * sin(TWO_PI * phase)) * (1.0 - p * 0.35) * 0.52
          }
          StudioSfxType.TRAINER_EYE_CONTACT -> {
            // Iconic sudden "!" exclamation alert sting: rapid double chirp -> bright ring
            val hz = when {
              p < 0.14 -> 880.0
              p < 0.28 -> 1318.51
              else -> 1760.0 + 40.0 * sin(TWO_PI * 14.0 * t)
            }
            phase += hz * INV_SAMPLE_RATE
            phase2 += (hz * 0.5) * INV_SAMPLE_RATE
            val pulse = if ((phase % 1.0) < 0.25) 0.85 else -0.85
            val fmBell = sin(TWO_PI * phase + 1.4 * exp(-t * 14.0) * sin(TWO_PI * phase2))
            val snap = if (t < 0.025) noise * exp(-t * 110.0) * 0.45 else 0.0
            val env = (1.0 - exp(-t * 180.0)) * exp(-t * 6.8)
            ((pulse * 0.48 + fmBell * 0.52) * env + snap) * 0.62
          }
          StudioSfxType.DOOR_WARP_SWOOSH -> {
            // Shimmering dimensional door/warp swoosh sweeping smoothly L -> R
            panOffset = (p - 0.5) * 0.75
            val spiralHz = 360.0 + 540.0 * sin(PI * p) + 160.0 * sin(TWO_PI * 5.0 * p)
            phase += spiralHz * INV_SAMPLE_RATE
            phase2 += (spiralHz * 1.5) * INV_SAMPLE_RATE
            val shimmer = sin(TWO_PI * phase + 1.6 * sin(PI * p) * sin(TWO_PI * phase2))
            val whoosh = lpNoise * sin(PI * p) * 0.72
            (whoosh + shimmer * sin(PI * p) * 0.38) * 0.62
          }
          StudioSfxType.EVOLUTION_CHIME -> {
            // Ascending harp/bell arpeggio resolving into a radiant Major-9th convergence chord
            panOffset = sin(TWO_PI * p * 2.0) * 0.35
            if (p < 0.62) {
              val arpNotes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 987.77, 1046.50, 1174.66)
              val notePos = (p / 0.62) * arpNotes.size
              val idx = notePos.toInt().coerceIn(0, arpNotes.lastIndex)
              val localFrac = notePos - idx
              val hz = arpNotes[idx]
              phase += hz * INV_SAMPLE_RATE
              phase2 += (hz * 2.0) * INV_SAMPLE_RATE
              val bell = sin(TWO_PI * phase + 1.5 * exp(-localFrac * 4.5) * sin(TWO_PI * phase2))
              val pulse = if ((phase % 1.0) < 0.25) 0.45 else -0.45
              (bell * 0.72 + pulse * 0.28) * exp(-localFrac * 2.2) * 0.56
            } else {
              val chordT = (p - 0.62) / 0.38
              val chordEnv = (1.0 - exp(-chordT * 28.0)) * exp(-chordT * 3.4)
              // C5 + E5 + G5 + B5 + D6 glowing convergence chord
              val c5 = sin(TWO_PI * 523.25 * t)
              val e5 = sin(TWO_PI * 659.25 * t)
              val g5 = sin(TWO_PI * 783.99 * t)
              val b5 = sin(TWO_PI * 987.77 * t)
              val d6 = sin(TWO_PI * 1174.66 * t)
              ((c5 + e5 + g5 + b5 * 0.85 + d6 * 0.8) * 0.24) * chordEnv * 0.74
            }
          }
          StudioSfxType.POKEBALL_THROW -> {
            val hz = 320.0 + 980.0 * sin(PI * p)
            phase += hz * INV_SAMPLE_RATE
            sin(TWO_PI * phase) * (1.0 - p) * 0.48
          }
          StudioSfxType.POKEBALL_SHAKE -> {
            val hz = 240.0 + 180.0 * sin(TWO_PI * p * 2.0)
            phase += hz * INV_SAMPLE_RATE
            (sin(TWO_PI * phase) + 0.25 * noise) * exp(-t * 16.0) * 0.45
          }
          StudioSfxType.CAPTURE_SPARKLE -> {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz * INV_SAMPLE_RATE
            (sin(TWO_PI * phase) + 0.35 * sin(TWO_PI * 2.0 * phase)) * exp(-t * 3.5) * 0.52
          }
          StudioSfxType.HIT_LIGHT -> {
            val hz = 260.0 * exp(-t * 22.0) + 75.0
            phase += hz * INV_SAMPLE_RATE
            (sin(TWO_PI * phase) * 0.65 + noise * 0.55) * exp(-t * 22.0) * 0.65
          }
          StudioSfxType.HIT_HEAVY_CRIT -> {
            val hz = 180.0 * exp(-t * 16.0) + 46.0
            phase += hz * INV_SAMPLE_RATE
            val fmBoom = sin(TWO_PI * phase + 2.5 * sin(TWO_PI * 2.0 * phase))
            (fmBoom * 0.72 + noise * 0.65) * exp(-t * 10.5) * 0.82
          }
          StudioSfxType.HIT_RESISTED -> {
            val hz = 155.0 * exp(-t * 26.0) + 60.0
            phase += hz * INV_SAMPLE_RATE
            sin(TWO_PI * phase) * exp(-t * 28.0) * 0.44
          }
          StudioSfxType.STAT_BOOST -> {
            val hz = 330.0 * 2.0.pow(p * 1.4)
            phase += hz * INV_SAMPLE_RATE
            (sin(TWO_PI * phase) + 0.4 * (2.0 * (phase % 1.0) - 1.0)) * (1.0 - p * 0.3) * 0.46
          }
          StudioSfxType.STAT_DROP -> {
            val hz = 740.0 * 2.0.pow(-p * 1.3)
            phase += hz * INV_SAMPLE_RATE
            (2.0 * (phase % 1.0) - 1.0) * (1.0 - p * 0.3) * 0.42
          }
          StudioSfxType.HEAL_CHIME -> {
            val notes = doubleArrayOf(587.33, 739.99, 880.0, 1174.66)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz * INV_SAMPLE_RATE
            sin(TWO_PI * phase) * exp(-t * 5.0) * 0.48
          }
          StudioSfxType.ITEM_USE -> {
            val hz = if (p < 0.5) 523.25 else 783.99
            phase += hz * INV_SAMPLE_RATE
            sin(TWO_PI * phase) * exp(-t * 9.0) * 0.46
          }
          StudioSfxType.LEVEL_UP -> {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 987.77, 1046.50, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz * INV_SAMPLE_RATE
            ((if ((phase % 1.0) < 0.25) 0.75 else -0.75) * 0.6 + sin(TWO_PI * phase) * 0.4) * (1.0 - p * 0.25) * 0.52
          }
        }

        val shaped = tanh(quantizeBitcrush(sanitize(sample), params.bitcrushDepth) * 1.2)
        val gainL = (1.0 - panOffset.coerceAtLeast(0.0)).coerceIn(0.25, 1.0)
        val gainR = (1.0 + panOffset.coerceAtMost(0.0)).coerceIn(0.25, 1.0)
        out[i * 2] = (shaped * gainL * Short.MAX_VALUE * 0.85).toInt().coerceIn(-32767, 32767).toShort()
        out[i * 2 + 1] = (shaped * gainR * Short.MAX_VALUE * 0.85).toInt().coerceIn(-32767, 32767).toShort()
      }
      return out
    }

    /**
     * Deterministic procedural Pokémon Cry synthesizer seeded by National Dex #, types, and base stats.
     */
    fun renderPokemonCryPcm(
      dexNumber: Int,
      primaryType: String,
      secondaryType: String,
      baseHp: Int,
      baseAtk: Int,
      baseSpe: Int,
      params: ConvergedAudioParams,
    ): ShortArray {
      val seed = (dexNumber * 2654435761L xor primaryType.hashCode().toLong() xor secondaryType.hashCode().toLong())
      val durationSec = (0.34 + ((baseHp.coerceIn(30, 160) - 30) / 130.0) * 0.24).coerceIn(0.32, 0.60)
      val frames = (SAMPLE_RATE * durationSec).toInt()
      val out = ShortArray(frames * 2)

      val typeBasePitch = when (primaryType.lowercase()) {
        "electric", "fairy", "bug" -> 620.0
        "dragon", "rock", "steel", "ground" -> 210.0
        "ghost", "psychic", "dark" -> 390.0
        "fire", "fighting" -> 340.0
        else -> 430.0
      }
      val speedLift = (baseSpe.coerceIn(20, 160) / 160.0) * 180.0
      val startHz = (typeBasePitch + speedLift + (abs(seed % 90L))).coerceIn(160.0, 960.0)
      val fmIndex = (1.1 + (baseAtk.coerceIn(30, 160) / 160.0) * 2.4)
      val vibRate = 18.0 + (abs((seed shr 8) % 24L)).toDouble()

      var phase = 0.0
      var modPhase = 0.0
      for (i in 0 until frames) {
        val t = i * INV_SAMPLE_RATE
        val p = i.toDouble() / frames
        val contour = 1.0 + 0.32 * sin(PI * p * 1.8) - 0.26 * p
        val vibrato = 1.0 + 0.06 * sin(TWO_PI * vibRate * t)
        val hz = (startHz * contour * vibrato).coerceIn(90.0, 2200.0)
        phase += hz * INV_SAMPLE_RATE
        modPhase += (hz * 1.5) * INV_SAMPLE_RATE

        val env = (1.0 - exp(-t * 55.0)) * exp(-t * 4.2) * (1.0 - p * 0.25)
        val fmVoice = sin(TWO_PI * phase + fmIndex * env * sin(TWO_PI * modPhase))
        val pulseVoice = if ((phase % 1.0) < 0.25) 0.55 else -0.55
        val raw = (fmVoice * 0.68 + pulseVoice * 0.32) * env * 0.72
        val shaped = tanh(quantizeBitcrush(sanitize(raw), params.bitcrushDepth) * 1.15)
        val sampleL = (shaped * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        val sampleR = (shaped * (0.94 + 0.06 * sin(p * 6.0)) * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        out[i * 2] = sampleL
        out[i * 2 + 1] = sampleR
      }
      return out
    }
  }
}

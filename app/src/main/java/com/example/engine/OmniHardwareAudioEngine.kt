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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

enum class BgmThemeId(
  val id: String,
  val displayName: String,
  val bpm: Double,
  val rootMidi: Int,
  val melodyOffsets: IntArray,
  val bassOffsets: IntArray,
  val arpOffsets: IntArray,
  val energetic: Boolean,
) {
  PRISM_TITLE(
    "bgm_prism_title", "Prism Resonance Title", 124.0, 62,
    intArrayOf(0, 4, 7, 12, 11, 7, 9, 4, 2, 5, 9, 14, 12, 9, 7, 11),
    intArrayOf(0, 0, -5, -5, -3, -3, -7, -7),
    intArrayOf(0, 4, 7, 11),
    true,
  ),
  HARMONIC_LAB(
    "bgm_harmonic_lab", "Harmonic Lab & Town", 104.0, 60,
    intArrayOf(0, 2, 4, 7, 9, 7, 4, 2, 5, 4, 2, 0, -3, 0, 2, 4),
    intArrayOf(0, 0, 5, 5, 2, 2, -5, -5),
    intArrayOf(0, 4, 7, 12),
    false,
  ),
  KANTO_JOHTO_ROUTE(
    "bgm_kanto_johto_route", "Kanto-Johto Classic Route", 132.0, 64,
    intArrayOf(0, 4, 7, 7, 9, 7, 4, 2, 0, 2, 4, 7, 5, 4, 2, 0),
    intArrayOf(0, 7, 5, 7, 0, 7, 2, -5),
    intArrayOf(0, 4, 7, 9),
    true,
  ),
  HOENN_SINNOH_ROUTE(
    "bgm_hoenn_sinnoh_route", "Hoenn-Sinnoh Horizon Route", 138.0, 65,
    intArrayOf(0, 7, 5, 4, 7, 12, 10, 9, 7, 5, 4, 2, 4, 7, 9, 12),
    intArrayOf(0, 0, 5, 5, -2, -2, -5, 0),
    intArrayOf(0, 5, 7, 12),
    true,
  ),
  UNOVA_KALOS_ROUTE(
    "bgm_unova_kalos_route", "Unova-Kalos Neon Route", 128.0, 67,
    intArrayOf(0, 3, 7, 10, 12, 10, 7, 5, 3, 7, 10, 14, 12, 10, 7, 3),
    intArrayOf(0, -2, -5, -7, 0, 3, 5, -2),
    intArrayOf(0, 3, 7, 10),
    true,
  ),
  ALOLA_GALAR_PALDEA_ROUTE(
    "bgm_alola_galar_paldea_route", "Alola-Galar-Paldea Frontier", 134.0, 62,
    intArrayOf(0, 4, 7, 9, 12, 9, 7, 4, 5, 9, 12, 14, 12, 9, 7, 11),
    intArrayOf(0, 5, 7, 2, 0, 5, 9, 7),
    intArrayOf(0, 4, 7, 14),
    true,
  ),
  WILD_ENCOUNTER(
    "bgm_wild_encounter", "Wild Encounter", 156.0, 64,
    intArrayOf(0, 3, 7, 6, 7, 10, 12, 10, 7, 6, 3, 5, 7, 10, 11, 12),
    intArrayOf(0, 0, 3, 3, 5, 5, 7, 6),
    intArrayOf(0, 3, 6, 10),
    true,
  ),
  RIVAL_KAEL(
    "bgm_rival_kael", "Rival Kael Duel", 162.0, 67,
    intArrayOf(0, 7, 6, 7, 10, 12, 10, 7, 5, 7, 10, 14, 15, 14, 12, 10),
    intArrayOf(0, 0, -2, -2, -5, -5, -4, -2),
    intArrayOf(0, 3, 7, 12),
    true,
  ),
  TEAM_MONOCHROME(
    "bgm_team_monochrome", "Team Monochrome Rift Battle", 152.0, 61,
    intArrayOf(0, 1, 4, 7, 6, 4, 1, 0, 7, 8, 11, 12, 11, 8, 7, 4),
    intArrayOf(0, 0, 1, 1, 6, 6, 5, 4),
    intArrayOf(0, 1, 6, 7),
    true,
  ),
  GYM_TRIAL(
    "bgm_gym_trial", "Gym / Trial Arena", 158.0, 69,
    intArrayOf(0, 3, 5, 7, 10, 12, 10, 7, 12, 15, 14, 12, 10, 7, 5, 7),
    intArrayOf(0, 0, -2, -2, -4, -4, -5, -2),
    intArrayOf(0, 3, 7, 10),
    true,
  ),
  CHAMPION_CONVERGENCE(
    "bgm_champion_convergence", "Champion Convergence Finale", 168.0, 64,
    intArrayOf(0, 4, 7, 12, 11, 12, 14, 16, 15, 12, 11, 7, 9, 11, 12, 16),
    intArrayOf(0, 0, -3, -3, -5, -5, -7, -2),
    intArrayOf(0, 4, 7, 12),
    true,
  ),
  VICTORY_FANFARE(
    "bgm_victory_fanfare", "Victory Fanfare", 140.0, 67,
    intArrayOf(0, 4, 7, 12, 9, 12, 14, 16, 12, 16, 14, 12, 11, 12, 14, 16),
    intArrayOf(0, 5, 7, 0, 5, 7, 9, 12),
    intArrayOf(0, 4, 7, 12),
    true,
  ),
  ;

  companion object {
    fun fromId(id: String): BgmThemeId = entries.firstOrNull { it.id == id } ?: PRISM_TITLE
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
 * - 6 Frequency-Separated Stereo Lanes:
 *   1) Mono Center NES 4-bit Triangle + YM2612 2-Op FM Slap Bass with 120-300Hz speaker harmonic lift
 *   2) Heroic Lead (GB/NES 25% Pulse + VRC6 Sawtooth + FM/GBA Brass)
 *   3) Panned Stereo C64 SID / PC Engine Arpeggios
 *   4) Wide Stereo SPC700 / PS1 SPU Cross-Fed Feedback Echo + MSU-1 Detuned Orchestral Pad (+ Extra Pretty Choir)
 *   5) 16-bit LFSR Snare/Hi-Hat + Pitched Kick
 *   6) Soft-limiting master bus via tanh() (guaranteed NaN-free & clipping-free)
 * - Seamless looping BGM via AudioTrack.MODE_STATIC + setLoopPoints(0, frameCount, -1)
 * - Non-stealing SFX bus + deterministic Pokémon Cry synthesizer
 * - Zero Thread.sleep(); safe suspending delay() and NonCancellable cleanup on ON_STOP / onCleared().
 */
class OmniHardwareAudioEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val bgmMutex = Mutex()
  private val trackLock = Any()

  @Volatile private var activeBgmTrack: AudioTrack? = null
  private var activeBgmJob: Job? = null

  @Volatile private var activeBgmThemeId: String? = null

  @Volatile var isMuted: Boolean = false
    private set

  @Volatile var masterVolume: Float = 0.72f
    private set

  @Volatile var activeAudioParams: ConvergedAudioParams = AudioConvergenceSolver.solveAudio(
    activePlatforms = emptyList(),
    disciplineMode = com.example.data.model.MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
    extraPretty = com.example.data.model.ExtraVisualAudioSettings(),
  )

  fun setMuted(muted: Boolean) {
    val wasMuted = isMuted
    isMuted = muted
    applyTrackVolume()
    if (wasMuted && !muted) {
      val themeId = activeBgmThemeId
      if (themeId != null && activeBgmTrack == null) {
        playBgm(BgmThemeId.fromId(themeId))
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
      playBgm(BgmThemeId.fromId(currentTheme))
    }
  }

  private fun applyTrackVolume() {
    val gain = if (isMuted) 0f else masterVolume
    synchronized(trackLock) {
      runCatching { activeBgmTrack?.setVolume(gain) }
    }
  }

  fun playBgm(theme: BgmThemeId) {
    activeBgmThemeId = theme.id
    activeBgmJob?.cancel()
    val paramsSnapshot = activeAudioParams
    activeBgmJob = scope.launch {
      val pcm = renderBgmLoopPcm(theme, paramsSnapshot)
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
          }
        }
      }
    }
  }

  fun stopBgm() {
    activeBgmJob?.cancel()
    activeBgmJob = null
    activeBgmThemeId = null
    scope.launch {
      bgmMutex.withLock {
        stopBgmTrackLocked()
      }
    }
  }

  fun stopBgmImmediate() {
    activeBgmJob?.cancel()
    activeBgmJob = null
    stopBgmTrackLocked()
  }

  private fun stopBgmTrackLocked() {
    val track = synchronized(trackLock) {
      val current = activeBgmTrack
      activeBgmTrack = null
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

  companion object {
    const val SAMPLE_RATE = 44100

    private fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun quantizeBitcrush(sample: Double, depth: Float): Double {
      if (depth <= 0.02f) return sample
      val steps = (32.0 - depth * 22.0).coerceIn(6.0, 32.0)
      return floor(sample * steps + 0.5) / steps
    }

    /**
     * Synthesizes a click-free, seamless 16-step stereo PCM loop (44.1kHz, 16-bit) across 6
     * frequency-separated lanes with cross-fed SPC700/SPU stereo feedback echo and tanh() soft-limiting.
     *
     * Runs 2 passes through the 16-step loop so echo delay lines and oscillator phases are fully
     * warmed up, outputs pass 2 only, and applies a 128-frame cosine micro-crossfade across the
     * loop seam so AudioTrack.setLoopPoints(0, frameCount, -1) loops without any click/pop.
     */
    fun renderBgmLoopPcm(theme: BgmThemeId, params: ConvergedAudioParams): ShortArray {
      val steps = theme.melodyOffsets.size
      val stepDurationSec = 60.0 / (theme.bpm * 2.0) // 8th-note grid
      val samplesPerStep = (SAMPLE_RATE * stepDurationSec).toInt().coerceAtLeast(1024)
      val totalFrames = steps * samplesPerStep
      val pass1L = DoubleArray(totalFrames)
      val pass1R = DoubleArray(totalFrames)
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
      var leadPhase = 0.0
      var bassPhase = 0.0
      var fmModPhase = 0.0
      var arpPhase = 0.0
      var padPhaseA = 0.0
      var padPhaseB = 0.0
      var choirPhase = 0.0

      val echoFb = (params.spcEchoFeedback * 0.42).coerceIn(0.0, 0.46)
      val width = params.stereoWidth.coerceIn(0.15f, 1.0f).toDouble()

      for (pass in 0 until 2) {
        val targetL = if (pass == 0) pass1L else pass2L
        val targetR = if (pass == 0) pass1R else pass2R
        for (frame in 0 until totalFrames) {
          val stepIdx = frame / samplesPerStep
          val stepPos = (frame % samplesPerStep).toDouble() / samplesPerStep
          val tStepSec = (frame % samplesPerStep).toDouble() / SAMPLE_RATE

          // 1. MONO BASS LANE: NES 4-Bit Triangle + YM2612 2-Op FM Slap Bass + 120-300Hz speaker lift
          val bassMidi = theme.rootMidi - 24 + theme.bassOffsets[stepIdx % theme.bassOffsets.size]
          val bassHz = midiToHz(bassMidi).coerceIn(55.0, 220.0)
          bassPhase = (bassPhase + bassHz / SAMPLE_RATE) % 1.0
          fmModPhase = (fmModPhase + (bassHz * 2.0) / SAMPLE_RATE) % 1.0
          val triRaw = (4.0 * abs(bassPhase - 0.5) - 1.0)
          val nesTri4Bit = floor(triRaw * 7.5) / 7.5
          val fmEnv = exp(-tStepSec * 14.0)
          val ym2612Slap = sin(2.0 * PI * bassPhase + (2.4 * fmEnv * sin(2.0 * PI * fmModPhase))) * exp(-tStepSec * 5.0)
          val speakerWarmth = 0.30 * sin(4.0 * PI * bassPhase) + 0.16 * sin(6.0 * PI * bassPhase)
          val bassMono = (nesTri4Bit * params.triangleBassGain * 0.24 +
            ym2612Slap * params.fmSlapBassGain * 0.22 +
            speakerWarmth * 0.14) * (1.0 - stepPos * 0.18)

          // 2. HEROIC LEAD LANE: GB/NES 25% Pulse + VRC6 Sawtooth + FM/GBA Brass
          val leadMidi = theme.rootMidi + theme.melodyOffsets[stepIdx]
          val leadHz = midiToHz(leadMidi)
          leadPhase = (leadPhase + leadHz / SAMPLE_RATE) % 1.0
          val pulse25 = if (leadPhase < 0.25) 0.85 else -0.85
          val vrc6Saw = 2.0 * leadPhase - 1.0
          val gbaBrass = sin(2.0 * PI * leadPhase + 0.9 * sin(2.0 * PI * leadPhase)) * (1.0 - exp(-tStepSec * 38.0))
          val leadEnv = (1.0 - exp(-tStepSec * 65.0)) * exp(-tStepSec * 3.8)
          val leadSig = (pulse25 * 0.45 + vrc6Saw * 0.28 + gbaBrass * 0.27) * leadEnv * params.pulseLeadGain * 0.26

          // 3. PANNED C64 SID / PC ENGINE ARPEGGIO LANE
          val arpSubIdx = ((stepPos * 3.0).toInt()) % theme.arpOffsets.size
          val arpMidi = theme.rootMidi - 12 + theme.arpOffsets[arpSubIdx]
          val arpHz = midiToHz(arpMidi)
          arpPhase = (arpPhase + arpHz / SAMPLE_RATE) % 1.0
          val sidPulse = if (arpPhase < 0.35) 0.7 else -0.7
          val arpSig = sidPulse * exp(-((stepPos * 3.0) % 1.0) * 3.2) * params.sidArpGain * 0.14
          val arpPan = if ((stepIdx + arpSubIdx) % 2 == 0) 0.65 * width else -0.65 * width
          val arpL = arpSig * (0.5 - arpPan * 0.5)
          val arpR = arpSig * (0.5 + arpPan * 0.5)

          // 4. WIDE STEREO MSU-1 DETUNED ORCHESTRAL PAD (+ Extra Pretty Shimmering Choir)
          val padMidi = theme.rootMidi - 12 + theme.bassOffsets[(stepIdx / 2) % theme.bassOffsets.size]
          val padHz = midiToHz(padMidi)
          padPhaseA = (padPhaseA + (padHz * 0.997) / SAMPLE_RATE) % 1.0
          padPhaseB = (padPhaseB + (padHz * 1.003) / SAMPLE_RATE) % 1.0
          val padL = (sin(2.0 * PI * padPhaseA) + 0.35 * sin(4.0 * PI * padPhaseA)) * params.orchestralPadGain * 0.11
          val padR = (sin(2.0 * PI * padPhaseB) + 0.35 * sin(4.0 * PI * padPhaseB)) * params.orchestralPadGain * 0.11

          var choirL = 0.0
          var choirR = 0.0
          if (params.extraLushChoirReverb) {
            choirPhase = (choirPhase + (padHz * 2.0) / SAMPLE_RATE) % 1.0
            val shimmer = sin(2.0 * PI * choirPhase) * 0.075
            val lfoPhase = (frame.toDouble() / totalFrames) * 2.0 * PI
            choirL = shimmer * (0.7 + 0.3 * sin(lfoPhase))
            choirR = shimmer * (0.7 - 0.3 * sin(lfoPhase))
          }

          // 5. 16-BIT LFSR PERCUSSION LANE (Kick + Snare + Hi-Hat)
          var perc = 0.0
          if (theme.energetic || stepIdx % 2 == 0) {
            val bit = ((lfsr shr 0) xor (lfsr shr 2) xor (lfsr shr 3) xor (lfsr shr 5)) and 1
            lfsr = (lfsr shr 1) or (bit shl 15)
            val noise = if ((lfsr and 1) == 1) 1.0 else -1.0
            val isKick = stepIdx % 4 == 0
            val isSnare = stepIdx % 4 == 2
            if (isKick && tStepSec < 0.09) {
              val kickFreq = 145.0 * exp(-tStepSec * 28.0) + 48.0
              perc += sin(2.0 * PI * kickFreq * tStepSec) * exp(-tStepSec * 26.0) * 0.28
            } else if (isSnare && tStepSec < 0.08) {
              perc += noise * exp(-tStepSec * 36.0) * 0.20
            } else if (tStepSec < 0.028) {
              perc += noise * exp(-tStepSec * 85.0) * 0.08
            }
            perc *= params.lfsrPercussionGain
          }

          // Cross-fed SPC700 / PS1 SPU Stereo Echo
          val dryL = leadSig * 0.88 + arpL + padL + choirL
          val dryR = leadSig * 0.88 + arpR + padR + choirR
          val tapL = delayL[dIdxL]
          val tapR = delayR[dIdxR]
          delayL[dIdxL] = dryL + tapR * echoFb
          delayR[dIdxR] = dryR + tapL * echoFb
          dIdxL = (dIdxL + 1) % delayLSize
          dIdxR = (dIdxR + 1) % delayRSize

          val echoMix = if (params.extraLushChoirReverb) 0.42 else 0.28
          var mixL = bassMono + dryL + perc + tapL * echoMix
          var mixR = bassMono + dryR + perc + tapR * echoMix

          if (params.bitcrushDepth > 0.02f) {
            mixL = quantizeBitcrush(mixL, params.bitcrushDepth)
            mixR = quantizeBitcrush(mixR, params.bitcrushDepth)
          }

          targetL[frame] = mixL
          targetR[frame] = mixR
        }
      }

      // 128-frame micro-crossfade across loop seam: smoothly welds end of warmed pass 2 into start of pass 2
      val xfadeFrames = 128.coerceAtMost(totalFrames / 8)
      for (i in 0 until xfadeFrames) {
        val alpha = 0.5 - 0.5 * kotlin.math.cos(PI * i.toDouble() / xfadeFrames)
        val tailIdx = totalFrames - xfadeFrames + i
        pass2L[tailIdx] = pass2L[tailIdx] * (1.0 - alpha) + pass2L[i] * alpha
        pass2R[tailIdx] = pass2R[tailIdx] * (1.0 - alpha) + pass2R[i] * alpha
      }

      // 6. Soft-limiting master bus via tanh() — zero clipping, zero NaN
      for (frame in 0 until totalFrames) {
        val limL = tanh(pass2L[frame] * 1.15)
        val limR = tanh(pass2R[frame] * 1.15)
        out[frame * 2] = (limL * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        out[frame * 2 + 1] = (limR * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
      }
      return out
    }

    /**
     * Synthesizes sharp-transient procedural SFX for UI, footsteps, capture, and battle impact.
     */
    fun renderSfxPcm(type: StudioSfxType, params: ConvergedAudioParams): ShortArray {
      val durationSec = when (type) {
        StudioSfxType.UI_HOVER -> 0.055
        StudioSfxType.UI_CONFIRM -> 0.13
        StudioSfxType.UI_CANCEL -> 0.12
        StudioSfxType.STEP_GRASS, StudioSfxType.STEP_STONE, StudioSfxType.STEP_CRYSTAL -> 0.07
        StudioSfxType.GRASS_RUSTLE -> 0.15
        StudioSfxType.ENCOUNTER_STING -> 0.42
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

      for (i in 0 until frames) {
        val t = i.toDouble() / SAMPLE_RATE
        val p = i.toDouble() / frames
        val bit = ((lfsr shr 0) xor (lfsr shr 2) xor (lfsr shr 3) xor (lfsr shr 5)) and 1
        lfsr = (lfsr shr 1) or (bit shl 15)
        val noise = if ((lfsr and 1) == 1) 1.0 else -1.0

        val sample = when (type) {
          StudioSfxType.UI_HOVER -> {
            val hz = 780.0 + p * 180.0
            phase += hz / SAMPLE_RATE
            (if ((phase % 1.0) < 0.25) 0.6 else -0.6) * exp(-t * 38.0) * 0.35
          }
          StudioSfxType.UI_CONFIRM -> {
            val hz = if (p < 0.45) 659.25 else 987.77
            phase += hz / SAMPLE_RATE
            (if ((phase % 1.0) < 0.25) 0.7 else -0.7) * exp(-t * 14.0) * 0.45
          }
          StudioSfxType.UI_CANCEL -> {
            val hz = 493.88 * (1.0 - p * 0.35)
            phase += hz / SAMPLE_RATE
            (2.0 * (phase % 1.0) - 1.0) * exp(-t * 18.0) * 0.42
          }
          StudioSfxType.STEP_GRASS -> noise * exp(-t * 42.0) * 0.26
          StudioSfxType.STEP_STONE -> {
            val hz = 210.0 * exp(-t * 25.0) + 90.0
            phase += hz / SAMPLE_RATE
            (sin(2.0 * PI * phase) * 0.6 + noise * 0.4) * exp(-t * 48.0) * 0.38
          }
          StudioSfxType.STEP_CRYSTAL -> {
            val hz = 1174.66 + 220.0 * p
            phase += hz / SAMPLE_RATE
            sin(2.0 * PI * phase) * exp(-t * 32.0) * 0.42
          }
          StudioSfxType.GRASS_RUSTLE -> noise * sin(PI * p) * (0.4 + 0.6 * abs(sin(p * 18.0))) * 0.30
          StudioSfxType.ENCOUNTER_STING -> {
            val notes = doubleArrayOf(587.33, 698.46, 880.0, 1174.66, 987.77, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz / SAMPLE_RATE
            ((if ((phase % 1.0) < 0.25) 0.8 else -0.8) + 0.3 * sin(2.0 * PI * phase)) * (1.0 - p * 0.35) * 0.52
          }
          StudioSfxType.POKEBALL_THROW -> {
            val hz = 320.0 + 980.0 * sin(PI * p)
            phase += hz / SAMPLE_RATE
            sin(2.0 * PI * phase) * (1.0 - p) * 0.48
          }
          StudioSfxType.POKEBALL_SHAKE -> {
            val hz = 240.0 + 180.0 * sin(2.0 * PI * p * 2.0)
            phase += hz / SAMPLE_RATE
            (sin(2.0 * PI * phase) + 0.25 * noise) * exp(-t * 16.0) * 0.45
          }
          StudioSfxType.CAPTURE_SPARKLE -> {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz / SAMPLE_RATE
            (sin(2.0 * PI * phase) + 0.35 * sin(4.0 * PI * phase)) * exp(-t * 3.5) * 0.52
          }
          StudioSfxType.HIT_LIGHT -> {
            val hz = 260.0 * exp(-t * 22.0) + 75.0
            phase += hz / SAMPLE_RATE
            (sin(2.0 * PI * phase) * 0.65 + noise * 0.55) * exp(-t * 22.0) * 0.65
          }
          StudioSfxType.HIT_HEAVY_CRIT -> {
            val hz = 180.0 * exp(-t * 16.0) + 46.0
            phase += hz / SAMPLE_RATE
            val fmBoom = sin(2.0 * PI * phase + 2.5 * sin(4.0 * PI * phase))
            (fmBoom * 0.72 + noise * 0.65) * exp(-t * 10.5) * 0.82
          }
          StudioSfxType.HIT_RESISTED -> {
            val hz = 155.0 * exp(-t * 26.0) + 60.0
            phase += hz / SAMPLE_RATE
            sin(2.0 * PI * phase) * exp(-t * 28.0) * 0.44
          }
          StudioSfxType.STAT_BOOST -> {
            val hz = 330.0 * 2.0.pow(p * 1.4)
            phase += hz / SAMPLE_RATE
            (sin(2.0 * PI * phase) + 0.4 * (2.0 * (phase % 1.0) - 1.0)) * (1.0 - p * 0.3) * 0.46
          }
          StudioSfxType.STAT_DROP -> {
            val hz = 740.0 * 2.0.pow(-p * 1.3)
            phase += hz / SAMPLE_RATE
            (2.0 * (phase % 1.0) - 1.0) * (1.0 - p * 0.3) * 0.42
          }
          StudioSfxType.HEAL_CHIME -> {
            val notes = doubleArrayOf(587.33, 739.99, 880.0, 1174.66)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz / SAMPLE_RATE
            sin(2.0 * PI * phase) * exp(-t * 5.0) * 0.48
          }
          StudioSfxType.ITEM_USE -> {
            val hz = if (p < 0.5) 523.25 else 783.99
            phase += hz / SAMPLE_RATE
            sin(2.0 * PI * phase) * exp(-t * 9.0) * 0.46
          }
          StudioSfxType.LEVEL_UP -> {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 987.77, 1046.50, 1318.51)
            val hz = notes[(p * notes.size).toInt().coerceIn(0, notes.lastIndex)]
            phase += hz / SAMPLE_RATE
            ((if ((phase % 1.0) < 0.25) 0.75 else -0.75) * 0.6 + sin(2.0 * PI * phase) * 0.4) * (1.0 - p * 0.25) * 0.52
          }
        }

        val shaped = tanh(quantizeBitcrush(sample, params.bitcrushDepth) * 1.2)
        val pcmVal = (shaped * Short.MAX_VALUE * 0.85).toInt().coerceIn(-32767, 32767).toShort()
        out[i * 2] = pcmVal
        out[i * 2 + 1] = pcmVal
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
        val t = i.toDouble() / SAMPLE_RATE
        val p = i.toDouble() / frames
        val contour = 1.0 + 0.32 * sin(PI * p * 1.8) - 0.26 * p
        val vibrato = 1.0 + 0.06 * sin(2.0 * PI * vibRate * t)
        val hz = (startHz * contour * vibrato).coerceIn(90.0, 2200.0)
        phase += hz / SAMPLE_RATE
        modPhase += (hz * 1.5) / SAMPLE_RATE

        val env = (1.0 - exp(-t * 55.0)) * exp(-t * 4.2) * (1.0 - p * 0.25)
        val fmVoice = sin(2.0 * PI * phase + fmIndex * env * sin(2.0 * PI * modPhase))
        val pulseVoice = if ((phase % 1.0) < 0.25) 0.55 else -0.55
        val raw = (fmVoice * 0.68 + pulseVoice * 0.32) * env * 0.72
        val shaped = tanh(quantizeBitcrush(raw, params.bitcrushDepth) * 1.15)
        val sampleL = (shaped * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        val sampleR = (shaped * (0.94 + 0.06 * sin(p * 6.0)) * Short.MAX_VALUE * 0.82).toInt().coerceIn(-32767, 32767).toShort()
        out[i * 2] = sampleL
        out[i * 2 + 1] = sampleR
      }
      return out
    }
  }
}

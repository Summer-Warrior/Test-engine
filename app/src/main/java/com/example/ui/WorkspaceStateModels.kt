package com.example.ui

import android.graphics.Bitmap
import com.example.data.model.CharacterMixMode
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.engine.BattleGimmickMode
import com.example.engine.BgmThemeId
import com.example.engine.ConvergedAudioParams
import com.example.engine.ConvergedVisualParams
import com.example.engine.HardwareConvergenceSolver
import com.example.engine.HardwarePlatformSpec
import com.example.engine.UniversalHardwareRegistry
import com.example.engine.AudioConvergenceSolver
import com.example.engine.PokemonType
import com.example.engine.SpritePixelMatrixEngine
import com.example.engine.StatusCondition
import com.example.engine.TerrainCondition
import com.example.engine.WeatherCondition

enum class SpriteToolMode(val label: String) {
  PENCIL("Pencil"),
  ERASER("Eraser"),
  FILL("Fill"),
  EYEDROPPER("Picker"),
}

/**
 * Hoisted Sprite Editor state so switching tabs NEVER loses user pixel frames, palette edits, or undo history.
 */
data class SpriteEditorUiState(
  val seedDexNumber: Int = 384,
  val spriteName: String = "Rayquaza Omni-Sprite",
  val activeSavedSpriteId: String? = null,
  val paletteArgb: List<Long> = SpritePixelMatrixEngine.buildInitialPalette("#10B981", "#FFD166"),
  val selectedColorSlot: Int = 0,
  val frames: List<List<Int>> = listOf(
    SpritePixelMatrixEngine.generateSeededFrame(384, 0),
    SpritePixelMatrixEngine.generateSeededFrame(384, 1),
  ),
  val activeFrameIndex: Int = 0,
  val undoStack: List<List<Int>> = emptyList(),
  val activeTool: SpriteToolMode = SpriteToolMode.PENCIL,
  val mirrorX: Boolean = true,
  val showGrid: Boolean = true,
  val isPlayingPreview: Boolean = false,
  val previewFps: Int = 6,
  val useCustomBitmapInViewport: Boolean = false,
  val renderedPreviewBitmap: Bitmap? = null,
) {
  val currentFrame: List<Int>
    get() = frames.getOrElse(activeFrameIndex) { List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 } }
}

/**
 * Hoisted Game Test state so switching tabs NEVER loses player coordinates, battle HP, party, bag, or log.
 */
data class GameTestUiState(
  val activeRegionId: String = "kanto",
  val playerRow: Int = 5,
  val playerCol: Int = 2,
  val stepsTaken: Int = 0,
  val inBattle: Boolean = false,
  val turnNumber: Int = 1,
  val activePartyDexIds: List<Int> = listOf(25, 6, 94, 448, 658, 384),
  val activePartyIndex: Int = 0,
  val partyCurrentHpMap: Map<Int, Int> = emptyMap(),
  val partyStatusMap: Map<Int, StatusCondition> = emptyMap(),
  val heldItemsMap: Map<Int, String> = mapOf(
    25 to "life_orb",
    6 to "choice_specs",
    94 to "focus_sash",
    448 to "choice_band",
    658 to "choice_scarf",
    384 to "leftovers",
  ),
  val enemyDexId: Int = 94,
  val enemyLevel: Int = 48,
  val enemyCurrentHp: Int = 135,
  val enemyMaxHp: Int = 135,
  val enemyStatus: StatusCondition = StatusCondition.HEALTHY,
  val isBossEncounter: Boolean = false,
  val activeGimmick: BattleGimmickMode = BattleGimmickMode.NONE,
  val teraType: PokemonType = PokemonType.ELECTRIC,
  val weather: WeatherCondition = WeatherCondition.CLEAR,
  val terrain: TerrainCondition = TerrainCondition.NONE,
  val isBagOpen: Boolean = false,
  val selectedBagCategory: String = "POKE_BALL",
  val battleLog: List<String> = listOf(
    "Prof. Lyra Vance: Welcome to Pokémon: Prism Resonance! Explore tall wild grass or step onto a Prism Sanctuary.",
  ),
  val lastEventNonce: Long = 0L,
  val lastHitSeverity: Int = 0, // 0=none, 1=normal, 2=super/crit, 3=resisted
  val lastAttackerWasPlayer: Boolean = true,
  val transitionPulseNonce: Long = 0L,
) {
  val activeLeadDexId: Int
    get() = activePartyDexIds.getOrElse(activePartyIndex) { activePartyDexIds.firstOrNull() ?: 25 }
}

/**
 * Hoisted Universal Hardware Registry & Convergence state (all 85+ real platforms 1962-2026).
 */
data class HardwareRegistryUiState(
  val allPlatforms: List<HardwarePlatformSpec> = emptyList(),
  val activePlatformIds: Set<String> = UniversalHardwareRegistry.defaultCuratedHardwareIds,
  val allHardwareEnabled: Boolean = false,
  val disciplineMode: MasterDisciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
  val characterMixMode: CharacterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
  val extraPretty: ExtraVisualAudioSettings = ExtraVisualAudioSettings(),
  val convergedVisuals: ConvergedVisualParams = HardwareConvergenceSolver.solveVisuals(
    activePlatforms = emptyList(),
    disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
    extraPretty = ExtraVisualAudioSettings(),
  ),
  val convergedAudio: ConvergedAudioParams = AudioConvergenceSolver.solveAudio(
    activePlatforms = emptyList(),
    disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
    extraPretty = ExtraVisualAudioSettings(),
  ),
)

/**
 * Hoisted Omni-Hardware Audio Console state.
 */
data class AudioConsoleUiState(
  val isMuted: Boolean = false,
  val masterVolume: Float = 0.72f,
  val isBgmPlaying: Boolean = false,
  val activeBgmTheme: BgmThemeId = BgmThemeId.PRISM_TITLE,
)

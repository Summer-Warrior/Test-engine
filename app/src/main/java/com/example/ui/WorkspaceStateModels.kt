package com.example.ui

import android.graphics.Bitmap
import com.example.data.model.CharacterMixMode
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.engine.AmbientBedType
import com.example.engine.AudioConvergenceSolver
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BgmThemeId
import com.example.engine.ConvergedAudioParams
import com.example.engine.ConvergedVisualParams
import com.example.engine.HardwareConvergenceSolver
import com.example.engine.HardwarePlatformSpec
import com.example.engine.HardwareRuntimePresetCatalog
import com.example.engine.MoveDirection
import com.example.engine.PokemonType
import com.example.engine.RegionPointOfInterest
import com.example.engine.SpritePixelMatrixEngine
import com.example.engine.StatusCondition
import com.example.engine.TerrainCondition
import com.example.engine.TimeOfDayCycle
import com.example.engine.TrainerAiTactic
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
import com.example.engine.UniversalHardwareRegistry
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
 * Hoisted Game Test state so switching tabs NEVER loses 16x16 coordinates, battle HP, levels,
 * stat stages, taught TM moves, NPC dialogues, party, bag, lighting cycle, or battle log.
 */
data class GameTestUiState(
  val activeRegionId: String = "kanto",
  val playerRow: Int = 13,
  val playerCol: Int = 3,
  val playerFacing: MoveDirection = MoveDirection.DOWN,
  val playerWalkStep: Int = 0,
  val prevPlayerRow: Int? = 13,
  val prevPlayerCol: Int? = 2,
  val showTrailingPartner: Boolean = true,
  val stepsTaken: Int = 0,
  val inBattle: Boolean = false,
  val turnNumber: Int = 1,
  val activePartyDexIds: List<Int> = listOf(25, 6, 94, 448, 658, 384),
  val activePartyIndex: Int = 0,
  val partyCurrentHpMap: Map<Int, Int> = emptyMap(),
  val partyStatusMap: Map<Int, StatusCondition> = emptyMap(),
  val partyLevelMap: Map<Int, Int> = mapOf(
    25 to 50,
    6 to 50,
    94 to 50,
    448 to 50,
    658 to 50,
    384 to 50,
  ),
  val taughtMovesMap: Map<Int, BattleMoveSpec> = emptyMap(),
  val heldItemsMap: Map<Int, String> = mapOf(
    25 to "life_orb",
    6 to "choice_specs",
    94 to "focus_sash",
    448 to "choice_band",
    658 to "choice_scarf",
    384 to "leftovers",
  ),
  val playerAtkStage: Int = 0,
  val playerSpAStage: Int = 0,
  val enemyAtkStage: Int = 0,
  val enemySpAStage: Int = 0,
  val bondGauge: Int = 80,
  val enemyDexId: Int = 94,
  val enemyLevel: Int = 48,
  val enemyCurrentHp: Int = 135,
  val enemyMaxHp: Int = 135,
  val enemyStatus: StatusCondition = StatusCondition.HEALTHY,
  val isBossEncounter: Boolean = false,
  val activeTrainerClass: String? = null,
  val activeTrainerAiTactic: TrainerAiTactic? = null,
  val activeTrainerGimmick: BattleGimmickMode = BattleGimmickMode.NONE,
  val activeTrainerDefeatQuote: String? = null,
  val activeEnemyTrainer: TrainerCharacterSpec? = null,
  val enemyPartyDexIds: List<Int> = emptyList(),
  val enemyPartyIndex: Int = 0,
  val enemyPartyCurrentHpMap: Map<Int, Int> = emptyMap(),
  val enemyPartyMaxHpMap: Map<Int, Int> = emptyMap(),
  val showTrainerVsBanner: Boolean = false,
  val enemyTrainerPose: TrainerPose = TrainerPose.BATTLE_IDLE,
  val activeGimmick: BattleGimmickMode = BattleGimmickMode.NONE,
  val teraType: PokemonType = PokemonType.ELECTRIC,
  val weather: WeatherCondition = WeatherCondition.CLEAR,
  val terrain: TerrainCondition = TerrainCondition.NONE,
  val timeOfDay: TimeOfDayCycle = TimeOfDayCycle.DAY,
  val showFullRegionView: Boolean = true,
  val showWorldAtlas: Boolean = false,
  val showSubZoneLabels: Boolean = true,
  val isBagOpen: Boolean = false,
  val selectedBagCategory: String = "POKE_BALL",
  val activeDialoguePoi: RegionPointOfInterest? = null,
  val showWarpGatePicker: Boolean = false,
  val battleLog: List<String> = listOf(
    "Prof. Lyra Vance: Welcome to Pokémon: Prism Resonance v5.0! Explore all 22 drawn fusion regions, battle every Trainer Type, and encounter all 1,025 Pokémon!",
  ),
  val lastEventNonce: Long = 0L,
  val lastHitSeverity: Int = 0, // 0=none, 1=normal, 2=super/crit, 3=resisted
  val lastAttackerWasPlayer: Boolean = true,
  val transitionPulseNonce: Long = 0L,
) {
  val activeLeadDexId: Int
    get() = activePartyDexIds.getOrElse(activePartyIndex) { activePartyDexIds.firstOrNull() ?: 25 }

  fun levelFor(dexNumber: Int): Int = (partyLevelMap[dexNumber] ?: 50).coerceIn(1, 100)
}

/**
 * Hoisted Universal Hardware Registry & Convergence state (all 156 real platforms 1962-2026).
 * Boots by default into Ultimate Target Style (All 156 Hardware — Shovel Knight+ Curated).
 */
data class HardwareRegistryUiState(
  val allPlatforms: List<HardwarePlatformSpec> = emptyList(),
  val activePlatformIds: Set<String> = UniversalHardwareRegistry.defaultCuratedHardwareIds,
  val allHardwareEnabled: Boolean = true,
  val disciplineMode: MasterDisciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
  val characterMixMode: CharacterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
  val extraPretty: ExtraVisualAudioSettings = ExtraVisualAudioSettings(),
  val activeRuntimePresetId: String = HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED,
  val activeRuntimeLabel: String = "Ultimate Target Style — All 156 Hardware (Shovel Knight+ Curated)",
  val isHardwareTourActive: Boolean = false,
  val hardwareTourStepLabel: String = "",
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
  val ambientBedEnabled: Boolean = true,
  val activeAmbientBed: AmbientBedType = AmbientBedType.MEADOW_WIND,
  val lowHpUrgencyActive: Boolean = false,
  val bossClimaxActive: Boolean = false,
)

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.local.StudioItemEntity
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.MasterDisciplineMode
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BgmThemeId
import com.example.engine.CampActivityType
import com.example.engine.CampaignProgressionState
import com.example.engine.ContestAppealSpec
import com.example.engine.ContestCategory
import com.example.engine.GameFeelFrameState
import com.example.engine.HardwareRuntimePreset
import com.example.engine.ImpactWeight
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.MoveDirection
import com.example.engine.PoiInteractionKind
import com.example.engine.PokemonType
import com.example.engine.RegionPointOfInterest
import com.example.engine.SideModeType
import com.example.engine.SideModesSessionState
import com.example.engine.StatStages
import com.example.engine.StatusCondition
import com.example.engine.StudioSfxType
import com.example.engine.TerrainCondition
import com.example.engine.TimeOfDayCycle
import com.example.engine.TowerBpReward
import com.example.engine.TrainerAppearanceConfig
import com.example.engine.TrainerArchetype
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
import com.example.engine.UniversalGameFeelEngine
import com.example.engine.WeatherCondition
import com.example.ui.AudioConsoleUiState
import com.example.ui.GameTestUiState
import com.example.ui.HardwareRegistryUiState
import com.example.ui.StudioViewModelHelpers
import com.example.ui.shaders.omniDisplayPostEffect
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.abs

private enum class GameTestHudPanel {
  SIDE_MODES,
  HARDWARE,
  AUDIO,
  REGIONS,
  WARDROBE,
  SUMMONER,
  LOG,
}

/**
 * Playmat-First Playable Game Test Sandbox for Pokémon: Prism Resonance v5.0.
 * - Immediately shows the Playable Overworld / Battle Stage + Controls + Active Party at the TOP.
 * - Compact HUD launcher bar above the stage toggles collapsible drawers for Hardware Runtime,
 *   Audio Console, 22-Region Atlas, Bag, Hero Wardrobe, Roster Summoner, and Combat Log.
 * - Strictly isolates Hero custom pixel avatar from Creature custom sprites.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameTestSection(
  gameState: GameTestUiState,
  campaignState: CampaignProgressionState,
  fusionConfig: ConsoleFusionConfig,
  hardwareState: HardwareRegistryUiState,
  audioState: AudioConsoleUiState,
  mapGrid: List<Int>,
  studioItems: List<StudioItemEntity>,
  customSpriteBitmap: Bitmap?,
  useCustomSprite: Boolean,
  resolvePokemonByDex: (Int) -> NativePokemonEntity,
  onSelectRegion: (String) -> Unit,
  onMoveOverworld: (dRow: Int, dCol: Int) -> Unit,
  onInteractOverworld: () -> Unit,
  onDismissDialogue: () -> Unit,
  onTriggerPoiBattle: (RegionPointOfInterest) -> Unit,
  onWarpToRegion: (String) -> Unit,
  onDismissWarpPicker: () -> Unit,
  onTriggerEncounter: (isBoss: Boolean) -> Unit,
  onExecuteMove: (BattleMoveSpec) -> Unit,
  onToggleGimmick: (BattleGimmickMode) -> Unit,
  onSelectTeraType: (PokemonType) -> Unit,
  onSelectWeather: (WeatherCondition) -> Unit,
  onSelectTerrain: (TerrainCondition) -> Unit,
  onSwitchPartySlot: (Int) -> Unit,
  onToggleBag: (Boolean?) -> Unit,
  onSelectBagCategory: (String) -> Unit,
  onUseBagItem: (StudioItemEntity) -> Unit,
  onFleeBattle: () -> Unit,
  onHealParty: () -> Unit,
  onToggleBgm: () -> Unit,
  onSelectBgmTheme: (BgmThemeId) -> Unit,
  onToggleMute: () -> Unit,
  onSetVolume: (Float) -> Unit,
  onPlayLeadCry: (NativePokemonEntity) -> Unit,
  onSelectHardwarePreset: (HardwareRuntimePreset) -> Unit,
  onApplyCustomHardwareCombo: (Set<String>, MasterDisciplineMode) -> Unit,
  onToggleHardwareTour: () -> Unit,
  onSelectTimeOfDay: (TimeOfDayCycle) -> Unit,
  onToggleFullRegionView: () -> Unit,
  onToggleWorldAtlas: () -> Unit,
  onToggleSubZoneLabels: () -> Unit,
  onChallengeTrainer: (TrainerArchetype) -> Unit,
  onSummonWildPokemon: (Int) -> Unit,
  onAddPokemonToParty: (Int) -> Unit,
  playerTrainer: TrainerCharacterSpec = TrainerCatalogAndRigEngine.defaultProtagonist,
  playerCustomAvatarBitmap: Bitmap? = null,
  useCustomPlayerAvatarBitmap: Boolean = false,
  hydratedByDex: Map<Int, NativePokemonEntity> = emptyMap(),
  onSelectProtagonistPreset: (TrainerCharacterSpec) -> Unit = {},
  onUpdateProtagonistIdentity: (String, String) -> Unit = { _, _ -> },
  onUpdateProtagonistAppearance: ((TrainerAppearanceConfig) -> TrainerAppearanceConfig) -> Unit = {},
  onEditHeroInSpriteEditor: () -> Unit = {},
  onToggleUseCustomPlayerAvatar: () -> Unit = {},
  onSaveHeroToRoom: (TrainerCharacterSpec) -> Unit = {},
  onDismissVsBanner: () -> Unit = {},
  onToggleTrailingPartner: () -> Unit = {},
  onOpenTrainerStudio: () -> Unit = {},
  onToggleAmbientBed: () -> Unit = {},
  onPlaySfx: (StudioSfxType) -> Unit = {},
  sideModesState: SideModesSessionState = SideModesSessionState(),
  onSelectSideMode: (SideModeType) -> Unit = {},
  onStartTowerGauntlet: () -> Unit = {},
  onExecuteTowerMove: (BattleMoveSpec) -> Unit = {},
  onSwitchTowerSlot: (Int) -> Unit = {},
  onAdvanceTowerRound: () -> Unit = {},
  onRedeemBpReward: (TowerBpReward) -> Unit = {},
  onStartContest: (ContestCategory) -> Unit = {},
  onPerformContestAppeal: (ContestAppealSpec) -> Unit = {},
  onStepSnapFrame: () -> Unit = {},
  onTossSnapIlluminaOrb: () -> Unit = {},
  onTossSnapApple: () -> Unit = {},
  onNextSnapSubject: () -> Unit = {},
  onCaptureSnapPhoto: () -> Unit = {},
  onStartRiftDungeon: () -> Unit = {},
  onStepRiftDirection: (Int, Int) -> Unit = { _, _ -> },
  onUseRiftPrismPulse: () -> Unit = {},
  onUseRiftOranBerry: () -> Unit = {},
  onStepRangerWild: () -> Unit = {},
  onBeginRangerDrag: (Float, Float) -> Unit = { _, _ -> },
  onStepRangerDrag: (Float, Float) -> Unit = { _, _ -> },
  onEndRangerDrag: () -> Unit = {},
  onNextRangerTarget: () -> Unit = {},
  onRecruitRangerTarget: () -> Unit = {},
  onPerformCampActivity: (CampActivityType) -> Unit = {},
) {
  val haptics = LocalHapticFeedback.current
  val visuals = hardwareState.convergedVisuals
  val activeChapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gameState.activeRegionId }
    ?: MasterCanonTemplateCatalog.worldChapters.first()
  val regionAccent = parseHexColorSafe(activeChapter.regionAccentHex, CyberCyan)

  val leadEntity = resolvePokemonByDex(gameState.activeLeadDexId)
  val enemyEntity = resolvePokemonByDex(gameState.enemyDexId)
  val leadLevel = gameState.levelFor(leadEntity.dexNumber)
  val leadHeldItem = gameState.heldItemsMap[leadEntity.dexNumber].orEmpty()
  val leadTaughtMove = gameState.taughtMovesMap[leadEntity.dexNumber]

  val leadSnap = remember(leadEntity, leadLevel, gameState.activeGimmick, gameState.teraType, leadHeldItem, gameState.playerAtkStage, gameState.playerSpAStage, gameState.bondGauge) {
    StudioViewModelHelpers.buildCombatantFromEntity(
      entity = leadEntity,
      level = leadLevel,
      gimmick = gameState.activeGimmick,
      teraType = gameState.teraType,
      heldItemId = leadHeldItem,
      stages = StatStages(atk = gameState.playerAtkStage, spA = gameState.playerSpAStage),
      bondGauge = gameState.bondGauge,
    )
  }
  val playerMaxHp = leadSnap.maxHp()
  val playerCurrentHp = (gameState.partyCurrentHpMap[leadEntity.dexNumber] ?: playerMaxHp).coerceIn(0, playerMaxHp)
  val moveset = remember(leadEntity, gameState.teraType, leadTaughtMove) {
    StudioViewModelHelpers.buildMovesetForPokemon(leadEntity, gameState.teraType, taughtSlot4Move = leadTaughtMove)
  }

  val gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE
  val currentTileCode = mapGrid.getOrElse(gameState.playerRow * gridSize + gameState.playerCol) { 0 }
  val currentTileLabel = MasterCanonTemplateCatalog.tileLegendLabels
    .firstOrNull { it.first == currentTileCode }
    ?.second ?: "Open Path"
  val activeSubZone = activeChapter.subZones.minByOrNull { zone ->
    abs(zone.col - gameState.playerCol) + abs(zone.row - gameState.playerRow)
  }
  val nearbyPoi = activeChapter.pointsOfInterest.firstOrNull { poi ->
    abs(poi.col - gameState.playerCol) + abs(poi.row - gameState.playerRow) <= 1
  }

  // Playmat-First HUD Panel Drawer State (all collapsed by default so Playable Stage is #1!)
  var activeHudPanel by remember { mutableStateOf<GameTestHudPanel?>(null) }
  fun toggleHudPanel(panel: GameTestHudPanel) {
    if (gameState.isBagOpen) onToggleBag(false)
    activeHudPanel = if (activeHudPanel == panel) null else panel
  }

  // 60 FPS Universal Game Feel Engine Controller
  var feelFrame by remember { mutableStateOf(GameFeelFrameState()) }

  LaunchedEffect(gameState.lastEventNonce) {
    if (gameState.lastEventNonce > 0L) {
      val weight = when (gameState.lastHitSeverity) {
        2 -> ImpactWeight.HEAVY_OR_CRIT
        3 -> ImpactWeight.RESISTED
        else -> ImpactWeight.NORMAL
      }
      feelFrame = UniversalGameFeelEngine.triggerImpact(
        current = feelFrame,
        weight = weight,
        targetIsEnemy = gameState.lastAttackerWasPlayer,
      )
      haptics.performHapticFeedback(
        if (weight == ImpactWeight.HEAVY_OR_CRIT) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
      )
    }
  }

  LaunchedEffect(gameState.transitionPulseNonce) {
    if (gameState.transitionPulseNonce > 0L) {
      feelFrame = UniversalGameFeelEngine.triggerBattleEntryMosaic(feelFrame)
    }
  }

  LaunchedEffect(Unit) {
    var lastFrameMs = 0L
    while (true) {
      withFrameMillis { frameTimeMs ->
        val dt = if (lastFrameMs == 0L) 16.6f else (frameTimeMs - lastFrameMs).toFloat().coerceIn(0f, 50f)
        lastFrameMs = frameTimeMs
        val stepped = UniversalGameFeelEngine.stepFrame(feelFrame, dt)
        val bufferedDir = stepped.bufferedDirection
        if (bufferedDir != null && !gameState.inBattle && stepped.hitstopRemainingMs <= 0f) {
          feelFrame = UniversalGameFeelEngine.consumeBufferedInput(stepped)
          onMoveOverworld(bufferedDir.dy, bufferedDir.dx)
        } else {
          feelFrame = stepped
        }
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // 1. COMPACT PLAYMAT-FIRST HUD LAUNCHER BAR (Always visible, 1-tap toggle drawers)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
        .testTag("game_test_hud_launcher_bar"),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${activeChapter.regionName.substringAfter(". ").substringBefore(" —")} • ${campaignState.harmonicShardsCollected}/${MasterCanonTemplateCatalog.worldChapters.size} Shards • ${visuals.activeCount} HW",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${playerTrainer.name} • Lead: ${leadEntity.name} Lv.$leadLevel • ${gameState.timeOfDay.label}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onToggleBgm,
              modifier = Modifier.size(40.dp).testTag("hud_quick_bgm_toggle"),
            ) {
              Icon(
                imageVector = if (audioState.isBgmPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (audioState.isBgmPlaying) "Pause BGM" else "Play BGM",
                tint = SgbEmerald,
                modifier = Modifier.size(20.dp),
              )
            }
            IconButton(
              onClick = onToggleMute,
              modifier = Modifier.size(40.dp).testTag("hud_quick_mute_toggle"),
            ) {
              Icon(
                imageVector = if (audioState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (audioState.isMuted) "Unmute Audio" else "Mute Audio",
                tint = if (audioState.isMuted) ElectricCrimson else CyberCyan,
                modifier = Modifier.size(19.dp),
              )
            }
            IconButton(
              onClick = onHealParty,
              modifier = Modifier.size(40.dp).testTag("game_test_heal_party_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Healing,
                contentDescription = "Heal Full Party",
                tint = SgbEmerald,
                modifier = Modifier.size(19.dp),
              )
            }
          }
        }

        // Scrollable Collapsible Drawer Toggle Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.SIDE_MODES,
            onClick = { toggleHudPanel(GameTestHudPanel.SIDE_MODES) },
            leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Side Modes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuperFxGold.copy(alpha = 0.28f), selectedLabelColor = SuperFxGold),
            modifier = Modifier.testTag("hud_toggle_side_modes"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.REGIONS,
            onClick = { toggleHudPanel(GameTestHudPanel.REGIONS) },
            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("22 Regions", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan.copy(alpha = 0.22f), selectedLabelColor = CyberCyan),
            modifier = Modifier.testTag("hud_toggle_regions"),
          )
          FilterChip(
            selected = gameState.isBagOpen,
            onClick = {
              activeHudPanel = null
              onToggleBag(null)
            },
            leadingIcon = { Icon(Icons.Default.Backpack, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Bag", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuperFxGold.copy(alpha = 0.24f), selectedLabelColor = SuperFxGold),
            modifier = Modifier.testTag("game_test_open_bag_button"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.WARDROBE,
            onClick = { toggleHudPanel(GameTestHudPanel.WARDROBE) },
            leadingIcon = { Icon(Icons.Default.Checkroom, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Wardrobe", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuperFxGold.copy(alpha = 0.24f), selectedLabelColor = SuperFxGold),
            modifier = Modifier.testTag("hud_toggle_wardrobe"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.SUMMONER,
            onClick = { toggleHudPanel(GameTestHudPanel.SUMMONER) },
            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Summoner", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoxelViolet.copy(alpha = 0.26f), selectedLabelColor = VoxelViolet),
            modifier = Modifier.testTag("hud_toggle_summoner"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.HARDWARE,
            onClick = { toggleHudPanel(GameTestHudPanel.HARDWARE) },
            leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Hardware", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SgbEmerald.copy(alpha = 0.22f), selectedLabelColor = SgbEmerald),
            modifier = Modifier.testTag("hud_toggle_hardware"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.AUDIO,
            onClick = { toggleHudPanel(GameTestHudPanel.AUDIO) },
            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Audio", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuperFxGold.copy(alpha = 0.22f), selectedLabelColor = SuperFxGold),
            modifier = Modifier.testTag("hud_toggle_audio"),
          )
          FilterChip(
            selected = activeHudPanel == GameTestHudPanel.LOG,
            onClick = { toggleHudPanel(GameTestHudPanel.LOG) },
            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Log (${gameState.battleLog.size})", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan.copy(alpha = 0.22f), selectedLabelColor = CyberCyan),
            modifier = Modifier.testTag("hud_toggle_log"),
          )
        }
      }
    }

    // 2. COLLAPSIBLE HUD DRAWER SLOT (Renders right above the playmat only when toggled open)
    if (gameState.isBagOpen) {
      GameTestBagDrawerCard(
        leadEntity = leadEntity,
        leadLevel = leadLevel,
        selectedCategory = gameState.selectedBagCategory,
        studioItems = studioItems,
        onCloseBag = { onToggleBag(false) },
        onSelectCategory = onSelectBagCategory,
        onUseBagItem = onUseBagItem,
      )
    } else {
      when (activeHudPanel) {
        GameTestHudPanel.SIDE_MODES -> SideModesHubSection(
          session = sideModesState,
          activeChapter = activeChapter,
          timeOfDay = gameState.timeOfDay,
          partyDexIds = gameState.activePartyDexIds,
          bondGauge = gameState.bondGauge,
          is3dCelModel = fusionConfig.pixelTo3dBlend >= 0.5f,
          resolvePokemonByDex = resolvePokemonByDex,
          onCloseHub = { activeHudPanel = null },
          onSelectMode = onSelectSideMode,
          onStartTowerGauntlet = onStartTowerGauntlet,
          onExecuteTowerMove = onExecuteTowerMove,
          onSwitchTowerSlot = onSwitchTowerSlot,
          onAdvanceTowerRound = onAdvanceTowerRound,
          onRedeemBpReward = onRedeemBpReward,
          onStartContest = onStartContest,
          onPerformContestAppeal = onPerformContestAppeal,
          onStepSnapFrame = onStepSnapFrame,
          onTossSnapIlluminaOrb = onTossSnapIlluminaOrb,
          onTossSnapApple = onTossSnapApple,
          onNextSnapSubject = onNextSnapSubject,
          onCaptureSnapPhoto = onCaptureSnapPhoto,
          onStartRiftDungeon = onStartRiftDungeon,
          onStepRiftDirection = onStepRiftDirection,
          onUseRiftPrismPulse = onUseRiftPrismPulse,
          onUseRiftOranBerry = onUseRiftOranBerry,
          onStepRangerWild = onStepRangerWild,
          onBeginRangerDrag = onBeginRangerDrag,
          onStepRangerDrag = onStepRangerDrag,
          onEndRangerDrag = onEndRangerDrag,
          onNextRangerTarget = onNextRangerTarget,
          onRecruitRangerTarget = onRecruitRangerTarget,
          onPerformCampActivity = onPerformCampActivity,
        )
        GameTestHudPanel.HARDWARE -> GameTestHardwareRuntimeCard(
          hardwareState = hardwareState,
          onSelectPreset = onSelectHardwarePreset,
          onApplyCustomCombo = onApplyCustomHardwareCombo,
          onToggleHardwareTour = onToggleHardwareTour,
        )
        GameTestHudPanel.AUDIO -> GameTestAudioConsoleCard(
          hardwareState = hardwareState,
          audioState = audioState,
          activeChapter = activeChapter,
          leadEntity = leadEntity,
          onToggleBgm = onToggleBgm,
          onSelectBgmTheme = onSelectBgmTheme,
          onToggleMute = onToggleMute,
          onSetVolume = onSetVolume,
          onPlayLeadCry = onPlayLeadCry,
          onToggleAmbientBed = onToggleAmbientBed,
          onPlaySfx = onPlaySfx,
        )
        GameTestHudPanel.REGIONS -> GameTestRegionAtlasPanel(
          gameState = gameState,
          campaignState = campaignState,
          activeChapter = activeChapter,
          onSelectRegion = onSelectRegion,
          onSelectTimeOfDay = onSelectTimeOfDay,
          onToggleFullRegionView = onToggleFullRegionView,
          onToggleSubZoneLabels = onToggleSubZoneLabels,
          onToggleWorldAtlas = onToggleWorldAtlas,
        )
        GameTestHudPanel.WARDROBE -> ProtagonistWardrobePanel(
          playerTrainer = playerTrainer,
          playerCustomAvatarBitmap = playerCustomAvatarBitmap,
          useCustomPlayerAvatarBitmap = useCustomPlayerAvatarBitmap,
          onSelectProtagonistPreset = onSelectProtagonistPreset,
          onUpdateProtagonistIdentity = onUpdateProtagonistIdentity,
          onUpdateProtagonistAppearance = onUpdateProtagonistAppearance,
          onEditHeroInSpriteEditor = onEditHeroInSpriteEditor,
          onToggleUseCustomPlayerAvatar = onToggleUseCustomPlayerAvatar,
          onSaveHeroToRoom = { onSaveHeroToRoom(playerTrainer) },
          compact = true,
        )
        GameTestHudPanel.SUMMONER -> GameTestTrainersAndDexPanel(
          activeChapter = activeChapter,
          activePartyDexIds = gameState.activePartyDexIds,
          hydratedByDex = hydratedByDex,
          onChallengeTrainer = {
            activeHudPanel = null
            onChallengeTrainer(it)
          },
          onSummonWildPokemon = {
            activeHudPanel = null
            onSummonWildPokemon(it)
          },
          onAddPokemonToParty = onAddPokemonToParty,
        )
        GameTestHudPanel.LOG -> Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
            .testTag("game_test_combat_log_panel"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "PRISM RESONANCE COMBAT & EXPLORATION LOG",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            gameState.battleLog.take(8).forEachIndexed { idx, line ->
              Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = if (idx == 0) StudioTextPrimary else StudioTextSecondary,
              )
            }
          }
        }
        null -> Unit
      }
    }

    // 3. CONTEXTUAL NPC DIALOGUE / WARP GATE BANNERS (Appear immediately above Playmat when triggered)
    gameState.activeDialoguePoi?.let { poi ->
      val npcTrainer = remember(poi.id, activeChapter.id) {
        TrainerCatalogAndRigEngine.resolveTrainerForPoi(poi, activeChapter.id)
      }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, SuperFxGold, RoundedCornerShape(14.dp))
          .testTag("npc_dialogue_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
        ) {
          TrainerPortraitBadge(
            trainer = npcTrainer,
            size = 70.dp,
            borderColor = SuperFxGold,
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.ChatBubble, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(15.dp))
              Text(
                text = "${npcTrainer.name} • ${poi.roleBadge}",
                style = MaterialTheme.typography.labelLarge,
                color = SuperFxGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "${npcTrainer.title} • ${npcTrainer.sourceInspiration} • Team: ${npcTrainer.teamDexIds.size} Partners",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "\"${poi.dialogue}\"",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (poi.battleDexSeed != null || poi.interactionKind == PoiInteractionKind.RIVAL_OR_CHALLENGER) {
                Button(
                  onClick = { onTriggerPoiBattle(poi) },
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson, contentColor = Color.White),
                  modifier = Modifier.testTag("poi_challenge_battle_button"),
                ) {
                  Text("Challenge (${npcTrainer.teamDexIds.size}v6)", fontWeight = FontWeight.Bold)
                }
              }
              OutlinedButton(
                onClick = onDismissDialogue,
                modifier = Modifier.testTag("dismiss_npc_dialogue_button"),
              ) {
                Text("Continue", color = CyberCyan)
              }
            }
          }
        }
      }
    }

    if (gameState.showWarpGatePicker) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, VoxelViolet, RoundedCornerShape(14.dp))
          .testTag("warp_gate_picker_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.TravelExplore, contentDescription = null, tint = VoxelViolet, modifier = Modifier.size(18.dp))
              Text(
                text = "REGION WARP GATE — FAST TRAVEL",
                style = MaterialTheme.typography.labelLarge,
                color = VoxelViolet,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(onClick = onDismissWarpPicker, modifier = Modifier.testTag("dismiss_warp_gate_button")) {
              Text("Stay")
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val connected = activeChapter.connectedRegionIds.mapNotNull { id ->
              MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == id }
            }
            connected.forEach { target ->
              Button(
                onClick = { onWarpToRegion(target.id) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
                modifier = Modifier.testTag("warp_gate_target_${target.id}"),
              ) {
                Text("Warp -> ${target.regionName.substringBefore(" —")}", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 4. MAIN PLAYABLE STAGE (Playmat-First: Overworld OR Shared Multi-Hardware Battle Arena)
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          translationX = feelFrame.shakeOffsetX
          translationY = feelFrame.shakeOffsetY
        },
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .omniDisplayPostEffect(
            visuals = visuals,
            mosaicFactor = feelFrame.mosaicPixelFactor,
          ),
      ) {
        if (!gameState.inBattle) {
          // 9x9 LOCKED CAMERA OVERWORLD EXPLORATION OVER 16x16 MAP
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardDark),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              // Sub-Zone Banner + Wild / Boss Encounter Quick Buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${activeSubZone?.name ?: "16×16 WORLD"} (${gameState.playerCol},${gameState.playerRow}) • $currentTileLabel",
                    fontFamily = PressStart2PFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = nearbyPoi?.let { "Nearby: ${it.name} — Press A to talk!" }
                      ?: "${activeChapter.keyLoreMilestone} • ${gameState.timeOfDay.label} • Steps: ${gameState.stepsTaken}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nearbyPoi != null) SuperFxGold else StudioTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { onTriggerEncounter(false) },
                    modifier = Modifier.testTag("game_test_wild_battle_button"),
                  ) {
                    Text("Wild", color = SgbEmerald)
                  }
                  Button(
                    onClick = { onTriggerEncounter(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson, contentColor = Color.White),
                    modifier = Modifier.testTag("game_test_boss_battle_button"),
                  ) {
                    Text("Boss", fontWeight = FontWeight.Bold)
                  }
                }
              }

              // Quick Overworld Companion & Hero Strip
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FilterChip(
                  selected = gameState.showTrailingPartner,
                  onClick = onToggleTrailingPartner,
                  label = {
                    Text(
                      text = if (gameState.showTrailingPartner) "Partner: ON" else "Partner: OFF",
                      style = MaterialTheme.typography.labelSmall,
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
                    selectedLabelColor = SgbEmerald,
                  ),
                  modifier = Modifier.testTag("toggle_trailing_partner_chip"),
                )
                FilterChip(
                  selected = activeHudPanel == GameTestHudPanel.WARDROBE,
                  onClick = { toggleHudPanel(GameTestHudPanel.WARDROBE) },
                  label = {
                    Text(
                      text = "Hero: ${playerTrainer.name} (Wardrobe)",
                      style = MaterialTheme.typography.labelSmall,
                      color = SuperFxGold,
                    )
                  },
                  modifier = Modifier.testTag("open_hero_customizer_chip"),
                )
                FilterChip(
                  selected = false,
                  onClick = onOpenTrainerStudio,
                  label = {
                    Text(
                      text = "3D Studio",
                      style = MaterialTheme.typography.labelSmall,
                      color = CyberCyan,
                    )
                  },
                  modifier = Modifier.testTag("open_trainer_3d_studio_chip"),
                )
              }

              AutoTileMapCanvas(
                tiles = mapGrid,
                gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE,
                viewportSize = if (gameState.showFullRegionView) null else 9,
                biomeStyle = activeChapter.biomeStyle,
                landmarkSilhouette = activeChapter.landmarkSilhouette,
                timeOfDay = gameState.timeOfDay,
                subZones = activeChapter.subZones,
                showSubZoneLabels = gameState.showSubZoneLabels,
                pointsOfInterest = activeChapter.pointsOfInterest,
                visuals = visuals,
                waterCausticsEnabled = visuals.waterCausticsEnabled,
                weatherParticlesEnabled = visuals.weatherParticlesEnabled,
                volumetricRaysEnabled = visuals.volumetricRaysEnabled,
                weather = gameState.weather,
                regionAccentColor = regionAccent,
                showGridOverlay = false,
                showMinimap = !gameState.showFullRegionView,
                regionId = activeChapter.id,
                playerRow = gameState.playerRow,
                playerCol = gameState.playerCol,
                playerFacing = gameState.playerFacing,
                playerWalkStep = gameState.playerWalkStep,
                playerTrainerSpec = playerTrainer,
                showTrailingPartner = gameState.showTrailingPartner,
                trailingPartnerRow = gameState.prevPlayerRow,
                trailingPartnerCol = gameState.prevPlayerCol,
                playerDexNumber = leadEntity.dexNumber,
                playerCustomBitmap = if (useCustomPlayerAvatarBitmap) playerCustomAvatarBitmap else null,
                trailingPartnerCustomBitmap = if (useCustomSprite) customSpriteBitmap else null,
                testTagPrefix = "game_test_overworld_canvas",
              )

              // Corner-Corrected D-Pad + "A — Interact" Action Button
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Button(
                    onClick = {
                      if (feelFrame.hitstopRemainingMs > 0f) {
                        feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, MoveDirection.UP)
                      } else {
                        onMoveOverworld(-1, 0)
                      }
                    },
                    modifier = Modifier
                      .size(54.dp)
                      .testTag("dpad_up"),
                  ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                      onClick = {
                        if (feelFrame.hitstopRemainingMs > 0f) {
                          feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, MoveDirection.LEFT)
                        } else {
                          onMoveOverworld(0, -1)
                        }
                      },
                      modifier = Modifier
                        .size(54.dp)
                        .testTag("dpad_left"),
                    ) {
                      Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Move Left")
                    }
                    Button(
                      onClick = {
                        if (feelFrame.hitstopRemainingMs > 0f) {
                          feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, MoveDirection.DOWN)
                        } else {
                          onMoveOverworld(1, 0)
                        }
                      },
                      modifier = Modifier
                        .size(54.dp)
                        .testTag("dpad_down"),
                    ) {
                      Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                    }
                    Button(
                      onClick = {
                        if (feelFrame.hitstopRemainingMs > 0f) {
                          feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, MoveDirection.RIGHT)
                        } else {
                          onMoveOverworld(0, 1)
                        }
                      },
                      modifier = Modifier
                        .size(54.dp)
                        .testTag("dpad_right"),
                    ) {
                      Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Move Right")
                    }
                  }
                }

                // A — Interact Button
                Button(
                  onClick = onInteractOverworld,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (nearbyPoi != null) SuperFxGold else CyberCyan,
                    contentColor = StudioDeepNavy,
                  ),
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .height(64.dp)
                    .testTag("dpad_interact_a"),
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A", fontFamily = PressStart2PFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (nearbyPoi != null) "Talk" else "Interact", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        } else {
          // LIVE BATTLE ARENA MODE WITH SHARED MULTI-HARDWARE STAGE BACKDROP
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.5.dp, regionAccent.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardDark),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              val leadAccent = parseHexColorSafe(leadEntity.primaryHex, CyberCyan)
              val enemyAccent = parseHexColorSafe(enemyEntity.primaryHex, ElectricCrimson)

              if (gameState.showTrainerVsBanner && gameState.activeEnemyTrainer != null) {
                TrainerVsBannerOverlay(
                  playerTrainer = playerTrainer,
                  enemyTrainer = gameState.activeEnemyTrainer,
                  enemyPartySize = gameState.enemyPartyDexIds.size.coerceAtLeast(1),
                  onDismiss = onDismissVsBanner,
                )
              }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(270.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(Color(0xFF070B14), Color(0xFF101D38), Color(0xFF060A12))
                    )
                  )
                  .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp)),
              ) {
                MultiHardwareBattleStageBackdrop(
                  primaryColor = leadAccent,
                  secondaryColor = enemyAccent,
                  visuals = visuals,
                  audio = hardwareState.convergedAudio,
                  activePlatformIds = hardwareState.activePlatformIds,
                  timeOfDay = gameState.timeOfDay,
                )

                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                ) {
                  val enemyStageBadge = if (gameState.enemyAtkStage > 0) "ATK+${gameState.enemyAtkStage}" else null
                  val enemyRoleBadge = when {
                    gameState.activeEnemyTrainer != null -> {
                      val gimmickTag = gameState.activeTrainerGimmick.takeIf { it != BattleGimmickMode.NONE }?.displayName?.substringBefore(" (")
                      if (gimmickTag != null) "${gameState.activeEnemyTrainer.name.uppercase()} • $gimmickTag"
                      else "${gameState.activeEnemyTrainer.name.uppercase()} • ${gameState.activeEnemyTrainer.roleCategory.badge}"
                    }
                    !gameState.activeTrainerClass.isNullOrBlank() -> {
                      val tactic = gameState.activeTrainerAiTactic?.label?.uppercase()
                      if (tactic != null) "${gameState.activeTrainerClass.uppercase()} • $tactic"
                      else gameState.activeTrainerClass.uppercase()
                    }
                    gameState.isBossEncounter -> "PRISM GUARDIAN"
                    else -> "WILD"
                  }
                  Column(
                    modifier = Modifier
                      .align(Alignment.TopStart)
                      .fillMaxWidth(0.58f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    PokemonBattleStatusHud(
                      name = enemyEntity.name,
                      level = gameState.enemyLevel,
                      primaryType = enemyEntity.primaryType,
                      secondaryType = enemyEntity.secondaryType,
                      currentHp = gameState.enemyCurrentHp,
                      maxHp = gameState.enemyMaxHp,
                      showNumericHp = false,
                      statusLabel = enemyStageBadge ?: gameState.enemyStatus.takeIf { it != StatusCondition.HEALTHY }?.label,
                      gimmickLabel = enemyRoleBadge,
                      modifier = Modifier.fillMaxWidth(),
                    )
                    if (gameState.enemyPartyDexIds.size > 1) {
                      EnemyTrainerPartyStrip(
                        trainerName = gameState.activeEnemyTrainer?.name ?: gameState.activeTrainerClass ?: "Trainer",
                        partyDexIds = gameState.enemyPartyDexIds,
                        activeIndex = gameState.enemyPartyIndex,
                        hpMap = gameState.enemyPartyCurrentHpMap,
                      )
                    }
                  }

                  gameState.activeEnemyTrainer?.let { enemyTrainerSpec ->
                    TrainerCharacterRenderer(
                      trainer = enemyTrainerSpec,
                      pose = gameState.enemyTrainerPose,
                      characterMixMode = hardwareState.characterMixMode,
                      visuals = visuals,
                      flipHorizontal = true,
                      modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(72.dp),
                    )
                  }

                  PokemonCharacterRenderer(
                    pokemon = enemyEntity,
                    style = PokemonGraphicsStyle.HYBRID_FUSION,
                    pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                    extrusionDepth = visuals.voxelDepth,
                    size = 118.dp,
                    mixMode = hardwareState.characterMixMode,
                    visuals = visuals,
                    hitFlash = feelFrame.hitFlashFramesRemaining > 0 && feelFrame.flashTargetIsEnemy,
                    extraScaleX = feelFrame.enemySquashX,
                    extraScaleY = feelFrame.enemySquashY,
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .offset(x = if (gameState.activeEnemyTrainer != null) (-44).dp else 0.dp, y = 6.dp)
                      .graphicsLayer { translationX = feelFrame.enemyRecoilOffsetPx },
                  )

                  // Playable Hero Trainer Rig (supports isolated custom pixel hero avatar!)
                  TrainerCharacterRenderer(
                    trainer = playerTrainer,
                    pose = if (gameState.turnNumber <= 1) TrainerPose.CAPSULE_THROW else TrainerPose.BATTLE_IDLE,
                    characterMixMode = hardwareState.characterMixMode,
                    visuals = visuals,
                    customAvatarBitmap = if (useCustomPlayerAvatarBitmap) playerCustomAvatarBitmap else null,
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .offset(x = (-6).dp, y = 2.dp)
                      .size(68.dp),
                  )

                  PokemonCharacterRenderer(
                    pokemon = leadEntity,
                    style = PokemonGraphicsStyle.HYBRID_FUSION,
                    pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                    extrusionDepth = visuals.voxelDepth,
                    size = 122.dp,
                    mirrored = true,
                    mixMode = hardwareState.characterMixMode,
                    visuals = visuals,
                    customSpriteBitmap = if (useCustomSprite) customSpriteBitmap else null,
                    hitFlash = feelFrame.hitFlashFramesRemaining > 0 && !feelFrame.flashTargetIsEnemy,
                    extraScaleX = feelFrame.playerSquashX,
                    extraScaleY = feelFrame.playerSquashY,
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .offset(x = 42.dp)
                      .graphicsLayer { translationX = feelFrame.playerLungeOffsetPx },
                  )

                  val stageNote = buildString {
                    if (gameState.playerAtkStage > 0) append("ATK+${gameState.playerAtkStage} ")
                    if (leadHeldItem.isNotBlank()) append(leadHeldItem.replace("_", " ").uppercase())
                  }.trim().ifBlank { "BOND ${gameState.bondGauge}%" }
                  PokemonBattleStatusHud(
                    name = leadEntity.name,
                    level = leadLevel,
                    primaryType = leadEntity.primaryType,
                    secondaryType = leadEntity.secondaryType,
                    currentHp = playerCurrentHp,
                    maxHp = playerMaxHp,
                    showNumericHp = true,
                    statusLabel = stageNote,
                    gimmickLabel = gameState.activeGimmick.takeIf { it != BattleGimmickMode.NONE }?.displayName,
                    modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .fillMaxWidth(0.56f),
                  )
                }
              }

              // Gimmick Catalyst Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                listOf(
                  BattleGimmickMode.MEGA_EVOLUTION,
                  BattleGimmickMode.Z_MOVE,
                  BattleGimmickMode.DYNAMAX,
                  BattleGimmickMode.TERASTALLIZATION,
                  BattleGimmickMode.ANIME_BATTLE_BOND,
                  BattleGimmickMode.ANIME_COUNTER_SHIELD,
                ).forEach { gimmick ->
                  val active = gameState.activeGimmick == gimmick
                  FilterChip(
                    selected = active,
                    onClick = { onToggleGimmick(gimmick) },
                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text(gimmick.displayName.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = SuperFxGold.copy(alpha = 0.25f),
                      selectedLabelColor = SuperFxGold,
                    ),
                    modifier = Modifier.testTag("gimmick_chip_${gimmick.name.lowercase()}"),
                  )
                }
              }

              if (gameState.activeGimmick == BattleGimmickMode.TERASTALLIZATION) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  listOf(
                    PokemonType.ELECTRIC, PokemonType.FIRE, PokemonType.WATER,
                    PokemonType.DRAGON, PokemonType.FAIRY, PokemonType.STEEL, PokemonType.GHOST,
                  ).forEach { tType ->
                    FilterChip(
                      selected = gameState.teraType == tType,
                      onClick = { onSelectTeraType(tType) },
                      label = { Text("Tera ${tType.displayName}", style = MaterialTheme.typography.labelSmall) },
                      modifier = Modifier.testTag("tera_type_${tType.name.lowercase()}"),
                    )
                  }
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                WeatherCondition.entries.forEach { w ->
                  FilterChip(
                    selected = gameState.weather == w,
                    onClick = { onSelectWeather(w) },
                    label = { Text(w.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("weather_chip_${w.name.lowercase()}"),
                  )
                }
                TerrainCondition.entries.forEach { t ->
                  FilterChip(
                    selected = gameState.terrain == t,
                    onClick = { onSelectTerrain(t) },
                    label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("terrain_chip_${t.name.lowercase()}"),
                  )
                }
              }

              // 2x2 Canonical Move Command Grid
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moveset.take(2).forEach { move ->
                  Button(
                    onClick = { onExecuteMove(move) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("battle_move_${move.name.lowercase().replace(" ", "_")}"),
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(move.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text("${move.type.displayName} • BP ${move.basePower}", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moveset.drop(2).take(2).forEach { move ->
                  Button(
                    onClick = { onExecuteMove(move) },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxelViolet, contentColor = Color.White),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("battle_move_${move.name.lowercase().replace(" ", "_")}"),
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(move.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text("${move.type.displayName} • BP ${move.basePower}", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickBall = studioItems.firstOrNull { it.category == StudioItemCatalog.CAT_POKE_BALL && it.quantity > 0 }
                OutlinedButton(
                  onClick = {
                    if (quickBall != null) onUseBagItem(quickBall) else onToggleBag(true)
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("battle_quick_throw_ball"),
                ) {
                  Text(
                    text = if (quickBall != null) "Throw ${quickBall.name} (×${quickBall.quantity})" else "Open Bag",
                    color = SuperFxGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                OutlinedButton(
                  onClick = onFleeBattle,
                  modifier = Modifier.testTag("battle_flee_button"),
                ) {
                  Text("Run", color = ElectricCrimson)
                }
              }
            }
          }
        }
      }
    }

    // 5. LIVE 1-LINE TICKER OF LATEST EVENT (Tap Log chip above for full history)
    gameState.battleLog.firstOrNull()?.let { latestLine ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(StudioSurfaceDark)
          .border(1.dp, StudioBorderDark, RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text("LOG", style = MaterialTheme.typography.labelSmall, color = CyberCyan, fontWeight = FontWeight.Bold)
        Text(
          text = latestLine,
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // 6. ACTIVE 6-SLOT PARTY STRIP (Immediately below playmat for fast switching)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp)),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "ACTIVE PARTY (${gameState.activePartyDexIds.size}/6) • BOND ${gameState.bondGauge}% — TAP TO SWITCH LEAD",
          style = MaterialTheme.typography.labelSmall,
          color = Mode7Sky,
          fontWeight = FontWeight.Bold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          gameState.activePartyDexIds.forEachIndexed { idx, dex ->
            val pkmn = resolvePokemonByDex(dex)
            val isLead = idx == gameState.activePartyIndex
            val lvl = gameState.levelFor(dex)
            val isFainted = (gameState.partyCurrentHpMap[dex] ?: 1) <= 0
            val held = gameState.heldItemsMap[dex].orEmpty()
            FilterChip(
              selected = isLead,
              onClick = { onSwitchPartySlot(idx) },
              label = {
                Text(
                  text = (if (isFainted) "[KO] " else "") + "${pkmn.name} Lv.$lvl" +
                    if (held.isNotBlank()) " [${held.substringAfter("_").uppercase()}]" else "",
                  style = MaterialTheme.typography.labelSmall,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
                selectedLabelColor = SgbEmerald,
              ),
              modifier = Modifier.testTag("party_slot_$idx"),
            )
          }
        }
      }
    }
  }
}

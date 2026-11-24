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
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.engine.CampaignProgressionState
import com.example.engine.GameFeelFrameState
import com.example.engine.HardwareRuntimePreset
import com.example.engine.ImpactWeight
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.MoveDirection
import com.example.engine.PoiInteractionKind
import com.example.engine.PokemonType
import com.example.engine.RegionPointOfInterest
import com.example.engine.StatStages
import com.example.engine.StatusCondition
import com.example.engine.StudioSfxType
import com.example.engine.TerrainCondition
import com.example.engine.TimeOfDayCycle
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

/**
 * Full-Featured Playable Game Test Sandbox for Pokémon: Prism Resonance v4.0.
 * - Live Hardware Runtime Mode Dropdown (All 156 Hardware Ultimate Target Style, 10 Curated Combos + Custom Combo Builder, Category/Era Suites, All 156 Individual Purist, and 4s Hardware Tour)
 * - 14 unique custom 16x16 regions with 9x9 locked scrolling camera, live corner minimap, Sub-Zone banner, NPCs/POIs, Healing Plazas, and Region Warp Gates
 * - Shared MultiHardwareBattleStageBackdrop matching OmniConsoleFusionViewport live shaders & VFX
 * - Complete Battle Engine (priority/speed turn order, Counter-Shield chip, Weakness Policy stages, auto-switch on faint, TMs, Evolution Stones, Rare Candies, Revives, Berries)
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
  onDismissVsBanner: () -> Unit = {},
  onToggleTrailingPartner: () -> Unit = {},
  onOpenTrainerStudio: () -> Unit = {},
  onToggleAmbientBed: () -> Unit = {},
  onPlaySfx: (StudioSfxType) -> Unit = {},
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

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // 1. Live Hardware Runtime Mode Dropdown + 4-second Hardware Tour
    GameTestHardwareRuntimeCard(
      hardwareState = hardwareState,
      onSelectPreset = onSelectHardwarePreset,
      onApplyCustomCombo = onApplyCustomHardwareCombo,
      onToggleHardwareTour = onToggleHardwareTour,
    )

    // 2. Live Pure-Kotlin Omni-Hardware Audio Console Bar
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(18.dp))
            Column {
              Text(
                text = "OMNI-HARDWARE 44.1kHz DSP AUDIO • ${hardwareState.convergedAudio.audioSignatureLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = SuperFxGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "BGM (64-Step): ${audioState.activeBgmTheme.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = buildString {
                  append(audioState.activeBgmTheme.palette.label)
                  append(" • ${audioState.activeBgmTheme.bpm.toInt()} BPM")
                  if (audioState.bossClimaxActive) append(" • CLIMAX +10% OCTAVE")
                  if (audioState.lowHpUrgencyActive) append(" • LOW-HP PULSE")
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                  audioState.lowHpUrgencyActive -> ElectricCrimson
                  audioState.bossClimaxActive -> SuperFxGold
                  else -> CyberCyan
                },
                fontWeight = if (audioState.lowHpUrgencyActive || audioState.bossClimaxActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onToggleBgm,
              modifier = Modifier.testTag("game_test_bgm_play_toggle"),
            ) {
              Icon(
                imageVector = if (audioState.isBgmPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (audioState.isBgmPlaying) "Stop BGM" else "Play BGM",
                tint = SgbEmerald,
              )
            }
            IconButton(
              onClick = onToggleMute,
              modifier = Modifier.testTag("game_test_audio_mute_toggle"),
            ) {
              Icon(
                imageVector = if (audioState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (audioState.isMuted) "Unmute Audio" else "Mute Audio",
                tint = if (audioState.isMuted) ElectricCrimson else CyberCyan,
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
          BgmThemeId.entries.forEach { theme ->
            val selected = audioState.activeBgmTheme == theme
            FilterChip(
              selected = selected,
              onClick = { onSelectBgmTheme(theme) },
              label = { Text(theme.displayName, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
                selectedLabelColor = SuperFxGold,
              ),
              modifier = Modifier.testTag("bgm_theme_${theme.id}"),
            )
          }
        }

        // Ambient Bed Toggle + Clean-Room SFX Audition Row ("!" Alert, Warp Swoosh, Evolution Chime)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val regionBedLabel = activeChapter.resolveAmbientBed().label
          FilterChip(
            selected = audioState.ambientBedEnabled,
            onClick = onToggleAmbientBed,
            label = {
              Text(
                text = if (audioState.ambientBedEnabled) "Ambient: $regionBedLabel" else "Ambient: Off",
                style = MaterialTheme.typography.labelSmall,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
              selectedLabelColor = SgbEmerald,
            ),
            modifier = Modifier.testTag("game_test_ambient_bed_toggle"),
          )
          FilterChip(
            selected = false,
            onClick = { onPlaySfx(StudioSfxType.TRAINER_EYE_CONTACT) },
            label = { Text("SFX: '!' Alert", style = MaterialTheme.typography.labelSmall, color = ElectricCrimson) },
            modifier = Modifier.testTag("sfx_audition_trainer_alert"),
          )
          FilterChip(
            selected = false,
            onClick = { onPlaySfx(StudioSfxType.DOOR_WARP_SWOOSH) },
            label = { Text("SFX: Warp", style = MaterialTheme.typography.labelSmall, color = VoxelViolet) },
            modifier = Modifier.testTag("sfx_audition_warp_swoosh"),
          )
          FilterChip(
            selected = false,
            onClick = { onPlaySfx(StudioSfxType.EVOLUTION_CHIME) },
            label = { Text("SFX: Evolve", style = MaterialTheme.typography.labelSmall, color = SuperFxGold) },
            modifier = Modifier.testTag("sfx_audition_evolution_chime"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Vol ${(audioState.masterVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
          Slider(
            value = audioState.masterVolume,
            onValueChange = onSetVolume,
            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
            modifier = Modifier
              .weight(1f)
              .testTag("game_test_volume_slider"),
          )
          OutlinedButton(
            onClick = { onPlayLeadCry(leadEntity) },
            modifier = Modifier.testTag("game_test_cry_button"),
          ) {
            Text("Cry #${leadEntity.dexNumber}", color = CyberCyan)
          }
        }
      }
    }

    // 3. 22-Region Fusion World Switcher, Atmospheric Lighting & View Controls
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "POKÉMON: PRISM RESONANCE • ${campaignState.harmonicShardsCollected}/${MasterCanonTemplateCatalog.worldChapters.size} HARMONIC SHARDS",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${activeChapter.regionName} • ${activeChapter.biomeStyle.label}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${activeChapter.landmarkSilhouette.label} • ${activeChapter.fusionSourcesLineage}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onToggleBag(null) },
              modifier = Modifier.testTag("game_test_open_bag_button"),
            ) {
              Icon(Icons.Default.Backpack, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuperFxGold)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Bag", color = SuperFxGold)
            }
            OutlinedButton(
              onClick = onHealParty,
              modifier = Modifier.testTag("game_test_heal_party_button"),
            ) {
              Icon(Icons.Default.Healing, contentDescription = "Heal Full Party", modifier = Modifier.size(16.dp), tint = SgbEmerald)
            }
          }
        }

        // Day / Dusk / Night Lighting Cycle + Full 16x16 / Sub-Zones / 22-Region World Atlas Toggles
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TimeOfDayCycle.entries.forEach { cycle ->
            FilterChip(
              selected = gameState.timeOfDay == cycle,
              onClick = { onSelectTimeOfDay(cycle) },
              label = { Text(cycle.label, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
                selectedLabelColor = SuperFxGold,
              ),
              modifier = Modifier.testTag("time_of_day_${cycle.id}"),
            )
          }
          FilterChip(
            selected = gameState.showFullRegionView,
            onClick = onToggleFullRegionView,
            label = { Text(if (gameState.showFullRegionView) "Full 16×16 Region" else "9×9 Zoom Cam", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
              selectedLabelColor = SgbEmerald,
            ),
            modifier = Modifier.testTag("toggle_full_region_view"),
          )
          FilterChip(
            selected = gameState.showSubZoneLabels,
            onClick = onToggleSubZoneLabels,
            label = { Text("Sub-Zones", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("toggle_sub_zone_labels"),
          )
          FilterChip(
            selected = gameState.showWorldAtlas,
            onClick = onToggleWorldAtlas,
            label = { Text("22-Region Atlas", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = VoxelViolet.copy(alpha = 0.26f),
              selectedLabelColor = VoxelViolet,
            ),
            modifier = Modifier.testTag("toggle_world_atlas_button"),
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
            val selected = ch.id == activeChapter.id
            val cleared = ch.id in campaignState.completedChapterIds
            FilterChip(
              selected = selected,
              onClick = { onSelectRegion(ch.id) },
              label = {
                Text(
                  text = (if (cleared) "★ " else "") + ch.regionName.substringAfter(". ").substringBefore(" —"),
                  style = MaterialTheme.typography.labelSmall,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("playtest_region_${ch.id}"),
            )
          }
        }
      }
    }

    // 3b. Optional Full 22-Region Drawn World Atlas (shows ALL 22 drawn 16x16 regions + warp network)
    if (gameState.showWorldAtlas) {
      FullRegionsWorldAtlasCanvas(
        activeRegionId = activeChapter.id,
        completedRegionIds = campaignState.completedChapterIds,
        timeOfDay = gameState.timeOfDay,
        onSelectRegion = { targetId ->
          onSelectRegion(targetId)
        },
      )
    }

    // 4. Interactive Bag Drawer Panel (All categories functional: Balls, Medicine, Hold, Berries, TMs, Evolution, Catalysts, Key Items)
    if (gameState.isBagOpen) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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
            Text(
              text = "TRAINER BAG • TARGET: ${leadEntity.name} (Lv.$leadLevel)",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
            )
            OutlinedButton(onClick = { onToggleBag(false) }, modifier = Modifier.testTag("game_test_close_bag_button")) {
              Text("Close Bag", color = StudioTextPrimary)
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StudioItemCatalog.allCategories.forEach { (catId, catLabel) ->
              FilterChip(
                selected = gameState.selectedBagCategory == catId,
                onClick = { onSelectBagCategory(catId) },
                label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("bag_category_$catId"),
              )
            }
          }

          val catItems = studioItems.filter { it.category == gameState.selectedBagCategory }
          catItems.forEach { item ->
            val accent = parseHexColorSafe(item.accentHex, CyberCyan)
            val reusable = item.category in setOf(
              StudioItemCatalog.CAT_HOLD_ITEM,
              StudioItemCatalog.CAT_GIMMICK_CATALYST,
              StudioItemCatalog.CAT_TM,
              StudioItemCatalog.CAT_KEY_ITEM,
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioCardDark)
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (reusable) item.name else "${item.name}  ×${item.quantity}",
                  style = MaterialTheme.typography.labelLarge,
                  color = StudioTextPrimary,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = StudioTextSecondary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { onUseBagItem(item) },
                enabled = item.quantity > 0 || reusable,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = StudioDeepNavy),
                modifier = Modifier.testTag("bag_use_${item.id}"),
              ) {
                Text(
                  text = when (item.category) {
                    StudioItemCatalog.CAT_POKE_BALL -> "Throw"
                    StudioItemCatalog.CAT_HOLD_ITEM -> "Equip"
                    StudioItemCatalog.CAT_GIMMICK_CATALYST -> "Trigger"
                    StudioItemCatalog.CAT_TM -> "Teach"
                    StudioItemCatalog.CAT_EVOLUTION -> "Evolve"
                    else -> "Use"
                  },
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    // 5. Interactive NPC / POI Dialogue Card with High-Contrast 40x40 Character Portrait
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

    // 6. Region Warp Gate Picker Card
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

    // 7. Main Playable Stage (9x9 Scrolling Overworld OR Shared Multi-Hardware Battle Arena)
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
              // Sub-Zone Banner
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
                      text = if (gameState.showTrailingPartner) "Trailing Partner: ON" else "Trailing Partner: OFF",
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
                  selected = false,
                  onClick = onOpenTrainerStudio,
                  label = {
                    Text(
                      text = "Hero: ${playerTrainer.name} (Customize)",
                      style = MaterialTheme.typography.labelSmall,
                      color = SuperFxGold,
                    )
                  },
                  modifier = Modifier.testTag("open_hero_customizer_chip"),
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
                playerCustomBitmap = if (useCustomSprite) customSpriteBitmap else null,
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

              // Dramatic Trainer VS Intro Banner on Trainer/Rival/Boss Battles
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
                // Shared Live Multi-Hardware Backdrop (responds to Hardware Runtime Dropdown + Day/Dusk/Night!)
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
                  // Enemy HUD + Multi-Creature Trainer Party Orb Strip (Top Left)
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

                  // Enemy Trainer 2D/3D Rig standing beside their active creature during Trainer/Boss battles
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

                  // Enemy Creature Renderer (Top Right, offset slightly inward when Enemy Trainer stands beside them)
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

                  // Playable Hero Trainer Rig standing behind the player's active partner
                  TrainerCharacterRenderer(
                    trainer = playerTrainer,
                    pose = if (gameState.turnNumber <= 1) TrainerPose.CAPSULE_THROW else TrainerPose.BATTLE_IDLE,
                    characterMixMode = hardwareState.characterMixMode,
                    visuals = visuals,
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .offset(x = (-6).dp, y = 2.dp)
                      .size(68.dp),
                  )

                  // Player Creature Renderer (Bottom Left)
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

                  // Player HUD (Bottom Right) showing live level, held item / stat stage boosts, & Bond Gauge
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

              // 2x2 Canonical Move Command Grid (Slot 4 updates dynamically when teaching a TM!)
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

    // 8. Active 6-Slot Party Bar (with Levels & KO status)
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

    // 8b. Interactive 57-Class Trainer Roster & All 1,025 Pokémon Summoner
    GameTestTrainersAndDexPanel(
      activeChapter = activeChapter,
      activePartyDexIds = gameState.activePartyDexIds,
      onChallengeTrainer = onChallengeTrainer,
      onSummonWildPokemon = onSummonWildPokemon,
      onAddPokemonToParty = onAddPokemonToParty,
    )

    // 9. Live Battle & Adventure Log
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp)),
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
        gameState.battleLog.take(6).forEachIndexed { idx, line ->
          Text(
            text = "• $line",
            style = MaterialTheme.typography.bodySmall,
            color = if (idx == 0) StudioTextPrimary else StudioTextSecondary,
          )
        }
      }
    }
  }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.local.StudioItemEntity
import com.example.data.model.ConsoleFusionConfig
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BgmThemeId
import com.example.engine.CampaignProgressionState
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.PokemonType
import com.example.engine.TerrainCondition
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

/**
 * Full-Featured Playable Game Test Sandbox for Pokémon: Prism Resonance.
 * - 100% hoisted state (never resets when switching tabs)
 * - All 14 playable regions with seamless AutoTileMapCanvas + corner-corrected D-Pad + 130ms input buffer
 * - Universal Game Feel 60fps withFrameMillis loop: non-stacking hitstop, linear screenshake,
 *   2-frame hit-flash, lunge/recoil, squash-stretch, haptic bursts, and GBA mosaic dissolve transitions
 * - Live Bag system (13 Poké Balls, Medicine, Hold Items, Berries, Catalysts)
 * - Live Pure-Kotlin Audio Console bar (12 looping BGM themes, mute, volume, Pokémon cries)
 * - Bug-fixed proportional Dynamax HP scaling + Terastallization + Weather/Terrain battle engine
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
) {
  val haptics = LocalHapticFeedback.current
  val visuals = hardwareState.convergedVisuals
  val activeChapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gameState.activeRegionId }
    ?: MasterCanonTemplateCatalog.worldChapters.first()
  val regionAccent = parseHexColorSafe(activeChapter.regionAccentHex, CyberCyan)

  val leadEntity = resolvePokemonByDex(gameState.activeLeadDexId)
  val enemyEntity = resolvePokemonByDex(gameState.enemyDexId)
  val leadHeldItem = gameState.heldItemsMap[leadEntity.dexNumber].orEmpty()
  val leadSnap = remember(leadEntity, gameState.activeGimmick, gameState.teraType, leadHeldItem) {
    StudioViewModelHelpers.buildCombatantFromEntity(
      entity = leadEntity,
      level = 50,
      gimmick = gameState.activeGimmick,
      teraType = gameState.teraType,
      heldItemId = leadHeldItem,
    )
  }
  val playerMaxHp = leadSnap.maxHp()
  val playerCurrentHp = (gameState.partyCurrentHpMap[leadEntity.dexNumber] ?: playerMaxHp).coerceIn(0, playerMaxHp)
  val moveset = remember(leadEntity, gameState.teraType) {
    StudioViewModelHelpers.buildMovesetForPokemon(leadEntity, gameState.teraType)
  }

  // 60 FPS Universal Game Feel Engine Controller (Hitstop, Shake, Flash, Lunge, Mosaic Dissolve)
  var feelFrame by remember { mutableStateOf(com.example.engine.GameFeelFrameState()) }

  LaunchedEffect(gameState.lastEventNonce) {
    if (gameState.lastEventNonce > 0L) {
      val weight = when (gameState.lastHitSeverity) {
        2 -> com.example.engine.ImpactWeight.HEAVY_OR_CRIT
        3 -> com.example.engine.ImpactWeight.RESISTED
        else -> com.example.engine.ImpactWeight.NORMAL
      }
      feelFrame = UniversalGameFeelEngine.triggerImpact(
        current = feelFrame,
        weight = weight,
        targetIsEnemy = gameState.lastAttackerWasPlayer,
      )
      haptics.performHapticFeedback(
        if (weight == com.example.engine.ImpactWeight.HEAVY_OR_CRIT) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
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
    // 1. Live Pure-Kotlin Omni-Hardware Audio Console Bar
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
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                text = "BGM: ${audioState.activeBgmTheme.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = StudioTextPrimary,
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

        // BGM Theme Picker (12 original themes) + Volume Slider
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

    // 2. 14-Region World Switcher & Harmonic Shard Progress
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
              text = "POKÉMON: PRISM RESONANCE • ${campaignState.harmonicShardsCollected}/14 HARMONIC SHARDS",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = activeChapter.regionName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
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
              Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(16.dp), tint = SgbEmerald)
            }
          }
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
                  text = (if (cleared) "★ " else "") + ch.regionName.substringBefore(" —"),
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

    // 3. Interactive Bag Drawer Panel (when toggled open)
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
              text = "TRAINER BAG • ROOM SQLITE INVENTORY",
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
                  text = "${item.name}  ×${item.quantity}",
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
                enabled = item.quantity > 0 || item.category == StudioItemCatalog.CAT_HOLD_ITEM || item.category == StudioItemCatalog.CAT_GIMMICK_CATALYST,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = StudioDeepNavy),
                modifier = Modifier.testTag("bag_use_${item.id}"),
              ) {
                Text(
                  text = when (item.category) {
                    StudioItemCatalog.CAT_POKE_BALL -> "Throw"
                    StudioItemCatalog.CAT_HOLD_ITEM -> "Equip"
                    StudioItemCatalog.CAT_GIMMICK_CATALYST -> "Trigger"
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

    // 4. Main Playable Stage (Overworld OR Battle Arena) with Universal Game Feel graphicsLayer & AGSL Mosaic Dissolve
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
          // OVERWORLD EXPLORATION MODE
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
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "OVERWORLD (${gameState.playerCol}, ${gameState.playerRow}) • Steps: ${gameState.stepsTaken}",
                  fontFamily = PressStart2PFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = CyberCyan,
                )
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
                    Text("Prism Boss", fontWeight = FontWeight.Bold)
                  }
                }
              }

              AutoTileMapCanvas(
                tiles = mapGrid,
                gridSize = 8,
                waterCausticsEnabled = visuals.waterCausticsEnabled,
                weatherParticlesEnabled = visuals.weatherParticlesEnabled,
                volumetricRaysEnabled = visuals.volumetricRaysEnabled,
                weather = gameState.weather,
                regionAccentColor = regionAccent,
                showGridOverlay = false,
                playerRow = gameState.playerRow,
                playerCol = gameState.playerCol,
                playerSpriteUrl = leadEntity.pixelSpriteUrl,
                playerCustomBitmap = if (useCustomSprite) customSpriteBitmap else null,
                testTagPrefix = "game_test_overworld_canvas",
              )

              // Corner-Corrected D-Pad with 130ms Input Buffering
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Button(
                  onClick = {
                    if (feelFrame.hitstopRemainingMs > 0f) {
                      feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, com.example.engine.MoveDirection.UP)
                    } else {
                      onMoveOverworld(-1, 0)
                    }
                  },
                  modifier = Modifier
                    .size(56.dp)
                    .testTag("dpad_up"),
                ) {
                  Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  Button(
                    onClick = {
                      if (feelFrame.hitstopRemainingMs > 0f) {
                        feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, com.example.engine.MoveDirection.LEFT)
                      } else {
                        onMoveOverworld(0, -1)
                      }
                    },
                    modifier = Modifier
                      .size(56.dp)
                      .testTag("dpad_left"),
                  ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Move Left")
                  }
                  Button(
                    onClick = {
                      if (feelFrame.hitstopRemainingMs > 0f) {
                        feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, com.example.engine.MoveDirection.DOWN)
                      } else {
                        onMoveOverworld(1, 0)
                      }
                    },
                    modifier = Modifier
                      .size(56.dp)
                      .testTag("dpad_down"),
                  ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                  }
                  Button(
                    onClick = {
                      if (feelFrame.hitstopRemainingMs > 0f) {
                        feelFrame = UniversalGameFeelEngine.bufferMovementInput(feelFrame, com.example.engine.MoveDirection.RIGHT)
                      } else {
                        onMoveOverworld(0, 1)
                      }
                    },
                    modifier = Modifier
                      .size(56.dp)
                      .testTag("dpad_right"),
                  ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Move Right")
                  }
                }
              }
            }
          }
        } else {
          // LIVE BATTLE ARENA MODE
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
              // Enemy HUD + Character Stage
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(250.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    Brush.verticalGradient(
                      listOf(Color(0xFF0A1228), Color(0xFF142240), Color(0xFF080D18))
                    )
                  )
                  .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
                  .padding(10.dp),
              ) {
                // Enemy HUD (Top Left)
                PokemonBattleStatusHud(
                  name = enemyEntity.name,
                  level = gameState.enemyLevel,
                  primaryType = enemyEntity.primaryType,
                  secondaryType = enemyEntity.secondaryType,
                  currentHp = gameState.enemyCurrentHp,
                  maxHp = gameState.enemyMaxHp,
                  showNumericHp = false,
                  statusLabel = gameState.enemyStatus.takeIf { it != com.example.engine.StatusCondition.HEALTHY }?.label,
                  gimmickLabel = if (gameState.isBossEncounter) "PRISM GUARDIAN" else "WILD",
                  modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.56f),
                )

                // Enemy Renderer (Top Right) with Universal Game Feel flash & recoil
                PokemonCharacterRenderer(
                  pokemon = enemyEntity,
                  style = PokemonGraphicsStyle.HYBRID_FUSION,
                  pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                  extrusionDepth = visuals.voxelDepth,
                  size = 122.dp,
                  mixMode = hardwareState.characterMixMode,
                  visuals = visuals,
                  hitFlash = feelFrame.hitFlashFramesRemaining > 0 && feelFrame.flashTargetIsEnemy,
                  extraScaleX = feelFrame.enemySquashX,
                  extraScaleY = feelFrame.enemySquashY,
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer { translationX = feelFrame.enemyRecoilOffsetPx },
                )

                // Player Renderer (Bottom Left) with lunge + custom sprite support
                PokemonCharacterRenderer(
                  pokemon = leadEntity,
                  style = PokemonGraphicsStyle.HYBRID_FUSION,
                  pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                  extrusionDepth = visuals.voxelDepth,
                  size = 128.dp,
                  mirrored = true,
                  mixMode = hardwareState.characterMixMode,
                  visuals = visuals,
                  customSpriteBitmap = if (useCustomSprite) customSpriteBitmap else null,
                  hitFlash = feelFrame.hitFlashFramesRemaining > 0 && !feelFrame.flashTargetIsEnemy,
                  extraScaleX = feelFrame.playerSquashX,
                  extraScaleY = feelFrame.playerSquashY,
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer { translationX = feelFrame.playerLungeOffsetPx },
                )

                // Player HUD (Bottom Right)
                PokemonBattleStatusHud(
                  name = leadEntity.name,
                  level = 50,
                  primaryType = leadEntity.primaryType,
                  secondaryType = leadEntity.secondaryType,
                  currentHp = playerCurrentHp,
                  maxHp = playerMaxHp,
                  showNumericHp = true,
                  statusLabel = leadHeldItem.takeIf { it.isNotBlank() }?.replace("_", " ")?.uppercase(),
                  gimmickLabel = gameState.activeGimmick.takeIf { it != BattleGimmickMode.NONE }?.displayName,
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.56f),
                )
              }

              // Gimmick Catalyst Row (Proportional Dynamax HP + Terastallization + Mega + Z-Move)
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

              // Tera Type Picker when Terastallized
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

              // Weather & Terrain Live Switches
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

              // Quick Throw Poké Ball & Flee Bar
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

    // 5. Active 6-Slot Party Bar
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
          text = "ACTIVE PARTY (${gameState.activePartyDexIds.size}/6) — TAP TO SWITCH LEAD",
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
            val held = gameState.heldItemsMap[dex].orEmpty()
            FilterChip(
              selected = isLead,
              onClick = { onSwitchPartySlot(idx) },
              label = {
                Text(
                  text = "#${pkmn.dexNumber} ${pkmn.name}" + if (held.isNotBlank()) " [${held.substringAfter("_").uppercase()}]" else "",
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

    // 6. Live Battle & Adventure Log
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

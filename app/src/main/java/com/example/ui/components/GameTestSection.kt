package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.NativePokemonEntity
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.FanGameProject
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTurnField
import com.example.engine.CanonBattleResolver
import com.example.engine.CombatantSnapshot
import com.example.engine.MoveCategory
import com.example.engine.NatureModifier
import com.example.engine.PokemonType
import com.example.engine.TerrainCondition
import com.example.engine.WeatherCondition
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.roundToInt

private enum class PlaytestMode(val label: String) {
  OVERWORLD("Overworld Test"),
  BATTLE("Battle Test"),
}

/**
 * Interactive Playtest / Game Test Runtime allowing developers to test-play their
 * active fan game project's overworld map, wild encounters, and turn-based battles
 * using the deterministic Canon engine and active Omni-Fusion visual shader pipeline.
 */
@Composable
fun GameTestSection(
  activeProject: FanGameProject?,
  primaryPokemon: NativePokemonEntity,
  secondaryPokemon: NativePokemonEntity,
  nativeDex: List<NativePokemonEntity>,
  fusionConfig: ConsoleFusionConfig,
  mapGrid: List<Int>,
  modifier: Modifier = Modifier,
) {
  var playtestMode by remember { mutableStateOf(PlaytestMode.OVERWORLD) }

  // Overworld playtest coordinates strictly clamped to 0..7
  var playerRow by remember { mutableIntStateOf(4) }
  var playerCol by remember { mutableIntStateOf(3) }
  var stepsTaken by remember { mutableIntStateOf(0) }
  var currentOpponent by remember(secondaryPokemon) { mutableStateOf(secondaryPokemon) }
  var activeGimmick by remember { mutableStateOf(BattleGimmickMode.ANIME_BATTLE_BOND) }
  var battleGraphicsStyle by remember { mutableStateOf(PokemonGraphicsStyle.HYBRID_FUSION) }

  val playerSnapshot = remember(primaryPokemon, activeGimmick) {
    toCombatantSnapshot(primaryPokemon, activeGimmick)
  }
  val opponentSnapshot = remember(currentOpponent) {
    toCombatantSnapshot(currentOpponent, BattleGimmickMode.NONE)
  }

  val playerMaxHp = remember(playerSnapshot) { playerSnapshot.maxHp() }
  val enemyMaxHp = remember(opponentSnapshot) { opponentSnapshot.maxHp() }

  var playerCurrentHp by remember(playerMaxHp) { mutableIntStateOf(playerMaxHp) }
  var enemyCurrentHp by remember(enemyMaxHp) { mutableIntStateOf(enemyMaxHp) }
  var turnCounter by remember(primaryPokemon.dexNumber, currentOpponent.dexNumber) { mutableIntStateOf(1) }

  val battleLog = remember(primaryPokemon.dexNumber, currentOpponent.dexNumber) {
    mutableStateListOf(
      "Playtest initialized: ${primaryPokemon.name} (Partner) ready in ${activeProject?.regionName ?: primaryPokemon.region}!",
      "Tap D-Pad to explore map tiles or switch to Battle Test.",
    )
  }

  val playerMoves = remember(primaryPokemon) { buildMovesForPokemon(primaryPokemon) }
  val enemyMoves = remember(currentOpponent) { buildMovesForPokemon(currentOpponent) }

  fun pushLog(entry: String) {
    battleLog.add(0, entry)
    if (battleLog.size > 6) {
      battleLog.removeAt(battleLog.lastIndex)
    }
  }

  fun resetBattle() {
    playerCurrentHp = playerMaxHp
    enemyCurrentHp = enemyMaxHp
    turnCounter = 1
    pushLog("Restored full HP for ${primaryPokemon.name} & ${currentOpponent.name}.")
  }

  fun triggerWildEncounter(tileType: Int) {
    val pool = nativeDex.ifEmpty { listOf(secondaryPokemon) }
    val target = pool[(stepsTaken + tileType) % pool.size]
    currentOpponent = if (target.dexNumber == primaryPokemon.dexNumber && pool.size > 1) {
      pool[(stepsTaken + tileType + 1) % pool.size]
    } else {
      target
    }
    playerCurrentHp = playerMaxHp
    enemyCurrentHp = toCombatantSnapshot(currentOpponent, BattleGimmickMode.NONE).maxHp()
    turnCounter = 1
    playtestMode = PlaytestMode.BATTLE
    val zoneName = if (tileType == 5) "Terastal Crystal Rift" else "Tall Grass"
    pushLog("Wild #${currentOpponent.dexNumber} ${currentOpponent.name} appeared in $zoneName!")
  }

  fun moveTrainer(dRow: Int, dCol: Int) {
    val nextRow = (playerRow + dRow).coerceIn(0, 7)
    val nextCol = (playerCol + dCol).coerceIn(0, 7)
    if (nextRow == playerRow && nextCol == playerCol) {
      pushLog("Reached region boundary ($nextRow, $nextCol).")
      return
    }
    playerRow = nextRow
    playerCol = nextCol
    stepsTaken += 1
    val tile = mapGrid.getOrElse(nextRow * 8 + nextCol) { 0 }
    if ((tile == 0 || tile == 5) && stepsTaken % 3 == 0) {
      triggerWildEncounter(tile)
    } else {
      val tileLabel = tileName(tile)
      pushLog("Stepped onto $tileLabel ($nextRow, $nextCol) • Step #$stepsTaken")
    }
  }

  fun executeBattleMove(move: BattleMoveSpec) {
    if (playerCurrentHp <= 0 || enemyCurrentHp <= 0) {
      pushLog("Battle concluded — tap Reset Battle to test another duel.")
      return
    }

    val field = BattleTurnField(
      weather = if (fusionConfig.sgbPaletteWarmth > 0.7f) WeatherCondition.HARSH_SUNLIGHT else WeatherCondition.CLEAR,
      terrain = if ("voxelizer_fx" in fusionConfig.activeChipIds) TerrainCondition.ELECTRIC_TERRAIN else TerrainCondition.NONE,
    )

    val playerResult = CanonBattleResolver.resolveTurn(
      attacker = playerSnapshot,
      defender = opponentSnapshot,
      move = move,
      field = field,
    )
    val playerDmg = if (playerResult.maxDamage == 0) 0 else ((playerResult.minDamage + playerResult.maxDamage) / 2).coerceAtLeast(1)

    val enemyMove = enemyMoves[turnCounter % enemyMoves.size]
    val enemyResult = CanonBattleResolver.resolveTurn(
      attacker = opponentSnapshot,
      defender = playerSnapshot,
      move = enemyMove,
      field = field,
    )
    val enemyDmg = if (enemyResult.maxDamage == 0) 0 else ((enemyResult.minDamage + enemyResult.maxDamage) / 2).coerceAtLeast(1)

    if (playerResult.attackerActsFirst) {
      enemyCurrentHp = (enemyCurrentHp - playerDmg).coerceAtLeast(0)
      pushLog("T$turnCounter: ${primaryPokemon.name} used ${move.name} -> $playerDmg dmg (${playerResult.typeEffectiveness}x)")
      if (enemyCurrentHp > 0) {
        playerCurrentHp = (playerCurrentHp - enemyDmg).coerceAtLeast(0)
        pushLog("T$turnCounter: ${currentOpponent.name} countered with ${enemyMove.name} -> $enemyDmg dmg")
      } else {
        pushLog("Victory! ${currentOpponent.name} fainted in Game Test!")
      }
    } else {
      playerCurrentHp = (playerCurrentHp - enemyDmg).coerceAtLeast(0)
      pushLog("T$turnCounter: ${currentOpponent.name} struck first with ${enemyMove.name} -> $enemyDmg dmg")
      if (playerCurrentHp > 0) {
        enemyCurrentHp = (enemyCurrentHp - playerDmg).coerceAtLeast(0)
        pushLog("T$turnCounter: ${primaryPokemon.name} used ${move.name} -> $playerDmg dmg (${playerResult.typeEffectiveness}x)")
        if (enemyCurrentHp <= 0) {
          pushLog("Victory! ${currentOpponent.name} fainted in Game Test!")
        }
      } else {
        pushLog("${primaryPokemon.name} fainted! Tap Reset Battle to retry.")
      }
    }
    turnCounter += 1
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("game_test_container"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Playtest Header Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, SgbEmerald.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
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
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "Live Game Test Runtime",
                tint = SgbEmerald,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "INTERACTIVE GAME TEST SANDBOX",
                style = MaterialTheme.typography.labelSmall,
                color = SgbEmerald,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = activeProject?.title ?: "Active Engine Playtest Build",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Blend ${(fusionConfig.pixelTo3dBlend * 100).roundToInt()}% 3D • ${fusionConfig.activeChipIds.size} Chips Active • 60 FPS Deterministic",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
          }

          OutlinedButton(
            onClick = { resetBattle() },
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("game_test_reset_button"),
          ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Game Test", tint = SuperFxGold, modifier = Modifier.size(18.dp))
          }
        }

        // Mode Switcher (Overworld Test vs Battle Test)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PlaytestMode.entries.forEach { mode ->
            val selected = playtestMode == mode
            FilterChip(
              selected = selected,
              onClick = { playtestMode = mode },
              leadingIcon = {
                Icon(
                  imageVector = if (mode == PlaytestMode.OVERWORLD) Icons.Default.Map else Icons.Default.Bolt,
                  contentDescription = mode.label,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(mode.label, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
                selectedLabelColor = SgbEmerald,
                selectedLeadingIconColor = SgbEmerald,
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("game_test_mode_${mode.name.lowercase()}"),
            )
          }
        }
      }
    }

    // Playtest Handheld / Console Screen Frame
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = StudioDeepNavy),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          if (playtestMode == PlaytestMode.OVERWORLD) {
            // Overworld Playtest View
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "OVERWORLD • POS ($playerRow,$playerCol) • STEPS: $stepsTaken",
                fontFamily = PressStart2PFamily,
                style = MaterialTheme.typography.labelSmall,
                color = SuperFxGold,
              )
              Button(
                onClick = { triggerWildEncounter(0) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson, contentColor = Color.White),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("game_test_trigger_encounter_button"),
              ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Encounter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }

            // 8x8 Playable Grid with Trainer + Partner Sprite
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF060B14))
                .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
                .padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              for (r in 0 until 8) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  for (c in 0 until 8) {
                    val idx = r * 8 + c
                    val tileType = mapGrid.getOrElse(idx) { 0 }
                    val isPlayerHere = r == playerRow && c == playerCol
                    val tileColor = tileColorFor(tileType)
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(
                          width = if (isPlayerHere) 2.dp else 0.75.dp,
                          color = if (isPlayerHere) SuperFxGold else tileColor.copy(alpha = 0.5f),
                          shape = RoundedCornerShape(5.dp),
                        )
                        .clickable {
                          playerRow = r
                          playerCol = c
                          stepsTaken += 1
                          pushLog("Fast-moved to ${tileName(tileType)} ($r, $c)")
                        }
                        .testTag("playtest_tile_$idx"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPokemonMapTile(tileType)
                      }
                      if (isPlayerHere) {
                        Box(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(CyberCyan.copy(alpha = 0.25f)),
                          contentAlignment = Alignment.Center,
                        ) {
                          AsyncImage(
                            model = primaryPokemon.pixelSpriteUrl,
                            contentDescription = "Trainer Partner ${primaryPokemon.name}",
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            // Handheld D-Pad + A/B Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // D-Pad Cluster
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                DPadButton(
                  icon = Icons.Default.KeyboardArrowUp,
                  label = "Move Up",
                  tag = "dpad_up_button",
                  onClick = { moveTrainer(-1, 0) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  DPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    label = "Move Left",
                    tag = "dpad_left_button",
                    onClick = { moveTrainer(0, -1) },
                  )
                  Spacer(modifier = Modifier.size(48.dp))
                  DPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "Move Right",
                    tag = "dpad_right_button",
                    onClick = { moveTrainer(0, 1) },
                  )
                }
                DPadButton(
                  icon = Icons.Default.KeyboardArrowDown,
                  label = "Move Down",
                  tag = "dpad_down_button",
                  onClick = { moveTrainer(1, 0) },
                )
              }

              // A / B Action Buttons
              Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = {
                    val currentTile = mapGrid.getOrElse(playerRow * 8 + playerCol) { 0 }
                    if (currentTile == 0 || currentTile == 5 || currentTile == 3) {
                      triggerWildEncounter(currentTile)
                    } else {
                      pushLog("Inspected ${tileName(currentTile)} at ($playerRow, $playerCol): No hazards.")
                    }
                  },
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = SgbEmerald, contentColor = StudioDeepNavy),
                  modifier = Modifier
                    .size(56.dp)
                    .testTag("game_test_action_a_button"),
                ) {
                  Text("A", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = {
                    playerRow = 4
                    playerCol = 3
                    stepsTaken = 0
                    pushLog("Returned Trainer to Spawn Tile (4, 3).")
                  },
                  shape = CircleShape,
                  modifier = Modifier
                    .size(52.dp)
                    .testTag("game_test_action_b_button"),
                ) {
                  Text("B", color = ElectricCrimson, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            // Interactive Battle Playtest View
            Text(
              text = "LIVE TURN-BASED BATTLE TEST • TURN $turnCounter",
              fontFamily = PressStart2PFamily,
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )

            // Gimmick Selector Strip
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(
                BattleGimmickMode.NONE,
                BattleGimmickMode.ANIME_BATTLE_BOND,
                BattleGimmickMode.MEGA_EVOLUTION,
                BattleGimmickMode.Z_MOVE,
                BattleGimmickMode.DYNAMAX,
                BattleGimmickMode.TERASTALLIZATION,
              ).forEach { mode ->
                FilterChip(
                  selected = activeGimmick == mode,
                  onClick = { activeGimmick = mode },
                  label = { Text(mode.displayName.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
                    selectedLabelColor = SuperFxGold,
                  ),
                )
              }
            }

            // Battle Graphics Style Selector (Sprite vs 3D Cel vs HD-2D Fusion)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              PokemonGraphicsStyle.entries.forEach { style ->
                FilterChip(
                  selected = battleGraphicsStyle == style,
                  onClick = { battleGraphicsStyle = style },
                  label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }

            // Authentic Diagonal Pokémon Battle Scene (Top-Right Foe + Bottom-Left Player)
            val enemyAccent = parseHexColorSafe(currentOpponent.primaryHex, ElectricCrimson)
            val playerAccent = parseHexColorSafe(primaryPokemon.primaryHex, CyberCyan)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1428), Color(0xFF13243E), Color(0xFF09101C)),
                  )
                )
                .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(8.dp),
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawPokemonBattlePlatform(
                  center = Offset(size.width * 0.75f, size.height * 0.38f),
                  widthPx = size.width * 0.44f,
                  heightPx = 30f,
                  primaryAccent = enemyAccent,
                  secondaryAccent = SuperFxGold,
                  pulsePhase = 0.7f,
                )
                drawPokemonBattlePlatform(
                  center = Offset(size.width * 0.25f, size.height * 0.85f),
                  widthPx = size.width * 0.48f,
                  heightPx = 34f,
                  primaryAccent = playerAccent,
                  secondaryAccent = SgbEmerald,
                  pulsePhase = 0.9f,
                )
              }

              // Top-Left Foe Status HUD Plate
              PokemonClassicStatusHud(
                name = currentOpponent.name,
                level = 50,
                primaryType = currentOpponent.primaryType,
                secondaryType = currentOpponent.secondaryType,
                currentHp = enemyCurrentHp,
                maxHp = enemyMaxHp,
                accentColor = enemyAccent,
                showNumericHp = false,
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .fillMaxWidth(0.54f),
              )

              // Top-Right Enemy Sprite / 3D Model
              PokemonCharacterRenderer(
                pokemon = currentOpponent,
                style = battleGraphicsStyle,
                pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                extrusionDepth = fusionConfig.voxelExtrusionDepth,
                size = 116.dp,
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(end = 10.dp, top = 6.dp),
              )

              // Bottom-Left Player Sprite / 3D Model (Mirrored facing Foe)
              PokemonCharacterRenderer(
                pokemon = primaryPokemon,
                style = battleGraphicsStyle,
                pixelTo3dBlend = fusionConfig.pixelTo3dBlend,
                extrusionDepth = fusionConfig.voxelExtrusionDepth,
                size = 128.dp,
                mirrored = true,
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(start = 8.dp),
              )

              // Bottom-Right Player Status HUD Plate
              PokemonClassicStatusHud(
                name = primaryPokemon.name,
                level = 50,
                primaryType = primaryPokemon.primaryType,
                secondaryType = primaryPokemon.secondaryType,
                currentHp = playerCurrentHp,
                maxHp = playerMaxHp,
                accentColor = playerAccent,
                gimmickLabel = activeGimmick.displayName.substringBefore(" ("),
                showNumericHp = true,
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .fillMaxWidth(0.56f),
              )
            }

            // 4 Interactive Move Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              for (rowIdx in 0 until 2) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  for (colIdx in 0 until 2) {
                    val moveIdx = rowIdx * 2 + colIdx
                    val move = playerMoves[moveIdx]
                    Button(
                      onClick = { executeBattleMove(move) },
                      enabled = playerCurrentHp > 0 && enemyCurrentHp > 0,
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCardDark,
                        contentColor = StudioTextPrimary,
                      ),
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("game_test_move_$moveIdx"),
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                          text = move.name,
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "${move.type.displayName} • ${move.basePower} BP",
                          style = MaterialTheme.typography.labelSmall,
                          color = CyberCyan,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Live Console Playtest Dialog Log
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(StudioCardDark)
              .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
              .padding(10.dp)
              .testTag("game_test_log_box"),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text = "PLAYTEST RUNTIME LOG",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            battleLog.take(4).forEach { msg ->
              Text(
                text = "▸ $msg",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary,
              )
            }
          }
        }

        // Subtle CRT Scanline Overlay honoring active FusionConfig
        if (fusionConfig.crtScanlineIntensity > 0.05f) {
          Canvas(modifier = Modifier.matchParentSize()) {
            val step = 7f
            var y = 0f
            while (y < size.height) {
              drawLine(
                color = Color.Black.copy(alpha = (fusionConfig.crtScanlineIntensity * 0.25f).coerceIn(0f, 0.2f)),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
              )
              y += step
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DPadButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tag: String,
  onClick: () -> Unit,
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier
      .size(48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(StudioCardDark)
      .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
      .testTag(tag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = CyberCyan,
    )
  }
}

private fun toCombatantSnapshot(entity: NativePokemonEntity, gimmick: BattleGimmickMode): CombatantSnapshot {
  return CombatantSnapshot(
    dexNumber = entity.dexNumber,
    name = entity.name,
    level = 50,
    primaryType = PokemonType.fromString(entity.primaryType),
    secondaryType = entity.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) },
    baseHp = entity.baseHp,
    baseAtk = entity.baseAttack,
    baseDef = entity.baseDefense,
    baseSpA = entity.baseSpAtk,
    baseSpD = entity.baseSpDef,
    baseSpe = entity.baseSpeed,
    abilityName = entity.abilityPrimary,
    nature = NatureModifier.HARDY,
    gimmickMode = gimmick,
    teraType = PokemonType.fromString(entity.primaryType),
  )
}

private fun buildMovesForPokemon(entity: NativePokemonEntity): List<BattleMoveSpec> {
  val primary = PokemonType.fromString(entity.primaryType)
  val secondary = entity.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) } ?: PokemonType.NORMAL
  val prefersPhysical = entity.baseAttack >= entity.baseSpAtk
  return listOf(
    BattleMoveSpec(
      name = entity.signatureMove.ifBlank { "${primary.displayName} Burst" },
      type = primary,
      category = if (prefersPhysical) MoveCategory.PHYSICAL else MoveCategory.SPECIAL,
      basePower = 95,
      isContact = prefersPhysical,
    ),
    BattleMoveSpec(
      name = "${secondary.displayName} Strike",
      type = secondary,
      category = MoveCategory.PHYSICAL,
      basePower = 80,
      isContact = true,
    ),
    BattleMoveSpec(
      name = "Quick Dash",
      type = PokemonType.NORMAL,
      category = MoveCategory.PHYSICAL,
      basePower = 50,
      priority = 1,
      isContact = true,
    ),
    BattleMoveSpec(
      name = "Aura Pulse",
      type = PokemonType.FIGHTING,
      category = MoveCategory.SPECIAL,
      basePower = 80,
    ),
  )
}

private fun tileColorFor(tileType: Int): Color = when (tileType) {
  0 -> SgbEmerald
  1 -> Mode7Sky
  2 -> SuperFxGold
  3 -> VoxelViolet
  4 -> ElectricCrimson
  else -> CyberCyan
}

private fun tileName(tileType: Int): String = when (tileType) {
  0 -> "Tall Grass"
  1 -> "TEV Water"
  2 -> "Mode-7 Road"
  3 -> "3D Gym Floor"
  4 -> "Super FX2 Magma"
  else -> "Terastal Crystal"
}

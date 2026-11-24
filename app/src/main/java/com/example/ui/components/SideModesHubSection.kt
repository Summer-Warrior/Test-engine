package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTowerEngine
import com.example.engine.BattleTowerState
import com.example.engine.CampActivityType
import com.example.engine.CanonRegionChapter
import com.example.engine.ContestAppealSpec
import com.example.engine.ContestCategory
import com.example.engine.PokemonType
import com.example.engine.PrismContestEngine
import com.example.engine.PrismContestState
import com.example.engine.SideModeType
import com.example.engine.SideModesSessionState
import com.example.engine.TimeOfDayCycle
import com.example.engine.TowerBpReward
import com.example.ui.StudioViewModelHelpers
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
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
 * Playmat-First Side Modes Hub for Pokémon: Prism Resonance (Room v6).
 * Houses all 5 playable Side Modes cleanly modularized outside main Overworld/Battle code:
 *  1. Battle Tower / Frontier Streak (7-battle 3v3 gauntlet + BP Exchange Shop)
 *  2. Prism Contest & Showcase Appeal Stage (5-round Cool/Beauty/Cute/Clever/Tough + Ribbons)
 *  3. Lental Photosphere Snap (Live Viewfinder + Illumina Orbs / Apples + 1★–4★ Diamond)
 *  4. Mystery Rift Dungeon Crawl (Procedural 5-floor 10x10 roguelite + Rift Guardian)
 *  5. Ranger Capture Styler & Prism Camp Bonding (Touch-Loop Styler + 6-Pokémon Picnic)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SideModesHubSection(
  session: SideModesSessionState,
  activeChapter: CanonRegionChapter,
  timeOfDay: TimeOfDayCycle,
  partyDexIds: List<Int>,
  bondGauge: Int,
  is3dCelModel: Boolean,
  resolvePokemonByDex: (Int) -> NativePokemonEntity,
  onCloseHub: () -> Unit,
  onSelectMode: (SideModeType) -> Unit,
  // Mode 1: Battle Tower
  onStartTowerGauntlet: () -> Unit,
  onExecuteTowerMove: (BattleMoveSpec) -> Unit,
  onSwitchTowerSlot: (Int) -> Unit,
  onAdvanceTowerRound: () -> Unit,
  onRedeemBpReward: (TowerBpReward) -> Unit,
  // Mode 2: Prism Contest
  onStartContest: (ContestCategory) -> Unit,
  onPerformContestAppeal: (ContestAppealSpec) -> Unit,
  // Mode 3: Lental Photosphere Snap
  onStepSnapFrame: () -> Unit,
  onTossSnapIlluminaOrb: () -> Unit,
  onTossSnapApple: () -> Unit,
  onNextSnapSubject: () -> Unit,
  onCaptureSnapPhoto: () -> Unit,
  // Mode 4: Mystery Rift Dungeon
  onStartRiftDungeon: () -> Unit,
  onStepRiftDirection: (Int, Int) -> Unit,
  onUseRiftPrismPulse: () -> Unit,
  onUseRiftOranBerry: () -> Unit,
  // Mode 5: Ranger Capture Styler & Prism Camp
  onStepRangerWild: () -> Unit,
  onBeginRangerDrag: (Float, Float) -> Unit,
  onStepRangerDrag: (Float, Float) -> Unit,
  onEndRangerDrag: () -> Unit,
  onNextRangerTarget: () -> Unit,
  onRecruitRangerTarget: () -> Unit,
  onPerformCampActivity: (CampActivityType) -> Unit,
) {
  val leadPokemon = resolvePokemonByDex(partyDexIds.firstOrNull() ?: 25)
  val record = session.record

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, SuperFxGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
      .testTag("side_modes_hub_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Header + Close Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PRISM SIDE MODES ARCADE",
            fontFamily = PressStart2PFamily,
            style = MaterialTheme.typography.labelMedium,
            color = SuperFxGold,
          )
          Text(
            text = "Room v6 Persisted Records • 5 Playable Clean-Room Modes",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )
        }
        IconButton(
          onClick = onCloseHub,
          modifier = Modifier.size(40.dp).testTag("side_modes_close_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Side Modes Hub", tint = StudioTextSecondary)
        }
      }

      // Persisted Room v6 Record Trophy Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        SideModeStatChip("Best Streak", "${record.battleTowerBestStreak}", SuperFxGold)
        SideModeStatChip("BP", "${record.battlePoints}", CyberCyan)
        SideModeStatChip("Ribbons", "${record.contestRibbonsWon}", Color(0xFFF472B6))
        SideModeStatChip(
          "Best Snap",
          if (record.bestSnapScore > 0) "${record.bestSnapScore} (${record.bestSnapStarRating}★)" else "—",
          SgbEmerald,
        )
        SideModeStatChip("Rift Floor", if (record.deepestRiftFloor > 0) "B${record.deepestRiftFloor}F" else "—", VoxelViolet)
        SideModeStatChip("Styler", "${record.rangerCapturesCompleted}", CyberCyan)
      }

      // 5-Mode Selector Pills
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        SideModeType.entries.forEach { mode ->
          val selected = mode == session.activeMode
          FilterChip(
            selected = selected,
            onClick = { onSelectMode(mode) },
            leadingIcon = {
              Icon(
                imageVector = when (mode) {
                  SideModeType.BATTLE_TOWER -> Icons.Default.EmojiEvents
                  SideModeType.PRISM_CONTEST -> Icons.Default.AutoAwesome
                  SideModeType.LENTAL_SNAP -> Icons.Default.CameraAlt
                  SideModeType.MYSTERY_RIFT -> Icons.Default.Explore
                  SideModeType.RANGER_CAMP -> Icons.Default.Favorite
                },
                contentDescription = null,
                modifier = Modifier.size(15.dp),
              )
            },
            label = {
              Text(
                text = mode.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
              selectedLabelColor = SuperFxGold,
              selectedLeadingIconColor = SuperFxGold,
            ),
            modifier = Modifier.testTag("side_mode_tab_${mode.id}"),
          )
        }
      }

      // Active Side Mode Playmat
      when (session.activeMode) {
        SideModeType.BATTLE_TOWER -> BattleTowerPlaymat(
          tower = session.tower,
          availableBp = record.battlePoints,
          resolvePokemonByDex = resolvePokemonByDex,
          onStartGauntlet = onStartTowerGauntlet,
          onExecuteMove = onExecuteTowerMove,
          onSwitchSlot = onSwitchTowerSlot,
          onAdvanceRound = onAdvanceTowerRound,
          onRedeemBpReward = onRedeemBpReward,
        )

        SideModeType.PRISM_CONTEST -> PrismContestPlaymat(
          contest = session.contest,
          leadPokemon = leadPokemon,
          onStartContest = onStartContest,
          onPerformAppeal = onPerformContestAppeal,
        )

        SideModeType.LENTAL_SNAP -> LentalSnapPlaymat(
          snapState = session.snap,
          activeChapter = activeChapter,
          timeOfDay = timeOfDay,
          is3dCelModel = is3dCelModel,
          resolvePokemonByDex = resolvePokemonByDex,
          onStepFrame = onStepSnapFrame,
          onTossIlluminaOrb = onTossSnapIlluminaOrb,
          onTossApple = onTossSnapApple,
          onNextSubject = onNextSnapSubject,
          onCapturePhoto = onCaptureSnapPhoto,
        )

        SideModeType.MYSTERY_RIFT -> MysteryRiftPlaymat(
          riftState = session.rift,
          leadPokemon = leadPokemon,
          onStartDungeon = onStartRiftDungeon,
          onStepDirection = onStepRiftDirection,
          onUsePrismPulse = onUseRiftPrismPulse,
          onUseOranBerry = onUseRiftOranBerry,
        )

        SideModeType.RANGER_CAMP -> RangerAndCampPlaymat(
          rangerState = session.ranger,
          campState = session.camp,
          partyDexIds = partyDexIds,
          bondGauge = bondGauge,
          resolvePokemonByDex = resolvePokemonByDex,
          onStepWildMovement = onStepRangerWild,
          onBeginDrag = onBeginRangerDrag,
          onStepDrag = onStepRangerDrag,
          onEndDrag = onEndRangerDrag,
          onNextTarget = onNextRangerTarget,
          onRecruitTarget = onRecruitRangerTarget,
          onPerformCampActivity = onPerformCampActivity,
        )
      }
    }
  }
}

@Composable
private fun SideModeStatChip(label: String, value: String, accent: Color) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(StudioSurfaceDark)
      .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
    Text(text = value, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
  }
}

// =================================================================================================
// MODE 1: BATTLE TOWER / FRONTIER STREAK + BP EXCHANGE SHOP
// =================================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BattleTowerPlaymat(
  tower: BattleTowerState,
  availableBp: Int,
  resolvePokemonByDex: (Int) -> NativePokemonEntity,
  onStartGauntlet: () -> Unit,
  onExecuteMove: (BattleMoveSpec) -> Unit,
  onSwitchSlot: (Int) -> Unit,
  onAdvanceRound: () -> Unit,
  onRedeemBpReward: (TowerBpReward) -> Unit,
) {
  var showBpShop by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (tower.isFrontierBrainRound) "ROUND 7/7 — FRONTIER BRAIN!" else "BATTLE TOWER ROUND ${tower.roundNumber}/${tower.totalRounds}",
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelSmall,
          color = if (tower.isFrontierBrainRound) SuperFxGold else CyberCyan,
        )
        Text(
          text = "${tower.opponentTitle} ${tower.opponentName} (Lv.${tower.enemyLevel}) • Streak: ${tower.currentStreak} • BP: $availableBp",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = { showBpShop = !showBpShop },
          modifier = Modifier.testTag("tower_bp_shop_toggle"),
        ) {
          Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("BP Shop", style = MaterialTheme.typography.labelSmall, color = SuperFxGold)
        }
        Button(
          onClick = onStartGauntlet,
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
          modifier = Modifier.testTag("tower_start_gauntlet_button"),
        ) {
          Text(if (tower.isActive) "Restart" else "Start 7-Battle", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 7-Battle Escalating Round Strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      for (r in 1..7) {
        val isCurrent = tower.isActive && tower.roundNumber == r
        val isCleared = tower.isActive && (r < tower.roundNumber || (r == tower.roundNumber && tower.roundCleared))
        val bg = when {
          isCleared -> SgbEmerald
          isCurrent && r == 7 -> SuperFxGold
          isCurrent -> CyberCyan
          else -> StudioSurfaceDark
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, if (r == 7) SuperFxGold else StudioBorderDark, RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (r == 7) "★7" else "$r",
            style = MaterialTheme.typography.labelSmall,
            color = if (isCleared || isCurrent) StudioDeepNavy else StudioTextSecondary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (showBpShop) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
          .testTag("tower_bp_shop_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "FRONTIER BP EXCHANGE SHOP (Wallet: $availableBp BP)",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            fontWeight = FontWeight.Bold,
          )
          BattleTowerEngine.bpShopCatalog.forEach { reward ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(reward.label, style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                Text(reward.description, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
              }
              Button(
                onClick = { onRedeemBpReward(reward) },
                enabled = availableBp >= reward.bpCost,
                colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
                modifier = Modifier.testTag("bp_buy_${reward.id}"),
              ) {
                Text("${reward.bpCost} BP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (tower.isActive) {
      val playerMon = resolvePokemonByDex(tower.activePlayerDex)
      val enemyMon = resolvePokemonByDex(tower.activeEnemyDex)
      val pHp = tower.playerHpMap[playerMon.dexNumber] ?: 0
      val pMax = (tower.playerMaxHpMap[playerMon.dexNumber] ?: 100).coerceAtLeast(1)
      val eHp = tower.enemyHpMap[enemyMon.dexNumber] ?: 0
      val eMax = (tower.enemyMaxHpMap[enemyMon.dexNumber] ?: 100).coerceAtLeast(1)
      val moves = remember(playerMon.dexNumber) {
        StudioViewModelHelpers.buildMovesetForPokemon(playerMon, PokemonType.fromString(playerMon.primaryType))
      }

      // 3v3 Active Combatants Stage
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PokemonBattleStatusHud(
              name = enemyMon.name,
              level = tower.enemyLevel,
              primaryType = enemyMon.primaryType,
              secondaryType = enemyMon.secondaryType,
              currentHp = eHp,
              maxHp = eMax,
              showNumericHp = true,
              gimmickLabel = if (tower.isFrontierBrainRound) "FRONTIER BRAIN" else "R${tower.roundNumber}",
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            ProceduralCreatureThumbnail(
              dexNumber = enemyMon.dexNumber,
              contentDescription = "Tower Enemy ${enemyMon.name}",
              modifier = Modifier.size(60.dp),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ProceduralCreatureThumbnail(
              dexNumber = playerMon.dexNumber,
              contentDescription = "Tower Ally ${playerMon.name}",
              modifier = Modifier.size(60.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            PokemonBattleStatusHud(
              name = playerMon.name,
              level = 55,
              primaryType = playerMon.primaryType,
              secondaryType = playerMon.secondaryType,
              currentHp = pHp,
              maxHp = pMax,
              showNumericHp = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Round Cleared -> Next Round Auto-Heal or Victory Banner
      if (tower.roundCleared && !tower.gauntletCompleted) {
        Button(
          onClick = onAdvanceRound,
          colors = ButtonDefaults.buttonColors(containerColor = SgbEmerald, contentColor = StudioDeepNavy),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tower_next_round_button"),
        ) {
          Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Auto-Heal & Advance to Round ${tower.roundNumber + 1}/7", fontWeight = FontWeight.Bold)
        }
      } else if (!tower.gauntletCompleted && !tower.gauntletFailed) {
        // 2x2 Move Buttons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          moves.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              pair.forEach { move ->
                Button(
                  onClick = { onExecuteMove(move) },
                  colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceDark, contentColor = StudioTextPrimary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .testTag("tower_move_${move.name.lowercase().replace(" ", "_")}"),
                ) {
                  Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(move.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${move.type.displayName} • Pow ${move.basePower}", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                  }
                }
              }
            }
          }
        }

        // 3-Slot Squad Switch Chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          tower.playerTeamDexIds.forEachIndexed { idx, dex ->
            val mon = resolvePokemonByDex(dex)
            val hp = tower.playerHpMap[dex] ?: 0
            FilterChip(
              selected = idx == tower.playerActiveSlot,
              enabled = hp > 0,
              onClick = { onSwitchSlot(idx) },
              label = { Text("${mon.name} (${hp} HP)", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("tower_switch_slot_$idx"),
            )
          }
        }
      }
    }

    // Battle Tower Log
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tower.battleLog.take(3).forEachIndexed { idx, line ->
          Text(
            text = "• $line",
            style = MaterialTheme.typography.labelSmall,
            color = if (idx == 0) StudioTextPrimary else StudioTextSecondary,
          )
        }
      }
    }
  }
}

// =================================================================================================
// MODE 2: PRISM CONTEST & SHOWCASE APPEAL STAGE (5-Round Competition + Crowd Excitement + Ribbons)
// =================================================================================================

@Composable
private fun PrismContestPlaymat(
  contest: PrismContestState,
  leadPokemon: NativePokemonEntity,
  onStartContest: (ContestCategory) -> Unit,
  onPerformAppeal: (ContestAppealSpec) -> Unit,
) {
  val leadMoves = remember(leadPokemon.dexNumber) {
    StudioViewModelHelpers.buildMovesetForPokemon(leadPokemon, PokemonType.fromString(leadPokemon.primaryType))
  }
  val appeals = remember(leadMoves) { leadMoves.map { PrismContestEngine.mapMoveToAppeal(it) } }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // 5 Contest Category Chips (Cool, Beauty, Cute, Clever, Tough)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ContestCategory.entries.forEach { cat ->
        val catColor = parseHexColorSafe(cat.accentHex, SuperFxGold)
        FilterChip(
          selected = contest.isActive && contest.category == cat,
          onClick = { onStartContest(cat) },
          label = { Text("${cat.label} Stage", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = catColor.copy(alpha = 0.25f),
            selectedLabelColor = catColor,
          ),
          modifier = Modifier.testTag("contest_category_${cat.name.lowercase()}"),
        )
      }
    }

    if (!contest.isActive) {
      Button(
        onClick = { onStartContest(ContestCategory.COOL) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF472B6), contentColor = StudioDeepNavy),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("contest_start_button"),
      ) {
        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Enter 5-Round Prism Contest with ${leadPokemon.name}!", fontWeight = FontWeight.Bold)
      }
    } else {
      // Round + Crowd Excitement Meter (0..5)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${contest.category.label.uppercase()} STAGE — ROUND ${contest.currentRound}/${contest.totalRounds}",
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelSmall,
          color = parseHexColorSafe(contest.category.accentHex, SuperFxGold),
        )
        Text(
          text = "Excitement ${contest.crowdExcitement}/5 ★",
          style = MaterialTheme.typography.labelSmall,
          color = SuperFxGold,
          fontWeight = FontWeight.Bold,
        )
      }
      LinearProgressIndicator(
        progress = { (contest.crowdExcitement / 5f).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(6.dp)),
        color = SuperFxGold,
        trackColor = StudioBorderDark,
      )

      // 4 Coordinator Leaderboard Strip
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        contest.contestants.sortedByDescending { it.totalHearts }.forEachIndexed { rank, coord ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (coord.isPlayer) CyberCyan.copy(alpha = 0.14f) else StudioSurfaceDark)
              .border(1.dp, if (coord.isPlayer) CyberCyan else StudioBorderDark, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("#${rank + 1}", style = MaterialTheme.typography.labelSmall, color = SuperFxGold, fontWeight = FontWeight.Bold)
              ProceduralCreatureThumbnail(
                dexNumber = coord.partnerDex,
                contentDescription = coord.partnerName,
                modifier = Modifier.size(30.dp),
              )
              Column {
                Text(coord.name, style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                Text(coord.partnerName, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
              }
            }
            Text(
              text = "${coord.totalHearts}♥${if (coord.lastRoundDelta > 0) " (+${coord.lastRoundDelta})" else ""}",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFFF472B6),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (!contest.isFinished) {
        // 4 Appeal Move Buttons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          appeals.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              pair.forEach { appeal ->
                val matchesStage = appeal.category == contest.category
                Button(
                  onClick = { onPerformAppeal(appeal) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (matchesStage) SuperFxGold.copy(alpha = 0.18f) else StudioSurfaceDark,
                    contentColor = StudioTextPrimary,
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .border(1.dp, if (matchesStage) SuperFxGold else StudioBorderDark, RoundedCornerShape(10.dp))
                    .testTag("contest_appeal_${appeal.move.name.lowercase().replace(" ", "_")}"),
                ) {
                  Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(appeal.move.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(appeal.flairSummary, style = MaterialTheme.typography.labelSmall, color = if (matchesStage) SuperFxGold else CyberCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }
      } else {
        Button(
          onClick = { onStartContest(contest.category) },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (contest.playerWonRibbon) SuperFxGold else CyberCyan,
            contentColor = StudioDeepNavy,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contest_replay_button"),
        ) {
          Text(
            text = if (contest.playerWonRibbon) "Ribbon Won! Perform Again" else "Retry ${contest.category.label} Stage",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    // Stage Log
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        contest.stageLog.take(3).forEachIndexed { idx, line ->
          Text(
            text = "• $line",
            style = MaterialTheme.typography.labelSmall,
            color = if (idx == 0) StudioTextPrimary else StudioTextSecondary,
          )
        }
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleTurnField
import com.example.engine.CampaignProgressionState
import com.example.engine.CanonBattleResolver
import com.example.engine.CanonRegionChapter
import com.example.engine.CombatantSnapshot
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.NatureModifier
import com.example.engine.PokemonType
import com.example.engine.TerrainCondition
import com.example.engine.TimeOfDayCycle
import com.example.engine.WeatherCondition
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

@Composable
fun CanonTemplateSandboxSection(
  attackerEntity: NativePokemonEntity,
  defenderEntity: NativePokemonEntity,
  campaignState: CampaignProgressionState,
  isSavingProject: Boolean,
  onCompleteChapter: (CanonRegionChapter) -> Unit,
  onResetCampaign: () -> Unit,
  onProvisionMasterBaseProject: () -> Unit,
) {
  var selectedRegionId by remember { mutableStateOf(MasterCanonTemplateCatalog.worldChapters.first().id) }

  var selectedMove by remember { mutableStateOf(MasterCanonTemplateCatalog.signatureMovePool.first()) }
  var attackerGimmick by remember { mutableStateOf(BattleGimmickMode.ANIME_BATTLE_BOND) }
  var defenderGimmick by remember { mutableStateOf(BattleGimmickMode.ANIME_COUNTER_SHIELD) }
  var weather by remember { mutableStateOf(WeatherCondition.RAIN) }
  var terrain by remember { mutableStateOf(TerrainCondition.ELECTRIC_TERRAIN) }
  var isTrickRoom by remember { mutableStateOf(false) }
  var isCritical by remember { mutableStateOf(false) }

  val auditResults = remember { MasterCanonTemplateCatalog.runEngineIntegrityAudit() }

  val attackerSnapshot = remember(attackerEntity, attackerGimmick) {
    CombatantSnapshot(
      dexNumber = attackerEntity.dexNumber,
      name = attackerEntity.name,
      level = 50,
      primaryType = PokemonType.fromString(attackerEntity.primaryType),
      secondaryType = attackerEntity.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) },
      baseHp = attackerEntity.baseHp,
      baseAtk = attackerEntity.baseAttack,
      baseDef = attackerEntity.baseDefense,
      baseSpA = attackerEntity.baseSpAtk,
      baseSpD = attackerEntity.baseSpDef,
      baseSpe = attackerEntity.baseSpeed,
      abilityName = attackerEntity.abilityPrimary,
      nature = NatureModifier.TIMID,
      gimmickMode = attackerGimmick,
      teraType = PokemonType.fromString(attackerEntity.primaryType),
    )
  }

  val defenderSnapshot = remember(defenderEntity, defenderGimmick) {
    CombatantSnapshot(
      dexNumber = defenderEntity.dexNumber,
      name = defenderEntity.name,
      level = 50,
      primaryType = PokemonType.fromString(defenderEntity.primaryType),
      secondaryType = defenderEntity.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) },
      baseHp = defenderEntity.baseHp,
      baseAtk = defenderEntity.baseAttack,
      baseDef = defenderEntity.baseDefense,
      baseSpA = defenderEntity.baseSpAtk,
      baseSpD = defenderEntity.baseSpDef,
      baseSpe = defenderEntity.baseSpeed,
      abilityName = defenderEntity.abilityPrimary,
      nature = NatureModifier.HARDY,
      gimmickMode = defenderGimmick,
      teraType = PokemonType.fromString(defenderEntity.primaryType),
    )
  }

  val turnResult = remember(attackerSnapshot, defenderSnapshot, selectedMove, weather, terrain, isTrickRoom, isCritical) {
    CanonBattleResolver.resolveTurn(
      attacker = attackerSnapshot,
      defender = defenderSnapshot,
      move = selectedMove,
      field = BattleTurnField(
        weather = weather,
        terrain = terrain,
        isTrickRoomActive = isTrickRoom,
        isCriticalHit = isCritical,
      ),
    )
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Master Canon Base Project Template Header
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.5.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Master Canon Base Template",
            tint = SuperFxGold,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "UNIFIED SINGLE-GAME BASE TEMPLATE (GAMES + ANIME)",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            fontWeight = FontWeight.Bold,
          )
        }

        Text(
          text = MasterCanonTemplateCatalog.TEMPLATE_TITLE,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = StudioTextPrimary,
        )

        Text(
          text = "A deterministic, zero-glitch single-game architecture uniting Gen I–IX mainline mechanics (18×18 type chart, EV/IV/Natures, Mega, Z-Move, Dynamax, Tera) with Pokémon Anime Battle Bond, Counter-Shield, Orange Archipelago, Contests, and World Coronation progression.",
          style = MaterialTheme.typography.bodyMedium,
          color = StudioTextSecondary,
        )

        Button(
          onClick = onProvisionMasterBaseProject,
          enabled = !isSavingProject,
          colors = ButtonDefaults.buttonColors(
            containerColor = SuperFxGold,
            contentColor = StudioDeepNavy,
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("provision_canon_template_button"),
        ) {
          Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isSavingProject) "Provisioning to Local Room Vault..." else "Create Active Project from Master Canon Template",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    // 2. Engine Integrity Self-Check Matrix
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, SgbEmerald.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Zero-Glitch Engine Verification",
            tint = SgbEmerald,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "ENGINE INTEGRITY & INVARIANT SELF-CHECK",
            style = MaterialTheme.typography.labelSmall,
            color = SgbEmerald,
            fontWeight = FontWeight.Bold,
          )
        }
        auditResults.forEach { (label, passed) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextPrimary,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = if (passed) "VERIFIED" else "FAIL",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (passed) SgbEmerald else ElectricCrimson,
            )
          }
        }
      }
    }

    // 3. Omni-Region Games + Anime Progression Graph
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
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
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Omni-Region Progression",
                tint = CyberCyan,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "OMNI-REGION GAME + ANIME CAMPAIGN GRAPH",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }
            Text(
              text = "Shards: ${campaignState.harmonicShardsCollected}/${MasterCanonTemplateCatalog.worldChapters.size} • Badges: ${campaignState.badgesEarned} • Ribbons: ${campaignState.animeRibbonsAndKeys} • WCS Rank #${campaignState.worldCoronationRank}",
              style = MaterialTheme.typography.bodySmall,
              color = SuperFxGold,
              fontWeight = FontWeight.SemiBold,
            )
          }

          OutlinedButton(
            onClick = onResetCampaign,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("reset_campaign_graph_button"),
          ) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Campaign", modifier = Modifier.size(16.dp), tint = CyberCyan)
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MasterCanonTemplateCatalog.worldChapters.forEach { chapter ->
            val isCompleted = chapter.id in campaignState.completedChapterIds
            val isUnlocked = campaignState.canUnlockChapter(chapter)
            val isSelected = chapter.id == selectedRegionId
            Column(
              modifier = Modifier
                .width(155.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  when {
                    isSelected -> CyberCyan.copy(alpha = 0.16f)
                    isCompleted -> SgbEmerald.copy(alpha = 0.14f)
                    else -> StudioDeepNavy
                  }
                )
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = when {
                    isSelected -> CyberCyan
                    isCompleted -> SgbEmerald
                    isUnlocked -> Mode7Sky
                    else -> StudioBorderDark
                  },
                  shape = RoundedCornerShape(12.dp),
                )
                .clickable { selectedRegionId = chapter.id }
                .padding(10.dp)
                .testTag("canon_chapter_${chapter.id}"),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "ARC ${chapter.orderIndex}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SuperFxGold,
                  fontWeight = FontWeight.Bold,
                )
                Icon(
                  imageVector = when {
                    isCompleted -> Icons.Default.CheckCircle
                    isUnlocked -> Icons.Default.Bolt
                    else -> Icons.Default.Lock
                  },
                  contentDescription = null,
                  tint = when {
                    isCompleted -> SgbEmerald
                    isUnlocked -> CyberCyan
                    else -> StudioTextSecondary
                  },
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text = chapter.regionName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = chapter.signatureGimmick.displayName.substringBefore(" ("),
                style = MaterialTheme.typography.labelSmall,
                color = Mode7Sky,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        val activeChapter = MasterCanonTemplateCatalog.worldChapters.first { it.id == selectedRegionId }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDeepNavy)
            .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "${activeChapter.regionName} • ${activeChapter.gymOrTrialCount} Badges/Trials",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
          )
          Text(
            text = "Game Sources: ${activeChapter.gameSources}",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextPrimary,
          )
          Text(
            text = "Anime / Manga / Spin-Off Fusion: ${activeChapter.fusionSourcesLineage}",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
          )
          Text(
            text = "Landmark Silhouette: ${activeChapter.landmarkSilhouette.label} • Zones: ${activeChapter.subZones.joinToString(" · ") { it.name }}",
            style = MaterialTheme.typography.labelSmall,
            color = Mode7Sky,
          )
          Text(
            text = activeChapter.keyLoreMilestone,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary,
          )

          val canUnlock = campaignState.canUnlockChapter(activeChapter)
          val alreadyDone = activeChapter.id in campaignState.completedChapterIds
          Button(
            onClick = { onCompleteChapter(activeChapter) },
            enabled = canUnlock && !alreadyDone,
            colors = ButtonDefaults.buttonColors(
              containerColor = SgbEmerald,
              contentColor = StudioDeepNavy,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("complete_canon_chapter_button"),
          ) {
            Text(
              text = when {
                alreadyDone -> "Chapter Completed (Invariants Validated)"
                canUnlock -> "Clear Chapter & Advance Coronation Rank"
                else -> "Locked: Clear Previous Region Arc First"
              },
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // 3b. Drawn 22-Region Multi-Source World Atlas Preview
    FullRegionsWorldAtlasCanvas(
      activeRegionId = selectedRegionId,
      completedRegionIds = campaignState.completedChapterIds,
      timeOfDay = TimeOfDayCycle.DAY,
      onSelectRegion = { selectedRegionId = it },
    )

    // 4. Deterministic Multi-Era Battle & Anime Mechanics Sandbox
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, VoxelViolet.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Battle Engine Simulator",
            tint = VoxelViolet,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "LIVE CANON BATTLE & ANIME MECHANICS SIMULATOR",
            style = MaterialTheme.typography.labelSmall,
            color = VoxelViolet,
            fontWeight = FontWeight.Bold,
          )
        }

        Text(
          text = "Lv.50 ${attackerSnapshot.name} vs ${defenderSnapshot.name} (${defenderSnapshot.maxHp()} HP)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = StudioTextPrimary,
        )

        Text("Select Move", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          MasterCanonTemplateCatalog.signatureMovePool.forEach { move ->
            FilterChip(
              selected = move == selectedMove,
              onClick = { selectedMove = move },
              label = { Text("${move.name} (${move.type.displayName})", style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
            )
          }
        }

        Text("Attacker Gimmick / Anime Mode", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            BattleGimmickMode.NONE,
            BattleGimmickMode.MEGA_EVOLUTION,
            BattleGimmickMode.Z_MOVE,
            BattleGimmickMode.DYNAMAX,
            BattleGimmickMode.TERASTALLIZATION,
            BattleGimmickMode.ANIME_BATTLE_BOND,
          ).forEach { mode ->
            FilterChip(
              selected = attackerGimmick == mode,
              onClick = { attackerGimmick = mode },
              label = { Text(mode.displayName.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
                selectedLabelColor = SuperFxGold,
              ),
            )
          }
        }

        Text("Defender Defense / Counter-Shield & Field", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            BattleGimmickMode.NONE,
            BattleGimmickMode.ANIME_COUNTER_SHIELD,
            BattleGimmickMode.TERASTALLIZATION,
            BattleGimmickMode.DYNAMAX,
          ).forEach { defMode ->
            FilterChip(
              selected = defenderGimmick == defMode,
              onClick = { defenderGimmick = defMode },
              label = { Text("Def: ${defMode.displayName.substringBefore(" (")}", style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = VoxelViolet.copy(alpha = 0.22f),
                selectedLabelColor = VoxelViolet,
              ),
            )
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
              selected = weather == w,
              onClick = { weather = w },
              label = { Text(w.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TerrainCondition.entries.forEach { t ->
            FilterChip(
              selected = terrain == t,
              onClick = { terrain = t },
              label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Trick Room", style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
            Switch(
              checked = isTrickRoom,
              onCheckedChange = { isTrickRoom = it },
              colors = SwitchDefaults.colors(checkedThumbColor = StudioDeepNavy, checkedTrackColor = CyberCyan),
            )
          }
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Critical Hit", style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
            Switch(
              checked = isCritical,
              onCheckedChange = { isCritical = it },
              colors = SwitchDefaults.colors(checkedThumbColor = StudioDeepNavy, checkedTrackColor = SuperFxGold),
            )
          }
        }

        HorizontalDivider(color = StudioBorderDark)

        // Deterministic Turn Result Panel
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioDeepNavy)
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("canon_battle_result_panel"),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${turnResult.minDamage}–${turnResult.maxDamage} HP",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = CyberCyan,
            )
            Text(
              text = "${"%.1f".format(turnResult.minPercentHp)}%–${"%.1f".format(turnResult.maxPercentHp)}%",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SuperFxGold,
            )
          }

          turnResult.breakdownSteps.forEach { step ->
            Text(
              text = "• $step",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
            )
          }
        }
      }
    }
  }
}

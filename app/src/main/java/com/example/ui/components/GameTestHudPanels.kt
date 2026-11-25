package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.local.StudioItemEntity
import com.example.engine.BgmThemeId
import com.example.engine.CampaignProgressionState
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.StudioSfxType
import com.example.engine.TimeOfDayCycle
import com.example.engine.CanonRegionChapter
import com.example.ui.AudioConsoleUiState
import com.example.ui.GameTestUiState
import com.example.ui.HardwareRegistryUiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
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
 * Collapsible HUD Drawer Panels for Playmat-First GameTestSection:
 * - Audio Console (64-Step DSP BGM + Ambient Bed + Clean-Room SFX Audition)
 * - 22-Region Fusion World Switcher + Lighting + 22-Region Drawn Atlas
 * - Interactive Trainer Bag Drawer
 */
@Composable
fun GameTestAudioConsoleCard(
  hardwareState: HardwareRegistryUiState,
  audioState: AudioConsoleUiState,
  activeChapter: CanonRegionChapter,
  leadEntity: NativePokemonEntity,
  onToggleBgm: () -> Unit,
  onSelectBgmTheme: (BgmThemeId) -> Unit,
  onToggleMute: () -> Unit,
  onSetVolume: (Float) -> Unit,
  onPlayLeadCry: (NativePokemonEntity) -> Unit,
  onToggleAmbientBed: () -> Unit,
  onPlaySfx: (StudioSfxType) -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp))
      .testTag("game_test_audio_console_panel"),
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
}

@Composable
fun GameTestRegionAtlasPanel(
  gameState: GameTestUiState,
  campaignState: CampaignProgressionState,
  activeChapter: CanonRegionChapter,
  onSelectRegion: (String) -> Unit,
  onSelectTimeOfDay: (TimeOfDayCycle) -> Unit,
  onToggleFullRegionView: () -> Unit,
  onToggleSubZoneLabels: () -> Unit,
  onToggleWorldAtlas: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp))
        .testTag("game_test_regions_panel"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
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
            label = { Text("Drawn Atlas", style = MaterialTheme.typography.labelSmall) },
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

    if (gameState.showWorldAtlas) {
      FullRegionsWorldAtlasCanvas(
        activeRegionId = activeChapter.id,
        completedRegionIds = campaignState.completedChapterIds,
        timeOfDay = gameState.timeOfDay,
        onSelectRegion = onSelectRegion,
      )
    }
  }
}

@Composable
fun GameTestBagDrawerCard(
  leadEntity: NativePokemonEntity,
  leadLevel: Int,
  selectedCategory: String,
  studioItems: List<StudioItemEntity>,
  onCloseBag: () -> Unit,
  onSelectCategory: (String) -> Unit,
  onUseBagItem: (StudioItemEntity) -> Unit,
) {
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
        OutlinedButton(onClick = onCloseBag, modifier = Modifier.testTag("game_test_close_bag_button")) {
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
            selected = selectedCategory == catId,
            onClick = { onSelectCategory(catId) },
            label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("bag_category_$catId"),
          )
        }
      }

      val catItems = studioItems.filter { it.category == selectedCategory }
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
            val showQty = !reusable || (item.category == StudioItemCatalog.CAT_HOLD_ITEM && item.quantity > 1)
            Text(
              text = if (showQty) "${item.name}  ×${item.quantity}" else item.name,
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

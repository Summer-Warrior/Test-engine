package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MasterDisciplineMode
import com.example.engine.HardwareRuntimePreset
import com.example.engine.HardwareRuntimePresetCatalog
import com.example.engine.HardwareRuntimeSection
import com.example.ui.HardwareRegistryUiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
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
 * Prominent Game Test Live Hardware Runtime Mode Selector Card + Searchable 4-Section Dropdown Dialog
 * with inline Custom Hardware Combo Builder and 4-second Hardware Tour -> Ultimate Target Style.
 */
@Composable
fun GameTestHardwareRuntimeCard(
  hardwareState: HardwareRegistryUiState,
  onSelectPreset: (HardwareRuntimePreset) -> Unit,
  onApplyCustomCombo: (Set<String>, MasterDisciplineMode) -> Unit,
  onToggleHardwareTour: () -> Unit,
) {
  var showDropdownDialog by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, CyberCyan.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
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
          Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
          Text(
            text = "LIVE HARDWARE RUNTIME MODE (${hardwareState.activePlatformIds.size}/${hardwareState.allPlatforms.size.coerceAtLeast(hardwareState.activePlatformIds.size)} ACTIVE)",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        OutlinedButton(
          onClick = onToggleHardwareTour,
          modifier = Modifier.testTag("game_test_hardware_tour_button"),
        ) {
          Icon(
            imageVector = if (hardwareState.isHardwareTourActive) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (hardwareState.isHardwareTourActive) "Stop Hardware Tour" else "Run Hardware Tour",
            tint = if (hardwareState.isHardwareTourActive) ElectricCrimson else SuperFxGold,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (hardwareState.isHardwareTourActive) "Stop Tour" else "Run Tour",
            style = MaterialTheme.typography.labelSmall,
            color = if (hardwareState.isHardwareTourActive) ElectricCrimson else SuperFxGold,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      // Interactive Dropdown Trigger
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(StudioDeepNavy)
          .border(1.2.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
          .clickable { showDropdownDialog = true }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("game_test_hardware_runtime_dropdown"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (hardwareState.isHardwareTourActive && hardwareState.hardwareTourStepLabel.isNotBlank()) {
              hardwareState.hardwareTourStepLabel
            } else {
              hardwareState.activeRuntimeLabel
            },
            style = MaterialTheme.typography.labelLarge,
            color = StudioTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${hardwareState.convergedVisuals.disciplineMode.label} • Grid ${hardwareState.convergedVisuals.virtualPixelGrid.toInt()}px • ${hardwareState.convergedAudio.audioSignatureLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.ExpandMore,
          contentDescription = "Choose Hardware Runtime Mode",
          tint = CyberCyan,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }

  if (showDropdownDialog) {
    HardwareRuntimeSelectorDialog(
      hardwareState = hardwareState,
      onDismiss = { showDropdownDialog = false },
      onSelectPreset = { preset ->
        onSelectPreset(preset)
        showDropdownDialog = false
      },
      onApplyCustomCombo = { ids, mode ->
        onApplyCustomCombo(ids, mode)
        showDropdownDialog = false
      },
    )
  }
}

@Composable
private fun HardwareRuntimeSelectorDialog(
  hardwareState: HardwareRegistryUiState,
  onDismiss: () -> Unit,
  onSelectPreset: (HardwareRuntimePreset) -> Unit,
  onApplyCustomCombo: (Set<String>, MasterDisciplineMode) -> Unit,
) {
  var activeSection by remember { mutableStateOf(HardwareRuntimeSection.ULTIMATE) }
  var showCustomBuilder by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var customSelectedIds by remember(hardwareState.activePlatformIds) {
    mutableStateOf(hardwareState.activePlatformIds)
  }
  var customDiscipline by remember(hardwareState.disciplineMode) {
    mutableStateOf(hardwareState.disciplineMode)
  }

  val allPlatforms = hardwareState.allPlatforms
  val ultimatePresets = remember(allPlatforms) { HardwareRuntimePresetCatalog.buildUltimatePresets(allPlatforms) }
  val curatedCombos = remember(allPlatforms) { HardwareRuntimePresetCatalog.buildCuratedCombos(allPlatforms) }
  val categoryEraPresets = remember(allPlatforms) { HardwareRuntimePresetCatalog.buildCategoryAndEraPresets(allPlatforms) }
  val individualPresets = remember(allPlatforms) {
    allPlatforms.map { HardwareRuntimePresetCatalog.buildSingleSystemPuristPreset(it) }
  }

  val q = searchQuery.trim()
  val filteredPresets = remember(activeSection, q, ultimatePresets, curatedCombos, categoryEraPresets, individualPresets) {
    val source = when (activeSection) {
      HardwareRuntimeSection.ULTIMATE -> ultimatePresets
      HardwareRuntimeSection.CURATED_COMBO -> curatedCombos
      HardwareRuntimeSection.CATEGORY_OR_ERA -> categoryEraPresets
      HardwareRuntimeSection.INDIVIDUAL_PURIST -> individualPresets
    }
    if (q.isEmpty()) source
    else source.filter { it.label.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true) }
  }

  val filteredPlatformsForCustom = remember(allPlatforms, q) {
    if (q.isEmpty()) allPlatforms
    else allPlatforms.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.manufacturer.contains(q, ignoreCase = true) ||
        it.category.contains(q, ignoreCase = true) ||
        it.generation.contains(q, ignoreCase = true)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.86f)
        .border(1.5.dp, CyberCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
      color = StudioSurfaceDark,
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "GAME TEST HARDWARE RUNTIME SELECTOR",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Instant Live Shaders, VFX & 44.1kHz Synth Switch",
              style = MaterialTheme.typography.titleMedium,
              color = StudioTextPrimary,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_hw_runtime_dialog")) {
            Icon(Icons.Default.Close, contentDescription = "Close Hardware Selector", tint = StudioTextPrimary)
          }
        }

        // Section Tabs + Custom Builder Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          HardwareRuntimeSection.entries.forEach { sec ->
            FilterChip(
              selected = !showCustomBuilder && activeSection == sec,
              onClick = {
                showCustomBuilder = false
                activeSection = sec
              },
              label = { Text(sec.label, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.24f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("hw_runtime_tab_${sec.name.lowercase()}"),
            )
          }
          FilterChip(
            selected = showCustomBuilder,
            onClick = { showCustomBuilder = true },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = { Text("Custom Combo Builder", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
              selectedLabelColor = SuperFxGold,
            ),
            modifier = Modifier.testTag("hw_runtime_tab_custom_builder"),
          )
        }

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              if (showCustomBuilder) "Filter all ${allPlatforms.size} systems to mix..."
              else "Search presets, eras, consoles, handhelds, arcade...",
              style = MaterialTheme.typography.bodySmall,
            )
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hw_runtime_search_input"),
        )

        if (showCustomBuilder) {
          // Inline Custom Hardware Combo Builder
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            MasterDisciplineMode.entries.forEach { mode ->
              FilterChip(
                selected = customDiscipline == mode,
                onClick = { customDiscipline = mode },
                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("custom_combo_discipline_${mode.name.lowercase()}"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${customSelectedIds.size} of ${allPlatforms.size} platforms selected",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(onClick = { customSelectedIds = allPlatforms.map { it.id }.toSet() }) {
                Text("All", style = MaterialTheme.typography.labelSmall)
              }
              OutlinedButton(onClick = { customSelectedIds = setOf("snes_sfc") }) {
                Text("Clear", style = MaterialTheme.typography.labelSmall)
              }
            }
          }

          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(filteredPlatformsForCustom, key = { it.id }) { spec ->
              val checked = spec.id in customSelectedIds
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (checked) CyberCyan.copy(alpha = 0.14f) else StudioCardDark)
                  .border(1.dp, if (checked) CyberCyan else StudioBorderDark, RoundedCornerShape(10.dp))
                  .clickable {
                    customSelectedIds = if (checked && customSelectedIds.size > 1) {
                      customSelectedIds - spec.id
                    } else {
                      customSelectedIds + spec.id
                    }
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { isChecked ->
                    customSelectedIds = if (!isChecked && customSelectedIds.size > 1) {
                      customSelectedIds - spec.id
                    } else {
                      customSelectedIds + spec.id
                    }
                  },
                  colors = CheckboxDefaults.colors(checkedColor = CyberCyan),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${spec.name} (${spec.year})",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${spec.manufacturer} • ${spec.category} • ${spec.gpuChip} / ${spec.audioChip}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          Button(
            onClick = { onApplyCustomCombo(customSelectedIds, customDiscipline) },
            colors = ButtonDefaults.buttonColors(containerColor = SgbEmerald, contentColor = StudioDeepNavy),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("apply_custom_combo_button"),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Custom Combo (${customSelectedIds.size} Platforms)", fontWeight = FontWeight.Bold)
          }
        } else {
          // Searchable Preset List for Active Section
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filteredPresets, key = { it.id }) { preset ->
              val isActive = hardwareState.activeRuntimePresetId == preset.id
              val accent = when (preset.section) {
                HardwareRuntimeSection.ULTIMATE -> SuperFxGold
                HardwareRuntimeSection.CURATED_COMBO -> CyberCyan
                HardwareRuntimeSection.CATEGORY_OR_ERA -> Mode7Sky
                HardwareRuntimeSection.INDIVIDUAL_PURIST -> VoxelViolet
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isActive) accent.copy(alpha = 0.18f) else StudioCardDark)
                  .border(1.2.dp, if (isActive) accent else StudioBorderDark, RoundedCornerShape(12.dp))
                  .clickable { onSelectPreset(preset) }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("hw_runtime_preset_${preset.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) accent else StudioTextPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${preset.platformIds.size} platform(s) • ${preset.disciplineMode.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                if (isActive) {
                  Icon(Icons.Default.Check, contentDescription = "Active Mode", tint = accent, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

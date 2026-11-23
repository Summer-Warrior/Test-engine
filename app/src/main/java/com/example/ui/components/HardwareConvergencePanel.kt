package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.MasterDisciplineMode
import com.example.engine.ConvergedAudioParams
import com.example.engine.ConvergedVisualParams
import com.example.engine.HardwarePlatformSpec
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

/**
 * Interactive Universal Hardware Registry Panel (85+ Gaming Platforms, 1962-2026).
 * Every toggle, discipline mode, and quick-select button feeds HardwareConvergenceSolver
 * and AudioConvergenceSolver in real time.
 */
@Composable
fun HardwareConvergencePanel(
  allPlatforms: List<HardwarePlatformSpec>,
  activePlatformIds: Set<String>,
  disciplineMode: MasterDisciplineMode,
  convergedVisuals: ConvergedVisualParams,
  convergedAudio: ConvergedAudioParams,
  onTogglePlatform: (String) -> Unit,
  onSelectAllPlatforms: () -> Unit,
  onResetCuratedPlatforms: () -> Unit,
  onSelectDisciplineMode: (MasterDisciplineMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var showFullRegistryList by remember { mutableStateOf(false) }

  val categories = remember(allPlatforms) {
    listOf("All") + allPlatforms.map { it.category }.distinct().sorted()
  }

  val filteredPlatforms = remember(allPlatforms, searchQuery, selectedCategory) {
    val q = searchQuery.trim().lowercase()
    allPlatforms.filter { spec ->
      val catMatch = selectedCategory == "All" || spec.category == selectedCategory
      val textMatch = q.isEmpty() ||
        spec.name.lowercase().contains(q) ||
        spec.manufacturer.lowercase().contains(q) ||
        spec.generation.lowercase().contains(q) ||
        spec.gpuChip.lowercase().contains(q) ||
        spec.audioChip.lowercase().contains(q)
      catMatch && textMatch
    }
  }

  val allSelected = allPlatforms.isNotEmpty() && activePlatformIds.size >= allPlatforms.size

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, SuperFxGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
      .testTag("universal_hardware_registry_panel"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Memory,
          contentDescription = "Universal Hardware Registry",
          tint = SuperFxGold,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "UNIVERSAL HARDWARE REGISTRY (${allPlatforms.size} PLATFORMS)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SuperFxGold,
          )
          Text(
            text = "${activePlatformIds.size}/${allPlatforms.size} Active • Grid ${convergedVisuals.virtualPixelGrid.toInt()}px • ${convergedVisuals.celBands}-Band Cel • ${convergedAudio.audioSignatureLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            maxLines = 2,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onSelectAllPlatforms,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (allSelected) SgbEmerald else SuperFxGold,
            contentColor = StudioDeepNavy,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("hardware_select_all_button"),
        ) {
          Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (allSelected) "All ${allPlatforms.size} Active" else "All Hardware (${allPlatforms.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }

        OutlinedButton(
          onClick = onResetCuratedPlatforms,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("hardware_reset_curated_button"),
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = "Reset to Shovel Knight Curated", tint = CyberCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Curated", color = CyberCyan)
        }
      }

      Text(
        text = "CONVERGENCE DISCIPLINE MODE (ANTI-MUD SOLVER)",
        style = MaterialTheme.typography.labelSmall,
        color = Mode7Sky,
        fontWeight = FontWeight.Bold,
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MasterDisciplineMode.entries.forEach { mode ->
          val selected = disciplineMode == mode
          FilterChip(
            selected = selected,
            onClick = { onSelectDisciplineMode(mode) },
            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyberCyan.copy(alpha = 0.24f),
              selectedLabelColor = CyberCyan,
            ),
            modifier = Modifier.testTag("discipline_mode_${mode.name.lowercase()}"),
          )
        }
      }
      Text(
        text = disciplineMode.subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search hardware platforms", tint = CyberCyan) },
        placeholder = { Text("Search NES, SNES, Genesis, Amiga, CPS-2, N64, Switch 2...") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("hardware_search_input"),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        categories.forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("hardware_cat_${cat.lowercase().replace(" ", "_")}"),
          )
        }
      }

      val visibleList = if (showFullRegistryList || searchQuery.isNotBlank() || selectedCategory != "All") {
        filteredPlatforms
      } else {
        filteredPlatforms.take(10)
      }

      visibleList.forEach { spec ->
        val isActive = spec.id in activePlatformIds
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) CyberCyan.copy(alpha = 0.10f) else StudioDeepNavy)
            .border(
              width = if (isActive) 1.5.dp else 1.dp,
              color = if (isActive) CyberCyan else StudioBorderDark,
              shape = RoundedCornerShape(12.dp),
            )
            .clickable { onTogglePlatform(spec.id) }
            .padding(10.dp)
            .testTag("hardware_row_${spec.id}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = if (isActive) "${spec.name} Active" else "${spec.name} Inactive",
            tint = if (isActive) SgbEmerald else StudioBorderDark,
            modifier = Modifier.size(20.dp),
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${spec.name} (${spec.year})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = spec.generation,
                style = MaterialTheme.typography.labelSmall,
                color = SuperFxGold,
              )
            }
            Text(
              text = "${spec.gpuChip} • ${spec.audioChip}",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Honors: ${spec.honoredSignature} | Breaks: ${spec.brokenLimitation}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (filteredPlatforms.size > visibleList.size) {
        OutlinedButton(
          onClick = { showFullRegistryList = true },
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("hardware_show_all_list_button"),
        ) {
          Text("Show All ${filteredPlatforms.size} Hardware Platforms", color = VoxelViolet, fontWeight = FontWeight.Bold)
        }
      } else if (showFullRegistryList && searchQuery.isBlank() && selectedCategory == "All") {
        OutlinedButton(
          onClick = { showFullRegistryList = false },
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        ) {
          Text("Collapse Registry List", color = CyberCyan)
        }
      }
    }
  }
}

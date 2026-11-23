package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.ConvergedVisualParams
import com.example.engine.MasterCanonTemplateCatalog
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

/**
 * Interactive 16x16 Seamless Auto-Tiled HD-2D Map Composer across all 14 custom source-inspired regions.
 * Supports all 12 terrain/landmark brushes, drag-to-paint strokes, and Room SQLite auto-saving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldMapAndChipLabSection(
  activeRegionId: String,
  mapGrid: List<Int>,
  selectedBrush: Int,
  convergedVisuals: ConvergedVisualParams,
  onSelectRegion: (String) -> Unit,
  onSelectBrush: (Int) -> Unit,
  onPaintTile: (Int) -> Unit,
  onResetRegionMap: () -> Unit,
  onPinMapAsset: () -> Unit,
) {
  val activeChapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == activeRegionId }
    ?: MasterCanonTemplateCatalog.worldChapters.first()
  val regionAccent = parseHexColorSafe(activeChapter.regionAccentHex, CyberCyan)

  val brushes = listOf(
    0 to ("0: Meadow" to SgbEmerald),
    1 to ("1: Wild Grass" to Color(0xFF22C55E)),
    2 to ("2: Water" to Mode7Sky),
    3 to ("3: Route Road" to SuperFxGold),
    4 to ("4: Rift Lava" to ElectricCrimson),
    5 to ("5: Sanctuary" to CyberCyan),
    6 to ("6: Canopy" to Color(0xFF16A34A)),
    7 to ("7: Bridge" to Color(0xFFD97706)),
    8 to ("8: Heal Plaza" to Color(0xFF10B981)),
    9 to ("9: Wild Biome" to Color(0xFFEAB308)),
    10 to ("10: Landmark" to Color(0xFFF43F5E)),
    11 to ("11: Warp Gate" to VoxelViolet),
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "16×16 AUTO-TILED HD-2D MAP COMPOSER • ${activeChapter.biomeStyle.label.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = activeChapter.regionName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = onResetRegionMap,
            modifier = Modifier.testTag("reset_region_map_button"),
          ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Region Map", modifier = Modifier.size(16.dp), tint = SuperFxGold)
          }
          OutlinedButton(
            onClick = onPinMapAsset,
            modifier = Modifier.testTag("pin_map_to_sidebar_button"),
          ) {
            Text("Pin Map", color = CyberCyan)
          }
        }
      }

      // 14-Region Selector Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MasterCanonTemplateCatalog.worldChapters.forEach { chapter ->
          val selected = chapter.id == activeChapter.id
          FilterChip(
            selected = selected,
            onClick = { onSelectRegion(chapter.id) },
            label = {
              Text(
                text = chapter.regionName.substringBefore(" —"),
                style = MaterialTheme.typography.labelSmall,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
              selectedLabelColor = CyberCyan,
            ),
            modifier = Modifier.testTag("map_region_chip_${chapter.id}"),
          )
        }
      }

      // All 12 Terrain & Landmark Brushes (scrollable horizontal palette or FlowRow)
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        brushes.forEach { (id, pair) ->
          BrushChip(
            tileType = id,
            label = pair.first,
            color = pair.second,
            selected = selectedBrush == id,
            onClick = { onSelectBrush(id) },
          )
        }
      }

      // Full 16x16 Drag-to-Paint AutoTileMapCanvas
      AutoTileMapCanvas(
        tiles = mapGrid,
        gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE,
        viewportSize = null,
        biomeStyle = activeChapter.biomeStyle,
        pointsOfInterest = activeChapter.pointsOfInterest,
        waterCausticsEnabled = convergedVisuals.waterCausticsEnabled,
        weatherParticlesEnabled = convergedVisuals.weatherParticlesEnabled,
        volumetricRaysEnabled = convergedVisuals.volumetricRaysEnabled,
        weather = activeChapter.weatherPreset,
        regionAccentColor = regionAccent,
        showGridOverlay = true,
        showMinimap = false,
        enableDragPaint = true,
        onTileTappedOrPainted = onPaintTile,
        testTagPrefix = "world_map_editor_canvas",
      )

      Text(
        text = "Full 16×16 (256-tile) custom source-inspired map with ${activeChapter.pointsOfInterest.size} interactive NPCs/POIs. Drag or tap to paint; edits auto-save to Room and stream live into Game Test!",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary,
      )
    }
  }
}

@Composable
private fun BrushChip(
  tileType: Int,
  label: String,
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) color.copy(alpha = 0.24f) else StudioDeepNavy)
      .border(1.2.dp, if (selected) color else StudioBorderDark, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp)
      .testTag("map_brush_$tileType"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Canvas(
      modifier = Modifier
        .size(18.dp)
        .clip(RoundedCornerShape(4.dp)),
    ) {
      drawPokemonMapTile(tileType)
    }
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) color else StudioTextPrimary,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

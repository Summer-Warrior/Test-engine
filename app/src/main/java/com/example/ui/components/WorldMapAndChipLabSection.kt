package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

/**
 * Interactive Multi-Console HD-2D Tilemap & Diorama Map Painter.
 */
@Composable
fun WorldMapAndChipLabSection(
  mapGrid: List<Int>,
  selectedBrush: Int,
  onSelectBrush: (Int) -> Unit,
  onPaintTile: (Int) -> Unit,
  onPinMapAsset: () -> Unit,
) {
  val brushes = listOf(
    0 to ("SGB2 Tall Grass" to SgbEmerald),
    1 to ("Dolphin TEV Water" to Mode7Sky),
    2 to ("Mode-7 Cyber Road" to SuperFxGold),
    3 to ("NDS 3D Gym Floor" to VoxelViolet),
    4 to ("Super FX2 Magma" to ElectricCrimson),
    5 to ("Switch Terastal Crystal" to CyberCyan),
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
            text = "HD-2D DIORAMA & MODE-7 OVERWORLD MAP COMPOSER",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
          )
          Text(
            text = "Paint Multi-Console Terrain & Voxel Elevation Tiles",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }

        OutlinedButton(
          onClick = onPinMapAsset,
          modifier = Modifier.testTag("pin_map_to_sidebar_button"),
        ) {
          Text("Pin Map to Sidebar", color = CyberCyan)
        }
      }

      // Terrain Brush Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        brushes.take(3).forEach { (id, pair) ->
          BrushChip(
            label = pair.first,
            color = pair.second,
            selected = selectedBrush == id,
            onClick = { onSelectBrush(id) },
            modifier = Modifier.weight(1f),
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        brushes.drop(3).forEach { (id, pair) ->
          BrushChip(
            label = pair.first,
            color = pair.second,
            selected = selectedBrush == id,
            onClick = { onSelectBrush(id) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // 8x8 Interactive Isometric/HD-2D Tile Grid
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(StudioDeepNavy)
          .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        for (row in 0 until 8) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            for (col in 0 until 8) {
              val idx = row * 8 + col
              val tileType = mapGrid.getOrElse(idx) { 0 }
              val tileColor = brushes.firstOrNull { it.first == tileType }?.second?.second ?: SgbEmerald
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(34.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .border(1.dp, tileColor, RoundedCornerShape(6.dp))
                  .clickable { onPaintTile(idx) }
                  .testTag("map_tile_$idx"),
                contentAlignment = Alignment.Center,
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  drawPokemonMapTile(tileType)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BrushChip(
  label: String,
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) color.copy(alpha = 0.28f) else StudioDeepNavy)
      .border(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) color else StudioBorderDark,
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextPrimary,
      maxLines = 1,
    )
  }
}

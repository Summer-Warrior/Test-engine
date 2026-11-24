package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NationalAtlasIndex
import com.example.engine.CanonRegionChapter
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.TimeOfDayCycle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold

/**
 * Interactive 22-Region Full Multi-Source Fusion World Atlas.
 * Draws ALL 22 custom 16x16 regions simultaneously with live biome tiles, connected warp-route
 * network map, Day/Dusk/Night tinting, shard badges, and one-tap travel into any full region.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullRegionsWorldAtlasCanvas(
  activeRegionId: String,
  completedRegionIds: Set<String>,
  timeOfDay: TimeOfDayCycle,
  onSelectRegion: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val chapters = MasterCanonTemplateCatalog.worldChapters
  val chapterById = chapters.associateBy { it.id }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.2.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
      .testTag("full_22_regions_world_atlas"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
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
          Icon(Icons.Default.Public, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
          Column {
            Text(
              text = "FULL 22-REGION MULTI-SOURCE WORLD ATLAS (${completedRegionIds.size}/22 SHARDS)",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "12 Mainline Eras + 10 Anime/Manga/Spin-Off Realms • All 1,025 Pokémon • Tap any drawn region to warp",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // 1. Connected Warp-Route Ocean Cartography Overview Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .clip(RoundedCornerShape(12.dp))
          .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp)),
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val bgBrush = when (timeOfDay) {
            TimeOfDayCycle.DAY -> Brush.verticalGradient(listOf(Color(0xFF082F49), Color(0xFF0C4A6E), Color(0xFF082F49)))
            TimeOfDayCycle.DUSK -> Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF9A3412), Color(0xFF1E1B4B)))
            TimeOfDayCycle.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E1B4B)))
          }
          drawRect(brush = bgBrush, size = size)

          // Subtle cartography lat/lon grid
          for (i in 1..5) {
            val y = size.height * i / 6f
            drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
          }
          for (i in 1..7) {
            val x = size.width * i / 8f
            drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1f)
          }

          // Dashed Warp-Portal sea/sky routes connecting linked regions
          val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
          chapters.forEach { src ->
            val p1 = Offset(src.atlasX * size.width, src.atlasY * size.height)
            src.connectedRegionIds.forEach { dstId ->
              val dst = chapterById[dstId] ?: return@forEach
              val p2 = Offset(dst.atlasX * size.width, dst.atlasY * size.height)
              val isHighlight = src.id == activeRegionId || dst.id == activeRegionId
              drawLine(
                color = if (isHighlight) SuperFxGold.copy(alpha = 0.85f) else CyberCyan.copy(alpha = 0.30f),
                start = p1,
                end = p2,
                strokeWidth = if (isHighlight) 2.2f else 1.2f,
                pathEffect = dash,
              )
            }
          }

          // Region nodes on the global map
          chapters.forEach { ch ->
            val pt = Offset(ch.atlasX * size.width, ch.atlasY * size.height)
            val accent = parseHexColorSafe(ch.regionAccentHex, CyberCyan)
            val isActive = ch.id == activeRegionId
            val isCleared = ch.id in completedRegionIds
            if (isActive) {
              drawCircle(SuperFxGold.copy(alpha = 0.35f), radius = 14f, center = pt)
            }
            drawCircle(Color(0xFF060B14), radius = if (isActive) 8.5f else 6.2f, center = pt)
            drawCircle(
              color = if (isCleared) SgbEmerald else accent,
              radius = if (isActive) 6.5f else 4.5f,
              center = pt,
            )
            if (isActive) {
              drawCircle(Color.White, radius = 8.5f, center = pt, style = Stroke(width = 1.6f))
            }
          }
        }
      }

      // 2. Live Drawn Full-Region 16x16 Miniature Gallery for ALL 22 Regions
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        chapters.forEach { chapter ->
          DrawnRegionMiniCard(
            chapter = chapter,
            isSelected = chapter.id == activeRegionId,
            isCleared = chapter.id in completedRegionIds,
            timeOfDay = timeOfDay,
            onClick = { onSelectRegion(chapter.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun DrawnRegionMiniCard(
  chapter: CanonRegionChapter,
  isSelected: Boolean,
  isCleared: Boolean,
  timeOfDay: TimeOfDayCycle,
  onClick: () -> Unit,
) {
  val accent = parseHexColorSafe(chapter.regionAccentHex, CyberCyan)
  val palette = paletteForBiome(chapter.biomeStyle)
  val speciesCount = NationalAtlasIndex.dexPoolForRegion(chapter.id).size
  val shortTitle = chapter.regionName.substringAfter(". ").substringBefore(" —")

  Column(
    modifier = Modifier
      .width(96.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) accent.copy(alpha = 0.22f) else StudioDeepNavy)
      .border(
        width = if (isSelected) 1.6.dp else 1.dp,
        color = if (isSelected) SuperFxGold else accent.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(6.dp)
      .testTag("atlas_drawn_region_${chapter.id}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Live drawn 16x16 full region pixel-tile canvas!
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(6.dp))
        .border(0.8.dp, StudioBorderDark, RoundedCornerShape(6.dp)),
    ) {
      val grid = MasterCanonTemplateCatalog.MAP_GRID_SIZE
      val cw = size.width / grid.toFloat()
      val ch = size.height / grid.toFloat()
      for (r in 0 until grid) {
        for (c in 0 until grid) {
          val t = chapter.defaultMapTiles.getOrElse(r * grid + c) { 0 }
          val col = when (t) {
            0 -> palette.meadowA
            1 -> palette.grassBase
            2 -> palette.waterDeep
            3 -> Color(0xFF94A3B8)
            4 -> Color(0xFFDC2626)
            5 -> CyberCyan
            6 -> palette.canopyOuter
            7 -> Color(0xFFB45309)
            8 -> SgbEmerald
            9 -> palette.wildBiomeA
            10 -> palette.roofTint
            11 -> Color(0xFFA855F7)
            else -> palette.meadowA
          }
          drawRect(col, Offset(c * cw, r * ch), Size(cw + 0.5f, ch + 0.5f))
        }
      }
      // Subtle atmospheric lighting overlay on miniature
      when (timeOfDay) {
        TimeOfDayCycle.DUSK -> drawRect(Color(0x38F97316), size = size)
        TimeOfDayCycle.NIGHT -> drawRect(Color(0x55070B1E), size = size)
        TimeOfDayCycle.DAY -> Unit
      }
    }

    Text(
      text = (if (isCleared) "★ " else "") + shortTitle,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = if (isSelected) SuperFxGold else StudioTextPrimary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "${chapter.orderIndex}/22 · ${speciesCount}pk",
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
      color = StudioTextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

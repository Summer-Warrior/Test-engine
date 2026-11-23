package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.engine.WeatherCondition
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.SuperFxGold
import kotlin.math.sin

/**
 * Pure Kotlin 4-neighbor auto-tiling bitmask & deterministic micro-variant solver.
 * Bitmask convention: N = 1, E = 2, S = 4, W = 8 (range 0..15).
 */
object AutoTileBitmaskSolver {
  const val BIT_NORTH = 1
  const val BIT_EAST = 2
  const val BIT_SOUTH = 4
  const val BIT_WEST = 8

  fun computeMask(tiles: List<Int>, gridSize: Int, col: Int, row: Int): Int {
    if (gridSize <= 0 || col !in 0 until gridSize || row !in 0 until gridSize) return 0
    val center = tiles.getOrElse(row * gridSize + col) { 0 }
    var mask = 0
    if (row > 0 && tiles.getOrElse((row - 1) * gridSize + col) { -1 } == center) mask = mask or BIT_NORTH
    if (col < gridSize - 1 && tiles.getOrElse(row * gridSize + (col + 1)) { -1 } == center) mask = mask or BIT_EAST
    if (row < gridSize - 1 && tiles.getOrElse((row + 1) * gridSize + col) { -1 } == center) mask = mask or BIT_SOUTH
    if (col > 0 && tiles.getOrElse(row * gridSize + (col - 1)) { -1 } == center) mask = mask or BIT_WEST
    return mask
  }

  /** Deterministic 4 micro-variants per cell so adjacent tiles look handcrafted instead of repetitive. */
  fun variantIndex(col: Int, row: Int): Int {
    val hash = (col * 73856093) xor (row * 19349663)
    return (hash and 0x7FFFFFFF) % 4
  }
}

/**
 * Seamless zero-gap 8x8 Auto-Tiled Canvas Map Renderer.
 * Renders continuous joined terrains (shorelines, road curbs, grass edges, crystal ridges)
 * with animated water caustics, HD-2D volumetric light shafts, regional weather particles,
 * and optional player avatar (or interactive drag-to-paint mode for the Map Editor).
 */
@Composable
fun AutoTileMapCanvas(
  tiles: List<Int>,
  gridSize: Int = 8,
  modifier: Modifier = Modifier,
  waterCausticsEnabled: Boolean = true,
  weatherParticlesEnabled: Boolean = false,
  volumetricRaysEnabled: Boolean = false,
  weather: WeatherCondition = WeatherCondition.CLEAR,
  regionAccentColor: Color = CyberCyan,
  showGridOverlay: Boolean = false,
  playerRow: Int? = null,
  playerCol: Int? = null,
  playerSpriteUrl: String? = null,
  playerCustomBitmap: Bitmap? = null,
  enableDragPaint: Boolean = false,
  onTileTappedOrPainted: ((index: Int) -> Unit)? = null,
  testTagPrefix: String = "autotile_map_canvas",
) {
  val infinite = rememberInfiniteTransition(label = "autotile_env_anim")
  val animPhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "env_phase",
  )

  val currentCallback by rememberUpdatedState(onTileTappedOrPainted)
  val customImg = remember(playerCustomBitmap) { playerCustomBitmap?.asImageBitmap() }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF060B14))
      .border(1.5.dp, StudioBorderDark, RoundedCornerShape(14.dp))
      .testTag(testTagPrefix),
  ) {
    val tileDp = maxWidth / gridSize.coerceAtLeast(1)

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .then(
          if (onTileTappedOrPainted != null) {
            Modifier.pointerInput(gridSize, enableDragPaint) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                var lastIdx = ((down.position.y / size.height) * gridSize).toInt().coerceIn(0, gridSize - 1) * gridSize +
                  ((down.position.x / size.width) * gridSize).toInt().coerceIn(0, gridSize - 1)
                currentCallback?.invoke(lastIdx)
                if (enableDragPaint) {
                  drag(down.id) { change ->
                    change.consume()
                    val r = ((change.position.y / size.height) * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val c = ((change.position.x / size.width) * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val idx = r * gridSize + c
                    if (idx != lastIdx) {
                      lastIdx = idx
                      currentCallback?.invoke(idx)
                    }
                  }
                }
              }
            }
          } else {
            Modifier
          }
        ),
    ) {
      val cellW = size.width / gridSize.toFloat()
      val cellH = size.height / gridSize.toFloat()

      for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
          val idx = row * gridSize + col
          val tileType = tiles.getOrElse(idx) { 0 }
          val mask = AutoTileBitmaskSolver.computeMask(tiles, gridSize, col, row)
          val variant = AutoTileBitmaskSolver.variantIndex(col, row)
          val topLeft = Offset(col * cellW, row * cellH)
          val cellSize = Size(cellW + 0.6f, cellH + 0.6f)

          drawAutoTiledCell(
            tileType = tileType,
            mask = mask,
            variant = variant,
            topLeft = topLeft,
            cellSize = cellSize,
            animPhase = animPhase,
            waterCausticsEnabled = waterCausticsEnabled,
          )
        }
      }

      if (showGridOverlay) {
        for (i in 1 until gridSize) {
          drawLine(Color.White.copy(alpha = 0.14f), Offset(i * cellW, 0f), Offset(i * cellW, size.height), 1f)
          drawLine(Color.White.copy(alpha = 0.14f), Offset(0f, i * cellH), Offset(size.width, i * cellH), 1f)
        }
      }

      if (volumetricRaysEnabled) {
        val rayShift = animPhase * size.width * 0.18f
        for (i in 0..2) {
          val x0 = (size.width * (0.12f + i * 0.34f) + rayShift) % (size.width * 1.15f)
          val path = Path().apply {
            moveTo(x0, 0f)
            lineTo(x0 + size.width * 0.14f, 0f)
            lineTo(x0 - size.width * 0.08f, size.height)
            lineTo(x0 - size.width * 0.22f, size.height)
            close()
          }
          drawPath(
            path = path,
            brush = Brush.verticalGradient(
              colors = listOf(
                regionAccentColor.copy(alpha = 0.14f),
                SuperFxGold.copy(alpha = 0.07f),
                Color.Transparent,
              ),
            ),
          )
        }
      }

      if (weatherParticlesEnabled) {
        drawWeatherParticlesOverlay(weather = weather, animPhase = animPhase, accent = regionAccentColor)
      }
    }

    if (playerRow != null && playerCol != null && playerRow in 0 until gridSize && playerCol in 0 until gridSize) {
      Box(
        modifier = Modifier
          .offset(x = tileDp * playerCol, y = tileDp * playerRow)
          .size(tileDp)
          .clip(RoundedCornerShape(8.dp))
          .background(CyberCyan.copy(alpha = 0.24f))
          .border(2.dp, SuperFxGold, RoundedCornerShape(8.dp))
          .testTag("autotile_player_avatar"),
        contentAlignment = Alignment.Center,
      ) {
        if (customImg != null) {
          Image(
            bitmap = customImg,
            contentDescription = "Trainer Custom Sprite",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(tileDp * 0.86f),
          )
        } else if (!playerSpriteUrl.isNullOrBlank()) {
          AsyncImage(
            model = playerSpriteUrl,
            contentDescription = "Trainer Partner Sprite",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(tileDp * 0.88f),
          )
        }
      }
    }
  }
}

private fun DrawScope.drawAutoTiledCell(
  tileType: Int,
  mask: Int,
  variant: Int,
  topLeft: Offset,
  cellSize: Size,
  animPhase: Float,
  waterCausticsEnabled: Boolean,
) {
  val x = topLeft.x
  val y = topLeft.y
  val w = cellSize.width
  val h = cellSize.height
  val hasN = (mask and AutoTileBitmaskSolver.BIT_NORTH) != 0
  val hasE = (mask and AutoTileBitmaskSolver.BIT_EAST) != 0
  val hasS = (mask and AutoTileBitmaskSolver.BIT_SOUTH) != 0
  val hasW = (mask and AutoTileBitmaskSolver.BIT_WEST) != 0
  val edgeW = (minOf(w, h) * 0.14f).coerceAtLeast(2.5f)

  when (tileType) {
    0 -> {
      // 0 = Meadow Plains (Walkable safe ground)
      val baseColor = when (variant) {
        0 -> Color(0xFF1F7A43)
        1 -> Color(0xFF228248)
        2 -> Color(0xFF1C703D)
        else -> Color(0xFF258A4D)
      }
      drawRect(baseColor, topLeft, cellSize)
      val edgeColor = Color(0xFF155730)
      if (!hasN) drawRect(edgeColor, Offset(x, y), Size(w, edgeW * 0.45f))
      if (!hasS) drawRect(edgeColor, Offset(x, y + h - edgeW * 0.45f), Size(w, edgeW * 0.45f))
      if (!hasW) drawRect(edgeColor, Offset(x, y), Size(edgeW * 0.45f, h))
      if (!hasE) drawRect(edgeColor, Offset(x + w - edgeW * 0.45f, y), Size(edgeW * 0.45f, h))

      // Subtle meadow pebble / wildflower micro-variants
      if (variant == 1) {
        drawCircle(Color(0xFFFDE047), radius = 2.1f, center = Offset(x + w * 0.62f, y + h * 0.34f))
      } else if (variant == 3) {
        drawCircle(Color(0xFF93C5FD), radius = 2.0f, center = Offset(x + w * 0.30f, y + h * 0.64f))
      } else {
        drawLine(Color(0xFF4ADE80).copy(alpha = 0.55f), Offset(x + w * 0.35f, y + h * 0.55f), Offset(x + w * 0.40f, y + h * 0.42f), 1.4f)
      }
    }

    1 -> {
      // 1 = Tall Wild Grass (Wild Pokémon Encounter Patch)
      val baseColor = when (variant) {
        0 -> Color(0xFF114B29)
        1 -> Color(0xFF14532D)
        2 -> Color(0xFF0E4425)
        else -> Color(0xFF165B32)
      }
      drawRect(baseColor, topLeft, cellSize)
      val edgeColor = Color(0xFF082E18)
      if (!hasN) drawRect(edgeColor, Offset(x, y), Size(w, edgeW * 0.7f))
      if (!hasS) drawRect(edgeColor, Offset(x, y + h - edgeW * 0.7f), Size(w, edgeW * 0.7f))
      if (!hasW) drawRect(edgeColor, Offset(x, y), Size(edgeW * 0.7f, h))
      if (!hasE) drawRect(edgeColor, Offset(x + w - edgeW * 0.7f, y), Size(edgeW * 0.7f, h))

      val sway = sin((animPhase * 6.283f + variant * 1.1f).toDouble()).toFloat() * 1.8f
      val bladeColor = if (variant % 2 == 0) Color(0xFF4ADE80) else Color(0xFF22C55E)
      val tufts = listOf(
        Offset(x + w * 0.24f, y + h * 0.78f),
        Offset(x + w * 0.52f, y + h * 0.72f),
        Offset(x + w * 0.78f, y + h * 0.80f),
      )
      tufts.forEach { base ->
        val path = Path().apply {
          moveTo(base.x - 3.4f, base.y)
          lineTo(base.x + sway, base.y - h * 0.38f)
          lineTo(base.x + 3.4f, base.y)
          close()
        }
        drawPath(path, bladeColor)
      }
    }

    2 -> {
      // 2 = Ocean / Coast Water (Auto-tiled shoreline & animated caustics)
      val deepWater = if (variant % 2 == 0) Color(0xFF0369A1) else Color(0xFF0284C7)
      drawRect(deepWater, topLeft, cellSize)
      val foamColor = Color(0xFFBAE6FD)
      val bankColor = Color(0xFFD6B982)
      if (!hasN) {
        drawRect(bankColor, Offset(x, y), Size(w, edgeW * 0.65f))
        drawRect(foamColor, Offset(x, y + edgeW * 0.65f), Size(w, edgeW * 0.5f))
      }
      if (!hasS) {
        drawRect(bankColor, Offset(x, y + h - edgeW * 0.65f), Size(w, edgeW * 0.65f))
        drawRect(foamColor, Offset(x, y + h - edgeW * 1.15f), Size(w, edgeW * 0.5f))
      }
      if (!hasW) {
        drawRect(bankColor, Offset(x, y), Size(edgeW * 0.65f, h))
        drawRect(foamColor, Offset(x + edgeW * 0.65f, y), Size(edgeW * 0.5f, h))
      }
      if (!hasE) {
        drawRect(bankColor, Offset(x + w - edgeW * 0.65f, y), Size(edgeW * 0.65f, h))
        drawRect(foamColor, Offset(x + w - edgeW * 1.15f, y), Size(edgeW * 0.5f, h))
      }

      val drift = ((animPhase + variant * 0.25f) % 1f) * w * 0.35f
      drawLine(
        color = Color(0xFF7DD3FC).copy(alpha = 0.8f),
        start = Offset(x + w * 0.14f + drift * 0.4f, y + h * 0.38f),
        end = Offset(x + w * 0.52f + drift * 0.4f, y + h * 0.38f),
        strokeWidth = 2.0f,
      )
      if (waterCausticsEnabled) {
        val shimmerAlpha = (0.28f + 0.25f * sin((animPhase * 6.283f + variant).toDouble()).toFloat()).coerceIn(0.1f, 0.65f)
        drawCircle(
          color = Color.White.copy(alpha = shimmerAlpha),
          radius = minOf(w, h) * 0.14f,
          center = Offset(x + w * 0.65f - drift * 0.25f, y + h * 0.68f),
          style = Stroke(width = 1.4f),
        )
      }
    }

    3 -> {
      // 3 = Route Road / Cobblestone Path
      drawRect(Color(0xFF334155), topLeft, cellSize)
      val stoneTone = if (variant % 2 == 0) Color(0xFF475569) else Color(0xFF3E4C61)
      drawRect(stoneTone, Offset(x + w * 0.08f, y + h * 0.10f), Size(w * 0.38f, h * 0.34f))
      drawRect(stoneTone, Offset(x + w * 0.52f, y + h * 0.52f), Size(w * 0.38f, h * 0.34f))
      val curb = Color(0xFF94A3B8)
      if (!hasN) drawRect(curb, Offset(x, y), Size(w, edgeW * 0.6f))
      if (!hasS) drawRect(curb, Offset(x, y + h - edgeW * 0.6f), Size(w, edgeW * 0.6f))
      if (!hasW) drawRect(curb, Offset(x, y), Size(edgeW * 0.6f, h))
      if (!hasE) drawRect(curb, Offset(x + w - edgeW * 0.6f, y), Size(edgeW * 0.6f, h))
      if (hasN || hasS) {
        drawLine(SuperFxGold.copy(alpha = 0.65f), Offset(x + w * 0.5f, y + h * 0.2f), Offset(x + w * 0.5f, y + h * 0.8f), 1.6f)
      } else {
        drawLine(SuperFxGold.copy(alpha = 0.65f), Offset(x + w * 0.2f, y + h * 0.5f), Offset(x + w * 0.8f, y + h * 0.5f), 1.6f)
      }
    }

    4 -> {
      // 4 = Volcanic / Fracture Rift (Blocked hazard)
      val pulse = (0.7f + 0.3f * sin((animPhase * 6.283f + variant).toDouble()).toFloat())
      drawRect(Color(0xFF7F1D1D), topLeft, cellSize)
      val rim = Color(0xFF1F2937)
      if (!hasN) drawRect(rim, Offset(x, y), Size(w, edgeW * 0.7f))
      if (!hasS) drawRect(rim, Offset(x, y + h - edgeW * 0.7f), Size(w, edgeW * 0.7f))
      if (!hasW) drawRect(rim, Offset(x, y), Size(edgeW * 0.7f, h))
      if (!hasE) drawRect(rim, Offset(x + w - edgeW * 0.7f, y), Size(edgeW * 0.7f, h))
      drawCircle(Color(0xFFF97316).copy(alpha = pulse), radius = minOf(w, h) * 0.28f, center = Offset(x + w * 0.5f, y + h * 0.5f))
      drawCircle(Color(0xFFFACC15), radius = minOf(w, h) * 0.11f, center = Offset(x + w * 0.5f, y + h * 0.5f))
    }

    else -> {
      // 5 = Prism Sanctuary / Gym Arena
      drawRect(Color(0xFF1E1B4B), topLeft, cellSize)
      drawRect(Color(0xFF312E81), Offset(x, y), Size(w * 0.5f, h * 0.5f))
      drawRect(Color(0xFF312E81), Offset(x + w * 0.5f, y + h * 0.5f), Size(w * 0.5f, h * 0.5f))
      val diamond = Path().apply {
        moveTo(x + w * 0.5f, y + h * 0.12f)
        lineTo(x + w * 0.86f, y + h * 0.5f)
        lineTo(x + w * 0.5f, y + h * 0.88f)
        lineTo(x + w * 0.14f, y + h * 0.5f)
        close()
      }
      drawPath(diamond, color = CyberCyan.copy(alpha = 0.45f + variant * 0.08f))
      drawPath(diamond, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.6f))
      if (variant == 0 || !hasN || !hasS) {
        drawCircle(SuperFxGold, radius = minOf(w, h) * 0.10f, center = Offset(x + w * 0.5f, y + h * 0.5f))
      }
    }
  }
}

private fun DrawScope.drawWeatherParticlesOverlay(
  weather: WeatherCondition,
  animPhase: Float,
  accent: Color,
) {
  val count = 18
  for (i in 0 until count) {
    val seedX = ((i * 53) % 100) / 100f
    val seedY = ((i * 37) % 100) / 100f
    val px = ((seedX + animPhase * if (weather == WeatherCondition.SANDSTORM) 0.9f else 0.15f) % 1f) * size.width
    val py = ((seedY + animPhase * if (weather == WeatherCondition.RAIN) 1.0f else 0.45f) % 1f) * size.height
    when (weather) {
      WeatherCondition.RAIN -> {
        drawLine(Color(0xFF7DD3FC).copy(alpha = 0.72f), Offset(px, py), Offset(px - 3f, py + 11f), strokeWidth = 1.6f)
      }
      WeatherCondition.SNOW -> {
        drawCircle(Color.White.copy(alpha = 0.82f), radius = 2.6f, center = Offset(px, py))
      }
      WeatherCondition.SANDSTORM -> {
        drawLine(Color(0xFFFDE68A).copy(alpha = 0.65f), Offset(px, py), Offset(px + 10f, py + 2f), strokeWidth = 1.8f)
      }
      WeatherCondition.HARSH_SUNLIGHT -> {
        drawCircle(SuperFxGold.copy(alpha = 0.45f), radius = 3.2f, center = Offset(px, py))
      }
      WeatherCondition.STRONG_WINDS -> {
        drawLine(CyberCyan.copy(alpha = 0.65f), Offset(px, py), Offset(px + 14f, py - 4f), strokeWidth = 1.6f)
      }
      WeatherCondition.CLEAR -> {
        drawCircle(accent.copy(alpha = 0.42f), radius = 2.2f, center = Offset(px, py))
      }
    }
  }
}

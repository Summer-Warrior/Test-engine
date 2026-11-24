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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.ConvergedVisualParams
import com.example.engine.LandmarkSilhouette
import com.example.engine.MoveDirection
import com.example.engine.PoiInteractionKind
import com.example.engine.RegionBiomeStyle
import com.example.engine.RegionPointOfInterest
import com.example.engine.RegionSubZone
import com.example.engine.SubZoneKind
import com.example.engine.TimeOfDayCycle
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
import com.example.engine.WeatherCondition
import com.example.ui.shaders.shovelKnightEntityEffect
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.SgbEmerald
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

internal data class BiomePalette(
  val meadowA: Color,
  val meadowB: Color,
  val grassBase: Color,
  val grassBlade: Color,
  val waterDeep: Color,
  val roadBase: Color,
  val canopyOuter: Color,
  val canopyHighlight: Color,
  val wildBiomeA: Color,
  val wildBiomeB: Color,
  val roofTint: Color,
)

internal fun paletteForBiome(biome: RegionBiomeStyle): BiomePalette = when (biome) {
  RegionBiomeStyle.TEMPERATE_MEADOW -> BiomePalette(
    Color(0xFF1F7A43), Color(0xFF258A4D), Color(0xFF114B29), Color(0xFF4ADE80),
    Color(0xFF0369A1), Color(0xFF334155), Color(0xFF0B3B22), Color(0xFF22C55E),
    Color(0xFFD9A95B), Color(0xFFFDE68A), Color(0xFFDC2626),
  )
  RegionBiomeStyle.AUTUMN_SHRINE -> BiomePalette(
    Color(0xFF5D6B2A), Color(0xFF707E32), Color(0xFF4D3B16), Color(0xFFF59E0B),
    Color(0xFF1D4ED8), Color(0xFF44403C), Color(0xFF7C2D12), Color(0xFFFB923C),
    Color(0xFFB45309), Color(0xFFFDE047), Color(0xFFB91C1C),
  )
  RegionBiomeStyle.VOLCANIC_SEA -> BiomePalette(
    Color(0xFF16804A), Color(0xFF1E9659), Color(0xFF0D4F30), Color(0xFF4ADE80),
    Color(0xFF0077B6), Color(0xFF475569), Color(0xFF065F46), Color(0xFF10B981),
    Color(0xFFE9C46A), Color(0xFFFEF3C7), Color(0xFF059669),
  )
  RegionBiomeStyle.ALPINE_SNOW -> BiomePalette(
    Color(0xFF3B6E5C), Color(0xFF477D6B), Color(0xFF234A42), Color(0xFF6EE7B7),
    Color(0xFF1E3A8A), Color(0xFF475569), Color(0xFF1E3A34), Color(0xFF94A3B8),
    Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFF4F46E5),
  )
  RegionBiomeStyle.ANCIENT_HISUI -> BiomePalette(
    Color(0xFF4A6B4F), Color(0xFF597D5E), Color(0xFF2D4735), Color(0xFFA7F3D0),
    Color(0xFF1E4E68), Color(0xFF57534E), Color(0xFF293B2F), Color(0xFFD6D3D1),
    Color(0xFFD6B982), Color(0xFFFEF3C7), Color(0xFF92400E),
  )
  RegionBiomeStyle.NEON_METROPOLIS -> BiomePalette(
    Color(0xFF1E4D5C), Color(0xFF255C6E), Color(0xFF0F3847), Color(0xFF38BDF8),
    Color(0xFF0C4A6E), Color(0xFF1E293B), Color(0xFF1E1B4B), Color(0xFF6366F1),
    Color(0xFF475569), Color(0xFF94A3B8), Color(0xFF0284C7),
  )
  RegionBiomeStyle.GRAND_BOULEVARD -> BiomePalette(
    Color(0xFF247A58), Color(0xFF2D8C66), Color(0xFF14523E), Color(0xFF5EEAD4),
    Color(0xFF0284C7), Color(0xFF3B3F5C), Color(0xFF1E3A5F), Color(0xFFF472B6),
    Color(0xFFE2E8F0), Color(0xFFFBCFE8), Color(0xFFEC4899),
  )
  RegionBiomeStyle.SUNLIT_ISLAND -> BiomePalette(
    Color(0xFF2B7A3F), Color(0xFF358C4B), Color(0xFF195229), Color(0xFF86EFAC),
    Color(0xFF0096C7), Color(0xFF44403C), Color(0xFF14532D), Color(0xFF4ADE80),
    Color(0xFFE9C46A), Color(0xFFFDE047), Color(0xFFF59E0B),
  )
  RegionBiomeStyle.STADIUM_HIGHLANDS -> BiomePalette(
    Color(0xFF2E6B4E), Color(0xFF387A5A), Color(0xFF1B4533), Color(0xFF6EE7B7),
    Color(0xFF1E40AF), Color(0xFF374151), Color(0xFF1E293B), Color(0xFF818CF8),
    Color(0xFF6B7280), Color(0xFFD1D5DB), Color(0xFFBE123C),
  )
  RegionBiomeStyle.LANTERN_TERRACE -> BiomePalette(
    Color(0xFF2E6E46), Color(0xFF397F53), Color(0xFF1A472C), Color(0xFF86EFAC),
    Color(0xFF0F766E), Color(0xFF44403C), Color(0xFF14532D), Color(0xFFFACC15),
    Color(0xFF9A6B3F), Color(0xFFFDE68A), Color(0xFF0D9488),
  )
  RegionBiomeStyle.CRYSTAL_CRATER -> BiomePalette(
    Color(0xFF2D7A52), Color(0xFF378C60), Color(0xFF1B4F37), Color(0xFF5EEAD4),
    Color(0xFF0284C7), Color(0xFF475569), Color(0xFF312E81), Color(0xFF38BDF8),
    Color(0xFFC084FC), Color(0xFFE0F2FE), Color(0xFF2563EB),
  )
  RegionBiomeStyle.APEX_TERRARIUM -> BiomePalette(
    Color(0xFF1F7A65), Color(0xFF278F77), Color(0xFF115245), Color(0xFF2DD4BF),
    Color(0xFF0369A1), Color(0xFF334155), Color(0xFF164E63), Color(0xFF00F5D4),
    Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFF06B6D4),
  )
  RegionBiomeStyle.SEVII_STAR_ISLES -> BiomePalette(
    Color(0xFF247A53), Color(0xFF2C9162), Color(0xFF154D34), Color(0xFF34D399),
    Color(0xFF075985), Color(0xFF52525B), Color(0xFF14532D), Color(0xFFFB923C),
    Color(0xFFE7B872), Color(0xFFFEF08A), Color(0xFFEA580C),
  )
  RegionBiomeStyle.ORANGE_CORAL_TRIALS -> BiomePalette(
    Color(0xFF22996C), Color(0xFF2BB37F), Color(0xFF0E6245), Color(0xFF5EEAD4),
    Color(0xFF0284C7), Color(0xFF64748B), Color(0xFF0F766E), Color(0xFF2DD4BF),
    Color(0xFFF3D59B), Color(0xFFFFFBEB), Color(0xFF0EA5E9),
  )
  RegionBiomeStyle.DESERT_CANYON -> BiomePalette(
    Color(0xFF8C5A2B), Color(0xFF9E6834), Color(0xFF6B3E19), Color(0xFFF59E0B),
    Color(0xFF0369A1), Color(0xFF57534E), Color(0xFF5C2B16), Color(0xFFEA580C),
    Color(0xFFD99B4E), Color(0xFFFDE68A), Color(0xFF7C3AED),
  )
  RegionBiomeStyle.RANGER_HIGHLANDS -> BiomePalette(
    Color(0xFF2B6F4B), Color(0xFF358259), Color(0xFF16442D), Color(0xFF4ADE80),
    Color(0xFF1D4ED8), Color(0xFF475569), Color(0xFF134E4A), Color(0xFFEF4444),
    Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFFDC2626),
  )
  RegionBiomeStyle.FEUDAL_KINGDOMS -> BiomePalette(
    Color(0xFF4A6630), Color(0xFF5A7A3A), Color(0xFF36451E), Color(0xFFFBBF24),
    Color(0xFF1E3A8A), Color(0xFF44403C), Color(0xFF451A03), Color(0xFFEF4444),
    Color(0xFFD6B982), Color(0xFFFEF3C7), Color(0xFF991B1B),
  )
  RegionBiomeStyle.FERRUM_ARENA -> BiomePalette(
    Color(0xFF365E52), Color(0xFF427062), Color(0xFF1E3F37), Color(0xFFFB923C),
    Color(0xFF0C4A6E), Color(0xFF27272A), Color(0xFF1F2937), Color(0xFFF97316),
    Color(0xFFD4A373), Color(0xFFFDE68A), Color(0xFFEA580C),
  )
  RegionBiomeStyle.LENTAL_PHOTOSPHERE -> BiomePalette(
    Color(0xFF157A6E), Color(0xFF1C9688), Color(0xFF0B4F4A), Color(0xFF2DD4BF),
    Color(0xFF0369A1), Color(0xFF334155), Color(0xFF065F46), Color(0xFF67E8F9),
    Color(0xFFA7F3D0), Color(0xFFECFEFF), Color(0xFF14B8A6),
  )
  RegionBiomeStyle.PASIO_CONVERGENCE -> BiomePalette(
    Color(0xFF27677A), Color(0xFF307C91), Color(0xFF164152), Color(0xFF38BDF8),
    Color(0xFF075985), Color(0xFF334155), Color(0xFF1E3A5F), Color(0xFF7DD3FC),
    Color(0xFFE0F2FE), Color(0xFFFDE68A), Color(0xFF0284C7),
  )
  RegionBiomeStyle.AEOS_STADIUM -> BiomePalette(
    Color(0xFF2A5B7A), Color(0xFF346E94), Color(0xFF193C57), Color(0xFFC084FC),
    Color(0xFF1E3A8A), Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFFA855F7),
    Color(0xFFD8B4FE), Color(0xFFFEF08A), Color(0xFF9333EA),
  )
  RegionBiomeStyle.MYSTERY_ULTRA_RIFT -> BiomePalette(
    Color(0xFF2E1A47), Color(0xFF3B225C), Color(0xFF1E1033), Color(0xFFE879F9),
    Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95), Color(0xFF00F5D4),
    Color(0xFF7C3AED), Color(0xFFDDD6FE), Color(0xFFA855F7),
  )
}

/**
 * Seamless zero-gap Auto-Tiled Canvas Map Renderer supporting BOTH:
 * - Full 16x16 interactive Map Editor mode (`viewportSize = null`)
 * - 9x9 Scrolling Locked-Camera Overworld mode (`viewportSize = 9`) with live corner 16x16 minimap,
 *   interactive NPC/POI beacons, HD-2D tilt, and AGSL-shaded player avatar with grounded contact shadow.
 */
@Composable
fun AutoTileMapCanvas(
  tiles: List<Int>,
  gridSize: Int = 16,
  viewportSize: Int? = null,
  modifier: Modifier = Modifier,
  biomeStyle: RegionBiomeStyle = RegionBiomeStyle.TEMPERATE_MEADOW,
  landmarkSilhouette: LandmarkSilhouette = LandmarkSilhouette.INDIGO_SPIRE,
  timeOfDay: TimeOfDayCycle = TimeOfDayCycle.DAY,
  subZones: List<RegionSubZone> = emptyList(),
  showSubZoneLabels: Boolean = false,
  pointsOfInterest: List<RegionPointOfInterest> = emptyList(),
  visuals: ConvergedVisualParams? = null,
  waterCausticsEnabled: Boolean = true,
  weatherParticlesEnabled: Boolean = false,
  volumetricRaysEnabled: Boolean = false,
  weather: WeatherCondition = WeatherCondition.CLEAR,
  regionAccentColor: Color = CyberCyan,
  showGridOverlay: Boolean = false,
  showMinimap: Boolean = false,
  regionId: String = "kanto",
  playerRow: Int? = null,
  playerCol: Int? = null,
  playerFacing: MoveDirection = MoveDirection.DOWN,
  playerWalkStep: Int = 0,
  playerTrainerSpec: TrainerCharacterSpec = TrainerCatalogAndRigEngine.defaultProtagonist,
  showTrailingPartner: Boolean = true,
  trailingPartnerRow: Int? = null,
  trailingPartnerCol: Int? = null,
  playerSpriteUrl: String? = null,
  playerDexNumber: Int = 25,
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
  val palette = remember(biomeStyle) { paletteForBiome(biomeStyle) }
  val density = LocalDensity.current

  val visibleCount = (viewportSize ?: gridSize).coerceIn(1, gridSize)
  val camCol = if (viewportSize != null && playerCol != null) {
    (playerCol - visibleCount / 2).coerceIn(0, (gridSize - visibleCount).coerceAtLeast(0))
  } else {
    0
  }
  val camRow = if (viewportSize != null && playerRow != null) {
    (playerRow - visibleCount / 2).coerceIn(0, (gridSize - visibleCount).coerceAtLeast(0))
  } else {
    0
  }

  val hd2dTiltDeg = if (viewportSize != null && visuals != null) {
    (visuals.affineWarp * 6.5f).coerceIn(0f, 7f)
  } else {
    0f
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF060B14))
      .border(1.5.dp, StudioBorderDark, RoundedCornerShape(14.dp))
      .graphicsLayer {
        if (hd2dTiltDeg > 0.2f) {
          rotationX = hd2dTiltDeg
          cameraDistance = 18f * density.density
        }
      }
      .testTag(testTagPrefix),
  ) {
    val tileDp = maxWidth / visibleCount.coerceAtLeast(1)

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .then(
          if (onTileTappedOrPainted != null) {
            Modifier.pointerInput(gridSize, visibleCount, camCol, camRow, enableDragPaint) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                fun toMapIdx(pos: Offset): Int {
                  val vr = ((pos.y / size.height) * visibleCount).toInt().coerceIn(0, visibleCount - 1)
                  val vc = ((pos.x / size.width) * visibleCount).toInt().coerceIn(0, visibleCount - 1)
                  val r = (camRow + vr).coerceIn(0, gridSize - 1)
                  val c = (camCol + vc).coerceIn(0, gridSize - 1)
                  return r * gridSize + c
                }
                var lastIdx = toMapIdx(down.position)
                currentCallback?.invoke(lastIdx)
                if (enableDragPaint) {
                  drag(down.id) { change ->
                    change.consume()
                    val idx = toMapIdx(change.position)
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
      val cellW = size.width / visibleCount.toFloat()
      val cellH = size.height / visibleCount.toFloat()

      for (vr in 0 until visibleCount) {
        val mapRow = camRow + vr
        for (vc in 0 until visibleCount) {
          val mapCol = camCol + vc
          val idx = mapRow * gridSize + mapCol
          val tileType = tiles.getOrElse(idx) { 0 }
          val mask = AutoTileBitmaskSolver.computeMask(tiles, gridSize, mapCol, mapRow)
          val variant = AutoTileBitmaskSolver.variantIndex(mapCol, mapRow)
          val topLeft = Offset(vc * cellW, vr * cellH)
          val cellSize = Size(cellW + 0.6f, cellH + 0.6f)

          drawAutoTiledCell(
            tileType = tileType,
            mask = mask,
            variant = variant,
            palette = palette,
            landmarkSilhouette = landmarkSilhouette,
            timeOfDay = timeOfDay,
            topLeft = topLeft,
            cellSize = cellSize,
            animPhase = animPhase,
            waterCausticsEnabled = waterCausticsEnabled,
          )
        }
      }

      // Subtle pulsing tile beacon under NPC / POI Trainer sprites (sprites rendered crisp in Compose layer above)
      val pulse = 0.7f + 0.3f * sin((animPhase * 6.283f).toDouble()).toFloat()
      pointsOfInterest.forEach { poi ->
        if (poi.col in camCol until (camCol + visibleCount) && poi.row in camRow until (camRow + visibleCount)) {
          val cx = (poi.col - camCol + 0.5f) * cellW
          val cy = (poi.row - camRow + 0.5f) * cellH
          val badgeColor = when (poi.interactionKind) {
            PoiInteractionKind.PROFESSOR_GUIDE -> SuperFxGold
            PoiInteractionKind.RIVAL_OR_CHALLENGER -> ElectricCrimson
            PoiInteractionKind.HEALER_OR_LORE -> SgbEmerald
          }
          drawOval(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(cx - cellW * 0.30f, cy + cellH * 0.18f),
            size = Size(cellW * 0.60f, cellH * 0.22f),
          )
          drawCircle(
            color = badgeColor.copy(alpha = 0.30f * pulse),
            radius = minOf(cellW, cellH) * 0.38f,
            center = Offset(cx, cy + cellH * 0.12f),
          )
        }
      }

      if (showGridOverlay) {
        for (i in 1 until visibleCount) {
          drawLine(Color.White.copy(alpha = 0.13f), Offset(i * cellW, 0f), Offset(i * cellW, size.height), 1f)
          drawLine(Color.White.copy(alpha = 0.13f), Offset(0f, i * cellH), Offset(size.width, i * cellH), 1f)
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

      // Atmospheric Day / Dusk / Night Lighting Pass + Luminous Night Lanterns
      drawTimeOfDayAtmospherePass(
        timeOfDay = timeOfDay,
        tiles = tiles,
        gridSize = gridSize,
        camCol = camCol,
        camRow = camRow,
        visibleCount = visibleCount,
        cellW = cellW,
        cellH = cellH,
        animPhase = animPhase,
        regionAccent = regionAccentColor,
      )

      if (weatherParticlesEnabled) {
        drawWeatherParticlesOverlay(weather = weather, animPhase = animPhase, accent = regionAccentColor)
      }
    }

    // Optional Crisp Regional Sub-Zone Landmark Ribbon Badges (Hub Town, Wild Biome, Prism Sanctuary)
    if (showSubZoneLabels && subZones.isNotEmpty()) {
      subZones.forEach { zone ->
        if (zone.col in camCol until (camCol + visibleCount) && zone.row in camRow until (camRow + visibleCount)) {
          val relCol = (zone.col - camCol).coerceIn(0, (visibleCount - 4).coerceAtLeast(0))
          val relRow = (zone.row - camRow).coerceIn(0, (visibleCount - 1).coerceAtLeast(0))
          val zoneColor = when (zone.kind) {
            SubZoneKind.HUB_TOWN -> SgbEmerald
            SubZoneKind.WILD_BIOME -> SuperFxGold
            SubZoneKind.PRISM_SANCTUARY -> CyberCyan
          }
          Box(
            modifier = Modifier
              .offset(x = tileDp * relCol, y = (tileDp * relRow - 4.dp).coerceAtLeast(2.dp))
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xD6080F1E))
              .border(1.dp, zoneColor.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
              .padding(horizontal = 5.dp, vertical = 2.dp),
          ) {
            Text(
              text = "${zone.name} · ${zone.sourceTag}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
              color = zoneColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Crisp 2D Overworld Trainer Pixel Sprites for every visible NPC / POI
    pointsOfInterest.forEach { poi ->
      if (poi.col in camCol until (camCol + visibleCount) && poi.row in camRow until (camRow + visibleCount)) {
        val relCol = poi.col - camCol
        val relRow = poi.row - camRow
        val npcTrainer = remember(poi.id, regionId) {
          TrainerCatalogAndRigEngine.resolveTrainerForPoi(poi, regionId)
        }
        val npcBobDp = (-sin((animPhase * 6.283f) + poi.col * 0.9f) * 1.4f).dp
        val roleTint = when (poi.interactionKind) {
          PoiInteractionKind.PROFESSOR_GUIDE -> SuperFxGold
          PoiInteractionKind.RIVAL_OR_CHALLENGER -> ElectricCrimson
          PoiInteractionKind.HEALER_OR_LORE -> SgbEmerald
        }
        Box(
          modifier = Modifier
            .offset(x = tileDp * relCol, y = tileDp * relRow)
            .size(tileDp)
            .testTag("autotile_npc_${poi.id}"),
          contentAlignment = Alignment.Center,
        ) {
          TrainerOverworldThumbnail(
            trainer = npcTrainer,
            pose = TrainerPose.OVERWORLD_DOWN,
            walkFrameIndex = 0,
            modifier = Modifier
              .size(tileDp * 0.94f)
              .offset(y = npcBobDp),
          )
          // Compact role exclamation / talk pip at top-right corner
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = (-1).dp, y = (-2).dp)
              .clip(RoundedCornerShape(4.dp))
              .background(roleTint)
              .padding(horizontal = 3.dp),
          ) {
            Text(
              text = when (poi.interactionKind) {
                PoiInteractionKind.RIVAL_OR_CHALLENGER -> "!"
                PoiInteractionKind.PROFESSOR_GUIDE -> "?"
                PoiInteractionKind.HEALER_OR_LORE -> "+"
              },
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 9.sp),
              color = Color.Black,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }

    // Optional Trailing Partner Creature walking right behind the Playable Trainer Avatar
    if (showTrailingPartner && trailingPartnerRow != null && trailingPartnerCol != null &&
      (trailingPartnerRow != playerRow || trailingPartnerCol != playerCol) &&
      trailingPartnerRow in camRow until (camRow + visibleCount) &&
      trailingPartnerCol in camCol until (camCol + visibleCount)
    ) {
      val relCol = trailingPartnerCol - camCol
      val relRow = trailingPartnerRow - camRow
      val partnerBobDp = (-sin((animPhase * 6.283f * 2f) + 1.1f) * 1.8f).dp
      val resolvedDex = playerSpriteUrl
        ?.substringAfterLast("/")
        ?.substringBefore(".")
        ?.toIntOrNull()
        ?.coerceIn(1, 1025)
        ?: playerDexNumber
      Box(
        modifier = Modifier
          .offset(x = tileDp * relCol, y = tileDp * relRow)
          .size(tileDp)
          .testTag("autotile_trailing_partner"),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .size(width = tileDp * 0.64f, height = tileDp * 0.20f),
        ) {
          drawOval(
            brush = Brush.radialGradient(
              colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
            ),
          )
        }
        ProceduralCreatureThumbnail(
          dexNumber = resolvedDex,
          contentDescription = "Trailing Partner Creature",
          modifier = Modifier
            .size(tileDp * 0.86f)
            .offset(y = partnerBobDp),
        )
      }
    }

    // Grounded Shovel Knight+ Playable Trainer Avatar (4-direction facing + animated walk stride!)
    if (playerRow != null && playerCol != null &&
      playerRow in camRow until (camRow + visibleCount) &&
      playerCol in camCol until (camCol + visibleCount)
    ) {
      val relCol = playerCol - camCol
      val relRow = playerRow - camRow
      val bobDp = (-sin(animPhase * 6.283f * 2f) * 2.2f).dp
      val avatarPx = with(density) { tileDp.toPx() }
      val shaderMod = if (visuals != null) {
        Modifier.shovelKnightEntityEffect(
          widthPx = avatarPx,
          heightPx = avatarPx,
          visuals = visuals,
          timeSec = animPhase * 3.6f,
          crystalGlint = 0.25f,
          hitFlash = false,
        )
      } else {
        Modifier
      }
      val playerPose = TrainerCatalogAndRigEngine.poseForDirection(playerFacing)

      Box(
        modifier = Modifier
          .offset(x = tileDp * relCol, y = tileDp * relRow)
          .size(tileDp)
          .testTag("autotile_player_avatar"),
        contentAlignment = Alignment.Center,
      ) {
        // Grounded elliptical contact shadow
        Canvas(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .size(width = tileDp * 0.72f, height = tileDp * 0.24f),
        ) {
          drawOval(
            brush = Brush.radialGradient(
              colors = listOf(
                Color.Black.copy(alpha = 0.72f),
                SuperFxGold.copy(alpha = 0.18f),
                Color.Transparent,
              ),
            ),
          )
        }
        Box(
          modifier = Modifier
            .size(tileDp * 0.96f)
            .offset(y = bobDp)
            .then(shaderMod),
          contentAlignment = Alignment.Center,
        ) {
          if (customImg != null) {
            Image(
              bitmap = customImg,
              contentDescription = "Trainer Custom Sprite",
              filterQuality = FilterQuality.None,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            TrainerOverworldThumbnail(
              trainer = playerTrainerSpec,
              pose = playerPose,
              walkFrameIndex = playerWalkStep,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }

    // Live Corner 16x16 Minimap Overlay in Scrolling Overworld Mode
    if (showMinimap && gridSize >= 12) {
      Canvas(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .size(68.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xCC060A14))
          .border(1.2.dp, CyberCyan.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
          .padding(3.dp)
          .testTag("overworld_minimap"),
      ) {
        val cw = size.width / gridSize.toFloat()
        val ch = size.height / gridSize.toFloat()
        for (r in 0 until gridSize) {
          for (c in 0 until gridSize) {
            val t = tiles.getOrElse(r * gridSize + c) { 0 }
            val col = when (t) {
              0 -> palette.meadowA
              1 -> palette.grassBase
              2 -> palette.waterDeep
              3 -> Color(0xFF94A3B8)
              4 -> Color(0xFFEF4444)
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
        // Camera viewport rectangle
        if (viewportSize != null) {
          drawRect(
            color = Color.White.copy(alpha = 0.65f),
            topLeft = Offset(camCol * cw, camRow * ch),
            size = Size(visibleCount * cw, visibleCount * ch),
            style = Stroke(width = 1.2f),
          )
        }
        // NPC pins
        pointsOfInterest.forEach { poi ->
          drawCircle(
            color = SuperFxGold,
            radius = cw * 0.55f,
            center = Offset((poi.col + 0.5f) * cw, (poi.row + 0.5f) * ch),
          )
        }
        // Player dot
        if (playerCol != null && playerRow != null) {
          drawCircle(
            color = Color.White,
            radius = cw * 0.75f,
            center = Offset((playerCol + 0.5f) * cw, (playerRow + 0.5f) * ch),
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
  palette: BiomePalette,
  landmarkSilhouette: LandmarkSilhouette,
  timeOfDay: TimeOfDayCycle,
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
  val edgeW = (minOf(w, h) * 0.14f).coerceAtLeast(2.0f)

  when (tileType) {
    0 -> {
      // 0 = Meadow Plains
      val baseColor = if (variant % 2 == 0) palette.meadowA else palette.meadowB
      drawRect(baseColor, topLeft, cellSize)
      val edgeColor = Color.Black.copy(alpha = 0.18f)
      if (!hasN) drawRect(edgeColor, Offset(x, y), Size(w, edgeW * 0.4f))
      if (!hasS) drawRect(edgeColor, Offset(x, y + h - edgeW * 0.4f), Size(w, edgeW * 0.4f))
      if (!hasW) drawRect(edgeColor, Offset(x, y), Size(edgeW * 0.4f, h))
      if (!hasE) drawRect(edgeColor, Offset(x + w - edgeW * 0.4f, y), Size(edgeW * 0.4f, h))
      if (variant == 1) {
        drawCircle(Color(0xFFFDE047), radius = 1.9f, center = Offset(x + w * 0.62f, y + h * 0.34f))
      } else if (variant == 3) {
        drawCircle(Color(0xFF93C5FD), radius = 1.8f, center = Offset(x + w * 0.30f, y + h * 0.64f))
      }
    }

    1 -> {
      // 1 = Tall Wild Grass
      drawRect(palette.grassBase, topLeft, cellSize)
      val edgeColor = Color.Black.copy(alpha = 0.28f)
      if (!hasN) drawRect(edgeColor, Offset(x, y), Size(w, edgeW * 0.6f))
      if (!hasS) drawRect(edgeColor, Offset(x, y + h - edgeW * 0.6f), Size(w, edgeW * 0.6f))
      if (!hasW) drawRect(edgeColor, Offset(x, y), Size(edgeW * 0.6f, h))
      if (!hasE) drawRect(edgeColor, Offset(x + w - edgeW * 0.6f, y), Size(edgeW * 0.6f, h))

      val sway = sin((animPhase * 6.283f + variant * 1.1f).toDouble()).toFloat() * 1.5f
      val tufts = listOf(
        Offset(x + w * 0.25f, y + h * 0.78f),
        Offset(x + w * 0.52f, y + h * 0.72f),
        Offset(x + w * 0.78f, y + h * 0.80f),
      )
      tufts.forEach { base ->
        val path = Path().apply {
          moveTo(base.x - 2.8f, base.y)
          lineTo(base.x + sway, base.y - h * 0.38f)
          lineTo(base.x + 2.8f, base.y)
          close()
        }
        drawPath(path, palette.grassBlade)
      }
    }

    2 -> {
      // 2 = Ocean / Coast Water
      drawRect(palette.waterDeep, topLeft, cellSize)
      val foamColor = Color(0xFFBAE6FD)
      val bankColor = Color(0xFFD6B982)
      if (!hasN) {
        drawRect(bankColor, Offset(x, y), Size(w, edgeW * 0.6f))
        drawRect(foamColor, Offset(x, y + edgeW * 0.6f), Size(w, edgeW * 0.45f))
      }
      if (!hasS) {
        drawRect(bankColor, Offset(x, y + h - edgeW * 0.6f), Size(w, edgeW * 0.6f))
        drawRect(foamColor, Offset(x, y + h - edgeW * 1.05f), Size(w, edgeW * 0.45f))
      }
      if (!hasW) {
        drawRect(bankColor, Offset(x, y), Size(edgeW * 0.6f, h))
        drawRect(foamColor, Offset(x + edgeW * 0.6f, y), Size(edgeW * 0.45f, h))
      }
      if (!hasE) {
        drawRect(bankColor, Offset(x + w - edgeW * 0.6f, y), Size(edgeW * 0.6f, h))
        drawRect(foamColor, Offset(x + w - edgeW * 1.05f, y), Size(edgeW * 0.45f, h))
      }

      val drift = ((animPhase + variant * 0.25f) % 1f) * w * 0.32f
      drawLine(
        color = Color(0xFF7DD3FC).copy(alpha = 0.8f),
        start = Offset(x + w * 0.14f + drift * 0.4f, y + h * 0.38f),
        end = Offset(x + w * 0.52f + drift * 0.4f, y + h * 0.38f),
        strokeWidth = 1.8f,
      )
      if (waterCausticsEnabled) {
        val shimmerAlpha = (0.28f + 0.25f * sin((animPhase * 6.283f + variant).toDouble()).toFloat()).coerceIn(0.1f, 0.65f)
        drawCircle(
          color = Color.White.copy(alpha = shimmerAlpha),
          radius = minOf(w, h) * 0.14f,
          center = Offset(x + w * 0.65f - drift * 0.25f, y + h * 0.68f),
          style = Stroke(width = 1.2f),
        )
      }
    }

    3 -> {
      // 3 = Route Road / Cobblestone
      drawRect(palette.roadBase, topLeft, cellSize)
      val stoneTone = Color.White.copy(alpha = if (variant % 2 == 0) 0.12f else 0.07f)
      drawRect(stoneTone, Offset(x + w * 0.08f, y + h * 0.10f), Size(w * 0.38f, h * 0.34f))
      drawRect(stoneTone, Offset(x + w * 0.52f, y + h * 0.52f), Size(w * 0.38f, h * 0.34f))
      val curb = Color(0xFF94A3B8)
      if (!hasN) drawRect(curb, Offset(x, y), Size(w, edgeW * 0.55f))
      if (!hasS) drawRect(curb, Offset(x, y + h - edgeW * 0.55f), Size(w, edgeW * 0.55f))
      if (!hasW) drawRect(curb, Offset(x, y), Size(edgeW * 0.55f, h))
      if (!hasE) drawRect(curb, Offset(x + w - edgeW * 0.55f, y), Size(edgeW * 0.55f, h))
      if (hasN || hasS) {
        drawLine(SuperFxGold.copy(alpha = 0.55f), Offset(x + w * 0.5f, y + h * 0.22f), Offset(x + w * 0.5f, y + h * 0.78f), 1.4f)
      } else {
        drawLine(SuperFxGold.copy(alpha = 0.55f), Offset(x + w * 0.22f, y + h * 0.5f), Offset(x + w * 0.78f, y + h * 0.5f), 1.4f)
      }
    }

    4 -> {
      // 4 = Volcanic / Fracture Rift
      val pulse = (0.7f + 0.3f * sin((animPhase * 6.283f + variant).toDouble()).toFloat())
      drawRect(Color(0xFF7F1D1D), topLeft, cellSize)
      val rim = Color(0xFF1F2937)
      if (!hasN) drawRect(rim, Offset(x, y), Size(w, edgeW * 0.65f))
      if (!hasS) drawRect(rim, Offset(x, y + h - edgeW * 0.65f), Size(w, edgeW * 0.65f))
      if (!hasW) drawRect(rim, Offset(x, y), Size(edgeW * 0.65f, h))
      if (!hasE) drawRect(rim, Offset(x + w - edgeW * 0.65f, y), Size(edgeW * 0.65f, h))
      drawCircle(Color(0xFFF97316).copy(alpha = pulse), radius = minOf(w, h) * 0.28f, center = Offset(x + w * 0.5f, y + h * 0.5f))
      drawCircle(Color(0xFFFACC15), radius = minOf(w, h) * 0.11f, center = Offset(x + w * 0.5f, y + h * 0.5f))
    }

    5 -> {
      // 5 = Prism Sanctuary / Gym Arena with Signature Silhouette Crest
      drawRect(Color(0xFF1E1B4B), topLeft, cellSize)
      drawRect(Color(0xFF312E81), Offset(x, y), Size(w * 0.5f, h * 0.5f))
      drawRect(Color(0xFF312E81), Offset(x + w * 0.5f, y + h * 0.5f), Size(w * 0.5f, h * 0.5f))
      val diamond = Path().apply {
        moveTo(x + w * 0.5f, y + h * 0.10f)
        lineTo(x + w * 0.88f, y + h * 0.5f)
        lineTo(x + w * 0.5f, y + h * 0.90f)
        lineTo(x + w * 0.12f, y + h * 0.5f)
        close()
      }
      drawPath(diamond, color = CyberCyan.copy(alpha = if (timeOfDay == TimeOfDayCycle.NIGHT) 0.72f else 0.50f))
      drawPath(diamond, color = Color.White.copy(alpha = 0.88f), style = Stroke(width = 1.4f))
      drawLandmarkSilhouetteOverlay(
        silhouette = landmarkSilhouette,
        palette = palette,
        x = x,
        y = y,
        w = w,
        h = h,
        animPhase = animPhase,
        isSanctuary = true,
      )
    }

    6 -> {
      // 6 = Forest Canopy / Cliff Wall (Blocked natural border)
      drawRect(palette.canopyOuter, topLeft, cellSize)
      drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = minOf(w, h) * 0.42f,
        center = Offset(x + w * 0.52f, y + h * 0.58f),
      )
      drawCircle(
        color = palette.canopyHighlight.copy(alpha = 0.72f),
        radius = minOf(w, h) * 0.36f,
        center = Offset(x + w * 0.48f, y + h * 0.46f),
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = minOf(w, h) * 0.16f,
        center = Offset(x + w * 0.40f, y + h * 0.36f),
      )
    }

    7 -> {
      // 7 = Wooden Bridge / Pier (Walkable over water)
      drawRect(palette.waterDeep, topLeft, cellSize)
      drawRect(Color(0xFF78350F), Offset(x + w * 0.04f, y + h * 0.04f), Size(w * 0.92f, h * 0.92f))
      drawRect(Color(0xFFB45309), Offset(x + w * 0.10f, y + h * 0.10f), Size(w * 0.80f, h * 0.80f))
      val plankColor = Color(0xFF451A03)
      drawLine(plankColor, Offset(x + w * 0.10f, y + h * 0.36f), Offset(x + w * 0.90f, y + h * 0.36f), 1.4f)
      drawLine(plankColor, Offset(x + w * 0.10f, y + h * 0.64f), Offset(x + w * 0.90f, y + h * 0.64f), 1.4f)
    }

    8 -> {
      // 8 = Town Plaza / Healing Pad (Walkable -> heals party!)
      drawRect(Color(0xFF1E293B), topLeft, cellSize)
      drawRect(Color(0xFF334155), Offset(x + 2f, y + 2f), Size(w - 4f, h - 4f))
      val pulse = 0.75f + 0.25f * sin((animPhase * 6.283f).toDouble()).toFloat()
      drawRect(SgbEmerald.copy(alpha = pulse), Offset(x + w * 0.40f, y + h * 0.18f), Size(w * 0.20f, h * 0.64f))
      drawRect(SgbEmerald.copy(alpha = pulse), Offset(x + w * 0.18f, y + h * 0.40f), Size(w * 0.64f, h * 0.20f))
      drawRect(Color.White.copy(alpha = 0.55f), Offset(x + 2f, y + 2f), Size(w - 4f, h - 4f), style = Stroke(1.2f))
    }

    9 -> {
      // 9 = Sand / Snow / Wild Biome (Walkable -> wild encounters)
      drawRect(if (variant % 2 == 0) palette.wildBiomeA else palette.wildBiomeB, topLeft, cellSize)
      drawCircle(Color.White.copy(alpha = 0.38f), radius = 2.0f, center = Offset(x + w * 0.32f, y + h * 0.34f))
      drawCircle(Color.Black.copy(alpha = 0.18f), radius = 1.8f, center = Offset(x + w * 0.68f, y + h * 0.68f))
    }

    10 -> {
      // 10 = Landmark Building / Regional Signature Silhouette (Blocked)
      drawRect(Color(0xFF111827), topLeft, cellSize)
      drawLandmarkSilhouetteOverlay(
        silhouette = landmarkSilhouette,
        palette = palette,
        x = x,
        y = y,
        w = w,
        h = h,
        animPhase = animPhase,
        isSanctuary = false,
      )
      if (timeOfDay != TimeOfDayCycle.DAY) {
        // Warm illuminated windows at Dusk/Night
        drawRect(
          color = SuperFxGold.copy(alpha = if (timeOfDay == TimeOfDayCycle.NIGHT) 0.92f else 0.65f),
          topLeft = Offset(x + w * 0.40f, y + h * 0.66f),
          size = Size(w * 0.20f, h * 0.18f),
        )
      }
    }

    else -> {
      // 11 = Region Warp Gate (Walkable -> opens Warp Gate travel modal)
      val glow = 0.65f + 0.35f * sin((animPhase * 6.283f).toDouble()).toFloat()
      drawRect(Color(0xFF1E1B4B), topLeft, cellSize)
      drawCircle(
        color = Color(0xFFA855F7).copy(alpha = glow),
        radius = minOf(w, h) * 0.38f,
        center = Offset(x + w * 0.5f, y + h * 0.5f),
        style = Stroke(width = 2.2f),
      )
      drawCircle(
        color = CyberCyan.copy(alpha = glow),
        radius = minOf(w, h) * 0.20f,
        center = Offset(x + w * 0.5f, y + h * 0.5f),
      )
      drawCircle(
        color = Color.White,
        radius = minOf(w, h) * 0.08f,
        center = Offset(x + w * 0.5f, y + h * 0.5f),
      )
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

/**
 * Procedurally draws signature inspired regional landmark silhouettes on Landmark (10) and Sanctuary (5) tiles.
 */
private fun DrawScope.drawLandmarkSilhouetteOverlay(
  silhouette: LandmarkSilhouette,
  palette: BiomePalette,
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  animPhase: Float,
  isSanctuary: Boolean,
) {
  val accent = if (isSanctuary) SuperFxGold else palette.roofTint
  val pulse = 0.7f + 0.3f * sin((animPhase * 6.283f).toDouble()).toFloat()

  when (silhouette) {
    LandmarkSilhouette.PAGODA_TOWER, LandmarkSilhouette.LANTERN_TEMPLE -> {
      // Tiered Pagoda eaves + golden spire
      drawRect(Color(0xFFCBD5E1), Offset(x + w * 0.26f, y + h * 0.54f), Size(w * 0.48f, h * 0.38f))
      for (tier in 0..2) {
        val tw = w * (0.88f - tier * 0.20f)
        val ty = y + h * (0.50f - tier * 0.17f)
        val roof = Path().apply {
          moveTo(x + (w - tw) * 0.5f, ty + h * 0.14f)
          lineTo(x + w * 0.5f, ty)
          lineTo(x + (w + tw) * 0.5f, ty + h * 0.14f)
          close()
        }
        drawPath(roof, accent)
      }
      drawCircle(SuperFxGold.copy(alpha = pulse), radius = minOf(w, h) * 0.08f, center = Offset(x + w * 0.5f, y + h * 0.12f))
    }

    LandmarkSilhouette.VOLCANIC_RIDGE -> {
      // Twin volcanic caldera peaks with glowing magma rim
      val peak = Path().apply {
        moveTo(x + w * 0.08f, y + h * 0.90f)
        lineTo(x + w * 0.36f, y + h * 0.22f)
        lineTo(x + w * 0.64f, y + h * 0.22f)
        lineTo(x + w * 0.92f, y + h * 0.90f)
        close()
      }
      drawPath(peak, Color(0xFF451A03))
      drawLine(Color(0xFFF97316).copy(alpha = pulse), Offset(x + w * 0.34f, y + h * 0.24f), Offset(x + w * 0.66f, y + h * 0.24f), strokeWidth = 2.6f)
      drawCircle(SuperFxGold.copy(alpha = pulse), radius = minOf(w, h) * 0.10f, center = Offset(x + w * 0.5f, y + h * 0.18f))
    }

    LandmarkSilhouette.CRYSTAL_SPIRE, LandmarkSilhouette.PRISM_TOWER, LandmarkSilhouette.INDIGO_SPIRE -> {
      // Faceted Crystal / Prism Tower silhouette
      val spire = Path().apply {
        moveTo(x + w * 0.50f, y + h * 0.06f)
        lineTo(x + w * 0.78f, y + h * 0.62f)
        lineTo(x + w * 0.66f, y + h * 0.92f)
        lineTo(x + w * 0.34f, y + h * 0.92f)
        lineTo(x + w * 0.22f, y + h * 0.62f)
        close()
      }
      drawPath(spire, accent.copy(alpha = 0.85f))
      drawPath(spire, Color.White.copy(alpha = 0.78f), style = Stroke(width = 1.3f))
      drawCircle(CyberCyan.copy(alpha = pulse), radius = minOf(w, h) * 0.11f, center = Offset(x + w * 0.5f, y + h * 0.44f))
    }

    LandmarkSilhouette.DESERT_MESA -> {
      // Terraced sandstone desert mesa
      drawRect(Color(0xFF92400E), Offset(x + w * 0.10f, y + h * 0.58f), Size(w * 0.80f, h * 0.34f))
      drawRect(Color(0xFFD97706), Offset(x + w * 0.20f, y + h * 0.32f), Size(w * 0.60f, h * 0.28f))
      drawRect(Color(0xFFFDE68A), Offset(x + w * 0.24f, y + h * 0.28f), Size(w * 0.52f, h * 0.08f))
    }

    LandmarkSilhouette.NEON_BRIDGE -> {
      // Neon twin suspension pylons + cables
      drawRect(Color(0xFF0F172A), Offset(x + w * 0.12f, y + h * 0.62f), Size(w * 0.76f, h * 0.26f))
      drawLine(CyberCyan, Offset(x + w * 0.24f, y + h * 0.14f), Offset(x + w * 0.24f, y + h * 0.88f), 2.4f)
      drawLine(CyberCyan, Offset(x + w * 0.76f, y + h * 0.14f), Offset(x + w * 0.76f, y + h * 0.88f), 2.4f)
      drawLine(SuperFxGold.copy(alpha = pulse), Offset(x + w * 0.24f, y + h * 0.22f), Offset(x + w * 0.50f, y + h * 0.60f), 1.4f)
      drawLine(SuperFxGold.copy(alpha = pulse), Offset(x + w * 0.76f, y + h * 0.22f), Offset(x + w * 0.50f, y + h * 0.60f), 1.4f)
    }

    LandmarkSilhouette.CORAL_REEF -> {
      // Coral arch & sunlit sea altar
      drawArc(
        color = Color(0xFFFB7185),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(x + w * 0.14f, y + h * 0.22f),
        size = Size(w * 0.72f, h * 0.72f),
        style = Stroke(width = minOf(w, h) * 0.14f),
      )
      drawCircle(Color(0xFF38BDF8).copy(alpha = pulse), radius = minOf(w, h) * 0.14f, center = Offset(x + w * 0.5f, y + h * 0.58f))
    }

    LandmarkSilhouette.SNOW_PEAKS -> {
      // Twin alpine snow peaks with glacier caps
      val mtn = Path().apply {
        moveTo(x + w * 0.06f, y + h * 0.90f)
        lineTo(x + w * 0.38f, y + h * 0.16f)
        lineTo(x + w * 0.58f, y + h * 0.50f)
        lineTo(x + w * 0.76f, y + h * 0.26f)
        lineTo(x + w * 0.94f, y + h * 0.90f)
        close()
      }
      drawPath(mtn, Color(0xFF475569))
      val cap = Path().apply {
        moveTo(x + w * 0.38f, y + h * 0.16f)
        lineTo(x + w * 0.28f, y + h * 0.40f)
        lineTo(x + w * 0.48f, y + h * 0.40f)
        close()
      }
      drawPath(cap, Color(0xFFF8FAFC))
    }

    LandmarkSilhouette.DIMENSIONAL_RIFT -> {
      // Swirling Ultra-Distortion portal vortex
      drawCircle(Color(0xFFA855F7).copy(alpha = pulse), radius = minOf(w, h) * 0.36f, center = Offset(x + w * 0.5f, y + h * 0.5f), style = Stroke(2.2f))
      drawCircle(CyberCyan.copy(alpha = pulse), radius = minOf(w, h) * 0.22f, center = Offset(x + w * 0.5f, y + h * 0.5f), style = Stroke(1.8f))
      drawCircle(Color.White, radius = minOf(w, h) * 0.09f, center = Offset(x + w * 0.5f, y + h * 0.5f))
    }

    LandmarkSilhouette.ARENA_DOME -> {
      // Stadium dome with floodlight arc
      drawArc(
        color = accent,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(x + w * 0.10f, y + h * 0.22f),
        size = Size(w * 0.80f, h * 0.68f),
      )
      drawRect(Color(0xFFE2E8F0), Offset(x + w * 0.12f, y + h * 0.56f), Size(w * 0.76f, h * 0.32f))
      drawLine(SuperFxGold.copy(alpha = pulse), Offset(x + w * 0.22f, y + h * 0.22f), Offset(x + w * 0.78f, y + h * 0.22f), 1.8f)
    }

    LandmarkSilhouette.FEUDAL_CASTLE -> {
      // Tiered feudal castle keep + banner crest
      drawRect(Color(0xFF44403C), Offset(x + w * 0.14f, y + h * 0.58f), Size(w * 0.72f, h * 0.34f))
      drawRect(Color(0xFFF8FAFC), Offset(x + w * 0.24f, y + h * 0.34f), Size(w * 0.52f, h * 0.26f))
      drawRect(accent, Offset(x + w * 0.16f, y + h * 0.26f), Size(w * 0.68f, h * 0.12f))
      drawRect(SuperFxGold, Offset(x + w * 0.46f, y + h * 0.10f), Size(w * 0.08f, h * 0.16f))
    }

    LandmarkSilhouette.ILLUMINA_BLOOM -> {
      // Glowing bioluminescent Crystabloom petals
      for (i in 0..3) {
        val dx = if (i % 2 == 0) -0.18f else 0.18f
        val dy = if (i < 2) -0.18f else 0.18f
        drawCircle(Color(0xFF2DD4BF).copy(alpha = 0.75f * pulse), radius = minOf(w, h) * 0.18f, center = Offset(x + w * (0.5f + dx), y + h * (0.5f + dy)))
      }
      drawCircle(Color.White, radius = minOf(w, h) * 0.12f, center = Offset(x + w * 0.5f, y + h * 0.5f))
    }

    LandmarkSilhouette.ANCIENT_SHRINE, LandmarkSilhouette.RANGER_TOWER -> {
      // Twin shrine columns / Ranger Styler relay arch
      drawRect(accent, Offset(x + w * 0.12f, y + h * 0.18f), Size(w * 0.76f, h * 0.14f))
      drawRect(Color(0xFFE2E8F0), Offset(x + w * 0.22f, y + h * 0.32f), Size(w * 0.14f, h * 0.58f))
      drawRect(Color(0xFFE2E8F0), Offset(x + w * 0.64f, y + h * 0.32f), Size(w * 0.14f, h * 0.58f))
      drawCircle(SuperFxGold.copy(alpha = pulse), radius = minOf(w, h) * 0.10f, center = Offset(x + w * 0.5f, y + h * 0.46f))
    }
  }
}

/**
 * Atmospheric Day / Dusk / Night lighting pass over the AutoTileMapCanvas:
 * - DAY: crisp subtle top highlight
 * - DUSK: warm golden-amber horizon wash + violet twilight edge shadow
 * - NIGHT: deep midnight-indigo moonlight wash + glowing radial lantern/sanctuary/healing/warp blooms
 */
private fun DrawScope.drawTimeOfDayAtmospherePass(
  timeOfDay: TimeOfDayCycle,
  tiles: List<Int>,
  gridSize: Int,
  camCol: Int,
  camRow: Int,
  visibleCount: Int,
  cellW: Float,
  cellH: Float,
  animPhase: Float,
  regionAccent: Color,
) {
  when (timeOfDay) {
    TimeOfDayCycle.DAY -> Unit
    TimeOfDayCycle.DUSK -> {
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0x4AF97316), // warm golden-amber sky glow
            Color(0x28EC4899), // sunset magenta mid-haze
            Color(0x3D1E1B4B), // cool dusk ground shadow
          ),
        ),
        size = size,
      )
    }
    TimeOfDayCycle.NIGHT -> {
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0x78070B1E),
            Color(0x660F172A),
            Color(0x80090D24),
          ),
        ),
        size = size,
      )
      // Luminous blooms around Healing Plazas (8), Prism Sanctuaries (5), Landmarks (10), & Warp Gates (11)
      val pulse = 0.8f + 0.2f * sin((animPhase * 6.283f).toDouble()).toFloat()
      for (vr in 0 until visibleCount) {
        val r = camRow + vr
        for (vc in 0 until visibleCount) {
          val c = camCol + vc
          val t = tiles.getOrElse(r * gridSize + c) { 0 }
          val glowColor = when (t) {
            5 -> CyberCyan
            8 -> SgbEmerald
            10 -> SuperFxGold
            11 -> Color(0xFFC084FC)
            else -> null
          }
          if (glowColor != null) {
            val center = Offset((vc + 0.5f) * cellW, (vr + 0.5f) * cellH)
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.42f * pulse), Color.Transparent),
                center = center,
                radius = maxOf(cellW, cellH) * 1.15f,
              ),
              center = center,
              radius = maxOf(cellW, cellH) * 1.15f,
            )
          }
        }
      }
      // Subtle twinkling night stars along top edge
      for (i in 0..8) {
        val sx = ((i * 37 + 11) % 97) / 97f * size.width
        val sy = ((i * 19 + 5) % 24) / 100f * size.height
        val starAlpha = (0.35f + 0.45f * sin((animPhase * 6.283f + i * 1.4f).toDouble()).toFloat()).coerceIn(0.15f, 0.9f)
        drawCircle(regionAccent.copy(alpha = starAlpha), radius = 1.6f, center = Offset(sx, sy))
      }
    }
  }
}

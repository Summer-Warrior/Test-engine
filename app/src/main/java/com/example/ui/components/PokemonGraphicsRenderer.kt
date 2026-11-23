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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.NativePokemonEntity
import com.example.data.model.CharacterMixMode
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.engine.ConvergedVisualParams
import com.example.engine.HardwareConvergenceSolver
import com.example.ui.shaders.shovelKnightEntityEffect
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.SuperFxGold
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

enum class PokemonGraphicsStyle(val label: String) {
  HYBRID_FUSION("HD-2D Fusion"),
  SPRITE_CRISP("Gen V Sprite"),
  MODEL_3D_CEL("3D Cel Model"),
}

/**
 * Unified Shovel Knight+ 2D Sprite & 3D Cel-Shaded Model Character Renderer.
 *
 * Replaces multi-copy AsyncImage silhouette hacks with single-pass AGSL ShovelKnightOmniShader
 * and provides 4 non-muddy CharacterMixMode presentations (zero translucent double-exposure):
 *  1) SPLIT_BEAM_MORPH: Clean clipRect beam dividing crisp pixel sprite & 3D cel render
 *  2) VOXELIZER_FX_3D: Extruded beveled voxel slice stack (8 or 14 slices) for sprites & custom bitmaps
 *  3) HD2D_CRISP_SPRITE: Nearest-neighbor sprite with AGSL ink outline, cel ramp & rim light
 *  4) PIXEL_LOCKED_3D: 3D cel model snapped to the shared virtual pixel grid
 *
 * Honors ArcSys stepped character keyframes (~14 FPS default, 60 FPS when Fluid 60 FPS is enabled)
 * and Universal Game Feel 2-frame hit-flash.
 */
@Composable
fun PokemonCharacterRenderer(
  pokemon: NativePokemonEntity,
  style: PokemonGraphicsStyle,
  pixelTo3dBlend: Float,
  extrusionDepth: Float,
  size: Dp,
  modifier: Modifier = Modifier,
  mirrored: Boolean = false,
  mixMode: CharacterMixMode = when (style) {
    PokemonGraphicsStyle.SPRITE_CRISP -> CharacterMixMode.HD2D_CRISP_SPRITE
    PokemonGraphicsStyle.MODEL_3D_CEL -> CharacterMixMode.PIXEL_LOCKED_3D
    PokemonGraphicsStyle.HYBRID_FUSION -> CharacterMixMode.SPLIT_BEAM_MORPH
  },
  visuals: ConvergedVisualParams = remember {
    HardwareConvergenceSolver.solveVisuals(
      activePlatforms = emptyList(),
      disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
      extraPretty = ExtraVisualAudioSettings(),
    )
  },
  customSpriteBitmap: Bitmap? = null,
  hitFlash: Boolean = false,
  extraScaleX: Float = 1f,
  extraScaleY: Float = 1f,
) {
  val infinite = rememberInfiniteTransition(label = "pkmn_char_idle_${pokemon.dexNumber}")
  val rawCycleSec by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "cycle_sec",
  )

  // ArcSys stepped keyframe clock (14 FPS authentic sprite cadence unless 60 FPS Fluid Motion is on)
  val stepFps = visuals.characterStepFps.coerceIn(8, 60)
  val steppedTimeSec = if (stepFps >= 55) {
    rawCycleSec
  } else {
    floor(rawCycleSec * stepFps) / stepFps.toFloat()
  }

  val breathPhase = (0.5f + 0.5f * sin(steppedTimeSec * 2.1f)).coerceIn(0f, 1f)
  val orbitPhase = sin(steppedTimeSec * 1.05f)

  val primaryAccent = parseHexColorSafe(pokemon.primaryHex, CyberCyan)
  val density = LocalDensity.current
  val sizePx = with(density) { size.toPx() }
  val facingSign = if (mirrored) -1f else 1f

  val spriteScaleY = (1f - breathPhase * 0.045f) * extraScaleY
  val spriteScaleX = (1f + breathPhase * 0.032f) * facingSign * extraScaleX
  val floatOffsetY = (-breathPhase * 5.5f).dp
  val shadowScale = (1f - breathPhase * 0.14f).coerceIn(0.75f, 1f)
  val rotY = orbitPhase * (6f + extrusionDepth * 14f) * facingSign
  val camDist = 14f * density.density

  val customImageBitmap = remember(customSpriteBitmap) {
    customSpriteBitmap?.asImageBitmap()
  }

  val effectiveMixMode = when {
    customImageBitmap != null && mixMode == CharacterMixMode.SPLIT_BEAM_MORPH -> CharacterMixMode.VOXELIZER_FX_3D
    style == PokemonGraphicsStyle.SPRITE_CRISP && mixMode == CharacterMixMode.SPLIT_BEAM_MORPH -> CharacterMixMode.HD2D_CRISP_SPRITE
    style == PokemonGraphicsStyle.MODEL_3D_CEL && mixMode == CharacterMixMode.SPLIT_BEAM_MORPH -> CharacterMixMode.PIXEL_LOCKED_3D
    else -> mixMode
  }

  val entityShaderModifier = Modifier.shovelKnightEntityEffect(
    widthPx = sizePx,
    heightPx = sizePx,
    visuals = visuals,
    timeSec = steppedTimeSec,
    crystalGlint = (visuals.bloom * 0.6f).coerceIn(0.15f, 0.9f),
    hitFlash = hitFlash,
  )

  val api24HitFlashFilter = if (hitFlash) {
    ColorFilter.tint(Color.White, BlendMode.SrcAtop)
  } else {
    null
  }

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    // Grounded Elliptical Contact Shadow
    Canvas(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .size(width = size * 0.78f, height = size * 0.22f)
        .graphicsLayer {
          scaleX = shadowScale
          scaleY = shadowScale
        },
    ) {
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.Black.copy(alpha = 0.68f),
            primaryAccent.copy(alpha = 0.22f),
            Color.Transparent,
          ),
        ),
        topLeft = Offset.Zero,
        size = this.size,
      )
    }

    when (effectiveMixMode) {
      CharacterMixMode.HD2D_CRISP_SPRITE -> {
        Box(
          modifier = Modifier
            .size(size * 0.92f)
            .offset(y = floatOffsetY)
            .then(entityShaderModifier)
            .graphicsLayer {
              scaleX = spriteScaleX
              scaleY = spriteScaleY
            },
          contentAlignment = Alignment.Center,
        ) {
          if (customImageBitmap != null) {
            Image(
              bitmap = customImageBitmap,
              contentDescription = "${pokemon.name} Custom Pixel Sprite",
              filterQuality = FilterQuality.None,
              colorFilter = api24HitFlashFilter,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            AsyncImage(
              model = pokemon.pixelSpriteUrl,
              contentDescription = "${pokemon.name} Crisp HD-2D Sprite",
              filterQuality = FilterQuality.None,
              colorFilter = api24HitFlashFilter,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }

      CharacterMixMode.PIXEL_LOCKED_3D -> {
        Box(
          modifier = Modifier
            .size(size * 0.95f)
            .offset(y = floatOffsetY)
            .then(entityShaderModifier)
            .graphicsLayer {
              rotationY = rotY
              scaleX = facingSign * (0.96f + extrusionDepth * 0.06f) * extraScaleX
              scaleY = (0.96f + extrusionDepth * 0.06f - breathPhase * 0.025f) * extraScaleY
              cameraDistance = camDist
            },
          contentAlignment = Alignment.Center,
        ) {
          AsyncImage(
            model = pokemon.home3dRenderUrl,
            contentDescription = "${pokemon.name} Pixel-Locked 3D Cel Model",
            filterQuality = FilterQuality.High,
            colorFilter = api24HitFlashFilter,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      CharacterMixMode.VOXELIZER_FX_3D -> {
        val sliceCount = visuals.voxelSliceCount.coerceIn(6, 14)
        val depthSpreadDp = (extrusionDepth.coerceIn(0.2f, 1f) * 14f)
        val yawRad = orbitPhase * 0.55f * facingSign

        Box(
          modifier = Modifier
            .size(size * 0.92f)
            .offset(y = floatOffsetY)
            .graphicsLayer {
              rotationY = rotY * 1.35f
              cameraDistance = camDist
              scaleX = facingSign * extraScaleX
              scaleY = spriteScaleY
            },
          contentAlignment = Alignment.Center,
        ) {
          for (i in sliceCount - 1 downTo 1) {
            val t = i.toFloat() / sliceCount.toFloat()
            val shiftX = (yawRad * depthSpreadDp * t).dp
            val shiftY = (depthSpreadDp * 0.35f * t).dp
            val shade = (0.38f + (1f - t) * 0.42f).coerceIn(0.28f, 0.85f)
            val sliceTint = Color(
              red = (primaryAccent.red * 0.35f + 0.08f) * shade,
              green = (primaryAccent.green * 0.35f + 0.10f) * shade,
              blue = (primaryAccent.blue * 0.45f + 0.18f) * shade,
              alpha = 0.92f,
            )
            val sliceModifier = Modifier
              .fillMaxSize()
              .offset(x = shiftX, y = shiftY)

            if (customImageBitmap != null) {
              Image(
                bitmap = customImageBitmap,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(sliceTint, BlendMode.Modulate),
                contentScale = ContentScale.Fit,
                modifier = sliceModifier,
              )
            } else {
              AsyncImage(
                model = pokemon.pixelSpriteUrl,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(sliceTint, BlendMode.Modulate),
                contentScale = ContentScale.Fit,
                modifier = sliceModifier,
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .then(entityShaderModifier),
          ) {
            if (customImageBitmap != null) {
              Image(
                bitmap = customImageBitmap,
                contentDescription = "${pokemon.name} 3D Voxelized Custom Sprite",
                filterQuality = FilterQuality.None,
                colorFilter = api24HitFlashFilter,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              AsyncImage(
                model = pokemon.pixelSpriteUrl,
                contentDescription = "${pokemon.name} 3D Voxelized Sprite",
                filterQuality = FilterQuality.None,
                colorFilter = api24HitFlashFilter,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }
      }

      CharacterMixMode.SPLIT_BEAM_MORPH -> {
        val splitFraction = (1f - pixelTo3dBlend).coerceIn(0.06f, 0.94f)
        Box(
          modifier = Modifier
            .size(size * 0.94f)
            .offset(y = floatOffsetY),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = 0f, top = 0f, right = this.size.width * splitFraction, bottom = this.size.height) {
                  this@drawWithContent.drawContent()
                }
              }
              .then(entityShaderModifier)
              .graphicsLayer {
                scaleX = spriteScaleX
                scaleY = spriteScaleY
              },
            contentAlignment = Alignment.Center,
          ) {
            AsyncImage(
              model = pokemon.pixelSpriteUrl,
              contentDescription = "${pokemon.name} Split-Beam 2D Sprite",
              filterQuality = FilterQuality.None,
              colorFilter = api24HitFlashFilter,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = this.size.width * splitFraction, top = 0f, right = this.size.width, bottom = this.size.height) {
                  this@drawWithContent.drawContent()
                }
              }
              .then(entityShaderModifier)
              .graphicsLayer {
                rotationY = rotY
                scaleX = facingSign * (0.96f + extrusionDepth * 0.06f) * extraScaleX
                scaleY = (0.96f + extrusionDepth * 0.06f - breathPhase * 0.025f) * extraScaleY
                cameraDistance = camDist
              },
            contentAlignment = Alignment.Center,
          ) {
            AsyncImage(
              model = pokemon.home3dRenderUrl,
              contentDescription = "${pokemon.name} Split-Beam 3D Model",
              filterQuality = FilterQuality.High,
              colorFilter = api24HitFlashFilter,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }

          Canvas(modifier = Modifier.fillMaxSize()) {
            val beamX = this.size.width * splitFraction
            drawLine(
              brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.55f), SuperFxGold, CyberCyan.copy(alpha = 0.55f), Color.Transparent),
              ),
              start = Offset(beamX, this.size.height * 0.08f),
              end = Offset(beamX, this.size.height * 0.92f),
              strokeWidth = 6f,
            )
            drawLine(
              color = Color.White,
              start = Offset(beamX, this.size.height * 0.14f),
              end = Offset(beamX, this.size.height * 0.86f),
              strokeWidth = 1.8f,
            )
          }
        }
      }
    }
  }
}

fun DrawScope.drawPokemonBattlePlatform(
  center: Offset,
  widthPx: Float,
  heightPx: Float,
  primaryAccent: Color,
  secondaryAccent: Color,
  pulsePhase: Float,
) {
  val topLeft = Offset(center.x - widthPx / 2f, center.y - heightPx / 2f)
  val ovalSize = Size(widthPx, heightPx)

  drawOval(
    brush = Brush.radialGradient(
      colors = listOf(
        primaryAccent.copy(alpha = 0.35f),
        secondaryAccent.copy(alpha = 0.14f),
        Color.Transparent,
      ),
      center = center,
      radius = widthPx * 0.65f,
    ),
    topLeft = Offset(center.x - widthPx * 0.65f, center.y - heightPx * 0.8f),
    size = Size(widthPx * 1.3f, heightPx * 1.6f),
  )

  drawOval(
    color = Color(0xFF0A1220),
    topLeft = topLeft.copy(y = topLeft.y + 6f),
    size = ovalSize,
  )
  drawOval(
    color = primaryAccent.copy(alpha = 0.45f),
    topLeft = topLeft.copy(y = topLeft.y + 6f),
    size = ovalSize,
    style = Stroke(width = 2.5f),
  )

  drawOval(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF1A2942),
        Color(0xFF0F1828),
      ),
      startY = topLeft.y,
      endY = topLeft.y + heightPx,
    ),
    topLeft = topLeft,
    size = ovalSize,
  )

  drawOval(
    color = primaryAccent.copy(alpha = 0.7f + pulsePhase * 0.25f),
    topLeft = topLeft,
    size = ovalSize,
    style = Stroke(width = 2.2f),
  )
  val innerScale = 0.62f
  drawOval(
    color = secondaryAccent.copy(alpha = 0.55f),
    topLeft = Offset(center.x - (widthPx * innerScale) / 2f, center.y - (heightPx * innerScale) / 2f),
    size = Size(widthPx * innerScale, heightPx * innerScale),
    style = Stroke(width = 1.4f),
  )
}

@Composable
fun PokemonClassicStatusHud(
  name: String,
  level: Int,
  primaryType: String,
  secondaryType: String,
  currentHp: Int,
  maxHp: Int,
  accentColor: Color,
  gimmickLabel: String? = null,
  statusLabel: String? = null,
  showNumericHp: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val hpFraction = (currentHp.toFloat() / max(1, maxHp).toFloat()).coerceIn(0f, 1f)
  val hpColor = when {
    hpFraction > 0.5f -> SgbEmerald
    hpFraction > 0.2f -> SuperFxGold
    else -> ElectricCrimson
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp))
      .background(Color(0xEE101829))
      .border(
        width = 2.dp,
        color = accentColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
      )
      .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = StudioTextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Lv.$level",
        fontFamily = PressStart2PFamily,
        style = MaterialTheme.typography.labelSmall,
        color = SuperFxGold,
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = primaryType.uppercase() + if (secondaryType.isNotBlank()) "/${secondaryType.uppercase()}" else "",
        style = MaterialTheme.typography.labelSmall,
        color = CyberCyan,
        fontWeight = FontWeight.Bold,
      )
      if (!statusLabel.isNullOrBlank()) {
        Text(
          text = "[$statusLabel]",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricCrimson,
          fontWeight = FontWeight.Bold,
        )
      }
      if (!gimmickLabel.isNullOrBlank() && gimmickLabel != "None") {
        Text(
          text = "• $gimmickLabel",
          style = MaterialTheme.typography.labelSmall,
          color = SuperFxGold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF070B12))
        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = "HP",
        fontFamily = PressStart2PFamily,
        style = MaterialTheme.typography.labelSmall,
        color = SuperFxGold,
      )
      LinearProgressIndicator(
        progress = { hpFraction },
        color = hpColor,
        trackColor = Color(0xFF1E293B),
        modifier = Modifier
          .weight(1f)
          .height(7.dp)
          .clip(RoundedCornerShape(3.dp)),
      )
    }

    if (showNumericHp) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "EXP",
          style = MaterialTheme.typography.labelSmall,
          color = Mode7Sky,
        )
        Text(
          text = "$currentHp / $maxHp",
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextPrimary,
        )
      }
    }
  }
}

fun DrawScope.drawPokemonMapTile(tileType: Int) {
  val w = size.width
  val h = size.height
  when (tileType) {
    0 -> {
      // 0 = Meadow Plains
      drawRect(Color(0xFF1C703D))
      drawRect(color = Color(0xFF228248), topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
      drawCircle(Color(0xFFFDE047), radius = 2.4f, center = Offset(w * 0.65f, h * 0.35f))
    }
    1 -> {
      // 1 = Tall Wild Grass
      drawRect(Color(0xFF0E4425))
      drawRect(color = Color(0xFF14532D), topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
      val tufts = listOf(Offset(w * 0.25f, h * 0.76f), Offset(w * 0.55f, h * 0.70f), Offset(w * 0.78f, h * 0.80f))
      tufts.forEach { base ->
        val path = Path().apply {
          moveTo(base.x - 4f, base.y)
          lineTo(base.x, base.y - h * 0.38f)
          lineTo(base.x + 4f, base.y)
          close()
        }
        drawPath(path, color = Color(0xFF4ADE80))
      }
    }
    2 -> {
      // 2 = Water / Coast
      drawRect(Color(0xFF0369A1))
      drawLine(Color(0xFF38BDF8), Offset(w * 0.15f, h * 0.32f), Offset(w * 0.55f, h * 0.32f), strokeWidth = 2.5f)
      drawLine(Color(0xFFBAE6FD), Offset(w * 0.42f, h * 0.68f), Offset(w * 0.85f, h * 0.68f), strokeWidth = 2.5f)
    }
    3 -> {
      // 3 = Route Road
      drawRect(Color(0xFF334155))
      drawRect(color = Color(0xFF475569), topLeft = Offset(2f, 2f), size = Size(w * 0.44f, h * 0.44f))
      drawRect(color = Color(0xFF475569), topLeft = Offset(w * 0.52f, h * 0.50f), size = Size(w * 0.44f, h * 0.44f))
      drawLine(SuperFxGold.copy(alpha = 0.7f), Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), strokeWidth = 1.5f)
    }
    4 -> {
      // 4 = Volcanic Rift
      drawRect(Color(0xFF7F1D1D))
      drawCircle(Color(0xFFF97316), radius = minOf(w, h) * 0.28f, center = Offset(w * 0.5f, h * 0.5f))
      drawCircle(Color(0xFFFACC15), radius = minOf(w, h) * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
    }
    else -> {
      // 5 = Prism Sanctuary / Gym Arena
      drawRect(Color(0xFF1E1B4B))
      drawRect(Color(0xFF312E81), Offset(0f, 0f), Size(w / 2f, h / 2f))
      drawRect(Color(0xFF312E81), Offset(w / 2f, h / 2f), Size(w / 2f, h / 2f))
      val diamond = Path().apply {
        moveTo(w * 0.5f, h * 0.12f)
        lineTo(w * 0.84f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.88f)
        lineTo(w * 0.16f, h * 0.5f)
        close()
      }
      drawPath(diamond, color = CyberCyan.copy(alpha = 0.55f))
      drawPath(diamond, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.8f))
    }
  }
}

fun parseHexColorSafe(hex: String, fallback: Color = CyberCyan): Color {
  val clean = hex.trim().removePrefix("#")
  return runCatching {
    when (clean.length) {
      6 -> Color(("FF$clean").toLong(16))
      8 -> Color(clean.toLong(16))
      else -> fallback
    }
  }.getOrDefault(fallback)
}

@Composable
fun PokemonBattleStatusHud(
  name: String,
  level: Int,
  primaryType: String,
  secondaryType: String = "",
  currentHp: Int,
  maxHp: Int,
  showNumericHp: Boolean,
  statusLabel: String? = null,
  gimmickLabel: String? = null,
  modifier: Modifier = Modifier,
) {
  val safeMax = max(1, maxHp)
  val hpRatio = (currentHp.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
  val hpColor = when {
    hpRatio > 0.5f -> SgbEmerald
    hpRatio > 0.22f -> SuperFxGold
    else -> ElectricCrimson
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xD9090E1C))
      .border(1.2.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
      .padding(horizontal = 9.dp, vertical = 7.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = StudioTextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Lv.$level",
        fontFamily = PressStart2PFamily,
        style = MaterialTheme.typography.labelSmall,
        color = SuperFxGold,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val typeText = if (secondaryType.isNotBlank()) "$primaryType/$secondaryType" else primaryType
      Text(
        text = typeText.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = CyberCyan,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (!statusLabel.isNullOrBlank()) {
        Text(
          text = statusLabel,
          style = MaterialTheme.typography.labelSmall,
          color = SuperFxGold,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }

    LinearProgressIndicator(
      progress = { hpRatio },
      modifier = Modifier
        .fillMaxWidth()
        .height(7.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = hpColor,
      trackColor = Color(0xFF1E293B),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (showNumericHp) "HP $currentHp / $safeMax" else "HP ${(hpRatio * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = hpColor,
        fontWeight = FontWeight.Bold,
      )
      if (!gimmickLabel.isNullOrBlank()) {
        Text(
          text = gimmickLabel,
          style = MaterialTheme.typography.labelSmall,
          color = Mode7Sky,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}


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
import com.example.engine.ConvergedAudioParams
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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

enum class PokemonGraphicsStyle(val label: String) {
  HYBRID_FUSION("HD-2D Fusion"),
  SPRITE_CRISP("Gen V Sprite"),
  MODEL_3D_CEL("3D Cel Model"),
}

/**
 * Shared 60fps Multi-Hardware Stage Backdrop used identically by BOTH
 * OmniConsoleFusionViewport AND GameTestSection Battle Arena.
 * Reacts live to active hardware platforms, discipline mode, and Extra Pretty toggles.
 */
@Composable
fun MultiHardwareBattleStageBackdrop(
  primaryColor: Color,
  secondaryColor: Color,
  visuals: ConvergedVisualParams,
  audio: ConvergedAudioParams,
  activePlatformIds: Set<String>,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "multi_hw_stage_60fps")
  val orbitDeg by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 10000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "stage_orbit_deg",
  )
  val pulsePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "stage_pulse_phase",
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.50f
    val horizonY = h * (0.50f - visuals.affineWarp * 0.12f)

    // 1. N64 / HD-2D Volumetric Spotlights & Sunbeams
    if (visuals.volumetricRaysEnabled || "n64_rcp" in activePlatformIds) {
      for (i in 0..2) {
        val x0 = w * (0.14f + i * 0.34f) + sin((pulsePhase * 6.283f + i).toDouble()).toFloat() * 16f
        val beam = Path().apply {
          moveTo(x0, 0f)
          lineTo(x0 + w * 0.14f, 0f)
          lineTo(x0 - w * 0.06f, h)
          lineTo(x0 - w * 0.22f, h)
          close()
        }
        drawPath(
          path = beam,
          brush = Brush.verticalGradient(
            listOf(primaryColor.copy(alpha = 0.16f), SuperFxGold.copy(alpha = 0.06f), Color.Transparent)
          ),
        )
      }
    }

    // 2. SNES Mode-7 / GBA Affine Perspective Floor
    val gridLines = 7
    for (i in 0..gridLines) {
      val t = i.toFloat() / gridLines
      val y = horizonY + (h - horizonY) * (t * t)
      drawLine(
        color = primaryColor.copy(alpha = 0.16f + t * 0.26f),
        start = Offset(0f, y),
        end = Offset(w, y),
        strokeWidth = 1.2f,
      )
    }
    for (i in -4..4) {
      val bottomX = cx + i * (w * 0.20f)
      drawLine(
        color = secondaryColor.copy(alpha = 0.22f),
        start = Offset(cx + i * 16f, horizonY),
        end = Offset(bottomX, h),
        strokeWidth = 1.1f,
      )
    }

    // 3. GameCube TEV / Switch Rim Aura Halo
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primaryColor.copy(alpha = 0.24f + visuals.bloom * 0.22f),
          secondaryColor.copy(alpha = 0.10f),
          Color.Transparent,
        ),
        center = Offset(cx, cy),
        radius = minOf(w, h) * 0.46f,
      ),
      radius = minOf(w, h) * 0.46f,
      center = Offset(cx, cy),
    )

    // 4. Super FX2 / Capcom CX4 Rotating Polygonal Crystal Ring
    val ringRx = w * 0.30f
    val ringRy = h * 0.11f
    val ringCenterY = h * 0.72f
    val sides = 8
    val polyPath = Path()
    for (s in 0 until sides) {
      val angle = Math.toRadians((orbitDeg + s * (360.0 / sides)))
      val px = cx + cos(angle).toFloat() * ringRx
      val py = ringCenterY + sin(angle).toFloat() * ringRy
      if (s == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
      drawCircle(SuperFxGold.copy(alpha = 0.78f), radius = 2.6f, center = Offset(px, py))
    }
    polyPath.close()
    drawPath(polyPath, color = CyberCyan.copy(alpha = 0.65f), style = Stroke(width = 1.6f))

    // 5. Sega 32X / Saturn VDP1 Geometric Mesh Arc
    if ("sega_32x" in activePlatformIds || "sega_saturn" in activePlatformIds || visuals.voxelDepth > 0.55f) {
      val arcRad = Math.toRadians(orbitDeg.toDouble())
      val ox = cx + cos(arcRad).toFloat() * (w * 0.24f)
      val oy = cy - h * 0.14f + sin(arcRad * 2.0).toFloat() * 14f
      drawCircle(secondaryColor.copy(alpha = 0.55f), radius = 10f, center = Offset(ox, oy), style = Stroke(1.8f))
    }

    // 6. MSU-1 / Omni-Audio 6-Lane Spectrum Visualizer Bars (bottom left)
    val lanes = listOf(
      audio.pulseLeadGain,
      audio.triangleBassGain,
      audio.fmSlapBassGain,
      audio.sidArpGain,
      audio.spcEchoFeedback,
      audio.orchestralPadGain,
    )
    lanes.forEachIndexed { idx, gain ->
      val bounce = 0.55f + 0.45f * sin((pulsePhase * 6.283f + idx * 0.9f).toDouble()).toFloat()
      val barH = (10f + gain * 34f * bounce).coerceAtMost(42f)
      drawRect(
        color = if (idx % 2 == 0) CyberCyan.copy(alpha = 0.68f) else SuperFxGold.copy(alpha = 0.68f),
        topLeft = Offset(12f + idx * 8f, h - 10f - barH),
        size = Size(4.5f, barH),
      )
    }

    // 7. Extra Pretty Dynamic Weather / Sparkle Particles
    if (visuals.weatherParticlesEnabled) {
      for (i in 0 until 16) {
        val px = (((i * 61) % 100) / 100f + pulsePhase * 0.25f) % 1f * w
        val py = (((i * 43) % 100) / 100f + pulsePhase * 0.55f) % 1f * h
        drawCircle(Color.White.copy(alpha = 0.65f), radius = 2.2f, center = Offset(px, py))
      }
    }

    // 8. Foreground Parallax Silhouette Framing
    if (visuals.foregroundParallaxEnabled) {
      val shift = sin(pulsePhase * 6.283).toFloat() * 6f
      val leftPillar = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.08f + shift, 0f)
        lineTo(w * 0.04f + shift, h)
        lineTo(0f, h)
        close()
      }
      val rightPillar = Path().apply {
        moveTo(w, 0f)
        lineTo(w * 0.92f - shift, 0f)
        lineTo(w * 0.96f - shift, h)
        lineTo(w, h)
        close()
      }
      drawPath(leftPillar, Color(0xFF050811).copy(alpha = 0.78f))
      drawPath(rightPillar, Color(0xFF050811).copy(alpha = 0.78f))
    }
  }
}

/**
 * Unified Shovel Knight+ 2D Sprite & 3D Cel-Shaded Model Character Renderer.
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

/**
 * Miniature preview glyph renderer for all 12 custom terrain & landmark tiles (0..11).
 */
fun DrawScope.drawPokemonMapTile(tileType: Int) {
  val w = size.width
  val h = size.height
  when (tileType) {
    0 -> {
      drawRect(Color(0xFF1C703D))
      drawRect(color = Color(0xFF228248), topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
      drawCircle(Color(0xFFFDE047), radius = 2.4f, center = Offset(w * 0.65f, h * 0.35f))
    }
    1 -> {
      drawRect(Color(0xFF0E4425))
      drawRect(color = Color(0xFF14532D), topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
      val tufts = listOf(Offset(w * 0.25f, h * 0.76f), Offset(w * 0.55f, h * 0.70f), Offset(w * 0.78f, h * 0.80f))
      tufts.forEach { base ->
        val path = Path().apply {
          moveTo(base.x - 3f, base.y)
          lineTo(base.x, base.y - h * 0.38f)
          lineTo(base.x + 3f, base.y)
          close()
        }
        drawPath(path, color = Color(0xFF4ADE80))
      }
    }
    2 -> {
      drawRect(Color(0xFF0369A1))
      drawLine(Color(0xFF38BDF8), Offset(w * 0.15f, h * 0.32f), Offset(w * 0.55f, h * 0.32f), strokeWidth = 2.2f)
      drawLine(Color(0xFFBAE6FD), Offset(w * 0.42f, h * 0.68f), Offset(w * 0.85f, h * 0.68f), strokeWidth = 2.2f)
    }
    3 -> {
      drawRect(Color(0xFF334155))
      drawRect(color = Color(0xFF475569), topLeft = Offset(2f, 2f), size = Size(w * 0.44f, h * 0.44f))
      drawRect(color = Color(0xFF475569), topLeft = Offset(w * 0.52f, h * 0.50f), size = Size(w * 0.44f, h * 0.44f))
      drawLine(SuperFxGold.copy(alpha = 0.7f), Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), strokeWidth = 1.5f)
    }
    4 -> {
      drawRect(Color(0xFF7F1D1D))
      drawCircle(Color(0xFFF97316), radius = minOf(w, h) * 0.28f, center = Offset(w * 0.5f, h * 0.5f))
      drawCircle(Color(0xFFFACC15), radius = minOf(w, h) * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
    }
    5 -> {
      drawRect(Color(0xFF1E1B4B))
      val diamond = Path().apply {
        moveTo(w * 0.5f, h * 0.12f)
        lineTo(w * 0.84f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.88f)
        lineTo(w * 0.16f, h * 0.5f)
        close()
      }
      drawPath(diamond, color = CyberCyan.copy(alpha = 0.65f))
      drawPath(diamond, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.5f))
    }
    6 -> {
      // 6 = Forest Canopy / Cliff Wall
      drawRect(Color(0xFF0B3B22))
      drawCircle(Color(0xFF166534), radius = minOf(w, h) * 0.38f, center = Offset(w * 0.5f, h * 0.48f))
      drawCircle(Color(0xFF22C55E), radius = minOf(w, h) * 0.20f, center = Offset(w * 0.42f, h * 0.38f))
    }
    7 -> {
      // 7 = Wooden Bridge / Pier
      drawRect(Color(0xFF0369A1))
      drawRect(Color(0xFF92400E), Offset(w * 0.1f, h * 0.12f), Size(w * 0.8f, h * 0.76f))
      drawLine(Color(0xFFD97706), Offset(w * 0.15f, h * 0.35f), Offset(w * 0.85f, h * 0.35f), 1.4f)
      drawLine(Color(0xFFD97706), Offset(w * 0.15f, h * 0.65f), Offset(w * 0.85f, h * 0.65f), 1.4f)
    }
    8 -> {
      // 8 = Town Plaza / Healing Pad
      drawRect(Color(0xFF1E293B))
      drawRect(Color(0xFF10B981), Offset(w * 0.40f, h * 0.18f), Size(w * 0.20f, h * 0.64f))
      drawRect(Color(0xFF10B981), Offset(w * 0.18f, h * 0.40f), Size(w * 0.64f, h * 0.20f))
    }
    9 -> {
      // 9 = Sand / Snow / Wild Biome
      drawRect(Color(0xFFD9A95B))
      drawCircle(Color(0xFFFDE68A), radius = 2.2f, center = Offset(w * 0.35f, h * 0.35f))
      drawCircle(Color(0xFFB45309), radius = 2.0f, center = Offset(w * 0.70f, h * 0.68f))
    }
    10 -> {
      // 10 = Landmark Roof / Building
      drawRect(Color(0xFF334155))
      drawRect(Color(0xFFDC2626), Offset(w * 0.10f, h * 0.14f), Size(w * 0.80f, h * 0.44f))
      drawRect(Color(0xFF38BDF8), Offset(w * 0.34f, h * 0.64f), Size(w * 0.32f, h * 0.24f))
    }
    else -> {
      // 11 = Region Warp Gate
      drawRect(Color(0xFF1E1B4B))
      drawCircle(Color(0xFFA855F7), radius = minOf(w, h) * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(2.2f))
      drawCircle(CyberCyan, radius = minOf(w, h) * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
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

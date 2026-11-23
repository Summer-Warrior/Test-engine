package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.NativePokemonEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class PokemonGraphicsStyle(val label: String) {
  HYBRID_FUSION("HD-2D Fusion"),
  SPRITE_CRISP("Gen V Sprite"),
  MODEL_3D_CEL("3D Cel Model"),
}

/**
 * High-fidelity Pokémon Sprite & 3D Cel-Shaded Model Character Renderer.
 * - Sprite mode uses FilterQuality.None (zero blur) + Gen V squash-and-stretch idle breathing
 *   and cast dark silhouette shadow.
 * - 3D Model mode uses HOME/Stadium renders with multi-angle cel ink outline pass, specular rim glow,
 *   camera perspective orbit, and grounded contact shadow.
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
) {
  val infinite = rememberInfiniteTransition(label = "pkmn_char_idle_${pokemon.dexNumber}")
  val breathPhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "breath",
  )
  val orbitPhase by infinite.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "orbit",
  )

  val primaryAccent = parseHexColorSafe(pokemon.primaryHex, CyberCyan)
  val density = LocalDensity.current.density
  val facingSign = if (mirrored) -1f else 1f
  val spriteAlpha = when (style) {
    PokemonGraphicsStyle.SPRITE_CRISP -> 1f
    PokemonGraphicsStyle.MODEL_3D_CEL -> 0f
    PokemonGraphicsStyle.HYBRID_FUSION -> (1f - pixelTo3dBlend * 0.72f).coerceIn(0f, 1f)
  }
  val modelAlpha = when (style) {
    PokemonGraphicsStyle.SPRITE_CRISP -> 0f
    PokemonGraphicsStyle.MODEL_3D_CEL -> 1f
    PokemonGraphicsStyle.HYBRID_FUSION -> pixelTo3dBlend.coerceIn(0.08f, 1f)
  }

  val spriteScaleY = 1f - breathPhase * 0.045f
  val spriteScaleX = (1f + breathPhase * 0.035f) * facingSign
  val floatOffsetY = (-breathPhase * 6f).dp
  val shadowScale = (1f - breathPhase * 0.14f).coerceIn(0.75f, 1f)

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    // Grounded Elliptical Stadium Contact Shadow
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
            Color.Black.copy(alpha = 0.65f),
            primaryAccent.copy(alpha = 0.2f),
            Color.Transparent,
          ),
        ),
        topLeft = Offset.Zero,
        size = this.size,
      )
    }

    // Layer A: Crisp Gen III–V Nearest-Neighbor Animated Pixel Sprite Pass
    if (spriteAlpha > 0.03f) {
      // Crisp Cast Pixel Silhouette Echo (gives classic DS/HD-2D pop)
      AsyncImage(
        model = pokemon.pixelSpriteUrl,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        colorFilter = ColorFilter.tint(Color(0xFF04070E), BlendMode.SrcIn),
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(size * 0.9f)
          .offset(x = (3f * extrusionDepth).dp, y = floatOffsetY + 4.dp)
          .graphicsLayer {
            alpha = spriteAlpha * 0.55f
            scaleX = spriteScaleX
            scaleY = spriteScaleY
          },
      )

      // Primary Pixel-Art Sprite (Zero blur via FilterQuality.None)
      AsyncImage(
        model = pokemon.pixelSpriteUrl,
        contentDescription = "${pokemon.name} Pixel Sprite",
        filterQuality = FilterQuality.None,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(size * 0.9f)
          .offset(y = floatOffsetY)
          .graphicsLayer {
            alpha = spriteAlpha
            scaleX = spriteScaleX
            scaleY = spriteScaleY
          },
      )
    }

    // Layer B: 3D Cel-Shaded Model Pass (PICA200 Ink Outline + Specular Rim + 3D Perspective Orbit)
    if (modelAlpha > 0.03f) {
      val rotY = orbitPhase * (4f + extrusionDepth * 7f) * facingSign
      val camDist = 14f * density

      // Dark 3D Cel-Shaded Silhouette Ink Outline Pass (3DS / Switch anime toon contour)
      val outlineOffsets = listOf(
        (-2).dp to 0.dp,
        2.dp to 0.dp,
        0.dp to (-2).dp,
        0.dp to 2.dp,
      )
      outlineOffsets.forEach { (ox, oy) ->
        AsyncImage(
          model = pokemon.home3dRenderUrl,
          contentDescription = null,
          filterQuality = FilterQuality.High,
          colorFilter = ColorFilter.tint(Color(0xFF070B14), BlendMode.SrcIn),
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .size(size * 0.94f)
            .offset(x = ox, y = floatOffsetY + oy)
            .graphicsLayer {
              alpha = modelAlpha * 0.85f
              rotationY = rotY
              scaleX = facingSign * (0.96f + extrusionDepth * 0.06f)
              scaleY = 0.96f + extrusionDepth * 0.06f - breathPhase * 0.025f
              cameraDistance = camDist
            },
        )
      }

      // Main 3D PBR/Cel Render
      AsyncImage(
        model = pokemon.home3dRenderUrl,
        contentDescription = "${pokemon.name} 3D Model",
        filterQuality = FilterQuality.High,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(size * 0.94f)
          .offset(y = floatOffsetY)
          .graphicsLayer {
            alpha = modelAlpha
            rotationY = rotY
            scaleX = facingSign * (0.96f + extrusionDepth * 0.06f)
            scaleY = 0.96f + extrusionDepth * 0.06f - breathPhase * 0.025f
            cameraDistance = camDist
          },
      )

      // Subtle Elemental Rim-Light Aura Overlay matching Pokémon Type
      AsyncImage(
        model = pokemon.home3dRenderUrl,
        contentDescription = null,
        filterQuality = FilterQuality.Medium,
        colorFilter = ColorFilter.tint(primaryAccent.copy(alpha = 0.25f), BlendMode.SrcAtop),
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(size * 0.94f)
          .offset(y = floatOffsetY)
          .graphicsLayer {
            alpha = modelAlpha * 0.35f
            rotationY = rotY
            scaleX = facingSign * (0.96f + extrusionDepth * 0.06f)
            scaleY = 0.96f + extrusionDepth * 0.06f - breathPhase * 0.025f
            cameraDistance = camDist
          },
      )
    }
  }
}

/**
 * Draws an angled elliptical Pokémon Battle Platform disc (authentic RPG battle ground pad)
 * with volumetric stadium glow rings.
 */
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

  // Ground Glow Halo
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

  // Raised 3D Platform Rim Edge (gives depth like DS/3DS/Switch battle discs)
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

  // Main Top Arena Disc Surface
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

  // Inner Pokéball/Stadium Concentric Ring
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
 * Classic Pokémon RPG Battle Status HUD Plate with Name, Level, Type Pill, HP Bar, and EXP Strip.
 */
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

    // Authentic Beveled HP Frame: [HP | ████████]
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

/**
 * Draws authentic GBA/NDS pixel-art terrain tiles via Canvas so overworld maps look
 * rich and unmistakable as a Pokémon RPG tilemap.
 */
fun DrawScope.drawPokemonMapTile(tileType: Int) {
  val w = size.width
  val h = size.height
  when (tileType) {
    0 -> {
      // SGB2/GBA Tall Wild Grass: layered green turf + 3 distinct grass blade tufts
      drawRect(Color(0xFF14532D))
      drawRect(color = Color(0xFF166534), topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f))
      val tufts = listOf(Offset(w * 0.25f, h * 0.72f), Offset(w * 0.55f, h * 0.65f), Offset(w * 0.78f, h * 0.78f))
      tufts.forEach { base ->
        val path = Path().apply {
          moveTo(base.x - 4f, base.y)
          lineTo(base.x, base.y - h * 0.34f)
          lineTo(base.x + 4f, base.y)
          close()
        }
        drawPath(path, color = Color(0xFF4ADE80))
      }
    }
    1 -> {
      // Dolphin TEV Surfable Ocean Water: deep blue + crisp white/cyan wave crests
      drawRect(Color(0xFF0369A1))
      drawLine(Color(0xFF38BDF8), Offset(w * 0.15f, h * 0.32f), Offset(w * 0.55f, h * 0.32f), strokeWidth = 2.5f)
      drawLine(Color(0xFFBAE6FD), Offset(w * 0.42f, h * 0.68f), Offset(w * 0.85f, h * 0.68f), strokeWidth = 2.5f)
    }
    2 -> {
      // Mode-7 Cobble / Route Path: warm slate pavers + gold route marker dash
      drawRect(Color(0xFF334155))
      drawRect(color = Color(0xFF475569), topLeft = Offset(2f, 2f), size = Size(w * 0.44f, h * 0.44f))
      drawRect(color = Color(0xFF475569), topLeft = Offset(w * 0.52f, h * 0.50f), size = Size(w * 0.44f, h * 0.44f))
      drawLine(SuperFxGold.copy(alpha = 0.7f), Offset(w * 0.15f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), strokeWidth = 1.5f)
    }
    3 -> {
      // NDS 3D Gym Checkerboard + Poké Ball Emblem
      drawRect(Color(0xFF312E81))
      drawRect(Color(0xFF4338CA), Offset(0f, 0f), Size(w / 2f, h / 2f))
      drawRect(Color(0xFF4338CA), Offset(w / 2f, h / 2f), Size(w / 2f, h / 2f))
      drawCircle(Color(0xFFEF4444), radius = minOf(w, h) * 0.24f, center = Offset(w / 2f, h / 2f))
      drawCircle(Color.White, radius = minOf(w, h) * 0.09f, center = Offset(w / 2f, h / 2f))
    }
    4 -> {
      // Super FX2 Magma Crater
      drawRect(Color(0xFF7F1D1D))
      drawCircle(Color(0xFFF97316), radius = minOf(w, h) * 0.28f, center = Offset(w * 0.5f, h * 0.5f))
      drawCircle(Color(0xFFFACC15), radius = minOf(w, h) * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
    }
    else -> {
      // Paldea Terastal Crystal Wall
      drawRect(Color(0xFF0F172A))
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

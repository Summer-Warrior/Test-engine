package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.NativePokemonEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.ConsoleHardwareCatalog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Omni-Console Sprite & 3D Model Fusion Viewport.
 * Simultaneously composites:
 * 1. SGB2 Custom Hardware Frame & 8-Bit/16-Bit/32-Bit Pixel Art Sprite Layer
 * 2. Mode-7 Affine Perspective Grid & NDS 2.5D Voxel Diorama Pedestal
 * 3. Super FX2 / Capcom CX4 / Sega 32X 3D Polygonal Wireframe & Vertex Ring Cage
 * 4. N64 Expansion Pak / GameCube TEV / Switch HD-2D Volumetric Bloom & CRT Scanline Pass
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmniConsoleFusionViewport(
  primaryPokemon: NativePokemonEntity,
  secondaryPokemon: NativePokemonEntity,
  config: ConsoleFusionConfig,
  onUpdateConfig: ((ConsoleFusionConfig) -> ConsoleFusionConfig) -> Unit,
  onToggleChip: (String) -> Unit,
  onPinToSidebar: (AssetCategory) -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "omni_fusion_anim")
  val rotationPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "wireframe_rotation",
  )
  val hoverBob by infiniteTransition.animateFloat(
    initialValue = -6f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "sprite_bob",
  )

  val primaryColor = parseHexColorSafe(primaryPokemon.primaryHex, CyberCyan)
  val secondaryColor = parseHexColorSafe(secondaryPokemon.primaryHex, ElectricCrimson)

  val hasSuperFx2 = "super_fx2" in config.activeChipIds
  val hasSgb2 = "sgb2_border" in config.activeChipIds
  val hasVoxelizer = "voxelizer_fx" in config.activeChipIds
  val has32x = "sega_32x" in config.activeChipIds
  val hasCx4 = "cx4" in config.activeChipIds
  val hasPokeCrtRtx = "pokecrt_rtx" in config.activeChipIds

  var renderStyle by remember { mutableStateOf(PokemonGraphicsStyle.HYBRID_FUSION) }
  var sideBySideCompare by remember { mutableStateOf(true) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Viewport Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(
          width = if (hasSgb2) 2.dp else 1.dp,
          brush = Brush.linearGradient(listOf(primaryColor, SuperFxGold, secondaryColor)),
          shape = RoundedCornerShape(20.dp),
        )
        .testTag("omni_console_fusion_viewport"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = StudioDeepNavy),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Top HUD Header inside Viewport
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "OMNI-CONSOLE COMPOSITOR // ${config.presetName.uppercase()}",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
            Text(
              text = "#${primaryPokemon.dexNumber} ${primaryPokemon.name} × #${secondaryPokemon.dexNumber} ${secondaryPokemon.name} Palette/Rig",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(SuperFxGold.copy(alpha = 0.16f))
              .border(1.dp, SuperFxGold, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text(
              text = "${config.activeChipIds.size} CHIPS ACTIVE",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Render Quality Mode Selector (Crisp Pixel Sprite / 3D Cel Model / HD-2D Fusion / Side-by-Side)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = sideBySideCompare,
            onClick = { sideBySideCompare = !sideBySideCompare },
            label = { Text("Dual Sprite ⇄ 3D Model", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SgbEmerald.copy(alpha = 0.24f),
              selectedLabelColor = SgbEmerald,
            ),
            modifier = Modifier.testTag("viewport_dual_compare_chip"),
          )
          PokemonGraphicsStyle.entries.forEach { mode ->
            FilterChip(
              selected = !sideBySideCompare && renderStyle == mode,
              onClick = {
                sideBySideCompare = false
                renderStyle = mode
              },
              label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("viewport_style_${mode.name.lowercase()}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Interactive Multi-System Shader Stage
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF060911))
            .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center,
        ) {
          // Layer 1: Custom Canvas rendering Mode-7 affine floor, 3D Voxel blocks, Super FX2 / CX4 3D wireframe polyhedron, HD-2D volumetric rays & SGB2 border
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // HD-2D Octopath-style Volumetric Backdrop Glow
            drawRect(
              brush = Brush.radialGradient(
                colors = listOf(
                  primaryColor.copy(alpha = 0.35f * config.hd2dVolumetricBloom),
                  secondaryColor.copy(alpha = 0.22f * config.hd2dVolumetricBloom),
                  Color(0xFF060911),
                ),
                center = Offset(cx, cy * 0.85f),
                radius = w * 0.65f,
              )
            )

            // Mode-7 Affine Perspective Floor Grid (SNES / GBA / NDS)
            val horizonY = h * (0.48f + (1f - config.mode7PerspectiveTilt) * 0.18f)
            drawLine(
              color = primaryColor.copy(alpha = 0.6f),
              start = Offset(0f, horizonY),
              end = Offset(w, horizonY),
              strokeWidth = 2f,
            )
            for (i in -8..8) {
              val topX = cx + i * (w * 0.03f)
              val bottomX = cx + i * (w * 0.16f)
              drawLine(
                color = primaryColor.copy(alpha = 0.28f),
                start = Offset(topX, horizonY),
                end = Offset(bottomX, h),
                strokeWidth = 1.5f,
              )
            }
            for (row in 1..6) {
              val t = row / 6f
              val y = horizonY + (h - horizonY) * (t * t)
              drawLine(
                color = secondaryColor.copy(alpha = 0.26f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f,
              )
            }

            // Authentic Pokémon Battle Arena Elliptical Stadium Platform(s)
            val pulsePhase = ((hoverBob + 6f) / 12f).coerceIn(0f, 1f)
            if (sideBySideCompare) {
              drawPokemonBattlePlatform(
                center = Offset(w * 0.29f, h * 0.72f),
                widthPx = w * 0.36f,
                heightPx = 38f,
                primaryAccent = primaryColor,
                secondaryAccent = CyberCyan,
                pulsePhase = pulsePhase,
              )
              drawPokemonBattlePlatform(
                center = Offset(w * 0.71f, h * 0.72f),
                widthPx = w * 0.36f,
                heightPx = 38f,
                primaryAccent = SuperFxGold,
                secondaryAccent = secondaryColor,
                pulsePhase = pulsePhase,
              )
            } else {
              drawPokemonBattlePlatform(
                center = Offset(cx, h * 0.73f),
                widthPx = w * 0.56f,
                heightPx = 44f,
                primaryAccent = primaryColor,
                secondaryAccent = secondaryColor,
                pulsePhase = pulsePhase,
              )
            }

            // Type-Colored Ambient Elemental Particles
            for (p in 0 until 8) {
              val ang = Math.toRadians((rotationPhase + p * 45.0))
              val orbitR = minOf(w, h) * 0.36f
              val px = cx + (cos(ang) * orbitR).toFloat()
              val py = cy * 0.78f + (sin(ang * 1.5) * orbitR * 0.35f).toFloat()
              drawCircle(
                color = if (p % 2 == 0) primaryColor.copy(alpha = 0.55f) else SuperFxGold.copy(alpha = 0.48f),
                radius = if (p % 2 == 0) 4.2f else 3f,
                center = Offset(px, py),
              )
            }

            // Voxelizer-FX 3D Isometric Voxel Pedestal (NDS / Saturn / Fan FPGA Core)
            if ((hasVoxelizer || config.voxelExtrusionDepth > 0.2f) && !sideBySideCompare) {
              val voxelDepth = 16f * config.voxelExtrusionDepth
              val pedW = w * 0.36f
              val pedH = 24f
              val pedTopY = h * 0.73f
              val topPath = Path().apply {
                moveTo(cx, pedTopY - pedH * 0.5f)
                lineTo(cx + pedW * 0.5f, pedTopY)
                lineTo(cx, pedTopY + pedH * 0.5f)
                lineTo(cx - pedW * 0.5f, pedTopY)
                close()
              }
              drawPath(topPath, color = primaryColor.copy(alpha = 0.28f))
              drawPath(topPath, color = CyberCyan.copy(alpha = 0.7f), style = Stroke(width = 1.6f))
              val leftPath = Path().apply {
                moveTo(cx - pedW * 0.5f, pedTopY)
                lineTo(cx, pedTopY + pedH * 0.5f)
                lineTo(cx, pedTopY + pedH * 0.5f + voxelDepth)
                lineTo(cx - pedW * 0.5f, pedTopY + voxelDepth)
                close()
              }
              drawPath(leftPath, color = primaryColor.copy(alpha = 0.45f))
              val rightPath = Path().apply {
                moveTo(cx + pedW * 0.5f, pedTopY)
                lineTo(cx, pedTopY + pedH * 0.5f)
                lineTo(cx, pedTopY + pedH * 0.5f + voxelDepth)
                lineTo(cx + pedW * 0.5f, pedTopY + voxelDepth)
                close()
              }
              drawPath(rightPath, color = secondaryColor.copy(alpha = 0.45f))
            }

            // Super FX2 / Capcom CX4 / Sega 32X Rotating 3D Polygonal Gyro-Cage
            if (hasSuperFx2 || hasCx4 || has32x) {
              val radius = minOf(w, h) * 0.33f
              val rad = Math.toRadians(rotationPhase.toDouble())
              val points = (0 until 6).map { idx ->
                val angle = rad + idx * (Math.PI / 3.0)
                val px = cx + (cos(angle) * radius).toFloat()
                val py = (cy * 0.88f) + (sin(angle) * radius * 0.45f).toFloat()
                Offset(px, py)
              }
              val apexTop = Offset(cx, cy * 0.88f - radius * 0.72f)
              val apexBottom = Offset(cx, cy * 0.88f + radius * 0.72f)

              points.forEachIndexed { i, pt ->
                val nextPt = points[(i + 1) % points.size]
                drawLine(
                  color = SuperFxGold.copy(alpha = 0.5f * config.pixelTo3dBlend),
                  start = pt,
                  end = nextPt,
                  strokeWidth = 1.6f,
                )
                drawLine(
                  color = CyberCyan.copy(alpha = 0.35f * config.pixelTo3dBlend),
                  start = apexTop,
                  end = pt,
                  strokeWidth = 1.2f,
                )
                drawLine(
                  color = ElectricCrimson.copy(alpha = 0.35f * config.pixelTo3dBlend),
                  start = apexBottom,
                  end = pt,
                  strokeWidth = 1.2f,
                )
              }
            }

            // Super Game Boy 2 (SGB2) Ornamental Corner Frame
            if (hasSgb2) {
              val cornerLen = 26f
              val pad = 10f
              val frameColor = SuperFxGold.copy(alpha = 0.85f)
              // Top-Left
              drawLine(frameColor, Offset(pad, pad), Offset(pad + cornerLen, pad), 4f)
              drawLine(frameColor, Offset(pad, pad), Offset(pad, pad + cornerLen), 4f)
              // Top-Right
              drawLine(frameColor, Offset(w - pad, pad), Offset(w - pad - cornerLen, pad), 4f)
              drawLine(frameColor, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), 4f)
              // Bottom-Left
              drawLine(frameColor, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), 4f)
              drawLine(frameColor, Offset(pad, h - pad), Offset(pad, h - pad - cornerLen), 4f)
              // Bottom-Right
              drawLine(frameColor, Offset(w - pad, h - pad), Offset(w - pad - cornerLen, h - pad), 4f)
              drawLine(frameColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLen), 4f)
            }
          }

          // Layer 2: High-Fidelity Pokémon Sprite & 3D Cel-Shaded Model Stage
          if (sideBySideCompare) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .offset(y = (-10).dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PokemonCharacterRenderer(
                  pokemon = primaryPokemon,
                  style = PokemonGraphicsStyle.SPRITE_CRISP,
                  pixelTo3dBlend = 0f,
                  extrusionDepth = config.voxelExtrusionDepth,
                  size = 138.dp,
                )
                Text(
                  text = "GEN V SPRITE",
                  fontFamily = PressStart2PFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = CyberCyan,
                )
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PokemonCharacterRenderer(
                  pokemon = primaryPokemon,
                  style = PokemonGraphicsStyle.MODEL_3D_CEL,
                  pixelTo3dBlend = 1f,
                  extrusionDepth = config.voxelExtrusionDepth,
                  size = 144.dp,
                  mirrored = true,
                )
                Text(
                  text = "3D CEL MODEL",
                  fontFamily = PressStart2PFamily,
                  style = MaterialTheme.typography.labelSmall,
                  color = SuperFxGold,
                )
              }
            }
          } else {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.offset(y = (-8).dp),
            ) {
              PokemonCharacterRenderer(
                pokemon = primaryPokemon,
                style = renderStyle,
                pixelTo3dBlend = config.pixelTo3dBlend,
                extrusionDepth = config.voxelExtrusionDepth,
                size = 176.dp,
              )

              // Secondary DNA / Palette Partner Mini-Badge in Upper Right of Stage
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .offset(x = 42.dp, y = (-4).dp)
                  .size(54.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(StudioCardDark.copy(alpha = 0.92f))
                  .border(1.5.dp, secondaryColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                AsyncImage(
                  model = secondaryPokemon.pixelSpriteUrl,
                  contentDescription = "Secondary Donor ${secondaryPokemon.name}",
                  filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                  modifier = Modifier.size(46.dp),
                )
              }
            }
          }

          // Layer 3: PokeCRT-RTX Sub-Pixel Phosphor Scanlines & Bokeh Overlay
          if (hasPokeCrtRtx || config.crtScanlineIntensity > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val step = 6.dp.toPx()
              var y = 0f
              while (y < size.height) {
                drawRect(
                  color = Color.Black.copy(alpha = 0.35f * config.crtScanlineIntensity),
                  topLeft = Offset(0f, y),
                  size = Size(size.width, 1.5f),
                )
                y += step
              }
            }
          }

          // Bottom Telemetry Readout Bar inside Viewport
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(StudioDeepNavy.copy(alpha = 0.86f))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "2D PIXEL ${(100 - (config.pixelTo3dBlend * 100).toInt())}% ⇄ 3D RIG ${(config.pixelTo3dBlend * 100).toInt()}%",
              fontFamily = PressStart2PFamily,
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
            Text(
              text = "VOXEL ${(config.voxelExtrusionDepth * 100).toInt()}% | BLOOM ${(config.hd2dVolumetricBloom * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick-Pin Fused Output directly to the Always-Visible Categorized Side Navigation Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = { onPinToSidebar(AssetCategory.SPRITES) },
            modifier = Modifier
              .weight(1f)
              .testTag("pin_fused_sprite_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CyberCyan,
              contentColor = StudioDeepNavy,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.BookmarkAdd,
              contentDescription = "Pin Fused Sprite",
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pin Fused Sprite",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          Button(
            onClick = { onPinToSidebar(AssetCategory.MODELS_3D) },
            modifier = Modifier
              .weight(1f)
              .testTag("pin_fused_3d_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = VoxelViolet,
              contentColor = StudioTextPrimary,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.AutoFixHigh,
              contentDescription = "Pin 3D Voxel Rig",
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pin 3D Voxel Rig",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Sliders & Multi-Console Pipeline Synthesizer Controls
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "MULTI-SYSTEM SPRITE & 3D SHADER MIXER",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = CyberCyan,
        )

        FusionSliderRow(
          label = "2D Pixel Sprite ⇄ 3D Polygon/Cel Blend",
          value = config.pixelTo3dBlend,
          accentColor = CyberCyan,
          onValueChange = { v -> onUpdateConfig { it.copy(pixelTo3dBlend = v) } },
        )

        FusionSliderRow(
          label = "Voxelizer-FX 3D Extrusion & Normal Depth",
          value = config.voxelExtrusionDepth,
          accentColor = VoxelViolet,
          onValueChange = { v -> onUpdateConfig { it.copy(voxelExtrusionDepth = v) } },
        )

        FusionSliderRow(
          label = "Switch HD-2D Volumetric Bloom & Point Lighting",
          value = config.hd2dVolumetricBloom,
          accentColor = SuperFxGold,
          onValueChange = { v -> onUpdateConfig { it.copy(hd2dVolumetricBloom = v) } },
        )

        FusionSliderRow(
          label = "SNES / GBA Mode-7 Affine Ground Perspective",
          value = config.mode7PerspectiveTilt,
          accentColor = SgbEmerald,
          onValueChange = { v -> onUpdateConfig { it.copy(mode7PerspectiveTilt = v) } },
        )

        FusionSliderRow(
          label = "PokeCRT-RTX Trinitron Phosphor Scanlines",
          value = config.crtScanlineIntensity,
          accentColor = ElectricCrimson,
          onValueChange = { v -> onUpdateConfig { it.copy(crtScanlineIntensity = v) } },
        )
      }
    }

    // Official & Fan-Made Enhancement Chips Matrix
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Hardware Enhancement Chips",
            tint = SuperFxGold,
          )
          Column {
            Text(
              text = "OFFICIAL & FAN-MADE CO-PROCESSOR CHIPS",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SuperFxGold,
            )
            Text(
              text = "Toggle cartridge ASICs, 32X/64DD add-ons, and fan FPGA cores in real time",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
            )
          }
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ConsoleHardwareCatalog.officialAndFanChips.forEach { chip ->
            val isActive = chip.id in config.activeChipIds
            val chipColor = parseHexColorSafe(chip.accentHex, CyberCyan)
            FilterChip(
              selected = isActive,
              onClick = { onToggleChip(chip.id) },
              label = {
                Text(
                  text = if (chip.isFanMade) "★ ${chip.name}" else chip.name,
                  style = MaterialTheme.typography.labelSmall,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = chipColor.copy(alpha = 0.22f),
                selectedLabelColor = StudioTextPrimary,
              ),
              modifier = Modifier.testTag("chip_toggle_${chip.id}"),
            )
          }
        }

        // Console Era Lineage Breakdown
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "ACTIVE CONSOLE & HANDHELD ERA FUSION PIPELINE",
          style = MaterialTheme.typography.labelLarge,
          color = Mode7Sky,
        )

        ConsoleHardwareCatalog.consoleEraLayers.forEach { layer ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(StudioDeepNavy)
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = layer.eraName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
              )
              Text(
                text = "${layer.hardwareCode} • ${layer.visualContribution}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FusionSliderRow(
  label: String,
  value: Float,
  accentColor: Color,
  onValueChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = StudioTextPrimary,
      )
      Text(
        text = "${(value * 100).toInt()}%",
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
      ),
    )
  }
}

fun parseHexColorSafe(hex: String, fallback: Color): Color {
  return try {
    val clean = hex.trim().removePrefix("#")
    val longVal = clean.toLong(16)
    if (clean.length == 6) {
      Color(0xFF000000 or longVal)
    } else {
      fallback
    }
  } catch (e: Exception) {
    fallback
  }
}

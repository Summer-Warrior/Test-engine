package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.model.AssetCategory
import com.example.data.model.CharacterMixMode
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.MasterDisciplineMode
import com.example.ui.HardwareRegistryUiState
import com.example.ui.shaders.omniDisplayPostEffect
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
 * Interactive Shovel Knight+ Omni-Console Fusion Viewport.
 * Composites unified multi-hardware AGSL shaders, 4 non-muddy CharacterMixModes,
 * custom painted 16x16 sprite 3D voxel extrusion, 60fps chip VFX layers,
 * all 87 real gaming hardware platforms (1962-2026), and 9 Extra Pretty detail switches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmniConsoleFusionViewport(
  primaryPokemon: NativePokemonEntity,
  secondaryPokemon: NativePokemonEntity,
  config: ConsoleFusionConfig,
  hardwareState: HardwareRegistryUiState,
  customSpriteBitmap: Bitmap?,
  useCustomSprite: Boolean,
  onToggleUseCustomSprite: () -> Unit,
  onUpdateConfig: ((ConsoleFusionConfig) -> ConsoleFusionConfig) -> Unit,
  onSelectMixMode: (CharacterMixMode) -> Unit,
  onToggleHardwarePlatform: (String) -> Unit,
  onToggleAllHardware: (Boolean) -> Unit,
  onSelectDiscipline: (MasterDisciplineMode) -> Unit,
  onUpdateExtraPretty: ((ExtraVisualAudioSettings) -> ExtraVisualAudioSettings) -> Unit,
  onPlayCry: () -> Unit,
  onPinToSidebar: (AssetCategory) -> Unit,
) {
  val visuals = hardwareState.convergedVisuals
  val audio = hardwareState.convergedAudio
  val activeIds = hardwareState.activePlatformIds
  val primaryColor = parseHexColorSafe(primaryPokemon.primaryHex, CyberCyan)
  val secondaryColor = parseHexColorSafe(secondaryPokemon.primaryHex, ElectricCrimson)

  val infinite = rememberInfiniteTransition(label = "omni_viewport_60fps")
  val orbitDeg by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 10000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "orbit_deg",
  )
  val pulsePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "pulse_phase",
  )

  val activeBitmapForStage = if (useCustomSprite) customSpriteBitmap else null
  val derivedStyle = when (hardwareState.characterMixMode) {
    CharacterMixMode.HD2D_CRISP_SPRITE -> PokemonGraphicsStyle.SPRITE_CRISP
    CharacterMixMode.PIXEL_LOCKED_3D -> PokemonGraphicsStyle.MODEL_3D_CEL
    CharacterMixMode.SPLIT_BEAM_MORPH, CharacterMixMode.VOXELIZER_FX_3D -> PokemonGraphicsStyle.HYBRID_FUSION
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        // Header telemetry row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "OMNI-HARDWARE RENDER STAGE • ${visuals.activeCount} PLATFORMS CONVERGED",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "#${primaryPokemon.dexNumber} ${primaryPokemon.name} × ${secondaryPokemon.name}",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${visuals.disciplineMode.label} • Grid ${visuals.virtualPixelGrid.toInt()}px • ${visuals.characterStepFps}fps Keyframes • ${visuals.voxelSliceCount} Voxel Slices",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onPlayCry,
            modifier = Modifier.testTag("viewport_play_cry_button"),
          ) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Play Pokémon Cry", modifier = Modifier.size(16.dp), tint = CyberCyan)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cry", color = CyberCyan)
          }
        }

        // Unified AGSL Stage with 60fps Hardware VFX & Non-Muddy Character Renderer
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .height(272.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              Brush.verticalGradient(
                listOf(Color(0xFF070B14), Color(0xFF0F1A32), Color(0xFF060A12))
              )
            )
            .border(1.5.dp, primaryColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .testTag("omni_fusion_stage"),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .omniDisplayPostEffect(
                visuals = visuals,
              ),
          ) {
            // 60fps Multi-Hardware Background & Chip VFX Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
              val w = size.width
              val h = size.height
              val cx = w * 0.5f
              val cy = h * 0.50f
              val horizonY = h * (0.50f - visuals.affineWarp * 0.12f)

              // 1. N64 / HD-2D Volumetric Spotlights & Sunbeams
              if (visuals.volumetricRaysEnabled || "n64_rcp" in activeIds) {
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
                    primaryColor.copy(alpha = 0.26f + visuals.bloom * 0.22f),
                    secondaryColor.copy(alpha = 0.10f),
                    Color.Transparent,
                  ),
                  center = Offset(cx, cy),
                  radius = minOf(w, h) * 0.44f,
                ),
                radius = minOf(w, h) * 0.44f,
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
              if ("sega_32x" in activeIds || "sega_saturn" in activeIds || visuals.voxelDepth > 0.55f) {
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
                val barH = (12f + gain * 36f * bounce).coerceAtMost(46f)
                drawRect(
                  color = if (idx % 2 == 0) CyberCyan.copy(alpha = 0.72f) else SuperFxGold.copy(alpha = 0.72f),
                  topLeft = Offset(14f + idx * 9f, h - 12f - barH),
                  size = Size(5f, barH),
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

              // 8. Foreground Parallax Silhouette Framing (Octopath / Shovel Knight depth framing)
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

            // Center Unified Non-Muddy Character Renderer
            PokemonCharacterRenderer(
              pokemon = primaryPokemon,
              style = derivedStyle,
              pixelTo3dBlend = config.pixelTo3dBlend,
              extrusionDepth = visuals.voxelDepth,
              size = 186.dp,
              mixMode = hardwareState.characterMixMode,
              visuals = visuals,
              customSpriteBitmap = activeBitmapForStage,
              modifier = Modifier.align(Alignment.Center),
            )

            // Corner HUD Telemetry Chips
            Box(
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC070B14))
                .border(1.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text(
                text = hardwareState.characterMixMode.label.uppercase(),
                fontFamily = PressStart2PFamily,
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }

            if (activeBitmapForStage != null) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(10.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(SgbEmerald.copy(alpha = 0.25f))
                  .border(1.dp, SgbEmerald, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "CUSTOM 16x16 RIG",
                  style = MaterialTheme.typography.labelSmall,
                  color = SgbEmerald,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        // 4 Non-Muddy Character Mix Mode Selector + Custom Sprite Toggle
        Text(
          text = "2D + 3D CHARACTER PRESENTATION MODE (ZERO DOUBLE-EXPOSURE)",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
          fontWeight = FontWeight.Bold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          CharacterMixMode.entries.forEach { mode ->
            val selected = hardwareState.characterMixMode == mode
            FilterChip(
              selected = selected,
              onClick = { onSelectMixMode(mode) },
              label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("mix_mode_${mode.name.lowercase()}"),
            )
          }
          if (customSpriteBitmap != null) {
            FilterChip(
              selected = useCustomSprite,
              onClick = onToggleUseCustomSprite,
              leadingIcon = {
                Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(14.dp))
              },
              label = { Text(if (useCustomSprite) "Showing Custom Sprite" else "Preview Custom Sprite", style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SgbEmerald.copy(alpha = 0.22f),
                selectedLabelColor = SgbEmerald,
              ),
              modifier = Modifier.testTag("toggle_custom_sprite_viewport"),
            )
          }
        }

        // Live Shader & Morph Sliders (all drive real AGSL/Canvas parameters)
        ViewportSliderRow(
          label = if (hardwareState.characterMixMode == CharacterMixMode.SPLIT_BEAM_MORPH) "Split-Beam Position (2D ↔ 3D)" else "2D/3D Fusion Ratio",
          value = config.pixelTo3dBlend,
          valueText = "${(config.pixelTo3dBlend * 100).toInt()}%",
          accent = CyberCyan,
          testTag = "slider_pixel_3d_blend",
          onValueChange = { v -> onUpdateConfig { it.copy(pixelTo3dBlend = v) } },
        )
        ViewportSliderRow(
          label = "Voxel Extrusion Depth (${visuals.voxelSliceCount} Slices)",
          value = config.voxelExtrusionDepth,
          valueText = "${(config.voxelExtrusionDepth * 100).toInt()}%",
          accent = VoxelViolet,
          testTag = "slider_voxel_depth",
          onValueChange = { v -> onUpdateConfig { it.copy(voxelExtrusionDepth = v) } },
        )
        ViewportSliderRow(
          label = "Mode-7 Horizon Tilt & Bloom",
          value = config.hd2dVolumetricBloom,
          valueText = "${(config.hd2dVolumetricBloom * 100).toInt()}%",
          accent = SuperFxGold,
          testTag = "slider_bloom_strength",
          onValueChange = { v -> onUpdateConfig { it.copy(hd2dVolumetricBloom = v, mode7PerspectiveTilt = v) } },
        )
        ViewportSliderRow(
          label = "CRT Scanline / Dot-Matrix Pass",
          value = config.crtScanlineIntensity,
          valueText = "${(config.crtScanlineIntensity * 100).toInt()}%",
          accent = Mode7Sky,
          testTag = "slider_crt_scanline",
          onValueChange = { v -> onUpdateConfig { it.copy(crtScanlineIntensity = v) } },
        )

        // Pin to Sidebar actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onPinToSidebar(AssetCategory.SPRITES) },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier
              .weight(1f)
              .testTag("pin_fusion_sprite_button"),
          ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pin Sprite Rig", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { onPinToSidebar(AssetCategory.CHIPS_SHADERS) },
            modifier = Modifier
              .weight(1f)
              .testTag("pin_fusion_shader_button"),
          ) {
            Text("Pin Shader Preset", color = SuperFxGold)
          }
        }
      }
    }

    // Phase 2: All 87+ Real Gaming Hardware Platforms & Master Discipline Controls
    HardwareConvergencePanel(
      allPlatforms = hardwareState.allPlatforms,
      activePlatformIds = hardwareState.activePlatformIds,
      disciplineMode = hardwareState.disciplineMode,
      convergedVisuals = hardwareState.convergedVisuals,
      convergedAudio = hardwareState.convergedAudio,
      onTogglePlatform = onToggleHardwarePlatform,
      onSelectAllPlatforms = { onToggleAllHardware(true) },
      onResetCuratedPlatforms = { onToggleAllHardware(false) },
      onSelectDisciplineMode = onSelectDiscipline,
    )

    // Phase 3: 9 Opt-In Extra Pretty Visual & Audio Detail Switches
    ExtraPrettyControlsPanel(
      settings = hardwareState.extraPretty,
      onUpdate = onUpdateExtraPretty,
    )
  }
}

@Composable
private fun ViewportSliderRow(
  label: String,
  value: Float,
  valueText: String,
  accent: Color,
  testTag: String,
  onValueChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
      Text(valueText, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      colors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = StudioDeepNavy,
      ),
      modifier = Modifier.testTag(testTag),
    )
  }
}

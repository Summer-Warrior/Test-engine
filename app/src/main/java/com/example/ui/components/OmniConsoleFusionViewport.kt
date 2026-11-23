package com.example.ui.components

import android.graphics.Bitmap
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Interactive Shovel Knight+ Omni-Console Fusion Viewport.
 * Uses the shared MultiHardwareBattleStageBackdrop so Omni-Fusion and Game Test share identical live hardware rendering.
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
              text = "${visuals.disciplineMode.label} • Grid ${visuals.virtualPixelGrid.toInt()}px • ${visuals.characterStepFps}fps • ${visuals.voxelSliceCount} Voxel Slices",
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

        // Unified AGSL Stage with Shared MultiHardwareBattleStageBackdrop
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
              .omniDisplayPostEffect(visuals = visuals),
          ) {
            MultiHardwareBattleStageBackdrop(
              primaryColor = primaryColor,
              secondaryColor = secondaryColor,
              visuals = visuals,
              audio = audio,
              activePlatformIds = activeIds,
            )

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

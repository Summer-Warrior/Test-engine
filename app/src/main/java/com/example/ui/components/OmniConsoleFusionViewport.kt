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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.engine.TrainerAppearanceConfig
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
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
  inspectTrainerMode: Boolean = false,
  selectedTrainer: TrainerCharacterSpec = TrainerCatalogAndRigEngine.defaultProtagonist,
  playerTrainer: TrainerCharacterSpec = TrainerCatalogAndRigEngine.defaultProtagonist,
  playerCustomAvatarBitmap: Bitmap? = null,
  useCustomPlayerAvatarBitmap: Boolean = false,
  onToggleInspectTrainerMode: (Boolean) -> Unit = {},
  onSelectViewportTrainer: (TrainerCharacterSpec) -> Unit = {},
  onSelectProtagonistPreset: (TrainerCharacterSpec) -> Unit = {},
  onUpdateProtagonistIdentity: (String, String) -> Unit = { _, _ -> },
  onUpdateProtagonistAppearance: ((TrainerAppearanceConfig) -> TrainerAppearanceConfig) -> Unit = {},
  onEditHeroInSpriteEditor: () -> Unit = {},
  onToggleUseCustomPlayerAvatar: () -> Unit = {},
  onSaveCustomTrainer: (TrainerCharacterSpec) -> Unit = {},
) {
  val visuals = hardwareState.convergedVisuals
  val audio = hardwareState.convergedAudio
  val activeIds = hardwareState.activePlatformIds
  var selectedTrainerPose by remember { mutableStateOf(TrainerPose.BATTLE_IDLE) }
  val activeStageTrainer = if (selectedTrainer.id == playerTrainer.id) playerTrainer else selectedTrainer
  val primaryColor = if (inspectTrainerMode) {
    parseHexColorSafe(activeStageTrainer.appearance.outfitPrimaryHex, CyberCyan)
  } else {
    parseHexColorSafe(primaryPokemon.primaryHex, CyberCyan)
  }
  val secondaryColor = if (inspectTrainerMode) {
    parseHexColorSafe(activeStageTrainer.appearance.accentHex, ElectricCrimson)
  } else {
    parseHexColorSafe(secondaryPokemon.primaryHex, ElectricCrimson)
  }

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
              text = if (inspectTrainerMode) {
                "${activeStageTrainer.name} • ${activeStageTrainer.title}"
              } else {
                "#${primaryPokemon.dexNumber} ${primaryPokemon.name} × ${secondaryPokemon.name}"
              },
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (inspectTrainerMode) {
                "${activeStageTrainer.roleCategory.label} • ${activeStageTrainer.sourceInspiration} • ${activeStageTrainer.teamDexIds.size} Partners"
              } else {
                "${visuals.disciplineMode.label} • Grid ${visuals.virtualPixelGrid.toInt()}px • ${visuals.characterStepFps}fps • ${visuals.voxelSliceCount} Voxel Slices"
              },
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

        // Creature Rig vs Trainer Rig Stage Inspector Switcher
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = !inspectTrainerMode,
            onClick = { onToggleInspectTrainerMode(false) },
            label = { Text("Creature Rig (2D⇄3D)", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
              selectedLabelColor = CyberCyan,
            ),
            modifier = Modifier.testTag("viewport_mode_creature_chip"),
          )
          FilterChip(
            selected = inspectTrainerMode,
            onClick = { onToggleInspectTrainerMode(true) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = { Text("Trainer & Hero Studio (78+)", style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
              selectedLabelColor = SuperFxGold,
            ),
            modifier = Modifier.testTag("viewport_mode_trainer_chip"),
          )
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

            if (inspectTrainerMode) {
              val isInspectingHero = activeStageTrainer.id == playerTrainer.id
              TrainerCharacterRenderer(
                trainer = activeStageTrainer,
                pose = selectedTrainerPose,
                characterMixMode = hardwareState.characterMixMode,
                visuals = visuals,
                cameraYawDegrees = (config.pixelTo3dBlend - 0.5f) * 70f,
                customAvatarBitmap = if (isInspectingHero && useCustomPlayerAvatarBitmap) playerCustomAvatarBitmap else null,
                modifier = Modifier
                  .align(Alignment.Center)
                  .size(188.dp),
              )
              // High-contrast 40x40 Dialogue Portrait inset
              TrainerPortraitBadge(
                trainer = activeStageTrainer,
                size = 64.dp,
                borderColor = SuperFxGold,
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(10.dp),
              )
            } else {
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
            }

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
                text = if (inspectTrainerMode) {
                  "${hardwareState.characterMixMode.shortTag} • ${selectedTrainerPose.label.uppercase()}"
                } else {
                  hardwareState.characterMixMode.label.uppercase()
                },
                fontFamily = PressStart2PFamily,
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }

            if (!inspectTrainerMode && activeBitmapForStage != null) {
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

        // Trainer Pose Switcher + Featured Cast Selector + Protagonist Customizer (when in Trainer Rig mode)
        if (inspectTrainerMode) {
          Text(
            text = "TRAINER ANIMATION POSE (4-DIR WALK • BATTLE • CAPSULE THROW • VICTORY)",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            TrainerPose.entries.forEach { pose ->
              FilterChip(
                selected = selectedTrainerPose == pose,
                onClick = { selectedTrainerPose = pose },
                label = { Text(pose.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                  selectedLabelColor = CyberCyan,
                ),
                modifier = Modifier.testTag("trainer_pose_${pose.name.lowercase()}"),
              )
            }
          }

          Text(
            text = "QUICK CAST SELECTOR (OR PICK ANY OF 78+ TRAINERS IN SIDEBAR → TRAINERS)",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val featuredCast = remember(playerTrainer) {
              listOf(
                playerTrainer,
                TrainerCatalogAndRigEngine.findById("prof_lyra_vance"),
                TrainerCatalogAndRigEngine.findById("rival_kael"),
                TrainerCatalogAndRigEngine.findById("commander_vex"),
                TrainerCatalogAndRigEngine.findById("leader_sinnoh"),
                TrainerCatalogAndRigEngine.findById("leader_galar"),
                TrainerCatalogAndRigEngine.findById("aura_guardian_sirion"),
              ).filterNotNull().distinctBy { it.id }
            }
            featuredCast.forEach { spec ->
              FilterChip(
                selected = activeStageTrainer.id == spec.id,
                onClick = { onSelectViewportTrainer(spec) },
                label = { Text(spec.name, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
                  selectedLabelColor = SuperFxGold,
                ),
                modifier = Modifier.testTag("viewport_cast_${spec.id}"),
              )
            }
          }

          // Complete Protagonist Wardrobe & Avatar Studio (Name, Title, Presets, Skin, Hair, Headgear, Outfit, Palette, Pose, Pixel Editor)
          ProtagonistWardrobePanel(
            playerTrainer = playerTrainer,
            playerCustomAvatarBitmap = playerCustomAvatarBitmap,
            useCustomPlayerAvatarBitmap = useCustomPlayerAvatarBitmap,
            onSelectProtagonistPreset = { preset ->
              onSelectProtagonistPreset(preset)
              onSelectViewportTrainer(preset)
            },
            onUpdateProtagonistIdentity = { name, title ->
              onUpdateProtagonistIdentity(name, title)
            },
            onUpdateProtagonistAppearance = { transform ->
              onSelectViewportTrainer(playerTrainer)
              onUpdateProtagonistAppearance(transform)
            },
            onEditHeroInSpriteEditor = onEditHeroInSpriteEditor,
            onToggleUseCustomPlayerAvatar = onToggleUseCustomPlayerAvatar,
            onSaveHeroToRoom = { onSaveCustomTrainer(activeStageTrainer) },
          )
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

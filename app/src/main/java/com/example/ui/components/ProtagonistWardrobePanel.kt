package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.TrainerAppearanceConfig
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerHeadgear
import com.example.engine.TrainerOutfitStyle
import com.example.engine.TrainerPose
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

private val hairStyleLabels = listOf(
  0 to "Spiky Hero",
  1 to "Flowing Locks",
  2 to "Swept Rival",
  3 to "Starlight Twin",
  4 to "Ranger Crest",
  5 to "Zenith Mane",
)

private val companionPoseLabels = listOf(
  0 to "Classic Stance",
  1 to "Prism Scarf",
  2 to "Champion Crest",
)

private val extendedHairColors: List<Pair<String, String>> =
  TrainerCatalogAndRigEngine.hairColorPresets + listOf(
    "Sakura" to "#EC4899",
    "Sunburst" to "#FACC15",
  )

/**
 * Complete Player Sprite & Hero Avatar Customizer shared between Omni-Console Trainer Studio
 * and the Game Test Wardrobe HUD drawer.
 */
@Composable
fun ProtagonistWardrobePanel(
  playerTrainer: TrainerCharacterSpec,
  playerCustomAvatarBitmap: Bitmap?,
  useCustomPlayerAvatarBitmap: Boolean,
  onSelectProtagonistPreset: (TrainerCharacterSpec) -> Unit,
  onUpdateProtagonistIdentity: (String, String) -> Unit,
  onUpdateProtagonistAppearance: ((TrainerAppearanceConfig) -> TrainerAppearanceConfig) -> Unit,
  onEditHeroInSpriteEditor: () -> Unit,
  onToggleUseCustomPlayerAvatar: () -> Unit,
  onSaveHeroToRoom: () -> Unit,
  compact: Boolean = false,
  showSpriteEditorBridge: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val app = playerTrainer.appearance
  val customImageBmp = remember(playerCustomAvatarBitmap) { playerCustomAvatarBitmap?.asImageBitmap() }
  var nameDraft by remember(playerTrainer.id) { mutableStateOf(playerTrainer.name) }
  var titleDraft by remember(playerTrainer.id) { mutableStateOf(playerTrainer.title) }
  var nameFieldFocused by remember { mutableStateOf(false) }
  var titleFieldFocused by remember { mutableStateOf(false) }

  LaunchedEffect(playerTrainer.id, playerTrainer.name) {
    if (!nameFieldFocused && playerTrainer.name != nameDraft) {
      nameDraft = playerTrainer.name
    }
  }
  LaunchedEffect(playerTrainer.id, playerTrainer.title) {
    if (!titleFieldFocused && playerTrainer.title != titleDraft) {
      titleDraft = playerTrainer.title
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(StudioDeepNavy.copy(alpha = 0.86f))
      .border(1.dp, SuperFxGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("protagonist_wardrobe_panel"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Header + Live Preview Strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (useCustomPlayerAvatarBitmap && customImageBmp != null) {
        Box(
          modifier = Modifier
            .size(if (compact) 48.dp else 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceDark)
            .border(2.dp, ElectricCrimson, RoundedCornerShape(12.dp))
            .padding(4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = customImageBmp,
            contentDescription = "${playerTrainer.name} custom pixel avatar",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (compact) 40.dp else 48.dp),
          )
        }
      } else {
        TrainerPortraitBadge(
          trainer = playerTrainer,
          size = if (compact) 48.dp else 56.dp,
          borderColor = SuperFxGold,
        )
      }

      TrainerOverworldThumbnail(
        trainer = playerTrainer,
        pose = TrainerPose.OVERWORLD_DOWN,
        modifier = Modifier
          .size(if (compact) 44.dp else 52.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(StudioSurfaceDark)
          .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Icon(Icons.Default.Checkroom, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(16.dp))
          Text(
            text = "HERO WARDROBE & AVATAR STUDIO",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = "${playerTrainer.name} • ${playerTrainer.title}",
          style = MaterialTheme.typography.labelLarge,
          color = StudioTextPrimary,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (useCustomPlayerAvatarBitmap && customImageBmp != null) {
            "Active Mode: Custom 16x16 Pixel Avatar"
          } else {
            "${app.outfitStyle.label} • ${app.headgear.label} • ${companionPoseLabels.getOrNull(app.companionPoseIndex.coerceIn(0, 2))?.second ?: "Classic"}"
          },
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 1-Tap Base Hero Presets (Nova, Kairo, Sora, Talia)
    Text(
      text = "1-TAP HERO PRESETS",
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
      TrainerCatalogAndRigEngine.playableProtagonists.forEach { preset ->
        val selected = playerTrainer.id == preset.id
        FilterChip(
          selected = selected,
          onClick = { onSelectProtagonistPreset(preset) },
          label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
            selectedLabelColor = SuperFxGold,
          ),
          modifier = Modifier.testTag("hero_preset_${preset.id}"),
        )
      }
    }

    // Editable Player Name & Trainer Title (local draft allows full backspace clearing without snapping back)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = nameDraft,
        onValueChange = { raw ->
          val clipped = raw.take(18)
          nameDraft = clipped
          if (clipped.isNotBlank()) {
            onUpdateProtagonistIdentity(clipped, titleDraft.ifBlank { playerTrainer.title })
          }
        },
        label = { Text("Player Name", style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .onFocusChanged { focusState ->
            val wasFocused = nameFieldFocused
            nameFieldFocused = focusState.isFocused
            if (wasFocused && !focusState.isFocused && nameDraft.isBlank()) {
              nameDraft = "Nova"
              onUpdateProtagonistIdentity("Nova", titleDraft.ifBlank { "Prism Ace" })
            }
          }
          .testTag("hero_name_input"),
      )
      OutlinedTextField(
        value = titleDraft,
        onValueChange = { raw ->
          val clipped = raw.take(24)
          titleDraft = clipped
          if (clipped.isNotBlank()) {
            onUpdateProtagonistIdentity(nameDraft.ifBlank { playerTrainer.name }, clipped)
          }
        },
        label = { Text("Trainer Title", style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = Modifier
          .weight(1.25f)
          .onFocusChanged { focusState ->
            val wasFocused = titleFieldFocused
            titleFieldFocused = focusState.isFocused
            if (wasFocused && !focusState.isFocused && titleDraft.isBlank()) {
              titleDraft = "Prism Ace"
              onUpdateProtagonistIdentity(nameDraft.ifBlank { "Nova" }, "Prism Ace")
            }
          }
          .testTag("hero_title_input"),
      )
    }

    // Skin Tone Swatches
    WardrobeSectionLabel("SKIN TONE")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TrainerCatalogAndRigEngine.skinTonePresets.forEach { (label, hex) ->
        val selected = app.skinHex.equals(hex, ignoreCase = true)
        SwatchChip(
          label = label,
          swatchColor = parseHexColorSafe(hex, Color(0xFFF6C8A5)),
          selected = selected,
          onClick = { onUpdateProtagonistAppearance { it.copy(skinHex = hex) } },
          tag = "hero_skin_${label.lowercase().replace(' ', '_')}",
        )
      }
    }

    // Hair Style (0..5)
    WardrobeSectionLabel("HAIR STYLE")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      hairStyleLabels.forEach { (idx, label) ->
        FilterChip(
          selected = app.hairStyleIndex % 6 == idx,
          onClick = { onUpdateProtagonistAppearance { it.copy(hairStyleIndex = idx) } },
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
            selectedLabelColor = CyberCyan,
          ),
          modifier = Modifier.testTag("hero_hair_style_$idx"),
        )
      }
    }

    // Hair Color (9 swatches)
    WardrobeSectionLabel("HAIR COLOR")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      extendedHairColors.forEach { (label, hex) ->
        val selected = app.hairHex.equals(hex, ignoreCase = true)
        SwatchChip(
          label = label,
          swatchColor = parseHexColorSafe(hex, Color(0xFF1E293B)),
          selected = selected,
          onClick = { onUpdateProtagonistAppearance { it.copy(hairHex = hex) } },
          tag = "hero_hair_color_${label.lowercase()}",
        )
      }
    }

    // Headgear (9 options)
    WardrobeSectionLabel("HEADGEAR (9)")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      TrainerHeadgear.entries.forEach { gear ->
        FilterChip(
          selected = app.headgear == gear,
          onClick = { onUpdateProtagonistAppearance { it.copy(headgear = gear) } },
          label = { Text(gear.label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VoxelViolet.copy(alpha = 0.24f),
            selectedLabelColor = VoxelViolet,
          ),
          modifier = Modifier.testTag("hero_headgear_${gear.name.lowercase()}"),
        )
      }
    }

    // Outfit Style (8 options)
    WardrobeSectionLabel("OUTFIT STYLE (8)")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      TrainerOutfitStyle.entries.forEach { style ->
        FilterChip(
          selected = app.outfitStyle == style,
          onClick = { onUpdateProtagonistAppearance { it.copy(outfitStyle = style) } },
          label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
            selectedLabelColor = CyberCyan,
          ),
          modifier = Modifier.testTag("hero_outfit_${style.name.lowercase()}"),
        )
      }
    }

    // Outfit Palette (6 curated multi-tone palettes)
    WardrobeSectionLabel("OUTFIT PALETTE")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TrainerCatalogAndRigEngine.outfitPalettePresets.forEach { (name, triple) ->
        val selected = app.outfitPrimaryHex.equals(triple.first, ignoreCase = true)
        SwatchChip(
          label = name,
          swatchColor = parseHexColorSafe(triple.first, CyberCyan),
          selected = selected,
          onClick = {
            onUpdateProtagonistAppearance {
              it.copy(
                outfitPrimaryHex = triple.first,
                outfitSecondaryHex = triple.second,
                accentHex = triple.third,
              )
            }
          },
          tag = "hero_palette_${name.lowercase().replace(' ', '_')}",
        )
      }
    }

    // Companion Pose (0..2)
    WardrobeSectionLabel("COMPANION POSE")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      companionPoseLabels.forEach { (poseIdx, label) ->
        FilterChip(
          selected = app.companionPoseIndex.coerceIn(0, 2) == poseIdx,
          onClick = { onUpdateProtagonistAppearance { it.copy(companionPoseIndex = poseIdx) } },
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SuperFxGold.copy(alpha = 0.24f),
            selectedLabelColor = SuperFxGold,
          ),
          modifier = Modifier.testTag("hero_companion_pose_$poseIdx"),
        )
      }
    }

    // Pixel Editor Bridge & Room Persistence Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showSpriteEditorBridge) {
        OutlinedButton(
          onClick = onEditHeroInSpriteEditor,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("edit_hero_in_sprite_editor_button"),
        ) {
          Icon(Icons.Default.Brush, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Edit Hero in Sprite Editor", style = MaterialTheme.typography.labelSmall, color = CyberCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }

      Button(
        onClick = {
          val safeName = nameDraft.trim().ifBlank { "Nova" }
          val safeTitle = titleDraft.trim().ifBlank { "Prism Ace" }
          nameDraft = safeName
          titleDraft = safeTitle
          onUpdateProtagonistIdentity(safeName, safeTitle)
          onSaveHeroToRoom()
        },
        colors = ButtonDefaults.buttonColors(containerColor = SgbEmerald, contentColor = StudioDeepNavy),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .let { if (!showSpriteEditorBridge) it.weight(1f) else it }
          .heightIn(min = 48.dp)
          .testTag("save_hero_wardrobe_button"),
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Save", fontWeight = FontWeight.Bold)
      }
    }

    // Toggle for Custom 16x16 Pixel Avatar Override (strictly separate from Creature custom bitmap)
    if (playerCustomAvatarBitmap != null || useCustomPlayerAvatarBitmap) {
      FilterChip(
        selected = useCustomPlayerAvatarBitmap,
        onClick = onToggleUseCustomPlayerAvatar,
        label = {
          Text(
            text = if (useCustomPlayerAvatarBitmap) {
              "Custom Pixel Hero Avatar: ACTIVE (Tap to revert to Procedural Hero)"
            } else {
              "Custom Pixel Hero Avatar: Saved (Tap to equip on Overworld)"
            },
            style = MaterialTheme.typography.labelSmall,
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = ElectricCrimson.copy(alpha = 0.24f),
          selectedLabelColor = ElectricCrimson,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("toggle_custom_hero_avatar_chip"),
      )
    }
  }
}

@Composable
private fun WardrobeSectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = StudioTextSecondary,
    fontWeight = FontWeight.Bold,
  )
}

@Composable
private fun SwatchChip(
  label: String,
  swatchColor: Color,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Row(
    modifier = Modifier
      .heightIn(min = 40.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) CyberCyan.copy(alpha = 0.18f) else StudioSurfaceDark)
      .border(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) CyberCyan else StudioBorderDark,
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(CircleShape)
        .background(swatchColor)
        .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) CyberCyan else StudioTextPrimary,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
  }
}

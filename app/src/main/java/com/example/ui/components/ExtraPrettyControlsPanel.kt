package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExtraVisualAudioSettings
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

/**
 * 9 Opt-In "Extra Pretty" Visual & Audio Detail Toggles.
 * Every switch drives a real AGSL uniform, Canvas pass, voxel slice depth, stepped clock, or DSP choir/reverb parameter.
 */
@Composable
fun ExtraPrettyControlsPanel(
  settings: ExtraVisualAudioSettings,
  onUpdate: ((ExtraVisualAudioSettings) -> ExtraVisualAudioSettings) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, VoxelViolet.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
      .testTag("extra_pretty_controls_panel"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = "Extra Pretty Visual & Audio Details", tint = VoxelViolet)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "EXTRA PRETTY VISUAL & AUDIO DETAILS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = VoxelViolet,
          )
          Text(
            text = "Real opt-in shader, voxel, weather, and DSP choir/reverb enhancers",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { onUpdate { ExtraVisualAudioSettings.ALL_ENABLED } },
          colors = ButtonDefaults.buttonColors(containerColor = VoxelViolet, contentColor = StudioTextPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("extra_pretty_enable_all_button"),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Enable All 9", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = { onUpdate { ExtraVisualAudioSettings() } },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag("extra_pretty_reset_button"),
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = "Reset Extra Pretty", tint = CyberCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Curated", color = CyberCyan)
        }
      }

      ExtraPrettyToggleRow(
        title = "Uncapped Bloom & Rim Flares",
        subtitle = "Boosts AGSL bloom lift & top-left rim light intensity",
        checked = settings.uncappedBloomAndRimFlares,
        tag = "toggle_extra_bloom",
        onCheckedChange = { v -> onUpdate { it.copy(uncappedBloomAndRimFlares = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Dynamic Weather Particles",
        subtitle = "Draws live rain, snow, sandstorm & elemental sparks over maps/battles",
        checked = settings.dynamicWeatherParticles,
        tag = "toggle_extra_weather",
        onCheckedChange = { v -> onUpdate { it.copy(dynamicWeatherParticles = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Water Caustics & Shoreline Foam",
        subtitle = "Animates shimmering specular caustics on auto-tiled ocean tiles",
        checked = settings.waterCausticsAndFoam,
        tag = "toggle_extra_water",
        onCheckedChange = { v -> onUpdate { it.copy(waterCausticsAndFoam = v) } },
      )
      ExtraPrettyToggleRow(
        title = "HD-2D Volumetric God Rays & Bokeh",
        subtitle = "Adds angled diagonal atmospheric light shafts across stage & overworld",
        checked = settings.hd2dVolumetricRaysAndBokeh,
        tag = "toggle_extra_rays",
        onCheckedChange = { v -> onUpdate { it.copy(hd2dVolumetricRaysAndBokeh = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Analog CRT Phosphor & Curvature",
        subtitle = "Enables Sony PVM aperture grille + subtle barrel curvature in AGSL",
        checked = settings.analogCrtPhosphorAndCurvature,
        tag = "toggle_extra_crt",
        onCheckedChange = { v -> onUpdate { it.copy(analogCrtPhosphorAndCurvature = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Ultra Voxel Slices (14 vs 8)",
        subtitle = "Increases Voxelizer-FX extrusion depth to 14 beveled slices",
        checked = settings.ultraVoxelSlices,
        tag = "toggle_extra_voxels",
        onCheckedChange = { v -> onUpdate { it.copy(ultraVoxelSlices = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Foreground Parallax Framing",
        subtitle = "Draws silhouette foliage & crystalline corner vignette depth frame",
        checked = settings.foregroundParallaxFraming,
        tag = "toggle_extra_parallax",
        onCheckedChange = { v -> onUpdate { it.copy(foregroundParallaxFraming = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Extra Lush Audio Reverb & Choir",
        subtitle = "Engages harmonic choir shimmer + deep SPC700/MSU-1 echo tail in DSP",
        checked = settings.extraLushAudioReverbAndChoir,
        tag = "toggle_extra_audio",
        onCheckedChange = { v -> onUpdate { it.copy(extraLushAudioReverbAndChoir = v) } },
      )
      ExtraPrettyToggleRow(
        title = "Fluid 60 FPS Character Motion",
        subtitle = "Unlocks smooth 60 FPS idle animation instead of 14 FPS stepped keyframes",
        checked = settings.fluid60FpsCharacterMotion,
        tag = "toggle_extra_60fps",
        onCheckedChange = { v -> onUpdate { it.copy(fluid60FpsCharacterMotion = v) } },
      )
    }
  }
}

@Composable
private fun ExtraPrettyToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(StudioDeepNavy)
      .border(1.dp, if (checked) SuperFxGold.copy(alpha = 0.55f) else StudioBorderDark, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
      Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = StudioDeepNavy,
        checkedTrackColor = SuperFxGold,
      ),
      modifier = Modifier.testTag(tag),
    )
  }
}

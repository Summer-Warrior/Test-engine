package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.FanGameProject
import com.example.ui.UiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold

/**
 * Shared Create / Edit dialog for Firestore FanGameProject records.
 */
@Composable
fun ProjectEditorDialog(
  existingProject: FanGameProject?,
  currentFusionConfig: ConsoleFusionConfig,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    region: String,
    synopsis: String,
    consolePreset: String,
    dexCount: Int,
    mapCount: Int,
    versionTag: String,
    captureCurrentEngineSettings: Boolean,
  ) -> Unit,
) {
  val isEdit = existingProject != null
  var title by remember(existingProject) {
    mutableStateOf(existingProject?.title ?: "Pokémon Omni-Horizon")
  }
  var region by remember(existingProject) {
    mutableStateOf(existingProject?.regionName ?: "Valkyra Archipelago")
  }
  var synopsis by remember(existingProject) {
    mutableStateOf(
      existingProject?.synopsis
        ?: "Multi-console fusion fan game blending GBC Hi-Color sprites, GBA Mode-7 routes, GameCube TEV shaders, and Switch HD-2D lighting."
    )
  }
  var consolePreset by remember(existingProject, currentFusionConfig) {
    mutableStateOf(existingProject?.consoleMatrixPreset ?: currentFusionConfig.presetName)
  }
  var dexCountStr by remember(existingProject) {
    mutableStateOf((existingProject?.dexCount ?: 300L).toString())
  }
  var mapCountStr by remember(existingProject) {
    mutableStateOf((existingProject?.mapCount ?: 24L).toString())
  }
  var versionTag by remember(existingProject) {
    mutableStateOf(existingProject?.versionTag ?: "v1.0.0-FUSION")
  }
  var captureEngineSettings by remember(existingProject) {
    mutableStateOf(!isEdit)
  }

  val presetTemplates = listOf(
    "GBC + GBA + N64 + Switch HD-2D",
    "Gen V Sprite + Sega 32X + GC TEV",
    "Super FX2 + SA-1 + Voxelizer-FX",
    "DMG/SGB2 + 3DS Cel + PokeCRT-RTX",
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioCardDark,
    title = {
      Text(
        text = if (isEdit) "Edit Cloud Fan Game Project" else "Create New Cloud Project",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = CyberCyan,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Project Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_game_title_input"),
        )
        OutlinedTextField(
          value = region,
          onValueChange = { region = it },
          label = { Text("Region Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_game_region_input"),
        )
        OutlinedTextField(
          value = synopsis,
          onValueChange = { synopsis = it },
          label = { Text("Synopsis & Architecture") },
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_game_synopsis_input"),
        )
        OutlinedTextField(
          value = consolePreset,
          onValueChange = { consolePreset = it },
          label = { Text("Console Matrix Preset") },
          singleLine = true,
          enabled = !captureEngineSettings || isEdit,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_game_preset_input"),
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          presetTemplates.forEach { preset ->
            FilterChip(
              selected = consolePreset == preset,
              onClick = {
                consolePreset = preset
                captureEngineSettings = false
              },
              label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = dexCountStr,
            onValueChange = { dexCountStr = it.filter { ch -> ch.isDigit() } },
            label = { Text("Dex") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("cloud_game_dex_input"),
          )
          OutlinedTextField(
            value = mapCountStr,
            onValueChange = { mapCountStr = it.filter { ch -> ch.isDigit() } },
            label = { Text("Maps") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("cloud_game_maps_input"),
          )
          OutlinedTextField(
            value = versionTag,
            onValueChange = { versionTag = it },
            label = { Text("Version") },
            singleLine = true,
            modifier = Modifier
              .weight(1.2f)
              .testTag("cloud_game_version_input"),
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDeepNavy)
            .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
            .clickable { captureEngineSettings = !captureEngineSettings }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Sync Active Engine Blend & Chips",
              style = MaterialTheme.typography.labelLarge,
              color = StudioTextPrimary,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Blend ${(currentFusionConfig.pixelTo3dBlend * 100).toInt()}% • ${currentFusionConfig.activeChipIds.size} chips",
              style = MaterialTheme.typography.labelSmall,
              color = Mode7Sky,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = captureEngineSettings,
            onCheckedChange = { captureEngineSettings = it },
            colors = SwitchDefaults.colors(checkedThumbColor = StudioDeepNavy, checkedTrackColor = CyberCyan),
            modifier = Modifier.testTag("capture_engine_settings_switch"),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            title.ifBlank { "Pokémon Omni-Horizon" },
            region.ifBlank { "Valkyra" },
            synopsis.ifBlank { "Multi-console fusion fan game." },
            consolePreset.ifBlank { currentFusionConfig.presetName },
            dexCountStr.toIntOrNull() ?: 251,
            mapCountStr.toIntOrNull() ?: 20,
            versionTag.ifBlank { "v1.0.0" },
            captureEngineSettings,
          )
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = CyberCyan,
          contentColor = StudioDeepNavy,
        ),
        modifier = Modifier.testTag("confirm_create_cloud_game_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Save,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isEdit) "Save Changes" else "Create & Switch",
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

/**
 * Top-bar Quick Project Switcher modal allowing instant project switching, saving, and creation from any workspace tab.
 */
@Composable
fun ProjectSwitcherDialog(
  cloudGamesState: UiState<List<FanGameProject>>,
  activeProject: FanGameProject?,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  onDismiss: () -> Unit,
  onSwitchProject: (FanGameProject) -> Unit,
  onSaveActiveProject: () -> Unit,
  onOpenNewProjectDialog: () -> Unit,
  onOpenFullProjectManager: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioCardDark,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.FolderOpen,
          contentDescription = null,
          tint = CyberCyan,
        )
        Text(
          text = "Switch Fan Game Project",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = StudioTextPrimary,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (activeProject != null && hasUnsavedChanges) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SuperFxGold.copy(alpha = 0.14f))
              .border(1.dp, SuperFxGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Unsaved edits in ${activeProject.title}",
                style = MaterialTheme.typography.labelLarge,
                color = SuperFxGold,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Save before switching so engine tweaks persist in Firestore.",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onSaveActiveProject,
              enabled = !isSavingProject,
              colors = ButtonDefaults.buttonColors(
                containerColor = SuperFxGold,
                contentColor = StudioDeepNavy,
              ),
              modifier = Modifier.testTag("switcher_save_active_button"),
            ) {
              Text("Save", fontWeight = FontWeight.Bold)
            }
          }
        }

        when (cloudGamesState) {
          is UiState.Loading -> {
            Text(
              text = "Syncing cloud projects from Firestore...",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextSecondary,
            )
          }

          is UiState.Error -> {
            Text(
              text = cloudGamesState.message,
              style = MaterialTheme.typography.bodySmall,
              color = ElectricCrimson,
            )
          }

          is UiState.Success -> {
            if (cloudGamesState.data.isEmpty()) {
              Text(
                text = "No cloud fan game projects yet. Create your first project below to sync builds with Firestore.",
                style = MaterialTheme.typography.bodyMedium,
                color = StudioTextSecondary,
              )
            } else {
              cloudGamesState.data.forEach { project ->
                val isActive = activeProject?.id == project.id
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) CyberCyan.copy(alpha = 0.14f) else StudioDeepNavy)
                    .border(
                      width = if (isActive) 2.dp else 1.dp,
                      color = if (isActive) CyberCyan else StudioBorderDark,
                      shape = RoundedCornerShape(12.dp),
                    )
                    .clickable {
                      onSwitchProject(project)
                      onDismiss()
                    }
                    .padding(12.dp)
                    .testTag("switcher_project_item_${project.id}"),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) CyberCyan else StudioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(6.dp))
                          .background(SuperFxGold.copy(alpha = 0.2f))
                          .padding(horizontal = 6.dp, vertical = 2.dp),
                      ) {
                        Text(
                          text = project.versionTag,
                          style = MaterialTheme.typography.labelSmall,
                          color = SuperFxGold,
                        )
                      }
                    }
                    Text(
                      text = "${project.regionName} • Blend ${project.pixel3dBlendRatio}% • ${project.dexCount} Dex",
                      style = MaterialTheme.typography.labelSmall,
                      color = StudioTextSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.SwapHoriz,
                    contentDescription = if (isActive) "Active Project" else "Switch to ${project.title}",
                    tint = if (isActive) SgbEmerald else CyberCyan,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenFullProjectManager()
          },
          modifier = Modifier.testTag("switcher_open_manager_button"),
        ) {
          Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = CyberCyan,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Manage", color = CyberCyan)
        }
        Button(
          onClick = {
            onDismiss()
            onOpenNewProjectDialog()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = CyberCyan,
            contentColor = StudioDeepNavy,
          ),
          modifier = Modifier.testTag("switcher_new_project_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("New", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
fun ActiveProjectHeaderPill(
  activeProject: FanGameProject?,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  onOpenSwitcher: () -> Unit,
  onQuickSave: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(StudioDeepNavy)
        .border(
          width = 1.dp,
          color = if (hasUnsavedChanges) SuperFxGold else CyberCyan.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
        )
        .clickable(onClick = onOpenSwitcher)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("top_project_switcher_button"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(
              when {
                activeProject == null -> StudioTextSecondary
                hasUnsavedChanges -> SuperFxGold
                else -> SgbEmerald
              }
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = activeProject?.title ?: "No Active Cloud Project",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (activeProject != null) {
              "${activeProject.versionTag} • ${activeProject.regionName}" +
                if (hasUnsavedChanges) " • Unsaved edits" else " • Synced"
            } else {
              "Tap to create or switch Firestore project"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (hasUnsavedChanges) SuperFxGold else CyberCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Icon(
        imageVector = Icons.Default.SwapHoriz,
        contentDescription = "Switch Project",
        tint = CyberCyan,
        modifier = Modifier.size(20.dp),
      )
    }

    if (activeProject != null) {
      Button(
        onClick = onQuickSave,
        enabled = !isSavingProject,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (hasUnsavedChanges) SuperFxGold else StudioCardDark,
          contentColor = if (hasUnsavedChanges) StudioDeepNavy else CyberCyan,
        ),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("top_quick_save_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Save,
          contentDescription = "Save Active Project",
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isSavingProject) "Saving" else "Save",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

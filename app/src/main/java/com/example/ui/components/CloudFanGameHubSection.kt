package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.ConsoleHardwareCatalog
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
import com.example.ui.theme.VoxelViolet
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CloudFanGameHubSection(
  cloudGamesState: UiState<List<FanGameProject>>,
  activeProject: FanGameProject?,
  currentFusionConfig: ConsoleFusionConfig,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onCreateCloudGame: (
    title: String,
    region: String,
    synopsis: String,
    consolePreset: String,
    dexCount: Int,
    mapCount: Int,
    versionTag: String,
    captureCurrentEngineSettings: Boolean,
  ) -> Unit,
  onUpdateCloudGame: (
    existing: FanGameProject,
    title: String,
    region: String,
    synopsis: String,
    consolePreset: String,
    dexCount: Int,
    mapCount: Int,
    versionTag: String,
    captureCurrentEngineSettings: Boolean,
  ) -> Unit,
  onSaveActiveProject: () -> Unit,
  onSwitchProject: (FanGameProject, Boolean) -> Unit,
  onDuplicateGame: (FanGameProject) -> Unit,
  onSeedStarterGames: () -> Unit,
  onProvisionMasterTemplate: () -> Unit,
  onDeleteGame: (String) -> Unit,
) {
  var showNewGameDialog by remember { mutableStateOf(false) }
  var editingProject by remember { mutableStateOf<FanGameProject?>(null) }
  var pendingDeleteProject by remember { mutableStateOf<FanGameProject?>(null) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Cloud Project Manager Hero Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Image(
          painter = painterResource(id = R.drawable.img_cloud_hub_banner_1794570185388),
          contentDescription = "Google Cloud Project Manager Banner",
          modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp)),
          contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Synced with Firestore",
                tint = CyberCyan,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "FIRESTORE PROJECT MANAGER",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }
            Text(
              text = "Create, Save & Switch Fan Game Projects",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = { showNewGameDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = CyberCyan,
              contentColor = StudioDeepNavy,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("new_cloud_game_button"),
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create New Project")
            Spacer(modifier = Modifier.width(4.dp))
            Text("New", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Active Workspace Control Strip
    ActiveWorkspaceProjectBanner(
      activeProject = activeProject,
      currentFusionConfig = currentFusionConfig,
      hasUnsavedChanges = hasUnsavedChanges,
      isSavingProject = isSavingProject,
      onSaveActiveProject = onSaveActiveProject,
      onEditActiveProject = { activeProject?.let { editingProject = it } },
      onOpenInEngine = { activeProject?.let { onSwitchProject(it, true) } },
      onCreateFromDraft = { showNewGameDialog = true },
    )

    // Search & Project Count Filter
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search projects by title, region, or preset") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = "Search Projects", tint = CyberCyan)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("project_search_input"),
      )

      if (cloudGamesState is UiState.Success) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardDark)
            .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
          Text(
            text = "${cloudGamesState.data.size} Saved",
            style = MaterialTheme.typography.labelLarge,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    when (cloudGamesState) {
      is UiState.Loading -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = CyberCyan)
        }
      }

      is UiState.Error -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = StudioCardDark),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Firestore Sync Alert",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = cloudGamesState.message,
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextSecondary,
            )
          }
        }
      }

      is UiState.Success -> {
        if (cloudGamesState.data.isEmpty()) {
          EmptyProjectsVaultCard(
            onCreateNew = { showNewGameDialog = true },
            onSeedStarterGames = onSeedStarterGames,
            onProvisionMasterTemplate = onProvisionMasterTemplate,
          )
        } else {
          val query = searchQuery.trim().lowercase()
          val filtered = if (query.isEmpty()) {
            cloudGamesState.data
          } else {
            cloudGamesState.data.filter { project ->
              project.title.lowercase().contains(query) ||
                project.regionName.lowercase().contains(query) ||
                project.consoleMatrixPreset.lowercase().contains(query) ||
                project.versionTag.lowercase().contains(query)
            }
          }

          if (filtered.isEmpty()) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = StudioCardDark),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "No projects match \"$searchQuery\"",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { onSearchQueryChange("") }) {
                  Text("Clear Filter", color = CyberCyan)
                }
              }
            }
          } else {
            filtered.forEach { game ->
              val isActive = activeProject?.id == game.id
              CloudFanGameCard(
                project = game,
                isActive = isActive,
                hasUnsavedChanges = isActive && hasUnsavedChanges,
                isSavingProject = isSavingProject,
                onSwitchToProject = { onSwitchProject(game, false) },
                onOpenInEngine = { onSwitchProject(game, true) },
                onSaveHere = onSaveActiveProject,
                onEdit = { editingProject = game },
                onDuplicate = { onDuplicateGame(game) },
                onDelete = { pendingDeleteProject = game },
              )
            }
          }
        }
      }
    }
  }

  if (showNewGameDialog) {
    ProjectEditorDialog(
      existingProject = null,
      currentFusionConfig = currentFusionConfig,
      onDismiss = { showNewGameDialog = false },
      onSave = { title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine ->
        onCreateCloudGame(title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine)
        showNewGameDialog = false
      },
    )
  }

  editingProject?.let { target ->
    ProjectEditorDialog(
      existingProject = target,
      currentFusionConfig = currentFusionConfig,
      onDismiss = { editingProject = null },
      onSave = { title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine ->
        onUpdateCloudGame(target, title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine)
        editingProject = null
      },
    )
  }

  pendingDeleteProject?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteProject = null },
      containerColor = StudioCardDark,
      title = {
        Text(
          text = "Delete Cloud Project?",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = ElectricCrimson,
        )
      },
      text = {
        Text(
          text = "This permanently removes \"${target.title}\" (${target.versionTag}) from your Firestore vault.",
          style = MaterialTheme.typography.bodyMedium,
          color = StudioTextSecondary,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteGame(target.id)
            pendingDeleteProject = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
          modifier = Modifier.testTag("confirm_delete_project_button"),
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteProject = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun ActiveWorkspaceProjectBanner(
  activeProject: FanGameProject?,
  currentFusionConfig: ConsoleFusionConfig,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  onSaveActiveProject: () -> Unit,
  onEditActiveProject: () -> Unit,
  onOpenInEngine: () -> Unit,
  onCreateFromDraft: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.5.dp,
        color = if (hasUnsavedChanges) SuperFxGold else SgbEmerald.copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
      )
      .testTag("active_project_banner"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (activeProject != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    if (hasUnsavedChanges) SuperFxGold.copy(alpha = 0.22f) else SgbEmerald.copy(alpha = 0.22f)
                  )
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = if (hasUnsavedChanges) "UNSAVED ENGINE CHANGES" else "ACTIVE & SYNCED",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (hasUnsavedChanges) SuperFxGold else SgbEmerald,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text = activeProject.versionTag,
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = activeProject.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
            )
            Text(
              text = "Engine: ${(currentFusionConfig.pixelTo3dBlend * 100).toInt()}% 3D • ${currentFusionConfig.activeChipIds.size} chips • ${formatProjectTimestamp(activeProject)}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onSaveActiveProject,
            enabled = !isSavingProject,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (hasUnsavedChanges) SuperFxGold else CyberCyan,
              contentColor = StudioDeepNavy,
            ),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("save_active_project_button"),
          ) {
            if (isSavingProject) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = StudioDeepNavy,
              )
            } else {
              Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (hasUnsavedChanges) "Save Changes to Cloud" else "Save Snapshot",
              fontWeight = FontWeight.Bold,
            )
          }

          OutlinedButton(
            onClick = onEditActiveProject,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("edit_active_project_button"),
          ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Details", color = CyberCyan)
          }

          OutlinedButton(
            onClick = onOpenInEngine,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("open_active_in_engine_button"),
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Mode7Sky)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Engine", color = Mode7Sky)
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "UNSAVED WORKSPACE DRAFT",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
            )
            Text(
              text = "Select a project below or save your current engine state as a new cloud project.",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextSecondary,
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = onCreateFromDraft,
            colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("save_draft_as_project_button"),
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Draft", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyProjectsVaultCard(
  onCreateNew: () -> Unit,
  onSeedStarterGames: () -> Unit,
  onProvisionMasterTemplate: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, SuperFxGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Default.CloudUpload,
        contentDescription = "Empty Firestore Vault",
        tint = SuperFxGold,
        modifier = Modifier.size(36.dp),
      )
      Text(
        text = "Your Firestore Project Vault is Empty",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Start from the unified Master Canon Base Template (all mainline games & anime), create a custom project, or seed starters.",
        style = MaterialTheme.typography.bodyMedium,
        color = StudioTextSecondary,
      )
      Button(
        onClick = onProvisionMasterTemplate,
        colors = ButtonDefaults.buttonColors(
          containerColor = SuperFxGold,
          contentColor = StudioDeepNavy,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("empty_vault_master_template_button"),
      ) {
        Text("Clone Master Canon Base Template", fontWeight = FontWeight.Bold)
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onCreateNew,
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text("Create Custom", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = onSeedStarterGames,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("seed_starter_cloud_games_button"),
        ) {
          Text("Seed Starters", color = SuperFxGold, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun CloudFanGameCard(
  project: FanGameProject,
  isActive: Boolean,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  onSwitchToProject: () -> Unit,
  onOpenInEngine: () -> Unit,
  onSaveHere: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
) {
  val chipLabels = remember(project.activeChipsCsv) {
    val catalog = ConsoleHardwareCatalog.officialAndFanChips
    project.activeChipsCsv.split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .map { token ->
        catalog.firstOrNull {
          it.id.equals(token, ignoreCase = true) || it.name.contains(token, ignoreCase = true)
        }?.name?.substringBefore(" (") ?: token
      }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isActive) 2.dp else 1.dp,
        color = if (isActive) CyberCyan else StudioBorderDark,
        shape = RoundedCornerShape(16.dp),
      )
      .testTag("cloud_game_card_${project.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isActive) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(SgbEmerald.copy(alpha = 0.24f))
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "ACTIVE",
                  style = MaterialTheme.typography.labelSmall,
                  color = SgbEmerald,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            Text(
              text = project.title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = if (isActive) CyberCyan else StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
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
            text = "Region: ${project.regionName} • ${project.consoleMatrixPreset}",
            style = MaterialTheme.typography.labelSmall,
            color = Mode7Sky,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row {
          IconButton(
            onClick = onDuplicate,
            modifier = Modifier
              .size(48.dp)
              .testTag("duplicate_project_button_${project.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Duplicate ${project.title}",
              tint = CyberCyan,
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_project_button_${project.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Delete ${project.title}",
              tint = ElectricCrimson,
            )
          }
        }
      }

      Text(
        text = project.synopsis,
        style = MaterialTheme.typography.bodyMedium,
        color = StudioTextSecondary,
      )

      if (chipLabels.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          chipLabels.forEach { label ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VoxelViolet.copy(alpha = 0.2f))
                .border(1.dp, VoxelViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextPrimary,
              )
            }
          }
        }
      }

      // 2D/3D Blend Ratio Telemetry Bar
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Dex: ${project.dexCount} • Maps: ${project.mapCount} • Blend: ${project.pixel3dBlendRatio}% 3D",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
          )
          Text(
            text = formatProjectTimestamp(project),
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )
        }
        LinearProgressIndicator(
          progress = { (project.pixel3dBlendRatio / 100f).coerceIn(0f, 1f) },
          color = if (isActive) CyberCyan else VoxelViolet,
          trackColor = StudioDeepNavy,
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        )
      }

      // Action Buttons Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isActive) {
          if (hasUnsavedChanges) {
            Button(
              onClick = onSaveHere,
              enabled = !isSavingProject,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("save_card_project_button_${project.id}"),
            ) {
              Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save Engine", fontWeight = FontWeight.Bold)
            }
          }
          Button(
            onClick = onOpenInEngine,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("open_engine_project_button_${project.id}"),
          ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open Engine", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onSwitchToProject,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("switch_project_button_${project.id}"),
          ) {
            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Switch To", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onOpenInEngine,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("switch_and_open_project_button_${project.id}"),
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Mode7Sky)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Load & Edit", color = Mode7Sky)
          }
        }

        OutlinedButton(
          onClick = onEdit,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("edit_project_button_${project.id}"),
        ) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioTextPrimary)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Edit", color = StudioTextPrimary)
        }
      }
    }
  }
}

private fun formatProjectTimestamp(project: FanGameProject): String {
  val date = project.updatedAt?.toDate() ?: project.createdAt?.toDate() ?: return "Just synced"
  val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
  return "Saved ${formatter.format(date)}"
}

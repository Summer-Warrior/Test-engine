package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.FanGameProject
import com.example.engine.HardwarePlatformSpec
import com.example.engine.UniversalHardwareRegistry
import com.example.ui.ProjectOverviewStats
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
import java.util.Date
import java.util.Locale

/**
 * Project Dashboard displaying the real-time Project Overview statistics component
 * alongside fan game projects stored in the Local Studio Vault (Offline Room v6 DB),
 * with Android Storage Access Framework JSON Export/Import, Standalone Comparison ROMs, and 'View Details'.
 */
@Composable
fun ProjectDashboard(
  cloudGamesState: UiState<List<FanGameProject>>,
  projectOverviewState: UiState<ProjectOverviewStats?>,
  allHardwarePlatforms: List<HardwarePlatformSpec>,
  activeProjectId: String?,
  hasUnsavedChanges: Boolean,
  onSavePendingChanges: () -> Unit,
  onOpenProjectSwitcher: () -> Unit,
  onSwitchAndOpenProject: (FanGameProject) -> Unit,
  onOpenProjectManager: () -> Unit,
  onCreateNewProject: () -> Unit,
  onSeedStarterProjects: () -> Unit,
  onExportActiveProjectJson: () -> Unit,
  onImportProjectJson: () -> Unit,
  compiledRoms: List<com.example.engine.PrismRomCartridge> = emptyList(),
  activeProjectTitle: String = "Pokémon: Prism Resonance",
  onGenerateAllTenRoms: () -> Unit = {},
  onCompileCurrentStudioRom: () -> Unit = {},
  onImportPrismRom: () -> Unit = {},
  onLaunchStandaloneRom: (com.example.engine.PrismRomCartridge) -> Unit = {},
  onInstallHomeShortcut: (com.example.engine.PrismRomCartridge) -> Unit = {},
  onExportPrismRom: (com.example.engine.PrismRomCartridge) -> Unit = {},
  onDeleteCompiledRom: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var selectedProjectForDetails by remember { mutableStateOf<FanGameProject?>(null) }
  var showComparisonRomsSheet by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("project_dashboard_container"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Live Offline Room DB Project Overview Card for the currently selected project
    ProjectOverviewDashboard(
      overviewState = projectOverviewState,
      hasUnsavedChanges = hasUnsavedChanges,
      onSavePendingChanges = onSavePendingChanges,
      onSwitchProject = onOpenProjectSwitcher,
      onCreateProject = onCreateNewProject,
    )

    // Dashboard Summary Header Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Local Studio Vault Dashboard",
                tint = CyberCyan,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "LOCAL STUDIO VAULT • OFFLINE ROOM DB",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Your Fan Game Projects",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
            )
          }

          Button(
            onClick = onCreateNewProject,
            colors = ButtonDefaults.buttonColors(
              containerColor = CyberCyan,
              contentColor = StudioDeepNavy,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("dashboard_new_project_button"),
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Project")
            Spacer(modifier = Modifier.width(4.dp))
            Text("New", fontWeight = FontWeight.Bold)
          }
        }

        val totalCount = (cloudGamesState as? UiState.Success)?.data?.size ?: 0
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          DashboardMetricChip(
            label = "Vault Projects",
            value = totalCount.toString(),
            accentColor = CyberCyan,
            modifier = Modifier.weight(1f),
          )
          DashboardMetricChip(
            label = "Storage Engine",
            value = when (cloudGamesState) {
              is UiState.Loading -> "Loading..."
              is UiState.Error -> "Notice"
              is UiState.Success -> "Offline Room v6"
            },
            accentColor = when (cloudGamesState) {
              is UiState.Loading -> SuperFxGold
              is UiState.Error -> ElectricCrimson
              is UiState.Success -> SgbEmerald
            },
            modifier = Modifier.weight(1f),
          )
        }

        // Local JSON Project Export & Import (Android Storage Access Framework)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onExportActiveProjectJson,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("export_project_json_button"),
          ) {
            Icon(
              imageVector = Icons.Default.FileUpload,
              contentDescription = "Export Active Project to JSON",
              tint = SuperFxGold,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export (.json)", color = SuperFxGold, fontWeight = FontWeight.Bold, maxLines = 1)
          }
          OutlinedButton(
            onClick = onImportProjectJson,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("import_project_json_button"),
          ) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = "Import Project from JSON",
              tint = CyberCyan,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Import (.json)", color = CyberCyan, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        // Standalone Comparison ROMs (.prismrom) Compiler & Home-Screen Installer Launcher
        Button(
          onClick = { showComparisonRomsSheet = !showComparisonRomsSheet },
          colors = ButtonDefaults.buttonColors(
            containerColor = SuperFxGold,
            contentColor = StudioDeepNavy,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("dashboard_comparison_roms_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Standalone Comparison ROMs",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (showComparisonRomsSheet) {
              "Hide Comparison ROMs Studio (${compiledRoms.size})"
            } else {
              "Comparison ROMs — Compile & Install Standalone (${compiledRoms.size})"
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }

    if (showComparisonRomsSheet) {
      RomComparisonExporterSheet(
        compiledRoms = compiledRoms,
        activeProjectTitle = activeProjectTitle,
        onGenerateAllTenRoms = onGenerateAllTenRoms,
        onCompileCurrentStudioRom = onCompileCurrentStudioRom,
        onImportPrismRom = onImportPrismRom,
        onLaunchStandaloneRom = onLaunchStandaloneRom,
        onInstallHomeShortcut = onInstallHomeShortcut,
        onExportPrismRom = onExportPrismRom,
        onDeleteCompiledRom = onDeleteCompiledRom,
        onClose = { showComparisonRomsSheet = false },
      )
    }

    // Offline Room Project List
    when (cloudGamesState) {
      is UiState.Loading -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StudioCardDark),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(color = CyberCyan)
            Text(
              text = "Loading fan game projects from Local Room Vault...",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextSecondary,
            )
          }
        }
      }

      is UiState.Error -> {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricCrimson.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StudioCardDark),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Unable to Load Projects",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ElectricCrimson,
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
        val projects = cloudGamesState.data
        if (projects.isEmpty()) {
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
                .padding(22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "No Local Vault Projects Yet",
                tint = SuperFxGold,
                modifier = Modifier.size(36.dp),
              )
              Text(
                text = "No Fan Game Projects in Local Vault Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
              )
              Text(
                text = "Create a new fan game project, import a .json bundle, or seed starter multi-console projects.",
                style = MaterialTheme.typography.bodyMedium,
                color = StudioTextSecondary,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = onCreateNewProject,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = StudioDeepNavy,
                  ),
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("dashboard_empty_create_button"),
                ) {
                  Text("Create Project", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = onSeedStarterProjects,
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("dashboard_seed_starters_button"),
                ) {
                  Text("Seed Starters", color = SuperFxGold)
                }
              }
            }
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            projects.forEach { project ->
              ProjectDashboardRowCard(
                project = project,
                isActive = project.id == activeProjectId,
                onViewDetails = { selectedProjectForDetails = project },
              )
            }
          }
        }
      }
    }
  }

  selectedProjectForDetails?.let { project ->
    ProjectDetailsDialog(
      project = project,
      allHardwarePlatforms = allHardwarePlatforms,
      onDismiss = { selectedProjectForDetails = null },
      onSwitchAndOpen = {
        selectedProjectForDetails = null
        onSwitchAndOpenProject(project)
      },
      onManageProject = {
        selectedProjectForDetails = null
        onOpenProjectManager()
      },
    )
  }
}

@Composable
private fun DashboardMetricChip(
  label: String,
  value: String,
  accentColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(StudioDeepNavy)
      .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextSecondary,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ProjectDashboardRowCard(
  project: FanGameProject,
  isActive: Boolean,
  onViewDetails: () -> Unit,
) {
  val createdDateText = remember(project.createdAtMs) {
    formatCreationDate(project)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isActive) 2.dp else 1.dp,
        color = if (isActive) CyberCyan else StudioBorderDark,
        shape = RoundedCornerShape(16.dp),
      )
      .testTag("dashboard_project_card_${project.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (isActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SgbEmerald.copy(alpha = 0.22f))
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
            text = project.title.ifBlank { "Untitled Fan Game" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isActive) CyberCyan else StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Creation Date",
            tint = Mode7Sky,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "Created $createdDateText",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      Button(
        onClick = onViewDetails,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = CyberCyan,
          contentColor = StudioDeepNavy,
        ),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("view_details_button_${project.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Visibility,
          contentDescription = "View Details for ${project.title}",
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "View Details",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun ProjectDetailsDialog(
  project: FanGameProject,
  allHardwarePlatforms: List<HardwarePlatformSpec>,
  onDismiss: () -> Unit,
  onSwitchAndOpen: () -> Unit,
  onManageProject: () -> Unit,
) {
  val (hardwareHeadline, hardwareBadges) = remember(project.activeChipsCsv, allHardwarePlatforms) {
    UniversalHardwareRegistry.formatReadableHardwareSummary(project.activeChipsCsv, allHardwarePlatforms)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioCardDark,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = project.title.ifBlank { "Untitled Fan Game" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CyberCyan,
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
          text = "Region: ${project.regionName}",
          style = MaterialTheme.typography.labelMedium,
          color = Mode7Sky,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = project.synopsis,
          style = MaterialTheme.typography.bodyMedium,
          color = StudioTextPrimary,
        )

        HorizontalDivider(color = StudioBorderDark)

        DetailKeyValueRow(label = "Created", value = formatCreationDate(project))
        DetailKeyValueRow(label = "Last Updated", value = formatUpdatedDate(project))
        DetailKeyValueRow(label = "Console Matrix", value = project.consoleMatrixPreset)
        DetailKeyValueRow(label = "Regional Dex", value = "${project.dexCount} Species")
        DetailKeyValueRow(label = "World Maps", value = "${project.mapCount} Maps")

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "2D Pixel / 3D Model Fusion Blend: ${project.pixel3dBlendRatio}%",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
          )
          LinearProgressIndicator(
            progress = { (project.pixel3dBlendRatio / 100f).coerceIn(0f, 1f) },
            color = CyberCyan,
            trackColor = StudioDeepNavy,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
          )
        }

        if (hardwareBadges.isNotEmpty()) {
          Text(
            text = "Hardware Convergence: $hardwareHeadline",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            fontWeight = FontWeight.SemiBold,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            hardwareBadges.forEach { badge ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(VoxelViolet.copy(alpha = 0.2f))
                  .border(1.dp, VoxelViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp),
              ) {
                Text(
                  text = badge,
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextPrimary,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onManageProject,
          modifier = Modifier.testTag("details_manage_project_button"),
        ) {
          Text("Manage", color = CyberCyan)
        }
        Button(
          onClick = onSwitchAndOpen,
          colors = ButtonDefaults.buttonColors(
            containerColor = CyberCyan,
            contentColor = StudioDeepNavy,
          ),
          modifier = Modifier.testTag("details_open_in_engine_button"),
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Load in Engine", fontWeight = FontWeight.Bold)
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
private fun DetailKeyValueRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = StudioTextSecondary,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = StudioTextPrimary,
    )
  }
}

private fun formatCreationDate(project: FanGameProject): String {
  if (project.createdAtMs <= 0L) return "Just created"
  val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
  return formatter.format(Date(project.createdAtMs))
}

private fun formatUpdatedDate(project: FanGameProject): String {
  val ms = if (project.updatedAtMs > 0L) project.updatedAtMs else project.createdAtMs
  if (ms <= 0L) return "Just saved"
  val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
  return formatter.format(Date(ms))
}

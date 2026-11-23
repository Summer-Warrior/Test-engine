package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
 * Project Overview Dashboard component displaying real-time statistics
 * (Asset Count, Last Modified, Maps/Dex, and 2D/3D Fusion Blend)
 * streamed directly from the Local Offline Room v4 Database for the currently selected project.
 */
@Composable
fun ProjectOverviewDashboard(
  overviewState: UiState<ProjectOverviewStats?>,
  hasUnsavedChanges: Boolean,
  onSavePendingChanges: () -> Unit,
  onSwitchProject: () -> Unit,
  onCreateProject: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.5.dp,
        color = CyberCyan.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
      )
      .testTag("project_overview_dashboard"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header Bar
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
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                  when (overviewState) {
                    is UiState.Success -> if (overviewState.data != null) SgbEmerald else StudioTextSecondary
                    is UiState.Loading -> SuperFxGold
                    is UiState.Error -> ElectricCrimson
                  }
                ),
            )
            Text(
              text = "REAL-TIME PROJECT OVERVIEW",
              style = MaterialTheme.typography.labelSmall,
              color = SgbEmerald,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = when (overviewState) {
              is UiState.Success -> overviewState.data?.projectTitle ?: "No Project Selected"
              is UiState.Loading -> "Fetching Live Statistics..."
              is UiState.Error -> "Telemetry Interrupted"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        OutlinedButton(
          onClick = onSwitchProject,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("overview_switch_project_button"),
        ) {
          Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Switch Selected Project",
            tint = CyberCyan,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Switch", color = CyberCyan)
        }
      }

      when (overviewState) {
        is UiState.Loading -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              color = CyberCyan,
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Loading live project statistics from Room DB...",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextSecondary,
            )
          }
        }

        is UiState.Error -> {
          Text(
            text = overviewState.message,
            style = MaterialTheme.typography.bodySmall,
            color = ElectricCrimson,
          )
        }

        is UiState.Success -> {
          val stats = overviewState.data
          if (stats == null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StudioDeepNavy)
                .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Select or create a fan game project to view real-time offline Room DB asset counts and modification telemetry.",
                  style = MaterialTheme.typography.bodySmall,
                  color = StudioTextSecondary,
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(
                  containerColor = CyberCyan,
                  contentColor = StudioDeepNavy,
                ),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("overview_create_project_button"),
              ) {
                Text("Create", fontWeight = FontWeight.Bold)
              }
            }
          } else {
            // Project subtitle tags
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(SuperFxGold.copy(alpha = 0.2f))
                  .padding(horizontal = 8.dp, vertical = 2.dp),
              ) {
                Text(
                  text = stats.versionTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = SuperFxGold,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text = "${stats.regionName} • ${stats.consoleMatrixPreset}",
                style = MaterialTheme.typography.labelSmall,
                color = Mode7Sky,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            // Primary Real-Time Stats Grid (Asset Count + Last Modified)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OverviewStatTile(
                icon = Icons.Default.Inventory2,
                label = "Asset Count",
                primaryValue = "${stats.assetCount}",
                subtitle = "${stats.pinnedCloudAssetsCount} Vault • ${stats.customDexCount} Dex • ${stats.activeChipsCount} HW",
                accentColor = CyberCyan,
                modifier = Modifier
                  .weight(1f)
                  .testTag("overview_stat_asset_count"),
              )
              OverviewStatTile(
                icon = Icons.Default.Schedule,
                label = "Last Modified",
                primaryValue = formatOverviewTimeShort(stats.lastModifiedMs),
                subtitle = formatOverviewDateFull(stats.lastModifiedMs),
                accentColor = SuperFxGold,
                modifier = Modifier
                  .weight(1f)
                  .testTag("overview_stat_last_modified"),
              )
            }

            // Secondary Telemetry Row (Maps/Dex + 2D/3D Fusion Ratio)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OverviewStatTile(
                icon = Icons.Default.Layers,
                label = "World Scope",
                primaryValue = "${stats.mapCount} Maps",
                subtitle = "${stats.dexTargetCount} Regional Dex Target",
                accentColor = VoxelViolet,
                modifier = Modifier
                  .weight(1f)
                  .testTag("overview_stat_world_scope"),
              )
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(StudioDeepNavy)
                  .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
                  .padding(12.dp)
                  .testTag("overview_stat_blend"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "2D/3D Fusion Blend",
                    tint = SgbEmerald,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "2D/3D Fusion",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary,
                  )
                }
                Text(
                  text = "${stats.blendRatioPercent}% 3D",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = SgbEmerald,
                )
                LinearProgressIndicator(
                  progress = { (stats.blendRatioPercent / 100f).coerceIn(0f, 1f) },
                  color = SgbEmerald,
                  trackColor = StudioCardDark,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                )
              }
            }

            if (hasUnsavedChanges) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(SuperFxGold.copy(alpha = 0.14f))
                  .border(1.dp, SuperFxGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = SuperFxGold,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Local engine changes waiting to save",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuperFxGold,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                Button(
                  onClick = onSavePendingChanges,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = SuperFxGold,
                    contentColor = StudioDeepNavy,
                  ),
                  modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("overview_save_changes_button"),
                ) {
                  Text("Save Now", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OverviewStatTile(
  icon: ImageVector,
  label: String,
  primaryValue: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(StudioDeepNavy)
      .border(1.dp, StudioBorderDark, RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accentColor,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )
    }
    Text(
      text = primaryValue,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextSecondary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun formatOverviewTimeShort(epochMs: Long?): String {
  if (epochMs == null || epochMs <= 0L) return "Just now"
  val date = Date(epochMs)
  val diffSeconds = ((System.currentTimeMillis() - epochMs) / 1000L).coerceAtLeast(0L)
  return when {
    diffSeconds < 60L -> "Just now"
    diffSeconds < 3600L -> "${diffSeconds / 60L}m ago"
    diffSeconds < 86400L -> "${diffSeconds / 3600L}h ago"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
  }
}

private fun formatOverviewDateFull(epochMs: Long?): String {
  if (epochMs == null || epochMs <= 0L) return "Saved locally in Room DB"
  return SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
}

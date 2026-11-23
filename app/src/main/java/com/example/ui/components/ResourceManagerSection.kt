package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.ResourceCategory
import com.example.data.local.StudioResourceEntity
import com.example.data.model.FanGameProject
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.Mode7Sky
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.roundToInt

/**
 * Local Room-backed Resource Manager UI Panel categorizing SFX, Background Music, and Map Tiles
 * with interactive drag-and-drop assignment directly onto Fan Game Project drop targets.
 */
@Composable
fun ResourceManagerSection(
  resources: List<StudioResourceEntity>,
  projects: List<FanGameProject>,
  activeProject: FanGameProject?,
  onDropResourceToProject: (StudioResourceEntity, FanGameProject) -> Unit,
  onUnassignResourceFromProject: (StudioResourceEntity, FanGameProject) -> Unit,
  onPreviewResource: (StudioResourceEntity) -> Unit,
  onAddCustomResource: (
    name: String,
    category: ResourceCategory,
    formatTag: String,
    consoleChip: String,
    durationOrGrid: String,
    description: String,
  ) -> Unit,
  onDeleteCustomResource: (String) -> Unit,
) {
  var selectedCategoryFilter by remember { mutableStateOf<ResourceCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }

  var rootPositionInWindow by remember { mutableStateOf(Offset.Zero) }
  val dropTargetBounds = remember { mutableStateMapOf<String, Rect>() }
  var draggingResource by remember { mutableStateOf<StudioResourceEntity?>(null) }
  var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }
  var hoveredProjectId by remember { mutableStateOf<String?>(null) }

  val filteredResources = remember(resources, selectedCategoryFilter, searchQuery) {
    resources.filter { res ->
      val catMatch = selectedCategoryFilter == null || res.categoryId == selectedCategoryFilter!!.id
      val qMatch = searchQuery.isBlank() ||
        res.name.contains(searchQuery, ignoreCase = true) ||
        res.formatTag.contains(searchQuery, ignoreCase = true) ||
        res.consoleChip.contains(searchQuery, ignoreCase = true) ||
        res.description.contains(searchQuery, ignoreCase = true)
      catMatch && qMatch
    }
  }

  val dropTargets = remember(projects, activeProject) {
    buildList {
      if (activeProject != null) add(activeProject)
      projects.forEach { proj ->
        if (none { it.id == proj.id } && size < 4) add(proj)
      }
    }
  }

  fun updateHoverProject(pointerRoot: Offset) {
    hoveredProjectId = dropTargetBounds.entries.firstOrNull { (_, rect) ->
      rect.contains(pointerRoot)
    }?.key
  }

  fun finishDragDrop() {
    val dragged = draggingResource
    val targetId = hoveredProjectId
    if (dragged != null && targetId != null) {
      dropTargets.firstOrNull { it.id == targetId }?.let { project ->
        onDropResourceToProject(dragged, project)
      }
    }
    draggingResource = null
    hoveredProjectId = null
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        rootPositionInWindow = coords.positionInRoot()
      },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Header & Room Summary Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardDark),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "LOCAL ROOM ASSET LIBRARY & DRAG-DROP DOCK",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
              Text(
                text = "Resource Manager (${resources.size} Room Assets)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            Button(
              onClick = { showAddDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("resource_add_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add local Room resource", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("New Asset", fontWeight = FontWeight.Bold)
            }
          }

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search resources") },
            placeholder = { Text("Search SFX, BGM tracks, or Mode-7 map tiles...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("resource_search_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilterChip(
              selected = selectedCategoryFilter == null,
              onClick = { selectedCategoryFilter = null },
              label = { Text("All (${resources.size})") },
              modifier = Modifier.testTag("resource_filter_all"),
            )
            ResourceCategory.entries.forEach { cat ->
              val count = resources.count { it.categoryId == cat.id }
              FilterChip(
                selected = selectedCategoryFilter == cat,
                onClick = { selectedCategoryFilter = cat },
                leadingIcon = {
                  Icon(
                    imageVector = iconForResourceCategory(cat),
                    contentDescription = cat.label,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text("${cat.label} ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                  selectedLabelColor = CyberCyan,
                ),
                modifier = Modifier.testTag("resource_filter_${cat.id}"),
              )
            }
          }
        }
      }

      // Game Project Drag-and-Drop Target Dock
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (draggingResource != null) 2.dp else 1.dp,
            color = if (draggingResource != null) SgbEmerald else StudioBorderDark,
            shape = RoundedCornerShape(18.dp),
          ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.FolderSpecial, contentDescription = "Project drop targets", tint = SuperFxGold, modifier = Modifier.size(18.dp))
            Text(
              text = if (draggingResource != null) {
                "Drop '${draggingResource?.name}' onto a Game Project below"
              } else {
                "Game Project Drop Targets (Drag any handle below into a project)"
              },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (draggingResource != null) SgbEmerald else StudioTextPrimary,
            )
          }

          if (dropTargets.isEmpty()) {
            Text(
              text = "Create or select a Fan Game Project in Project Manager to enable drag-and-drop attachment.",
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextSecondary,
            )
          } else {
            dropTargets.forEach { project ->
              val isHovered = hoveredProjectId == project.id
              val isActive = project.id == activeProject?.id
              val attachedCount = resources.count { it.isAssignedToProject(project.id) }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                    when {
                      isHovered -> SgbEmerald.copy(alpha = 0.28f)
                      isActive -> CyberCyan.copy(alpha = 0.12f)
                      else -> StudioDeepNavy
                    }
                  )
                  .border(
                    width = if (isHovered || isActive) 2.dp else 1.dp,
                    color = when {
                      isHovered -> SgbEmerald
                      isActive -> CyberCyan
                      else -> StudioBorderDark
                    },
                    shape = RoundedCornerShape(12.dp),
                  )
                  .onGloballyPositioned { coords ->
                    dropTargetBounds[project.id] = coords.boundsInRoot()
                  }
                  .padding(12.dp)
                  .testTag("project_drop_zone_${project.id}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = (if (isActive) "★ ACTIVE: " else "") + project.title.ifBlank { "Untitled Fan Game" },
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isHovered) SgbEmerald else StudioTextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${project.regionName} • ${project.versionTag} • $attachedCount Room assets attached",
                      style = MaterialTheme.typography.labelSmall,
                      color = StudioTextSecondary,
                    )
                  }
                  if (isHovered) {
                    Text(
                      text = "RELEASE TO ATTACH",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = SgbEmerald,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Categorized Resource Cards with Drag Handles
      filteredResources.forEach { res ->
        val category = ResourceCategory.entries.firstOrNull { it.id == res.categoryId } ?: ResourceCategory.SFX
        val accent = parseHexColorSafe(res.accentHex, CyberCyan)
        val assignedToActive = activeProject != null && res.isAssignedToProject(activeProject.id)
        var cardPositionInRoot by remember(res.id) { mutableStateOf(Offset.Zero) }
        val latestRes by rememberUpdatedState(res)

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = if (draggingResource?.id == res.id) 2.dp else 1.dp,
              color = if (draggingResource?.id == res.id) SuperFxGold else accent.copy(alpha = 0.5f),
              shape = RoundedCornerShape(16.dp),
            )
            .testTag("resource_card_${res.id}"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StudioCardDark),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Dedicated Drag Handle Box (48dp touch target)
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(accent.copy(alpha = 0.16f))
                  .border(1.dp, accent, RoundedCornerShape(12.dp))
                  .onGloballyPositioned { coords ->
                    cardPositionInRoot = coords.positionInRoot()
                  }
                  .pointerInput(res.id) {
                    detectDragGestures(
                      onDragStart = { localOffset ->
                        draggingResource = latestRes
                        val rootPos = cardPositionInRoot + localOffset
                        dragPointerInRoot = rootPos
                        updateHoverProject(rootPos)
                      },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        dragPointerInRoot += dragAmount
                        updateHoverProject(dragPointerInRoot)
                      },
                      onDragEnd = { finishDragDrop() },
                      onDragCancel = {
                        draggingResource = null
                        hoveredProjectId = null
                      },
                    )
                  }
                  .testTag("resource_drag_handle_${res.id}"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.DragIndicator,
                  contentDescription = "Drag ${res.name} into a game project",
                  tint = accent,
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = category.shortBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = res.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = "${res.formatTag} • ${res.durationOrGrid} • ${res.consoleChip}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SuperFxGold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              IconButton(
                onClick = { onPreviewResource(res) },
                modifier = Modifier.size(48.dp).testTag("resource_preview_${res.id}"),
              ) {
                Icon(
                  imageVector = if (category == ResourceCategory.MAP_TILES) Icons.Default.GridView else Icons.Default.PlayArrow,
                  contentDescription = "Preview ${res.name}",
                  tint = SgbEmerald,
                )
              }
            }

            Text(
              text = res.description,
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextSecondary,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Assigned to ${res.assignedProjectCount()} project(s)",
                style = MaterialTheme.typography.labelSmall,
                color = if (assignedToActive) SgbEmerald else StudioTextSecondary,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (activeProject != null) {
                  OutlinedButton(
                    onClick = {
                      if (assignedToActive) {
                        onUnassignResourceFromProject(res, activeProject)
                      } else {
                        onDropResourceToProject(res, activeProject)
                      }
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("resource_assign_${res.id}"),
                  ) {
                    if (assignedToActive) {
                      Icon(Icons.Default.CheckCircle, contentDescription = "Attached to project", tint = SgbEmerald, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Attached", color = SgbEmerald)
                    } else {
                      Text("Attach to Active", color = CyberCyan)
                    }
                  }
                }
                if (!res.isBuiltIn) {
                  IconButton(
                    onClick = { onDeleteCustomResource(res.id) },
                    modifier = Modifier.size(48.dp),
                  ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete custom resource", tint = ElectricCrimson)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Floating Drag Ghost Pill rendered cleanly over workspace while dragging
    val activeDrag = draggingResource
    if (activeDrag != null) {
      val localOffset = dragPointerInRoot - rootPositionInWindow
      Surface(
        color = CyberCyan,
        contentColor = StudioDeepNavy,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
          .offset {
            IntOffset(
              x = (localOffset.x - 80f).roundToInt(),
              y = (localOffset.y - 60f).roundToInt(),
            )
          }
          .border(2.dp, SuperFxGold, RoundedCornerShape(12.dp)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(
            text = activeDrag.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }

  if (showAddDialog) {
    AddLocalResourceDialog(
      onDismiss = { showAddDialog = false },
      onCreate = { name, category, format, chip, metric, desc ->
        onAddCustomResource(name, category, format, chip, metric, desc)
        showAddDialog = false
      },
    )
  }
}

private fun iconForResourceCategory(category: ResourceCategory) = when (category) {
  ResourceCategory.SFX -> Icons.Default.VolumeUp
  ResourceCategory.BGM -> Icons.Default.LibraryMusic
  ResourceCategory.MAP_TILES -> Icons.Default.GridView
}

@Composable
private fun AddLocalResourceDialog(
  onDismiss: () -> Unit,
  onCreate: (
    name: String,
    category: ResourceCategory,
    formatTag: String,
    consoleChip: String,
    durationOrGrid: String,
    description: String,
  ) -> Unit,
) {
  var name by remember { mutableStateOf("Celadon Neon Overworld Theme") }
  var selectedCategory by remember { mutableStateOf(ResourceCategory.BGM) }
  var formatTag by remember { mutableStateOf("MSU-1 44.1kHz PCM") }
  var consoleChip by remember { mutableStateOf("MSU-1 + Super FX2") }
  var durationOrGrid by remember { mutableStateOf("02:10 loop") }
  var description by remember { mutableStateOf("Custom Room asset ready for drag-and-drop into any fan game project.") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioCardDark,
    title = { Text("Add Asset to Room Database", color = CyberCyan, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ResourceCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.shortBadge) },
            )
          }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name") }, singleLine = true)
        OutlinedTextField(value = formatTag, onValueChange = { formatTag = it }, label = { Text("Format / Compression") }, singleLine = true)
        OutlinedTextField(value = consoleChip, onValueChange = { consoleChip = it }, label = { Text("Hardware Chip") }, singleLine = true)
        OutlinedTextField(value = durationOrGrid, onValueChange = { durationOrGrid = it }, label = { Text("Duration / Tile Size") }, singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") }, maxLines = 2)
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name, selectedCategory, formatTag, consoleChip, durationOrGrid, description) },
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
        modifier = Modifier.testTag("confirm_add_resource_button"),
      ) {
        Text("Save to Room")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

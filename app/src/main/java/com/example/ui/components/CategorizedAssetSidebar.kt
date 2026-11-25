package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomTrainerEntity
import com.example.data.local.NativePokemonEntity
import com.example.data.local.SavedSpriteEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ConsoleHardwareCatalog
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
import com.example.ui.BuiltInAssetItem
import com.example.ui.StudioViewModel
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

/**
 * Permanent navigation drawer / sidebar categorizing game assets
 * (Sprites, 3D Models, Textures, Tilesets, Chips & Shaders, World Maps, Audio),
 * with an integrated database search bar and active project quick access.
 */
@Composable
fun CategorizedAssetSidebar(
  selectedCategory: AssetCategory,
  isExpanded: Boolean,
  customCloudAssets: List<CustomStudioAsset>,
  nativeDatabasePokemon: List<NativePokemonEntity>,
  customCloudDex: List<CustomDexEntry>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  activeProject: FanGameProject?,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  filterByActiveProject: Boolean,
  onToggleFilterByActiveProject: () -> Unit,
  onSelectCategory: (AssetCategory) -> Unit,
  onToggleExpand: () -> Unit,
  onSelectAsset: (BuiltInAssetItem) -> Unit,
  onPinCurrentFusionAsset: () -> Unit,
  onDeleteCustomAsset: (String) -> Unit,
  onQuickSaveActiveProject: () -> Unit,
  onOpenProjectSwitcher: () -> Unit,
  onOpenProjectDashboard: () -> Unit,
  savedSprites: List<SavedSpriteEntity> = emptyList(),
  onLoadSavedSprite: ((SavedSpriteEntity) -> Unit)? = null,
  customTrainers: List<CustomTrainerEntity> = emptyList(),
  onSelectTrainer: ((TrainerCharacterSpec) -> Unit)? = null,
  onDeleteCustomTrainer: ((String) -> Unit)? = null,
  isCompactDrawerMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val activeChipKeywords = remember(activeProject) {
    if (activeProject == null) {
      emptySet()
    } else {
      val catalog = ConsoleHardwareCatalog.officialAndFanChips
      activeProject.activeChipsCsv.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { token ->
          val chip = catalog.firstOrNull {
            it.id.equals(token, ignoreCase = true) || it.name.contains(token, ignoreCase = true)
          }
          if (chip != null) {
            listOf(chip.id.lowercase(), chip.name.substringBefore(" (").lowercase())
          } else {
            listOf(token.lowercase())
          }
        }
        .toSet()
    }
  }

  // Synthesize searchable Sprite and 3D Model entries directly from the integrated Room + Cloud Dex databases
  val integratedDatabaseAssets = remember(nativeDatabasePokemon, customCloudDex) {
    buildList {
      nativeDatabasePokemon.forEach { entity ->
        val typeLine = if (entity.secondaryType.isNotBlank()) {
          "${entity.primaryType}/${entity.secondaryType}"
        } else {
          entity.primaryType
        }
        add(
          BuiltInAssetItem(
            id = "db_spr_${entity.dexNumber}",
            name = "${entity.name} Sprite",
            category = AssetCategory.SPRITES,
            consoleLineage = "$typeLine • ${entity.consoleOriginSystem}",
            chipAddon = entity.recommendedChipAddon,
            spriteSeedId = entity.dexNumber,
            shaderFormula = "SPRITE:2D|GEN:${entity.generation}|#${entity.dexNumber}",
            databaseTag = "DB #${entity.dexNumber}",
          )
        )
        add(
          BuiltInAssetItem(
            id = "db_3d_${entity.dexNumber}",
            name = "${entity.name} 3D Model",
            category = AssetCategory.MODELS_3D,
            consoleLineage = "${entity.region} (${entity.generation}) • 3D Mesh",
            chipAddon = entity.recommendedChipAddon,
            spriteSeedId = entity.dexNumber,
            shaderFormula = "MODEL:3D|VOXEL:ON|#${entity.dexNumber}",
            databaseTag = "3D #${entity.dexNumber}",
          )
        )
      }

      customCloudDex.forEach { custom ->
        val seedId = custom.baseSpriteId.toInt().coerceIn(1, 1025)
        add(
          BuiltInAssetItem(
            id = "cloud_dex_spr_${custom.id}",
            name = "${custom.name} Custom Sprite",
            category = AssetCategory.SPRITES,
            consoleLineage = "${custom.primaryType} • ${custom.fusionStylePreset}",
            chipAddon = custom.enhancementChip,
            spriteSeedId = seedId,
            shaderFormula = "CUSTOM_DEX|#${custom.dexNumber}",
            isCustomCloud = true,
            cloudDocId = custom.id,
            databaseTag = "DEX #${custom.dexNumber}",
          )
        )
        add(
          BuiltInAssetItem(
            id = "cloud_dex_3d_${custom.id}",
            name = "${custom.name} 3D Rig",
            category = AssetCategory.MODELS_3D,
            consoleLineage = "${custom.category} • ${custom.fusionStylePreset}",
            chipAddon = custom.enhancementChip,
            spriteSeedId = seedId,
            shaderFormula = "CUSTOM_3D|#${custom.dexNumber}",
            isCustomCloud = true,
            cloudDocId = custom.id,
            databaseTag = "3D #${custom.dexNumber}",
          )
        )
      }
    }
  }

  val allCloudAssets = remember(customCloudAssets) {
    customCloudAssets.map { cloudAsset ->
      val matchedCategory = AssetCategory.entries.firstOrNull {
        it.label.equals(cloudAsset.category, ignoreCase = true)
      } ?: AssetCategory.SPRITES
      BuiltInAssetItem(
        id = "cloud_${cloudAsset.id}",
        name = cloudAsset.name,
        category = matchedCategory,
        consoleLineage = cloudAsset.consoleLineage,
        chipAddon = cloudAsset.chipAddon,
        spriteSeedId = cloudAsset.spriteSeedId.toInt(),
        shaderFormula = cloudAsset.shaderFormula,
        isCustomCloud = true,
        cloudDocId = cloudAsset.id,
        projectId = cloudAsset.projectId,
        databaseTag = if (cloudAsset.projectId.isNotBlank()) "PROJECT" else "GLOBAL",
      )
    }
  }

  val roomSpriteAssets = remember(savedSprites) {
    savedSprites.map { spr ->
      BuiltInAssetItem(
        id = "room_spr_${spr.id}",
        name = spr.name,
        category = AssetCategory.SPRITES,
        consoleLineage = "Room Saved 16×16 (${spr.frameCount}f)",
        chipAddon = "PPU Voxel Rig",
        spriteSeedId = spr.spriteSeedId,
        shaderFormula = "ROOM_SAVED_SPRITE|${spr.id}",
        projectId = spr.projectId,
        databaseTag = "SAVED",
      )
    }
  }

  val trainerByIdMap = remember(customTrainers) {
    buildMap {
      TrainerCatalogAndRigEngine.allBuiltInTrainers.forEach { put(it.id, it) }
      customTrainers.forEach { put(it.id, it.toTrainerSpec()) }
    }
  }

  val trainerSidebarAssets = remember(customTrainers) {
    buildList {
      customTrainers.forEach { custom ->
        val spec = custom.toTrainerSpec()
        add(
          BuiltInAssetItem(
            id = "custom_trainer_${custom.id}",
            name = spec.name,
            category = AssetCategory.TRAINERS,
            consoleLineage = "${spec.title} • ${spec.teamDexIds.size} Partners",
            chipAddon = if (custom.isPlayerAvatar) "Player Avatar (Room v6)" else "Custom Trainer (Room v6)",
            spriteSeedId = spec.teamDexIds.firstOrNull() ?: 25,
            shaderFormula = "TRAINER:${spec.id}",
            isCustomCloud = true,
            cloudDocId = custom.id,
            projectId = custom.projectId,
            databaseTag = if (custom.isPlayerAvatar) "HERO" else "CUSTOM",
          )
        )
      }
      TrainerCatalogAndRigEngine.allBuiltInTrainers.forEach { spec ->
        add(
          BuiltInAssetItem(
            id = "trainer_${spec.id}",
            name = spec.name,
            category = AssetCategory.TRAINERS,
            consoleLineage = "${spec.title} • ${spec.regionId.replace("_", " ").uppercase()}",
            chipAddon = "${spec.roleCategory.label} (${spec.teamDexIds.size}v6)",
            spriteSeedId = spec.teamDexIds.firstOrNull() ?: 25,
            shaderFormula = "TRAINER:${spec.id}|${spec.sourceInspiration}",
            databaseTag = spec.roleCategory.badge,
          )
        )
      }
    }
  }

  val isSearching = searchQuery.isNotBlank()

  val displayedAssets = remember(
    allCloudAssets,
    roomSpriteAssets,
    trainerSidebarAssets,
    integratedDatabaseAssets,
    selectedCategory,
    searchQuery,
    filterByActiveProject,
    activeChipKeywords,
    activeProject?.id,
  ) {
    val combinedPool = buildList {
      addAll(roomSpriteAssets)
      addAll(allCloudAssets)
      addAll(trainerSidebarAssets)
      addAll(StudioViewModel.builtInSidebarAssets)
      addAll(integratedDatabaseAssets)
    }

    val searchFiltered = if (isSearching) {
      val q = searchQuery.trim().lowercase().removePrefix("#")
      combinedPool.filter { item ->
        item.name.lowercase().contains(q) ||
          item.category.label.lowercase().contains(q) ||
          item.consoleLineage.lowercase().contains(q) ||
          item.chipAddon.lowercase().contains(q) ||
          item.shaderFormula.lowercase().contains(q) ||
          item.spriteSeedId.toString() == q ||
          (item.databaseTag?.lowercase()?.contains(q) == true)
      }.sortedByDescending { it.category == selectedCategory }
    } else {
      combinedPool.filter { it.category == selectedCategory }
    }

    val activeId = activeProject?.id.orEmpty()
    if (filterByActiveProject && activeId.isNotBlank()) {
      val matched = searchFiltered.filter { item ->
        item.projectId == activeId ||
          (item.projectId.isBlank() && activeChipKeywords.any { key ->
            item.chipAddon.lowercase().contains(key) || item.consoleLineage.lowercase().contains(key)
          })
      }
      if (!isSearching) matched.ifEmpty { searchFiltered } else matched
    } else {
      searchFiltered
    }
  }

  Surface(
    modifier = modifier
      .then(if (isCompactDrawerMode) Modifier.fillMaxWidth() else Modifier)
      .fillMaxHeight()
      .border(width = 1.dp, color = StudioBorderDark)
      .testTag("categorized_asset_sidebar"),
    color = StudioSurfaceDark,
  ) {
    Row(
      modifier = Modifier
        .then(if (isCompactDrawerMode) Modifier.fillMaxWidth() else Modifier)
        .fillMaxHeight(),
    ) {
      // Permanent Icon Category & Project Rail (Always visible)
      Column(
        modifier = Modifier
          .width(66.dp)
          .fillMaxHeight()
          .background(StudioDeepNavy)
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = onToggleExpand,
            modifier = Modifier
              .size(48.dp)
              .testTag("toggle_asset_sidebar_button"),
          ) {
            Icon(
              imageVector = if (isExpanded) {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
              } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
              },
              contentDescription = if (isExpanded) "Collapse Asset Drawer" else "Expand Asset Drawer",
              tint = CyberCyan,
            )
          }

          // Quick Project Switcher Shortcut on Rail
          IconButton(
            onClick = onOpenProjectSwitcher,
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (hasUnsavedChanges) SuperFxGold.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.14f)
              )
              .border(
                width = 1.dp,
                color = if (hasUnsavedChanges) SuperFxGold else CyberCyan.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
              )
              .testTag("rail_active_project_button"),
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = activeProject?.title ?: "Active Project Quick Access",
              tint = if (hasUnsavedChanges) SuperFxGold else CyberCyan,
            )
          }

          HorizontalDivider(
            color = StudioBorderDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
          )

          AssetCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            val accent = categoryAccentColor(category)
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier
                .width(58.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent)
                .border(
                  width = if (isSelected) 1.dp else 0.dp,
                  color = if (isSelected) accent else Color.Transparent,
                  shape = RoundedCornerShape(12.dp),
                )
                .clickable { onSelectCategory(category) }
                .padding(vertical = 6.dp, horizontal = 2.dp)
                .testTag("sidebar_cat_${category.id}"),
            ) {
              Icon(
                imageVector = categoryIcon(category),
                contentDescription = category.label,
                tint = if (isSelected) accent else StudioTextSecondary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = category.badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) StudioTextPrimary else StudioTextSecondary,
                maxLines = 1,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pin Current Fusion to Sidebar Button
        IconButton(
          onClick = onPinCurrentFusionAsset,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ElectricCrimson.copy(alpha = 0.2f))
            .border(1.dp, ElectricCrimson, CircleShape)
            .testTag("pin_fusion_asset_button"),
        ) {
          Icon(
            imageVector = Icons.Default.AddCircleOutline,
            contentDescription = "Pin Current Fusion Asset to Sidebar",
            tint = ElectricCrimson,
          )
        }
      }

      // Permanent Expandable Navigation Drawer Pane
      AnimatedVisibility(
        visible = isExpanded,
        modifier = if (isCompactDrawerMode) Modifier.weight(1f) else Modifier,
      ) {
        Column(
          modifier = Modifier
            .then(if (isCompactDrawerMode) Modifier.fillMaxWidth() else Modifier.width(214.dp))
            .fillMaxHeight()
            .background(StudioSurfaceDark)
            .padding(10.dp),
        ) {
          // Active Project Quick-Access Card inside Drawer
          ActiveProjectSidebarCard(
            activeProject = activeProject,
            hasUnsavedChanges = hasUnsavedChanges,
            isSavingProject = isSavingProject,
            onQuickSave = onQuickSaveActiveProject,
            onSwitchProject = onOpenProjectSwitcher,
            onOpenDashboard = onOpenProjectDashboard,
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Integrated Database Search Bar inside Navigation Drawer
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
              Text(
                text = "Search DB sprites, 3D...",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Integrated Asset Database",
                tint = CyberCyan,
                modifier = Modifier.size(18.dp),
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { onSearchQueryChange("") },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("sidebar_clear_search_icon_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Asset Search",
                    tint = StudioTextSecondary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = StudioTextPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sidebar_asset_search_input"),
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isSearching) "INTEGRATED DB SEARCH" else "GAME ASSETS",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
              )
              Text(
                text = if (isSearching) "All Categories" else selectedCategory.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StudioCardDark)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = "${displayedAssets.size}",
                style = MaterialTheme.typography.labelSmall,
                color = SuperFxGold,
              )
            }
          }

          if (activeProject != null) {
            Spacer(modifier = Modifier.height(4.dp))
            FilterChip(
              selected = filterByActiveProject,
              onClick = onToggleFilterByActiveProject,
              label = {
                Text(
                  text = if (filterByActiveProject) "Project Matched" else "Filter by Active Project",
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SgbEmerald.copy(alpha = 0.2f),
                selectedLabelColor = SgbEmerald,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sidebar_project_filter_chip"),
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          if (displayedAssets.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioCardDark)
                .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "No matching assets in integrated DB",
                style = MaterialTheme.typography.labelLarge,
                color = StudioTextPrimary,
              )
              Text(
                text = "Try searching by species name, Dex #, type, or chip name.",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary,
              )
              if (isSearching) {
                TextButton(
                  onClick = { onSearchQueryChange("") },
                  modifier = Modifier.testTag("sidebar_clear_search_button"),
                ) {
                  Text("Clear Search", color = CyberCyan)
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(bottom = 12.dp),
            ) {
              items(displayedAssets, key = { it.id }) { asset ->
                val matchesActiveProject = activeChipKeywords.isNotEmpty() && activeChipKeywords.any { key ->
                  asset.chipAddon.lowercase().contains(key) || asset.consoleLineage.lowercase().contains(key)
                }
                val matchedTrainerSpec = if (asset.category == AssetCategory.TRAINERS) {
                  val tId = asset.shaderFormula.substringAfter("TRAINER:").substringBefore("|")
                  trainerByIdMap[tId]
                } else {
                  null
                }
                SidebarAssetCard(
                  asset = asset,
                  accentColor = categoryAccentColor(asset.category),
                  matchesActiveProject = matchesActiveProject,
                  showCategoryTag = isSearching,
                  trainerSpec = matchedTrainerSpec,
                  onClick = {
                    if (matchedTrainerSpec != null && onSelectTrainer != null) {
                      onSelectTrainer(matchedTrainerSpec)
                    } else if (asset.id.startsWith("room_spr_")) {
                      val sprId = asset.id.removePrefix("room_spr_")
                      val match = savedSprites.firstOrNull { it.id == sprId }
                      if (match != null && onLoadSavedSprite != null) {
                        onLoadSavedSprite(match)
                      } else {
                        onSelectAsset(asset)
                      }
                    } else {
                      onSelectAsset(asset)
                    }
                  },
                  onDelete = when {
                    asset.id.startsWith("custom_trainer_") && onDeleteCustomTrainer != null -> {
                      { onDeleteCustomTrainer(asset.cloudDocId) }
                    }
                    asset.isCustomCloud && asset.id.startsWith("cloud_") && !asset.id.startsWith("cloud_dex_") -> {
                      { onDeleteCustomAsset(asset.cloudDocId) }
                    }
                    else -> null
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveProjectSidebarCard(
  activeProject: FanGameProject?,
  hasUnsavedChanges: Boolean,
  isSavingProject: Boolean,
  onQuickSave: () -> Unit,
  onSwitchProject: () -> Unit,
  onOpenDashboard: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(StudioDeepNavy)
      .border(
        width = 1.dp,
        color = if (hasUnsavedChanges) SuperFxGold else CyberCyan.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
      )
      .padding(8.dp)
      .testTag("sidebar_active_project_card"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "ACTIVE PROJECT",
        style = MaterialTheme.typography.labelSmall,
        color = if (hasUnsavedChanges) SuperFxGold else CyberCyan,
        fontWeight = FontWeight.Bold,
      )
      if (activeProject != null) {
        Text(
          text = activeProject.versionTag,
          style = MaterialTheme.typography.labelSmall,
          color = SuperFxGold,
        )
      }
    }

    Text(
      text = activeProject?.title ?: "No Local Project Loaded",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = StudioTextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    if (activeProject != null) {
      Text(
        text = "${activeProject.regionName} • ${activeProject.dexCount} Dex",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (activeProject != null) {
        SidebarQuickActionPill(
          icon = Icons.Default.Save,
          label = if (isSavingProject) "..." else if (hasUnsavedChanges) "Save*" else "Save",
          tint = if (hasUnsavedChanges) SuperFxGold else CyberCyan,
          onClick = onQuickSave,
          modifier = Modifier
            .weight(1f)
            .testTag("sidebar_save_project_button"),
        )
      }
      SidebarQuickActionPill(
        icon = Icons.Default.SwapHoriz,
        label = "Switch",
        tint = CyberCyan,
        onClick = onSwitchProject,
        modifier = Modifier
          .weight(1f)
          .testTag("sidebar_switch_project_button"),
      )
      SidebarQuickActionPill(
        icon = Icons.Default.Dashboard,
        label = "Hub",
        tint = Mode7Sky,
        onClick = onOpenDashboard,
        modifier = Modifier
          .weight(1f)
          .testTag("sidebar_dashboard_button"),
      )
    }
  }
}

@Composable
private fun SidebarQuickActionPill(
  icon: ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .heightIn(min = 36.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(StudioCardDark)
      .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(13.dp),
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = tint,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
    )
  }
}

@Composable
private fun SidebarAssetCard(
  asset: BuiltInAssetItem,
  accentColor: Color,
  matchesActiveProject: Boolean,
  showCategoryTag: Boolean,
  trainerSpec: TrainerCharacterSpec? = null,
  onClick: () -> Unit,
  onDelete: (() -> Unit)?,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(StudioCardDark)
      .border(
        width = if (matchesActiveProject) 1.5.dp else 1.dp,
        color = if (matchesActiveProject) SgbEmerald else accentColor.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
      )
      .clickable(onClick = onClick)
      .padding(8.dp)
      .testTag("asset_item_${asset.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AssetThumbnailPreview(
        asset = asset,
        accentColor = accentColor,
        trainerSpec = trainerSpec,
      )

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (asset.isCustomCloud) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "Saved in Local Room Vault",
              tint = CyberCyan,
              modifier = Modifier
                .size(12.dp)
                .padding(end = 3.dp),
            )
          }
          Text(
            text = asset.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = asset.chipAddon,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (onDelete != null) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp),
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Custom Vault Asset",
            tint = StudioTextSecondary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = asset.consoleLineage,
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showCategoryTag) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(accentColor.copy(alpha = 0.2f))
              .padding(horizontal = 4.dp, vertical = 1.dp),
          ) {
            Text(
              text = asset.category.badge,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
            )
          }
        }
        if (asset.databaseTag != null) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(CyberCyan.copy(alpha = 0.16f))
              .padding(horizontal = 4.dp, vertical = 1.dp),
          ) {
            Text(
              text = asset.databaseTag,
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
          }
        }
        if (matchesActiveProject) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(SgbEmerald.copy(alpha = 0.22f))
              .padding(horizontal = 4.dp, vertical = 1.dp),
          ) {
            Text(
              text = "PROJ",
              style = MaterialTheme.typography.labelSmall,
              color = SgbEmerald,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AssetThumbnailPreview(
  asset: BuiltInAssetItem,
  accentColor: Color,
  trainerSpec: TrainerCharacterSpec? = null,
) {
  Box(
    modifier = Modifier
      .size(46.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            accentColor.copy(alpha = 0.22f),
            StudioDeepNavy,
          ),
        ),
      )
      .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
      .testTag("asset_thumbnail_${asset.id}"),
    contentAlignment = Alignment.Center,
  ) {
    if (trainerSpec != null) {
      TrainerOverworldThumbnail(
        trainer = trainerSpec,
        pose = TrainerPose.BATTLE_IDLE,
        use3dVoxel = false,
        modifier = Modifier
          .size(42.dp)
          .padding(2.dp),
      )
    } else {
      ProceduralCreatureThumbnail(
        dexNumber = asset.spriteSeedId.coerceIn(1, 1025),
        is3dModel = asset.category == AssetCategory.MODELS_3D,
        contentDescription = "${asset.name} thumbnail preview",
        modifier = Modifier
          .size(40.dp)
          .padding(2.dp),
      )
    }
  }
}

private fun categoryIcon(category: AssetCategory): ImageVector = when (category) {
  AssetCategory.SPRITES -> Icons.Default.AutoAwesomeMotion
  AssetCategory.MODELS_3D -> Icons.Default.ViewInAr
  AssetCategory.TRAINERS -> Icons.Default.Person
  AssetCategory.TEXTURES -> Icons.Default.Texture
  AssetCategory.TILESETS -> Icons.Default.GridOn
  AssetCategory.CHIPS_SHADERS -> Icons.Default.Memory
  AssetCategory.MAPS -> Icons.Default.Map
  AssetCategory.AUDIO_FX -> Icons.Default.Audiotrack
}

private fun categoryAccentColor(category: AssetCategory): Color = when (category) {
  AssetCategory.SPRITES -> CyberCyan
  AssetCategory.MODELS_3D -> VoxelViolet
  AssetCategory.TRAINERS -> SuperFxGold
  AssetCategory.TEXTURES -> Mode7Sky
  AssetCategory.TILESETS -> SgbEmerald
  AssetCategory.CHIPS_SHADERS -> SuperFxGold
  AssetCategory.MAPS -> Mode7Sky
  AssetCategory.AUDIO_FX -> ElectricCrimson
}

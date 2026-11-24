package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.FranchiseEncyclopediaAndResourceRepository
import com.example.data.local.NativeFranchiseCatalogRepository
import com.example.data.local.NativePokemonDatabase
import com.example.data.model.AssetCategory
import com.example.data.repository.LocalStudioProjectRepository
import com.example.ui.StudioViewModel
import com.example.ui.StudioWorkspaceTab
import com.example.ui.UiState
import com.example.ui.components.ActiveProjectHeaderPill
import com.example.ui.components.CanonTemplateSandboxSection
import com.example.ui.components.CategorizedAssetSidebar
import com.example.ui.components.CloudFanGameHubSection
import com.example.ui.components.FranchiseDexAndCreatorSection
import com.example.ui.components.GameTestSection
import com.example.ui.components.OmniConsoleFusionViewport
import com.example.ui.components.ProjectDashboard
import com.example.ui.components.ProjectEditorDialog
import com.example.ui.components.ProjectSwitcherDialog
import com.example.ui.components.ResourceManagerSection
import com.example.ui.components.SpriteEditorSection
import com.example.ui.components.WorldMapAndChipLabSection
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        PokeFusionRootApp()
      }
    }
  }
}

/**
 * 100% Offline Room v5 Root Entrypoint — boots IMMEDIATELY into PokeFusionStudioWorkspace
 * with ZERO login wall, ZERO Firebase/Auth dependencies, and instant offline startup.
 */
@Composable
fun PokeFusionRootApp() {
  val viewModel: StudioViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val localDb = NativePokemonDatabase.getInstance(app)
        val localProjectRepo = LocalStudioProjectRepository(
          projectDao = localDb.fanGameProjectDao(),
          customDexDao = localDb.customDexDao(),
          customAssetDao = localDb.customAssetDao(),
          studioPersistenceDao = localDb.studioPersistenceDao(),
          customTrainerDao = localDb.customTrainerDao(),
        )
        StudioViewModel(
          appContext = app.applicationContext,
          localProjectRepository = localProjectRepo,
          localDexRepository = NativeFranchiseCatalogRepository(localDb.pokemonDao()),
          encyclopediaAndResourceRepository = FranchiseEncyclopediaAndResourceRepository(
            localDb.resourceDao(),
            localDb.encyclopediaDao(),
          ),
          persistenceDao = localDb.studioPersistenceDao(),
          customTrainerDao = localDb.customTrainerDao(),
        )
      }
    },
  )
  PokeFusionStudioWorkspace(viewModel = viewModel)
}

@Composable
fun PokeFusionStudioWorkspace(
  viewModel: StudioViewModel,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedSidebarCategory.collectAsStateWithLifecycle()
  val isSidebarExpanded by viewModel.sidebarExpanded.collectAsStateWithLifecycle()
  val primaryPokemon by viewModel.selectedPokemon.collectAsStateWithLifecycle()
  val secondaryPokemon by viewModel.secondaryFusionPokemon.collectAsStateWithLifecycle()
  val fusionConfig by viewModel.fusionConfig.collectAsStateWithLifecycle()
  val nativePokemonList by viewModel.nativePokemonList.collectAsStateWithLifecycle()
  val dexSearchQuery by viewModel.dexSearchQuery.collectAsStateWithLifecycle()
  val selectedGenFilter by viewModel.selectedGenerationFilter.collectAsStateWithLifecycle()
  val isSyncingPokeApi by viewModel.isSyncingPokeApi.collectAsStateWithLifecycle()
  val playerTrainer by viewModel.playerTrainer.collectAsStateWithLifecycle()
  val selectedViewportTrainer by viewModel.selectedViewportTrainer.collectAsStateWithLifecycle()
  val viewportInspectsTrainer by viewModel.viewportInspectsTrainer.collectAsStateWithLifecycle()
  val customTrainers by viewModel.customTrainers.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

  val cloudFanGamesState by viewModel.cloudFanGamesState.collectAsStateWithLifecycle()
  val projectOverviewState by viewModel.projectOverviewState.collectAsStateWithLifecycle()
  val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
  val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
  val isSavingProject by viewModel.isSavingProject.collectAsStateWithLifecycle()
  val projectSearchQuery by viewModel.projectSearchQuery.collectAsStateWithLifecycle()
  val filterSidebarByActiveProject by viewModel.sidebarFilterByActiveProject.collectAsStateWithLifecycle()
  val sidebarSearchQuery by viewModel.sidebarSearchQuery.collectAsStateWithLifecycle()
  val customDexState by viewModel.customDexState.collectAsStateWithLifecycle()
  val customAssetsState by viewModel.customAssetsState.collectAsStateWithLifecycle()
  val activeMapRegionId by viewModel.activeMapRegionId.collectAsStateWithLifecycle()
  val mapGrid by viewModel.mapGrid.collectAsStateWithLifecycle()
  val selectedBrush by viewModel.selectedBrushTile.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val roomResources by viewModel.roomResources.collectAsStateWithLifecycle()
  val encyclopediaEntries by viewModel.encyclopediaEntries.collectAsStateWithLifecycle()
  val encyclopediaSourceStatuses by viewModel.encyclopediaSourceStatuses.collectAsStateWithLifecycle()
  val isSyncingSources by viewModel.isSyncingSources.collectAsStateWithLifecycle()

  val hardwareState by viewModel.hardwareRegistryState.collectAsStateWithLifecycle()
  val audioState by viewModel.audioConsoleState.collectAsStateWithLifecycle()
  val spriteEditorState by viewModel.spriteEditorState.collectAsStateWithLifecycle()
  val savedSprites by viewModel.savedSprites.collectAsStateWithLifecycle()
  val gameTestState by viewModel.gameTestState.collectAsStateWithLifecycle()
  val campaignState by viewModel.campaignState.collectAsStateWithLifecycle()
  val sideModesState by viewModel.sideModesState.collectAsStateWithLifecycle()
  val studioItems by viewModel.studioItems.collectAsStateWithLifecycle()
  val atlasProgressMap by viewModel.atlasProgressMap.collectAsStateWithLifecycle()

  // Android Storage Access Framework JSON Project Export Launcher (zero broad storage permissions)
  val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val jsonPayload = viewModel.exportActiveProjectJson().getOrThrow()
          withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
              stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
              stream.flush()
            } ?: error("Unable to open output stream for selected file.")
          }
          viewModel.notifyStatusBanner("Exported active studio project bundle (.json) cleanly.")
        } catch (t: Throwable) {
          viewModel.notifyStatusBanner("Export failed: ${t.message}")
        }
      }
    }
  }

  // Android Storage Access Framework JSON Project Import Launcher
  val importJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val rawJson = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Unable to read selected JSON file.")
          }
          viewModel.importProjectFromJson(rawJson)
        } catch (t: Throwable) {
          viewModel.notifyStatusBanner("Import failed: ${t.message}")
        }
      }
    }
  }

  val launchExportJson: () -> Unit = {
    val slug = (activeProject?.title ?: "pokefusion_studio_project")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
      .ifBlank { "pokefusion_studio_project" }
    exportJsonLauncher.launch("${slug}_v6.json")
  }

  val launchImportJson: () -> Unit = {
    importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
  }

  // Clean audio lifecycle management (stop active BGM cleanly on background)
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
    viewModel.onAppStopped()
  }

  var showProjectSwitcherDialog by remember { mutableStateOf(false) }
  var showQuickCreateProjectDialog by remember { mutableStateOf(false) }

  if (activeTab != StudioWorkspaceTab.FUSION_ENGINE) {
    BackHandler {
      viewModel.selectWorkspaceTab(StudioWorkspaceTab.FUSION_ENGINE)
    }
  }

  val customAssetsList = (customAssetsState as? UiState.Success)?.data ?: emptyList()
  val customDexList = (customDexState as? UiState.Success)?.data ?: emptyList()
  val localProjectsList = (cloudFanGamesState as? UiState.Success)?.data ?: emptyList()
  val hydratedByDex = remember(nativePokemonList) { nativePokemonList.associateBy { it.dexNumber } }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = StudioDeepNavy,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      val isCompactPhone = maxWidth < 600.dp
      val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

      val sidebarComposable: @Composable (Boolean) -> Unit = { compactMode ->
        CategorizedAssetSidebar(
          selectedCategory = selectedCategory,
          isExpanded = if (compactMode) true else isSidebarExpanded,
          customCloudAssets = customAssetsList,
          nativeDatabasePokemon = nativePokemonList,
          customCloudDex = customDexList,
          searchQuery = sidebarSearchQuery,
          onSearchQueryChange = { viewModel.updateSidebarSearchQuery(it) },
          activeProject = activeProject,
          hasUnsavedChanges = hasUnsavedChanges,
          isSavingProject = isSavingProject,
          filterByActiveProject = filterSidebarByActiveProject,
          onToggleFilterByActiveProject = { viewModel.toggleSidebarActiveProjectFilter() },
          onSelectCategory = { cat -> viewModel.selectSidebarCategory(cat) },
          onToggleExpand = {
            if (compactMode) {
              scope.launch { drawerState.close() }
            } else {
              viewModel.toggleSidebarExpanded()
            }
          },
          onSelectAsset = { asset ->
            viewModel.applySidebarAssetToEngine(asset)
            if (compactMode) scope.launch { drawerState.close() }
          },
          onPinCurrentFusionAsset = {
            viewModel.saveCurrentFusionAsSidebarAsset(
              customName = "${primaryPokemon.name} × ${secondaryPokemon.name} ${selectedCategory.badge}",
              category = selectedCategory,
            )
          },
          onDeleteCustomAsset = { id -> viewModel.deleteCustomSidebarAsset(id) },
          onQuickSaveActiveProject = { viewModel.saveActiveProjectChanges() },
          onOpenProjectSwitcher = {
            if (compactMode) scope.launch { drawerState.close() }
            showProjectSwitcherDialog = true
          },
          onOpenProjectDashboard = {
            if (compactMode) scope.launch { drawerState.close() }
            viewModel.selectWorkspaceTab(StudioWorkspaceTab.PROJECT_DASHBOARD)
          },
          savedSprites = savedSprites,
          onLoadSavedSprite = { spr ->
            viewModel.loadSavedSpriteFromRoom(spr)
            viewModel.selectWorkspaceTab(StudioWorkspaceTab.SPRITE_EDITOR)
            if (compactMode) scope.launch { drawerState.close() }
          },
          customTrainers = customTrainers,
          onSelectTrainer = { trainerSpec ->
            viewModel.selectTrainerForViewport(trainerSpec, openViewportTab = true)
            if (compactMode) scope.launch { drawerState.close() }
          },
          onDeleteCustomTrainer = { id -> viewModel.deleteCustomTrainerFromRoom(id) },
          isCompactDrawerMode = compactMode,
        )
      }

      val mainWorkspaceContent: @Composable (Modifier) -> Unit = { paneModifier ->
        Column(
          modifier = paneModifier
            .fillMaxHeight()
            .background(StudioDeepNavy),
        ) {
          // Top Studio Header Bar + Workspace Section Switcher
          Surface(
            color = StudioSurfaceDark,
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, StudioBorderDark),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f),
                ) {
                  if (isCompactPhone) {
                    IconButton(
                      onClick = { scope.launch { drawerState.open() } },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("open_asset_drawer_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Categorized Asset Drawer",
                        tint = CyberCyan,
                      )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "POKÉFUSION STUDIO",
                      fontFamily = PressStart2PFamily,
                      style = MaterialTheme.typography.labelLarge,
                      color = CyberCyan,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${hardwareState.convergedVisuals.activeCount}/${hardwareState.allPlatforms.size} HW • ${syncStatus.statusLabel} • Offline Room v6",
                      style = MaterialTheme.typography.labelSmall,
                      color = StudioTextSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  IconButton(
                    enabled = !syncStatus.isSyncing,
                    onClick = { viewModel.refreshStudioNow() },
                    modifier = Modifier.testTag("studio_sync_button"),
                  ) {
                    if (syncStatus.isSyncing) {
                      CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Refresh local Room database and Pokédex sources",
                        tint = CyberCyan,
                      )
                    }
                  }

                  Button(
                    onClick = { viewModel.selectWorkspaceTab(StudioWorkspaceTab.GAME_TEST) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = SgbEmerald,
                      contentColor = StudioDeepNavy,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("top_game_test_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.PlayArrow,
                      contentDescription = "Launch Game Test",
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Playtest",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }

              ActiveProjectHeaderPill(
                activeProject = activeProject,
                hasUnsavedChanges = hasUnsavedChanges,
                isSavingProject = isSavingProject,
                onOpenSwitcher = { showProjectSwitcherDialog = true },
                onQuickSave = { viewModel.saveActiveProjectChanges() },
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Workspace Mode Tabs
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                StudioWorkspaceTab.entries.forEach { tab ->
                  val selected = tab == activeTab
                  FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectWorkspaceTab(tab) },
                    leadingIcon = {
                      Icon(
                        imageVector = when (tab) {
                          StudioWorkspaceTab.FUSION_ENGINE -> Icons.Default.AutoAwesome
                          StudioWorkspaceTab.SPRITE_EDITOR -> Icons.Default.Brush
                          StudioWorkspaceTab.RESOURCE_MANAGER -> Icons.Default.LibraryMusic
                          StudioWorkspaceTab.GAME_TEST -> Icons.Default.PlayArrow
                          StudioWorkspaceTab.BASE_TEMPLATE -> Icons.Default.SportsEsports
                          StudioWorkspaceTab.PROJECT_DASHBOARD -> Icons.Default.Dashboard
                          StudioWorkspaceTab.CLOUD_GAMES -> Icons.Default.FolderOpen
                          StudioWorkspaceTab.FRANCHISE_DEX -> Icons.Default.Storage
                          StudioWorkspaceTab.WORLD_MAP_BUILDER -> Icons.Default.Map
                        },
                        contentDescription = tab.title,
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    label = {
                      Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                      selectedLabelColor = CyberCyan,
                      selectedLeadingIconColor = CyberCyan,
                    ),
                    modifier = Modifier.testTag("tab_${tab.id}"),
                  )
                }
              }
            }
          }

          // Status Toast / Telemetry Banner
          if (statusBanner != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(StudioCardDark)
                .padding(horizontal = 14.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = statusBanner!!,
                style = MaterialTheme.typography.labelSmall,
                color = SuperFxGold,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = { viewModel.dismissStatusBanner() }) {
                Text("OK", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
              }
            }
          }

          // Scrollable Workspace Content
          Column(
            modifier = Modifier
              .weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            when (activeTab) {
              StudioWorkspaceTab.FUSION_ENGINE -> {
                OmniConsoleFusionViewport(
                  primaryPokemon = primaryPokemon,
                  secondaryPokemon = secondaryPokemon,
                  config = fusionConfig,
                  hardwareState = hardwareState,
                  customSpriteBitmap = spriteEditorState.renderedPreviewBitmap,
                  useCustomSprite = spriteEditorState.useCustomBitmapInViewport,
                  onToggleUseCustomSprite = { viewModel.toggleUseCustomSpriteInViewport() },
                  onUpdateConfig = { transform -> viewModel.updateFusionConfig(transform) },
                  onSelectMixMode = { mode -> viewModel.setCharacterMixMode(mode) },
                  onToggleHardwarePlatform = { id -> viewModel.toggleHardwarePlatform(id) },
                  onToggleAllHardware = { enableAll -> viewModel.toggleAllHardwareOnRecord(enableAll) },
                  onSelectDiscipline = { mode -> viewModel.setMasterDisciplineMode(mode) },
                  onUpdateExtraPretty = { transform -> viewModel.updateExtraPrettySettings(transform) },
                  onPlayCry = { viewModel.playPokemonCry(primaryPokemon) },
                  onPinToSidebar = { category ->
                    viewModel.saveCurrentFusionAsSidebarAsset(
                      customName = "${primaryPokemon.name} × ${secondaryPokemon.name} ${category.badge}",
                      category = category,
                    )
                  },
                  inspectTrainerMode = viewportInspectsTrainer,
                  selectedTrainer = selectedViewportTrainer,
                  playerTrainer = playerTrainer,
                  playerCustomAvatarBitmap = spriteEditorState.playerCustomSpriteBitmap,
                  useCustomPlayerAvatarBitmap = spriteEditorState.useCustomPlayerAvatarBitmap,
                  onToggleInspectTrainerMode = { viewModel.toggleViewportTrainerMode(it) },
                  onSelectViewportTrainer = { viewModel.selectTrainerForViewport(it) },
                  onSelectProtagonistPreset = { preset -> viewModel.selectProtagonistPreset(preset) },
                  onUpdateProtagonistIdentity = { name, title -> viewModel.updateProtagonistIdentity(name, title) },
                  onUpdateProtagonistAppearance = { transform -> viewModel.updateProtagonistAppearance(transform) },
                  onEditHeroInSpriteEditor = { viewModel.editHeroInSpriteEditor(openTab = true) },
                  onToggleUseCustomPlayerAvatar = { viewModel.toggleUseCustomPlayerAvatarBitmap() },
                  onSaveCustomTrainer = { spec -> viewModel.saveCustomTrainerToRoom(spec) },
                )
              }

              StudioWorkspaceTab.SPRITE_EDITOR -> {
                SpriteEditorSection(
                  activePokemon = primaryPokemon,
                  editorState = spriteEditorState,
                  savedSprites = savedSprites,
                  isSaving = isSavingProject,
                  onSelectTool = { viewModel.selectSpriteTool(it) },
                  onSelectColorSlot = { viewModel.selectSpriteColorSlot(it) },
                  onUpdatePaletteSlot = { slot, argb -> viewModel.updateSpritePaletteSlot(slot, argb) },
                  onApplyPresetPaletteValues = { viewModel.applySpritePresetPaletteValues(it) },
                  onUpdateSpriteName = { viewModel.updateSpriteName(it) },
                  onToggleMirrorX = { viewModel.toggleSpriteMirrorX() },
                  onToggleShowGrid = { viewModel.toggleSpriteShowGrid() },
                  onTogglePlayPreview = { viewModel.toggleSpritePlayPreview() },
                  onSelectFrame = { viewModel.selectSpriteFrame(it) },
                  onAddFrame = { viewModel.addSpriteFrame() },
                  onDuplicateFrame = { viewModel.duplicateSpriteFrame() },
                  onDeleteFrame = { viewModel.deleteSpriteFrame() },
                  onApplyPixelStroke = { viewModel.applySpritePixelStroke(it) },
                  onUndo = { viewModel.undoSpriteEdit() },
                  onClearFrame = { viewModel.clearCurrentSpriteFrame() },
                  onImportBitmap = { bmp, hint -> viewModel.importBitmapIntoSpriteEditor(bmp, hint) },
                  onLoadActiveSpeciesSeed = { viewModel.loadSeedIntoSpriteEditor(primaryPokemon) },
                  onSampleHeroToEditor = { viewModel.editHeroInSpriteEditor(openTab = false) },
                  onApplySpriteToHeroAvatar = { viewModel.applyCurrentSpriteToPlayerAvatar() },
                  onSaveSprite = { viewModel.saveCurrentSpriteToRoomAndCloud() },
                  onLoadSavedSprite = { viewModel.loadSavedSpriteFromRoom(it) },
                  onDeleteSavedSprite = { viewModel.deleteSavedSpriteFromRoom(it) },
                  onApplyPaletteToSpecies = { p1, p2 -> viewModel.applyEditedPaletteToActivePokemon(p1, p2) },
                )
              }

              StudioWorkspaceTab.RESOURCE_MANAGER -> {
                ResourceManagerSection(
                  resources = roomResources,
                  projects = localProjectsList,
                  activeProject = activeProject,
                  onDropResourceToProject = { resource, project ->
                    viewModel.assignResourceToGameProject(resource, project)
                  },
                  onUnassignResourceFromProject = { resource, project ->
                    viewModel.unassignResourceFromGameProject(resource, project)
                  },
                  onPreviewResource = { resource ->
                    viewModel.previewRoomResource(resource)
                  },
                  onAddCustomResource = { name, category, formatTag, consoleChip, durationOrGrid, description ->
                    viewModel.addCustomRoomResource(
                      name = name,
                      category = category,
                      formatTag = formatTag,
                      consoleChip = consoleChip,
                      durationOrGrid = durationOrGrid,
                      description = description,
                    )
                  },
                  onDeleteCustomResource = { id ->
                    viewModel.deleteCustomRoomResource(id)
                  },
                )
              }

              StudioWorkspaceTab.GAME_TEST -> {
                GameTestSection(
                  gameState = gameTestState,
                  campaignState = campaignState,
                  fusionConfig = fusionConfig,
                  hardwareState = hardwareState,
                  audioState = audioState,
                  mapGrid = mapGrid,
                  studioItems = studioItems,
                  customSpriteBitmap = spriteEditorState.renderedPreviewBitmap,
                  useCustomSprite = spriteEditorState.useCustomBitmapInViewport,
                  resolvePokemonByDex = { dex -> viewModel.findPokemonByDex(dex) },
                  onSelectRegion = { viewModel.selectGameTestRegion(it) },
                  onMoveOverworld = { dr, dc -> viewModel.movePlayerOnOverworld(dr, dc) },
                  onInteractOverworld = { viewModel.interactAtCurrentPosition() },
                  onDismissDialogue = { viewModel.dismissDialogueBanner() },
                  onTriggerPoiBattle = { viewModel.triggerPoiBattle(it) },
                  onWarpToRegion = { viewModel.warpToRegion(it) },
                  onDismissWarpPicker = { viewModel.dismissWarpGatePicker() },
                  onSelectHardwarePreset = { viewModel.applyHardwareRuntimePreset(it) },
                  onApplyCustomHardwareCombo = { ids, mode -> viewModel.applyCustomHardwareCombo(ids, mode) },
                  onToggleHardwareTour = { viewModel.toggleHardwareTour() },
                  onTriggerEncounter = { isBoss -> viewModel.triggerManualEncounter(isBoss) },
                  onExecuteMove = { move -> viewModel.executePlayerBattleMove(move) },
                  onToggleGimmick = { gimmick -> viewModel.toggleGameTestGimmick(gimmick) },
                  onSelectTeraType = { viewModel.setGameTestTeraType(it) },
                  onSelectWeather = { viewModel.setGameTestWeather(it) },
                  onSelectTerrain = { viewModel.setGameTestTerrain(it) },
                  onSwitchPartySlot = { viewModel.switchGameTestPartySlot(it) },
                  onToggleBag = { viewModel.toggleGameTestBag(it) },
                  onSelectBagCategory = { viewModel.selectBagCategory(it) },
                  onUseBagItem = { viewModel.useBagItem(it) },
                  onFleeBattle = { viewModel.fleeBattle() },
                  onHealParty = { viewModel.healFullParty() },
                  onToggleBgm = { viewModel.toggleBgmPlayback() },
                  onSelectBgmTheme = { viewModel.playBgmTheme(it) },
                  onToggleMute = { viewModel.toggleAudioMute() },
                  onSetVolume = { viewModel.setAudioMasterVolume(it) },
                  onPlayLeadCry = { viewModel.playPokemonCry(it) },
                  onSelectTimeOfDay = { viewModel.setTimeOfDayCycle(it) },
                  onToggleFullRegionView = { viewModel.toggleFullRegionView() },
                  onToggleWorldAtlas = { viewModel.toggleWorldAtlas() },
                  onToggleSubZoneLabels = { viewModel.toggleSubZoneLabels() },
                  onChallengeTrainer = { viewModel.challengeTrainerArchetype(it) },
                  onSummonWildPokemon = { viewModel.summonWildPokemonByDex(it) },
                  onAddPokemonToParty = { viewModel.addPokemonToActiveParty(it) },
                  playerTrainer = playerTrainer,
                  playerCustomAvatarBitmap = spriteEditorState.playerCustomSpriteBitmap,
                  useCustomPlayerAvatarBitmap = spriteEditorState.useCustomPlayerAvatarBitmap,
                  hydratedByDex = hydratedByDex,
                  onSelectProtagonistPreset = { preset -> viewModel.selectProtagonistPreset(preset) },
                  onUpdateProtagonistIdentity = { name, title -> viewModel.updateProtagonistIdentity(name, title) },
                  onUpdateProtagonistAppearance = { transform -> viewModel.updateProtagonistAppearance(transform) },
                  onEditHeroInSpriteEditor = { viewModel.editHeroInSpriteEditor(openTab = true) },
                  onToggleUseCustomPlayerAvatar = { viewModel.toggleUseCustomPlayerAvatarBitmap() },
                  onSaveHeroToRoom = { spec -> viewModel.saveCustomTrainerToRoom(spec) },
                  onDismissVsBanner = { viewModel.dismissTrainerVsBanner() },
                  onToggleTrailingPartner = { viewModel.toggleTrailingPartner() },
                  onOpenTrainerStudio = { viewModel.openHeroCustomizerInViewport() },
                  onToggleAmbientBed = { viewModel.toggleAmbientBed() },
                  onPlaySfx = { viewModel.playSfx(it) },
                  sideModesState = sideModesState,
                  onSelectSideMode = { viewModel.selectSideMode(it) },
                  onStartTowerGauntlet = { viewModel.startBattleTowerGauntlet() },
                  onExecuteTowerMove = { viewModel.executeBattleTowerMove(it) },
                  onSwitchTowerSlot = { viewModel.switchBattleTowerSlot(it) },
                  onAdvanceTowerRound = { viewModel.advanceBattleTowerRound() },
                  onRedeemBpReward = { viewModel.redeemBattleTowerBpReward(it) },
                  onStartContest = { viewModel.startPrismContest(it) },
                  onPerformContestAppeal = { viewModel.performPrismContestAppeal(it) },
                  onStepSnapFrame = { viewModel.stepLentalSnapFrame() },
                  onTossSnapIlluminaOrb = { viewModel.tossSnapIlluminaOrb() },
                  onTossSnapApple = { viewModel.tossSnapApple() },
                  onNextSnapSubject = { viewModel.nextSnapSubject() },
                  onCaptureSnapPhoto = { viewModel.captureLentalSnapPhoto() },
                  onStartRiftDungeon = { viewModel.startMysteryRiftDungeon() },
                  onStepRiftDirection = { dr, dc -> viewModel.stepMysteryRift(dr, dc) },
                  onUseRiftPrismPulse = { viewModel.stepMysteryRift(0, 0, useSignatureSkill = true) },
                  onUseRiftOranBerry = { viewModel.useMysteryRiftOranBerry() },
                  onStepRangerWild = { viewModel.stepRangerWildMovement() },
                  onBeginRangerDrag = { x, y -> viewModel.beginRangerLoopDrag(x, y) },
                  onStepRangerDrag = { x, y -> viewModel.stepRangerLoopDrag(x, y) },
                  onEndRangerDrag = { viewModel.endRangerLoopDrag() },
                  onNextRangerTarget = { viewModel.selectNextRangerTarget() },
                  onRecruitRangerTarget = { viewModel.recruitRangerTargetToParty() },
                  onPerformCampActivity = { viewModel.performPrismCampActivity(it) },
                )
              }

              StudioWorkspaceTab.BASE_TEMPLATE -> {
                CanonTemplateSandboxSection(
                  attackerEntity = primaryPokemon,
                  defenderEntity = secondaryPokemon,
                  campaignState = campaignState,
                  isSavingProject = isSavingProject,
                  onCompleteChapter = { ch -> viewModel.completeCampaignChapter(ch) },
                  onResetCampaign = { viewModel.resetCampaignProgression() },
                  onProvisionMasterBaseProject = { viewModel.provisionMasterCanonBaseProject() },
                )
              }

              StudioWorkspaceTab.PROJECT_DASHBOARD -> {
                ProjectDashboard(
                  cloudGamesState = cloudFanGamesState,
                  projectOverviewState = projectOverviewState,
                  allHardwarePlatforms = hardwareState.allPlatforms,
                  activeProjectId = activeProject?.id,
                  hasUnsavedChanges = hasUnsavedChanges,
                  onSavePendingChanges = { viewModel.saveActiveProjectChanges() },
                  onOpenProjectSwitcher = { showProjectSwitcherDialog = true },
                  onSwitchAndOpenProject = { project ->
                    viewModel.switchActiveProject(project, navigateToEngine = true)
                  },
                  onOpenProjectManager = {
                    viewModel.selectWorkspaceTab(StudioWorkspaceTab.CLOUD_GAMES)
                  },
                  onCreateNewProject = { showQuickCreateProjectDialog = true },
                  onSeedStarterProjects = { viewModel.seedStarterCloudFanGames() },
                  onExportActiveProjectJson = launchExportJson,
                  onImportProjectJson = launchImportJson,
                )
              }

              StudioWorkspaceTab.FRANCHISE_DEX -> {
                FranchiseDexAndCreatorSection(
                  nativeList = nativePokemonList,
                  customCloudDexState = customDexState,
                  encyclopediaEntries = encyclopediaEntries,
                  encyclopediaSourceStatuses = encyclopediaSourceStatuses,
                  atlasProgressMap = atlasProgressMap,
                  selectedPrimary = primaryPokemon,
                  selectedSecondary = secondaryPokemon,
                  searchQuery = dexSearchQuery,
                  selectedGen = selectedGenFilter,
                  isSyncingPokeApi = isSyncingPokeApi,
                  isSyncingSources = isSyncingSources,
                  onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                  onSelectGen = { viewModel.updateGenerationFilter(it) },
                  onSelectPrimary = { pkmn -> viewModel.selectPrimaryPokemon(pkmn) },
                  onSelectSecondary = { pkmn -> viewModel.selectSecondaryPokemon(pkmn) },
                  onImportFromPokeApi = { q -> viewModel.importFromNationalPokeApi(q) },
                  onCreateCustomPokemon = { entity, pinToVault ->
                    viewModel.saveLocalCustomPokemon(entity, pinToVault)
                  },
                  onDeleteCloudCustomDex = { id -> viewModel.deleteCustomDexEntry(id) },
                  onSyncEncyclopediaNow = { viewModel.syncEncyclopediaSourcesNow() },
                  onPlayAtlasSpeciesCry = { item -> viewModel.playAtlasSpeciesCry(item) },
                  onHarmonizeSpecies = { dex -> viewModel.harmonizeSpeciesWithHardware(dex) },
                )
              }

              StudioWorkspaceTab.CLOUD_GAMES -> {
                CloudFanGameHubSection(
                  cloudGamesState = cloudFanGamesState,
                  allHardwarePlatforms = hardwareState.allPlatforms,
                  activeProject = activeProject,
                  currentFusionConfig = fusionConfig,
                  hasUnsavedChanges = hasUnsavedChanges,
                  isSavingProject = isSavingProject,
                  searchQuery = projectSearchQuery,
                  onSearchQueryChange = { viewModel.updateProjectSearchQuery(it) },
                  onCreateCloudGame = { title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine ->
                    viewModel.createCloudFanGame(
                      title = title,
                      regionName = region,
                      synopsis = synopsis,
                      consolePreset = consolePreset,
                      dexCount = dexCount,
                      mapCount = mapCount,
                      versionTag = version,
                      captureCurrentEngineSettings = captureEngine,
                    )
                  },
                  onUpdateCloudGame = { existing, title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine ->
                    viewModel.updateCloudFanGameDetails(
                      existing = existing,
                      title = title,
                      regionName = region,
                      synopsis = synopsis,
                      consolePreset = consolePreset,
                      dexCount = dexCount,
                      mapCount = mapCount,
                      versionTag = version,
                      captureCurrentEngineSettings = captureEngine,
                    )
                  },
                  onSaveActiveProject = { viewModel.saveActiveProjectChanges() },
                  onSwitchProject = { project, openEngine ->
                    viewModel.switchActiveProject(project, navigateToEngine = openEngine)
                  },
                  onDuplicateGame = { project -> viewModel.duplicateCloudFanGame(project) },
                  onSeedStarterGames = { viewModel.seedStarterCloudFanGames() },
                  onProvisionMasterTemplate = { viewModel.provisionMasterCanonBaseProject() },
                  onDeleteGame = { id -> viewModel.deleteCloudFanGame(id) },
                  onExportActiveProjectJson = launchExportJson,
                  onImportProjectJson = launchImportJson,
                )
              }

              StudioWorkspaceTab.WORLD_MAP_BUILDER -> {
                WorldMapAndChipLabSection(
                  activeRegionId = activeMapRegionId,
                  mapGrid = mapGrid,
                  selectedBrush = selectedBrush,
                  convergedVisuals = hardwareState.convergedVisuals,
                  timeOfDay = gameTestState.timeOfDay,
                  completedRegionIds = campaignState.completedChapterIds,
                  onSelectRegion = { viewModel.selectMapRegion(it) },
                  onSelectBrush = { viewModel.selectMapBrush(it) },
                  onPaintTile = { viewModel.paintMapTile(it) },
                  onSelectTimeOfDay = { viewModel.setTimeOfDayCycle(it) },
                  onResetRegionMap = { viewModel.resetActiveRegionMapToDefault() },
                  onPinMapAsset = {
                    viewModel.saveCurrentFusionAsSidebarAsset(
                      customName = "${activeMapRegionId.uppercase()} HD-2D Sector Map",
                      category = AssetCategory.MAPS,
                    )
                  },
                )
              }
            }
          }
        }
      }

      if (isCompactPhone) {
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet(
              drawerContainerColor = StudioSurfaceDark,
              modifier = Modifier.width(310.dp),
            ) {
              sidebarComposable(true)
            }
          },
        ) {
          mainWorkspaceContent(Modifier.fillMaxSize())
        }
      } else {
        Row(modifier = Modifier.fillMaxSize()) {
          sidebarComposable(false)
          mainWorkspaceContent(Modifier.weight(1f))
        }
      }
    }
  }

  if (showProjectSwitcherDialog) {
    ProjectSwitcherDialog(
      cloudGamesState = cloudFanGamesState,
      activeProject = activeProject,
      hasUnsavedChanges = hasUnsavedChanges,
      isSavingProject = isSavingProject,
      onDismiss = { showProjectSwitcherDialog = false },
      onSwitchProject = { project -> viewModel.switchActiveProject(project, navigateToEngine = false) },
      onSaveActiveProject = { viewModel.saveActiveProjectChanges() },
      onOpenNewProjectDialog = { showQuickCreateProjectDialog = true },
      onOpenFullProjectManager = { viewModel.selectWorkspaceTab(StudioWorkspaceTab.CLOUD_GAMES) },
    )
  }

  if (showQuickCreateProjectDialog) {
    ProjectEditorDialog(
      existingProject = null,
      currentFusionConfig = fusionConfig,
      onDismiss = { showQuickCreateProjectDialog = false },
      onSave = { title, region, synopsis, consolePreset, dexCount, mapCount, version, captureEngine ->
        viewModel.createCloudFanGame(
          title = title,
          regionName = region,
          synopsis = synopsis,
          consolePreset = consolePreset,
          dexCount = dexCount,
          mapCount = mapCount,
          versionTag = version,
          captureCurrentEngineSettings = captureEngine,
        )
        showQuickCreateProjectDialog = false
      },
    )
  }
}

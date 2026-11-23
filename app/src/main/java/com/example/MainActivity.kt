package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
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
import com.example.data.repository.PokeFusionCloudRepository
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
import com.example.ui.components.StudioAuthGateScreen
import com.example.ui.components.WorldMapAndChipLabSection
import com.example.ui.components.signOutUser
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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

@Composable
fun PokeFusionRootApp() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    StudioAuthGateScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
    )
  } else {
    val uid = user.uid
    val email = user.email ?: "Google Developer"
    key(uid) {
      val viewModel: StudioViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            val localDb = NativePokemonDatabase.getInstance(app)
            StudioViewModel(
              cloudRepository = PokeFusionCloudRepository(firestore, Firebase.auth),
              localDexRepository = NativeFranchiseCatalogRepository(localDb.pokemonDao()),
              encyclopediaAndResourceRepository = FranchiseEncyclopediaAndResourceRepository(
                localDb.resourceDao(),
                localDb.encyclopediaDao(),
              ),
              currentUserId = uid,
            )
          }
        },
      )
      PokeFusionStudioWorkspace(
        viewModel = viewModel,
        userEmail = email,
      )
    }
  }
}

@Composable
fun PokeFusionStudioWorkspace(
  viewModel: StudioViewModel,
  userEmail: String,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

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
  val mapGrid by viewModel.mapGrid.collectAsStateWithLifecycle()
  val selectedBrush by viewModel.selectedBrushTile.collectAsStateWithLifecycle()
  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val roomResources by viewModel.roomResources.collectAsStateWithLifecycle()
  val encyclopediaEntries by viewModel.encyclopediaEntries.collectAsStateWithLifecycle()
  val encyclopediaSourceStatuses by viewModel.encyclopediaSourceStatuses.collectAsStateWithLifecycle()
  val isSyncingSources by viewModel.isSyncingSources.collectAsStateWithLifecycle()

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.onAppForegrounded()
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
  val cloudProjectsList = (cloudFanGamesState as? UiState.Success)?.data ?: emptyList()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = StudioDeepNavy,
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // ALWAYS-VISIBLE CATEGORIZED ASSET SIDE NAVIGATION BAR
      CategorizedAssetSidebar(
        selectedCategory = selectedCategory,
        isExpanded = isSidebarExpanded,
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
        onToggleExpand = { viewModel.toggleSidebarExpanded() },
        onSelectAsset = { asset -> viewModel.applySidebarAssetToEngine(asset) },
        onPinCurrentFusionAsset = {
          viewModel.saveCurrentFusionAsSidebarAsset(
            customName = "${primaryPokemon.name} × ${secondaryPokemon.name} ${selectedCategory.badge}",
            category = selectedCategory,
          )
        },
        onDeleteCustomAsset = { id -> viewModel.deleteCustomSidebarAsset(id) },
        onQuickSaveActiveProject = { viewModel.saveActiveProjectChanges() },
        onOpenProjectSwitcher = { showProjectSwitcherDialog = true },
        onOpenProjectDashboard = { viewModel.selectWorkspaceTab(StudioWorkspaceTab.PROJECT_DASHBOARD) },
      )

      // MAIN STUDIO WORKSPACE PANE
      Column(
        modifier = Modifier
          .weight(1f)
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
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "POKÉFUSION STUDIO",
                  fontFamily = PressStart2PFamily,
                  style = MaterialTheme.typography.labelLarge,
                  color = CyberCyan,
                )
                Text(
                  text = "Connected via Google Cloud • ${syncStatus.statusLabel} • $userEmail",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
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
                      contentDescription = "Refresh live studio updates",
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

                IconButton(
                  onClick = {
                    signOutUser(
                      credentialManager = credentialManager,
                      scope = scope,
                    )
                  },
                  modifier = Modifier.testTag("sign_out_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = ElectricCrimson,
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
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          when (activeTab) {
            StudioWorkspaceTab.FUSION_ENGINE -> {
              OmniConsoleFusionViewport(
                primaryPokemon = primaryPokemon,
                secondaryPokemon = secondaryPokemon,
                config = fusionConfig,
                onUpdateConfig = { transform -> viewModel.updateFusionConfig(transform) },
                onToggleChip = { chipId -> viewModel.toggleHardwareChip(chipId) },
                onPinToSidebar = { category ->
                  viewModel.saveCurrentFusionAsSidebarAsset(
                    customName = "${primaryPokemon.name} × ${secondaryPokemon.name} ${category.badge}",
                    category = category,
                  )
                },
              )
            }

            StudioWorkspaceTab.SPRITE_EDITOR -> {
              SpriteEditorSection(
                activePokemon = primaryPokemon,
                customAssets = customAssetsList,
                isSavingToCloud = isSavingProject,
                onSaveEditedSprite = { name, seedDex, primaryHex, secondaryHex, paletteSummary, frameCount ->
                  viewModel.saveEditedSpriteAsset(
                    name = name,
                    seedDexNumber = seedDex,
                    primaryHex = primaryHex,
                    secondaryHex = secondaryHex,
                    paletteSummary = paletteSummary,
                    frameCount = frameCount,
                  )
                },
                onApplyPaletteToSpecies = { primaryHex, secondaryHex ->
                  viewModel.applyEditedPaletteToActivePokemon(primaryHex, secondaryHex)
                },
              )
            }

            StudioWorkspaceTab.RESOURCE_MANAGER -> {
              ResourceManagerSection(
                resources = roomResources,
                projects = cloudProjectsList,
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
                activeProject = activeProject,
                primaryPokemon = primaryPokemon,
                secondaryPokemon = secondaryPokemon,
                nativeDex = nativePokemonList,
                fusionConfig = fusionConfig,
                mapGrid = mapGrid,
              )
            }

            StudioWorkspaceTab.BASE_TEMPLATE -> {
              CanonTemplateSandboxSection(
                attackerEntity = primaryPokemon,
                defenderEntity = secondaryPokemon,
                isSavingProject = isSavingProject,
                onProvisionMasterBaseProject = { viewModel.provisionMasterCanonBaseProject() },
              )
            }

            StudioWorkspaceTab.PROJECT_DASHBOARD -> {
              ProjectDashboard(
                cloudGamesState = cloudFanGamesState,
                projectOverviewState = projectOverviewState,
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
              )
            }

            StudioWorkspaceTab.FRANCHISE_DEX -> {
              FranchiseDexAndCreatorSection(
                nativeList = nativePokemonList,
                customCloudDexState = customDexState,
                encyclopediaEntries = encyclopediaEntries,
                encyclopediaSourceStatuses = encyclopediaSourceStatuses,
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
                onCreateCustomPokemon = { entity, syncCloud ->
                  viewModel.saveLocalCustomPokemon(entity, syncCloud)
                },
                onDeleteCloudCustomDex = { id -> viewModel.deleteCustomDexEntry(id) },
                onSyncEncyclopediaNow = { viewModel.syncEncyclopediaSourcesNow() },
              )
            }

            StudioWorkspaceTab.CLOUD_GAMES -> {
              CloudFanGameHubSection(
                cloudGamesState = cloudFanGamesState,
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
              )
            }

            StudioWorkspaceTab.WORLD_MAP_BUILDER -> {
              WorldMapAndChipLabSection(
                mapGrid = mapGrid,
                selectedBrush = selectedBrush,
                onSelectBrush = { viewModel.selectMapBrush(it) },
                onPaintTile = { viewModel.paintMapTile(it) },
                onPinMapAsset = {
                  viewModel.saveCurrentFusionAsSidebarAsset(
                    customName = "${primaryPokemon.region} HD-2D Sector Map",
                    category = AssetCategory.MAPS,
                  )
                },
              )
            }
          }
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

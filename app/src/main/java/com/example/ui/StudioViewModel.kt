package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EncyclopediaEntryEntity
import com.example.data.local.EncyclopediaSeedCatalog
import com.example.data.local.EncyclopediaSourceStatusEntity
import com.example.data.local.FranchiseEncyclopediaAndResourceRepository
import com.example.data.local.NativeFranchiseCatalogRepository
import com.example.data.local.NativePokemonEntity
import com.example.data.local.ResourceCategory
import com.example.data.local.StudioResourceEntity
import com.example.data.model.AssetCategory
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.ConsoleHardwareCatalog
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import com.example.data.repository.PokeFusionCloudRepository
import com.example.engine.MasterCanonTemplateCatalog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class StudioWorkspaceTab(val id: String, val title: String) {
  FUSION_ENGINE("fusion_engine", "Omni-Fusion Engine"),
  SPRITE_EDITOR("sprite_editor", "Sprite Editor"),
  RESOURCE_MANAGER("resource_manager", "Resource Manager"),
  GAME_TEST("game_test", "Game Test"),
  BASE_TEMPLATE("base_template", "Canon Base Template"),
  PROJECT_DASHBOARD("project_dashboard", "Project Dashboard"),
  CLOUD_GAMES("cloud_games", "Project Manager"),
  FRANCHISE_DEX("franchise_dex", "Integrated Dex & Creator"),
  WORLD_MAP_BUILDER("world_map", "HD-2D Map & Chip Lab"),
}

data class BuiltInAssetItem(
  val id: String,
  val name: String,
  val category: AssetCategory,
  val consoleLineage: String,
  val chipAddon: String,
  val spriteSeedId: Int,
  val shaderFormula: String,
  val isCustomCloud: Boolean = false,
  val cloudDocId: String = "",
  val thumbnailUrl: String? = null,
  val databaseTag: String? = null,
)

data class ProjectOverviewStats(
  val projectId: String,
  val projectTitle: String,
  val versionTag: String,
  val regionName: String,
  val consoleMatrixPreset: String,
  val assetCount: Int,
  val pinnedCloudAssetsCount: Int,
  val customDexCount: Int,
  val activeChipsCount: Int,
  val dexTargetCount: Long,
  val mapCount: Long,
  val blendRatioPercent: Long,
  val lastModified: Timestamp?,
  val createdAt: Timestamp?,
)

data class StudioSyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncedAtMs: Long? = null,
  val statusLabel: String = "Checking updates...",
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
  private val cloudRepository: PokeFusionCloudRepository,
  private val localDexRepository: NativeFranchiseCatalogRepository,
  private val encyclopediaAndResourceRepository: FranchiseEncyclopediaAndResourceRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _activeTab = MutableStateFlow(StudioWorkspaceTab.FUSION_ENGINE)
  val activeTab: StateFlow<StudioWorkspaceTab> = _activeTab.asStateFlow()

  private val _selectedSidebarCategory = MutableStateFlow(AssetCategory.SPRITES)
  val selectedSidebarCategory: StateFlow<AssetCategory> = _selectedSidebarCategory.asStateFlow()

  private val _sidebarExpanded = MutableStateFlow(true)
  val sidebarExpanded: StateFlow<Boolean> = _sidebarExpanded.asStateFlow()

  private val _selectedPokemon = MutableStateFlow(
    NativeFranchiseCatalogRepository.builtInFranchiseDex.first { it.dexNumber == 384 }
  )
  val selectedPokemon: StateFlow<NativePokemonEntity> = _selectedPokemon.asStateFlow()

  private val _secondaryFusionPokemon = MutableStateFlow(
    NativeFranchiseCatalogRepository.builtInFranchiseDex.first { it.dexNumber == 6 }
  )
  val secondaryFusionPokemon: StateFlow<NativePokemonEntity> = _secondaryFusionPokemon.asStateFlow()

  private val _fusionConfig = MutableStateFlow(ConsoleFusionConfig())
  val fusionConfig: StateFlow<ConsoleFusionConfig> = _fusionConfig.asStateFlow()

  private val _dexSearchQuery = MutableStateFlow("")
  val dexSearchQuery: StateFlow<String> = _dexSearchQuery.asStateFlow()

  private val _selectedGenerationFilter = MutableStateFlow("All")
  val selectedGenerationFilter: StateFlow<String> = _selectedGenerationFilter.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  private val _isSyncingPokeApi = MutableStateFlow(false)
  val isSyncingPokeApi: StateFlow<Boolean> = _isSyncingPokeApi.asStateFlow()

  private val _activeProjectId = MutableStateFlow<String?>(null)
  val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

  private val _hasUnsavedChanges = MutableStateFlow(false)
  val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

  private val _isSavingProject = MutableStateFlow(false)
  val isSavingProject: StateFlow<Boolean> = _isSavingProject.asStateFlow()

  private val _projectSearchQuery = MutableStateFlow("")
  val projectSearchQuery: StateFlow<String> = _projectSearchQuery.asStateFlow()

  private val _sidebarFilterByActiveProject = MutableStateFlow(false)
  val sidebarFilterByActiveProject: StateFlow<Boolean> = _sidebarFilterByActiveProject.asStateFlow()

  private val _sidebarSearchQuery = MutableStateFlow("")
  val sidebarSearchQuery: StateFlow<String> = _sidebarSearchQuery.asStateFlow()

  private val _syncStatus = MutableStateFlow(StudioSyncStatus(isSyncing = true))
  val syncStatus: StateFlow<StudioSyncStatus> = _syncStatus.asStateFlow()

  private var hasAutoLoadedInitialProject = false
  private var lastSyncStartedAtMs: Long = 0L

  // Map Editor 8x8 tile grid storing tile terrain types (0=Grass, 1=Water, 2=CyberRoad, 3=GymFloor, 4=Lava, 5=Crystal)
  private val _mapGrid = MutableStateFlow(
    List(64) { idx ->
      val r = idx / 8
      val c = idx % 8
      when {
        r == 0 || r == 7 -> 5
        c == 3 || c == 4 -> 2
        (r == 3 || r == 4) && (c <= 1 || c >= 6) -> 1
        r == 2 && c == 2 -> 3
        else -> 0
      }
    }
  )
  val mapGrid: StateFlow<List<Int>> = _mapGrid.asStateFlow()

  private val _selectedBrushTile = MutableStateFlow(2)
  val selectedBrushTile: StateFlow<Int> = _selectedBrushTile.asStateFlow()

  private val _isSyncingSources = MutableStateFlow(false)
  val isSyncingSources: StateFlow<Boolean> = _isSyncingSources.asStateFlow()

  val nativePokemonList: StateFlow<List<NativePokemonEntity>> =
    localDexRepository.allNativePokemon.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = NativeFranchiseCatalogRepository.builtInFranchiseDex,
    )

  val roomResources: StateFlow<List<StudioResourceEntity>> =
    encyclopediaAndResourceRepository.allResources.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = EncyclopediaSeedCatalog.builtInResources,
    )

  val encyclopediaEntries: StateFlow<List<EncyclopediaEntryEntity>> =
    encyclopediaAndResourceRepository.allEncyclopediaEntries.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = EncyclopediaSeedCatalog.defaultEncyclopediaEntries,
    )

  val encyclopediaSourceStatuses: StateFlow<List<EncyclopediaSourceStatusEntity>> =
    encyclopediaAndResourceRepository.sourceStatuses.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = EncyclopediaSeedCatalog.defaultSourceStatuses,
    )

  val cloudFanGamesState: StateFlow<UiState<List<FanGameProject>>> =
    cloudRepository.observeFanGames(currentUserId)
      .map<List<FanGameProject>, UiState<List<FanGameProject>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.message ?: "Failed to load cloud fan games"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val activeProject: StateFlow<FanGameProject?> =
    combine(cloudFanGamesState, _activeProjectId) { gamesState, activeId ->
      val projects = (gamesState as? UiState.Success)?.data ?: return@combine null
      if (activeId != null) {
        projects.firstOrNull { it.id == activeId }
      } else {
        null
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  val customDexState: StateFlow<UiState<List<CustomDexEntry>>> =
    cloudRepository.observeCustomDex(currentUserId)
      .map<List<CustomDexEntry>, UiState<List<CustomDexEntry>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.message ?: "Failed to load custom Pokedex entries"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val customAssetsState: StateFlow<UiState<List<CustomStudioAsset>>> =
    cloudRepository.observeCustomAssets(currentUserId)
      .map<List<CustomStudioAsset>, UiState<List<CustomStudioAsset>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        emit(UiState.Error(rootError.message ?: "Failed to load custom studio assets"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val projectOverviewState: StateFlow<UiState<ProjectOverviewStats?>> =
    _activeProjectId.flatMapLatest { selectedId ->
      if (selectedId.isNullOrBlank()) {
        flowOf<UiState<ProjectOverviewStats?>>(UiState.Success(null))
      } else {
        combine(
          cloudRepository.observeFanGameById(currentUserId, selectedId),
          customAssetsState,
          customDexState,
        ) { liveProject, assetsState, dexState ->
            val overview: ProjectOverviewStats? = if (liveProject == null) {
              null
            } else {
              val pinnedAssets = (assetsState as? UiState.Success)?.data ?: emptyList()
              val customDex = (dexState as? UiState.Success)?.data ?: emptyList()
              val activeChips = parseChipIds(liveProject.activeChipsCsv)
              val totalAssetCount = pinnedAssets.size + customDex.size + activeChips.size

              // Pick the latest modification timestamp across the project document and synced assets
              val candidateTimestamps = buildList {
                liveProject.updatedAt?.let { add(it) }
                liveProject.createdAt?.let { add(it) }
                pinnedAssets.mapNotNull { it.updatedAt ?: it.createdAt }.maxByOrNull { it.seconds }?.let { add(it) }
                customDex.mapNotNull { it.updatedAt ?: it.createdAt }.maxByOrNull { it.seconds }?.let { add(it) }
              }
              val latestModified = candidateTimestamps.maxByOrNull { it.seconds * 1_000_000_000L + it.nanoseconds }

              ProjectOverviewStats(
                projectId = liveProject.id,
                projectTitle = liveProject.title.ifBlank { "Untitled Fan Game" },
                versionTag = liveProject.versionTag,
                regionName = liveProject.regionName,
                consoleMatrixPreset = liveProject.consoleMatrixPreset,
                assetCount = totalAssetCount,
                pinnedCloudAssetsCount = pinnedAssets.size,
                customDexCount = customDex.size,
                activeChipsCount = activeChips.size,
                dexTargetCount = liveProject.dexCount,
                mapCount = liveProject.mapCount,
                blendRatioPercent = liveProject.pixel3dBlendRatio,
                lastModified = latestModified,
                createdAt = liveProject.createdAt,
              )
            }
            UiState.Success(overview) as UiState<ProjectOverviewStats?>
        }.catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          emit(UiState.Error(rootError.message ?: "Failed to load real-time project statistics"))
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  init {
    syncStudioDataOnLoad(announceCompletion = false)

    viewModelScope.launch {
      nativePokemonList.collect { catalog ->
        val updatedPrimary = catalog.firstOrNull { it.dexNumber == _selectedPokemon.value.dexNumber }
        if (updatedPrimary != null && updatedPrimary != _selectedPokemon.value) {
          _selectedPokemon.value = updatedPrimary
        }
        val updatedSecondary = catalog.firstOrNull { it.dexNumber == _secondaryFusionPokemon.value.dexNumber }
        if (updatedSecondary != null && updatedSecondary != _secondaryFusionPokemon.value) {
          _secondaryFusionPokemon.value = updatedSecondary
        }
      }
    }

    viewModelScope.launch {
      cloudFanGamesState.collect { state ->
        if (!hasAutoLoadedInitialProject && state is UiState.Success && state.data.isNotEmpty() && _activeProjectId.value == null) {
          hasAutoLoadedInitialProject = true
          applyProjectToEngine(state.data.first(), announce = false)
        }
      }
    }

    viewModelScope.launch {
      activeProject.collect { latestProject ->
        if (latestProject != null && !_hasUnsavedChanges.value) {
          val parsedChips = parseChipIds(latestProject.activeChipsCsv)
          val remotePreset = latestProject.consoleMatrixPreset.ifBlank { latestProject.title }
          val remoteBlend = (latestProject.pixel3dBlendRatio / 100f).coerceIn(0f, 1f)
          val currentCfg = _fusionConfig.value
          val targetChips = if (parsedChips.isNotEmpty()) parsedChips else currentCfg.activeChipIds
          if (currentCfg.presetName != remotePreset ||
            currentCfg.pixelTo3dBlend != remoteBlend ||
            currentCfg.activeChipIds != targetChips
          ) {
            _fusionConfig.update {
              it.copy(
                presetName = remotePreset,
                pixelTo3dBlend = remoteBlend,
                activeChipIds = targetChips,
              )
            }
          }
        }
      }
    }
  }

  fun onAppForegrounded() {
    val now = System.currentTimeMillis()
    if (now - lastSyncStartedAtMs >= 25_000L && !_syncStatus.value.isSyncing) {
      syncStudioDataOnLoad(announceCompletion = false)
    }
  }

  fun refreshStudioNow() {
    syncStudioDataOnLoad(announceCompletion = true)
  }

  private fun syncStudioDataOnLoad(announceCompletion: Boolean) {
    lastSyncStartedAtMs = System.currentTimeMillis()
    viewModelScope.launch {
      _syncStatus.update { it.copy(isSyncing = true, statusLabel = "Syncing live data...") }
      _isSyncingSources.value = true

      val localDeferred = async { runCatching { localDexRepository.ensureSeeded() }.getOrDefault(0) }
      val encyclopediaDeferred = async {
        runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      }
      val cloudDeferred = async { cloudRepository.refreshFromServer(currentUserId) }
      val primaryRefreshDeferred = async {
        localDexRepository.refreshLivePokemonIfOnline(_selectedPokemon.value.dexNumber)
      }

      val updatedLocalCount = localDeferred.await()
      val webSourcesSynced = encyclopediaDeferred.await()
      val cloudResult = cloudDeferred.await()
      val apiResult = primaryRefreshDeferred.await()
      _isSyncingSources.value = false

      val finishedAt = System.currentTimeMillis()
      val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(finishedAt))
      val cloudOk = cloudResult.isSuccess
      val apiOk = apiResult.isSuccess
      val statusText = when {
        cloudOk && webSourcesSynced > 0 -> "Up to date ($webSourcesSynced/3 DBs) • $timeStr"
        cloudOk || apiOk -> "Up to date • $timeStr"
        else -> "Local synced • $timeStr"
      }

      _syncStatus.value = StudioSyncStatus(
        isSyncing = false,
        lastSyncedAtMs = finishedAt,
        statusLabel = statusText,
      )

      if (announceCompletion) {
        val cloudDocs = cloudResult.getOrNull() ?: 0
        _statusBannerMessage.value =
          "Synced ($timeStr): $webSourcesSynced/3 wiki DBs (Bulbapedia/PokémonDB/Serebii), $cloudDocs cloud docs & $updatedLocalCount Dex updates."
      }
    }
  }

  fun syncEncyclopediaSourcesNow() {
    viewModelScope.launch {
      _isSyncingSources.value = true
      val count = runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      _isSyncingSources.value = false
      _statusBannerMessage.value = "Updated $count/3 canonical databases from Bulbapedia, PokémonDB & Serebii.net."
    }
  }

  fun addCustomRoomResource(
    name: String,
    category: ResourceCategory,
    formatTag: String,
    consoleChip: String,
    durationOrGrid: String,
    description: String,
  ) {
    viewModelScope.launch {
      val created = encyclopediaAndResourceRepository.addCustomResource(
        name = name,
        category = category,
        formatTag = formatTag,
        consoleChip = consoleChip,
        durationOrGrid = durationOrGrid,
        description = description,
        accentHex = when (category) {
          ResourceCategory.SFX -> "#00F5D4"
          ResourceCategory.BGM -> "#FF2A85"
          ResourceCategory.MAP_TILES -> "#10B981"
        },
      )
      _statusBannerMessage.value = "Saved '${created.name}' [${category.shortBadge}] to local Room Resource Manager!"
    }
  }

  fun deleteCustomRoomResource(resourceId: String) {
    viewModelScope.launch {
      encyclopediaAndResourceRepository.deleteCustomResource(resourceId)
      _statusBannerMessage.value = "Removed custom resource from local Room database."
    }
  }

  fun previewRoomResource(resource: StudioResourceEntity) {
    if (resource.categoryId == ResourceCategory.MAP_TILES.id) {
      if (resource.tileTypeIndex in 0..5) {
        _selectedBrushTile.value = resource.tileTypeIndex
      }
      _statusBannerMessage.value = "Loaded Map Tile '${resource.name}' into HD-2D Tile Brush!"
    } else {
      _statusBannerMessage.value = "Playing synth audio preview: ${resource.name} (${resource.formatTag})"
      viewModelScope.launch {
        encyclopediaAndResourceRepository.playRetroAudioPreview(resource)
      }
    }
  }

  fun assignResourceToGameProject(resource: StudioResourceEntity, project: FanGameProject) {
    viewModelScope.launch {
      encyclopediaAndResourceRepository.assignResourceToProject(resource.id, project.id)
      val chipToken = when (resource.categoryId) {
        ResourceCategory.BGM.id, ResourceCategory.SFX.id -> "msu_1"
        else -> "super_fx2"
      }
      if (project.id == _activeProjectId.value) {
        _fusionConfig.update { it.copy(activeChipIds = it.activeChipIds + chipToken) }
      }
      val updatedMapCount = if (resource.categoryId == ResourceCategory.MAP_TILES.id) {
        project.mapCount + 1L
      } else {
        project.mapCount
      }
      if (project.id.isNotBlank()) {
        cloudRepository.updateFanGame(project.copy(mapCount = updatedMapCount))
      }
      _statusBannerMessage.value = "Attached '${resource.name}' into '${project.title.ifBlank { "Fan Game" }}'!"
    }
  }

  fun unassignResourceFromGameProject(resource: StudioResourceEntity, project: FanGameProject) {
    viewModelScope.launch {
      encyclopediaAndResourceRepository.unassignResourceFromProject(resource.id, project.id)
      _statusBannerMessage.value = "Detached '${resource.name}' from '${project.title}'."
    }
  }

  fun selectWorkspaceTab(tab: StudioWorkspaceTab) {
    _activeTab.value = tab
  }

  fun selectSidebarCategory(category: AssetCategory) {
    _selectedSidebarCategory.value = category
  }

  fun toggleSidebarExpanded() {
    _sidebarExpanded.update { !it }
  }

  fun selectPrimaryPokemon(pokemon: NativePokemonEntity) {
    _selectedPokemon.value = pokemon
    _statusBannerMessage.value = "Loaded #${pokemon.dexNumber} ${pokemon.name} into Omni-Console Fusion Viewport"
  }

  fun selectSecondaryPokemon(pokemon: NativePokemonEntity) {
    _secondaryFusionPokemon.value = pokemon
    _statusBannerMessage.value = "Set #${pokemon.dexNumber} ${pokemon.name} as Secondary DNA / Palette Donor"
  }

  fun updateSearchQuery(query: String) {
    _dexSearchQuery.value = query
  }

  fun updateGenerationFilter(gen: String) {
    _selectedGenerationFilter.value = gen
  }

  fun updateProjectSearchQuery(query: String) {
    _projectSearchQuery.value = query
  }

  fun toggleSidebarActiveProjectFilter() {
    _sidebarFilterByActiveProject.update { !it }
  }

  fun updateSidebarSearchQuery(query: String) {
    _sidebarSearchQuery.value = query
  }

  fun dismissStatusBanner() {
    _statusBannerMessage.value = null
  }

  fun updateFusionConfig(transform: (ConsoleFusionConfig) -> ConsoleFusionConfig) {
    _fusionConfig.update(transform)
    if (_activeProjectId.value != null) {
      _hasUnsavedChanges.value = true
    }
  }

  fun toggleHardwareChip(chipId: String) {
    _fusionConfig.update { current ->
      val updated = if (chipId in current.activeChipIds) {
        current.activeChipIds - chipId
      } else {
        current.activeChipIds + chipId
      }
      current.copy(activeChipIds = updated)
    }
    if (_activeProjectId.value != null) {
      _hasUnsavedChanges.value = true
    }
  }

  fun applySidebarAssetToEngine(asset: BuiltInAssetItem) {
    val match = nativePokemonList.value.find { it.dexNumber == asset.spriteSeedId }
      ?: NativeFranchiseCatalogRepository.builtInFranchiseDex.firstOrNull { it.dexNumber == asset.spriteSeedId }
    if (match != null) {
      _selectedPokemon.value = match
    }
    _fusionConfig.update { current ->
      val updatedBlend = when {
        asset.id.startsWith("db_3d_") -> 0.85f
        asset.id.startsWith("db_spr_") -> 0.25f
        else -> current.pixelTo3dBlend
      }
      current.copy(
        presetName = "${asset.name} (${asset.chipAddon})",
        pixelTo3dBlend = updatedBlend,
      )
    }
    if (_activeProjectId.value != null) {
      _hasUnsavedChanges.value = true
    }
    _statusBannerMessage.value = "Applied [${asset.category.label}] ${asset.name} (${asset.consoleLineage})"
  }

  fun paintMapTile(index: Int) {
    if (index !in 0..63) return
    val brush = _selectedBrushTile.value
    _mapGrid.update { current ->
      current.toMutableList().also { it[index] = brush }
    }
  }

  fun selectMapBrush(tileType: Int) {
    _selectedBrushTile.value = tileType
  }

  fun importFromNationalPokeApi(query: String) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _isSyncingPokeApi.value = true
      val result = localDexRepository.fetchAndImportFromPokeApi(query)
      _isSyncingPokeApi.value = false
      result.onSuccess { entity ->
        _selectedPokemon.value = entity
        _statusBannerMessage.value = "Synced #${entity.dexNumber} ${entity.name} from National Dex into local & fusion engine!"
      }.onFailure { err ->
        _statusBannerMessage.value = "National Dex lookup notice: ${err.message}"
      }
    }
  }

  fun saveLocalCustomPokemon(entity: NativePokemonEntity, alsoSyncToCloud: Boolean) {
    viewModelScope.launch {
      localDexRepository.upsertLocalPokemon(entity.copy(isUserModified = true))
      _selectedPokemon.value = entity
      if (alsoSyncToCloud) {
        val cloudEntry = CustomDexEntry(
          dexNumber = entity.dexNumber.toLong(),
          name = entity.name,
          category = "${entity.region} Fusion Species",
          primaryType = entity.primaryType,
          secondaryType = entity.secondaryType,
          abilityName = entity.abilityPrimary,
          signatureMove = entity.signatureMove,
          loreDescription = entity.loreSummary,
          baseHp = entity.baseHp.toLong(),
          baseAttack = entity.baseAttack.toLong(),
          baseDefense = entity.baseDefense.toLong(),
          baseSpAtk = entity.baseSpAtk.toLong(),
          baseSpDef = entity.baseSpDef.toLong(),
          baseSpeed = entity.baseSpeed.toLong(),
          baseSpriteId = entity.dexNumber.coerceIn(1, 1025).toLong(),
          fusionStylePreset = entity.consoleOriginSystem,
          enhancementChip = entity.recommendedChipAddon,
          primaryHex = entity.primaryHex,
          secondaryHex = entity.secondaryHex,
        )
        cloudRepository.createCustomDexEntry(cloudEntry)
          .onSuccess {
            _statusBannerMessage.value = "Saved ${entity.name} to Integrated Database & Google Cloud Dex!"
          }
          .onFailure { e ->
            _statusBannerMessage.value = "Saved locally; Cloud sync error: ${e.localizedMessage}"
          }
      } else {
        _statusBannerMessage.value = "Saved ${entity.name} to Integrated Franchise Database!"
      }
    }
  }

  fun switchActiveProject(project: FanGameProject, navigateToEngine: Boolean = false) {
    applyProjectToEngine(project, announce = true)
    if (navigateToEngine) {
      _activeTab.value = StudioWorkspaceTab.FUSION_ENGINE
    }
  }

  private fun applyProjectToEngine(project: FanGameProject, announce: Boolean) {
    _activeProjectId.value = project.id
    val parsedChips = parseChipIds(project.activeChipsCsv)
    _fusionConfig.update { current ->
      current.copy(
        presetName = project.consoleMatrixPreset.ifBlank { project.title },
        pixelTo3dBlend = (project.pixel3dBlendRatio / 100f).coerceIn(0f, 1f),
        activeChipIds = if (parsedChips.isNotEmpty()) parsedChips else current.activeChipIds,
      )
    }
    _hasUnsavedChanges.value = false
    if (announce) {
      _statusBannerMessage.value = "Switched active project to '${project.title}' (${project.versionTag})"
    }
  }

  private fun parseChipIds(csv: String): Set<String> {
    if (csv.isBlank()) return emptySet()
    val catalog = ConsoleHardwareCatalog.officialAndFanChips
    return csv.split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .mapNotNull { token ->
        catalog.firstOrNull {
          it.id.equals(token, ignoreCase = true) ||
            it.name.contains(token, ignoreCase = true)
        }?.id
      }
      .toSet()
  }

  fun saveActiveProjectChanges() {
    val current = activeProject.value
    if (current == null || current.id.isBlank()) {
      _statusBannerMessage.value = "Select or create a cloud project first to save engine changes."
      return
    }
    viewModelScope.launch {
      _isSavingProject.value = true
      val cfg = _fusionConfig.value
      val updated = current.copy(
        consoleMatrixPreset = cfg.presetName.ifBlank { current.consoleMatrixPreset },
        activeChipsCsv = cfg.activeChipIds.joinToString(","),
        pixel3dBlendRatio = (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong(),
      )
      cloudRepository.updateFanGame(updated)
        .onSuccess {
          _isSavingProject.value = false
          _hasUnsavedChanges.value = false
          _statusBannerMessage.value = "Saved '${updated.title}' engine build to Cloud Firestore!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Failed to save project: ${e.localizedMessage}"
        }
    }
  }

  fun updateCloudFanGameDetails(
    existing: FanGameProject,
    title: String,
    regionName: String,
    synopsis: String,
    consolePreset: String,
    dexCount: Int,
    mapCount: Int,
    versionTag: String,
    captureCurrentEngineSettings: Boolean,
  ) {
    if (existing.id.isBlank()) return
    viewModelScope.launch {
      _isSavingProject.value = true
      val cfg = _fusionConfig.value
      val updated = existing.copy(
        title = title.trim().ifEmpty { existing.title },
        regionName = regionName.trim().ifEmpty { existing.regionName },
        synopsis = synopsis.trim().ifEmpty { existing.synopsis },
        consoleMatrixPreset = if (captureCurrentEngineSettings) {
          cfg.presetName.ifBlank { consolePreset }
        } else {
          consolePreset.trim().ifEmpty { existing.consoleMatrixPreset }
        },
        activeChipsCsv = if (captureCurrentEngineSettings) {
          cfg.activeChipIds.joinToString(",")
        } else {
          existing.activeChipsCsv
        },
        pixel3dBlendRatio = if (captureCurrentEngineSettings) {
          (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong()
        } else {
          existing.pixel3dBlendRatio
        },
        dexCount = dexCount.coerceIn(0, 2000).toLong(),
        mapCount = mapCount.coerceIn(0, 500).toLong(),
        versionTag = versionTag.trim().ifEmpty { existing.versionTag },
      )
      cloudRepository.updateFanGame(updated)
        .onSuccess {
          _isSavingProject.value = false
          if (updated.id == _activeProjectId.value) {
            applyProjectToEngine(updated, announce = false)
          }
          _statusBannerMessage.value = "Updated '${updated.title}' in Cloud Firestore!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Update failed: ${e.localizedMessage}"
        }
    }
  }

  fun createCloudFanGame(
    title: String,
    regionName: String,
    synopsis: String,
    consolePreset: String,
    dexCount: Int,
    mapCount: Int,
    versionTag: String,
    captureCurrentEngineSettings: Boolean = true,
  ) {
    viewModelScope.launch {
      _isSavingProject.value = true
      val cfg = _fusionConfig.value
      val resolvedPreset = if (captureCurrentEngineSettings) {
        consolePreset.ifBlank { cfg.presetName }
      } else {
        consolePreset.ifBlank { "Omni-Era HD-2D + Super FX2" }
      }
      val project = FanGameProject(
        title = title,
        regionName = regionName,
        synopsis = synopsis,
        consoleMatrixPreset = resolvedPreset,
        activeChipsCsv = if (captureCurrentEngineSettings) {
          cfg.activeChipIds.joinToString(",")
        } else {
          "super_fx2,sa_1,sgb2_border,voxelizer_fx"
        },
        pixel3dBlendRatio = if (captureCurrentEngineSettings) {
          (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong()
        } else {
          65L
        },
        dexCount = dexCount.coerceIn(0, 2000).toLong(),
        mapCount = mapCount.coerceIn(0, 500).toLong(),
        versionTag = versionTag,
      )
      cloudRepository.createFanGame(project)
        .onSuccess { newId ->
          _isSavingProject.value = false
          _activeProjectId.value = newId
          _fusionConfig.update { it.copy(presetName = resolvedPreset) }
          _hasUnsavedChanges.value = false
          hasAutoLoadedInitialProject = true
          _statusBannerMessage.value = "Created & switched active project to '$title' in Firestore!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Failed to save cloud fan game: ${e.localizedMessage}"
        }
    }
  }

  fun duplicateCloudFanGame(source: FanGameProject) {
    viewModelScope.launch {
      _isSavingProject.value = true
      val copyTitle = "${source.title.take(70)} (Copy)"
      val duplicate = source.copy(
        id = "",
        title = copyTitle,
        createdAt = null,
        updatedAt = null,
      )
      cloudRepository.createFanGame(duplicate)
        .onSuccess { newId ->
          _isSavingProject.value = false
          applyProjectToEngine(duplicate.copy(id = newId), announce = false)
          _statusBannerMessage.value = "Duplicated '${source.title}' as '$copyTitle' and switched to it!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Duplicate failed: ${e.localizedMessage}"
        }
    }
  }

  fun provisionMasterCanonBaseProject() {
    viewModelScope.launch {
      _isSavingProject.value = true
      val templateProject = MasterCanonTemplateCatalog.createBaseProjectTemplate()
      cloudRepository.createFanGame(templateProject)
        .onSuccess { newId ->
          _isSavingProject.value = false
          applyProjectToEngine(templateProject.copy(id = newId), announce = false)
          hasAutoLoadedInitialProject = true
          _statusBannerMessage.value = "Provisioned Master Canon Base Template ('${templateProject.title}') to Firestore!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Failed to provision base template: ${e.localizedMessage}"
        }
    }
  }

  fun seedStarterCloudFanGames() {
    viewModelScope.launch {
      _isSavingProject.value = true
      val starters = listOf(
        FanGameProject(
          title = "Pokémon Chrono Quartz",
          regionName = "Aethelgard Sector",
          synopsis = "Time-rifting RPG fusing GBC Hi-Color sprites, GBA Mode-7 sky islands, N64 Stadium vertex lighting, and Switch HD-2D bloom.",
          consoleMatrixPreset = "GBC + GBA + N64 + Switch HD-2D",
          activeChipsCsv = "super_fx2,sa_1,sgb2_border,n64_exp_pak,voxelizer_fx,pokecrt_rtx",
          pixel3dBlendRatio = 68L,
          dexCount = 386L,
          mapCount = 42L,
          versionTag = "v2.4.0-OMEGA",
        ),
        FanGameProject(
          title = "Pokémon Shadow Orre: 32X Redux",
          regionName = "Orre Cyber-Wastes",
          synopsis = "Demake + Remaster fusion combining GameCube Colosseum/XD TEV shadow shaders with Sega 32X polygons and Gen V animated sprites.",
          consoleMatrixPreset = "Gen V Sprite + Sega 32X + GameCube TEV",
          activeChipsCsv = "sega_32x,e_reader_gba,msu_1,voxelizer_fx",
          pixel3dBlendRatio = 75L,
          dexCount = 251L,
          mapCount = 28L,
          versionTag = "v1.8.2-TEV",
        ),
      )
      var firstId: String? = null
      starters.forEachIndexed { index, starter ->
        val res = cloudRepository.createFanGame(starter)
        if (index == 0) {
          firstId = res.getOrNull()
        }
      }
      _isSavingProject.value = false
      if (firstId != null) {
        applyProjectToEngine(starters.first().copy(id = firstId!!), announce = false)
      }
      _statusBannerMessage.value = "Provisioned 2 flagship multi-console fan game projects to Firestore!"
    }
  }

  fun deleteCloudFanGame(gameId: String) {
    viewModelScope.launch {
      cloudRepository.deleteFanGame(gameId)
        .onSuccess {
          if (_activeProjectId.value == gameId) {
            _activeProjectId.value = null
            _hasUnsavedChanges.value = false
          }
          _statusBannerMessage.value = "Removed project from Google Cloud Firestore."
        }
        .onFailure { e ->
          _statusBannerMessage.value = "Delete failed: ${e.localizedMessage}"
        }
    }
  }

  fun deleteCustomDexEntry(entryId: String) {
    viewModelScope.launch {
      cloudRepository.deleteCustomDexEntry(entryId)
        .onSuccess { _statusBannerMessage.value = "Removed custom species from Google Cloud." }
    }
  }

  fun saveCurrentFusionAsSidebarAsset(customName: String, category: AssetCategory) {
    viewModelScope.launch {
      val pkmn = _selectedPokemon.value
      val cfg = _fusionConfig.value
      val asset = CustomStudioAsset(
        name = customName.ifBlank { "${pkmn.name} ${category.badge} Rig" },
        category = category.label,
        consoleLineage = pkmn.consoleOriginSystem,
        chipAddon = cfg.activeChipIds.take(3).joinToString("+").ifEmpty { "Super FX2" },
        spriteSeedId = pkmn.dexNumber.toLong(),
        shaderFormula = "BLEND:${(cfg.pixelTo3dBlend * 100).toInt()}|VOX:${(cfg.voxelExtrusionDepth * 100).toInt()}|CRT:${(cfg.crtScanlineIntensity * 100).toInt()}",
      )
      cloudRepository.createCustomAsset(asset)
        .onSuccess {
          _selectedSidebarCategory.value = category
          _statusBannerMessage.value = "Pinned '${asset.name}' to Side Navigation Bar [${category.label}] & synced to Google Cloud!"
        }
        .onFailure { e ->
          _statusBannerMessage.value = "Could not pin asset: ${e.localizedMessage}"
        }
    }
  }

  fun saveEditedSpriteAsset(
    name: String,
    seedDexNumber: Int,
    primaryHex: String,
    secondaryHex: String,
    paletteSummary: String,
    frameCount: Int,
  ) {
    viewModelScope.launch {
      _isSavingProject.value = true
      val cfg = _fusionConfig.value
      val asset = CustomStudioAsset(
        name = name.trim().ifBlank { "${_selectedPokemon.value.name} Sprite Rig" },
        category = AssetCategory.SPRITES.label,
        consoleLineage = _selectedPokemon.value.consoleOriginSystem,
        chipAddon = cfg.activeChipIds.take(2).joinToString("+").ifEmpty { "PPU Palette" },
        spriteSeedId = seedDexNumber.toLong(),
        shaderFormula = "SPRITE_EDITED|FRAMES:$frameCount|PAL:$primaryHex,$secondaryHex|$paletteSummary",
      )
      cloudRepository.createCustomAsset(asset)
        .onSuccess {
          _isSavingProject.value = false
          _selectedSidebarCategory.value = AssetCategory.SPRITES
          _statusBannerMessage.value = "Saved '${asset.name}' ($frameCount frames) to Cloud & Sprites Drawer!"
        }
        .onFailure { e ->
          _isSavingProject.value = false
          _statusBannerMessage.value = "Could not save edited sprite: ${e.localizedMessage}"
        }
    }
  }

  fun applyEditedPaletteToActivePokemon(primaryHex: String, secondaryHex: String) {
    viewModelScope.launch {
      val updated = _selectedPokemon.value.copy(
        primaryHex = primaryHex,
        secondaryHex = secondaryHex,
        isUserModified = true,
      )
      localDexRepository.upsertLocalPokemon(updated)
      _selectedPokemon.value = updated
      if (_activeProjectId.value != null) {
        _hasUnsavedChanges.value = true
      }
      _statusBannerMessage.value = "Applied PPU palette ($primaryHex / $secondaryHex) to #${updated.dexNumber} ${updated.name}!"
    }
  }

  fun deleteCustomSidebarAsset(assetId: String) {
    viewModelScope.launch {
      cloudRepository.deleteCustomAsset(assetId)
    }
  }

  companion object {
    val builtInSidebarAssets = listOf(
      BuiltInAssetItem(
        id = "spr_rayquaza_omni",
        name = "Rayquaza Sky-Pillar Omni-Sprite",
        category = AssetCategory.SPRITES,
        consoleLineage = "GBA Emerald + Gen V Skeletal + Switch Bloom",
        chipAddon = "Super FX2 + Voxelizer-FX",
        spriteSeedId = 384,
        shaderFormula = "BLEND:65|VOXEL:75|CRT:25",
      ),
      BuiltInAssetItem(
        id = "spr_charizard_sgb",
        name = "Charizard SGB2 + Gen V Wing Rig",
        category = AssetCategory.SPRITES,
        consoleLineage = "DMG/SGB2 + NDS B2W2 + HD-2D Point Light",
        chipAddon = "Super Game Boy 2 + PokeCRT-RTX",
        spriteSeedId = 6,
        shaderFormula = "BLEND:55|VOXEL:68|CRT:35",
      ),
      BuiltInAssetItem(
        id = "spr_pikachu_yellow",
        name = "Pikachu Yellow PPU + Cel-Outline",
        category = AssetCategory.SPRITES,
        consoleLineage = "GBC Hi-Color + 3DS PICA200 Toon",
        chipAddon = "MSU-1 PCM + SA-1",
        spriteSeedId = 25,
        shaderFormula = "BLEND:50|VOXEL:60|CRT:30",
      ),
      BuiltInAssetItem(
        id = "spr_gengar_shadow",
        name = "Shadow Gengar XD001 Chromatic Sprite",
        category = AssetCategory.SPRITES,
        consoleLineage = "GBC Crystal Dither + GameCube Orre TEV",
        chipAddon = "GBA e-Reader + Dolphin TEV",
        spriteSeedId = 94,
        shaderFormula = "BLEND:70|VOXEL:80|CRT:40",
      ),
      BuiltInAssetItem(
        id = "mod_mewtwo_cx4",
        name = "Mewtwo Stadium + CX4 Wireframe Mesh",
        category = AssetCategory.MODELS_3D,
        consoleLineage = "N64 Stadium + Capcom CX4 + Pokkén PBR",
        chipAddon = "Capcom CX4 + N64 8MB Expansion Pak",
        spriteSeedId = 150,
        shaderFormula = "BLEND:88|VOXEL:90|CRT:15",
      ),
      BuiltInAssetItem(
        id = "mod_lugia_xd",
        name = "XD001 Shadow Lugia Volumetric Rig",
        category = AssetCategory.MODELS_3D,
        consoleLineage = "GameCube Flipper TEV + NDS SoulSilver 2.5D",
        chipAddon = "Sega 32X + GameCube TEV",
        spriteSeedId = 249,
        shaderFormula = "BLEND:82|VOXEL:85|CRT:20",
      ),
      BuiltInAssetItem(
        id = "mod_miraidon_matrix",
        name = "Miraidon Hadron Voxel-Polygon Core",
        category = AssetCategory.MODELS_3D,
        consoleLineage = "Switch PBR + 8-Bit LED Dot-Matrix Visor",
        chipAddon = "Super FX2 + PokeCRT-RTX",
        spriteSeedId = 1008,
        shaderFormula = "BLEND:85|VOXEL:92|CRT:28",
      ),
      BuiltInAssetItem(
        id = "tex_gbc_dither_pbr",
        name = "GBC Ordered Dither + PBR Normal Atlas",
        category = AssetCategory.TEXTURES,
        consoleLineage = "GBC Crystal 4-Tone + Switch Metallic Roughness",
        chipAddon = "Super Game Boy 2 + Voxelizer-FX",
        spriteSeedId = 245,
        shaderFormula = "TEX:512PX|DITHER:BAYER4|NORMAL:ON",
      ),
      BuiltInAssetItem(
        id = "tex_orre_tev_specular",
        name = "Orre Flipper TEV Shadow Aura Texture",
        category = AssetCategory.TEXTURES,
        consoleLineage = "GameCube XD TEV + 3DS PICA200 Rim Map",
        chipAddon = "GBA e-Reader + Sega 32X",
        spriteSeedId = 249,
        shaderFormula = "TEX:1024PX|TEV_STAGE:4|RIM_SPEC:HIGH",
      ),
      BuiltInAssetItem(
        id = "tex_mode7_stone_tile",
        name = "Mode-7 Ancient Ruins Albedo & Height Map",
        category = AssetCategory.TEXTURES,
        consoleLineage = "SNES Mode-7 + GBA Tile + HD-2D Parallax",
        chipAddon = "Nintendo SA-1 + PokeCRT-RTX",
        spriteSeedId = 384,
        shaderFormula = "TEX:256PX|PARALLAX:0.75|CRT:ON",
      ),
      BuiltInAssetItem(
        id = "tile_celadon_hd2d",
        name = "Celadon Rooftop HD-2D Octo-Tileset",
        category = AssetCategory.TILESETS,
        consoleLineage = "GBC + GBA FR/LG + NDS 3D + Switch Bokeh",
        chipAddon = "SA-1 + PokeCRT-RTX",
        spriteSeedId = 133,
        shaderFormula = "MODE7:48|BLOOM:88|CRT:22",
      ),
      BuiltInAssetItem(
        id = "tile_sky_pillar_mode7",
        name = "Sky Pillar Affine Mode-7 Cloud Matrix",
        category = AssetCategory.TILESETS,
        consoleLineage = "SNES Mode-7 + GBA Affine + 3DS Horizon",
        chipAddon = "Super FX2 (GSU-2)",
        spriteSeedId = 384,
        shaderFormula = "MODE7:85|BLOOM:75|CRT:30",
      ),
      BuiltInAssetItem(
        id = "chip_super_fx3_fan",
        name = "Super FX2 + Voxelizer-FX Hybrid Pipeline",
        category = AssetCategory.CHIPS_SHADERS,
        consoleLineage = "SNES 21MHz GSU-2 + Fan FPGA Voxel Extruder",
        chipAddon = "Super FX2 + Voxelizer-FX",
        spriteSeedId = 448,
        shaderFormula = "BLEND:74|VOXEL:88|CRT:24",
      ),
      BuiltInAssetItem(
        id = "chip_32x_tev_bridge",
        name = "Sega 32X SH-2 + GameCube TEV Shader",
        category = AssetCategory.CHIPS_SHADERS,
        consoleLineage = "32X Gouraud Polygons + Dolphin TEV Rim Glow",
        chipAddon = "Sega 32X + GBA e-Reader",
        spriteSeedId = 887,
        shaderFormula = "BLEND:80|VOXEL:78|CRT:32",
      ),
      BuiltInAssetItem(
        id = "map_ecuteak_diorama",
        name = "Ecruteak Bell Tower 2.5D Voxel Diorama",
        category = AssetCategory.MAPS,
        consoleLineage = "GBC Gold + NDS HGSS 3D + Switch Autumn Raytrace",
        chipAddon = "Voxelizer-FX + PokeCRT-RTX",
        spriteSeedId = 249,
        shaderFormula = "MODE7:55|BLOOM:92|VOXEL:84",
      ),
      BuiltInAssetItem(
        id = "map_unova_bridge",
        name = "Skyarrow Bridge Multi-Camera Spline Map",
        category = AssetCategory.MAPS,
        consoleLineage = "NDS B/W 3D Camera + PS1/Saturn Sprite Scaling",
        chipAddon = "SA-1 + Super FX2",
        spriteSeedId = 643,
        shaderFormula = "MODE7:76|BLOOM:80|VOXEL:70",
      ),
      BuiltInAssetItem(
        id = "aud_msu1_red_battle",
        name = "Mt. Silver Final Battle (DMG +YM2612 + MSU-1)",
        category = AssetCategory.AUDIO_FX,
        consoleLineage = "GB 4-Ch Wave + Genesis FM + MSU-1 44.1kHz PCM",
        chipAddon = "MSU-1 Media Chip",
        spriteSeedId = 6,
        shaderFormula = "PCM:44100|CH1_PULSE:ON|FM_SYNTH:ON",
      ),
      BuiltInAssetItem(
        id = "aud_hoenn_brass_hd",
        name = "Littleroot & Weather Trio Brass Synthesis",
        category = AssetCategory.AUDIO_FX,
        consoleLineage = "GBA DirectSound + SNES SPC700 + 3DS Live Stem",
        chipAddon = "MSU-1 + SA-1",
        spriteSeedId = 384,
        shaderFormula = "PCM:44100|SPC700_ECHO:65|BRASS_BOOST:ON",
      ),
    )
  }
}

package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.data.local.AtlasProgressEntity
import com.example.data.local.AtlasSpeciesIndexItem
import com.example.data.local.CampaignStateEntity
import com.example.data.local.EncyclopediaEntryEntity
import com.example.data.local.EncyclopediaSeedCatalog
import com.example.data.local.EncyclopediaSourceStatusEntity
import com.example.data.local.FranchiseEncyclopediaAndResourceRepository
import com.example.data.local.NativeFranchiseCatalogRepository
import com.example.data.local.NativePokemonEntity
import com.example.data.local.ResourceCategory
import com.example.data.local.SavedMapEntity
import com.example.data.local.SavedSpriteEntity
import com.example.data.local.StudioItemCatalog
import com.example.data.local.StudioItemEntity
import com.example.data.local.StudioPersistenceDao
import com.example.data.local.StudioResourceEntity
import com.example.data.model.AssetCategory
import com.example.data.model.CharacterMixMode
import com.example.data.model.ConsoleFusionConfig
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.ExtraVisualAudioSettings
import com.example.data.model.FanGameProject
import com.example.data.model.MasterDisciplineMode
import com.example.data.repository.LocalStudioProjectRepository
import com.example.engine.AudioConvergenceSolver
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTurnField
import com.example.engine.BgmThemeId
import com.example.engine.CampaignProgressionState
import com.example.engine.CanonBattleResolver
import com.example.engine.CanonRegionChapter
import com.example.engine.HardwareConvergenceSolver
import com.example.engine.HardwarePlatformSpec
import com.example.engine.HardwareRuntimePreset
import com.example.engine.HardwareRuntimePresetCatalog
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.MoveDirection
import com.example.engine.OmniHardwareAudioEngine
import com.example.engine.PoiInteractionKind
import com.example.engine.PokemonType
import com.example.engine.RegionPointOfInterest
import com.example.engine.SpritePixelMatrixEngine
import com.example.engine.StatStages
import com.example.engine.StatusCondition
import com.example.engine.StudioSfxType
import com.example.engine.TerrainCondition
import com.example.engine.UniversalGameFeelEngine
import com.example.engine.UniversalHardwareRegistry
import com.example.engine.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

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
  CLOUD_GAMES("cloud_games", "Local Vault Projects"),
  FRANCHISE_DEX("franchise_dex", "Resonance Atlas Dex"),
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
  val projectId: String = "",
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
  val lastModifiedMs: Long?,
  val createdAtMs: Long?,
  val globalSharedAssetsCount: Int = 0,
)

data class StudioSyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncedAtMs: Long? = null,
  val statusLabel: String = "Pre-warming Offline Studio...",
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
  private val appContext: Context,
  private val localProjectRepository: LocalStudioProjectRepository,
  private val localDexRepository: NativeFranchiseCatalogRepository,
  private val encyclopediaAndResourceRepository: FranchiseEncyclopediaAndResourceRepository,
  private val persistenceDao: StudioPersistenceDao,
  private val audioEngine: OmniHardwareAudioEngine = OmniHardwareAudioEngine(),
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

  private val _isSyncingSources = MutableStateFlow(false)
  val isSyncingSources: StateFlow<Boolean> = _isSyncingSources.asStateFlow()

  // Hoisted Hardware Registry & Convergence state (all 156 platforms)
  private val _hardwareRegistryState = MutableStateFlow(HardwareRegistryUiState())
  val hardwareRegistryState: StateFlow<HardwareRegistryUiState> = _hardwareRegistryState.asStateFlow()

  // Hoisted Audio Console state
  private val _audioConsoleState = MutableStateFlow(AudioConsoleUiState())
  val audioConsoleState: StateFlow<AudioConsoleUiState> = _audioConsoleState.asStateFlow()

  // Hoisted Sprite Editor state (survives tab switches & persists to Room saved_sprites)
  private val _spriteEditorState = MutableStateFlow(
    SpriteEditorUiState().let { initial ->
      initial.copy(
        renderedPreviewBitmap = SpritePixelMatrixEngine.renderFrameToBitmap(
          initial.currentFrame,
          initial.paletteArgb,
          scale = 4,
        )
      )
    }
  )
  val spriteEditorState: StateFlow<SpriteEditorUiState> = _spriteEditorState.asStateFlow()

  // Hoisted 16x16 Map Editor state (per project + per 14 regions, persisted to Room saved_maps)
  private val _activeMapRegionId = MutableStateFlow(MasterCanonTemplateCatalog.worldChapters.first().id)
  val activeMapRegionId: StateFlow<String> = _activeMapRegionId.asStateFlow()

  private val _mapGrid = MutableStateFlow(MasterCanonTemplateCatalog.worldChapters.first().defaultMapTiles)
  val mapGrid: StateFlow<List<Int>> = _mapGrid.asStateFlow()

  private val _selectedBrushTile = MutableStateFlow(1) // 1 = Tall Wild Grass
  val selectedBrushTile: StateFlow<Int> = _selectedBrushTile.asStateFlow()

  // Hoisted Game Test & Campaign state (survives tab switches & persists to Room campaign_state)
  private val _gameTestState = MutableStateFlow(GameTestUiState())
  val gameTestState: StateFlow<GameTestUiState> = _gameTestState.asStateFlow()

  private val _campaignState = MutableStateFlow(CampaignProgressionState())
  val campaignState: StateFlow<CampaignProgressionState> = _campaignState.asStateFlow()

  private var hasAutoLoadedInitialProject = false
  private var battleActionCounter = 0
  private var hardwareTourJob: Job? = null
  private val pendingHydrationDexIds: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

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

  val savedSprites: StateFlow<List<SavedSpriteEntity>> =
    persistenceDao.observeAllSavedSprites().stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList(),
    )

  val studioItems: StateFlow<List<StudioItemEntity>> =
    persistenceDao.observeAllItems().stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = StudioItemCatalog.defaultItems,
    )

  val atlasProgressMap: StateFlow<Map<Int, AtlasProgressEntity>> =
    persistenceDao.observeAtlasProgress()
      .map { list -> list.associateBy { it.dexNumber } }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyMap(),
      )

  // 100% Offline Room v4 Single-Source-of-Truth Project Stream
  val cloudFanGamesState: StateFlow<UiState<List<FanGameProject>>> =
    localProjectRepository.observeProjects()
      .map<List<FanGameProject>, UiState<List<FanGameProject>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.message ?: "Failed to load local studio projects"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val activeProject: StateFlow<FanGameProject?> =
    combine(cloudFanGamesState, _activeProjectId) { gamesState, activeId ->
      val projects = (gamesState as? UiState.Success)?.data ?: return@combine null
      if (activeId != null) projects.firstOrNull { it.id == activeId } ?: projects.firstOrNull()
      else projects.firstOrNull()
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  val customDexState: StateFlow<UiState<List<CustomDexEntry>>> =
    _activeProjectId.flatMapLatest { projId ->
      localProjectRepository.observeCustomDex(projId ?: LocalStudioProjectRepository.DEFAULT_MASTER_PROJECT_ID)
    }
      .map<List<CustomDexEntry>, UiState<List<CustomDexEntry>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.message ?: "Failed to load custom Dex entries"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Success(emptyList()),
      )

  val customAssetsState: StateFlow<UiState<List<CustomStudioAsset>>> =
    _activeProjectId.flatMapLatest { projId ->
      localProjectRepository.observeStudioAssets(projId ?: LocalStudioProjectRepository.DEFAULT_MASTER_PROJECT_ID)
    }
      .map<List<CustomStudioAsset>, UiState<List<CustomStudioAsset>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.message ?: "Failed to load custom studio assets"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Success(emptyList()),
      )

  val projectOverviewState: StateFlow<UiState<ProjectOverviewStats?>> =
    combine(
      activeProject,
      customAssetsState,
      customDexState,
      _hardwareRegistryState,
    ) { liveProject, assetsState, dexState, hwState ->
      val overview: ProjectOverviewStats? = if (liveProject == null) {
        null
      } else {
        val allAssets = (assetsState as? UiState.Success)?.data ?: emptyList()
        val allDex = (dexState as? UiState.Success)?.data ?: emptyList()
        val projectAssets = allAssets.filter { it.projectId == liveProject.id }
        val globalAssets = allAssets.filter { it.projectId.isBlank() }
        val projectDex = allDex.filter { it.projectId == liveProject.id || it.projectId.isBlank() }
        val catalogIds = hwState.allPlatforms.map { it.id }.toSet()
        val (_, decodedIds) = UniversalHardwareRegistry.decodeSelectionToken(liveProject.activeChipsCsv, catalogIds)
        val totalAssetCount = projectAssets.size + projectDex.size + decodedIds.size

        val candidateMs = buildList {
          add(liveProject.updatedAtMs)
          add(liveProject.createdAtMs)
          projectAssets.maxOfOrNull { maxOf(it.updatedAtMs, it.createdAtMs) }?.let { add(it) }
          projectDex.maxOfOrNull { maxOf(it.updatedAtMs, it.createdAtMs) }?.let { add(it) }
        }
        val latestModifiedMs = candidateMs.maxOrNull()

        ProjectOverviewStats(
          projectId = liveProject.id,
          projectTitle = liveProject.title.ifBlank { "Untitled Fan Game" },
          versionTag = liveProject.versionTag,
          regionName = liveProject.regionName,
          consoleMatrixPreset = liveProject.consoleMatrixPreset,
          assetCount = totalAssetCount,
          pinnedCloudAssetsCount = projectAssets.size,
          customDexCount = projectDex.size,
          activeChipsCount = decodedIds.size,
          dexTargetCount = liveProject.dexCount,
          mapCount = liveProject.mapCount,
          blendRatioPercent = liveProject.pixel3dBlendRatio,
          lastModifiedMs = latestModifiedMs,
          createdAtMs = liveProject.createdAtMs,
          globalSharedAssetsCount = globalAssets.size,
        )
      }
      UiState.Success(overview) as UiState<ProjectOverviewStats?>
    }.catch { error ->
      emit(UiState.Error(error.message ?: "Failed to load local project statistics"))
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  init {
    prewarmStudioStartup()
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
  }

  /**
   * Zero-Wait Offline Startup Pre-warming:
   * 1. Seeds default "Pokémon: Prism Resonance" master project in Room v4.
   * 2. Loads all 156 Hardware Platforms into Room & defaults to Ultimate Target Style (All 156 Hardware — Shovel Knight+ Curated).
   * 3. Seeds Items Catalog & 14 custom 16x16 Region Maps.
   * 4. Restores saved Campaign State (levels, taught TMs, position, shards).
   * 5. Pre-warms Coil singleton cache for starter party sprites.
   */
  private fun prewarmStudioStartup() {
    viewModelScope.launch(Dispatchers.IO) {
      // 1. Ensure default master project exists in Room v4
      val defaultProject = localProjectRepository.ensureDefaultMasterProjectSeeded()
      val projectKey = _activeProjectId.value?.takeIf { it.isNotBlank() } ?: defaultProject.id
      if (_activeProjectId.value == null) {
        _activeProjectId.value = projectKey
        hasAutoLoadedInitialProject = true
      }

      // 2. Load & persist all 156 hardware profiles
      val specs = UniversalHardwareRegistry.loadFromAssets(appContext)
      if (persistenceDao.countHardwareProfiles() < specs.size) {
        persistenceDao.upsertHardwareProfiles(specs.map { StudioViewModelHelpers.toHardwareProfileEntity(it) })
      }
      val allIds = specs.map { it.id }.toSet().ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds }
      val ultimateLabel = "Ultimate Target Style — All ${allIds.size} Hardware (Shovel Knight+ Curated)"

      withContext(Dispatchers.Main) {
        recomputeHardwareConvergence(
          allPlatforms = specs,
          activeIds = allIds,
          disciplineMode = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED,
          runtimePresetId = HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED,
          runtimeLabel = ultimateLabel,
        )
      }

      // 3. Seed items catalog if empty
      if (persistenceDao.countItems() == 0) {
        persistenceDao.upsertItems(StudioItemCatalog.defaultItems)
      }

      // 4. Restore campaign state & active 16x16 regional map
      val savedCampaign = persistenceDao.getCampaignState(projectKey)
      if (savedCampaign != null) {
        withContext(Dispatchers.Main) {
          applyCampaignEntityToState(savedCampaign, specs)
        }
      } else {
        persistenceDao.upsertCampaignState(
          CampaignStateEntity(
            projectId = projectKey,
            playerX = MasterCanonTemplateCatalog.worldChapters.first().spawnCol,
            playerY = MasterCanonTemplateCatalog.worldChapters.first().spawnRow,
            activeHardwareIdsCsv = allIds.joinToString(","),
            disciplineModeToken = MasterDisciplineMode.SHOVEL_KNIGHT_CURATED.token,
          )
        )
      }

      // 5. Ensure all 14 custom 16x16 maps are saved (upgrading any legacy 8x8 maps seamlessly)
      val defaultMaps = MasterCanonTemplateCatalog.worldChapters.mapNotNull { ch ->
        val key = mapStorageKey(projectKey, ch.id)
        val existing = persistenceDao.getSavedMap(key)
        if (existing == null || existing.gridSize != MasterCanonTemplateCatalog.MAP_GRID_SIZE) {
          SavedMapEntity(
            mapKey = key,
            projectId = projectKey,
            regionId = ch.id,
            regionName = ch.regionName,
            gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE,
            tilesCsv = ch.defaultMapTiles.joinToString(","),
          )
        } else {
          null
        }
      }
      if (defaultMaps.isNotEmpty()) {
        persistenceDao.upsertSavedMaps(defaultMaps)
      }
      loadMapForRegion(projectKey, _activeMapRegionId.value)

      // 6. Warm Coil singleton cache for core party & starter sprites
      val imageLoader = appContext.imageLoader
      val warmDexIds = listOf(1, 4, 7, 25, 6, 94, 384, 448, 658)
      warmDexIds.forEach { dexId ->
        val url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$dexId.png"
        imageLoader.enqueue(
          ImageRequest.Builder(appContext)
            .data(url)
            .build()
        )
      }
    }
  }

  private fun activePersistenceProjectKey(): String =
    _activeProjectId.value?.takeIf { it.isNotBlank() } ?: LocalStudioProjectRepository.DEFAULT_MASTER_PROJECT_ID

  private fun mapStorageKey(projectKey: String, regionId: String): String = "$projectKey:$regionId"

  private suspend fun loadMapForRegion(projectKey: String, regionId: String) {
    val saved = persistenceDao.getSavedMap(mapStorageKey(projectKey, regionId))
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val tiles = saved
      ?.takeIf { it.gridSize == MasterCanonTemplateCatalog.MAP_GRID_SIZE }
      ?.tilesCsv
      ?.split(",")
      ?.mapNotNull { it.trim().toIntOrNull() }
      ?.takeIf { it.size == MasterCanonTemplateCatalog.MAP_TILE_COUNT }
      ?: chapter.defaultMapTiles
    _mapGrid.value = tiles
  }

  private fun applyCampaignEntityToState(
    entity: CampaignStateEntity,
    allSpecs: List<HardwarePlatformSpec>,
  ) {
    val completed = entity.completedRegionIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val party = entity.partyDexIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(25, 6, 94, 448, 658, 384) }
    val heldMap = StudioViewModelHelpers.parseHeldItemsCsv(entity.heldItemsMapCsv)
    val levelsMap = StudioViewModelHelpers.parsePartyLevelsCsv(entity.partyLevelsCsv)
    val taughtMap = StudioViewModelHelpers.parseTaughtMovesCsv(entity.taughtMovesCsv)
    val badges = MasterCanonTemplateCatalog.worldChapters.filter { it.id in completed }.sumOf { it.gymOrTrialCount }
    _campaignState.value = CampaignProgressionState(
      completedChapterIds = completed,
      badgesEarned = badges.coerceAtMost(112),
      animeRibbonsAndKeys = (completed.size * 2).coerceAtMost(42),
      worldCoronationRank = (10000 - completed.size * 740).coerceAtLeast(1),
      harmonicShardsCollected = completed.size,
      partyDexIds = party,
    )
    val regionId = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == entity.activeRegionId }?.id ?: "kanto"
    val chapter = MasterCanonTemplateCatalog.worldChapters.first { it.id == regionId }
    _activeMapRegionId.value = regionId
    val maxCoord = MasterCanonTemplateCatalog.MAP_GRID_SIZE - 1
    val safeCol = entity.playerX.coerceIn(0, maxCoord)
    val safeRow = entity.playerY.coerceIn(0, maxCoord)
    _gameTestState.update {
      it.copy(
        activeRegionId = regionId,
        playerCol = safeCol,
        playerRow = safeRow,
        activePartyDexIds = party,
        heldItemsMap = if (heldMap.isNotEmpty()) heldMap else it.heldItemsMap,
        partyLevelMap = if (levelsMap.isNotEmpty()) levelsMap else it.partyLevelMap,
        taughtMovesMap = taughtMap,
        weather = chapter.weatherPreset,
      )
    }

    val discipline = MasterDisciplineMode.entries.firstOrNull { it.token == entity.disciplineModeToken }
      ?: MasterDisciplineMode.SHOVEL_KNIGHT_CURATED
    val extraPretty = StudioViewModelHelpers.decodeExtraPrettyMask(entity.extraPrettyMask)
    val allIds = allSpecs.map { it.id }.toSet().ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds }
    val hwIds = entity.activeHardwareIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
      .ifEmpty { allIds }
    val presetLabel = deriveRuntimeLabel(hwIds, allSpecs, discipline)
    val presetId = deriveRuntimePresetId(hwIds, allSpecs, discipline)
    recomputeHardwareConvergence(
      allPlatforms = allSpecs,
      activeIds = hwIds,
      disciplineMode = discipline,
      extraPretty = extraPretty,
      runtimePresetId = presetId,
      runtimeLabel = presetLabel,
    )
  }

  private fun persistCampaignStateToRoom() {
    val projectKey = activePersistenceProjectKey()
    val gt = _gameTestState.value
    val camp = _campaignState.value
    val hw = _hardwareRegistryState.value
    val entity = CampaignStateEntity(
      projectId = projectKey,
      activeRegionId = gt.activeRegionId,
      playerX = gt.playerCol,
      playerY = gt.playerRow,
      partyDexIdsCsv = gt.activePartyDexIds.joinToString(","),
      heldItemsMapCsv = StudioViewModelHelpers.encodeHeldItemsCsv(gt.heldItemsMap),
      completedRegionIdsCsv = camp.completedChapterIds.joinToString(","),
      harmonicShardsCsv = camp.completedChapterIds.joinToString(","),
      activeHardwareIdsCsv = hw.activePlatformIds.joinToString(","),
      disciplineModeToken = hw.disciplineMode.token,
      extraPrettyMask = StudioViewModelHelpers.encodeExtraPrettyMask(hw.extraPretty),
      updatedAtEpochMs = System.currentTimeMillis(),
      partyLevelsCsv = StudioViewModelHelpers.encodePartyLevelsCsv(gt.partyLevelMap),
      taughtMovesCsv = StudioViewModelHelpers.encodeTaughtMovesCsv(gt.taughtMovesMap),
    )
    viewModelScope.launch(Dispatchers.IO) {
      persistenceDao.upsertCampaignState(entity)
    }
  }

  /**
   * Lifecycle hook: cleanly stops active BGM audio track on ON_STOP so sound never leaks in background.
   */
  fun onAppStopped() {
    hardwareTourJob?.cancel()
    hardwareTourJob = null
    _hardwareRegistryState.update { it.copy(isHardwareTourActive = false, hardwareTourStepLabel = "") }
    audioEngine.stopBgmImmediate()
    _audioConsoleState.update { it.copy(isBgmPlaying = false) }
  }

  override fun onCleared() {
    super.onCleared()
    hardwareTourJob?.cancel()
    hardwareTourJob = null
    audioEngine.releaseAll()
  }

  fun refreshStudioNow() {
    syncStudioDataOnLoad(announceCompletion = true)
  }

  private fun syncStudioDataOnLoad(announceCompletion: Boolean) {
    viewModelScope.launch {
      _syncStatus.update { it.copy(isSyncing = true, statusLabel = "Refreshing local & PokéAPI data...") }
      _isSyncingSources.value = true

      val localDeferred = async { runCatching { localDexRepository.ensureSeeded() }.getOrDefault(0) }
      val encyclopediaDeferred = async {
        runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      }
      val primaryRefreshDeferred = async {
        localDexRepository.refreshLivePokemonIfOnline(_selectedPokemon.value.dexNumber)
      }

      val updatedLocalCount = localDeferred.await()
      val webSourcesSynced = encyclopediaDeferred.await()
      val apiResult = primaryRefreshDeferred.await()
      _isSyncingSources.value = false

      val finishedAt = System.currentTimeMillis()
      val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(finishedAt))
      val statusText = when {
        webSourcesSynced > 0 -> "Offline Vault + $webSourcesSynced/3 DBs • $timeStr"
        apiResult.isSuccess -> "Offline Vault + PokéAPI • $timeStr"
        else -> "100% Offline Room Ready • $timeStr"
      }

      _syncStatus.value = StudioSyncStatus(
        isSyncing = false,
        lastSyncedAtMs = finishedAt,
        statusLabel = statusText,
      )

      if (announceCompletion) {
        _statusBannerMessage.value =
          "Refreshed ($timeStr): $webSourcesSynced/3 encyclopedia sources & $updatedLocalCount local Dex entries ready."
      }
    }
  }

  fun syncEncyclopediaSourcesNow() {
    viewModelScope.launch {
      _isSyncingSources.value = true
      val count = runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      _isSyncingSources.value = false
      _statusBannerMessage.value = "Updated $count/3 canonical databases into local Room cache."
    }
  }

  // =========================================================================
  // UNIVERSAL HARDWARE REGISTRY + GAME TEST RUNTIME DROPDOWN + HARDWARE TOUR
  // =========================================================================

  private fun deriveRuntimeLabel(
    activeIds: Set<String>,
    allPlatforms: List<HardwarePlatformSpec>,
    disciplineMode: MasterDisciplineMode,
  ): String {
    val total = allPlatforms.size.coerceAtLeast(activeIds.size)
    return when {
      total > 0 && activeIds.size >= total && disciplineMode == MasterDisciplineMode.SHOVEL_KNIGHT_CURATED ->
        "Ultimate Target Style — All $total Hardware (Shovel Knight+ Curated)"
      total > 0 && activeIds.size >= total && disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY ->
        "Ultimate Target Style — All $total Hardware (Maximalist Pretty)"
      activeIds.size == 1 -> {
        val spec = allPlatforms.firstOrNull { it.id == activeIds.first() }
        "${spec?.name ?: activeIds.first()} (${spec?.year ?: ""}) — ${disciplineMode.label}"
      }
      else -> "Custom Combo (${activeIds.size} Hardware) • ${disciplineMode.label}"
    }
  }

  private fun deriveRuntimePresetId(
    activeIds: Set<String>,
    allPlatforms: List<HardwarePlatformSpec>,
    disciplineMode: MasterDisciplineMode,
  ): String {
    val total = allPlatforms.size
    return when {
      total > 0 && activeIds.size >= total && disciplineMode == MasterDisciplineMode.SHOVEL_KNIGHT_CURATED ->
        HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED
      total > 0 && activeIds.size >= total && disciplineMode == MasterDisciplineMode.MAXIMALIST_PRETTY ->
        HardwareRuntimePresetCatalog.MODE_ULTIMATE_MAXIMALIST
      activeIds.size == 1 -> "single_${activeIds.first()}"
      else -> "custom_combo"
    }
  }

  private fun cancelHardwareTourIfRunning() {
    if (hardwareTourJob?.isActive == true) {
      hardwareTourJob?.cancel()
      hardwareTourJob = null
      _hardwareRegistryState.update { it.copy(isHardwareTourActive = false, hardwareTourStepLabel = "") }
    }
  }

  private fun recomputeHardwareConvergence(
    allPlatforms: List<HardwarePlatformSpec> = _hardwareRegistryState.value.allPlatforms,
    activeIds: Set<String> = _hardwareRegistryState.value.activePlatformIds,
    disciplineMode: MasterDisciplineMode = _hardwareRegistryState.value.disciplineMode,
    characterMixMode: CharacterMixMode = _hardwareRegistryState.value.characterMixMode,
    extraPretty: ExtraVisualAudioSettings = _hardwareRegistryState.value.extraPretty,
    runtimePresetId: String = _hardwareRegistryState.value.activeRuntimePresetId,
    runtimeLabel: String = _hardwareRegistryState.value.activeRuntimeLabel,
    isTourActive: Boolean = _hardwareRegistryState.value.isHardwareTourActive,
    tourStepLabel: String = _hardwareRegistryState.value.hardwareTourStepLabel,
  ) {
    val cfg = _fusionConfig.value
    val activeSpecs = allPlatforms.filter { it.id in activeIds }
    val visuals = HardwareConvergenceSolver.solveVisuals(
      activePlatforms = activeSpecs,
      disciplineMode = disciplineMode,
      extraPretty = extraPretty,
      userSliderOutline = 0.72f,
      userSliderBloom = cfg.hd2dVolumetricBloom,
      userSliderScanline = cfg.crtScanlineIntensity,
      userSliderVoxel = cfg.voxelExtrusionDepth,
      userSliderAffine = cfg.mode7PerspectiveTilt,
    )
    val audio = AudioConvergenceSolver.solveAudio(
      activePlatforms = activeSpecs,
      disciplineMode = disciplineMode,
      extraPretty = extraPretty,
    )
    val allEnabled = allPlatforms.isNotEmpty() && activeIds.size >= allPlatforms.size

    _hardwareRegistryState.value = HardwareRegistryUiState(
      allPlatforms = allPlatforms,
      activePlatformIds = activeIds,
      allHardwareEnabled = allEnabled,
      disciplineMode = disciplineMode,
      characterMixMode = characterMixMode,
      extraPretty = extraPretty,
      activeRuntimePresetId = runtimePresetId,
      activeRuntimeLabel = runtimeLabel,
      isHardwareTourActive = isTourActive,
      hardwareTourStepLabel = tourStepLabel,
      convergedVisuals = visuals,
      convergedAudio = audio,
    )
    audioEngine.updateAudioParams(audio)
  }

  fun toggleHardwarePlatform(platformId: String) {
    cancelHardwareTourIfRunning()
    val current = _hardwareRegistryState.value
    val updated = if (platformId in current.activePlatformIds) {
      if (current.activePlatformIds.size > 1) current.activePlatformIds - platformId else current.activePlatformIds
    } else {
      current.activePlatformIds + platformId
    }
    recomputeHardwareConvergence(
      activeIds = updated,
      runtimePresetId = deriveRuntimePresetId(updated, current.allPlatforms, current.disciplineMode),
      runtimeLabel = deriveRuntimeLabel(updated, current.allPlatforms, current.disciplineMode),
    )
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
  }

  fun toggleAllHardwareOnRecord(enableAll: Boolean) {
    cancelHardwareTourIfRunning()
    val current = _hardwareRegistryState.value
    val targetIds = if (enableAll) {
      current.allPlatforms.map { it.id }.toSet().ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds }
    } else {
      UniversalHardwareRegistry.defaultCuratedHardwareIds
    }
    recomputeHardwareConvergence(
      activeIds = targetIds,
      runtimePresetId = deriveRuntimePresetId(targetIds, current.allPlatforms, current.disciplineMode),
      runtimeLabel = deriveRuntimeLabel(targetIds, current.allPlatforms, current.disciplineMode),
    )
    audioEngine.playSfx(if (enableAll) StudioSfxType.LEVEL_UP else StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
    _statusBannerMessage.value = if (enableAll) {
      "Enabled ALL ${targetIds.size} Gaming Hardware Platforms on Record via Peak-Preserving Convergence!"
    } else {
      "Switched to curated ${targetIds.size}-platform hardware showcase stack."
    }
  }

  fun setMasterDisciplineMode(mode: MasterDisciplineMode) {
    cancelHardwareTourIfRunning()
    val current = _hardwareRegistryState.value
    recomputeHardwareConvergence(
      disciplineMode = mode,
      runtimePresetId = deriveRuntimePresetId(current.activePlatformIds, current.allPlatforms, mode),
      runtimeLabel = deriveRuntimeLabel(current.activePlatformIds, current.allPlatforms, mode),
    )
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
  }

  /**
   * Game Test Hardware Runtime Mode selector:
   * Immediately updates AGSL shaders, stage VFX, live looping BGM synthesis, and all subsequent SFX/cries.
   */
  fun applyHardwareRuntimePreset(preset: HardwareRuntimePreset, fromTour: Boolean = false) {
    if (!fromTour) cancelHardwareTourIfRunning()
    val current = _hardwareRegistryState.value
    val nextExtra = if (preset.enableAllExtraPretty) {
      ExtraVisualAudioSettings(
        dynamicWeatherParticles = true,
        waterCausticsAndFoam = true,
        hd2dVolumetricRaysAndBokeh = true,
        ultraVoxelSlices = true,
        uncappedBloomAndRimFlares = true,
        analogCrtPhosphorAndCurvature = true,
        fluid60FpsCharacterMotion = true,
        foregroundParallaxFraming = true,
        extraLushAudioReverbAndChoir = true,
      )
    } else {
      current.extraPretty
    }
    recomputeHardwareConvergence(
      activeIds = preset.platformIds,
      disciplineMode = preset.disciplineMode,
      extraPretty = nextExtra,
      runtimePresetId = preset.id,
      runtimeLabel = preset.label,
      isTourActive = if (fromTour) current.isHardwareTourActive else false,
      tourStepLabel = if (fromTour) current.hardwareTourStepLabel else "",
    )
    audioEngine.playSfx(
      if (preset.id == HardwareRuntimePresetCatalog.MODE_ULTIMATE_CURATED ||
        preset.id == HardwareRuntimePresetCatalog.MODE_ULTIMATE_MAXIMALIST
      ) {
        StudioSfxType.LEVEL_UP
      } else {
        StudioSfxType.UI_CONFIRM
      }
    )
    if (!fromTour) {
      if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
      persistCampaignStateToRoom()
      _gameTestState.update {
        it.copy(
          transitionPulseNonce = it.transitionPulseNonce + 1L,
          battleLog = (listOf("Hardware Runtime -> ${preset.label}") + it.battleLog).take(16),
        )
      }
    }
  }

  /**
   * Inline Custom Hardware Combo Builder from Game Test dropdown.
   */
  fun applyCustomHardwareCombo(
    selectedIds: Set<String>,
    disciplineMode: MasterDisciplineMode,
  ) {
    cancelHardwareTourIfRunning()
    val current = _hardwareRegistryState.value
    val nonEmpty = selectedIds.ifEmpty { current.allPlatforms.map { it.id }.toSet() }
    val label = deriveRuntimeLabel(nonEmpty, current.allPlatforms, disciplineMode)
    recomputeHardwareConvergence(
      activeIds = nonEmpty,
      disciplineMode = disciplineMode,
      runtimePresetId = "custom_combo",
      runtimeLabel = label,
    )
    audioEngine.playSfx(StudioSfxType.ITEM_USE)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
    _gameTestState.update {
      it.copy(
        transitionPulseNonce = it.transitionPulseNonce + 1L,
        battleLog = (listOf("Applied Custom Combo: $label") + it.battleLog).take(16),
      )
    }
  }

  /**
   * 4-second per step live Hardware Tour:
   * Game Boy DMG -> NES -> SNES -> GBA -> PS1 -> GameCube -> 3DS -> Ultimate Target Style (All 156 Hardware).
   */
  fun toggleHardwareTour() {
    if (_hardwareRegistryState.value.isHardwareTourActive) {
      cancelHardwareTourIfRunning()
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      return
    }
    val specs = _hardwareRegistryState.value.allPlatforms
    val sequence = HardwareRuntimePresetCatalog.buildHardwareTourSequence(specs)
    if (sequence.isEmpty()) return

    hardwareTourJob?.cancel()
    hardwareTourJob = viewModelScope.launch {
      _hardwareRegistryState.update {
        it.copy(
          isHardwareTourActive = true,
          hardwareTourStepLabel = "Tour 1/${sequence.size}: ${sequence.first().label}",
        )
      }
      sequence.forEachIndexed { idx, preset ->
        _hardwareRegistryState.update {
          it.copy(
            isHardwareTourActive = true,
            hardwareTourStepLabel = "Tour ${idx + 1}/${sequence.size}: ${preset.label}",
          )
        }
        applyHardwareRuntimePreset(preset, fromTour = true)
        _gameTestState.update {
          it.copy(
            transitionPulseNonce = it.transitionPulseNonce + 1L,
            battleLog = (listOf("[Hardware Tour ${idx + 1}/${sequence.size}] ${preset.label}") + it.battleLog).take(16),
          )
        }
        if (idx < sequence.lastIndex) {
          delay(4000L)
        }
      }
      _hardwareRegistryState.update {
        it.copy(
          isHardwareTourActive = false,
          hardwareTourStepLabel = "",
        )
      }
      persistCampaignStateToRoom()
      _statusBannerMessage.value = "Hardware Tour converged onto Ultimate Target Style (All ${specs.size} Hardware)!"
    }
  }

  fun setCharacterMixMode(mode: CharacterMixMode) {
    recomputeHardwareConvergence(characterMixMode = mode)
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun updateExtraPrettySettings(transform: (ExtraVisualAudioSettings) -> ExtraVisualAudioSettings) {
    val updated = transform(_hardwareRegistryState.value.extraPretty)
    recomputeHardwareConvergence(extraPretty = updated)
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
    persistCampaignStateToRoom()
  }

  // =========================================================================
  // PURE-KOTLIN OMNI-HARDWARE AUDIO CONSOLE
  // =========================================================================

  fun toggleAudioMute() {
    val newMuted = !_audioConsoleState.value.isMuted
    audioEngine.setMuted(newMuted)
    _audioConsoleState.update { it.copy(isMuted = newMuted) }
  }

  fun setAudioMasterVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    audioEngine.setMasterVolume(clamped)
    _audioConsoleState.update { it.copy(masterVolume = clamped) }
  }

  fun playBgmTheme(theme: BgmThemeId) {
    audioEngine.playBgm(theme)
    _audioConsoleState.update { it.copy(isBgmPlaying = true, activeBgmTheme = theme) }
  }

  fun toggleBgmPlayback() {
    val current = _audioConsoleState.value
    if (current.isBgmPlaying) {
      stopBgm()
    } else {
      playBgmTheme(current.activeBgmTheme)
    }
  }

  fun stopBgm() {
    audioEngine.stopBgm()
    _audioConsoleState.update { it.copy(isBgmPlaying = false) }
  }

  fun playSfx(type: StudioSfxType) {
    audioEngine.playSfx(type)
  }

  fun playPokemonCry(pokemon: NativePokemonEntity) {
    audioEngine.playPokemonCry(
      dexNumber = pokemon.dexNumber,
      primaryType = pokemon.primaryType,
      secondaryType = pokemon.secondaryType,
      baseHp = pokemon.baseHp,
      baseAtk = pokemon.baseAttack,
      baseSpe = pokemon.baseSpeed,
    )
  }

  fun playAtlasSpeciesCry(item: AtlasSpeciesIndexItem) {
    audioEngine.playPokemonCry(
      dexNumber = item.dexNumber,
      primaryType = item.primaryType,
      secondaryType = item.secondaryType,
    )
  }

  // =========================================================================
  // HOISTED SPRITE EDITOR + LOCAL ROOM PERSISTENCE
  // =========================================================================

  private fun updateSpriteStateWithBitmap(transform: (SpriteEditorUiState) -> SpriteEditorUiState) {
    _spriteEditorState.update { prev ->
      val next = transform(prev)
      val safeIdx = next.activeFrameIndex.coerceIn(0, (next.frames.size - 1).coerceAtLeast(0))
      val fixed = next.copy(activeFrameIndex = safeIdx)
      fixed.copy(
        renderedPreviewBitmap = SpritePixelMatrixEngine.renderFrameToBitmap(
          pixels = fixed.currentFrame,
          paletteArgb = fixed.paletteArgb,
          scale = 4,
        )
      )
    }
  }

  fun selectSpriteTool(tool: SpriteToolMode) {
    _spriteEditorState.update { it.copy(activeTool = tool) }
  }

  fun selectSpriteColorSlot(slot: Int) {
    _spriteEditorState.update { it.copy(selectedColorSlot = slot.coerceIn(0, 7)) }
  }

  fun updateSpritePaletteSlot(slot: Int, argbLong: Long) {
    updateSpriteStateWithBitmap { s ->
      val updatedPal = s.paletteArgb.toMutableList()
      if (slot in updatedPal.indices) updatedPal[slot] = argbLong
      s.copy(paletteArgb = updatedPal)
    }
  }

  fun applySpritePresetPalette(presetName: String) {
    val preset = SpritePixelMatrixEngine.consolePresetPalettes[presetName] ?: return
    applySpritePresetPaletteValues(preset)
  }

  /**
   * Applies an 8-color ARGB palette directly in a single bitmap allocation.
   */
  fun applySpritePresetPaletteValues(palette: List<Long>) {
    if (palette.isEmpty()) return
    val normalized = List(8) { idx -> palette.getOrElse(idx) { 0xFF00F5D4L } }
    updateSpriteStateWithBitmap { it.copy(paletteArgb = normalized) }
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
  }

  fun updateSpriteName(name: String) {
    _spriteEditorState.update { it.copy(spriteName = name) }
  }

  fun toggleSpriteMirrorX() {
    _spriteEditorState.update { it.copy(mirrorX = !it.mirrorX) }
  }

  fun toggleSpriteShowGrid() {
    _spriteEditorState.update { it.copy(showGrid = !it.showGrid) }
  }

  fun toggleSpritePlayPreview() {
    _spriteEditorState.update { it.copy(isPlayingPreview = !it.isPlayingPreview) }
  }

  fun setSpritePreviewFps(fps: Int) {
    _spriteEditorState.update { it.copy(previewFps = fps.coerceIn(2, 15)) }
  }

  fun selectSpriteFrame(index: Int) {
    updateSpriteStateWithBitmap { it.copy(activeFrameIndex = index.coerceIn(0, (it.frames.size - 1).coerceAtLeast(0))) }
  }

  fun addSpriteFrame() {
    updateSpriteStateWithBitmap { s ->
      if (s.frames.size >= 8) return@updateSpriteStateWithBitmap s
      val blank = List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 }
      s.copy(frames = s.frames + listOf(blank), activeFrameIndex = s.frames.size)
    }
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
  }

  fun duplicateSpriteFrame() {
    updateSpriteStateWithBitmap { s ->
      if (s.frames.size >= 8) return@updateSpriteStateWithBitmap s
      val copy = s.currentFrame.toList()
      s.copy(frames = s.frames + listOf(copy), activeFrameIndex = s.frames.size)
    }
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
  }

  fun deleteSpriteFrame() {
    updateSpriteStateWithBitmap { s ->
      if (s.frames.size <= 1) return@updateSpriteStateWithBitmap s
      val mutable = s.frames.toMutableList()
      mutable.removeAt(s.activeFrameIndex)
      s.copy(frames = mutable, activeFrameIndex = (s.activeFrameIndex - 1).coerceAtLeast(0))
    }
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
  }

  fun applySpritePixelStroke(pixelIndex: Int) {
    if (pixelIndex !in 0 until SpritePixelMatrixEngine.PIXEL_COUNT) return
    val current = _spriteEditorState.value
    val frame = current.currentFrame
    when (current.activeTool) {
      SpriteToolMode.EYEDROPPER -> {
        val picked = frame[pixelIndex]
        if (picked in 0..7) {
          _spriteEditorState.update { it.copy(selectedColorSlot = picked, activeTool = SpriteToolMode.PENCIL) }
        }
      }
      SpriteToolMode.FILL -> {
        val filled = SpritePixelMatrixEngine.floodFill(frame, pixelIndex, current.selectedColorSlot)
        if (filled != frame) {
          commitSpriteFrameEdit(filled)
        }
      }
      SpriteToolMode.PENCIL, SpriteToolMode.ERASER -> {
        val value = if (current.activeTool == SpriteToolMode.PENCIL) current.selectedColorSlot else -1
        val mutable = frame.toMutableList()
        mutable[pixelIndex] = value
        if (current.mirrorX) {
          val row = pixelIndex / SpritePixelMatrixEngine.GRID_SIZE
          val col = pixelIndex % SpritePixelMatrixEngine.GRID_SIZE
          val mirrorCol = SpritePixelMatrixEngine.GRID_SIZE - 1 - col
          mutable[row * SpritePixelMatrixEngine.GRID_SIZE + mirrorCol] = value
        }
        if (mutable != frame) {
          commitSpriteFrameEdit(mutable)
        }
      }
    }
  }

  private fun commitSpriteFrameEdit(updatedFrame: List<Int>) {
    updateSpriteStateWithBitmap { s ->
      val newUndo = (s.undoStack + listOf(s.currentFrame)).takeLast(24)
      val newFrames = s.frames.toMutableList().also { it[s.activeFrameIndex] = updatedFrame }
      s.copy(frames = newFrames, undoStack = newUndo, useCustomBitmapInViewport = true)
    }
  }

  fun undoSpriteEdit() {
    val stack = _spriteEditorState.value.undoStack
    if (stack.isEmpty()) return
    updateSpriteStateWithBitmap { s ->
      val restored = s.undoStack.last()
      val newFrames = s.frames.toMutableList().also { it[s.activeFrameIndex] = restored }
      s.copy(frames = newFrames, undoStack = s.undoStack.dropLast(1))
    }
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
  }

  fun clearCurrentSpriteFrame() {
    commitSpriteFrameEdit(List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 })
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
  }

  /**
   * Quantizes an imported bitmap into BOTH Frame 1 (active) AND a gently 1px-bobbed Frame 2
   * so pressing Play animates the newly sampled sprite instead of flashing a stale monster!
   */
  fun importBitmapIntoSpriteEditor(bitmap: Bitmap, nameHint: String? = null) {
    val pkmn = _selectedPokemon.value
    val extractedPalette = SpritePixelMatrixEngine.extractPaletteFromBitmap(bitmap, pkmn.primaryHex, pkmn.secondaryHex)
    val quantizedF0 = SpritePixelMatrixEngine.quantizeBitmapToIndexedGrid(bitmap, extractedPalette)
    val bobbedF1 = buildBobbedSpriteFrame(quantizedF0)
    updateSpriteStateWithBitmap { s ->
      val newFrames = s.frames.toMutableList()
      if (newFrames.size < 2) {
        newFrames.clear()
        newFrames.add(quantizedF0)
        newFrames.add(bobbedF1)
      } else {
        newFrames[0] = quantizedF0
        newFrames[1] = bobbedF1
      }
      s.copy(
        seedDexNumber = pkmn.dexNumber,
        spriteName = nameHint ?: s.spriteName,
        paletteArgb = extractedPalette,
        frames = newFrames,
        activeFrameIndex = 0,
        undoStack = (s.undoStack + listOf(s.currentFrame)).takeLast(24),
        useCustomBitmapInViewport = true,
      )
    }
    audioEngine.playSfx(StudioSfxType.ITEM_USE)
    _statusBannerMessage.value = "Quantized asset into animated 2-frame 16x16 PPU sprite & 8-color palette!"
  }

  private fun buildBobbedSpriteFrame(baseFrame: List<Int>): List<Int> {
    val grid = SpritePixelMatrixEngine.GRID_SIZE
    val shifted = MutableList(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 }
    for (r in 0 until grid) {
      val srcRow = (r + 1).coerceAtMost(grid - 1)
      for (c in 0 until grid) {
        shifted[r * grid + c] = baseFrame[srcRow * grid + c]
      }
    }
    return shifted
  }

  fun loadSeedIntoSpriteEditor(pokemon: NativePokemonEntity) {
    val pal = SpritePixelMatrixEngine.buildInitialPalette(pokemon.primaryHex, pokemon.secondaryHex)
    val f0 = SpritePixelMatrixEngine.generateSeededFrame(pokemon.dexNumber, 0)
    val f1 = SpritePixelMatrixEngine.generateSeededFrame(pokemon.dexNumber, 1)
    updateSpriteStateWithBitmap { s ->
      s.copy(
        seedDexNumber = pokemon.dexNumber,
        spriteName = "${pokemon.name} Omni-Sprite",
        activeSavedSpriteId = null,
        paletteArgb = pal,
        frames = listOf(f0, f1),
        activeFrameIndex = 0,
        undoStack = emptyList(),
      )
    }
  }

  fun toggleUseCustomSpriteInViewport() {
    _spriteEditorState.update { it.copy(useCustomBitmapInViewport = !it.useCustomBitmapInViewport) }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun saveCurrentSpriteToRoomAndCloud() {
    val s = _spriteEditorState.value
    val spriteId = s.activeSavedSpriteId ?: "spr_${UUID.randomUUID().toString().take(10)}"
    val projectKey = activePersistenceProjectKey()
    val palJson = SpritePixelMatrixEngine.serializePaletteJson(s.paletteArgb)
    val framesJson = SpritePixelMatrixEngine.serializeFramesRleJson(s.frames)
    val cleanName = s.spriteName.trim().ifBlank { "${_selectedPokemon.value.name} Custom Rig" }
    val primaryHex = SpritePixelMatrixEngine.argbLongToHex(s.paletteArgb.getOrElse(0) { 0xFF00F5D4 })
    val secondaryHex = SpritePixelMatrixEngine.argbLongToHex(s.paletteArgb.getOrElse(1) { 0xFFFF2A85 })

    viewModelScope.launch {
      _isSavingProject.value = true
      val entity = SavedSpriteEntity(
        id = spriteId,
        projectId = projectKey,
        name = cleanName,
        spriteSeedId = s.seedDexNumber,
        paletteJson = palJson,
        framesRleJson = framesJson,
        frameCount = s.frames.size,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
      withContext(Dispatchers.IO) {
        persistenceDao.upsertSavedSprite(entity)
      }
      _spriteEditorState.update { it.copy(activeSavedSpriteId = spriteId, useCustomBitmapInViewport = true) }

      val compactPayload = "RLE_SPRITE|ID:$spriteId|PAL:$primaryHex,$secondaryHex|F:${s.frames.size}|${SpritePixelMatrixEngine.encodeFrameRle(s.currentFrame)}".take(200)
      val localAsset = CustomStudioAsset(
        name = cleanName,
        category = AssetCategory.SPRITES.label,
        consoleLineage = _selectedPokemon.value.consoleOriginSystem,
        chipAddon = "PPU 16x16 Voxel Rig",
        spriteSeedId = s.seedDexNumber.toLong(),
        shaderFormula = compactPayload,
        projectId = projectKey,
      )
      localProjectRepository.addStudioAsset(localAsset)
      _isSavingProject.value = false
      audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
      _statusBannerMessage.value = "Saved '$cleanName' (${s.frames.size} frames) to Local Room Vault! Active in Viewport & Game Test."
    }
  }

  fun loadSavedSpriteFromRoom(entity: SavedSpriteEntity) {
    val palette = SpritePixelMatrixEngine.deserializePaletteJson(entity.paletteJson)
    val frames = SpritePixelMatrixEngine.deserializeFramesRleJson(entity.framesRleJson)
      .ifEmpty { listOf(SpritePixelMatrixEngine.generateSeededFrame(entity.spriteSeedId, 0)) }
    updateSpriteStateWithBitmap { s ->
      s.copy(
        seedDexNumber = entity.spriteSeedId,
        spriteName = entity.name,
        activeSavedSpriteId = entity.id,
        paletteArgb = palette,
        frames = frames,
        activeFrameIndex = 0,
        undoStack = emptyList(),
        useCustomBitmapInViewport = true,
      )
    }
    audioEngine.playSfx(StudioSfxType.ITEM_USE)
    _statusBannerMessage.value = "Loaded saved sprite '${entity.name}' (${frames.size} frames) into Editor & 3D Voxel Viewport!"
  }

  fun deleteSavedSpriteFromRoom(spriteId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      persistenceDao.deleteSavedSprite(spriteId)
    }
    if (_spriteEditorState.value.activeSavedSpriteId == spriteId) {
      _spriteEditorState.update { it.copy(activeSavedSpriteId = null) }
    }
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
  }

  // =========================================================================
  // 16x16 PER-PROJECT PER-REGION MAP EDITOR + OVERWORLD + NPC INTERACTIONS
  // =========================================================================

  fun selectMapRegion(regionId: String) {
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId } ?: return
    _activeMapRegionId.value = chapter.id
    viewModelScope.launch(Dispatchers.IO) {
      loadMapForRegion(activePersistenceProjectKey(), chapter.id)
    }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun paintMapTile(index: Int) {
    if (index !in 0 until MasterCanonTemplateCatalog.MAP_TILE_COUNT) return
    val brush = _selectedBrushTile.value
    val current = _mapGrid.value
    if (current.size != MasterCanonTemplateCatalog.MAP_TILE_COUNT) return
    val updated = current.toMutableList().also { it[index] = brush }
    _mapGrid.value = updated
    persistActiveMapToRoom(updated)
  }

  fun resetActiveRegionMapToDefault() {
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == _activeMapRegionId.value }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    _mapGrid.value = chapter.defaultMapTiles
    persistActiveMapToRoom(chapter.defaultMapTiles)
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
    _statusBannerMessage.value = "Restored ${chapter.regionName} custom 16x16 tilemap."
  }

  private fun persistActiveMapToRoom(tiles: List<Int>) {
    val projectKey = activePersistenceProjectKey()
    val regionId = _activeMapRegionId.value
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    viewModelScope.launch(Dispatchers.IO) {
      persistenceDao.upsertSavedMap(
        SavedMapEntity(
          mapKey = mapStorageKey(projectKey, regionId),
          projectId = projectKey,
          regionId = regionId,
          regionName = chapter.regionName,
          gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE,
          tilesCsv = tiles.joinToString(","),
          updatedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }

  fun selectMapBrush(tileType: Int) {
    _selectedBrushTile.value = tileType.coerceIn(0, 11)
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun selectGameTestRegion(regionId: String) {
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId } ?: return
    _activeMapRegionId.value = chapter.id
    viewModelScope.launch(Dispatchers.IO) {
      loadMapForRegion(activePersistenceProjectKey(), chapter.id)
    }
    _gameTestState.update { s ->
      s.copy(
        activeRegionId = chapter.id,
        playerRow = chapter.spawnRow,
        playerCol = chapter.spawnCol,
        inBattle = false,
        activeDialoguePoi = null,
        showWarpGatePicker = false,
        playerAtkStage = 0,
        playerSpAStage = 0,
        enemyAtkStage = 0,
        enemySpAStage = 0,
        weather = chapter.weatherPreset,
        transitionPulseNonce = s.transitionPulseNonce + 1L,
        battleLog = (listOf("Traveled to ${chapter.regionName} (${chapter.biomeStyle.label}): ${chapter.storyLogEntry}") + s.battleLog).take(16),
      )
    }
    val regionTheme = BgmThemeId.fromId(chapter.defaultBgmThemeId)
    if (_audioConsoleState.value.isBgmPlaying) {
      playBgmTheme(regionTheme)
    } else {
      _audioConsoleState.update { it.copy(activeBgmTheme = regionTheme) }
    }
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    persistCampaignStateToRoom()
  }

  fun warpToRegion(regionId: String) {
    _gameTestState.update { it.copy(showWarpGatePicker = false) }
    selectGameTestRegion(regionId)
  }

  fun dismissWarpGatePicker() {
    _gameTestState.update { it.copy(showWarpGatePicker = false) }
  }

  fun dismissDialogueBanner() {
    _gameTestState.update { it.copy(activeDialoguePoi = null) }
  }

  fun movePlayerOnOverworld(dRow: Int, dCol: Int) {
    val gt = _gameTestState.value
    if (gt.inBattle) return
    val gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE
    val tiles = _mapGrid.value
    val moveDir = when {
      dRow < 0 -> MoveDirection.UP
      dRow > 0 -> MoveDirection.DOWN
      dCol < 0 -> MoveDirection.LEFT
      else -> MoveDirection.RIGHT
    }
    val (newCol, newRow) = UniversalGameFeelEngine.resolveStepWithCornerCorrection(
      x = gt.playerCol,
      y = gt.playerRow,
      direction = moveDir,
      gridSize = gridSize,
      tiles = tiles,
    )
    if (newRow == gt.playerRow && newCol == gt.playerCol) {
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      return
    }
    val landedTile = tiles.getOrElse(newRow * gridSize + newCol) { 0 }
    when (landedTile) {
      1, 9 -> audioEngine.playSfx(StudioSfxType.GRASS_RUSTLE)
      5, 11 -> audioEngine.playSfx(StudioSfxType.STEP_CRYSTAL)
      3, 7, 8 -> audioEngine.playSfx(StudioSfxType.STEP_STONE)
      else -> audioEngine.playSfx(StudioSfxType.STEP_GRASS)
    }

    val newSteps = gt.stepsTaken + 1
    _gameTestState.update {
      it.copy(
        playerRow = newRow,
        playerCol = newCol,
        stepsTaken = newSteps,
        activeDialoguePoi = null,
      )
    }
    persistCampaignStateToRoom()

    when (landedTile) {
      5 -> triggerManualEncounter(isBoss = true)
      8 -> healFullParty()
      11 -> {
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _gameTestState.update {
          it.copy(
            showWarpGatePicker = true,
            battleLog = (listOf("Stepped onto Region Warp Gate! Choose a connected region to travel.") + it.battleLog).take(16),
          )
        }
      }
      1, 9 -> {
        if (newSteps % 3 == 0) {
          triggerManualEncounter(isBoss = false)
        }
      }
    }
  }

  /**
   * "A — Interact" action on the 16x16 Overworld:
   * Checks current & 4-cardinal adjacent tiles for NPCs / Landmarks, Healing Plazas,
   * Prism Sanctuaries, or Region Warp Gates.
   */
  fun interactAtCurrentPosition() {
    val gt = _gameTestState.value
    if (gt.inBattle) return
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gt.activeRegionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE
    val tiles = _mapGrid.value

    // 1. Check same tile or adjacent tile for an NPC / POI
    val nearbyPoi = chapter.pointsOfInterest.firstOrNull { poi ->
      abs(poi.col - gt.playerCol) + abs(poi.row - gt.playerRow) <= 1
    }
    if (nearbyPoi != null) {
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      if (nearbyPoi.interactionKind == PoiInteractionKind.HEALER_OR_LORE) {
        healFullParty()
      }
      _gameTestState.update {
        it.copy(
          activeDialoguePoi = nearbyPoi,
          battleLog = (listOf("${nearbyPoi.name}: \"${nearbyPoi.dialogue}\"") + it.battleLog).take(16),
        )
      }
      return
    }

    // 2. Check current or adjacent special tiles (Healing Plaza=8, Prism Sanctuary=5, Warp Gate=11)
    val checkCoords = listOf(
      gt.playerCol to gt.playerRow,
      (gt.playerCol + 1).coerceAtMost(gridSize - 1) to gt.playerRow,
      (gt.playerCol - 1).coerceAtLeast(0) to gt.playerRow,
      gt.playerCol to (gt.playerRow + 1).coerceAtMost(gridSize - 1),
      gt.playerCol to (gt.playerRow - 1).coerceAtLeast(0),
    )
    val foundTile = checkCoords.map { (c, r) -> tiles.getOrElse(r * gridSize + c) { 0 } }
    when {
      8 in foundTile -> healFullParty()
      5 in foundTile -> triggerManualEncounter(isBoss = true)
      11 in foundTile -> {
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _gameTestState.update { it.copy(showWarpGatePicker = true) }
      }
      else -> {
        val currentTile = tiles.getOrElse(gt.playerRow * gridSize + gt.playerCol) { 0 }
        val tileName = MasterCanonTemplateCatalog.tileLegendLabels.firstOrNull { it.first == currentTile }?.second ?: "Open Path"
        audioEngine.playSfx(StudioSfxType.UI_HOVER)
        _gameTestState.update {
          it.copy(
            battleLog = (listOf("Inspected ($tileName) at [${gt.playerCol}, ${gt.playerRow}] in ${chapter.regionName}. Step near an NPC marker or Sanctuary to interact!") + it.battleLog).take(16),
          )
        }
      }
    }
  }

  fun triggerPoiBattle(poi: RegionPointOfInterest) {
    val dex = poi.battleDexSeed
    _gameTestState.update { it.copy(activeDialoguePoi = null) }
    val isRival = poi.interactionKind == PoiInteractionKind.RIVAL_OR_CHALLENGER
    if (dex != null) {
      triggerEncounterWithSpecificDex(
        enemyDex = dex,
        isBoss = isRival,
        titlePrefix = if (isRival) poi.name else "Guardian",
      )
    } else {
      triggerManualEncounter(isBoss = isRival)
    }
  }

  fun triggerManualEncounter(isBoss: Boolean) {
    val gt = _gameTestState.value
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gt.activeRegionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val pool = chapter.encounterPoolDexIds.ifEmpty { listOf(25, 94, 384) }
    val enemyDex = if (isBoss) {
      pool.last()
    } else {
      pool[abs(gt.stepsTaken + battleActionCounter++) % pool.size]
    }
    val prefix = if (isBoss) "Prism Sanctuary Guardian" else "Wild"
    triggerEncounterWithSpecificDex(enemyDex, isBoss, prefix)
  }

  private fun triggerEncounterWithSpecificDex(enemyDex: Int, isBoss: Boolean, titlePrefix: String) {
    val gt = _gameTestState.value
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gt.activeRegionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val enemyEntity = findPokemonByDex(enemyDex)
    val enemyLevel = if (isBoss) 55 else 46
    val enemySnap = StudioViewModelHelpers.buildCombatantFromEntity(enemyEntity, level = enemyLevel)
    val leadEntity = findPokemonByDex(gt.activeLeadDexId)
    val leadLevel = gt.levelFor(leadEntity.dexNumber)
    val leadSnap = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = leadEntity,
      level = leadLevel,
      gimmick = BattleGimmickMode.NONE,
      heldItemId = gt.heldItemsMap[leadEntity.dexNumber].orEmpty(),
    )
    val existingLeadHp = gt.partyCurrentHpMap[leadEntity.dexNumber]?.takeIf { it > 0 } ?: leadSnap.maxHp()
    val updatedHpMap = gt.partyCurrentHpMap + (leadEntity.dexNumber to existingLeadHp.coerceAtMost(leadSnap.maxHp()))

    audioEngine.playSfx(StudioSfxType.ENCOUNTER_STING)
    playPokemonCry(enemyEntity)
    if (_audioConsoleState.value.isBgmPlaying) {
      playBgmTheme(if (isBoss) BgmThemeId.GYM_TRIAL else BgmThemeId.WILD_ENCOUNTER)
    }

    markAtlasSeenOrBattled(enemyDex, seen = true, battled = false)

    _gameTestState.update {
      it.copy(
        inBattle = true,
        turnNumber = 1,
        partyCurrentHpMap = updatedHpMap,
        playerAtkStage = 0,
        playerSpAStage = 0,
        enemyAtkStage = 0,
        enemySpAStage = 0,
        enemyDexId = enemyDex,
        enemyLevel = enemyLevel,
        enemyCurrentHp = enemySnap.maxHp(),
        enemyMaxHp = enemySnap.maxHp(),
        enemyStatus = StatusCondition.HEALTHY,
        isBossEncounter = isBoss,
        activeGimmick = BattleGimmickMode.NONE,
        activeDialoguePoi = null,
        showWarpGatePicker = false,
        weather = chapter.weatherPreset,
        transitionPulseNonce = it.transitionPulseNonce + 1L,
        battleLog = (listOf("$titlePrefix ${enemyEntity.name} (Lv.$enemyLevel) materialized in ${chapter.regionName}!") + it.battleLog).take(16),
      )
    }
  }

  /**
   * Scales current HP proportionally via CanonBattleResolver.scaleHpProportionally instead of resetting to full HP!
   */
  fun toggleGameTestGimmick(targetGimmick: BattleGimmickMode) {
    val gt = _gameTestState.value
    val nextGimmick = if (gt.activeGimmick == targetGimmick) BattleGimmickMode.NONE else targetGimmick
    val lead = findPokemonByDex(gt.activeLeadDexId)
    val leadLevel = gt.levelFor(lead.dexNumber)
    val held = gt.heldItemsMap[lead.dexNumber].orEmpty()
    val oldMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
      lead, level = leadLevel, gimmick = gt.activeGimmick, teraType = gt.teraType, heldItemId = held,
    ).maxHp()
    val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
      lead, level = leadLevel, gimmick = nextGimmick, teraType = gt.teraType, heldItemId = held,
    ).maxHp()
    val currentHp = gt.partyCurrentHpMap[lead.dexNumber] ?: oldMaxHp
    val scaledHp = CanonBattleResolver.scaleHpProportionally(currentHp, oldMaxHp, newMaxHp)

    audioEngine.playSfx(if (nextGimmick == BattleGimmickMode.NONE) StudioSfxType.UI_CANCEL else StudioSfxType.STAT_BOOST)
    _gameTestState.update {
      it.copy(
        activeGimmick = nextGimmick,
        partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to scaledHp),
        battleLog = (listOf("${lead.name} shifted resonance to ${nextGimmick.displayName} (HP scaled: $scaledHp/$newMaxHp).") + it.battleLog).take(16),
      )
    }
  }

  fun setGameTestTeraType(type: PokemonType) {
    _gameTestState.update { it.copy(teraType = type) }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun setGameTestWeather(weather: WeatherCondition) {
    _gameTestState.update { it.copy(weather = weather) }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun setGameTestTerrain(terrain: TerrainCondition) {
    _gameTestState.update { it.copy(terrain = terrain) }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun toggleGameTestBag(open: Boolean? = null) {
    _gameTestState.update { it.copy(isBagOpen = open ?: !it.isBagOpen) }
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun selectBagCategory(category: String) {
    _gameTestState.update { it.copy(selectedBagCategory = category) }
  }

  fun switchGameTestPartySlot(index: Int) {
    val gt = _gameTestState.value
    if (index !in gt.activePartyDexIds.indices || index == gt.activePartyIndex) return
    val newLead = findPokemonByDex(gt.activePartyDexIds[index])
    val newLevel = gt.levelFor(newLead.dexNumber)
    val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(newLead, level = newLevel).maxHp()
    val existingHp = gt.partyCurrentHpMap[newLead.dexNumber] ?: newMaxHp
    if (existingHp <= 0) {
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      _statusBannerMessage.value = "${newLead.name} has fainted! Use a Revive or Healing Plaza first."
      return
    }
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    playPokemonCry(newLead)
    _gameTestState.update {
      it.copy(
        activePartyIndex = index,
        activeGimmick = BattleGimmickMode.NONE,
        playerAtkStage = 0,
        playerSpAStage = 0,
        partyCurrentHpMap = it.partyCurrentHpMap + (newLead.dexNumber to existingHp.coerceIn(1, newMaxHp)),
        battleLog = (listOf("Go! ${newLead.name} (Lv.$newLevel) entered the Resonance field!") + it.battleLog).take(16),
      )
    }
  }

  /**
   * Bug-fixed Turn Resolver:
   * - Picks enemy move first and passes defenderMovePriority into CanonBattleResolver.resolveTurn().
   * - Honors attackerActsFirst (faster / higher priority combatant strikes first).
   * - Applies counterShieldChipRecoil and Weakness Policy (+2 Atk / +2 SpA stages persisted across turns).
   * - Auto-switches to next conscious party member on faint; only returns to Healing Bay when ALL party members faint.
   */
  fun executePlayerBattleMove(move: BattleMoveSpec) {
    val gt = _gameTestState.value
    if (!gt.inBattle) return

    val playerEntity = findPokemonByDex(gt.activeLeadDexId)
    val enemyEntity = findPokemonByDex(gt.enemyDexId)
    val playerLevel = gt.levelFor(playerEntity.dexNumber)
    val playerHeld = gt.heldItemsMap[playerEntity.dexNumber].orEmpty()
    val playerStatus = gt.partyStatusMap[playerEntity.dexNumber] ?: StatusCondition.HEALTHY

    var pAtkStage = gt.playerAtkStage
    var pSpAStage = gt.playerSpAStage
    var eAtkStage = gt.enemyAtkStage
    var eSpAStage = gt.enemySpAStage

    fun currentPlayerSnap(hp: Int, maxHp: Int) = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = playerEntity,
      level = playerLevel,
      gimmick = gt.activeGimmick,
      teraType = gt.teraType,
      status = playerStatus,
      heldItemId = playerHeld,
      stages = StatStages(atk = pAtkStage, spA = pSpAStage),
      bondGauge = gt.bondGauge,
    ).copy(currentHpPercent = (hp * 100 / maxHp.coerceAtLeast(1)).coerceIn(1, 100))

    fun currentEnemySnap(hp: Int, maxHp: Int) = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = enemyEntity,
      level = gt.enemyLevel,
      status = gt.enemyStatus,
      stages = StatStages(atk = eAtkStage, spA = eSpAStage),
    ).copy(currentHpPercent = (hp * 100 / maxHp.coerceAtLeast(1)).coerceIn(1, 100))

    val basePlayerSnap = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = playerEntity,
      level = playerLevel,
      gimmick = gt.activeGimmick,
      teraType = gt.teraType,
      status = playerStatus,
      heldItemId = playerHeld,
      stages = StatStages(atk = pAtkStage, spA = pSpAStage),
      bondGauge = gt.bondGauge,
    )
    val playerMaxHp = basePlayerSnap.maxHp()
    var playerHp = (gt.partyCurrentHpMap[playerEntity.dexNumber] ?: playerMaxHp).coerceIn(1, playerMaxHp)
    var enemyHp = gt.enemyCurrentHp.coerceIn(1, gt.enemyMaxHp)

    val enemyMoves = StudioViewModelHelpers.buildMovesetForPokemon(
      enemyEntity,
      StudioViewModelHelpers.parsePokemonType(enemyEntity.primaryType),
    )
    val enemyMove = enemyMoves[gt.turnNumber % enemyMoves.size]

    val field = BattleTurnField(
      weather = gt.weather,
      terrain = gt.terrain,
      speedTieSeed = gt.turnNumber + battleActionCounter++,
    )

    val orderPreview = CanonBattleResolver.resolveTurn(
      attacker = currentPlayerSnap(playerHp, playerMaxHp),
      defender = currentEnemySnap(enemyHp, gt.enemyMaxHp),
      move = move,
      field = field,
      defenderMovePriority = enemyMove.priority,
    )
    val playerGoesFirst = orderPreview.attackerActsFirst

    val logs = mutableListOf<String>()
    var lastSeverity = 1
    var lastAttackerWasPlayer = true

    fun executePlayerStrike() {
      val result = CanonBattleResolver.resolveTurn(
        attacker = currentPlayerSnap(playerHp, playerMaxHp),
        defender = currentEnemySnap(enemyHp, gt.enemyMaxHp),
        move = move,
        field = field,
        defenderMovePriority = enemyMove.priority,
      )
      val dealt = if (result.isBlockedByTerrain || result.typeEffectiveness == 0f) {
        0
      } else {
        ((result.minDamage + result.maxDamage) / 2).coerceAtLeast(1)
      }
      enemyHp = (enemyHp - dealt).coerceAtLeast(if (result.focusSashTriggered) 1 else 0)
      if (result.lifeOrbRecoil > 0) {
        playerHp = (playerHp - result.lifeOrbRecoil).coerceAtLeast(0)
      }
      if (result.counterShieldChipRecoil > 0) {
        playerHp = (playerHp - result.counterShieldChipRecoil).coerceAtLeast(0)
      }
      if (result.weaknessPolicyTriggered && enemyHp > 0) {
        eAtkStage = (eAtkStage + 2).coerceAtMost(6)
        eSpAStage = (eSpAStage + 2).coerceAtMost(6)
        logs.add("Foe ${enemyEntity.name}'s Weakness Policy raised Atk & SpA (+2)!")
      }
      lastSeverity = when {
        result.typeEffectiveness >= 2f -> 2
        result.typeEffectiveness == 0f || result.typeEffectiveness < 1f -> 3
        else -> 1
      }
      lastAttackerWasPlayer = true
      audioEngine.playSfx(
        when (lastSeverity) {
          2 -> StudioSfxType.HIT_HEAVY_CRIT
          3 -> StudioSfxType.HIT_RESISTED
          else -> StudioSfxType.HIT_LIGHT
        }
      )
      val effTag = when {
        result.isBlockedByTerrain -> "Blocked by Psychic Terrain!"
        result.typeEffectiveness == 0f -> "No effect!"
        result.typeEffectiveness >= 2f -> "Super Effective (${result.typeEffectiveness}x)!"
        result.typeEffectiveness < 1f -> "Resisted (${result.typeEffectiveness}x)."
        else -> "Clean hit."
      }
      logs.add("${playerEntity.name} used ${move.name} [${result.resolvedMoveType.displayName}] for $dealt dmg! $effTag")
      if (result.lifeOrbRecoil > 0) logs.add("${playerEntity.name} took ${result.lifeOrbRecoil} Life Orb recoil.")
      if (result.counterShieldChipRecoil > 0) logs.add("Counter-Shield deflected ${result.counterShieldChipRecoil} chip recoil!")
    }

    fun executeEnemyStrike() {
      val enemyResult = CanonBattleResolver.resolveTurn(
        attacker = currentEnemySnap(enemyHp, gt.enemyMaxHp),
        defender = currentPlayerSnap(playerHp, playerMaxHp),
        move = enemyMove,
        field = field,
        defenderMovePriority = move.priority,
      )
      val enemyDealt = if (enemyResult.isBlockedByTerrain || enemyResult.typeEffectiveness == 0f) {
        0
      } else {
        ((enemyResult.minDamage + enemyResult.maxDamage) / 2).coerceAtLeast(1)
      }
      playerHp = (playerHp - enemyDealt).coerceAtLeast(if (enemyResult.focusSashTriggered) 1 else 0)
      if (enemyResult.counterShieldChipRecoil > 0) {
        enemyHp = (enemyHp - enemyResult.counterShieldChipRecoil).coerceAtLeast(0)
        logs.add("${playerEntity.name}'s Counter-Shield dealt ${enemyResult.counterShieldChipRecoil} chip recoil!")
      }
      logs.add("Foe ${enemyEntity.name} used ${enemyMove.name} for $enemyDealt dmg!")
      if (enemyResult.focusSashTriggered) {
        logs.add("${playerEntity.name} hung on with Focus Sash (1 HP)!")
      }
      if (enemyResult.weaknessPolicyTriggered && playerHp > 0) {
        pAtkStage = (pAtkStage + 2).coerceAtMost(6)
        pSpAStage = (pSpAStage + 2).coerceAtMost(6)
        audioEngine.playSfx(StudioSfxType.STAT_BOOST)
        logs.add("${playerEntity.name}'s Weakness Policy triggered! Atk +2, SpA +2!")
      }
      lastAttackerWasPlayer = false
    }

    if (playerGoesFirst) {
      executePlayerStrike()
      if (enemyHp > 0 && playerHp > 0) {
        executeEnemyStrike()
      }
    } else {
      logs.add("Foe ${enemyEntity.name} moved first!")
      executeEnemyStrike()
      if (playerHp > 0 && enemyHp > 0) {
        executePlayerStrike()
      }
    }

    markAtlasSeenOrBattled(enemyEntity.dexNumber, seen = true, battled = true)

    // Victory check
    if (enemyHp <= 0) {
      audioEngine.playSfx(StudioSfxType.LEVEL_UP)
      logs.add("${enemyEntity.name} fainted! Victory in ${gt.turnNumber} turn(s)!")
      if (gt.isBossEncounter) {
        val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gt.activeRegionId }
        if (chapter != null) {
          completeCampaignChapter(chapter)
          logs.add("Recovered Harmonic Shard for ${chapter.regionName}!")
        }
      }
      if (_audioConsoleState.value.isBgmPlaying) {
        playBgmTheme(BgmThemeId.VICTORY_FANFARE)
      }
      _gameTestState.update {
        it.copy(
          inBattle = false,
          enemyCurrentHp = 0,
          playerAtkStage = 0,
          playerSpAStage = 0,
          enemyAtkStage = 0,
          enemySpAStage = 0,
          partyCurrentHpMap = it.partyCurrentHpMap + (playerEntity.dexNumber to playerHp.coerceAtLeast(1)),
          battleLog = (logs.reversed() + it.battleLog).take(16),
          lastEventNonce = it.lastEventNonce + 1L,
          lastHitSeverity = lastSeverity,
          lastAttackerWasPlayer = true,
        )
      }
      return
    }

    // Leftovers end-of-turn recovery if alive
    if (playerHp in 1 until playerMaxHp) {
      val leftoversHeal = CanonBattleResolver.computeEndOfTurnLeftoversHeal(basePlayerSnap)
      if (leftoversHeal > 0) {
        playerHp = (playerHp + leftoversHeal).coerceAtMost(playerMaxHp)
        logs.add("${playerEntity.name} restored $leftoversHeal HP via Leftovers.")
      }
    }

    var updatedHpMap = gt.partyCurrentHpMap + (playerEntity.dexNumber to playerHp)
    var nextPartyIndex = gt.activePartyIndex
    var nextGimmick = gt.activeGimmick
    var stillInBattle = true

    // Auto-switch to next conscious party member on faint; only blackout when entire party is at 0 HP
    if (playerHp <= 0) {
      logs.add("${playerEntity.name} fainted!")
      val nextConsciousIdx = gt.activePartyDexIds.indices.firstOrNull { idx ->
        if (idx == gt.activePartyIndex) return@firstOrNull false
        val dex = gt.activePartyDexIds[idx]
        val memberHp = updatedHpMap[dex]
        memberHp == null || memberHp > 0
      }
      if (nextConsciousIdx != null) {
        val replacement = findPokemonByDex(gt.activePartyDexIds[nextConsciousIdx])
        val repMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
          replacement, level = gt.levelFor(replacement.dexNumber),
        ).maxHp()
        val repHp = updatedHpMap[replacement.dexNumber] ?: repMaxHp
        updatedHpMap = updatedHpMap + (replacement.dexNumber to repHp.coerceIn(1, repMaxHp))
        nextPartyIndex = nextConsciousIdx
        nextGimmick = BattleGimmickMode.NONE
        pAtkStage = 0
        pSpAStage = 0
        playPokemonCry(replacement)
        logs.add("Auto-sent ${replacement.name} to continue the battle!")
      } else {
        logs.add("All party partners fainted! Returned to Prism Healing Plaza.")
        updatedHpMap = gt.activePartyDexIds.associateWith { dex ->
          val e = findPokemonByDex(dex)
          StudioViewModelHelpers.buildCombatantFromEntity(e, level = gt.levelFor(dex)).maxHp()
        }
        nextPartyIndex = 0
        nextGimmick = BattleGimmickMode.NONE
        pAtkStage = 0
        pSpAStage = 0
        eAtkStage = 0
        eSpAStage = 0
        stillInBattle = false
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
      }
    }

    _gameTestState.update {
      it.copy(
        inBattle = stillInBattle,
        turnNumber = it.turnNumber + 1,
        activePartyIndex = nextPartyIndex,
        activeGimmick = nextGimmick,
        playerAtkStage = pAtkStage,
        playerSpAStage = pSpAStage,
        enemyAtkStage = eAtkStage,
        enemySpAStage = eSpAStage,
        enemyCurrentHp = enemyHp,
        partyCurrentHpMap = updatedHpMap,
        battleLog = (logs.reversed() + it.battleLog).take(16),
        lastEventNonce = it.lastEventNonce + 1L,
        lastHitSeverity = lastSeverity,
        lastAttackerWasPlayer = lastAttackerWasPlayer,
      )
    }
  }

  fun throwPokeBall(ballItem: StudioItemEntity) {
    val gt = _gameTestState.value
    if (!gt.inBattle) {
      _statusBannerMessage.value = "Enter a wild encounter first to throw ${ballItem.name}!"
      return
    }
    if (ballItem.quantity <= 0) {
      _statusBannerMessage.value = "Out of ${ballItem.name}!"
      return
    }

    val enemyEntity = findPokemonByDex(gt.enemyDexId)
    val alreadyCaptured = atlasProgressMap.value[enemyEntity.dexNumber]?.captured == true
    val gridSize = MasterCanonTemplateCatalog.MAP_GRID_SIZE
    val landedTile = _mapGrid.value.getOrElse(gt.playerRow * gridSize + gt.playerCol) { 0 }
    val roll = ((abs((gt.turnNumber * 37 + gt.enemyCurrentHp * 13 + battleActionCounter++ * 29) % 100)) / 100f)

    val result = CanonBattleResolver.calculateCaptureResult(
      ballId = ballItem.id,
      ballName = ballItem.name,
      targetCurrentHp = gt.enemyCurrentHp,
      targetMaxHp = gt.enemyMaxHp,
      targetBaseStatTotal = enemyEntity.baseStatTotal,
      targetPrimaryType = StudioViewModelHelpers.parsePokemonType(enemyEntity.primaryType),
      targetSecondaryType = enemyEntity.secondaryType.takeIf { it.isNotBlank() }?.let { StudioViewModelHelpers.parsePokemonType(it) },
      targetStatus = gt.enemyStatus,
      turnNumber = gt.turnNumber,
      isWaterSector = landedTile == 2,
      isRiftOrShadow = gt.isBossEncounter || landedTile == 4 || landedTile == 5,
      alreadyCaptured = alreadyCaptured,
      deterministicRoll01 = roll,
    )

    viewModelScope.launch(Dispatchers.IO) {
      persistenceDao.upsertItem(ballItem.copy(quantity = (ballItem.quantity - 1).coerceAtLeast(0)))
    }

    audioEngine.playSfx(StudioSfxType.POKEBALL_THROW)
    if (result.captured) {
      audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
      markAtlasCaptured(enemyEntity.dexNumber)
      val newParty = if (gt.activePartyDexIds.size < 6 && enemyEntity.dexNumber !in gt.activePartyDexIds) {
        gt.activePartyDexIds + enemyEntity.dexNumber
      } else {
        gt.activePartyDexIds
      }
      _gameTestState.update {
        it.copy(
          inBattle = false,
          isBagOpen = false,
          activePartyDexIds = newParty,
          battleLog = (listOf("Gotcha! ${enemyEntity.name} was caught! ${result.summary}") + it.battleLog).take(16),
        )
      }
      persistCampaignStateToRoom()
    } else {
      audioEngine.playSfx(StudioSfxType.POKEBALL_SHAKE)
      _gameTestState.update {
        it.copy(
          turnNumber = it.turnNumber + 1,
          isBagOpen = false,
          battleLog = (listOf(result.summary) + it.battleLog).take(16),
        )
      }
    }
  }

  /**
   * 100% Functional Bag Item dispatcher honoring every single effectTag:
   * HEAL_HP, HEAL_PCT, HEAL_FULL_HP, HEAL_FULL_ALL, REVIVE_HALF, CURE_STATUS,
   * LEVEL_UP, BOOST_GAUGE, TM, EVO, HOLD_*, GIMMICK:*, KEY:*, SHARD:*.
   */
  fun useBagItem(item: StudioItemEntity) {
    val gt = _gameTestState.value
    val tag = item.effectTag.trim()
    val lead = findPokemonByDex(gt.activeLeadDexId)
    val leadLevel = gt.levelFor(lead.dexNumber)
    val held = gt.heldItemsMap[lead.dexNumber].orEmpty()
    val leadMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
      lead, level = leadLevel, gimmick = gt.activeGimmick, heldItemId = held,
    ).maxHp()

    fun consumeItem() {
      viewModelScope.launch(Dispatchers.IO) {
        persistenceDao.upsertItem(item.copy(quantity = (item.quantity - 1).coerceAtLeast(0)))
      }
    }

    when {
      item.category == StudioItemCatalog.CAT_POKE_BALL || tag.startsWith("CATCH") -> {
        throwPokeBall(item)
      }

      tag.startsWith("HEAL_HP:") -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val amt = tag.removePrefix("HEAL_HP:").toIntOrNull() ?: item.effectPower.toInt().coerceAtLeast(1)
        val curHp = gt.partyCurrentHpMap[lead.dexNumber] ?: leadMaxHp
        val newHp = (curHp + amt).coerceAtMost(leadMaxHp)
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to newHp),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} (+$amt HP -> $newHp/$leadMaxHp).") + it.battleLog).take(16),
          )
        }
      }

      tag.startsWith("HEAL_PCT:") -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val pct = tag.removePrefix("HEAL_PCT:").toFloatOrNull() ?: item.effectPower
        val amt = (leadMaxHp * pct).roundToInt().coerceAtLeast(1)
        val curHp = gt.partyCurrentHpMap[lead.dexNumber] ?: leadMaxHp
        val newHp = (curHp + amt).coerceAtMost(leadMaxHp)
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to newHp),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} (+${(pct * 100).roundToInt()}% = +$amt HP -> $newHp/$leadMaxHp).") + it.battleLog).take(16),
          )
        }
      }

      tag == "HEAL_FULL_HP" -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to leadMaxHp),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} -> Full HP ($leadMaxHp/$leadMaxHp)!") + it.battleLog).take(16),
          )
        }
      }

      tag == "HEAL_FULL_ALL" -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to leadMaxHp),
            partyStatusMap = it.partyStatusMap + (lead.dexNumber to StatusCondition.HEALTHY),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} -> Full HP ($leadMaxHp/$leadMaxHp) & cured all status!") + it.battleLog).take(16),
          )
        }
      }

      tag == "REVIVE_HALF" -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val faintedDex = gt.activePartyDexIds.firstOrNull { dex ->
          (gt.partyCurrentHpMap[dex] ?: 1) <= 0
        }
        if (faintedDex == null) {
          audioEngine.playSfx(StudioSfxType.UI_CANCEL)
          _statusBannerMessage.value = "No fainted party partner needs a Revive right now."
          return
        }
        val faintedEntity = findPokemonByDex(faintedDex)
        val faintedMax = StudioViewModelHelpers.buildCombatantFromEntity(
          faintedEntity, level = gt.levelFor(faintedDex),
        ).maxHp()
        val halfHp = (faintedMax / 2).coerceAtLeast(1)
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (faintedDex to halfHp),
            partyStatusMap = it.partyStatusMap + (faintedDex to StatusCondition.HEALTHY),
            isBagOpen = false,
            battleLog = (listOf("Revived ${faintedEntity.name} to $halfHp/$faintedMax HP (50%)!") + it.battleLog).take(16),
          )
        }
      }

      tag == "CURE_STATUS" -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        consumeItem()
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyStatusMap = it.partyStatusMap + (lead.dexNumber to StatusCondition.HEALTHY),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} -> Status cured (HP unchanged)!") + it.battleLog).take(16),
          )
        }
      }

      tag.startsWith("LEVEL_UP:") -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val delta = tag.removePrefix("LEVEL_UP:").toIntOrNull() ?: 5
        val newLevel = (leadLevel + delta).coerceIn(1, 100)
        val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
          lead, level = newLevel, gimmick = gt.activeGimmick, heldItemId = held,
        ).maxHp()
        val curHp = gt.partyCurrentHpMap[lead.dexNumber] ?: leadMaxHp
        val scaledHp = CanonBattleResolver.scaleHpProportionally(curHp, leadMaxHp, newMaxHp)
        consumeItem()
        audioEngine.playSfx(StudioSfxType.LEVEL_UP)
        _gameTestState.update {
          it.copy(
            partyLevelMap = it.partyLevelMap + (lead.dexNumber to newLevel),
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to scaledHp),
            isBagOpen = false,
            battleLog = (listOf("${item.name} boosted ${lead.name} to Lv.$newLevel (HP $scaledHp/$newMaxHp)!") + it.battleLog).take(16),
          )
        }
        persistCampaignStateToRoom()
      }

      tag.startsWith("BOOST_GAUGE:") -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val boost = tag.removePrefix("BOOST_GAUGE:").toIntOrNull() ?: 20
        val newGauge = (gt.bondGauge + boost).coerceIn(0, 100)
        consumeItem()
        audioEngine.playSfx(StudioSfxType.STAT_BOOST)
        _gameTestState.update {
          it.copy(
            bondGauge = newGauge,
            isBagOpen = false,
            battleLog = (listOf("${item.name} recharged Resonance Bond Gauge to $newGauge/100!") + it.battleLog).take(16),
          )
        }
      }

      tag.startsWith("TM:") -> {
        val taught = StudioViewModelHelpers.parseTmEffectTag(tag)
        if (taught == null) {
          _statusBannerMessage.value = "Could not decode TM move data for ${item.name}."
          return
        }
        audioEngine.playSfx(StudioSfxType.ITEM_USE)
        _gameTestState.update {
          it.copy(
            taughtMovesMap = it.taughtMovesMap + (lead.dexNumber to taught),
            isBagOpen = false,
            battleLog = (listOf("${lead.name} learned ${taught.name} [${taught.type.displayName} • ${taught.basePower} BP] in Move Slot 4!") + it.battleLog).take(16),
          )
        }
        persistCampaignStateToRoom()
      }

      tag.startsWith("EVO:") -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val evoType = tag.removePrefix("EVO:")
        val targetDex = StudioViewModelHelpers.resolveStoneEvolutionTarget(lead.dexNumber, evoType)
        consumeItem()
        if (targetDex != null && targetDex != lead.dexNumber) {
          val evolved = findPokemonByDex(targetDex)
          val newParty = gt.activePartyDexIds.toMutableList().also {
            if (gt.activePartyIndex in it.indices) it[gt.activePartyIndex] = targetDex
          }
          val evolvedMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
            evolved, level = leadLevel, gimmick = gt.activeGimmick, heldItemId = held,
          ).maxHp()
          val curHp = gt.partyCurrentHpMap[lead.dexNumber] ?: leadMaxHp
          val scaledHp = CanonBattleResolver.scaleHpProportionally(curHp, leadMaxHp, evolvedMaxHp)
          markAtlasCaptured(targetDex)
          audioEngine.playSfx(StudioSfxType.LEVEL_UP)
          playPokemonCry(evolved)
          _gameTestState.update {
            it.copy(
              activePartyDexIds = newParty,
              partyLevelMap = it.partyLevelMap + (targetDex to leadLevel),
              partyCurrentHpMap = it.partyCurrentHpMap + (targetDex to scaledHp),
              heldItemsMap = if (held.isNotBlank()) it.heldItemsMap + (targetDex to held) else it.heldItemsMap,
              taughtMovesMap = it.taughtMovesMap[lead.dexNumber]?.let { mv -> it.taughtMovesMap + (targetDex to mv) } ?: it.taughtMovesMap,
              isBagOpen = false,
              transitionPulseNonce = it.transitionPulseNonce + 1L,
              battleLog = (listOf("What?! ${lead.name} reacted to ${item.name} and evolved into ${evolved.name} (#$targetDex)!") + it.battleLog).take(16),
            )
          }
          persistCampaignStateToRoom()
        } else {
          val boostedLevel = (leadLevel + 2).coerceIn(1, 100)
          val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
            lead, level = boostedLevel, gimmick = gt.activeGimmick, heldItemId = held,
          ).maxHp()
          audioEngine.playSfx(StudioSfxType.LEVEL_UP)
          _gameTestState.update {
            it.copy(
              partyLevelMap = it.partyLevelMap + (lead.dexNumber to boostedLevel),
              partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to newMaxHp),
              playerAtkStage = (it.playerAtkStage + 1).coerceAtMost(6),
              playerSpAStage = (it.playerSpAStage + 1).coerceAtMost(6),
              isBagOpen = false,
              transitionPulseNonce = it.transitionPulseNonce + 1L,
              battleLog = (listOf("${item.name} triggered Resonance Evolution Surge on ${lead.name}! Lv.$boostedLevel, Atk/SpA +1!") + it.battleLog).take(16),
            )
          }
          persistCampaignStateToRoom()
        }
      }

      item.category == StudioItemCatalog.CAT_HOLD_ITEM || tag.startsWith("HOLD_") -> {
        audioEngine.playSfx(StudioSfxType.ITEM_USE)
        _gameTestState.update {
          it.copy(
            heldItemsMap = it.heldItemsMap + (lead.dexNumber to item.id),
            isBagOpen = false,
            battleLog = (listOf("Equipped ${item.name} onto ${lead.name}! Active in battle calculations.") + it.battleLog).take(16),
          )
        }
        persistCampaignStateToRoom()
      }

      item.category == StudioItemCatalog.CAT_GIMMICK_CATALYST || tag.startsWith("GIMMICK:") -> {
        val gimmick = when {
          tag.contains("MEGA", ignoreCase = true) || item.id.contains("mega") -> BattleGimmickMode.MEGA_EVOLUTION
          tag.contains("Z_MOVE", ignoreCase = true) || item.id.contains("z_ring") -> BattleGimmickMode.Z_MOVE
          tag.contains("DYNAMAX", ignoreCase = true) || item.id.contains("dynamax") -> BattleGimmickMode.DYNAMAX
          tag.contains("TERASTAL", ignoreCase = true) || item.id.contains("tera") -> BattleGimmickMode.TERASTALLIZATION
          else -> BattleGimmickMode.ANIME_BATTLE_BOND
        }
        toggleGameTestGimmick(gimmick)
        _gameTestState.update { it.copy(isBagOpen = false) }
      }

      tag == "KEY:COMPASS" -> {
        val camp = _campaignState.value
        val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == gt.activeRegionId }
        val msg = "Prism Compass: ${camp.harmonicShardsCollected}/14 Shards collected • Active Region: ${chapter?.regionName ?: gt.activeRegionId} (${chapter?.keyLoreMilestone ?: "Sanctuary"})."
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _gameTestState.update {
          it.copy(
            isBagOpen = false,
            battleLog = (listOf(msg) + it.battleLog).take(16),
          )
        }
        _statusBannerMessage.value = msg
      }

      else -> {
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _gameTestState.update {
          it.copy(
            isBagOpen = false,
            battleLog = (listOf("${item.name}: ${item.description}") + it.battleLog).take(16),
          )
        }
        _statusBannerMessage.value = "${item.name}: ${item.description}"
      }
    }
  }

  fun fleeBattle() {
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == _gameTestState.value.activeRegionId }
    if (_audioConsoleState.value.isBgmPlaying && chapter != null) {
      playBgmTheme(BgmThemeId.fromId(chapter.defaultBgmThemeId))
    }
    _gameTestState.update {
      it.copy(
        inBattle = false,
        activeGimmick = BattleGimmickMode.NONE,
        playerAtkStage = 0,
        playerSpAStage = 0,
        enemyAtkStage = 0,
        enemySpAStage = 0,
        battleLog = (listOf("Disengaged cleanly back to 16x16 overworld.") + it.battleLog).take(16),
      )
    }
  }

  fun healFullParty() {
    audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
    val gt = _gameTestState.value
    val restored = gt.activePartyDexIds.associateWith { dex ->
      val entity = findPokemonByDex(dex)
      StudioViewModelHelpers.buildCombatantFromEntity(entity, level = gt.levelFor(dex)).maxHp()
    }
    _gameTestState.update {
      it.copy(
        partyCurrentHpMap = restored,
        partyStatusMap = emptyMap(),
        battleLog = (listOf("Prism Healing Plaza restored full HP & status for all ${gt.activePartyDexIds.size} partners!") + it.battleLog).take(16),
      )
    }
  }

  fun completeCampaignChapter(chapter: CanonRegionChapter) {
    val updated = _campaignState.value.completeChapter(chapter)
    if (updated != _campaignState.value) {
      _campaignState.value = updated
      audioEngine.playSfx(StudioSfxType.LEVEL_UP)
      persistCampaignStateToRoom()
      _statusBannerMessage.value = "Completed ${chapter.regionName}! Harmonic Shards: ${updated.harmonicShardsCollected}/14."
    }
  }

  fun resetCampaignProgression() {
    _campaignState.value = CampaignProgressionState()
    persistCampaignStateToRoom()
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
  }

  private fun markAtlasSeenOrBattled(dexNumber: Int, seen: Boolean, battled: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val existing = persistenceDao.getAtlasProgress(dexNumber) ?: AtlasProgressEntity(dexNumber = dexNumber)
      persistenceDao.upsertAtlasProgress(
        existing.copy(
          seen = existing.seen || seen,
          battled = existing.battled || battled,
          updatedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }

  private fun markAtlasCaptured(dexNumber: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      val existing = persistenceDao.getAtlasProgress(dexNumber) ?: AtlasProgressEntity(dexNumber = dexNumber)
      persistenceDao.upsertAtlasProgress(
        existing.copy(
          seen = true,
          battled = true,
          captured = true,
          updatedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }

  fun harmonizeSpeciesWithHardware(dexNumber: Int) {
    val hwSignature = _hardwareRegistryState.value.convergedVisuals.signatureSummary.take(64)
    viewModelScope.launch(Dispatchers.IO) {
      val existing = persistenceDao.getAtlasProgress(dexNumber) ?: AtlasProgressEntity(dexNumber = dexNumber)
      persistenceDao.upsertAtlasProgress(
        existing.copy(
          seen = true,
          hardwareHarmonized = true,
          harmonizedHardwareId = hwSignature,
          updatedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
    audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
    _statusBannerMessage.value = "Harmonized #$dexNumber with $hwSignature in Resonance Atlas Dex!"
  }

  /**
   * Bug-fixed National Dex resolver:
   * Checks Room cache -> built-in curated catalog -> instant clean-room synthesis from NationalAtlasIndex
   * (with deduplicated background PokéAPI hydration), so it NEVER falls back to Bulbasaur!
   */
  fun findPokemonByDex(dexNumber: Int): NativePokemonEntity {
    val cached = nativePokemonList.value.firstOrNull { it.dexNumber == dexNumber }
      ?: NativeFranchiseCatalogRepository.builtInFranchiseDex.firstOrNull { it.dexNumber == dexNumber }
    if (cached != null) return cached

    if (dexNumber in 1..1025 && pendingHydrationDexIds.add(dexNumber)) {
      viewModelScope.launch(Dispatchers.IO) {
        runCatching { localDexRepository.refreshLivePokemonIfOnline(dexNumber) }
      }
    }
    return StudioViewModelHelpers.synthesizePokemonFromAtlas(dexNumber)
  }

  // =========================================================================
  // RESOURCE MANAGER (EXACT AUDIO/TILE ROUTING + ROOM PROJECT LINKING)
  // =========================================================================

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
      audioEngine.playSfx(StudioSfxType.ITEM_USE)
      _statusBannerMessage.value = "Saved '${created.name}' [${category.shortBadge}] to Room Resource Manager!"
    }
  }

  fun deleteCustomRoomResource(resourceId: String) {
    viewModelScope.launch {
      encyclopediaAndResourceRepository.deleteCustomResource(resourceId)
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      _statusBannerMessage.value = "Removed custom resource from Room database."
    }
  }

  /**
   * Exact Resource Manager preview routing by resource ID & category (no generic fallback mismatch).
   */
  fun previewRoomResource(resource: StudioResourceEntity) {
    when {
      resource.categoryId == ResourceCategory.MAP_TILES.id -> {
        if (resource.tileTypeIndex in 0..11) {
          _selectedBrushTile.value = resource.tileTypeIndex
        }
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _statusBannerMessage.value = "Loaded Map Tile '${resource.name}' (Tile #${resource.tileTypeIndex}) into 16x16 HD-2D Tile Brush!"
      }

      resource.id == "res_sfx_battle_cry_synth" -> {
        playPokemonCry(findPokemonByDex(384))
        _statusBannerMessage.value = "Synthesized Rayquaza (#384) FM+PSG Cry via Omni-Hardware DSP!"
      }

      resource.id == "res_sfx_super_effective_hit" -> {
        playSfx(StudioSfxType.HIT_HEAVY_CRIT)
        _statusBannerMessage.value = "Played Super-Effective Heavy Crit SFX (${resource.formatTag})."
      }

      resource.id == "res_sfx_pokeball_shake" -> {
        playSfx(StudioSfxType.POKEBALL_SHAKE)
        _statusBannerMessage.value = "Played Poké Ball Shake SFX (${resource.formatTag})."
      }

      resource.id == "res_sfx_level_up_fanfare" -> {
        playSfx(StudioSfxType.LEVEL_UP)
        _statusBannerMessage.value = "Played Level Up Fanfare SFX (${resource.formatTag})."
      }

      resource.id == "res_bgm_mt_silver_fusion" -> {
        playBgmTheme(BgmThemeId.CHAMPION_CONVERGENCE)
        _statusBannerMessage.value = "Streaming Champion Convergence 44.1kHz Omni-Hardware BGM!"
      }

      resource.id == "res_bgm_littleroot_hd2d" -> {
        playBgmTheme(BgmThemeId.HARMONIC_LAB)
        _statusBannerMessage.value = "Streaming Harmonic Lab 44.1kHz Omni-Hardware BGM!"
      }

      resource.id == "res_bgm_orre_mirorb_battle" -> {
        playBgmTheme(BgmThemeId.TEAM_MONOCHROME)
        _statusBannerMessage.value = "Streaming Team Monochrome Battle 44.1kHz Omni-Hardware BGM!"
      }

      resource.categoryId == ResourceCategory.BGM.id -> {
        val theme = when {
          resource.name.contains("Battle", ignoreCase = true) -> BgmThemeId.WILD_ENCOUNTER
          resource.name.contains("Champion", ignoreCase = true) || resource.name.contains("Silver", ignoreCase = true) -> BgmThemeId.CHAMPION_CONVERGENCE
          resource.name.contains("Route", ignoreCase = true) -> BgmThemeId.KANTO_JOHTO_ROUTE
          resource.name.contains("Lab", ignoreCase = true) || resource.name.contains("Town", ignoreCase = true) -> BgmThemeId.HARMONIC_LAB
          else -> BgmThemeId.PRISM_TITLE
        }
        playBgmTheme(theme)
        _statusBannerMessage.value = "Streaming 44.1kHz Omni-Hardware BGM: ${theme.displayName}"
      }

      else -> {
        val sfx = when {
          resource.name.contains("Cry", ignoreCase = true) -> {
            playPokemonCry(_selectedPokemon.value)
            _statusBannerMessage.value = "Synthesized ${_selectedPokemon.value.name} Cry preview."
            return
          }
          resource.name.contains("Crit", ignoreCase = true) || resource.name.contains("Hit", ignoreCase = true) -> StudioSfxType.HIT_HEAVY_CRIT
          resource.name.contains("Ball", ignoreCase = true) -> StudioSfxType.POKEBALL_SHAKE
          resource.name.contains("Heal", ignoreCase = true) -> StudioSfxType.HEAL_CHIME
          else -> StudioSfxType.LEVEL_UP
        }
        playSfx(sfx)
        _statusBannerMessage.value = "Synthesizing SFX preview: ${resource.name} (${resource.formatTag})"
      }
    }
  }

  fun assignResourceToGameProject(resource: StudioResourceEntity, project: FanGameProject) {
    viewModelScope.launch {
      val alreadyAssigned = resource.isAssignedToProject(project.id)
      encyclopediaAndResourceRepository.assignResourceToProject(resource.id, project.id)
      if (!alreadyAssigned && project.id.isNotBlank() && resource.categoryId == ResourceCategory.MAP_TILES.id) {
        localProjectRepository.updateProject(project.copy(mapCount = (project.mapCount + 1L).coerceAtMost(100L)))
      }
      audioEngine.playSfx(StudioSfxType.ITEM_USE)
      _statusBannerMessage.value = if (alreadyAssigned) {
        "'${resource.name}' is already linked to '${project.title.ifBlank { "Fan Game" }}'."
      } else {
        "Attached '${resource.name}' into '${project.title.ifBlank { "Fan Game" }}'!"
      }
    }
  }

  fun unassignResourceFromGameProject(resource: StudioResourceEntity, project: FanGameProject) {
    viewModelScope.launch {
      val wasAssigned = resource.isAssignedToProject(project.id)
      encyclopediaAndResourceRepository.unassignResourceFromProject(resource.id, project.id)
      if (wasAssigned && project.id.isNotBlank() && resource.categoryId == ResourceCategory.MAP_TILES.id) {
        localProjectRepository.updateProject(project.copy(mapCount = (project.mapCount - 1L).coerceAtLeast(1L)))
      }
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      _statusBannerMessage.value = "Detached '${resource.name}' from '${project.title}'."
    }
  }

  // =========================================================================
  // NAVIGATION, DEX, ENGINE CONFIG & 100% OFFLINE LOCAL PROJECT VAULT + SAF JSON
  // =========================================================================

  fun selectWorkspaceTab(tab: StudioWorkspaceTab) {
    _activeTab.value = tab
    audioEngine.playSfx(StudioSfxType.UI_HOVER)
  }

  fun selectSidebarCategory(category: AssetCategory) {
    _selectedSidebarCategory.value = category
  }

  fun toggleSidebarExpanded() {
    _sidebarExpanded.update { !it }
  }

  fun selectPrimaryPokemon(pokemon: NativePokemonEntity) {
    _selectedPokemon.value = pokemon
    markAtlasSeenOrBattled(pokemon.dexNumber, seen = true, battled = false)
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
    recomputeHardwareConvergence()
    if (_activeProjectId.value != null) {
      _hasUnsavedChanges.value = true
    }
  }

  fun applySidebarAssetToEngine(asset: BuiltInAssetItem) {
    val match = findPokemonByDex(asset.spriteSeedId)
    _selectedPokemon.value = match
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
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    _statusBannerMessage.value = "Applied [${asset.category.label}] ${asset.name} (${asset.consoleLineage})"
  }

  fun importFromNationalPokeApi(query: String) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _isSyncingPokeApi.value = true
      val result = localDexRepository.fetchAndImportFromPokeApi(query)
      _isSyncingPokeApi.value = false
      result.onSuccess { entity ->
        _selectedPokemon.value = entity
        markAtlasSeenOrBattled(entity.dexNumber, seen = true, battled = false)
        playPokemonCry(entity)
        _statusBannerMessage.value = "Hydrated #${entity.dexNumber} ${entity.name} from PokéAPI into Room!"
      }.onFailure { err ->
        _statusBannerMessage.value = "National Dex lookup notice: ${err.message}"
      }
    }
  }

  fun saveLocalCustomPokemon(entity: NativePokemonEntity, alsoPinToProjectVault: Boolean) {
    viewModelScope.launch {
      localDexRepository.upsertLocalPokemon(entity.copy(isUserModified = true))
      _selectedPokemon.value = entity
      if (alsoPinToProjectVault) {
        val vaultEntry = CustomDexEntry(
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
          projectId = activePersistenceProjectKey(),
        )
        localProjectRepository.addCustomDexEntry(vaultEntry)
        audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
        _statusBannerMessage.value = "Saved ${entity.name} to Local Room Atlas & Project Dex Vault!"
      } else {
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
        _statusBannerMessage.value = "Saved ${entity.name} to Resonance Atlas Database!"
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
    syncLocalHardwareFromProjectToken(project)
    viewModelScope.launch(Dispatchers.IO) {
      val saved = persistenceDao.getCampaignState(project.id)
      if (saved != null) {
        withContext(Dispatchers.Main) {
          applyCampaignEntityToState(saved, _hardwareRegistryState.value.allPlatforms)
        }
      }
      loadMapForRegion(project.id, _activeMapRegionId.value)
    }
    _hasUnsavedChanges.value = false
    if (announce) {
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Switched active project to '${project.title}' (${project.versionTag})"
    }
  }

  private fun syncLocalHardwareFromProjectToken(project: FanGameProject) {
    val hw = _hardwareRegistryState.value
    val catalogIds = hw.allPlatforms.map { it.id }.toSet()
    val (mode, decodedIds) = UniversalHardwareRegistry.decodeSelectionToken(project.activeChipsCsv, catalogIds)
    val remotePreset = project.consoleMatrixPreset.ifBlank { project.title }
    val remoteBlend = (project.pixel3dBlendRatio / 100f).coerceIn(0f, 1f)
    _fusionConfig.update {
      it.copy(
        presetName = remotePreset,
        pixelTo3dBlend = remoteBlend,
        activeChipIds = decodedIds,
      )
    }
    if (hw.allPlatforms.isNotEmpty()) {
      recomputeHardwareConvergence(
        activeIds = decodedIds,
        disciplineMode = mode,
        runtimePresetId = deriveRuntimePresetId(decodedIds, hw.allPlatforms, mode),
        runtimeLabel = deriveRuntimeLabel(decodedIds, hw.allPlatforms, mode),
      )
    }
  }

  private fun encodeCurrentHardwareToken(): String {
    val hw = _hardwareRegistryState.value
    return UniversalHardwareRegistry.encodeSelectionToken(
      activeIds = hw.activePlatformIds,
      allCatalogIds = hw.allPlatforms.map { it.id }.toSet(),
      disciplineMode = hw.disciplineMode,
    )
  }

  fun saveActiveProjectChanges() {
    val current = activeProject.value
    if (current == null || current.id.isBlank()) {
      _statusBannerMessage.value = "Create or select a local project first to save engine changes."
      return
    }
    viewModelScope.launch {
      _isSavingProject.value = true
      val cfg = _fusionConfig.value
      val updated = current.copy(
        consoleMatrixPreset = cfg.presetName.ifBlank { current.consoleMatrixPreset },
        activeChipsCsv = encodeCurrentHardwareToken(),
        pixel3dBlendRatio = (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong(),
      )
      localProjectRepository.updateProject(updated)
      _isSavingProject.value = false
      _hasUnsavedChanges.value = false
      persistCampaignStateToRoom()
      audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
      _statusBannerMessage.value = "Saved '${updated.title}' engine build to Local Room Vault!"
    }
  }

  /**
   * Generates a complete v4.0 portable JSON bundle for SAF CreateDocument("application/json").
   */
  suspend fun exportActiveProjectJson(): Result<String> {
    val projectId = activeProject.value?.id ?: activePersistenceProjectKey()
    persistCampaignStateToRoom()
    return localProjectRepository.exportProjectToJson(projectId)
  }

  /**
   * Imports a portable v4.0 JSON project bundle via SAF OpenDocument() into Room SQLite
   * and immediately activates it.
   */
  fun importProjectFromJson(jsonText: String) {
    viewModelScope.launch {
      _isSavingProject.value = true
      val res = localProjectRepository.importProjectFromJson(jsonText)
      _isSavingProject.value = false
      res.onSuccess { imported ->
        applyProjectToEngine(imported, announce = false)
        audioEngine.playSfx(StudioSfxType.LEVEL_UP)
        _statusBannerMessage.value = "Imported '${imported.title}' (${imported.versionTag}) into Local Room Vault!"
      }.onFailure { err ->
        audioEngine.playSfx(StudioSfxType.UI_CANCEL)
        _statusBannerMessage.value = "JSON import failed: ${err.localizedMessage ?: "Invalid project bundle"}"
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
          encodeCurrentHardwareToken()
        } else {
          existing.activeChipsCsv
        },
        pixel3dBlendRatio = if (captureCurrentEngineSettings) {
          (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong()
        } else {
          existing.pixel3dBlendRatio
        },
        dexCount = dexCount.coerceIn(1, 5000).toLong(),
        mapCount = mapCount.coerceIn(1, 100).toLong(),
        versionTag = versionTag.trim().ifEmpty { existing.versionTag },
      )
      val saved = localProjectRepository.updateProject(updated)
      _isSavingProject.value = false
      if (saved.id == _activeProjectId.value) {
        applyProjectToEngine(saved, announce = false)
      }
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Updated '${saved.title}' in Local Room Vault!"
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
        consolePreset.ifBlank { "Ultimate Target Style — All 156 Hardware" }
      }
      val project = FanGameProject(
        title = title,
        regionName = regionName,
        synopsis = synopsis,
        consoleMatrixPreset = resolvedPreset,
        activeChipsCsv = if (captureCurrentEngineSettings) {
          encodeCurrentHardwareToken()
        } else {
          MasterCanonTemplateCatalog.TEMPLATE_CHIPS_CSV
        },
        pixel3dBlendRatio = if (captureCurrentEngineSettings) {
          (cfg.pixelTo3dBlend * 100).toInt().coerceIn(0, 100).toLong()
        } else {
          65L
        },
        dexCount = dexCount.coerceIn(1, 5000).toLong(),
        mapCount = mapCount.coerceIn(1, 100).toLong(),
        versionTag = versionTag,
      )
      val created = localProjectRepository.createProject(project)
      _isSavingProject.value = false
      _activeProjectId.value = created.id
      _fusionConfig.update { it.copy(presetName = resolvedPreset) }
      _hasUnsavedChanges.value = false
      hasAutoLoadedInitialProject = true
      audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
      _statusBannerMessage.value = "Created & activated '${created.title}' in Local Room Vault!"
    }
  }

  fun duplicateCloudFanGame(source: FanGameProject) {
    viewModelScope.launch {
      _isSavingProject.value = true
      val copyTitle = "${source.title.take(70)} (Copy)"
      val duplicate = source.copy(id = "", title = copyTitle, createdAtMs = 0L, updatedAtMs = 0L)
      val created = localProjectRepository.createProject(duplicate)
      _isSavingProject.value = false
      applyProjectToEngine(created, announce = false)
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Duplicated '${source.title}' as '${created.title}'!"
    }
  }

  fun provisionMasterCanonBaseProject() {
    viewModelScope.launch {
      _isSavingProject.value = true
      val templateProject = MasterCanonTemplateCatalog.createBaseProjectTemplate()
      val created = localProjectRepository.createProject(templateProject)
      _isSavingProject.value = false
      applyProjectToEngine(created, announce = false)
      hasAutoLoadedInitialProject = true
      audioEngine.playSfx(StudioSfxType.LEVEL_UP)
      _statusBannerMessage.value = "Provisioned '${created.title}' (14 Custom 16x16 Regions) to Local Room Vault!"
    }
  }

  fun seedStarterCloudFanGames() {
    viewModelScope.launch {
      _isSavingProject.value = true
      val starters = listOf(
        MasterCanonTemplateCatalog.createBaseProjectTemplate(),
        FanGameProject(
          title = "Pokémon Chrono Quartz",
          regionName = "Aethelgard Sector",
          synopsis = "Time-rifting RPG fusing GBC Hi-Color sprites, GBA Mode-7 sky islands, N64 Stadium lighting, and Switch HD-2D bloom.",
          consoleMatrixPreset = "Ultimate Target Style — All 156 Hardware",
          activeChipsCsv = MasterCanonTemplateCatalog.TEMPLATE_CHIPS_CSV,
          pixel3dBlendRatio = 68L,
          dexCount = 1025L,
          mapCount = 14L,
          versionTag = "v4.0.0-PRISM",
        ),
      )
      var firstCreated: FanGameProject? = null
      starters.forEachIndexed { index, starter ->
        val res = localProjectRepository.createProject(starter)
        if (index == 0) firstCreated = res
      }
      _isSavingProject.value = false
      firstCreated?.let { applyProjectToEngine(it, announce = false) }
      audioEngine.playSfx(StudioSfxType.LEVEL_UP)
      _statusBannerMessage.value = "Provisioned 2 flagship multi-console fan game projects to Local Room Vault!"
    }
  }

  fun deleteCloudFanGame(gameId: String) {
    viewModelScope.launch {
      localProjectRepository.deleteProject(gameId)
      if (_activeProjectId.value == gameId) {
        _activeProjectId.value = null
        _hasUnsavedChanges.value = false
      }
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      _statusBannerMessage.value = "Removed project from Local Room Vault."
    }
  }

  fun deleteCustomDexEntry(entryId: String) {
    viewModelScope.launch {
      localProjectRepository.deleteCustomDexEntry(entryId)
      _statusBannerMessage.value = "Removed custom species from Local Room Vault."
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
        chipAddon = _hardwareRegistryState.value.convergedVisuals.signatureSummary.take(40),
        spriteSeedId = pkmn.dexNumber.toLong(),
        shaderFormula = "BLEND:${(cfg.pixelTo3dBlend * 100).toInt()}|VOX:${(cfg.voxelExtrusionDepth * 100).toInt()}|CRT:${(cfg.crtScanlineIntensity * 100).toInt()}",
        projectId = activePersistenceProjectKey(),
      )
      localProjectRepository.addStudioAsset(asset)
      _selectedSidebarCategory.value = category
      audioEngine.playSfx(StudioSfxType.ITEM_USE)
      _statusBannerMessage.value = "Pinned '${asset.name}' to [${category.label}] in Local Room Vault!"
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
      if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Applied PPU palette ($primaryHex / $secondaryHex) to #${updated.dexNumber} ${updated.name}!"
    }
  }

  fun deleteCustomSidebarAsset(assetId: String) {
    viewModelScope.launch {
      localProjectRepository.deleteStudioAsset(assetId)
    }
  }

  fun notifyStatusBanner(message: String) {
    _statusBannerMessage.value = message
  }

  companion object {
    const val DEFAULT_LOCAL_PROJECT_KEY = LocalStudioProjectRepository.DEFAULT_MASTER_PROJECT_ID
    val builtInSidebarAssets: List<BuiltInAssetItem> = StudioViewModelHelpers.builtInSidebarAssets
  }
}

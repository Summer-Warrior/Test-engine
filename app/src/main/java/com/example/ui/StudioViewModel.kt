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
import com.example.data.repository.PokeFusionCloudRepository
import com.example.engine.AudioConvergenceSolver
import com.example.engine.BattleGimmickMode
import com.example.engine.BattleMoveSpec
import com.example.engine.BattleTurnField
import com.example.engine.BgmThemeId
import com.example.engine.CampaignProgressionState
import com.example.engine.CanonBattleResolver
import com.example.engine.CanonRegionChapter
import com.example.engine.HardwareConvergenceSolver
import com.example.engine.MasterCanonTemplateCatalog
import com.example.engine.OmniHardwareAudioEngine
import com.example.engine.PokemonType
import com.example.engine.SpritePixelMatrixEngine
import com.example.engine.StatusCondition
import com.example.engine.StudioSfxType
import com.example.engine.TerrainCondition
import com.example.engine.UniversalGameFeelEngine
import com.example.engine.UniversalHardwareRegistry
import com.example.engine.WeatherCondition
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

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
  val lastModified: Timestamp?,
  val createdAt: Timestamp?,
  val globalSharedAssetsCount: Int = 0,
)

data class StudioSyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncedAtMs: Long? = null,
  val statusLabel: String = "Pre-warming Studio...",
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
  private val appContext: Context,
  private val cloudRepository: PokeFusionCloudRepository,
  private val localDexRepository: NativeFranchiseCatalogRepository,
  private val encyclopediaAndResourceRepository: FranchiseEncyclopediaAndResourceRepository,
  private val persistenceDao: StudioPersistenceDao,
  private val currentUserId: String,
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

  // Hoisted Hardware Registry & Convergence state (85+ platforms)
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

  // Hoisted Map Editor state (per project + per 14 regions, persisted to Room saved_maps)
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

  val cloudFanGamesState: StateFlow<UiState<List<FanGameProject>>> =
    cloudRepository.observeFanGames(currentUserId)
      .map<List<FanGameProject>, UiState<List<FanGameProject>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load cloud fan games"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val activeProject: StateFlow<FanGameProject?> =
    combine(cloudFanGamesState, _activeProjectId) { gamesState, activeId ->
      val projects = (gamesState as? UiState.Success)?.data ?: return@combine null
      if (activeId != null) projects.firstOrNull { it.id == activeId } else null
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
        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load custom Pokedex entries"))
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
        emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load custom studio assets"))
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

            val candidateTimestamps = buildList {
              liveProject.updatedAt?.let { add(it) }
              liveProject.createdAt?.let { add(it) }
              projectAssets.mapNotNull { it.updatedAt ?: it.createdAt }.maxByOrNull { it.seconds }?.let { add(it) }
              projectDex.mapNotNull { it.updatedAt ?: it.createdAt }.maxByOrNull { it.seconds }?.let { add(it) }
            }
            val latestModified = candidateTimestamps.maxByOrNull { it.seconds * 1_000_000_000L + it.nanoseconds }

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
              lastModified = latestModified,
              createdAt = liveProject.createdAt,
              globalSharedAssetsCount = globalAssets.size,
            )
          }
          UiState.Success(overview) as UiState<ProjectOverviewStats?>
        }.catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load real-time project statistics"))
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  init {
    prewarmStudioStartup()
    syncStudioDataOnLoad(announceCompletion = false, refreshServer = false)

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
          syncLocalHardwareFromProjectToken(latestProject)
        }
      }
    }
  }

  /**
   * Zero-Wait Startup Pre-warming: loads 87-platform Hardware Registry into Room, seeds Items & 14 Regional Maps,
   * restores saved Campaign State, and pre-warms Coil image cache for starter party sprites.
   */
  private fun prewarmStudioStartup() {
    viewModelScope.launch(Dispatchers.IO) {
      // 1. Load & persist 85+ hardware profiles
      val specs = UniversalHardwareRegistry.loadFromAssets(appContext)
      if (persistenceDao.countHardwareProfiles() < specs.size) {
        persistenceDao.upsertHardwareProfiles(specs.map { StudioViewModelHelpers.toHardwareProfileEntity(it) })
      }
      withContext(Dispatchers.Main) {
        recomputeHardwareConvergence(allPlatforms = specs)
      }

      // 2. Seed items catalog if empty
      if (persistenceDao.countItems() == 0) {
        persistenceDao.upsertItems(StudioItemCatalog.defaultItems)
      }

      // 3. Restore campaign state & active regional map for current project key
      val projectKey = activePersistenceProjectKey()
      val savedCampaign = persistenceDao.getCampaignState(projectKey)
      if (savedCampaign != null) {
        applyCampaignEntityToState(savedCampaign, specs)
      } else {
        persistenceDao.upsertCampaignState(CampaignStateEntity(projectId = projectKey))
      }

      // 4. Seed 14 default maps if not yet saved
      val defaultMaps = MasterCanonTemplateCatalog.worldChapters.mapNotNull { ch ->
        val key = mapStorageKey(projectKey, ch.id)
        if (persistenceDao.getSavedMap(key) == null) {
          SavedMapEntity(
            mapKey = key,
            projectId = projectKey,
            regionId = ch.id,
            regionName = ch.regionName,
            gridSize = 8,
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

      // 5. Warm Coil singleton cache for core party & starter sprites (zero loading stutter in Playtest)
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
    _activeProjectId.value?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_PROJECT_KEY

  private fun mapStorageKey(projectKey: String, regionId: String): String = "$projectKey:$regionId"

  private suspend fun loadMapForRegion(projectKey: String, regionId: String) {
    val saved = persistenceDao.getSavedMap(mapStorageKey(projectKey, regionId))
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val tiles = saved?.tilesCsv
      ?.split(",")
      ?.mapNotNull { it.trim().toIntOrNull() }
      ?.takeIf { it.size == 64 }
      ?: chapter.defaultMapTiles
    _mapGrid.value = tiles
  }

  private fun applyCampaignEntityToState(
    entity: CampaignStateEntity,
    allSpecs: List<com.example.engine.HardwarePlatformSpec>,
  ) {
    val completed = entity.completedRegionIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val party = entity.partyDexIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(25, 6, 94, 448, 658, 384) }
    val heldMap = StudioViewModelHelpers.parseHeldItemsCsv(entity.heldItemsMapCsv)
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
    _gameTestState.update {
      it.copy(
        activeRegionId = regionId,
        playerCol = entity.playerX.coerceIn(0, 7),
        playerRow = entity.playerY.coerceIn(0, 7),
        activePartyDexIds = party,
        heldItemsMap = if (heldMap.isNotEmpty()) heldMap else it.heldItemsMap,
        weather = chapter.weatherPreset,
      )
    }

    val discipline = MasterDisciplineMode.entries.firstOrNull { it.token == entity.disciplineModeToken }
      ?: MasterDisciplineMode.SHOVEL_KNIGHT_CURATED
    val extraPretty = StudioViewModelHelpers.decodeExtraPrettyMask(entity.extraPrettyMask)
    val hwIds = entity.activeHardwareIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
      .ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds }
    recomputeHardwareConvergence(
      allPlatforms = allSpecs,
      activeIds = hwIds,
      disciplineMode = discipline,
      extraPretty = extraPretty,
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
    )
    viewModelScope.launch(Dispatchers.IO) {
      persistenceDao.upsertCampaignState(entity)
    }
  }

  /**
   * Lifecycle hooks:
   * - ON_STOP cleanly stops active BGM audio track so sound never leaks in background.
   * - No redundant Source.SERVER Firestore reads on ON_RESUME (real-time .snapshots() handles live cloud updates).
   */
  fun onAppStopped() {
    audioEngine.stopBgmImmediate()
    _audioConsoleState.update { it.copy(isBgmPlaying = false) }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.releaseAll()
  }

  fun refreshStudioNow() {
    syncStudioDataOnLoad(announceCompletion = true, refreshServer = true)
  }

  private fun syncStudioDataOnLoad(announceCompletion: Boolean, refreshServer: Boolean) {
    viewModelScope.launch {
      _syncStatus.update { it.copy(isSyncing = true, statusLabel = "Syncing live data...") }
      _isSyncingSources.value = true

      val localDeferred = async { runCatching { localDexRepository.ensureSeeded() }.getOrDefault(0) }
      val encyclopediaDeferred = async {
        runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      }
      val cloudDeferred = if (refreshServer) {
        async { cloudRepository.refreshFromServer(currentUserId) }
      } else {
        null
      }
      val primaryRefreshDeferred = async {
        localDexRepository.refreshLivePokemonIfOnline(_selectedPokemon.value.dexNumber)
      }

      val updatedLocalCount = localDeferred.await()
      val webSourcesSynced = encyclopediaDeferred.await()
      val cloudResult = cloudDeferred?.await()
      val apiResult = primaryRefreshDeferred.await()
      _isSyncingSources.value = false

      val finishedAt = System.currentTimeMillis()
      val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(finishedAt))
      val statusText = when {
        webSourcesSynced > 0 -> "Up to date ($webSourcesSynced/3 DBs) • $timeStr"
        apiResult.isSuccess || cloudResult?.isSuccess == true -> "Up to date • $timeStr"
        else -> "Local synced • $timeStr"
      }

      _syncStatus.value = StudioSyncStatus(
        isSyncing = false,
        lastSyncedAtMs = finishedAt,
        statusLabel = statusText,
      )

      if (announceCompletion) {
        val cloudDocs = cloudResult?.getOrNull() ?: 0
        _statusBannerMessage.value =
          "Synced ($timeStr): $webSourcesSynced/3 DBs, $cloudDocs cloud docs & $updatedLocalCount Dex updates."
      }
    }
  }

  fun syncEncyclopediaSourcesNow() {
    viewModelScope.launch {
      _isSyncingSources.value = true
      val count = runCatching { encyclopediaAndResourceRepository.syncEncyclopediaSourcesOnOpen() }.getOrDefault(0)
      _isSyncingSources.value = false
      _statusBannerMessage.value = "Updated $count/3 canonical databases."
    }
  }

  // =========================================================================
  // PHASE 2 & 3: UNIVERSAL HARDWARE REGISTRY + CONVERGENCE + EXTRA PRETTY
  // =========================================================================

  private fun recomputeHardwareConvergence(
    allPlatforms: List<com.example.engine.HardwarePlatformSpec> = _hardwareRegistryState.value.allPlatforms,
    activeIds: Set<String> = _hardwareRegistryState.value.activePlatformIds,
    disciplineMode: MasterDisciplineMode = _hardwareRegistryState.value.disciplineMode,
    characterMixMode: CharacterMixMode = _hardwareRegistryState.value.characterMixMode,
    extraPretty: ExtraVisualAudioSettings = _hardwareRegistryState.value.extraPretty,
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
      convergedVisuals = visuals,
      convergedAudio = audio,
    )
    audioEngine.updateAudioParams(audio)
  }

  fun toggleHardwarePlatform(platformId: String) {
    val current = _hardwareRegistryState.value
    val updated = if (platformId in current.activePlatformIds) {
      if (current.activePlatformIds.size > 1) current.activePlatformIds - platformId else current.activePlatformIds
    } else {
      current.activePlatformIds + platformId
    }
    recomputeHardwareConvergence(activeIds = updated)
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
  }

  fun toggleAllHardwareOnRecord(enableAll: Boolean) {
    val current = _hardwareRegistryState.value
    val targetIds = if (enableAll) {
      current.allPlatforms.map { it.id }.toSet().ifEmpty { UniversalHardwareRegistry.defaultCuratedHardwareIds }
    } else {
      UniversalHardwareRegistry.defaultCuratedHardwareIds
    }
    recomputeHardwareConvergence(activeIds = targetIds)
    audioEngine.playSfx(if (enableAll) StudioSfxType.LEVEL_UP else StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
    _statusBannerMessage.value = if (enableAll) {
      "Enabled ALL ${targetIds.size} Gaming Hardware Platforms on Record via Peak-Preserving Convergence!"
    } else {
      "Restored Shovel Knight+ Curated ${targetIds.size}-platform hardware stack."
    }
  }

  fun setMasterDisciplineMode(mode: MasterDisciplineMode) {
    recomputeHardwareConvergence(disciplineMode = mode)
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
    persistCampaignStateToRoom()
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
  // PHASE 4: PURE-KOTLIN OMNI-HARDWARE AUDIO CONSOLE
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
  // PHASE 1: HOISTED SPRITE EDITOR + REAL ROOM/CLOUD PERSISTENCE
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
    updateSpriteStateWithBitmap { it.copy(paletteArgb = preset) }
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

  fun importBitmapIntoSpriteEditor(bitmap: Bitmap, nameHint: String? = null) {
    val pkmn = _selectedPokemon.value
    val extractedPalette = SpritePixelMatrixEngine.extractPaletteFromBitmap(bitmap, pkmn.primaryHex, pkmn.secondaryHex)
    val quantized = SpritePixelMatrixEngine.quantizeBitmapToIndexedGrid(bitmap, extractedPalette)
    updateSpriteStateWithBitmap { s ->
      val newFrames = s.frames.toMutableList().also { it[s.activeFrameIndex] = quantized }
      s.copy(
        spriteName = nameHint ?: s.spriteName,
        paletteArgb = extractedPalette,
        frames = newFrames,
        undoStack = (s.undoStack + listOf(s.currentFrame)).takeLast(24),
        useCustomBitmapInViewport = true,
      )
    }
    audioEngine.playSfx(StudioSfxType.ITEM_USE)
    _statusBannerMessage.value = "Quantized imported asset into 16x16 PPU pixel frame & 8-color palette!"
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
    val projectKey = _activeProjectId.value.orEmpty()
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

      val compactPayload = "RLE_SPRITE|ID:$spriteId|PAL:$primaryHex,$secondaryHex|F:${s.frames.size}|${SpritePixelMatrixEngine.encodeFrameRle(s.currentFrame)}".take(950)
      val cloudAsset = CustomStudioAsset(
        name = cleanName,
        category = AssetCategory.SPRITES.label,
        consoleLineage = _selectedPokemon.value.consoleOriginSystem,
        chipAddon = "PPU 16x16 Voxel Rig",
        spriteSeedId = s.seedDexNumber.toLong(),
        shaderFormula = compactPayload,
        projectId = projectKey,
      )
      cloudRepository.createCustomAsset(cloudAsset)
      _isSavingProject.value = false
      audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
      _statusBannerMessage.value = "Saved '$cleanName' (${s.frames.size} frames) to Room & Cloud! Active in Viewport & Game Test."
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
  // PHASE 1 & 5: PER-PROJECT PER-REGION MAP EDITOR + GAME TEST ENGINE
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
    if (index !in 0..63) return
    val brush = _selectedBrushTile.value
    val updated = _mapGrid.value.toMutableList().also { it[index] = brush }
    _mapGrid.value = updated
    persistActiveMapToRoom(updated)
  }

  fun resetActiveRegionMapToDefault() {
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == _activeMapRegionId.value }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    _mapGrid.value = chapter.defaultMapTiles
    persistActiveMapToRoom(chapter.defaultMapTiles)
    audioEngine.playSfx(StudioSfxType.UI_CANCEL)
    _statusBannerMessage.value = "Restored ${chapter.regionName} default 8x8 tilemap."
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
          gridSize = 8,
          tilesCsv = tiles.joinToString(","),
          updatedAtEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }

  fun selectMapBrush(tileType: Int) {
    _selectedBrushTile.value = tileType.coerceIn(0, 5)
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
        playerRow = 5,
        playerCol = 2,
        inBattle = false,
        weather = chapter.weatherPreset,
        transitionPulseNonce = s.transitionPulseNonce + 1L,
        battleLog = (listOf("Traveled to ${chapter.regionName}: ${chapter.storyLogEntry}") + s.battleLog).take(14),
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

  fun movePlayerOnOverworld(dRow: Int, dCol: Int) {
    val gt = _gameTestState.value
    if (gt.inBattle) return
    val tiles = _mapGrid.value
    val moveDir = when {
      dRow < 0 -> com.example.engine.MoveDirection.UP
      dRow > 0 -> com.example.engine.MoveDirection.DOWN
      dCol < 0 -> com.example.engine.MoveDirection.LEFT
      else -> com.example.engine.MoveDirection.RIGHT
    }
    val (newCol, newRow) = UniversalGameFeelEngine.resolveStepWithCornerCorrection(
      x = gt.playerCol,
      y = gt.playerRow,
      direction = moveDir,
      gridSize = 8,
      tiles = tiles,
    )
    if (newRow == gt.playerRow && newCol == gt.playerCol) {
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      return
    }
    val landedTile = tiles.getOrElse(newRow * 8 + newCol) { 0 }
    when (landedTile) {
      1 -> audioEngine.playSfx(StudioSfxType.GRASS_RUSTLE)
      5 -> audioEngine.playSfx(StudioSfxType.STEP_CRYSTAL)
      3 -> audioEngine.playSfx(StudioSfxType.STEP_STONE)
      else -> audioEngine.playSfx(StudioSfxType.STEP_GRASS)
    }

    val newSteps = gt.stepsTaken + 1
    _gameTestState.update {
      it.copy(
        playerRow = newRow,
        playerCol = newCol,
        stepsTaken = newSteps,
      )
    }
    persistCampaignStateToRoom()

    if (landedTile == 5) {
      triggerManualEncounter(isBoss = true)
    } else if (landedTile == 1 && newSteps % 2 == 0) {
      triggerManualEncounter(isBoss = false)
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
    val enemyEntity = findPokemonByDex(enemyDex)
    val enemyLevel = if (isBoss) 55 else 46
    val enemySnap = StudioViewModelHelpers.buildCombatantFromEntity(enemyEntity, level = enemyLevel)
    val leadEntity = findPokemonByDex(gt.activeLeadDexId)
    val leadSnap = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = leadEntity,
      level = 50,
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

    val titlePrefix = if (isBoss) "Prism Sanctuary Guardian" else "Wild"
    _gameTestState.update {
      it.copy(
        inBattle = true,
        turnNumber = 1,
        partyCurrentHpMap = updatedHpMap,
        enemyDexId = enemyDex,
        enemyLevel = enemyLevel,
        enemyCurrentHp = enemySnap.maxHp(),
        enemyMaxHp = enemySnap.maxHp(),
        enemyStatus = StatusCondition.HEALTHY,
        isBossEncounter = isBoss,
        activeGimmick = BattleGimmickMode.NONE,
        weather = chapter.weatherPreset,
        transitionPulseNonce = it.transitionPulseNonce + 1L,
        battleLog = (listOf("$titlePrefix ${enemyEntity.name} (Lv.$enemyLevel) materialized in ${chapter.regionName}!") + it.battleLog).take(14),
      )
    }
  }

  /**
   * Bug-fixed Dynamax & Gimmick switcher:
   * Scales current HP proportionally via CanonBattleResolver.scaleHpProportionally instead of resetting to full HP!
   */
  fun toggleGameTestGimmick(targetGimmick: BattleGimmickMode) {
    val gt = _gameTestState.value
    val nextGimmick = if (gt.activeGimmick == targetGimmick) BattleGimmickMode.NONE else targetGimmick
    val lead = findPokemonByDex(gt.activeLeadDexId)
    val held = gt.heldItemsMap[lead.dexNumber].orEmpty()
    val oldMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
      lead, level = 50, gimmick = gt.activeGimmick, teraType = gt.teraType, heldItemId = held,
    ).maxHp()
    val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(
      lead, level = 50, gimmick = nextGimmick, teraType = gt.teraType, heldItemId = held,
    ).maxHp()
    val currentHp = gt.partyCurrentHpMap[lead.dexNumber] ?: oldMaxHp
    val scaledHp = CanonBattleResolver.scaleHpProportionally(currentHp, oldMaxHp, newMaxHp)

    audioEngine.playSfx(if (nextGimmick == BattleGimmickMode.NONE) StudioSfxType.UI_CANCEL else StudioSfxType.STAT_BOOST)
    _gameTestState.update {
      it.copy(
        activeGimmick = nextGimmick,
        partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to scaledHp),
        battleLog = (listOf("${lead.name} shifted resonance to ${nextGimmick.displayName} (HP scaled proportionally: $scaledHp/$newMaxHp).") + it.battleLog).take(14),
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
    val newMaxHp = StudioViewModelHelpers.buildCombatantFromEntity(newLead, level = 50).maxHp()
    val existingHp = gt.partyCurrentHpMap[newLead.dexNumber] ?: newMaxHp
    audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
    playPokemonCry(newLead)
    _gameTestState.update {
      it.copy(
        activePartyIndex = index,
        activeGimmick = BattleGimmickMode.NONE,
        partyCurrentHpMap = it.partyCurrentHpMap + (newLead.dexNumber to existingHp.coerceIn(1, newMaxHp)),
        battleLog = (listOf("Go! ${newLead.name} entered the Resonance field!") + it.battleLog).take(14),
      )
    }
  }

  fun executePlayerBattleMove(move: BattleMoveSpec) {
    val gt = _gameTestState.value
    if (!gt.inBattle) return

    val playerEntity = findPokemonByDex(gt.activeLeadDexId)
    val enemyEntity = findPokemonByDex(gt.enemyDexId)
    val playerHeld = gt.heldItemsMap[playerEntity.dexNumber].orEmpty()
    val playerStatus = gt.partyStatusMap[playerEntity.dexNumber] ?: StatusCondition.HEALTHY

    val playerSnap = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = playerEntity,
      level = 50,
      gimmick = gt.activeGimmick,
      teraType = gt.teraType,
      status = playerStatus,
      heldItemId = playerHeld,
    )
    val enemySnap = StudioViewModelHelpers.buildCombatantFromEntity(
      entity = enemyEntity,
      level = gt.enemyLevel,
      status = gt.enemyStatus,
    )
    val playerMaxHp = playerSnap.maxHp()
    var playerHp = (gt.partyCurrentHpMap[playerEntity.dexNumber] ?: playerMaxHp).coerceIn(1, playerMaxHp)
    var enemyHp = gt.enemyCurrentHp.coerceIn(1, gt.enemyMaxHp)

    val field = BattleTurnField(
      weather = gt.weather,
      terrain = gt.terrain,
      speedTieSeed = gt.turnNumber + battleActionCounter++,
    )

    val playerResult = CanonBattleResolver.resolveTurn(
      attacker = playerSnap.copy(currentHpPercent = (playerHp * 100 / playerMaxHp).coerceIn(1, 100)),
      defender = enemySnap.copy(currentHpPercent = (enemyHp * 100 / gt.enemyMaxHp.coerceAtLeast(1)).coerceIn(1, 100)),
      move = move,
      field = field,
    )

    val dealt = ((playerResult.minDamage + playerResult.maxDamage) / 2).coerceAtLeast(if (playerResult.typeEffectiveness == 0f) 0 else 1)
    enemyHp = (enemyHp - dealt).coerceAtLeast(0)
    playerHp = (playerHp - playerResult.lifeOrbRecoil).coerceAtLeast(if (playerHp > 1) 1 else 0)

    val severity = when {
      playerResult.typeEffectiveness >= 2f -> 2
      playerResult.typeEffectiveness == 0f || playerResult.typeEffectiveness < 1f -> 3
      else -> 1
    }
    audioEngine.playSfx(
      when (severity) {
        2 -> StudioSfxType.HIT_HEAVY_CRIT
        3 -> StudioSfxType.HIT_RESISTED
        else -> StudioSfxType.HIT_LIGHT
      }
    )

    val logs = mutableListOf<String>()
    val effTag = when {
      playerResult.typeEffectiveness == 0f -> "No effect!"
      playerResult.typeEffectiveness >= 2f -> "Super Effective (${playerResult.typeEffectiveness}x)!"
      playerResult.typeEffectiveness < 1f -> "Resisted (${playerResult.typeEffectiveness}x)."
      else -> "Clean hit."
    }
    logs.add("${playerEntity.name} used ${move.name} [${playerResult.resolvedMoveType.displayName}] for $dealt dmg! $effTag")
    if (playerResult.lifeOrbRecoil > 0) {
      logs.add("${playerEntity.name} took ${playerResult.lifeOrbRecoil} Life Orb recoil.")
    }

    markAtlasSeenOrBattled(enemyEntity.dexNumber, seen = true, battled = true)

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
          partyCurrentHpMap = it.partyCurrentHpMap + (playerEntity.dexNumber to playerHp),
          battleLog = (logs.reversed() + it.battleLog).take(14),
          lastEventNonce = it.lastEventNonce + 1L,
          lastHitSeverity = severity,
          lastAttackerWasPlayer = true,
        )
      }
      return
    }

    // Enemy counter-attack
    val enemyMoves = StudioViewModelHelpers.buildMovesetForPokemon(enemyEntity, StudioViewModelHelpers.parsePokemonType(enemyEntity.primaryType))
    val enemyMove = enemyMoves[gt.turnNumber % enemyMoves.size]
    val enemyResult = CanonBattleResolver.resolveTurn(
      attacker = enemySnap.copy(currentHpPercent = (enemyHp * 100 / gt.enemyMaxHp.coerceAtLeast(1)).coerceIn(1, 100)),
      defender = playerSnap.copy(currentHpPercent = (playerHp * 100 / playerMaxHp).coerceIn(1, 100)),
      move = enemyMove,
      field = field,
    )
    val enemyDealt = ((enemyResult.minDamage + enemyResult.maxDamage) / 2).coerceAtLeast(if (enemyResult.typeEffectiveness == 0f) 0 else 1)
    playerHp = (playerHp - enemyDealt).coerceAtLeast(if (enemyResult.focusSashTriggered) 1 else 0)
    logs.add("Foe ${enemyEntity.name} countered with ${enemyMove.name} for $enemyDealt dmg!")
    if (enemyResult.focusSashTriggered) {
      logs.add("${playerEntity.name} hung on with Focus Sash (1 HP)!")
    }

    // Leftovers end-of-turn recovery
    val leftoversHeal = CanonBattleResolver.computeEndOfTurnLeftoversHeal(playerSnap)
    if (leftoversHeal > 0 && playerHp in 1 until playerMaxHp) {
      playerHp = (playerHp + leftoversHeal).coerceAtMost(playerMaxHp)
      logs.add("${playerEntity.name} restored $leftoversHeal HP via Leftovers.")
    }

    val stillInBattle = if (playerHp <= 0) {
      logs.add("${playerEntity.name} fainted! Returned to Prism Lab healing bay.")
      playerHp = playerMaxHp
      false
    } else {
      true
    }

    _gameTestState.update {
      it.copy(
        inBattle = stillInBattle,
        turnNumber = it.turnNumber + 1,
        enemyCurrentHp = enemyHp,
        partyCurrentHpMap = it.partyCurrentHpMap + (playerEntity.dexNumber to playerHp),
        battleLog = (logs.reversed() + it.battleLog).take(14),
        lastEventNonce = it.lastEventNonce + 1L,
        lastHitSeverity = severity,
        lastAttackerWasPlayer = true,
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
    val landedTile = _mapGrid.value.getOrElse(gt.playerRow * 8 + gt.playerCol) { 0 }
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
          battleLog = (listOf("Gotcha! ${enemyEntity.name} was caught! ${result.summary}") + it.battleLog).take(14),
        )
      }
      persistCampaignStateToRoom()
    } else {
      audioEngine.playSfx(StudioSfxType.POKEBALL_SHAKE)
      _gameTestState.update {
        it.copy(
          turnNumber = it.turnNumber + 1,
          isBagOpen = false,
          battleLog = (listOf(result.summary) + it.battleLog).take(14),
        )
      }
    }
  }

  fun useBagItem(item: StudioItemEntity) {
    val gt = _gameTestState.value
    when (item.category) {
      StudioItemCatalog.CAT_POKE_BALL -> throwPokeBall(item)
      StudioItemCatalog.CAT_MEDICINE, StudioItemCatalog.CAT_BERRY -> {
        if (item.quantity <= 0) {
          _statusBannerMessage.value = "Out of ${item.name}!"
          return
        }
        val lead = findPokemonByDex(gt.activeLeadDexId)
        val maxHp = StudioViewModelHelpers.buildCombatantFromEntity(
          lead, level = 50, gimmick = gt.activeGimmick, heldItemId = gt.heldItemsMap[lead.dexNumber].orEmpty(),
        ).maxHp()
        val curHp = gt.partyCurrentHpMap[lead.dexNumber] ?: maxHp
        val healAmt = if (item.effectPower >= 999f) maxHp else item.effectPower.toInt().coerceAtLeast(20)
        val newHp = (curHp + healAmt).coerceAtMost(maxHp)
        viewModelScope.launch(Dispatchers.IO) {
          persistenceDao.upsertItem(item.copy(quantity = (item.quantity - 1).coerceAtLeast(0)))
        }
        audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
        _gameTestState.update {
          it.copy(
            partyCurrentHpMap = it.partyCurrentHpMap + (lead.dexNumber to newHp),
            partyStatusMap = it.partyStatusMap + (lead.dexNumber to StatusCondition.HEALTHY),
            isBagOpen = false,
            battleLog = (listOf("Used ${item.name} on ${lead.name} -> $newHp/$maxHp HP & cured status!") + it.battleLog).take(14),
          )
        }
      }
      StudioItemCatalog.CAT_HOLD_ITEM -> {
        val lead = findPokemonByDex(gt.activeLeadDexId)
        audioEngine.playSfx(StudioSfxType.ITEM_USE)
        _gameTestState.update {
          it.copy(
            heldItemsMap = it.heldItemsMap + (lead.dexNumber to item.id),
            isBagOpen = false,
            battleLog = (listOf("Equipped ${item.name} onto ${lead.name}! Active in battle calculations.") + it.battleLog).take(14),
          )
        }
        persistCampaignStateToRoom()
      }
      StudioItemCatalog.CAT_GIMMICK_CATALYST -> {
        val gimmick = when {
          item.id.contains("mega") -> BattleGimmickMode.MEGA_EVOLUTION
          item.id.contains("z_ring") -> BattleGimmickMode.Z_MOVE
          item.id.contains("dynamax") -> BattleGimmickMode.DYNAMAX
          item.id.contains("tera") -> BattleGimmickMode.TERASTALLIZATION
          else -> BattleGimmickMode.ANIME_BATTLE_BOND
        }
        toggleGameTestGimmick(gimmick)
        _gameTestState.update { it.copy(isBagOpen = false) }
      }
      else -> {
        audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
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
        battleLog = (listOf("Disengaged cleanly back to overworld.") + it.battleLog).take(14),
      )
    }
  }

  fun healFullParty() {
    audioEngine.playSfx(StudioSfxType.HEAL_CHIME)
    val gt = _gameTestState.value
    val restored = gt.activePartyDexIds.associateWith { dex ->
      val entity = findPokemonByDex(dex)
      StudioViewModelHelpers.buildCombatantFromEntity(entity, level = 50).maxHp()
    }
    _gameTestState.update {
      it.copy(
        partyCurrentHpMap = restored,
        partyStatusMap = emptyMap(),
        battleLog = (listOf("Prism Healing Pod restored full HP & status for all ${gt.activePartyDexIds.size} partners!") + it.battleLog).take(14),
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

  fun findPokemonByDex(dexNumber: Int): NativePokemonEntity {
    return nativePokemonList.value.firstOrNull { it.dexNumber == dexNumber }
      ?: NativeFranchiseCatalogRepository.builtInFranchiseDex.firstOrNull { it.dexNumber == dexNumber }
      ?: NativeFranchiseCatalogRepository.builtInFranchiseDex.first()
  }

  // =========================================================================
  // RESOURCE MANAGER (RACE-FREE ASSIGNMENT + AUDIO ENGINE PREVIEW)
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

  fun previewRoomResource(resource: StudioResourceEntity) {
    if (resource.categoryId == ResourceCategory.MAP_TILES.id) {
      if (resource.tileTypeIndex in 0..5) {
        _selectedBrushTile.value = resource.tileTypeIndex
      }
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Loaded Map Tile '${resource.name}' into HD-2D Tile Brush!"
    } else if (resource.categoryId == ResourceCategory.BGM.id) {
      val theme = when {
        resource.name.contains("Battle", ignoreCase = true) -> BgmThemeId.WILD_ENCOUNTER
        resource.name.contains("Champion", ignoreCase = true) -> BgmThemeId.CHAMPION_CONVERGENCE
        resource.name.contains("Route", ignoreCase = true) -> BgmThemeId.KANTO_JOHTO_ROUTE
        else -> BgmThemeId.PRISM_TITLE
      }
      playBgmTheme(theme)
      _statusBannerMessage.value = "Streaming 44.1kHz Omni-Hardware BGM: ${theme.displayName}"
    } else {
      val sfx = when {
        resource.name.contains("Crit", ignoreCase = true) -> StudioSfxType.HIT_HEAVY_CRIT
        resource.name.contains("Ball", ignoreCase = true) -> StudioSfxType.POKEBALL_THROW
        resource.name.contains("Heal", ignoreCase = true) -> StudioSfxType.HEAL_CHIME
        else -> StudioSfxType.LEVEL_UP
      }
      playSfx(sfx)
      _statusBannerMessage.value = "Synthesizing SFX preview: ${resource.name} (${resource.formatTag})"
    }
  }

  /**
   * Bug-fixed assignment: checks !isAssignedToProject BEFORE incrementing mapCount so duplicate drops
   * never double-increment, and decrements cleanly on unassign.
   */
  fun assignResourceToGameProject(resource: StudioResourceEntity, project: FanGameProject) {
    viewModelScope.launch {
      val alreadyAssigned = resource.isAssignedToProject(project.id)
      encyclopediaAndResourceRepository.assignResourceToProject(resource.id, project.id)
      if (!alreadyAssigned && project.id.isNotBlank() && resource.categoryId == ResourceCategory.MAP_TILES.id) {
        cloudRepository.updateFanGame(project.copy(mapCount = (project.mapCount + 1L).coerceAtMost(500L)))
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
        cloudRepository.updateFanGame(project.copy(mapCount = (project.mapCount - 1L).coerceAtLeast(0L)))
      }
      audioEngine.playSfx(StudioSfxType.UI_CANCEL)
      _statusBannerMessage.value = "Detached '${resource.name}' from '${project.title}'."
    }
  }

  // =========================================================================
  // NAVIGATION, DEX, ENGINE CONFIG & CLOUD PROJECT MANAGEMENT
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
          projectId = _activeProjectId.value.orEmpty(),
        )
        cloudRepository.createCustomDexEntry(cloudEntry)
          .onSuccess {
            audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
            _statusBannerMessage.value = "Saved ${entity.name} to Room & Cloud Dex!"
          }
          .onFailure { e ->
            _statusBannerMessage.value = "Saved locally; Cloud sync error: ${e.localizedMessage}"
          }
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
        applyCampaignEntityToState(saved, _hardwareRegistryState.value.allPlatforms)
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
      recomputeHardwareConvergence(activeIds = decodedIds, disciplineMode = mode)
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
      _statusBannerMessage.value = "Select or create a cloud project first to save engine changes."
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
      cloudRepository.updateFanGame(updated)
        .onSuccess {
          _isSavingProject.value = false
          _hasUnsavedChanges.value = false
          persistCampaignStateToRoom()
          audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
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
          encodeCurrentHardwareToken()
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
          audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
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
        consolePreset.ifBlank { "Shovel Knight+ Omni-Era Fusion" }
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
          audioEngine.playSfx(StudioSfxType.CAPTURE_SPARKLE)
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
      val duplicate = source.copy(id = "", title = copyTitle, createdAt = null, updatedAt = null)
      cloudRepository.createFanGame(duplicate)
        .onSuccess { newId ->
          _isSavingProject.value = false
          applyProjectToEngine(duplicate.copy(id = newId), announce = false)
          audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
          _statusBannerMessage.value = "Duplicated '${source.title}' as '$copyTitle'!"
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
          audioEngine.playSfx(StudioSfxType.LEVEL_UP)
          _statusBannerMessage.value = "Provisioned '${templateProject.title}' (14 Regions) to Firestore!"
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
        MasterCanonTemplateCatalog.createBaseProjectTemplate(),
        FanGameProject(
          title = "Pokémon Chrono Quartz",
          regionName = "Aethelgard Sector",
          synopsis = "Time-rifting RPG fusing GBC Hi-Color sprites, GBA Mode-7 sky islands, N64 Stadium lighting, and Switch HD-2D bloom.",
          consoleMatrixPreset = "GBC + GBA + N64 + Switch HD-2D",
          activeChipsCsv = MasterCanonTemplateCatalog.TEMPLATE_CHIPS_CSV,
          pixel3dBlendRatio = 68L,
          dexCount = 1025L,
          mapCount = 14L,
          versionTag = "v3.0.0-OMEGA",
        ),
      )
      var firstId: String? = null
      starters.forEachIndexed { index, starter ->
        val res = cloudRepository.createFanGame(starter)
        if (index == 0) firstId = res.getOrNull()
      }
      _isSavingProject.value = false
      if (firstId != null) {
        applyProjectToEngine(starters.first().copy(id = firstId!!), announce = false)
      }
      audioEngine.playSfx(StudioSfxType.LEVEL_UP)
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
          audioEngine.playSfx(StudioSfxType.UI_CANCEL)
          _statusBannerMessage.value = "Removed project from Cloud Firestore."
        }
        .onFailure { e ->
          _statusBannerMessage.value = "Delete failed: ${e.localizedMessage}"
        }
    }
  }

  fun deleteCustomDexEntry(entryId: String) {
    viewModelScope.launch {
      cloudRepository.deleteCustomDexEntry(entryId)
        .onSuccess { _statusBannerMessage.value = "Removed custom species from Cloud." }
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
        projectId = _activeProjectId.value.orEmpty(),
      )
      cloudRepository.createCustomAsset(asset)
        .onSuccess {
          _selectedSidebarCategory.value = category
          audioEngine.playSfx(StudioSfxType.ITEM_USE)
          _statusBannerMessage.value = "Pinned '${asset.name}' to [${category.label}] & synced to Cloud!"
        }
        .onFailure { e ->
          _statusBannerMessage.value = "Could not pin asset: ${e.localizedMessage}"
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
      if (_activeProjectId.value != null) _hasUnsavedChanges.value = true
      audioEngine.playSfx(StudioSfxType.UI_CONFIRM)
      _statusBannerMessage.value = "Applied PPU palette ($primaryHex / $secondaryHex) to #${updated.dexNumber} ${updated.name}!"
    }
  }

  fun deleteCustomSidebarAsset(assetId: String) {
    viewModelScope.launch {
      cloudRepository.deleteCustomAsset(assetId)
    }
  }

  companion object {
    const val DEFAULT_LOCAL_PROJECT_KEY = "local_prism_resonance"
    val builtInSidebarAssets: List<BuiltInAssetItem> = StudioViewModelHelpers.builtInSidebarAssets
  }
}

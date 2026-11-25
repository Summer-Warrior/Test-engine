package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.data.repository.LocalStudioProjectRepository
import com.example.engine.PrismRomCartridge
import com.example.engine.PrismRomPackageEngine
import com.example.ui.StudioViewModel
import com.example.ui.components.GameTestSection
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold

/**
 * Isolated Full-Screen Standalone `.prismrom` Player Activity.
 *
 * Launches as its own multi-document Android task window (separate from Studio) from:
 * - "Launch Standalone" inside the Studio Comparison ROM Exporter,
 * - A Home-Screen Shortcut Icon installed via [PrismRomPackageEngine.requestPinRomHomeShortcut], or
 * - Any external `.prismrom` file opened via `ACTION_VIEW`.
 *
 * Renders ONLY the standalone game experience (22-region Overworld, Battles, Wardrobe, Bag, and all
 * 5 Side Modes) locked to that cartridge's exact hardware AGSL shaders, CRT/LCD optics, and 64-step
 * hardware-tuned DSP audio — with ZERO Studio tabs, ZERO Asset Sidebar, and ZERO Map/Sprite editors.
 */
class PrismRomPlayerActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val initialCartridge = resolveCartridgeFromIntent(intent)
    setContent {
      MyApplicationTheme {
        StandalonePrismRomScreen(
          cartridge = initialCartridge,
          onExitToStudio = { finish() },
        )
      }
    }
  }

  private fun resolveCartridgeFromIntent(launchIntent: Intent?): PrismRomCartridge? {
    if (launchIntent == null) return null
    val romPath = launchIntent.getStringExtra(PrismRomPackageEngine.EXTRA_ROM_PATH)
    if (!romPath.isNullOrBlank()) {
      PrismRomPackageEngine.loadRomFromPath(romPath)?.let { return it }
    }
    val romId = launchIntent.getStringExtra(PrismRomPackageEngine.EXTRA_ROM_ID)
    if (!romId.isNullOrBlank()) {
      PrismRomPackageEngine.loadRomById(this, romId)?.let { return it }
    }
    val uri = launchIntent.data
    if (uri != null) {
      if (uri.scheme == "prismrom") {
        val segId = uri.lastPathSegment
        if (!segId.isNullOrBlank()) {
          PrismRomPackageEngine.loadRomById(this, segId)?.let { return it }
        }
      } else {
        PrismRomPackageEngine.loadRomFromUri(this, uri).getOrNull()?.let { return it }
      }
    }
    // Fallback to most recent compiled ROM if invoked directly without extras
    return PrismRomPackageEngine.listCompiledRoms(this).firstOrNull()
  }
}

@Composable
private fun StandalonePrismRomScreen(
  cartridge: PrismRomCartridge?,
  onExitToStudio: () -> Unit,
) {
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

  val hardwareState by viewModel.hardwareRegistryState.collectAsStateWithLifecycle()
  val audioState by viewModel.audioConsoleState.collectAsStateWithLifecycle()
  val fusionConfig by viewModel.fusionConfig.collectAsStateWithLifecycle()
  val gameTestState by viewModel.gameTestState.collectAsStateWithLifecycle()
  val campaignState by viewModel.campaignState.collectAsStateWithLifecycle()
  val sideModesState by viewModel.sideModesState.collectAsStateWithLifecycle()
  val mapGrid by viewModel.mapGrid.collectAsStateWithLifecycle()
  val studioItems by viewModel.studioItems.collectAsStateWithLifecycle()
  val spriteEditorState by viewModel.spriteEditorState.collectAsStateWithLifecycle()
  val playerTrainer by viewModel.playerTrainer.collectAsStateWithLifecycle()
  val nativePokemonList by viewModel.nativePokemonList.collectAsStateWithLifecycle()
  val hydratedByDex = remember(nativePokemonList) { nativePokemonList.associateBy { it.dexNumber } }

  var showRomInfoDialog by remember { mutableStateOf(false) }

  // Apply the cartridge hardware lock as soon as hardware catalog finishes hydrating
  LaunchedEffect(cartridge?.romId, hardwareState.allPlatforms.size) {
    if (cartridge != null && hardwareState.allPlatforms.isNotEmpty()) {
      viewModel.loadStandaloneRomCartridge(cartridge)
    }
  }

  // Stop BGM cleanly when standalone ROM player goes to background
  LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
    viewModel.onAppStopped()
  }

  val accentColor = remember(cartridge?.accentHex) {
    runCatching { Color(android.graphics.Color.parseColor(cartridge?.accentHex ?: "#00E5FF")) }
      .getOrDefault(CyberCyan)
  }

  Scaffold(
    containerColor = StudioDeepNavy,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    if (cartridge == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Default.Memory, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(48.dp))
          Text(
            text = "CARTRIDGE NOT FOUND",
            fontFamily = PressStart2PFamily,
            style = MaterialTheme.typography.titleSmall,
            color = SuperFxGold,
          )
          Text(
            text = "Compile a Comparison ROM inside PokéFusion Studio first, then launch it standalone or pin it to your Home Screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextSecondary,
          )
          Button(
            onClick = onExitToStudio,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier.heightIn(min = 48.dp).testTag("rom_player_missing_exit_button"),
          ) {
            Text("Return to Studio", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Minimal Hardware-Branded Standalone Cartridge Header + ROM Info / Exit Pill
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceDark)
            .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("standalone_rom_header"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              text = "${cartridge.romTitle.uppercase()} — ${cartridge.editionSubtitle}",
              fontFamily = PressStart2PFamily,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "LOCKED HARDWARE: ${cartridge.hardwareBadgeSummary}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = { showRomInfoDialog = true },
              shape = RoundedCornerShape(9.dp),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("rom_player_info_pill"),
            ) {
              Icon(Icons.Default.Info, contentDescription = "ROM Info", tint = accentColor, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ROM Info", style = MaterialTheme.typography.labelSmall, color = accentColor)
            }
            OutlinedButton(
              onClick = onExitToStudio,
              shape = RoundedCornerShape(9.dp),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag("rom_player_exit_pill"),
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit to Studio", tint = StudioTextPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Exit", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
            }
          }
        }

        // Full Playable Isolated Game Experience (ZERO Studio tabs, ZERO Asset Sidebar, ZERO Map/Sprite Editors)
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
          onSelectHardwarePreset = { /* Locked to cartridge hardware profile */ },
          onApplyCustomHardwareCombo = { _, _ -> /* Locked to cartridge hardware profile */ },
          onToggleHardwareTour = { /* Locked to cartridge hardware profile */ },
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
          onEditHeroInSpriteEditor = {},
          onToggleUseCustomPlayerAvatar = { viewModel.toggleUseCustomPlayerAvatarBitmap() },
          onSaveHeroToRoom = { spec -> viewModel.saveCustomTrainerToRoom(spec) },
          onDismissVsBanner = { viewModel.dismissTrainerVsBanner() },
          onToggleTrailingPartner = { viewModel.toggleTrailingPartner() },
          onOpenTrainerStudio = {},
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
          isStandaloneRomPlayer = true,
        )
      }
    }
  }

  if (showRomInfoDialog && cartridge != null) {
    AlertDialog(
      onDismissRequest = { showRomInfoDialog = false },
      containerColor = StudioSurfaceDark,
      title = {
        Text(
          text = cartridge.editionSubtitle,
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelMedium,
          color = accentColor,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Game Title: ${cartridge.romTitle}", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
          Text("Comparison Tag: ${cartridge.comparisonTag}", style = MaterialTheme.typography.bodySmall, color = CyberCyan)
          Text("Hardware Lock: ${cartridge.hardwareBadgeSummary}", style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
          Text("Discipline Mode: ${cartridge.disciplineMode.label}", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
          Text("Character Mix: ${cartridge.characterMixMode.label}", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
          Text("Extra Pretty Toggles: ${cartridge.extraVisualAudio.enabledCount}/9 active", style = MaterialTheme.typography.bodySmall, color = SgbEmerald)
          Text("Bundled Hero: ${cartridge.playerTrainer.name} (${cartridge.playerTrainer.title})", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
          Text("Bundled Maps: ${cartridge.regionMapOverrides.size} regional grids • 6-Slot Party", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
        }
      },
      confirmButton = {
        TextButton(onClick = { showRomInfoDialog = false }) {
          Text("Resume Playing", color = accentColor, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showRomInfoDialog = false
            onExitToStudio()
          },
        ) {
          Text("Exit to Studio", color = StudioTextSecondary)
        }
      },
    )
  }
}

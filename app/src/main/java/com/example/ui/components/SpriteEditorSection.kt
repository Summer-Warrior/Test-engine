package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.model.AssetCategory
import com.example.data.model.CustomStudioAsset
import com.example.engine.SpritePixelMatrixEngine
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioSurfaceDark
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SpriteTool(val label: String, val icon: ImageVector) {
  PENCIL("Pencil", Icons.Default.Brush),
  ERASER("Eraser", Icons.Default.CleaningServices),
  FILL("Fill", Icons.Default.FormatColorFill),
  EYEDROPPER("Pick", Icons.Default.Colorize),
}

/**
 * Interactive 16x16 Indexed-Palette Sprite & Animation Frame Editor.
 * Supports sampling active species sprites via Coil, importing external PNGs via Photo Picker,
 * real-time PPU palette swapping across animation frames, and saving edited assets to Firestore.
 */
@Composable
fun SpriteEditorSection(
  activePokemon: NativePokemonEntity,
  customAssets: List<CustomStudioAsset>,
  isSavingToCloud: Boolean,
  onSaveEditedSprite: (
    name: String,
    seedDexNumber: Int,
    primaryHex: String,
    secondaryHex: String,
    paletteSummary: String,
    frameCount: Int,
  ) -> Unit,
  onApplyPaletteToSpecies: (primaryHex: String, secondaryHex: String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var assetName by remember(activePokemon.dexNumber) {
    mutableStateOf("${activePokemon.name} Custom Sprite")
  }
  var palette by remember(activePokemon.dexNumber) {
    mutableStateOf(
      SpritePixelMatrixEngine.buildInitialPalette(activePokemon.primaryHex, activePokemon.secondaryHex)
    )
  }
  var baseSampledPalette by remember(activePokemon.dexNumber) { mutableStateOf(palette) }
  var frames by remember(activePokemon.dexNumber) {
    mutableStateOf(
      listOf(
        SpritePixelMatrixEngine.generateSeededFrame(activePokemon.dexNumber, 0),
        SpritePixelMatrixEngine.generateSeededFrame(activePokemon.dexNumber, 1),
      )
    )
  }
  var activeFrameIndex by remember { mutableIntStateOf(0) }
  var selectedSlotIndex by remember { mutableIntStateOf(0) }
  var selectedTool by remember { mutableStateOf(SpriteTool.PENCIL) }
  var mirrorX by remember { mutableStateOf(false) }
  var showGridLines by remember { mutableStateOf(true) }
  var isPlayingPreview by remember { mutableStateOf(true) }
  var previewFps by remember { mutableIntStateOf(4) }
  var previewFrameIdx by remember { mutableIntStateOf(0) }
  var undoStack by remember { mutableStateOf<List<Pair<Int, List<Int>>>>(emptyList()) }
  var sourceLabel by remember { mutableStateOf("Sampling #${activePokemon.dexNumber} ${activePokemon.name}...") }

  suspend fun loadPokemonSpriteAsset() {
    val fallbackPalette = SpritePixelMatrixEngine.buildInitialPalette(
      activePokemon.primaryHex,
      activePokemon.secondaryHex,
    )
    val bitmap = loadSoftwareBitmapFromUrl(context, activePokemon.pixelSpriteUrl)
    if (bitmap != null) {
      val extracted = SpritePixelMatrixEngine.extractPaletteFromBitmap(
        bitmap = bitmap,
        fallbackPrimaryHex = activePokemon.primaryHex,
        fallbackSecondaryHex = activePokemon.secondaryHex,
      )
      val frame0 = SpritePixelMatrixEngine.quantizeBitmapToIndexedGrid(bitmap, extracted)
      val frame1 = createBobVariantFrame(frame0)
      palette = extracted
      baseSampledPalette = extracted
      frames = listOf(frame0, frame1)
      activeFrameIndex = 0
      undoStack = emptyList()
      sourceLabel = "Loaded PokéAPI sprite (#${activePokemon.dexNumber} ${activePokemon.name})"
    } else {
      palette = fallbackPalette
      baseSampledPalette = fallbackPalette
      frames = listOf(
        SpritePixelMatrixEngine.generateSeededFrame(activePokemon.dexNumber, 0),
        SpritePixelMatrixEngine.generateSeededFrame(activePokemon.dexNumber, 1),
      )
      activeFrameIndex = 0
      undoStack = emptyList()
      sourceLabel = "Loaded seeded matrix (#${activePokemon.dexNumber} ${activePokemon.name})"
    }
  }

  LaunchedEffect(activePokemon.dexNumber, activePokemon.pixelSpriteUrl) {
    loadPokemonSpriteAsset()
  }

  LaunchedEffect(isPlayingPreview, previewFps, frames.size) {
    if (!isPlayingPreview || frames.isEmpty()) return@LaunchedEffect
    while (true) {
      delay((1000L / previewFps.coerceAtLeast(1)))
      previewFrameIdx = (previewFrameIdx + 1) % frames.size
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val imported = decodeSoftwareBitmapFromUri(context, uri)
        if (imported != null) {
          val extracted = SpritePixelMatrixEngine.extractPaletteFromBitmap(
            bitmap = imported,
            fallbackPrimaryHex = activePokemon.primaryHex,
            fallbackSecondaryHex = activePokemon.secondaryHex,
          )
          val frame0 = SpritePixelMatrixEngine.quantizeBitmapToIndexedGrid(imported, extracted)
          val frame1 = createBobVariantFrame(frame0)
          palette = extracted
          baseSampledPalette = extracted
          frames = listOf(frame0, frame1)
          activeFrameIndex = 0
          undoStack = emptyList()
          sourceLabel = "Imported external PNG/Image via Photo Picker"
        } else {
          sourceLabel = "Could not decode selected image"
        }
      }
    }
  }

  val safeFrameIdx = activeFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
  val currentPixels = frames.getOrElse(safeFrameIdx) { List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 } }

  fun pushUndoSnapshot() {
    undoStack = (undoStack + (safeFrameIdx to currentPixels)).takeLast(20)
  }

  fun updateCurrentFramePixels(nextPixels: List<Int>) {
    frames = frames.toMutableList().also { list ->
      if (safeFrameIdx in list.indices) list[safeFrameIdx] = nextPixels
    }
  }

  fun handleCellInteraction(row: Int, col: Int, isStrokeStart: Boolean) {
    val grid = SpritePixelMatrixEngine.GRID_SIZE
    val idx = row * grid + col
    when (selectedTool) {
      SpriteTool.PENCIL, SpriteTool.ERASER -> {
        val targetColor = if (selectedTool == SpriteTool.ERASER) -1 else selectedSlotIndex
        val mirrorCol = grid - 1 - col
        val mirrorIdx = row * grid + mirrorCol
        val changed = currentPixels[idx] != targetColor || (mirrorX && currentPixels[mirrorIdx] != targetColor)
        if (isStrokeStart && changed) pushUndoSnapshot()
        if (changed) {
          val mutable = currentPixels.toMutableList()
          mutable[idx] = targetColor
          if (mirrorX) mutable[mirrorIdx] = targetColor
          updateCurrentFramePixels(mutable)
        }
      }
      SpriteTool.FILL -> if (isStrokeStart) {
        val filled = SpritePixelMatrixEngine.floodFill(currentPixels, idx, selectedSlotIndex)
        if (filled != currentPixels) {
          pushUndoSnapshot()
          updateCurrentFramePixels(filled)
        }
      }
      SpriteTool.EYEDROPPER -> {
        val picked = currentPixels[idx]
        if (picked in palette.indices) {
          selectedSlotIndex = picked
          selectedTool = SpriteTool.PENCIL
        }
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Header & Importer Controls Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "SPRITE & PALETTE MATRIX EDITOR (16×16 PPU)",
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
        )
        Text(
          text = sourceLabel,
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextSecondary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { scope.launch { loadPokemonSpriteAsset() } },
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("sprite_reload_active_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reload active species sprite", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reload Active", maxLines = 1)
          }
          Button(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = VoxelViolet, contentColor = StudioTextPrimary),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("sprite_import_photo_button"),
          ) {
            Icon(Icons.Default.Image, contentDescription = "Import sprite from gallery", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Import PNG", maxLines = 1)
          }
        }
      }
    }

    // Live Animated Preview + Multi-Frame Timeline Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          val animPixels = frames.getOrElse(previewFrameIdx % frames.size.coerceAtLeast(1)) { currentPixels }
          Canvas(
            modifier = Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(StudioDeepNavy)
              .border(1.dp, CyberCyan, RoundedCornerShape(10.dp))
              .testTag("sprite_animated_preview"),
          ) {
            drawIndexedSprite(pixels = animPixels, palette = palette, showGrid = false)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Animation Timeline (${frames.size} Frames)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { isPlayingPreview = !isPlayingPreview },
                modifier = Modifier.size(48.dp).testTag("sprite_play_pause_button"),
              ) {
                Icon(
                  imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isPlayingPreview) "Pause animation preview" else "Play animation preview",
                  tint = SgbEmerald,
                )
              }
              listOf(2, 4, 8).forEach { fps ->
                FilterChip(
                  selected = previewFps == fps,
                  onClick = { previewFps = fps },
                  label = { Text("${fps}FPS", style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          frames.forEachIndexed { idx, _ ->
            FilterChip(
              selected = idx == safeFrameIdx,
              onClick = { activeFrameIndex = idx },
              label = { Text("F${idx + 1}") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("sprite_frame_chip_$idx"),
            )
          }
          OutlinedButton(
            onClick = {
              frames = frames + listOf(List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 })
              activeFrameIndex = frames.lastIndex
            },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sprite_add_frame_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add blank frame", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
          }
          OutlinedButton(
            onClick = {
              frames = frames + listOf(currentPixels.toList())
              activeFrameIndex = frames.lastIndex
            },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sprite_dup_frame_button"),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate current frame", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Dup")
          }
          OutlinedButton(
            enabled = frames.size > 1,
            onClick = {
              if (frames.size > 1) {
                frames = frames.toMutableList().apply { removeAt(safeFrameIdx) }
                activeFrameIndex = safeFrameIdx.coerceAtMost(frames.lastIndex)
              }
            },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sprite_delete_frame_button"),
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete active frame", tint = ElectricCrimson, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    // 16x16 Interactive Pixel Canvas + Drawing Toolbar Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SpriteTool.entries.forEach { tool ->
            FilterChip(
              selected = selectedTool == tool,
              onClick = { selectedTool = tool },
              leadingIcon = { Icon(tool.icon, contentDescription = tool.label, modifier = Modifier.size(16.dp)) },
              label = { Text(tool.label) },
              modifier = Modifier.testTag("sprite_tool_${tool.name.lowercase()}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          FilterChip(
            selected = mirrorX,
            onClick = { mirrorX = !mirrorX },
            leadingIcon = { Icon(Icons.Default.Flip, contentDescription = "Toggle horizontal symmetry", modifier = Modifier.size(16.dp)) },
            label = { Text("Mirror X") },
            modifier = Modifier.testTag("sprite_mirror_toggle"),
          )
          FilterChip(
            selected = showGridLines,
            onClick = { showGridLines = !showGridLines },
            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = "Toggle pixel gridlines", modifier = Modifier.size(16.dp)) },
            label = { Text("Grid") },
            modifier = Modifier.testTag("sprite_grid_toggle"),
          )
          IconButton(
            onClick = {
              pushUndoSnapshot()
              val grid = SpritePixelMatrixEngine.GRID_SIZE
              val flipped = List(SpritePixelMatrixEngine.PIXEL_COUNT) { idx ->
                val r = idx / grid
                val c = idx % grid
                currentPixels[r * grid + (grid - 1 - c)]
              }
              updateCurrentFramePixels(flipped)
            },
            modifier = Modifier.size(48.dp).testTag("sprite_flip_horizontal_button"),
          ) {
            Icon(Icons.Default.Flip, contentDescription = "Flip frame horizontally", tint = SuperFxGold)
          }
          IconButton(
            enabled = undoStack.isNotEmpty(),
            onClick = {
              val last = undoStack.lastOrNull() ?: return@IconButton
              undoStack = undoStack.dropLast(1)
              activeFrameIndex = last.first.coerceIn(0, frames.lastIndex)
              frames = frames.toMutableList().also { list ->
                if (activeFrameIndex in list.indices) list[activeFrameIndex] = last.second
              }
            },
            modifier = Modifier.size(48.dp).testTag("sprite_undo_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last pixel edit", tint = CyberCyan)
          }
        }

        val currentHandler by rememberUpdatedState(::handleCellInteraction)
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, CyberCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val g = SpritePixelMatrixEngine.GRID_SIZE
                var lastCol = ((down.position.x / size.width) * g).toInt().coerceIn(0, g - 1)
                var lastRow = ((down.position.y / size.height) * g).toInt().coerceIn(0, g - 1)
                currentHandler(lastRow, lastCol, true)
                drag(down.id) { change ->
                  change.consume()
                  val col = ((change.position.x / size.width) * g).toInt().coerceIn(0, g - 1)
                  val row = ((change.position.y / size.height) * g).toInt().coerceIn(0, g - 1)
                  if (row != lastRow || col != lastCol) {
                    lastRow = row
                    lastCol = col
                    currentHandler(row, col, false)
                  }
                }
              }
            }
            .testTag("sprite_pixel_canvas"),
        ) {
          drawIndexedSprite(pixels = currentPixels, palette = palette, showGrid = showGridLines)
        }
      }
    }

    SpritePaletteEditorCard(
      palette = palette,
      baseSampledPalette = baseSampledPalette,
      selectedSlotIndex = selectedSlotIndex,
      onSelectSlot = { selectedSlotIndex = it },
      onUpdatePalette = { palette = it },
    )

    // Save & Apply Actions Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = assetName,
          onValueChange = { assetName = it },
          label = { Text("Edited Sprite Asset Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("sprite_editor_name_input"),
        )
        val primaryHex = SpritePixelMatrixEngine.argbLongToHex(palette.getOrElse(0) { 0xFF00F5D4 })
        val secondaryHex = SpritePixelMatrixEngine.argbLongToHex(palette.getOrElse(1) { 0xFFFF2A85 })
        val paletteSummary = palette.take(4).joinToString(",") { SpritePixelMatrixEngine.argbLongToHex(it) }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { onApplyPaletteToSpecies(primaryHex, secondaryHex) },
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("sprite_editor_apply_palette_button"),
          ) {
            Text("Apply Palette", maxLines = 1)
          }
          Button(
            enabled = !isSavingToCloud,
            onClick = {
              onSaveEditedSprite(
                assetName.ifBlank { "${activePokemon.name} Sprite" },
                activePokemon.dexNumber,
                primaryHex,
                secondaryHex,
                paletteSummary,
                frames.size,
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .testTag("sprite_editor_save_button"),
          ) {
            Icon(Icons.Default.Save, contentDescription = "Save edited sprite to Cloud Drawer", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSavingToCloud) "Saving..." else "Save Sprite", fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        val savedSpriteCount = customAssets.count { it.category == AssetCategory.SPRITES.label }
        Text(
          text = "Synced custom sprites in Drawer: $savedSpriteCount",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
      }
    }
  }
}

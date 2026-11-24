package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.data.local.SavedSpriteEntity
import com.example.engine.SpritePixelMatrixEngine
import com.example.ui.SpriteEditTarget
import com.example.ui.SpriteEditorUiState
import com.example.ui.SpriteToolMode
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

/**
 * Interactive 16x16 Indexed-Palette Sprite & Animation Frame Editor.
 * State is 100% hoisted into StudioViewModel + Room `saved_sprites` so tab switches NEVER lose work,
 * and saved sprites restore exact multi-frame pixels & palettes directly into the Viewport & Game Test.
 */
@Composable
fun SpriteEditorSection(
  activePokemon: NativePokemonEntity,
  editorState: SpriteEditorUiState,
  savedSprites: List<SavedSpriteEntity>,
  isSaving: Boolean,
  onSelectTool: (SpriteToolMode) -> Unit,
  onSelectColorSlot: (Int) -> Unit,
  onUpdatePaletteSlot: (Int, Long) -> Unit,
  onApplyPresetPaletteValues: (List<Long>) -> Unit,
  onUpdateSpriteName: (String) -> Unit,
  onToggleMirrorX: () -> Unit,
  onToggleShowGrid: () -> Unit,
  onTogglePlayPreview: () -> Unit,
  onSelectFrame: (Int) -> Unit,
  onAddFrame: () -> Unit,
  onDuplicateFrame: () -> Unit,
  onDeleteFrame: () -> Unit,
  onApplyPixelStroke: (Int) -> Unit,
  onUndo: () -> Unit,
  onClearFrame: () -> Unit,
  onImportBitmap: (android.graphics.Bitmap, String?) -> Unit,
  onLoadActiveSpeciesSeed: () -> Unit,
  onSampleHeroToEditor: () -> Unit = {},
  onApplySpriteToHeroAvatar: () -> Unit = {},
  onSaveSprite: () -> Unit,
  onLoadSavedSprite: (SavedSpriteEntity) -> Unit,
  onDeleteSavedSprite: (String) -> Unit,
  onApplyPaletteToSpecies: (String, String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val currentStrokeCallback by rememberUpdatedState(onApplyPixelStroke)

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        val bmp = decodeSoftwareBitmapFromUri(context, uri)
        if (bmp != null) {
          onImportBitmap(bmp, "${activePokemon.name} Photo Rig")
        }
      }
    }
  }

  // Animated frame playback loop when Play Preview is active
  LaunchedEffect(editorState.isPlayingPreview, editorState.frames.size, editorState.previewFps) {
    if (editorState.isPlayingPreview && editorState.frames.size > 1) {
      var idx = editorState.activeFrameIndex
      while (true) {
        delay((1000L / editorState.previewFps.coerceIn(2, 15)).coerceAtLeast(60L))
        idx = (idx + 1) % editorState.frames.size
        onSelectFrame(idx)
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // 1. Saved Sprites Strip (Room Persistence)
    if (savedSprites.isNotEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SgbEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardDark),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "SAVED SPRITES IN ROOM SQLITE (${savedSprites.size}) — TAP TO RESTORE",
            style = MaterialTheme.typography.labelSmall,
            color = SgbEmerald,
            fontWeight = FontWeight.Bold,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            savedSprites.forEach { entity ->
              val isLoaded = editorState.activeSavedSpriteId == entity.id
              val thumbBmp = remember(entity.id, entity.updatedAtEpochMs) {
                val pal = SpritePixelMatrixEngine.deserializePaletteJson(entity.paletteJson)
                val frames = SpritePixelMatrixEngine.deserializeFramesRleJson(entity.framesRleJson)
                val first = frames.firstOrNull() ?: List(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 }
                SpritePixelMatrixEngine.renderFrameToBitmap(first, pal, scale = 3).asImageBitmap()
              }
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isLoaded) SgbEmerald.copy(alpha = 0.18f) else StudioSurfaceDark)
                  .border(1.dp, if (isLoaded) SgbEmerald else StudioBorderDark, RoundedCornerShape(10.dp))
                  .clickable { onLoadSavedSprite(entity) }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("saved_sprite_card_${entity.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Image(
                  bitmap = thumbBmp,
                  contentDescription = entity.name,
                  filterQuality = FilterQuality.None,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF070B14)),
                )
                Column(modifier = Modifier.widthIn(max = 120.dp)) {
                  Text(
                    text = entity.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${entity.frameCount}f • #${entity.spriteSeedId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextSecondary,
                  )
                }
                IconButton(
                  onClick = { onDeleteSavedSprite(entity.id) },
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${entity.name}",
                    tint = ElectricCrimson,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. Main Sprite Canvas & Tools Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, StudioBorderDark, RoundedCornerShape(18.dp)),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = StudioCardDark),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "16×16 PPU INDEXED SPRITE & ANIMATION EDITOR (HOISTED + PERSISTED)",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${editorState.spriteName} • Frame ${editorState.activeFrameIndex + 1}/${editorState.frames.size}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = {
                onLoadActiveSpeciesSeed()
              },
              modifier = Modifier.testTag("sprite_sample_active_button"),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Creature", color = CyberCyan)
            }
            OutlinedButton(
              onClick = onSampleHeroToEditor,
              modifier = Modifier.testTag("sprite_sample_hero_button"),
            ) {
              Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuperFxGold)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hero", color = SuperFxGold)
            }
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.testTag("sprite_import_photo_button"),
            ) {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoxelViolet)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Photo", color = VoxelViolet)
            }
          }
        }

        // Active Sprite Target Indicator (Creature Rig vs Player Hero Avatar — isolated bitmaps!)
        val isHeroTarget = editorState.editingTarget == SpriteEditTarget.PLAYER_AVATAR
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHeroTarget) SuperFxGold.copy(alpha = 0.14f) else CyberCyan.copy(alpha = 0.12f))
            .border(1.dp, if (isHeroTarget) SuperFxGold.copy(alpha = 0.65f) else CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isHeroTarget) {
              "TARGET: PLAYER HERO AVATAR (Overworld & Battle Hero — Creature Rig untouched)"
            } else {
              "TARGET: CREATURE RIG (#${activePokemon.dexNumber} ${activePokemon.name} — Hero Avatar untouched)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isHeroTarget) SuperFxGold else CyberCyan,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }

        // Editable Sprite Name
        OutlinedTextField(
          value = editorState.spriteName,
          onValueChange = onUpdateSpriteName,
          label = { Text("Sprite Asset Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sprite_name_input"),
        )

        // Tool Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SpriteToolMode.entries.forEach { tool ->
            val icon = when (tool) {
              SpriteToolMode.PENCIL -> Icons.Default.Brush
              SpriteToolMode.ERASER -> Icons.Default.CleaningServices
              SpriteToolMode.FILL -> Icons.Default.FormatColorFill
              SpriteToolMode.EYEDROPPER -> Icons.Default.Colorize
            }
            FilterChip(
              selected = editorState.activeTool == tool,
              onClick = { onSelectTool(tool) },
              leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text(tool.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                selectedLabelColor = CyberCyan,
              ),
              modifier = Modifier.testTag("sprite_tool_${tool.name.lowercase()}"),
            )
          }
          FilterChip(
            selected = editorState.mirrorX,
            onClick = onToggleMirrorX,
            leadingIcon = { Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Mirror X") },
            modifier = Modifier.testTag("sprite_mirror_toggle"),
          )
          FilterChip(
            selected = editorState.showGrid,
            onClick = onToggleShowGrid,
            leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Grid") },
            modifier = Modifier.testTag("sprite_grid_toggle"),
          )
          IconButton(
            onClick = onUndo,
            enabled = editorState.undoStack.isNotEmpty(),
            modifier = Modifier.testTag("sprite_undo_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Stroke", tint = SuperFxGold)
          }
          IconButton(
            onClick = onClearFrame,
            modifier = Modifier.testTag("sprite_clear_frame_button"),
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Clear Frame", tint = ElectricCrimson)
          }
        }

        // Interactive 16x16 Pixel Canvas with Drag-to-Paint
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B14))
            .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        ) {
          Canvas(
            modifier = Modifier
              .matchParentSize()
              .testTag("sprite_pixel_canvas")
              .pointerInput(editorState.activeFrameIndex, editorState.activeTool, editorState.selectedColorSlot, editorState.mirrorX) {
                awaitEachGesture {
                  val down = awaitFirstDown(requireUnconsumed = false)
                  down.consume()
                  val g = SpritePixelMatrixEngine.GRID_SIZE
                  var lastIdx = ((down.position.y / size.height) * g).toInt().coerceIn(0, g - 1) * g +
                    ((down.position.x / size.width) * g).toInt().coerceIn(0, g - 1)
                  currentStrokeCallback(lastIdx)
                  drag(down.id) { change ->
                    change.consume()
                    val r = ((change.position.y / size.height) * g).toInt().coerceIn(0, g - 1)
                    val c = ((change.position.x / size.width) * g).toInt().coerceIn(0, g - 1)
                    val idx = r * g + c
                    if (idx != lastIdx) {
                      lastIdx = idx
                      currentStrokeCallback(idx)
                    }
                  }
                }
              },
          ) {
            drawIndexedSprite(
              pixels = editorState.currentFrame,
              palette = editorState.paletteArgb,
              showGrid = editorState.showGrid,
            )
            if (editorState.mirrorX) {
              drawLine(
                color = CyberCyan.copy(alpha = 0.45f),
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                strokeWidth = 2f,
              )
            }
          }
        }

        // Animation Frame Timeline & Playback
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onTogglePlayPreview,
            modifier = Modifier.testTag("sprite_play_preview_button"),
          ) {
            Icon(
              imageVector = if (editorState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (editorState.isPlayingPreview) "Pause Sprite Animation" else "Play Sprite Animation",
              tint = SgbEmerald,
            )
          }

          editorState.frames.forEachIndexed { idx, framePixels ->
            val selected = idx == editorState.activeFrameIndex
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) CyberCyan.copy(alpha = 0.22f) else StudioSurfaceDark)
                .border(1.dp, if (selected) CyberCyan else StudioBorderDark, RoundedCornerShape(8.dp))
                .clickable { onSelectFrame(idx) }
                .padding(5.dp)
                .testTag("sprite_frame_tab_$idx"),
            ) {
              Canvas(modifier = Modifier.size(34.dp)) {
                drawIndexedSprite(
                  pixels = framePixels,
                  palette = editorState.paletteArgb,
                  showGrid = false,
                )
              }
              Text("F${idx + 1}", style = MaterialTheme.typography.labelSmall, color = if (selected) CyberCyan else StudioTextSecondary)
            }
          }

          IconButton(onClick = onAddFrame, modifier = Modifier.testTag("sprite_add_frame_button")) {
            Icon(Icons.Default.Add, contentDescription = "Add Blank Frame", tint = CyberCyan)
          }
          IconButton(onClick = onDuplicateFrame, modifier = Modifier.testTag("sprite_dup_frame_button")) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Frame", tint = SuperFxGold)
          }
          IconButton(
            onClick = onDeleteFrame,
            enabled = editorState.frames.size > 1,
            modifier = Modifier.testTag("sprite_del_frame_button"),
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Frame", tint = ElectricCrimson)
          }
        }

        // 8-Color PPU Indexed Palette Panel
        val basePal = remember(activePokemon.dexNumber) {
          SpritePixelMatrixEngine.buildInitialPalette(activePokemon.primaryHex, activePokemon.secondaryHex)
        }
        SpritePaletteEditorCard(
          palette = editorState.paletteArgb,
          baseSampledPalette = basePal,
          selectedSlotIndex = editorState.selectedColorSlot,
          onSelectSlot = onSelectColorSlot,
          onUpdatePalette = { updatedList ->
            val changedSlots = updatedList.indices.filter { idx ->
              updatedList[idx] != editorState.paletteArgb.getOrNull(idx)
            }
            when {
              changedSlots.size == 1 -> onUpdatePaletteSlot(changedSlots.first(), updatedList[changedSlots.first()])
              changedSlots.size > 1 -> onApplyPresetPaletteValues(updatedList)
              else -> Unit
            }
          },
        )

        // Save & Apply Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onSaveSprite,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier
              .weight(1f)
              .testTag("sprite_save_room_cloud_button"),
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSaving) "Saving..." else "Save to Vault", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = onApplySpriteToHeroAvatar,
            modifier = Modifier
              .weight(1f)
              .testTag("sprite_apply_to_hero_button"),
          ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = SuperFxGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Apply to Hero", color = SuperFxGold)
          }
        }
        OutlinedButton(
          onClick = {
            val p1 = SpritePixelMatrixEngine.argbLongToHex(editorState.paletteArgb.getOrElse(0) { 0xFF00E5FF })
            val p2 = SpritePixelMatrixEngine.argbLongToHex(editorState.paletteArgb.getOrElse(1) { 0xFFFF2A85 })
            onApplyPaletteToSpecies(p1, p2)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sprite_apply_palette_species_button"),
        ) {
          Text("Apply Palette to #${activePokemon.dexNumber} ${activePokemon.name}", color = VoxelViolet)
        }
      }
    }
  }
}

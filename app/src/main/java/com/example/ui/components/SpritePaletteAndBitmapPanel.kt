package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.SpritePixelMatrixEngine
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.SuperFxGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SpritePaletteEditorCard(
  palette: List<Long>,
  baseSampledPalette: List<Long>,
  selectedSlotIndex: Int,
  onSelectSlot: (Int) -> Unit,
  onUpdatePalette: (List<Long>) -> Unit,
) {
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
      val safeSlot = selectedSlotIndex.coerceIn(0, (palette.size - 1).coerceAtLeast(0))
      val activeArgb = palette.getOrElse(safeSlot) { 0xFF00F5D4L }
      val activeHex = SpritePixelMatrixEngine.argbLongToHex(activeArgb)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Palette, contentDescription = "Palette editor", tint = SuperFxGold, modifier = Modifier.size(18.dp))
          Text("Indexed PPU Palette (Slot $safeSlot)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(activeHex, style = MaterialTheme.typography.labelLarge, color = CyberCyan)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        palette.forEachIndexed { idx, argb ->
          val selected = idx == safeSlot
          Box(
            modifier = Modifier
              .size(48.dp)
              .padding(4.dp)
              .clip(CircleShape)
              .background(Color(argb))
              .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) CyberCyan else StudioBorderDark,
                shape = CircleShape,
              )
              .clickable { onSelectSlot(idx) }
              .testTag("palette_slot_$idx"),
          )
        }
      }

      val red = ((activeArgb shr 16) and 0xFF).toInt()
      val green = ((activeArgb shr 8) and 0xFF).toInt()
      val blue = (activeArgb and 0xFF).toInt()

      fun updateSlotRgb(r: Int, g: Int, b: Int) {
        val nextArgb = (0xFFL shl 24) or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
        onUpdatePalette(palette.toMutableList().also { if (safeSlot in it.indices) it[safeSlot] = nextArgb })
      }

      RgbChannelSlider("R", red, ElectricCrimson) { updateSlotRgb(it, green, blue) }
      RgbChannelSlider("G", green, SgbEmerald) { updateSlotRgb(red, it, blue) }
      RgbChannelSlider("B", blue, CyberCyan) { updateSlotRgb(red, green, it) }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        SpritePixelMatrixEngine.consolePresetPalettes.forEach { (presetName, presetColors) ->
          FilterChip(
            selected = palette == presetColors || (presetName == "Active Asset" && palette == baseSampledPalette),
            onClick = {
              onUpdatePalette(if (presetName == "Active Asset") baseSampledPalette else presetColors)
            },
            label = { Text(presetName, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("palette_preset_${presetName.lowercase().replace(' ', '_')}"),
          )
        }
      }
    }
  }
}

@Composable
private fun RgbChannelSlider(
  channelLabel: String,
  value: Int,
  accentColor: Color,
  onChange: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "$channelLabel:${value.toString().padStart(3, ' ')}",
      style = MaterialTheme.typography.labelSmall,
      color = accentColor,
      modifier = Modifier.width(48.dp),
    )
    Slider(
      value = value.toFloat(),
      onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
      valueRange = 0f..255f,
      colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
      ),
      modifier = Modifier.weight(1f).height(28.dp),
    )
  }
}

internal fun DrawScope.drawIndexedSprite(
  pixels: List<Int>,
  palette: List<Long>,
  showGrid: Boolean,
) {
  val grid = SpritePixelMatrixEngine.GRID_SIZE
  val cellW = size.width / grid
  val cellH = size.height / grid
  val checkerDark = Color(0xFF0B1220)
  val checkerLight = Color(0xFF151F33)

  for (row in 0 until grid) {
    for (col in 0 until grid) {
      val idx = row * grid + col
      val paletteIdx = pixels.getOrElse(idx) { -1 }
      val topLeft = Offset(col * cellW, row * cellH)
      val cellSize = Size(cellW + 0.5f, cellH + 0.5f)
      val bgColor = if ((row + col) % 2 == 0) checkerDark else checkerLight
      drawRect(color = bgColor, topLeft = topLeft, size = cellSize)
      if (paletteIdx in palette.indices) {
        drawRect(color = Color(palette[paletteIdx]), topLeft = topLeft, size = cellSize)
      }
    }
  }

  if (showGrid) {
    val lineColor = Color.White.copy(alpha = 0.14f)
    for (i in 1 until grid) {
      val x = i * cellW
      val y = i * cellH
      drawLine(lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
      drawLine(lineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
    }
  }
}

internal suspend fun decodeSoftwareBitmapFromUri(context: Context, uri: Uri): Bitmap? {
  return withContext(Dispatchers.IO) {
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
          decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
          decoder.isMutableRequired = false
        }
      } else {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)?.let { bmp ->
            if (bmp.config == Bitmap.Config.ARGB_8888) bmp else bmp.copy(Bitmap.Config.ARGB_8888, false)
          }
        }
      }
    }.getOrNull()
  }
}

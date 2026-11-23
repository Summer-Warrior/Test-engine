package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/**
 * Deterministic 16x16 indexed-color pixel matrix & palette engine for the PokéFusion Sprite Editor.
 * Uses indexed palette slots (-1 = transparent, 0..7 = editable palette indices) so modifying
 * any palette color updates all pixel frames in real time like authentic console PPU hardware.
 */
object SpritePixelMatrixEngine {
  const val GRID_SIZE = 16
  const val PIXEL_COUNT = GRID_SIZE * GRID_SIZE

  val consolePresetPalettes: Map<String, List<Long>> = mapOf(
    "Active Asset" to listOf(
      0xFF00F5D4, 0xFFFF2A85, 0xFFFFFFFF, 0xFF141B2D,
      0xFFFFD166, 0xFF10B981, 0xFF38BDF8, 0xFFA855F7,
    ),
    "DMG Game Boy" to listOf(
      0xFF9BBC0F, 0xFF8BAC0F, 0xFFE0F8D0, 0xFF0F380F,
      0xFF306230, 0xFF8BAC0F, 0xFF306230, 0xFF0F380F,
    ),
    "GBC Shiny Gold" to listOf(
      0xFFFFD166, 0xFFEF4444, 0xFFFFFBEB, 0xFF1E1B4B,
      0xFFF97316, 0xFFFACC15, 0xFF38BDF8, 0xFF7C3AED,
    ),
    "Orre Shadow TEV" to listOf(
      0xFF7C3AED, 0xFFEC4899, 0xFFE2E8F0, 0xFF090D16,
      0xFF38BDF8, 0xFFA855F7, 0xFF1E1B4B, 0xFFFF2A85,
    ),
  )

  fun parseHexToArgbLong(hex: String, fallback: Long): Long {
    return try {
      val clean = hex.trim().removePrefix("#")
      val full = when (clean.length) {
        6 -> "FF$clean"
        8 -> clean
        else -> return fallback
      }
      full.toLong(16)
    } catch (_: NumberFormatException) {
      fallback
    }
  }

  fun argbLongToHex(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toInt()
    return String.format("#%06X", rgb)
  }

  fun buildInitialPalette(primaryHex: String, secondaryHex: String): List<Long> {
    val primary = parseHexToArgbLong(primaryHex, 0xFF00F5D4)
    val secondary = parseHexToArgbLong(secondaryHex, 0xFFFF2A85)
    return listOf(
      primary,
      secondary,
      0xFFF8FAFC,
      0xFF0F172A,
      0xFFFFD166,
      0xFF10B981,
      0xFF38BDF8,
      0xFFA855F7,
    )
  }

  /**
   * Samples an imported Bitmap (e.g., from PokéAPI sprite or Android Photo Picker)
   * into a crisp 16x16 indexed pixel grid mapped to the closest palette colors.
   */
  fun quantizeBitmapToIndexedGrid(bitmap: Bitmap, paletteArgb: List<Long>): List<Int> {
    val safePalette = if (paletteArgb.isEmpty()) buildInitialPalette("#00F5D4", "#FF2A85") else paletteArgb
    val scaled = Bitmap.createScaledBitmap(bitmap, GRID_SIZE, GRID_SIZE, false)
    val result = MutableList(PIXEL_COUNT) { -1 }

    for (y in 0 until GRID_SIZE) {
      for (x in 0 until GRID_SIZE) {
        val px = scaled.getPixel(x, y)
        val alpha = Color.alpha(px)
        if (alpha < 96) {
          result[y * GRID_SIZE + x] = -1
        } else {
          val r = Color.red(px)
          val g = Color.green(px)
          val b = Color.blue(px)
          var bestIdx = 0
          var bestDist = Int.MAX_VALUE
          for (i in safePalette.indices) {
            val c = safePalette[i].toInt()
            val dr = r - Color.red(c)
            val dg = g - Color.green(c)
            val db = b - Color.blue(c)
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
              bestDist = dist
              bestIdx = i
            }
          }
          result[y * GRID_SIZE + x] = bestIdx
        }
      }
    }
    if (scaled !== bitmap) {
      scaled.recycle()
    }
    return result
  }

  /**
   * Extracts up to 8 distinct dominant colors from an imported asset Bitmap
   * so users can immediately edit the asset's real palette.
   */
  fun extractPaletteFromBitmap(
    bitmap: Bitmap,
    fallbackPrimaryHex: String,
    fallbackSecondaryHex: String,
  ): List<Long> {
    val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
    val buckets = linkedMapOf<Int, Int>()
    for (y in 0 until scaled.height) {
      for (x in 0 until scaled.width) {
        val px = scaled.getPixel(x, y)
        if (Color.alpha(px) < 128) continue
        // Quantize slightly (to 5-bit channels) to group near-identical anti-aliased shades
        val qr = (Color.red(px) shr 3) shl 3
        val qg = (Color.green(px) shr 3) shl 3
        val qb = (Color.blue(px) shr 3) shl 3
        val quantized = Color.argb(255, qr, qg, qb)
        buckets[quantized] = (buckets[quantized] ?: 0) + 1
      }
    }
    if (scaled !== bitmap) {
      scaled.recycle()
    }

    val topColors = buckets.entries
      .sortedByDescending { it.value }
      .map { it.key.toLong() and 0xFFFFFFFFL }
      .distinctBy { c ->
        val r = ((c shr 16) and 0xFF) / 28
        val g = ((c shr 8) and 0xFF) / 28
        val b = (c and 0xFF) / 28
        "$r:$g:$b"
      }
      .take(8)

    val fallback = buildInitialPalette(fallbackPrimaryHex, fallbackSecondaryHex)
    if (topColors.isEmpty()) return fallback
    return List(8) { idx -> topColors.getOrElse(idx) { fallback[idx] } }
  }

  /**
   * Iterative BFS Flood Fill (zero recursion, zero stack overflow risk).
   */
  fun floodFill(pixels: List<Int>, startIndex: Int, replacementColorIdx: Int): List<Int> {
    if (startIndex !in 0 until PIXEL_COUNT) return pixels
    val target = pixels[startIndex]
    if (target == replacementColorIdx) return pixels

    val mutable = pixels.toMutableList()
    val queue = ArrayDeque<Int>()
    queue.add(startIndex)
    while (queue.isNotEmpty()) {
      val idx = queue.removeFirst()
      if (mutable[idx] != target) continue
      mutable[idx] = replacementColorIdx
      val r = idx / GRID_SIZE
      val c = idx % GRID_SIZE
      if (r > 0) queue.add((r - 1) * GRID_SIZE + c)
      if (r < GRID_SIZE - 1) queue.add((r + 1) * GRID_SIZE + c)
      if (c > 0) queue.add(r * GRID_SIZE + (c - 1))
      if (c < GRID_SIZE - 1) queue.add(r * GRID_SIZE + (c + 1))
    }
    return mutable
  }

  /**
   * Generates a crisp mirrored 16x16 fallback sprite frame seeded by National Dex number.
   */
  fun generateSeededFrame(dexNumber: Int, frameOffset: Int = 0): List<Int> {
    val grid = MutableList(PIXEL_COUNT) { -1 }
    val seed = abs(dexNumber * 31 + 17)
    for (y in 2 until 14) {
      val bobY = (y + (frameOffset % 2)).coerceIn(1, 14)
      for (x in 3 until 8) {
        val hash = ((seed xor (x * 73 + y * 139)) and 0xFF)
        val inCore = (x >= 5 && y in 4..11) || (hash % 5 == 0 && y in 3..12)
        if (inCore) {
          val colorIdx = when {
            y <= 4 -> 2
            x == 6 && y == 6 -> 3
            hash % 3 == 0 -> 1
            else -> 0
          }
          grid[bobY * GRID_SIZE + x] = colorIdx
          grid[bobY * GRID_SIZE + (GRID_SIZE - 1 - x)] = colorIdx
        }
      }
    }
    // Outline pass on transparent neighbors
    val outlined = grid.toMutableList()
    for (y in 1 until GRID_SIZE - 1) {
      for (x in 1 until GRID_SIZE - 1) {
        val idx = y * GRID_SIZE + x
        if (grid[idx] != -1) continue
        val hasBodyNeighbor =
          grid[(y - 1) * GRID_SIZE + x] != -1 ||
            grid[(y + 1) * GRID_SIZE + x] != -1 ||
            grid[y * GRID_SIZE + (x - 1)] != -1 ||
            grid[y * GRID_SIZE + (x + 1)] != -1
        if (hasBodyNeighbor) {
          outlined[idx] = 3
        }
      }
    }
    return outlined
  }

  /**
   * Compact run-length encoding for a 16x16 indexed pixel frame (-1..7).
   * Example token: "t12,0x4,3x1" where 't' = transparent (-1) and '0'..'7' = palette index.
   */
  fun encodeFrameRle(pixels: List<Int>): String {
    if (pixels.isEmpty()) return "t$PIXEL_COUNT"
    val normalized = List(PIXEL_COUNT) { idx ->
      pixels.getOrElse(idx) { -1 }.let { if (it in 0..7) it else -1 }
    }
    val sb = StringBuilder(128)
    var i = 0
    while (i < normalized.size) {
      val v = normalized[i]
      var run = 1
      while (i + run < normalized.size && normalized[i + run] == v) {
        run++
      }
      if (sb.isNotEmpty()) sb.append(',')
      val sym = if (v < 0) "t" else v.toString()
      if (run == 1) sb.append(sym) else sb.append(sym).append('x').append(run)
      i += run
    }
    return sb.toString()
  }

  fun decodeFrameRle(encoded: String): List<Int> {
    if (encoded.isBlank()) return List(PIXEL_COUNT) { -1 }
    val out = ArrayList<Int>(PIXEL_COUNT)
    encoded.split(',').forEach { token ->
      val trimmed = token.trim()
      if (trimmed.isEmpty()) return@forEach
      val parts = trimmed.split('x')
      val value = if (parts[0] == "t") -1 else parts[0].toIntOrNull()?.coerceIn(-1, 7) ?: -1
      val count = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, PIXEL_COUNT) ?: 1
      repeat(count) {
        if (out.size < PIXEL_COUNT) out.add(value)
      }
    }
    while (out.size < PIXEL_COUNT) out.add(-1)
    return out
  }

  fun serializePaletteJson(paletteArgb: List<Long>): String {
    val arr = org.json.JSONArray()
    paletteArgb.take(8).forEach { arr.put(argbLongToHex(it)) }
    return arr.toString()
  }

  fun deserializePaletteJson(paletteJson: String): List<Long> {
    val fallback = buildInitialPalette("#00F5D4", "#FF2A85")
    return runCatching {
      val arr = org.json.JSONArray(paletteJson)
      if (arr.length() == 0) return fallback
      List(8) { idx ->
        val hex = if (idx < arr.length()) arr.optString(idx) else ""
        parseHexToArgbLong(hex, fallback[idx])
      }
    }.getOrDefault(fallback)
  }

  fun serializeFramesRleJson(frames: List<List<Int>>): String {
    val arr = org.json.JSONArray()
    frames.forEach { frame -> arr.put(encodeFrameRle(frame)) }
    return arr.toString()
  }

  fun deserializeFramesRleJson(framesJson: String): List<List<Int>> {
    return runCatching {
      val arr = org.json.JSONArray(framesJson)
      if (arr.length() == 0) return emptyList()
      (0 until arr.length()).map { idx -> decodeFrameRle(arr.optString(idx)) }
    }.getOrDefault(emptyList())
  }

  /**
   * Renders an indexed 16x16 frame + 8-color ARGB palette into a crisp ARGB_8888 Bitmap
   * for direct display in the Omni-Fusion Viewport, 3D Voxelizer-FX extrusion, and Game Test.
   */
  fun renderFrameToBitmap(pixels: List<Int>, paletteArgb: List<Long>, scale: Int = 4): Bitmap {
    val safeScale = scale.coerceIn(1, 16)
    val dim = GRID_SIZE * safeScale
    val bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
    for (row in 0 until GRID_SIZE) {
      for (col in 0 until GRID_SIZE) {
        val slot = pixels.getOrElse(row * GRID_SIZE + col) { -1 }
        val colorInt = if (slot in paletteArgb.indices) paletteArgb[slot].toInt() else Color.TRANSPARENT
        for (dy in 0 until safeScale) {
          for (dx in 0 until safeScale) {
            bmp.setPixel(col * safeScale + dx, row * safeScale + dy, colorInt)
          }
        }
      }
    }
    return bmp
  }
}

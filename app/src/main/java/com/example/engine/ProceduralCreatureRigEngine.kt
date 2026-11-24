package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.local.AtlasSpeciesIndexItem
import com.example.data.local.CreatureAnatomyClass
import com.example.data.local.CreatureVariantForm
import com.example.data.local.NationalAtlasIndex
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-JVM representation of a procedurally generated 32x32 indexed creature sprite +
 * per-pixel voxel thickness map (`depthMap`, 0..9) for multi-slice 3D cel-voxel rendering.
 */
data class ProceduralSpriteMatrix(
  val dexNumber: Int,
  val form: CreatureVariantForm,
  val width: Int,
  val height: Int,
  val pixels: IntArray, // -1 = transparent, 0..7 = palette index (0 = ink outline)
  val depthMap: ByteArray, // 0 = empty, 1..9 = voxel half-thickness
  val paletteArgb: LongArray, // 8 ARGB colors (0xAARRGGBB)
) {
  fun opaquePixelCount(): Int = pixels.count { it >= 0 }
  fun transparentPixelCount(): Int = pixels.count { it < 0 }
  fun inkPixelCount(): Int = pixels.count { it == 0 }
}

/**
 * 100% Clean-Room Offline Procedural Creature Rig Engine.
 *
 * Deterministically synthesizes high-craft 32x32 indexed 2D pixel sprites AND
 * layered multi-slice 3D cel-voxel rigs for all #0001-#1025 species + alternate forms
 * (Regional, Mega-Resonance, Paradox-Echo, Prism-Awakened) using 7 modular anatomy rigs,
 * directional hue-shifted Shovel Knight+ shading, crisp 1px ink outlines, and LRU bitmap caching.
 */
object ProceduralCreatureRigEngine {
  const val SPRITE_DIM = 32
  private const val PIXEL_COUNT = SPRITE_DIM * SPRITE_DIM
  private const val MAX_BITMAP_CACHE = 180

  private val bitmapCache = object : LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean =
      size > MAX_BITMAP_CACHE
  }

  private val matrixCache = object : LinkedHashMap<String, ProceduralSpriteMatrix>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ProceduralSpriteMatrix>?): Boolean =
      size > 240
  }

  /**
   * Generates (or retrieves from cache) the deterministic 32x32 indexed sprite & 3D voxel depth map.
   * Pure Kotlin — runs identically on Android runtime and pure JVM unit tests.
   */
  fun generateIndexedSprite32(
    dexNumber: Int,
    form: CreatureVariantForm = CreatureVariantForm.STANDARD,
    frameIndex: Int = 0,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
  ): ProceduralSpriteMatrix {
    val safeDex = dexNumber.coerceIn(1, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES)
    val safeFrame = frameIndex and 1
    val key = "$safeDex:${form.token}:$safeFrame:${customPrimaryHex.orEmpty()}:${customSecondaryHex.orEmpty()}"
    synchronized(matrixCache) {
      matrixCache[key]?.let { return it }
    }

    val species = NationalAtlasIndex.peekSpecies(safeDex)
    val palette = buildShovelKnightPalette(
      species = species,
      form = form,
      overridePrimaryHex = customPrimaryHex,
      overrideSecondaryHex = customSecondaryHex,
    )

    val grid = IntArray(PIXEL_COUNT) { -1 }
    val depth = ByteArray(PIXEL_COUNT) { 0 }
    val auraMask = BooleanArray(PIXEL_COUNT) { false }

    val seed = hashSeed(safeDex, species.familyId, form.ordinal)
    val bobY = if (safeFrame == 1) -1 else 0
    val roleScale = when (species.resonanceRoleTier) {
      "Legendary / Mythical" -> 1.08f
      "Paradox / Ultra" -> 1.04f
      "Regional Ace" -> 1.02f
      "Starter" -> if (species.evolvesToDex == null) 1.02f else 0.90f
      else -> if (species.evolvesToDex == null) 0.96f else 0.86f
    }

    // 1. Sculpt anatomy silhouette + directional depth map
    when (species.anatomy) {
      CreatureAnatomyClass.QUADRUPED ->
        sculptQuadruped(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.AVIAN_WINGED ->
        sculptAvianWinged(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.SERPENTINE_DRACONIC ->
        sculptSerpentineDraconic(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.BIPEDAL_FIGHTER ->
        sculptBipedalFighter(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.AMORPHOUS_SPECTRAL ->
        sculptAmorphousSpectral(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.CRYSTALLINE_MECHA ->
        sculptCrystallineMecha(grid, depth, seed, roleScale, bobY, species, form)
      CreatureAnatomyClass.AQUATIC ->
        sculptAquatic(grid, depth, seed, roleScale, bobY, species, form)
    }

    // 2. Apply top-left directional hue-shifted cel shading across body pixels
    applyCelShadingPass(grid, depth)

    // 3. Apply 1px dark hue-shifted Shovel Knight+ ink outline around solid body
    applyInkOutlinePass(grid, depth)

    // 4. Overlay expressive eyes + form/elemental aura accents (outside/around outline)
    stampElementalAndFormAuras(grid, depth, auraMask, seed, bobY, species, form)

    val matrix = ProceduralSpriteMatrix(
      dexNumber = safeDex,
      form = form,
      width = SPRITE_DIM,
      height = SPRITE_DIM,
      pixels = grid,
      depthMap = depth,
      paletteArgb = palette,
    )
    synchronized(matrixCache) {
      matrixCache[key] = matrix
    }
    return matrix
  }

  /**
   * Renders a crisp, nearest-neighbor 2D pixel sprite Bitmap (`ARGB_8888`, transparent background).
   */
  fun renderSpriteBitmap32(
    dexNumber: Int,
    form: CreatureVariantForm = CreatureVariantForm.STANDARD,
    frameIndex: Int = 0,
    scale: Int = 4,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
  ): Bitmap {
    val safeScale = scale.coerceIn(1, 8)
    val safeFrame = frameIndex and 1
    val key = "2d:$dexNumber:${form.token}:$safeFrame:$safeScale:${customPrimaryHex.orEmpty()}:${customSecondaryHex.orEmpty()}"
    synchronized(bitmapCache) {
      bitmapCache[key]?.let { return it }
    }

    val matrix = generateIndexedSprite32(dexNumber, form, safeFrame, customPrimaryHex, customSecondaryHex)
    val dim = SPRITE_DIM * safeScale
    val argbBuffer = IntArray(dim * dim) { 0 }

    for (y in 0 until SPRITE_DIM) {
      for (x in 0 until SPRITE_DIM) {
        val slot = matrix.pixels[y * SPRITE_DIM + x]
        if (slot !in matrix.paletteArgb.indices) continue
        val colorInt = matrix.paletteArgb[slot].toInt()
        val baseY = y * safeScale
        val baseX = x * safeScale
        for (dy in 0 until safeScale) {
          val rowOffset = (baseY + dy) * dim + baseX
          for (dx in 0 until safeScale) {
            argbBuffer[rowOffset + dx] = colorInt
          }
        }
      }
    }

    val bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
    bmp.setPixels(argbBuffer, 0, dim, 0, 0, dim, dim)
    synchronized(bitmapCache) {
      bitmapCache[key] = bmp
    }
    return bmp
  }

  /**
   * Renders a sculpted multi-slice 3D Cel-Voxel model Bitmap (`ARGB_8888`, transparent background)
   * by extruding the creature's depthMap along Z with isometric yaw, darkened bevel sidewalls,
   * directional cel lighting, and top-left rim highlights.
   */
  fun renderCelVoxelModelBitmap(
    dexNumber: Int,
    form: CreatureVariantForm = CreatureVariantForm.STANDARD,
    yawDegrees: Float = 20f,
    scale: Int = 4,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
  ): Bitmap {
    val safeScale = scale.coerceIn(2, 8)
    val yawBucket = ((yawDegrees / 5f).roundToInt() * 5).coerceIn(-35, 35)
    val key = "3d:$dexNumber:${form.token}:$yawBucket:$safeScale:${customPrimaryHex.orEmpty()}:${customSecondaryHex.orEmpty()}"
    synchronized(bitmapCache) {
      bitmapCache[key]?.let { return it }
    }

    val matrix = generateIndexedSprite32(dexNumber, form, 0, customPrimaryHex, customSecondaryHex)
    val dim = SPRITE_DIM * safeScale
    val canvas = IntArray(dim * dim) { 0 }
    val zBuffer = IntArray(dim * dim) { Int.MIN_VALUE }

    val sliceCount = 9
    val yawSlope = sin(Math.toRadians(yawBucket.toDouble())).toFloat() * 1.45f
    val pitchSlope = 0.42f

    // Back-to-front depth extrusion across Z = -4 .. +4
    for (z in -4..4) {
      val absZ = abs(z)
      val shiftXPx = (z * yawSlope * (safeScale * 0.42f)).roundToInt()
      val shiftYPx = (-z * pitchSlope * (safeScale * 0.34f)).roundToInt()
      // Depth lighting curve: rear slices darker cool shade, front slice bright cel + rim
      val depthShade = (0.58f + ((z + 4) / 8f) * 0.48f).coerceIn(0.45f, 1.08f)

      for (y in 0 until SPRITE_DIM) {
        for (x in 0 until SPRITE_DIM) {
          val idx = y * SPRITE_DIM + x
          val slot = matrix.pixels[idx]
          if (slot !in matrix.paletteArgb.indices) continue
          val thickness = matrix.depthMap[idx].toInt().coerceAtLeast(1)
          val maxHalfZ = (thickness / 2).coerceIn(0, 4)
          if (absZ > maxHalfZ) continue

          val baseArgb = matrix.paletteArgb[slot].toInt()
          val isFrontCap = (z == maxHalfZ)
          val isRimEdge = isFrontCap && (x <= 1 || y <= 1 || matrix.depthMap[max(0, y - 1) * SPRITE_DIM + x] < thickness)

          val shadedColor = when {
            slot == 0 && !isFrontCap -> darkenVoxelBevel(baseArgb, 0.72f)
            isRimEdge && slot != 0 -> blendColorToward(baseArgb, 0xFFFFFFFF.toInt(), 0.24f)
            isFrontCap -> modulateColor(baseArgb, 1.04f)
            else -> darkenVoxelBevel(baseArgb, depthShade * 0.86f)
          }

          val dstBaseX = x * safeScale + shiftXPx
          val dstBaseY = y * safeScale + shiftYPx
          val zScore = (z + 8) * 100 + (if (isFrontCap) 10 else 0)

          for (dy in 0 until safeScale) {
            val py = dstBaseY + dy
            if (py !in 0 until dim) continue
            val rowOff = py * dim
            for (dx in 0 until safeScale) {
              val px = dstBaseX + dx
              if (px !in 0 until dim) continue
              val outIdx = rowOff + px
              if (zScore >= zBuffer[outIdx]) {
                zBuffer[outIdx] = zScore
                canvas[outIdx] = shadedColor
              }
            }
          }
        }
      }
    }

    // Subtle specular cel gleam on upper-left core voxels for unmistakable 3D model finish
    if (form == CreatureVariantForm.PRISM_AWAKENED || form == CreatureVariantForm.MEGA_RESONANCE) {
      val rimAccent = matrix.paletteArgb[7].toInt()
      for (y in 1 until dim - 1) {
        for (x in 1 until dim - 1) {
          val i = y * dim + x
          if (canvas[i] != 0 && canvas[i - 1] == 0 && ((x + y) % 3 == 0)) {
            canvas[i] = blendColorToward(canvas[i], rimAccent, 0.45f)
          }
        }
      }
    }

    val bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
    bmp.setPixels(canvas, 0, dim, 0, 0, dim, dim)
    synchronized(bitmapCache) {
      bitmapCache[key] = bmp
    }
    return bmp
  }

  /**
   * Generates depth-filtered slice Bitmaps for `VOXELIZER_FX_3D` extrusion mode so inner core
   * slices extrude thicker than thin wings/tails/outlines.
   */
  fun renderVoxelSliceBitmaps(
    dexNumber: Int,
    form: CreatureVariantForm = CreatureVariantForm.STANDARD,
    sliceCount: Int = 8,
    scale: Int = 3,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
  ): List<Bitmap> {
    val safeSlices = sliceCount.coerceIn(4, 12)
    val matrix = generateIndexedSprite32(dexNumber, form, 0, customPrimaryHex, customSecondaryHex)
    val dim = SPRITE_DIM * scale
    return List(safeSlices) { sliceIdx ->
      val key = "sl:$dexNumber:${form.token}:$sliceIdx/$safeSlices:$scale:${customPrimaryHex.orEmpty()}:${customSecondaryHex.orEmpty()}"
      synchronized(bitmapCache) {
        bitmapCache[key]?.let { return@List it }
      }
      val distFromCenter = abs(sliceIdx - (safeSlices - 1) / 2f) / ((safeSlices - 1) / 2f).coerceAtLeast(1f)
      val requiredDepth = (1 + distFromCenter * 6f).roundToInt()
      val buf = IntArray(dim * dim) { 0 }
      for (y in 0 until SPRITE_DIM) {
        for (x in 0 until SPRITE_DIM) {
          val idx = y * SPRITE_DIM + x
          val slot = matrix.pixels[idx]
          if (slot !in matrix.paletteArgb.indices) continue
          if (matrix.depthMap[idx].toInt() < requiredDepth && sliceIdx != 0) continue
          val col = matrix.paletteArgb[slot].toInt()
          val by = y * scale
          val bx = x * scale
          for (dy in 0 until scale) {
            val rowOff = (by + dy) * dim + bx
            for (dx in 0 until scale) {
              buf[rowOff + dx] = col
            }
          }
        }
      }
      val bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
      bmp.setPixels(buf, 0, dim, 0, 0, dim, dim)
      synchronized(bitmapCache) {
        bitmapCache[key] = bmp
      }
      bmp
    }
  }

  /**
   * Cleanly samples any procedural creature into an editable 2-frame 16x16 Sprite Editor rig
   * plus its exact 8-color PPU palette — 100% offline, zero network required!
   */
  fun sampleToSpriteEditor16(
    dexNumber: Int,
    form: CreatureVariantForm = CreatureVariantForm.STANDARD,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
  ): Pair<List<Long>, List<List<Int>>> {
    val m0 = generateIndexedSprite32(dexNumber, form, 0, customPrimaryHex, customSecondaryHex)
    val m1 = generateIndexedSprite32(dexNumber, form, 1, customPrimaryHex, customSecondaryHex)
    val f0 = downsample32To16PreservingInkAndEyes(m0.pixels)
    val f1 = downsample32To16PreservingInkAndEyes(m1.pixels)
    // Re-order palette so slot 0 = primary base, slot 1 = secondary base, slot 2 = highlight, slot 3 = ink outline
    // matching SpritePixelMatrixEngine's convention!
    val p = m0.paletteArgb
    val editorPalette = listOf(
      p[2], // 0: Primary Base
      p[5], // 1: Secondary Base
      p[3], // 2: Primary Highlight
      p[0], // 3: Ink Outline
      p[7], // 4: Elemental Energy / Iris
      p[1], // 5: Primary Shadow
      p[6], // 6: Specular White
      p[4], // 7: Secondary Shadow
    )
    val remap = intArrayOf(3, 5, 0, 2, 7, 1, 6, 4)
    fun mapFrame(src: List<Int>): List<Int> = src.map { slot -> if (slot in remap.indices) remap[slot] else -1 }
    return editorPalette to listOf(mapFrame(f0), mapFrame(f1))
  }

  private fun downsample32To16PreservingInkAndEyes(pixels32: IntArray): List<Int> {
    val out = MutableList(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 }
    for (r in 0 until 16) {
      for (c in 0 until 16) {
        val block = intArrayOf(
          pixels32[(r * 2) * SPRITE_DIM + (c * 2)],
          pixels32[(r * 2) * SPRITE_DIM + (c * 2 + 1)],
          pixels32[(r * 2 + 1) * SPRITE_DIM + (c * 2)],
          pixels32[(r * 2 + 1) * SPRITE_DIM + (c * 2 + 1)],
        )
        // Priority: eye/specular(6,7) > body/accent(2,3,5,1,4) > ink(0) > transparent(-1)
        val picked = when {
          block.any { it == 7 } -> 7
          block.any { it == 6 } -> 6
          block.count { it >= 1 } >= 2 -> block.first { it >= 1 }
          block.any { it in 1..5 } && block.any { it == 0 } -> block.first { it in 1..5 }
          block.any { it == 0 } -> 0
          else -> -1
        }
        out[r * 16 + c] = picked
      }
    }
    return out
  }

  // ============================================================================================
  // 7 ANATOMY SCULPTORS (32x32 grid with depth map)
  // ============================================================================================

  private fun sculptQuadruped(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val bodyW = (5.5f + (seed % 3) * 0.7f) * scale
    val bodyH = (3.8f + ((seed shr 2) % 3) * 0.5f) * scale
    val cy = 19 + bobY

    // Sweeping tail on right
    val tailStyle = (seed shr 4) % 3
    val tailColor = if ((seed and 1) == 0) 5 else 2
    for (i in 0..8) {
      val tx = (21 + i * 0.85f).roundToInt()
      val ty = (cy - 1 - sin(i * 0.55f + tailStyle) * 3.2f - i * 0.45f).roundToInt()
      val r = if (i > 5 && tailStyle == 2) 2.1f else 1.2f
      drawBlob(g, d, tx.toFloat(), ty.toFloat(), r, r, tailColor, 4)
    }

    // Back & front legs
    val legH = (5 + ((seed shr 6) % 2))
    drawCapsule(g, d, 12f, (cy + 2).toFloat(), 11f, (cy + legH + 2).toFloat(), 1.5f, 1, 4)
    drawCapsule(g, d, 20f, (cy + 2).toFloat(), 21f, (cy + legH + 2).toFloat(), 1.5f, 1, 4)
    drawCapsule(g, d, 14f, (cy + 2).toFloat(), 13f, (cy + legH + 3).toFloat(), 1.6f, 2, 5)
    drawCapsule(g, d, 18f, (cy + 2).toFloat(), 19f, (cy + legH + 3).toFloat(), 1.6f, 2, 5)

    // Main torso + belly/back crest
    drawBlob(g, d, 16.5f, cy.toFloat(), bodyW, bodyH, 2, 8)
    drawBlob(g, d, 16.5f, (cy + 1.5f), bodyW * 0.65f, bodyH * 0.48f, 5, 7)

    // Dorsal bulb / mane / armor plates
    if (species.primaryType in setOf("Grass", "Fire", "Ice", "Rock", "Steel") || form != CreatureVariantForm.STANDARD) {
      drawBlob(g, d, 17f, (cy - bodyH * 0.85f), bodyW * 0.52f, 2.4f, 5, 7)
    }

    // Expressive Head (front-left 3/4 view)
    val hx = 10.5f
    val hy = (cy - 4.5f)
    val hr = (3.9f + ((seed shr 8) % 2) * 0.6f) * scale
    drawBlob(g, d, hx, hy, hr, hr * 0.92f, 2, 8)

    // Ears / Horns / Crest
    drawHeadCrest(g, d, hx, hy - hr * 0.7f, seed, form, mirrored = false)

    // Expressive eyes + cheek spark
    stampEye(g, d, (hx - 1.5f).roundToInt(), (hy - 0.3f).roundToInt(), isSharp = species.resonanceRoleTier != "Wild Resonance")
    stampEye(g, d, (hx + 1.5f).roundToInt(), (hy - 0.3f).roundToInt(), isSharp = species.resonanceRoleTier != "Wild Resonance")
    if (species.primaryType == "Electric" || species.primaryType == "Fairy") {
      setPx(g, d, (hx - 2.3f).roundToInt(), (hy + 1.2f).roundToInt(), 7, 8)
      setPx(g, d, (hx + 2.2f).roundToInt(), (hy + 1.2f).roundToInt(), 7, 8)
    }
  }

  private fun sculptAvianWinged(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val cx = 16f
    val cy = 17f + bobY
    val wingFlap = if (bobY != 0) -2.2f else 0.8f

    // Sweeping layered wings (left & right)
    val wingSpan = (9.5f + (seed % 3) * 1.1f) * scale
    for (side in listOf(-1f, 1f)) {
      for (i in 0..9) {
        val t = i / 9f
        val wx = cx + side * (3f + t * wingSpan)
        val wy = cy - 2f + sin(t * 2.4f) * (-3.6f) + t * wingFlap
        val thickness = (2.8f - t * 1.3f).coerceAtLeast(1.1f)
        drawBlob(g, d, wx, wy, thickness, thickness * 1.2f, if (i % 3 == 0) 5 else 2, (6 - t * 3f).roundToInt().coerceAtLeast(2))
        // Feather tips
        if (i in 4..8) {
          drawCapsule(g, d, wx, wy, wx + side * 1.4f, wy + 2.8f, 0.9f, 5, 3)
        }
      }
    }

    // Tail plumage
    drawCapsule(g, d, cx, cy + 4f, cx - 2.5f, cy + 9.5f, 1.4f, 5, 4)
    drawCapsule(g, d, cx, cy + 4f, cx + 2.5f, cy + 9.5f, 1.4f, 5, 4)
    drawCapsule(g, d, cx, cy + 4f, cx, cy + 10f, 1.5f, 2, 5)

    // Chest torso
    drawBlob(g, d, cx, cy + 1f, 4.2f * scale, 5.2f * scale, 2, 8)
    drawBlob(g, d, cx, cy + 1.8f, 2.7f * scale, 3.4f * scale, 5, 8)

    // Talons
    drawCapsule(g, d, cx - 2f, cy + 5.5f, cx - 2.5f, cy + 8f, 0.9f, 7, 4)
    drawCapsule(g, d, cx + 2f, cy + 5.5f, cx + 2.5f, cy + 8f, 0.9f, 7, 4)

    // Head + Beak + Plumage Crest
    val hy = cy - 5.2f
    drawBlob(g, d, cx, hy, 3.4f * scale, 3.2f * scale, 2, 8)
    drawHeadCrest(g, d, cx, hy - 3f, seed, form, mirrored = true)
    // Hooked beak
    drawBlob(g, d, cx, hy + 1.4f, 1.3f, 1.1f, 7, 9)
    stampEye(g, d, (cx - 1.8f).roundToInt(), (hy - 0.3f).roundToInt(), isSharp = true)
    stampEye(g, d, (cx + 1.8f).roundToInt(), (hy - 0.3f).roundToInt(), isSharp = true)
  }

  private fun sculptSerpentineDraconic(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val hasWings = species.secondaryType == "Flying" || species.primaryType == "Dragon" || form == CreatureVariantForm.MEGA_RESONANCE
    val cy = 16f + bobY

    // Optional draconic wings behind S-coil
    if (hasWings) {
      for (side in listOf(-1f, 1f)) {
        drawCapsule(g, d, 16f, cy - 2f, 16f + side * 11f, cy - 7f, 1.4f, 5, 4)
        drawBlob(g, d, 16f + side * 7.5f, cy - 3.5f, 3.8f, 2.8f, 5, 3)
        drawCapsule(g, d, 16f + side * 11f, cy - 7f, 16f + side * 9f, cy + 1f, 1.1f, 2, 3)
      }
    }

    // S-curve serpentine body with belly scutes
    val segments = 18
    for (i in segments downTo 0) {
      val t = i / segments.toFloat()
      val sx = 16f + sin(t * 5.2f + (seed % 3)) * 6.8f
      val sy = 8.5f + t * 16.5f + bobY
      val radius = (3.4f - abs(t - 0.45f) * 2.6f).coerceIn(1.3f, 3.6f) * scale
      drawBlob(g, d, sx, sy, radius, radius * 0.9f, 2, (8 - abs(t - 0.4f) * 5f).roundToInt().coerceIn(3, 8))
      if (i % 2 == 0 && i in 2..15) {
        drawBlob(g, d, sx, sy + 0.8f, radius * 0.55f, radius * 0.45f, 5, 8)
      }
      if (i % 3 == 0 && i in 3..14) {
        setPx(g, d, (sx - radius * 0.9f).roundToInt(), (sy - 1f).roundToInt(), 7, 5)
      }
    }

    // Fierce Draconic Head at top of S-coil
    val hx = 16f + sin((seed % 3).toFloat()) * 4.5f
    val hy = 8.5f + bobY
    drawBlob(g, d, hx, hy, 4.0f * scale, 3.2f * scale, 2, 9)
    drawBlob(g, d, hx - 2.2f, hy + 0.6f, 2.2f, 1.6f, 2, 8) // Snout
    drawHeadCrest(g, d, hx + 1f, hy - 2.6f, seed + 3, form, mirrored = false)
    stampEye(g, d, (hx - 1.2f).roundToInt(), (hy - 0.6f).roundToInt(), isSharp = true)
    setPx(g, d, (hx - 3.2f).roundToInt(), (hy + 1.2f).roundToInt(), 6, 9) // Fang glint
  }

  private fun sculptBipedalFighter(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val cx = 16f
    val cy = 17f + bobY

    // Tail or cape behind torso
    if ((seed and 2) == 0) {
      drawCapsule(g, d, cx + 2f, cy + 3f, cx + 9f, cy + 1f, 1.5f, 5, 4)
      drawBlob(g, d, cx + 9.5f, cy + 0.5f, 2.0f, 2.0f, 7, 5)
    }

    // Planted legs
    drawCapsule(g, d, cx - 2.5f, cy + 4f, cx - 4.2f, cy + 9.5f, 1.8f, 2, 6)
    drawCapsule(g, d, cx + 2.5f, cy + 4f, cx + 4.2f, cy + 9.5f, 1.8f, 2, 6)
    drawBlob(g, d, cx - 4.8f, cy + 9.8f, 2.0f, 1.2f, 5, 6)
    drawBlob(g, d, cx + 4.8f, cy + 9.8f, 2.0f, 1.2f, 5, 6)

    // Muscular/Heroic torso + chest emblem
    drawBlob(g, d, cx, cy + 0.5f, 4.6f * scale, 5.2f * scale, 2, 8)
    drawBlob(g, d, cx, cy + 0.5f, 2.8f * scale, 3.2f * scale, 5, 9)

    // Raised combat arms / gauntlets / energy blades
    val armLift = if (bobY != 0) -1.5f else 0.5f
    drawCapsule(g, d, cx - 4f, cy - 2f, cx - 8.5f, cy + 0.5f + armLift, 1.6f, 2, 6)
    drawCapsule(g, d, cx + 4f, cy - 2f, cx + 8.5f, cy - 3.5f + armLift, 1.6f, 2, 6)
    drawBlob(g, d, cx - 9f, cy + 0.8f + armLift, 2.0f, 1.9f, 5, 7)
    drawBlob(g, d, cx + 9f, cy - 3.8f + armLift, 2.1f, 2.0f, 7, 7)

    // Head + helmet/ears
    val hy = cy - 6.2f
    drawBlob(g, d, cx, hy, 3.8f * scale, 3.6f * scale, 2, 9)
    drawHeadCrest(g, d, cx, hy - 3.2f, seed, form, mirrored = true)
    stampEye(g, d, (cx - 1.7f).roundToInt(), (hy - 0.2f).roundToInt(), isSharp = true)
    stampEye(g, d, (cx + 1.7f).roundToInt(), (hy - 0.2f).roundToInt(), isSharp = true)
  }

  private fun sculptAmorphousSpectral(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val cx = 16f
    val cy = 15f + bobY * 2

    // Undulating spectral cloak & wisp tendrils at bottom
    for (i in -3..3) {
      val wx = cx + i * 2.2f
      val len = 6.5f + ((abs(i + seed) % 3) * 1.4f)
      val sway = sin((i + bobY).toDouble()).toFloat() * 1.4f
      drawCapsule(g, d, wx, cy + 2f, wx + sway, cy + len, 1.4f, if (abs(i) == 3) 5 else 2, 4)
    }

    // Core floating orb / hooded phantom body
    drawBlob(g, d, cx, cy, 6.2f * scale, 5.8f * scale, 2, 8)
    drawBlob(g, d, cx, cy - 1f, 4.2f * scale, 3.8f * scale, 5, 8)

    // Crown spikes / spectral horns
    drawHeadCrest(g, d, cx, cy - 5.4f, seed + 5, form, mirrored = true)

    // Floating side spirit wisps
    val orbY = cy - 1f + if (bobY != 0) 2f else -1f
    drawBlob(g, d, cx - 9.2f, orbY, 1.7f, 1.7f, 7, 5)
    drawBlob(g, d, cx + 9.2f, orbY - 1.5f, 1.7f, 1.7f, 7, 5)

    // Glowing spectral eyes + mischievous grin
    stampEye(g, d, (cx - 2.4f).roundToInt(), (cy - 1f).roundToInt(), isSharp = true)
    stampEye(g, d, (cx + 2.4f).roundToInt(), (cy - 1f).roundToInt(), isSharp = true)
    for (gx in -2..2) {
      setPx(g, d, (cx + gx).roundToInt(), (cy + 2f - abs(gx) * 0.25f).roundToInt(), 6, 9)
    }
  }

  private fun sculptCrystallineMecha(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val cx = 16f
    val cy = 16f + bobY

    // Floating orbital prism shields / mecha shoulder fins
    for (side in listOf(-1f, 1f)) {
      drawDiamond(g, d, cx + side * 9.5f, cy - 1f + side * bobY, 2.8f, 4.4f, 5, 5)
      drawDiamond(g, d, cx + side * 9.5f, cy - 1f + side * bobY, 1.4f, 2.2f, 7, 6)
    }

    // Faceted octagonal armored chassis
    drawDiamond(g, d, cx, cy, 6.6f * scale, 7.6f * scale, 2, 9)
    drawDiamond(g, d, cx, cy, 4.2f * scale, 4.8f * scale, 5, 9)
    drawBlob(g, d, cx, cy, 2.2f, 2.2f, 7, 9) // Reactor core

    // Armored mecha legs / hover thrusters
    drawCapsule(g, d, cx - 3.5f, cy + 5f, cx - 5f, cy + 9.5f, 1.8f, 2, 6)
    drawCapsule(g, d, cx + 3.5f, cy + 5f, cx + 5f, cy + 9.5f, 1.8f, 2, 6)

    // Geometric crest & glowing visor strip
    drawHeadCrest(g, d, cx, cy - 7f, seed + 2, form, mirrored = true)
    for (vx in -3..3) {
      setPx(g, d, (cx + vx).roundToInt(), (cy - 3).roundToInt(), if (abs(vx) <= 1) 6 else 7, 9)
    }
  }

  private fun sculptAquatic(
    g: IntArray, d: ByteArray, seed: Int, scale: Float, bobY: Int,
    species: AtlasSpeciesIndexItem, form: CreatureVariantForm,
  ) {
    val cx = 15.5f
    val cy = 17f + bobY

    // Flowing caudal tail fluke on right
    drawCapsule(g, d, cx + 4f, cy + 1f, cx + 9.5f, cy - 2f, 1.7f, 2, 5)
    drawBlob(g, d, cx + 10.5f, cy - 4.2f, 2.4f, 1.8f, 5, 4)
    drawBlob(g, d, cx + 10.5f, cy + 0.2f, 2.4f, 1.8f, 5, 4)

    // Dorsal fin / hydrodynamic shell
    drawDiamond(g, d, cx + 0.5f, cy - 5.5f, 3.2f, 3.5f, 5, 5)

    // Sleek torso / shell + ventral belly
    drawBlob(g, d, cx, cy, 6.4f * scale, 4.6f * scale, 2, 8)
    drawBlob(g, d, cx - 0.5f, cy + 1.8f, 4.8f * scale, 2.4f * scale, 5, 8)

    // Pectoral fins / flippers
    drawCapsule(g, d, cx - 1f, cy + 2f, cx - 4.5f, cy + 7.2f, 1.6f, 5, 5)
    drawCapsule(g, d, cx + 2.5f, cy + 2f, cx + 5f, cy + 6.8f, 1.4f, 2, 4)

    // Head + gill frills / coral crest
    val hx = cx - 4.5f
    val hy = cy - 2.2f
    drawBlob(g, d, hx, hy, 3.6f * scale, 3.3f * scale, 2, 8)
    drawHeadCrest(g, d, hx, hy - 3f, seed + 1, form, mirrored = false)
    stampEye(g, d, (hx - 1.5f).roundToInt(), (hy - 0.4f).roundToInt(), isSharp = false)
    stampEye(g, d, (hx + 1.3f).roundToInt(), (hy - 0.4f).roundToInt(), isSharp = false)
  }

  private fun drawHeadCrest(
    g: IntArray, d: ByteArray, cx: Float, topY: Float,
    seed: Int, form: CreatureVariantForm, mirrored: Boolean,
  ) {
    val crestType = (seed shr 3) % 4
    when (crestType) {
      0 -> {
        // Pointed fox/wolf ears
        drawCapsule(g, d, cx - 2.2f, topY + 1f, cx - 3.6f, topY - 3.2f, 1.2f, 5, 5)
        drawCapsule(g, d, cx + 2.2f, topY + 1f, cx + 3.6f, topY - 3.2f, 1.2f, 5, 5)
      }
      1 -> {
        // Swept twin horns
        drawCapsule(g, d, cx - 1.8f, topY + 1f, cx - 4.8f, topY - 2.6f, 1.1f, 7, 5)
        drawCapsule(g, d, cx + 1.8f, topY + 1f, cx + 4.8f, topY - 2.6f, 1.1f, 7, 5)
      }
      2 -> {
        // Central crystal/plume crest
        drawDiamond(g, d, cx, topY - 1.4f, 2.2f, 3.2f, 7, 6)
        if (mirrored) {
          drawBlob(g, d, cx - 2.6f, topY + 0.4f, 1.3f, 1.3f, 5, 5)
          drawBlob(g, d, cx + 2.6f, topY + 0.4f, 1.3f, 1.3f, 5, 5)
        }
      }
      else -> {
        // Leaf / crown triad
        drawCapsule(g, d, cx, topY + 1f, cx, topY - 3.5f, 1.2f, 5, 5)
        drawCapsule(g, d, cx - 1.5f, topY + 0.8f, cx - 3.5f, topY - 2.0f, 1.0f, 5, 4)
        drawCapsule(g, d, cx + 1.5f, topY + 0.8f, cx + 3.5f, topY - 2.0f, 1.0f, 5, 4)
      }
    }

    // Form-specific crown mutation
    when (form) {
      CreatureVariantForm.MEGA_RESONANCE -> {
        drawDiamond(g, d, cx, (topY - 3.2f).coerceAtLeast(3f), 1.8f, 2.4f, 7, 7)
      }
      CreatureVariantForm.PRISM_AWAKENED -> {
        val crownY = (topY - 3.4f).coerceAtLeast(3f)
        drawDiamond(g, d, cx, crownY, 2.6f, 2.2f, 7, 7)
        setPx(g, d, cx.roundToInt(), crownY.roundToInt(), 6, 8)
      }
      CreatureVariantForm.PARADOX_ECHO -> {
        val barY = (topY - 2.4f).coerceAtLeast(3f).roundToInt()
        for (x in -3..3 step 2) {
          setPx(g, d, (cx + x).roundToInt(), barY, 7, 6)
        }
      }
      else -> Unit
    }
  }

  private fun stampElementalAndFormAuras(
    g: IntArray,
    d: ByteArray,
    auraMask: BooleanArray,
    seed: Int,
    bobY: Int,
    species: AtlasSpeciesIndexItem,
    form: CreatureVariantForm,
  ) {
    // Legendary / Mega / Paradox / Prism orbiting energy sparkles (no outline so they glow crisp)
    val auraCount = when {
      form == CreatureVariantForm.PRISM_AWAKENED -> 8
      form == CreatureVariantForm.MEGA_RESONANCE -> 6
      form == CreatureVariantForm.PARADOX_ECHO -> 6
      species.resonanceRoleTier == "Legendary / Mythical" -> 6
      species.resonanceRoleTier == "Paradox / Ultra" -> 4
      species.resonanceRoleTier == "Regional Ace" -> 2
      else -> 0
    }
    if (auraCount > 0) {
      for (i in 0 until auraCount) {
        val angle = (i.toFloat() / auraCount) * 6.28318f + (seed % 7) * 0.4f + bobY * 0.25f
        val ax = (16f + cos(angle) * 12.2f).roundToInt().coerceIn(2, SPRITE_DIM - 3)
        val ay = (16f + sin(angle) * 11.4f).roundToInt().coerceIn(2, SPRITE_DIM - 3)
        val idx = ay * SPRITE_DIM + ax
        if (g[idx] == -1) {
          g[idx] = if (i % 2 == 0) 7 else 6
          d[idx] = 2
          auraMask[idx] = true
        }
      }
    }
  }

  private fun applyCelShadingPass(g: IntArray, d: ByteArray) {
    // Top-left key light: remap base primary (2) -> highlight (3) or shadow (1),
    // and secondary (5) -> secondary shadow (4) on lower-right faces.
    val copy = g.clone()
    for (y in 2 until SPRITE_DIM - 2) {
      for (x in 2 until SPRITE_DIM - 2) {
        val idx = y * SPRITE_DIM + x
        val slot = copy[idx]
        if (slot != 2 && slot != 5) continue

        val hasTopEmpty = copy[(y - 1) * SPRITE_DIM + x] == -1 || copy[(y - 2) * SPRITE_DIM + x] == -1
        val hasLeftEmpty = copy[y * SPRITE_DIM + (x - 1)] == -1
        val hasBottomEmpty = copy[(y + 1) * SPRITE_DIM + x] == -1 || copy[(y + 2) * SPRITE_DIM + x] == -1
        val hasRightEmpty = copy[y * SPRITE_DIM + (x + 1)] == -1

        if (slot == 2) {
          g[idx] = when {
            hasTopEmpty && (hasLeftEmpty || y < 16) -> 3 // Warm top-left highlight
            hasBottomEmpty || (hasRightEmpty && y > 15) -> 1 // Cool bottom-right shadow
            else -> 2
          }
        } else if (slot == 5 && (hasBottomEmpty || hasRightEmpty)) {
          g[idx] = 4 // Secondary shadow
        }
      }
    }
  }

  private fun applyInkOutlinePass(g: IntArray, d: ByteArray) {
    val src = g.clone()
    for (y in 1 until SPRITE_DIM - 1) {
      for (x in 1 until SPRITE_DIM - 1) {
        val idx = y * SPRITE_DIM + x
        if (src[idx] != -1) continue
        val n = src[(y - 1) * SPRITE_DIM + x]
        val s = src[(y + 1) * SPRITE_DIM + x]
        val w = src[y * SPRITE_DIM + (x - 1)]
        val e = src[y * SPRITE_DIM + (x + 1)]
        if (n >= 1 || s >= 1 || w >= 1 || e >= 1) {
          g[idx] = 0
          d[idx] = 2
        }
      }
    }
  }

  private fun stampEye(g: IntArray, d: ByteArray, x: Int, y: Int, isSharp: Boolean) {
    setPx(g, d, x, y, 0, 9) // Pupil / lash
    setPx(g, d, x, y - 1, 6, 9) // White glint
    setPx(g, d, x + 1, y, 7, 9) // Glowing iris
    if (isSharp) {
      setPx(g, d, x - 1, y - 1, 0, 9) // Heroic brow pixel
    }
  }

  private fun drawBlob(
    g: IntArray, d: ByteArray, cx: Float, cy: Float, rx: Float, ry: Float, colorSlot: Int, maxDepth: Int,
  ) {
    val minX = max(2, (cx - rx - 1f).toInt())
    val maxX = min(SPRITE_DIM - 3, (cx + rx + 1f).toInt())
    val minY = max(2, (cy - ry - 1f).toInt())
    val maxY = min(SPRITE_DIM - 3, (cy + ry + 1f).toInt())
    val safeRx = rx.coerceAtLeast(0.6f)
    val safeRy = ry.coerceAtLeast(0.6f)
    for (y in minY..maxY) {
      for (x in minX..maxX) {
        val nx = (x - cx) / safeRx
        val ny = (y - cy) / safeRy
        val distSq = nx * nx + ny * ny
        if (distSq <= 1.0f) {
          val localDepth = (maxDepth * sqrt((1.05f - distSq).coerceIn(0.15f, 1f))).roundToInt().coerceIn(1, 9)
          setPx(g, d, x, y, colorSlot, localDepth)
        }
      }
    }
  }

  private fun drawDiamond(
    g: IntArray, d: ByteArray, cx: Float, cy: Float, rx: Float, ry: Float, colorSlot: Int, maxDepth: Int,
  ) {
    val minX = max(2, (cx - rx - 1f).toInt())
    val maxX = min(SPRITE_DIM - 3, (cx + rx + 1f).toInt())
    val minY = max(2, (cy - ry - 1f).toInt())
    val maxY = min(SPRITE_DIM - 3, (cy + ry + 1f).toInt())
    val safeRx = rx.coerceAtLeast(0.6f)
    val safeRy = ry.coerceAtLeast(0.6f)
    for (y in minY..maxY) {
      for (x in minX..maxX) {
        val manhattan = abs(x - cx) / safeRx + abs(y - cy) / safeRy
        if (manhattan <= 1.0f) {
          val localDepth = (maxDepth * (1.05f - manhattan * 0.7f)).roundToInt().coerceIn(1, 9)
          setPx(g, d, x, y, colorSlot, localDepth)
        }
      }
    }
  }

  private fun drawCapsule(
    g: IntArray, d: ByteArray, x0: Float, y0: Float, x1: Float, y1: Float, r: Float, colorSlot: Int, maxDepth: Int,
  ) {
    val steps = max(4, (sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)) * 2f).roundToInt())
    for (s in 0..steps) {
      val t = s / steps.toFloat()
      val px = x0 + (x1 - x0) * t
      val py = y0 + (y1 - y0) * t
      drawBlob(g, d, px, py, r, r, colorSlot, maxDepth)
    }
  }

  private fun setPx(g: IntArray, d: ByteArray, x: Int, y: Int, slot: Int, depthVal: Int) {
    if (x !in 2 until SPRITE_DIM - 2 || y !in 2 until SPRITE_DIM - 2) return
    val idx = y * SPRITE_DIM + x
    g[idx] = slot
    if (depthVal > d[idx].toInt()) {
      d[idx] = depthVal.coerceIn(1, 9).toByte()
    }
  }

  // ============================================================================================
  // HUE-SHIFTED SHOVEL KNIGHT+ PALETTE SYNTHESIZER (PURE JVM — NO ANDROID COLOR.RGBTOHSV NEEDED)
  // ============================================================================================

  private fun buildShovelKnightPalette(
    species: AtlasSpeciesIndexItem,
    form: CreatureVariantForm,
    overridePrimaryHex: String?,
    overrideSecondaryHex: String?,
  ): LongArray {
    val p1 = SpritePixelMatrixEngine.parseHexToArgbLong(overridePrimaryHex ?: species.primaryHex, 0xFF00F5D4)
    val p2 = SpritePixelMatrixEngine.parseHexToArgbLong(overrideSecondaryHex ?: species.secondaryHex, 0xFFFF2A85)

    // Subtle per-species hue uniqueness (+/- 16 deg) + variant form hue shift
    val speciesMicroHue = ((species.dexNumber * 29) % 33) - 16f
    val formHue = form.hueShiftDegrees

    val base1 = shiftHsl(p1, hueDeltaDeg = speciesMicroHue + formHue, satMult = 1.06f, lightMult = 1.0f)
    val shadow1 = shiftHsl(base1, hueDeltaDeg = -20f, satMult = 1.12f, lightMult = 0.56f)
    val highlight1 = shiftHsl(base1, hueDeltaDeg = 16f, satMult = 0.92f, lightMult = 1.28f)
    val ink = shiftHsl(base1, hueDeltaDeg = -32f, satMult = 0.85f, lightMult = 0.16f)

    val base2 = shiftHsl(p2, hueDeltaDeg = -speciesMicroHue * 0.5f + formHue * 0.7f, satMult = 1.08f, lightMult = 1.02f)
    val shadow2 = shiftHsl(base2, hueDeltaDeg = -18f, satMult = 1.12f, lightMult = 0.58f)

    val energy = when (form) {
      CreatureVariantForm.PRISM_AWAKENED -> 0xFF00F5D4L
      CreatureVariantForm.MEGA_RESONANCE -> 0xFFFF2A85L
      CreatureVariantForm.PARADOX_ECHO -> 0xFFFFD166L
      CreatureVariantForm.REGIONAL -> shiftHsl(base2, hueDeltaDeg = 60f, satMult = 1.25f, lightMult = 1.25f)
      CreatureVariantForm.STANDARD -> shiftHsl(base2, hueDeltaDeg = 28f, satMult = 1.25f, lightMult = 1.22f)
    }

    return longArrayOf(
      ink,         // 0: Hue-shifted deep ink outline
      shadow1,     // 1: Primary cool shadow
      base1,       // 2: Primary base
      highlight1,  // 3: Primary warm highlight
      shadow2,     // 4: Secondary shadow
      base2,       // 5: Secondary accent
      0xFFF8FAFC,  // 6: Crisp specular white / eye glint
      energy,      // 7: Glowing iris / elemental energy / crown gem
    )
  }

  private fun shiftHsl(argb: Long, hueDeltaDeg: Float, satMult: Float, lightMult: Float): Long {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    var l = (maxC + minC) / 2f
    var s = if (delta < 0.0001f) 0f else delta / (1f - abs(2f * l - 1f)).coerceAtLeast(0.0001f)
    var h = when {
      delta < 0.0001f -> 0f
      maxC == r -> 60f * (((g - b) / delta) % 6f)
      maxC == g -> 60f * (((b - r) / delta) + 2f)
      else -> 60f * (((r - g) / delta) + 4f)
    }
    if (h < 0f) h += 360f

    h = ((h + hueDeltaDeg) % 360f + 360f) % 360f
    s = (s * satMult).coerceIn(0.18f, 0.98f)
    l = (l * lightMult).coerceIn(0.08f, 0.92f)

    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(((h / 60f) % 2f) - 1f))
    val m = l - c / 2f
    val (rr, gg, bb) = when {
      h < 60f -> Triple(c, x, 0f)
      h < 120f -> Triple(x, c, 0f)
      h < 180f -> Triple(0f, c, x)
      h < 240f -> Triple(0f, x, c)
      h < 300f -> Triple(x, 0f, c)
      else -> Triple(c, 0f, x)
    }
    val ri = ((rr + m) * 255f).roundToInt().coerceIn(0, 255)
    val gi = ((gg + m) * 255f).roundToInt().coerceIn(0, 255)
    val bi = ((bb + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFFL shl 24) or (ri.toLong() shl 16) or (gi.toLong() shl 8) or bi.toLong()
  }

  private fun darkenVoxelBevel(argb: Int, factor: Float): Int {
    val a = (argb ushr 24) and 0xFF
    val r = (((argb shr 16) and 0xFF) * factor * 0.94f).roundToInt().coerceIn(0, 255)
    val g = (((argb shr 8) and 0xFF) * factor * 0.96f).roundToInt().coerceIn(0, 255)
    val b = ((argb and 0xFF) * (factor * 1.06f).coerceAtMost(1f)).roundToInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun modulateColor(argb: Int, factor: Float): Int {
    val a = (argb ushr 24) and 0xFF
    val r = (((argb shr 16) and 0xFF) * factor).roundToInt().coerceIn(0, 255)
    val g = (((argb shr 8) and 0xFF) * factor).roundToInt().coerceIn(0, 255)
    val b = ((argb and 0xFF) * factor).roundToInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun blendColorToward(argb: Int, target: Int, ratio: Float): Int {
    val inv = 1f - ratio
    val a = (argb ushr 24) and 0xFF
    val r = (((argb shr 16) and 0xFF) * inv + ((target shr 16) and 0xFF) * ratio).roundToInt().coerceIn(0, 255)
    val g = (((argb shr 8) and 0xFF) * inv + ((target shr 8) and 0xFF) * ratio).roundToInt().coerceIn(0, 255)
    val b = ((argb and 0xFF) * inv + (target and 0xFF) * ratio).roundToInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun hashSeed(dex: Int, familyId: Int, formOrdinal: Int): Int {
    var h = dex * 0x45d9f3b xor (familyId * 0x119de1f3) xor (formOrdinal * 0x27d4eb2d)
    h = (h xor (h ushr 16)) * 0x45d9f3b
    h = h xor (h ushr 16)
    return abs(h)
  }
}

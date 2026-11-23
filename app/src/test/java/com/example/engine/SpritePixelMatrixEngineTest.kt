package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpritePixelMatrixEngineTest {

  @Test
  fun generateSeededFrame_producesValid16x16IndexedGrid() {
    val frame0 = SpritePixelMatrixEngine.generateSeededFrame(384, frameOffset = 0)
    val frame1 = SpritePixelMatrixEngine.generateSeededFrame(384, frameOffset = 1)

    assertEquals(SpritePixelMatrixEngine.PIXEL_COUNT, frame0.size)
    assertEquals(SpritePixelMatrixEngine.PIXEL_COUNT, frame1.size)
    assertTrue(frame0.all { it in -1..7 })
    assertTrue(frame1.all { it in -1..7 })
    assertTrue(frame0.any { it >= 0 })
  }

  @Test
  fun buildInitialPalette_parsesPrimaryAndSecondaryHexes() {
    val palette = SpritePixelMatrixEngine.buildInitialPalette("#10B981", "#FFD166")

    assertEquals(8, palette.size)
    assertEquals("#10B981", SpritePixelMatrixEngine.argbLongToHex(palette[0]))
    assertEquals("#FFD166", SpritePixelMatrixEngine.argbLongToHex(palette[1]))
  }

  @Test
  fun floodFill_replacesOnlyContiguousMatchingRegion() {
    val grid = MutableList(SpritePixelMatrixEngine.PIXEL_COUNT) { -1 }
    // Create a 3x3 enclosed region at top-left (rows 0..2, cols 0..2) with color 0,
    // and an isolated pixel of color 0 at (15, 15).
    for (r in 0..2) {
      for (c in 0..2) {
        grid[r * SpritePixelMatrixEngine.GRID_SIZE + c] = 0
      }
    }
    val isolatedIdx = 15 * SpritePixelMatrixEngine.GRID_SIZE + 15
    grid[isolatedIdx] = 0

    val filled = SpritePixelMatrixEngine.floodFill(grid, startIndex = 0, replacementColorIdx = 4)

    for (r in 0..2) {
      for (c in 0..2) {
        assertEquals(4, filled[r * SpritePixelMatrixEngine.GRID_SIZE + c])
      }
    }
    assertEquals(0, filled[isolatedIdx])
    assertEquals(-1, filled[3 * SpritePixelMatrixEngine.GRID_SIZE + 0])
  }
}

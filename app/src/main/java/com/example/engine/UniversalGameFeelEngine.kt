package com.example.engine

import kotlin.math.abs
import kotlin.math.sin

enum class ImpactWeight {
  RESISTED,
  NORMAL,
  HEAVY_OR_CRIT,
}

enum class MoveDirection(val dx: Int, val dy: Int) {
  UP(0, -1),
  DOWN(0, 1),
  LEFT(-1, 0),
  RIGHT(1, 0),
}

data class GameFeelFrameState(
  val hitstopRemainingMs: Float = 0f,
  val shakeRemainingMs: Float = 0f,
  val shakeTotalMs: Float = 1f,
  val shakeAmplitudePx: Float = 0f,
  val shakeOffsetX: Float = 0f,
  val shakeOffsetY: Float = 0f,
  val hitFlashFramesRemaining: Int = 0,
  val flashTargetIsEnemy: Boolean = true,
  val playerLungeOffsetPx: Float = 0f,
  val enemyRecoilOffsetPx: Float = 0f,
  val playerSquashX: Float = 1f,
  val playerSquashY: Float = 1f,
  val enemySquashX: Float = 1f,
  val enemySquashY: Float = 1f,
  val mosaicDissolveRemainingMs: Float = 0f,
  val mosaicPixelFactor: Float = 1f,
  val bufferedDirection: MoveDirection? = null,
  val bufferedInputRemainingMs: Float = 0f,
  val elapsedClockMs: Float = 0f,
)

/**
 * Clean-room Universal Game Feel engine implementing:
 * - Scaled hitstop (0ms resisted / 50ms normal / 95ms heavy/crit) with non-stacking maxOf invariant
 * - Linear-decay screenshake (2.5px..8.0px over 110..230ms)
 * - 2-frame white hit-flash + directional lunge/recoil + elastic squash-and-stretch springs
 * - Celeste-inspired 130ms directional input buffer + tile corner correction
 * - GBA mosaic battle-entry dissolve (10f -> 1f over 320ms)
 */
object UniversalGameFeelEngine {
  const val INPUT_BUFFER_MS = 130f
  const val MOSAIC_DISSOLVE_DURATION_MS = 320f

  fun triggerImpact(
    current: GameFeelFrameState,
    weight: ImpactWeight,
    targetIsEnemy: Boolean,
  ): GameFeelFrameState {
    val hitstopMs = when (weight) {
      ImpactWeight.RESISTED -> 0f
      ImpactWeight.NORMAL -> 50f
      ImpactWeight.HEAVY_OR_CRIT -> 95f
    }
    val shakeAmp = when (weight) {
      ImpactWeight.RESISTED -> 2.5f
      ImpactWeight.NORMAL -> 5.2f
      ImpactWeight.HEAVY_OR_CRIT -> 8.0f
    }
    val shakeDur = when (weight) {
      ImpactWeight.RESISTED -> 110f
      ImpactWeight.NORMAL -> 165f
      ImpactWeight.HEAVY_OR_CRIT -> 230f
    }

    return current.copy(
      // Non-stacking maxOf invariant prevents sluggish hitstop accumulation
      hitstopRemainingMs = maxOf(current.hitstopRemainingMs, hitstopMs),
      shakeRemainingMs = maxOf(current.shakeRemainingMs, shakeDur),
      shakeTotalMs = maxOf(current.shakeTotalMs, shakeDur),
      shakeAmplitudePx = maxOf(current.shakeAmplitudePx, shakeAmp),
      hitFlashFramesRemaining = if (weight == ImpactWeight.RESISTED) 1 else 2,
      flashTargetIsEnemy = targetIsEnemy,
      playerLungeOffsetPx = if (targetIsEnemy) 16f else -14f,
      enemyRecoilOffsetPx = if (targetIsEnemy) 18f else -12f,
      playerSquashX = if (targetIsEnemy) 1.16f else 0.86f,
      playerSquashY = if (targetIsEnemy) 0.86f else 1.15f,
      enemySquashX = if (targetIsEnemy) 0.82f else 1.14f,
      enemySquashY = if (targetIsEnemy) 1.18f else 0.88f,
    )
  }

  fun triggerBattleEntryMosaic(current: GameFeelFrameState): GameFeelFrameState =
    current.copy(
      mosaicDissolveRemainingMs = MOSAIC_DISSOLVE_DURATION_MS,
      mosaicPixelFactor = 10f,
    )

  fun bufferMovementInput(current: GameFeelFrameState, direction: MoveDirection): GameFeelFrameState =
    current.copy(
      bufferedDirection = direction,
      bufferedInputRemainingMs = INPUT_BUFFER_MS,
    )

  fun consumeBufferedInput(current: GameFeelFrameState): GameFeelFrameState =
    current.copy(
      bufferedDirection = null,
      bufferedInputRemainingMs = 0f,
    )

  fun stepFrame(current: GameFeelFrameState, deltaMs: Float): GameFeelFrameState {
    val dt = deltaMs.coerceIn(0f, 50f)
    val nextClock = current.elapsedClockMs + dt
    val nextHitstop = (current.hitstopRemainingMs - dt).coerceAtLeast(0f)

    // Decay screenshake linearly
    val nextShakeRem = (current.shakeRemainingMs - dt).coerceAtLeast(0f)
    val shakeRatio = if (current.shakeTotalMs > 0f) (nextShakeRem / current.shakeTotalMs).coerceIn(0f, 1f) else 0f
    val currentAmp = current.shakeAmplitudePx * shakeRatio
    val shakeX = if (nextShakeRem > 0f) sin(nextClock * 0.14f) * currentAmp else 0f
    val shakeY = if (nextShakeRem > 0f) sin(nextClock * 0.21f + 1.3f) * currentAmp * 0.72f else 0f

    // GBA mosaic dissolve 10f -> 1f over 320ms
    val nextMosaicRem = (current.mosaicDissolveRemainingMs - dt).coerceAtLeast(0f)
    val mosaicFactor = if (nextMosaicRem > 0f) {
      1f + 9f * (nextMosaicRem / MOSAIC_DISSOLVE_DURATION_MS).coerceIn(0f, 1f)
    } else {
      1f
    }

    // Input buffer decay
    val nextBufRem = (current.bufferedInputRemainingMs - dt).coerceAtLeast(0f)
    val nextBufDir = if (nextBufRem > 0f) current.bufferedDirection else null

    // Pause spring/lunge motion during active hitstop for crisp arcade freeze-frame impact!
    val springRate = if (nextHitstop > 0f) 0f else (dt / 120f).coerceIn(0f, 1f)
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    val nextLunge = if (abs(current.playerLungeOffsetPx) < 0.2f) 0f else lerp(current.playerLungeOffsetPx, 0f, springRate * 1.4f)
    val nextRecoil = if (abs(current.enemyRecoilOffsetPx) < 0.2f) 0f else lerp(current.enemyRecoilOffsetPx, 0f, springRate * 1.4f)
    val nextPX = lerp(current.playerSquashX, 1f, springRate * 1.5f)
    val nextPY = lerp(current.playerSquashY, 1f, springRate * 1.5f)
    val nextEX = lerp(current.enemySquashX, 1f, springRate * 1.5f)
    val nextEY = lerp(current.enemySquashY, 1f, springRate * 1.5f)

    val nextFlash = if (nextHitstop > 0f) current.hitFlashFramesRemaining else (current.hitFlashFramesRemaining - 1).coerceAtLeast(0)

    return current.copy(
      hitstopRemainingMs = nextHitstop,
      shakeRemainingMs = nextShakeRem,
      shakeTotalMs = if (nextShakeRem <= 0f) 1f else current.shakeTotalMs,
      shakeAmplitudePx = if (nextShakeRem <= 0f) 0f else current.shakeAmplitudePx,
      shakeOffsetX = shakeX,
      shakeOffsetY = shakeY,
      hitFlashFramesRemaining = nextFlash,
      playerLungeOffsetPx = nextLunge,
      enemyRecoilOffsetPx = nextRecoil,
      playerSquashX = nextPX,
      playerSquashY = nextPY,
      enemySquashX = nextEX,
      enemySquashY = nextEY,
      mosaicDissolveRemainingMs = nextMosaicRem,
      mosaicPixelFactor = mosaicFactor,
      bufferedDirection = nextBufDir,
      bufferedInputRemainingMs = nextBufRem,
      elapsedClockMs = nextClock,
    )
  }

  /**
   * 12-tile passability rule:
   * Passable: 0 (Meadow/Town), 1 (Tall Grass), 3 (Road), 5 (Prism Sanctuary),
   *           7 (Bridge/Causeway), 8 (Healing Lab/Plaza), 9 (Wild Biome Floor), 11 (Region Warp Gate)
   * Blocked:  2 (Deep Water), 4 (Mountain/Volcanic Ridge), 6 (Tree Canopy), 10 (Landmark Building)
   */
  fun isTilePassable(tile: Int): Boolean = tile != 2 && tile != 4 && tile != 6 && tile != 10

  /**
   * Tile corner correction for 16x16 (and legacy 8x8) overworld traversal:
   * If moving directly into an impassable obstacle (Water=2, Ridge=4, Tree=6, Building=10)
   * while an adjacent diagonal corner tile is passable, nudges the player cleanly around the corner.
   */
  fun resolveStepWithCornerCorrection(
    x: Int,
    y: Int,
    direction: MoveDirection,
    gridSize: Int = 16,
    tiles: List<Int>,
  ): Pair<Int, Int> {
    fun isPassable(nx: Int, ny: Int): Boolean {
      if (nx !in 0 until gridSize || ny !in 0 until gridSize) return false
      val tile = tiles.getOrElse(ny * gridSize + nx) { 0 }
      return isTilePassable(tile)
    }

    val tx = x + direction.dx
    val ty = y + direction.dy
    if (isPassable(tx, ty)) return tx to ty

    // Corner-correction nudge perpendicular to movement if obstructed
    val candidates = when (direction) {
      MoveDirection.UP, MoveDirection.DOWN -> listOf((x + 1) to ty, (x - 1) to ty)
      MoveDirection.LEFT, MoveDirection.RIGHT -> listOf(tx to (y + 1), tx to (y - 1))
    }
    for ((cx, cy) in candidates) {
      if (isPassable(cx, cy)) return cx to cy
    }
    return x to y
  }
}

package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterMixMode
import com.example.engine.ConvergedVisualParams
import com.example.engine.TrainerCatalogAndRigEngine
import com.example.engine.TrainerCharacterSpec
import com.example.engine.TrainerPose
import com.example.ui.shaders.shovelKnightEntityEffect
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.PressStart2PFamily
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet
import kotlin.math.sin

/**
 * Full 2D/3D Trainer Rig Renderer supporting all 4 CharacterMixMode options
 * (Split-Beam 2D⇄3D, Voxelizer-FX 3D, HD-2D Crisp Sprite, Pixel-Locked 3D)
 * plus ShovelKnightOmniShader AGSL effects.
 */
@Composable
fun TrainerCharacterRenderer(
  trainer: TrainerCharacterSpec,
  pose: TrainerPose = TrainerPose.BATTLE_IDLE,
  characterMixMode: CharacterMixMode = CharacterMixMode.SPLIT_BEAM_MORPH,
  visuals: ConvergedVisualParams? = null,
  walkFrameIndex: Int = 0,
  cameraYawDegrees: Float = 20f,
  flipHorizontal: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "trainer_rig_anim")
  val idlePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
    label = "trainer_idle",
  )

  val sprite2dBmp = remember(trainer.id, trainer.appearance, pose, walkFrameIndex) {
    TrainerCatalogAndRigEngine.renderTrainerSpriteBitmap(trainer, pose, walkFrameIndex, scale = 4).asImageBitmap()
  }
  val voxel3dBmp = remember(trainer.id, trainer.appearance, pose, cameraYawDegrees) {
    TrainerCatalogAndRigEngine.renderTrainerCelVoxelBitmap(trainer, pose, cameraYawDegrees, scale = 4).asImageBitmap()
  }
  val voxelSlices = remember(trainer.id, trainer.appearance, pose, characterMixMode) {
    if (characterMixMode == CharacterMixMode.VOXELIZER_FX_3D) {
      TrainerCatalogAndRigEngine.renderTrainerVoxelSlices(trainer, pose, sliceCount = 7, scale = 3).map { it.asImageBitmap() }
    } else {
      emptyList()
    }
  }

  val bobDp = (-sin(idlePhase * 6.283f) * 2.5f).dp
  val flipScale = if (flipHorizontal) -1f else 1f
  val density = LocalDensity.current

  BoxWithConstraints(
    modifier = modifier.testTag("trainer_rig_${trainer.id}"),
    contentAlignment = Alignment.Center,
  ) {
    val wPx = with(density) { maxWidth.toPx().coerceAtLeast(32f) }
    val hPx = with(density) { maxHeight.toPx().coerceAtLeast(32f) }
    val shaderMod = if (visuals != null) {
      Modifier.shovelKnightEntityEffect(
        widthPx = wPx,
        heightPx = hPx,
        visuals = visuals,
        timeSec = idlePhase * 4f,
        crystalGlint = 0.28f,
        hitFlash = false,
      )
    } else {
      Modifier
    }

    // Grounded elliptical shadow
    Canvas(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth(0.68f)
        .height(10.dp),
    ) {
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent),
        ),
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .offset(y = bobDp)
        .graphicsLayer { scaleX = flipScale }
        .then(shaderMod),
      contentAlignment = Alignment.Center,
    ) {
      when (characterMixMode) {
        CharacterMixMode.HD2D_CRISP_SPRITE -> {
          Image(
            bitmap = sprite2dBmp,
            contentDescription = "${trainer.name} 2D pixel sprite",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        }
        CharacterMixMode.PIXEL_LOCKED_3D -> {
          Image(
            bitmap = voxel3dBmp,
            contentDescription = "${trainer.name} 3D cel-voxel model",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        }
        CharacterMixMode.VOXELIZER_FX_3D -> {
          val sway = sin(idlePhase * 6.283f) * 6f + cameraYawDegrees * 0.12f
          voxelSlices.forEachIndexed { idx, sliceBmp ->
            val depthOff = (idx - voxelSlices.size / 2f)
            Image(
              bitmap = sliceBmp,
              contentDescription = if (idx == voxelSlices.lastIndex) "${trainer.name} 3D voxel stack" else null,
              filterQuality = FilterQuality.None,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .offset(x = (depthOff * (sway * 0.14f)).dp, y = (-depthOff * 0.45f).dp),
            )
          }
        }
        CharacterMixMode.SPLIT_BEAM_MORPH -> {
          // Cross-faded 2D crisp sprite layered cleanly with 3D cel-voxel depth rig
          Image(
            bitmap = voxel3dBmp,
            contentDescription = null,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            alpha = 0.72f,
            modifier = Modifier.fillMaxSize(),
          )
          Image(
            bitmap = sprite2dBmp,
            contentDescription = "${trainer.name} Split-Beam 2D/3D rig",
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            alpha = 0.82f,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

/**
 * High-contrast 40x40 GBA/DS-anime style Character Dialogue Portrait frame.
 */
@Composable
fun TrainerPortraitBadge(
  trainer: TrainerCharacterSpec,
  size: Dp = 64.dp,
  borderColor: Color = CyberCyan,
  modifier: Modifier = Modifier,
) {
  val portraitBmp = remember(trainer.id, trainer.appearance) {
    TrainerCatalogAndRigEngine.renderTrainerPortraitBitmap(trainer, scale = 4).asImageBitmap()
  }
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(12.dp))
      .background(StudioDeepNavy)
      .border(2.dp, borderColor, RoundedCornerShape(12.dp))
      .testTag("trainer_portrait_${trainer.id}"),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      bitmap = portraitBmp,
      contentDescription = "${trainer.name} dialogue portrait",
      filterQuality = FilterQuality.None,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

/**
 * Compact 2D/3D Trainer Thumbnail for Sidebars, Roster Cards, and Overworld markers.
 */
@Composable
fun TrainerOverworldThumbnail(
  trainer: TrainerCharacterSpec,
  pose: TrainerPose = TrainerPose.OVERWORLD_DOWN,
  walkFrameIndex: Int = 0,
  use3dVoxel: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val bmp = remember(trainer.id, trainer.appearance, pose, walkFrameIndex, use3dVoxel) {
    if (use3dVoxel) {
      TrainerCatalogAndRigEngine.renderTrainerCelVoxelBitmap(trainer, pose, yawDegrees = 22f, scale = 3).asImageBitmap()
    } else {
      TrainerCatalogAndRigEngine.renderTrainerSpriteBitmap(trainer, pose, walkFrameIndex, scale = 3).asImageBitmap()
    }
  }
  Image(
    bitmap = bmp,
    contentDescription = "${trainer.name} sprite",
    filterQuality = FilterQuality.None,
    contentScale = ContentScale.Fit,
    modifier = modifier,
  )
}

/**
 * Multi-creature Enemy Trainer Party Orb Strip (2-6 creatures) displayed in the Battle HUD.
 */
@Composable
fun EnemyTrainerPartyStrip(
  trainerName: String,
  partyDexIds: List<Int>,
  activeIndex: Int,
  hpMap: Map<Int, Int>,
  modifier: Modifier = Modifier,
) {
  if (partyDexIds.isEmpty()) return
  val aliveCount = partyDexIds.count { dex -> (hpMap[dex] ?: 1) > 0 }
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xCC060A14))
      .border(1.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("enemy_trainer_party_strip"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = "$trainerName ($aliveCount/${partyDexIds.size})",
      style = MaterialTheme.typography.labelSmall,
      color = SuperFxGold,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      partyDexIds.forEachIndexed { idx, dex ->
        val alive = (hpMap[dex] ?: 1) > 0
        val isActive = idx == activeIndex
        Box(
          modifier = Modifier
            .size(if (isActive) 11.dp else 9.dp)
            .clip(CircleShape)
            .background(
              when {
                !alive -> Color(0xFF475569)
                isActive -> CyberCyan
                else -> ElectricCrimson
              }
            )
            .border(
              width = if (isActive) 1.5.dp else 1.dp,
              color = if (alive) Color.White else StudioBorderDark,
              shape = CircleShape,
            ),
        )
      }
    }
  }
}

/**
 * Dramatic Trainer VS Intro Banner showing Player Portrait vs Enemy Trainer Portrait,
 * Trainer Title, Quote, and Party Count — inspired by classic DS/Switch Champion cut-ins.
 */
@Composable
fun TrainerVsBannerOverlay(
  playerTrainer: TrainerCharacterSpec,
  enemyTrainer: TrainerCharacterSpec,
  enemyPartySize: Int,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(
        brush = Brush.horizontalGradient(
          colors = listOf(
            Color(0xF20B132B),
            Color(0xF2311042),
            Color(0xF23B0A1A),
          ),
        ),
      )
      .border(1.5.dp, SuperFxGold, RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("trainer_vs_intro_banner"),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          TrainerPortraitBadge(trainer = playerTrainer, size = 54.dp, borderColor = CyberCyan)
          Column {
            Text(
              text = playerTrainer.name,
              style = MaterialTheme.typography.labelLarge,
              color = CyberCyan,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = playerTrainer.title,
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Box(
          modifier = Modifier
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElectricCrimson)
            .border(1.dp, SuperFxGold, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = "VS",
            fontFamily = PressStart2PFamily,
            fontSize = 11.sp,
            color = Color.White,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = enemyTrainer.name,
              style = MaterialTheme.typography.labelLarge,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${enemyTrainer.title} • ${enemyPartySize}v6",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          TrainerPortraitBadge(trainer = enemyTrainer, size = 54.dp, borderColor = ElectricCrimson)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "\"${enemyTrainer.introQuote}\"",
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = SgbEmerald, contentColor = StudioDeepNavy),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("dismiss_vs_banner_button"),
        ) {
          Text("Battle!", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

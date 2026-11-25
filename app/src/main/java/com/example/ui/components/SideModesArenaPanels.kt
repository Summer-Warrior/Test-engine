package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.NativePokemonEntity
import com.example.engine.CampActivityType
import com.example.engine.CanonRegionChapter
import com.example.engine.LentalSnapState
import com.example.engine.MysteryRiftState
import com.example.engine.PrismCampState
import com.example.engine.RangerStylerState
import com.example.engine.RiftTile
import com.example.engine.SnapPoseState
import com.example.engine.TimeOfDayCycle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.PressStart2PFamily
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
import kotlin.math.roundToInt

// =================================================================================================
// MODE 3: LENTAL PHOTOSPHERE SNAP (Live Animated Viewfinder + Illumina Orbs / Apples + 1★–4★)
// =================================================================================================

@Composable
fun LentalSnapPlaymat(
  snapState: LentalSnapState,
  activeChapter: CanonRegionChapter,
  timeOfDay: TimeOfDayCycle,
  is3dCelModel: Boolean,
  resolvePokemonByDex: (Int) -> NativePokemonEntity,
  onStepFrame: () -> Unit,
  onTossIlluminaOrb: () -> Unit,
  onTossApple: () -> Unit,
  onNextSubject: () -> Unit,
  onCapturePhoto: () -> Unit,
) {
  val subject = resolvePokemonByDex(snapState.subjectDex)
  val regionAccent = parseHexColorSafe(activeChapter.regionAccentHex, CyberCyan)

  // Live viewfinder motion loop (~12 FPS smooth deterministic stride)
  LaunchedEffect(snapState.subjectDex, snapState.regionId) {
    while (true) {
      delay(85L)
      onStepFrame()
    }
  }

  val skyColors = remember(timeOfDay, regionAccent) {
    when (timeOfDay) {
      TimeOfDayCycle.DAY -> listOf(Color(0xFF0C4A6E), Color(0xFF0284C7), regionAccent.copy(alpha = 0.55f))
      TimeOfDayCycle.DUSK -> listOf(Color(0xFF1E1B4B), Color(0xFFBE185D), Color(0xFFEA580C))
      TimeOfDayCycle.NIGHT -> listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E1B4B))
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "LENTAL PHOTOSPHERE — ${activeChapter.regionName.substringAfter(". ").substringBefore(" —")}",
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Subject: ${subject.name} • Pose: ${snapState.poseState.label} • ${timeOfDay.label}",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
      }
      OutlinedButton(
        onClick = onNextSubject,
        modifier = Modifier.testTag("snap_next_subject_button"),
      ) {
        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Next", style = MaterialTheme.typography.labelSmall)
      }
    }

    // Animated Live Viewfinder
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .height(235.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(skyColors))
        .border(
          width = if (snapState.illuminaTimerTicks > 0) 2.dp else 1.5.dp,
          color = if (snapState.illuminaTimerTicks > 0) SuperFxGold else CyberCyan.copy(alpha = 0.7f),
          shape = RoundedCornerShape(14.dp),
        )
        .testTag("lental_snap_viewfinder"),
    ) {
      val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
      val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

      // Biome horizon + viewfinder reticle + Illumina radiance rings
      Canvas(modifier = Modifier.fillMaxSize()) {
        // Distant biome ridge & foreground meadow
        drawRect(
          color = Color(0xFF0B1F1A).copy(alpha = 0.58f),
          topLeft = Offset(0f, size.height * 0.64f),
          size = Size(size.width, size.height * 0.36f),
        )
        drawOval(
          color = regionAccent.copy(alpha = 0.24f),
          topLeft = Offset(size.width * 0.12f, size.height * 0.60f),
          size = Size(size.width * 0.76f, size.height * 0.28f),
        )

        val subjPx = Offset(snapState.subjectX * size.width, snapState.subjectY * size.height)
        if (snapState.illuminaTimerTicks > 0) {
          drawCircle(
            color = SuperFxGold.copy(alpha = 0.28f),
            radius = 66f * snapState.depthScale,
            center = subjPx,
          )
          drawCircle(
            color = CyberCyan.copy(alpha = 0.75f),
            radius = 52f * snapState.depthScale,
            center = subjPx,
            style = Stroke(width = 3f),
          )
        } else if (snapState.appleTimerTicks > 0) {
          drawCircle(
            color = ElectricCrimson.copy(alpha = 0.9f),
            radius = 9f,
            center = Offset(subjPx.x + 26f, subjPx.y + 34f),
          )
        }

        // Photosphere center focus bracket & corner framing ticks
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val reticleColor = if (snapState.poseState == SnapPoseState.ILLUMINA_RADIANCE) SuperFxGold else Color.White.copy(alpha = 0.75f)
        drawCircle(
          color = reticleColor,
          radius = 38f,
          center = Offset(cx, cy),
          style = Stroke(width = 2f),
        )
        drawLine(reticleColor, Offset(cx - 52f, cy), Offset(cx - 28f, cy), strokeWidth = 2f)
        drawLine(reticleColor, Offset(cx + 28f, cy), Offset(cx + 52f, cy), strokeWidth = 2f)
        drawLine(reticleColor, Offset(cx, cy - 52f), Offset(cx, cy - 28f), strokeWidth = 2f)
        drawLine(reticleColor, Offset(cx, cy + 28f), Offset(cx, cy + 52f), strokeWidth = 2f)
      }

      // Moving procedural creature inside the viewfinder
      val spriteDp = (74f * snapState.depthScale).coerceIn(48f, 112f).dp
      val spritePx = with(androidx.compose.ui.platform.LocalDensity.current) { spriteDp.toPx() }
      ProceduralCreatureThumbnail(
        dexNumber = subject.dexNumber,
        contentDescription = "${subject.name} in Lental Viewfinder",
        is3dModel = is3dCelModel,
        customPrimaryHex = if (subject.isUserModified) subject.primaryHex else null,
        customSecondaryHex = if (subject.isUserModified) subject.secondaryHex else null,
        modifier = Modifier
          .size(spriteDp)
          .offset {
            IntOffset(
              x = (snapState.subjectX * widthPx - spritePx * 0.5f).roundToInt(),
              y = (snapState.subjectY * heightPx - spritePx * 0.5f).roundToInt(),
            )
          },
      )

      // Top-left & Top-right HUD telemetry pills
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "REC • ${snapState.poseState.label.uppercase()}",
          style = MaterialTheme.typography.labelSmall,
          color = if (snapState.illuminaTimerTicks > 0) SuperFxGold else Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x99050A18))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        )
        Text(
          text = "ZOOM ${(snapState.depthScale * 100).roundToInt()}%",
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x99050A18))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        )
      }
    }

    // Photosphere Action Controls: Illumina Orb, Apple, Shutter
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = onTossIlluminaOrb,
        enabled = snapState.orbsRemaining > 0,
        modifier = Modifier
          .weight(1f)
          .testTag("snap_toss_orb_button"),
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Orb (${snapState.orbsRemaining})", style = MaterialTheme.typography.labelSmall, color = SuperFxGold)
      }
      OutlinedButton(
        onClick = onTossApple,
        enabled = snapState.applesRemaining > 0,
        modifier = Modifier
          .weight(1f)
          .testTag("snap_toss_apple_button"),
      ) {
        Icon(Icons.Default.Restaurant, contentDescription = null, tint = ElectricCrimson, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Apple (${snapState.applesRemaining})", style = MaterialTheme.typography.labelSmall, color = ElectricCrimson)
      }
      Button(
        onClick = onCapturePhoto,
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
        modifier = Modifier
          .weight(1.1f)
          .testTag("snap_shutter_button"),
      ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Shutter!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
      }
    }

    // Latest Shot 4-Axis Score Breakdown
    snapState.lastShot?.let { shot ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SuperFxGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
          .testTag("snap_last_shot_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${shot.speciesName} — ${shot.tierLabel}",
              style = MaterialTheme.typography.labelLarge,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${shot.totalScore} pts",
              fontFamily = PressStart2PFamily,
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
          }
          Text(
            text = "Center ${shot.centeringScore} • Size ${shot.sizeScore} • Pose ${shot.poseScore} • Illumina +${shot.illuminaBonus}",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )
        }
      }
    }
  }
}

// =================================================================================================
// MODE 4: MYSTERY RIFT DUNGEON CRAWL (Procedural 5-Floor 10x10 Roguelite + Time Gears + Guardian)
// =================================================================================================

@Composable
fun MysteryRiftPlaymat(
  riftState: MysteryRiftState,
  leadPokemon: NativePokemonEntity,
  onStartDungeon: () -> Unit,
  onStepDirection: (dRow: Int, dCol: Int) -> Unit,
  onUsePrismPulse: () -> Unit,
  onUseOranBerry: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (riftState.isActive) "MYSTERY RIFT B${riftState.floorNumber}F / B${riftState.maxFloors}F" else "MYSTERY RIFT DUNGEON",
          fontFamily = PressStart2PFamily,
          style = MaterialTheme.typography.labelSmall,
          color = VoxelViolet,
        )
        Text(
          text = "Lead: ${leadPokemon.name} Lv.${riftState.playerLevel} • HP ${riftState.playerHp}/${riftState.playerMaxHp} • Gears: ${riftState.timeGearsCollected} • Berries: ${riftState.oranBerriesHeld}",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
      }
      Button(
        onClick = onStartDungeon,
        colors = ButtonDefaults.buttonColors(containerColor = VoxelViolet, contentColor = Color.White),
        modifier = Modifier.testTag("rift_start_button"),
      ) {
        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (riftState.isActive) "New Run" else "Enter Rift", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
    }

    if (!riftState.isActive || riftState.tiles.size < riftState.gridSize * riftState.gridSize) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, VoxelViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Text(
          text = "Guide ${leadPokemon.name} through 5 procedural 10×10 Mystery Rift floors! Collect golden Time Gears, dodge Spike Traps, battle roaming foes, descend glowing Stairs, and vanquish the Floor-5 Rift Guardian.",
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextPrimary,
          modifier = Modifier.padding(12.dp),
        )
      }
    } else {
      val hpRatio = (riftState.playerHp.toFloat() / riftState.playerMaxHp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { hpRatio },
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(6.dp)),
        color = if (hpRatio > 0.45f) SgbEmerald else if (hpRatio > 0.2f) SuperFxGold else ElectricCrimson,
        trackColor = StudioBorderDark,
      )

      // 10x10 Procedural Roguelite Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.18f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF070A14))
          .border(1.5.dp, VoxelViolet.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
          .testTag("mystery_rift_grid_canvas"),
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
          val g = riftState.gridSize
          val cellW = size.width / g
          val cellH = size.height / g
          for (r in 0 until g) {
            for (c in 0 until g) {
              val tile = riftState.tiles[r * g + c]
              val left = c * cellW
              val top = r * cellH
              val tileColor = when (tile) {
                RiftTile.WALL -> Color(0xFF141B2D)
                RiftTile.FLOOR -> Color(0xFF23304C)
                RiftTile.STAIRS -> Color(0xFF0E7490)
                RiftTile.TIME_GEAR -> Color(0xFF23304C)
                RiftTile.ORAN_BERRY -> Color(0xFF23304C)
                RiftTile.SPIKE_TRAP -> Color(0xFF3B1D2A)
              }
              drawRoundRect(
                color = tileColor,
                topLeft = Offset(left + 1.5f, top + 1.5f),
                size = Size(cellW - 3f, cellH - 3f),
                cornerRadius = CornerRadius(4f, 4f),
              )
              val center = Offset(left + cellW * 0.5f, top + cellH * 0.5f)
              when (tile) {
                RiftTile.STAIRS -> {
                  drawRect(CyberCyan, Offset(left + cellW * 0.22f, top + cellH * 0.22f), Size(cellW * 0.56f, cellH * 0.14f))
                  drawRect(CyberCyan, Offset(left + cellW * 0.30f, top + cellH * 0.44f), Size(cellW * 0.48f, cellH * 0.14f))
                  drawRect(SuperFxGold, Offset(left + cellW * 0.38f, top + cellH * 0.66f), Size(cellW * 0.40f, cellH * 0.14f))
                }
                RiftTile.TIME_GEAR -> {
                  drawCircle(SuperFxGold, radius = minOf(cellW, cellH) * 0.28f, center = center, style = Stroke(width = 4f))
                  drawCircle(CyberCyan, radius = minOf(cellW, cellH) * 0.11f, center = center)
                }
                RiftTile.ORAN_BERRY -> {
                  drawCircle(Color(0xFF38BDF8), radius = minOf(cellW, cellH) * 0.22f, center = center)
                  drawCircle(SgbEmerald, radius = minOf(cellW, cellH) * 0.09f, center = Offset(center.x, center.y - cellH * 0.18f))
                }
                RiftTile.SPIKE_TRAP -> {
                  drawLine(ElectricCrimson, Offset(left + 6f, top + 6f), Offset(left + cellW - 6f, top + cellH - 6f), strokeWidth = 2.5f)
                  drawLine(ElectricCrimson, Offset(left + cellW - 6f, top + 6f), Offset(left + 6f, top + cellH - 6f), strokeWidth = 2.5f)
                }
                else -> Unit
              }
            }
          }

          // Roaming Rift Foes & Floor-5 Guardian
          riftState.enemies.forEach { foe ->
            val cx = (foe.col + 0.5f) * cellW
            val cy = (foe.row + 0.5f) * cellH
            val r = minOf(cellW, cellH) * if (foe.isFloorGuardian) 0.40f else 0.30f
            if (foe.isFloorGuardian) {
              drawCircle(SuperFxGold.copy(alpha = 0.4f), radius = r * 1.32f, center = Offset(cx, cy))
            }
            drawCircle(if (foe.isFloorGuardian) VoxelViolet else ElectricCrimson, radius = r, center = Offset(cx, cy))
            drawCircle(Color.White, radius = r * 0.35f, center = Offset(cx, cy))
          }

          // Lead Pokémon Player Marker
          val px = (riftState.playerCol + 0.5f) * cellW
          val py = (riftState.playerRow + 0.5f) * cellH
          drawCircle(SgbEmerald.copy(alpha = 0.35f), radius = minOf(cellW, cellH) * 0.44f, center = Offset(px, py))
          drawCircle(SgbEmerald, radius = minOf(cellW, cellH) * 0.31f, center = Offset(px, py))
          drawCircle(StudioDeepNavy, radius = minOf(cellW, cellH) * 0.12f, center = Offset(px, py))
        }
      }

      // Legend + Controls
      Text(
        text = "Legend: Green=You • Red/Violet=Foe/Guardian • Cyan=Stairs • Gold=Time Gear • Blue=Berry • X=Trap",
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Roguelite 4-Way D-Pad
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Button(
            onClick = { onStepDirection(-1, 0) },
            enabled = !riftState.dungeonCleared && !riftState.playerDefeated,
            modifier = Modifier.size(48.dp).testTag("rift_dpad_up"),
          ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Rift Up")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = { onStepDirection(0, -1) },
              enabled = !riftState.dungeonCleared && !riftState.playerDefeated,
              modifier = Modifier.size(48.dp).testTag("rift_dpad_left"),
            ) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Rift Left")
            }
            Button(
              onClick = { onStepDirection(1, 0) },
              enabled = !riftState.dungeonCleared && !riftState.playerDefeated,
              modifier = Modifier.size(48.dp).testTag("rift_dpad_down"),
            ) {
              Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Rift Down")
            }
            Button(
              onClick = { onStepDirection(0, 1) },
              enabled = !riftState.dungeonCleared && !riftState.playerDefeated,
              modifier = Modifier.size(48.dp).testTag("rift_dpad_right"),
            ) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Rift Right")
            }
          }
        }

        // Signature Prism Pulse + Oran Berry
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
          Button(
            onClick = onUsePrismPulse,
            enabled = !riftState.dungeonCleared && !riftState.playerDefeated,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = StudioDeepNavy),
            modifier = Modifier.testTag("rift_prism_pulse_button"),
          ) {
            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Prism Pulse", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = onUseOranBerry,
            enabled = riftState.oranBerriesHeld > 0 && !riftState.dungeonCleared && !riftState.playerDefeated,
            modifier = Modifier.testTag("rift_oran_berry_button"),
          ) {
            Icon(Icons.Default.Healing, contentDescription = null, tint = SgbEmerald, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Berry (${riftState.oranBerriesHeld})", style = MaterialTheme.typography.labelSmall, color = SgbEmerald)
          }
        }
      }

      // Rift Log
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          riftState.riftLog.take(3).forEachIndexed { idx, line ->
            Text(
              text = "• $line",
              style = MaterialTheme.typography.labelSmall,
              color = if (idx == 0) StudioTextPrimary else StudioTextSecondary,
            )
          }
        }
      }
    }
  }
}

// =================================================================================================
// MODE 5: RANGER CAPTURE STYLER (Touch-Loop Canvas) & PRISM CAMP BONDING (6-Pokémon Picnic)
// =================================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RangerAndCampPlaymat(
  rangerState: RangerStylerState,
  campState: PrismCampState,
  partyDexIds: List<Int>,
  bondGauge: Int,
  resolvePokemonByDex: (Int) -> NativePokemonEntity,
  onStepWildMovement: () -> Unit,
  onBeginDrag: (normX: Float, normY: Float) -> Unit,
  onStepDrag: (normX: Float, normY: Float) -> Unit,
  onEndDrag: () -> Unit,
  onNextTarget: () -> Unit,
  onRecruitTarget: () -> Unit,
  onPerformCampActivity: (CampActivityType) -> Unit,
) {
  var showCampSubView by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Sub-Mode Switcher: Capture Styler vs Prism Camp & Picnic
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = !showCampSubView,
        onClick = { showCampSubView = false },
        leadingIcon = { Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(15.dp)) },
        label = { Text("Capture Styler", style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan.copy(alpha = 0.22f), selectedLabelColor = CyberCyan),
        modifier = Modifier.testTag("ranger_subtab_styler"),
      )
      FilterChip(
        selected = showCampSubView,
        onClick = { showCampSubView = true },
        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp)) },
        label = { Text("Prism Camp ($bondGauge%)", style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SgbEmerald.copy(alpha = 0.22f), selectedLabelColor = SgbEmerald),
        modifier = Modifier.testTag("ranger_subtab_camp"),
      )
    }

    if (!showCampSubView) {
      // Live wild creature motion loop
      LaunchedEffect(rangerState.targetDex, rangerState.isCaptured) {
        while (!rangerState.isCaptured) {
          delay(100L)
          onStepWildMovement()
        }
      }

      val trailPoints = remember { mutableStateListOf<Offset>() }
      val targetEntity = resolvePokemonByDex(rangerState.targetDex)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "CAPTURE STYLER — ${targetEntity.name.uppercase()}",
            fontFamily = PressStart2PFamily,
            style = MaterialTheme.typography.labelSmall,
            color = if (rangerState.isAgitated) ElectricCrimson else CyberCyan,
          )
          Text(
            text = "Loops: ${rangerState.loopsCompleted}/${rangerState.loopsRequired} • Friendship ${(rangerState.friendshipGauge * 100).roundToInt()}% • Energy ${rangerState.stylerEnergy}%",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )
        }
        OutlinedButton(
          onClick = {
            trailPoints.clear()
            onNextTarget()
          },
          modifier = Modifier.testTag("ranger_next_target_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Target", style = MaterialTheme.typography.labelSmall)
        }
      }

      LinearProgressIndicator(
        progress = { rangerState.friendshipGauge.coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(6.dp)),
        color = if (rangerState.isCaptured) SuperFxGold else SgbEmerald,
        trackColor = StudioBorderDark,
      )

      // Interactive Touch-Loop Capture Styler Canvas
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .height(235.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Brush.radialGradient(listOf(Color(0xFF132A3E), Color(0xFF070D19))))
          .border(
            width = 1.5.dp,
            color = when {
              rangerState.isCaptured -> SuperFxGold
              rangerState.isAgitated -> ElectricCrimson
              else -> CyberCyan
            },
            shape = RoundedCornerShape(14.dp),
          )
          .testTag("ranger_styler_canvas"),
      ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val spriteDp = 64.dp
        val spritePx = with(androidx.compose.ui.platform.LocalDensity.current) { spriteDp.toPx() }

        val latestBeginDrag by rememberUpdatedState(onBeginDrag)
        val latestStepDrag by rememberUpdatedState(onStepDrag)
        val latestEndDrag by rememberUpdatedState(onEndDrag)

        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(rangerState.targetDex, rangerState.isCaptured) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val start = down.position
                trailPoints.clear()
                trailPoints.add(start)
                latestBeginDrag(
                  (start.x / size.width).coerceIn(0f, 1f),
                  (start.y / size.height).coerceIn(0f, 1f),
                )
                drag(down.id) { change ->
                  change.consume()
                  val pos = change.position
                  trailPoints.add(pos)
                  if (trailPoints.size > 48) trailPoints.removeAt(0)
                  latestStepDrag(
                    (pos.x / size.width).coerceIn(0f, 1f),
                    (pos.y / size.height).coerceIn(0f, 1f),
                  )
                }
                trailPoints.clear()
                latestEndDrag()
              }
            },
        ) {
          val targetCenter = Offset(rangerState.targetX * size.width, rangerState.targetY * size.height)

          // Friendship Progress Ring around target
          drawCircle(
            color = if (rangerState.isAgitated) ElectricCrimson.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.18f),
            radius = 68f,
            center = targetCenter,
          )
          drawArc(
            color = if (rangerState.isCaptured) SuperFxGold else SgbEmerald,
            startAngle = -90f,
            sweepAngle = 360f * rangerState.friendshipGauge.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(targetCenter.x - 68f, targetCenter.y - 68f),
            size = Size(136f, 136f),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
          )

          // Glowing Styler Capture Line Trail
          if (trailPoints.size >= 2) {
            val path = Path().apply {
              moveTo(trailPoints.first().x, trailPoints.first().y)
              for (i in 1 until trailPoints.size) {
                lineTo(trailPoints[i].x, trailPoints[i].y)
              }
            }
            drawPath(
              path = path,
              color = CyberCyan.copy(alpha = 0.35f),
              style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
              path = path,
              color = Color.White,
              style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          }
        }

        // Animated wild creature sprite at (targetX, targetY)
        ProceduralCreatureThumbnail(
          dexNumber = targetEntity.dexNumber,
          contentDescription = "${targetEntity.name} Capture Styler target",
          modifier = Modifier
            .size(spriteDp)
            .offset {
              IntOffset(
                x = (rangerState.targetX * widthPx - spritePx * 0.5f).roundToInt(),
                y = (rangerState.targetY * heightPx - spritePx * 0.5f).roundToInt(),
              )
            },
        )

        if (rangerState.isAgitated && !rangerState.isCaptured) {
          Text(
            text = "AGITATED — CIRCLE WIDE!",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCrimson,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 8.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xB3090E1C))
              .padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Text(
        text = rangerState.statusMessage,
        style = MaterialTheme.typography.bodySmall,
        color = if (rangerState.isCaptured) SuperFxGold else StudioTextPrimary,
      )

      if (rangerState.isCaptured) {
        Button(
          onClick = onRecruitTarget,
          colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ranger_recruit_target_button"),
        ) {
          Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Recruit ${targetEntity.name} to Active Party & Atlas!", fontWeight = FontWeight.Bold)
        }
      }
    } else {
      // PRISM CAMP & PICNIC (6-Pokémon Party Bonding + Curry / Oran Snacks / Play Toy)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "PRISM CAMP & PICNIC MEADOW",
            fontFamily = PressStart2PFamily,
            style = MaterialTheme.typography.labelSmall,
            color = SgbEmerald,
          )
          Text(
            text = "BOND $bondGauge%",
            fontFamily = PressStart2PFamily,
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
          )
        }

        LinearProgressIndicator(
          progress = { (bondGauge / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp)),
          color = SuperFxGold,
          trackColor = StudioBorderDark,
        )

        // 6-Pokémon Meadow Grid
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SgbEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .testTag("prism_camp_meadow_card"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0C241E)),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              partyDexIds.take(6).forEachIndexed { idx, dex ->
                val mon = resolvePokemonByDex(dex)
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardDark.copy(alpha = 0.85f))
                    .border(1.dp, if (idx == 0) SuperFxGold else StudioBorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                  Box {
                    ProceduralCreatureThumbnail(
                      dexNumber = mon.dexNumber,
                      contentDescription = "${mon.name} at Prism Camp",
                      modifier = Modifier.size(54.dp),
                    )
                    if (campState.activePartyMoodBoostNonce > 0) {
                      Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier
                          .size(16.dp)
                          .align(Alignment.TopEnd)
                          .clip(CircleShape),
                      )
                    }
                  }
                  Text(
                    text = mon.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
            Text(
              text = campState.lastActivityLabel,
              style = MaterialTheme.typography.bodySmall,
              color = SuperFxGold,
            )
          }
        }

        // Camp Activity Buttons
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CampActivityType.entries.forEach { activity ->
            Button(
              onClick = { onPerformCampActivity(activity) },
              colors = ButtonDefaults.buttonColors(
                containerColor = when (activity) {
                  CampActivityType.ORAN_SNACK -> SgbEmerald
                  CampActivityType.PRISM_CURRY -> SuperFxGold
                  CampActivityType.PLAY_TOY -> CyberCyan
                },
                contentColor = StudioDeepNavy,
              ),
              modifier = Modifier.testTag("camp_activity_${activity.name.lowercase()}"),
            ) {
              Text(
                text = "${activity.label} (+${activity.bondGain}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

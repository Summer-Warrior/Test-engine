package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.PrismRomCartridge
import com.example.engine.PrismRomPackageEngine
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

/**
 * Standalone Comparison `.prismrom` Compiler, Launcher, Home-Screen Icon Installer & SAF Exporter.
 * Accessible from both the Game Test HUD ("ROMs" pill) and the Project Dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RomComparisonExporterSheet(
  compiledRoms: List<PrismRomCartridge>,
  activeProjectTitle: String,
  onGenerateAllTenRoms: () -> Unit,
  onCompileCurrentStudioRom: () -> Unit,
  onImportPrismRom: () -> Unit,
  onLaunchStandaloneRom: (PrismRomCartridge) -> Unit,
  onInstallHomeShortcut: (PrismRomCartridge) -> Unit,
  onExportPrismRom: (PrismRomCartridge) -> Unit,
  onDeleteCompiledRom: (String) -> Unit,
  onClose: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.5.dp, SuperFxGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      .testTag("rom_comparison_exporter_sheet"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = "Standalone ROM Compiler",
              tint = SuperFxGold,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "STANDALONE .PRISMROM COMPARISON STUDIO",
              fontFamily = PressStart2PFamily,
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "Compile & install isolated full-screen test builds of \"$activeProjectTitle\" across 10 hardware eras — zero Studio chrome.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary,
          )
        }
        if (onClose != null) {
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).testTag("rom_sheet_close_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Comparison ROMs", tint = StudioTextSecondary)
          }
        }
      }

      // Primary 1-Tap Batch Compiler + Custom Build + Import Row
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onGenerateAllTenRoms,
          colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("generate_all_10_roms_button"),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Generate All 10 Comparison ROMs", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onCompileCurrentStudioRom,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("compile_current_studio_rom_button"),
        ) {
          Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Build Current Studio ROM", color = CyberCyan)
        }

        OutlinedButton(
          onClick = onImportPrismRom,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("import_prism_rom_button"),
        ) {
          Icon(Icons.Default.UploadFile, contentDescription = null, tint = SgbEmerald, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Import .prismrom", color = SgbEmerald)
        }
      }

      // Library Status Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(StudioDeepNavy.copy(alpha = 0.85f))
          .border(1.dp, StudioBorderDark, RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "COMPILED CARTRIDGES (${compiledRoms.size})",
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Multi-Task Windows • Home Icons • SAF Export",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
      }

      if (compiledRoms.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardDark)
            .border(1.dp, StudioBorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "No Standalone Comparison ROMs Compiled Yet",
              style = MaterialTheme.typography.titleSmall,
              color = StudioTextPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Tap \"Generate All 10 Comparison ROMs\" above to package GB DMG, NES VRC6, SNES FX2, GBA, PS1, GameCube, NDS, 3DS, Switch HD-2D, and Ultimate 156 Hardware editions ready for side-by-side play and Home Screen pinning.",
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextSecondary,
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          compiledRoms.forEach { cartridge ->
            CompiledRomCartridgeCard(
              cartridge = cartridge,
              onLaunch = { onLaunchStandaloneRom(cartridge) },
              onPinHomeIcon = { onInstallHomeShortcut(cartridge) },
              onExport = { onExportPrismRom(cartridge) },
              onDelete = { onDeleteCompiledRom(cartridge.romId) },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompiledRomCartridgeCard(
  cartridge: PrismRomCartridge,
  onLaunch: () -> Unit,
  onPinHomeIcon: () -> Unit,
  onExport: () -> Unit,
  onDelete: () -> Unit,
) {
  val accentColor = remember(cartridge.accentHex) {
    runCatching { Color(android.graphics.Color.parseColor(cartridge.accentHex)) }
      .getOrDefault(CyberCyan)
  }
  val iconImageBitmap = remember(cartridge.romId, cartridge.accentHex, cartridge.comparisonTag) {
    PrismRomPackageEngine.renderCartridgeIconBitmap(cartridge, sizePx = 128).asImageBitmap()
  }
  val extraCount = cartridge.extraVisualAudio.enabledCount
  val hwCountLabel = if (cartridge.useAllCatalogPlatforms) {
    "ALL ${cartridge.activePlatformIds.size.coerceAtLeast(156)} HW"
  } else {
    "${cartridge.activePlatformIds.size} HW LOCKED"
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
      .testTag("rom_card_${cartridge.comparisonTag.lowercase()}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Image(
          bitmap = iconImageBitmap,
          contentDescription = "${cartridge.editionSubtitle} cartridge icon",
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = cartridge.editionSubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = cartridge.hardwareBadgeSummary,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "$hwCountLabel • ${cartridge.characterMixMode.label} • Hero: ${cartridge.playerTrainer.name} • Extra Pretty: $extraCount/9",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(48.dp)
            .testTag("rom_delete_${cartridge.comparisonTag.lowercase()}"),
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete ${cartridge.editionSubtitle}",
            tint = ElectricCrimson.copy(alpha = 0.85f),
          )
        }
      }

      // 3 Action Buttons per ROM: Launch Standalone, Install Home Icon, Export .prismrom
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Button(
          onClick = onLaunch,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = StudioDeepNavy),
          shape = RoundedCornerShape(9.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("rom_launch_${cartridge.comparisonTag.lowercase()}"),
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Launch Standalone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onPinHomeIcon,
          shape = RoundedCornerShape(9.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("rom_pin_home_${cartridge.comparisonTag.lowercase()}"),
        ) {
          Icon(Icons.Default.AddToHomeScreen, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Install Home Icon", color = SuperFxGold, style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onExport,
          shape = RoundedCornerShape(9.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("rom_export_${cartridge.comparisonTag.lowercase()}"),
        ) {
          Icon(Icons.Default.Download, contentDescription = null, tint = VoxelViolet, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export .prismrom", color = VoxelViolet, style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

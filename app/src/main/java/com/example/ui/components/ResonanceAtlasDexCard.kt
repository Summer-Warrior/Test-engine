package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AtlasProgressEntity
import com.example.data.local.AtlasSpeciesIndexItem
import com.example.data.local.CreatureVariantForm
import com.example.data.local.NationalAtlasIndex
import com.example.data.local.NativePokemonEntity
import com.example.engine.MasterCanonTemplateCatalog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.Mode7Sky
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
 * The Offline Resonance Atlas Dex — 5 complete views over #0001-#1025 (+ Regional, Mega-Resonance,
 * Paradox-Echo, and Prism-Awakened alternate forms), rendered 100% offline via ProceduralCreatureRigEngine:
 *  0) National Index (#0001-#1025)
 *  1) 22-Region Fusion Atlas filter
 *  2) Resonance Role Tiers
 *  3) Alternate Forms (Regional / Mega / Paradox / Prism)
 *  4) Mastery Tracker (persisted in Room)
 */
@Composable
fun ResonanceAtlasDexCard(
  nativeList: List<NativePokemonEntity> = emptyList(),
  atlasProgressMap: Map<Int, AtlasProgressEntity>,
  isSyncingPokeApi: Boolean,
  onHydrateFromPokeApi: (String) -> Unit,
  onPlaySpeciesCry: (AtlasSpeciesIndexItem) -> Unit,
  onHarmonizeSpecies: (Int) -> Unit,
) {
  val hydratedByDex = remember(nativeList) { nativeList.associateBy { it.dexNumber } }
  val fullIndex = remember(hydratedByDex) { NationalAtlasIndex.buildCompleteIndex(hydratedByDex) }
  val totalFormsCount = remember(fullIndex) { fullIndex.sumOf { it.availableForms.size } }

  var selectedAtlasTab by remember { mutableIntStateOf(0) }
  var atlasQuery by remember { mutableStateOf("") }
  var selectedRegionId by remember { mutableStateOf("kanto") }
  var selectedRoleTier by remember { mutableStateOf("Starter") }
  var selectedFormFilter by remember { mutableStateOf(CreatureVariantForm.MEGA_RESONANCE) }
  var show3dVoxelThumbs by remember { mutableStateOf(false) }
  val activeFormByDex = remember { mutableStateMapOf<Int, CreatureVariantForm>() }

  val seenCount = atlasProgressMap.values.count { it.seen }
  val battledCount = atlasProgressMap.values.count { it.battled }
  val capturedCount = atlasProgressMap.values.count { it.captured }
  val harmonizedCount = atlasProgressMap.values.count { it.hardwareHarmonized }

  val displayedList = remember(
    fullIndex,
    selectedAtlasTab,
    atlasQuery,
    selectedRegionId,
    selectedRoleTier,
    selectedFormFilter,
    atlasProgressMap,
  ) {
    val base = when (selectedAtlasTab) {
      1 -> {
        val regionDexSet = NationalAtlasIndex.dexPoolForRegion(selectedRegionId).toSet()
        fullIndex.filter { it.dexNumber in regionDexSet }
      }
      2 -> fullIndex.filter { it.resonanceRoleTier == selectedRoleTier }
      3 -> fullIndex.filter { selectedFormFilter in it.availableForms }
      4 -> fullIndex.filter {
        atlasProgressMap[it.dexNumber]?.let { p -> p.seen || p.battled || p.captured || p.hardwareHarmonized } == true
      }
      else -> fullIndex
    }
    val q = atlasQuery.trim()
    if (q.isEmpty()) base else base.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.epithet.contains(q, ignoreCase = true) ||
        it.dexNumber.toString() == q ||
        it.primaryType.contains(q, ignoreCase = true) ||
        it.secondaryType.contains(q, ignoreCase = true) ||
        it.regionName.contains(q, ignoreCase = true) ||
        it.signatureAbility.contains(q, ignoreCase = true) ||
        it.signatureMove.contains(q, ignoreCase = true)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "OFFLINE RESONANCE ATLAS • #0001–#1025 ($totalFormsCount FORMS)",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${displayedList.size} Shown • Seen $seenCount • Caught $capturedCount • Harmonized $harmonizedCount",
            style = MaterialTheme.typography.labelMedium,
            color = StudioTextPrimary,
          )
        }
        FilterChip(
          selected = show3dVoxelThumbs,
          onClick = { show3dVoxelThumbs = !show3dVoxelThumbs },
          leadingIcon = {
            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(15.dp))
          },
          label = { Text(if (show3dVoxelThumbs) "3D Voxel" else "2D Pixel", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VoxelViolet.copy(alpha = 0.25f),
            selectedLabelColor = VoxelViolet,
          ),
          modifier = Modifier.testTag("atlas_toggle_3d_thumbs"),
        )
      }

      // 5 Atlas Views Switcher
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          0 to "National #0001–#1025",
          1 to "22-Region Atlas",
          2 to "Resonance Tiers",
          3 to "Alternate Forms",
          4 to "Mastery Tracker",
        ).forEach { (idx, label) ->
          FilterChip(
            selected = selectedAtlasTab == idx,
            onClick = { selectedAtlasTab = idx },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
              selectedLabelColor = CyberCyan,
            ),
            modifier = Modifier.testTag("atlas_view_tab_$idx"),
          )
        }
      }

      // Contextual Sub-Filters
      when (selectedAtlasTab) {
        1 -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            MasterCanonTemplateCatalog.worldChapters.forEach { ch ->
              FilterChip(
                selected = selectedRegionId == ch.id,
                onClick = { selectedRegionId = ch.id },
                label = { Text(ch.regionName.substringBefore(" —"), style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
        2 -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("Starter", "Regional Ace", "Paradox / Ultra", "Legendary / Mythical", "Wild Resonance").forEach { tier ->
              FilterChip(
                selected = selectedRoleTier == tier,
                onClick = { selectedRoleTier = tier },
                label = { Text(tier, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
        3 -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(
              CreatureVariantForm.REGIONAL,
              CreatureVariantForm.MEGA_RESONANCE,
              CreatureVariantForm.PARADOX_ECHO,
              CreatureVariantForm.PRISM_AWAKENED,
            ).forEach { variant ->
              FilterChip(
                selected = selectedFormFilter == variant,
                onClick = { selectedFormFilter = variant },
                label = { Text(variant.displayLabel, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
                  selectedLabelColor = SuperFxGold,
                ),
              )
            }
          }
        }
        4 -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(StudioDeepNavy)
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "MASTERY MILESTONES (PERSISTED IN ROOM)",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
            )
            MasteryRow("Seen", seenCount, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES, Mode7Sky)
            MasteryRow("Battled", battledCount, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES, SuperFxGold)
            MasteryRow("Captured", capturedCount, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES, SgbEmerald)
            MasteryRow("Hardware-Harmonized", harmonizedCount, NationalAtlasIndex.TOTAL_NATIONAL_SPECIES, VoxelViolet)
          }
        }
      }

      // Search box across all 1,025 species
      OutlinedTextField(
        value = atlasQuery,
        onValueChange = { atlasQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text("Filter #0001–#1025 by Dex #, name, epithet, type, ability, or region...") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("atlas_search_input"),
      )

      // Smooth LazyColumn over #0001-#1025 using 100% offline ProceduralCreatureThumbnail
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 200.dp, max = 420.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(StudioDeepNavy)
          .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(displayedList, key = { it.dexNumber }) { species ->
          val prog = atlasProgressMap[species.dexNumber]
          val accent = parseHexColorSafe(species.primaryHex, CyberCyan)
          val defaultForm = if (selectedAtlasTab == 3 && selectedFormFilter in species.availableForms) {
            selectedFormFilter
          } else {
            CreatureVariantForm.STANDARD
          }
          val activeForm = activeFormByDex[species.dexNumber] ?: defaultForm
          val displayName = species.displayNameForForm(activeForm)
          val displayBst = species.statTotalForForm(activeForm)

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(StudioSurfaceDark)
              .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ProceduralCreatureThumbnail(
                dexNumber = species.dexNumber,
                form = activeForm,
                is3dModel = show3dVoxelThumbs,
                contentDescription = "$displayName procedural sprite",
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(StudioDeepNavy)
                  .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                  .padding(2.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "#${species.dexNumber.toString().padStart(4, '0')} $displayName",
                  style = MaterialTheme.typography.labelLarge,
                  color = StudioTextPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${species.epithet} • ${species.regionName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = CyberCyan,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${species.primaryType}" +
                    (if (species.secondaryType.isNotBlank()) "/${species.secondaryType}" else "") +
                    " • ${species.resonanceRoleTier} • BST $displayBst",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val evoSuffix = species.evolvesToDex?.let { nxt ->
                  " • Evo → #${nxt.toString().padStart(4, '0')} ${NationalAtlasIndex.peekSpecies(nxt).name}"
                }.orEmpty()
                Text(
                  text = "${species.signatureAbility} • ${species.signatureMove}$evoSuffix",
                  style = MaterialTheme.typography.labelSmall,
                  color = SuperFxGold.copy(alpha = 0.9f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (prog != null) {
                  val flags = buildList {
                    if (prog.seen) add("Seen")
                    if (prog.battled) add("Battled")
                    if (prog.captured) add("Caught")
                    if (prog.hardwareHarmonized) add("Harmonized")
                  }.joinToString(" • ")
                  if (flags.isNotEmpty()) {
                    Text(flags, style = MaterialTheme.typography.labelSmall, color = SgbEmerald)
                  }
                }
              }

              IconButton(
                onClick = { onPlaySpeciesCry(species) },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Play ${species.name} Cry", tint = CyberCyan, modifier = Modifier.size(18.dp))
              }
              IconButton(
                onClick = { onHarmonizeSpecies(species.dexNumber) },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Harmonize ${species.name}", tint = SuperFxGold, modifier = Modifier.size(18.dp))
              }
              OutlinedButton(
                onClick = { onHydrateFromPokeApi(species.dexNumber.toString()) },
                enabled = !isSyncingPokeApi,
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = "Load #${species.dexNumber} Rig", modifier = Modifier.size(15.dp), tint = SgbEmerald)
              }
            }

            // Form Switcher Row for Regional / Mega-Resonance / Paradox / Prism-Awakened variants
            if (species.availableForms.size > 1) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "FORMS:",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  fontWeight = FontWeight.Bold,
                )
                species.availableForms.forEach { variant ->
                  val isChosen = activeForm == variant
                  Text(
                    text = variant.shortTag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                    color = if (isChosen) StudioDeepNavy else SuperFxGold,
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(if (isChosen) SuperFxGold else StudioDeepNavy)
                      .border(1.dp, SuperFxGold.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                      .clickable { activeFormByDex[species.dexNumber] = variant }
                      .padding(horizontal = 8.dp, vertical = 3.dp)
                      .testTag("atlas_form_${species.dexNumber}_${variant.token.lowercase()}"),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MasteryRow(
  label: String,
  count: Int,
  total: Int,
  color: androidx.compose.ui.graphics.Color,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
      Text("$count / $total", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
      progress = { (count.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f) },
      color = color,
      trackColor = StudioCardDark,
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
    )
  }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.NationalAtlasIndex
import com.example.engine.BattleGimmickMode
import com.example.engine.CanonRegionChapter
import com.example.engine.TrainerArchetype
import com.example.engine.TrainerArchetypeCatalog
import com.example.engine.TrainerCategory
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

/**
 * Interactive Game Test Roster Hub:
 *  - Tab 0: All Trainer Types (Route & Field, Specialists, League & Frontier, Anime/Manga/Spin-Offs, Factions)
 *    with instant live battle challenges utilizing each Trainer Type's real AI tactics, gimmick, weather, and quotes.
 *  - Tab 1: All 1,025 Pokémon (#0001-#1025) searchable by Regional Habitat or Full National Dex
 *    with instant "Wild" encounter summoning and "+Party" lead-slot recruitment.
 */
@Composable
fun GameTestTrainersAndDexPanel(
  activeChapter: CanonRegionChapter,
  activePartyDexIds: List<Int>,
  onChallengeTrainer: (TrainerArchetype) -> Unit,
  onSummonWildPokemon: (Int) -> Unit,
  onAddPokemonToParty: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activePanelTab by remember { mutableIntStateOf(0) } // 0 = Trainers, 1 = All 1,025 Pokémon
  var trainerQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<TrainerCategory?>(null) }
  var showRegionalTrainersOnly by remember { mutableStateOf(false) }

  var dexQuery by remember { mutableStateOf("") }
  var showRegionalHabitatOnly by remember { mutableStateOf(true) }

  val allTrainers = remember { TrainerArchetypeCatalog.allTrainerTypes }
  val filteredTrainers = remember(allTrainers, trainerQuery, selectedCategory, showRegionalTrainersOnly, activeChapter.id) {
    val regionalSet = activeChapter.regionalTrainerClassIds.toSet()
    val base = when {
      showRegionalTrainersOnly -> allTrainers.filter { it.id in regionalSet }
      selectedCategory != null -> allTrainers.filter { it.category == selectedCategory }
      else -> allTrainers
    }
    val q = trainerQuery.trim()
    if (q.isEmpty()) base else base.filter {
      it.className.contains(q, ignoreCase = true) ||
        it.aiTactic.label.contains(q, ignoreCase = true) ||
        it.sourceLineage.contains(q, ignoreCase = true) ||
        it.preferredTypes.any { t -> t.displayName.contains(q, ignoreCase = true) }
    }
  }

  val fullNationalIndex = remember { NationalAtlasIndex.buildCompleteIndex() }
  val regionalDexSet = remember(activeChapter.id) {
    NationalAtlasIndex.dexPoolForRegion(activeChapter.id).toSet()
  }
  val filteredPokemon = remember(fullNationalIndex, regionalDexSet, showRegionalHabitatOnly, dexQuery) {
    val base = if (showRegionalHabitatOnly) {
      fullNationalIndex.filter { it.dexNumber in regionalDexSet }
    } else {
      fullNationalIndex
    }
    val q = dexQuery.trim()
    if (q.isEmpty()) base else base.filter {
      it.name.contains(q, ignoreCase = true) ||
        it.dexNumber.toString() == q ||
        it.primaryType.contains(q, ignoreCase = true) ||
        it.secondaryType.contains(q, ignoreCase = true)
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, StudioBorderDark, RoundedCornerShape(16.dp))
      .testTag("game_test_trainers_and_dex_panel"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.Groups, contentDescription = null, tint = SuperFxGold, modifier = Modifier.size(18.dp))
          Column {
            Text(
              text = "FUSION WORLD ROSTER • ${allTrainers.size} TRAINER TYPES & ALL 1,025 POKÉMON",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${activeChapter.regionName.substringBefore(" —")}: ${activeChapter.regionalTrainerClassIds.size} Featured Classes • ${regionalDexSet.size} Native Species",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Main Mode Toggle: All Trainer Types vs All 1,025 Pokémon
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = activePanelTab == 0,
          onClick = { activePanelTab = 0 },
          label = { Text("All ${allTrainers.size} Trainer Types", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SuperFxGold.copy(alpha = 0.22f),
            selectedLabelColor = SuperFxGold,
          ),
          modifier = Modifier.testTag("roster_tab_trainers"),
        )
        FilterChip(
          selected = activePanelTab == 1,
          onClick = { activePanelTab = 1 },
          label = { Text("All 1,025 Pokémon (#0001–#1025)", style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
            selectedLabelColor = CyberCyan,
          ),
          modifier = Modifier.testTag("roster_tab_all_pokemon"),
        )
      }

      if (activePanelTab == 0) {
        // Trainer Category Filter Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = !showRegionalTrainersOnly && selectedCategory == null,
            onClick = {
              showRegionalTrainersOnly = false
              selectedCategory = null
            },
            label = { Text("All (${allTrainers.size})", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("trainer_filter_all"),
          )
          FilterChip(
            selected = showRegionalTrainersOnly,
            onClick = {
              showRegionalTrainersOnly = true
              selectedCategory = null
            },
            label = { Text("In Region (${activeChapter.regionalTrainerClassIds.size})", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("trainer_filter_regional"),
          )
          TrainerCategory.entries.forEach { cat ->
            FilterChip(
              selected = !showRegionalTrainersOnly && selectedCategory == cat,
              onClick = {
                showRegionalTrainersOnly = false
                selectedCategory = cat
              },
              label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("trainer_filter_${cat.id}"),
            )
          }
        }

        OutlinedTextField(
          value = trainerQuery,
          onValueChange = { trainerQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          placeholder = { Text("Search Ace Trainer, Ranger, Warlord, Champion, Hex Maniac...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("trainer_roster_search_input"),
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDeepNavy)
            .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(filteredTrainers, key = { it.id }) { trainer ->
            val accent = parseHexColorSafe(trainer.accentHex, SuperFxGold)
            val typesLabel = trainer.preferredTypes.joinToString("/") { it.displayName }
            val gimmickTag = trainer.preferredGimmick
              .takeIf { it != BattleGimmickMode.NONE }
              ?.displayName
              ?.substringBefore(" (")
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioSurfaceDark)
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "${trainer.className} • ${trainer.aiTactic.label}",
                  style = MaterialTheme.typography.labelLarge,
                  color = StudioTextPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = buildString {
                    append(typesLabel)
                    if (gimmickTag != null) append(" • $gimmickTag")
                    append(" • ${trainer.sourceLineage}")
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = accent,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { onChallengeTrainer(trainer) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson, contentColor = Color.White),
                modifier = Modifier.testTag("challenge_trainer_${trainer.id}"),
              ) {
                Text("Battle", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // All 1,025 Pokémon Habitat / National Summoner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = showRegionalHabitatOnly,
            onClick = { showRegionalHabitatOnly = true },
            label = { Text("Region Habitat (${regionalDexSet.size})", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("dex_summoner_regional_filter"),
          )
          FilterChip(
            selected = !showRegionalHabitatOnly,
            onClick = { showRegionalHabitatOnly = false },
            label = { Text("All #0001–#1025 (1,025)", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("dex_summoner_national_filter"),
          )
        }

        OutlinedTextField(
          value = dexQuery,
          onValueChange = { dexQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          placeholder = { Text("Search any of #0001–#1025 by name, number, or type...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dex_summoner_search_input"),
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDeepNavy)
            .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(filteredPokemon, key = { it.dexNumber }) { species ->
            val inParty = species.dexNumber in activePartyDexIds
            val accent = parseHexColorSafe(species.primaryHex, CyberCyan)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioSurfaceDark)
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ProceduralCreatureThumbnail(
                dexNumber = species.dexNumber,
                contentDescription = species.name,
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(StudioDeepNavy),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "#${species.dexNumber.toString().padStart(4, '0')} ${species.name}",
                  style = MaterialTheme.typography.labelLarge,
                  color = StudioTextPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = species.primaryType +
                    (if (species.secondaryType.isNotBlank()) "/${species.secondaryType}" else "") +
                    " • ${species.resonanceRoleTier}",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = { onSummonWildPokemon(species.dexNumber) },
                modifier = Modifier.testTag("summon_wild_${species.dexNumber}"),
              ) {
                Text("Wild", color = SgbEmerald)
              }
              Button(
                onClick = { onAddPokemonToParty(species.dexNumber) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (inParty) SuperFxGold else CyberCyan,
                  contentColor = StudioDeepNavy,
                ),
                modifier = Modifier.testTag("add_party_${species.dexNumber}"),
              ) {
                Text(if (inParty) "Lead" else "+Party", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.EncyclopediaEntryEntity
import com.example.data.local.EncyclopediaSourceStatusEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EncyclopediaKnowledgePanel(
  entries: List<EncyclopediaEntryEntity>,
  sourceStatuses: List<EncyclopediaSourceStatusEntity>,
  isSyncing: Boolean,
  onSyncSourcesNow: () -> Unit,
) {
  var selectedSourceFilter by remember { mutableStateOf("All Sources") }
  var query by remember { mutableStateOf("") }

  val sources = listOf("All Sources", "Bulbapedia", "PokemonDB", "Serebii.net", "All 3 Canon Sources")
  val filtered = remember(entries, selectedSourceFilter, query) {
    entries.filter { entry ->
      val srcMatch = selectedSourceFilter == "All Sources" ||
        entry.sourceOrigin.equals(selectedSourceFilter, ignoreCase = true)
      val qMatch = query.isBlank() ||
        entry.title.contains(query, ignoreCase = true) ||
        entry.summary.contains(query, ignoreCase = true) ||
        entry.structuredData.contains(query, ignoreCase = true) ||
        entry.domainCategory.contains(query, ignoreCase = true)
      srcMatch && qMatch
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, SuperFxGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
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
              text = "BULBAPEDIA • POKÉMONDB • SEREBII DEFAULT ROOM DB",
              style = MaterialTheme.typography.labelSmall,
              color = SuperFxGold,
            )
            Text(
              text = "Auto-Updated Encyclopedia (${entries.size} Records)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          Button(
            enabled = !isSyncing,
            onClick = onSyncSourcesNow,
            colors = ButtonDefaults.buttonColors(containerColor = SuperFxGold, contentColor = StudioDeepNavy),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("encyclopedia_sync_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Sync Bulbapedia PokemonDB and Serebii", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSyncing) "Syncing..." else "Sync Web", fontWeight = FontWeight.Bold)
          }
        }

        // Live Source Feed Status Badges
        sourceStatuses.forEach { src ->
          val accent = when (src.sourceId) {
            "bulbapedia" -> SgbEmerald
            "pokemondb" -> Mode7Sky
            else -> SuperFxGold
          }
          val timeText = if (src.lastSyncEpochMs > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(src.lastSyncEpochMs))
          } else {
            "Seeded"
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.Public, contentDescription = src.displayName, tint = accent, modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${src.displayName} (${src.baseUrl.removePrefix("https://")})",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = src.liveHeadline,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = if (src.httpStatusCode > 0) "HTTP ${src.httpStatusCode}\n$timeText" else timeText,
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextPrimary,
            )
          }
        }

        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search encyclopedia database") },
          placeholder = { Text("Search Natures, Egg Groups, Catch Formula, Items, Encounter Tables...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("encyclopedia_search_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          sources.forEach { src ->
            FilterChip(
              selected = selectedSourceFilter == src,
              onClick = { selectedSourceFilter = src },
              label = { Text(src, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }
    }

    filtered.forEach { entry ->
      val badgeColor = when (entry.sourceOrigin) {
        "Bulbapedia" -> SgbEmerald
        "PokemonDB" -> Mode7Sky
        "Serebii.net" -> SuperFxGold
        else -> VoxelViolet
      }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
          .testTag("encyclopedia_entry_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.MenuBook, contentDescription = entry.sourceOrigin, tint = badgeColor, modifier = Modifier.size(16.dp))
              Text(
                text = "${entry.sourceOrigin} • ${entry.domainCategory}",
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = entry.dexRangeOrTag,
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
          }
          Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
          )
          Text(
            text = entry.summary,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary,
          )
          Text(
            text = entry.structuredData,
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCrimson.copy(alpha = 0.9f),
          )
        }
      }
    }
  }
}

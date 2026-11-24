package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.EncyclopediaEntryEntity
import com.example.data.local.EncyclopediaSourceStatusEntity
import com.example.data.local.NativePokemonEntity
import com.example.data.model.CustomDexEntry
import com.example.ui.UiState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.SgbEmerald
import com.example.ui.theme.StudioBorderDark
import com.example.ui.theme.StudioCardDark
import com.example.ui.theme.StudioDeepNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.SuperFxGold
import com.example.ui.theme.VoxelViolet

@Composable
fun FranchiseDexAndCreatorSection(
  nativeList: List<NativePokemonEntity>,
  customCloudDexState: UiState<List<CustomDexEntry>>,
  encyclopediaEntries: List<EncyclopediaEntryEntity>,
  encyclopediaSourceStatuses: List<EncyclopediaSourceStatusEntity>,
  atlasProgressMap: Map<Int, AtlasProgressEntity>,
  selectedPrimary: NativePokemonEntity,
  selectedSecondary: NativePokemonEntity,
  searchQuery: String,
  selectedGen: String,
  isSyncingPokeApi: Boolean,
  isSyncingSources: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onSelectGen: (String) -> Unit,
  onSelectPrimary: (NativePokemonEntity) -> Unit,
  onSelectSecondary: (NativePokemonEntity) -> Unit,
  onImportFromPokeApi: (String) -> Unit,
  onCreateCustomPokemon: (NativePokemonEntity, Boolean) -> Unit,
  onDeleteCloudCustomDex: (String) -> Unit,
  onSyncEncyclopediaNow: () -> Unit,
  onPlayAtlasSpeciesCry: (AtlasSpeciesIndexItem) -> Unit,
  onHarmonizeSpecies: (Int) -> Unit,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var pokeApiInput by remember { mutableStateOf("") }
  var showEncyclopediaMode by remember { mutableStateOf(false) }

  val generations = listOf(
    "All", "Gen I", "Gen II", "Gen III", "Gen IV",
    "Gen V", "Gen VI", "Gen VII", "Gen VIII", "Gen IX",
  )

  val filteredPokemon = nativeList.filter { pkmn ->
    val matchesGen = selectedGen == "All" || pkmn.generation.equals(selectedGen, ignoreCase = true)
    val matchesQuery = searchQuery.isBlank() ||
      pkmn.name.contains(searchQuery, ignoreCase = true) ||
      pkmn.dexNumber.toString() == searchQuery.trim() ||
      pkmn.primaryType.contains(searchQuery, ignoreCase = true) ||
      pkmn.secondaryType.contains(searchQuery, ignoreCase = true) ||
      pkmn.region.contains(searchQuery, ignoreCase = true)
    matchesGen && matchesQuery
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = !showEncyclopediaMode,
        onClick = { showEncyclopediaMode = false },
        label = { Text("Resonance Atlas (#0001–#1025)") },
        modifier = Modifier.weight(1f).testTag("dex_mode_species"),
      )
      FilterChip(
        selected = showEncyclopediaMode,
        onClick = { showEncyclopediaMode = true },
        label = { Text("Bulbapedia•DB•Serebii (${encyclopediaEntries.size})") },
        modifier = Modifier.weight(1f).testTag("dex_mode_encyclopedia"),
      )
    }

    if (showEncyclopediaMode) {
      EncyclopediaKnowledgePanel(
        entries = encyclopediaEntries,
        sourceStatuses = encyclopediaSourceStatuses,
        isSyncing = isSyncingSources,
        onSyncSourcesNow = onSyncEncyclopediaNow,
      )
      return@Column
    }

    ResonanceAtlasDexCard(
      nativeList = nativeList,
      atlasProgressMap = atlasProgressMap,
      isSyncingPokeApi = isSyncingPokeApi,
      onHydrateFromPokeApi = onImportFromPokeApi,
      onPlaySpeciesCry = onPlayAtlasSpeciesCry,
      onHarmonizeSpecies = onHarmonizeSpecies,
    )

    // Header & Create Unique Species CTA
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
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
              text = "INTEGRATED FRANCHISE DATABASE & FAKEMON FORGE",
              style = MaterialTheme.typography.labelSmall,
              color = CyberCyan,
            )
            Text(
              text = "Native Gen I–IX Dex + Custom Species Builder",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = ElectricCrimson,
              contentColor = StudioTextPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_unique_pokemon_button"),
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Unique Species")
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "New Species", style = MaterialTheme.typography.labelLarge)
          }
        }

        // Live National Dex (#1-#1025) PokeAPI Importer Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = pokeApiInput,
            onValueChange = { pokeApiInput = it },
            placeholder = { Text("Import any National Dex #1-1025 or name (e.g. 212, scizor, sylveon)") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("pokeapi_import_input"),
          )
          Button(
            onClick = { onImportFromPokeApi(pokeApiInput) },
            enabled = !isSyncingPokeApi && pokeApiInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
              containerColor = SuperFxGold,
              contentColor = StudioDeepNavy,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(52.dp)
              .testTag("pokeapi_import_button"),
          ) {
            if (isSyncingPokeApi) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = StudioDeepNavy,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Import")
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pull Dex", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        // Search & Generation Filter Chips
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
          placeholder = { Text("Filter integrated database by name, #ID, type, or region...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dex_search_input"),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          generations.forEach { gen ->
            FilterChip(
              selected = selectedGen == gen,
              onClick = { onSelectGen(gen) },
              label = { Text(gen, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }
    }

    // Local Room Vault Custom Species / Fakemon Section
    when (customCloudDexState) {
      is UiState.Loading -> {}
      is UiState.Error -> {
        Text(
          text = "Local Custom Dex notice: ${customCloudDexState.message}",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      is UiState.Success -> {
        if (customCloudDexState.data.isNotEmpty()) {
          Text(
            text = "YOUR LOCAL ROOM VAULT CUSTOM SPECIES (${customCloudDexState.data.size})",
            style = MaterialTheme.typography.labelLarge,
            color = SuperFxGold,
          )
          customCloudDexState.data.forEach { customEntry ->
            CustomDexCloudCard(
              entry = customEntry,
              onLoadIntoViewport = {
                onSelectPrimary(
                  NativePokemonEntity(
                    dexNumber = customEntry.baseSpriteId.toInt(),
                    name = customEntry.name,
                    generation = "Custom",
                    region = customEntry.category,
                    primaryType = customEntry.primaryType,
                    secondaryType = customEntry.secondaryType,
                    abilityPrimary = customEntry.abilityName,
                    abilityHidden = customEntry.abilityName,
                    signatureMove = customEntry.signatureMove,
                    baseHp = customEntry.baseHp.toInt(),
                    baseAttack = customEntry.baseAttack.toInt(),
                    baseDefense = customEntry.baseDefense.toInt(),
                    baseSpAtk = customEntry.baseSpAtk.toInt(),
                    baseSpDef = customEntry.baseSpDef.toInt(),
                    baseSpeed = customEntry.baseSpeed.toInt(),
                    consoleOriginSystem = customEntry.fusionStylePreset,
                    recommendedChipAddon = customEntry.enhancementChip,
                    primaryHex = customEntry.primaryHex,
                    secondaryHex = customEntry.secondaryHex,
                    loreSummary = customEntry.loreDescription,
                    isUserModified = true,
                  )
                )
              },
              onDelete = { onDeleteCloudCustomDex(customEntry.id) },
            )
          }
        }
      }
    }

    // Native Integrated Franchise Database List (Bounded LazyColumn for 60fps scrolling)
    Text(
      text = "NATIVE FRANCHISE DATABASE (${filteredPokemon.size} SPECIES READY)",
      style = MaterialTheme.typography.labelLarge,
      color = CyberCyan,
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 240.dp, max = 560.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(StudioDeepNavy.copy(alpha = 0.6f))
        .padding(6.dp)
        .testTag("native_dex_lazy_list"),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(filteredPokemon, key = { "${it.dexNumber}_${it.name}" }) { pokemon ->
        NativePokemonCard(
          pokemon = pokemon,
          isPrimary = pokemon.dexNumber == selectedPrimary.dexNumber && pokemon.name == selectedPrimary.name,
          isSecondary = pokemon.dexNumber == selectedSecondary.dexNumber && pokemon.name == selectedSecondary.name,
          onSelectPrimary = { onSelectPrimary(pokemon) },
          onSelectSecondary = { onSelectSecondary(pokemon) },
        )
      }
    }
  }

  if (showCreateDialog) {
    CreateUniquePokemonDialog(
      defaultSpriteSeed = selectedPrimary.dexNumber,
      onDismiss = { showCreateDialog = false },
      onSave = { entity, syncCloud ->
        onCreateCustomPokemon(entity, syncCloud)
        showCreateDialog = false
      },
    )
  }
}

@Composable
private fun NativePokemonCard(
  pokemon: NativePokemonEntity,
  isPrimary: Boolean,
  isSecondary: Boolean,
  onSelectPrimary: () -> Unit,
  onSelectSecondary: () -> Unit,
) {
  val accentColor = parseHexColorSafe(pokemon.primaryHex, CyberCyan)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isPrimary || isSecondary) 2.dp else 1.dp,
        color = when {
          isPrimary -> CyberCyan
          isSecondary -> ElectricCrimson
          else -> StudioBorderDark
        },
        shape = RoundedCornerShape(16.dp),
      )
      .clickable { onSelectPrimary() }
      .testTag("dex_card_${pokemon.dexNumber}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Dual Sprite + 3D Preview Box
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudioDeepNavy)
            .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          ProceduralCreatureThumbnail(
            dexNumber = pokemon.dexNumber,
            contentDescription = pokemon.name,
            customPrimaryHex = if (pokemon.isUserModified) pokemon.primaryHex else null,
            customSecondaryHex = if (pokemon.isUserModified) pokemon.secondaryHex else null,
            modifier = Modifier.size(64.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "#${pokemon.dexNumber.toString().padStart(4, '0')} ${pokemon.name}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = StudioTextPrimary,
            )
            TypeBadge(type = pokemon.primaryType, color = accentColor)
            if (pokemon.secondaryType.isNotBlank()) {
              TypeBadge(
                type = pokemon.secondaryType,
                color = parseHexColorSafe(pokemon.secondaryHex, VoxelViolet),
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "${pokemon.generation} (${pokemon.region}) • Ability: ${pokemon.abilityPrimary} • Signature: ${pokemon.signatureMove}",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary,
          )

          Text(
            text = "System Lineage: ${pokemon.consoleOriginSystem} [${pokemon.recommendedChipAddon}]",
            style = MaterialTheme.typography.labelSmall,
            color = SuperFxGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = pokemon.loreSummary,
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary,
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Base Stat Bar Summary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatMiniPill("HP", pokemon.baseHp)
        StatMiniPill("ATK", pokemon.baseAttack)
        StatMiniPill("DEF", pokemon.baseDefense)
        StatMiniPill("SPA", pokemon.baseSpAtk)
        StatMiniPill("SPD", pokemon.baseSpDef)
        StatMiniPill("SPE", pokemon.baseSpeed)
        Text(
          text = "BST ${pokemon.baseStatTotal}",
          style = MaterialTheme.typography.labelLarge,
          color = CyberCyan,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onSelectPrimary,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) CyberCyan else StudioDeepNavy,
            contentColor = if (isPrimary) StudioDeepNavy else CyberCyan,
          ),
        ) {
          Text(
            text = if (isPrimary) "★ Active Primary Base" else "Set Primary Rig",
            style = MaterialTheme.typography.labelSmall,
          )
        }

        OutlinedButton(
          onClick = onSelectSecondary,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = if (isSecondary) "★ Active DNA Donor" else "Set DNA / Palette Donor",
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCrimson,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomDexCloudCard(
  entry: CustomDexEntry,
  onLoadIntoViewport: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, SuperFxGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = StudioCardDark),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ProceduralCreatureThumbnail(
        dexNumber = entry.baseSpriteId.toInt().coerceIn(1, 1025),
        contentDescription = entry.name,
        modifier = Modifier.size(52.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "#${entry.dexNumber} ${entry.name} (${entry.primaryType}${if (entry.secondaryType.isNotBlank()) "/${entry.secondaryType}" else ""})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = SuperFxGold,
        )
        Text(
          text = "${entry.fusionStylePreset} • Chip: ${entry.enhancementChip} • BST ${entry.baseStatTotal}",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
        )
        Text(
          text = entry.loreDescription,
          style = MaterialTheme.typography.bodySmall,
          color = StudioTextPrimary,
          maxLines = 2,
        )
      }
      TextButton(onClick = onLoadIntoViewport) {
        Text("Load", color = CyberCyan)
      }
      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete Custom Species",
          tint = StudioTextSecondary,
        )
      }
    }
  }
}

@Composable
private fun TypeBadge(type: String, color: androidx.compose.ui.graphics.Color) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.2f))
      .border(1.dp, color, RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  ) {
    Text(
      text = type.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextPrimary,
    )
  }
}

@Composable
private fun StatMiniPill(label: String, value: Int) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = "$label $value",
      style = MaterialTheme.typography.labelSmall,
      color = StudioTextSecondary,
    )
    LinearProgressIndicator(
      progress = { (value / 180f).coerceIn(0.08f, 1f) },
      modifier = Modifier
        .width(36.dp)
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = SgbEmerald,
      trackColor = StudioDeepNavy,
    )
  }
}

@Composable
private fun CreateUniquePokemonDialog(
  defaultSpriteSeed: Int,
  onDismiss: () -> Unit,
  onSave: (NativePokemonEntity, Boolean) -> Unit,
) {
  var dexNumberStr by remember { mutableStateOf("1026") }
  var name by remember { mutableStateOf("Aetherwyrm-FX") }
  var region by remember { mutableStateOf("Chrono-Kanto") }
  var primaryType by remember { mutableStateOf("Dragon") }
  var secondaryType by remember { mutableStateOf("Electric") }
  var ability by remember { mutableStateOf("Super FX Overdrive") }
  var signatureMove by remember { mutableStateOf("Mode-7 Rayburst") }
  var baseSpriteSeedStr by remember { mutableStateOf(defaultSpriteSeed.toString()) }
  var consoleSystem by remember { mutableStateOf("GBC Hi-Color + GBA + N64 + Switch HD-2D") }
  var chipAddon by remember { mutableStateOf("Super FX2 + Voxelizer-FX") }
  var lore by remember {
    mutableStateOf("A custom fan-game Pokémon whose scales shift between 16-bit SGB pixel tiles and 3D cel-shaded polygons.")
  }
  var syncToGoogleCloud by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StudioCardDark,
    title = {
      Text(
        text = "Add Unique Pokémon / Fakemon",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = CyberCyan,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = dexNumberStr,
            onValueChange = { dexNumberStr = it },
            label = { Text("Dex #") },
            singleLine = true,
            modifier = Modifier.weight(0.4f),
          )
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Species Name") },
            singleLine = true,
            modifier = Modifier
              .weight(0.6f)
              .testTag("custom_pokemon_name_input"),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = primaryType,
            onValueChange = { primaryType = it },
            label = { Text("Type 1") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
          OutlinedTextField(
            value = secondaryType,
            onValueChange = { secondaryType = it },
            label = { Text("Type 2") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = ability,
            onValueChange = { ability = it },
            label = { Text("Ability") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
          OutlinedTextField(
            value = signatureMove,
            onValueChange = { signatureMove = it },
            label = { Text("Signature Move") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = baseSpriteSeedStr,
            onValueChange = { baseSpriteSeedStr = it },
            label = { Text("Base Rig ID (1-1025)") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
          OutlinedTextField(
            value = chipAddon,
            onValueChange = { chipAddon = it },
            label = { Text("Hardware Chip") },
            singleLine = true,
            modifier = Modifier.weight(0.5f),
          )
        }

        OutlinedTextField(
          value = lore,
          onValueChange = { lore = it },
          label = { Text("Pokédex Lore & Custom Data") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Pin to Local Room Custom Dex Vault",
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextPrimary,
          )
          Switch(
            checked = syncToGoogleCloud,
            onCheckedChange = { syncToGoogleCloud = it },
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val dexNum = dexNumberStr.toIntOrNull()?.coerceIn(1, 9999) ?: 1026
          val seedId = baseSpriteSeedStr.toIntOrNull()?.coerceIn(1, 1025) ?: defaultSpriteSeed
          val entity = NativePokemonEntity(
            dexNumber = dexNum,
            name = name.ifBlank { "Vectragon" },
            generation = "Custom",
            region = region.ifBlank { "Omni-Region" },
            primaryType = primaryType.ifBlank { "Dragon" },
            secondaryType = secondaryType,
            abilityPrimary = ability.ifBlank { "Polygon Core" },
            abilityHidden = ability.ifBlank { "Polygon Core" },
            signatureMove = signatureMove.ifBlank { "Mode-7 Beam" },
            baseHp = 95,
            baseAttack = 115,
            baseDefense = 90,
            baseSpAtk = 125,
            baseSpDef = 90,
            baseSpeed = 110,
            consoleOriginSystem = consoleSystem,
            recommendedChipAddon = chipAddon,
            primaryHex = "#00F5D4",
            secondaryHex = "#FF2A85",
            loreSummary = "$lore (Rig Seed #$seedId)",
            isUserModified = true,
          )
          onSave(entity, syncToGoogleCloud)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = CyberCyan,
          contentColor = StudioDeepNavy,
        ),
        modifier = Modifier.testTag("save_custom_pokemon_button"),
      ) {
        Text("Save Species")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

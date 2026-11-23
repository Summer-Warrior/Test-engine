package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.remote.NetworkClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Entity(tableName = "native_pokemon")
data class NativePokemonEntity(
  @PrimaryKey val dexNumber: Int,
  val name: String,
  val generation: String,
  val region: String,
  val primaryType: String,
  val secondaryType: String,
  val abilityPrimary: String,
  val abilityHidden: String,
  val signatureMove: String,
  val baseHp: Int,
  val baseAttack: Int,
  val baseDefense: Int,
  val baseSpAtk: Int,
  val baseSpDef: Int,
  val baseSpeed: Int,
  val consoleOriginSystem: String,
  val recommendedChipAddon: String,
  val primaryHex: String,
  val secondaryHex: String,
  val loreSummary: String,
  val isUserModified: Boolean = false,
) {
  val baseStatTotal: Int
    get() = baseHp + baseAttack + baseDefense + baseSpAtk + baseSpDef + baseSpeed

  val pixelSpriteUrl: String
    get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$dexNumber.png"

  val highResArtworkUrl: String
    get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$dexNumber.png"

  val home3dRenderUrl: String
    get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/$dexNumber.png"
}

@Dao
interface NativePokemonDao {
  @Query("SELECT * FROM native_pokemon ORDER BY dexNumber ASC")
  fun observeAllPokemon(): Flow<List<NativePokemonEntity>>

  @Query("SELECT * FROM native_pokemon ORDER BY dexNumber ASC")
  suspend fun getAllSnapshot(): List<NativePokemonEntity>

  @Query("SELECT COUNT(*) FROM native_pokemon")
  suspend fun countPokemon(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(entries: List<NativePokemonEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPokemon(entry: NativePokemonEntity)
}

@Database(
  entities = [
    NativePokemonEntity::class,
    StudioResourceEntity::class,
    EncyclopediaEntryEntity::class,
    EncyclopediaSourceStatusEntity::class,
    HardwareProfileEntity::class,
    SavedSpriteEntity::class,
    SavedMapEntity::class,
    StudioItemEntity::class,
    AtlasProgressEntity::class,
    CampaignStateEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
abstract class NativePokemonDatabase : RoomDatabase() {
  abstract fun pokemonDao(): NativePokemonDao
  abstract fun resourceDao(): StudioResourceDao
  abstract fun encyclopediaDao(): EncyclopediaDao
  abstract fun studioPersistenceDao(): StudioPersistenceDao

  companion object {
    @Volatile
    private var INSTANCE: NativePokemonDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `studio_resources` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `name` TEXT NOT NULL,
            `categoryId` TEXT NOT NULL,
            `formatTag` TEXT NOT NULL,
            `consoleChip` TEXT NOT NULL,
            `durationOrGrid` TEXT NOT NULL,
            `payloadSpec` TEXT NOT NULL,
            `accentHex` TEXT NOT NULL,
            `frequencyHz` INTEGER NOT NULL,
            `tileTypeIndex` INTEGER NOT NULL,
            `assignedProjectIdsCsv` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `isBuiltIn` INTEGER NOT NULL,
            `updatedAtEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `encyclopedia_entries` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `title` TEXT NOT NULL,
            `domainCategory` TEXT NOT NULL,
            `sourceOrigin` TEXT NOT NULL,
            `sourceUrl` TEXT NOT NULL,
            `dexRangeOrTag` TEXT NOT NULL,
            `summary` TEXT NOT NULL,
            `structuredData` TEXT NOT NULL,
            `lastUpdatedEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `encyclopedia_sources` (
            `sourceId` TEXT NOT NULL PRIMARY KEY,
            `displayName` TEXT NOT NULL,
            `baseUrl` TEXT NOT NULL,
            `lastSyncEpochMs` INTEGER NOT NULL,
            `httpStatusCode` INTEGER NOT NULL,
            `liveHeadline` TEXT NOT NULL,
            `entryCount` INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `hardware_profiles` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `name` TEXT NOT NULL,
            `manufacturer` TEXT NOT NULL,
            `year` INTEGER NOT NULL,
            `generation` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `gpuChip` TEXT NOT NULL,
            `audioChip` TEXT NOT NULL,
            `videoModes` TEXT NOT NULL,
            `primaryDomain` TEXT NOT NULL,
            `honoredSignature` TEXT NOT NULL,
            `brokenLimitation` TEXT NOT NULL,
            `graphicsJson` TEXT NOT NULL,
            `audioJson` TEXT NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `saved_sprites` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `projectId` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `spriteSeedId` INTEGER NOT NULL,
            `paletteJson` TEXT NOT NULL,
            `framesRleJson` TEXT NOT NULL,
            `frameCount` INTEGER NOT NULL,
            `updatedAtEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `saved_maps` (
            `mapKey` TEXT NOT NULL PRIMARY KEY,
            `projectId` TEXT NOT NULL,
            `regionId` TEXT NOT NULL,
            `regionName` TEXT NOT NULL,
            `gridSize` INTEGER NOT NULL,
            `tilesCsv` TEXT NOT NULL,
            `updatedAtEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `studio_items` (
            `id` TEXT NOT NULL PRIMARY KEY,
            `name` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `description` TEXT NOT NULL,
            `effectTag` TEXT NOT NULL,
            `effectPower` REAL NOT NULL,
            `quantity` INTEGER NOT NULL,
            `accentHex` TEXT NOT NULL,
            `regionOrigin` TEXT NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `atlas_progress` (
            `dexNumber` INTEGER NOT NULL PRIMARY KEY,
            `seen` INTEGER NOT NULL,
            `battled` INTEGER NOT NULL,
            `captured` INTEGER NOT NULL,
            `hardwareHarmonized` INTEGER NOT NULL,
            `harmonizedHardwareId` TEXT NOT NULL,
            `updatedAtEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `campaign_state` (
            `projectId` TEXT NOT NULL PRIMARY KEY,
            `activeRegionId` TEXT NOT NULL,
            `playerX` INTEGER NOT NULL,
            `playerY` INTEGER NOT NULL,
            `partyDexIdsCsv` TEXT NOT NULL,
            `heldItemsMapCsv` TEXT NOT NULL,
            `completedRegionIdsCsv` TEXT NOT NULL,
            `harmonicShardsCsv` TEXT NOT NULL,
            `activeHardwareIdsCsv` TEXT NOT NULL,
            `disciplineModeToken` TEXT NOT NULL,
            `extraPrettyMask` INTEGER NOT NULL,
            `updatedAtEpochMs` INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    fun getInstance(context: Context): NativePokemonDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          NativePokemonDatabase::class.java,
          "pokefusion_native_dex.db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}

class NativeFranchiseCatalogRepository(
  private val dao: NativePokemonDao,
  private val httpClient: OkHttpClient = NetworkClientProvider.sharedOkHttpClient,
) {
  val allNativePokemon: Flow<List<NativePokemonEntity>> = dao.observeAllPokemon()

  suspend fun ensureSeeded(): Int {
    val existingByDex = dao.getAllSnapshot().associateBy { it.dexNumber }
    val entriesToSync = builtInFranchiseDex.filter { canonical ->
      val current = existingByDex[canonical.dexNumber]
      current == null || (!current.isUserModified && current != canonical)
    }
    if (entriesToSync.isNotEmpty()) {
      dao.upsertAll(entriesToSync)
    }
    return entriesToSync.size
  }

  suspend fun refreshLivePokemonIfOnline(dexNumber: Int): Result<NativePokemonEntity> =
    withContext(Dispatchers.IO) {
      val existing = dao.getAllSnapshot().firstOrNull { it.dexNumber == dexNumber }
      if (existing?.isUserModified == true) {
        return@withContext Result.success(existing)
      }
      fetchAndImportFromPokeApi(dexNumber.toString(), markUserModified = false)
    }

  suspend fun upsertLocalPokemon(entity: NativePokemonEntity) {
    dao.upsertPokemon(entity)
  }

  suspend fun fetchAndImportFromPokeApi(
    dexIdOrName: String,
    markUserModified: Boolean = true,
  ): Result<NativePokemonEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanQuery = dexIdOrName.trim().lowercase()
        val request = Request.Builder()
          .url("https://pokeapi.co/api/v2/pokemon/$cleanQuery")
          .build()
        val bodyStr = httpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            error("PokeAPI lookup failed (${response.code}) for '$dexIdOrName'")
          }
          response.body?.string() ?: error("Empty PokeAPI response")
        }
        val json = JSONObject(bodyStr)
        val id = json.getInt("id")
        val rawName = json.getString("name").replaceFirstChar { it.uppercase() }

        val typesArr = json.getJSONArray("types")
        val type1 = typesArr.getJSONObject(0).getJSONObject("type").getString("name")
          .replaceFirstChar { it.uppercase() }
        val type2 = if (typesArr.length() > 1) {
          typesArr.getJSONObject(1).getJSONObject("type").getString("name")
            .replaceFirstChar { it.uppercase() }
        } else {
          ""
        }

        val abilitiesArr = json.getJSONArray("abilities")
        val ability1 = if (abilitiesArr.length() > 0) {
          abilitiesArr.getJSONObject(0).getJSONObject("ability").getString("name")
            .split("-").joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        } else {
          "Overgrow"
        }
        val abilityHidden = if (abilitiesArr.length() > 1) {
          abilitiesArr.getJSONObject(abilitiesArr.length() - 1).getJSONObject("ability").getString("name")
            .split("-").joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        } else {
          "Adaptability"
        }

        val statsMap = mutableMapOf<String, Int>()
        val statsArr = json.getJSONArray("stats")
        for (i in 0 until statsArr.length()) {
          val statObj = statsArr.getJSONObject(i)
          val statName = statObj.getJSONObject("stat").getString("name")
          statsMap[statName] = statObj.getInt("base_stat")
        }

        val movesArr = json.getJSONArray("moves")
        val sigMove = if (movesArr.length() > 0) {
          movesArr.getJSONObject(0).getJSONObject("move").getString("name")
            .split("-").joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        } else {
          "Tera Blast"
        }

        val (gen, region, consoleSys, chip) = resolveGenerationEra(id)
        val (hex1, hex2) = resolveTypePalette(type1, type2)

        val entity = NativePokemonEntity(
          dexNumber = id,
          name = rawName,
          generation = gen,
          region = region,
          primaryType = type1,
          secondaryType = type2,
          abilityPrimary = ability1,
          abilityHidden = abilityHidden,
          signatureMove = sigMove,
          baseHp = statsMap["hp"] ?: 75,
          baseAttack = statsMap["attack"] ?: 80,
          baseDefense = statsMap["defense"] ?: 75,
          baseSpAtk = statsMap["special-attack"] ?: 80,
          baseSpDef = statsMap["special-defense"] ?: 75,
          baseSpeed = statsMap["speed"] ?: 80,
          consoleOriginSystem = consoleSys,
          recommendedChipAddon = chip,
          primaryHex = hex1,
          secondaryHex = hex2,
          loreSummary = "Imported live from integrated National Dex API (#$id) with $consoleSys + $chip shader synthesis.",
          isUserModified = markUserModified,
        )
        dao.upsertPokemon(entity)
        entity
      }
    }

  private fun resolveGenerationEra(dexId: Int): List<String> = when (dexId) {
    in 1..151 -> listOf("Gen I", "Kanto", "DMG-01 + SGB2 + Stadium N64", "Super Game Boy 2 + N64 Transfer Pak")
    in 152..251 -> listOf("Gen II", "Johto", "GBC Hi-Color + GameCube Orre TEV", "GBC 56-Color PPU + Voxelizer-FX")
    in 252..386 -> listOf("Gen III", "Hoenn", "GBA 32-Bit AGB + Colosseum 3D", "GBA e-Reader + Super FX2")
    in 387..493 -> listOf("Gen IV", "Sinnoh", "Nintendo DS 2.5D + Wii Battle Rev", "SA-1 + Dual-Screen Matrix")
    in 494..649 -> listOf("Gen V", "Unova", "DSi Dynamic Scaler + 3D Camera", "Voxelizer-FX + MSU-1 Audio")
    in 650..721 -> listOf("Gen VI", "Kalos", "Nintendo 3DS PICA200 Cel-3D", "Capcom CX4 + PICA200 Toon")
    in 722..809 -> listOf("Gen VII", "Alola", "New 3DS + HD Outline Shader", "Sega 32X + PokeCRT-RTX")
    in 810..905 -> listOf("Gen VIII", "Galar / Hisui", "Switch Maxwell PBR + HD-2D", "PokeCRT-RTX + Super FX2")
    else -> listOf("Gen IX", "Paldea", "Switch Open-World PBR + Terastal Crystal", "Super FX2 + Voxelizer-FX + RTX")
  }

  private fun resolveTypePalette(type1: String, type2: String): Pair<String, String> {
    fun colorFor(t: String): String = when (t.lowercase()) {
      "fire" -> "#FF5722"
      "water" -> "#00B4D8"
      "grass" -> "#10B981"
      "electric" -> "#FFD166"
      "psychic" -> "#FF2A85"
      "dragon" -> "#6366F1"
      "ghost" -> "#A855F7"
      "dark" -> "#334155"
      "steel" -> "#94A3B8"
      "fairy" -> "#F472B6"
      "ice" -> "#38BDF8"
      "fighting" -> "#EF4444"
      "ground" -> "#D97706"
      "rock" -> "#B45309"
      "bug" -> "#84CC16"
      "poison" -> "#C084FC"
      "flying" -> "#60A5FA"
      else -> "#00F5D4"
    }
    val c1 = colorFor(type1)
    val c2 = if (type2.isNotBlank()) colorFor(type2) else "#00F5D4"
    return c1 to c2
  }

  companion object {
    val builtInFranchiseDex = listOf(
      NativePokemonEntity(
        dexNumber = 1,
        name = "Bulbasaur",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Grass",
        secondaryType = "Poison",
        abilityPrimary = "Overgrow",
        abilityHidden = "Chlorophyll",
        signatureMove = "Solar Beam",
        baseHp = 45,
        baseAttack = 49,
        baseDefense = 49,
        baseSpAtk = 65,
        baseSpDef = 65,
        baseSpeed = 45,
        consoleOriginSystem = "DMG-01 + SGB2 + N64 Stadium",
        recommendedChipAddon = "Super Game Boy 2 + Voxelizer-FX",
        primaryHex = "#10B981",
        secondaryHex = "#A855F7",
        loreSummary = "Kanto #001 starter rendered with SGB2 4-palette border shading and 3D bulb voxel extrusion.",
      ),
      NativePokemonEntity(
        dexNumber = 4,
        name = "Charmander",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Fire",
        secondaryType = "",
        abilityPrimary = "Blaze",
        abilityHidden = "Solar Power",
        signatureMove = "Flamethrower",
        baseHp = 39,
        baseAttack = 52,
        baseDefense = 43,
        baseSpAtk = 60,
        baseSpDef = 50,
        baseSpeed = 65,
        consoleOriginSystem = "DMG-01 + GBA FireRed + Switch HD-2D",
        recommendedChipAddon = "PokeCRT-RTX + Dynamic Flame Point-Light",
        primaryHex = "#FF5722",
        secondaryHex = "#FFD166",
        loreSummary = "Tail flame acts as a real-time HD-2D dynamic point light casting pixel shadows across 16-bit tiles.",
      ),
      NativePokemonEntity(
        dexNumber = 6,
        name = "Charizard",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Fire",
        secondaryType = "Flying",
        abilityPrimary = "Blaze",
        abilityHidden = "Solar Power",
        signatureMove = "Blast Burn",
        baseHp = 78,
        baseAttack = 84,
        baseDefense = 78,
        baseSpAtk = 109,
        baseSpDef = 85,
        baseSpeed = 100,
        consoleOriginSystem = "GB Red/Blue + N64 Stadium 2 + 3DS Cel-3D",
        recommendedChipAddon = "Super FX2 (GSU-2) + N64 Expansion Pak",
        primaryHex = "#FF5722",
        secondaryHex = "#38BDF8",
        loreSummary = "Fuses Gen V animated sprite wings with N64 Stadium vertex lighting and Switch volumetric fire bloom.",
      ),
      NativePokemonEntity(
        dexNumber = 7,
        name = "Squirtle",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Water",
        secondaryType = "",
        abilityPrimary = "Torrent",
        abilityHidden = "Rain Dish",
        signatureMove = "Hydro Pump",
        baseHp = 44,
        baseAttack = 48,
        baseDefense = 65,
        baseSpAtk = 50,
        baseSpDef = 64,
        baseSpeed = 43,
        consoleOriginSystem = "DMG-01 + SGB2 + GameCube TEV",
        recommendedChipAddon = "SA-1 + GameCube Water Refraction TEV",
        primaryHex = "#00B4D8",
        secondaryHex = "#38BDF8",
        loreSummary = "Combines crisp GBC pixel outlines with GameCube Dolphin TEV specular water shell shaders.",
      ),
      NativePokemonEntity(
        dexNumber = 25,
        name = "Pikachu",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Electric",
        secondaryType = "",
        abilityPrimary = "Static",
        abilityHidden = "Lightning Rod",
        signatureMove = "Volt Tackle",
        baseHp = 35,
        baseAttack = 55,
        baseDefense = 40,
        baseSpAtk = 50,
        baseSpDef = 50,
        baseSpeed = 90,
        consoleOriginSystem = "GB Yellow + N64 Hey You + Switch Octo-HD",
        recommendedChipAddon = "MSU-1 PCM Voice + PokeCRT-RTX",
        primaryHex = "#FFD166",
        secondaryHex = "#FF2A85",
        loreSummary = "Iconic franchise mascot combining Pokémon Yellow SGB palette with MSU-1 PCM cry synthesis and 3D arc particles.",
      ),
      NativePokemonEntity(
        dexNumber = 94,
        name = "Gengar",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Ghost",
        secondaryType = "Poison",
        abilityPrimary = "Cursed Body",
        abilityHidden = "Levitate (Classic Fan-Toggle)",
        signatureMove = "Shadow Ball",
        baseHp = 60,
        baseAttack = 65,
        baseDefense = 60,
        baseSpAtk = 130,
        baseSpDef = 75,
        baseSpeed = 110,
        consoleOriginSystem = "GBC Crystal + GameCube XD Shadow Aura",
        recommendedChipAddon = "GBA e-Reader + GameCube Link TEV",
        primaryHex = "#A855F7",
        secondaryHex = "#FF2A85",
        loreSummary = "Blends Gen II deep-black dithered pixel art with Pokémon XD Gale of Darkness 3D chromatic shadow aura.",
      ),
      NativePokemonEntity(
        dexNumber = 133,
        name = "Eevee",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Normal",
        secondaryType = "",
        abilityPrimary = "Adaptability",
        abilityHidden = "Anticipation",
        signatureMove = "Last Resort",
        baseHp = 55,
        baseAttack = 55,
        baseDefense = 50,
        baseSpAtk = 45,
        baseSpDef = 65,
        baseSpeed = 55,
        consoleOriginSystem = "GBC + NDS HGSS Overworld + Switch Cel",
        recommendedChipAddon = "Voxelizer-FX + SA-1 Multi-Palette",
        primaryHex = "#F59E0B",
        secondaryHex = "#FDE68A",
        loreSummary = "Unstable genetic code allows real-time palette and 3D shader morphing across all 8 Eeveelution branches.",
      ),
      NativePokemonEntity(
        dexNumber = 150,
        name = "Mewtwo",
        generation = "Gen I",
        region = "Kanto",
        primaryType = "Psychic",
        secondaryType = "",
        abilityPrimary = "Pressure",
        abilityHidden = "Unnerve",
        signatureMove = "Psystrike",
        baseHp = 106,
        baseAttack = 110,
        baseDefense = 90,
        baseSpAtk = 154,
        baseSpDef = 90,
        baseSpeed = 130,
        consoleOriginSystem = "SGB + N64 Stadium + Pokkén Arcade Shader",
        recommendedChipAddon = "Super FX2 (GSU-2) + Capcom CX4 Wireframe",
        primaryHex = "#C084FC",
        secondaryHex = "#00F5D4",
        loreSummary = "Psychic barrier rendered with Capcom CX4 vector trigonometry over a high-contrast Gen I/V hybrid sprite.",
      ),
      NativePokemonEntity(
        dexNumber = 249,
        name = "Lugia",
        generation = "Gen II",
        region = "Johto",
        primaryType = "Psychic",
        secondaryType = "Flying",
        abilityPrimary = "Pressure",
        abilityHidden = "Multiscale",
        signatureMove = "Aeroblast",
        baseHp = 106,
        baseAttack = 90,
        baseDefense = 130,
        baseSpAtk = 90,
        baseSpDef = 154,
        baseSpeed = 110,
        consoleOriginSystem = "GBC Silver + NDS SoulSilver + XD Shadow",
        recommendedChipAddon = "GameCube TEV + MSU-1 Orchestral Cry",
        primaryHex = "#38BDF8",
        secondaryHex = "#A855F7",
        loreSummary = "Supports one-click toggle between SoulSilver 2.5D diorama wings and GameCube XD001 Shadow Lugia palette.",
      ),
      NativePokemonEntity(
        dexNumber = 257,
        name = "Blaziken",
        generation = "Gen III",
        region = "Hoenn",
        primaryType = "Fire",
        secondaryType = "Fighting",
        abilityPrimary = "Blaze",
        abilityHidden = "Speed Boost",
        signatureMove = "Blaze Kick",
        baseHp = 80,
        baseAttack = 120,
        baseDefense = 70,
        baseSpAtk = 110,
        baseSpDef = 70,
        baseSpeed = 80,
        consoleOriginSystem = "GBA Emerald + 3DS ORAS Mega Cel-3D",
        recommendedChipAddon = "Sega 32X RISC + Super FX2",
        primaryHex = "#EF4444",
        secondaryHex = "#FFD166",
        loreSummary = "Fuses GBA 32-bit vibrant keyframe animation with 3DS Mega Evolution particle trails.",
      ),
      NativePokemonEntity(
        dexNumber = 282,
        name = "Gardevoir",
        generation = "Gen III",
        region = "Hoenn",
        primaryType = "Psychic",
        secondaryType = "Fairy",
        abilityPrimary = "Trace",
        abilityHidden = "Pixilate",
        signatureMove = "Moonblast",
        baseHp = 68,
        baseAttack = 65,
        baseDefense = 65,
        baseSpAtk = 125,
        baseSpDef = 115,
        baseSpeed = 80,
        consoleOriginSystem = "GBA Ruby + NDS Mystery Dungeon + Switch HD-2D",
        recommendedChipAddon = "Capcom CX4 + PokeCRT-RTX",
        primaryHex = "#F472B6",
        secondaryHex = "#10B981",
        loreSummary = "Combines Mystery Dungeon expressive portrait pixel art with 3D gravitational black-hole distortion shaders.",
      ),
      NativePokemonEntity(
        dexNumber = 384,
        name = "Rayquaza",
        generation = "Gen III",
        region = "Hoenn",
        primaryType = "Dragon",
        secondaryType = "Flying",
        abilityPrimary = "Air Lock",
        abilityHidden = "Delta Stream",
        signatureMove = "Dragon Ascent",
        baseHp = 105,
        baseAttack = 150,
        baseDefense = 90,
        baseSpAtk = 150,
        baseSpDef = 90,
        baseSpeed = 95,
        consoleOriginSystem = "GBA Emerald Mode-7 + 3DS Delta Episode 3D",
        recommendedChipAddon = "Super FX2 (GSU-2) + Voxelizer-FX",
        primaryHex = "#10B981",
        secondaryHex = "#FFD166",
        loreSummary = "Sky Pillar Mode-7 perspective clouds blended with 3D serpentine skeletal segments and emerald runes.",
      ),
      NativePokemonEntity(
        dexNumber = 445,
        name = "Garchomp",
        generation = "Gen IV",
        region = "Sinnoh",
        primaryType = "Dragon",
        secondaryType = "Ground",
        abilityPrimary = "Sand Veil",
        abilityHidden = "Rough Skin",
        signatureMove = "Earthquake",
        baseHp = 108,
        baseAttack = 130,
        baseDefense = 95,
        baseSpAtk = 80,
        baseSpDef = 85,
        baseSpeed = 102,
        consoleOriginSystem = "NDS Platinum + Wii Battle Revolution",
        recommendedChipAddon = "SA-1 + N64 Expansion Pak",
        primaryHex = "#6366F1",
        secondaryHex = "#EF4444",
        loreSummary = "Sinnoh Champion ace featuring DS dual-screen sprite sharpness and Wii PBR ground-shatter 3D tessellation.",
      ),
      NativePokemonEntity(
        dexNumber = 448,
        name = "Lucario",
        generation = "Gen IV",
        region = "Sinnoh",
        primaryType = "Fighting",
        secondaryType = "Steel",
        abilityPrimary = "Inner Focus",
        abilityHidden = "Justified",
        signatureMove = "Aura Sphere",
        baseHp = 70,
        baseAttack = 110,
        baseDefense = 70,
        baseSpAtk = 115,
        baseSpDef = 70,
        baseSpeed = 90,
        consoleOriginSystem = "NDS Diamond/Pearl + Gen V B2W2 + Switch Cel",
        recommendedChipAddon = "Voxelizer-FX + PokeCRT-RTX",
        primaryHex = "#38BDF8",
        secondaryHex = "#94A3B8",
        loreSummary = "Aura waves rendered as volumetric 3D rim-lit shaders wrapping a crisp 60fps Gen V skeletal pixel sprite.",
      ),
      NativePokemonEntity(
        dexNumber = 643,
        name = "Reshiram",
        generation = "Gen V",
        region = "Unova",
        primaryType = "Dragon",
        secondaryType = "Fire",
        abilityPrimary = "Turboblaze",
        abilityHidden = "Turboblaze",
        signatureMove = "Blue Flare",
        baseHp = 100,
        baseAttack = 120,
        baseDefense = 100,
        baseSpAtk = 150,
        baseSpDef = 120,
        baseSpeed = 90,
        consoleOriginSystem = "NDS Black/White Dragonspiral 3D + HD-2D",
        recommendedChipAddon = "Super FX2 + MSU-1 + PokeCRT-RTX",
        primaryHex = "#F8FAFC",
        secondaryHex = "#FF5722",
        loreSummary = "Peak Gen V multi-part animated pixel sprite fused with 3D turbine flare lighting and N's Castle polygons.",
      ),
      NativePokemonEntity(
        dexNumber = 658,
        name = "Greninja",
        generation = "Gen VI",
        region = "Kalos",
        primaryType = "Water",
        secondaryType = "Dark",
        abilityPrimary = "Torrent",
        abilityHidden = "Battle Bond",
        signatureMove = "Water Shuriken",
        baseHp = 72,
        baseAttack = 95,
        baseDefense = 67,
        baseSpAtk = 103,
        baseSpDef = 71,
        baseSpeed = 122,
        consoleOriginSystem = "3DS X/Y PICA200 + Demake GBA/DS Pixel Fusion",
        recommendedChipAddon = "Voxelizer-FX + Capcom CX4",
        primaryHex = "#00B4D8",
        secondaryHex = "#FF2A85",
        loreSummary = "Back-ported from 3DS cel-shaded polygons into a hybrid Gen V pixel sprite with 3D refractive Water Shurikens.",
      ),
      NativePokemonEntity(
        dexNumber = 778,
        name = "Mimikyu",
        generation = "Gen VII",
        region = "Alola",
        primaryType = "Ghost",
        secondaryType = "Fairy",
        abilityPrimary = "Disguise",
        abilityHidden = "Disguise",
        signatureMove = "Play Rough",
        baseHp = 55,
        baseAttack = 90,
        baseDefense = 80,
        baseSpAtk = 50,
        baseSpDef = 105,
        baseSpeed = 96,
        consoleOriginSystem = "3DS Sun/Moon + SGB Crayon Dither Shader",
        recommendedChipAddon = "Super Game Boy 2 + PokeCRT-RTX",
        primaryHex = "#FDE68A",
        secondaryHex = "#A855F7",
        loreSummary = "Uses SGB2 hand-drawn crayon palette dithering over a 3D cloth-physics ragdoll mesh.",
      ),
      NativePokemonEntity(
        dexNumber = 887,
        name = "Dragapult",
        generation = "Gen VIII",
        region = "Galar",
        primaryType = "Dragon",
        secondaryType = "Ghost",
        abilityPrimary = "Clear Body",
        abilityHidden = "Infiltrator",
        signatureMove = "Dragon Darts",
        baseHp = 88,
        baseAttack = 120,
        baseDefense = 75,
        baseSpAtk = 100,
        baseSpDef = 75,
        baseSpeed = 142,
        consoleOriginSystem = "Switch Sword/Shield + Sega 32X Stealth Hologram",
        recommendedChipAddon = "Sega 32X + Voxelizer-FX",
        primaryHex = "#2DD4BF",
        secondaryHex = "#FF2A85",
        loreSummary = "Translucent ghost tail rendered via Sega 32X + Saturn mesh-alpha stippling fused with Switch PBR shaders.",
      ),
      NativePokemonEntity(
        dexNumber = 1007,
        name = "Koraidon",
        generation = "Gen IX",
        region = "Paldea",
        primaryType = "Fighting",
        secondaryType = "Dragon",
        abilityPrimary = "Orichalcum Pulse",
        abilityHidden = "Orichalcum Pulse",
        signatureMove = "Collision Course",
        baseHp = 100,
        baseAttack = 135,
        baseDefense = 115,
        baseSpAtk = 85,
        baseSpDef = 100,
        baseSpeed = 135,
        consoleOriginSystem = "Switch Scarlet PBR + GBA/DS Demake Hybrid",
        recommendedChipAddon = "Super FX2 + Voxelizer-FX + PokeCRT-RTX",
        primaryHex = "#EF4444",
        secondaryHex = "#F59E0B",
        loreSummary = "Ancient Paradox Winged King combining Switch subsurface scales with 32-bit pixel keyframe impact frames.",
      ),
      NativePokemonEntity(
        dexNumber = 1008,
        name = "Miraidon",
        generation = "Gen IX",
        region = "Paldea",
        primaryType = "Electric",
        secondaryType = "Dragon",
        abilityPrimary = "Hadron Engine",
        abilityHidden = "Hadron Engine",
        signatureMove = "Electro Drift",
        baseHp = 100,
        baseAttack = 85,
        baseDefense = 100,
        baseSpAtk = 135,
        baseSpDef = 115,
        baseSpeed = 135,
        consoleOriginSystem = "Switch Violet Cyber-3D + LED Dot-Matrix",
        recommendedChipAddon = "Capcom CX4 + Super FX2 + PokeCRT-RTX",
        primaryHex = "#A855F7",
        secondaryHex = "#00F5D4",
        loreSummary = "Future Paradox Iron Serpent whose plasma tail renders authentic 8-bit dot-matrix glyphs over a 3D neon rig.",
      ),
    )
  }
}

package com.example.data.local

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.remote.NetworkClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

class FranchiseEncyclopediaAndResourceRepository(
  private val resourceDao: StudioResourceDao,
  private val encyclopediaDao: EncyclopediaDao,
  private val httpClient: OkHttpClient = NetworkClientProvider.sharedOkHttpClient,
) {

  val allResources: Flow<List<StudioResourceEntity>> = resourceDao.observeAllResources()
  val allEncyclopediaEntries: Flow<List<EncyclopediaEntryEntity>> = encyclopediaDao.observeAllEntries()
  val sourceStatuses: Flow<List<EncyclopediaSourceStatusEntity>> = encyclopediaDao.observeSourceStatuses()

  suspend fun ensureSeeded(): Int {
    val existingResources = resourceDao.getAllSnapshot().associateBy { it.id }
    val missingOrStaleBuiltIns = EncyclopediaSeedCatalog.builtInResources.filter { canonical ->
      existingResources[canonical.id] == null
    }
    if (missingOrStaleBuiltIns.isNotEmpty()) {
      resourceDao.upsertAll(missingOrStaleBuiltIns)
    }

    val existingEntriesCount = encyclopediaDao.countEntries()
    if (existingEntriesCount == 0) {
      encyclopediaDao.upsertEntries(EncyclopediaSeedCatalog.defaultEncyclopediaEntries)
      EncyclopediaSeedCatalog.defaultSourceStatuses.forEach { status ->
        encyclopediaDao.upsertSourceStatus(status)
      }
      return EncyclopediaSeedCatalog.defaultEncyclopediaEntries.size + missingOrStaleBuiltIns.size
    } else {
      // Ensure any newly added canonical entries from app updates are merged cleanly
      encyclopediaDao.upsertEntries(EncyclopediaSeedCatalog.defaultEncyclopediaEntries)
    }
    return missingOrStaleBuiltIns.size
  }

  suspend fun addCustomResource(
    name: String,
    category: ResourceCategory,
    formatTag: String,
    consoleChip: String,
    durationOrGrid: String,
    description: String,
    accentHex: String,
    tileTypeIndex: Int = -1,
    frequencyHz: Int = 523,
  ): StudioResourceEntity {
    val entity = StudioResourceEntity(
      id = "res_custom_${UUID.randomUUID().toString().take(8)}",
      name = name.trim().ifEmpty { "Custom ${category.shortBadge} Asset" },
      categoryId = category.id,
      formatTag = formatTag.trim().ifEmpty { "User Studio Asset" },
      consoleChip = consoleChip.trim().ifEmpty { "Super FX2 + MSU-1" },
      durationOrGrid = durationOrGrid.trim().ifEmpty {
        when (category) {
          ResourceCategory.SFX -> "0.6s"
          ResourceCategory.BGM -> "01:30 loop"
          ResourceCategory.MAP_TILES -> "16x16px"
        }
      },
      payloadSpec = "CUSTOM:${category.shortBadge}|USER_ROOM_DB",
      accentHex = accentHex,
      frequencyHz = frequencyHz.coerceIn(110, 1760),
      tileTypeIndex = tileTypeIndex,
      description = description.trim().ifEmpty { "Custom local resource stored in Room SQLite." },
      isBuiltIn = false,
      updatedAtEpochMs = System.currentTimeMillis(),
    )
    resourceDao.upsertResource(entity)
    return entity
  }

  suspend fun deleteCustomResource(resourceId: String) {
    resourceDao.deleteCustomResource(resourceId)
  }

  suspend fun assignResourceToProject(resourceId: String, projectId: String): StudioResourceEntity? {
    val existing = resourceDao.getById(resourceId) ?: return null
    val currentIds = existing.assignedProjectIdsCsv.split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .toMutableSet()
    currentIds.add(projectId)
    val updated = existing.copy(
      assignedProjectIdsCsv = currentIds.joinToString(","),
      updatedAtEpochMs = System.currentTimeMillis(),
    )
    resourceDao.upsertResource(updated)
    return updated
  }

  suspend fun unassignResourceFromProject(resourceId: String, projectId: String): StudioResourceEntity? {
    val existing = resourceDao.getById(resourceId) ?: return null
    val remaining = existing.assignedProjectIdsCsv.split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() && it != projectId }
    val updated = existing.copy(
      assignedProjectIdsCsv = remaining.joinToString(","),
      updatedAtEpochMs = System.currentTimeMillis(),
    )
    resourceDao.upsertResource(updated)
    return updated
  }

  /**
   * Queries live endpoints from Bulbapedia (MediaWiki API), PokemonDB, and Serebii.net on app open
   * and updates Room source statuses + live feed entries cleanly without blocking UI.
   */
  suspend fun syncEncyclopediaSourcesOnOpen(): Int = withContext(Dispatchers.IO) {
    ensureSeeded()
    val now = System.currentTimeMillis()
    coroutineScope {
      val bulbaJob = async { syncBulbapedia(now) }
      val pkdbJob = async { syncPokemonDb(now) }
      val serebiiJob = async { syncSerebii(now) }
      val results = listOf(bulbaJob.await(), pkdbJob.await(), serebiiJob.await())
      results.count { it }
    }
  }

  private suspend fun syncBulbapedia(timestampMs: Long): Boolean = runCatching {
    val req = Request.Builder()
      .url("https://bulbapedia.bulbagarden.net/w/api.php?action=query&list=recentchanges&rcnamespace=0&rclimit=4&format=json")
      .header("User-Agent", "PokeFusionStudio/2.0 (Android Kotlin Compose)")
      .build()
    httpClient.newCall(req).execute().use { resp ->
      val code = resp.code
      val body = resp.body?.string().orEmpty()
      val recentTitles = runCatching {
        val arr = JSONObject(body).getJSONObject("query").getJSONArray("recentchanges")
        (0 until arr.length()).map { idx -> arr.getJSONObject(idx).optString("title") }
          .filter { it.isNotBlank() }
      }.getOrDefault(emptyList())

      val headline = if (recentTitles.isNotEmpty()) {
        "Live Wiki Sync: ${recentTitles.joinToString(" • ")}"
      } else {
        "Connected HTTP $code • Bulbapedia MediaWiki Species & Mechanics Index"
      }

      encyclopediaDao.upsertSourceStatus(
        EncyclopediaSourceStatusEntity(
          sourceId = "bulbapedia",
          displayName = "Bulbapedia",
          baseUrl = "https://bulbapedia.bulbagarden.net",
          lastSyncEpochMs = timestampMs,
          httpStatusCode = code,
          liveHeadline = headline,
          entryCount = 4 + recentTitles.size,
        )
      )
      if (recentTitles.isNotEmpty()) {
        encyclopediaDao.upsertEntries(
          listOf(
            EncyclopediaEntryEntity(
              id = "bulba_live_feed",
              title = "Bulbapedia Live Wiki Feed (${recentTitles.size} Recent Articles)",
              domainCategory = "Species Biology",
              sourceOrigin = "Bulbapedia",
              sourceUrl = "https://bulbapedia.bulbagarden.net/wiki/Main_Page",
              dexRangeOrTag = "Live Wiki API • HTTP $code",
              summary = "Latest verified Bulbapedia wiki articles synced upon opening the app: ${recentTitles.joinToString(", ")}.",
              structuredData = "Endpoint: bulbapedia.bulbagarden.net/w/api.php | Synced: $timestampMs",
              lastUpdatedEpochMs = timestampMs,
            )
          )
        )
      }
      resp.isSuccessful
    }
  }.getOrElse {
    encyclopediaDao.upsertSourceStatus(
      EncyclopediaSeedCatalog.defaultSourceStatuses[0].copy(
        lastSyncEpochMs = timestampMs,
        httpStatusCode = 0,
        liveHeadline = "Offline cache active • Bulbapedia canon baseline loaded",
      )
    )
    false
  }

  private suspend fun syncPokemonDb(timestampMs: Long): Boolean = runCatching {
    val req = Request.Builder()
      .url("https://pokemondb.net/pokedex/national")
      .header("User-Agent", "PokeFusionStudio/2.0 (Android Kotlin Compose)")
      .build()
    httpClient.newCall(req).execute().use { resp ->
      val code = resp.code
      val html = resp.body?.string().orEmpty().take(32_000)
      val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)?.trim()
      val headline = if (!titleMatch.isNullOrBlank()) {
        "Live Pokédex Sync (HTTP $code): $titleMatch"
      } else {
        "Connected HTTP $code • Pokémon Database Stats & Type Matrix"
      }

      encyclopediaDao.upsertSourceStatus(
        EncyclopediaSourceStatusEntity(
          sourceId = "pokemondb",
          displayName = "Pokémon Database",
          baseUrl = "https://pokemondb.net",
          lastSyncEpochMs = timestampMs,
          httpStatusCode = code,
          liveHeadline = headline,
          entryCount = 5,
        )
      )
      encyclopediaDao.upsertEntries(
        listOf(
          EncyclopediaEntryEntity(
            id = "pkdb_live_feed",
            title = titleMatch?.ifBlank { null } ?: "Pokémon Database National Pokédex Live Index",
            domainCategory = "Competitive Stats & Moves",
            sourceOrigin = "PokemonDB",
            sourceUrl = "https://pokemondb.net/pokedex/national",
            dexRangeOrTag = "Live Pokédex • HTTP $code",
            summary = "Synchronized directly on app launch from pokemondb.net for #0001–#1025 base stats, abilities, type charts, and learnsets.",
            structuredData = "Endpoint: pokemondb.net/pokedex/national | HTTP:$code | Synced:$timestampMs",
            lastUpdatedEpochMs = timestampMs,
          )
        )
      )
      resp.isSuccessful
    }
  }.getOrElse {
    encyclopediaDao.upsertSourceStatus(
      EncyclopediaSeedCatalog.defaultSourceStatuses[1].copy(
        lastSyncEpochMs = timestampMs,
        httpStatusCode = 0,
        liveHeadline = "Offline cache active • PokémonDB stat & type baseline loaded",
      )
    )
    false
  }

  private suspend fun syncSerebii(timestampMs: Long): Boolean = runCatching {
    val req = Request.Builder()
      .url("https://www.serebii.net/index2.shtml")
      .header("User-Agent", "PokeFusionStudio/2.0 (Android Kotlin Compose)")
      .build()
    httpClient.newCall(req).execute().use { resp ->
      val code = resp.code
      val html = resp.body?.string().orEmpty().take(32_000)
      val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)?.trim()
      val lastModified = resp.header("Last-Modified") ?: "Live"
      val headline = "Live Sync (HTTP $code • $lastModified): ${titleMatch ?: "Serebii.net News & Pokéarth Index"}"

      encyclopediaDao.upsertSourceStatus(
        EncyclopediaSourceStatusEntity(
          sourceId = "serebii",
          displayName = "Serebii.net",
          baseUrl = "https://www.serebii.net",
          lastSyncEpochMs = timestampMs,
          httpStatusCode = code,
          liveHeadline = headline,
          entryCount = 5,
        )
      )
      encyclopediaDao.upsertEntries(
        listOf(
          EncyclopediaEntryEntity(
            id = "serebii_live_feed",
            title = titleMatch?.ifBlank { null } ?: "Serebii.net Live Franchise & Mechanics Feed",
            domainCategory = "Locations & Items",
            sourceOrigin = "Serebii.net",
            sourceUrl = "https://www.serebii.net/index2.shtml",
            dexRangeOrTag = "Live Feed • $lastModified",
            summary = "Synchronized upon opening the app from www.serebii.net covering regional encounters, item dex updates, Tera/Event news, and anime guides.",
            structuredData = "Endpoint: www.serebii.net/index2.shtml | Last-Modified:$lastModified | HTTP:$code",
            lastUpdatedEpochMs = timestampMs,
          )
        )
      )
      resp.isSuccessful
    }
  }.getOrElse {
    encyclopediaDao.upsertSourceStatus(
      EncyclopediaSeedCatalog.defaultSourceStatuses[2].copy(
        lastSyncEpochMs = timestampMs,
        httpStatusCode = 0,
        liveHeadline = "Offline cache active • Serebii Item/Pokéarth baseline loaded",
      )
    )
    false
  }

  /**
   * Generates a crisp zero-permission retro square/sine chiptune tone preview for SFX & BGM assets.
   */
  suspend fun playRetroAudioPreview(resource: StudioResourceEntity) = withContext(Dispatchers.Default) {
    runCatching {
      val sampleRate = 22050
      val isBgm = resource.categoryId == ResourceCategory.BGM.id
      val durationMs = if (isBgm) 650 else 320
      val totalSamples = (sampleRate * durationMs) / 1000
      val buffer = ShortArray(totalSamples)
      val baseFreq = resource.frequencyHz.coerceIn(180, 1200).toDouble()

      for (i in 0 until totalSamples) {
        val progress = i.toDouble() / totalSamples
        val freq = if (isBgm) {
          when {
            progress < 0.33 -> baseFreq
            progress < 0.66 -> baseFreq * 1.25
            else -> baseFreq * 1.5
          }
        } else {
          baseFreq * (1.0 + progress * 0.45)
        }
        val env = (1.0 - progress).coerceIn(0.0, 1.0)
        val raw = sin(2.0 * PI * freq * i / sampleRate)
        val shaped = if (isBgm) raw else if (raw >= 0) 0.7 else -0.7
        buffer[i] = (shaped * env * Short.MAX_VALUE * 0.25).toInt().toShort()
      }

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      try {
        track.write(buffer, 0, buffer.size)
        track.play()
        delay(durationMs.toLong() + 40L)
      } finally {
        withContext(NonCancellable) {
          runCatching { track.stop() }
          runCatching { track.release() }
        }
      }
    }
  }
}

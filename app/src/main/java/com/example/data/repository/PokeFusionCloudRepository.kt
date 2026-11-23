package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PokeFusionCloudRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw FirebaseFirestoreException(
        "User must be signed in with Google before accessing Firestore.",
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
      )
  }

  fun observeFanGames(userId: String = requireUserId()): Flow<List<FanGameProject>> = flow {
    val path = "users/$userId/fan_games"
    emitAll(
      db.collection("users").document(userId).collection("fan_games")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            FanGameProject::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ).sortedByDescending { project ->
            project.updatedAt?.seconds ?: project.createdAt?.seconds ?: 0L
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeFanGameById(
    userId: String = requireUserId(),
    gameId: String,
  ): Flow<FanGameProject?> = flow {
    val path = "users/$userId/fan_games/$gameId"
    emitAll(
      db.collection("users").document(userId).collection("fan_games").document(gameId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              FanGameProject::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  suspend fun createFanGame(project: FanGameProject): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("fan_games").document()
    val payload = mapOf(
      "userId" to uid,
      "title" to project.title.trim().take(80).ifEmpty { "Untitled Fan Game" },
      "regionName" to project.regionName.trim().take(60).ifEmpty { "Kanto-Neo" },
      "synopsis" to project.synopsis.trim().take(500).ifEmpty { "A multi-console fusion adventure." },
      "consoleMatrixPreset" to project.consoleMatrixPreset.trim().take(80).ifEmpty { "Omni-Era HD-2D" },
      "activeChipsCsv" to project.activeChipsCsv.trim().take(300),
      "pixel3dBlendRatio" to project.pixel3dBlendRatio.coerceIn(0L, 100L),
      "dexCount" to project.dexCount.coerceIn(0L, 2000L),
      "mapCount" to project.mapCount.coerceIn(0L, 500L),
      "versionTag" to project.versionTag.trim().take(30).ifEmpty { "v1.0.0" },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateFanGame(project: FanGameProject): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(project.id.isNotBlank()) { "Project ID must not be blank" }
    val docRef = db.collection("users").document(uid).collection("fan_games").document(project.id)
    val updates = mapOf(
      "title" to project.title.trim().take(80).ifEmpty { "Untitled Fan Game" },
      "regionName" to project.regionName.trim().take(60).ifEmpty { "Kanto-Neo" },
      "synopsis" to project.synopsis.trim().take(500).ifEmpty { "A multi-console fusion adventure." },
      "consoleMatrixPreset" to project.consoleMatrixPreset.trim().take(80).ifEmpty { "Omni-Era HD-2D" },
      "activeChipsCsv" to project.activeChipsCsv.trim().take(300),
      "pixel3dBlendRatio" to project.pixel3dBlendRatio.coerceIn(0L, 100L),
      "dexCount" to project.dexCount.coerceIn(0L, 2000L),
      "mapCount" to project.mapCount.coerceIn(0L, 500L),
      "versionTag" to project.versionTag.trim().take(30).ifEmpty { "v1.0.0" },
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun getFanGameById(ownerUid: String, gameId: String): Result<FanGameProject?> = runCatching {
    val docRef = db.collection("users").document(ownerUid).collection("fan_games").document(gameId)
    try {
      docRef.get().await().toObject(
        FanGameProject::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun deleteFanGame(gameId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("fan_games").document(gameId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeCustomDex(userId: String = requireUserId()): Flow<List<CustomDexEntry>> = flow {
    val path = "users/$userId/custom_dex"
    emitAll(
      db.collection("users").document(userId).collection("custom_dex")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CustomDexEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createCustomDexEntry(entry: CustomDexEntry): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("custom_dex").document()
    val payload = mapOf(
      "userId" to uid,
      "dexNumber" to entry.dexNumber.coerceIn(1L, 9999L),
      "name" to entry.name.trim().take(60).ifEmpty { "MissingNo-FX" },
      "category" to entry.category.trim().take(60).ifEmpty { "Fusion Species" },
      "primaryType" to entry.primaryType.trim().take(30).ifEmpty { "Electric" },
      "secondaryType" to entry.secondaryType.trim().take(30),
      "abilityName" to entry.abilityName.trim().take(60).ifEmpty { "Super FX Core" },
      "signatureMove" to entry.signatureMove.trim().take(60).ifEmpty { "Mode-7 Burst" },
      "loreDescription" to entry.loreDescription.trim().take(500).ifEmpty { "Synthesized across console generations." },
      "baseHp" to entry.baseHp.coerceIn(1L, 255L),
      "baseAttack" to entry.baseAttack.coerceIn(1L, 255L),
      "baseDefense" to entry.baseDefense.coerceIn(1L, 255L),
      "baseSpAtk" to entry.baseSpAtk.coerceIn(1L, 255L),
      "baseSpDef" to entry.baseSpDef.coerceIn(1L, 255L),
      "baseSpeed" to entry.baseSpeed.coerceIn(1L, 255L),
      "baseSpriteId" to entry.baseSpriteId.coerceIn(1L, 1025L),
      "fusionStylePreset" to entry.fusionStylePreset.trim().take(80).ifEmpty { "Omni-Console Hybrid" },
      "enhancementChip" to entry.enhancementChip.trim().take(60).ifEmpty { "Super FX2 (GSU-2)" },
      "primaryHex" to entry.primaryHex.trim().take(9).ifEmpty { "#00F5D4" },
      "secondaryHex" to entry.secondaryHex.trim().take(9).ifEmpty { "#FF2A85" },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteCustomDexEntry(entryId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("custom_dex").document(entryId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeCustomAssets(userId: String = requireUserId()): Flow<List<CustomStudioAsset>> = flow {
    val path = "users/$userId/custom_assets"
    emitAll(
      db.collection("users").document(userId).collection("custom_assets")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CustomStudioAsset::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createCustomAsset(asset: CustomStudioAsset): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("custom_assets").document()
    val payload = mapOf(
      "userId" to uid,
      "name" to asset.name.trim().take(80).ifEmpty { "Fused Asset" },
      "category" to asset.category.trim().take(40).ifEmpty { "Sprites" },
      "consoleLineage" to asset.consoleLineage.trim().take(120).ifEmpty { "GBC + GBA + Switch HD-2D" },
      "chipAddon" to asset.chipAddon.trim().take(80).ifEmpty { "Super FX2 + Voxelizer-FX" },
      "spriteSeedId" to asset.spriteSeedId.coerceIn(1L, 1025L),
      "shaderFormula" to asset.shaderFormula.trim().take(200).ifEmpty { "BLEND:65|VOXEL:72" },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteCustomAsset(assetId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("custom_assets").document(assetId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  /**
   * Forces a server reconciliation read across user project collections upon app load/resume,
   * ensuring the active Firestore snapshot cache reflects the newest remote state.
   */
  suspend fun refreshFromServer(userId: String = requireUserId()): Result<Int> = runCatching {
    val userDoc = db.collection("users").document(userId)
    val gamesSnap = userDoc.collection("fan_games").get(Source.SERVER).await()
    val dexSnap = userDoc.collection("custom_dex").get(Source.SERVER).await()
    val assetsSnap = userDoc.collection("custom_assets").get(Source.SERVER).await()
    gamesSnap.size() + dexSnap.size() + assetsSnap.size()
  }
}

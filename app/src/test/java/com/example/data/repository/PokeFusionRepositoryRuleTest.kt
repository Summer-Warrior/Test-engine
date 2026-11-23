package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CustomDexEntry
import com.example.data.model.CustomStudioAsset
import com.example.data.model.FanGameProject
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PokeFusionRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveFanGame_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = PokeFusionCloudRepository(firestore, auth)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createFanGame(
        FanGameProject(
          title = "Pokemon Omni-Crystal",
          regionName = "Aethelgard",
          synopsis = "Fusing 8-bit, 16-bit Super FX2, GBA, DS, and Switch HD-2D shaders.",
          pixel3dBlendRatio = 72L,
        )
      )
    }
    assertTrue(createResult.isSuccess)
    val gameId = createResult.getOrThrow()
    assertTrue(gameId.isNotBlank())

    val emittedGames = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeFanGames(aliceUid).first { list -> list.any { it.id == gameId } }
    }
    assertTrue(emittedGames.any { it.id == gameId && it.title == "Pokemon Omni-Crystal" })
  }

  @Test
  fun createCustomDexAndAsset_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = PokeFusionCloudRepository(firestore, auth)

    val dexId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createCustomDexEntry(
        CustomDexEntry(
          dexNumber = 1026L,
          name = "Chromawyrm",
          primaryType = "Dragon",
          secondaryType = "Steel",
          loreDescription = "Rendered simultaneously in GBC Hi-Color and Super FX2 3D polygons.",
        )
      ).getOrThrow()
    }
    assertTrue(dexId.isNotBlank())

    val assetId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createCustomAsset(
        CustomStudioAsset(
          name = "SGB2 + 32X Voxel Gym Tile",
          category = "Tilesets",
        )
      ).getOrThrow()
    }
    assertTrue(assetId.isNotBlank())

    val dexList = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeCustomDex(aliceUid).first { list -> list.any { it.id == dexId } }
    }
    assertTrue(dexList.any { it.name == "Chromawyrm" })
  }

  @Test
  fun crossUserReadFanGame_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = PokeFusionCloudRepository(firestore, auth)
    val gameId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createFanGame(
        FanGameProject(
          title = "Alice Secret Fan Game",
          regionName = "Johto-X",
          synopsis = "Private cloud development build.",
        )
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = PokeFusionCloudRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getFanGameById(aliceUid, gameId)
    }
    assertTrue(result.isFailure)
    val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun observeFanGames_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = PokeFusionCloudRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeFanGames(aliceUid).first()
      }
      fail("Expected exception for unauthenticated flow observation")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@pokefusion.test"
    const val BOB_EMAIL = "bob@pokefusion.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}

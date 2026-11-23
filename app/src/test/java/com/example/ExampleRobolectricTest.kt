package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EncyclopediaSeedCatalog
import com.example.data.local.NativeFranchiseCatalogRepository
import com.example.data.local.ResourceCategory
import com.example.data.model.ConsoleHardwareCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name and verify integrated franchise and console catalog`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PokéFusion Studio", appName)
    assertTrue(NativeFranchiseCatalogRepository.builtInFranchiseDex.size >= 20)
    assertTrue(ConsoleHardwareCatalog.officialAndFanChips.any { it.isFanMade })
    assertTrue(ConsoleHardwareCatalog.officialAndFanChips.any { !it.isFanMade })
  }

  @Test
  fun `verify default encyclopedia seed covers Bulbapedia PokemonDB and Serebii`() {
    val entries = EncyclopediaSeedCatalog.defaultEncyclopediaEntries
    val statuses = EncyclopediaSeedCatalog.defaultSourceStatuses
    assertTrue(entries.size >= 10)
    assertEquals(3, statuses.size)
    assertTrue(entries.any { it.sourceOrigin.contains("Bulbapedia", ignoreCase = true) })
    assertTrue(entries.any { it.sourceOrigin.contains("PokemonDB", ignoreCase = true) })
    assertTrue(entries.any { it.sourceOrigin.contains("Serebii", ignoreCase = true) })
  }

  @Test
  fun `verify Room resource manager seed categories and project attachment helper`() {
    val resources = EncyclopediaSeedCatalog.builtInResources
    assertTrue(resources.any { it.categoryId == ResourceCategory.SFX.id })
    assertTrue(resources.any { it.categoryId == ResourceCategory.BGM.id })
    assertTrue(resources.any { it.categoryId == ResourceCategory.MAP_TILES.id })

    val sample = resources.first().copy(assignedProjectIdsCsv = "proj_alpha,proj_beta")
    assertTrue(sample.isAssignedToProject("proj_alpha"))
    assertTrue(sample.isAssignedToProject("proj_beta"))
    assertFalse(sample.isAssignedToProject("proj_gamma"))
  }
}

package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonEngineVerificationTest {

  @Test
  fun `18-type matrix enforces immunities 4x dual weaknesses and special overrides`() {
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.GHOST, PokemonType.NORMAL))
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ELECTRIC, PokemonType.GROUND))
    assertEquals(0.0f, CanonTypeChart.calculateEffectiveness(PokemonType.DRAGON, PokemonType.FAIRY))
    assertEquals(4.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ROCK, PokemonType.FIRE, PokemonType.FLYING))
    assertEquals(2.0f, CanonTypeChart.calculateEffectiveness(PokemonType.ICE, PokemonType.WATER, isFreezeDry = true))
    assertEquals(
      1.0f,
      CanonTypeChart.calculateEffectiveness(
        moveType = PokemonType.ROCK,
        defenderPrimary = PokemonType.FLYING,
        weather = WeatherCondition.STRONG_WINDS,
      ),
    )
    assertEquals(
      2.0f,
      CanonTypeChart.calculateEffectiveness(
        moveType = PokemonType.WATER,
        defenderPrimary = PokemonType.GRASS,
        defenderTeraType = PokemonType.FIRE,
      ),
    )
  }

  @Test
  fun `stat spreads clamp EVs to 510 total and enforce Shedinja 1 HP invariant`() {
    val overflow = StatSpread(
      ivHp = 99,
      evHp = 252,
      evAtk = 252,
      evDef = 252,
      evSpe = 252,
    ).normalized()
    assertEquals(31, overflow.ivHp)
    assertEquals(510, overflow.totalEvs)
    assertEquals(6, overflow.evDef)

    val shedinja = CombatantSnapshot(
      dexNumber = 292,
      name = "Shedinja",
      level = 100,
      primaryType = PokemonType.BUG,
      secondaryType = PokemonType.GHOST,
      baseHp = 1,
      baseAtk = 90,
      baseDef = 45,
      baseSpA = 30,
      baseSpD = 30,
      baseSpe = 40,
      abilityName = "Wonder Guard",
    )
    assertEquals(1, shedinja.maxHp())
  }

  @Test
  fun `battle resolver handles anime mechanics terrain blocks and extreme stages without NaN or div-by-zero`() {
    val attacker = CombatantSnapshot(
      dexNumber = 658,
      name = "Ash-Greninja",
      level = 50,
      primaryType = PokemonType.WATER,
      secondaryType = PokemonType.DARK,
      baseHp = 72,
      baseAtk = 145,
      baseDef = 67,
      baseSpA = 153,
      baseSpD = 71,
      baseSpe = 132,
      abilityName = "Battle Bond",
      stages = StatStages(atk = -99, spA = 99),
      gimmickMode = BattleGimmickMode.ANIME_BATTLE_BOND,
    )
    val defender = CombatantSnapshot(
      dexNumber = 6,
      name = "Charizard",
      level = 50,
      primaryType = PokemonType.FIRE,
      secondaryType = PokemonType.FLYING,
      baseHp = 78,
      baseAtk = 84,
      baseDef = 78,
      baseSpA = 109,
      baseSpD = 85,
      baseSpe = 100,
      abilityName = "Blaze",
      gimmickMode = BattleGimmickMode.ANIME_COUNTER_SHIELD,
    )

    val waterShuriken = MasterCanonTemplateCatalog.signatureMovePool[1]
    val normalFieldResult = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = defender,
      move = waterShuriken,
      field = BattleTurnField(weather = WeatherCondition.RAIN),
    )
    assertTrue(normalFieldResult.minDamage >= 3)
    assertTrue(normalFieldResult.maxDamage >= normalFieldResult.minDamage)
    assertFalse(normalFieldResult.minPercentHp.isNaN())
    assertTrue(normalFieldResult.attackerActsFirst)

    // Priority move blocked on Psychic Terrain against grounded target
    val groundedDefender = defender.copy(secondaryType = null)
    val terrainBlocked = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = groundedDefender,
      move = waterShuriken,
      field = BattleTurnField(terrain = TerrainCondition.PSYCHIC_TERRAIN),
    )
    assertTrue(terrainBlocked.isBlockedByTerrain)
    assertEquals(0, terrainBlocked.maxDamage)

    // Contact move triggers Anime Counter-Shield chip recoil
    val contactMove = MasterCanonTemplateCatalog.signatureMovePool[2]
    val contactResult = CanonBattleResolver.resolveTurn(
      attacker = attacker,
      defender = defender,
      move = contactMove,
      field = BattleTurnField(),
    )
    assertTrue(contactResult.counterShieldChipRecoil > 0)
  }

  @Test
  fun `omni-region campaign prevents sequence-breaking and integrity audit passes`() {
    val start = CampaignProgressionState()
    val sinnoh = MasterCanonTemplateCatalog.worldChapters.first { it.id == "sinnoh_coronet" }
    val kanto = MasterCanonTemplateCatalog.worldChapters.first { it.id == "kanto_indigo" }

    val blockedSinnoh = start.completeChapter(sinnoh)
    assertTrue(blockedSinnoh.completedChapterIds.isEmpty())

    val afterKanto = start.completeChapter(kanto)
    assertTrue("kanto_indigo" in afterKanto.completedChapterIds)
    assertTrue(afterKanto.worldCoronationRank < start.worldCoronationRank)
    assertTrue(afterKanto.validateInvariants().isEmpty())

    val audit = MasterCanonTemplateCatalog.runEngineIntegrityAudit()
    assertTrue(audit.isNotEmpty())
    assertTrue(audit.all { it.second })
  }
}

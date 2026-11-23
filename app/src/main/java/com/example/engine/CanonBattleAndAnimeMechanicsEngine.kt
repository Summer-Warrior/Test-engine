package com.example.engine

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Mutually-exclusive battle gimmick & resonance modes.
 * Enforcing one active transformation per trainer per battle eliminates state corruption glitches.
 */
enum class BattleGimmickMode(val displayName: String, val sourceOrigin: String) {
  NONE("Standard Form", "Core Gen I–IX Baseline"),
  MEGA_EVOLUTION("Mega Evolution (+100 BST)", "XY / ORAS Resonance"),
  Z_MOVE("Z-Power Burst (1.75x Signature)", "Alola Z-Crystal Tech"),
  DYNAMAX("Dynamax (2.0x HP + Max Move)", "Galar Stadium Resonance"),
  TERASTALLIZATION("Terastallization (Adaptive Tera)", "Paldea Prism Core"),
  ANIME_BATTLE_BOND("Battle Bond (Aura Resonance)", "Synchronized Bond Form"),
  ANIME_COUNTER_SHIELD("Counter-Shield Spin", "Defensive Spin Technique"),
}

enum class NatureModifier(val label: String, val boostedStat: String, val loweredStat: String) {
  ADAMANT("Adamant", "ATK", "SPA"),
  MODEST("Modest", "SPA", "ATK"),
  JOLLY("Jolly", "SPE", "SPA"),
  TIMID("Timid", "SPE", "ATK"),
  BOLD("Bold", "DEF", "ATK"),
  CAREFUL("Careful", "SPD", "SPA"),
  HARDY("Hardy (Neutral)", "", ""),
  ;

  fun multiplierFor(statCode: String): Float = when {
    boostedStat.isEmpty() -> 1.0f
    boostedStat == statCode -> 1.1f
    loweredStat == statCode -> 0.9f
    else -> 1.0f
  }
}

data class StatSpread(
  val ivHp: Int = 31,
  val ivAtk: Int = 31,
  val ivDef: Int = 31,
  val ivSpA: Int = 31,
  val ivSpD: Int = 31,
  val ivSpe: Int = 31,
  val evHp: Int = 4,
  val evAtk: Int = 252,
  val evDef: Int = 0,
  val evSpA: Int = 0,
  val evSpD: Int = 0,
  val evSpe: Int = 252,
) {
  fun normalized(): StatSpread {
    val cIvHp = ivHp.coerceIn(0, 31)
    val cIvAtk = ivAtk.coerceIn(0, 31)
    val cIvDef = ivDef.coerceIn(0, 31)
    val cIvSpA = ivSpA.coerceIn(0, 31)
    val cIvSpD = ivSpD.coerceIn(0, 31)
    val cIvSpe = ivSpe.coerceIn(0, 31)

    var budget = 510
    fun takeEv(raw: Int): Int {
      val clamped = raw.coerceIn(0, min(252, budget))
      budget -= clamped
      return clamped
    }

    val nHp = takeEv(evHp)
    val nAtk = takeEv(evAtk)
    val nDef = takeEv(evDef)
    val nSpA = takeEv(evSpA)
    val nSpD = takeEv(evSpD)
    val nSpe = takeEv(evSpe)

    return StatSpread(
      ivHp = cIvHp, ivAtk = cIvAtk, ivDef = cIvDef,
      ivSpA = cIvSpA, ivSpD = cIvSpD, ivSpe = cIvSpe,
      evHp = nHp, evAtk = nAtk, evDef = nDef,
      evSpA = nSpA, evSpD = nSpD, evSpe = nSpe,
    )
  }

  val totalEvs: Int
    get() = evHp + evAtk + evDef + evSpA + evSpD + evSpe
}

data class StatStages(
  val atk: Int = 0,
  val def: Int = 0,
  val spA: Int = 0,
  val spD: Int = 0,
  val spe: Int = 0,
  val accuracy: Int = 0,
  val evasion: Int = 0,
) {
  fun clamped(): StatStages = StatStages(
    atk = atk.coerceIn(-6, 6),
    def = def.coerceIn(-6, 6),
    spA = spA.coerceIn(-6, 6),
    spD = spD.coerceIn(-6, 6),
    spe = spe.coerceIn(-6, 6),
    accuracy = accuracy.coerceIn(-6, 6),
    evasion = evasion.coerceIn(-6, 6),
  )

  companion object {
    fun stageRatio(stage: Int): Float {
      val s = stage.coerceIn(-6, 6)
      return if (s >= 0) (2f + s) / 2f else 2f / (2f - s)
    }
  }
}

data class BattleMoveSpec(
  val name: String,
  val type: PokemonType,
  val category: MoveCategory,
  val basePower: Int,
  val accuracy: Int = 100,
  val priority: Int = 0,
  val isContact: Boolean = false,
  val isSoundBased: Boolean = false,
  val highCritRatio: Boolean = false,
  val isFreezeDry: Boolean = false,
  val isFlyingPress: Boolean = false,
  val isTeraBlast: Boolean = false,
  val hitsMin: Int = 1,
  val hitsMax: Int = 1,
)

data class CombatantSnapshot(
  val dexNumber: Int,
  val name: String,
  val level: Int = 50,
  val primaryType: PokemonType,
  val secondaryType: PokemonType? = null,
  val baseHp: Int,
  val baseAtk: Int,
  val baseDef: Int,
  val baseSpA: Int,
  val baseSpD: Int,
  val baseSpe: Int,
  val abilityName: String,
  val nature: NatureModifier = NatureModifier.HARDY,
  val spread: StatSpread = StatSpread(),
  val stages: StatStages = StatStages(),
  val status: StatusCondition = StatusCondition.HEALTHY,
  val gimmickMode: BattleGimmickMode = BattleGimmickMode.NONE,
  val teraType: PokemonType? = null,
  val currentHpPercent: Int = 100,
  val bondGauge: Int = 100,
  val heldItemId: String = "",
  val isNotFullyEvolved: Boolean = false,
) {
  fun resolvedLevel(): Int = level.coerceIn(1, 100)

  fun effectiveDefensiveTypes(): List<PokemonType> =
    if (gimmickMode == BattleGimmickMode.TERASTALLIZATION && teraType != null) {
      listOf(teraType)
    } else {
      listOfNotNull(primaryType, secondaryType?.takeIf { it != primaryType })
    }

  fun isGrounded(isGravityActive: Boolean): Boolean {
    if (isGravityActive) return true
    if (effectiveDefensiveTypes().contains(PokemonType.FLYING)) return false
    if (abilityName.equals("Levitate", ignoreCase = true)) return false
    return true
  }

  fun maxHp(): Int {
    if (dexNumber == 292) return 1 // Shedinja canonical Wonder Guard 1 HP invariant
    val lvl = resolvedLevel()
    val safeBase = baseHp.coerceIn(1, 255)
    val s = spread.normalized()
    val raw = floor(((2 * safeBase + s.ivHp + floor(s.evHp / 4.0)) * lvl) / 100.0).toInt() + lvl + 10
    val dynFactor = if (gimmickMode == BattleGimmickMode.DYNAMAX) 2 else 1
    return max(1, raw * dynFactor)
  }

  fun effectiveStat(statCode: String, ignoreNegativeStage: Boolean = false, ignorePositiveStage: Boolean = false): Int {
    val lvl = resolvedLevel()
    val s = spread.normalized()
    val cStages = stages.clamped()
    val (base, iv, ev, stageVal) = when (statCode) {
      "ATK" -> Quad(baseAtk, s.ivAtk, s.evAtk, cStages.atk)
      "DEF" -> Quad(baseDef, s.ivDef, s.evDef, cStages.def)
      "SPA" -> Quad(baseSpA, s.ivSpA, s.evSpA, cStages.spA)
      "SPD" -> Quad(baseSpD, s.ivSpD, s.evSpD, cStages.spD)
      else -> Quad(baseSpe, s.ivSpe, s.evSpe, cStages.spe)
    }
    val safeBase = base.coerceIn(1, 255)
    val core = floor(((2 * safeBase + iv + floor(ev / 4.0)) * lvl) / 100.0).toInt() + 5
    val natureApplied = floor(core * nature.multiplierFor(statCode)).toInt()
    val gimmickBoost = when {
      gimmickMode == BattleGimmickMode.MEGA_EVOLUTION && (statCode == "ATK" || statCode == "SPA" || statCode == "SPE") -> 1.18f
      gimmickMode == BattleGimmickMode.ANIME_BATTLE_BOND && (statCode == "ATK" || statCode == "SPA" || statCode == "SPE") -> 1.25f
      else -> 1.0f
    }
    val itemBoost = when {
      heldItemId == "choice_band" && statCode == "ATK" -> 1.5f
      heldItemId == "choice_specs" && statCode == "SPA" -> 1.5f
      heldItemId == "choice_scarf" && statCode == "SPE" -> 1.5f
      heldItemId == "assault_vest" && statCode == "SPD" -> 1.5f
      heldItemId == "eviolite" && isNotFullyEvolved && (statCode == "DEF" || statCode == "SPD") -> 1.5f
      else -> 1.0f
    }
    val adjustedStage = when {
      ignoreNegativeStage && stageVal < 0 -> 0
      ignorePositiveStage && stageVal > 0 -> 0
      else -> stageVal
    }
    val staged = floor(natureApplied * gimmickBoost * itemBoost * StatStages.stageRatio(adjustedStage)).toInt()
    val statusAdjusted = if (statCode == "SPE" && status == StatusCondition.PARALYSIS) {
      floor(staged * 0.5).toInt()
    } else {
      staged
    }
    return max(1, statusAdjusted)
  }

  private data class Quad(val a: Int, val b: Int, val c: Int, val d: Int)
}

data class BattleTurnField(
  val weather: WeatherCondition = WeatherCondition.CLEAR,
  val terrain: TerrainCondition = TerrainCondition.NONE,
  val isTrickRoomActive: Boolean = false,
  val isTailwindAttacker: Boolean = false,
  val isTailwindDefender: Boolean = false,
  val isGravityActive: Boolean = false,
  val isCriticalHit: Boolean = false,
  val animeDodgeTimingBoost: Boolean = false,
  val speedTieSeed: Int = 0,
)

data class BattleResolutionResult(
  val minDamage: Int,
  val maxDamage: Int,
  val minPercentHp: Float,
  val maxPercentHp: Float,
  val typeEffectiveness: Float,
  val stabMultiplier: Float,
  val counterShieldChipRecoil: Int,
  val attackerActsFirst: Boolean,
  val isBlockedByTerrain: Boolean,
  val lifeOrbRecoil: Int = 0,
  val focusSashTriggered: Boolean = false,
  val weaknessPolicyTriggered: Boolean = false,
  val resolvedMoveType: PokemonType = PokemonType.NORMAL,
  val breakdownSteps: List<String> = emptyList(),
)

data class CaptureRollResult(
  val captured: Boolean,
  val shakes: Int, // 0..3
  val catchProbability: Float,
  val summary: String,
)

object CanonBattleResolver {
  /**
   * Scales current HP proportionally when max HP changes (e.g. Dynamax activation or revert)
   * so mid-battle transformations never perform an unintended full-heal.
   */
  fun scaleHpProportionally(currentHp: Int, oldMaxHp: Int, newMaxHp: Int): Int {
    if (currentHp <= 0) return 0
    if (oldMaxHp <= 0) return newMaxHp.coerceAtLeast(1)
    val ratio = currentHp.toFloat() / oldMaxHp.toFloat()
    return (ratio * newMaxHp).roundToInt().coerceIn(1, newMaxHp.coerceAtLeast(1))
  }

  fun computeEndOfTurnLeftoversHeal(combatant: CombatantSnapshot): Int {
    if (combatant.heldItemId != "leftovers") return 0
    return max(1, combatant.maxHp() / 16)
  }

  /**
   * Resolves a single turn interaction using clean-room Gen III-IX canonical damage rules,
   * Terastallization (preserving regular move types & changing Tera Blast only),
   * Levitate Ground immunity, Wonder Guard, Sandstorm Rock Sp.Def, Snow Ice Def,
   * Misty Terrain Dragon mitigation, grounded-attacker Terrain boosts, Hold Items,
   * and deterministic seeded speed-tie handling.
   */
  fun resolveTurn(
    attacker: CombatantSnapshot,
    defender: CombatantSnapshot,
    move: BattleMoveSpec,
    field: BattleTurnField,
    defenderMovePriority: Int = 0,
  ): BattleResolutionResult {
    val steps = mutableListOf<String>()
    val defenderMaxHp = defender.maxHp()

    // 1. Speed & Priority Turn Order Resolution (with seeded fair speed-tie)
    val atkSpeed = attacker.effectiveStat("SPE") * (if (field.isTailwindAttacker) 2 else 1)
    val defSpeed = defender.effectiveStat("SPE") * (if (field.isTailwindDefender) 2 else 1)
    val attackerFirst = resolveTurnOrder(
      atkPriority = move.priority,
      defPriority = defenderMovePriority,
      atkSpeed = atkSpeed,
      defSpeed = defSpeed,
      isTrickRoom = field.isTrickRoomActive,
      speedTieSeed = field.speedTieSeed,
    )
    steps.add("Speed Check: ${attacker.name} ($atkSpeed SPE, Prio ${move.priority}) vs ${defender.name} ($defSpeed SPE, Prio $defenderMovePriority) -> ${if (attackerFirst) attacker.name else defender.name} moves first")

    // 2. Grounded checks & Psychic Terrain priority immunity
    val attackerGrounded = attacker.isGrounded(field.isGravityActive)
    val defenderGrounded = defender.isGrounded(field.isGravityActive)

    val blockedByTerrain = field.terrain == TerrainCondition.PSYCHIC_TERRAIN &&
      move.priority > 0 &&
      defenderGrounded
    if (blockedByTerrain) {
      steps.add("Psychic Terrain neutralized priority move ${move.name} against grounded ${defender.name}!")
      return BattleResolutionResult(
        minDamage = 0,
        maxDamage = 0,
        minPercentHp = 0f,
        maxPercentHp = 0f,
        typeEffectiveness = 0f,
        stabMultiplier = 1f,
        counterShieldChipRecoil = 0,
        attackerActsFirst = attackerFirst,
        isBlockedByTerrain = true,
        resolvedMoveType = move.type,
        breakdownSteps = steps,
      )
    }

    if (move.category == MoveCategory.STATUS || move.basePower <= 0) {
      steps.add("${move.name} is a Status technique; executes tactical field/stat effect cleanly.")
      return BattleResolutionResult(
        minDamage = 0,
        maxDamage = 0,
        minPercentHp = 0f,
        maxPercentHp = 0f,
        typeEffectiveness = 1f,
        stabMultiplier = 1f,
        counterShieldChipRecoil = 0,
        attackerActsFirst = attackerFirst,
        isBlockedByTerrain = false,
        resolvedMoveType = move.type,
        breakdownSteps = steps,
      )
    }

    // 3. Canonical Terastallization Move Type Rule:
    // Regular moves keep their own type! Only Tera Blast changes to the user's Tera Type.
    val isTeraBlastMove = move.isTeraBlast || move.name.equals("Tera Blast", ignoreCase = true)
    val effectiveMoveType = if (
      isTeraBlastMove &&
      attacker.gimmickMode == BattleGimmickMode.TERASTALLIZATION &&
      attacker.teraType != null
    ) {
      steps.add("Tera Blast shifted type to ${attacker.teraType.displayName}!")
      attacker.teraType
    } else {
      move.type
    }

    // 3b. Levitate Ground-Type Immunity (unless Gravity grounds the defender)
    val levitateBlocksGround = effectiveMoveType == PokemonType.GROUND &&
      !field.isGravityActive &&
      defender.abilityName.equals("Levitate", ignoreCase = true)
    if (levitateBlocksGround) {
      steps.add("${defender.name}'s Levitate granted Ground-type immunity against ${move.name}!")
      return BattleResolutionResult(
        minDamage = 0,
        maxDamage = 0,
        minPercentHp = 0f,
        maxPercentHp = 0f,
        typeEffectiveness = 0f,
        stabMultiplier = 1f,
        counterShieldChipRecoil = 0,
        attackerActsFirst = attackerFirst,
        isBlockedByTerrain = false,
        resolvedMoveType = effectiveMoveType,
        breakdownSteps = steps,
      )
    }

    val effectiveness = CanonTypeChart.calculateEffectiveness(
      moveType = effectiveMoveType,
      defenderPrimary = defender.primaryType,
      defenderSecondary = defender.secondaryType,
      defenderTeraType = if (defender.gimmickMode == BattleGimmickMode.TERASTALLIZATION) defender.teraType else null,
      isFreezeDry = move.isFreezeDry,
      isFlyingPress = move.isFlyingPress,
      weather = field.weather,
      isGravityActive = field.isGravityActive,
    )

    if (effectiveness == 0.0f) {
      steps.add("Type Immunity: ${effectiveMoveType.displayName} has 0.0x effect against ${defender.name}.")
      return BattleResolutionResult(
        minDamage = 0,
        maxDamage = 0,
        minPercentHp = 0f,
        maxPercentHp = 0f,
        typeEffectiveness = 0f,
        stabMultiplier = 1f,
        counterShieldChipRecoil = 0,
        attackerActsFirst = attackerFirst,
        isBlockedByTerrain = false,
        resolvedMoveType = effectiveMoveType,
        breakdownSteps = steps,
      )
    }

    // 3c. Wonder Guard (Shedinja) immunity to non-super-effective moves
    val hasWonderGuard = defender.abilityName.equals("Wonder Guard", ignoreCase = true) || defender.dexNumber == 292
    if (hasWonderGuard && effectiveness <= 1.0f) {
      steps.add("Wonder Guard shielded ${defender.name} from non-super-effective ${move.name} (${effectiveness}x)!")
      return BattleResolutionResult(
        minDamage = 0,
        maxDamage = 0,
        minPercentHp = 0f,
        maxPercentHp = 0f,
        typeEffectiveness = 0f,
        stabMultiplier = 1f,
        counterShieldChipRecoil = 0,
        attackerActsFirst = attackerFirst,
        isBlockedByTerrain = false,
        resolvedMoveType = effectiveMoveType,
        breakdownSteps = steps,
      )
    }

    // 4. Offensive & Defensive Stats (Tera Blast adapts to higher Atk/SpA when Terastallized)
    val isPhysical = if (isTeraBlastMove && attacker.gimmickMode == BattleGimmickMode.TERASTALLIZATION) {
      attacker.effectiveStat("ATK", ignoreNegativeStage = field.isCriticalHit) >
        attacker.effectiveStat("SPA", ignoreNegativeStage = field.isCriticalHit)
    } else {
      move.category == MoveCategory.PHYSICAL
    }

    val atkStat = attacker.effectiveStat(
      statCode = if (isPhysical) "ATK" else "SPA",
      ignoreNegativeStage = field.isCriticalHit,
    )
    var defStat = defender.effectiveStat(
      statCode = if (isPhysical) "DEF" else "SPD",
      ignorePositiveStage = field.isCriticalHit,
    ).coerceAtLeast(1)

    // 4b. Sandstorm +1.5x Rock Sp.Def and Snow +1.5x Ice Def
    val defenderTypes = defender.effectiveDefensiveTypes()
    if (!isPhysical && field.weather == WeatherCondition.SANDSTORM && defenderTypes.contains(PokemonType.ROCK)) {
      defStat = floor(defStat * 1.5).toInt().coerceAtLeast(1)
      steps.add("Sandstorm fortified Rock-type ${defender.name}'s Sp.Def by 1.5x.")
    }
    if (isPhysical && field.weather == WeatherCondition.SNOW && defenderTypes.contains(PokemonType.ICE)) {
      defStat = floor(defStat * 1.5).toInt().coerceAtLeast(1)
      steps.add("Snow fortified Ice-type ${defender.name}'s Defense by 1.5x.")
    }

    // 5. Base Power Modifiers (Z-Move, Dynamax, Grounded Terrain, Battle Bond)
    var adjustedPower = move.basePower.toFloat()
    if (attacker.gimmickMode == BattleGimmickMode.Z_MOVE) {
      adjustedPower *= 1.75f
      steps.add("Z-Power Sync amplified ${move.name} base power to ${adjustedPower.toInt()}")
    } else if (attacker.gimmickMode == BattleGimmickMode.DYNAMAX) {
      adjustedPower *= 1.35f
      steps.add("Max Move conversion scaled power to ${adjustedPower.toInt()}")
    } else if (attacker.gimmickMode == BattleGimmickMode.ANIME_BATTLE_BOND && attacker.bondGauge >= 75) {
      adjustedPower *= 1.30f
      steps.add("Battle Bond Aura (${attacker.bondGauge}% sync) surged power by 1.30x!")
    }

    // Grounded-attacker checks for Electric / Grassy / Psychic Terrain 1.3x boosts
    if (attackerGrounded) {
      if (field.terrain == TerrainCondition.ELECTRIC_TERRAIN && effectiveMoveType == PokemonType.ELECTRIC) {
        adjustedPower *= 1.3f
        steps.add("Electric Terrain boosted grounded ${attacker.name}'s Electric strike by 1.3x.")
      } else if (field.terrain == TerrainCondition.GRASSY_TERRAIN && effectiveMoveType == PokemonType.GRASS) {
        adjustedPower *= 1.3f
        steps.add("Grassy Terrain boosted grounded ${attacker.name}'s Grass strike by 1.3x.")
      } else if (field.terrain == TerrainCondition.PSYCHIC_TERRAIN && effectiveMoveType == PokemonType.PSYCHIC) {
        adjustedPower *= 1.3f
        steps.add("Psychic Terrain boosted grounded ${attacker.name}'s Psychic strike by 1.3x.")
      }
    }

    // Misty Terrain halves Dragon-type damage against grounded defenders
    val mistyDragonMitigation = if (
      field.terrain == TerrainCondition.MISTY_TERRAIN &&
      defenderGrounded &&
      effectiveMoveType == PokemonType.DRAGON
    ) {
      steps.add("Misty Terrain halved Dragon-type damage against grounded ${defender.name} (0.5x).")
      0.5f
    } else {
      1.0f
    }

    // 6. Core Gen III-IX Damage Equation
    val lvl = attacker.resolvedLevel()
    val baseDamage = floor(
      floor((floor((2.0 * lvl) / 5.0 + 2.0) * adjustedPower * atkStat) / defStat) / 50.0
    ) + 2.0

    // 7. Weather Multiplier
    val weatherMult = when {
      field.weather == WeatherCondition.HARSH_SUNLIGHT && effectiveMoveType == PokemonType.FIRE -> 1.5f
      field.weather == WeatherCondition.HARSH_SUNLIGHT && effectiveMoveType == PokemonType.WATER -> 0.5f
      field.weather == WeatherCondition.RAIN && effectiveMoveType == PokemonType.WATER -> 1.5f
      field.weather == WeatherCondition.RAIN && effectiveMoveType == PokemonType.FIRE -> 0.5f
      else -> 1.0f
    }

    // 8. Canonical STAB + Terastallization 2.0x rule (preserves original STAB!)
    val matchesOriginalStab = effectiveMoveType == attacker.primaryType || effectiveMoveType == attacker.secondaryType
    val matchesTera = attacker.gimmickMode == BattleGimmickMode.TERASTALLIZATION && effectiveMoveType == attacker.teraType
    val stab = when {
      matchesTera && matchesOriginalStab -> 2.0f
      matchesTera || matchesOriginalStab -> 1.5f
      else -> 1.0f
    }

    // 9. Critical, Burn / Frostbite, Life Orb, Counter-Shield, Dodge mitigation
    val critMult = if (field.isCriticalHit) 1.5f else 1.0f
    val statusPenalty = when {
      isPhysical && attacker.status == StatusCondition.BURN && !attacker.abilityName.equals("Guts", ignoreCase = true) -> 0.5f
      !isPhysical && attacker.status == StatusCondition.FROSTBITE -> 0.5f
      else -> 1.0f
    }
    val lifeOrbMult = if (attacker.heldItemId == "life_orb") 1.3f else 1.0f
    val lifeOrbRecoil = if (attacker.heldItemId == "life_orb") max(1, attacker.maxHp() / 10) else 0
    val counterShieldMitigation = if (defender.gimmickMode == BattleGimmickMode.ANIME_COUNTER_SHIELD) 0.72f else 1.0f
    val dodgeGlance = if (field.animeDodgeTimingBoost) 0.85f else 1.0f

    val hitCount = max(1, move.hitsMin)
    val maxHitCount = max(hitCount, move.hitsMax)

    val combinedMultiplier = weatherMult * critMult * stab * effectiveness * statusPenalty *
      counterShieldMitigation * dodgeGlance * mistyDragonMitigation * lifeOrbMult
    var rawMin = max(1, floor(baseDamage * 0.85 * combinedMultiplier).toInt()) * hitCount
    var rawMax = max(1, floor(baseDamage * 1.00 * combinedMultiplier).toInt()) * maxHitCount

    // Focus Sash check (survive lethal hit from full HP with 1 HP remaining)
    var sashTriggered = false
    if (defender.heldItemId == "focus_sash" && defender.currentHpPercent >= 100 && rawMax >= defenderMaxHp && defenderMaxHp > 1) {
      rawMin = min(rawMin, defenderMaxHp - 1)
      rawMax = defenderMaxHp - 1
      sashTriggered = true
      steps.add("${defender.name} hung on using its Focus Sash (1 HP remaining)!")
    }

    val weaknessPolicy = defender.heldItemId == "weakness_policy" && effectiveness >= 2.0f
    if (weaknessPolicy) {
      steps.add("${defender.name}'s Weakness Policy activated (+2 Atk & Sp.Atk)!")
    }

    val counterRecoil = if (defender.gimmickMode == BattleGimmickMode.ANIME_COUNTER_SHIELD && move.isContact) {
      max(1, attacker.maxHp() / 8)
    } else {
      0
    }

    steps.add("Effectiveness: ${effectiveness}x • STAB: ${stab}x • Weather: ${weatherMult}x • Damage: $rawMin–$rawMax HP")
    if (lifeOrbRecoil > 0) {
      steps.add("${attacker.name}'s Life Orb boosted damage by 1.3x (-$lifeOrbRecoil recoil HP).")
    }
    if (counterRecoil > 0) {
      steps.add("Counter-Shield deflected contact strike and dealt $counterRecoil chip damage back!")
    }

    return BattleResolutionResult(
      minDamage = rawMin,
      maxDamage = rawMax,
      minPercentHp = ((rawMin.toFloat() / defenderMaxHp) * 100f).coerceIn(0f, 999f),
      maxPercentHp = ((rawMax.toFloat() / defenderMaxHp) * 100f).coerceIn(0f, 999f),
      typeEffectiveness = effectiveness,
      stabMultiplier = stab,
      counterShieldChipRecoil = counterRecoil,
      attackerActsFirst = attackerFirst,
      isBlockedByTerrain = false,
      lifeOrbRecoil = lifeOrbRecoil,
      focusSashTriggered = sashTriggered,
      weaknessPolicyTriggered = weaknessPolicy,
      resolvedMoveType = effectiveMoveType,
      breakdownSteps = steps,
    )
  }

  fun resolveTurnOrder(
    atkPriority: Int,
    defPriority: Int,
    atkSpeed: Int,
    defSpeed: Int,
    isTrickRoom: Boolean,
    speedTieSeed: Int = 0,
  ): Boolean {
    if (atkPriority != defPriority) return atkPriority > defPriority
    if (atkSpeed == defSpeed) return (speedTieSeed and 1) == 0
    return if (isTrickRoom) atkSpeed < defSpeed else atkSpeed > defSpeed
  }

  /**
   * Clean-room Poké Ball capture formula wired to all 13 Studio Poké Balls.
   */
  fun calculateCaptureResult(
    ballId: String,
    ballName: String,
    targetCurrentHp: Int,
    targetMaxHp: Int,
    targetBaseStatTotal: Int,
    targetPrimaryType: PokemonType,
    targetSecondaryType: PokemonType?,
    targetStatus: StatusCondition,
    turnNumber: Int,
    isWaterSector: Boolean,
    isRiftOrShadow: Boolean,
    alreadyCaptured: Boolean,
    deterministicRoll01: Float, // 0f..1f
  ): CaptureRollResult {
    if (ballId == "ball_master") {
      return CaptureRollResult(
        captured = true,
        shakes = 3,
        catchProbability = 1.0f,
        summary = "Master Ball locked on with 100% Harmonic certainty!",
      )
    }

    val safeMaxHp = targetMaxHp.coerceAtLeast(1)
    val safeCurHp = targetCurrentHp.coerceIn(1, safeMaxHp)
    val hpFactor = (3f * safeMaxHp - 2f * safeCurHp) / (3f * safeMaxHp)

    val speciesRate = when {
      targetBaseStatTotal >= 600 -> 45f
      targetBaseStatTotal >= 500 -> 85f
      targetBaseStatTotal >= 400 -> 140f
      else -> 210f
    }

    val ballMult = when (ballId) {
      "ball_great" -> 1.5f
      "ball_ultra" -> 2.0f
      "ball_net" -> if (targetPrimaryType == PokemonType.WATER || targetSecondaryType == PokemonType.WATER ||
        targetPrimaryType == PokemonType.BUG || targetSecondaryType == PokemonType.BUG
      ) 3.5f else 1.0f
      "ball_dusk" -> if (isRiftOrShadow) 3.0f else 1.2f
      "ball_quick" -> if (turnNumber <= 1) 5.0f else 1.0f
      "ball_timer" -> (1.0f + turnNumber * 0.35f).coerceAtMost(4.0f)
      "ball_dive" -> if (isWaterSector || targetPrimaryType == PokemonType.WATER || targetSecondaryType == PokemonType.WATER) 3.5f else 1.0f
      "ball_repeat" -> if (alreadyCaptured) 3.5f else 1.0f
      "ball_luxury" -> 1.25f
      "ball_premier" -> 1.15f
      "ball_prism" -> if (isRiftOrShadow) 4.0f else 2.2f
      else -> 1.0f
    }

    val statusMult = when (targetStatus) {
      StatusCondition.SLEEP, StatusCondition.FROZEN -> 2.0f
      StatusCondition.PARALYSIS, StatusCondition.BURN, StatusCondition.POISON, StatusCondition.TOXIC, StatusCondition.FROSTBITE -> 1.5f
      StatusCondition.HEALTHY -> 1.0f
    }

    val rawProb = ((hpFactor * speciesRate * ballMult * statusMult) / 255f).coerceIn(0.05f, 0.98f)
    val roll = deterministicRoll01.coerceIn(0f, 0.999f)
    val captured = roll < rawProb
    val shakes = if (captured) {
      3
    } else {
      val ratio = (rawProb / (roll.coerceAtLeast(0.01f))).coerceIn(0f, 0.99f)
      (ratio * 3f).toInt().coerceIn(0, 2)
    }

    val pct = (rawProb * 100f).roundToInt()
    val summary = if (captured) {
      "$ballName clicked after 3 shakes! ($pct% catch rate, ${ballMult}x ball mult)"
    } else {
      "$ballName wobbled $shakes time(s) and broke free! ($pct% catch rate)"
    }
    return CaptureRollResult(
      captured = captured,
      shakes = shakes,
      catchProbability = rawProb,
      summary = summary,
    )
  }
}

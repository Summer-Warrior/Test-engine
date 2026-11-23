package com.example.engine

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Mutually-exclusive battle gimmick & Anime resonance modes.
 * Enforcing one active transformation per trainer per battle eliminates state corruption glitches.
 */
enum class BattleGimmickMode(val displayName: String, val sourceOrigin: String) {
  NONE("Standard Form", "Core Gen I–IX Baseline"),
  MEGA_EVOLUTION("Mega Evolution (+100 BST)", "XY / ORAS / Anime XY"),
  Z_MOVE("Z-Power Burst (1.75x Signature)", "Sun & Moon / Alola League"),
  DYNAMAX("Dynamax (2.0x HP + Max Move)", "Sword & Shield / World Coronation"),
  TERASTALLIZATION("Terastallization (2.0x Adaptive STAB)", "Scarlet & Violet / Horizons"),
  ANIME_BATTLE_BOND("Anime Battle Bond (Aura Resonance)", "Ash-Greninja / XY&Z Anime"),
  ANIME_COUNTER_SHIELD("Anime Counter-Shield Spin", "Sinnoh League / Dawn & Ash Tech"),
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
  /**
   * Enforces canonical Pokémon EV/IV invariants:
   * - IVs strictly in [0, 31]
   * - Single EV strictly in [0, 252]
   * - Total EV sum strictly <= 510
   */
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
) {
  fun resolvedLevel(): Int = level.coerceIn(1, 100)

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
    val adjustedStage = when {
      ignoreNegativeStage && stageVal < 0 -> 0
      ignorePositiveStage && stageVal > 0 -> 0
      else -> stageVal
    }
    val staged = floor(natureApplied * gimmickBoost * StatStages.stageRatio(adjustedStage)).toInt()
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
  val breakdownSteps: List<String>,
)

object CanonBattleResolver {
  /**
   * Resolves a single turn interaction using deterministic Gen III-IX damage formulas
   * merged with Anime Battle Bond, Counter-Shield, Mega, Z-Move, Dynamax, and Tera rules.
   * Zero division-by-zero, zero NaN, and strict bounds guaranteed.
   */
  fun resolveTurn(
    attacker: CombatantSnapshot,
    defender: CombatantSnapshot,
    move: BattleMoveSpec,
    field: BattleTurnField,
  ): BattleResolutionResult {
    val steps = mutableListOf<String>()
    val defenderMaxHp = defender.maxHp()

    // 1. Speed & Priority Turn Order Resolution
    val atkSpeed = attacker.effectiveStat("SPE") * (if (field.isTailwindAttacker) 2 else 1)
    val defSpeed = defender.effectiveStat("SPE") * (if (field.isTailwindDefender) 2 else 1)
    val attackerFirst = resolveTurnOrder(
      atkPriority = move.priority,
      defPriority = 0,
      atkSpeed = atkSpeed,
      defSpeed = defSpeed,
      isTrickRoom = field.isTrickRoomActive,
    )
    steps.add("Speed Check: ${attacker.name} ($atkSpeed SPE, Prio ${move.priority}) vs ${defender.name} ($defSpeed SPE) -> ${if (attackerFirst) attacker.name else defender.name} moves first")

    // 2. Psychic Terrain Priority Immunity Check
    val defenderGrounded = defender.primaryType != PokemonType.FLYING &&
      defender.secondaryType != PokemonType.FLYING &&
      !defender.abilityName.equals("Levitate", ignoreCase = true)
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
        breakdownSteps = steps,
      )
    }

    // 3. Type Effectiveness (with Terastallization, Freeze-Dry, Flying Press, Strong Winds)
    val effectiveMoveType = if (attacker.gimmickMode == BattleGimmickMode.TERASTALLIZATION &&
      attacker.teraType != null &&
      move.type == attacker.primaryType
    ) {
      attacker.teraType
    } else {
      move.type
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
        breakdownSteps = steps,
      )
    }

    // 4. Offensive & Defensive Stats (Crit ignores negative atk stages & positive def stages)
    val isPhysical = move.category == MoveCategory.PHYSICAL
    val atkStat = attacker.effectiveStat(
      statCode = if (isPhysical) "ATK" else "SPA",
      ignoreNegativeStage = field.isCriticalHit,
    )
    val defStat = defender.effectiveStat(
      statCode = if (isPhysical) "DEF" else "SPD",
      ignorePositiveStage = field.isCriticalHit,
    ).coerceAtLeast(1)

    // 5. Base Power Modifiers (Z-Move, Dynamax, Terrain, Anime Battle Bond)
    var adjustedPower = move.basePower.toFloat()
    if (attacker.gimmickMode == BattleGimmickMode.Z_MOVE) {
      adjustedPower *= 1.75f
      steps.add("Z-Power Sync amplified ${move.name} base power to ${adjustedPower.toInt()}")
    } else if (attacker.gimmickMode == BattleGimmickMode.DYNAMAX) {
      adjustedPower *= 1.35f
      steps.add("Max Move conversion scaled power to ${adjustedPower.toInt()}")
    } else if (attacker.gimmickMode == BattleGimmickMode.ANIME_BATTLE_BOND && attacker.bondGauge >= 75) {
      adjustedPower *= 1.30f
      steps.add("Anime Battle Bond Aura (${attacker.bondGauge}% sync) surged power by 1.30x!")
    }

    if (field.terrain == TerrainCondition.ELECTRIC_TERRAIN && effectiveMoveType == PokemonType.ELECTRIC) {
      adjustedPower *= 1.3f
    } else if (field.terrain == TerrainCondition.GRASSY_TERRAIN && effectiveMoveType == PokemonType.GRASS) {
      adjustedPower *= 1.3f
    } else if (field.terrain == TerrainCondition.PSYCHIC_TERRAIN && effectiveMoveType == PokemonType.PSYCHIC) {
      adjustedPower *= 1.3f
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

    // 8. STAB + Terastallization 2.0x rule
    val matchesOriginalStab = effectiveMoveType == attacker.primaryType || effectiveMoveType == attacker.secondaryType
    val matchesTera = attacker.gimmickMode == BattleGimmickMode.TERASTALLIZATION && effectiveMoveType == attacker.teraType
    val stab = when {
      matchesTera && matchesOriginalStab -> 2.0f
      matchesTera || matchesOriginalStab -> 1.5f
      else -> 1.0f
    }

    // 9. Critical, Burn / Frostbite, Anime Counter-Shield, and Anime Dodge mitigation
    val critMult = if (field.isCriticalHit) 1.5f else 1.0f
    val statusPenalty = when {
      isPhysical && attacker.status == StatusCondition.BURN && !attacker.abilityName.equals("Guts", ignoreCase = true) -> 0.5f
      !isPhysical && attacker.status == StatusCondition.FROSTBITE -> 0.5f
      else -> 1.0f
    }
    val counterShieldMitigation = if (defender.gimmickMode == BattleGimmickMode.ANIME_COUNTER_SHIELD) 0.72f else 1.0f
    val dodgeGlance = if (field.animeDodgeTimingBoost) 0.85f else 1.0f

    val hitCount = max(1, move.hitsMin)
    val maxHitCount = max(hitCount, move.hitsMax)

    val combinedMultiplier = weatherMult * critMult * stab * effectiveness * statusPenalty * counterShieldMitigation * dodgeGlance
    val rawMin = max(1, floor(baseDamage * 0.85 * combinedMultiplier).toInt()) * hitCount
    val rawMax = max(1, floor(baseDamage * 1.00 * combinedMultiplier).toInt()) * maxHitCount

    val counterRecoil = if (defender.gimmickMode == BattleGimmickMode.ANIME_COUNTER_SHIELD && move.isContact) {
      max(1, attacker.maxHp() / 8)
    } else {
      0
    }

    steps.add("Effectiveness: ${effectiveness}x • STAB: ${stab}x • Weather: ${weatherMult}x • Damage: $rawMin–$rawMax HP")
    if (counterRecoil > 0) {
      steps.add("Anime Counter-Shield deflected contact strike and dealt $counterRecoil chip damage back!")
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
      breakdownSteps = steps,
    )
  }

  fun resolveTurnOrder(
    atkPriority: Int,
    defPriority: Int,
    atkSpeed: Int,
    defSpeed: Int,
    isTrickRoom: Boolean,
  ): Boolean {
    if (atkPriority != defPriority) return atkPriority > defPriority
    if (atkSpeed == defSpeed) return true
    return if (isTrickRoom) atkSpeed < defSpeed else atkSpeed > defSpeed
  }
}

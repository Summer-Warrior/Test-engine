package com.example.engine

/**
 * Complete 18-type Pokémon Type Chart, Weather, Terrain, and Status/Volatile system
 * unifying Gen I–IX mainline game mechanics and anime battle conditions.
 */
enum class PokemonType(val displayName: String) {
  NORMAL("Normal"),
  FIRE("Fire"),
  WATER("Water"),
  ELECTRIC("Electric"),
  GRASS("Grass"),
  ICE("Ice"),
  FIGHTING("Fighting"),
  POISON("Poison"),
  GROUND("Ground"),
  FLYING("Flying"),
  PSYCHIC("Psychic"),
  BUG("Bug"),
  ROCK("Rock"),
  GHOST("Ghost"),
  DRAGON("Dragon"),
  DARK("Dark"),
  STEEL("Steel"),
  FAIRY("Fairy"),
  ;

  companion object {
    fun fromString(raw: String): PokemonType {
      val clean = raw.trim()
      return entries.firstOrNull { it.displayName.equals(clean, ignoreCase = true) || it.name.equals(clean, ignoreCase = true) }
        ?: NORMAL
    }
  }
}

enum class MoveCategory(val label: String) {
  PHYSICAL("Physical"),
  SPECIAL("Special"),
  STATUS("Status"),
}

enum class WeatherCondition(val label: String) {
  CLEAR("Clear Skies"),
  HARSH_SUNLIGHT("Harsh Sunlight"),
  RAIN("Rain Dance"),
  SANDSTORM("Sandstorm"),
  SNOW("Snowscape"),
  STRONG_WINDS("Delta Stream (Strong Winds)"),
}

enum class TerrainCondition(val label: String) {
  NONE("Standard Field"),
  ELECTRIC_TERRAIN("Electric Terrain"),
  GRASSY_TERRAIN("Grassy Terrain"),
  MISTY_TERRAIN("Misty Terrain"),
  PSYCHIC_TERRAIN("Psychic Terrain"),
}

enum class StatusCondition(val label: String) {
  HEALTHY("Healthy"),
  BURN("Burned"),
  PARALYSIS("Paralyzed"),
  POISON("Poisoned"),
  TOXIC("Badly Poisoned"),
  SLEEP("Asleep"),
  FROZEN("Frozen"),
  FROSTBITE("Frostbite"),
}

object CanonTypeChart {
  private val matrix: Map<PokemonType, Map<PokemonType, Float>> = mapOf(
    PokemonType.NORMAL to mapOf(
      PokemonType.ROCK to 0.5f,
      PokemonType.GHOST to 0.0f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.FIRE to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.WATER to 0.5f,
      PokemonType.GRASS to 2.0f,
      PokemonType.ICE to 2.0f,
      PokemonType.BUG to 2.0f,
      PokemonType.ROCK to 0.5f,
      PokemonType.DRAGON to 0.5f,
      PokemonType.STEEL to 2.0f,
    ),
    PokemonType.WATER to mapOf(
      PokemonType.FIRE to 2.0f,
      PokemonType.WATER to 0.5f,
      PokemonType.GRASS to 0.5f,
      PokemonType.GROUND to 2.0f,
      PokemonType.ROCK to 2.0f,
      PokemonType.DRAGON to 0.5f,
    ),
    PokemonType.ELECTRIC to mapOf(
      PokemonType.WATER to 2.0f,
      PokemonType.ELECTRIC to 0.5f,
      PokemonType.GRASS to 0.5f,
      PokemonType.GROUND to 0.0f,
      PokemonType.FLYING to 2.0f,
      PokemonType.DRAGON to 0.5f,
    ),
    PokemonType.GRASS to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.WATER to 2.0f,
      PokemonType.GRASS to 0.5f,
      PokemonType.POISON to 0.5f,
      PokemonType.GROUND to 2.0f,
      PokemonType.FLYING to 0.5f,
      PokemonType.BUG to 0.5f,
      PokemonType.ROCK to 2.0f,
      PokemonType.DRAGON to 0.5f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.ICE to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.WATER to 0.5f,
      PokemonType.GRASS to 2.0f,
      PokemonType.ICE to 0.5f,
      PokemonType.GROUND to 2.0f,
      PokemonType.FLYING to 2.0f,
      PokemonType.DRAGON to 2.0f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.FIGHTING to mapOf(
      PokemonType.NORMAL to 2.0f,
      PokemonType.ICE to 2.0f,
      PokemonType.POISON to 0.5f,
      PokemonType.FLYING to 0.5f,
      PokemonType.PSYCHIC to 0.5f,
      PokemonType.BUG to 0.5f,
      PokemonType.ROCK to 2.0f,
      PokemonType.GHOST to 0.0f,
      PokemonType.DARK to 2.0f,
      PokemonType.STEEL to 2.0f,
      PokemonType.FAIRY to 0.5f,
    ),
    PokemonType.POISON to mapOf(
      PokemonType.GRASS to 2.0f,
      PokemonType.POISON to 0.5f,
      PokemonType.GROUND to 0.5f,
      PokemonType.ROCK to 0.5f,
      PokemonType.GHOST to 0.5f,
      PokemonType.STEEL to 0.0f,
      PokemonType.FAIRY to 2.0f,
    ),
    PokemonType.GROUND to mapOf(
      PokemonType.FIRE to 2.0f,
      PokemonType.ELECTRIC to 2.0f,
      PokemonType.GRASS to 0.5f,
      PokemonType.POISON to 2.0f,
      PokemonType.FLYING to 0.0f,
      PokemonType.BUG to 0.5f,
      PokemonType.ROCK to 2.0f,
      PokemonType.STEEL to 2.0f,
    ),
    PokemonType.FLYING to mapOf(
      PokemonType.ELECTRIC to 0.5f,
      PokemonType.GRASS to 2.0f,
      PokemonType.FIGHTING to 2.0f,
      PokemonType.BUG to 2.0f,
      PokemonType.ROCK to 0.5f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.PSYCHIC to mapOf(
      PokemonType.FIGHTING to 2.0f,
      PokemonType.POISON to 2.0f,
      PokemonType.PSYCHIC to 0.5f,
      PokemonType.DARK to 0.0f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.BUG to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.GRASS to 2.0f,
      PokemonType.FIGHTING to 0.5f,
      PokemonType.POISON to 0.5f,
      PokemonType.FLYING to 0.5f,
      PokemonType.PSYCHIC to 2.0f,
      PokemonType.GHOST to 0.5f,
      PokemonType.DARK to 2.0f,
      PokemonType.STEEL to 0.5f,
      PokemonType.FAIRY to 0.5f,
    ),
    PokemonType.ROCK to mapOf(
      PokemonType.FIRE to 2.0f,
      PokemonType.ICE to 2.0f,
      PokemonType.FIGHTING to 0.5f,
      PokemonType.GROUND to 0.5f,
      PokemonType.FLYING to 2.0f,
      PokemonType.BUG to 2.0f,
      PokemonType.STEEL to 0.5f,
    ),
    PokemonType.GHOST to mapOf(
      PokemonType.NORMAL to 0.0f,
      PokemonType.PSYCHIC to 2.0f,
      PokemonType.GHOST to 2.0f,
      PokemonType.DARK to 0.5f,
    ),
    PokemonType.DRAGON to mapOf(
      PokemonType.DRAGON to 2.0f,
      PokemonType.STEEL to 0.5f,
      PokemonType.FAIRY to 0.0f,
    ),
    PokemonType.DARK to mapOf(
      PokemonType.FIGHTING to 0.5f,
      PokemonType.PSYCHIC to 2.0f,
      PokemonType.GHOST to 2.0f,
      PokemonType.DARK to 0.5f,
      PokemonType.FAIRY to 0.5f,
    ),
    PokemonType.STEEL to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.WATER to 0.5f,
      PokemonType.ELECTRIC to 0.5f,
      PokemonType.ICE to 2.0f,
      PokemonType.ROCK to 2.0f,
      PokemonType.STEEL to 0.5f,
      PokemonType.FAIRY to 2.0f,
    ),
    PokemonType.FAIRY to mapOf(
      PokemonType.FIRE to 0.5f,
      PokemonType.FIGHTING to 2.0f,
      PokemonType.POISON to 0.5f,
      PokemonType.DRAGON to 2.0f,
      PokemonType.DARK to 2.0f,
      PokemonType.STEEL to 0.5f,
    ),
  )

  /**
   * Computes single or dual-type effectiveness with full support for Terastallization,
   * Flying Press (Fighting + Flying), Freeze-Dry (2x vs Water), and Strong Winds.
   */
  fun calculateEffectiveness(
    moveType: PokemonType,
    defenderPrimary: PokemonType,
    defenderSecondary: PokemonType? = null,
    defenderTeraType: PokemonType? = null,
    isFreezeDry: Boolean = false,
    isFlyingPress: Boolean = false,
    weather: WeatherCondition = WeatherCondition.CLEAR,
    isGravityActive: Boolean = false,
    hasRingTarget: Boolean = false,
  ): Float {
    val effectiveDefenseTypes = if (defenderTeraType != null) {
      listOf(defenderTeraType)
    } else {
      listOfNotNull(
        defenderPrimary,
        defenderSecondary?.takeIf { it != defenderPrimary },
      )
    }

    val attackTypes = if (isFlyingPress) {
      listOf(PokemonType.FIGHTING, PokemonType.FLYING)
    } else {
      listOf(moveType)
    }

    var multiplier = 1.0f
    for (atk in attackTypes) {
      for (def in effectiveDefenseTypes) {
        var single = matrix[atk]?.get(def) ?: 1.0f
        if (isFreezeDry && atk == PokemonType.ICE && def == PokemonType.WATER) {
          single = 2.0f
        }
        if (isGravityActive && atk == PokemonType.GROUND && def == PokemonType.FLYING) {
          single = 1.0f
        }
        if (hasRingTarget && single == 0.0f) {
          single = 1.0f
        }
        if (weather == WeatherCondition.STRONG_WINDS && def == PokemonType.FLYING && single > 1.0f) {
          single = 1.0f
        }
        multiplier *= single
      }
    }
    return multiplier
  }
}

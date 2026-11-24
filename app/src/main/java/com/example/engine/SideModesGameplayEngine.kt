package com.example.engine

import com.example.data.local.NationalAtlasIndex
import com.example.data.local.NativePokemonEntity
import com.example.data.local.SideModeRecordEntity
import com.example.ui.StudioViewModelHelpers
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 100% Clean-Room Pure-Kotlin State Machines & Deterministic Engines for all 5 Playable Side Modes
 * of Pokémon: Prism Resonance:
 *  1. Battle Tower / Frontier Streak (7-battle escalating 3-slot gauntlet + Frontier Brain Boss + BP Shop)
 *  2. Prism Contest & Showcase Appeal Stage (5-round Cool/Beauty/Cute/Clever/Tough + Excitement + Ribbons)
 *  3. Lental Photosphere Snap (Animated viewfinder + Illumina Orbs/Apples + 1★–4★ Diamond Scoring)
 *  4. Mystery Rift Dungeon Crawl (Procedural 5-floor 10x10 roguelite + Time Gears + Traps + Rift Guardian)
 *  5. Ranger Capture Styler & Prism Camp Bonding (Touch-loop Capture Styler + 6-Pokémon Camp & Curry)
 */
enum class SideModeType(val id: String, val label: String, val shortLabel: String) {
  BATTLE_TOWER("battle_tower", "Battle Tower Frontier", "Tower"),
  PRISM_CONTEST("prism_contest", "Prism Contest Stage", "Contest"),
  LENTAL_SNAP("lental_snap", "Lental Photosphere Snap", "Snap"),
  MYSTERY_RIFT("mystery_rift", "Mystery Rift Dungeon", "Rift"),
  RANGER_CAMP("ranger_camp", "Ranger Styler & Camp", "Styler/Camp"),
}

// =================================================================================================
// 1. BATTLE TOWER / FRONTIER STREAK (Hoenn/Sinnoh-Inspired 7-Battle 3v3 Escalating Gauntlet)
// =================================================================================================

data class TowerBpReward(
  val id: String,
  val itemId: String,
  val label: String,
  val description: String,
  val bpCost: Int,
  val grantQuantity: Int = 1,
)

data class BattleTowerState(
  val isActive: Boolean = false,
  val roundNumber: Int = 1, // 1..7 (Round 7 = Frontier Brain Boss)
  val totalRounds: Int = 7,
  val currentStreak: Int = 0,
  val opponentTitle: String = "Tower Challenger",
  val opponentName: String = "Ace Trainer Sol",
  val isFrontierBrainRound: Boolean = false,
  val playerTeamDexIds: List<Int> = listOf(25, 6, 448),
  val playerActiveSlot: Int = 0,
  val playerHpMap: Map<Int, Int> = emptyMap(),
  val playerMaxHpMap: Map<Int, Int> = emptyMap(),
  val enemyTeamDexIds: List<Int> = listOf(130, 59, 149),
  val enemyActiveSlot: Int = 0,
  val enemyHpMap: Map<Int, Int> = emptyMap(),
  val enemyMaxHpMap: Map<Int, Int> = emptyMap(),
  val enemyLevel: Int = 50,
  val turnNumber: Int = 1,
  val roundCleared: Boolean = false,
  val gauntletCompleted: Boolean = false,
  val gauntletFailed: Boolean = false,
  val bpEarnedThisRun: Int = 0,
  val battleLog: List<String> = listOf("Enter the 7-Battle Frontier Gauntlet to earn Battle Points (BP)!"),
) {
  val activePlayerDex: Int get() = playerTeamDexIds.getOrElse(playerActiveSlot) { playerTeamDexIds.firstOrNull() ?: 25 }
  val activeEnemyDex: Int get() = enemyTeamDexIds.getOrElse(enemyActiveSlot) { enemyTeamDexIds.firstOrNull() ?: 130 }
}

data class BattleTowerTurnOutcome(
  val nextState: BattleTowerState,
  val bpAwardedNow: Int = 0,
  val newStreak: Int? = null,
  val sfxCue: StudioSfxType = StudioSfxType.HIT_LIGHT,
)

object BattleTowerEngine {

  val bpShopCatalog: List<TowerBpReward> = listOf(
    TowerBpReward("bp_life_orb", "life_orb", "Life Orb", "1.3x damage boost hold item (+1)", bpCost = 10, grantQuantity = 1),
    TowerBpReward("bp_focus_sash", "focus_sash", "Focus Sash", "Survive a 1HKO at full HP (+1)", bpCost = 10, grantQuantity = 1),
    TowerBpReward("bp_choice_band", "choice_band", "Choice Band", "1.5x Physical Attack boost (+1)", bpCost = 12, grantQuantity = 1),
    TowerBpReward("bp_ultra_pack", "ball_ultra", "Ultra Ball ×5", "High-catch Prism Capsules (+5)", bpCost = 6, grantQuantity = 5),
    TowerBpReward("bp_max_potion", "med_max_potion", "Max Potion ×3", "Full HP restoration pack (+3)", bpCost = 5, grantQuantity = 3),
    TowerBpReward("bp_master_ball", "ball_master", "Master Ball", "Guaranteed 100% capture (+1)", bpCost = 25, grantQuantity = 1),
  )

  private val roundChallengers: List<Triple<String, String, List<Int>>> = listOf(
    Triple("Tower Rookie", "Lass Miri", listOf(133, 59, 131)),
    Triple("Sky Tactician", "Ace Aviator Ren", listOf(18, 130, 212)),
    Triple("Steel Vanguard", "Veteran Dax", listOf(208, 306, 376)),
    Triple("Psychic Savant", "Esoteric Lyra", listOf(65, 282, 196)),
    Triple("Dragon Tamer", "Master Drakon", listOf(330, 373, 149)),
    Triple("Elite Strategist", "Frontier Ace Vesper", listOf(445, 448, 468)),
    Triple("Frontier Brain", "Tower Tycoon Valen", listOf(381, 488, 384)),
  )

  fun startGauntlet(
    partyDexIds: List<Int>,
    resolveEntity: (Int) -> NativePokemonEntity,
    startingStreak: Int = 0,
  ): BattleTowerState {
    val team = partyDexIds.distinct().take(3).ifEmpty { listOf(25, 6, 448) }
    return prepareRound(
      roundNumber = 1,
      playerTeamDexIds = team,
      currentStreak = startingStreak,
      bpEarnedSoFar = 0,
      resolveEntity = resolveEntity,
    )
  }

  fun prepareRound(
    roundNumber: Int,
    playerTeamDexIds: List<Int>,
    currentStreak: Int,
    bpEarnedSoFar: Int,
    resolveEntity: (Int) -> NativePokemonEntity,
  ): BattleTowerState {
    val clampedRound = roundNumber.coerceIn(1, 7)
    val (title, name, enemyTeam) = roundChallengers[clampedRound - 1]
    val enemyLevel = 50 + (clampedRound - 1) + if (clampedRound == 7) 1 else 0
    val playerMaxMap = playerTeamDexIds.associateWith { dex ->
      StudioViewModelHelpers.buildCombatantFromEntity(resolveEntity(dex), level = 55).maxHp()
    }
    val enemyMaxMap = enemyTeam.associateWith { dex ->
      StudioViewModelHelpers.buildCombatantFromEntity(resolveEntity(dex), level = enemyLevel).maxHp()
    }
    val isBrain = clampedRound == 7
    val intro = if (isBrain) {
      "ROUND 7 / 7 — FRONTIER BRAIN $name challenges you (Lv.$enemyLevel)! Auto-heal restored your 3-slot squad."
    } else {
      "Round $clampedRound / 7: $title $name sends out ${resolveEntity(enemyTeam.first()).name} (Lv.$enemyLevel)! Squad auto-healed."
    }
    return BattleTowerState(
      isActive = true,
      roundNumber = clampedRound,
      currentStreak = currentStreak,
      opponentTitle = title,
      opponentName = name,
      isFrontierBrainRound = isBrain,
      playerTeamDexIds = playerTeamDexIds,
      playerActiveSlot = 0,
      playerHpMap = playerMaxMap,
      playerMaxHpMap = playerMaxMap,
      enemyTeamDexIds = enemyTeam,
      enemyActiveSlot = 0,
      enemyHpMap = enemyMaxMap,
      enemyMaxHpMap = enemyMaxMap,
      enemyLevel = enemyLevel,
      turnNumber = 1,
      roundCleared = false,
      gauntletCompleted = false,
      gauntletFailed = false,
      bpEarnedThisRun = bpEarnedSoFar,
      battleLog = listOf(intro),
    )
  }

  fun switchPlayerSlot(state: BattleTowerState, newSlot: Int): BattleTowerState {
    if (!state.isActive || state.roundCleared || state.gauntletFailed) return state
    val dex = state.playerTeamDexIds.getOrNull(newSlot) ?: return state
    if ((state.playerHpMap[dex] ?: 0) <= 0 || newSlot == state.playerActiveSlot) return state
    return state.copy(
      playerActiveSlot = newSlot,
      battleLog = (listOf("Switched active Tower lead to slot #${newSlot + 1}!") + state.battleLog).take(12),
    )
  }

  fun executeTurn(
    state: BattleTowerState,
    move: BattleMoveSpec,
    resolveEntity: (Int) -> NativePokemonEntity,
  ): BattleTowerTurnOutcome {
    if (!state.isActive || state.roundCleared || state.gauntletFailed || state.gauntletCompleted) {
      return BattleTowerTurnOutcome(state)
    }
    val pDex = state.activePlayerDex
    val eDex = state.activeEnemyDex
    val pEntity = resolveEntity(pDex)
    val eEntity = resolveEntity(eDex)
    val pSnap = StudioViewModelHelpers.buildCombatantFromEntity(pEntity, level = 55)
    val eSnap = StudioViewModelHelpers.buildCombatantFromEntity(eEntity, level = state.enemyLevel)
    val logs = mutableListOf<String>()

    val playerHp = state.playerHpMap.toMutableMap()
    val enemyHp = state.enemyHpMap.toMutableMap()
    var activePSlot = state.playerActiveSlot
    var activeESlot = state.enemyActiveSlot

    // Tactical AI switching check on Frontier Brain & Veteran rounds (rounds 4..7)
    val resolveTypes: (Int) -> Pair<PokemonType, PokemonType?> = { dex ->
      val ent = resolveEntity(dex)
      PokemonType.fromString(ent.primaryType) to ent.secondaryType.takeIf { it.isNotBlank() }?.let { PokemonType.fromString(it) }
    }
    var enemySwitchedThisTurn = false
    if (state.roundNumber >= 4) {
      val pivotIdx = TrainerBattleAiEngine.chooseTacticalMidBattleSwitchIndex(
        tactic = if (state.isFrontierBrainRound) TrainerAiTactic.GIMMICK_ACE else TrainerAiTactic.STATUS_TACTICIAN,
        partyDexIds = state.enemyTeamDexIds,
        currentIndex = activeESlot,
        hpMap = enemyHp,
        turnNumber = state.turnNumber,
        playerPrimaryType = pSnap.primaryType,
        resolveTypes = resolveTypes,
      )
      if (pivotIdx != null && pivotIdx != activeESlot) {
        activeESlot = pivotIdx
        enemySwitchedThisTurn = true
        val newEnemyEnt = resolveEntity(state.enemyTeamDexIds[activeESlot])
        logs.add("${state.opponentName} tactically pivoted to ${newEnemyEnt.name}!")
      }
    }

    val targetEnemyDex = state.enemyTeamDexIds[activeESlot]
    val targetEnemyEntity = resolveEntity(targetEnemyDex)
    val targetEnemySnap = StudioViewModelHelpers.buildCombatantFromEntity(targetEnemyEntity, level = state.enemyLevel)
    val enemyMoves = StudioViewModelHelpers.buildMovesetForPokemon(targetEnemyEntity, PokemonType.fromString(targetEnemyEntity.primaryType))
    val enemyMove = enemyMoves[(state.turnNumber + state.roundNumber) % enemyMoves.size]

    val playerTurnRes = CanonBattleResolver.resolveTurn(
      attacker = pSnap,
      defender = targetEnemySnap,
      move = move,
      field = BattleTurnField(),
      defenderMovePriority = enemyMove.priority,
    )
    val enemyTurnRes = CanonBattleResolver.resolveTurn(
      attacker = targetEnemySnap,
      defender = pSnap,
      move = enemyMove,
      field = BattleTurnField(),
      defenderMovePriority = move.priority,
    )

    val pDmg = ((playerTurnRes.minDamage + playerTurnRes.maxDamage) / 2).coerceAtLeast(if (playerTurnRes.typeEffectiveness > 0f) 4 else 0)
    val eDmg = ((enemyTurnRes.minDamage + enemyTurnRes.maxDamage) / 2).coerceAtLeast(if (enemyTurnRes.typeEffectiveness > 0f) 4 else 0)

    fun applyPlayerHit(): Boolean {
      val rem = ((enemyHp[targetEnemyDex] ?: targetEnemySnap.maxHp()) - pDmg).coerceAtLeast(0)
      enemyHp[targetEnemyDex] = rem
      val effNote = when {
        playerTurnRes.typeEffectiveness >= 2f -> " Super effective!"
        playerTurnRes.typeEffectiveness == 0f -> " No effect!"
        playerTurnRes.typeEffectiveness < 1f -> " Resisted."
        else -> ""
      }
      logs.add("${pEntity.name} used ${move.name} for $pDmg dmg!$effNote")
      return rem <= 0
    }

    fun applyEnemyHit(): Boolean {
      val rem = ((playerHp[pDex] ?: pSnap.maxHp()) - eDmg).coerceAtLeast(0)
      playerHp[pDex] = rem
      logs.add("${targetEnemyEntity.name} countered with ${enemyMove.name} for $eDmg dmg!")
      return rem <= 0
    }

    var enemyFainted = false
    var playerFainted = false
    if (enemySwitchedThisTurn) {
      // Tactical switch consumes opponent's turn; incoming partner absorbs player's move
      enemyFainted = applyPlayerHit()
    } else if (playerTurnRes.attackerActsFirst) {
      enemyFainted = applyPlayerHit()
      if (!enemyFainted) playerFainted = applyEnemyHit()
    } else {
      playerFainted = applyEnemyHit()
      if (!playerFainted) enemyFainted = applyPlayerHit()
    }

    if (enemyFainted) {
      logs.add("${targetEnemyEntity.name} fainted!")
      val nextEnemy = TrainerBattleAiEngine.chooseNextConsciousIndex(
        partyDexIds = state.enemyTeamDexIds,
        currentIndex = activeESlot,
        hpMap = enemyHp,
        playerPrimaryType = pSnap.primaryType,
        resolveTypes = resolveTypes,
      )
      if (nextEnemy != null) {
        activeESlot = nextEnemy
        logs.add("${state.opponentName} sent out ${resolveEntity(state.enemyTeamDexIds[nextEnemy]).name}!")
      } else {
        // Round Victory!
        val bpGain = if (state.isFrontierBrainRound) 15 else 3
        val updatedStreak = state.currentStreak + 1
        val finishedAll7 = state.roundNumber >= state.totalRounds
        logs.add(
          if (finishedAll7) {
            "FRONTIER SYMBOL EARNED! Defeated ${state.opponentName} — +$bpGain BP! Streak: $updatedStreak!"
          } else {
            "Cleared Round ${state.roundNumber}/7! Earned +$bpGain BP (Streak: $updatedStreak). Tap Next Round to auto-heal!"
          }
        )
        return BattleTowerTurnOutcome(
          nextState = state.copy(
            playerHpMap = playerHp,
            enemyHpMap = enemyHp,
            currentStreak = updatedStreak,
            roundCleared = true,
            gauntletCompleted = finishedAll7,
            bpEarnedThisRun = state.bpEarnedThisRun + bpGain,
            turnNumber = state.turnNumber + 1,
            battleLog = (logs.reversed() + state.battleLog).take(14),
          ),
          bpAwardedNow = bpGain,
          newStreak = updatedStreak,
          sfxCue = if (finishedAll7) StudioSfxType.LEVEL_UP else StudioSfxType.CAPTURE_SPARKLE,
        )
      }
    }

    if (playerFainted) {
      logs.add("${pEntity.name} fainted!")
      val nextPlayer = state.playerTeamDexIds.indices.firstOrNull { i -> (playerHp[state.playerTeamDexIds[i]] ?: 0) > 0 }
      if (nextPlayer != null) {
        activePSlot = nextPlayer
        logs.add("Go! ${resolveEntity(state.playerTeamDexIds[nextPlayer]).name} entered the arena!")
      } else {
        logs.add("Your 3-slot Tower squad was defeated on Round ${state.roundNumber}. Streak reset to 0.")
        return BattleTowerTurnOutcome(
          nextState = state.copy(
            playerHpMap = playerHp,
            enemyHpMap = enemyHp,
            currentStreak = 0,
            gauntletFailed = true,
            turnNumber = state.turnNumber + 1,
            battleLog = (logs.reversed() + state.battleLog).take(14),
          ),
          bpAwardedNow = 0,
          newStreak = 0,
          sfxCue = StudioSfxType.HIT_HEAVY_CRIT,
        )
      }
    }

    val sfx = if (playerTurnRes.typeEffectiveness >= 2f) StudioSfxType.HIT_HEAVY_CRIT else StudioSfxType.HIT_LIGHT
    return BattleTowerTurnOutcome(
      nextState = state.copy(
        playerActiveSlot = activePSlot,
        enemyActiveSlot = activeESlot,
        playerHpMap = playerHp,
        enemyHpMap = enemyHp,
        turnNumber = state.turnNumber + 1,
        battleLog = (logs.reversed() + state.battleLog).take(14),
      ),
      sfxCue = sfx,
    )
  }

  fun advanceToNextRound(
    state: BattleTowerState,
    resolveEntity: (Int) -> NativePokemonEntity,
  ): BattleTowerState {
    if (!state.roundCleared || state.gauntletCompleted) return state
    return prepareRound(
      roundNumber = state.roundNumber + 1,
      playerTeamDexIds = state.playerTeamDexIds,
      currentStreak = state.currentStreak,
      bpEarnedSoFar = state.bpEarnedThisRun,
      resolveEntity = resolveEntity,
    )
  }
}

// =================================================================================================
// 2. PRISM CONTEST & SHOWCASE APPEAL STAGE (Hoenn/Sinnoh/Kalos-Inspired 5-Round Competition)
// =================================================================================================

enum class ContestCategory(val label: String, val accentHex: String) {
  COOL("Cool", "#EF4444"),
  BEAUTY("Beauty", "#38BDF8"),
  CUTE("Cute", "#F472B6"),
  CLEVER("Clever", "#10B981"),
  TOUGH("Tough", "#F59E0B"),
}

data class ContestAppealSpec(
  val move: BattleMoveSpec,
  val category: ContestCategory,
  val baseHearts: Int,
  val jamPower: Int,
  val isCounterShield: Boolean,
  val excitementDelta: Int,
  val flairSummary: String,
)

data class ContestCoordinatorScore(
  val id: String,
  val name: String,
  val partnerDex: Int,
  val partnerName: String,
  val totalHearts: Int = 0,
  val lastRoundDelta: Int = 0,
  val isPlayer: Boolean = false,
)

data class PrismContestState(
  val isActive: Boolean = false,
  val category: ContestCategory = ContestCategory.COOL,
  val currentRound: Int = 1, // 1..5
  val totalRounds: Int = 5,
  val crowdExcitement: Int = 0, // 0..5 -> at 5 triggers Prism Encore (+6 Hearts!)
  val lastUsedMoveName: String = "",
  val contestants: List<ContestCoordinatorScore> = emptyList(),
  val isFinished: Boolean = false,
  val playerWonRibbon: Boolean = false,
  val stageLog: List<String> = listOf("Pick a Contest Category and perform 5 dazzling Appeals to win a Prism Ribbon!"),
)

data class ContestRoundOutcome(
  val nextState: PrismContestState,
  val ribbonAwarded: Boolean = false,
  val finalPlayerScore: Int = 0,
)

object PrismContestEngine {

  fun mapMoveToAppeal(move: BattleMoveSpec): ContestAppealSpec {
    val category = when (move.type) {
      PokemonType.ELECTRIC, PokemonType.DRAGON, PokemonType.FLYING, PokemonType.DARK -> ContestCategory.COOL
      PokemonType.WATER, PokemonType.ICE, PokemonType.FIRE -> ContestCategory.BEAUTY
      PokemonType.FAIRY, PokemonType.NORMAL -> ContestCategory.CUTE
      PokemonType.PSYCHIC, PokemonType.GHOST, PokemonType.POISON, PokemonType.BUG -> ContestCategory.CLEVER
      PokemonType.FIGHTING, PokemonType.ROCK, PokemonType.GROUND, PokemonType.STEEL, PokemonType.GRASS -> ContestCategory.TOUGH
    }
    val isShield = move.isTeraBlast || move.priority > 0 || move.category == MoveCategory.STATUS
    val jam = if (move.basePower >= 95 && !isShield) 2 else if (move.category == MoveCategory.PHYSICAL && move.basePower >= 80) 1 else 0
    val baseHearts = when {
      move.basePower >= 100 -> 5
      move.basePower >= 80 -> 4
      isShield -> 4
      else -> 3
    }
    val summary = buildString {
      append("+${baseHearts}♥ ${category.label}")
      if (isShield) append(" • Counter-Shield ★")
      if (jam > 0) append(" • Jam -$jam")
    }
    return ContestAppealSpec(
      move = move,
      category = category,
      baseHearts = baseHearts,
      jamPower = jam,
      isCounterShield = isShield,
      excitementDelta = 1,
      flairSummary = summary,
    )
  }

  fun startContest(
    category: ContestCategory,
    playerName: String,
    leadPokemon: NativePokemonEntity,
  ): PrismContestState {
    val rivals = when (category) {
      ContestCategory.COOL -> listOf(
        ContestCoordinatorScore("coord_1", "Coordinator Lyric", 448, "Lucario"),
        ContestCoordinatorScore("coord_2", "Starlight Ciel", 658, "Greninja"),
        ContestCoordinatorScore("coord_3", "Ace Solenne", 384, "Rayquaza"),
      )
      ContestCategory.BEAUTY -> listOf(
        ContestCoordinatorScore("coord_1", "Performer Aria", 282, "Gardevoir"),
        ContestCoordinatorScore("coord_2", "Coordinator Lyric", 350, "Milotic"),
        ContestCoordinatorScore("coord_3", "Starlight Ciel", 249, "Lugia"),
      )
      ContestCategory.CUTE -> listOf(
        ContestCoordinatorScore("coord_1", "Performer Aria", 133, "Eevee"),
        ContestCoordinatorScore("coord_2", "Idol Miri", 778, "Mimikyu"),
        ContestCoordinatorScore("coord_3", "Coordinator Lyric", 25, "Pikachu"),
      )
      ContestCategory.CLEVER -> listOf(
        ContestCoordinatorScore("coord_1", "Esoteric Lyra", 94, "Gengar"),
        ContestCoordinatorScore("coord_2", "Performer Aria", 150, "Mewtwo"),
        ContestCoordinatorScore("coord_3", "Starlight Ciel", 282, "Gardevoir"),
      )
      ContestCategory.TOUGH -> listOf(
        ContestCoordinatorScore("coord_1", "Master Drakon", 445, "Garchomp"),
        ContestCoordinatorScore("coord_2", "Ace Solenne", 257, "Blaziken"),
        ContestCoordinatorScore("coord_3", "Coordinator Lyric", 1007, "Koraidon"),
      )
    }
    val playerEntry = ContestCoordinatorScore(
      id = "player",
      name = playerName,
      partnerDex = leadPokemon.dexNumber,
      partnerName = leadPokemon.name,
      isPlayer = true,
    )
    return PrismContestState(
      isActive = true,
      category = category,
      currentRound = 1,
      crowdExcitement = 1,
      contestants = listOf(playerEntry) + rivals,
      stageLog = listOf("Prism ${category.label} Showcase began! ${leadPokemon.name} takes center stage for Round 1/5."),
    )
  }

  fun performAppealRound(
    state: PrismContestState,
    appeal: ContestAppealSpec,
  ): ContestRoundOutcome {
    if (!state.isActive || state.isFinished) return ContestRoundOutcome(state)

    val logs = mutableListOf<String>()
    val repeatedPenalty = if (appeal.move.name == state.lastUsedMoveName) -2 else 0
    val matchBonus = if (appeal.category == state.category) 2 else 0
    val shieldBonus = if (appeal.isCounterShield) 2 else 0

    var excitement = (state.crowdExcitement + if (appeal.category == state.category) 2 else 1).coerceAtMost(5)
    var encoreBonus = 0
    if (excitement >= 5) {
      encoreBonus = 6
      excitement = 0
      logs.add("PRISM ENCORE! The crowd erupts for +6 Star Bonus Hearts!")
    }

    val playerGain = (appeal.baseHearts + matchBonus + shieldBonus + encoreBonus + repeatedPenalty).coerceAtLeast(1)
    if (repeatedPenalty < 0) logs.add("Judges noticed repeated move ${appeal.move.name} (-2♥).")
    logs.add("You performed ${appeal.move.name} (${appeal.category.label}) for +$playerGain♥!")

    // AI Coordinators perform deterministic appeals + optional jam pressure
    val updated = state.contestants.mapIndexed { idx, c ->
      if (c.isPlayer) {
        c.copy(totalHearts = c.totalHearts + playerGain, lastRoundDelta = playerGain)
      } else {
        val aiBase = 3 + ((state.currentRound * 3 + idx * 5 + state.category.ordinal) % 4) // 3..6
        val jammed = if (appeal.jamPower > 0) appeal.jamPower else 0
        val aiGain = (aiBase - jammed).coerceAtLeast(1)
        c.copy(totalHearts = c.totalHearts + aiGain, lastRoundDelta = aiGain)
      }
    }
    if (appeal.jamPower > 0) {
      logs.add("Your dazzling finish rattled rivals (-${appeal.jamPower}♥ jam)!")
    }

    val finished = state.currentRound >= state.totalRounds
    val sorted = updated.sortedByDescending { it.totalHearts }
    val playerScore = updated.firstOrNull { it.isPlayer }?.totalHearts ?: 0
    val won = finished && sorted.firstOrNull()?.isPlayer == true
    if (finished) {
      logs.add(
        if (won) {
          "CONTEST WINNER! You won the ${state.category.label} Prism Ribbon with $playerScore♥!"
        } else {
          "Showcase complete! ${sorted.first().name} edged ahead with ${sorted.first().totalHearts}♥ (You: $playerScore♥)."
        }
      )
    }

    return ContestRoundOutcome(
      nextState = state.copy(
        currentRound = if (finished) state.totalRounds else state.currentRound + 1,
        crowdExcitement = excitement,
        lastUsedMoveName = appeal.move.name,
        contestants = updated,
        isFinished = finished,
        playerWonRibbon = won,
        stageLog = (logs.reversed() + state.stageLog).take(12),
      ),
      ribbonAwarded = won,
      finalPlayerScore = playerScore,
    )
  }
}

// =================================================================================================
// 3. LENTAL PHOTOSPHERE SNAP (Animated Viewfinder + Illumina Orbs/Apples + 1★–4★ Diamond Scoring)
// =================================================================================================

enum class SnapPoseState(val label: String, val posePoints: Int) {
  CRUISING("Natural Stride", 500),
  HAPPY_HOP("Playful Hop", 850),
  APPLE_DELIGHT("Fluffruit Feast", 1150),
  ILLUMINA_RADIANCE("Illumina Aura ★", 1400),
}

data class SnapPhotoResult(
  val dexNumber: Int,
  val speciesName: String,
  val pose: SnapPoseState,
  val centeringScore: Int,
  val sizeScore: Int,
  val poseScore: Int,
  val illuminaBonus: Int,
  val totalScore: Int,
  val starRating: Int, // 1..4
  val tierLabel: String, // "1★ Bronze", "2★ Silver", "3★ Gold", "4★ Diamond"
)

data class LentalSnapState(
  val regionId: String = "lental",
  val subjectDex: Int = 25,
  val subjectPool: List<Int> = listOf(25, 131, 133, 249, 384, 658),
  val subjectPoolIndex: Int = 0,
  val subjectX: Float = 0.5f, // 0.12..0.88
  val subjectY: Float = 0.52f, // 0.25..0.78
  val depthScale: Float = 1.0f, // 0.60..1.35
  val poseState: SnapPoseState = SnapPoseState.CRUISING,
  val illuminaTimerTicks: Int = 0,
  val appleTimerTicks: Int = 0,
  val tickCount: Int = 0,
  val orbsRemaining: Int = 6,
  val applesRemaining: Int = 6,
  val shotsTaken: List<SnapPhotoResult> = emptyList(),
  val lastShot: SnapPhotoResult? = null,
)

object LentalSnapEngine {

  fun createSession(regionId: String): LentalSnapState {
    val chapter = MasterCanonTemplateCatalog.worldChapters.firstOrNull { it.id == regionId }
      ?: MasterCanonTemplateCatalog.worldChapters.first()
    val pool = (chapter.encounterPoolDexIds + NationalAtlasIndex.dexPoolForRegion(chapter.id).take(8))
      .distinct()
      .take(10)
      .ifEmpty { listOf(25, 131, 133, 249, 384) }
    return LentalSnapState(
      regionId = chapter.id,
      subjectDex = pool.first(),
      subjectPool = pool,
      subjectPoolIndex = 0,
    )
  }

  fun stepFrame(state: LentalSnapState): LentalSnapState {
    val nextTick = state.tickCount + 1
    val phase = nextTick * 0.16
    val applePull = if (state.appleTimerTicks > 0) 0.35f else 1.0f
    val x = (0.5f + sin(phase).toFloat() * 0.30f * applePull).coerceIn(0.14f, 0.86f)
    val y = (0.52f + sin(phase * 1.7 + 0.8).toFloat() * 0.14f * applePull).coerceIn(0.24f, 0.78f)
    val scale = (0.98f + sin(phase * 0.75).toFloat() * 0.24f).coerceIn(0.62f, 1.32f)

    val nextIllumina = (state.illuminaTimerTicks - 1).coerceAtLeast(0)
    val nextApple = (state.appleTimerTicks - 1).coerceAtLeast(0)
    val nextPose = when {
      nextIllumina > 0 -> SnapPoseState.ILLUMINA_RADIANCE
      nextApple > 0 -> SnapPoseState.APPLE_DELIGHT
      nextTick % 18 in 0..5 -> SnapPoseState.HAPPY_HOP
      else -> SnapPoseState.CRUISING
    }
    return state.copy(
      subjectX = x,
      subjectY = y,
      depthScale = scale,
      poseState = nextPose,
      illuminaTimerTicks = nextIllumina,
      appleTimerTicks = nextApple,
      tickCount = nextTick,
    )
  }

  fun tossIlluminaOrb(state: LentalSnapState): LentalSnapState {
    if (state.orbsRemaining <= 0) return state
    return state.copy(
      orbsRemaining = state.orbsRemaining - 1,
      illuminaTimerTicks = 28,
      poseState = SnapPoseState.ILLUMINA_RADIANCE,
    )
  }

  fun tossApple(state: LentalSnapState): LentalSnapState {
    if (state.applesRemaining <= 0) return state
    return state.copy(
      applesRemaining = state.applesRemaining - 1,
      appleTimerTicks = 26,
      subjectX = (state.subjectX + 0.5f) * 0.5f,
      subjectY = (state.subjectY + 0.52f) * 0.5f,
      poseState = if (state.illuminaTimerTicks > 0) SnapPoseState.ILLUMINA_RADIANCE else SnapPoseState.APPLE_DELIGHT,
    )
  }

  fun nextSubject(state: LentalSnapState): LentalSnapState {
    if (state.subjectPool.isEmpty()) return state
    val nextIdx = (state.subjectPoolIndex + 1) % state.subjectPool.size
    return state.copy(
      subjectPoolIndex = nextIdx,
      subjectDex = state.subjectPool[nextIdx],
      illuminaTimerTicks = 0,
      appleTimerTicks = 0,
      poseState = SnapPoseState.CRUISING,
      orbsRemaining = 6,
      applesRemaining = 6,
    )
  }

  fun capturePhoto(
    state: LentalSnapState,
    speciesName: String,
    timeOfDay: TimeOfDayCycle = TimeOfDayCycle.DAY,
  ): Pair<LentalSnapState, SnapPhotoResult> {
    val dist = hypot((state.subjectX - 0.5f).toDouble(), (state.subjectY - 0.5f).toDouble()).toFloat()
    val centeringScore = ((1f - (dist / 0.45f).coerceIn(0f, 1f)) * 1200f).roundToInt()
    val sizeDeviation = abs(state.depthScale - 1.05f)
    val sizeScore = ((1f - (sizeDeviation / 0.55f).coerceIn(0f, 1f)) * 1200f).roundToInt()
    val poseScore = state.poseState.posePoints
    val illuminaBonus = if (state.illuminaTimerTicks > 0) {
      if (timeOfDay == TimeOfDayCycle.NIGHT) 1000 else 800
    } else if (state.appleTimerTicks > 0) {
      350
    } else {
      0
    }
    val total = centeringScore + sizeScore + poseScore + illuminaBonus
    val (stars, tier) = when {
      total >= 4000 -> 4 to "4★ Diamond"
      total >= 3100 -> 3 to "3★ Gold"
      total >= 2100 -> 2 to "2★ Silver"
      else -> 1 to "1★ Bronze"
    }
    val shot = SnapPhotoResult(
      dexNumber = state.subjectDex,
      speciesName = speciesName,
      pose = state.poseState,
      centeringScore = centeringScore,
      sizeScore = sizeScore,
      poseScore = poseScore,
      illuminaBonus = illuminaBonus,
      totalScore = total,
      starRating = stars,
      tierLabel = tier,
    )
    val nextState = state.copy(
      lastShot = shot,
      shotsTaken = (listOf(shot) + state.shotsTaken).take(8),
    )
    return nextState to shot
  }
}

// =================================================================================================
// 4. MYSTERY RIFT DUNGEON CRAWL (Procedural 5-Floor 10x10 Roguelite + Time Gears + Rift Guardian)
// =================================================================================================

enum class RiftTile(val code: Int, val passable: Boolean) {
  WALL(0, false),
  FLOOR(1, true),
  STAIRS(2, true),
  TIME_GEAR(3, true),
  ORAN_BERRY(4, true),
  SPIKE_TRAP(5, true),
}

data class RiftEnemy(
  val id: String,
  val dexNumber: Int,
  val name: String,
  val row: Int,
  val col: Int,
  val hp: Int,
  val maxHp: Int,
  val attackPower: Int,
  val isFloorGuardian: Boolean = false,
)

data class MysteryRiftFloor(
  val floorNumber: Int, // 1..5
  val gridSize: Int = 10,
  val tiles: List<RiftTile>,
  val spawnRow: Int,
  val spawnCol: Int,
  val stairsRow: Int,
  val stairsCol: Int,
  val enemies: List<RiftEnemy>,
)

data class MysteryRiftState(
  val isActive: Boolean = false,
  val seed: Int = 101,
  val regionId: String = "mystery_rift",
  val floorNumber: Int = 1,
  val maxFloors: Int = 5,
  val gridSize: Int = 10,
  val tiles: List<RiftTile> = emptyList(),
  val playerRow: Int = 1,
  val playerCol: Int = 1,
  val playerHp: Int = 120,
  val playerMaxHp: Int = 120,
  val playerLevel: Int = 1,
  val timeGearsCollected: Int = 0,
  val oranBerriesHeld: Int = 2,
  val enemies: List<RiftEnemy> = emptyList(),
  val dungeonCleared: Boolean = false,
  val playerDefeated: Boolean = false,
  val riftLog: List<String> = listOf("Enter the 5-Floor Mystery Rift! Find the glowing Stairs and defeat the Floor-5 Guardian."),
)

data class MysteryRiftOutcome(
  val nextState: MysteryRiftState,
  val reachedNewFloor: Int? = null,
  val guardianDefeated: Boolean = false,
  val sfxCue: StudioSfxType? = null,
)

object MysteryRiftEngine {
  const val GRID_SIZE = 10

  fun generateFloor(
    floorNumber: Int,
    seed: Int,
    regionId: String,
    resolveName: (Int) -> String = { dex -> NationalAtlasIndex.peekSpecies(dex).name },
  ): MysteryRiftFloor {
    val grid = MutableList(GRID_SIZE * GRID_SIZE) { RiftTile.WALL }
    val rng = java.util.Random((seed * 7919L) + floorNumber * 104729L)

    fun carveRect(r0: Int, c0: Int, r1: Int, c1: Int) {
      for (r in r0.coerceIn(1, GRID_SIZE - 2)..r1.coerceIn(1, GRID_SIZE - 2)) {
        for (c in c0.coerceIn(1, GRID_SIZE - 2)..c1.coerceIn(1, GRID_SIZE - 2)) {
          grid[r * GRID_SIZE + c] = RiftTile.FLOOR
        }
      }
    }

    fun carveCorridor(r0: Int, c0: Int, r1: Int, c1: Int) {
      var r = r0.coerceIn(1, GRID_SIZE - 2)
      var c = c0.coerceIn(1, GRID_SIZE - 2)
      val tr = r1.coerceIn(1, GRID_SIZE - 2)
      val tc = c1.coerceIn(1, GRID_SIZE - 2)
      grid[r * GRID_SIZE + c] = RiftTile.FLOOR
      while (c != tc) {
        c += if (tc > c) 1 else -1
        grid[r * GRID_SIZE + c] = RiftTile.FLOOR
      }
      while (r != tr) {
        r += if (tr > r) 1 else -1
        grid[r * GRID_SIZE + c] = RiftTile.FLOOR
      }
    }

    // Carve 4 connected rooms in quadrants so every floor has organic roguelite variety & guaranteed reachability
    val rA = 1 + rng.nextInt(2)
    val cA = 1 + rng.nextInt(2)
    carveRect(rA, cA, rA + 2, cA + 2)

    val rB = 1 + rng.nextInt(2)
    val cB = 5 + rng.nextInt(2)
    carveRect(rB, cB, rB + 2, cB + 2)

    val rC = 5 + rng.nextInt(2)
    val cC = 1 + rng.nextInt(2)
    carveRect(rC, cC, rC + 2, cC + 2)

    val rD = 5 + rng.nextInt(2)
    val cD = 5 + rng.nextInt(2)
    carveRect(rD, cD, rD + 2, cD + 2)

    carveCorridor(rA + 1, cA + 1, rB + 1, cB + 1)
    carveCorridor(rB + 1, cB + 1, rD + 1, cD + 1)
    carveCorridor(rA + 1, cA + 1, rC + 1, cC + 1)
    carveCorridor(rC + 1, cC + 1, rD + 1, cD + 1)

    val spawnRow = rA + 1
    val spawnCol = cA + 1
    val stairsRow = rD + 1
    val stairsCol = cD + 1
    if (floorNumber < 5) {
      grid[stairsRow * GRID_SIZE + stairsCol] = RiftTile.STAIRS
    }

    // Collect open candidate cells excluding spawn and stairs
    val openCells = mutableListOf<Pair<Int, Int>>()
    for (r in 1 until GRID_SIZE - 1) {
      for (c in 1 until GRID_SIZE - 1) {
        if (grid[r * GRID_SIZE + c] == RiftTile.FLOOR &&
          (r != spawnRow || c != spawnCol) &&
          (r != stairsRow || c != stairsCol)
        ) {
          openCells += r to c
        }
      }
    }
    openCells.shuffle(rng)

    // Scatter Time Gears, Oran Berries, and Traps
    var cursor = 0
    repeat(2) {
      if (cursor < openCells.size) {
        val (r, c) = openCells[cursor++]
        grid[r * GRID_SIZE + c] = RiftTile.TIME_GEAR
      }
    }
    if (cursor < openCells.size) {
      val (r, c) = openCells[cursor++]
      grid[r * GRID_SIZE + c] = RiftTile.ORAN_BERRY
    }
    repeat(2) {
      if (cursor < openCells.size) {
        val (r, c) = openCells[cursor++]
        grid[r * GRID_SIZE + c] = RiftTile.SPIKE_TRAP
      }
    }

    // Spawn roaming Rift enemies (and Floor-5 Rift Guardian on the boss room!)
    val pool = NationalAtlasIndex.dexPoolForRegion(regionId).ifEmpty { listOf(94, 445, 448, 491, 487) }
    val enemies = mutableListOf<RiftEnemy>()
    val foeCount = if (floorNumber == 5) 2 else (2 + (floorNumber % 2))
    repeat(foeCount) { idx ->
      if (cursor < openCells.size) {
        val (r, c) = openCells[cursor++]
        val dex = pool[(seed + floorNumber * 7 + idx * 13) % pool.size]
        val maxHp = 38 + floorNumber * 14
        enemies += RiftEnemy(
          id = "foe_${floorNumber}_$idx",
          dexNumber = dex,
          name = resolveName(dex),
          row = r,
          col = c,
          hp = maxHp,
          maxHp = maxHp,
          attackPower = 8 + floorNumber * 3,
        )
      }
    }
    if (floorNumber == 5) {
      val guardianDex = 487 // Giratina-inspired Rift Guardian
      enemies += RiftEnemy(
        id = "rift_guardian_f5",
        dexNumber = guardianDex,
        name = "Rift Guardian ${resolveName(guardianDex)}",
        row = stairsRow,
        col = stairsCol,
        hp = 145,
        maxHp = 145,
        attackPower = 20,
        isFloorGuardian = true,
      )
    }

    return MysteryRiftFloor(
      floorNumber = floorNumber,
      gridSize = GRID_SIZE,
      tiles = grid,
      spawnRow = spawnRow,
      spawnCol = spawnCol,
      stairsRow = stairsRow,
      stairsCol = stairsCol,
      enemies = enemies,
    )
  }

  /** BFS reachability verifier (also asserted directly in unit tests). */
  fun isTargetReachable(floor: MysteryRiftFloor, targetRow: Int, targetCol: Int): Boolean {
    val visited = BooleanArray(floor.gridSize * floor.gridSize)
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.add(floor.spawnRow to floor.spawnCol)
    visited[floor.spawnRow * floor.gridSize + floor.spawnCol] = true
    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    while (queue.isNotEmpty()) {
      val (r, c) = queue.removeFirst()
      if (r == targetRow && c == targetCol) return true
      for ((dr, dc) in dirs) {
        val nr = r + dr
        val nc = c + dc
        if (nr in 0 until floor.gridSize && nc in 0 until floor.gridSize) {
          val idx = nr * floor.gridSize + nc
          if (!visited[idx] && floor.tiles[idx].passable) {
            visited[idx] = true
            queue.add(nr to nc)
          }
        }
      }
    }
    return false
  }

  fun startDungeon(
    regionId: String,
    seed: Int = 101,
    resolveName: (Int) -> String = { dex -> NationalAtlasIndex.peekSpecies(dex).name },
  ): MysteryRiftState {
    val f1 = generateFloor(1, seed, regionId, resolveName)
    return MysteryRiftState(
      isActive = true,
      seed = seed,
      regionId = regionId,
      floorNumber = 1,
      tiles = f1.tiles,
      playerRow = f1.spawnRow,
      playerCol = f1.spawnCol,
      playerHp = 120,
      playerMaxHp = 120,
      playerLevel = 1,
      timeGearsCollected = 0,
      oranBerriesHeld = 2,
      enemies = f1.enemies,
      riftLog = listOf("Entered Mystery Rift B1F! Collect Time Gears and locate the glowing Stairs."),
    )
  }

  fun useOranBerry(state: MysteryRiftState): MysteryRiftOutcome {
    if (!state.isActive || state.dungeonCleared || state.playerDefeated || state.oranBerriesHeld <= 0) {
      return MysteryRiftOutcome(state)
    }
    val healedHp = (state.playerHp + 55).coerceAtMost(state.playerMaxHp)
    return MysteryRiftOutcome(
      nextState = state.copy(
        playerHp = healedHp,
        oranBerriesHeld = state.oranBerriesHeld - 1,
        riftLog = (listOf("Ate an Oran Berry! Restored HP to $healedHp/${state.playerMaxHp}.") + state.riftLog).take(12),
      ),
      sfxCue = StudioSfxType.HEAL_CHIME,
    )
  }

  fun stepOrAttack(
    state: MysteryRiftState,
    dRow: Int,
    dCol: Int,
    useSignatureSkill: Boolean = false,
    resolveName: (Int) -> String = { dex -> NationalAtlasIndex.peekSpecies(dex).name },
  ): MysteryRiftOutcome {
    if (!state.isActive || state.dungeonCleared || state.playerDefeated) return MysteryRiftOutcome(state)

    val logs = mutableListOf<String>()
    var pRow = state.playerRow
    var pCol = state.playerCol
    var pHp = state.playerHp
    var pLevel = state.playerLevel
    var pMaxHp = state.playerMaxHp
    var gears = state.timeGearsCollected
    var berries = state.oranBerriesHeld
    val tiles = state.tiles.toMutableList()
    val enemies = state.enemies.toMutableList()
    var guardianCleared = false
    var sfx: StudioSfxType? = null

    if (useSignatureSkill) {
      // Signature Prism Pulse hits all adjacent enemies in 1-tile radius
      val adjacent = enemies.filter { abs(it.row - pRow) + abs(it.col - pCol) <= 2 }
      if (adjacent.isEmpty()) {
        logs.add("Signature Prism Pulse echoed through the chamber—no foes within 2 tiles!")
      } else {
        val skillDmg = 34 + pLevel * 7
        val iter = enemies.listIterator()
        while (iter.hasNext()) {
          val foe = iter.next()
          if (abs(foe.row - pRow) + abs(foe.col - pCol) <= 2) {
            val rem = foe.hp - skillDmg
            if (rem <= 0) {
              iter.remove()
              pLevel += 1
              pMaxHp += 8
              pHp = (pHp + 14).coerceAtMost(pMaxHp)
              logs.add("Prism Pulse defeated ${foe.name}! Lv.$pLevel (+14 HP).")
              if (foe.isFloorGuardian) guardianCleared = true
            } else {
              iter.set(foe.copy(hp = rem))
              logs.add("Prism Pulse blasted ${foe.name} for $skillDmg dmg ($rem/${foe.maxHp} HP)!")
            }
          }
        }
        sfx = StudioSfxType.HIT_HEAVY_CRIT
      }
    } else {
      val nr = pRow + dRow
      val nc = pCol + dCol
      if (nr !in 0 until state.gridSize || nc !in 0 until state.gridSize) return MysteryRiftOutcome(state)
      val bumpedIdx = enemies.indexOfFirst { it.row == nr && it.col == nc }
      if (bumpedIdx >= 0) {
        val foe = enemies[bumpedIdx]
        val dmg = 26 + pLevel * 5
        val rem = foe.hp - dmg
        sfx = StudioSfxType.HIT_LIGHT
        if (rem <= 0) {
          enemies.removeAt(bumpedIdx)
          pLevel += 1
          pMaxHp += 8
          pHp = (pHp + 12).coerceAtMost(pMaxHp)
          logs.add("Defeated ${foe.name}! Reached Rift Lv.$pLevel (+12 HP).")
          if (foe.isFloorGuardian) guardianCleared = true
        } else {
          enemies[bumpedIdx] = foe.copy(hp = rem)
          logs.add("Struck ${foe.name} for $dmg dmg ($rem/${foe.maxHp} HP)!")
        }
      } else {
        val targetTile = tiles[nr * state.gridSize + nc]
        if (!targetTile.passable) return MysteryRiftOutcome(state)
        pRow = nr
        pCol = nc
        when (targetTile) {
          RiftTile.TIME_GEAR -> {
            gears += 1
            tiles[nr * state.gridSize + nc] = RiftTile.FLOOR
            logs.add("Collected a gleaming Time Gear! (Total: $gears)")
            sfx = StudioSfxType.CAPTURE_SPARKLE
          }
          RiftTile.ORAN_BERRY -> {
            berries += 1
            tiles[nr * state.gridSize + nc] = RiftTile.FLOOR
            logs.add("Found an Oran Berry! (Bag: $berries)")
            sfx = StudioSfxType.HEAL_CHIME
          }
          RiftTile.SPIKE_TRAP -> {
            pHp = (pHp - 14).coerceAtLeast(0)
            tiles[nr * state.gridSize + nc] = RiftTile.FLOOR
            logs.add("Triggered a Hidden Spike Trap (-14 HP)!")
            sfx = StudioSfxType.HIT_LIGHT
          }
          RiftTile.STAIRS -> {
            val nextFloorNum = (state.floorNumber + 1).coerceAtMost(state.maxFloors)
            val nextFloor = generateFloor(nextFloorNum, state.seed, state.regionId, resolveName)
            val nextLog = if (nextFloorNum == state.maxFloors) {
              "Descended to B5F! The Rift Guardian awaits in the final chamber!"
            } else {
              "Descended the glowing Stairs to Mystery Rift B${nextFloorNum}F!"
            }
            return MysteryRiftOutcome(
              nextState = state.copy(
                floorNumber = nextFloorNum,
                tiles = nextFloor.tiles,
                playerRow = nextFloor.spawnRow,
                playerCol = nextFloor.spawnCol,
                playerHp = (pHp + 18).coerceAtMost(pMaxHp),
                playerMaxHp = pMaxHp,
                playerLevel = pLevel,
                timeGearsCollected = gears,
                oranBerriesHeld = berries,
                enemies = nextFloor.enemies,
                riftLog = (listOf(nextLog) + state.riftLog).take(12),
              ),
              reachedNewFloor = nextFloorNum,
              sfxCue = StudioSfxType.DOOR_WARP_SWOOSH,
            )
          }
          else -> Unit
        }
      }
    }

    if (guardianCleared) {
      gears += 3
      logs.add("RIFT CLEARED! Floor-5 Rift Guardian vanquished (+3 Time Gears)! Total: $gears.")
      return MysteryRiftOutcome(
        nextState = state.copy(
          tiles = tiles,
          playerRow = pRow,
          playerCol = pCol,
          playerHp = pHp,
          playerMaxHp = pMaxHp,
          playerLevel = pLevel,
          timeGearsCollected = gears,
          oranBerriesHeld = berries,
          enemies = enemies,
          dungeonCleared = true,
          riftLog = (logs.reversed() + state.riftLog).take(12),
        ),
        reachedNewFloor = state.floorNumber,
        guardianDefeated = true,
        sfxCue = StudioSfxType.LEVEL_UP,
      )
    }

    // Roaming Enemy AI Turn (move closer or strike if adjacent)
    val occupied = enemies.map { it.row to it.col }.toMutableSet()
    for (i in enemies.indices) {
      val foe = enemies[i]
      val manhattan = abs(foe.row - pRow) + abs(foe.col - pCol)
      if (manhattan == 1) {
        pHp = (pHp - foe.attackPower).coerceAtLeast(0)
        logs.add("${foe.name} attacked for ${foe.attackPower} dmg!")
      } else if (manhattan <= 4 && !foe.isFloorGuardian) {
        val stepR = if (pRow > foe.row) 1 else if (pRow < foe.row) -1 else 0
        val stepC = if (pCol > foe.col) 1 else if (pCol < foe.col) -1 else 0
        val cand = listOf(foe.row + stepR to foe.col, foe.row to foe.col + stepC)
          .firstOrNull { (rr, cc) ->
            rr in 0 until state.gridSize &&
              cc in 0 until state.gridSize &&
              tiles[rr * state.gridSize + cc].passable &&
              (rr != pRow || cc != pCol) &&
              (rr to cc) !in occupied
          }
        if (cand != null) {
          occupied.remove(foe.row to foe.col)
          occupied.add(cand)
          enemies[i] = foe.copy(row = cand.first, col = cand.second)
        }
      }
    }

    val defeated = pHp <= 0
    if (defeated) {
      logs.add("You fainted on B${state.floorNumber}F! Rescued back to Base Camp.")
    }
    return MysteryRiftOutcome(
      nextState = state.copy(
        tiles = tiles,
        playerRow = pRow,
        playerCol = pCol,
        playerHp = pHp,
        playerMaxHp = pMaxHp,
        playerLevel = pLevel,
        timeGearsCollected = gears,
        oranBerriesHeld = berries,
        enemies = enemies,
        playerDefeated = defeated,
        riftLog = (logs.reversed() + state.riftLog).take(12),
      ),
      sfxCue = sfx,
    )
  }
}

// =================================================================================================
// 5. RANGER CAPTURE STYLER & PRISM CAMP BONDING (Fiore/Almia + Galar/Paldea-Inspired)
// =================================================================================================

data class RangerStylerState(
  val targetDex: Int = 448,
  val targetName: String = "Lucario",
  val targetX: Float = 0.5f,
  val targetY: Float = 0.5f,
  val isAgitated: Boolean = false,
  val tickCount: Int = 0,
  val accumulatedAngleRad: Float = 0f,
  val lastDragAngleRad: Float? = null,
  val loopsCompleted: Int = 0,
  val loopsRequired: Int = 4,
  val friendshipGauge: Float = 0f, // 0f..1f
  val stylerEnergy: Int = 100,
  val isCaptured: Boolean = false,
  val statusMessage: String = "Drag glowing Capture Loops around the wild Pokémon to fill its Friendship Ring!",
)

data class RangerLoopOutcome(
  val nextState: RangerStylerState,
  val justCompletedLoop: Boolean = false,
  val justCaptured: Boolean = false,
  val lineBroken: Boolean = false,
)

enum class CampActivityType(val label: String, val bondGain: Int, val healAll: Boolean) {
  ORAN_SNACK("Feed Oran Berries", bondGain = 14, healAll = true),
  PRISM_CURRY("Cook Prism Curry", bondGain = 24, healAll = true),
  PLAY_TOY("Pet & Play Toy", bondGain = 18, healAll = false),
}

data class PrismCampState(
  val lastActivityLabel: String = "Your 6-Pokémon squad is relaxing by the Prism Campfire!",
  val activePartyMoodBoostNonce: Int = 0,
  val featuredPartnerSlot: Int = 0,
)

object RangerCampEngine {

  fun createTrial(dexNumber: Int, name: String): RangerStylerState = RangerStylerState(
    targetDex = dexNumber,
    targetName = name,
    loopsRequired = if (dexNumber in setOf(150, 249, 384, 487, 643, 1007, 1008)) 6 else 4,
    statusMessage = "Circle $name with your Capture Styler to fill its Friendship Ring!",
  )

  fun stepWildMovement(state: RangerStylerState): RangerStylerState {
    if (state.isCaptured) return state
    val nextTick = state.tickCount + 1
    val phase = nextTick * 0.12
    val agitated = (nextTick % 45) in 34..42
    val amp = if (agitated) 0.22f else 0.14f
    val tx = (0.5f + sin(phase).toFloat() * amp).coerceIn(0.22f, 0.78f)
    val ty = (0.5f + sin(phase * 1.4 + 0.7).toFloat() * amp).coerceIn(0.22f, 0.78f)
    return state.copy(
      targetX = tx,
      targetY = ty,
      isAgitated = agitated,
      tickCount = nextTick,
    )
  }

  fun beginDrag(state: RangerStylerState, normX: Float, normY: Float): RangerStylerState {
    if (state.isCaptured) return state
    val angle = atan2((normY - state.targetY).toDouble(), (normX - state.targetX).toDouble()).toFloat()
    return state.copy(lastDragAngleRad = angle, accumulatedAngleRad = 0f)
  }

  /**
   * Processes one normalized touch-drag point `(normX, normY)` in `0..1`.
   * Tracks signed angular sweep around `(targetX, targetY)`; each 2π sweep fills the Friendship Ring!
   */
  fun stepLoopDrag(state: RangerStylerState, normX: Float, normY: Float): RangerLoopOutcome {
    if (state.isCaptured) return RangerLoopOutcome(state)
    val dist = hypot((normX - state.targetX).toDouble(), (normY - state.targetY).toDouble()).toFloat()
    // Direct collision during agitated charge breaks current loop safely
    if (state.isAgitated && dist < 0.085f) {
      val nextEnergy = (state.stylerEnergy - 12).coerceAtLeast(20)
      return RangerLoopOutcome(
        nextState = state.copy(
          accumulatedAngleRad = 0f,
          lastDragAngleRad = null,
          stylerEnergy = nextEnergy,
          statusMessage = "${state.targetName} charged through the Capture Line! Keep circling safely!",
        ),
        lineBroken = true,
      )
    }

    val angle = atan2((normY - state.targetY).toDouble(), (normX - state.targetX).toDouble()).toFloat()
    val prev = state.lastDragAngleRad ?: return RangerLoopOutcome(state.copy(lastDragAngleRad = angle))

    var delta = angle - prev
    while (delta > PI) delta -= (2.0 * PI).toFloat()
    while (delta < -PI) delta += (2.0 * PI).toFloat()

    // Require reasonable loop distance so tiny jitters on top of center don't fake loops
    val accum = if (dist >= 0.07f) state.accumulatedAngleRad + delta else state.accumulatedAngleRad
    val fullCircle = (2.0 * PI).toFloat()
    if (abs(accum) >= fullCircle * 0.92f) {
      val nextLoops = state.loopsCompleted + 1
      val nextGauge = (nextLoops.toFloat() / state.loopsRequired.toFloat()).coerceIn(0f, 1f)
      val captured = nextLoops >= state.loopsRequired
      val msg = if (captured) {
        "CAPTURE COMPLETE! ${state.targetName} calmed down and joined your Prism Bond!"
      } else {
        "Loop $nextLoops/${state.loopsRequired}! Friendship Ring ${(nextGauge * 100).toInt()}%!"
      }
      return RangerLoopOutcome(
        nextState = state.copy(
          accumulatedAngleRad = 0f,
          lastDragAngleRad = angle,
          loopsCompleted = nextLoops,
          friendshipGauge = nextGauge,
          isCaptured = captured,
          statusMessage = msg,
        ),
        justCompletedLoop = true,
        justCaptured = captured,
      )
    }

    val partialProgress = (abs(accum) / fullCircle).coerceIn(0f, 0.95f)
    val gauge = ((state.loopsCompleted + partialProgress) / state.loopsRequired.toFloat()).coerceIn(state.friendshipGauge, 0.99f)
    return RangerLoopOutcome(
      nextState = state.copy(
        accumulatedAngleRad = accum,
        lastDragAngleRad = angle,
        friendshipGauge = gauge,
      ),
    )
  }

  fun endDrag(state: RangerStylerState): RangerStylerState =
    state.copy(lastDragAngleRad = null, accumulatedAngleRad = 0f)

  fun performCampActivity(
    currentBondGauge: Int,
    activity: CampActivityType,
    partnerName: String,
    campState: PrismCampState,
  ): Pair<Int, PrismCampState> {
    val nextBond = (currentBondGauge + activity.bondGain).coerceIn(0, 100)
    val msg = when (activity) {
      CampActivityType.ORAN_SNACK -> "Shared Oran Berry Snacks with $partnerName & squad! HP/Status restored • Bond $nextBond%."
      CampActivityType.PRISM_CURRY -> "Served Charizard-Class Prism Curry! Full squad revitalized • Bond $nextBond%!"
      CampActivityType.PLAY_TOY -> "Played Prism Feather with $partnerName! Hearts flying • Bond $nextBond%!"
    }
    return nextBond to campState.copy(
      lastActivityLabel = msg,
      activePartyMoodBoostNonce = campState.activePartyMoodBoostNonce + 1,
    )
  }
}

// =================================================================================================
// COMBINED SIDE MODES SESSION WRAPPER (Hoisted cleanly in StudioViewModel)
// =================================================================================================

data class SideModesSessionState(
  val activeMode: SideModeType = SideModeType.BATTLE_TOWER,
  val tower: BattleTowerState = BattleTowerState(),
  val contest: PrismContestState = PrismContestState(),
  val snap: LentalSnapState = LentalSnapEngine.createSession("lental"),
  val rift: MysteryRiftState = MysteryRiftState(),
  val ranger: RangerStylerState = RangerCampEngine.createTrial(448, "Lucario"),
  val camp: PrismCampState = PrismCampState(),
  val record: SideModeRecordEntity = SideModeRecordEntity(projectId = "prism_resonance_master_v4"),
)

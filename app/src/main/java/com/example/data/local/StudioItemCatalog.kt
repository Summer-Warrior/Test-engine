package com.example.data.local

/**
 * Complete clean-room Item System for Pokémon: Prism Resonance.
 * Every Poké Ball, Medicine, Hold Item, Berry, Evolution Item, TM, Gimmick Catalyst,
 * and Story Key Item drives live behavior in GameTestSection and CanonBattleResolver.
 */
object StudioItemCatalog {
  const val CAT_POKE_BALL = "POKE_BALL"
  const val CAT_MEDICINE = "MEDICINE"
  const val CAT_HOLD_ITEM = "HOLD_ITEM"
  const val CAT_EVOLUTION = "EVOLUTION"
  const val CAT_BERRY = "BERRY"
  const val CAT_TM = "TM"
  const val CAT_GIMMICK_CATALYST = "GIMMICK_CATALYST"
  const val CAT_KEY_ITEM = "KEY_ITEM"

  val allCategories: List<Pair<String, String>> = listOf(
    CAT_POKE_BALL to "Poké Balls",
    CAT_MEDICINE to "Medicine",
    CAT_HOLD_ITEM to "Hold Items",
    CAT_BERRY to "Berries",
    CAT_TM to "TMs",
    CAT_EVOLUTION to "Evolution",
    CAT_GIMMICK_CATALYST to "Catalysts",
    CAT_KEY_ITEM to "Key Items",
  )

  val defaultItems: List<StudioItemEntity> = listOf(
    // 13 Poké Balls (real capture-rate multipliers)
    StudioItemEntity("ball_poke", "Poké Ball", CAT_POKE_BALL, "Standard capsule for catching wild species (1.0x rate).", "CATCH:1.0", 1.0f, 15, "#EF4444", "Kanto"),
    StudioItemEntity("ball_great", "Great Ball", CAT_POKE_BALL, "High-performance capsule with 1.5x catch rate.", "CATCH:1.5", 1.5f, 10, "#38BDF8", "Kanto"),
    StudioItemEntity("ball_ultra", "Ultra Ball", CAT_POKE_BALL, "Elite capsule with 2.0x catch rate.", "CATCH:2.0", 2.0f, 8, "#FFD166", "Kanto"),
    StudioItemEntity("ball_master", "Master Ball", CAT_POKE_BALL, "Guaranteed 100% catch rate without fail.", "CATCH:255.0", 255.0f, 2, "#A855F7", "Kanto"),
    StudioItemEntity("ball_net", "Net Ball", CAT_POKE_BALL, "3.5x catch rate on Water- and Bug-type targets.", "CATCH_NET:3.5", 3.5f, 6, "#2DD4BF", "Hoenn"),
    StudioItemEntity("ball_dusk", "Dusk Ball", CAT_POKE_BALL, "3.0x catch rate in caves, rift zones, or Shadow weather.", "CATCH_DUSK:3.0", 3.0f, 6, "#10B981", "Sinnoh"),
    StudioItemEntity("ball_quick", "Quick Ball", CAT_POKE_BALL, "5.0x catch rate if thrown on Turn 1 of battle.", "CATCH_QUICK:5.0", 5.0f, 6, "#FACC15", "Sinnoh"),
    StudioItemEntity("ball_timer", "Timer Ball", CAT_POKE_BALL, "Scales up to 4.0x catch rate on longer battles.", "CATCH_TIMER:4.0", 4.0f, 5, "#F8FAFC", "Hoenn"),
    StudioItemEntity("ball_dive", "Dive Ball", CAT_POKE_BALL, "3.5x catch rate near ocean/water sectors or Water types.", "CATCH_DIVE:3.5", 3.5f, 5, "#00B4D8", "Hoenn"),
    StudioItemEntity("ball_repeat", "Repeat Ball", CAT_POKE_BALL, "3.5x catch rate on species already captured in the Atlas Dex.", "CATCH_REPEAT:3.5", 3.5f, 5, "#F97316", "Hoenn"),
    StudioItemEntity("ball_luxury", "Luxury Ball", CAT_POKE_BALL, "Comfortable 1.25x capsule that boosts Bond/Friendship gain.", "CATCH_LUXURY:1.25", 1.25f, 5, "#F59E0B", "Hoenn"),
    StudioItemEntity("ball_premier", "Premier Ball", CAT_POKE_BALL, "Commemorative white capsule with crisp sparkle aura (1.1x).", "CATCH:1.1", 1.1f, 6, "#E2E8F0", "Hoenn"),
    StudioItemEntity("ball_prism", "Prism Resonance Ball", CAT_POKE_BALL, "Original studio capsule tuned to Harmonic Rifts (4.0x on Rift/Tera encounters).", "CATCH_PRISM:4.0", 4.0f, 4, "#00F5D4", "Prism Nexus"),

    // Medicine (8 items)
    StudioItemEntity("med_potion", "Potion", CAT_MEDICINE, "Restores 35 HP to the active Pokémon.", "HEAL_HP:35", 35f, 10, "#A855F7", "All Regions"),
    StudioItemEntity("med_super_potion", "Super Potion", CAT_MEDICINE, "Restores 75 HP to the active Pokémon.", "HEAL_HP:75", 75f, 8, "#EF4444", "All Regions"),
    StudioItemEntity("med_hyper_potion", "Hyper Potion", CAT_MEDICINE, "Restores 140 HP to the active Pokémon.", "HEAL_HP:140", 140f, 5, "#EC4899", "All Regions"),
    StudioItemEntity("med_max_potion", "Max Potion", CAT_MEDICINE, "Restores 100% of active Pokémon's HP.", "HEAL_FULL_HP", 999f, 4, "#38BDF8", "All Regions"),
    StudioItemEntity("med_full_restore", "Full Restore", CAT_MEDICINE, "Fully restores HP and cures all status conditions.", "HEAL_FULL_ALL", 999f, 4, "#10B981", "All Regions"),
    StudioItemEntity("med_revive", "Revive", CAT_MEDICINE, "Revives a fainted ally with 50% max HP.", "REVIVE_HALF", 0.5f, 4, "#FFD166", "All Regions"),
    StudioItemEntity("med_full_heal", "Full Heal", CAT_MEDICINE, "Cures Burn, Paralysis, Poison, Sleep, or Freeze.", "CURE_STATUS", 1f, 6, "#FACC15", "All Regions"),
    StudioItemEntity("med_rare_candy", "Rare Candy", CAT_MEDICINE, "Grants +5 temporary combat level power boost for this playtest.", "LEVEL_UP:5", 5f, 3, "#00F5D4", "All Regions"),

    // Battle & Hold Items (10 competitive items wired directly into CanonBattleResolver!)
    StudioItemEntity("choice_band", "Choice Band", CAT_HOLD_ITEM, "Boosts Physical Attack by 1.5x (locks move focus).", "HOLD_ATK:1.5", 1.5f, 2, "#EF4444", "Hoenn"),
    StudioItemEntity("choice_specs", "Choice Specs", CAT_HOLD_ITEM, "Boosts Special Attack by 1.5x (locks move focus).", "HOLD_SPA:1.5", 1.5f, 2, "#38BDF8", "Sinnoh"),
    StudioItemEntity("choice_scarf", "Choice Scarf", CAT_HOLD_ITEM, "Boosts Speed by 1.5x for turn-order advantage.", "HOLD_SPE:1.5", 1.5f, 2, "#00F5D4", "Sinnoh"),
    StudioItemEntity("life_orb", "Life Orb", CAT_HOLD_ITEM, "Boosts all move damage by 1.3x at 10% max HP recoil.", "HOLD_LIFE_ORB:1.3", 1.3f, 2, "#FF2A85", "Sinnoh"),
    StudioItemEntity("focus_sash", "Focus Sash", CAT_HOLD_ITEM, "Survives a lethal hit from full HP with 1 HP remaining.", "HOLD_FOCUS_SASH:1.0", 1.0f, 2, "#F97316", "Sinnoh"),
    StudioItemEntity("leftovers", "Leftovers", CAT_HOLD_ITEM, "Restores 1/16 max HP at the end of every battle turn.", "HOLD_LEFTOVERS:0.0625", 0.0625f, 2, "#10B981", "Johto"),
    StudioItemEntity("assault_vest", "Assault Vest", CAT_HOLD_ITEM, "Boosts Special Defense by 1.5x against special attacks.", "HOLD_SPD:1.5", 1.5f, 2, "#A855F7", "Kalos"),
    StudioItemEntity("heavy_duty_boots", "Heavy-Duty Boots", CAT_HOLD_ITEM, "Protects bearer from entry hazards (Stealth Rock / Spikes).", "HOLD_BOOTS:1.0", 1.0f, 2, "#94A3B8", "Galar"),
    StudioItemEntity("weakness_policy", "Weakness Policy", CAT_HOLD_ITEM, "Boosts Atk & Sp.Atk when hit by a Super-Effective move (+2 stages).", "HOLD_WP:2.0", 2.0f, 2, "#FFD166", "Kalos"),
    StudioItemEntity("eviolite", "Eviolite", CAT_HOLD_ITEM, "Boosts Def & Sp.Def by 1.5x for not-fully-evolved Pokémon.", "HOLD_EVIOLITE:1.5", 1.5f, 2, "#C084FC", "Unova"),

    // Berries (4 items)
    StudioItemEntity("berry_oran", "Oran Berry", CAT_BERRY, "Restores 20 HP immediately when used or pinched.", "HEAL_HP:20", 20f, 8, "#38BDF8", "Hoenn"),
    StudioItemEntity("berry_sitrus", "Sitrus Berry", CAT_BERRY, "Restores 25% of max HP in combat.", "HEAL_PCT:0.25", 0.25f, 6, "#FFD166", "Hoenn"),
    StudioItemEntity("berry_lum", "Lum Berry", CAT_BERRY, "Instantly cures any non-volatile status condition.", "CURE_STATUS", 1f, 6, "#10B981", "Hoenn"),
    StudioItemEntity("berry_leppa", "Leppa Berry", CAT_BERRY, "Restores tactical energy / charges Aura & Z-Gauge.", "BOOST_GAUGE:20", 20f, 6, "#EF4444", "Hoenn"),

    // Evolution Stones & Items (8 items)
    StudioItemEntity("evo_fire_stone", "Fire Stone", CAT_EVOLUTION, "Radiates volcanic warmth; triggers Fire-branch evolution.", "EVO:Fire", 1f, 2, "#FF5722", "Kanto"),
    StudioItemEntity("evo_water_stone", "Water Stone", CAT_EVOLUTION, "Glimmers like deep tidewater; triggers Water evolution.", "EVO:Water", 1f, 2, "#00B4D8", "Kanto"),
    StudioItemEntity("evo_thunder_stone", "Thunder Stone", CAT_EVOLUTION, "Crackles with static arcs; evolves Pikachu and Eevee.", "EVO:Electric", 1f, 2, "#FFD166", "Kanto"),
    StudioItemEntity("evo_leaf_stone", "Leaf Stone", CAT_EVOLUTION, "Emerald botanical mineral for Grass evolutions.", "EVO:Grass", 1f, 2, "#10B981", "Kanto"),
    StudioItemEntity("evo_moon_stone", "Moon Stone", CAT_EVOLUTION, "Mt. Moon astral stone for lunar evolutions.", "EVO:Fairy", 1f, 2, "#A855F7", "Kanto"),
    StudioItemEntity("evo_dawn_stone", "Dawn Stone", CAT_EVOLUTION, "Sparkling Sinnoh crystal for branched evolutions.", "EVO:Psychic", 1f, 2, "#38BDF8", "Sinnoh"),
    StudioItemEntity("evo_dusk_stone", "Dusk Stone", CAT_EVOLUTION, "Shadow-tinted Sinnoh gem for Ghost/Dark evolutions.", "EVO:Ghost", 1f, 2, "#6366F1", "Sinnoh"),
    StudioItemEntity("evo_prism_scale", "Prism Scale", CAT_EVOLUTION, "Refracts harmonic light for radiant trade/link evolutions.", "EVO:Dragon", 1f, 2, "#00F5D4", "Unova"),

    // Signature TMs (8 TMs including Tera Blast)
    StudioItemEntity("tm_tera_blast", "TM171 Tera Blast", CAT_TM, "Changes type to match the user's active Tera Type when Terastallized.", "TM:Tera Blast|Normal|80", 80f, 1, "#00F5D4", "Paldea"),
    StudioItemEntity("tm_thunderbolt", "TM024 Thunderbolt", CAT_TM, "90-power Electric beam with 10% paralysis chance.", "TM:Thunderbolt|Electric|90", 90f, 1, "#FFD166", "Kanto"),
    StudioItemEntity("tm_ice_beam", "TM013 Ice Beam", CAT_TM, "90-power Ice ray with 10% freeze chance.", "TM:Ice Beam|Ice|90", 90f, 1, "#38BDF8", "Kanto"),
    StudioItemEntity("tm_earthquake", "TM026 Earthquake", CAT_TM, "100-power Ground tremor (blocked by Levitate/Flying).", "TM:Earthquake|Ground|100", 100f, 1, "#D97706", "Kanto"),
    StudioItemEntity("tm_flamethrower", "TM035 Flamethrower", CAT_TM, "90-power Fire stream with 10% burn chance.", "TM:Flamethrower|Fire|90", 90f, 1, "#FF5722", "Kanto"),
    StudioItemEntity("tm_shadow_ball", "TM030 Shadow Ball", CAT_TM, "80-power Ghost sphere with Sp.Def drop chance.", "TM:Shadow Ball|Ghost|80", 80f, 1, "#A855F7", "Johto"),
    StudioItemEntity("tm_dragon_pulse", "TM059 Dragon Pulse", CAT_TM, "85-power Draconic shockwave.", "TM:Dragon Pulse|Dragon|85", 85f, 1, "#6366F1", "Sinnoh"),
    StudioItemEntity("tm_aura_sphere", "TM112 Aura Sphere", CAT_TM, "80-power Fighting blast that never misses.", "TM:Aura Sphere|Fighting|80", 80f, 1, "#2DD4BF", "Sinnoh"),

    // Gimmick Catalysts (5)
    StudioItemEntity("cat_mega_ring", "Mega Keystone", CAT_GIMMICK_CATALYST, "Unlocks Mega Evolution resonance (+30% Atk/SpA/+20% Spe).", "GIMMICK:MEGA", 1f, 1, "#FF2A85", "Kalos"),
    StudioItemEntity("cat_z_ring", "Z-Power Ring", CAT_GIMMICK_CATALYST, "Channels Z-Crystal burst power (1.75x peak strike).", "GIMMICK:Z_MOVE", 1f, 1, "#FFD166", "Alola"),
    StudioItemEntity("cat_dynamax_band", "Dynamax Band", CAT_GIMMICK_CATALYST, "Doubles Max HP proportionally and upgrades attacks to Max Moves.", "GIMMICK:DYNAMAX", 1f, 1, "#EC4899", "Galar"),
    StudioItemEntity("cat_tera_orb", "Tera Orb", CAT_GIMMICK_CATALYST, "Shifts defensive type to Tera Type, unlocks 2.0x Tera STAB & Tera Blast shift.", "GIMMICK:TERASTAL", 1f, 1, "#00F5D4", "Paldea"),
    StudioItemEntity("cat_resonance_prism", "Resonance Prism", CAT_GIMMICK_CATALYST, "Unites hardware eras for Bond Phenomenon & Synergy Aura.", "GIMMICK:BOND", 1f, 1, "#A855F7", "Prism Nexus"),

    // Story Key Items: Prism Loom Compass + 22 Harmonic Shards
    StudioItemEntity("key_prism_compass", "Prism Loom Compass", CAT_KEY_ITEM, "Professor Lyra Vance's harmonic navigational device linking all 22 fused multi-source regions.", "KEY:COMPASS", 1f, 1, "#00F5D4", "Prism Nexus"),
    StudioItemEntity("shard_01_kanto", "Harmonic Shard I (Kanto)", CAT_KEY_ITEM, "Indigo Resonance Shard restoring 8-bit/SGB2 harmony across Kanto.", "SHARD:kanto", 1f, 0, "#EF4444", "Kanto"),
    StudioItemEntity("shard_02_johto", "Harmonic Shard II (Johto)", CAT_KEY_ITEM, "Bell Tower Shard harmonizing GBC Hi-Color day/night palettes.", "SHARD:johto", 1f, 0, "#FFD166", "Johto"),
    StudioItemEntity("shard_03_hoenn", "Harmonic Shard III (Hoenn)", CAT_KEY_ITEM, "Sky Pillar Shard stabilizing GBA Mode-7 weather currents.", "SHARD:hoenn", 1f, 0, "#10B981", "Hoenn"),
    StudioItemEntity("shard_04_sinnoh", "Harmonic Shard IV (Sinnoh)", CAT_KEY_ITEM, "Spear Pillar Shard locking NDS 2.5D spatial-temporal geometry.", "SHARD:sinnoh", 1f, 0, "#6366F1", "Sinnoh"),
    StudioItemEntity("shard_05_hisui", "Harmonic Shard V (Hisui Rift)", CAT_KEY_ITEM, "Ancient Rift Shard anchoring watercolor cel-3D echoes.", "SHARD:hisui", 1f, 0, "#F59E0B", "Hisui Rift"),
    StudioItemEntity("shard_06_unova", "Harmonic Shard VI (Unova)", CAT_KEY_ITEM, "Dragonspiral Shard synchronizing 60fps skeletal pixel sprites.", "SHARD:unova", 1f, 0, "#F8FAFC", "Unova"),
    StudioItemEntity("shard_07_kalos", "Harmonic Shard VII (Kalos)", CAT_KEY_ITEM, "Prism Tower Shard aligning 3DS PICA200 ink contours.", "SHARD:kalos", 1f, 0, "#EC4899", "Kalos"),
    StudioItemEntity("shard_08_alola", "Harmonic Shard VIII (Alola)", CAT_KEY_ITEM, "Mount Lanakila Shard channeling Z-Crystal tropical light.", "SHARD:alola", 1f, 0, "#FACC15", "Alola"),
    StudioItemEntity("shard_09_galar", "Harmonic Shard IX (Galar)", CAT_KEY_ITEM, "Hammerlocke Shard balancing stadium Dynamax energy.", "SHARD:galar", 1f, 0, "#FF2A85", "Galar"),
    StudioItemEntity("shard_10_kitakami", "Harmonic Shard X (Kitakami)", CAT_KEY_ITEM, "Crystal Pool Shard illuminating lantern-lit autumn valleys.", "SHARD:kitakami", 1f, 0, "#2DD4BF", "Kitakami"),
    StudioItemEntity("shard_11_paldea", "Harmonic Shard XI (Paldea)", CAT_KEY_ITEM, "Area Zero Shard refracting crystalline Terastal glints.", "SHARD:paldea", 1f, 0, "#38BDF8", "Paldea"),
    StudioItemEntity("shard_12_blueberry", "Harmonic Shard XII (Blueberry Terrarium)", CAT_KEY_ITEM, "Apex Biome Shard stabilizing the quad-climate convergence dome.", "SHARD:blueberry_summit", 1f, 0, "#00F5D4", "Blueberry Terrarium"),
    StudioItemEntity("shard_13_sevii", "Harmonic Shard XIII (Sevii Isles)", CAT_KEY_ITEM, "Starlight Archipelago Shard tuning volcanic and astral sea lanes.", "SHARD:sevii_isles", 1f, 0, "#00B4D8", "Sevii Isles"),
    StudioItemEntity("shard_14_orange", "Harmonic Shard XIV (Orange Archipelago)", CAT_KEY_ITEM, "Coral Trials Shard resonating with anime island-circuit trophies.", "SHARD:orange_archipelago", 1f, 0, "#FB923C", "Orange Archipelago"),
    StudioItemEntity("shard_15_orre", "Harmonic Shard XV (Orre Wastes)", CAT_KEY_ITEM, "Relic Stone Shard purifying GameCube TEV Shadow auras.", "SHARD:orre", 1f, 0, "#A855F7", "Orre Wastes"),
    StudioItemEntity("shard_16_fiore_almia", "Harmonic Shard XVI (Fiore & Almia)", CAT_KEY_ITEM, "Capture Styler Shard unifying Ranger highlands and wind sanctuaries.", "SHARD:fiore_almia", 1f, 0, "#22C55E", "Fiore / Almia"),
    StudioItemEntity("shard_17_ransei", "Harmonic Shard XVII (Ransei Kingdoms)", CAT_KEY_ITEM, "Feudal Blossom Shard linking tactical warlord fortresses.", "SHARD:ransei", 1f, 0, "#F43F5E", "Ransei"),
    StudioItemEntity("shard_18_ferrum", "Harmonic Shard XVIII (Ferrum Isle)", CAT_KEY_ITEM, "Synergy Burst Shard charging Ferrum stadium battle gauges.", "SHARD:ferrum", 1f, 0, "#EAB308", "Ferrum"),
    StudioItemEntity("shard_19_lental", "Harmonic Shard XIX (Lental Atolls)", CAT_KEY_ITEM, "Illumina Photosphere Shard brightening bioluminescent reefs.", "SHARD:lental", 1f, 0, "#06B6D4", "Lental"),
    StudioItemEntity("shard_20_pasio", "Harmonic Shard XX (Pasio Isle)", CAT_KEY_ITEM, "Sync Pair Shard converging Champions from every era.", "SHARD:pasio", 1f, 0, "#8B5CF6", "Pasio"),
    StudioItemEntity("shard_21_aeos", "Harmonic Shard XXI (Aeos Island)", CAT_KEY_ITEM, "Aeos Energy Shard amplifying team-stadium goal zones.", "SHARD:aeos", 1f, 0, "#F97316", "Aeos"),
    StudioItemEntity("shard_22_mystery_rift", "Harmonic Shard XXII (Mystery Rift)", CAT_KEY_ITEM, "Final Distortion Shard harmonizing Mystery Guilds and Ultra Space.", "SHARD:mystery_rift", 1f, 0, "#C084FC", "Mystery Rift"),
  )
}

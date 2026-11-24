package com.example.engine

/**
 * 100% Clean-Room Original 64-Step Multi-Section Soundtrack Score Catalog for Pokémon: Prism Resonance.
 *
 * Every composition uses a 64-step 8th-note architecture:
 *  - Steps  0..7  : Intro Motif / Pickup Hook
 *  - Steps  8..23 : Section A (Main Memorable Theme)
 *  - Steps 24..39 : Section B (Contrasting Chorus / Lift)
 *  - Steps 40..55 : Section C (Bridge / Harmonic Development)
 *  - Steps 56..63 : Turnaround Cadence (seamlessly resolves back to Step 0)
 *
 * All melodies, counter-melodies, basslines, and chord progressions are 100% original compositions
 * inspired by the musical eras of handheld chiptune, pentatonic shrine chimes, GBA brass horizons,
 * alpine jazz piano, neon FM funk, orchestral waltz/stadium anthems, and tropical island grooves.
 */
enum class SoundPaletteStyle(val label: String) {
  CHIPTUNE_ADVENTURE("Chiptune Pulse & Triangle"),
  PENTATONIC_BELL_KOTO("Pentatonic Bell & Koto"),
  HEROIC_BRASS_TRUMPET("Heroic Brass & Snare"),
  JAZZ_PIANO_ALPINE("Jazz Piano & Alpine Synth"),
  NEON_SLAP_FUNK("FM Slap Bass & Neon Funk"),
  ORCHESTRAL_ACCORDION_WALTZ("Strings, Accordion & Stadium"),
  TROPICAL_MARIMBA_GUITAR("Tropical Marimba & Guitar"),
  BATTLE_CHIPTUNE_ROCK("High-Energy Battle Synth"),
  MONOCHROME_RIFT_SYNTH("Dark Rift Industrial FM"),
  CHAMPION_SYMPHONIC_CLIMAX("Symphonic Convergence"),
}

enum class AmbientBedType(val id: String, val label: String) {
  NONE("none", "Off"),
  OCEAN_SURF("ocean_surf", "Ocean Surf"),
  MEADOW_WIND("meadow_wind", "Meadow Wind"),
  SNOW_SHIMMER("snow_shimmer", "Snow Shimmer"),
  CRYSTAL_HUM("crystal_hum", "Crystal Hum"),
}

data class DynamicBattleAudioState(
  val lowHpUrgency: Boolean = false,
  val bossFinalAceClimax: Boolean = false,
  val ambientBed: AmbientBedType = AmbientBedType.NONE,
  val ambientBedEnabled: Boolean = true,
)

data class BgmScore64(
  val melody64: IntArray,
  val harmony64: IntArray,
  val bass64: IntArray,
  val chordRoots16: IntArray,
  val chordTypes16: IntArray, // 0=Maj, 1=Min, 2=Dom7, 3=Min7, 4=Sus4, 5=Dim/Phrygian
) {
  companion object {
    const val STEPS = 64
    const val REST = -999
  }
}

internal object OmniSoundtrackScores {
  private const val R = BgmScore64.REST

  private fun concat(vararg arrays: IntArray): IntArray {
    val total = arrays.sumOf { it.size }
    val out = IntArray(total)
    var pos = 0
    for (arr in arrays) {
      arr.copyInto(out, pos)
      pos += arr.size
    }
    return out
  }

  private fun expandBass16To64(pattern16: IntArray, pulseOctave: Boolean = false): IntArray {
    val out = IntArray(64)
    for (bar in 0 until 16) {
      val root = pattern16[bar % pattern16.size]
      val baseIdx = bar * 4
      out[baseIdx] = root
      out[baseIdx + 1] = if (pulseOctave) root + 12 else root
      out[baseIdx + 2] = root + 7
      out[baseIdx + 3] = if (pulseOctave) root + 12 else root + 5
    }
    return out
  }

  // 1. PRISM RESONANCE TITLE — Majestic Symphonic Adventure Overture (D Major)
  val prismTitle = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Rising prism fanfare
      intArrayOf(0, 4, 7, 12, R, 11, 12, 14),
      // Section A (8..23): Heroic horizon theme
      intArrayOf(12, R, 7, 9, 11, 12, 14, 11, 9, R, 7, 4, 5, 7, 9, 11),
      // Section B (24..39): Soaring chorus with octave peak
      intArrayOf(12, 14, 16, 14, 12, 11, 9, 7, 9, 11, 12, 14, 16, 17, 16, 14),
      // Section C Bridge (40..55): Reflective modulation & build
      intArrayOf(9, 12, 11, 9, 7, 9, 11, 12, 14, 12, 11, 9, 7, 9, 11, 14),
      // Turnaround (56..63): V7 triumphant cadence into loop
      intArrayOf(16, 14, 12, 11, 9, 11, 7, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, 0, 4, 7, R, 7, 9, 11),
      intArrayOf(7, R, 4, 5, 7, 7, 9, 7, 5, R, 4, 0, 2, 4, 5, 7),
      intArrayOf(7, 9, 12, 11, 9, 7, 5, 4, 5, 7, 9, 11, 12, 14, 12, 11),
      intArrayOf(5, 9, 7, 5, 4, 5, 7, 9, 9, 7, 7, 5, 4, 5, 7, 9),
      intArrayOf(12, 11, 9, 7, 5, 7, 4, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, -5, -5, -3, -3, -7, -5, 5, 5, 0, 0, 2, 2, 7, 7)),
    chordRoots16 = intArrayOf(0, 0, 7, 7, 9, 9, 4, 7, 5, 5, 0, 0, 2, 2, 7, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 2, 1, 1, 1, 0, 0, 0, 0, 0, 1, 3, 4, 2),
  )

  // 2. HARMONIC LAB & TOWN — Warm Gentle Town & Research Sanctuary (C Major)
  val harmonicLab = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Gentle chime welcome
      intArrayOf(0, 4, 7, R, 9, 7, 4, R),
      // Section A (8..23): Cozy hometown melody
      intArrayOf(4, 7, 9, 7, 4, 2, 0, R, 2, 4, 5, 4, 2, 0, -3, R),
      // Section B (24..39): Curious laboratory wonder
      intArrayOf(5, 9, 12, 11, 9, 7, 4, 7, 9, 7, 5, 4, 2, 4, 7, R),
      // Section C Bridge (40..55): Warm pastoral harmony
      intArrayOf(9, 11, 12, 9, 7, 4, 5, 7, 4, 2, 0, 2, 4, 7, 9, 7),
      // Turnaround (56..63): Soft resolving cadence
      intArrayOf(5, 4, 2, 4, 7, 4, 2, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, 0, 4, R, 5, 4, 0, R),
      intArrayOf(0, 4, 5, 4, 0, -1, -5, R, -1, 0, 2, 0, -1, -3, -5, R),
      intArrayOf(0, 5, 9, 7, 5, 4, 0, 4, 5, 4, 2, 0, -1, 0, 4, R),
      intArrayOf(5, 7, 9, 5, 4, 0, 2, 4, 0, -1, -3, -1, 0, 4, 5, 4),
      intArrayOf(2, 0, -1, 0, 4, 2, -1, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 5, 5, 2, 2, 7, 7, 5, 5, 0, 9, 5, 2, 7, 7)),
    chordRoots16 = intArrayOf(0, 0, 5, 5, 2, 2, 7, 7, 5, 5, 0, 9, 5, 2, 7, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 0, 1, 3, 4, 2, 0, 0, 0, 1, 0, 1, 4, 2),
  )

  // 3. KANTO / SEVII — Chiptune Adventure (25% Pulse + Triangle Walking Bass, E Major)
  val kantoSeviiChiptune = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Crisp 8-bit brisk adventure pickup
      intArrayOf(0, 2, 4, 7, 9, 7, 4, R),
      // Section A (8..23): Upbeat open-route chiptune hook
      intArrayOf(7, 7, 9, 7, 4, 2, 0, 4, 7, 9, 11, 9, 7, 4, 2, R),
      // Section B (24..39): Playful ascending adventure chorus
      intArrayOf(9, 11, 12, 11, 9, 7, 4, 7, 11, 12, 14, 12, 11, 9, 7, R),
      // Section C Bridge (40..55): Classic 8-bit double-time bounce
      intArrayOf(12, 9, 7, 9, 11, 7, 4, 7, 9, 7, 5, 4, 2, 4, 7, 9),
      // Turnaround (56..63): Walking cadence back to root
      intArrayOf(11, 9, 7, 4, 2, 4, 7, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, -3, 0, 4, 5, 4, 0, R),
      intArrayOf(4, 4, 5, 4, 0, -1, -5, 0, 4, 5, 7, 5, 4, 0, -1, R),
      intArrayOf(5, 7, 9, 7, 5, 4, 0, 4, 7, 9, 11, 9, 7, 5, 4, R),
      intArrayOf(9, 5, 4, 5, 7, 4, 0, 4, 5, 4, 2, 0, -1, 0, 4, 5),
      intArrayOf(7, 5, 4, 0, -1, 0, 2, R),
    ),
    bass64 = concat(
      // Walking triangle bassline across 64 steps
      intArrayOf(0, 4, 7, 9, 7, 4, 2, -1),
      intArrayOf(0, 2, 4, 7, 5, 4, 2, 0, -3, 0, 4, 7, 2, 4, 7, -5),
      intArrayOf(5, 7, 9, 12, 0, 4, 7, 9, 2, 5, 9, 11, 7, 4, 2, -5),
      intArrayOf(9, 7, 5, 4, 7, 5, 4, 2, 5, 4, 2, 0, 2, 4, 7, 9),
      intArrayOf(7, 5, 4, 2, 7, 2, -5, -1),
    ),
    chordRoots16 = intArrayOf(0, 7, 0, 5, 9, 2, 7, 7, 5, 0, 2, 7, 9, 4, 5, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 1, 2, 1, 1, 0, 2),
  )

  // 4. JOHTO / KITAKAMI / RANSEI — Pentatonic Bell & Koto (D Major / Yo-In Pentatonic)
  val johtoKitakamiKoto = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Temple bell chime opening
      intArrayOf(12, R, 9, 7, 4, R, 2, 0),
      // Section A (8..23): Graceful pentatonic koto motif
      intArrayOf(0, 2, 4, 7, 9, R, 7, 4, 7, 9, 12, 9, 7, 4, 2, R),
      // Section B (24..39): Autumn shrine sweeping chimes
      intArrayOf(14, 12, 9, 12, 14, 16, 14, 12, 9, 7, 4, 7, 9, 12, 9, R),
      // Section C Bridge (40..55): Contemplative In-sen modal tint
      intArrayOf(7, 9, 12, 14, 12, 9, 7, 4, 2, 4, 7, 9, 7, 4, 2, 0),
      // Turnaround (56..63): Resonant bell & koto cadence
      intArrayOf(4, 7, 9, 12, 9, 7, 4, R),
    ),
    harmony64 = concat(
      intArrayOf(7, R, 4, 2, 0, R, -3, -5),
      intArrayOf(-5, -3, 0, 2, 4, R, 2, 0, 2, 4, 7, 4, 2, 0, -3, R),
      intArrayOf(9, 7, 4, 7, 9, 12, 9, 7, 4, 2, 0, 2, 4, 7, 4, R),
      intArrayOf(2, 4, 7, 9, 7, 4, 2, 0, -3, 0, 2, 4, 2, 0, -3, -5),
      intArrayOf(0, 2, 4, 7, 4, 2, 0, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 7, 7, 9, 9, 4, 7, 5, 5, 0, 0, 2, 9, 7, 7)),
    chordRoots16 = intArrayOf(0, 0, 7, 7, 9, 9, 4, 7, 5, 5, 0, 0, 2, 9, 7, 7),
    chordTypes16 = intArrayOf(4, 0, 4, 0, 1, 1, 1, 4, 0, 0, 0, 4, 1, 1, 4, 0),
  )

  // 5. HOENN / ORANGE ARCHIPELAGO — Heroic Brass & Trumpet Horizon (F Major)
  val hoennOrangeBrass = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Bright GBA trumpet call-to-arms
      intArrayOf(0, 0, 7, 7, 9, 10, 12, R),
      // Section A (8..23): Heroic sea-route brass fanfare
      intArrayOf(12, 10, 9, 7, 5, 7, 9, 12, 14, 12, 10, 9, 7, 5, 7, R),
      // Section B (24..39): Soaring ocean-horizon chorus
      intArrayOf(12, 14, 16, 17, 16, 14, 12, 9, 10, 12, 14, 16, 17, 19, 17, 16),
      // Section C Bridge (40..55): Marching horn counter-fanfare
      intArrayOf(14, 14, 12, 10, 9, 10, 12, 14, 12, 9, 7, 9, 10, 12, 14, 12),
      // Turnaround (56..63): Snare roll + triumphant trumpet lift
      intArrayOf(7, 9, 10, 12, 14, 12, 7, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, -5, 4, 4, 5, 7, 7, R),
      intArrayOf(7, 7, 5, 4, 0, 4, 5, 7, 10, 9, 7, 5, 4, 0, 4, R),
      intArrayOf(7, 9, 12, 12, 12, 10, 9, 5, 7, 9, 10, 12, 12, 14, 12, 12),
      intArrayOf(10, 10, 9, 7, 5, 7, 9, 10, 9, 5, 4, 5, 7, 9, 10, 7),
      intArrayOf(4, 5, 7, 9, 10, 9, 4, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 5, 5, 7, 7, 0, 7, 5, 5, 0, 9, 10, 10, 7, 7), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 5, 5, 7, 7, 0, 7, 5, 5, 0, 9, 10, 10, 7, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 1, 0, 0, 4, 2),
  )

  // 6. SINNOH / HISUI — Jazz Piano & Alpine Synth (Eb Major / C Minor 9 tint)
  val sinnohHisuiJazz = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Sparkling alpine synth & Rhodes pickup
      intArrayOf(7, 10, 12, 14, R, 12, 10, 7),
      // Section A (8..23): Smooth jazzy ii-V-I electric piano melody
      intArrayOf(9, 10, 12, 9, 7, 5, 3, 5, 7, 10, 12, 14, 12, 10, 7, R),
      // Section B (24..39): Crystalline snow-peak lift with jazz 7ths
      intArrayOf(14, 15, 17, 15, 14, 12, 10, 7, 9, 12, 14, 15, 14, 12, 10, R),
      // Section C Bridge (40..55): Expressive alpine chord solo
      intArrayOf(12, 10, 9, 7, 5, 7, 9, 10, 12, 14, 15, 14, 12, 9, 7, 10),
      // Turnaround (56..63): ii9 -> V13 -> I turnaround
      intArrayOf(12, 10, 9, 7, 5, 7, 3, R),
    ),
    harmony64 = concat(
      intArrayOf(3, 7, 9, 10, R, 9, 7, 3),
      intArrayOf(5, 7, 9, 5, 3, 2, 0, 2, 3, 7, 9, 10, 9, 7, 3, R),
      intArrayOf(10, 12, 14, 12, 10, 9, 7, 3, 5, 9, 10, 12, 10, 9, 7, R),
      intArrayOf(9, 7, 5, 3, 2, 3, 5, 7, 9, 10, 12, 10, 9, 5, 3, 7),
      intArrayOf(9, 7, 5, 3, 2, 3, 0, R),
    ),
    bass64 = concat(
      intArrayOf(0, 3, 7, 10, 5, 9, 7, 2),
      intArrayOf(2, 5, 9, 10, 7, 10, 11, 7, 0, 4, 7, 9, 9, 7, 5, 2),
      intArrayOf(5, 9, 12, 10, 0, 3, 7, 9, 2, 5, 9, 12, 7, 5, 3, 2),
      intArrayOf(9, 12, 10, 7, 5, 7, 9, 10, 2, 5, 7, 9, 7, 5, 2, -1),
      intArrayOf(2, 5, 7, 10, 7, 5, 2, -5),
    ),
    chordRoots16 = intArrayOf(0, 5, 2, 7, 0, 9, 2, 7, 5, 0, 2, 7, 9, 5, 2, 7),
    chordTypes16 = intArrayOf(0, 3, 3, 2, 0, 3, 3, 2, 0, 0, 3, 2, 3, 0, 3, 2),
  )

  // 7. UNOVA / ORRE / FERRUM — High-Energy Slap Bass & Neon Funk (G Minor)
  val unovaOrreFunk = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Punchy neon synth brass stab intro
      intArrayOf(0, R, 3, 5, 7, R, 10, 7),
      // Section A (8..23): Syncopated metropolitan funk groove
      intArrayOf(12, R, 10, 7, 5, 7, R, 3, 5, 7, 10, R, 12, 10, 7, 5),
      // Section B (24..39): High-voltage skyline bridge chorus
      intArrayOf(15, 14, 12, 10, 12, R, 10, 7, 10, 12, 14, 15, 17, 15, 14, 12),
      // Section C Bridge (40..55): Funky chromatic slap-break
      intArrayOf(10, R, 9, 10, 12, R, 7, 10, 12, 13, 12, 10, 7, 5, 3, 5),
      // Turnaround (56..63): Syncopated brass turnaround
      intArrayOf(7, 10, 12, R, 10, 7, 5, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, R, 0, 2, 3, R, 7, 3),
      intArrayOf(7, R, 5, 3, 2, 3, R, 0, 2, 3, 7, R, 7, 5, 3, 2),
      intArrayOf(10, 10, 7, 5, 7, R, 5, 3, 7, 7, 10, 10, 12, 10, 10, 7),
      intArrayOf(5, R, 5, 7, 7, R, 3, 5, 7, 9, 7, 5, 3, 2, 0, 2),
      intArrayOf(3, 5, 7, R, 5, 3, 2, R),
    ),
    bass64 = concat(
      intArrayOf(0, 12, R, 0, 3, 5, 7, 10),
      intArrayOf(0, 12, 0, 10, 3, 15, 5, 7, 10, 22, 10, 7, 5, 17, 7, 5),
      intArrayOf(8, 20, 8, 7, 10, 22, 10, 12, 3, 15, 5, 17, 7, 19, 10, 7),
      intArrayOf(5, 17, 5, 3, 0, 12, 3, 5, 8, 20, 7, 19, 5, 17, 3, 2),
      intArrayOf(7, 19, 7, 5, 10, 7, 5, 3),
    ),
    chordRoots16 = intArrayOf(0, 3, 0, 5, 10, 5, 8, 10, 3, 5, 7, 10, 5, 0, 8, 7),
    chordTypes16 = intArrayOf(3, 0, 3, 3, 0, 3, 0, 0, 0, 3, 3, 2, 3, 3, 0, 2),
  )

  // 8. KALOS / GALAR / PASIO / AEOS — Orchestral Strings, Accordion Waltz & Stadium Pulse (A Major)
  val kalosGalarWaltz = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Graceful Parisian accordion flourish
      intArrayOf(4, 7, 9, 12, 11, 9, 7, R),
      // Section A (8..23): Sweeping boulevard strings & reed melody
      intArrayOf(12, 11, 9, 7, 9, 11, 12, R, 14, 12, 11, 9, 7, 4, 7, R),
      // Section B (24..39): Uplifting Galar stadium anthem chant
      intArrayOf(16, 16, 14, 12, 14, 16, 19, 16, 14, 12, 11, 12, 14, 16, 14, R),
      // Section C Bridge (40..55): Romantic orchestral swell
      intArrayOf(9, 11, 12, 14, 16, 14, 12, 11, 9, 12, 11, 9, 7, 9, 11, 12),
      // Turnaround (56..63): Grand cadence into loop
      intArrayOf(14, 12, 11, 9, 7, 9, 11, R),
    ),
    harmony64 = concat(
      intArrayOf(0, 4, 5, 7, 7, 5, 4, R),
      intArrayOf(7, 7, 5, 4, 5, 7, 9, R, 9, 9, 7, 5, 4, 0, 4, R),
      intArrayOf(12, 12, 9, 7, 9, 12, 14, 12, 9, 7, 7, 9, 9, 12, 11, R),
      intArrayOf(5, 7, 9, 9, 12, 9, 9, 7, 5, 9, 7, 5, 4, 5, 7, 9),
      intArrayOf(9, 9, 7, 5, 4, 5, 7, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 9, 9, 5, 5, 7, 7, 0, 4, 5, 7, 9, 5, 2, 7)),
    chordRoots16 = intArrayOf(0, 0, 9, 9, 5, 5, 7, 7, 0, 4, 5, 7, 9, 5, 2, 7),
    chordTypes16 = intArrayOf(0, 0, 1, 1, 0, 0, 4, 2, 0, 1, 0, 0, 1, 0, 1, 2),
  )

  // 9. ALOLA / PALDEA / BLUEBERRY / LENTAL — Tropical Marimba & Acoustic Guitar (G Major)
  val alolaPaldeaTropical = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Bright island marimba roll
      intArrayOf(7, 9, 12, 14, R, 12, 9, 7),
      // Section A (8..23): Sunny coral island groove
      intArrayOf(12, R, 9, 12, 14, 12, 9, 7, 4, 7, 9, 12, 9, 7, 4, R),
      // Section B (24..39): Paldean acoustic open-world exploration chorus
      intArrayOf(16, 14, 12, 14, 16, 19, 16, 14, 12, 14, 16, 14, 12, 9, 7, R),
      // Section C Bridge (40..55): Marimba syncopated sunshine dance
      intArrayOf(9, 12, 9, 7, 9, 12, 14, 12, 11, 14, 11, 9, 7, 9, 12, 14),
      // Turnaround (56..63): Acoustic strum cadence
      intArrayOf(12, 9, 7, 4, 7, 9, 11, R),
    ),
    harmony64 = concat(
      intArrayOf(4, 5, 7, 9, R, 7, 5, 4),
      intArrayOf(7, R, 5, 7, 9, 7, 5, 4, 0, 4, 5, 7, 5, 4, 0, R),
      intArrayOf(12, 9, 7, 9, 12, 14, 12, 9, 7, 9, 12, 9, 7, 5, 4, R),
      intArrayOf(5, 7, 5, 4, 5, 7, 9, 7, 7, 9, 7, 5, 4, 5, 7, 9),
      intArrayOf(7, 5, 4, 0, 4, 5, 7, R),
    ),
    bass64 = concat(
      intArrayOf(0, R, 7, 0, 5, R, 9, 7),
      intArrayOf(0, R, 7, 4, 5, R, 9, 5, 9, R, 4, 7, 7, R, 2, 7),
      intArrayOf(5, R, 0, 5, 0, R, 7, 4, 2, R, 9, 5, 7, R, 4, 7),
      intArrayOf(9, R, 4, 9, 5, R, 0, 5, 7, R, 2, 7, 0, 4, 7, 9),
      intArrayOf(5, R, 2, 5, 7, R, 2, 7),
    ),
    chordRoots16 = intArrayOf(0, 5, 0, 5, 9, 7, 5, 0, 2, 7, 9, 5, 7, 0, 5, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 0, 1, 0, 0, 0, 1, 2, 1, 0, 4, 0, 0, 2),
  )

  // 10. WILD ENCOUNTER — Urgent High-Tempo Wild Battle (E Minor)
  val wildEncounter = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Sudden chromatic battle alarm
      intArrayOf(0, 1, 3, 6, 7, 10, 12, 11),
      // Section A (8..23): Relentless wild chase motif
      intArrayOf(12, 10, 7, 6, 7, 10, 12, 15, 14, 12, 10, 7, 6, 7, 10, 7),
      // Section B (24..39): High-energy duel chorus
      intArrayOf(15, 14, 12, 14, 15, 17, 15, 14, 12, 10, 12, 14, 12, 10, 7, 10),
      // Section C Bridge (40..55): Tense rhythmic breakdown
      intArrayOf(7, 7, 10, 7, 11, 11, 14, 11, 12, 12, 15, 12, 14, 12, 11, 10),
      // Turnaround (56..63): Ascending riff back to alarm
      intArrayOf(7, 10, 12, 14, 15, 14, 12, 11),
    ),
    harmony64 = concat(
      intArrayOf(-5, -4, 0, 2, 3, 7, 7, 6),
      intArrayOf(7, 7, 3, 2, 3, 7, 7, 10, 10, 7, 7, 3, 2, 3, 7, 3),
      intArrayOf(10, 10, 7, 10, 10, 12, 10, 10, 7, 5, 7, 10, 7, 5, 3, 7),
      intArrayOf(3, 3, 7, 3, 7, 7, 10, 7, 7, 7, 10, 7, 10, 7, 7, 5),
      intArrayOf(3, 7, 7, 10, 10, 10, 7, 6),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 3, 3, 5, 5, 7, 6, 8, 8, 10, 10, 5, 7, 8, 11), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 3, 3, 5, 5, 7, 6, 8, 8, 10, 10, 5, 7, 8, 11),
    chordTypes16 = intArrayOf(1, 1, 0, 0, 1, 1, 1, 5, 0, 0, 0, 0, 1, 1, 0, 5),
  )

  // 11. TRAINER EYE-CONTACT DUEL — Swaggering Competitive Trainer Battle (F# Minor / A Major)
  val trainerDuel = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Confident brass/pulse challenge flourish
      intArrayOf(7, R, 10, 12, 14, 12, 10, 7),
      // Section A (8..23): Strutting competitive duel riff
      intArrayOf(12, 12, 10, 7, 10, 12, 15, R, 14, 12, 10, 7, 5, 7, 10, R),
      // Section B (24..39): Heroic major-key competitive chorus
      intArrayOf(15, 17, 19, 17, 15, 14, 12, 10, 12, 14, 15, 17, 19, 17, 15, 14),
      // Section C Bridge (40..55): Tactical back-and-forth exchange
      intArrayOf(12, 10, 12, 14, 15, 14, 12, 10, 7, 10, 12, 14, 12, 10, 7, 5),
      // Turnaround (56..63): Driving snare turnaround
      intArrayOf(7, 10, 12, 14, 12, 10, 7, R),
    ),
    harmony64 = concat(
      intArrayOf(3, R, 7, 7, 10, 7, 7, 3),
      intArrayOf(7, 7, 5, 3, 7, 7, 10, R, 10, 7, 7, 3, 2, 3, 7, R),
      intArrayOf(10, 12, 15, 12, 10, 10, 7, 7, 7, 10, 10, 12, 15, 12, 10, 10),
      intArrayOf(7, 5, 7, 10, 10, 10, 7, 5, 3, 5, 7, 10, 7, 5, 3, 2),
      intArrayOf(3, 5, 7, 10, 7, 5, 3, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 10, 10, 8, 8, 7, 7, 3, 3, 10, 10, 5, 5, 7, 7), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 10, 10, 8, 8, 7, 7, 3, 3, 10, 10, 5, 5, 7, 7),
    chordTypes16 = intArrayOf(1, 1, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 1, 1, 4, 2),
  )

  // 12. RIVAL KAEL SHOWDOWN — Blazing High-Tempo Rival Theme (G Minor)
  val rivalKael = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Fiery rising rival shred
      intArrayOf(0, 7, 6, 7, 10, 12, 14, 15),
      // Section A (8..23): Sharp competitive rival melody
      intArrayOf(14, 12, 10, 12, 14, 15, 14, 12, 10, 7, 10, 12, 14, 12, 10, 7),
      // Section B (24..39): Relentless high-octane chorus
      intArrayOf(15, 17, 19, 17, 15, 14, 15, 17, 19, 20, 19, 17, 15, 14, 12, 14),
      // Section C Bridge (40..55): Dramatic counter-attack sequence
      intArrayOf(12, 10, 8, 10, 12, 14, 15, 14, 12, 10, 7, 10, 12, 14, 15, 17),
      // Turnaround (56..63): Power-chord slam into intro
      intArrayOf(19, 17, 15, 14, 12, 10, 7, 6),
    ),
    harmony64 = concat(
      intArrayOf(-5, 3, 2, 3, 7, 7, 10, 10),
      intArrayOf(10, 7, 7, 7, 10, 10, 10, 7, 5, 3, 5, 7, 10, 7, 5, 3),
      intArrayOf(10, 12, 15, 12, 10, 10, 10, 12, 15, 15, 15, 12, 10, 10, 7, 10),
      intArrayOf(8, 7, 5, 7, 8, 10, 10, 10, 7, 5, 3, 5, 7, 10, 10, 12),
      intArrayOf(15, 12, 10, 10, 7, 5, 3, 2),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, -2, -2, -4, -4, -5, -2, 3, 3, 5, 5, 8, 10, 7, 7), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 10, 10, 8, 8, 7, 10, 3, 3, 5, 5, 8, 10, 7, 7),
    chordTypes16 = intArrayOf(1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 4, 2),
  )

  // 13. GYM / TRIAL CLIMAX — Electrifying Stadium Boss Theme (A Minor / C Major)
  val gymTrial = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Stadium spotlight fanfare
      intArrayOf(0, 3, 5, 7, 10, 12, 15, 14),
      // Section A (8..23): Driving Gym Leader chorus
      intArrayOf(12, 12, 10, 12, 15, 14, 12, 10, 7, 10, 12, 14, 12, 10, 7, 5),
      // Section B (24..39): Crowd-roaring uplifting bridge
      intArrayOf(15, 15, 17, 15, 14, 12, 14, 15, 19, 17, 15, 14, 15, 17, 19, 17),
      // Section C Bridge (40..55): Heroic key-change surge
      intArrayOf(14, 15, 17, 19, 17, 15, 14, 12, 10, 12, 14, 15, 14, 12, 10, 7),
      // Turnaround (56..63): Climactic arena drum-break
      intArrayOf(12, 14, 15, 17, 15, 14, 12, 10),
    ),
    harmony64 = concat(
      intArrayOf(-5, 0, 2, 3, 7, 7, 10, 10),
      intArrayOf(7, 7, 5, 7, 10, 10, 7, 5, 3, 5, 7, 10, 7, 5, 3, 2),
      intArrayOf(10, 10, 12, 10, 10, 7, 10, 10, 15, 12, 10, 10, 10, 12, 15, 12),
      intArrayOf(10, 10, 12, 15, 12, 10, 10, 7, 5, 7, 10, 10, 10, 7, 5, 3),
      intArrayOf(7, 10, 10, 12, 10, 10, 7, 5),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 8, 8, 10, 10, 7, 7, 3, 3, 5, 5, 8, 10, 7, 7), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 8, 8, 10, 10, 7, 7, 3, 3, 5, 5, 8, 10, 7, 7),
    chordTypes16 = intArrayOf(1, 1, 0, 0, 0, 0, 4, 2, 0, 0, 1, 1, 0, 0, 4, 2),
  )

  // 14. TEAM MONOCHROME RIFT — Menacing Phrygian/Industrial Syndicate Theme (C# Phrygian)
  val teamMonochrome = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Glitchy Phrygian rift alarm
      intArrayOf(0, 1, 4, 7, 6, 4, 1, 0),
      // Section A (8..23): Industrial syndicate march
      intArrayOf(7, 8, 7, 6, 4, 1, 0, 1, 7, 8, 11, 12, 11, 8, 7, 6),
      // Section B (24..39): Dimensional distortion surge
      intArrayOf(12, 13, 12, 11, 8, 7, 6, 7, 13, 16, 15, 13, 12, 11, 8, 7),
      // Section C Bridge (40..55): Commander Vex dark tritone motif
      intArrayOf(6, 7, 12, 13, 12, 7, 6, 4, 1, 4, 6, 7, 8, 7, 6, 1),
      // Turnaround (56..63): Falling rift cadence
      intArrayOf(12, 11, 8, 7, 6, 4, 1, 0),
    ),
    harmony64 = concat(
      intArrayOf(-5, -4, 0, 1, 0, 0, -4, -5),
      intArrayOf(1, 4, 1, 0, 0, -4, -5, -4, 1, 4, 7, 7, 7, 4, 1, 0),
      intArrayOf(7, 8, 7, 6, 4, 1, 0, 1, 8, 12, 11, 8, 7, 6, 4, 1),
      intArrayOf(0, 1, 6, 8, 6, 1, 0, -4, -4, 0, 1, 4, 4, 1, 0, -4),
      intArrayOf(7, 6, 4, 1, 0, -1, -4, -5),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, 1, 1, 6, 6, 5, 1, 0, 1, 8, 8, 6, 5, 4, 1), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 1, 1, 6, 6, 5, 1, 0, 1, 8, 8, 6, 5, 4, 1),
    chordTypes16 = intArrayOf(5, 5, 0, 0, 5, 5, 1, 5, 5, 0, 1, 1, 5, 1, 5, 5),
  )

  // 15. CHAMPION CONVERGENCE FINALE — Epic Multi-Era Boss Finale (E Minor -> G Major -> E Minor)
  val championConvergence = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Iconic rapid arpeggiated piano/synth prelude
      intArrayOf(0, 4, 7, 12, 11, 12, 14, 16),
      // Section A (8..23): Relentless Champion duel theme
      intArrayOf(15, 14, 12, 11, 12, 14, 15, 19, 17, 15, 14, 12, 11, 12, 14, 11),
      // Section B (24..39): Soaring heroic symphonic summit
      intArrayOf(19, 19, 17, 15, 17, 19, 22, 19, 17, 15, 14, 15, 17, 19, 20, 19),
      // Section C Bridge (40..55): Dramatic modal key-lift climax (+2 semitones)
      intArrayOf(17, 19, 21, 19, 17, 16, 14, 16, 17, 19, 21, 22, 21, 19, 17, 16),
      // Turnaround (56..63): Thunderous timpani/brass descent
      intArrayOf(19, 17, 15, 14, 12, 11, 12, 14),
    ),
    harmony64 = concat(
      intArrayOf(-5, 0, 3, 7, 7, 7, 10, 12),
      intArrayOf(10, 10, 7, 7, 7, 10, 10, 15, 12, 10, 10, 7, 7, 7, 10, 7),
      intArrayOf(15, 15, 12, 10, 12, 15, 17, 15, 12, 10, 10, 10, 12, 15, 15, 14),
      intArrayOf(12, 14, 16, 14, 12, 12, 10, 12, 12, 14, 16, 17, 16, 14, 12, 11),
      intArrayOf(15, 12, 10, 10, 7, 7, 7, 10),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 0, -3, -3, -5, -5, -7, -2, 3, 3, 5, 5, 7, 9, 10, 11), pulseOctave = true),
    chordRoots16 = intArrayOf(0, 0, 9, 9, 7, 7, 5, 10, 3, 3, 5, 5, 7, 9, 10, 11),
    chordTypes16 = intArrayOf(1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 2),
  )

  // 16. VICTORY & SHARD FANFARE — Joyous Celebratory Brass & Bell Fanfare (G Major)
  val victoryFanfare = BgmScore64(
    melody64 = concat(
      // Intro (0..7): Triumphant 4-beat fanfare burst
      intArrayOf(0, 4, 7, 12, R, 9, 11, 12),
      // Section A (8..23): Celebratory victory bounce
      intArrayOf(12, R, 14, 16, 14, 12, 9, 12, 14, 16, 17, 16, 14, 12, 11, R),
      // Section B (24..39): Sparkling Harmonic Shard chorus
      intArrayOf(16, 14, 12, 14, 16, 19, 16, 14, 12, 14, 16, 19, 21, 19, 16, R),
      // Section C Bridge (40..55): Jubilant brass/bell call-and-response
      intArrayOf(14, 16, 17, 16, 14, 12, 14, 16, 12, 14, 16, 14, 12, 9, 11, 12),
      // Turnaround (56..63): Grand celebratory flourish
      intArrayOf(14, 16, 19, 16, 14, 12, 11, R),
    ),
    harmony64 = concat(
      intArrayOf(-5, 0, 4, 7, R, 5, 7, 7),
      intArrayOf(7, R, 9, 12, 9, 7, 5, 7, 9, 12, 12, 12, 9, 7, 7, R),
      intArrayOf(12, 9, 7, 9, 12, 14, 12, 9, 7, 9, 12, 14, 16, 14, 12, R),
      intArrayOf(9, 12, 12, 12, 9, 7, 9, 12, 7, 9, 12, 9, 7, 5, 7, 7),
      intArrayOf(9, 12, 14, 12, 9, 7, 7, R),
    ),
    bass64 = expandBass16To64(intArrayOf(0, 5, 7, 0, 5, 7, 9, 7, 5, 0, 2, 7, 5, 7, 9, 7)),
    chordRoots16 = intArrayOf(0, 5, 7, 0, 5, 7, 9, 7, 5, 0, 2, 7, 5, 7, 9, 7),
    chordTypes16 = intArrayOf(0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2),
  )
}

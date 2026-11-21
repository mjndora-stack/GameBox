package com.example.data.model

enum class GameEngineType(val label: String) {
  WEB_CANVAS("2D Web Engine"),
  PUZZLE_NATIVE("Puzzle Engine"),
}

enum class PlayerTier(
  val levelIndex: Int,
  val displayName: String,
  val minXp: Int,
  val subtitle: String,
) {
  HOBBYIST(0, "Hobbyist", 0, "Curated Vault Initiate"),
  ENTHUSIAST(1, "Enthusiast", 500, "Unlocks Cipher Mastermind"),
  EXPERT(2, "Expert", 1500, "Unlocks Sudoku Six"),
  PRO(3, "Pro", 3000, "Veteran Arcade & Logic Solver"),
  MASTER(4, "Master", 5000, "Unlocks Crate Pusher"),
  GRANDMASTER(5, "Grandmaster", 8000, "Elite Tactical Thinker"),
  VIRTUOSO(6, "Virtuoso", 12000, "Precision Logic Architect"),
  LEGEND(7, "Legend", 17500, "Unlocks Queens N-Grid"),
  MYTHIC(8, "Mythic", 25000, "Pinnacle Offline Vault Champion");

  fun nextTier(): PlayerTier? =
    entries.getOrNull(levelIndex + 1)

  companion object {
    fun fromXp(xp: Int): PlayerTier =
      entries.lastOrNull { xp >= it.minXp } ?: HOBBYIST

    fun progressInTier(xp: Int): Float {
      val current = fromXp(xp)
      val next = current.nextTier() ?: return 1f
      val span = (next.minXp - current.minXp).coerceAtLeast(1)
      return ((xp - current.minXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }
  }
}

enum class VaultThemePreset(
  val id: String,
  val displayName: String,
  val requiredTier: PlayerTier,
  val description: String,
) {
  OBSIDIAN_MINIMAL("obsidian_minimal", "Obsidian Minimal", PlayerTier.HOBBYIST, "Stark monochrome ink & slate"),
  CYBER_MINT("cyber_mint", "Cyber Mint", PlayerTier.HOBBYIST, "Clean teal-mint arcade precision"),
  SOLAR_AMBER("solar_amber", "Solar Amber", PlayerTier.HOBBYIST, "Warm phosphor CRT amber"),
  COBALT_PULSE("cobalt_pulse", "Cobalt Pulse", PlayerTier.ENTHUSIAST, "Electric cobalt telemetry"),
  CRIMSON_VELVET("crimson_velvet", "Crimson Velvet", PlayerTier.EXPERT, "High-contrast apex crimson");

  companion object {
    fun fromId(id: String): VaultThemePreset =
      entries.firstOrNull { it.id == id } ?: CYBER_MINT
  }
}

enum class LibraryFilter(val label: String) {
  ALL("All"),
  PLATFORMER("Platformers"),
  PUZZLE("Puzzles"),
  FAN_FAVOURITE("Fan Fav"),
  LOCKED("Rank Locked"),
}

data class ControlConfig(
  val leftHandedHud: Boolean = false,
  val compactControls: Boolean = false,
  val hapticsEnabled: Boolean = true,
)

data class VaultPreferences(
  val isDarkMode: Boolean = true,
  val themePreset: VaultThemePreset = VaultThemePreset.CYBER_MINT,
  val activeProfileId: Long = 1L,
  val controlConfig: ControlConfig = ControlConfig(),
)

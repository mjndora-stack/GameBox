package com.example.data.model

enum class GameEngineType(val label: String) {
  WEB_CANVAS("2D Web Engine"),
  ARCADE_NATIVE("Arcade Engine"),
  PUZZLE_NATIVE("Puzzle Engine"),
}

enum class DailyMissionType {
  CHESS_WIN,
  SCORE_TARGET,
  STAGE_CLEAR,
  SF_WIN,
  PUZZLE_SOLVE_COUNT,
  GAMES_PLAYED_COUNT,
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
  ARCADE("Arcade"),
  PLATFORMER("Platformers"),
  PUZZLE("Puzzles"),
  FAN_FAVOURITE("Fan Fav"),
  LOCKED("Rank Locked"),
}

enum class FighterStat {
  ATTACK,
  HEALTH,
  SPEED,
}

data class FighterUpgradeLevels(
  val attackLevel: Int = 1,
  val healthLevel: Int = 1,
  val speedLevel: Int = 1,
) {
  fun levelOf(stat: FighterStat): Int =
    when (stat) {
      FighterStat.ATTACK -> attackLevel
      FighterStat.HEALTH -> healthLevel
      FighterStat.SPEED -> speedLevel
    }

  fun upgradeCost(stat: FighterStat): Int {
    val cur = levelOf(stat)
    return if (cur >= 5) 0 else cur * 40
  }

  fun withUpgraded(stat: FighterStat): FighterUpgradeLevels =
    when (stat) {
      FighterStat.ATTACK -> copy(attackLevel = (attackLevel + 1).coerceAtMost(5))
      FighterStat.HEALTH -> copy(healthLevel = (healthLevel + 1).coerceAtMost(5))
      FighterStat.SPEED -> copy(speedLevel = (speedLevel + 1).coerceAtMost(5))
    }
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
  val musicEnabled: Boolean = true,
  val lobbySfxEnabled: Boolean = true,
  val gameSfxEnabled: Boolean = true,
  val selectedLobbySongId: String = "lobby_lounge",
  val fighterUpgrades: Map<String, FighterUpgradeLevels> = defaultFighterUpgrades(),
) {
  companion object {
    fun defaultFighterUpgrades(): Map<String, FighterUpgradeLevels> =
      mapOf(
        "kai" to FighterUpgradeLevels(),
        "mira" to FighterUpgradeLevels(),
        "titan" to FighterUpgradeLevels(),
      )

    fun parseFighterUpgrades(raw: String?): Map<String, FighterUpgradeLevels> {
      val base = defaultFighterUpgrades().toMutableMap()
      if (raw.isNullOrBlank()) return base
      raw.split(";").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
          val id = parts[0]
          val nums = parts[1].split(",").mapNotNull { it.toIntOrNull() }
          if (nums.size == 3 && id in base) {
            base[id] =
              FighterUpgradeLevels(
                attackLevel = nums[0].coerceIn(1, 5),
                healthLevel = nums[1].coerceIn(1, 5),
                speedLevel = nums[2].coerceIn(1, 5),
              )
          }
        }
      }
      return base
    }

    fun encodeFighterUpgrades(map: Map<String, FighterUpgradeLevels>): String =
      map.entries.joinToString(";") { (id, lv) ->
        "$id:${lv.attackLevel},${lv.healthLevel},${lv.speedLevel}"
      }
  }
}

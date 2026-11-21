package com.example.data.model

enum class GameEngineType(val label: String) {
  WEB_CANVAS("2D Web Engine"),
  TEXT_ADVENTURE("Narrative Engine"),
  DUNGEON_3D("3D Raycast Engine"),
  RACING_3D("3D Horizon Engine"),
  VICE_SANDBOX("2.5D Open District"),
}

enum class PlayerTier(
  val levelIndex: Int,
  val displayName: String,
  val minXp: Int,
  val subtitle: String,
) {
  HOBBYIST(0, "Hobbyist", 0, "Curated Vault Initiate"),
  ENTHUSIAST(1, "Enthusiast", 500, "Dedicated Arcade Traveler"),
  EXPERT(2, "Expert", 1500, "Master Tactician & Speedrunner"),
  PRO(3, "Pro", 3000, "Full Vault Clearance · Vice Horizon Unlocked");

  fun nextTier(): PlayerTier? =
    when (this) {
      HOBBYIST -> ENTHUSIAST
      ENTHUSIAST -> EXPERT
      EXPERT -> PRO
      PRO -> null
    }

  companion object {
    fun fromXp(xp: Int): PlayerTier =
      when {
        xp >= PRO.minXp -> PRO
        xp >= EXPERT.minXp -> EXPERT
        xp >= ENTHUSIAST.minXp -> ENTHUSIAST
        else -> HOBBYIST
      }

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
  PLATFORMER_WEB("2D Web"),
  THREE_D("3D"),
  STORY("Story"),
  PRO("Pro"),
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

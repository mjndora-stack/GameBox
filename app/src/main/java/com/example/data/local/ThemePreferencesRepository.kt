package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ControlConfig
import com.example.data.model.FighterStat
import com.example.data.model.VaultPreferences
import com.example.data.model.VaultThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "vaultplay_prefs")

class ThemePreferencesRepository(private val context: Context) {
  private object Keys {
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    val THEME_PRESET = stringPreferencesKey("theme_preset")
    val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
    val LEFT_HANDED_HUD = booleanPreferencesKey("left_handed_hud")
    val COMPACT_CONTROLS = booleanPreferencesKey("compact_controls")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    val LOBBY_SFX_ENABLED = booleanPreferencesKey("lobby_sfx_enabled")
    val GAME_SFX_ENABLED = booleanPreferencesKey("game_sfx_enabled")
    val FIGHTER_UPGRADES = stringPreferencesKey("fighter_upgrades")
    val CREATOR_PIN = stringPreferencesKey("creator_pin")
    val DAILY_LAST_DAY_KEY = stringPreferencesKey("daily_last_day_key")
    val DAILY_CYCLE_OFFSET = intPreferencesKey("daily_cycle_offset")
    val DAILY_PLAYED_GAMES = stringPreferencesKey("daily_played_games")
  }

  val preferencesFlow: Flow<VaultPreferences> =
    context.vaultDataStore.data.map { prefs ->
      VaultPreferences(
        isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
        themePreset = VaultThemePreset.fromId(prefs[Keys.THEME_PRESET] ?: VaultThemePreset.CYBER_MINT.id),
        activeProfileId = prefs[Keys.ACTIVE_PROFILE_ID] ?: 1L,
        controlConfig =
          ControlConfig(
            leftHandedHud = prefs[Keys.LEFT_HANDED_HUD] ?: false,
            compactControls = prefs[Keys.COMPACT_CONTROLS] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
          ),
        musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
        lobbySfxEnabled = prefs[Keys.LOBBY_SFX_ENABLED] ?: true,
        gameSfxEnabled = prefs[Keys.GAME_SFX_ENABLED] ?: true,
        fighterUpgrades = VaultPreferences.parseFighterUpgrades(prefs[Keys.FIGHTER_UPGRADES]),
      )
    }

  val creatorPinFlow: Flow<String> =
    context.vaultDataStore.data.map { prefs -> prefs[Keys.CREATOR_PIN] ?: "2011" }

  suspend fun toggleDarkMode() {
    context.vaultDataStore.edit { prefs ->
      val current = prefs[Keys.IS_DARK_MODE] ?: true
      prefs[Keys.IS_DARK_MODE] = !current
    }
  }

  suspend fun setDarkMode(dark: Boolean) {
    context.vaultDataStore.edit { prefs -> prefs[Keys.IS_DARK_MODE] = dark }
  }

  suspend fun setThemePreset(preset: VaultThemePreset) {
    context.vaultDataStore.edit { prefs -> prefs[Keys.THEME_PRESET] = preset.id }
  }

  suspend fun setActiveProfileId(profileId: Long) {
    context.vaultDataStore.edit { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] = profileId }
  }

  suspend fun toggleMusicEnabled() {
    context.vaultDataStore.edit { prefs ->
      val current = prefs[Keys.MUSIC_ENABLED] ?: true
      prefs[Keys.MUSIC_ENABLED] = !current
    }
  }

  suspend fun toggleLobbySfxEnabled() {
    context.vaultDataStore.edit { prefs ->
      val current = prefs[Keys.LOBBY_SFX_ENABLED] ?: true
      prefs[Keys.LOBBY_SFX_ENABLED] = !current
    }
  }

  suspend fun toggleGameSfxEnabled() {
    context.vaultDataStore.edit { prefs ->
      val current = prefs[Keys.GAME_SFX_ENABLED] ?: true
      prefs[Keys.GAME_SFX_ENABLED] = !current
    }
  }

  suspend fun upgradeFighterStat(fighterId: String, stat: FighterStat) {
    context.vaultDataStore.edit { prefs ->
      val map = VaultPreferences.parseFighterUpgrades(prefs[Keys.FIGHTER_UPGRADES]).toMutableMap()
      val current = map[fighterId] ?: return@edit
      map[fighterId] = current.withUpgraded(stat)
      prefs[Keys.FIGHTER_UPGRADES] = VaultPreferences.encodeFighterUpgrades(map)
    }
  }

  suspend fun updateControlConfig(transform: (ControlConfig) -> ControlConfig) {
    context.vaultDataStore.edit { prefs ->
      val current =
        ControlConfig(
          leftHandedHud = prefs[Keys.LEFT_HANDED_HUD] ?: false,
          compactControls = prefs[Keys.COMPACT_CONTROLS] ?: false,
          hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        )
      val next = transform(current)
      prefs[Keys.LEFT_HANDED_HUD] = next.leftHandedHud
      prefs[Keys.COMPACT_CONTROLS] = next.compactControls
      prefs[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
    }
  }

  suspend fun setCreatorPin(pin: String) {
    context.vaultDataStore.edit { prefs -> prefs[Keys.CREATOR_PIN] = pin }
  }

  suspend fun syncDailyCycle(dayKey: String): Int {
    var cycle = 0
    context.vaultDataStore.edit { prefs ->
      val prevDay = prefs[Keys.DAILY_LAST_DAY_KEY]
      val prevCycle = prefs[Keys.DAILY_CYCLE_OFFSET] ?: 0
      when {
        prevDay == null -> {
          prefs[Keys.DAILY_LAST_DAY_KEY] = dayKey
          prefs[Keys.DAILY_CYCLE_OFFSET] = 0
          prefs[Keys.DAILY_PLAYED_GAMES] = ""
          cycle = 0
        }
        prevDay != dayKey -> {
          val nextCycle = prevCycle + 1
          prefs[Keys.DAILY_LAST_DAY_KEY] = dayKey
          prefs[Keys.DAILY_CYCLE_OFFSET] = nextCycle
          prefs[Keys.DAILY_PLAYED_GAMES] = ""
          cycle = nextCycle
        }
        else -> {
          cycle = prevCycle
        }
      }
    }
    return cycle
  }

  suspend fun advanceDailyCycle(dayKey: String): Int {
    var cycle = 0
    context.vaultDataStore.edit { prefs ->
      val nextCycle = (prefs[Keys.DAILY_CYCLE_OFFSET] ?: 0) + 1
      prefs[Keys.DAILY_LAST_DAY_KEY] = dayKey
      prefs[Keys.DAILY_CYCLE_OFFSET] = nextCycle
      prefs[Keys.DAILY_PLAYED_GAMES] = ""
      cycle = nextCycle
    }
    return cycle
  }

  suspend fun recordDistinctGameToday(dayKey: String, gameId: String): Int {
    var count = 0
    context.vaultDataStore.edit { prefs ->
      val storedDay = prefs[Keys.DAILY_LAST_DAY_KEY]
      val existing =
        if (storedDay == dayKey) {
          (prefs[Keys.DAILY_PLAYED_GAMES] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
        } else {
          prefs[Keys.DAILY_LAST_DAY_KEY] = dayKey
          mutableSetOf()
        }
      existing.add(gameId)
      prefs[Keys.DAILY_PLAYED_GAMES] = existing.joinToString(",")
      count = existing.size
    }
    return count
  }
}

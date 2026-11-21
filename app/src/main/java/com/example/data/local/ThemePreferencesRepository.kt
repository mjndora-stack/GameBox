package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ControlConfig
import com.example.data.model.VaultPreferences
import com.example.data.model.VaultThemePreset
import kotlinx.coroutines.flow.Flow
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
    val CREATOR_PIN = stringPreferencesKey("creator_pin")
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
      )
    }

  val creatorPinFlow: Flow<String> =
    context.vaultDataStore.data.map { prefs -> prefs[Keys.CREATOR_PIN] ?: "2026" }

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
}

package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AchievementEntity
import com.example.data.local.GameEntryEntity
import com.example.data.local.LeaderboardEntryEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.TextAdventureSaveEntity
import com.example.data.local.ThemePreferencesRepository
import com.example.data.local.VaultDatabase
import com.example.data.model.ControlConfig
import com.example.data.model.GameEngineType
import com.example.data.model.LibraryFilter
import com.example.data.model.PlayerTier
import com.example.data.model.VaultPreferences
import com.example.data.model.VaultThemePreset
import com.example.data.repository.InspectedPackage
import com.example.data.repository.RewardEvent
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultUiState(
  val isReady: Boolean = false,
  val preferences: VaultPreferences = VaultPreferences(),
  val activeProfile: ProfileEntity? = null,
  val allProfiles: List<ProfileEntity> = emptyList(),
  val currentTier: PlayerTier = PlayerTier.HOBBYIST,
  val tierProgress: Float = 0f,
  val games: List<GameEntryEntity> = emptyList(),
  val filteredGames: List<GameEntryEntity> = emptyList(),
  val selectedFilter: LibraryFilter = LibraryFilter.ALL,
  val searchQuery: String = "",
  val achievements: List<AchievementEntity> = emptyList(),
  val leaderboardEntries: List<LeaderboardEntryEntity> = emptyList(),
  val textAdventureSave: TextAdventureSaveEntity? = null,
  val creatorUnlocked: Boolean = false,
  val creatorPin: String = "2026",
  val statusBanner: String? = null,
  val latestReward: RewardEvent? = null,
  val inspectedPackage: InspectedPackage? = null,
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
  private val database = VaultDatabase.getInstance(application)
  private val prefsRepo = ThemePreferencesRepository(application)
  private val repository = VaultRepository(application, database.vaultDao(), prefsRepo)

  private val selectedFilter = MutableStateFlow(LibraryFilter.ALL)
  private val searchQuery = MutableStateFlow("")
  private val creatorUnlocked = MutableStateFlow(false)
  private val statusBanner = MutableStateFlow<String?>(null)
  private val latestReward = MutableStateFlow<RewardEvent?>(null)
  private val inspectedPackage = MutableStateFlow<InspectedPackage?>(null)
  private val seeded = MutableStateFlow(false)

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  private val activeProfileFlow =
    repository.preferencesFlow.flatMapLatest { prefs ->
      repository.observeProfile(prefs.activeProfileId)
    }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  private val textSaveFlow =
    repository.preferencesFlow.flatMapLatest { prefs ->
      repository.observeTextSave(prefs.activeProfileId)
    }

  private val libraryControlsFlow =
    combine(
      selectedFilter,
      searchQuery,
      creatorUnlocked,
      statusBanner,
      latestReward,
    ) { filter, query, unlocked, banner, reward ->
      LibraryControlTuple(filter, query, unlocked, banner, reward)
    }

  val uiState: StateFlow<VaultUiState> =
    combine(
      seeded,
      repository.preferencesFlow,
      activeProfileFlow,
      repository.profilesFlow,
      repository.gamesFlow,
      repository.achievementsFlow,
      repository.leaderboardFlow,
      textSaveFlow,
      repository.creatorPinFlow,
      libraryControlsFlow,
      inspectedPackage,
    ) { values ->
      val isSeeded = values[0] as Boolean
      val prefs = values[1] as VaultPreferences
      val activeProfile = values[2] as ProfileEntity?
      @Suppress("UNCHECKED_CAST")
      val profiles = values[3] as List<ProfileEntity>
      @Suppress("UNCHECKED_CAST")
      val games = values[4] as List<GameEntryEntity>
      @Suppress("UNCHECKED_CAST")
      val achievements = values[5] as List<AchievementEntity>
      @Suppress("UNCHECKED_CAST")
      val leaderboard = values[6] as List<LeaderboardEntryEntity>
      val textSave = values[7] as TextAdventureSaveEntity?
      val pin = values[8] as String
      val controls = values[9] as LibraryControlTuple
      val inspected = values[10] as InspectedPackage?

      val xp = activeProfile?.totalXp ?: 0
      val tier = PlayerTier.fromXp(xp)
      val progress = PlayerTier.progressInTier(xp)

      val filtered =
        games.filter { game ->
          val matchesQuery =
            controls.query.isBlank() ||
              game.title.contains(controls.query, ignoreCase = true) ||
              game.genre.contains(controls.query, ignoreCase = true)
          val matchesFilter =
            when (controls.filter) {
              LibraryFilter.ALL -> true
              LibraryFilter.PLATFORMER_WEB ->
                game.engineType == GameEngineType.WEB_CANVAS.name
              LibraryFilter.THREE_D ->
                game.engineType == GameEngineType.DUNGEON_3D.name ||
                  game.engineType == GameEngineType.RACING_3D.name ||
                  game.engineType == GameEngineType.VICE_SANDBOX.name
              LibraryFilter.STORY ->
                game.engineType == GameEngineType.TEXT_ADVENTURE.name
              LibraryFilter.PRO -> game.requiredTier == PlayerTier.PRO.name
            }
          matchesQuery && matchesFilter
        }

      VaultUiState(
        isReady = isSeeded,
        preferences = prefs,
        activeProfile = activeProfile ?: profiles.firstOrNull(),
        allProfiles = profiles,
        currentTier = tier,
        tierProgress = progress,
        games = games,
        filteredGames = filtered,
        selectedFilter = controls.filter,
        searchQuery = controls.query,
        achievements = achievements,
        leaderboardEntries = leaderboard,
        textAdventureSave = textSave,
        creatorUnlocked = controls.unlocked,
        creatorPin = pin,
        statusBanner = controls.banner,
        latestReward = controls.reward,
        inspectedPackage = inspected,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(),
      )

  init {
    viewModelScope.launch {
      repository.ensureSeeded()
      seeded.value = true
    }
  }

  fun setFilter(filter: LibraryFilter) {
    selectedFilter.value = filter
  }

  fun setSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun toggleDarkMode() {
    viewModelScope.launch { repository.toggleDarkMode() }
  }

  fun selectThemePreset(preset: VaultThemePreset) {
    val tier = uiState.value.currentTier
    if (tier.levelIndex >= preset.requiredTier.levelIndex) {
      viewModelScope.launch { repository.setThemePreset(preset) }
    } else {
      statusBanner.value = "Reach ${preset.requiredTier.displayName} tier (${preset.requiredTier.minXp} XP) to unlock ${preset.displayName}."
    }
  }

  fun updateControlConfig(transform: (ControlConfig) -> ControlConfig) {
    viewModelScope.launch { repository.updateControlConfig(transform) }
  }

  fun switchProfile(profileId: Long) {
    viewModelScope.launch {
      repository.setActiveProfile(profileId)
      statusBanner.value = "Switched offline traveler profile."
    }
  }

  fun createProfile(name: String, avatarIcon: String) {
    viewModelScope.launch {
      repository.createProfile(name, avatarIcon)
      statusBanner.value = "Created traveler profile \"$name\"."
    }
  }

  fun onGameLaunched(gameId: String) {
    val profileId = uiState.value.activeProfile?.id ?: 1L
    viewModelScope.launch {
      val reward = repository.recordGameLaunch(gameId, profileId)
      if (reward != null) latestReward.value = reward
    }
  }

  fun awardGameProgress(
    gameId: String,
    xpDelta: Int,
    score: Int? = null,
    achievementId: String? = null,
  ) {
    val profileId = uiState.value.activeProfile?.id ?: 1L
    viewModelScope.launch {
      val reward = repository.awardProgress(profileId, gameId, xpDelta, score, achievementId)
      if (reward != null && (reward.promotedToTier != null || reward.unlockedAchievementTitle != null)) {
        latestReward.value = reward
      }
    }
  }

  fun saveTextAdventureState(save: TextAdventureSaveEntity) {
    viewModelScope.launch { repository.saveTextAdventure(save) }
  }

  fun unlockCreatorMode(enteredPin: String): Boolean {
    val ok = enteredPin.trim() == uiState.value.creatorPin
    creatorUnlocked.value = ok
    statusBanner.value = if (ok) "Creator Studio unlocked." else "Incorrect Creator PIN."
    return ok
  }

  fun lockCreatorMode() {
    creatorUnlocked.value = false
  }

  fun updateCreatorPin(newPin: String) {
    if (newPin.trim().length >= 4) {
      viewModelScope.launch {
        repository.setCreatorPin(newPin.trim())
        statusBanner.value = "Creator PIN updated."
      }
    }
  }

  fun addCreatorGame(
    title: String,
    subtitle: String,
    genre: String,
    engineType: GameEngineType,
    requiredTier: PlayerTier,
    versionTag: String,
    sizeLabel: String,
    description: String,
    releaseNote: String,
    customHtml: String? = null,
  ) {
    viewModelScope.launch {
      repository.addCreatorGame(
        title = title,
        subtitle = subtitle,
        genre = genre,
        engineType = engineType,
        requiredTier = requiredTier,
        versionTag = versionTag,
        sizeLabel = sizeLabel,
        description = description,
        releaseNote = releaseNote,
        customHtml = customHtml,
      )
      inspectedPackage.value = null
      statusBanner.value = "Added \"$title\" to VaultPlay Library."
    }
  }

  fun removeCreatorGame(gameId: String) {
    viewModelScope.launch {
      repository.deleteCreatorGame(gameId)
      statusBanner.value = "Removed creator drop from library."
    }
  }

  fun inspectPackageUri(uri: Uri) {
    viewModelScope.launch {
      val result = repository.inspectLocalPackageFile(uri)
      result
        .onSuccess { pkg ->
          inspectedPackage.value = pkg
          statusBanner.value = "Inspected package: ${pkg.title} (${pkg.version})"
        }
        .onFailure { err ->
          statusBanner.value = "Could not read package: ${err.message ?: "Invalid file"}"
        }
    }
  }

  fun clearInspectedPackage() {
    inspectedPackage.value = null
  }

  suspend fun buildOfflineBackupJson(): String = repository.exportOfflineBackupJson()

  fun importOfflineBackupJson(rawJson: String) {
    viewModelScope.launch {
      repository
        .importOfflineBackupJson(rawJson)
        .onSuccess { msg -> statusBanner.value = msg }
        .onFailure { err -> statusBanner.value = "Import failed: ${err.message}" }
    }
  }

  fun clearStatusBanner() {
    statusBanner.value = null
  }

  fun clearLatestReward() {
    latestReward.value = null
  }

  private data class LibraryControlTuple(
    val filter: LibraryFilter,
    val query: String,
    val unlocked: Boolean,
    val banner: String?,
    val reward: RewardEvent?,
  )
}

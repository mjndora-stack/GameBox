package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.data.local.AchievementEntity
import com.example.data.local.GameEntryEntity
import com.example.data.local.LeaderboardEntryEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.ThemePreferencesRepository
import com.example.data.local.VaultDao
import com.example.data.local.VaultDatabase
import com.example.data.model.ControlConfig
import com.example.data.model.FighterStat
import com.example.data.model.GameEngineType
import com.example.data.model.PlayerTier
import com.example.data.model.VaultPreferences
import com.example.data.model.VaultThemePreset
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RewardEvent(
  val xpGained: Int,
  val newTotalXp: Int,
  val promotedToTier: PlayerTier? = null,
  val unlockedAchievementTitle: String? = null,
)

class VaultRepository(
  private val context: Context,
  private val dao: VaultDao,
  private val prefs: ThemePreferencesRepository,
) {
  val preferencesFlow: Flow<VaultPreferences> = prefs.preferencesFlow
  val creatorPinFlow: Flow<String> = prefs.creatorPinFlow
  val gamesFlow: Flow<List<GameEntryEntity>> = dao.observeGames()
  val profilesFlow: Flow<List<ProfileEntity>> = dao.observeProfiles()
  val achievementsFlow: Flow<List<AchievementEntity>> = dao.observeAchievements()
  val leaderboardFlow: Flow<List<LeaderboardEntryEntity>> = dao.observeAllLeaderboardEntries()

  fun observeProfile(profileId: Long): Flow<ProfileEntity?> = dao.observeProfile(profileId)

  suspend fun ensureSeeded() {
    val defaultGames = VaultDatabase.defaultGames()
    val existingById = dao.getAllGamesOnce().associateBy { it.id }
    dao.deleteRemovedCuratedGames(defaultGames.map { it.id })
    // Preserve player high scores/play counts while syncing curated catalog metadata
    val synced =
      defaultGames.map { def ->
        val old = existingById[def.id]
        if (old != null) {
          def.copy(
            highScore = old.highScore,
            playCount = old.playCount,
            lastPlayedAt = old.lastPlayedAt,
          )
        } else {
          def
        }
      }
    dao.insertGames(synced)

    dao.insertAchievementsIfMissing(VaultDatabase.defaultAchievements())
    val existingGhostGames =
      dao.getAllLeaderboardOnce().filter { it.isGhostRival }.map { it.gameId }.toSet()
    val missingGhosts =
      VaultDatabase.defaultGhostRivals().filter { it.gameId !in existingGhostGames }
    if (missingGhosts.isNotEmpty()) {
      dao.insertLeaderboardEntries(missingGhosts)
    }
    if (dao.getProfileCount() == 0) {
      val id =
        dao.insertProfile(
          ProfileEntity(
            name = "Traveler One",
            avatarIcon = "rocket",
            totalXp = 0,
            vaultCredits = 100,
          )
        )
      prefs.setActiveProfileId(id)
    }
  }

  suspend fun toggleDarkMode() = prefs.toggleDarkMode()

  suspend fun setThemePreset(preset: VaultThemePreset) = prefs.setThemePreset(preset)

  suspend fun setActiveProfile(profileId: Long) = prefs.setActiveProfileId(profileId)

  suspend fun toggleMusicEnabled() = prefs.toggleMusicEnabled()

  suspend fun toggleLobbySfxEnabled() = prefs.toggleLobbySfxEnabled()

  suspend fun upgradeFighterStat(
    profileId: Long,
    fighterId: String,
    stat: FighterStat,
    cost: Int,
  ): Boolean {
    if (cost <= 0) return false
    val spent = dao.spendCredits(profileId, cost)
    if (spent <= 0) return false
    prefs.upgradeFighterStat(fighterId, stat)
    return true
  }

  suspend fun updateControlConfig(transform: (ControlConfig) -> ControlConfig) =
    prefs.updateControlConfig(transform)

  suspend fun createProfile(name: String, avatarIcon: String): Long {
    val cleanName = name.trim().take(24).ifEmpty { "Traveler" }
    val newId = dao.insertProfile(ProfileEntity(name = cleanName, avatarIcon = avatarIcon))
    prefs.setActiveProfileId(newId)
    return newId
  }

  suspend fun recordGameLaunch(gameId: String, profileId: Long): RewardEvent? {
    dao.recordGameLaunch(gameId, System.currentTimeMillis())
    val profile = dao.getProfileOnce(profileId) ?: return null
    dao.updateProfile(profile.copy(gamesPlayedCount = profile.gamesPlayedCount + 1))
    return unlockAchievementInternal("first_launch", profileId)
  }

  suspend fun awardProgress(
    profileId: Long,
    gameId: String,
    xpDelta: Int,
    score: Int? = null,
    achievementId: String? = null,
  ): RewardEvent? {
    val profile = dao.getProfileOnce(profileId) ?: return null
    val prevTier = PlayerTier.fromXp(profile.totalXp)

    var extraXp = 0
    var unlockedTitle: String? = null

    if (achievementId != null) {
      val ach = dao.getAchievementOnce(achievementId)
      if (ach != null && ach.unlockedAt == null) {
        val updated = dao.markAchievementUnlocked(achievementId, System.currentTimeMillis())
        if (updated > 0) {
          extraXp += ach.xpReward
          unlockedTitle = ach.title
        }
      }
    }

    val safeDelta = xpDelta.coerceAtLeast(0) + extraXp
    val newTotalXp = profile.totalXp + safeDelta
    val newCredits = profile.vaultCredits + (safeDelta / 5).coerceAtLeast(1)
    val newTier = PlayerTier.fromXp(newTotalXp)

    dao.updateProfile(profile.copy(totalXp = newTotalXp, vaultCredits = newCredits))

    // Auto-unlock rank milestone badges
    if (newTotalXp >= PlayerTier.ENTHUSIAST.minXp) {
      unlockedTitle = unlockedTitle ?: unlockAchievementBadgeOnly("tier_enthusiast")
    }
    if (newTotalXp >= PlayerTier.EXPERT.minXp) {
      unlockedTitle = unlockedTitle ?: unlockAchievementBadgeOnly("tier_expert")
    }
    if (newTotalXp >= PlayerTier.MASTER.minXp) {
      unlockedTitle = unlockedTitle ?: unlockAchievementBadgeOnly("tier_master")
    }
    if (newTotalXp >= PlayerTier.LEGEND.minXp) {
      unlockedTitle = unlockedTitle ?: unlockAchievementBadgeOnly("tier_legend")
    }
    if (newTotalXp >= PlayerTier.MYTHIC.minXp) {
      unlockedTitle = unlockedTitle ?: unlockAchievementBadgeOnly("tier_mythic")
    }

    if (score != null && score > 0) {
      dao.updateHighScoreIfBetter(gameId, score)
      val existing = dao.getPlayerGameLeaderboard(gameId, profile.name)
      if (existing == null || score > existing.score) {
        dao.insertLeaderboardEntry(
          LeaderboardEntryEntity(
            id = existing?.id ?: 0L,
            gameId = gameId,
            playerName = profile.name,
            score = score,
            tierName = newTier.displayName,
            isGhostRival = false,
            recordedAt = System.currentTimeMillis(),
          )
        )
      }
    }

    return RewardEvent(
      xpGained = safeDelta,
      newTotalXp = newTotalXp,
      promotedToTier = if (newTier != prevTier) newTier else null,
      unlockedAchievementTitle = unlockedTitle,
    )
  }

  private suspend fun unlockAchievementInternal(
    achievementId: String,
    profileId: Long,
  ): RewardEvent? {
    val ach = dao.getAchievementOnce(achievementId) ?: return null
    if (ach.unlockedAt != null) return null
    val updated = dao.markAchievementUnlocked(achievementId, System.currentTimeMillis())
    if (updated <= 0) return null
    val profile = dao.getProfileOnce(profileId) ?: return null
    val prevTier = PlayerTier.fromXp(profile.totalXp)
    val newXp = profile.totalXp + ach.xpReward
    val newTier = PlayerTier.fromXp(newXp)
    dao.updateProfile(
      profile.copy(totalXp = newXp, vaultCredits = profile.vaultCredits + ach.xpReward / 5)
    )
    return RewardEvent(
      xpGained = ach.xpReward,
      newTotalXp = newXp,
      promotedToTier = if (newTier != prevTier) newTier else null,
      unlockedAchievementTitle = ach.title,
    )
  }

  private suspend fun unlockAchievementBadgeOnly(achievementId: String): String? {
    val ach = dao.getAchievementOnce(achievementId) ?: return null
    if (ach.unlockedAt != null) return null
    val updated = dao.markAchievementUnlocked(achievementId, System.currentTimeMillis())
    return if (updated > 0) ach.title else null
  }

  suspend fun addCreatorGame(
    title: String,
    subtitle: String,
    genre: String,
    engineType: GameEngineType,
    requiredTier: PlayerTier,
    versionTag: String,
    sizeLabel: String,
    description: String,
    releaseNote: String,
    customHtml: String?,
  ) {
    val id =
      title
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "vault_drop_${System.currentTimeMillis()}" } +
        "_" +
        (System.currentTimeMillis() % 10000)
    val existingGames = dao.getAllGamesOnce()
    val maxOrder = existingGames.maxOfOrNull { it.sortOrder } ?: 14
    val assetPath =
      when (engineType) {
        GameEngineType.WEB_CANVAS -> "file:///android_asset/games/neon_hop.html"
        GameEngineType.ARCADE_NATIVE -> "arcade://flappy_bird"
        GameEngineType.PUZZLE_NATIVE -> "puzzle://prism_2048"
      }
    dao.insertGame(
      GameEntryEntity(
        id = id,
        title = title.trim().ifEmpty { "Untitled Drop" },
        subtitle = subtitle.trim().ifEmpty { "Creator Curated Release" },
        genre = genre.trim().ifEmpty { engineType.label },
        engineType = engineType.name,
        requiredTier = requiredTier.name,
        versionTag = versionTag.trim().ifEmpty { "v1.0.0" },
        sizeLabel = sizeLabel.trim().ifEmpty { "Embedded" },
        assetPath = assetPath,
        customHtmlContent = customHtml,
        coverKey =
          when (engineType) {
            GameEngineType.WEB_CANVAS -> "neon_hop"
            GameEngineType.ARCADE_NATIVE -> "street_fighter_2d"
            GameEngineType.PUZZLE_NATIVE -> "prism_2048"
          },
        description = description.trim().ifEmpty { "Added via Creator Studio release manager." },
        controlsHint = "Adaptive Touch + Keyboard controls",
        releaseNote = releaseNote.trim().ifEmpty { "GitHub / Itch Vault Drop" },
        sortOrder = maxOrder + 1,
        isCreatorAdded = true,
      )
    )
  }

  suspend fun deleteCreatorGame(gameId: String) = dao.deleteCreatorGame(gameId)

  suspend fun inspectLocalPackageFile(uri: Uri): Result<InspectedPackage> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val fileName =
          resolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
          } ?: "package"

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val sizeKb = (bytes.size / 1024).coerceAtLeast(1)
        val sizeLabel = if (sizeKb >= 1024) String.format("%.1f MB", sizeKb / 1024f) else "$sizeKb KB"

        if (fileName.endsWith(".html", ignoreCase = true) ||
          fileName.endsWith(".htm", ignoreCase = true)
        ) {
          val html = bytes.decodeToString()
          val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
          val title = titleMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: fileName.substringBeforeLast('.')
          InspectedPackage(
            title = title,
            subtitle = "Imported HTML5 Canvas Cartridge",
            version = "v1.0.0",
            sizeLabel = "$sizeLabel · HTML5",
            engineType = GameEngineType.WEB_CANVAS,
            htmlContent = html,
            notes = "Self-contained HTML5 cartridge ready to embed in the offline WebView engine.",
          )
        } else if (fileName.endsWith(".apk", ignoreCase = true)) {
          val tempFile = File(context.cacheDir, "inspect_temp.apk")
          FileOutputStream(tempFile).use { it.write(bytes) }
          val pm = context.packageManager
          val info =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
              @Suppress("DEPRECATION")
              pm.getPackageArchiveInfo(tempFile.absolutePath, 0)
            }
          val appLabel =
            info?.applicationInfo?.let { appInfo ->
              appInfo.sourceDir = tempFile.absolutePath
              appInfo.publicSourceDir = tempFile.absolutePath
              pm.getApplicationLabel(appInfo).toString()
            } ?: fileName.substringBeforeLast('.')
          val versionName = info?.versionName ?: "1.0.0"
          val pkgName = info?.packageName ?: "com.creator.game"
          tempFile.delete()
          InspectedPackage(
            title = appLabel,
            subtitle = "Cataloged APK Build ($pkgName)",
            version = "v$versionName",
            sizeLabel = "$sizeLabel · APK Manifest",
            engineType = GameEngineType.WEB_CANVAS,
            htmlContent = null,
            notes =
              "Manifest metadata extracted ($pkgName v$versionName). Android security blocks executing external APK DEX binaries inside another app; bundle this release's HTML5/Native module in your next GitHub/Itch APK build.",
          )
        } else {
          val jsonText = bytes.decodeToString()
          val json = JSONObject(jsonText)
          InspectedPackage(
            title = json.optString("title", fileName.substringBeforeLast('.')),
            subtitle = json.optString("subtitle", "JSON Cartridge Manifest"),
            version = json.optString("version", "v1.0.0"),
            sizeLabel = "$sizeLabel · Manifest",
            engineType =
              runCatching { GameEngineType.valueOf(json.optString("engineType", "WEB_CANVAS")) }
                .getOrDefault(GameEngineType.WEB_CANVAS),
            htmlContent = json.optString("html", "").takeIf { it.isNotBlank() },
            notes = json.optString("releaseNote", "Imported from JSON release manifest."),
          )
        }
      }
    }

  suspend fun exportOfflineBackupJson(): String =
    withContext(Dispatchers.IO) {
      val root = JSONObject()
      root.put("format", "vaultplay_offline_backup_v1")
      root.put("exportedAt", System.currentTimeMillis())

      val profilesArr = JSONArray()
      dao.getAllProfilesOnce().forEach { p ->
        profilesArr.put(
          JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("avatarIcon", p.avatarIcon)
            put("totalXp", p.totalXp)
            put("vaultCredits", p.vaultCredits)
            put("gamesPlayedCount", p.gamesPlayedCount)
          }
        )
      }
      root.put("profiles", profilesArr)

      val achArr = JSONArray()
      dao.getAllAchievementsOnce().filter { it.unlockedAt != null }.forEach { a ->
        achArr.put(
          JSONObject().apply {
            put("id", a.id)
            put("unlockedAt", a.unlockedAt)
          }
        )
      }
      root.put("unlockedAchievements", achArr)

      val scoresArr = JSONArray()
      dao.getAllGamesOnce().forEach { g ->
        scoresArr.put(
          JSONObject().apply {
            put("id", g.id)
            put("highScore", g.highScore)
            put("playCount", g.playCount)
          }
        )
      }
      root.put("gameProgress", scoresArr)
      root.toString(2)
    }

  suspend fun importOfflineBackupJson(rawJson: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val root = JSONObject(rawJson)
        require(root.optString("format").startsWith("vaultplay_offline_backup")) {
          "Unrecognized backup file format."
        }
        val profilesArr = root.optJSONArray("profiles")
        if (profilesArr != null && profilesArr.length() > 0) {
          for (i in 0 until profilesArr.length()) {
            val obj = profilesArr.getJSONObject(i)
            val existing = dao.getProfileOnce(obj.optLong("id", -1L))
            val importedXp = obj.optInt("totalXp", 0)
            if (existing != null) {
              dao.updateProfile(
                existing.copy(
                  totalXp = maxOf(existing.totalXp, importedXp),
                  vaultCredits = maxOf(existing.vaultCredits, obj.optInt("vaultCredits", 100)),
                  gamesPlayedCount = maxOf(existing.gamesPlayedCount, obj.optInt("gamesPlayedCount", 0)),
                )
              )
            } else {
              dao.insertProfile(
                ProfileEntity(
                  name = obj.optString("name", "Traveler"),
                  avatarIcon = obj.optString("avatarIcon", "rocket"),
                  totalXp = importedXp,
                  vaultCredits = obj.optInt("vaultCredits", 100),
                  gamesPlayedCount = obj.optInt("gamesPlayedCount", 0),
                )
              )
            }
          }
        }
        val achArr = root.optJSONArray("unlockedAchievements")
        if (achArr != null) {
          for (i in 0 until achArr.length()) {
            val obj = achArr.getJSONObject(i)
            dao.markAchievementUnlocked(obj.getString("id"), obj.optLong("unlockedAt", System.currentTimeMillis()))
          }
        }
        val gamesArr = root.optJSONArray("gameProgress")
        if (gamesArr != null) {
          for (i in 0 until gamesArr.length()) {
            val obj = gamesArr.getJSONObject(i)
            dao.updateHighScoreIfBetter(obj.getString("id"), obj.optInt("highScore", 0))
          }
        }
        "Offline backup merged successfully."
      }
    }
}

data class InspectedPackage(
  val title: String,
  val subtitle: String,
  val version: String,
  val sizeLabel: String,
  val engineType: GameEngineType,
  val htmlContent: String?,
  val notes: String,
)

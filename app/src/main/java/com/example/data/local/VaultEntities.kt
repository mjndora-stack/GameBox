package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val avatarIcon: String = "gamepad",
  val totalXp: Int = 0,
  val vaultCredits: Int = 100,
  val gamesPlayedCount: Int = 0,
  val totalPlaySeconds: Long = 0L,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "games")
data class GameEntryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val subtitle: String,
  val genre: String,
  val engineType: String,
  val requiredTier: String,
  val versionTag: String,
  val sizeLabel: String,
  val assetPath: String,
  val customHtmlContent: String? = null,
  val coverKey: String,
  val description: String,
  val controlsHint: String,
  val releaseNote: String,
  val highScore: Int = 0,
  val playCount: Int = 0,
  val lastPlayedAt: Long = 0L,
  val sortOrder: Int = 0,
  val isCreatorAdded: Boolean = false,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val iconKey: String,
  val xpReward: Int,
  val gameId: String? = null,
  val unlockedAt: Long? = null,
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val gameId: String,
  val playerName: String,
  val score: Int,
  val tierName: String,
  val isGhostRival: Boolean = false,
  val recordedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "text_adventure_saves")
data class TextAdventureSaveEntity(
  @PrimaryKey val profileId: Long,
  val currentNodeId: String,
  val hp: Int,
  val focus: Int,
  val inventoryCsv: String,
  val cluesCsv: String,
  val visitedNodesCsv: String,
  val endingsCount: Int,
  val updatedAt: Long = System.currentTimeMillis(),
)

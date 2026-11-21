package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
  // Profiles
  @Query("SELECT * FROM profiles ORDER BY totalXp DESC, id ASC")
  fun observeProfiles(): Flow<List<ProfileEntity>>

  @Query("SELECT * FROM profiles WHERE id = :id")
  fun observeProfile(id: Long): Flow<ProfileEntity?>

  @Query("SELECT * FROM profiles WHERE id = :id")
  suspend fun getProfileOnce(id: Long): ProfileEntity?

  @Query("SELECT * FROM profiles ORDER BY id ASC")
  suspend fun getAllProfilesOnce(): List<ProfileEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProfile(profile: ProfileEntity): Long

  @Update
  suspend fun updateProfile(profile: ProfileEntity)

  @Query("UPDATE profiles SET vaultCredits = vaultCredits - :cost WHERE id = :profileId AND vaultCredits >= :cost")
  suspend fun spendCredits(profileId: Long, cost: Int): Int

  @Query("SELECT COUNT(*) FROM profiles")
  suspend fun getProfileCount(): Int

  // Games
  @Query("SELECT * FROM games ORDER BY sortOrder ASC, title ASC")
  fun observeGames(): Flow<List<GameEntryEntity>>

  @Query("SELECT * FROM games ORDER BY sortOrder ASC")
  suspend fun getAllGamesOnce(): List<GameEntryEntity>

  @Query("SELECT * FROM games WHERE id = :gameId")
  suspend fun getGameOnce(gameId: String): GameEntryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGames(games: List<GameEntryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGame(game: GameEntryEntity)

  @Query("DELETE FROM games WHERE isCreatorAdded = 0 AND id NOT IN (:validIds)")
  suspend fun deleteRemovedCuratedGames(validIds: List<String>)

  @Query("UPDATE games SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :gameId")
  suspend fun recordGameLaunch(gameId: String, timestamp: Long)

  @Query("UPDATE games SET highScore = :score WHERE id = :gameId AND highScore < :score")
  suspend fun updateHighScoreIfBetter(gameId: String, score: Int)

  @Query("DELETE FROM games WHERE id = :gameId AND isCreatorAdded = 1")
  suspend fun deleteCreatorGame(gameId: String)

  // Achievements
  @Query("SELECT * FROM achievements ORDER BY unlockedAt IS NULL ASC, xpReward ASC")
  fun observeAchievements(): Flow<List<AchievementEntity>>

  @Query("SELECT * FROM achievements")
  suspend fun getAllAchievementsOnce(): List<AchievementEntity>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAchievementsIfMissing(items: List<AchievementEntity>)

  @Query("SELECT * FROM achievements WHERE id = :id")
  suspend fun getAchievementOnce(id: String): AchievementEntity?

  @Query("UPDATE achievements SET unlockedAt = :timestamp WHERE id = :id AND unlockedAt IS NULL")
  suspend fun markAchievementUnlocked(id: String, timestamp: Long): Int

  // Leaderboards
  @Query("SELECT * FROM leaderboard_entries ORDER BY score DESC")
  fun observeAllLeaderboardEntries(): Flow<List<LeaderboardEntryEntity>>

  @Query("SELECT * FROM leaderboard_entries")
  suspend fun getAllLeaderboardOnce(): List<LeaderboardEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLeaderboardEntry(entry: LeaderboardEntryEntity)

  @Query("SELECT * FROM leaderboard_entries WHERE gameId = :gameId AND playerName = :playerName AND isGhostRival = 0 LIMIT 1")
  suspend fun getPlayerGameLeaderboard(gameId: String, playerName: String): LeaderboardEntryEntity?
}

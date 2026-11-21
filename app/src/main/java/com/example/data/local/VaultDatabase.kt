package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GameEngineType
import com.example.data.model.PlayerTier

@Database(
  entities = [
    ProfileEntity::class,
    GameEntryEntity::class,
    AchievementEntity::class,
    LeaderboardEntryEntity::class,
    TextAdventureSaveEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
  abstract fun vaultDao(): VaultDao

  companion object {
    @Volatile private var INSTANCE: VaultDatabase? = null

    fun getInstance(context: Context): VaultDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                VaultDatabase::class.java,
                "vaultplay_offline.db",
              )
              .build()
              .also { INSTANCE = it }
        }

    fun defaultGames(): List<GameEntryEntity> =
      listOf(
        GameEntryEntity(
          id = "neon_hop",
          title = "Neon Hop",
          subtitle = "2D Precision Cyber Platformer",
          genre = "Platformer · Web",
          engineType = GameEngineType.WEB_CANVAS.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.4.0",
          sizeLabel = "18 KB · HTML5",
          assetPath = "file:///android_asset/games/neon_hop.html",
          coverKey = "neon_hop",
          description =
            "Three handcrafted cybernetic stages running inside the embedded WebView Canvas engine. Double-jump across moving bridges, collect orbs, and reach the emerald beacon.",
          controlsHint = "Left / Right arrows + Double-Jump button (or A/D + Space)",
          releaseNote = "Vault Launch Drop · 3 stages & VaultBridge XP sync",
          sortOrder = 1,
        ),
        GameEntryEntity(
          id = "skyline_dash",
          title = "Skyline Dash",
          subtitle = "High-Tempo Rooftop Runner",
          genre = "Platformer · Web",
          engineType = GameEngineType.WEB_CANVAS.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.2.0",
          sizeLabel = "16 KB · HTML5",
          assetPath = "file:///android_asset/games/skyline_dash.html",
          coverKey = "skyline_dash",
          description =
            "Sprint across twilight rooftops inside the embedded WebView engine. Chain double-jumps with Air-Dash to smash patrolling laser drones for bonus credits.",
          controlsHint = "Dash + Jump touch pads (or Shift + Space)",
          releaseNote = "Vault Launch Drop · Air-Dash combat & combo scaling",
          sortOrder = 2,
        ),
        GameEntryEntity(
          id = "ash_cipher",
          title = "Ash & Cipher",
          subtitle = "Branching Offline Mystery Fiction",
          genre = "Text Adventure",
          engineType = GameEngineType.TEXT_ADVENTURE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.1.0",
          sizeLabel = "Native · Ink",
          assetPath = "native://ash_cipher",
          coverKey = "ash_cipher",
          description =
            "Investigate an abandoned coastal observatory lit only by a brass lantern. Track HP, Focus, cipher relics, and discover multiple endings with automatic offline saves.",
          controlsHint = "Tap narrative choices · Auto-saves after every decision",
          releaseNote = "Vault Launch Drop · 14 interconnected story nodes & 3 endings",
          sortOrder = 3,
        ),
        GameEntryEntity(
          id = "crypt_echoes",
          title = "Crypt of Echoes",
          subtitle = "3D First-Person Dungeon Crawler",
          genre = "3D Dungeon RPG",
          engineType = GameEngineType.DUNGEON_3D.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.3.0",
          sizeLabel = "Native · 3D Raycast",
          assetPath = "native://crypt_echoes",
          coverKey = "crypt_echoes",
          description =
            "Explore 3 procedural torchlit stone floors rendered in real-time 3D first-person raycasting. Battle lurking wraiths, collect runed keys, loot chests, and unlock the emerald portal.",
          controlsHint = "D-Pad Move/Turn + Strike / Shield buttons (or WASD + Space)",
          releaseNote = "Vault Launch Drop · 3 floors, live minimap & 3D sprite depth",
          sortOrder = 4,
        ),
        GameEntryEntity(
          id = "apex_vector",
          title = "Apex Vector",
          subtitle = "3D Horizon Racing Simulation",
          genre = "3D Racing Sim",
          engineType = GameEngineType.RACING_3D.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.2.5",
          sizeLabel = "Native · 3D Horizon",
          assetPath = "native://apex_vector",
          coverKey = "apex_vector",
          description =
            "Carve through a 3-lap geometric coastal highway at 300+ km/h. Draft behind rival racers, fire Nitro Boost on straightaways, and chase Ghost Lap records.",
          controlsHint = "Left / Right steer + Throttle / Brake / Nitro pedals (or Arrow keys)",
          releaseNote = "Vault Launch Drop · Dynamic elevation & rival traffic AI",
          sortOrder = 5,
        ),
        GameEntryEntity(
          id = "vice_horizon",
          title = "Vice Horizon",
          subtitle = "Open-District Action Sandbox",
          genre = "3D/2.5D Open City · Pro",
          engineType = GameEngineType.VICE_SANDBOX.name,
          requiredTier = PlayerTier.PRO.name,
          versionTag = "v1.0.0-PRO",
          sizeLabel = "Native · Vice Engine",
          assetPath = "native://vice_horizon",
          coverKey = "vice_horizon",
          description =
            "Exclusive Pro-tier open-city sandbox inspired by Vice coastal action. Roam on foot or hijack neon speedsters, complete courier drops, evade Heat cruisers, and rule the bay.",
          controlsHint = "Analog Joystick + Action (Enter/Exit Car, Boost/Sprint) · Unlocks at Pro (3,000 XP)",
          releaseNote = "Pro Tier Milestone Exclusive · Unlocks automatically at 3,000 XP",
          sortOrder = 6,
        ),
      )

    fun defaultAchievements(): List<AchievementEntity> =
      listOf(
        AchievementEntity(
          id = "first_launch",
          title = "Vault Initiated",
          description = "Launch any embedded game inside VaultPlay.",
          iconKey = "rocket",
          xpReward = 75,
        ),
        AchievementEntity(
          id = "platform_pioneer",
          title = "Orb Collector",
          description = "Collect 10 energy orbs in Neon Hop.",
          iconKey = "bolt",
          xpReward = 150,
          gameId = "neon_hop",
        ),
        AchievementEntity(
          id = "neon_finisher",
          title = "Beacon Reached",
          description = "Clear a Neon Hop cyber stage.",
          iconKey = "flag",
          xpReward = 200,
          gameId = "neon_hop",
        ),
        AchievementEntity(
          id = "skyline_ace",
          title = "Rooftop Sprinter",
          description = "Score 600+ in a single Skyline Dash run.",
          iconKey = "speed",
          xpReward = 220,
          gameId = "skyline_dash",
        ),
        AchievementEntity(
          id = "lorekeeper",
          title = "Cipher Decoded",
          description = "Reach an ending in Ash & Cipher.",
          iconKey = "book",
          xpReward = 250,
          gameId = "ash_cipher",
        ),
        AchievementEntity(
          id = "dungeon_delver",
          title = "Crypt Conqueror",
          description = "Defeat a wraith or escape a floor in Crypt of Echoes.",
          iconKey = "shield",
          xpReward = 260,
          gameId = "crypt_echoes",
        ),
        AchievementEntity(
          id = "apex_finisher",
          title = "Checkered Horizon",
          description = "Complete a full 3-lap race in Apex Vector.",
          iconKey = "trophy",
          xpReward = 300,
          gameId = "apex_vector",
        ),
        AchievementEntity(
          id = "tier_enthusiast",
          title = "Enthusiast Rank",
          description = "Reach 500 shared XP across your library.",
          iconKey = "star",
          xpReward = 100,
        ),
        AchievementEntity(
          id = "tier_expert",
          title = "Expert Rank",
          description = "Reach 1,500 shared XP across your library.",
          iconKey = "verified",
          xpReward = 150,
        ),
        AchievementEntity(
          id = "vice_unlocked",
          title = "Pro Clearance: Vice",
          description = "Reach Pro tier (3,000 XP) and unlock Vice Horizon.",
          iconKey = "crown",
          xpReward = 500,
          gameId = "vice_horizon",
        ),
      )

    fun defaultGhostRivals(): List<LeaderboardEntryEntity> =
      listOf(
        LeaderboardEntryEntity(gameId = "neon_hop", playerName = "Ghost · Dev_Kael", score = 1450, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "neon_hop", playerName = "Ghost · Nova99", score = 950, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "skyline_dash", playerName = "Ghost · Dev_Kael", score = 1820, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "skyline_dash", playerName = "Ghost · Aero", score = 1100, tierName = "Enthusiast", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "ash_cipher", playerName = "Ghost · Archivist", score = 900, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "crypt_echoes", playerName = "Ghost · Warden", score = 1600, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "crypt_echoes", playerName = "Ghost · Delver", score = 850, tierName = "Enthusiast", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "apex_vector", playerName = "Ghost · ApexZero", score = 2400, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "apex_vector", playerName = "Ghost · DriftX", score = 1500, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "vice_horizon", playerName = "Ghost · ViceKing", score = 3200, tierName = "Pro", isGhostRival = true),
      )
  }
}

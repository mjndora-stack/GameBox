package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DailyMissionType
import com.example.data.model.GameEngineType
import com.example.data.model.PlayerTier
import java.util.Calendar

@Database(
  entities = [
    ProfileEntity::class,
    GameEntryEntity::class,
    AchievementEntity::class,
    LeaderboardEntryEntity::class,
    DailyMissionEntity::class,
    CosmeticBadgeEntity::class,
    GameQuickResumeEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
  abstract fun vaultDao(): VaultDao

  companion object {
    @Volatile private var INSTANCE: VaultDatabase? = null

    private val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE games ADD COLUMN howItWorks TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE games ADD COLUMN howToComplete TEXT NOT NULL DEFAULT ''")
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS game_quick_resumes (
              gameId TEXT NOT NULL PRIMARY KEY,
              summaryLabel TEXT NOT NULL,
              stateJson TEXT NOT NULL,
              updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
          )
        }
      }

    fun getInstance(context: Context): VaultDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                VaultDatabase::class.java,
                "vaultplay_offline.db",
              )
              .addMigrations(MIGRATION_3_4)
              .fallbackToDestructiveMigration(true)
              .build()
              .also { INSTANCE = it }
        }

    fun defaultGames(): List<GameEntryEntity> =
      listOf(
        // 2 Kept Platformers (Neon Hop marked Fan Favourite)
        GameEntryEntity(
          id = "neon_hop",
          title = "Neon Hop",
          subtitle = "Fan Favourite · 2D Cyber Platformer",
          genre = "Platformer · Web",
          engineType = GameEngineType.WEB_CANVAS.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v2.0.0",
          sizeLabel = "28 KB · HTML5",
          assetPath = "file:///android_asset/games/neon_hop.html",
          coverKey = "neon_hop",
          description =
            "Community Fan Favourite! 50 curated cybernetic stages across 6 visual sectors plus Endless Infinity Mode (51+). Double-jump across moving bridges, bounce off neon jump pads, dodge patrol drones, and enter the warp gate.",
          howItWorks =
            "You start every run with 3 HP and a mid-air double jump. Step onto neon jump pads for high launches, cross crumbling phase ledges quickly before they vanish, ride moving bridges, and collect floating green energy cores (+50 pts, +15 XP each). Reaching a warp gate restores 1 HP and advances seamlessly.",
          howToComplete =
            "Reach and enter the glowing Warp Gate at the right end of each sector to clear Stages 1–50, then continue indefinitely into Endless Infinity Sectors (51+). Collect 10+ total orbs to earn Fan Fav Orb Hunter and clear a stage for Neon Beacon Cleared.",
          controlsHint = "Left / Right arrows + Double-Jump button (or A/D + Space)",
          releaseNote = "Fan Favourite Edition · 50+ stages, bounce pads & Endless Mode",
          sortOrder = 1,
          isFanFavourite = true,
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
          howItWorks =
            "Your runner automatically accelerates across procedural skyscraper rooftops. Combine double-jumps to cross wide gaps, collect holo-credits (+25 pts each), and trigger Air-Dash when approaching armored hover drones—dashing turns you invincible briefly and smashes drones for +2 credits and bonus XP instead of ending the run.",
          howToComplete =
            "Skyline Dash is an endless survival distance runner—survive as long as possible without falling between buildings or colliding un-dashed with a drone. Reach 600+ points in a run to unlock Rooftop Sprinter and top the Ghost · Aero rival!",
          controlsHint = "Dash + Jump touch pads (or Shift + Space)",
          releaseNote = "Vault Drop · Air-Dash combat & combo scaling",
          sortOrder = 2,
        ),
        // 2 New Arcade Engines (Street Fighter 2D, Flappy Bird)
        GameEntryEntity(
          id = "street_fighter_2d",
          title = "Street Fighter 2D",
          subtitle = "Upgradable 2D Fighter · 1P CPU & 2P Friend",
          genre = "Fighting · Arcade",
          engineType = GameEngineType.ARCADE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.1.0",
          sizeLabel = "Native · Fighter",
          assetPath = "arcade://street_fighter_2d",
          coverKey = "street_fighter_2d",
          description =
            "Classic 2D side-view martial arts fighting with 3 upgradable fighters (Kai, Mira, Titan). Battle the CPU or challenge a friend on the same screen in 2P Friend Mode!",
          howItWorks =
            "Choose Kai, Mira, or Titan and spend Vault Credits to permanently upgrade Health, Attack, or Speed before entering the arena. Landing hits or blocking fills your Super Meter—at 50+ meter you can unleash a character Special projectile. Switch between 1P vs AI CPU or 2P Play with Friend on the same screen.",
          howToComplete =
            "Deplete your opponent's health bar to win a round—first fighter to win 2 rounds in a Best-of-3 match claims victory, XP, and Street Champion achievement rewards.",
          controlsHint = "Left/Right move · Block, Jump, Punch, Kick & SP Blast (Dual decks in 2P)",
          releaseNote = "Arcade Drop · Persistent Fighter Upgrades & Play with Friend 2P",
          sortOrder = 3,
        ),
        GameEntryEntity(
          id = "flappy_bird",
          title = "Flappy Bird",
          subtitle = "Precision Pipe-Weaving Arcade",
          genre = "Arcade · Reflex",
          engineType = GameEngineType.ARCADE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Arcade",
          assetPath = "arcade://flappy_bird",
          coverKey = "flappy_bird",
          description =
            "Tap anywhere to flap through narrow emerald pipe gates with crisp gravity physics, parallax skies, and escalating speed.",
          howItWorks =
            "Every tap gives the aviator bird an upward impulse against constant gravity. Each pipe pair you weave through awards +100 score points (+1 pipe), and every 5 pipes grants extra milestone XP as flight speed gradually increases and pipe gaps tighten.",
          howToComplete =
            "Flappy Bird is an endless arcade flight—stay airborne without touching the pipes, ground, or top ceiling. Clear 15 pipes (1,500 pts) in one run to unlock Pipe Weaver, and aim for 500+ pts to clear daily flight challenges.",
          controlsHint = "Tap anywhere on the canvas to flap upward",
          releaseNote = "Arcade Drop · Smooth 60fps flight & medal tiers",
          sortOrder = 4,
        ),
        GameEntryEntity(
          id = "block_blast",
          title = "Block Blast",
          subtitle = "8×8 Endless Line-Blaster · High Score",
          genre = "Block Puzzle · Endless",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Endless",
          assetPath = "puzzle://block_blast",
          coverKey = "block_blast",
          description =
            "Place polyomino cube clusters onto an 8×8 board to blast full rows and columns in endless play! Build multi-line combos, beat your personal High Score, or switch on Zen Endless shockwaves.",
          howItWorks =
            "Drag any of the 3 polyomino pieces from the tray (or tap a shape, then tap the 8×8 grid) to lock it into empty cells. Once all 3 pieces are placed, a fresh trio deals automatically. Completing any full 8-cell row or column blasts those lines clear and builds consecutive Combo streaks for massive bonus points. Toggle Zen ∞ if you want automatic center-lane shockwaves when jammed.",
          howToComplete =
            "Play endlessly to chase your highest High Score before running out of legal board space in Classic Endless mode. Reach 1,000+ points in a single run to unlock Cube Blaster.",
          controlsHint = "Drag shapes onto the 8×8 board or tap tray + tap cell · Toggle Endless / Zen ∞ at top",
          releaseNote = "Puzzle Drop · Endless Play, Combo Blasts & High Score tracking",
          sortOrder = 5,
        ),
        // Unlocked Puzzle Games (Distinct Mechanics)
        GameEntryEntity(
          id = "prism_2048",
          title = "Prism 2048",
          subtitle = "Sliding Power-Tile Merger",
          genre = "Number Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://prism_2048",
          coverKey = "prism_2048",
          description =
            "Slide numbered prism tiles across a 4×4 grid to merge matching powers of two and forge the 2048 core.",
          howItWorks =
            "Swiping or pressing a direction slides every tile on the 4×4 board as far as possible in that direction. Adjacent matching numbers merge into double their value (2→4→8→16...), adding that merged number to your score while a new 2 or 4 tile spawns.",
          howToComplete =
            "Merge tiles up to the 2048 tile (and keep pushing for higher scores until no merges remain). Reaching a 256+ tile or 1,200+ score unlocks the Prism Architect achievement.",
          controlsHint = "Swipe anywhere or tap D-Pad / Arrow keys",
          releaseNote = "Puzzle Pack · Smooth tile merge logic",
          sortOrder = 3,
        ),
        GameEntryEntity(
          id = "flux_circuit",
          title = "Flux Circuit",
          subtitle = "Rotate Conduit Pipe Network",
          genre = "Spatial Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://flux_circuit",
          coverKey = "flux_circuit",
          description =
            "Tap conduit tiles to rotate 90° and route live energy from the Source cell to every Terminal node simultaneously.",
          howItWorks =
            "Current flows outward in real time from the amber Source (S) tile at top-left along any connected cyan conduit arms. Tapping any non-source tile rotates its pipe joints 90° clockwise, illuminating newly connected paths in bright teal.",
          howToComplete =
            "Route continuous power from S to BOTH blue Terminal (T) nodes at the same time to solve the stage, unlock Live Conduit, and advance to the next scrambled stage.",
          controlsHint = "Tap any conduit tile to rotate 90° clockwise",
          releaseNote = "Puzzle Pack · Multi-stage circuit routing",
          sortOrder = 4,
        ),
        GameEntryEntity(
          id = "vault_sweeper",
          title = "Vault Sweeper",
          subtitle = "Tactical Minefield Deduction",
          genre = "Logic Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://vault_sweeper",
          coverKey = "vault_sweeper",
          description =
            "Sweep an 8×8 sector using proximity clues. Switch between Probe and Flag modes to mark all hidden hazards.",
          howItWorks =
            "Ten mines are hidden beneath the 8×8 grid (your very first tap is always safe!). Probing an empty tile flood-fills open safe zones and shows numbers indicating how many of the 8 surrounding cells contain mines. Toggle Flag Mode to pin suspected hazards.",
          howToComplete =
            "Uncover all 54 safe tiles without detonating any of the 10 hidden mines to clear the sector and earn Zero Detonation (+110 XP).",
          controlsHint = "Tap cells to reveal · Toggle Flag mode to mark mines",
          releaseNote = "Puzzle Pack · First-tap safe flood-fill",
          sortOrder = 5,
        ),
        GameEntryEntity(
          id = "lumen_toggle",
          title = "Lumen Toggle",
          subtitle = "Lights-Out Matrix Inversion",
          genre = "Matrix Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://lumen_toggle",
          coverKey = "lumen_toggle",
          description =
            "Tapping any node flips its state along with its four orthogonal neighbors. Extinguish every glowing node to clear the matrix.",
          howItWorks =
            "Every generated 4×4 or 5×5 board is guaranteed solvable. Each tap toggles the tapped cell plus Up, Down, Left, and Right neighbors between Lit and Off—plan sequential toggles row by row to chase remaining lights downward.",
          howToComplete =
            "Turn OFF every single glowing node across the entire grid at once in as few moves as possible to extinguish the matrix.",
          controlsHint = "Tap any cell to toggle cross pattern",
          releaseNote = "Puzzle Pack · Solvable 4×4 & 5×5 boards",
          sortOrder = 6,
        ),
        GameEntryEntity(
          id = "chroma_flood",
          title = "Chroma Flood",
          subtitle = "Color-Territory Flood Strategy",
          genre = "Color Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://chroma_flood",
          coverKey = "chroma_flood",
          description =
            "Expand from the top-left origin across a 10×10 mosaic by selecting palette swatches to unify the entire board within 22 moves.",
          howItWorks =
            "Your controlled territory begins at the top-left corner cell (0,0). Choosing one of the 6 color swatches repaints your connected territory to match adjacent clusters of that color, absorbing them into your growing region.",
          howToComplete =
            "Unify all 100 cells of the 10×10 mosaic into a single solid color before hitting the 22-move limit.",
          controlsHint = "Tap color swatches below the mosaic",
          releaseNote = "Puzzle Pack · 6-color strategic flood-fill",
          sortOrder = 7,
        ),
        GameEntryEntity(
          id = "nonogrid",
          title = "NonoGrid",
          subtitle = "Picross Row-Column Cipher",
          genre = "Nonogram Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://nonogrid",
          coverKey = "nonogrid",
          description =
            "Decode numeric row and column run-length clues on a 5×5 logic canvas to uncover hidden pixel emblems.",
          howItWorks =
            "Numbers beside each row and above each column tell you the exact lengths of consecutive filled blocks in that line (for example, '1 3' means 1 filled square, at least one blank space, then 3 filled squares). Use Mark X mode to rule out guaranteed blanks.",
          howToComplete =
            "Match every row and column clue simultaneously to reveal the hidden 5×5 pixel emblem (Rocket, Key, Crown) and advance to the next emblem.",
          controlsHint = "Tap cells to Fill or Mark X",
          releaseNote = "Puzzle Pack · Multiple handcrafted emblems",
          sortOrder = 8,
        ),
        GameEntryEntity(
          id = "slide_15",
          title = "Slide-15",
          subtitle = "Classic 4×4 Tile Permutations",
          genre = "Sliding Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://slide_15",
          coverKey = "slide_15",
          description =
            "Order tiles 1 through 15 into ascending sequence using guaranteed-solvable shuffles and minimal moves.",
          howItWorks =
            "Every puzzle is shuffled using valid legal slide moves so 100% of boards are mathematically solvable. Tapping any numbered tile next to the open gap slides it in; tiles resting in their final target position glow in primary accent color.",
          howToComplete =
            "Arrange tiles 1 through 15 sequentially left-to-right, top-to-bottom with the blank space in the bottom-right corner.",
          controlsHint = "Tap any tile adjacent to the empty slot",
          releaseNote = "Puzzle Pack · Parity-verified solvable boards",
          sortOrder = 9,
        ),
        GameEntryEntity(
          id = "hanoi_vault",
          title = "Tower of Hanoi",
          subtitle = "Recursive Stack Transfer",
          genre = "Recursive Puzzle",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Puzzle",
          assetPath = "puzzle://hanoi_vault",
          coverKey = "hanoi_vault",
          description =
            "Transfer the weighted disk stack from Pillar A to Pillar C without ever placing a larger disk atop a smaller one.",
          howItWorks =
            "Select a difficulty of 3, 4, or 5 disks. Tap a source pillar to lift its top disk, then tap a destination pillar to drop it—the engine enforces the classic rule that wider disks can never rest on narrower disks. Optimal par is 7, 15, or 31 moves.",
          howToComplete =
            "Reassemble the entire ordered pyramid stack onto Pillar C from widest at the bottom to smallest at the top.",
          controlsHint = "Tap source pillar, then destination pillar",
          releaseNote = "Puzzle Pack · 3, 4, and 5-disk difficulty modes",
          sortOrder = 10,
        ),
        GameEntryEntity(
          id = "vault_chess",
          title = "Vault Chess",
          subtitle = "100–1000 Elo Bot & Pass-and-Play Friend",
          genre = "Strategy · Chess",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.HOBBYIST.name,
          versionTag = "v1.1.0",
          sizeLabel = "Native · Chess",
          assetPath = "puzzle://vault_chess",
          coverKey = "vault_chess",
          description =
            "Full-rules offline Chess! Play against the interchangeable 100–1000 Elo AI bot (100 Elo unlocked at start, 800+ on Super Ranks) or switch to Play with Friend mode for local 2P White vs Black!",
          howItWorks =
            "Includes full legal Chess rules—castling, en passant, pawn promotion, check detection, and stalemate/50-move draws. Play VS AI Bot with difficulty presets scaling from 100 Elo up to 1000 Elo as your rank rises, or toggle Play with Friend for pass-and-play matches with optional auto-flipping.",
          howToComplete =
            "Deliver Checkmate against the opponent's King (either the AI Bot or Player 2) to win the match and earn XP, Elo achievements (Pawn's First Step / Super Rank Tactician), and Obsidian King progress.",
          controlsHint = "Tap piece, then tap highlighted destination · Switch AI / Friend mode at top",
          releaseNote = "Chess Drop · 100–1000 Elo AI Bot & 2P Pass-and-Play Friend Mode",
          sortOrder = 11,
        ),
        // 4 Rank-Locked Puzzle Games (Each requires a distinct Rank)
        GameEntryEntity(
          id = "cipher_break",
          title = "Cipher Mastermind",
          subtitle = "4-Symbol Code Deduction",
          genre = "Code Puzzle · Enthusiast",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.ENTHUSIAST.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Locked",
          assetPath = "puzzle://cipher_break",
          coverKey = "cipher_break",
          description =
            "Crack the hidden 4-token cipher sequence within 8 attempts using exact-hit and near-hit logic pegs. Unlocks at Enthusiast (500 XP).",
          howItWorks =
            "A secret 4-color code (repeats allowed) is generated from 6 neon tokens. Pick colors for your 4 draft slots and press Submit Guess—the feedback reads how many tokens are Exact (right color AND slot) vs Near (right color, wrong slot).",
          howToComplete =
            "Deduce the exact 4-color sequence (4 Exact hits) within 8 guesses to crack the cipher for +140 XP and up to 1,840 points.",
          controlsHint = "Select 4 tokens and tap Submit Guess",
          releaseNote = "Enthusiast Rank Unlock · 500 XP milestone",
          sortOrder = 12,
        ),
        GameEntryEntity(
          id = "sudoku_six",
          title = "Sudoku Six",
          subtitle = "2×3 Region Number Logic",
          genre = "Grid Puzzle · Expert",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.EXPERT.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Locked",
          assetPath = "puzzle://sudoku_six",
          coverKey = "sudoku_six",
          description =
            "Fill every row, column, and 2×3 block with digits 1–6 with zero repeats. Features conflict detection and multiple puzzles. Unlocks at Expert (1,500 XP).",
          howItWorks =
            "Tap any unlocked cell and enter a digit 1–6 using the number bar below (or Clr to erase). Any duplicate number within the same row, column, or 2×3 sub-grid automatically highlights in red for instant deduction feedback.",
          howToComplete =
            "Fill all 36 squares of the 6×6 board with digits 1–6 and zero red conflicts to solve the board (+160 XP) and unlock the next puzzle.",
          controlsHint = "Tap an empty cell, then tap digits 1–6",
          releaseNote = "Expert Rank Unlock · 1,500 XP milestone",
          sortOrder = 13,
        ),
        GameEntryEntity(
          id = "crate_sokoban",
          title = "Crate Pusher",
          subtitle = "Warehouse Sokoban Tactics",
          genre = "Tactical Puzzle · Master",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.MASTER.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Locked",
          assetPath = "puzzle://crate_sokoban",
          coverKey = "crate_sokoban",
          description =
            "Push heavy alloy crates onto all floor pads across tight corridors without wedging them into corners. Includes Undo & Reset. Unlocks at Master (5,000 XP).",
          howItWorks =
            "Guide your warehouse bot with the directional pad. You can push one crate forward at a time, but cannot pull crates backward or push two crates at once—use the Undo button anytime if you wedge a crate against a wall.",
          howToComplete =
            "Push every amber alloy crate onto a glowing green target dock pad simultaneously (turning all crates green) to clear each warehouse room.",
          controlsHint = "D-Pad or Arrow keys to move & push crates",
          releaseNote = "Master Rank Unlock · 5,000 XP milestone",
          sortOrder = 14,
        ),
        GameEntryEntity(
          id = "queens_grid",
          title = "Queens N-Grid",
          subtitle = "Chess Line-of-Sight Logic",
          genre = "Chess Puzzle · Legend",
          engineType = GameEngineType.PUZZLE_NATIVE.name,
          requiredTier = PlayerTier.LEGEND.name,
          versionTag = "v1.0.0",
          sizeLabel = "Native · Locked",
          assetPath = "puzzle://queens_grid",
          coverKey = "queens_grid",
          description =
            "Place N Queens on an N×N board (6×6 & 8×8) so no two queens share any row, column, or diagonal. Unlocks at Legend (17,500 XP).",
          howItWorks =
            "Switch between 6×6 (6 Queens) and classic 8×8 (8 Queens). Tapping an empty square places a Queen; tapping an existing Queen removes it. Any two Queens that threaten each other horizontally, vertically, or diagonally turn crimson to warn of clashes.",
          howToComplete =
            "Place all N Queens (6 on 6×6, or 8 on 8×8) on the board at once with zero red attack clashes to earn +240 XP and up to 2,800 points.",
          controlsHint = "Tap squares to place or remove Queens",
          releaseNote = "Legend Rank Unlock · 17,500 XP milestone",
          sortOrder = 15,
        ),
      )

    fun defaultAchievements(): List<AchievementEntity> =
      listOf(
        AchievementEntity(
          id = "first_launch",
          title = "Vault Initiated",
          description = "Launch any embedded game inside VaultPlay 2.",
          iconKey = "rocket",
          xpReward = 75,
        ),
        AchievementEntity(
          id = "platform_pioneer",
          title = "Fan Fav Orb Hunter",
          description = "Collect 10 energy orbs in Neon Hop.",
          iconKey = "bolt",
          xpReward = 150,
          gameId = "neon_hop",
        ),
        AchievementEntity(
          id = "neon_finisher",
          title = "Neon Beacon Cleared",
          description = "Complete a stage in Neon Hop.",
          iconKey = "flag",
          xpReward = 200,
          gameId = "neon_hop",
        ),
        AchievementEntity(
          id = "skyline_ace",
          title = "Rooftop Sprinter",
          description = "Score 600+ in a Skyline Dash run.",
          iconKey = "speed",
          xpReward = 220,
          gameId = "skyline_dash",
        ),
        AchievementEntity(
          id = "puzzle_2048",
          title = "Prism Architect",
          description = "Reach a 256+ tile or score 1,200+ in Prism 2048.",
          iconKey = "star",
          xpReward = 250,
          gameId = "prism_2048",
        ),
        AchievementEntity(
          id = "circuit_master",
          title = "Live Conduit",
          description = "Power all nodes in Flux Circuit.",
          iconKey = "bolt",
          xpReward = 200,
          gameId = "flux_circuit",
        ),
        AchievementEntity(
          id = "sweeper_clear",
          title = "Zero Detonation",
          description = "Clear an entire Vault Sweeper board.",
          iconKey = "shield",
          xpReward = 260,
          gameId = "vault_sweeper",
        ),
        AchievementEntity(
          id = "logic_solver",
          title = "Clean Matrix",
          description = "Solve Lumen Toggle, NonoGrid, Chroma Flood, Slide-15, or Hanoi.",
          iconKey = "verified",
          xpReward = 220,
        ),
        AchievementEntity(
          id = "tier_enthusiast",
          title = "Enthusiast · Cipher Unlocked",
          description = "Reach 500 XP to unlock Cipher Mastermind.",
          iconKey = "star",
          xpReward = 150,
          gameId = "cipher_break",
        ),
        AchievementEntity(
          id = "tier_expert",
          title = "Expert · Sudoku Unlocked",
          description = "Reach 1,500 XP to unlock Sudoku Six.",
          iconKey = "verified",
          xpReward = 200,
          gameId = "sudoku_six",
        ),
        AchievementEntity(
          id = "tier_master",
          title = "Master · Crate Pusher",
          description = "Reach 5,000 XP to unlock Crate Pusher.",
          iconKey = "trophy",
          xpReward = 400,
          gameId = "crate_sokoban",
        ),
        AchievementEntity(
          id = "tier_legend",
          title = "Legend · Queens Unlocked",
          description = "Reach 17,500 XP to unlock Queens N-Grid.",
          iconKey = "crown",
          xpReward = 600,
          gameId = "queens_grid",
        ),
        AchievementEntity(
          id = "chess_100_win",
          title = "Pawn's First Step",
          description = "Checkmate any Vault Chess AI bot.",
          iconKey = "star",
          xpReward = 150,
          gameId = "vault_chess",
        ),
        AchievementEntity(
          id = "chess_800_super",
          title = "Super Rank Tactician",
          description = "Checkmate an 800+ Elo Vault Chess AI bot.",
          iconKey = "crown",
          xpReward = 500,
          gameId = "vault_chess",
        ),
        AchievementEntity(
          id = "sf_champion",
          title = "Street Champion",
          description = "Win a Best-of-3 match in Street Fighter 2D.",
          iconKey = "trophy",
          xpReward = 200,
          gameId = "street_fighter_2d",
        ),
        AchievementEntity(
          id = "flappy_ace",
          title = "Pipe Weaver",
          description = "Clear 15 pipes in a single Flappy Bird run.",
          iconKey = "bolt",
          xpReward = 160,
          gameId = "flappy_bird",
        ),
        AchievementEntity(
          id = "block_blast_master",
          title = "Cube Blaster",
          description = "Score 1,000+ points in a single Endless Block Blast run.",
          iconKey = "star",
          xpReward = 220,
          gameId = "block_blast",
        ),
        AchievementEntity(
          id = "tier_mythic",
          title = "Mythic Vault Sovereign",
          description = "Reach the pinnacle 25,000 XP Mythic rank.",
          iconKey = "crown",
          xpReward = 1000,
        ),
      )

    fun defaultGhostRivals(): List<LeaderboardEntryEntity> =
      listOf(
        LeaderboardEntryEntity(gameId = "neon_hop", playerName = "Ghost · Dev_Kael", score = 1450, tierName = "Master", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "skyline_dash", playerName = "Ghost · Aero", score = 1820, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "street_fighter_2d", playerName = "Ghost · RyuBot", score = 1650, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "flappy_bird", playerName = "Ghost · Wing", score = 2500, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "prism_2048", playerName = "Ghost · Prism", score = 3400, tierName = "Grandmaster", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "flux_circuit", playerName = "Ghost · Volt", score = 1200, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "vault_sweeper", playerName = "Ghost · Sapper", score = 1600, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "lumen_toggle", playerName = "Ghost · Photon", score = 950, tierName = "Enthusiast", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "chroma_flood", playerName = "Ghost · Hue", score = 1100, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "nonogrid", playerName = "Ghost · Cipher", score = 1050, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "slide_15", playerName = "Ghost · Parity", score = 1300, tierName = "Pro", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "hanoi_vault", playerName = "Ghost · Monk", score = 1400, tierName = "Master", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "vault_chess", playerName = "Ghost · StockBot", score = 2100, tierName = "Virtuoso", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "cipher_break", playerName = "Ghost · Enigma", score = 1500, tierName = "Expert", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "sudoku_six", playerName = "Ghost · Euler", score = 1900, tierName = "Grandmaster", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "crate_sokoban", playerName = "Ghost · Loader", score = 2200, tierName = "Virtuoso", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "queens_grid", playerName = "Ghost · Kasparov", score = 2800, tierName = "Mythic", isGhostRival = true),
        LeaderboardEntryEntity(gameId = "block_blast", playerName = "Ghost · Poly", score = 2400, tierName = "Pro", isGhostRival = true),
      )

    fun defaultCosmeticBadges(): List<CosmeticBadgeEntity> =
      listOf(
        CosmeticBadgeEntity(
          id = "cosmetic_obsidian_king",
          title = "Obsidian King Crest",
          description = "Earned by winning a Vault Chess daily mission.",
          rarity = "EPIC",
          accentHex = 0xFFFFC857,
          emblemKey = "king",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_gilded_wings",
          title = "Gilded Aviator Wings",
          description = "Earned by scoring 500+ in Flappy Bird daily mission.",
          rarity = "RARE",
          accentHex = 0xFF38BDF8,
          emblemKey = "wings",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_cyber_visor",
          title = "Cyber Runner Halo",
          description = "Earned by clearing a Neon Hop stage mission.",
          rarity = "RARE",
          accentHex = 0xFF4CE8C8,
          emblemKey = "visor",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_crimson_dragon",
          title = "Crimson Dojo Seal",
          description = "Earned by winning a Street Fighter 2D match mission.",
          rarity = "EPIC",
          accentHex = 0xFFFF5C7C,
          emblemKey = "dragon",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_supersonic_bolt",
          title = "Supersonic Bolt",
          description = "Earned by scoring 600+ in Skyline Dash mission.",
          rarity = "RARE",
          accentHex = 0xFFFF884D,
          emblemKey = "bolt",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_prism_core",
          title = "Prism Tesseract",
          description = "Earned by solving 2 daily puzzle challenges.",
          rarity = "EPIC",
          accentHex = 0xFFA78BFA,
          emblemKey = "prism",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_solar_crown",
          title = "Solar Matrix Crown",
          description = "Earned by reaching 800+ score in Prism 2048 mission.",
          rarity = "MYTHIC",
          accentHex = 0xFFFDE047,
          emblemKey = "crown",
        ),
        CosmeticBadgeEntity(
          id = "cosmetic_starlight_compass",
          title = "Vault Voyager Star",
          description = "Earned by playing 3 distinct games in a single day.",
          rarity = "RARE",
          accentHex = 0xFF34D399,
          emblemKey = "compass",
        ),
      )

    fun currentDayKey(nowMs: Long = System.currentTimeMillis()): String {
      val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
      val y = cal.get(Calendar.YEAR)
      val d = cal.get(Calendar.DAY_OF_YEAR)
      return "$y-$d"
    }

    fun generateDailyMissionsForDay(dayKey: String, cycleOffset: Int = 0): List<DailyMissionEntity> {
      val set0 =
        listOf(
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_chess",
            title = "Win a Chess match",
            description = "Checkmate any AI bot or win a Friend match in Vault Chess.",
            gameId = "vault_chess",
            missionType = DailyMissionType.CHESS_WIN.name,
            targetValue = 1,
            bonusXp = 220,
            cosmeticBadgeId = "cosmetic_obsidian_king",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_flappy",
            title = "Score 500 in Flappy Bird",
            description = "Reach 500 points (5 pipes cleared) in a Flappy Bird flight.",
            gameId = "flappy_bird",
            missionType = DailyMissionType.SCORE_TARGET.name,
            targetValue = 500,
            bonusXp = 180,
            cosmeticBadgeId = "cosmetic_gilded_wings",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_neon",
            title = "Clear a stage in Neon Hop",
            description = "Reach the emerald portal in any Neon Hop stage.",
            gameId = "neon_hop",
            missionType = DailyMissionType.STAGE_CLEAR.name,
            targetValue = 1,
            bonusXp = 160,
            cosmeticBadgeId = "cosmetic_cyber_visor",
            dayKey = dayKey,
          ),
        )

      val set1 =
        listOf(
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_sf2d",
            title = "Win a Street Fighter 2D match",
            description = "Win a Best-of-3 arena battle against the CPU or a friend.",
            gameId = "street_fighter_2d",
            missionType = DailyMissionType.SF_WIN.name,
            targetValue = 1,
            bonusXp = 210,
            cosmeticBadgeId = "cosmetic_crimson_dragon",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_skyline",
            title = "Score 600 in Skyline Dash",
            description = "Chain jumps, dashes, and drone smashes to 600+ points.",
            gameId = "skyline_dash",
            missionType = DailyMissionType.SCORE_TARGET.name,
            targetValue = 600,
            bonusXp = 190,
            cosmeticBadgeId = "cosmetic_supersonic_bolt",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_puzzles",
            title = "Solve 2 Puzzle Games",
            description = "Complete any two logic, sliding, or cipher puzzles.",
            gameId = null,
            missionType = DailyMissionType.PUZZLE_SOLVE_COUNT.name,
            targetValue = 2,
            bonusXp = 200,
            cosmeticBadgeId = "cosmetic_prism_core",
            dayKey = dayKey,
          ),
        )

      val set2 =
        listOf(
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_prism",
            title = "Score 800 in Prism 2048",
            description = "Merge power-tiles to hit 800+ points in Prism 2048.",
            gameId = "prism_2048",
            missionType = DailyMissionType.SCORE_TARGET.name,
            targetValue = 800,
            bonusXp = 240,
            cosmeticBadgeId = "cosmetic_solar_crown",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_explore",
            title = "Vault Triple Threat",
            description = "Play 3 different games from the library today.",
            gameId = null,
            missionType = DailyMissionType.GAMES_PLAYED_COUNT.name,
            targetValue = 3,
            bonusXp = 165,
            cosmeticBadgeId = "cosmetic_starlight_compass",
            dayKey = dayKey,
          ),
          DailyMissionEntity(
            id = "dm_${dayKey}_${cycleOffset}_chess2",
            title = "Win a Chess match",
            description = "Deliver checkmate on the 64-square board today.",
            gameId = "vault_chess",
            missionType = DailyMissionType.CHESS_WIN.name,
            targetValue = 1,
            bonusXp = 220,
            cosmeticBadgeId = "cosmetic_obsidian_king",
            dayKey = dayKey,
          ),
        )

      val sets = listOf(set0, set1, set2)
      return sets[cycleOffset.mod(sets.size)]
    }
  }
}

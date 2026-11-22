package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.VaultTrackCatalog
import com.example.data.local.VaultDatabase
import com.example.data.model.FighterStat
import com.example.data.model.PlayerTier
import com.example.data.model.VaultPreferences
import com.example.ui.engines.CHESS_ELO_PRESETS
import com.example.ui.engines.ChessAiEngine
import com.example.ui.engines.ChessColor
import com.example.ui.engines.ChessRules
import com.example.ui.engines.ChessState
import com.example.ui.engines.STREET_FIGHTERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches VaultPlay and creator pin is hidden from strings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue(appName.startsWith("VaultPlay"))
    val pinHint = context.getString(R.string.creator_pin_hint)
    assertFalse(pinHint.contains("2011"))
    assertFalse(pinHint.contains("2026"))
  }

  @Test
  fun `rank ladder has 9 ranks up to Mythic and locks exactly 4 distinct games`() {
    assertEquals(9, PlayerTier.entries.size)
    assertEquals(PlayerTier.HOBBYIST, PlayerTier.fromXp(0))
    assertEquals(PlayerTier.ENTHUSIAST, PlayerTier.fromXp(500))
    assertEquals(PlayerTier.EXPERT, PlayerTier.fromXp(1500))
    assertEquals(PlayerTier.PRO, PlayerTier.fromXp(3000))
    assertEquals(PlayerTier.MASTER, PlayerTier.fromXp(5000))
    assertEquals(PlayerTier.GRANDMASTER, PlayerTier.fromXp(8000))
    assertEquals(PlayerTier.VIRTUOSO, PlayerTier.fromXp(12000))
    assertEquals(PlayerTier.LEGEND, PlayerTier.fromXp(17500))
    assertEquals(PlayerTier.MYTHIC, PlayerTier.fromXp(25000))

    val games = VaultDatabase.defaultGames()
    // 2 kept platformers + 2 arcade (SF2D, Flappy) + Block Blast + 12 puzzles + Vault Chess = 18 games
    assertEquals(18, games.size)
    assertEquals(18, games.map { it.id }.toSet().size)

    // Kept platformers, Neon Hop marked Fan Favourite, and new games present
    val neon = games.first { it.id == "neon_hop" }
    assertTrue(neon.isFanFavourite)
    assertTrue(games.any { it.id == "skyline_dash" })
    assertTrue(games.any { it.id == "vault_chess" && it.requiredTier == PlayerTier.HOBBYIST.name })
    assertTrue(games.any { it.id == "street_fighter_2d" && it.requiredTier == PlayerTier.HOBBYIST.name })
    assertTrue(games.any { it.id == "flappy_bird" && it.requiredTier == PlayerTier.HOBBYIST.name })
    assertTrue(games.any { it.id == "block_blast" && it.requiredTier == PlayerTier.HOBBYIST.name })

    // Removed titles including Hill Climb Racing
    assertTrue(games.none { it.id in setOf("hill_climb", "ash_cipher", "crypt_echoes", "apex_vector", "vice_horizon") })

    // Exactly 4 locked library games, each requiring a distinct rank
    val lockedGames = games.filter { it.requiredTier != PlayerTier.HOBBYIST.name }
    assertEquals(4, lockedGames.size)
    assertEquals(4, lockedGames.map { it.requiredTier }.toSet().size)
  }

  @Test
  fun `every game and the lobby have distinct soundtrack blueprints`() {
    val games = VaultDatabase.defaultGames()
    val lobbyTrack = VaultTrackCatalog.forKey("lobby")
    assertEquals("lobby_lounge", lobbyTrack.id)
    val gameTracks = games.map { VaultTrackCatalog.forKey(it.id) }
    assertEquals(games.size, gameTracks.map { it.id }.toSet().size)
    assertTrue(gameTracks.none { it.id.startsWith("lobby") })
  }

  @Test
  fun `street fighter has 3 upgradable characters with persisted encoding`() {
    assertEquals(3, STREET_FIGHTERS.size)
    val defaults = VaultPreferences.defaultFighterUpgrades()
    val kaiLv1 = defaults.getValue("kai")
    assertEquals(40, kaiLv1.upgradeCost(FighterStat.ATTACK))
    val upgraded = kaiLv1.withUpgraded(FighterStat.ATTACK).withUpgraded(FighterStat.HEALTH)
    assertEquals(2, upgraded.attackLevel)
    assertEquals(2, upgraded.healthLevel)
    val encoded = VaultPreferences.encodeFighterUpgrades(defaults + ("kai" to upgraded))
    val decoded = VaultPreferences.parseFighterUpgrades(encoded)
    assertEquals(upgraded, decoded["kai"])
  }

  @Test
  fun `chess elo presets and 2P white plus black legal moves work cleanly`() {
    assertEquals(10, CHESS_ELO_PRESETS.size)
    assertEquals(100, CHESS_ELO_PRESETS.first().elo)
    assertEquals(1000, CHESS_ELO_PRESETS.last().elo)

    // Only 100 Elo is unlocked at Hobbyist (start)
    val startUnlocked = CHESS_ELO_PRESETS.filter { it.requiredTier == PlayerTier.HOBBYIST }
    assertEquals(listOf(100), startUnlocked.map { it.elo })

    // 200-700 require lower/mid ranks below Super Ranks
    val lowerMid = CHESS_ELO_PRESETS.filter { it.elo in 200..700 }
    assertTrue(lowerMid.all { it.requiredTier.levelIndex in PlayerTier.ENTHUSIAST.levelIndex..PlayerTier.GRANDMASTER.levelIndex })

    // 800+ Elo require Super Ranks (Virtuoso, Legend, Mythic)
    val superElo = CHESS_ELO_PRESETS.filter { it.elo >= 800 }
    assertEquals(listOf(800, 900, 1000), superElo.map { it.elo })
    assertTrue(superElo.all { it.requiredTier.levelIndex >= PlayerTier.VIRTUOSO.levelIndex })

    val initial = ChessState.initial()
    assertEquals(20, ChessRules.legalMoves(initial, ChessColor.WHITE).size)
    assertEquals(20, ChessRules.legalMoves(initial, ChessColor.BLACK).size)
    assertNotNull(ChessAiEngine.chooseMove(initial, 100))
    assertNotNull(ChessAiEngine.chooseMove(initial, 1000))
  }

  @Test
  fun `game sfx catalog covers jump dash hit chess move checkmate and victory safely`() {
    val prefs = VaultPreferences()
    assertTrue(prefs.gameSfxEnabled)
    assertTrue(prefs.lobbySfxEnabled)

    val expectedEffects = setOf(
      com.example.audio.GameSfx.JUMP,
      com.example.audio.GameSfx.DASH,
      com.example.audio.GameSfx.HIT,
      com.example.audio.GameSfx.CHESS_MOVE,
      com.example.audio.GameSfx.CHECK,
      com.example.audio.GameSfx.CHECKMATE,
      com.example.audio.GameSfx.PICKUP,
      com.example.audio.GameSfx.PUZZLE_STEP,
      com.example.audio.GameSfx.BLOCK_PLACE,
      com.example.audio.GameSfx.BLOCK_BLAST,
      com.example.audio.GameSfx.VICTORY,
    )
    assertEquals(expectedEffects, com.example.audio.GameSfx.entries.toSet())
    com.example.audio.GameSfx.entries.forEach { sfx ->
      com.example.audio.VaultAudioEngine.playGameSfx(sfx)
    }
  }

  @Test
  fun `rank promotion reward captures previous xp tier and progress transition`() {
    val reward =
      com.example.data.repository.RewardEvent(
        xpGained = 150,
        newTotalXp = 600,
        previousTotalXp = 450,
        previousTier = PlayerTier.HOBBYIST,
        promotedToTier = PlayerTier.ENTHUSIAST,
      )
    assertEquals(PlayerTier.HOBBYIST, reward.previousTier)
    assertEquals(PlayerTier.ENTHUSIAST, reward.promotedToTier)
    assertEquals(0.9f, PlayerTier.progressInTier(reward.previousTotalXp), 0.001f)
    assertEquals(0.1f, PlayerTier.progressInTier(reward.newTotalXp), 0.001f)
    assertEquals(1.0f, PlayerTier.progressInTier(PlayerTier.MYTHIC.minXp + 500), 0.001f)
  }

  @Test
  fun `lobby song library affects lobby track only and leaves game tracks untouched`() {
    assertEquals(7, VaultTrackCatalog.LOBBY_SONGS.size)
    assertEquals(7, VaultTrackCatalog.LOBBY_SONGS.map { it.id }.toSet().size)

    val lobbyCyber = VaultTrackCatalog.forKey("lobby", "lobby_cyberpunk")
    assertEquals("lobby_cyberpunk", lobbyCyber.id)

    val lobbyZen = VaultTrackCatalog.forKey("lobby", "lobby_zen")
    assertEquals("lobby_zen", lobbyZen.id)

    val neonHopTrack = VaultTrackCatalog.forKey("neon_hop", "lobby_cyberpunk")
    assertEquals("neon_hop", neonHopTrack.id)
    val chessTrack = VaultTrackCatalog.forKey("vault_chess", "lobby_lofi")
    assertEquals("vault_chess", chessTrack.id)
  }

  @Test
  fun `daily missions and cosmetic badges include chess win and flappy 500 challenges`() {
    val badges = VaultDatabase.defaultCosmeticBadges()
    assertEquals(8, badges.size)
    assertEquals(8, badges.map { it.id }.toSet().size)

    val missionsDay0 = VaultDatabase.generateDailyMissionsForDay("2026-100", 0)
    assertEquals(3, missionsDay0.size)
    assertTrue(missionsDay0.any { it.title == "Win a Chess match" && it.gameId == "vault_chess" && it.cosmeticBadgeId in badges.map { b -> b.id } })
    assertTrue(missionsDay0.any { it.title == "Score 500 in Flappy Bird" && it.gameId == "flappy_bird" && it.targetValue == 500 })

    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("VaultPlay 2", context.getString(R.string.app_name))
  }

  @Test
  fun `some lobby songs require higher ranks while default songs remain open`() {
    val unlockedStarter = VaultTrackCatalog.LOBBY_SONGS.filter { VaultTrackCatalog.isLobbySongUnlocked(it.id, PlayerTier.HOBBYIST) }
    val lockedStarter = VaultTrackCatalog.LOBBY_SONGS.filterNot { VaultTrackCatalog.isLobbySongUnlocked(it.id, PlayerTier.HOBBYIST) }

    assertEquals(2, unlockedStarter.size)
    assertEquals(5, lockedStarter.size)
    assertTrue(VaultTrackCatalog.isLobbySongUnlocked("lobby_cyberpunk", PlayerTier.PRO))
    assertFalse(VaultTrackCatalog.isLobbySongUnlocked("lobby_cyberpunk", PlayerTier.EXPERT))
    assertTrue(VaultTrackCatalog.isLobbySongUnlocked("lobby_zen", PlayerTier.VIRTUOSO))
    assertFalse(VaultTrackCatalog.isLobbySongUnlocked("lobby_zen", PlayerTier.MASTER))
  }

  @Test
  fun `avatar catalog resolves all 4 generated avatars cleanly`() {
    val options = com.example.ui.components.VaultAvatarCatalog.OPTIONS
    assertEquals(4, options.size)
    assertEquals("striker", com.example.ui.components.VaultAvatarCatalog.resolve("striker").id)
    assertEquals("tactician", com.example.ui.components.VaultAvatarCatalog.resolve("tactician").id)
    assertEquals("aviator", com.example.ui.components.VaultAvatarCatalog.resolve("aviator").id)
    assertEquals("cyber", com.example.ui.components.VaultAvatarCatalog.resolve("rocket").id)
  }

  @Test
  fun `block blast logic clears full rows and columns and supports endless rescue`() {
    val grid = com.example.ui.engines.BlockBlastLogic.emptyGrid().map { it.toMutableList() }
    // Fill row 0 except column 7
    for (c in 0..6) grid[0][c] = 0xFF4CE8C8L
    // Fill col 7 except row 0
    for (r in 1..7) grid[r][7] = 0xFF38BDF8L

    val dot = com.example.ui.engines.BlockShape("dot", listOf(0 to 0), 0xFFFACC15L)
    assertTrue(com.example.ui.engines.BlockBlastLogic.canPlace(grid, dot, 0, 7))
    val result = com.example.ui.engines.BlockBlastLogic.placeAndClear(grid, dot, 0, 7, comboStreak = 1)
    assertEquals(setOf(0), result.clearedRows)
    assertEquals(setOf(7), result.clearedCols)
    assertTrue(result.pointsEarned > 300)
    assertTrue(result.nextGrid[0].all { it == 0L })
    assertTrue(result.nextGrid.all { row -> row[7] == 0L })

    assertEquals("block_blast", VaultTrackCatalog.forKey("block_blast").id)
  }

  @Test
  fun `all 18 games include detailed howItWorks and howToComplete guides`() {
    val games = VaultDatabase.defaultGames()
    assertEquals(18, games.size)
    games.forEach { game ->
      assertTrue("${game.id} missing description", game.description.trim().length >= 20)
      assertTrue("${game.id} missing howItWorks", game.howItWorks.trim().length >= 30)
      assertTrue("${game.id} missing howToComplete", game.howToComplete.trim().length >= 30)
    }
  }

  @Test
  fun `quick resume serializer round trips arrays and mid-game chess state`() {
    val ints = intArrayOf(0, 4, 8, 16, 1024, 0, -3)
    val decodedInts = com.example.ui.engines.QuickResumeSerializer.decodeIntArray(
      com.example.ui.engines.QuickResumeSerializer.encodeIntArray(ints)
    )
    assertNotNull(decodedInts)
    assertEquals(ints.toList(), decodedInts!!.toList())

    val bools = booleanArrayOf(true, false, true, true, false)
    val decodedBools = com.example.ui.engines.QuickResumeSerializer.decodeBooleanArray(
      com.example.ui.engines.QuickResumeSerializer.encodeBooleanArray(bools)
    )
    assertNotNull(decodedBools)
    assertEquals(bools.toList(), decodedBools!!.toList())

    val initial = ChessState.initial()
    val firstMove = ChessRules.legalMoves(initial, ChessColor.WHITE).first()
    val midGame = ChessRules.applyMove(initial, firstMove)
    val json = com.example.ui.engines.QuickResumeSerializer.encodeChessState(midGame)
    val restored = com.example.ui.engines.QuickResumeSerializer.decodeChessState(json)
    assertNotNull(restored)
    assertEquals(midGame.turn, restored!!.turn)
    assertEquals(midGame.lastMove?.fromRow, restored.lastMove?.fromRow)
    assertEquals(midGame.lastMove?.toRow, restored.lastMove?.toRow)
    assertEquals(midGame.lastMove?.toCol, restored.lastMove?.toCol)
    assertEquals(
      ChessRules.legalMoves(midGame, ChessColor.BLACK).size,
      ChessRules.legalMoves(restored, ChessColor.BLACK).size,
    )
  }
}


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
    assertEquals("VaultPlay", appName)
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
    // 2 kept platformers + 2 arcade (SF2D, Flappy) + 12 puzzles + Vault Chess = 17 games
    assertEquals(17, games.size)
    assertEquals(17, games.map { it.id }.toSet().size)

    // Kept platformers, Neon Hop marked Fan Favourite, and new games present
    val neon = games.first { it.id == "neon_hop" }
    assertTrue(neon.isFanFavourite)
    assertTrue(games.any { it.id == "skyline_dash" })
    assertTrue(games.any { it.id == "vault_chess" && it.requiredTier == PlayerTier.HOBBYIST.name })
    assertTrue(games.any { it.id == "street_fighter_2d" && it.requiredTier == PlayerTier.HOBBYIST.name })
    assertTrue(games.any { it.id == "flappy_bird" && it.requiredTier == PlayerTier.HOBBYIST.name })

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
    assertEquals("lobby", lobbyTrack.id)
    val gameTracks = games.map { VaultTrackCatalog.forKey(it.id) }
    assertEquals(games.size, gameTracks.map { it.id }.toSet().size)
    assertTrue(gameTracks.none { it.id == "lobby" })
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
}

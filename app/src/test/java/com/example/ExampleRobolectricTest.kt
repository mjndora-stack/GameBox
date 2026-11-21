package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VaultDatabase
import com.example.data.model.PlayerTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches VaultPlay`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VaultPlay", appName)
  }

  @Test
  fun `player tier ladder progresses from Hobbyist to Pro and unlocks Vice Horizon`() {
    assertEquals(PlayerTier.HOBBYIST, PlayerTier.fromXp(0))
    assertEquals(PlayerTier.ENTHUSIAST, PlayerTier.fromXp(500))
    assertEquals(PlayerTier.EXPERT, PlayerTier.fromXp(1500))
    assertEquals(PlayerTier.PRO, PlayerTier.fromXp(3000))

    val defaultGames = VaultDatabase.defaultGames()
    assertEquals(6, defaultGames.size)
    val viceGame = defaultGames.first { it.id == "vice_horizon" }
    assertEquals(PlayerTier.PRO.name, viceGame.requiredTier)
    assertTrue(defaultGames.any { it.id == "neon_hop" })
    assertTrue(defaultGames.any { it.id == "skyline_dash" })
    assertTrue(defaultGames.any { it.id == "ash_cipher" })
    assertTrue(defaultGames.any { it.id == "crypt_echoes" })
    assertTrue(defaultGames.any { it.id == "apex_vector" })
  }
}

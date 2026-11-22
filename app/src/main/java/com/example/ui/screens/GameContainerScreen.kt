package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.GameEntryEntity
import com.example.data.model.FighterStat
import com.example.data.model.GameEngineType
import com.example.ui.engines.BlockBlastGame
import com.example.ui.engines.ChromaFloodGame
import com.example.ui.engines.CipherMastermindGame
import com.example.ui.engines.CrateSokobanGame
import com.example.ui.engines.EmbeddedWebGameEngine
import com.example.ui.engines.FlappyBirdGame
import com.example.ui.engines.FluxCircuitGame
import com.example.ui.engines.HanoiVaultGame
import com.example.ui.engines.LumenToggleGame
import com.example.ui.engines.NonoGridGame
import com.example.ui.engines.Prism2048Game
import com.example.ui.engines.QueensGridGame
import com.example.ui.engines.Slide15Game
import com.example.ui.engines.StreetFighter2DGame
import com.example.ui.engines.SudokuSixGame
import com.example.ui.engines.VaultChessGame
import com.example.ui.engines.VaultSweeperGame
import com.example.ui.viewmodel.VaultUiState

@Composable
fun GameContainerScreen(
  game: GameEntryEntity,
  uiState: VaultUiState,
  onAwardProgress: (String, Int, Int?, String?) -> Unit,
  onUpgradeFighter: (String, FighterStat, Int) -> Unit,
  onFlipHud: () -> Unit,
  onExitGame: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isPaused by remember { mutableStateOf(false) }

  BackHandler {
    if (isPaused) {
      onExitGame()
    } else {
      isPaused = true
    }
  }

  val engineType =
    runCatching { GameEngineType.valueOf(game.engineType) }.getOrDefault(GameEngineType.WEB_CANVAS)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { isPaused = true },
            modifier = Modifier.testTag("container_back_btn"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Pause or exit game")
          }
          Column {
            Text(game.title, style = MaterialTheme.typography.titleMedium)
            Text(
              "${engineType.label} · ${uiState.activeProfile?.totalXp ?: 0} XP",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onFlipHud, modifier = Modifier.testTag("container_flip_hud")) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = "Flip left/right controls")
          }
          IconButton(onClick = { isPaused = !isPaused }, modifier = Modifier.testTag("container_pause_btn")) {
            Icon(
              imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
              contentDescription = "Pause container",
            )
          }
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (engineType) {
        GameEngineType.WEB_CANVAS ->
          EmbeddedWebGameEngine(
            gameId = game.id,
            assetUrl = game.assetPath,
            customHtmlContent = game.customHtmlContent,
            onAwardXp = { gId, amt -> onAwardProgress(gId, amt, null, null) },
            onSubmitScore = { gId, sc -> onAwardProgress(gId, 0, sc, null) },
            onUnlockAchievement = { achId -> onAwardProgress(game.id, 0, null, achId) },
          )
        GameEngineType.ARCADE_NATIVE -> {
          val callback: (Int, Int?, String?) -> Unit = { xp, score, ach ->
            onAwardProgress(game.id, xp, score, ach)
          }
          val leftHanded = uiState.preferences.controlConfig.leftHandedHud
          when (game.id) {
            "street_fighter_2d" ->
              StreetFighter2DGame(
                vaultCredits = uiState.activeProfile?.vaultCredits ?: 100,
                fighterUpgrades = uiState.preferences.fighterUpgrades,
                leftHandedHud = leftHanded,
                onUpgradeStat = onUpgradeFighter,
                onAwardXp = callback,
              )
            "flappy_bird" -> FlappyBirdGame(onAwardXp = callback)
            else -> FlappyBirdGame(onAwardXp = callback)
          }
        }
        GameEngineType.PUZZLE_NATIVE -> {
          val callback: (Int, Int?, String?) -> Unit = { xp, score, ach ->
            onAwardProgress(game.id, xp, score, ach)
          }
          when (game.id) {
            "prism_2048" -> Prism2048Game(onAwardXp = callback)
            "flux_circuit" -> FluxCircuitGame(onAwardXp = callback)
            "vault_sweeper" -> VaultSweeperGame(onAwardXp = callback)
            "lumen_toggle" -> LumenToggleGame(onAwardXp = callback)
            "chroma_flood" -> ChromaFloodGame(onAwardXp = callback)
            "nonogrid" -> NonoGridGame(onAwardXp = callback)
            "slide_15" -> Slide15Game(onAwardXp = callback)
            "hanoi_vault" -> HanoiVaultGame(onAwardXp = callback)
            "vault_chess" -> VaultChessGame(currentTier = uiState.currentTier, onAwardXp = callback)
            "cipher_break" -> CipherMastermindGame(onAwardXp = callback)
            "sudoku_six" -> SudokuSixGame(onAwardXp = callback)
            "crate_sokoban" -> CrateSokobanGame(onAwardXp = callback)
            "queens_grid" -> QueensGridGame(onAwardXp = callback)
            "block_blast" -> BlockBlastGame(initialBestScore = game.highScore, onAwardXp = callback)
            else -> Prism2048Game(onAwardXp = callback)
          }
        }
      }

      if (isPaused) {
        Surface(
          color = Color(0xDE0B0E14),
          modifier = Modifier.fillMaxSize(),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text("${game.title} Paused", style = MaterialTheme.typography.headlineSmall)
                Text(
                  "All XP, badges, and scores stay safely synced to your offline Vault.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                  onClick = { isPaused = false },
                  modifier = Modifier.fillMaxWidth().testTag("pause_resume_btn"),
                ) {
                  Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.size(6.dp))
                  Text("Resume Game")
                }
                OutlinedButton(
                  onClick = onExitGame,
                  modifier = Modifier.fillMaxWidth().testTag("pause_exit_btn"),
                ) {
                  Text("Return to Vault Library")
                }
              }
            }
          }
        }
      }
    }
  }
}

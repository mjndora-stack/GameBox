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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.GameEntryEntity
import com.example.data.local.GameQuickResumeEntity
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
  savedQuickResume: GameQuickResumeEntity? = null,
  onSaveQuickResume: (String, String, String) -> Unit = { _, _, _ -> },
  onClearQuickResume: (String) -> Unit = {},
  onAwardProgress: (String, Int, Int?, String?) -> Unit,
  onUpgradeFighter: (String, FighterStat, Int) -> Unit,
  onFlipHud: () -> Unit,
  onExitGame: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isPaused by remember { mutableStateOf(false) }
  var showGuideDialog by remember { mutableStateOf(false) }
  var saveTriggerCount by remember(game.id) { mutableIntStateOf(0) }
  var freshRestartCounter by remember(game.id) { mutableIntStateOf(0) }
  var suppressInitialResume by remember(game.id) { mutableStateOf(false) }
  var skipNextDisposeSave by remember(game.id) { mutableStateOf(false) }
  var quickResumeProvider by remember(game.id, freshRestartCounter) {
    mutableStateOf<(() -> Pair<String, String>?)?>(null)
  }

  val initialResumeJson = remember(game.id, freshRestartCounter) {
    if (suppressInitialResume) null else savedQuickResume?.stateJson
  }

  val engineType =
    runCatching { GameEngineType.valueOf(game.engineType) }.getOrDefault(GameEngineType.WEB_CANVAS)

  fun persistCurrentProgress() {
    if (engineType == GameEngineType.WEB_CANVAS) {
      saveTriggerCount++
    } else {
      val provider = quickResumeProvider ?: return
      val snapshot = provider()
      if (snapshot != null) {
        onSaveQuickResume(game.id, snapshot.first, snapshot.second)
      } else {
        onClearQuickResume(game.id)
      }
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, game.id, freshRestartCounter) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        persistCurrentProgress()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      if (!skipNextDisposeSave) {
        persistCurrentProgress()
      }
      skipNextDisposeSave = false
    }
  }

  BackHandler {
    if (showGuideDialog) {
      showGuideDialog = false
    } else if (isPaused) {
      persistCurrentProgress()
      onExitGame()
    } else {
      persistCurrentProgress()
      isPaused = true
    }
  }

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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(
            onClick = {
              persistCurrentProgress()
              isPaused = true
            },
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
          IconButton(
            onClick = { showGuideDialog = true },
            modifier = Modifier.testTag("container_guide_btn"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "How it works and how to complete")
          }
          IconButton(onClick = onFlipHud, modifier = Modifier.testTag("container_flip_hud")) {
            Icon(Icons.Rounded.SwapHoriz, contentDescription = "Flip left/right controls")
          }
          IconButton(
            onClick = {
              if (!isPaused) persistCurrentProgress()
              isPaused = !isPaused
            },
            modifier = Modifier.testTag("container_pause_btn"),
          ) {
            Icon(
              imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
              contentDescription = "Pause container",
            )
          }
        }
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      key(game.id, freshRestartCounter) {
        when (engineType) {
          GameEngineType.WEB_CANVAS ->
            EmbeddedWebGameEngine(
              gameId = game.id,
              assetUrl = game.assetPath,
              customHtmlContent = game.customHtmlContent,
              initialResumeJson = initialResumeJson,
              saveTriggerCount = saveTriggerCount,
              onAwardXp = { gId, amt -> onAwardProgress(gId, amt, null, null) },
              onSubmitScore = { gId, sc -> onAwardProgress(gId, 0, sc, null) },
              onUnlockAchievement = { achId -> onAwardProgress(game.id, 0, null, achId) },
              onSaveQuickResume = onSaveQuickResume,
              onClearQuickResume = onClearQuickResume,
            )
          GameEngineType.ARCADE_NATIVE -> {
            val callback: (Int, Int?, String?) -> Unit = { xp, score, ach ->
              onAwardProgress(game.id, xp, score, ach)
            }
            val registerProvider: (() -> Pair<String, String>?) -> Unit = { p ->
              quickResumeProvider = p
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
                  savedStateJson = initialResumeJson,
                  onRegisterQuickResumeProvider = registerProvider,
                )
              "flappy_bird" ->
                FlappyBirdGame(
                  onAwardXp = callback,
                  savedStateJson = initialResumeJson,
                  onRegisterQuickResumeProvider = registerProvider,
                )
              else ->
                FlappyBirdGame(
                  onAwardXp = callback,
                  savedStateJson = initialResumeJson,
                  onRegisterQuickResumeProvider = registerProvider,
                )
            }
          }
          GameEngineType.PUZZLE_NATIVE -> {
            val callback: (Int, Int?, String?) -> Unit = { xp, score, ach ->
              onAwardProgress(game.id, xp, score, ach)
            }
            val registerProvider: (() -> Pair<String, String>?) -> Unit = { p ->
              quickResumeProvider = p
            }
            when (game.id) {
              "prism_2048" -> Prism2048Game(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "flux_circuit" -> FluxCircuitGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "vault_sweeper" -> VaultSweeperGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "lumen_toggle" -> LumenToggleGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "chroma_flood" -> ChromaFloodGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "nonogrid" -> NonoGridGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "slide_15" -> Slide15Game(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "hanoi_vault" -> HanoiVaultGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "vault_chess" -> VaultChessGame(currentTier = uiState.currentTier, onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "cipher_break" -> CipherMastermindGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "sudoku_six" -> SudokuSixGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "crate_sokoban" -> CrateSokobanGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "queens_grid" -> QueensGridGame(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              "block_blast" -> BlockBlastGame(initialBestScore = game.highScore, onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
              else -> Prism2048Game(onAwardXp = callback, savedStateJson = initialResumeJson, onRegisterQuickResumeProvider = registerProvider)
            }
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
                  "All XP, badges, scores, and Quick Resume progress stay safely saved to your offline Vault.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                savedQuickResume?.let { saved ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                      Text("Quick Resume Saved: ${saved.summaryLabel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
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
                  onClick = {
                    persistCurrentProgress()
                    onExitGame()
                  },
                  modifier = Modifier.fillMaxWidth().testTag("pause_exit_btn"),
                ) {
                  Text("Save & Return to Library")
                }
                TextButton(
                  onClick = {
                    skipNextDisposeSave = true
                    suppressInitialResume = true
                    onClearQuickResume(game.id)
                    freshRestartCounter++
                    isPaused = false
                  },
                  modifier = Modifier.testTag("pause_restart_fresh_btn"),
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.size(6.dp))
                  Text("Restart Fresh Run")
                }
              }
            }
          }
        }
      }
    }
  }

  if (showGuideDialog) {
    AlertDialog(
      onDismissRequest = { showGuideDialog = false },
      title = { Text("${game.title} Guide") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(game.description, style = MaterialTheme.typography.bodyMedium)
          if (game.howItWorks.isNotBlank()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("HOW IT WORKS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(game.howItWorks, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          if (game.howToComplete.isNotBlank()) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("HOW TO COMPLETE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(game.howToComplete, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("CONTROLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              Text(game.controlsHint, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showGuideDialog = false }) { Text("Back to Game") }
      },
    )
  }
}


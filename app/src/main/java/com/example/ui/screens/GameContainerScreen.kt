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
import com.example.data.local.TextAdventureSaveEntity
import com.example.data.model.ControlConfig
import com.example.data.model.GameEngineType
import com.example.ui.engines.ApexVectorEngine
import com.example.ui.engines.AshCipherEngine
import com.example.ui.engines.CryptOfEchoesEngine
import com.example.ui.engines.EmbeddedWebGameEngine
import com.example.ui.engines.ViceHorizonEngine
import com.example.ui.viewmodel.VaultUiState

@Composable
fun GameContainerScreen(
  game: GameEntryEntity,
  uiState: VaultUiState,
  onAwardProgress: (String, Int, Int?, String?) -> Unit,
  onSaveTextAdventure: (TextAdventureSaveEntity) -> Unit,
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
    // Minimalist Embedded Container Bar
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

    // Active Game Engine Surface
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
        GameEngineType.TEXT_ADVENTURE ->
          AshCipherEngine(
            profileId = uiState.activeProfile?.id ?: 1L,
            savedState = uiState.textAdventureSave,
            onSaveState = onSaveTextAdventure,
            onAwardXp = { xp, score, ach -> onAwardProgress(game.id, xp, score, ach) },
          )
        GameEngineType.DUNGEON_3D ->
          CryptOfEchoesEngine(
            controlConfig = uiState.preferences.controlConfig,
            onAwardXp = { xp, score, ach -> onAwardProgress(game.id, xp, score, ach) },
          )
        GameEngineType.RACING_3D ->
          ApexVectorEngine(
            controlConfig = uiState.preferences.controlConfig,
            onAwardXp = { xp, score, ach -> onAwardProgress(game.id, xp, score, ach) },
          )
        GameEngineType.VICE_SANDBOX ->
          ViceHorizonEngine(
            controlConfig = uiState.preferences.controlConfig,
            onAwardXp = { xp, score, ach -> onAwardProgress(game.id, xp, score, ach) },
          )
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

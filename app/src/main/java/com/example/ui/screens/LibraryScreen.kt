package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.VaultAudioEngine
import com.example.data.local.GameEntryEntity
import com.example.data.model.LibraryFilter
import com.example.data.model.PlayerTier
import com.example.ui.components.DailyMissionsPanel
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.TierProgressHeader
import com.example.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
  uiState: VaultUiState,
  onFilterSelected: (LibraryFilter) -> Unit,
  onSearchChanged: (String) -> Unit,
  onLaunchGame: (GameEntryEntity) -> Unit,
  onEquipBadge: (String?) -> Unit,
  onCycleBonusMissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var inspectingGame by remember { mutableStateOf<GameEntryEntity?>(null) }

  val profile = uiState.activeProfile
  val xp = profile?.totalXp ?: 0

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns =
      when {
        maxWidth >= 900.dp -> 3
        maxWidth >= 600.dp -> 2
        else -> 1
      }

    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(span = { GridItemSpan(columns) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          TierProgressHeader(
            playerName = profile?.name ?: "Traveler One",
            xp = xp,
            credits = profile?.vaultCredits ?: 100,
            tier = uiState.currentTier,
            progress = uiState.tierProgress,
            equippedBadge = uiState.equippedCosmeticBadge,
          )

          DailyMissionsPanel(
            missions = uiState.dailyMissions,
            cosmeticBadges = uiState.cosmeticBadges,
            equippedBadgeId = uiState.activeProfile?.equippedBadgeId,
            onLaunchMissionGame = { gameId ->
              val target = uiState.games.firstOrNull { it.id == gameId }
              if (target != null) {
                val reqTier = runCatching { PlayerTier.valueOf(target.requiredTier) }.getOrDefault(PlayerTier.HOBBYIST)
                if (uiState.currentTier.levelIndex < reqTier.levelIndex) {
                  inspectingGame = target
                } else {
                  VaultAudioEngine.playLobbyButtonSound(accent = true)
                  onLaunchGame(target)
                }
              }
            },
            onEquipBadge = { badgeId ->
              VaultAudioEngine.playLobbyButtonSound(accent = true)
              onEquipBadge(badgeId)
            },
            onCycleBonusSet = {
              VaultAudioEngine.playLobbyButtonSound(accent = true)
              onCycleBonusMissions()
            },
          )

          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            singleLine = true,
            placeholder = { Text("Search arcade, platformers, chess & puzzles...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
              if (uiState.searchQuery.isNotEmpty()) {
                IconButton(onClick = { VaultAudioEngine.playLobbyButtonSound(); onSearchChanged("") }) {
                  Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
              }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LibraryFilter.entries) { filter ->
              FilterChip(
                selected = uiState.selectedFilter == filter,
                onClick = { VaultAudioEngine.playLobbyButtonSound(); onFilterSelected(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
              )
            }
          }
        }
      }

      if (uiState.filteredGames.isEmpty()) {
        item(span = { GridItemSpan(columns) }) {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.OfflineBolt, contentDescription = null, modifier = Modifier.size(36.dp))
              Text("No matching drops in vault", style = MaterialTheme.typography.titleMedium)
              Text(
                "Clear filters or add a new game drop in Creator Studio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(uiState.filteredGames, key = { it.id }) { game ->
          val requiredTier = runCatching { PlayerTier.valueOf(game.requiredTier) }.getOrDefault(PlayerTier.HOBBYIST)
          val isLocked = uiState.currentTier.levelIndex < requiredTier.levelIndex

          GameVaultCard(
            game = game,
            isLocked = isLocked,
            requiredTier = requiredTier,
            currentXp = xp,
            onCardClick = { VaultAudioEngine.playLobbyButtonSound(); inspectingGame = game },
            onQuickLaunch = {
              VaultAudioEngine.playLobbyButtonSound(accent = !isLocked)
              if (isLocked) inspectingGame = game else onLaunchGame(game)
            },
          )
        }
      }

      item(span = { GridItemSpan(columns) }) {
        Spacer(Modifier.height(12.dp))
      }
    }
  }

  inspectingGame?.let { game ->
    val requiredTier = runCatching { PlayerTier.valueOf(game.requiredTier) }.getOrDefault(PlayerTier.HOBBYIST)
    val isLocked = uiState.currentTier.levelIndex < requiredTier.levelIndex
    ModalBottomSheet(onDismissRequest = { inspectingGame = null }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        GameCoverArtwork(
          coverKey = game.coverKey,
          title = game.title,
          isLocked = isLocked,
          requiredTierLabel = requiredTier.displayName,
          isFanFavourite = game.isFanFavourite,
          modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.large),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(game.title, style = MaterialTheme.typography.headlineSmall)
            Text("${game.genre} · ${game.versionTag}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
          Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(999.dp)) {
            Text(game.sizeLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
          }
        }
        Text(game.description, style = MaterialTheme.typography.bodyLarge)
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CONTROLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(game.controlsHint, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("RELEASE NOTE · ${game.releaseNote}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        if (isLocked) {
          val remaining = (requiredTier.minXp - xp).coerceAtLeast(0)
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
              Text(
                "Requires ${requiredTier.displayName} Rank (${requiredTier.minXp} XP). Earn $remaining more XP across unlocked games to play!",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        } else {
          Button(
            onClick = {
              VaultAudioEngine.playLobbyButtonSound(accent = true)
              inspectingGame = null
              onLaunchGame(game)
            },
            modifier = Modifier.fillMaxWidth().testTag("sheet_launch_${game.id}"),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Launch in Embedded Engine")
          }
        }
        Spacer(Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun GameVaultCard(
  game: GameEntryEntity,
  isLocked: Boolean,
  requiredTier: PlayerTier,
  currentXp: Int,
  onCardClick: () -> Unit,
  onQuickLaunch: () -> Unit,
) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onCardClick)
        .testTag("game_card_${game.id}"),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        GameCoverArtwork(
          coverKey = game.coverKey,
          title = game.title,
          isLocked = isLocked,
          requiredTierLabel = requiredTier.displayName,
          isFanFavourite = game.isFanFavourite,
          modifier = Modifier.fillMaxSize(),
        )
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
        ) {
          Text(
            text = game.versionTag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          )
        }
      }

      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = game.title,
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = game.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Button(
            onClick = onQuickLaunch,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("play_button_${game.id}"),
          ) {
            if (isLocked) {
              Icon(Icons.Rounded.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(4.dp))
              Text(requiredTier.displayName)
            } else {
              Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(4.dp))
              Text("Play")
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = game.genre,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              if (isLocked) "${(requiredTier.minXp - currentXp).coerceAtLeast(0)} XP left"
              else if (game.highScore > 0) "BEST ${game.highScore}"
              else "OFFLINE READY",
            style = MaterialTheme.typography.labelSmall,
            color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

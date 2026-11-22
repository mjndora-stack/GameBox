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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.VaultAudioEngine
import com.example.data.local.GameEntryEntity
import com.example.data.local.GameQuickResumeEntity
import com.example.data.model.LibraryFilter
import com.example.data.model.PlayerTier
import com.example.ui.components.DailyMissionsPanel
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.UserProfileCard
import com.example.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
  uiState: VaultUiState,
  onFilterSelected: (LibraryFilter) -> Unit,
  onSearchChanged: (String) -> Unit,
  onLaunchGame: (GameEntryEntity) -> Unit,
  onClearQuickResume: (String) -> Unit = {},
  onEquipBadge: (String?) -> Unit,
  onSelectAvatar: (String) -> Unit,
  onCycleBonusMissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var inspectingGame by remember { mutableStateOf<GameEntryEntity?>(null) }

  val profile = uiState.activeProfile
  val xp = profile?.totalXp ?: 0
  val resumableEntries =
    remember(uiState.games, uiState.quickResumes, uiState.currentTier) {
      uiState.quickResumes.values
        .sortedByDescending { it.updatedAt }
        .mapNotNull { resume ->
          val game = uiState.games.firstOrNull { it.id == resume.gameId } ?: return@mapNotNull null
          val reqTier = runCatching { PlayerTier.valueOf(game.requiredTier) }.getOrDefault(PlayerTier.HOBBYIST)
          if (uiState.currentTier.levelIndex < reqTier.levelIndex) null else game to resume
        }
    }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val columns =
      when {
        maxWidth >= 900.dp -> 4
        maxWidth >= 600.dp -> 3
        else -> 2
      }

    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(span = { GridItemSpan(columns) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          UserProfileCard(
            playerName = profile?.name ?: "Traveler One",
            avatarId = profile?.avatarIcon,
            cumulativeXp = xp,
            credits = profile?.vaultCredits ?: 100,
            gamesPlayed = profile?.gamesPlayedCount ?: 0,
            tier = uiState.currentTier,
            tierProgress = uiState.tierProgress,
            cosmeticBadges = uiState.cosmeticBadges,
            equippedBadge = uiState.equippedCosmeticBadge,
            onSelectAvatar = onSelectAvatar,
            onEquipBadge = onEquipBadge,
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

          if (resumableEntries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  Icons.Rounded.BookmarkAdded,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  "CONTINUE PLAYING · QUICK RESUME",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(resumableEntries, key = { it.first.id }) { (game, resume) ->
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier =
                      Modifier
                        .width(230.dp)
                        .clickable {
                          VaultAudioEngine.playLobbyButtonSound(accent = true)
                          onLaunchGame(game)
                        }
                        .testTag("quick_resume_strip_${game.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                      ) {
                        Icon(
                          Icons.Rounded.PlayArrow,
                          contentDescription = "Resume ${game.title}",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(8.dp).size(20.dp),
                        )
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = game.title,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = resume.summaryLabel,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

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
          val savedResume = if (isLocked) null else uiState.quickResumes[game.id]

          GameVaultCard(
            game = game,
            isLocked = isLocked,
            requiredTier = requiredTier,
            currentXp = xp,
            quickResume = savedResume,
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
    val savedResume = if (isLocked) null else uiState.quickResumes[game.id]
    ModalBottomSheet(onDismissRequest = { inspectingGame = null }) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
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

        if (savedResume != null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column(modifier = Modifier.weight(1f)) {
                Text("QUICK RESUME READY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Saved state: ${savedResume.summaryLabel}", style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }

        Text(game.description, style = MaterialTheme.typography.bodyLarge)

        if (game.howItWorks.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("HOW IT WORKS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
              Text(game.howItWorks, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        if (game.howToComplete.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("HOW TO COMPLETE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
              Text(game.howToComplete, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

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
            Text(if (savedResume != null) "Resume Saved Game" else "Launch in Embedded Engine")
          }
          if (savedResume != null) {
            OutlinedButton(
              onClick = {
                VaultAudioEngine.playLobbyButtonSound()
                onClearQuickResume(game.id)
                inspectingGame = null
                onLaunchGame(game)
              },
              modifier = Modifier.fillMaxWidth().testTag("sheet_fresh_${game.id}"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.size(6.dp))
              Text("Start Fresh Run")
            }
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
  quickResume: GameQuickResumeEntity?,
  onCardClick: () -> Unit,
  onQuickLaunch: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onCardClick)
        .testTag("game_card_${game.id}"),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Roblox-style rounded square in-game screenshot thumbnail
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border =
        BorderStroke(
          1.dp,
          if (quickResume != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
          else MaterialTheme.colorScheme.outlineVariant,
        ),
      modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        GameCoverArtwork(
          coverKey = game.coverKey,
          title = game.title,
          isLocked = isLocked,
          requiredTierLabel = requiredTier.displayName,
          isFanFavourite = game.isFanFavourite,
          modifier = Modifier.fillMaxSize(),
        )

        if (quickResume != null) {
          Surface(
            color = androidx.compose.ui.graphics.Color(0xEB0B121D),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                Icons.Rounded.BookmarkAdded,
                contentDescription = "Quick Resume saved",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = "SAVED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        // Roblox-style Play / Resume Corner Action Button (48.dp accessible touch target)
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .align(Alignment.BottomEnd)
              .padding(4.dp)
              .size(48.dp)
              .clickable(onClick = onQuickLaunch)
              .testTag("play_button_${game.id}"),
        ) {
          Surface(
            color =
              when {
                isLocked -> androidx.compose.ui.graphics.Color(0xEB1E2430)
                quickResume != null -> MaterialTheme.colorScheme.primary
                else -> androidx.compose.ui.graphics.Color(0xFF00B06F)
              },
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.PlayArrow,
                contentDescription = if (isLocked) "Locked" else if (quickResume != null) "Resume ${game.title}" else "Play ${game.title}",
                tint =
                  when {
                    isLocked -> androidx.compose.ui.graphics.Color(0xFFFFC857)
                    quickResume != null -> MaterialTheme.colorScheme.onPrimary
                    else -> androidx.compose.ui.graphics.Color.White
                  },
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }
    }

    // Compact Roblox-style Title + Best/Genre Caption
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = game.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = quickResume?.summaryLabel ?: game.genre,
          style = MaterialTheme.typography.labelSmall,
          color = if (quickResume != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Text(
          text =
            when {
              isLocked -> "${(requiredTier.minXp - currentXp).coerceAtLeast(0)} XP"
              game.highScore > 0 -> "★ ${game.highScore}"
              else -> game.versionTag
            },
          style = MaterialTheme.typography.labelSmall,
          color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}


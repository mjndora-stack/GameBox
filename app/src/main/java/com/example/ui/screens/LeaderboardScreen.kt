package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.example.audio.VaultAudioEngine
import com.example.data.local.CosmeticBadgeEntity
import com.example.data.model.PlayerTier
import com.example.ui.components.CosmeticBadgeEmblem
import com.example.ui.components.UserAvatarImage
import com.example.ui.components.VaultAvatarCatalog
import com.example.ui.viewmodel.VaultUiState

@Composable
fun LeaderboardScreen(
  uiState: VaultUiState,
  onSwitchProfile: (Long) -> Unit,
  onCreateProfile: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedGameId by remember { mutableStateOf("global_xp") }
  var showAddProfileDialog by remember { mutableStateOf(false) }
  var newProfileName by remember { mutableStateOf("") }
  var newProfileAvatar by remember { mutableStateOf("cyber") }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("OFFLINE SOCIAL & RIVALS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Text("Local Pass-and-Play & Ghosts", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedButton(
          onClick = { VaultAudioEngine.playLobbyButtonSound(); showAddProfileDialog = true },
          modifier = Modifier.testTag("add_traveler_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(4.dp))
          Text("Traveler")
        }
      }
    }

    // Traveler Profile Switcher
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uiState.allProfiles, key = { it.id }) { profile ->
          val active = profile.id == uiState.activeProfile?.id
          FilterChip(
            selected = active,
            onClick = { VaultAudioEngine.playLobbyButtonSound(); onSwitchProfile(profile.id) },
            leadingIcon = { UserAvatarImage(avatarId = profile.avatarIcon, contentDescription = null, size = 20.dp) },
            label = { Text("${profile.name} (${profile.totalXp} XP)") },
            modifier = Modifier.testTag("profile_chip_${profile.id}"),
          )
        }
      }
    }

    // Board Selector
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedGameId == "global_xp",
            onClick = { VaultAudioEngine.playLobbyButtonSound(); selectedGameId = "global_xp" },
            label = { Text("Global XP") },
            modifier = Modifier.testTag("lb_tab_global"),
          )
        }
        items(uiState.games, key = { it.id }) { game ->
          FilterChip(
            selected = selectedGameId == game.id,
            onClick = { VaultAudioEngine.playLobbyButtonSound(); selectedGameId = game.id },
            label = { Text(game.title) },
            modifier = Modifier.testTag("lb_tab_${game.id}"),
          )
        }
      }
    }

    if (selectedGameId == "global_xp") {
      val sortedProfiles = uiState.allProfiles.sortedByDescending { it.totalXp }
      val badgeById = uiState.cosmeticBadges.associateBy { it.id }
      itemsIndexed(sortedProfiles, key = { _, item -> item.id }) { index, profile ->
        val tier = PlayerTier.fromXp(profile.totalXp)
        val equipped = profile.equippedBadgeId?.let { badgeById[it] }
        LeaderboardRowCard(
          rank = index + 1,
          name = profile.name,
          subtitle =
            if (equipped != null) "${tier.displayName} · ${equipped.title}"
            else "${tier.displayName} · ${profile.gamesPlayedCount} launches",
          scoreText = "${profile.totalXp} XP",
          isGhost = false,
          isCurrentPlayer = profile.id == uiState.activeProfile?.id,
          equippedBadge = equipped,
          avatarId = profile.avatarIcon,
        )
      }
    } else {
      val entries =
        uiState.leaderboardEntries
          .filter { it.gameId == selectedGameId }
          .sortedByDescending { it.score }
      if (entries.isEmpty()) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "No scores recorded yet for this title. Play a session to claim #1!",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(20.dp),
            )
          }
        }
      } else {
        itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
          LeaderboardRowCard(
            rank = index + 1,
            name = entry.playerName,
            subtitle = if (entry.isGhostRival) "Dev Ghost Benchmark · ${entry.tierName}" else "Local Traveler · ${entry.tierName}",
            scoreText = "${entry.score} PTS",
            isGhost = entry.isGhostRival,
            isCurrentPlayer = !entry.isGhostRival && entry.playerName == uiState.activeProfile?.name,
          )
        }
      }
    }

    item { Spacer(Modifier.height(12.dp)) }
  }

  if (showAddProfileDialog) {
    AlertDialog(
      onDismissRequest = { showAddProfileDialog = false },
      title = { Text("New Offline Traveler") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Add a local player profile for pass-and-play leaderboard rivalry with zero internet required.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedTextField(
            value = newProfileName,
            onValueChange = { newProfileName = it },
            singleLine = true,
            label = { Text("Traveler Name") },
            modifier = Modifier.fillMaxWidth().testTag("new_traveler_input"),
          )
          Text("Pick Avatar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            VaultAvatarCatalog.OPTIONS.forEach { option ->
              val picked = newProfileAvatar == option.id
              Surface(
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                  if (picked) 2.dp else 1.dp,
                  if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.clickable { newProfileAvatar = option.id },
              ) {
                UserAvatarImage(
                  avatarId = option.id,
                  contentDescription = option.label,
                  size = 44.dp,
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newProfileName.isNotBlank()) {
              onCreateProfile(newProfileName, newProfileAvatar)
              newProfileName = ""
              showAddProfileDialog = false
            }
          },
          modifier = Modifier.testTag("confirm_new_traveler"),
        ) {
          Text("Create")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddProfileDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun LeaderboardRowCard(
  rank: Int,
  name: String,
  subtitle: String,
  scoreText: String,
  isGhost: Boolean,
  isCurrentPlayer: Boolean,
  equippedBadge: CosmeticBadgeEntity? = null,
  avatarId: String? = null,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (isCurrentPlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        1.dp,
        if (isCurrentPlayer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(36.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelMedium,
            color = if (rank == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      if (avatarId != null) {
        UserAvatarImage(avatarId = avatarId, contentDescription = null, size = 34.dp)
      }
      if (equippedBadge != null) {
        CosmeticBadgeEmblem(
          emblemKey = equippedBadge.emblemKey,
          accentColor = androidx.compose.ui.graphics.Color(equippedBadge.accentHex),
          unlocked = true,
          size = 30.dp,
        )
      } else if (avatarId == null) {
        Icon(
          imageVector = if (isGhost) Icons.Rounded.SmartToy else Icons.Rounded.EmojiEvents,
          contentDescription = null,
          tint = if (isGhost) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(scoreText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
  }
}

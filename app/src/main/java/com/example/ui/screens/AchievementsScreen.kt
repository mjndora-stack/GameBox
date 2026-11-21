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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.VaultAudioEngine
import com.example.data.local.AchievementEntity
import com.example.data.model.PlayerTier
import com.example.ui.components.CosmeticBadgesShowcase
import com.example.ui.components.DailyMissionsPanel
import com.example.ui.components.TierProgressHeader
import com.example.ui.viewmodel.VaultUiState

@Composable
fun AchievementsScreen(
  uiState: VaultUiState,
  onEquipBadge: (String?) -> Unit,
  onCycleBonusMissions: () -> Unit,
  onLaunchMissionGame: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val profile = uiState.activeProfile
  val xp = profile?.totalXp ?: 0
  val unlockedCount = uiState.achievements.count { it.unlockedAt != null }
  val cosmeticUnlockedCount = uiState.cosmeticBadges.count { it.unlockedAt != null }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      TierProgressHeader(
        playerName = profile?.name ?: "Traveler One",
        xp = xp,
        credits = profile?.vaultCredits ?: 100,
        tier = uiState.currentTier,
        progress = uiState.tierProgress,
        equippedBadge = uiState.equippedCosmeticBadge,
      )
    }

    item {
      DailyMissionsPanel(
        missions = uiState.dailyMissions,
        cosmeticBadges = uiState.cosmeticBadges,
        equippedBadgeId = uiState.activeProfile?.equippedBadgeId,
        onLaunchMissionGame = { gameId ->
          VaultAudioEngine.playLobbyButtonSound(accent = true)
          onLaunchMissionGame(gameId)
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
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "SPECIAL COSMETIC BADGES",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "$cosmeticUnlockedCount / ${uiState.cosmeticBadges.size} COLLECTED",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      CosmeticBadgesShowcase(
        cosmeticBadges = uiState.cosmeticBadges,
        equippedBadgeId = uiState.activeProfile?.equippedBadgeId,
        onEquipBadge = { badgeId ->
          VaultAudioEngine.playLobbyButtonSound(accent = true)
          onEquipBadge(badgeId)
        },
      )
    }

    item {
      Text(
        text = "PROGRESSION LADDER",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    item {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          PlayerTier.entries.forEach { tier ->
            val reached = uiState.currentTier.levelIndex >= tier.levelIndex
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = if (reached) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                  Text(tier.displayName, style = MaterialTheme.typography.titleMedium)
                  Text(tier.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (reached) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = "${tier.minXp} XP",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ACHIEVEMENT BADGES",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "$unlockedCount / ${uiState.achievements.size} UNLOCKED",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    items(uiState.achievements, key = { it.id }) { badge ->
      AchievementBadgeCard(badge)
    }

    item { Spacer(Modifier.height(12.dp)) }
  }
}

@Composable
private fun AchievementBadgeCard(badge: AchievementEntity) {
  val unlocked = badge.unlockedAt != null
  val icon =
    when (badge.iconKey) {
      "rocket" -> Icons.Rounded.RocketLaunch
      "bolt" -> Icons.Rounded.Bolt
      "flag" -> Icons.Rounded.Flag
      "speed" -> Icons.Rounded.Speed
      "book" -> Icons.Rounded.AutoStories
      "shield" -> Icons.Rounded.Shield
      "trophy" -> Icons.Rounded.EmojiEvents
      "star" -> Icons.Rounded.Star
      "verified" -> Icons.Rounded.Verified
      else -> Icons.Rounded.WorkspacePremium
    }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border =
      BorderStroke(
        1.dp,
        if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth().testTag("badge_card_${badge.id}"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(44.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(10.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(badge.title, style = MaterialTheme.typography.titleMedium)
        Text(badge.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
      ) {
        Text(
          text = if (unlocked) "+${badge.xpReward} XP" else "${badge.xpReward} XP",
          style = MaterialTheme.typography.labelSmall,
          color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
      }
    }
  }
}

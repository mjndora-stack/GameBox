package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.VaultAudioEngine
import com.example.data.local.CosmeticBadgeEntity
import com.example.data.model.PlayerTier

data class VaultAvatarOption(
  val id: String,
  val label: String,
  val subtitle: String,
  @DrawableRes val drawableRes: Int,
)

object VaultAvatarCatalog {
  val OPTIONS: List<VaultAvatarOption> =
    listOf(
      VaultAvatarOption(
        id = "cyber",
        label = "Cyber Visor",
        subtitle = "Neon Runner",
        drawableRes = R.drawable.img_avatar_cyber,
      ),
      VaultAvatarOption(
        id = "striker",
        label = "Dojo Striker",
        subtitle = "Street Martial",
        drawableRes = R.drawable.img_avatar_striker,
      ),
      VaultAvatarOption(
        id = "tactician",
        label = "Grand Tactician",
        subtitle = "Vault Knight",
        drawableRes = R.drawable.img_avatar_tactician,
      ),
      VaultAvatarOption(
        id = "aviator",
        label = "Sky Aviator",
        subtitle = "Horizon Pilot",
        drawableRes = R.drawable.img_avatar_aviator,
      ),
    )

  fun resolve(avatarId: String?): VaultAvatarOption =
    OPTIONS.firstOrNull { it.id == avatarId } ?: OPTIONS.first()
}

@Composable
fun UserAvatarImage(
  avatarId: String?,
  contentDescription: String? = null,
  size: Dp = 56.dp,
  borderColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  val option = VaultAvatarCatalog.resolve(avatarId)
  Image(
    painter = painterResource(id = option.drawableRes),
    contentDescription = contentDescription,
    contentScale = ContentScale.Crop,
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, borderColor, CircleShape),
  )
}

@Composable
fun UserProfileCard(
  playerName: String,
  avatarId: String?,
  cumulativeXp: Int,
  credits: Int,
  gamesPlayed: Int,
  tier: PlayerTier,
  tierProgress: Float,
  cosmeticBadges: List<CosmeticBadgeEntity>,
  equippedBadge: CosmeticBadgeEntity?,
  onSelectAvatar: (String) -> Unit,
  onEquipBadge: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAvatarPicker by remember { mutableStateOf(false) }
  val earnedBadges = remember(cosmeticBadges) { cosmeticBadges.filter { it.unlockedAt != null } }
  val nextTier = tier.nextTier()

  Surface(
    modifier = modifier.fillMaxWidth().testTag("user_profile_card"),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .clickable {
                VaultAudioEngine.playLobbyButtonSound()
                showAvatarPicker = true
              }
              .testTag("profile_avatar_picker_trigger"),
        ) {
          UserAvatarImage(
            avatarId = avatarId,
            contentDescription = "Change profile avatar",
            size = 64.dp,
            borderColor =
              if (equippedBadge != null) Color(equippedBadge.accentHex)
              else MaterialTheme.colorScheme.primary,
          )
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                Icons.Rounded.Edit,
                contentDescription = "Pick avatar",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp),
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = playerName,
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (equippedBadge != null) {
              CosmeticBadgeEmblem(
                emblemKey = equippedBadge.emblemKey,
                accentColor = Color(equippedBadge.accentHex),
                unlocked = true,
                size = 24.dp,
              )
            }
          }
          Text(
            text = "${tier.displayName} Rank · ${equippedBadge?.title ?: tier.subtitle}",
            style = MaterialTheme.typography.bodyMedium,
            color =
              if (equippedBadge != null) Color(equippedBadge.accentHex)
              else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = "$cumulativeXp XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp).testTag("profile_cumulative_xp"),
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = "$credits CR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = "$gamesPlayed PLAYS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        LinearProgressIndicator(
          progress = { tierProgress.coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(7.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          strokeCap = StrokeCap.Round,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text = "Cumulative XP: $cumulativeXp",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              if (nextTier != null) "${(nextTier.minXp - cumulativeXp).coerceAtLeast(0)} XP to ${nextTier.displayName}"
              else "MAX RANK · MYTHIC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Earned Special Cosmetic Badges strip
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "EARNED COSMETIC BADGES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = "${earnedBadges.size} / ${cosmeticBadges.size}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (earnedBadges.isEmpty()) {
            Text(
              text = "Complete daily missions to unlock collectible cosmetic badges for your profile.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(end = 4.dp),
            ) {
              items(earnedBadges, key = { it.id }) { badge ->
                val isEquipped = equippedBadge?.id == badge.id
                val accent = Color(badge.accentHex)
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color =
                    if (isEquipped) accent.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface,
                  border =
                    BorderStroke(
                      if (isEquipped) 1.5.dp else 1.dp,
                      if (isEquipped) accent else MaterialTheme.colorScheme.outlineVariant,
                    ),
                  modifier =
                    Modifier
                      .clickable {
                        VaultAudioEngine.playLobbyButtonSound(accent = true)
                        onEquipBadge(if (isEquipped) null else badge.id)
                      }
                      .testTag("profile_badge_${badge.id}"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    CosmeticBadgeEmblem(
                      emblemKey = badge.emblemKey,
                      accentColor = accent,
                      unlocked = true,
                      size = 24.dp,
                    )
                    Column {
                      Text(badge.title, style = MaterialTheme.typography.labelMedium)
                      Text(
                        text = if (isEquipped) "Equipped" else "Tap to equip",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEquipped) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAvatarPicker) {
    AvatarPickerDialog(
      selectedAvatarId = VaultAvatarCatalog.resolve(avatarId).id,
      onSelectAvatar = { chosen ->
        onSelectAvatar(chosen)
        showAvatarPicker = false
      },
      onDismiss = { showAvatarPicker = false },
    )
  }
}

@Composable
fun AvatarPickerDialog(
  selectedAvatarId: String,
  onSelectAvatar: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Choose Profile Avatar")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Pick a custom illustrated traveler portrait for your VaultPlay 2 profile.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val rows = VaultAvatarCatalog.OPTIONS.chunked(2)
        rows.forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            rowItems.forEach { option ->
              val isSelected = option.id == selectedAvatarId
              Surface(
                shape = MaterialTheme.shapes.medium,
                color =
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border =
                  BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier =
                  Modifier
                    .weight(1f)
                    .clickable {
                      VaultAudioEngine.playLobbyButtonSound(accent = true)
                      onSelectAvatar(option.id)
                    }
                    .testTag("avatar_option_${option.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box {
                    Image(
                      painter = painterResource(id = option.drawableRes),
                      contentDescription = option.label,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.size(84.dp).clip(CircleShape),
                    )
                    if (isSelected) {
                      Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier =
                          Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .size(22.dp),
                      )
                    }
                  }
                  Text(option.label, style = MaterialTheme.typography.labelLarge)
                  Text(
                    option.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

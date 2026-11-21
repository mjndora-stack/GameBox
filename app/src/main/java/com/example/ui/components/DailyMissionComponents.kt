package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.CosmeticBadgeEntity
import com.example.data.local.DailyMissionEntity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CosmeticBadgeEmblem(
  emblemKey: String,
  accentColor: Color,
  unlocked: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
) {
  val primary = if (unlocked) accentColor else Color(0xFF64748B)
  val bgTop = if (unlocked) accentColor.copy(alpha = 0.28f) else Color(0xFF1A202C)
  val bgBot = if (unlocked) Color(0xFF0E1422) else Color(0xFF12161F)

  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.46f

    // Sculpted Hexagonal Shield Frame
    val frame = Path().apply {
      for (i in 0 until 6) {
        val a = (i * 60f - 90f) * (PI.toFloat() / 180f)
        val px = cx + cos(a) * r
        val py = cy + sin(a) * r
        if (i == 0) moveTo(px, py) else lineTo(px, py)
      }
      close()
    }
    drawPath(frame, Brush.verticalGradient(listOf(bgTop, bgBot)))
    drawPath(
      frame,
      color = primary.copy(alpha = if (unlocked) 0.9f else 0.4f),
      style = Stroke(width = w * 0.055f),
    )

    val strokeW = w * 0.06f
    val glyphColor = if (unlocked) Color.White else Color(0xFF94A3B8)

    when (emblemKey) {
      "king" -> {
        // Chess King Crown + Cross
        drawLine(primary, Offset(cx, cy - r * 0.56f), Offset(cx, cy - r * 0.22f), strokeW, StrokeCap.Round)
        drawLine(primary, Offset(cx - r * 0.16f, cy - r * 0.42f), Offset(cx + r * 0.16f, cy - r * 0.42f), strokeW, StrokeCap.Round)
        val body = Path().apply {
          moveTo(cx - r * 0.36f, cy + r * 0.42f)
          lineTo(cx - r * 0.44f, cy - r * 0.08f)
          lineTo(cx - r * 0.14f, cy + r * 0.08f)
          lineTo(cx, cy - r * 0.22f)
          lineTo(cx + r * 0.14f, cy + r * 0.08f)
          lineTo(cx + r * 0.44f, cy - r * 0.08f)
          lineTo(cx + r * 0.36f, cy + r * 0.42f)
          close()
        }
        drawPath(body, glyphColor)
      }
      "wings" -> {
        // Dual Aviator Wings
        for (dir in listOf(-1f, 1f)) {
          val wing = Path().apply {
            moveTo(cx, cy + r * 0.18f)
            lineTo(cx + dir * r * 0.56f, cy - r * 0.28f)
            lineTo(cx + dir * r * 0.46f, cy + r * 0.04f)
            lineTo(cx + dir * r * 0.24f, cy + r * 0.26f)
            close()
          }
          drawPath(wing, glyphColor)
        }
        drawCircle(primary, r * 0.16f, Offset(cx, cy + r * 0.06f))
      }
      "visor" -> {
        // Cyber Helmet Visor
        val visor = Path().apply {
          moveTo(cx - r * 0.48f, cy - r * 0.12f)
          lineTo(cx + r * 0.48f, cy - r * 0.12f)
          lineTo(cx + r * 0.34f, cy + r * 0.24f)
          lineTo(cx - r * 0.34f, cy + r * 0.24f)
          close()
        }
        drawPath(visor, primary)
        drawLine(Color.White, Offset(cx - r * 0.3f, cy), Offset(cx + r * 0.28f, cy), strokeW * 0.8f, StrokeCap.Round)
      }
      "dragon" -> {
        // Dojo Martial Flame Crest
        val flame = Path().apply {
          moveTo(cx, cy - r * 0.54f)
          lineTo(cx + r * 0.42f, cy + r * 0.06f)
          lineTo(cx + r * 0.16f, cy + r * 0.44f)
          lineTo(cx - r * 0.16f, cy + r * 0.44f)
          lineTo(cx - r * 0.42f, cy + r * 0.06f)
          close()
        }
        drawPath(flame, primary)
        drawCircle(glyphColor, r * 0.16f, Offset(cx, cy + r * 0.08f))
      }
      "bolt" -> {
        // Supersonic Lightning Bolt
        val bolt = Path().apply {
          moveTo(cx + r * 0.12f, cy - r * 0.52f)
          lineTo(cx - r * 0.36f, cy + r * 0.06f)
          lineTo(cx - r * 0.02f, cy + r * 0.06f)
          lineTo(cx - r * 0.14f, cy + r * 0.52f)
          lineTo(cx + r * 0.36f, cy - r * 0.08f)
          lineTo(cx + r * 0.02f, cy - r * 0.08f)
          close()
        }
        drawPath(bolt, glyphColor)
      }
      "prism" -> {
        // Prism Tesseract Diamond
        val dia = Path().apply {
          moveTo(cx, cy - r * 0.50f)
          lineTo(cx + r * 0.44f, cy)
          lineTo(cx, cy + r * 0.50f)
          lineTo(cx - r * 0.44f, cy)
          close()
        }
        drawPath(dia, primary.copy(alpha = 0.7f))
        drawPath(dia, glyphColor, style = Stroke(width = strokeW * 0.85f, join = StrokeJoin.Round))
      }
      "crown" -> {
        // 3-Peak Solar Crown
        val crown = Path().apply {
          moveTo(cx - r * 0.44f, cy + r * 0.34f)
          lineTo(cx - r * 0.50f, cy - r * 0.24f)
          lineTo(cx - r * 0.18f, cy + r * 0.04f)
          lineTo(cx, cy - r * 0.44f)
          lineTo(cx + r * 0.18f, cy + r * 0.04f)
          lineTo(cx + r * 0.50f, cy - r * 0.24f)
          lineTo(cx + r * 0.44f, cy + r * 0.34f)
          close()
        }
        drawPath(crown, glyphColor)
      }
      else -> {
        // Voyager Star Compass
        val star = Path().apply {
          for (i in 0 until 8) {
            val rad = if (i % 2 == 0) r * 0.52f else r * 0.20f
            val a = (i * 45f - 90f) * (PI.toFloat() / 180f)
            val px = cx + cos(a) * rad
            val py = cy + sin(a) * rad
            if (i == 0) moveTo(px, py) else lineTo(px, py)
          }
          close()
        }
        drawPath(star, glyphColor)
      }
    }
  }
}

@Composable
fun DailyMissionsPanel(
  missions: List<DailyMissionEntity>,
  cosmeticBadges: List<CosmeticBadgeEntity>,
  equippedBadgeId: String?,
  onLaunchMissionGame: (String) -> Unit,
  onEquipBadge: (String?) -> Unit,
  onCycleBonusSet: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (missions.isEmpty()) return
  val completedCount = missions.count { it.completedAt != null }
  val allCompleted = completedCount == missions.size
  val badgeById = cosmeticBadges.associateBy { it.id }

  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
    modifier = modifier.fillMaxWidth().testTag("daily_missions_panel"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
          )
          Text("TODAY'S DAILY MISSIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (allCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "$completedCount / ${missions.size} DONE",
            style = MaterialTheme.typography.labelSmall,
            color = if (allCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          )
        }
      }

      missions.forEach { mission ->
        val badge = badgeById[mission.cosmeticBadgeId]
        val isComplete = mission.completedAt != null
        val isEquipped = badge != null && equippedBadgeId == badge.id
        val progress =
          if (mission.targetValue > 0) (mission.currentProgress.toFloat() / mission.targetValue.toFloat()).coerceIn(0f, 1f)
          else 0f
        val animatedProg by
          animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(550, easing = FastOutSlowInEasing),
            label = "mission_prog_${mission.id}",
          )
        val accent = badge?.let { Color(it.accentHex) } ?: MaterialTheme.colorScheme.primary

        Surface(
          shape = MaterialTheme.shapes.medium,
          color = if (isComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, if (isComplete) accent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("daily_mission_card_${mission.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CosmeticBadgeEmblem(
              emblemKey = badge?.emblemKey ?: "compass",
              accentColor = accent,
              unlocked = isComplete || (badge?.unlockedAt != null),
              size = 42.dp,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = mission.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface) {
                  Text(
                    text = "+${mission.bonusXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                  )
                }
              }

              Text(
                text = "${mission.description} Reward: ${badge?.title ?: "Special Badge"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                LinearProgressIndicator(
                  progress = { animatedProg },
                  modifier = Modifier.weight(1f).height(6.dp),
                  color = if (isComplete) accent else MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surface,
                  strokeCap = StrokeCap.Round,
                )
                Text(
                  text = "${mission.currentProgress}/${mission.targetValue}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (isComplete) {
              if (badge != null) {
                OutlinedButton(
                  onClick = { onEquipBadge(if (isEquipped) null else badge.id) },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.heightIn(min = 48.dp).testTag("mission_equip_${badge.id}"),
                ) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(Modifier.size(4.dp))
                  Text(if (isEquipped) "Equipped" else "Equip", style = MaterialTheme.typography.labelSmall)
                }
              }
            } else if (mission.gameId != null) {
              Button(
                onClick = { onLaunchMissionGame(mission.gameId) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("mission_play_${mission.gameId}"),
              ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(2.dp))
                Text("Go", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }

      if (allCompleted) {
        OutlinedButton(
          onClick = onCycleBonusSet,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cycle_bonus_missions_btn"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.size(6.dp))
          Text("All Daily Missions Cleared! Roll Bonus Set")
        }
      }
    }
  }
}

@Composable
fun CosmeticBadgesShowcase(
  cosmeticBadges: List<CosmeticBadgeEntity>,
  equippedBadgeId: String?,
  onEquipBadge: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    cosmeticBadges.forEach { badge ->
      val unlocked = badge.unlockedAt != null
      val equipped = unlocked && equippedBadgeId == badge.id
      val accent = Color(badge.accentHex)

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (equipped) accent else if (unlocked) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("cosmetic_badge_${badge.id}"),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CosmeticBadgeEmblem(
            emblemKey = badge.emblemKey,
            accentColor = accent,
            unlocked = unlocked,
            size = 46.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(badge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = if (unlocked) 0.2f else 0.08f)) {
                Text(
                  text = badge.rarity,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = badge.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (unlocked) {
            if (equipped) {
              OutlinedButton(
                onClick = { onEquipBadge(null) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("equip_cosmetic_${badge.id}"),
              ) {
                Text("Equipped", style = MaterialTheme.typography.labelSmall)
              }
            } else {
              Button(
                onClick = { onEquipBadge(badge.id) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("equip_cosmetic_${badge.id}"),
              ) {
                Text("Equip", style = MaterialTheme.typography.labelSmall)
              }
            }
          } else {
            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface) {
              Text(
                text = "Daily Drop",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              )
            }
          }
        }
      }
    }
  }
}

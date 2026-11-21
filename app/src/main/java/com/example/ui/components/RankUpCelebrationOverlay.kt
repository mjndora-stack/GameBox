package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import com.example.data.model.PlayerTier
import com.example.data.repository.RewardEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ConfettiParticle(
  val angleRad: Float,
  val speed: Float,
  val sizeDp: Float,
  val spinSpeed: Float,
  val color: Color,
  val isStar: Boolean,
)

private fun tierAccentColor(tier: PlayerTier): Color =
  when (tier) {
    PlayerTier.HOBBYIST -> Color(0xFF38BDF8)
    PlayerTier.ENTHUSIAST -> Color(0xFF4CE8C8)
    PlayerTier.EXPERT -> Color(0xFF818CF8)
    PlayerTier.PRO -> Color(0xFFF472B6)
    PlayerTier.MASTER -> Color(0xFFFF884D)
    PlayerTier.GRANDMASTER -> Color(0xFFFF5C7C)
    PlayerTier.VIRTUOSO -> Color(0xFFFFC857)
    PlayerTier.LEGEND -> Color(0xFFA78BFA)
    PlayerTier.MYTHIC -> Color(0xFFFDE047)
  }

@Composable
fun RankUpCelebrationOverlay(
  reward: RewardEvent,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val newTier = reward.promotedToTier ?: return
  val prevTier = reward.previousTier
  val prevColor = tierAccentColor(prevTier)
  val newColor = tierAccentColor(newTier)

  val cardScale = remember(reward) { Animatable(0.84f) }
  val crestScale = remember(reward) { Animatable(0.9f) }
  val barFill = remember(reward) { Animatable(PlayerTier.progressInTier(reward.previousTotalXp)) }
  val shockwaveProgress = remember(reward) { Animatable(0f) }
  val confettiProgress = remember(reward) { Animatable(0f) }

  var barDisplayTier by remember(reward) { mutableStateOf(prevTier) }
  var hasBurst by remember(reward) { mutableStateOf(false) }
  var displayedXp by remember(reward) { mutableIntStateOf(reward.previousTotalXp) }

  val infinite = rememberInfiniteTransition(label = "rank_up_rays")
  val rayRotation by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
      label = "ray_rot",
    )
  val glowPulse by
    infinite.animateFloat(
      initialValue = 0.72f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "glow_pulse",
    )

  val particles = remember(reward) {
    val palette = listOf(newColor, Color(0xFFFFC857), Color(0xFF4CE8C8), Color(0xFFFF5C7C), Color(0xFF38BDF8), Color.White)
    List(42) { idx ->
      val angle = ((idx.toFloat() / 42f) * 2f * PI.toFloat()) + ((idx % 5) * 0.14f)
      val spd = 0.55f + ((idx * 17) % 10) * 0.085f
      ConfettiParticle(
        angleRad = angle,
        speed = spd,
        sizeDp = 5f + (idx % 4) * 2.2f,
        spinSpeed = if (idx % 2 == 0) 540f else -480f,
        color = palette[idx % palette.size],
        isStar = idx % 3 == 0,
      )
    }
  }

  LaunchedEffect(reward) {
    val startProgress = PlayerTier.progressInTier(reward.previousTotalXp)
    val startXp = reward.previousTotalXp
    val thresholdXp = newTier.minXp.coerceAtMost(reward.newTotalXp)
    val finalProgress = PlayerTier.progressInTier(reward.newTotalXp)

    launch {
      cardScale.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
      )
    }

    // Stage 1: Animate the old rank's XP bar filling smoothly all the way up to 100%
    barFill.snapTo(startProgress)
    val xpCounterJob1 = launch {
      val steps = 28
      val duration = 900L
      for (i in 1..steps) {
        val t = i / steps.toFloat()
        val eased = 1f - (1f - t) * (1f - t)
        displayedXp = (startXp + (thresholdXp - startXp) * eased).roundToInt()
        delay(duration / steps)
      }
      displayedXp = thresholdXp
    }
    barFill.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 920, easing = FastOutSlowInEasing),
    )
    xpCounterJob1.join()

    // Stage 2: Climax! 100% Bar Filled -> Shockwave, Confetti, Crest Pop & Victory Chime
    hasBurst = true
    VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
    launch {
      crestScale.snapTo(0.72f)
      crestScale.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
      )
    }
    launch {
      shockwaveProgress.snapTo(0f)
      shockwaveProgress.animateTo(1f, tween(durationMillis = 850, easing = FastOutSlowInEasing))
    }
    launch {
      confettiProgress.snapTo(0f)
      confettiProgress.animateTo(1f, tween(durationMillis = 3200, easing = LinearEasing))
    }

    // Hold at full 100% glow briefly so user clearly sees the bar topped out
    delay(320L)

    // Stage 3: Transition progress bar to the new rank and fill overflow XP
    barDisplayTier = newTier
    if (newTier.nextTier() != null) {
      barFill.snapTo(0f)
      val xpCounterJob2 = launch {
        val steps = 20
        val duration = 760L
        for (i in 1..steps) {
          val t = i / steps.toFloat()
          val eased = 1f - (1f - t) * (1f - t)
          displayedXp = (thresholdXp + (reward.newTotalXp - thresholdXp) * eased).roundToInt()
          delay(duration / steps)
        }
        displayedXp = reward.newTotalXp
      }
      barFill.animateTo(
        targetValue = finalProgress,
        animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing),
      )
      xpCounterJob2.join()
    } else {
      displayedXp = reward.newTotalXp
      barFill.snapTo(1f)
    }
  }

  val activeBarColor = if (!hasBurst) prevColor else newColor
  val currentNextTier = barDisplayTier.nextTier()
  val xpPercent = (barFill.value.coerceIn(0f, 1f) * 100f).roundToInt()
  val isSuperRank = newTier.levelIndex >= PlayerTier.VIRTUOSO.levelIndex

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xC9060912))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss,
        )
        .testTag("rank_up_celebration_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    // Full-screen celebratory confetti & shockwave Canvas layer
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height * 0.43f)
      val maxRadius = size.minDimension * 0.62f

      if (hasBurst && shockwaveProgress.value < 1f) {
        val sw = shockwaveProgress.value
        val swAlpha = (1f - sw).coerceIn(0f, 1f)
        drawCircle(
          color = newColor.copy(alpha = swAlpha * 0.72f),
          radius = 48.dp.toPx() + sw * maxRadius * 0.88f,
          center = center,
          style = Stroke(width = (10f * (1f - sw)).coerceAtLeast(2f)),
        )
        drawCircle(
          color = Color.White.copy(alpha = swAlpha * 0.45f),
          radius = 30.dp.toPx() + sw * maxRadius * 0.64f,
          center = center,
          style = Stroke(width = (5f * (1f - sw)).coerceAtLeast(1.5f)),
        )
      }

      if (hasBurst && confettiProgress.value > 0f) {
        val t = confettiProgress.value
        particles.forEach { p ->
          val radial = (t * 1.15f).coerceAtMost(1f) * maxRadius * p.speed
          val gravityDrop = (t * t) * size.height * 0.34f
          val px = center.x + cos(p.angleRad) * radial
          val py = center.y + sin(p.angleRad) * radial * 0.78f + gravityDrop
          val fade = if (t < 0.72f) 1f else ((1f - t) / 0.28f).coerceIn(0f, 1f)
          val sz = p.sizeDp.dp.toPx()

          rotate(degrees = t * p.spinSpeed, pivot = Offset(px, py)) {
            if (p.isStar) {
              val diamond = Path().apply {
                moveTo(px, py - sz)
                lineTo(px + sz * 0.65f, py)
                lineTo(px, py + sz)
                lineTo(px - sz * 0.65f, py)
                close()
              }
              drawPath(diamond, p.color.copy(alpha = fade * 0.95f))
            } else {
              drawRoundRect(
                color = p.color.copy(alpha = fade * 0.92f),
                topLeft = Offset(px - sz * 0.7f, py - sz * 0.38f),
                size = Size(sz * 1.4f, sz * 0.76f),
                cornerRadius = CornerRadius(3f, 3f),
              )
            }
          }
        }
      }
    }

    // Main Celebration Card
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = Color(0xFF0E1422),
      border = BorderStroke(1.5.dp, activeBarColor.copy(alpha = 0.7f)),
      shadowElevation = 20.dp,
      modifier =
        Modifier
          .padding(horizontal = 20.dp)
          .widthIn(max = 430.dp)
          .graphicsLayer {
            scaleX = cardScale.value
            scaleY = cardScale.value
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Top Header Chip
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = activeBarColor.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, activeBarColor.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = activeBarColor,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text =
                when {
                  !hasBurst -> "XP MILESTONE REACHED"
                  isSuperRank -> "SUPER RANK PROMOTION!"
                  else -> "NEW RANK UNLOCKED!"
                },
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = activeBarColor,
            )
          }
        }

        // Animated Rank Crest & Sunbeams
        Box(
          modifier =
            Modifier
              .size(116.dp)
              .graphicsLayer {
                scaleX = crestScale.value
                scaleY = crestScale.value
              },
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.44f

            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(activeBarColor.copy(alpha = 0.42f * glowPulse), Color.Transparent),
                  center = c,
                  radius = radius * 1.35f,
                ),
              radius = radius * 1.35f,
              center = c,
            )

            // Rotating Sunbeams
            rotate(degrees = rayRotation, pivot = c) {
              val rayCount = 12
              for (i in 0 until rayCount) {
                val theta = (i * 2f * PI.toFloat()) / rayCount
                val startR = radius * 0.72f
                val endR = radius * (if (i % 2 == 0) 1.24f else 1.04f)
                drawLine(
                  color = activeBarColor.copy(alpha = if (hasBurst) 0.52f else 0.25f),
                  start = Offset(c.x + cos(theta) * startR, c.y + sin(theta) * startR),
                  end = Offset(c.x + cos(theta) * endR, c.y + sin(theta) * endR),
                  strokeWidth = if (i % 2 == 0) 3.5f else 2f,
                  cap = StrokeCap.Round,
                )
              }
            }

            // Sculpted Hexagonal Shield Crest
            val shield = Path().apply {
              for (i in 0 until 6) {
                val a = (i * 60f - 90f) * (PI.toFloat() / 180f)
                val px = c.x + cos(a) * radius * 0.86f
                val py = c.y + sin(a) * radius * 0.86f
                if (i == 0) moveTo(px, py) else lineTo(px, py)
              }
              close()
            }
            drawPath(
              path = shield,
              brush = Brush.verticalGradient(
                listOf(activeBarColor.copy(alpha = 0.92f), Color(0xFF151C2F)),
              ),
            )
            drawPath(
              path = shield,
              color = Color.White.copy(alpha = 0.78f),
              style = Stroke(width = 3.5f),
            )

            // Inner Crown / Star Emblem
            val crown = Path().apply {
              val cw = radius * 0.56f
              val ch = radius * 0.40f
              moveTo(c.x - cw * 0.72f, c.y + ch * 0.55f)
              lineTo(c.x - cw * 0.82f, c.y - ch * 0.42f)
              lineTo(c.x - cw * 0.32f, c.y + ch * 0.05f)
              lineTo(c.x, c.y - ch * 0.68f)
              lineTo(c.x + cw * 0.32f, c.y + ch * 0.05f)
              lineTo(c.x + cw * 0.82f, c.y - ch * 0.42f)
              lineTo(c.x + cw * 0.72f, c.y + ch * 0.55f)
              close()
            }
            drawPath(crown, Color.White)
          }
        }

        // Rank Transition Title
        AnimatedContent(
          targetState = hasBurst,
          transitionSpec = {
            (fadeIn(tween(260)) + scaleIn(initialScale = 0.86f)) togetherWith fadeOut(tween(150))
          },
          label = "rank_title_morph",
        ) { burst ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (!burst) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = prevTier.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color(0xFFCBD5E1),
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                  contentDescription = null,
                  tint = newColor,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = newTier.displayName,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = newColor,
                )
              }
              Text(
                text = "Filling Rank Progress...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
              )
            } else {
              Text(
                text = "${newTier.displayName.uppercase()} RANK!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
              )
              Text(
                text = newTier.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = newColor,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        // Animated Progress Bar Section
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF172034),
          border = BorderStroke(1.dp, activeBarColor.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (barDisplayTier == prevTier) "${prevTier.displayName} → ${newTier.displayName}"
                  else "${barDisplayTier.displayName} Progress",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE2E8F0),
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = activeBarColor.copy(alpha = 0.18f),
              ) {
                Text(
                  text = "+${reward.xpGained} XP",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = activeBarColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }

            // Custom Glowing Animated Fill Bar
            Canvas(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(22.dp)
                  .testTag("rank_up_progress_bar"),
            ) {
              val w = size.width
              val h = size.height
              val fraction = barFill.value.coerceIn(0f, 1f)
              val fillW = (w * fraction).coerceAtLeast(if (fraction > 0f) h else 0f).coerceAtMost(w)
              val r = CornerRadius(h / 2f, h / 2f)

              // Track
              drawRoundRect(
                color = Color(0xFF0A0F1A),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = r,
              )

              // Segment tick guides inside track
              for (seg in 1..4) {
                val sx = w * (seg / 5f)
                drawLine(
                  color = Color.White.copy(alpha = 0.08f),
                  start = Offset(sx, 3f),
                  end = Offset(sx, h - 3f),
                  strokeWidth = 2f,
                )
              }

              // Animated gradient fill
              if (fillW > 0f) {
                drawRoundRect(
                  brush = Brush.horizontalGradient(
                    colors = listOf(activeBarColor.copy(alpha = 0.75f), activeBarColor, Color.White.copy(alpha = 0.92f)),
                    startX = 0f,
                    endX = fillW.coerceAtLeast(1f),
                  ),
                  topLeft = Offset.Zero,
                  size = Size(fillW, h),
                  cornerRadius = r,
                )

                // Top specular sheen
                drawRoundRect(
                  color = Color.White.copy(alpha = 0.28f),
                  topLeft = Offset(3f, 3f),
                  size = Size((fillW - 6f).coerceAtLeast(0f), h * 0.34f),
                  cornerRadius = CornerRadius(h / 4f, h / 4f),
                )

                // Glowing leading orb spark
                if (w > h) {
                  val tipX = (fillW - h * 0.4f).coerceIn(h * 0.4f, w - h * 0.4f)
                  drawCircle(
                    color = Color.White.copy(alpha = 0.85f * glowPulse),
                    radius = h * 0.26f,
                    center = Offset(tipX, h / 2f),
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "$displayedXp XP · $xpPercent%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
              Text(
                text =
                  if (currentNextTier != null) "${(currentNextTier.minXp - displayedXp).coerceAtLeast(0)} XP to ${currentNextTier.displayName}"
                  else "MAX RANK · MYTHIC",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
              )
            }
          }
        }

        if (reward.unlockedAchievementTitle != null || reward.completedMissionTitle != null) {
          val note =
            when {
              reward.completedMissionTitle != null && reward.unlockedCosmeticBadgeTitle != null ->
                "Daily Mission: ${reward.completedMissionTitle} · Unlocked ${reward.unlockedCosmeticBadgeTitle}"
              reward.completedMissionTitle != null ->
                "Daily Mission Cleared: ${reward.completedMissionTitle}"
              else ->
                "Bonus Badge: ${reward.unlockedAchievementTitle}"
            }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1F293D),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFC857),
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = note,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFDE68A),
              )
            }
          }
        }

        Spacer(Modifier.height(2.dp))

        Button(
          onClick = onDismiss,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = activeBarColor,
              contentColor = Color(0xFF060912),
            ),
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
              .testTag("rank_up_continue_btn"),
        ) {
          Text(
            text = "AWESOME · CONTINUE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }
  }
}

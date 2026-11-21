package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CosmeticBadgeEntity
import com.example.data.model.PlayerTier

@Composable
fun GameCoverArtwork(
  coverKey: String,
  title: String,
  isLocked: Boolean,
  requiredTierLabel: String = "RANK",
  isFanFavourite: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    when (coverKey) {
      "neon_hop" -> {
        Image(
          painter = painterResource(id = R.drawable.img_cover_neon_hop),
          contentDescription = "$title artwork",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      "skyline_dash" -> {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF1A102B), Color(0xFF471F48), Color(0xFFFF884D))), size = size)
          drawCircle(Color(0xFFFFD166).copy(alpha = 0.85f), h * 0.24f, Offset(w * 0.72f, h * 0.36f))
          val heights = listOf(0.42f, 0.60f, 0.36f, 0.72f, 0.50f, 0.64f, 0.45f)
          val bw = w / heights.size
          heights.forEachIndexed { idx, frac ->
            val bh = h * frac
            drawRect(Color(0xFF0E0A17), Offset(idx * bw, h - bh), Size(bw + 2f, bh))
            drawRect(Color(0xFFFFB347), Offset(idx * bw, h - bh), Size(bw + 2f, 4f))
          }
        }
      }
      else -> {
        // Distinct minimalist geometric artwork tailored to each of the 12 puzzles
        PuzzleCoverCanvas(coverKey = coverKey, modifier = Modifier.fillMaxSize())
      }
    }

    if (isFanFavourite) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xEE18121E),
        border = BorderStroke(1.dp, Color(0xFFFF5C7C)),
        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Rounded.Favorite, contentDescription = "Fan Favourite", tint = Color(0xFFFF5C7C), modifier = Modifier.size(13.dp))
          Text("FAN FAVOURITE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB3C1))
        }
      }
    }

    if (isLocked) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color(0xBF080A0F)),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xEE141923),
          border = BorderStroke(1.dp, Color(0xFFFFC857)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = "Rank locked", tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
            Text("${requiredTierLabel.uppercase()} RANK", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857))
          }
        }
      }
    }
  }
}

@Composable
private fun PuzzleCoverCanvas(coverKey: String, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    when (coverKey) {
      "prism_2048" -> {
        drawRect(Brush.linearGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B3A4B))))
        val colors = listOf(Color(0xFF4CE8C8), Color(0xFF38BDF8), Color(0xFFFFC857), Color(0xFF6366F1))
        for (i in 0..3) {
          drawRoundRect(
            colors[i],
            topLeft = Offset(w * (0.22f + (i % 2) * 0.28f), h * (0.18f + (i / 2) * 0.34f)),
            size = Size(w * 0.22f, h * 0.28f),
            cornerRadius = CornerRadius(14f, 14f),
          )
        }
      }
      "flux_circuit" -> {
        drawRect(Color(0xFF0C141D))
        val c = Color(0xFF4CE8C8)
        drawLine(c, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.5f, h * 0.5f), 10f, StrokeCap.Round)
        drawLine(c, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.25f), 10f, StrokeCap.Round)
        drawLine(c, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.85f, h * 0.75f), 10f, StrokeCap.Round)
        drawCircle(Color(0xFFFFC857), 16f, Offset(w * 0.15f, h * 0.5f))
        drawCircle(Color(0xFF38BDF8), 16f, Offset(w * 0.85f, h * 0.75f))
      }
      "vault_sweeper" -> {
        drawRect(Color(0xFF121822))
        for (r in 0..2) for (c in 0..4) {
          val col = if (r == 1 && c == 2) Color(0xFFFF5C7C) else if ((r + c) % 2 == 0) Color(0xFF222D3D) else Color(0xFF2D3B4F)
          drawRoundRect(col, Offset(w * 0.14f + c * w * 0.15f, h * 0.2f + r * h * 0.22f), Size(w * 0.12f, h * 0.18f), CornerRadius(8f))
        }
      }
      "lumen_toggle" -> {
        drawRect(Color(0xFF11131B))
        for (r in 0..2) for (c in 0..2) {
          val lit = (r == 1 || c == 1)
          drawRoundRect(
            if (lit) Color(0xFF4CE8C8) else Color(0xFF222836),
            Offset(w * 0.27f + c * w * 0.16f, h * 0.16f + r * h * 0.24f),
            Size(w * 0.13f, h * 0.2f),
            CornerRadius(10f),
          )
        }
      }
      "chroma_flood" -> {
        val cols = listOf(Color(0xFF4CE8C8), Color(0xFFFF5C7C), Color(0xFFFFC857), Color(0xFF38BDF8), Color(0xFFA78BFA))
        val cw = w / 5f
        cols.forEachIndexed { idx, col ->
          drawRect(col, Offset(idx * cw, 0f), Size(cw + 2f, h))
        }
      }
      "nonogrid" -> {
        drawRect(Color(0xFF13161E))
        val pat = listOf(0, 1, 0, 1, 1, 1, 0, 1, 0)
        for (i in pat.indices) {
          val r = i / 3; val c = i % 3
          drawRoundRect(
            if (pat[i] == 1) Color(0xFFE2E8F0) else Color(0xFF262C3B),
            Offset(w * 0.28f + c * w * 0.15f, h * 0.16f + r * h * 0.24f),
            Size(w * 0.12f, h * 0.20f),
            CornerRadius(6f),
          )
        }
      }
      "slide_15" -> {
        drawRect(Color(0xFF141B29))
        for (i in 0..3) {
          if (i == 3) continue
          drawRoundRect(
            if (i == 0) Color(0xFF38BDF8) else Color(0xFF273549),
            Offset(w * 0.24f + (i % 2) * w * 0.26f, h * 0.18f + (i / 2) * h * 0.34f),
            Size(w * 0.23f, h * 0.30f),
            CornerRadius(12f),
          )
        }
      }
      "hanoi_vault" -> {
        drawRect(Color(0xFF12151E))
        listOf(0.7f, 0.5f, 0.3f).forEachIndexed { idx, frac ->
          drawRoundRect(
            Color(0xFF4CE8C8).copy(alpha = 0.45f + idx * 0.2f),
            Offset(w * (0.5f - frac * 0.35f), h * (0.68f - idx * 0.18f)),
            Size(w * frac * 0.7f, h * 0.14f),
            CornerRadius(20f),
          )
        }
      }
      "cipher_break" -> {
        drawRect(Color(0xFF191324))
        listOf(Color(0xFF4CE8C8), Color(0xFFFF5C7C), Color(0xFFFFC857), Color(0xFF38BDF8)).forEachIndexed { i, col ->
          drawCircle(col, h * 0.13f, Offset(w * (0.22f + i * 0.19f), h * 0.5f))
        }
      }
      "sudoku_six" -> {
        drawRect(Color(0xFF0E1726))
        for (i in 1..2) {
          drawLine(Color(0xFF60A5FA), Offset(w * (i / 3f), h * 0.12f), Offset(w * (i / 3f), h * 0.88f), 4f)
          drawLine(Color(0xFF60A5FA), Offset(w * 0.12f, h * (i / 3f)), Offset(w * 0.88f, h * (i / 3f)), 4f)
        }
      }
      "crate_sokoban" -> {
        drawRect(Color(0xFF1A1612))
        drawRoundRect(Color(0xFFD97706), Offset(w * 0.26f, h * 0.26f), Size(w * 0.22f, h * 0.48f), CornerRadius(10f))
        drawRoundRect(Color(0xFF22C55E), Offset(w * 0.54f, h * 0.26f), Size(w * 0.22f, h * 0.48f), CornerRadius(10f))
      }
      "vault_chess" -> {
        drawRect(Color(0xFF0E131B))
        for (r in 0..3) for (c in 0..5) {
          val light = (r + c) % 2 == 0
          drawRect(
            if (light) Color(0xFFCAD4E0).copy(alpha = 0.22f) else Color(0xFF1D2736),
            Offset(w * 0.14f + c * w * 0.12f, h * 0.14f + r * h * 0.18f),
            Size(w * 0.12f, h * 0.18f),
          )
        }
        drawCircle(Color(0xFF4CE8C8), h * 0.14f, Offset(w * 0.38f, h * 0.48f))
        drawCircle(Color(0xFFFFC857), h * 0.14f, Offset(w * 0.62f, h * 0.48f))
      }
      "street_fighter_2d" -> {
        drawRect(Brush.horizontalGradient(listOf(Color(0xFF280D1B), Color(0xFF0E131E), Color(0xFF0D2136))))
        drawRoundRect(Color(0xFFFF5C7C), Offset(w * 0.20f, h * 0.28f), Size(w * 0.16f, h * 0.46f), CornerRadius(12f))
        drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.64f, h * 0.28f), Size(w * 0.16f, h * 0.46f), CornerRadius(12f))
        drawCircle(Color(0xFFFFC857), h * 0.12f, Offset(w * 0.50f, h * 0.50f))
      }
      "flappy_bird" -> {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2A47), Color(0xFF38BDF8))))
        drawRect(Color(0xFF16A34A), Offset(w * 0.64f, 0f), Size(w * 0.12f, h * 0.34f))
        drawRect(Color(0xFF16A34A), Offset(w * 0.64f, h * 0.62f), Size(w * 0.12f, h * 0.38f))
        drawCircle(Color(0xFFFACC15), h * 0.14f, Offset(w * 0.34f, h * 0.48f))
      }
      else -> { // queens_grid
        drawRect(Color(0xFF141118))
        for (r in 0..3) for (c in 0..3) {
          val dark = (r + c) % 2 == 0
          drawRect(
            if (dark) Color(0xFF282032) else Color(0xFF423254),
            Offset(w * 0.24f + c * w * 0.13f, h * 0.14f + r * h * 0.18f),
            Size(w * 0.13f, h * 0.18f),
          )
        }
        drawCircle(Color(0xFFFFC857), 16f, Offset(w * 0.43f, h * 0.41f))
      }
    }
  }
}

@Composable
fun TierProgressHeader(
  playerName: String,
  xp: Int,
  credits: Int,
  tier: PlayerTier,
  progress: Float,
  modifier: Modifier = Modifier,
  equippedBadge: CosmeticBadgeEntity? = null,
) {
  val nextTier = tier.nextTier()
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (equippedBadge != null) {
            CosmeticBadgeEmblem(
              emblemKey = equippedBadge.emblemKey,
              accentColor = Color(equippedBadge.accentHex),
              unlocked = true,
              size = 38.dp,
            )
          } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
              Icon(
                Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(6.dp).size(20.dp),
              )
            }
          }
          Column {
            Text("$playerName · ${tier.displayName}", style = MaterialTheme.typography.titleMedium)
            Text(
              text =
                if (equippedBadge != null) "${equippedBadge.title} · ${tier.subtitle}"
                else tier.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = if (equippedBadge != null) Color(equippedBadge.accentHex) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
          Text(
            text = "$credits CR",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      val animatedProgress by
        animateFloatAsState(
          targetValue = progress.coerceIn(0f, 1f),
          animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
          label = "header_tier_progress",
        )

      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$xp XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
          text =
            if (nextTier != null) "${nextTier.minXp - xp} XP to ${nextTier.displayName}"
            else "MAX RANK · MYTHIC",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

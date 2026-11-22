package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    MinimalistGameCover(coverKey = coverKey, modifier = Modifier.fillMaxSize())

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

/**
 * Clean, balanced minimalist covers with subtle two-tone backdrops,
 * bevels, and secondary cues so each of the 18 games feels crafted and unmistakable.
 */
@Composable
private fun MinimalistGameCover(coverKey: String, modifier: Modifier = Modifier) {
  when (coverKey) {
    "neon_hop" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val teal = Color(0xFF4CE8C8)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0C121D), Color(0xFF142132))))
        // Subtle horizon grid line + 3 stepped neon ledges with top highlights
        drawLine(Color(0x184CE8C8), Offset(0f, h * 0.85f), Offset(w, h * 0.85f), 2f)
        drawRoundRect(Color(0xFF1E293B), Offset(w * 0.14f, h * 0.72f), Size(w * 0.18f, h * 0.08f), CornerRadius(6f))
        drawLine(Color(0xFF475569), Offset(w * 0.15f, h * 0.72f), Offset(w * 0.31f, h * 0.72f), 2.5f)
        drawRoundRect(Color(0xFF28384F), Offset(w * 0.36f, h * 0.56f), Size(w * 0.18f, h * 0.08f), CornerRadius(6f))
        drawLine(Color(0xFF60A5FA), Offset(w * 0.37f, h * 0.56f), Offset(w * 0.53f, h * 0.56f), 2.5f)
        drawRoundRect(Color(0xFF164E4A), Offset(w * 0.58f, h * 0.42f), Size(w * 0.24f, h * 0.08f), CornerRadius(6f))
        drawLine(teal, Offset(w * 0.59f, h * 0.42f), Offset(w * 0.81f, h * 0.42f), 3f)
        // Collectible orb floating above middle step
        drawCircle(Color(0x33FACC15), h * 0.065f, Offset(w * 0.23f, h * 0.58f))
        drawCircle(Color(0xFFFACC15), h * 0.032f, Offset(w * 0.23f, h * 0.58f))
        // Hopper character with visor + hop trail arc
        val hopArc = Path().apply {
          moveTo(w * 0.26f, h * 0.68f)
          quadraticTo(w * 0.35f, h * 0.38f, w * 0.43f, h * 0.46f)
        }
        drawPath(hopArc, Color(0x444CE8C8), style = Stroke(3f, cap = StrokeCap.Round))
        drawCircle(teal, h * 0.06f, Offset(w * 0.45f, h * 0.34f))
        drawRoundRect(Color(0xFFE2E8F0), Offset(w * 0.425f, h * 0.40f), Size(w * 0.05f, h * 0.11f), CornerRadius(8f))
        drawRoundRect(Color(0xFF0F172A), Offset(w * 0.455f, h * 0.32f), Size(w * 0.025f, h * 0.03f), CornerRadius(3f))
        // Glowing portal gate on top step
        drawRoundRect(Color(0x2838BDF8), Offset(w * 0.695f, h * 0.17f), Size(w * 0.08f, h * 0.25f), CornerRadius(14f))
        drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.70f, h * 0.18f), Size(w * 0.07f, h * 0.24f), CornerRadius(14f), style = Stroke(3.5f))
      }
    }
    "skyline_dash" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val amber = Color(0xFFFFB347)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF141022), Color(0xFF281A38))))
        // Subtle twilight sun disc + flat rooftops with subtle lit window dots
        drawCircle(Color(0x38FFB347), h * 0.20f, Offset(w * 0.78f, h * 0.28f))
        drawRect(Color(0xFF1C172B), Offset(w * 0.08f, h * 0.58f), Size(w * 0.22f, h * 0.42f))
        drawRect(Color(0xFF26203B), Offset(w * 0.34f, h * 0.46f), Size(w * 0.26f, h * 0.54f))
        drawRect(Color(0xFF1C172B), Offset(w * 0.64f, h * 0.60f), Size(w * 0.26f, h * 0.40f))
        drawLine(amber, Offset(w * 0.34f, h * 0.46f), Offset(w * 0.60f, h * 0.46f), 4f)
        drawLine(Color(0xFF60A5FA), Offset(w * 0.08f, h * 0.58f), Offset(w * 0.30f, h * 0.58f), 3f)
        // Minimal building window accents
        for (i in 0..1) {
          drawRoundRect(Color(0x55FDE047), Offset(w * (0.40f + i * 0.08f), h * 0.58f), Size(w * 0.035f, h * 0.06f), CornerRadius(2f))
          drawRoundRect(Color(0x33FDE047), Offset(w * (0.14f + i * 0.07f), h * 0.68f), Size(w * 0.03f, h * 0.055f), CornerRadius(2f))
        }
        // Sprinter + double dash chevron + speed trail
        drawLine(Color(0x55FFB347), Offset(w * 0.20f, h * 0.30f), Offset(w * 0.36f, h * 0.30f), 3.5f, StrokeCap.Round)
        val chev = Path().apply {
          moveTo(w * 0.31f, h * 0.24f); lineTo(w * 0.39f, h * 0.30f); lineTo(w * 0.31f, h * 0.36f)
          moveTo(w * 0.38f, h * 0.24f); lineTo(w * 0.46f, h * 0.30f); lineTo(w * 0.38f, h * 0.36f)
        }
        drawPath(chev, amber, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFFFDE047), h * 0.06f, Offset(w * 0.54f, h * 0.28f))
        drawRoundRect(Color(0xFFFDE047), Offset(w * 0.515f, h * 0.34f), Size(w * 0.045f, h * 0.085f), CornerRadius(6f))
      }
    }
    "street_fighter_2d" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val p1 = Color(0xFFFF5C7C)
        val p2 = Color(0xFF38BDF8)
        drawRect(Brush.horizontalGradient(listOf(Color(0xFF1E111B), Color(0xFF15141E), Color(0xFF101D2C))))
        // Subtle twin health bar strip at top + dojo mat floor
        drawRoundRect(p1.copy(alpha = 0.7f), Offset(w * 0.14f, h * 0.14f), Size(w * 0.28f, h * 0.045f), CornerRadius(4f))
        drawRoundRect(p2.copy(alpha = 0.7f), Offset(w * 0.58f, h * 0.14f), Size(w * 0.28f, h * 0.045f), CornerRadius(4f))
        drawLine(Color(0xFF3A3247), Offset(w * 0.14f, h * 0.80f), Offset(w * 0.86f, h * 0.80f), 4f, StrokeCap.Round)
        // Left Fighter (crimson headband + tail + belt)
        drawCircle(p1, h * 0.08f, Offset(w * 0.33f, h * 0.36f))
        drawLine(Color(0xFFFFD166), Offset(w * 0.28f, h * 0.33f), Offset(w * 0.23f, h * 0.38f), 4f, StrokeCap.Round)
        drawRoundRect(p1, Offset(w * 0.285f, h * 0.45f), Size(w * 0.09f, h * 0.32f), CornerRadius(10f))
        drawRect(Color(0xFF18181B), Offset(w * 0.285f, h * 0.61f), Size(w * 0.09f, h * 0.035f))
        drawLine(p1, Offset(w * 0.37f, h * 0.50f), Offset(w * 0.46f, h * 0.46f), 6f, StrokeCap.Round)
        // Right Fighter (cyan stance + belt)
        drawCircle(p2, h * 0.08f, Offset(w * 0.67f, h * 0.36f))
        drawRoundRect(p2, Offset(w * 0.625f, h * 0.45f), Size(w * 0.09f, h * 0.32f), CornerRadius(10f))
        drawRect(Color(0xFF0F172A), Offset(w * 0.625f, h * 0.61f), Size(w * 0.09f, h * 0.035f))
        drawLine(p2, Offset(w * 0.63f, h * 0.50f), Offset(w * 0.54f, h * 0.46f), 6f, StrokeCap.Round)
        // Minimal clash spark
        val cx = w * 0.5f; val cy = h * 0.46f; val r = h * 0.085f
        val spark = Path().apply {
          moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
        }
        drawCircle(Color(0x33FFC857), r * 1.4f, Offset(cx, cy))
        drawPath(spark, Color(0xFFFFC857))
      }
    }
    "flappy_bird" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val pipe = Color(0xFF22C55E)
        val pipeLip = Color(0xFF16A34A)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2338), Color(0xFF163754))))
        // Subtle cloud pill + grassy ground band
        drawRoundRect(Color(0x28E2E8F0), Offset(w * 0.14f, h * 0.20f), Size(w * 0.18f, h * 0.09f), CornerRadius(20f))
        drawRect(Color(0xFF15803D), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
        drawLine(Color(0xFF4ADE80), Offset(0f, h * 0.88f), Offset(w, h * 0.88f), 3f)
        // Pipes with vertical highlight stripe
        drawRect(pipe, Offset(w * 0.62f, 0f), Size(w * 0.11f, h * 0.28f))
        drawRect(Color(0xFF4ADE80), Offset(w * 0.635f, 0f), Size(w * 0.02f, h * 0.24f))
        drawRoundRect(pipeLip, Offset(w * 0.60f, h * 0.23f), Size(w * 0.15f, h * 0.08f), CornerRadius(4f))
        drawRect(pipe, Offset(w * 0.62f, h * 0.63f), Size(w * 0.11f, h * 0.25f))
        drawRect(Color(0xFF4ADE80), Offset(w * 0.635f, h * 0.69f), Size(w * 0.02f, h * 0.19f))
        drawRoundRect(pipeLip, Offset(w * 0.60f, h * 0.61f), Size(w * 0.15f, h * 0.08f), CornerRadius(4f))
        // Crisp minimalist yellow bird
        val bx = w * 0.36f; val by = h * 0.46f; val br = h * 0.105f
        drawCircle(Color(0xFFFACC15), br, Offset(bx, by))
        drawRoundRect(Color(0xFFFEF9C3), Offset(bx - br * 0.7f, by - br * 0.12f), Size(br * 0.85f, br * 0.55f), CornerRadius(12f))
        drawCircle(Color.White, br * 0.26f, Offset(bx + br * 0.42f, by - br * 0.24f))
        drawCircle(Color(0xFF0F172A), br * 0.14f, Offset(bx + br * 0.48f, by - br * 0.24f))
        val beak = Path().apply {
          moveTo(bx + br * 0.85f, by - br * 0.10f)
          lineTo(bx + br * 1.48f, by + br * 0.10f)
          lineTo(bx + br * 0.85f, by + br * 0.32f)
          close()
        }
        drawPath(beak, Color(0xFFF97316))
      }
    }
    "vault_chess" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E141E), Color(0xFF16202F))))
        val boardSize = h * 0.74f
        val bx = w * 0.5f - boardSize * 0.5f
        val by = h * 0.13f
        val sq = boardSize / 4f
        for (r in 0..3) for (c in 0..3) {
          drawRect(
            if ((r + c) % 2 == 0) Color(0xFF2D394D) else Color(0xFF18212E),
            Offset(bx + c * sq, by + r * sq),
            Size(sq, sq),
          )
        }
        drawRoundRect(Color(0xFFFFC857).copy(alpha = 0.55f), Offset(bx - 3f, by - 3f), Size(boardSize + 6f, boardSize + 6f), CornerRadius(6f), style = Stroke(2.5f))
        // Highlight target move square on right
        drawCircle(Color(0x774CE8C8), sq * 0.16f, Offset(bx + sq * 3.5f, by + sq * 1.5f))
        // Clean centered ivory Knight silhouette with pedestal base
        val cx = w * 0.5f; val cy = h * 0.51f; val k = h * 0.25f
        val knight = Path().apply {
          moveTo(cx - k * 0.46f, cy + k * 0.76f)
          lineTo(cx + k * 0.46f, cy + k * 0.76f)
          lineTo(cx + k * 0.36f, cy + k * 0.46f)
          lineTo(cx + k * 0.28f, cy - k * 0.25f)
          lineTo(cx + k * 0.05f, cy - k * 0.72f)
          lineTo(cx - k * 0.12f, cy - k * 0.55f)
          lineTo(cx - k * 0.48f, cy - k * 0.20f)
          lineTo(cx - k * 0.35f, cy + k * 0.02f)
          lineTo(cx - k * 0.05f, cy - k * 0.08f)
          lineTo(cx - k * 0.22f, cy + k * 0.46f)
          close()
        }
        drawPath(knight, Color(0xFFF8FAFC))
        drawPath(knight, Color(0xFF94A3B8), style = Stroke(1.8f, join = StrokeJoin.Round))
        drawCircle(Color(0xFF1E293B), k * 0.06f, Offset(cx - k * 0.14f, cy - k * 0.32f))
      }
    }
    "prism_2048" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0E1524), Color(0xFF162035)))),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0B111C), RoundedCornerShape(12.dp))
              .border(1.5.dp, Color(0xFF28374F), RoundedCornerShape(12.dp))
              .padding(7.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MinimalTile("2", Color(0xFF1E293B), Color(0xFFE2E8F0))
            MinimalTile("4", Color(0xFF1D4ED8), Color.White)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MinimalTile("8", Color(0xFF0D9488), Color.White)
            MinimalTile("2048", Color(0xFFEAB308), Color(0xFF111827), highlightBorder = Color(0xFFFEF08A))
          }
        }
      }
    }
    "flux_circuit" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.linearGradient(listOf(Color(0xFF0C131E), Color(0xFF122130))))
        // Subtle secondary trace
        val branch = Path().apply {
          moveTo(w * 0.50f, h * 0.46f)
          lineTo(w * 0.66f, h * 0.46f)
          lineTo(w * 0.66f, h * 0.28f)
        }
        drawPath(branch, Color(0xFF244254), style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Primary energized amber->teal trace
        val wire = Path().apply {
          moveTo(w * 0.22f, h * 0.34f)
          lineTo(w * 0.50f, h * 0.34f)
          lineTo(w * 0.50f, h * 0.68f)
          lineTo(w * 0.78f, h * 0.68f)
        }
        drawPath(wire, Color(0xFF4CE8C8), style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFF1E293B), h * 0.11f, Offset(w * 0.50f, h * 0.50f))
        drawCircle(Color(0xFF4CE8C8), h * 0.11f, Offset(w * 0.50f, h * 0.50f), style = Stroke(3f))
        drawCircle(Color(0x33FFC857), h * 0.15f, Offset(w * 0.22f, h * 0.34f))
        drawCircle(Color(0xFFFFC857), h * 0.095f, Offset(w * 0.22f, h * 0.34f))
        drawCircle(Color(0x3338BDF8), h * 0.15f, Offset(w * 0.78f, h * 0.68f))
        drawCircle(Color(0xFF38BDF8), h * 0.095f, Offset(w * 0.78f, h * 0.68f))
      }
    }
    "vault_sweeper" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF101620), Color(0xFF172130)))),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0D131D), RoundedCornerShape(10.dp))
              .border(1.5.dp, Color(0xFF2B394F), RoundedCornerShape(10.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SweeperMiniCell("1", Color(0xFF38BDF8), revealed = true)
            SweeperMiniCell("2", Color(0xFF4ADE80), revealed = true)
            SweeperMiniCell("1", Color(0xFF38BDF8), revealed = true)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SweeperMiniCell("1", Color(0xFF38BDF8), revealed = true)
            Box(
              modifier =
                Modifier
                  .size(28.dp)
                  .background(Color(0xFF283548), RoundedCornerShape(5.dp))
                  .border(1.dp, Color(0xFFFF5C7C).copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFFFF5C7C), modifier = Modifier.size(17.dp))
            }
            SweeperMiniCell("", Color.Transparent, revealed = false)
          }
        }
      }
    }
    "lumen_toggle" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0E111B), Color(0xFF141B29)))),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0A0E17), RoundedCornerShape(10.dp))
              .border(1.dp, Color(0xFF263248), RoundedCornerShape(10.dp))
              .padding(7.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          for (r in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              for (c in 0..2) {
                val lit = (r == 1 || c == 1)
                Box(
                  modifier =
                    Modifier
                      .size(22.dp)
                      .background(if (lit) Color(0xFF4CE8C8) else Color(0xFF202737), RoundedCornerShape(6.dp))
                      .then(
                        if (lit) Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        else Modifier,
                      ),
                )
              }
            }
          }
        }
      }
    }
    "chroma_flood" -> {
      val grid =
        listOf(
          listOf(Color(0xFF4CE8C8), Color(0xFF4CE8C8), Color(0xFF38BDF8), Color(0xFFA78BFA)),
          listOf(Color(0xFF4CE8C8), Color(0xFFFFC857), Color(0xFFFFC857), Color(0xFFFF5C7C)),
          listOf(Color(0xFF38BDF8), Color(0xFFFF5C7C), Color(0xFF4CE8C8), Color(0xFF38BDF8)),
        )
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0F121C), Color(0xFF171D2B)))),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0A0D14), RoundedCornerShape(10.dp))
              .border(1.5.dp, Color(0xFF2C364D), RoundedCornerShape(10.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          grid.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              row.forEachIndexed { c, col ->
                Box(
                  modifier = Modifier.size(22.dp).background(col, RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  if (r == 0 && c == 0) {
                    Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                  }
                }
              }
            }
          }
        }
      }
    }
    "nonogrid" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF101421), Color(0xFF191D33)))),
        contentAlignment = Alignment.Center,
      ) {
        val topClues = listOf("1", "3", "1")
        val rowClues = listOf("1", "3", "1")
        val pattern = listOf(listOf(0, 1, 0), listOf(1, 1, 1), listOf(0, 1, 0))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 2.dp)) {
            topClues.forEach { tc ->
              Text(tc, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.width(20.dp))
            }
          }
          pattern.forEachIndexed { r, row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(rowClues[r], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857), modifier = Modifier.width(12.dp))
              row.forEach { on ->
                Box(
                  modifier =
                    Modifier
                      .size(20.dp)
                      .background(if (on == 1) Color(0xFF4CE8C8) else Color(0xFF232A3D), RoundedCornerShape(4.dp))
                      .then(
                        if (on == 1) Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                        else Modifier,
                      ),
                )
              }
            }
          }
        }
      }
    }
    "slide_15" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0E1522), Color(0xFF17243A)))),
        contentAlignment = Alignment.Center,
      ) {
        val cells = listOf("1", "2", "3", "4", "5", "6", "7", "8", "")
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0A0F18), RoundedCornerShape(10.dp))
              .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              row.forEach { label ->
                Box(
                  modifier =
                    Modifier
                      .size(24.dp)
                      .background(
                        when (label) {
                          "" -> Color(0xFF111827)
                          "1", "2", "3" -> Color(0xFF2563EB)
                          else -> Color(0xFF27364D)
                        },
                        RoundedCornerShape(5.dp),
                      )
                      .then(
                        if (label.isNotEmpty()) Modifier.border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                        else Modifier,
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  if (label.isNotEmpty()) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }
    }
    "hanoi_vault" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E131F), Color(0xFF171E30))))
        val baseY = h * 0.78f
        drawRoundRect(Color(0xFF334155), Offset(w * 0.16f, baseY), Size(w * 0.68f, h * 0.065f), CornerRadius(5f))
        drawLine(Color(0xFF64748B), Offset(w * 0.17f, baseY), Offset(w * 0.83f, baseY), 2f)
        val pegs = listOf(w * 0.28f, w * 0.50f, w * 0.72f)
        pegs.forEach { px ->
          drawRoundRect(Color(0xFF475569), Offset(px - 3.5f, h * 0.24f), Size(7f, baseY - h * 0.24f), CornerRadius(3.5f))
        }
        val disks = listOf(Color(0xFF4CE8C8) to 0.19f, Color(0xFF38BDF8) to 0.145f, Color(0xFFFFC857) to 0.105f)
        disks.forEachIndexed { idx, (col, widthFrac) ->
          val dw = w * widthFrac
          val dx = pegs[0] - dw / 2f
          val dy = baseY - (idx + 1) * h * 0.115f
          drawRoundRect(col, Offset(dx, dy), Size(dw, h * 0.088f), CornerRadius(8f))
          drawLine(Color.White.copy(alpha = 0.45f), Offset(dx + 6f, dy + 2f), Offset(dx + dw - 6f, dy + 2f), 2f, StrokeCap.Round)
        }
        // Clean transfer arc to Peg C
        val arc = Path().apply {
          moveTo(pegs[0], h * 0.34f)
          quadraticTo(w * 0.50f, h * 0.14f, pegs[2], h * 0.38f)
        }
        drawPath(arc, Color(0x55FFC857), style = Stroke(2.5f, cap = StrokeCap.Round))
      }
    }
    "cipher_break" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF141021), Color(0xFF1F1833)))),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          modifier =
            Modifier
              .background(Color(0xFF231D38), RoundedCornerShape(12.dp))
              .border(1.5.dp, Color(0xFF453B6B), RoundedCornerShape(12.dp))
              .padding(horizontal = 14.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(Color(0xFFFF5C7C), Color(0xFF4CE8C8), Color(0xFFFFC857), Color(0xFF38BDF8)).forEach { c ->
            Box(
              modifier =
                Modifier
                  .size(19.dp)
                  .background(c, CircleShape)
                  .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            )
          }
          SpacerBox()
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(modifier = Modifier.size(7.dp).background(Color(0xFF4CE8C8), CircleShape))
              Box(modifier = Modifier.size(7.dp).background(Color(0xFFFFC857), CircleShape))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(modifier = Modifier.size(7.dp).background(Color(0xFF4CE8C8), CircleShape))
              Box(modifier = Modifier.size(7.dp).background(Color.White.copy(alpha = 0.22f), CircleShape))
            }
          }
        }
      }
    }
    "sudoku_six" -> {
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0D1422), Color(0xFF142138)))),
        contentAlignment = Alignment.Center,
      ) {
        val cells = listOf("1", "3", "4", "", "6", "2")
        Column(
          modifier =
            Modifier
              .background(Color(0xFF0B121F), RoundedCornerShape(9.dp))
              .border(1.5.dp, Color(0xFF60A5FA), RoundedCornerShape(9.dp))
              .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          cells.chunked(3).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              row.forEachIndexed { c, n ->
                Box(
                  modifier =
                    Modifier
                      .size(28.dp)
                      .background(if ((r + c) % 2 == 0) Color(0xFF1A263C) else Color(0xFF152033), RoundedCornerShape(4.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  if (n.isNotEmpty()) {
                    Text(
                      n,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (n == "6") Color(0xFF4CE8C8) else Color(0xFF93C5FD),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    "block_blast" -> {
      val miniGrid =
        listOf(
          listOf(Color(0xFFFACC15), Color(0xFFFACC15), Color(0xFF1E293B), Color(0xFF38BDF8)),
          listOf(Color(0xFFFACC15), Color(0xFFFACC15), Color(0xFF1E293B), Color(0xFF38BDF8)),
          listOf(Color(0xFF4CE8C8), Color(0xFF4CE8C8), Color(0xFF4CE8C8), Color(0xFF4CE8C8)),
          listOf(Color(0xFF1E293B), Color(0xFFFF5C7C), Color(0xFF1E293B), Color(0xFF1E293B)),
        )
      Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF0C1220), Color(0xFF152036)))),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
            modifier =
              Modifier
                .background(Color(0xFF111A2B), RoundedCornerShape(10.dp))
                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            miniGrid.forEachIndexed { r, row ->
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { col ->
                  val filled = col != Color(0xFF1E293B)
                  Box(
                    modifier =
                      Modifier
                        .size(17.dp)
                        .background(col, RoundedCornerShape(4.dp))
                        .then(
                          if (r == 2) Modifier.border(1.2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                          else if (filled) Modifier.border(0.7.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                          else Modifier,
                        ),
                  )
                }
              }
            }
          }
          // Subtle upcoming tray L-block preview beside the board
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(modifier = Modifier.size(13.dp).background(Color(0xFFA78BFA), RoundedCornerShape(3.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              Box(modifier = Modifier.size(13.dp).background(Color(0xFFA78BFA), RoundedCornerShape(3.dp)))
              Box(modifier = Modifier.size(13.dp).background(Color(0xFFA78BFA), RoundedCornerShape(3.dp)))
            }
          }
        }
      }
    }
    "crate_sokoban" -> {
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF16120E), Color(0xFF211A13))))
        // Subtle corridor floor line
        drawLine(Color(0xFF3A2E22), Offset(w * 0.14f, h * 0.72f), Offset(w * 0.86f, h * 0.72f), 3f)
        val s = h * 0.28f
        val top = h * 0.5f - s * 0.5f
        // Pusher bot with visor
        drawCircle(Color(0xFF38BDF8), s * 0.38f, Offset(w * 0.24f, h * 0.5f))
        drawRoundRect(Color(0xFFE0F2FE), Offset(w * 0.245f, h * 0.45f), Size(s * 0.28f, s * 0.18f), CornerRadius(3f))
        // Arrow pointing right
        drawLine(Color(0xFF94A3B8), Offset(w * 0.34f, h * 0.5f), Offset(w * 0.41f, h * 0.5f), 4f, StrokeCap.Round)
        // Crate box with gold rim & X braces
        val cx = w * 0.44f
        drawRoundRect(Color(0xFFD97706), Offset(cx, top), Size(s, s), CornerRadius(6f))
        drawRoundRect(Color(0xFFF59E0B), Offset(cx, top), Size(s, s), CornerRadius(6f), style = Stroke(2.5f))
        drawLine(Color(0xFF78350F), Offset(cx + 6f, top + 6f), Offset(cx + s - 6f, top + s - 6f), 4f)
        drawLine(Color(0xFF78350F), Offset(cx + s - 6f, top + 6f), Offset(cx + 6f, top + s - 6f), 4f)
        // Green target dock ring
        drawCircle(Color(0x3322C55E), s * 0.42f, Offset(w * 0.76f, h * 0.5f))
        drawCircle(Color(0xFF22C55E), s * 0.36f, Offset(w * 0.76f, h * 0.5f), style = Stroke(3.5f))
        drawCircle(Color(0xFF4ADE80), s * 0.14f, Offset(w * 0.76f, h * 0.5f))
      }
    }
    else -> { // queens_grid
      Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF140F1C), Color(0xFF20162E))))
        val bw = h * 0.74f
        val bx = w * 0.5f - bw * 0.5f
        val by = h * 0.13f
        val sq = bw / 4f
        for (r in 0..3) for (c in 0..3) {
          drawRect(
            if ((r + c) % 2 == 0) Color(0xFF2F2242) else Color(0xFF1D1529),
            Offset(bx + c * sq, by + r * sq),
            Size(sq, sq),
          )
        }
        drawRoundRect(Color(0xFF6B4E96), Offset(bx, by), Size(bw, bw), CornerRadius(5f), style = Stroke(2.5f))
        // Subtle dashed-look diagonal sightline from top queen
        drawLine(Color(0x44FF5C7C), Offset(bx + sq * 0.5f, by + sq * 1.5f), Offset(bx + sq * 2.5f, by + sq * 3.5f), 2.5f)
        listOf(1 to 0, 3 to 1).forEach { (r, c) ->
          val qx = bx + (c + 0.5f) * sq
          val qy = by + (r + 0.5f) * sq
          val crown = Path().apply {
            moveTo(qx - sq * 0.28f, qy + sq * 0.22f)
            lineTo(qx - sq * 0.32f, qy - sq * 0.18f)
            lineTo(qx - sq * 0.12f, qy + sq * 0.02f)
            lineTo(qx, qy - sq * 0.26f)
            lineTo(qx + sq * 0.12f, qy + sq * 0.02f)
            lineTo(qx + sq * 0.32f, qy - sq * 0.18f)
            lineTo(qx + sq * 0.28f, qy + sq * 0.22f)
            close()
          }
          drawPath(crown, Color(0xFFFFC857))
          drawPath(crown, Color.White.copy(alpha = 0.5f), style = Stroke(1.5f, join = StrokeJoin.Round))
        }
      }
    }
  }
}

@Composable
private fun MinimalTile(
  label: String,
  bg: Color,
  textCol: Color,
  highlightBorder: Color = Color.White.copy(alpha = 0.18f),
) {
  Box(
    modifier =
      Modifier
        .size(37.dp)
        .background(bg, RoundedCornerShape(7.dp))
        .border(1.dp, highlightBorder, RoundedCornerShape(7.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      fontSize = if (label.length >= 4) 10.sp else 14.sp,
      fontWeight = FontWeight.Bold,
      color = textCol,
    )
  }
}

@Composable
private fun SweeperMiniCell(label: String, textCol: Color, revealed: Boolean) {
  Box(
    modifier =
      Modifier
        .size(28.dp)
        .background(if (revealed) Color(0xFF1A2230) else Color(0xFF283548), RoundedCornerShape(5.dp))
        .border(0.7.dp, Color.White.copy(alpha = if (revealed) 0.08f else 0.16f), RoundedCornerShape(5.dp)),
    contentAlignment = Alignment.Center,
  ) {
    if (label.isNotEmpty()) {
      Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textCol)
    }
  }
}

@Composable
private fun SpacerBox() {
  Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color(0xFF3B3359)))
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

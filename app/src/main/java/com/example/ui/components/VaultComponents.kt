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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    InGameScreenshotCover(coverKey = coverKey, modifier = Modifier.fillMaxSize())

    if (isFanFavourite) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xEE18121E),
        border = BorderStroke(1.dp, Color(0xFFFF5C7C)),
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Icon(Icons.Rounded.Favorite, contentDescription = "Fan Favourite", tint = Color(0xFFFF5C7C), modifier = Modifier.size(11.dp))
          Text("FAV", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFFFFB3C1))
        }
      }
    }

    if (isLocked) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color(0xC2080A0F)),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color(0xEE141923),
          border = BorderStroke(1.dp, Color(0xFFFFC857)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = "Rank locked", tint = Color(0xFFFFC857), modifier = Modifier.size(14.dp))
            Text("${requiredTierLabel.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
          }
        }
      }
    }
  }
}

@Composable
private fun ScreenshotHudChip(
  label: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = Color(0xDB070B13),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
    shadowElevation = 3.dp,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(
        modifier = Modifier.size(5.dp).background(accent, CircleShape)
      )
      Text(
        text = label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFF1F5F9),
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeveledTile(
  topLeft: Offset,
  tileSize: Size,
  baseColor: Color,
  corner: Float = 6f,
  glowBorder: Color? = null,
) {
  drawRoundRect(
    color = Color.Black.copy(alpha = 0.35f),
    topLeft = Offset(topLeft.x, topLeft.y + 2f),
    size = tileSize,
    cornerRadius = CornerRadius(corner),
  )
  drawRoundRect(
    brush = Brush.verticalGradient(
      listOf(baseColor.copy(alpha = 1f), baseColor.copy(red = baseColor.red * 0.78f, green = baseColor.green * 0.78f, blue = baseColor.blue * 0.78f))
    ),
    topLeft = topLeft,
    size = tileSize,
    cornerRadius = CornerRadius(corner),
  )
  // Top specular bevel catchlight
  drawRoundRect(
    color = Color.White.copy(alpha = 0.24f),
    topLeft = Offset(topLeft.x + 1.5f, topLeft.y + 1.5f),
    size = Size((tileSize.width - 3f).coerceAtLeast(1f), (tileSize.height * 0.28f).coerceAtLeast(1f)),
    cornerRadius = CornerRadius((corner - 1f).coerceAtLeast(1f)),
  )
  if (glowBorder != null) {
    drawRoundRect(
      color = glowBorder,
      topLeft = topLeft,
      size = tileSize,
      cornerRadius = CornerRadius(corner),
      style = Stroke(width = 2f),
    )
  }
}

/**
 * Rich, proportional in-game screenshot previews for all 18 games so every Roblox-style
 * Discover tile shows crisp gameplay, shaded characters, detailed boards, and live HUD telemetry.
 */
@Composable
private fun InGameScreenshotCover(coverKey: String, modifier: Modifier = Modifier) {
  val textMeasurer = rememberTextMeasurer()

  when (coverKey) {
    "neon_hop" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          val teal = Color(0xFF4CE8C8)
          drawRect(Brush.verticalGradient(listOf(Color(0xFF050811), Color(0xFF0E1B2E), Color(0xFF132D46))))
          // Stars & cyber towers skyline
          listOf(0.14f to 0.14f, 0.36f to 0.10f, 0.62f to 0.16f, 0.88f to 0.12f).forEach { (sx, sy) ->
            drawCircle(Color.White.copy(alpha = 0.45f), 1.8f, Offset(w * sx, h * sy))
          }
          listOf(0.05f to 0.36f, 0.21f to 0.48f, 0.41f to 0.33f, 0.63f to 0.52f, 0.82f to 0.40f).forEach { (xf, bh) ->
            drawRect(Color(0xFF0A1322), Offset(w * xf, h * (1f - bh)), Size(w * 0.14f, h * bh))
            drawLine(teal.copy(alpha = 0.18f), Offset(w * xf, h * (1f - bh)), Offset(w * (xf + 0.14f), h * (1f - bh)), 2f)
          }
          // Horizon synth grid
          for (i in 1..4) {
            drawLine(Color(0x224CE8C8), Offset(0f, h * (0.56f + i * 0.10f)), Offset(w, h * (0.56f + i * 0.10f)), 1.5f)
          }
          // Stepped 3D Bevel Platforms + Spring Bounce Pad + Warp Gate
          drawBeveledTile(Offset(w * 0.08f, h * 0.76f), Size(w * 0.24f, h * 0.09f), Color(0xFF1E293B), corner = 6f)
          drawLine(teal, Offset(w * 0.09f, h * 0.76f), Offset(w * 0.31f, h * 0.76f), 3.5f, StrokeCap.Round)
          drawBeveledTile(Offset(w * 0.20f, h * 0.715f), Size(w * 0.09f, h * 0.045f), Color(0xFFFACC15), corner = 4f)

          drawBeveledTile(Offset(w * 0.37f, h * 0.59f), Size(w * 0.22f, h * 0.09f), Color(0xFF28384F), corner = 6f)
          drawLine(Color(0xFF38BDF8), Offset(w * 0.38f, h * 0.59f), Offset(w * 0.58f, h * 0.59f), 3.5f, StrokeCap.Round)
          // Glowing collectible orb
          drawCircle(Color(0x44B8FF78), h * 0.06f, Offset(w * 0.48f, h * 0.49f))
          drawCircle(Color(0xFFB8FF78), h * 0.028f, Offset(w * 0.48f, h * 0.49f))

          // Patrol Drone with eye & glow
          drawCircle(Color(0x44FF5C7C), h * 0.075f, Offset(w * 0.55f, h * 0.29f))
          drawLine(Color(0xFF94A3B8), Offset(w * 0.47f, h * 0.245f), Offset(w * 0.63f, h * 0.245f), 2.2f)
          drawBeveledTile(Offset(w * 0.495f, h * 0.26f), Size(w * 0.11f, h * 0.065f), Color(0xFF334155), corner = 7f, glowBorder = Color(0xFFFF5C7C))
          drawCircle(Color(0xFFFF5C7C), h * 0.018f, Offset(w * 0.55f, h * 0.292f))

          // Goal dock & Multi-ring Warp Gate
          drawBeveledTile(Offset(w * 0.65f, h * 0.45f), Size(w * 0.27f, h * 0.09f), Color(0xFF14534B), corner = 6f)
          drawLine(teal, Offset(w * 0.66f, h * 0.45f), Offset(w * 0.91f, h * 0.45f), 3.5f, StrokeCap.Round)
          drawOval(Color(0x334CE8C8), Offset(w * 0.76f, h * 0.19f), Size(w * 0.13f, h * 0.26f))
          drawOval(Color(0xFF4CE8C8), Offset(w * 0.775f, h * 0.205f), Size(w * 0.10f, h * 0.23f), style = Stroke(3.5f))
          drawOval(Color(0xFF38BDF8), Offset(w * 0.795f, h * 0.24f), Size(w * 0.06f, h * 0.16f), style = Stroke(2f))

          // Articulated Cyber Runner mid-leap + Scarf trail
          val arc = Path().apply {
            moveTo(w * 0.23f, h * 0.70f)
            quadraticTo(w * 0.30f, h * 0.32f, w * 0.38f, h * 0.42f)
          }
          drawPath(arc, Brush.horizontalGradient(listOf(Color.Transparent, teal.copy(alpha = 0.75f))), style = Stroke(4f, cap = StrokeCap.Round))
          val px = w * 0.40f; val py = h * 0.42f
          // Fluttering crimson scarf
          drawLine(Color(0xFFFF5C7C), Offset(px - w * 0.02f, py - h * 0.01f), Offset(px - w * 0.10f, py + h * 0.02f), 4f, StrokeCap.Round)
          // Legs & boots
          drawLine(Color(0xFF94A3B8), Offset(px - w * 0.01f, py + h * 0.05f), Offset(px - w * 0.045f, py + h * 0.11f), 4f, StrokeCap.Round)
          drawLine(Color(0xFFF8FAFC), Offset(px + w * 0.015f, py + h * 0.05f), Offset(px + w * 0.05f, py + h * 0.09f), 4f, StrokeCap.Round)
          // Armored torso & helmet visor
          drawBeveledTile(Offset(px - w * 0.04f, py - h * 0.02f), Size(w * 0.08f, h * 0.075f), Color(0xFF1E293B), corner = 5f, glowBorder = teal)
          drawCircle(Color(0xFFF1F5F9), h * 0.042f, Offset(px + w * 0.008f, py - h * 0.05f))
          drawRoundRect(teal, Offset(px + w * 0.005f, py - h * 0.062f), Size(w * 0.045f, h * 0.024f), CornerRadius(3f))
        }
        ScreenshotHudChip(
          label = "SECTOR 14 · ♥♥♥",
          accent = Color(0xFF4CE8C8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "skyline_dash" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          val amber = Color(0xFFFFB347)
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0E091B), Color(0xFF28163A), Color(0xFF471E3E))))
          // Retro striped synthwave sunset
          drawCircle(Brush.radialGradient(listOf(Color(0x88FFB347), Color(0x00FF5C7C)), Offset(w * 0.75f, h * 0.31f), h * 0.24f), h * 0.24f, Offset(w * 0.75f, h * 0.31f))
          drawCircle(Color(0x99FF9F43), h * 0.13f, Offset(w * 0.75f, h * 0.31f))
          // Distant towers
          drawRect(Color(0xFF151024), Offset(w * 0.22f, h * 0.38f), Size(w * 0.13f, h * 0.62f))
          drawRect(Color(0xFF151024), Offset(w * 0.61f, h * 0.34f), Size(w * 0.14f, h * 0.66f))
          // Rooftops with beveled ledges & lit window grids
          drawRect(Color(0xFF1A1429), Offset(w * 0.03f, h * 0.58f), Size(w * 0.26f, h * 0.42f))
          drawLine(Color(0xFF38BDF8), Offset(w * 0.03f, h * 0.58f), Offset(w * 0.29f, h * 0.58f), 4f)
          drawRect(Color(0xFF221B36), Offset(w * 0.35f, h * 0.46f), Size(w * 0.30f, h * 0.54f))
          drawLine(amber, Offset(w * 0.35f, h * 0.46f), Offset(w * 0.65f, h * 0.46f), 4.5f)
          drawRect(Color(0xFF181326), Offset(w * 0.71f, h * 0.60f), Size(w * 0.26f, h * 0.40f))
          drawLine(Color(0xFF4CE8C8), Offset(w * 0.71f, h * 0.60f), Offset(w * 0.97f, h * 0.60f), 3.5f)
          for (r in 0..2) for (c in 0..2) {
            drawRoundRect(Color(0x55FDE047), Offset(w * (0.39f + c * 0.075f), h * (0.53f + r * 0.11f)), Size(w * 0.035f, h * 0.055f), CornerRadius(2f))
          }
          // Glowing holo-coins & hover security drone
          listOf(0.46f, 0.56f, 0.80f).forEach { cx ->
            drawCircle(Color(0x44FACC15), h * 0.055f, Offset(w * cx, h * 0.37f))
            drawCircle(Color(0xFFFACC15), h * 0.030f, Offset(w * cx, h * 0.37f))
            drawCircle(Color.White.copy(alpha = 0.8f), h * 0.010f, Offset(w * cx - 2f, h * 0.36f))
          }
          drawBeveledTile(Offset(w * 0.73f, h * 0.50f), Size(w * 0.11f, h * 0.06f), Color(0xFF334155), corner = 6f, glowBorder = Color(0xFFFF5C7C))
          drawCircle(Color(0xFFFF5C7C), h * 0.018f, Offset(w * 0.76f, h * 0.53f))
          // Speed dash shockwave cone + articulated parkour runner
          listOf(0.32f, 0.39f, 0.45f).forEachIndexed { idx, yf ->
            drawLine(Color(0xFF4CE8C8).copy(alpha = 0.7f - idx * 0.18f), Offset(w * 0.10f, h * yf), Offset(w * 0.32f, h * (yf - 0.03f)), 3.5f, StrokeCap.Round)
          }
          val rx = w * 0.39f; val ry = h * 0.33f
          drawLine(Color(0xFFFF5C7C), Offset(rx, ry), Offset(rx - w * 0.11f, ry + h * 0.03f), 4f, StrokeCap.Round)
          drawLine(Color(0xFFF8FAFC), Offset(rx, ry + h * 0.07f), Offset(rx + w * 0.055f, ry + h * 0.12f), 4.5f, StrokeCap.Round)
          drawLine(Color(0xFF94A3B8), Offset(rx - w * 0.01f, ry + h * 0.07f), Offset(rx - w * 0.055f, ry + h * 0.125f), 4.5f, StrokeCap.Round)
          drawBeveledTile(Offset(rx - w * 0.035f, ry + h * 0.01f), Size(w * 0.075f, h * 0.085f), Color(0xFFFF5C7C), corner = 5f)
          drawCircle(Color(0xFFFFF4E6), h * 0.044f, Offset(rx + w * 0.01f, ry - h * 0.035f))
          drawRoundRect(amber, Offset(rx + w * 0.012f, ry - h * 0.048f), Size(w * 0.04f, h * 0.022f), CornerRadius(3f))
        }
        ScreenshotHudChip(
          label = "640m · DASH READY",
          accent = Color(0xFFFFB347),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "street_fighter_2d" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          val p1 = Color(0xFFFF5C7C)
          val p2 = Color(0xFF38BDF8)
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0D111E), Color(0xFF1D223A), Color(0xFF0C101A))))
          // Crimson Arena Moon & Dojo Beams + Lantern Glow
          drawCircle(Brush.radialGradient(listOf(p1.copy(alpha = 0.36f), Color.Transparent), Offset(w * 0.5f, h * 0.42f), h * 0.28f), h * 0.28f, Offset(w * 0.5f, h * 0.42f))
          drawRect(Color(0xFF161B2B), Offset(w * 0.11f, h * 0.22f), Size(w * 0.04f, h * 0.56f))
          drawRect(Color(0xFF161B2B), Offset(w * 0.85f, h * 0.22f), Size(w * 0.04f, h * 0.56f))
          drawRoundRect(Color(0xFF232942), Offset(w * 0.06f, h * 0.20f), Size(w * 0.88f, h * 0.035f), CornerRadius(4f))
          listOf(0.20f, 0.80f).forEach { lx ->
            drawCircle(Color(0x33FF9F43), h * 0.07f, Offset(w * lx, h * 0.31f))
            drawOval(Color(0xFFFF9F43), Offset(w * lx - w * 0.028f, h * 0.27f), Size(w * 0.056f, h * 0.085f))
          }
          // Perspective wood plank dojo floor
          drawRect(Color(0xFF171C29), Offset(0f, h * 0.78f), Size(w, h * 0.22f))
          drawLine(Color(0xFF4CE8C8).copy(alpha = 0.65f), Offset(0f, h * 0.78f), Offset(w, h * 0.78f), 3f)
          for (i in 1..5) {
            val xt = w * (i / 6f)
            drawLine(Color(0xFF283046), Offset(xt, h * 0.78f), Offset(w * 0.5f + (xt - w * 0.5f) * 1.4f, h), 1.5f)
          }
          // Twin HP & Super Bars
          drawRoundRect(Color(0xFF1E293B), Offset(w * 0.06f, h * 0.135f), Size(w * 0.34f, h * 0.045f), CornerRadius(4f))
          drawRoundRect(p1, Offset(w * 0.06f, h * 0.135f), Size(w * 0.27f, h * 0.045f), CornerRadius(4f))
          drawRoundRect(Color(0xFF1E293B), Offset(w * 0.60f, h * 0.135f), Size(w * 0.34f, h * 0.045f), CornerRadius(4f))
          drawRoundRect(p2, Offset(w * 0.69f, h * 0.135f), Size(w * 0.25f, h * 0.045f), CornerRadius(4f))

          // Ground shadows
          drawOval(Color.Black.copy(alpha = 0.45f), Offset(w * 0.17f, h * 0.76f), Size(w * 0.16f, h * 0.05f))
          drawOval(Color.Black.copy(alpha = 0.45f), Offset(w * 0.67f, h * 0.76f), Size(w * 0.16f, h * 0.05f))

          // Kai (Left) in Martial Gi casting Hadou Flare
          drawCircle(Color(0xFFF3D2B3), h * 0.062f, Offset(w * 0.25f, h * 0.44f))
          drawLine(Color(0xFFFFC857), Offset(w * 0.20f, h * 0.42f), Offset(w * 0.12f, h * 0.46f), 4f, StrokeCap.Round)
          drawBeveledTile(Offset(w * 0.20f, h * 0.50f), Size(w * 0.105f, h * 0.185f), p1, corner = 7f)
          drawRect(Color(0xFF0F172A), Offset(w * 0.205f, h * 0.635f), Size(w * 0.095f, h * 0.025f))
          drawLine(Color(0xFFF3D2B3), Offset(w * 0.29f, h * 0.54f), Offset(w * 0.37f, h * 0.55f), 5f, StrokeCap.Round)
          drawCircle(Color(0xFFFFC857), h * 0.028f, Offset(w * 0.375f, h * 0.55f))
          drawLine(Color(0xFF0F172A), Offset(w * 0.22f, h * 0.69f), Offset(w * 0.19f, h * 0.78f), 6f, StrokeCap.Round)
          drawLine(Color(0xFF0F172A), Offset(w * 0.28f, h * 0.69f), Offset(w * 0.32f, h * 0.78f), 6f, StrokeCap.Round)

          // Mira (Right) Guarding behind Hex Force Shield
          drawCircle(Color(0xFFF3D2B3), h * 0.062f, Offset(w * 0.75f, h * 0.44f))
          drawRoundRect(Color(0xFF4CE8C8), Offset(w * 0.71f, h * 0.425f), Size(w * 0.048f, h * 0.024f), CornerRadius(3f))
          drawBeveledTile(Offset(w * 0.70f, h * 0.50f), Size(w * 0.105f, h * 0.185f), p2, corner = 7f)
          drawLine(Color(0xFF0F172A), Offset(w * 0.72f, h * 0.69f), Offset(w * 0.68f, h * 0.78f), 6f, StrokeCap.Round)
          drawLine(Color(0xFF0F172A), Offset(w * 0.78f, h * 0.69f), Offset(w * 0.81f, h * 0.78f), 6f, StrokeCap.Round)
          drawCircle(p2.copy(alpha = 0.22f), h * 0.185f, Offset(w * 0.72f, h * 0.57f))
          drawCircle(Color(0xFF7DD3FC), h * 0.185f, Offset(w * 0.72f, h * 0.57f), style = Stroke(3f))

          // Hadou Flare Comet Fireball
          val flareTail = Path().apply {
            moveTo(w * 0.54f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.49f)
            lineTo(w * 0.44f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.61f)
            close()
          }
          drawPath(flareTail, Color(0x99FF9F43))
          drawCircle(Color(0x44FF9F43), h * 0.095f, Offset(w * 0.51f, h * 0.55f))
          drawCircle(Color(0xFFFFC857), h * 0.056f, Offset(w * 0.51f, h * 0.55f))
          drawCircle(Color.White, h * 0.026f, Offset(w * 0.525f, h * 0.55f))
        }
        ScreenshotHudChip(
          label = "KO · RD 2",
          accent = Color(0xFFFF5C7C),
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
        )
      }
    }
    "flappy_bird" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          val pipe = Color(0xFF15803D)
          val lip = Color(0xFF22C55E)
          drawRect(Brush.verticalGradient(listOf(Color(0xFF091B34), Color(0xFF154370), Color(0xFF38BDF8))))
          // Fluffy clouds + distant ridge silhouettes
          drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(w * 0.08f, h * 0.21f), Size(w * 0.22f, h * 0.08f), CornerRadius(14f))
          drawCircle(Color.White.copy(alpha = 0.22f), h * 0.05f, Offset(w * 0.19f, h * 0.21f))
          drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(w * 0.68f, h * 0.16f), Size(w * 0.20f, h * 0.07f), CornerRadius(14f))
          drawCircle(Color(0xFF0F3B5E), h * 0.19f, Offset(w * 0.18f, h * 0.88f))
          drawCircle(Color(0xFF0F3B5E), h * 0.22f, Offset(w * 0.68f, h * 0.90f))

          fun drawCoverPipe(pxF: Float, topH: Float, botTop: Float) {
            val px = w * pxF; val pw = w * 0.125f
            // Top pipe + highlight stripe + lip
            drawRect(pipe, Offset(px, 0f), Size(pw, h * topH))
            drawRect(Color(0xFF4ADE80).copy(alpha = 0.35f), Offset(px + pw * 0.18f, 0f), Size(pw * 0.22f, h * topH))
            drawBeveledTile(Offset(px - pw * 0.12f, h * (topH - 0.06f)), Size(pw * 1.24f, h * 0.07f), lip, corner = 4f)
            // Bottom pipe + highlight stripe + lip
            val bh = h * 0.86f - h * botTop
            drawRect(pipe, Offset(px, h * botTop), Size(pw, bh))
            drawRect(Color(0xFF4ADE80).copy(alpha = 0.35f), Offset(px + pw * 0.18f, h * botTop), Size(pw * 0.22f, bh))
            drawBeveledTile(Offset(px - pw * 0.12f, h * botTop), Size(pw * 1.24f, h * 0.07f), lip, corner = 4f)
          }
          drawCoverPipe(0.49f, 0.31f, 0.59f)
          drawCoverPipe(0.80f, 0.42f, 0.70f)

          // Striped grass & soil ground
          drawRect(Color(0xFF22C55E), Offset(0f, h * 0.86f), Size(w, h * 0.055f))
          drawRect(Color(0xFF78350F), Offset(0f, h * 0.915f), Size(w, h * 0.085f))
          // Aviator Bird with Scarf, Goggles, Feathers & Dual Beak
          val bx = w * 0.27f; val by = h * 0.46f; val br = h * 0.098f
          drawLine(Color(0xFFEF4444), Offset(bx - br * 0.5f, by), Offset(bx - br * 1.5f, by - br * 0.22f), 6f, StrokeCap.Round)
          drawCircle(Color(0xFFFACC15), br, Offset(bx, by))
          drawOval(Color(0xFFFEF08A), Offset(bx - br * 0.75f, by - br * 0.05f), Size(br * 0.90f, br * 0.55f))
          drawCircle(Color(0xFF0F172A), br * 0.35f, Offset(bx + br * 0.40f, by - br * 0.22f))
          drawCircle(Color.White, br * 0.25f, Offset(bx + br * 0.40f, by - br * 0.22f))
          drawCircle(Color(0xFF0EA5E9), br * 0.13f, Offset(bx + br * 0.46f, by - br * 0.22f))
          val beak = Path().apply {
            moveTo(bx + br * 0.8f, by - br * 0.14f)
            lineTo(bx + br * 1.48f, by + br * 0.06f)
            lineTo(bx + br * 0.8f, by + br * 0.28f)
            close()
          }
          drawPath(beak, Color(0xFFF97316))
        }
        ScreenshotHudChip(
          label = "SCORE 1200",
          accent = Color(0xFFFACC15),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "vault_chess" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0B101A), Color(0xFF162131))))
          val boardSide = minOf(w, h) * 0.72f
          val bx = (w - boardSide) * 0.5f
          val by = h * 0.20f
          // Outer walnut/slate frame
          drawBeveledTile(Offset(bx - 6f, by - 6f), Size(boardSide + 12f, boardSide + 12f), Color(0xFF283446), corner = 8f, glowBorder = Color(0x99FFC857))
          val sq = boardSide / 5f
          for (r in 0..4) for (c in 0..4) {
            val col = when {
              r == 2 && c == 2 -> Color(0xFF2D6A73)
              (r + c) % 2 == 0 -> Color(0xFFD6E0EC)
              else -> Color(0xFF2A3647)
            }
            drawRect(col, Offset(bx + c * sq, by + r * sq), Size(sq + 0.4f, sq + 0.4f))
          }
          // Legal move aura ring
          drawCircle(Color(0x444CE8C8), sq * 0.30f, Offset(bx + sq * 3.5f, by + sq * 1.5f))
          drawCircle(Color(0xFF4CE8C8), sq * 0.15f, Offset(bx + sq * 3.5f, by + sq * 1.5f))
          // Black King silhouette on top-right
          val kxB = bx + sq * 3.5f; val kyB = by + sq * 0.54f
          drawOval(Color.Black.copy(alpha = 0.35f), Offset(kxB - sq * 0.28f, kyB + sq * 0.24f), Size(sq * 0.56f, sq * 0.14f))
          drawCircle(Color(0xFF111827), sq * 0.27f, Offset(kxB, kyB))
          drawCircle(Color(0xFFCBD5E1), sq * 0.27f, Offset(kxB, kyB), style = Stroke(1.8f))
          drawLine(Color(0xFFFACC15), Offset(kxB, kyB - sq * 0.38f), Offset(kxB, kyB - sq * 0.15f), 2.5f)
          drawLine(Color(0xFFFACC15), Offset(kxB - sq * 0.11f, kyB - sq * 0.28f), Offset(kxB + sq * 0.11f, kyB - sq * 0.28f), 2.5f)
          // Ivory Pawn on bottom-left
          drawCircle(Color(0xFFF8FAFC), sq * 0.22f, Offset(bx + sq * 1.5f, by + sq * 3.54f))
          drawCircle(Color(0xFF334155), sq * 0.22f, Offset(bx + sq * 1.5f, by + sq * 3.54f), style = Stroke(1.6f))
          // Featured Sculpted Ivory Knight in Center
          val kx = bx + sq * 2.5f; val ky = by + sq * 2.52f; val k = sq * 0.40f
          drawOval(Color.Black.copy(alpha = 0.32f), Offset(kx - k * 0.7f, ky + k * 0.72f), Size(k * 1.4f, k * 0.32f))
          val knight = Path().apply {
            moveTo(kx - k * 0.65f, ky + k * 0.85f)
            lineTo(kx + k * 0.65f, ky + k * 0.85f)
            lineTo(kx + k * 0.46f, ky - k * 0.28f)
            lineTo(kx + k * 0.05f, ky - k * 0.90f)
            lineTo(kx - k * 0.62f, ky - k * 0.24f)
            lineTo(kx - k * 0.40f, ky + k * 0.06f)
            lineTo(kx - k * 0.06f, ky - k * 0.06f)
            close()
          }
          drawPath(knight, Color(0xFFF8FAFC))
          drawPath(knight, Color(0xFF334155), style = Stroke(2.2f, join = StrokeJoin.Round))
          drawCircle(Color(0xFF334155), k * 0.09f, Offset(kx - k * 0.12f, ky - k * 0.36f))
        }
        ScreenshotHudChip(
          label = "480 ELO · 2P",
          accent = Color(0xFFFFC857),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "prism_2048" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0B111E), Color(0xFF152238))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          drawRoundRect(Color(0xFF080D17), Offset(ox - 6f, oy - 6f), Size(side + 12f, side + 12f), CornerRadius(12f))
          val cell = side / 4f; val gap = cell * 0.12f
          val vals = listOf(2, 4, 16, 32, 0, 8, 64, 128, 2, 16, 256, 512, 4, 32, 64, 2048)
          for (i in vals.indices) {
            val r = i / 4; val c = i % 4; val v = vals[i]
            val col = when (v) {
              0 -> Color(0xFF151E2E); 2 -> Color(0xFF243247); 4 -> Color(0xFF334155); 8 -> Color(0xFF0F766E)
              16 -> Color(0xFF0284C7); 32 -> Color(0xFF6366F1); 64 -> Color(0xFFDB2777); 128 -> Color(0xFFD97706)
              256, 512 -> Color(0xFF16A34A); else -> Color(0xFFEAB308)
            }
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            if (v == 0) {
              drawRoundRect(col, Offset(tx, ty), Size(ts, ts), CornerRadius(7f))
            } else {
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), col, corner = 7f, glowBorder = if (v >= 512) Color(0xAAFEF08A) else null)
              val label = if (v >= 1024) "2K" else "$v"
              val measured = textMeasurer.measure(
                label,
                style = TextStyle(color = if (v == 2048) Color(0xFF0F172A) else Color.White, fontSize = (ts * 0.34f).toSp(), fontWeight = FontWeight.Bold),
              )
              drawText(measured, topLeft = Offset(tx + (ts - measured.size.width) * 0.5f, ty + (ts - measured.size.height) * 0.5f))
            }
          }
        }
        ScreenshotHudChip(
          label = "SCORE 2480",
          accent = Color(0xFFFACC15),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "flux_circuit" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.linearGradient(listOf(Color(0xFF0A101A), Color(0xFF102233))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          val cell = side / 4f; val gap = cell * 0.12f
          for (r in 0..3) for (c in 0..3) {
            drawBeveledTile(Offset(ox + c * cell + gap * 0.5f, oy + r * cell + gap * 0.5f), Size(cell - gap, cell - gap), Color(0xFF151E2B), corner = 6f)
          }
          fun centerOf(r: Int, c: Int) = Offset(ox + (c + 0.5f) * cell, oy + (r + 0.5f) * cell)
          val energized = Path().apply {
            moveTo(centerOf(0, 0).x, centerOf(0, 0).y)
            lineTo(centerOf(0, 2).x, centerOf(0, 2).y)
            lineTo(centerOf(2, 2).x, centerOf(2, 2).y)
            lineTo(centerOf(2, 3).x, centerOf(2, 3).y)
            lineTo(centerOf(3, 3).x, centerOf(3, 3).y)
          }
          // Outer glow bloom + bright core trace
          drawPath(energized, Color(0x444CE8C8), style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
          drawPath(energized, Color(0xFF4CE8C8), style = Stroke(6.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
          drawPath(energized, Color.White.copy(alpha = 0.65f), style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
          val branch = Path().apply {
            moveTo(centerOf(0, 2).x, centerOf(0, 2).y)
            lineTo(centerOf(0, 3).x, centerOf(0, 3).y)
          }
          drawPath(branch, Color(0xFF4CE8C8), style = Stroke(6f, cap = StrokeCap.Round))
          // Terminals with rings
          drawCircle(Color(0x55FFC857), cell * 0.36f, centerOf(0, 0))
          drawCircle(Color(0xFFFFC857), cell * 0.24f, centerOf(0, 0))
          drawCircle(Color(0x5538BDF8), cell * 0.36f, centerOf(0, 3))
          drawCircle(Color(0xFF38BDF8), cell * 0.24f, centerOf(0, 3))
          drawCircle(Color(0x5538BDF8), cell * 0.36f, centerOf(3, 3))
          drawCircle(Color(0xFF38BDF8), cell * 0.24f, centerOf(3, 3))
        }
        ScreenshotHudChip(
          label = "STAGE 4 · 9 MOVES",
          accent = Color(0xFF4CE8C8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "vault_sweeper" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0E141F), Color(0xFF172233))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          val cell = side / 4f; val gap = cell * 0.1f
          val layout = listOf("0", "1", "F", "?", "1", "2", "1", "?", "F", "2", "0", "1", "1", "1", "0", "1")
          for (i in layout.indices) {
            val r = i / 4; val c = i % 4; val v = layout[i]
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            when (v) {
              "F" -> {
                drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(0xFFD97706), corner = 6f)
                // Crisp vector flag
                drawLine(Color.White, Offset(tx + ts * 0.32f, ty + ts * 0.22f), Offset(tx + ts * 0.32f, ty + ts * 0.78f), 2.5f, StrokeCap.Round)
                val pennant = Path().apply {
                  moveTo(tx + ts * 0.34f, ty + ts * 0.22f)
                  lineTo(tx + ts * 0.74f, ty + ts * 0.36f)
                  lineTo(tx + ts * 0.34f, ty + ts * 0.50f)
                  close()
                }
                drawPath(pennant, Color(0xFFFFFBEB))
              }
              "?" -> drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(0xFF2B3A50), corner = 6f)
              else -> {
                drawRoundRect(Color(0xFF151D2B), Offset(tx, ty), Size(ts, ts), CornerRadius(6f))
                if (v == "1" || v == "2") {
                  val col = if (v == "1") Color(0xFF38BDF8) else Color(0xFF4ADE80)
                  val measured = textMeasurer.measure(v, style = TextStyle(color = col, fontSize = (ts * 0.48f).toSp(), fontWeight = FontWeight.Bold))
                  drawText(measured, topLeft = Offset(tx + (ts - measured.size.width) * 0.5f, ty + (ts - measured.size.height) * 0.5f))
                }
              }
            }
          }
        }
        ScreenshotHudChip(
          label = "MINES 3 · PROBE",
          accent = Color(0xFFFF5C7C),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "lumen_toggle" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0C101A), Color(0xFF141D2D))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          val cell = side / 4f; val gap = cell * 0.14f
          val litSet = setOf(1, 4, 5, 6, 9, 14)
          for (i in 0 until 16) {
            val r = i / 4; val c = i % 4
            val lit = i in litSet
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            if (lit) {
              drawCircle(Color(0x334CE8C8), ts * 0.65f, Offset(tx + ts * 0.5f, ty + ts * 0.5f))
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(0xFF4CE8C8), corner = 8f, glowBorder = Color.White.copy(alpha = 0.65f))
              drawCircle(Color.White.copy(alpha = 0.85f), ts * 0.16f, Offset(tx + ts * 0.5f, ty + ts * 0.5f))
            } else {
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(0xFF1E2636), corner = 8f)
              drawCircle(Color(0xFF334155), ts * 0.12f, Offset(tx + ts * 0.5f, ty + ts * 0.5f))
            }
          }
        }
        ScreenshotHudChip(
          label = "4×4 · 6 MOVES",
          accent = Color(0xFF4CE8C8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "chroma_flood" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0D111C), Color(0xFF161E2E))))
          val palette = listOf(Color(0xFF4CE8C8), Color(0xFFFF5C7C), Color(0xFFFFC857), Color(0xFF38BDF8), Color(0xFFA78BFA))
          val gridCols = listOf(0, 0, 0, 3, 4, 0, 0, 2, 2, 1, 0, 1, 2, 3, 4, 3, 1, 0, 0, 2, 4, 3, 2, 1, 0)
          val side = minOf(w, h) * 0.68f
          val ox = (w - side) * 0.5f; val oy = h * 0.21f
          val cell = side / 5f
          for (i in gridCols.indices) {
            val r = i / 5; val c = i % 5
            val base = palette[gridCols[i]]
            drawRect(base, Offset(ox + c * cell, oy + r * cell), Size(cell + 0.5f, cell + 0.5f))
            drawLine(Color.White.copy(alpha = 0.18f), Offset(ox + c * cell, oy + r * cell), Offset(ox + (c + 1) * cell, oy + r * cell), 1.5f)
          }
          drawCircle(Color.White, cell * 0.22f, Offset(ox + cell * 0.5f, oy + cell * 0.5f))
          drawRoundRect(Color.White.copy(alpha = 0.45f), Offset(ox, oy), Size(side, side), CornerRadius(6f), style = Stroke(2.5f))
        }
        ScreenshotHudChip(
          label = "MOVES 11/22",
          accent = Color(0xFFA78BFA),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "nonogrid" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0E1320), Color(0xFF181E36))))
          val side = minOf(w, h) * 0.68f
          val ox = (w - side) * 0.5f; val oy = h * 0.22f
          val cell = side / 5f; val gap = cell * 0.12f
          val crown = listOf(1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0)
          for (i in crown.indices) {
            val r = i / 5; val c = i % 5; val filled = crown[i] == 1
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            if (filled) {
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(0xFF4CE8C8), corner = 5f)
            } else {
              drawRoundRect(Color(0xFF21293E), Offset(tx, ty), Size(ts, ts), CornerRadius(5f))
              drawLine(Color(0xFF475569), Offset(tx + ts * 0.3f, ty + ts * 0.3f), Offset(tx + ts * 0.7f, ty + ts * 0.7f), 1.8f)
              drawLine(Color(0xFF475569), Offset(tx + ts * 0.7f, ty + ts * 0.3f), Offset(tx + ts * 0.3f, ty + ts * 0.7f), 1.8f)
            }
          }
        }
        ScreenshotHudChip(
          label = "PUZZLE 2/3",
          accent = Color(0xFF38BDF8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "slide_15" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0C121F), Color(0xFF16243B))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          val cell = side / 4f; val gap = cell * 0.12f
          val tiles = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "", "12", "13", "14", "11", "15")
          for (i in tiles.indices) {
            val r = i / 4; val c = i % 4; val t = tiles[i]
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            if (t.isNotEmpty()) {
              val inPlace = t == "${i + 1}"
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), if (inPlace) Color(0xFF0F766E) else Color(0xFF28374E), corner = 6f)
              val measured = textMeasurer.measure(t, style = TextStyle(color = Color.White, fontSize = (ts * 0.40f).toSp(), fontWeight = FontWeight.Bold))
              drawText(measured, topLeft = Offset(tx + (ts - measured.size.width) * 0.5f, ty + (ts - measured.size.height) * 0.5f))
            } else {
              drawRoundRect(Color(0xFF0A101B), Offset(tx, ty), Size(ts, ts), CornerRadius(6f))
            }
          }
        }
        ScreenshotHudChip(
          label = "MOVES 18",
          accent = Color(0xFF4CE8C8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "hanoi_vault" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0C111D), Color(0xFF161F33))))
          val baseY = h * 0.80f
          drawBeveledTile(Offset(w * 0.12f, baseY), Size(w * 0.76f, h * 0.065f), Color(0xFF334155), corner = 5f)
          val pegs = listOf(w * 0.25f, w * 0.50f, w * 0.75f)
          pegs.forEach { px ->
            drawBeveledTile(Offset(px - 4.5f, h * 0.28f), Size(9f, baseY - h * 0.28f), Color(0xFF64748B), corner = 4f)
          }
          val aDisks = listOf(Color(0xFF4CE8C8) to 0.22f, Color(0xFF38BDF8) to 0.17f)
          aDisks.forEachIndexed { idx, (col, wf) ->
            val dw = w * wf
            drawBeveledTile(Offset(pegs[0] - dw / 2f, baseY - (idx + 1) * h * 0.108f), Size(dw, h * 0.088f), col, corner = 8f)
          }
          val cDisk = w * 0.125f
          drawBeveledTile(Offset(pegs[2] - cDisk / 2f, baseY - h * 0.108f), Size(cDisk, h * 0.088f), Color(0xFFFFC857), corner = 8f)
          val arc = Path().apply {
            moveTo(pegs[0], h * 0.44f)
            quadraticTo(w * 0.50f, h * 0.18f, pegs[2], h * 0.54f)
          }
          drawPath(arc, Color(0x99FFC857), style = Stroke(3.2f, cap = StrokeCap.Round))
        }
        ScreenshotHudChip(
          label = "4 DISKS · PAR 15",
          accent = Color(0xFFFFC857),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "cipher_break" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF120D20), Color(0xFF211738))))
          val rows = listOf(
            listOf(Color(0xFFFF5C7C), Color(0xFF4CE8C8), Color(0xFFFFC857), Color(0xFF38BDF8)),
            listOf(Color(0xFF4CE8C8), Color(0xFFFF5C7C), Color(0xFFA78BFA), Color(0xFF4ADE80)),
            listOf(Color(0xFF4CE8C8), Color(0xFFFFC857), Color(0xFF38BDF8), Color(0xFFA78BFA)),
          )
          val boardW = w * 0.74f; val boardH = h * 0.66f
          val ox = (w - boardW) * 0.5f; val oy = h * 0.22f
          drawBeveledTile(Offset(ox, oy), Size(boardW, boardH), Color(0xFF221C36), corner = 12f, glowBorder = Color(0x55A78BFA))
          val rowH = boardH / 3f
          rows.forEachIndexed { r, pegs ->
            val cy = oy + (r + 0.5f) * rowH
            pegs.forEachIndexed { c, col ->
              val px = ox + boardW * (0.18f + c * 0.16f)
              drawCircle(Color.Black.copy(alpha = 0.35f), rowH * 0.25f, Offset(px, cy + 2f))
              drawCircle(col, rowH * 0.24f, Offset(px, cy))
              drawCircle(Color.White.copy(alpha = 0.55f), rowH * 0.08f, Offset(px - rowH * 0.07f, cy - rowH * 0.07f))
            }
            drawCircle(Color(0xFF4CE8C8), rowH * 0.12f, Offset(ox + boardW * 0.84f, cy - rowH * 0.11f))
            drawCircle(Color(0xFFFFC857), rowH * 0.12f, Offset(ox + boardW * 0.92f, cy + rowH * 0.11f))
          }
        }
        ScreenshotHudChip(
          label = "ATTEMPT 4/8",
          accent = Color(0xFFA78BFA),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "sudoku_six" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF14233C))))
          val side = minOf(w, h) * 0.72f
          val ox = (w - side) * 0.5f; val oy = h * 0.20f
          val cell = side / 4f; val gap = cell * 0.1f
          val gridNums = listOf("1", "3", "4", "", "4", "", "1", "3", "2", "4", "6", "5", "5", "6", "2", "1")
          for (i in gridNums.indices) {
            val r = i / 4; val c = i % 4; val v = gridNums[i]
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            val isSelected = i == 5
            val inCrosshair = (r == 1 || c == 1) && !isSelected
            drawBeveledTile(
              Offset(tx, ty),
              Size(ts, ts),
              when {
                isSelected -> Color(0xFF155E75)
                inCrosshair -> Color(0xFF1E324F)
                else -> Color(0xFF192438)
              },
              corner = 5f,
              glowBorder = if (isSelected) Color(0xFF38BDF8) else null,
            )
            if (v.isNotEmpty()) {
              val measured = textMeasurer.measure(
                v,
                style = TextStyle(color = if (v == "6") Color(0xFF4CE8C8) else Color(0xFFE2E8F0), fontSize = (ts * 0.44f).toSp(), fontWeight = FontWeight.Bold),
              )
              drawText(measured, topLeft = Offset(tx + (ts - measured.size.width) * 0.5f, ty + (ts - measured.size.height) * 0.5f))
            }
          }
        }
        ScreenshotHudChip(
          label = "BOARD 1/2 · 6×6",
          accent = Color(0xFF60A5FA),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "block_blast" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF0A101E), Color(0xFF142138))))
          val boardSide = minOf(w, h) * 0.62f
          val ox = (w - boardSide) * 0.5f; val oy = h * 0.20f
          val cell = boardSide / 5f; val gap = cell * 0.1f
          val colors = listOf(
            0xFFFACC15, 0xFFFACC15, 0x00000000, 0xFF38BDF8, 0xFF38BDF8,
            0xFFFACC15, 0xFFFACC15, 0x00000000, 0x00000000, 0xFF38BDF8,
            0xFF4CE8C8, 0xFF4CE8C8, 0xFF4CE8C8, 0xFF4CE8C8, 0xFF4CE8C8,
            0x00000000, 0xFFFF5C7C, 0xFF1E293B, 0xFFA78BFA, 0x00000000,
            0x00000000, 0xFFFF5C7C, 0x00000000, 0xFFA78BFA, 0xFFA78BFA,
          )
          for (i in colors.indices) {
            val r = i / 5; val c = i % 5; val hex = colors[i]
            val tx = ox + c * cell + gap * 0.5f; val ty = oy + r * cell + gap * 0.5f; val ts = cell - gap
            if (hex == 0x00000000L || hex == 0xFF1E293BL) {
              drawRoundRect(Color(0xFF172133), Offset(tx, ty), Size(ts, ts), CornerRadius(4f))
            } else {
              drawBeveledTile(Offset(tx, ty), Size(ts, ts), Color(hex), corner = 4f, glowBorder = if (r == 2) Color.White.copy(alpha = 0.75f) else null)
            }
          }
          val trayY = oy + boardSide + h * 0.045f
          val mini = cell * 0.62f
          drawBeveledTile(Offset(w * 0.26f, trayY), Size(mini, mini), Color(0xFFA78BFA), corner = 3f)
          drawBeveledTile(Offset(w * 0.26f + mini + 2f, trayY), Size(mini, mini), Color(0xFFA78BFA), corner = 3f)
          drawBeveledTile(Offset(w * 0.62f, trayY), Size(mini, mini), Color(0xFF34D399), corner = 3f)
          drawBeveledTile(Offset(w * 0.62f + mini + 2f, trayY), Size(mini, mini), Color(0xFF34D399), corner = 3f)
        }
        ScreenshotHudChip(
          label = "SCORE 1420 · x3",
          accent = Color(0xFF4CE8C8),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    "crate_sokoban" -> {
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF10141F), Color(0xFF1A2232))))
          // Brick warehouse walls
          drawBeveledTile(Offset(w * 0.08f, h * 0.22f), Size(w * 0.84f, h * 0.11f), Color(0xFF334155), corner = 4f)
          drawBeveledTile(Offset(w * 0.08f, h * 0.74f), Size(w * 0.84f, h * 0.11f), Color(0xFF334155), corner = 4f)
          drawRect(Color(0xFF151C28), Offset(w * 0.08f, h * 0.33f), Size(w * 0.84f, h * 0.41f))
          val s = minOf(w, h) * 0.22f
          val top = h * 0.535f - s * 0.5f
          // Warehouse Bot with hard hat & visor
          drawBeveledTile(Offset(w * 0.20f, top + s * 0.26f), Size(s * 0.66f, s * 0.66f), Color(0xFF0EA5E9), corner = 6f)
          drawArc(Color(0xFFFACC15), 180f, 180f, true, Offset(w * 0.19f, top), Size(s * 0.68f, s * 0.48f))
          drawRoundRect(Color(0xFF0F172A), Offset(w * 0.24f, top + s * 0.34f), Size(s * 0.42f, s * 0.18f), CornerRadius(3f))
          drawCircle(Color(0xFF4CE8C8), s * 0.06f, Offset(w * 0.20f + s * 0.36f, top + s * 0.43f))
          // Braced cargo crate
          val cx = w * 0.43f
          drawBeveledTile(Offset(cx, top), Size(s, s), Color(0xFFD97706), corner = 6f, glowBorder = Color(0xFF92400E))
          drawLine(Color(0xFF78350F), Offset(cx + 4f, top + 4f), Offset(cx + s - 4f, top + s - 4f), 3f)
          drawLine(Color(0xFF78350F), Offset(cx + s - 4f, top + 4f), Offset(cx + 4f, top + s - 4f), 3f)
          // Glowing green dock beacon
          drawCircle(Color(0x444CE8C8), s * 0.48f, Offset(w * 0.75f, h * 0.535f))
          drawCircle(Color(0xFF4CE8C8), s * 0.30f, Offset(w * 0.75f, h * 0.535f), style = Stroke(2.5f))
          drawCircle(Color(0xFF4CE8C8), s * 0.15f, Offset(w * 0.75f, h * 0.535f))
        }
        ScreenshotHudChip(
          label = "WAREHOUSE 2/2",
          accent = Color(0xFFF59E0B),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
      }
    }
    else -> { // queens_grid
      Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width; val h = size.height
          drawRect(Brush.verticalGradient(listOf(Color(0xFF120C1A), Color(0xFF231733))))
          val bw = minOf(w, h) * 0.72f
          val bx = (w - bw) * 0.5f; val by = h * 0.20f
          drawBeveledTile(Offset(bx - 5f, by - 5f), Size(bw + 10f, bw + 10f), Color(0xFF2F2242), corner = 7f, glowBorder = Color(0xAAFFC857))
          val sq = bw / 4f
          for (r in 0..3) for (c in 0..3) {
            drawRect(if ((r + c) % 2 == 0) Color(0xFF35264A) else Color(0xFF1D1529), Offset(bx + c * sq, by + r * sq), Size(sq + 0.4f, sq + 0.4f))
          }
          listOf(0 to 1, 1 to 3, 2 to 0, 3 to 2).forEach { (r, c) ->
            val qx = bx + (c + 0.5f) * sq; val qy = by + (r + 0.5f) * sq
            drawCircle(Color(0x33FFC857), sq * 0.38f, Offset(qx, qy))
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
            drawPath(crown, Color.White.copy(alpha = 0.7f), style = Stroke(1.6f, join = StrokeJoin.Round))
            drawCircle(Color(0xFFFF5C7C), sq * 0.06f, Offset(qx, qy - sq * 0.26f))
          }
        }
        ScreenshotHudChip(
          label = "QUEENS 5/6",
          accent = Color(0xFFFFC857),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
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

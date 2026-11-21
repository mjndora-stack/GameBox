package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlayerTier

@Composable
fun GameCoverArtwork(
  coverKey: String,
  title: String,
  isLocked: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    val drawableRes =
      when (coverKey) {
        "neon_hop" -> R.drawable.img_cover_neon_hop
        "ash_cipher" -> R.drawable.img_cover_ash_cipher
        "crypt_echoes" -> R.drawable.img_cover_crypt_echoes
        "apex_vector" -> R.drawable.img_cover_apex_vector
        "vice_horizon" -> R.drawable.img_cover_vice_horizon
        else -> null
      }

    if (drawableRes != null) {
      Image(
        painter = painterResource(id = drawableRes),
        contentDescription = "$title artwork",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      // Crisp custom procedural sunset skyline vector cover for Skyline Dash
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
          brush = Brush.verticalGradient(listOf(Color(0xFF1A102B), Color(0xFF471F48), Color(0xFFFF884D))),
          size = size,
        )
        drawCircle(
          color = Color(0xFFFFD166).copy(alpha = 0.85f),
          radius = h * 0.24f,
          center = Offset(w * 0.72f, h * 0.36f),
        )
        val heights = listOf(0.42f, 0.60f, 0.36f, 0.72f, 0.50f, 0.64f, 0.45f)
        val bw = w / heights.size
        heights.forEachIndexed { idx, frac ->
          val bh = h * frac
          drawRect(
            color = Color(0xFF0E0A17),
            topLeft = Offset(idx * bw, h - bh),
            size = Size(bw + 2f, bh),
          )
          drawRect(
            color = Color(0xFFFFB347),
            topLeft = Offset(idx * bw, h - bh),
            size = Size(bw + 2f, 4f),
          )
        }
      }
    }

    if (isLocked) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(Color(0xB8080A0F)),
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
            Icon(
              Icons.Rounded.Lock,
              contentDescription = "Locked until Pro",
              tint = Color(0xFFFFC857),
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "PRO TIER LOCK",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFFFFC857),
            )
          }
        }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              Icons.Rounded.WorkspacePremium,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
          Column {
            Text(
              text = "$playerName · ${tier.displayName}",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = tier.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "$credits CR",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "$xp XP",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            if (nextTier != null) "${nextTier.minXp - xp} XP to ${nextTier.displayName}"
            else "MAX RANK · PRO UNLOCKED",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

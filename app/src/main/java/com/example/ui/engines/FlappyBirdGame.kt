package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class FlappyPipe(
  val x: Float, // normalized 0..1000
  val gapY: Float, // center of gap in 0..1000 vertical space
  val gapHeight: Float,
  val passed: Boolean = false,
)

@Composable
fun FlappyBirdGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var running by remember { mutableStateOf(false) }
  var gameOver by remember { mutableStateOf(false) }
  var birdY by remember { mutableFloatStateOf(460f) } // 0..1000
  var birdVy by remember { mutableFloatStateOf(0f) }
  var score by remember { mutableIntStateOf(0) }
  var bestScore by remember { mutableIntStateOf(0) }
  val pipes = remember { mutableStateListOf<FlappyPipe>() }
  var groundScroll by remember { mutableFloatStateOf(0f) }

  fun resetAndFlap() {
    pipes.clear()
    pipes.add(FlappyPipe(x = 880f, gapY = 440f, gapHeight = 220f))
    pipes.add(FlappyPipe(x = 1360f, gapY = 340f + Random.nextFloat() * 260f, gapHeight = 215f))
    birdY = 460f
    birdVy = -430f
    score = 0
    gameOver = false
    running = true
  }

  fun flap() {
    if (!running && !gameOver) {
      resetAndFlap()
    } else if (running) {
      birdVy = -430f
    }
  }

  LaunchedEffect(running) {
    if (!running) return@LaunchedEffect
    var lastNanos = 0L
    while (running) {
      withFrameNanos { now ->
        if (lastNanos == 0L) {
          lastNanos = now
          return@withFrameNanos
        }
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.035f)
        lastNanos = now

        val speed = (240f + score * 4f).coerceAtMost(380f)
        groundScroll = (groundScroll + speed * dt) % 60f

        birdVy += 1280f * dt
        birdY += birdVy * dt

        // Move & spawn pipes
        for (i in pipes.indices) {
          var pipe = pipes[i].copy(x = pipes[i].x - speed * dt)
          if (!pipe.passed && pipe.x < 240f) {
            pipe = pipe.copy(passed = true)
            score++
            if (score % 5 == 0) {
              val ach = if (score >= 15) "flappy_ace" else null
              onAwardXp(20, score, ach)
            }
          }
          pipes[i] = pipe
        }

        if (pipes.isNotEmpty() && pipes.first().x < -120f) {
          pipes.removeAt(0)
          val lastX = pipes.lastOrNull()?.x ?: 900f
          val nextGapH = (220f - score * 1.4f).coerceAtLeast(165f)
          pipes.add(
            FlappyPipe(
              x = lastX + 460f,
              gapY = 250f + Random.nextFloat() * 440f,
              gapHeight = nextGapH,
            )
          )
        }

        // Collision check (bird centered at x=240, radius ~22)
        val hitGroundOrCeiling = birdY < 24f || birdY > 860f
        val hitPipe =
          pipes.any { pipe ->
            val withinX = 240f + 20f > pipe.x - 42f && 240f - 20f < pipe.x + 42f
            val outsideGap = birdY - 20f < pipe.gapY - pipe.gapHeight / 2f || birdY + 20f > pipe.gapY + pipe.gapHeight / 2f
            withinX && outsideGap
          }

        if (hitGroundOrCeiling || hitPipe) {
          running = false
          gameOver = true
          if (score > bestScore) bestScore = score
          val xp = (10 + score * 6).coerceAtLeast(10)
          val ach = if (score >= 15) "flappy_ace" else null
          onAwardXp(xp, score, ach)
        }
      }
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text("PIPES $score · BEST $bestScore", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
      }
      Text("TAP SKY TO FLAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }

    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .pointerInput(Unit) {
            detectTapGestures(onPress = { flap() })
          }
          .testTag("flappy_canvas"),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val sx = size.width / 1000f
        val sy = size.height / 1000f
        val groundTop = 885f * sy

        // Sky & clouds
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0E2A47), Color(0xFF1D4E7A), Color(0xFF38BDF8))))
        drawCircle(Color.White.copy(alpha = 0.22f), 42f, Offset(size.width * 0.22f, size.height * 0.2f))
        drawCircle(Color.White.copy(alpha = 0.18f), 56f, Offset(size.width * 0.72f, size.height * 0.28f))

        // Pipes
        pipes.forEach { pipe ->
          val px = pipe.x * sx
          val halfW = 42f * sx
          val topBottom = (pipe.gapY - pipe.gapHeight / 2f) * sy
          val botTop = (pipe.gapY + pipe.gapHeight / 2f) * sy

          // Top Pipe
          drawRect(Color(0xFF16A34A), Offset(px - halfW, 0f), Size(halfW * 2f, topBottom))
          drawRoundRect(Color(0xFF22C55E), Offset(px - halfW - 6f, (topBottom - 24f).coerceAtLeast(0f)), Size(halfW * 2f + 12f, 24f), CornerRadius(6f))

          // Bottom Pipe
          drawRect(Color(0xFF16A34A), Offset(px - halfW, botTop), Size(halfW * 2f, (groundTop - botTop).coerceAtLeast(0f)))
          drawRoundRect(Color(0xFF22C55E), Offset(px - halfW - 6f, botTop), Size(halfW * 2f + 12f, 24f), CornerRadius(6f))
        }

        // Ground strip
        drawRect(Color(0xFF22C55E), Offset(0f, groundTop), Size(size.width, 16f))
        drawRect(Color(0xFF78350F), Offset(0f, groundTop + 16f), Size(size.width, size.height - groundTop))

        // Bird
        val bx = 240f * sx
        val by = birdY * sy
        val angle = (birdVy / 14f).coerceIn(-28f, 65f)
        rotate(degrees = angle, pivot = Offset(bx, by)) {
          drawCircle(Color(0xFFFACC15), 22f * sy.coerceAtLeast(0.7f), Offset(bx, by))
          drawCircle(Color(0xFFFEF08A), 12f * sy.coerceAtLeast(0.7f), Offset(bx - 6f, by + 2f)) // wing
          drawCircle(Color.White, 7f, Offset(bx + 9f, by - 6f))
          drawCircle(Color.Black, 3.5f, Offset(bx + 11f, by - 6f))
          drawRoundRect(Color(0xFFF97316), Offset(bx + 14f, by - 2f), Size(14f, 8f), CornerRadius(4f))
        }
      }

      if (!running) {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = Color(0xD90B111C),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = if (gameOver) "FLIGHT OVER" else "FLAPPY BIRD",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = if (gameOver) "Cleared $score pipes (Best $bestScore). Tap below to fly again!" else "Tap anywhere to weave through emerald pipe gates.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White,
            )
            Button(
              onClick = { resetAndFlap() },
              modifier = Modifier.testTag("flappy_start_btn"),
            ) {
              Text(if (gameOver) "Flap Again" else "Launch Flight")
            }
          }
        }
      }
    }
  }
}

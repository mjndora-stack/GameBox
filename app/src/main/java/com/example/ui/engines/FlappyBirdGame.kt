package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import kotlin.math.sin
import kotlin.random.Random

private data class FlappyPipe(
  val x: Float, // normalized 0..1000
  val gapY: Float, // center of gap in 0..1000 vertical space
  val gapHeight: Float,
  val passed: Boolean = false,
)

private data class FeatherParticle(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val life: Float,
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
  var wingTimer by remember { mutableFloatStateOf(0f) }
  var animTime by remember { mutableFloatStateOf(0f) }
  var score by remember { mutableIntStateOf(0) }
  var bestScore by remember { mutableIntStateOf(0) }
  val pipes = remember { mutableStateListOf<FlappyPipe>() }
  val feathers = remember { mutableStateListOf<FeatherParticle>() }
  var groundScroll by remember { mutableFloatStateOf(0f) }

  fun spawnFeathers(y: Float) {
    repeat(5) {
      feathers.add(
        FeatherParticle(
          x = 225f,
          y = y + Random.nextFloat() * 14f - 7f,
          vx = -90f - Random.nextFloat() * 80f,
          vy = 40f + Random.nextFloat() * 80f,
          life = 0.45f,
        )
      )
    }
  }

  fun resetAndFlap() {
    pipes.clear()
    feathers.clear()
    pipes.add(FlappyPipe(x = 880f, gapY = 440f, gapHeight = 220f))
    pipes.add(FlappyPipe(x = 1360f, gapY = 340f + Random.nextFloat() * 260f, gapHeight = 215f))
    birdY = 460f
    birdVy = -430f
    wingTimer = 0.25f
    score = 0
    gameOver = false
    running = true
    VaultAudioEngine.playGameSfx(GameSfx.JUMP)
    spawnFeathers(birdY)
  }

  fun flap() {
    if (!running && !gameOver) {
      resetAndFlap()
    } else if (running) {
      birdVy = -430f
      wingTimer = 0.25f
      VaultAudioEngine.playGameSfx(GameSfx.JUMP)
      spawnFeathers(birdY)
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
        animTime += dt
        if (wingTimer > 0f) wingTimer = (wingTimer - dt).coerceAtLeast(0f)

        val speed = (240f + score * 4f).coerceAtMost(380f)
        groundScroll = (groundScroll + speed * dt) % 60f

        birdVy += 1280f * dt
        birdY += birdVy * dt

        for (i in feathers.lastIndex downTo 0) {
          val ft = feathers[i]
          if (ft.life - dt <= 0f) {
            feathers.removeAt(i)
          } else {
            feathers[i] = ft.copy(x = ft.x + ft.vx * dt, y = ft.y + ft.vy * dt, life = ft.life - dt)
          }
        }

        // Move & spawn pipes
        for (i in pipes.indices) {
          var pipe = pipes[i].copy(x = pipes[i].x - speed * dt)
          if (!pipe.passed && pipe.x < 240f) {
            pipe = pipe.copy(passed = true)
            score++
            val points = score * 100
            if (score % 5 == 0) {
              VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
              val ach = if (score >= 15) "flappy_ace" else null
              onAwardXp(20, points, ach)
            } else {
              VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
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
          val beatBest = score > bestScore && score > 0
          if (score > bestScore) bestScore = score
          VaultAudioEngine.playGameSfx(if (beatBest && score >= 5) GameSfx.VICTORY else GameSfx.HIT)
          val xp = (10 + score * 6).coerceAtLeast(10)
          val points = score * 100
          val ach = if (score >= 15) "flappy_ace" else null
          onAwardXp(xp, points, ach)
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
        Text("SCORE ${score * 100} ($score PIPES) · BEST ${bestScore * 100}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
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

        // Sky & layered stylized clouds
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0C2340), Color(0xFF1A4978), Color(0xFF38BDF8))))
        listOf(0.18f to 0.22f, 0.56f to 0.16f, 0.80f to 0.28f).forEach { (cxF, cyF) ->
          val cx = size.width * cxF
          val cy = size.height * cyF
          drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(cx - 44f, cy), Size(88f, 26f), CornerRadius(14f))
          drawCircle(Color.White.copy(alpha = 0.18f), 20f, Offset(cx - 12f, cy))
          drawCircle(Color.White.copy(alpha = 0.18f), 26f, Offset(cx + 14f, cy - 6f))
        }

        // Beveled Emerald Pipes with highlight sheen
        pipes.forEach { pipe ->
          val px = pipe.x * sx
          val halfW = 42f * sx
          val topBottom = (pipe.gapY - pipe.gapHeight / 2f) * sy
          val botTop = (pipe.gapY + pipe.gapHeight / 2f) * sy

          // Top Pipe
          drawRect(Color(0xFF15803D), Offset(px - halfW, 0f), Size(halfW * 2f, topBottom))
          drawRect(Color(0xFF4ADE80).copy(alpha = 0.35f), Offset(px - halfW + 8f, 0f), Size(halfW * 0.42f, topBottom))
          drawRoundRect(Color(0xFF22C55E), Offset(px - halfW - 6f, (topBottom - 26f).coerceAtLeast(0f)), Size(halfW * 2f + 12f, 26f), CornerRadius(6f))

          // Bottom Pipe
          drawRect(Color(0xFF15803D), Offset(px - halfW, botTop), Size(halfW * 2f, (groundTop - botTop).coerceAtLeast(0f)))
          drawRect(Color(0xFF4ADE80).copy(alpha = 0.35f), Offset(px - halfW + 8f, botTop), Size(halfW * 0.42f, (groundTop - botTop).coerceAtLeast(0f)))
          drawRoundRect(Color(0xFF22C55E), Offset(px - halfW - 6f, botTop), Size(halfW * 2f + 12f, 26f), CornerRadius(6f))
        }

        // Scrolling Ground
        drawRect(Color(0xFF22C55E), Offset(0f, groundTop), Size(size.width, 18f))
        val stripeOffset = -(groundScroll * sx)
        var gx = stripeOffset
        while (gx < size.width + 60f) {
          drawRect(Color(0xFF4ADE80), Offset(gx, groundTop), Size(28f * sx, 18f))
          gx += 60f * sx
        }
        drawRect(Color(0xFF78350F), Offset(0f, groundTop + 18f), Size(size.width, size.height - groundTop))

        // Feather Particles
        feathers.forEach { ft ->
          drawOval(
            Color(0xFFFEF08A).copy(alpha = (ft.life / 0.45f).coerceIn(0f, 1f)),
            Offset(ft.x * sx, ft.y * sy),
            Size(11f, 5f),
          )
        }

        // Articulated Aviator Bird Character Model
        val bx = 240f * sx
        val by = birdY * sy
        val unit = 26f * sy.coerceAtLeast(0.72f)
        val pitchAngle = (birdVy / 14f).coerceIn(-30f, 65f)
        val flapWave = if (wingTimer > 0f) sin((wingTimer / 0.25f) * 18f) * 28f else sin(animTime * 9f) * 10f

        rotate(degrees = pitchAngle, pivot = Offset(bx, by)) {
          // Fluttering Red Aviator Scarf trailing behind bird
          val scarfWave = sin(animTime * 16f) * (unit * 0.25f)
          val scarf = Path().apply {
            moveTo(bx - unit * 0.3f, by + unit * 0.05f)
            quadraticBezierTo(bx - unit * 1.1f, by + scarfWave, bx - unit * 1.55f, by - unit * 0.2f - scarfWave)
            lineTo(bx - unit * 1.45f, by + unit * 0.15f - scarfWave)
            close()
          }
          drawPath(scarf, Color(0xFFEF4444))

          // Fanned Tail Feathers
          val tail = Path().apply {
            moveTo(bx - unit * 0.72f, by)
            lineTo(bx - unit * 1.32f, by - unit * 0.36f)
            lineTo(bx - unit * 1.18f, by)
            lineTo(bx - unit * 1.32f, by + unit * 0.32f)
            close()
          }
          drawPath(tail, Color(0xFFF59E0B))

          // Sculpted Bird Body & Head Crest
          val body = Path().apply {
            moveTo(bx - unit * 0.9f, by + unit * 0.08f)
            quadraticBezierTo(bx - unit * 0.3f, by - unit * 0.92f, bx + unit * 0.45f, by - unit * 0.78f)
            lineTo(bx + unit * 0.25f, by - unit * 1.1f) // feather crest
            lineTo(bx + unit * 0.62f, by - unit * 0.68f)
            quadraticBezierTo(bx + unit * 1.05f, by - unit * 0.15f, bx + unit * 0.72f, by + unit * 0.55f)
            quadraticBezierTo(bx - unit * 0.2f, by + unit * 0.85f, bx - unit * 0.9f, by + unit * 0.08f)
            close()
          }
          drawPath(body, Color(0xFFFACC15))

          // Cream belly highlight
          drawOval(
            Color(0xFFFEF9C3),
            Offset(bx - unit * 0.35f, by + unit * 0.05f),
            Size(unit * 0.95f, unit * 0.52f),
          )

          // Dual Beak (upper & lower mandible)
          val upperBeak = Path().apply {
            moveTo(bx + unit * 0.76f, by - unit * 0.18f)
            lineTo(bx + unit * 1.45f, by + unit * 0.02f)
            lineTo(bx + unit * 0.76f, by + unit * 0.12f)
            close()
          }
          drawPath(upperBeak, Color(0xFFF97316))
          val lowerBeak = Path().apply {
            moveTo(bx + unit * 0.76f, by + unit * 0.12f)
            lineTo(bx + unit * 1.28f, by + unit * 0.18f)
            lineTo(bx + unit * 0.76f, by + unit * 0.32f)
            close()
          }
          drawPath(lowerBeak, Color(0xFFEA580C))

          // Aviator Goggle Strap + Lens
          drawLine(Color(0xFF78350F), Offset(bx - unit * 0.58f, by - unit * 0.36f), Offset(bx + unit * 0.52f, by - unit * 0.34f), 4f)
          drawCircle(Color(0xFF0F172A), unit * 0.34f, Offset(bx + unit * 0.42f, by - unit * 0.28f))
          drawCircle(Color.White, unit * 0.26f, Offset(bx + unit * 0.42f, by - unit * 0.28f))
          drawCircle(Color(0xFF0EA5E9), unit * 0.13f, Offset(bx + unit * 0.48f, by - unit * 0.28f))
          drawCircle(Color.White, unit * 0.05f, Offset(bx + unit * 0.52f, by - unit * 0.32f))

          // Animated Sculpted Wing with Feathers
          rotate(degrees = flapWave, pivot = Offset(bx - unit * 0.05f, by)) {
            val wing = Path().apply {
              moveTo(bx + unit * 0.18f, by - unit * 0.06f)
              quadraticBezierTo(bx - unit * 0.55f, by - unit * 0.56f, bx - unit * 0.85f, by + unit * 0.12f)
              quadraticBezierTo(bx - unit * 0.35f, by + unit * 0.44f, bx + unit * 0.18f, by + unit * 0.16f)
              close()
            }
            drawPath(wing, Color(0xFFFEF08A))
            drawPath(wing, Color(0xFFCA8A04), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
          }
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
              text = if (gameOver) "Score ${score * 100} ($score pipes cleared · Best ${bestScore * 100}). Tap below to fly again!" else "Tap anywhere to weave through emerald pipe gates (+100 pts per pipe).",
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

package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private data class TrackPickup(
  val worldX: Float,
  val isFuel: Boolean,
  val taken: Boolean = false,
)

@Composable
fun HillClimbGame(
  leftHandedHud: Boolean,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var running by remember { mutableStateOf(true) }
  var gameOverReason by remember { mutableStateOf<String?>(null) }

  var carX by remember { mutableFloatStateOf(120f) }
  var carY by remember { mutableFloatStateOf(0f) }
  var vx by remember { mutableFloatStateOf(0f) }
  var vy by remember { mutableFloatStateOf(0f) }
  var angleDeg by remember { mutableFloatStateOf(0f) }
  var angVel by remember { mutableFloatStateOf(0f) }
  var airRotationAccum by remember { mutableFloatStateOf(0f) }
  var fuel by remember { mutableFloatStateOf(100f) }
  var coins by remember { mutableIntStateOf(0) }
  var flipCount by remember { mutableIntStateOf(0) }
  var stuntText by remember { mutableStateOf("Climb the canyon ridges!") }

  var gasPressed by remember { mutableStateOf(false) }
  var brakePressed by remember { mutableStateOf(false) }

  val pickups = remember { mutableStateListOf<TrackPickup>() }

  // Deterministic procedural hill function that grows steeper with distance
  fun terrainHeight(wx: Float): Float {
    if (wx < 220f) return 0f
    val ramp = ((wx - 220f) / 3000f).coerceIn(0.25f, 1.35f)
    return (sin(wx * 0.0055f) * 70f +
      sin(wx * 0.013f + 0.8f) * 38f +
      cos(wx * 0.0024f) * 45f) * ramp
  }

  fun populatePickupsUpTo(maxX: Float) {
    var cursor = ((pickups.lastOrNull()?.worldX ?: 260f) + 140f)
    while (cursor < maxX) {
      val isFuel = (cursor.toInt() / 140) % 5 == 0
      pickups.add(TrackPickup(worldX = cursor, isFuel = isFuel))
      cursor += 140f
    }
  }

  fun resetRun() {
    carX = 120f
    carY = terrainHeight(120f) - 24f
    vx = 0f
    vy = 0f
    angleDeg = 0f
    angVel = 0f
    airRotationAccum = 0f
    fuel = 100f
    coins = 0
    flipCount = 0
    stuntText = "Climb the canyon ridges!"
    pickups.clear()
    populatePickupsUpTo(2600f)
    gameOverReason = null
    running = true
  }

  LaunchedEffect(Unit) {
    if (pickups.isEmpty()) resetRun()
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

        val halfWheelBase = 30f
        val rad = angleDeg * (PI.toFloat() / 180f)
        val frontX = carX + cos(rad) * halfWheelBase
        val rearX = carX - cos(rad) * halfWheelBase
        val frontGround = terrainHeight(frontX)
        val rearGround = terrainHeight(rearX)
        val groundCenterY = (frontGround + rearGround) * 0.5f - 22f
        val slopeAngle = atan2(frontGround - rearGround, halfWheelBase * 2f) * (180f / PI.toFloat())
        val onGround = carY >= groundCenterY - 4f && vy >= -40f

        if (onGround) {
          carY = groundCenterY
          if (vy > 0f) vy = 0f

          // Check completed 360 flip upon landing
          if (kotlin.math.abs(airRotationAccum) >= 290f) {
            flipCount++
            coins += 6
            stuntText = "STUNT FLIP! +150 PTS · +35 XP"
            onAwardXp(35, ((carX / 10f).toInt() + coins * 25 + flipCount * 150), "hill_climber")
          }
          airRotationAccum = 0f

          // Normalize angle close to slope unless roof-crashed
          var diff = ((angleDeg - slopeAngle + 540f) % 360f) - 180f
          if (kotlin.math.abs(diff) > 115f) {
            running = false
            gameOverReason = "ROOF CRASH! Keep your wheels down."
            val distM = (carX / 10f).toInt().coerceAtLeast(0)
            val totalScore = distM + coins * 25 + flipCount * 150
            val ach = if (distM >= 400 || flipCount > 0) "hill_climber" else null
            onAwardXp((15 + distM / 20).coerceAtLeast(15), totalScore, ach)
            return@withFrameNanos
          }
          angleDeg += (slopeAngle - angleDeg) * (dt * 12f).coerceAtMost(1f)
          angVel = 0f

          val thrust =
            when {
              fuel <= 0f -> 0f
              gasPressed -> 460f
              brakePressed -> -340f
              else -> 0f
            }
          val slopeGravity = sin(slopeAngle * PI.toFloat() / 180f) * 320f
          vx = (vx + (thrust - slopeGravity - vx * 0.65f) * dt).coerceIn(-220f, 520f)
          vy = 0f
        } else {
          // Airborne physics: Gas pitches nose up (negative screen deg), Brake pitches nose down
          vy += 680f * dt
          val pitchTorque =
            when {
              gasPressed -> -250f
              brakePressed -> 250f
              else -> 0f
            }
          angVel = (angVel + pitchTorque * dt - angVel * 1.4f * dt).coerceIn(-310f, 310f)
          angleDeg += angVel * dt
          airRotationAccum += angVel * dt
        }

        carX = (carX + vx * dt).coerceAtLeast(40f)
        carY += vy * dt

        // Fuel consumption
        val burnRate = if (gasPressed) 4.4f else 1.4f
        fuel = (fuel - burnRate * dt).coerceAtLeast(0f)
        if (fuel <= 0f && kotlin.math.abs(vx) < 12f && onGround) {
          running = false
          gameOverReason = "OUT OF FUEL!"
          val distM = (carX / 10f).toInt().coerceAtLeast(0)
          val totalScore = distM + coins * 25 + flipCount * 150
          val ach = if (distM >= 400 || flipCount > 0) "hill_climber" else null
          onAwardXp((15 + distM / 20).coerceAtLeast(15), totalScore, ach)
        }

        // Populate & collect Coins / Fuel
        populatePickupsUpTo(carX + 1200f)
        for (i in pickups.indices) {
          val p = pickups[i]
          if (!p.taken && kotlin.math.abs(p.worldX - carX) < 38f) {
            val py = terrainHeight(p.worldX) - 26f
            if (kotlin.math.abs(py - carY) < 58f) {
              pickups[i] = p.copy(taken = true)
              if (p.isFuel) {
                fuel = 100f
                stuntText = "FUEL REFILLED! (100%)"
              } else {
                coins++
                onAwardXp(8, (carX / 10f).toInt() + coins * 25 + flipCount * 150, null)
              }
            }
          }
        }
        if (pickups.size > 40 && pickups.first().worldX < carX - 500f) {
          pickups.removeAt(0)
        }
      }
    }
  }

  val distMeters = (carX / 10f).toInt().coerceAtLeast(0)
  val liveScore = distMeters + coins * 25 + flipCount * 150

  Column(
    modifier = modifier.fillMaxSize().padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Telemetry HUD
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("${distMeters}m · COINS $coins · FLIPS $flipCount", style = MaterialTheme.typography.labelLarge)
          Text("SCORE $liveScore", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.LocalGasStation, contentDescription = "Fuel", tint = if (fuel < 25f) MaterialTheme.colorScheme.error else Color(0xFF4CE8C8), modifier = Modifier.size(18.dp))
          LinearProgressIndicator(
            progress = { (fuel / 100f).coerceIn(0f, 1f) },
            color = if (fuel < 25f) MaterialTheme.colorScheme.error else Color(0xFF4CE8C8),
            modifier = Modifier.weight(1f).height(8.dp),
          )
          Text(stuntText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    // Track Canvas
    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val camX = carX - size.width * 0.28f
        val baseScreenY = size.height * 0.62f

        // Sky
        drawRect(Brush.verticalGradient(listOf(Color(0xFF141B2D), Color(0xFF2B3A55), Color(0xFF475569))))

        // Terrain Path
        val path = Path()
        val grassPath = Path()
        val stepPx = 12f
        val firstY = baseScreenY + terrainHeight(camX)
        path.moveTo(0f, firstY)
        grassPath.moveTo(0f, firstY)
        var sx = stepPx
        while (sx <= size.width + stepPx) {
          val ty = baseScreenY + terrainHeight(camX + sx)
          path.lineTo(sx, ty)
          grassPath.lineTo(sx, ty)
          sx += stepPx
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, Color(0xFF5C3A21))

        grassPath.lineTo(size.width, baseScreenY + terrainHeight(camX + size.width) + 12f)
        sx = size.width
        while (sx >= 0f) {
          grassPath.lineTo(sx, baseScreenY + terrainHeight(camX + sx) + 12f)
          sx -= stepPx
        }
        grassPath.close()
        drawPath(grassPath, Color(0xFF22C55E))

        // Pickups
        pickups.forEach { p ->
          if (!p.taken) {
            val px = p.worldX - camX
            if (px in -40f..(size.width + 40f)) {
              val py = baseScreenY + terrainHeight(p.worldX) - 24f
              if (p.isFuel) {
                drawRoundRect(Color(0xFFEF4444), Offset(px - 12f, py - 14f), Size(24f, 28f), CornerRadius(5f))
                drawRect(Color.White, Offset(px - 5f, py - 4f), Size(10f, 8f))
              } else {
                drawCircle(Color(0xFFFACC15), 11f, Offset(px, py))
                drawCircle(Color(0xFFCA8A04), 5f, Offset(px, py))
              }
            }
          }
        }

        // Buggy
        val cx = carX - camX
        val cy = baseScreenY + carY
        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
          // Chassis
          drawRoundRect(Color(0xFFFF5C7C), Offset(cx - 38f, cy - 16f), Size(76f, 20f), CornerRadius(8f))
          // Rollcage / Driver Helmet
          drawCircle(Color(0xFFE2E8F0), 10f, Offset(cx - 4f, cy - 22f))
          drawRect(Color(0xFF38BDF8), Offset(cx - 2f, cy - 25f), Size(8f, 6f))
          // Wheels
          drawCircle(Color(0xFF0F172A), 14f, Offset(cx - 26f, cy + 10f))
          drawCircle(Color(0xFF94A3B8), 6f, Offset(cx - 26f, cy + 10f))
          drawCircle(Color(0xFF0F172A), 14f, Offset(cx + 26f, cy + 10f))
          drawCircle(Color(0xFF94A3B8), 6f, Offset(cx + 26f, cy + 10f))
        }
      }

      if (!running && gameOverReason != null) {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = Color(0xDE0B111C),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          modifier = Modifier.align(Alignment.Center).padding(20.dp),
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(gameOverReason ?: "RUN ENDED", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Distance: ${distMeters}m · Coins: $coins · Flips: $flipCount · Score: $liveScore", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Button(onClick = { resetRun() }, modifier = Modifier.testTag("hill_retry_btn")) {
              Text("Drive Again")
            }
          }
        }
      }
    }

    // Gas & Brake Pedals
    val brakePedal: @Composable (Modifier) -> Unit = { m ->
      PedalButton(
        label = "BRAKE / PITCH -",
        activeColor = Color(0xFFEF4444),
        onHold = { brakePressed = it },
        modifier = m.testTag("hill_brake"),
      )
    }
    val gasPedal: @Composable (Modifier) -> Unit = { m ->
      PedalButton(
        label = "GAS / PITCH +",
        activeColor = Color(0xFF22C55E),
        onHold = { gasPressed = it },
        modifier = m.testTag("hill_gas"),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      if (leftHandedHud) {
        gasPedal(Modifier.weight(1f))
        brakePedal(Modifier.weight(1f))
      } else {
        brakePedal(Modifier.weight(1f))
        gasPedal(Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun PedalButton(
  label: String,
  activeColor: Color,
  onHold: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pressed by remember { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (pressed) activeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.5.dp, if (pressed) activeColor else MaterialTheme.colorScheme.outline),
    modifier =
      modifier
        .height(58.dp)
        .pointerInput(Unit) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            onHold(true)
            do {
              val ev = awaitPointerEvent()
            } while (ev.changes.any { it.pressed })
            pressed = false
            onHold(false)
          }
        },
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(label, style = MaterialTheme.typography.labelLarge, color = if (pressed) activeColor else MaterialTheme.colorScheme.onSurface)
    }
  }
}

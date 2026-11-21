package com.example.ui.engines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ControlConfig
import kotlin.math.sin
import kotlinx.coroutines.delay

private data class RivalCar(
  var zPos: Float,
  var laneOffset: Float,
  val speed: Float,
  val color: Color,
)

@Composable
fun ApexVectorEngine(
  controlConfig: ControlConfig,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var trackPos by remember { mutableFloatStateOf(0f) }
  var playerX by remember { mutableFloatStateOf(0f) } // -1.2 to 1.2
  var speed by remember { mutableFloatStateOf(0f) } // km/h 0..320
  var nitroFuel by remember { mutableFloatStateOf(100f) }
  var lap by remember { mutableIntStateOf(1) }
  var score by remember { mutableIntStateOf(0) }
  var raceFinished by remember { mutableStateOf(false) }

  val rivals = remember {
    mutableStateListOf(
      RivalCar(18f, -0.55f, 155f, Color(0xFF60A5FA)),
      RivalCar(36f, 0.45f, 175f, Color(0xFFFFB347)),
      RivalCar(58f, -0.2f, 190f, Color(0xFF34D399)),
      RivalCar(82f, 0.6f, 165f, Color(0xFFA78BFA)),
    )
  }

  val leftInteract = remember { MutableInteractionSource() }
  val rightInteract = remember { MutableInteractionSource() }
  val gasInteract = remember { MutableInteractionSource() }
  val nitroInteract = remember { MutableInteractionSource() }

  val leftPressed by leftInteract.collectIsPressedAsState()
  val rightPressed by rightInteract.collectIsPressedAsState()
  val gasPressed by gasInteract.collectIsPressedAsState()
  val nitroPressed by nitroInteract.collectIsPressedAsState()

  var kbLeft by remember { mutableStateOf(false) }
  var kbRight by remember { mutableStateOf(false) }
  var kbGas by remember { mutableStateOf(false) }
  var kbNitro by remember { mutableStateOf(false) }

  val lapLength = 180f

  fun resetRace() {
    trackPos = 0f
    playerX = 0f
    speed = 0f
    nitroFuel = 100f
    lap = 1
    score = 0
    raceFinished = false
    rivals[0].zPos = 18f
    rivals[1].zPos = 36f
    rivals[2].zPos = 58f
    rivals[3].zPos = 82f
  }

  LaunchedEffect(raceFinished) {
    var frame = 0
    while (!raceFinished) {
      delay(25L)
      frame++
      val accelerating = gasPressed || kbGas || (!leftPressed && !rightPressed && speed < 90f)
      val boosting = (nitroPressed || kbNitro) && nitroFuel > 1f

      val maxSpd = if (boosting) 325f else 245f
      if (boosting) {
        nitroFuel = (nitroFuel - 1.1f).coerceAtLeast(0f)
        speed = (speed + 5.2f).coerceAtMost(maxSpd)
      } else {
        nitroFuel = (nitroFuel + 0.18f).coerceAtMost(100f)
        speed =
          if (accelerating) (speed + 2.6f).coerceAtMost(maxSpd)
          else (speed - 3.2f).coerceAtLeast(0f)
      }

      val curveAtPlayer = sin(trackPos * 0.055f) * 0.65f
      val steerSpeed = 0.048f * (0.4f + (speed / 280f))
      if (leftPressed || kbLeft) playerX -= steerSpeed
      if (rightPressed || kbRight) playerX += steerSpeed
      playerX -= curveAtPlayer * (speed / 320f) * 0.025f
      playerX = playerX.coerceIn(-1.25f, 1.25f)

      // Off-road drag
      if (kotlin.math.abs(playerX) > 0.92f) {
        speed = (speed - 4.5f).coerceAtLeast(70f)
      }

      val prevLap = (trackPos / lapLength).toInt() + 1
      trackPos += (speed * 0.0042f)
      val newLap = (trackPos / lapLength).toInt() + 1

      if (frame % 15 == 0 && speed > 100f) {
        score += (speed / 18f).toInt()
      }

      if (newLap > prevLap) {
        if (newLap > 3) {
          raceFinished = true
          score += 500
          onAwardXp(140, score, "apex_finisher")
        } else {
          lap = newLap
          score += 250
          onAwardXp(45, score, null)
        }
      }

      // Rival AI & Collision
      for (rival in rivals) {
        rival.zPos += rival.speed * 0.0032f
        if (rival.zPos - trackPos < -4f) {
          rival.zPos = trackPos + 65f + (frame % 25)
          score += 60
        }
        val dz = rival.zPos - trackPos
        if (dz in 0f..1.6f && kotlin.math.abs(rival.laneOffset - playerX) < 0.28f) {
          speed = (speed * 0.62f).coerceAtLeast(50f)
          rival.zPos += 4f
        }
      }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF0A0812))
        .onPreviewKeyEvent { ev ->
          val down = ev.type == KeyEventType.KeyDown
          when (ev.key) {
            Key.A, Key.DirectionLeft -> { kbLeft = down; true }
            Key.D, Key.DirectionRight -> { kbRight = down; true }
            Key.W, Key.DirectionUp -> { kbGas = down; true }
            Key.Spacebar, Key.ShiftLeft -> { kbNitro = down; true }
            else -> false
          }
        }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val horizonY = h * 0.40f

      // Sky & Neon Sunset
      drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0D081B), Color(0xFF281134)), 0f, horizonY),
        size = Size(w, horizonY),
      )
      val curveOffset = sin(trackPos * 0.055f) * w * 0.18f
      drawCircle(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFC857), Color(0xFFFF4D6D))),
        radius = h * 0.13f,
        center = Offset(w * 0.5f - curveOffset * 0.4f, horizonY - h * 0.02f),
      )

      // Distant wireframe mountain silhouettes
      val ridgePath =
        Path().apply {
          moveTo(0f, horizonY)
          lineTo(w * 0.18f, horizonY - 38f)
          lineTo(w * 0.36f, horizonY - 14f)
          lineTo(w * 0.54f, horizonY - 46f)
          lineTo(w * 0.76f, horizonY - 18f)
          lineTo(w * 0.92f, horizonY - 34f)
          lineTo(w, horizonY)
          close()
        }
      drawPath(ridgePath, color = Color(0xFF150E26))

      // Ground plane
      drawRect(
        color = Color(0xFF0F131C),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY),
      )

      // 3D Perspective Road Slices
      val slices = 42
      val roadH = h - horizonY
      for (i in 0 until slices) {
        val t0 = i.toFloat() / slices
        val t1 = (i + 1).toFloat() / slices
        val y0 = horizonY + (t0 * t0) * roadH
        val y1 = horizonY + (t1 * t1) * roadH
        val segH = (y1 - y0).coerceAtLeast(1f)

        val depth = (1f - t0).coerceAtLeast(0.05f)
        val centerShift = curveOffset * (depth * depth) - (playerX * w * 0.34f * t0)
        val roadHalfW = (w * 0.06f) + (w * 0.44f * t0)
        val cx = w * 0.5f + centerShift

        val stripePhase = ((trackPos * 2.2f + (1f - t0) * 14f).toInt()) % 2 == 0
        val roadColor = if (stripePhase) Color(0xFF1B202C) else Color(0xFF161A24)
        val curbColor = if (stripePhase) Color(0xFFFF4D6D) else Color(0xFFE2E8F0)

        // Curbs
        drawRect(
          color = curbColor,
          topLeft = Offset(cx - roadHalfW - 14f * t0, y0),
          size = Size((roadHalfW + 14f * t0) * 2f, segH + 1f),
        )
        // Asphalt
        drawRect(
          color = roadColor,
          topLeft = Offset(cx - roadHalfW, y0),
          size = Size(roadHalfW * 2f, segH + 1f),
        )
        // Center dashed line
        if (stripePhase) {
          drawRect(
            color = Color(0xFF5CF2D6).copy(alpha = 0.7f),
            topLeft = Offset(cx - 3f * t0, y0),
            size = Size(6f * t0, segH + 1f),
          )
        }
      }

      // Draw Rivals in 3D Perspective
      for (rival in rivals) {
        val dz = rival.zPos - trackPos
        if (dz in 1f..58f) {
          val t = (1f - (dz / 58f)).coerceIn(0.05f, 1f)
          val y = horizonY + (t * t) * roadH
          val depth = 1f - t
          val centerShift = curveOffset * (depth * depth) - (playerX * w * 0.34f * t)
          val roadHalfW = (w * 0.06f) + (w * 0.44f * t)
          val rx = w * 0.5f + centerShift + rival.laneOffset * roadHalfW * 0.75f
          val carW = 56f * t + 10f
          val carH = 30f * t + 6f
          drawRect(rival.color, Offset(rx - carW / 2f, y - carH), Size(carW, carH))
          drawRect(Color(0xFFFF3B5C), Offset(rx - carW * 0.35f, y - carH * 0.3f), Size(carW * 0.2f, carH * 0.16f))
          drawRect(Color(0xFFFF3B5C), Offset(rx + carW * 0.15f, y - carH * 0.3f), Size(carW * 0.2f, carH * 0.16f))
        }
      }

      // Player Supercar (bottom center)
      val carX = w * 0.5f
      val carY = h * 0.80f
      val lean = if (leftPressed || kbLeft) -10f else if (rightPressed || kbRight) 10f else 0f
      drawRect(Color(0xFF0A0C10), Offset(carX - 58f + lean, carY + 26f), Size(116f, 18f))
      drawRect(Color(0xFFE2E8F0), Offset(carX - 52f + lean, carY), Size(104f, 34f))
      drawRect(Color(0xFF1E293B), Offset(carX - 36f + lean, carY - 16f), Size(72f, 20f))
      // Tail lights & Nitro flame
      val boosting = (nitroPressed || kbNitro) && nitroFuel > 1f
      drawRect(Color(0xFFFF3B5C), Offset(carX - 44f + lean, carY + 10f), Size(22f, 8f))
      drawRect(Color(0xFFFF3B5C), Offset(carX + 22f + lean, carY + 10f), Size(22f, 8f))
      if (boosting) {
        drawCircle(Color(0xFF38BDF8), 14f, Offset(carX - 20f + lean, carY + 34f))
        drawCircle(Color(0xFF38BDF8), 14f, Offset(carX + 20f + lean, carY + 34f))
      }
    }

    // Top Telemetry HUD
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(color = Color(0xCC11161F), shape = RoundedCornerShape(999.dp)) {
        Text(
          text = "LAP $lap/3 · ${speed.toInt()} KM/H",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFF5CF2D6),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Surface(color = Color(0xCC11161F), shape = RoundedCornerShape(999.dp)) {
        Text(
          text = "NITRO ${nitroFuel.toInt()}% · PTS $score",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFFFFC857),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }

    // Adaptive Steering & Pedals
    val steerPad: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC141923))
              .border(1.5.dp, Color(0xFF5CF2D6), RoundedCornerShape(16.dp))
              .clickable(interactionSource = leftInteract, indication = null, onClick = {})
              .testTag("apex_left"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Steer Left", tint = Color.White)
        }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC141923))
              .border(1.5.dp, Color(0xFF5CF2D6), RoundedCornerShape(16.dp))
              .clickable(interactionSource = rightInteract, indication = null, onClick = {})
              .testTag("apex_right"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Steer Right", tint = Color.White)
        }
      }
    }

    val pedals: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC1B1828))
              .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
              .clickable(interactionSource = nitroInteract, indication = null, onClick = {})
              .testTag("apex_nitro"),
        ) {
          Icon(Icons.Rounded.Bolt, contentDescription = "Nitro Boost", tint = Color(0xFF38BDF8))
        }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(68.dp, 64.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC22151C))
              .border(1.5.dp, Color(0xFFFF5C7C), RoundedCornerShape(16.dp))
              .clickable(interactionSource = gasInteract, indication = null, onClick = {})
              .testTag("apex_gas"),
        ) {
          Icon(Icons.Rounded.Speed, contentDescription = "Accelerate", tint = Color(0xFFFF8AA4))
        }
      }
    }

    Row(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      if (controlConfig.leftHandedHud) {
        pedals()
        steerPad()
      } else {
        steerPad()
        pedals()
      }
    }

    if (raceFinished) {
      Surface(color = Color(0xE00A0812), modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text("CHECKERED FLAG!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF5CF2D6))
          Spacer(Modifier.height(8.dp))
          Text("3-Lap Score: $score", style = MaterialTheme.typography.titleLarge, color = Color.White)
          Text("+140 XP & Apex Badge synced to Vault.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
          Spacer(Modifier.height(20.dp))
          Button(onClick = { resetRace() }) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Race Again")
          }
        }
      }
    }
  }
}

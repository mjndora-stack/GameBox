package com.example.ui.engines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ControlConfig
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

private data class CityBlock(val x: Float, val y: Float, val w: Float, val h: Float, val roofColor: Color)

private data class StreetVehicle(
  var x: Float,
  var y: Float,
  var angle: Float,
  var speed: Float,
  val color: Color,
  var isPolice: Boolean = false,
)

@Composable
fun ViceHorizonEngine(
  controlConfig: ControlConfig,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var playerX by remember { mutableFloatStateOf(460f) }
  var playerY by remember { mutableFloatStateOf(460f) }
  var playerAngle by remember { mutableFloatStateOf(0f) }
  var inCarIndex by remember { mutableIntStateOf(-1) }
  var joyX by remember { mutableFloatStateOf(0f) }
  var joyY by remember { mutableFloatStateOf(0f) }
  var sprintBoost by remember { mutableStateOf(false) }

  var cash by remember { mutableIntStateOf(500) }
  var wantedStars by remember { mutableIntStateOf(0) }
  var targetX by remember { mutableFloatStateOf(980f) }
  var targetY by remember { mutableFloatStateOf(300f) }
  var missionCount by remember { mutableIntStateOf(0) }
  var statusNote by remember { mutableStateOf("Reach the Cyan Drop Beacon or hijack a Coupe!") }

  var kbX by remember { mutableFloatStateOf(0f) }
  var kbY by remember { mutableFloatStateOf(0f) }

  val blocks = remember {
    listOf(
      CityBlock(140f, 140f, 180f, 160f, Color(0xFF1F2737)),
      CityBlock(520f, 140f, 200f, 160f, Color(0xFF281F36)),
      CityBlock(880f, 140f, 220f, 140f, Color(0xFF1B2B34)),
      CityBlock(140f, 520f, 180f, 200f, Color(0xFF251E30)),
      CityBlock(520f, 520f, 200f, 180f, Color(0xFF1E293B)),
      CityBlock(880f, 500f, 220f, 220f, Color(0xFF2A1E2B)),
    )
  }

  val vehicles = remember {
    mutableStateListOf(
      StreetVehicle(400f, 380f, 0f, 0f, Color(0xFFFF4D8D)),
      StreetVehicle(780f, 410f, 90f, 0f, Color(0xFF4CE8C8)),
      StreetVehicle(400f, 760f, 0f, 0f, Color(0xFFFFC857)),
      StreetVehicle(1050f, 400f, 180f, 0f, Color(0xFF38BDF8), isPolice = true),
    )
  }

  fun collidesWithBuilding(x: Float, y: Float, pad: Float): Boolean {
    if (x < 40f || y < 40f || x > 1320f || y > 920f) return true
    return blocks.any { b -> x > b.x - pad && x < b.x + b.w + pad && y > b.y - pad && y < b.y + b.h + pad }
  }

  fun toggleEnterExitCar() {
    if (inCarIndex >= 0) {
      vehicles[inCarIndex].speed = 0f
      inCarIndex = -1
      statusNote = "On foot · Hold Boost to sprint"
    } else {
      val nearest =
        vehicles.indexOfFirst { v -> hypot(v.x - playerX, v.y - playerY) < 68f }
      if (nearest >= 0) {
        inCarIndex = nearest
        if (vehicles[nearest].isPolice) {
          wantedStars = (wantedStars + 1).coerceAtMost(4)
          statusNote = "Patrol Cruiser hijacked! Heat Level $wantedStars"
        } else {
          statusNote = "Driving Coupe · Deliver to Cyan Beacon!"
        }
      } else {
        statusNote = "Get closer to a parked car to enter"
      }
    }
  }

  LaunchedEffect(Unit) {
    val dropPoints = listOf(980f to 300f, 360f to 820f, 1140f to 780f, 420f to 100f)
    var tick = 0
    while (true) {
      delay(25L)
      tick++
      val ix = if (kbX != 0f) kbX else joyX
      val iy = if (kbY != 0f) kbY else joyY
      val mag = hypot(ix, iy).coerceAtMost(1f)

      if (inCarIndex >= 0) {
        val car = vehicles[inCarIndex]
        val topSpeed = if (sprintBoost) 8.4f else 6.0f
        if (mag > 0.1f) {
          val desiredAngle = Math.toDegrees(atan2(iy.toDouble(), ix.toDouble())).toFloat()
          car.angle = desiredAngle
          car.speed = (car.speed + 0.35f).coerceAtMost(topSpeed)
        } else {
          car.speed = (car.speed - 0.25f).coerceAtLeast(0f)
        }
        val rad = Math.toRadians(car.angle.toDouble())
        val nx = car.x + cos(rad).toFloat() * car.speed
        val ny = car.y + sin(rad).toFloat() * car.speed
        if (!collidesWithBuilding(nx, car.y, 20f)) car.x = nx else car.speed *= 0.4f
        if (!collidesWithBuilding(car.x, ny, 20f)) car.y = ny else car.speed *= 0.4f
        playerX = car.x
        playerY = car.y
        playerAngle = car.angle
      } else {
        val walkSpeed = if (sprintBoost) 4.4f else 2.8f
        if (mag > 0.1f) {
          playerAngle = Math.toDegrees(atan2(iy.toDouble(), ix.toDouble())).toFloat()
          val nx = playerX + ix * walkSpeed
          val ny = playerY + iy * walkSpeed
          if (!collidesWithBuilding(nx, playerY, 12f)) playerX = nx
          if (!collidesWithBuilding(playerX, ny, 12f)) playerY = ny
        }
      }

      // Police Cruiser pursuit when Wanted > 0
      if (wantedStars > 0) {
        val cop = vehicles.firstOrNull { it.isPolice && vehicles.indexOf(it) != inCarIndex }
        if (cop != null) {
          val dist = hypot(playerX - cop.x, playerY - cop.y).coerceAtLeast(1f)
          val dx = (playerX - cop.x) / dist
          val dy = (playerY - cop.y) / dist
          val chaseSpd = 2.4f + wantedStars * 0.4f
          if (!collidesWithBuilding(cop.x + dx * chaseSpd, cop.y, 18f)) cop.x += dx * chaseSpd
          if (!collidesWithBuilding(cop.x, cop.y + dy * chaseSpd, 18f)) cop.y += dy * chaseSpd
          cop.angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }
        if (tick % 240 == 0) {
          wantedStars = (wantedStars - 1).coerceAtLeast(0)
        }
      }

      // Courier Drop Completion
      if (hypot(playerX - targetX, playerY - targetY) < 42f) {
        missionCount++
        val payout = 450 + missionCount * 100
        cash += payout
        wantedStars = (wantedStars + 1).coerceAtMost(3)
        val next = dropPoints[missionCount % dropPoints.size]
        targetX = next.first
        targetY = next.second
        statusNote = "Drop Complete! +$$payout · Heat Level $wantedStars"
        onAwardXp(95, cash, null)
      }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF0D111A))
        .onPreviewKeyEvent { ev ->
          val down = ev.type == KeyEventType.KeyDown
          when (ev.key) {
            Key.A, Key.DirectionLeft -> { kbX = if (down) -1f else 0f; true }
            Key.D, Key.DirectionRight -> { kbX = if (down) 1f else 0f; true }
            Key.W, Key.DirectionUp -> { kbY = if (down) -1f else 0f; true }
            Key.S, Key.DirectionDown -> { kbY = if (down) 1f else 0f; true }
            Key.F, Key.Enter -> { if (down) toggleEnterExitCar(); true }
            Key.Spacebar -> { sprintBoost = down; true }
            else -> false
          }
        }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val camX = playerX - w / 2f
      val camY = playerY - h / 2f

      // Asphalt Streets & Coastal Ocean Strip
      drawRect(Color(0xFF151A24), size = Size(w, h))
      drawRect(
        color = Color(0xFF0B2C42),
        topLeft = Offset(1240f - camX, -camY),
        size = Size(400f, 1200f),
      )

      // Neon Avenue Markings
      for (gx in 80..1200 step 180) {
        drawLine(
          color = Color(0x28FFFFFF),
          start = Offset(gx - camX, -camY),
          end = Offset(gx - camX, 1000f - camY),
          strokeWidth = 3f,
        )
      }
      for (gy in 80..900 step 180) {
        drawLine(
          color = Color(0x28FFFFFF),
          start = Offset(-camX, gy - camY),
          end = Offset(1240f - camX, gy - camY),
          strokeWidth = 3f,
        )
      }

      // 2.5D Extruded Buildings
      for (b in blocks) {
        val bx = b.x - camX
        val by = b.y - camY
        drawRect(Color(0xFF0A0D14), Offset(bx + 10f, by + 12f), Size(b.w, b.h))
        drawRect(b.roofColor, Offset(bx, by), Size(b.w, b.h))
        drawRect(Color(0x44FF4D8D), Offset(bx + 6f, by + 6f), Size(b.w - 12f, 4f))
      }

      // Mission Target Beacon
      drawCircle(
        color = Color(0x444CE8C8),
        radius = 36f,
        center = Offset(targetX - camX, targetY - camY),
      )
      drawCircle(
        color = Color(0xFF4CE8C8),
        radius = 12f,
        center = Offset(targetX - camX, targetY - camY),
      )

      // Vehicles
      for (v in vehicles) {
        val vx = v.x - camX
        val vy = v.y - camY
        rotate(degrees = v.angle, pivot = Offset(vx, vy)) {
          drawRect(v.color, Offset(vx - 24f, vy - 13f), Size(48f, 26f))
          drawRect(Color(0xFF0F172A), Offset(vx - 8f, vy - 10f), Size(20f, 20f))
          if (v.isPolice) {
            drawRect(Color(0xFFEF4444), Offset(vx - 4f, vy - 10f), Size(5f, 9f))
            drawRect(Color(0xFF3B82F6), Offset(vx - 4f, vy + 1f), Size(5f, 9f))
          }
        }
      }

      // On-foot Player
      if (inCarIndex < 0) {
        val px = playerX - camX
        val py = playerY - camY
        drawCircle(Color(0xFFFFD166), 11f, Offset(px, py))
        drawCircle(Color.White, 5f, Offset(px, py))
      }
    }

    // Vice HUD Top Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = Color(0xCC11161F), shape = RoundedCornerShape(999.dp)) {
        Text(
          text = "$$cash · DROPS $missionCount",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFF4CE8C8),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Surface(color = Color(0xCC11161F), shape = RoundedCornerShape(999.dp)) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(4) { idx ->
            Icon(
              Icons.Rounded.Star,
              contentDescription = "Heat Star",
              tint = if (idx < wantedStars) Color(0xFFFFC857) else Color(0xFF334155),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    Surface(
      color = Color(0xB811161F),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 54.dp),
    ) {
      Text(
        text = statusNote,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFF8FAFC),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
      )
    }

    // Virtual Joystick + Action Controls
    val joystick: @Composable () -> Unit = {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(Color(0x99121722))
            .border(1.5.dp, Color(0x88FF4D8D), CircleShape)
            .pointerInput(Unit) {
              detectDragGestures(
                onDragEnd = { joyX = 0f; joyY = 0f },
                onDragCancel = { joyX = 0f; joyY = 0f },
              ) { change, dragAmount ->
                change.consume()
                joyX = (joyX + dragAmount.x / 40f).coerceIn(-1f, 1f)
                joyY = (joyY + dragAmount.y / 40f).coerceIn(-1f, 1f)
              }
            }
            .testTag("vice_joystick"),
      ) {
        Box(
          modifier =
            Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFFFF4D8D))
        )
      }
    }

    val actions: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        Surface(
          onClick = { sprintBoost = !sprintBoost },
          shape = CircleShape,
          color = if (sprintBoost) Color(0xFF0284C7) else Color(0xCC161C28),
          modifier = Modifier.size(60.dp).border(1.5.dp, Color(0xFF38BDF8), CircleShape).testTag("vice_boost"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FlashOn, contentDescription = "Toggle Turbo Boost", tint = Color.White)
          }
        }
        Surface(
          onClick = { toggleEnterExitCar() },
          shape = CircleShape,
          color = Color(0xCC241522),
          modifier = Modifier.size(68.dp).border(1.5.dp, Color(0xFF4CE8C8), CircleShape).testTag("vice_car"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsCar, contentDescription = "Enter or Exit Vehicle", tint = Color(0xFF4CE8C8))
          }
        }
      }
    }

    Row(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      if (controlConfig.leftHandedHud) {
        actions()
        joystick()
      } else {
        joystick()
        actions()
      }
    }
  }
}

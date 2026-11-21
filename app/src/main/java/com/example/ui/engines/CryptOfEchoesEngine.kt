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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ControlConfig
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

private enum class EntityKind {
  WRAITH,
  KEY,
  CHEST,
  ELIXIR,
  EXIT_PORTAL,
}

private data class DungeonSprite(
  var x: Float,
  var y: Float,
  val kind: EntityKind,
  var hp: Int = 2,
  var alive: Boolean = true,
)

private val DUNGEON_MAPS =
  listOf(
    arrayOf(
      "##########",
      "#..#.....#",
      "#..#.##..#",
      "#........#",
      "###.###..#",
      "#...#....#",
      "#.#.#.##.#",
      "#.#......#",
      "#.....##.#",
      "##########",
    ),
    arrayOf(
      "############",
      "#.....#....#",
      "#.##..#.##.#",
      "#.#........#",
      "#.#.####.#.#",
      "#....#...#.#",
      "###..#.#.#.#",
      "#......#...#",
      "#.##.#####.#",
      "#..........#",
      "############",
    ),
  )

@Composable
fun CryptOfEchoesEngine(
  controlConfig: ControlConfig,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var floorIndex by remember { mutableIntStateOf(0) }
  var playerX by remember { mutableFloatStateOf(1.5f) }
  var playerY by remember { mutableFloatStateOf(1.5f) }
  var playerAngle by remember { mutableFloatStateOf(0f) }
  var hp by remember { mutableIntStateOf(100) }
  var keysHeld by remember { mutableIntStateOf(0) }
  var score by remember { mutableIntStateOf(0) }
  var swordAnim by remember { mutableFloatStateOf(0f) }
  var shieldActive by remember { mutableStateOf(false) }
  var gameOver by remember { mutableStateOf(false) }
  var bannerMsg by remember { mutableStateOf("Find the Golden Key & reach the Emerald Portal") }

  val sprites = remember { mutableStateListOf<DungeonSprite>() }

  fun loadFloor(idx: Int) {
    floorIndex = idx % DUNGEON_MAPS.size
    playerX = 1.5f
    playerY = 1.5f
    playerAngle = 0f
    keysHeld = 0
    sprites.clear()
    if (floorIndex == 0) {
      sprites.addAll(
        listOf(
          DungeonSprite(4.5f, 3.5f, EntityKind.WRAITH, hp = 2),
          DungeonSprite(7.5f, 1.5f, EntityKind.KEY),
          DungeonSprite(8.5f, 5.5f, EntityKind.CHEST),
          DungeonSprite(1.5f, 7.5f, EntityKind.ELIXIR),
          DungeonSprite(6.5f, 7.5f, EntityKind.WRAITH, hp = 3),
          DungeonSprite(8.5f, 8.5f, EntityKind.EXIT_PORTAL),
        )
      )
    } else {
      sprites.addAll(
        listOf(
          DungeonSprite(5.5f, 3.5f, EntityKind.WRAITH, hp = 3),
          DungeonSprite(9.5f, 1.5f, EntityKind.KEY),
          DungeonSprite(1.5f, 7.5f, EntityKind.CHEST),
          DungeonSprite(6.5f, 7.5f, EntityKind.WRAITH, hp = 3),
          DungeonSprite(10.5f, 5.5f, EntityKind.ELIXIR),
          DungeonSprite(10.5f, 9.5f, EntityKind.EXIT_PORTAL),
        )
      )
    }
  }

  LaunchedEffect(Unit) { loadFloor(0) }

  // Holdable button states + keyboard states
  val upInteract = remember { MutableInteractionSource() }
  val downInteract = remember { MutableInteractionSource() }
  val leftInteract = remember { MutableInteractionSource() }
  val rightInteract = remember { MutableInteractionSource() }

  val upPressed by upInteract.collectIsPressedAsState()
  val downPressed by downInteract.collectIsPressedAsState()
  val leftPressed by leftInteract.collectIsPressedAsState()
  val rightPressed by rightInteract.collectIsPressedAsState()

  var kbForward by remember { mutableStateOf(false) }
  var kbBack by remember { mutableStateOf(false) }
  var kbLeft by remember { mutableStateOf(false) }
  var kbRight by remember { mutableStateOf(false) }

  fun isWall(x: Float, y: Float): Boolean {
    val map = DUNGEON_MAPS[floorIndex]
    val mx = x.toInt()
    val my = y.toInt()
    if (my !in map.indices || mx !in map[my].indices) return true
    return map[my][mx] == '#'
  }

  fun performStrike() {
    if (gameOver) return
    swordAnim = 1f
    val hitTarget =
      sprites.firstOrNull { sp ->
        sp.alive && sp.kind == EntityKind.WRAITH && hypot(sp.x - playerX, sp.y - playerY) < 1.8f
      }
    if (hitTarget != null) {
      hitTarget.hp -= 1
      if (hitTarget.hp <= 0) {
        hitTarget.alive = false
        score += 200
        bannerMsg = "Wraith vanquished! +200 Score"
        onAwardXp(45, score, "dungeon_delver")
      } else {
        bannerMsg = "Wraith struck! (${hitTarget.hp} HP left)"
      }
    }
  }

  // Real-time 30Hz Simulation Loop
  LaunchedEffect(gameOver, floorIndex) {
    var tick = 0
    while (!gameOver) {
      delay(33L)
      tick++
      val moveSpeed = 0.085f
      val rotSpeed = 0.068f
      if (leftPressed || kbLeft) playerAngle -= rotSpeed
      if (rightPressed || kbRight) playerAngle += rotSpeed

      val moveDir =
        when {
          upPressed || kbForward -> 1f
          downPressed || kbBack -> -1f
          else -> 0f
        }
      if (moveDir != 0f) {
        val nx = playerX + cos(playerAngle) * moveSpeed * moveDir
        val ny = playerY + sin(playerAngle) * moveSpeed * moveDir
        if (!isWall(nx, playerY)) playerX = nx
        if (!isWall(playerX, ny)) playerY = ny
      }

      if (swordAnim > 0f) swordAnim = (swordAnim - 0.14f).coerceAtLeast(0f)

      // Sprite interactions & Wraith AI
      for (sp in sprites) {
        if (!sp.alive) continue
        val dist = hypot(sp.x - playerX, sp.y - playerY)
        when (sp.kind) {
          EntityKind.KEY ->
            if (dist < 0.7f) {
              sp.alive = false
              keysHeld++
              score += 150
              bannerMsg = "Golden Key collected! Head to the Emerald Portal."
              onAwardXp(30, score, null)
            }
          EntityKind.CHEST ->
            if (dist < 0.7f) {
              sp.alive = false
              score += 250
              bannerMsg = "Ancient Chest looted! +250 Score"
              onAwardXp(40, score, null)
            }
          EntityKind.ELIXIR ->
            if (dist < 0.7f) {
              sp.alive = false
              hp = (hp + 35).coerceAtMost(100)
              bannerMsg = "Elixir restored +35 HP!"
            }
          EntityKind.EXIT_PORTAL ->
            if (dist < 0.75f) {
              if (keysHeld > 0) {
                score += 400
                bannerMsg = "Floor Cleared! Descending deeper..."
                onAwardXp(85, score, "dungeon_delver")
                loadFloor(floorIndex + 1)
                break
              } else {
                bannerMsg = "Portal locked — find the Golden Key first!"
              }
            }
          EntityKind.WRAITH -> {
            if (dist in 0.85f..4.2f) {
              val dx = (playerX - sp.x) / dist
              val dy = (playerY - sp.y) / dist
              val step = 0.025f
              if (!isWall(sp.x + dx * step, sp.y)) sp.x += dx * step
              if (!isWall(sp.x, sp.y + dy * step)) sp.y += dy * step
            }
            if (dist < 0.85f && tick % 24 == 0) {
              val dmg = if (shieldActive) 4 else 12
              hp = (hp - dmg).coerceAtLeast(0)
              bannerMsg = if (shieldActive) "Blocked Wraith strike (-4 HP)" else "Wraith clawed you! (-12 HP)"
              if (hp <= 0) {
                gameOver = true
                onAwardXp(20, score, null)
              }
            }
          }
        }
      }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF07090C))
        .onPreviewKeyEvent { ev ->
          val down = ev.type == KeyEventType.KeyDown
          when (ev.key) {
            Key.W, Key.DirectionUp -> { kbForward = down; true }
            Key.S, Key.DirectionDown -> { kbBack = down; true }
            Key.A, Key.DirectionLeft -> { kbLeft = down; true }
            Key.D, Key.DirectionRight -> { kbRight = down; true }
            Key.Spacebar -> { if (down) performStrike(); true }
            else -> false
          }
        }
  ) {
    // 3D Raycasting Canvas
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val halfH = h / 2f

      // Ceiling & Floor gradients
      drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF07090E), Color(0xFF151923)), 0f, halfH),
        size = Size(w, halfH),
      )
      drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF1A1512), Color(0xFF0A0806)), halfH, h),
        topLeft = Offset(0f, halfH),
        size = Size(w, halfH),
      )

      val numRays = 96
      val fov = (PI / 3.0).toFloat()
      val rayWidth = w / numRays
      val zBuffer = FloatArray(numRays) { 20f }

      for (i in 0 until numRays) {
        val rayAngle = (playerAngle - fov / 2f) + (i.toFloat() / numRays) * fov
        val dirX = cos(rayAngle)
        val dirY = sin(rayAngle)

        var dist = 0.05f
        var hit = false
        var hitX = playerX
        var hitY = playerY
        while (!hit && dist < 14f) {
          dist += 0.04f
          hitX = playerX + dirX * dist
          hitY = playerY + dirY * dist
          if (isWall(hitX, hitY)) hit = true
        }

        val correctedDist = (dist * cos(rayAngle - playerAngle)).coerceAtLeast(0.15f)
        zBuffer[i] = correctedDist
        val wallH = (h / correctedDist).coerceAtMost(h * 1.4f)
        val top = halfH - wallH / 2f

        val shade = (1f - (correctedDist / 10f)).coerceIn(0.12f, 0.95f)
        val isBoundaryEdge = (hitX % 1f < 0.06f || hitY % 1f < 0.06f)
        val baseColor =
          if (isBoundaryEdge) Color(0xFF6B533E) else Color(0xFF3E4554)
        val wallColor =
          Color(
            red = baseColor.red * shade,
            green = baseColor.green * shade,
            blue = baseColor.blue * shade,
            alpha = 1f,
          )
        drawRect(
          color = wallColor,
          topLeft = Offset(i * rayWidth, top),
          size = Size(rayWidth + 1.2f, wallH),
        )
      }

      // 3D Projected Billboards (sorted far-to-near)
      val visibleSprites =
        sprites
          .filter { it.alive }
          .map { it to hypot(it.x - playerX, it.y - playerY) }
          .sortedByDescending { it.second }

      for ((sp, dist) in visibleSprites) {
        if (dist < 0.2f || dist > 11f) continue
        var angleDiff = atan2(sp.y - playerY, sp.x - playerX) - playerAngle
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        if (kotlin.math.abs(angleDiff) > fov * 0.65f) continue

        val screenX = ((angleDiff / fov) + 0.5f) * w
        val rayIdx = ((screenX / w) * numRays).toInt().coerceIn(0, numRays - 1)
        if (dist > zBuffer[rayIdx] + 0.2f) continue

        val spriteSize = (h * 0.52f / dist).coerceIn(16f, h * 0.65f)
        val topY = halfH - spriteSize * 0.45f
        val spColor =
          when (sp.kind) {
            EntityKind.WRAITH -> Color(0xFFFF5277)
            EntityKind.KEY -> Color(0xFFFFD166)
            EntityKind.CHEST -> Color(0xFFD97706)
            EntityKind.ELIXIR -> Color(0xFF38BDF8)
            EntityKind.EXIT_PORTAL -> Color(0xFF34D399)
          }
        drawCircle(
          color = spColor.copy(alpha = 0.24f),
          radius = spriteSize * 0.65f,
          center = Offset(screenX, topY + spriteSize * 0.5f),
        )
        drawRect(
          color = spColor,
          topLeft = Offset(screenX - spriteSize * 0.35f, topY),
          size = Size(spriteSize * 0.7f, spriteSize),
        )
      }

      // First-person Sword Slash Overlay
      val slashOffset = swordAnim * 68f
      drawRect(
        color = if (shieldActive) Color(0xFF60A5FA) else Color(0xFFE2E8F0),
        topLeft = Offset(w * 0.72f - slashOffset, h * 0.62f - slashOffset * 0.5f),
        size = Size(16f, h * 0.28f),
      )

      // Tactical Top-Right Minimap
      val map = DUNGEON_MAPS[floorIndex]
      val cell = 9f
      val mapX = w - map[0].length * cell - 16f
      val mapY = 16f
      drawRect(
        color = Color(0xAA0B0E14),
        topLeft = Offset(mapX - 4f, mapY - 4f),
        size = Size(map[0].length * cell + 8f, map.size * cell + 8f),
      )
      for (r in map.indices) {
        for (c in map[r].indices) {
          if (map[r][c] == '#') {
            drawRect(
              color = Color(0xFF64748B),
              topLeft = Offset(mapX + c * cell, mapY + r * cell),
              size = Size(cell - 1f, cell - 1f),
            )
          }
        }
      }
      for (sp in sprites) {
        if (!sp.alive) continue
        val dotColor = if (sp.kind == EntityKind.WRAITH) Color.Red else Color(0xFF34D399)
        drawCircle(dotColor, 2.8f, Offset(mapX + sp.x * cell, mapY + sp.y * cell))
      }
      drawCircle(Color(0xFFFFD166), 3.4f, Offset(mapX + playerX * cell, mapY + playerY * cell))
    }

    // Top-Left Status Pills
    Column(
      modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(color = Color(0xCC11161F), shape = RoundedCornerShape(999.dp)) {
        Text(
          text = "FL ${floorIndex + 1} · HP $hp · KEY $keysHeld · SCR $score",
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFFE2E8F0),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Surface(color = Color(0xB311161F), shape = RoundedCornerShape(10.dp)) {
        Text(
          text = bannerMsg,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF5CF2D6),
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    // Touch Controls (Respecting Left-Handed Flip)
    val dpadContent: @Composable () -> Unit = {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HoldControlButton(Icons.Rounded.ArrowUpward, "Move Forward", upInteract, Modifier.testTag("crypt_up"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          HoldControlButton(Icons.AutoMirrored.Rounded.ArrowBack, "Turn Left", leftInteract, Modifier.testTag("crypt_left"))
          HoldControlButton(Icons.Rounded.ArrowDownward, "Move Back", downInteract, Modifier.testTag("crypt_down"))
          HoldControlButton(Icons.AutoMirrored.Rounded.ArrowForward, "Turn Right", rightInteract, Modifier.testTag("crypt_right"))
        }
      }
    }

    val combatContent: @Composable () -> Unit = {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        Surface(
          onClick = { shieldActive = !shieldActive },
          shape = CircleShape,
          color = if (shieldActive) Color(0xFF2563EB) else Color(0xCC151C28),
          modifier = Modifier.size(60.dp).border(1.5.dp, Color(0xFF60A5FA), CircleShape).testTag("crypt_shield"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Shield, contentDescription = "Toggle Shield", tint = Color.White)
          }
        }
        Surface(
          onClick = { performStrike() },
          shape = CircleShape,
          color = Color(0xCC24151D),
          modifier = Modifier.size(68.dp).border(1.5.dp, Color(0xFFFF5C7C), CircleShape).testTag("crypt_strike"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FlashOn, contentDescription = "Sword Strike", tint = Color(0xFFFF8AA4))
          }
        }
      }
    }

    Row(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      if (controlConfig.leftHandedHud) {
        combatContent()
        dpadContent()
      } else {
        dpadContent()
        combatContent()
      }
    }

    if (gameOver) {
      Surface(
        color = Color(0xDD0B0E14),
        modifier = Modifier.fillMaxSize(),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text("FALLEN IN THE CRYPT", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFF5C7C))
          Spacer(Modifier.height(8.dp))
          Text("Final Score: $score", style = MaterialTheme.typography.titleLarge, color = Color.White)
          Spacer(Modifier.height(20.dp))
          Button(
            onClick = {
              hp = 100
              score = 0
              gameOver = false
              loadFloor(0)
            }
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Re-enter Crypt")
          }
        }
      }
    }
  }
}

@Composable
private fun HoldControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  desc: String,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(52.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xCC151C28))
        .border(1.dp, Color(0x884CE8C8), RoundedCornerShape(14.dp))
        .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
  ) {
    Icon(icon, contentDescription = desc, tint = Color(0xFF7CF7E1))
  }
}

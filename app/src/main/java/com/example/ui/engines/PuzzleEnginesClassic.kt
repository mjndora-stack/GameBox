package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import kotlin.math.abs
import kotlin.random.Random
import org.json.JSONObject

// 1. PRISM 2048
@Composable
fun Prism2048Game(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  fun spawnTile(b: IntArray): IntArray {
    val empties = b.indices.filter { b[it] == 0 }
    if (empties.isEmpty()) return b
    val copy = b.copyOf()
    copy[empties.random()] = if (Random.nextFloat() < 0.85f) 2 else 4
    return copy
  }

  fun newBoard(): IntArray = spawnTile(spawnTile(IntArray(16)))

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val arr = QuickResumeSerializer.decodeIntArray(obj.optJSONArray("board")) ?: return@runCatching null
      if (arr.size != 16) return@runCatching null
      Triple(arr, obj.optInt("score", 0), obj.optBoolean("ach256", false))
    }.getOrNull()
  }

  var board by remember(savedStateJson) { mutableStateOf(initialSnapshot?.first ?: newBoard()) }
  var score by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.second ?: 0) }
  var achieved256 by remember(savedStateJson) { mutableStateOf(initialSnapshot?.third ?: false) }

  val latestBoard by rememberUpdatedState(board)
  val latestScore by rememberUpdatedState(score)
  val latestAch by rememberUpdatedState(achieved256)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestScore <= 0) null
      else {
        val obj = JSONObject().apply {
          put("board", QuickResumeSerializer.encodeIntArray(latestBoard))
          put("score", latestScore)
          put("ach256", latestAch)
        }
        "Score $latestScore · Max ${latestBoard.maxOrNull() ?: 0}" to obj.toString()
      }
    }
    onDispose {}
  }

  fun slideLine(line: IntArray): Pair<IntArray, Int> {
    val nonZero = line.filter { it != 0 }.toMutableList()
    var gained = 0
    val merged = mutableListOf<Int>()
    var i = 0
    while (i < nonZero.size) {
      if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
        val v = nonZero[i] * 2
        merged.add(v)
        gained += v
        i += 2
      } else {
        merged.add(nonZero[i])
        i += 1
      }
    }
    while (merged.size < 4) merged.add(0)
    return merged.toIntArray() to gained
  }

  fun move(dir: Int) { // 0=left, 1=right, 2=up, 3=down
    val next = IntArray(16)
    var gained = 0
    for (idx in 0 until 4) {
      val line =
        IntArray(4) { j ->
          when (dir) {
            0 -> board[idx * 4 + j]
            1 -> board[idx * 4 + (3 - j)]
            2 -> board[j * 4 + idx]
            else -> board[(3 - j) * 4 + idx]
          }
        }
      val (res, pts) = slideLine(line)
      gained += pts
      for (j in 0 until 4) {
        when (dir) {
          0 -> next[idx * 4 + j] = res[j]
          1 -> next[idx * 4 + (3 - j)] = res[j]
          2 -> next[j * 4 + idx] = res[j]
          else -> next[(3 - j) * 4 + idx] = res[j]
        }
      }
    }
    if (!next.contentEquals(board)) {
      board = spawnTile(next)
      score += gained
      if (gained > 0) {
        val maxTile = board.maxOrNull() ?: 0
        val unlocked256 = !achieved256 && (maxTile >= 256 || score >= 1200)
        val ach = if (unlocked256) { achieved256 = true; "puzzle_2048" } else null
        VaultAudioEngine.playGameSfx(if (unlocked256) GameSfx.VICTORY else GameSfx.PUZZLE_STEP)
        onAwardXp((gained / 6).coerceIn(5, 45), score, ach)
      } else {
        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
      }
    }
  }

  var dragDx = 0f
  var dragDy = 0f

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(16.dp)
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { dragDx = 0f; dragDy = 0f },
            onDragEnd = {
              if (abs(dragDx) > abs(dragDy) && abs(dragDx) > 36f) {
                move(if (dragDx > 0) 1 else 0)
              } else if (abs(dragDy) > 36f) {
                move(if (dragDy > 0) 3 else 2)
              }
            },
          ) { change, amt ->
            change.consume()
            dragDx += amt.x
            dragDy += amt.y
          }
        },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Text("SCORE $score · MAX ${board.maxOrNull() ?: 0}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
      }
      OutlinedButton(onClick = { board = newBoard(); score = 0 }) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(4.dp))
        Text("New")
      }
    }

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until 4) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in 0 until 4) {
              val v = board[r * 4 + c]
              val tileColor =
                when (v) {
                  0 -> MaterialTheme.colorScheme.surface
                  2 -> Color(0xFF1E293B)
                  4 -> Color(0xFF334155)
                  8 -> Color(0xFF0F766E)
                  16 -> Color(0xFF0284C7)
                  32 -> Color(0xFF6366F1)
                  64 -> Color(0xFFDB2777)
                  128 -> Color(0xFFD97706)
                  else -> Color(0xFF16A34A)
                }
              val isHigh = v >= 128
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileColor)
                    .border(
                      width = if (isHigh) 1.5.dp else 1.dp,
                      color = if (isHigh) Color.White.copy(alpha = 0.45f) else if (v > 0) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                      shape = RoundedCornerShape(10.dp),
                    ),
              ) {
                if (v > 0) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                      color = Color.White.copy(alpha = 0.18f),
                      topLeft = Offset(size.width * 0.08f, size.height * 0.06f),
                      size = Size(size.width * 0.84f, size.height * 0.16f),
                      cornerRadius = CornerRadius(6f, 6f),
                    )
                    drawRoundRect(
                      color = Color.Black.copy(alpha = 0.24f),
                      topLeft = Offset(size.width * 0.04f, size.height * 0.82f),
                      size = Size(size.width * 0.92f, size.height * 0.14f),
                      cornerRadius = CornerRadius(6f, 6f),
                    )
                  }
                  Text(
                    text = "$v",
                    color = Color.White,
                    fontSize = if (v >= 100) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Accessible D-Pad buttons for tapping / Chromebook
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = { move(0) }, modifier = Modifier.testTag("p2048_left")) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Left") }
      OutlinedButton(onClick = { move(2) }, modifier = Modifier.testTag("p2048_up")) { Icon(Icons.Rounded.ArrowUpward, "Up") }
      OutlinedButton(onClick = { move(3) }, modifier = Modifier.testTag("p2048_down")) { Icon(Icons.Rounded.ArrowDownward, "Down") }
      OutlinedButton(onClick = { move(1) }, modifier = Modifier.testTag("p2048_right")) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Right") }
    }
  }
}

// 2. FLUX CIRCUIT (Pipe Rotation Puzzle)
private data class PipeCell(val mask: Int, val isSource: Boolean = false, val isSink: Boolean = false)

@Composable
fun FluxCircuitGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  // Bitmask: 1=Up, 2=Right, 4=Down, 8=Left
  fun rotateMask(m: Int): Int = ((m shl 1) or (m ushr 3)) and 0xF

  val baseTemplate = remember {
    listOf(
      PipeCell(2 or 4, isSource = true), PipeCell(2 or 8), PipeCell(4 or 8), PipeCell(2 or 4), PipeCell(4 or 8, isSink = true),
      PipeCell(1 or 4), PipeCell(1 or 2), PipeCell(1 or 2 or 8), PipeCell(1 or 8), PipeCell(1 or 4),
      PipeCell(1 or 2), PipeCell(2 or 4 or 8), PipeCell(2 or 8), PipeCell(4 or 8), PipeCell(1 or 4),
      PipeCell(1 or 4), PipeCell(1 or 4), PipeCell(2 or 4), PipeCell(1 or 2 or 8), PipeCell(1 or 8),
      PipeCell(1 or 2), PipeCell(1 or 8), PipeCell(1 or 2), PipeCell(2 or 8), PipeCell(1 or 8, isSink = true),
    )
  }

  fun buildLevel(level: Int): List<PipeCell> {
    return baseTemplate.mapIndexed { idx, cell ->
      val twists = (idx + level * 2) % 3 + 1
      var m = cell.mask
      if (!cell.isSource) repeat(twists) { m = rotateMask(m) }
      cell.copy(mask = m)
    }
  }

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val masks = QuickResumeSerializer.decodeIntArray(obj.optJSONArray("masks")) ?: return@runCatching null
      if (masks.size != 25) return@runCatching null
      val restoredGrid = baseTemplate.mapIndexed { idx, c -> c.copy(mask = masks[idx]) }
      Triple(obj.optInt("level", 1).coerceAtLeast(1), obj.optInt("moves", 0), restoredGrid)
    }.getOrNull()
  }

  var level by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.first ?: 1) }
  var grid by remember(savedStateJson) { mutableStateOf(initialSnapshot?.third ?: buildLevel(1)) }
  var moves by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.second ?: 0) }
  var won by remember(savedStateJson) { mutableStateOf(false) }

  fun startStage(nextLevel: Int) {
    level = nextLevel
    grid = buildLevel(nextLevel)
    moves = 0
    won = false
  }

  val latestLevel by rememberUpdatedState(level)
  val latestGrid by rememberUpdatedState(grid)
  val latestMoves by rememberUpdatedState(moves)
  val latestWon by rememberUpdatedState(won)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestWon || (latestLevel == 1 && latestMoves == 0)) null
      else {
        val masks = IntArray(25) { i -> latestGrid[i].mask }
        val obj = JSONObject().apply {
          put("level", latestLevel)
          put("moves", latestMoves)
          put("masks", QuickResumeSerializer.encodeIntArray(masks))
        }
        "Stage $latestLevel · $latestMoves moves" to obj.toString()
      }
    }
    onDispose {}
  }

  fun computePowered(cells: List<PipeCell>): Set<Int> {
    val visited = mutableSetOf(0)
    val queue = ArrayDeque<Int>().apply { add(0) }
    val dirs = listOf(Triple(-1, 0, 1 to 4), Triple(0, 1, 2 to 8), Triple(1, 0, 4 to 1), Triple(0, -1, 8 to 2))
    while (queue.isNotEmpty()) {
      val cur = queue.removeFirst()
      val r = cur / 5
      val c = cur % 5
      val m = cells[cur].mask
      for ((dr, dc, bits) in dirs) {
        val nr = r + dr
        val nc = c + dc
        if (nr in 0..4 && nc in 0..4) {
          val nIdx = nr * 5 + nc
          if (nIdx !in visited && (m and bits.first) != 0 && (cells[nIdx].mask and bits.second) != 0) {
            visited.add(nIdx)
            queue.add(nIdx)
          }
        }
      }
    }
    return visited
  }

  val powered = remember(grid) { computePowered(grid) }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("STAGE $level · MOVES $moves", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { grid = buildLevel(level); moves = 0; won = false }) { Text("Reset") }
    }

    Text(
      "Tap tiles to rotate 90° and route power from S to both T terminals.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Surface(
      shape = MaterialTheme.shapes.large,
      color = Color(0xFF0E131B),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until 5) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (c in 0 until 5) {
              val idx = r * 5 + c
              val cell = grid[idx]
              val lit = idx in powered
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (lit) Color(0xFF132836) else Color(0xFF171E29))
                    .border(
                      width = if (lit) 1.dp else 0.5.dp,
                      color = if (lit) Color(0x444CE8C8) else Color(0x2294A3B8),
                      shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(enabled = !won) {
                      val updated = grid.toMutableList()
                      updated[idx] = cell.copy(mask = rotateMask(cell.mask))
                      grid = updated
                      moves++
                      val p = computePowered(updated)
                      val allSinksLit = updated.indices.filter { updated[it].isSink }.all { it in p }
                      if (allSinksLit) {
                        won = true
                        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                        val pts = (600 - moves * 10).coerceAtLeast(250) + level * 100
                        onAwardXp(85, pts, "circuit_master")
                      } else {
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                      }
                    },
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  // Subtle PCB trace grid crosshair
                  drawLine(Color(0x1494A3B8), Offset(cx, 2f), Offset(cx, size.height - 2f), 1.5f)
                  drawLine(Color(0x1494A3B8), Offset(2f, cy), Offset(size.width - 2f, cy), 1.5f)

                  val pipeCol = if (lit) Color(0xFF4CE8C8) else Color(0xFF475569)
                  val glowCol = Color(0x444CE8C8)
                  val m = cell.mask
                  if (lit) {
                    if ((m and 1) != 0) drawLine(glowCol, Offset(cx, cy), Offset(cx, 0f), 18f, StrokeCap.Round)
                    if ((m and 2) != 0) drawLine(glowCol, Offset(cx, cy), Offset(size.width, cy), 18f, StrokeCap.Round)
                    if ((m and 4) != 0) drawLine(glowCol, Offset(cx, cy), Offset(cx, size.height), 18f, StrokeCap.Round)
                    if ((m and 8) != 0) drawLine(glowCol, Offset(cx, cy), Offset(0f, cy), 18f, StrokeCap.Round)
                  }
                  if ((m and 1) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(cx, 0f), 10f, StrokeCap.Round)
                  if ((m and 2) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(size.width, cy), 10f, StrokeCap.Round)
                  if ((m and 4) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(cx, size.height), 10f, StrokeCap.Round)
                  if ((m and 8) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(0f, cy), 10f, StrokeCap.Round)

                  if (cell.isSource || cell.isSink) {
                    val termCol = if (cell.isSource) Color(0xFFFFC857) else Color(0xFF38BDF8)
                    drawCircle(termCol.copy(alpha = 0.25f), 17f, Offset(cx, cy))
                    drawCircle(termCol, 12f, Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.45f), 12f, Offset(cx, cy), style = Stroke(2f))
                  } else {
                    drawCircle(if (lit) Color(0xFFBDFDF0) else Color(0xFF64748B), 7f, Offset(cx, cy))
                  }
                }
                if (cell.isSource) Text("S", color = Color(0xFF0B1018), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                if (cell.isSink) Text("T", color = Color(0xFF0B1018), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }

    if (won) {
      Button(onClick = { startStage(level + 1) }) { Text("Circuit Powered! Next Stage") }
    }
  }
}

// 3. VAULT SWEEPER (Minesweeper 8x8)
@Composable
fun VaultSweeperGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  val sizeN = 8
  val mineCount = 10

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val m = QuickResumeSerializer.decodeBooleanArray(obj.optJSONArray("mines")) ?: return@runCatching null
      val rev = QuickResumeSerializer.decodeBooleanArray(obj.optJSONArray("rev")) ?: return@runCatching null
      val flg = QuickResumeSerializer.decodeBooleanArray(obj.optJSONArray("flag")) ?: return@runCatching null
      if (m.size != 64 || rev.size != 64 || flg.size != 64) return@runCatching null
      Triple(m, rev, flg to obj.optBoolean("flagMode", false))
    }.getOrNull()
  }

  var mines by remember(savedStateJson) { mutableStateOf(initialSnapshot?.first ?: BooleanArray(sizeN * sizeN)) }
  var revealed by remember(savedStateJson) { mutableStateOf(initialSnapshot?.second ?: BooleanArray(sizeN * sizeN)) }
  var flagged by remember(savedStateJson) { mutableStateOf(initialSnapshot?.third?.first ?: BooleanArray(sizeN * sizeN)) }
  var initialized by remember(savedStateJson) { mutableStateOf(initialSnapshot != null) }
  var flagMode by remember(savedStateJson) { mutableStateOf(initialSnapshot?.third?.second ?: false) }
  var lost by remember { mutableStateOf(false) }
  var won by remember { mutableStateOf(false) }

  val latestMines by rememberUpdatedState(mines)
  val latestRevealed by rememberUpdatedState(revealed)
  val latestFlagged by rememberUpdatedState(flagged)
  val latestInit by rememberUpdatedState(initialized)
  val latestFlagMode by rememberUpdatedState(flagMode)
  val latestLost by rememberUpdatedState(lost)
  val latestWon by rememberUpdatedState(won)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (!latestInit || latestLost || latestWon) null
      else {
        val obj = JSONObject().apply {
          put("mines", QuickResumeSerializer.encodeBooleanArray(latestMines))
          put("rev", QuickResumeSerializer.encodeBooleanArray(latestRevealed))
          put("flag", QuickResumeSerializer.encodeBooleanArray(latestFlagged))
          put("flagMode", latestFlagMode)
        }
        "8×8 · ${latestRevealed.count { it }} cleared" to obj.toString()
      }
    }
    onDispose {}
  }

  fun countAdj(m: BooleanArray, idx: Int): Int {
    val r = idx / sizeN
    val c = idx % sizeN
    var cnt = 0
    for (dr in -1..1) for (dc in -1..1) {
      if (dr == 0 && dc == 0) continue
      val nr = r + dr; val nc = c + dc
      if (nr in 0 until sizeN && nc in 0 until sizeN && m[nr * sizeN + nc]) cnt++
    }
    return cnt
  }

  fun revealFlood(start: Int, m: BooleanArray, rev: BooleanArray) {
    val q = ArrayDeque<Int>().apply { add(start) }
    while (q.isNotEmpty()) {
      val cur = q.removeFirst()
      if (rev[cur] || m[cur]) continue
      rev[cur] = true
      if (countAdj(m, cur) == 0) {
        val r = cur / sizeN; val c = cur % sizeN
        for (dr in -1..1) for (dc in -1..1) {
          val nr = r + dr; val nc = c + dc
          if (nr in 0 until sizeN && nc in 0 until sizeN) {
            val nIdx = nr * sizeN + nc
            if (!rev[nIdx]) q.add(nIdx)
          }
        }
      }
    }
  }

  fun resetBoard() {
    mines = BooleanArray(sizeN * sizeN)
    revealed = BooleanArray(sizeN * sizeN)
    flagged = BooleanArray(sizeN * sizeN)
    initialized = false
    lost = false
    won = false
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      FilterChip(
        selected = flagMode,
        onClick = { flagMode = !flagMode },
        leadingIcon = { Icon(Icons.Rounded.Flag, null, modifier = Modifier.size(16.dp)) },
        label = { Text(if (flagMode) "Flag Mode ON" else "Probe Mode") },
      )
      Text("MINES ${mineCount - flagged.count { it }}", style = MaterialTheme.typography.labelMedium)
      OutlinedButton(onClick = { resetBoard() }) { Text("Reset") }
    }

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0 until sizeN) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in 0 until sizeN) {
              val idx = r * sizeN + c
              val isRev = revealed[idx]
              val isFlag = flagged[idx]
              val isMine = mines[idx]
              val adj = if (isRev && !isMine) countAdj(mines, idx) else 0
              val bg =
                when {
                  lost && isMine -> Color(0xFFEF4444)
                  isRev -> MaterialTheme.colorScheme.surface
                  isFlag -> Color(0xFFD97706)
                  else -> Color(0xFF283244)
                }
              val numColor =
                when (adj) {
                  1 -> Color(0xFF38BDF8)
                  2 -> Color(0xFF4ADE80)
                  3 -> Color(0xFFFACC15)
                  else -> Color(0xFFFF7A95)
                }
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(bg)
                    .border(
                      width = 1.dp,
                      color = if (!isRev) Color(0x33FFFFFF) else Color(0x1894A3B8),
                      shape = RoundedCornerShape(5.dp),
                    )
                    .clickable(enabled = !lost && !won) {
                      if (flagMode && !isRev) {
                        val f = flagged.copyOf()
                        f[idx] = !f[idx]
                        flagged = f
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                        return@clickable
                      }
                      if (isFlag) return@clickable
                      var curMines = mines
                      if (!initialized) {
                        val m = BooleanArray(sizeN * sizeN)
                        val candidates = (0 until sizeN * sizeN).filter { it != idx }.shuffled().take(mineCount)
                        candidates.forEach { m[it] = true }
                        curMines = m
                        mines = m
                        initialized = true
                      }
                      if (curMines[idx]) {
                        lost = true
                        VaultAudioEngine.playGameSfx(GameSfx.HIT)
                        onAwardXp(15, revealed.count { it } * 15, null)
                      } else {
                        val nextRev = revealed.copyOf()
                        revealFlood(idx, curMines, nextRev)
                        revealed = nextRev
                        val safeRevealed = nextRev.indices.count { nextRev[it] && !curMines[it] }
                        if (safeRevealed >= sizeN * sizeN - mineCount) {
                          won = true
                          VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                          onAwardXp(110, 1100, "sweeper_clear")
                        } else {
                          VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                        }
                      }
                    },
              ) {
                if (!isRev) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color(0x28FFFFFF), Offset.Zero, Size(size.width, size.height * 0.16f))
                    drawRect(Color(0x30000000), Offset(0f, size.height * 0.84f), Size(size.width, size.height * 0.16f))
                  }
                }
                when {
                  lost && isMine -> {
                    // Vector Naval Spike Mine
                    Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
                      val cx = size.width / 2f; val cy = size.height / 2f
                      val r = size.minDimension * 0.30f
                      drawLine(Color(0xFF0F172A), Offset(cx - r * 1.45f, cy), Offset(cx + r * 1.45f, cy), 3.2f, StrokeCap.Round)
                      drawLine(Color(0xFF0F172A), Offset(cx, cy - r * 1.45f), Offset(cx, cy + r * 1.45f), 3.2f, StrokeCap.Round)
                      drawLine(Color(0xFF0F172A), Offset(cx - r, cy - r), Offset(cx + r, cy + r), 2.8f, StrokeCap.Round)
                      drawLine(Color(0xFF0F172A), Offset(cx + r, cy - r), Offset(cx - r, cy + r), 2.8f, StrokeCap.Round)
                      drawCircle(Color(0xFF0F172A), r, Offset(cx, cy))
                      drawCircle(Color(0xCCFFFFFF), r * 0.3f, Offset(cx - r * 0.32f, cy - r * 0.32f))
                    }
                  }
                  isFlag && !isRev -> Icon(Icons.Rounded.Flag, null, tint = Color.White, modifier = Modifier.size(15.dp))
                  isRev && adj > 0 -> Text("$adj", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = numColor)
                }
              }
            }
          }
        }
      }
    }

    if (won) Text("SECTOR CLEARED! +110 XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
    if (lost) Text("DETONATION! Tap Reset to sweep again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
  }
}

// 4. LUMEN TOGGLE (Lights Out)
@Composable
fun LumenToggleGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  fun makeSolvable(n: Int): BooleanArray {
    val arr = BooleanArray(n * n)
    repeat(n + 3) {
      val pick = Random.nextInt(n * n)
      val r = pick / n; val c = pick % n
      listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dr, dc) ->
        val nr = r + dr; val nc = c + dc
        if (nr in 0 until n && nc in 0 until n) arr[nr * n + nc] = !arr[nr * n + nc]
      }
    }
    if (arr.none { it }) arr[0] = true
    return arr
  }

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val n = obj.optInt("n", 4).let { if (it == 5) 5 else 4 }
      val arr = QuickResumeSerializer.decodeBooleanArray(obj.optJSONArray("board")) ?: return@runCatching null
      if (arr.size != n * n) return@runCatching null
      Triple(n, arr, obj.optInt("moves", 0))
    }.getOrNull()
  }

  var gridN by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.first ?: 4) }
  var board by remember(savedStateJson) { mutableStateOf(initialSnapshot?.second ?: makeSolvable(gridN)) }
  var moves by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.third ?: 0) }
  var solved by remember { mutableStateOf(false) }

  fun startBoard(n: Int) {
    gridN = n
    board = makeSolvable(n)
    moves = 0
    solved = false
  }

  val latestN by rememberUpdatedState(gridN)
  val latestBoard by rememberUpdatedState(board)
  val latestMoves by rememberUpdatedState(moves)
  val latestSolved by rememberUpdatedState(solved)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestSolved || latestMoves <= 0) null
      else {
        val obj = JSONObject().apply {
          put("n", latestN)
          put("moves", latestMoves)
          put("board", QuickResumeSerializer.encodeBooleanArray(latestBoard))
        }
        "${latestN}×${latestN} · $latestMoves moves" to obj.toString()
      }
    }
    onDispose {}
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      FilterChip(selected = gridN == 4, onClick = { startBoard(4) }, label = { Text("4×4") })
      FilterChip(selected = gridN == 5, onClick = { startBoard(5) }, label = { Text("5×5") })
      Spacer(Modifier.weight(1f))
      Text("MOVES $moves", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { startBoard(gridN) }) { Text("New") }
    }

    Text("Turn off every glowing node. Each tap flips the cross around it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until gridN) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in 0 until gridN) {
              val idx = r * gridN + c
              val lit = board[idx]
              Box(
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (lit) Color(0xFF0E3436) else Color(0xFF131924))
                    .border(
                      width = if (lit) 1.5.dp else 1.dp,
                      color = if (lit) Color(0xFF4CE8C8) else Color(0xFF283446),
                      shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(enabled = !solved) {
                      val next = board.copyOf()
                      listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dr, dc) ->
                        val nr = r + dr; val nc = c + dc
                        if (nr in 0 until gridN && nc in 0 until gridN) {
                          val nIdx = nr * gridN + nc
                          next[nIdx] = !next[nIdx]
                        }
                      }
                      board = next
                      moves++
                      if (next.none { it }) {
                        solved = true
                        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                        val score = (800 - moves * 15).coerceAtLeast(300) + gridN * 80
                        onAwardXp(90, score, "logic_solver")
                      } else {
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                      }
                    },
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cx = size.width / 2f; val cy = size.height / 2f
                  val r = size.minDimension * 0.34f
                  if (lit) {
                    drawCircle(
                      brush = Brush.radialGradient(
                        listOf(Color(0x994CE8C8), Color(0x224CE8C8), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.52f,
                      ),
                      radius = size.minDimension * 0.52f,
                      center = Offset(cx, cy),
                    )
                    drawCircle(Color(0xFF4CE8C8), r, Offset(cx, cy))
                    drawCircle(Color(0xFFE0FFF9), r * 0.55f, Offset(cx, cy))
                    drawCircle(Color.White, r * 0.22f, Offset(cx - r * 0.32f, cy - r * 0.32f))
                  } else {
                    drawCircle(Color(0xFF1F2A3A), r * 0.85f, Offset(cx, cy))
                    drawCircle(Color(0xFF334155), r * 0.85f, Offset(cx, cy), style = Stroke(2f))
                    drawCircle(Color(0xFF283548), r * 0.28f, Offset(cx, cy))
                  }
                }
              }
            }
          }
        }
      }
    }

    if (solved) {
      Button(onClick = { board = makeSolvable(gridN); moves = 0; solved = false }) {
        Text("Matrix Extinguished! Next Board")
      }
    }
  }
}

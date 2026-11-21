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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

// 1. PRISM 2048
@Composable
fun Prism2048Game(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  fun spawnTile(b: IntArray): IntArray {
    val empties = b.indices.filter { b[it] == 0 }
    if (empties.isEmpty()) return b
    val copy = b.copyOf()
    copy[empties.random()] = if (Random.nextFloat() < 0.85f) 2 else 4
    return copy
  }

  fun newBoard(): IntArray = spawnTile(spawnTile(IntArray(16)))

  var board by remember { mutableStateOf(newBoard()) }
  var score by remember { mutableIntStateOf(0) }
  var achieved256 by remember { mutableStateOf(false) }

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
        val ach = if (!achieved256 && (maxTile >= 256 || score >= 1200)) { achieved256 = true; "puzzle_2048" } else null
        onAwardXp((gained / 6).coerceIn(5, 45), score, ach)
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
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp)).background(tileColor),
              ) {
                if (v > 0) {
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
) {
  // Bitmask: 1=Up, 2=Right, 4=Down, 8=Left
  fun rotateMask(m: Int): Int = ((m shl 1) or (m ushr 3)) and 0xF

  fun buildLevel(level: Int): List<PipeCell> {
    // 5x5 guaranteed connected path from (0,0) to (4,4) and (0,4)
    val base =
      listOf(
        PipeCell(2 or 4, isSource = true), PipeCell(2 or 8), PipeCell(4 or 8), PipeCell(2 or 4), PipeCell(4 or 8, isSink = true),
        PipeCell(1 or 4), PipeCell(1 or 2), PipeCell(1 or 2 or 8), PipeCell(1 or 8), PipeCell(1 or 4),
        PipeCell(1 or 2), PipeCell(2 or 4 or 8), PipeCell(2 or 8), PipeCell(4 or 8), PipeCell(1 or 4),
        PipeCell(1 or 4), PipeCell(1 or 4), PipeCell(2 or 4), PipeCell(1 or 2 or 8), PipeCell(1 or 8),
        PipeCell(1 or 2), PipeCell(1 or 8), PipeCell(1 or 2), PipeCell(2 or 8), PipeCell(1 or 8, isSink = true),
      )
    return base.mapIndexed { idx, cell ->
      val twists = (idx + level * 2) % 3 + 1
      var m = cell.mask
      if (!cell.isSource) repeat(twists) { m = rotateMask(m) }
      cell.copy(mask = m)
    }
  }

  var level by remember { mutableIntStateOf(1) }
  var grid by remember(level) { mutableStateOf(buildLevel(level)) }
  var moves by remember(level) { mutableIntStateOf(0) }
  var won by remember(level) { mutableStateOf(false) }

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
                    .background(Color(0xFF171E29))
                    .clickable(enabled = !won) {
                      val updated = grid.toMutableList()
                      updated[idx] = cell.copy(mask = rotateMask(cell.mask))
                      grid = updated
                      moves++
                      val p = computePowered(updated)
                      val allSinksLit = updated.indices.filter { updated[it].isSink }.all { it in p }
                      if (allSinksLit) {
                        won = true
                        val pts = (600 - moves * 10).coerceAtLeast(250) + level * 100
                        onAwardXp(85, pts, "circuit_master")
                      }
                    },
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val pipeCol = if (lit) Color(0xFF4CE8C8) else Color(0xFF475569)
                  val m = cell.mask
                  if ((m and 1) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(cx, 0f), 10f, StrokeCap.Round)
                  if ((m and 2) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(size.width, cy), 10f, StrokeCap.Round)
                  if ((m and 4) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(cx, size.height), 10f, StrokeCap.Round)
                  if ((m and 8) != 0) drawLine(pipeCol, Offset(cx, cy), Offset(0f, cy), 10f, StrokeCap.Round)
                  drawCircle(if (cell.isSource) Color(0xFFFFC857) else if (cell.isSink) Color(0xFF38BDF8) else pipeCol, 10f, Offset(cx, cy))
                }
                if (cell.isSource) Text("S", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (cell.isSink) Text("T", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (won) {
      Button(onClick = { level++ }) { Text("Circuit Powered! Next Stage") }
    }
  }
}

// 3. VAULT SWEEPER (Minesweeper 8x8)
@Composable
fun VaultSweeperGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val sizeN = 8
  val mineCount = 10

  var mines by remember { mutableStateOf(BooleanArray(sizeN * sizeN)) }
  var revealed by remember { mutableStateOf(BooleanArray(sizeN * sizeN)) }
  var flagged by remember { mutableStateOf(BooleanArray(sizeN * sizeN)) }
  var initialized by remember { mutableStateOf(false) }
  var flagMode by remember { mutableStateOf(false) }
  var lost by remember { mutableStateOf(false) }
  var won by remember { mutableStateOf(false) }

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
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(bg)
                    .clickable(enabled = !lost && !won) {
                      if (flagMode && !isRev) {
                        val f = flagged.copyOf()
                        f[idx] = !f[idx]
                        flagged = f
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
                        onAwardXp(15, revealed.count { it } * 15, null)
                      } else {
                        val nextRev = revealed.copyOf()
                        revealFlood(idx, curMines, nextRev)
                        revealed = nextRev
                        val safeRevealed = nextRev.indices.count { nextRev[it] && !curMines[it] }
                        if (safeRevealed >= sizeN * sizeN - mineCount) {
                          won = true
                          onAwardXp(110, 1100, "sweeper_clear")
                        }
                      }
                    },
              ) {
                when {
                  lost && isMine -> Text("*", color = Color.White, fontWeight = FontWeight.Bold)
                  isFlag && !isRev -> Icon(Icons.Rounded.Flag, null, tint = Color.White, modifier = Modifier.size(14.dp))
                  isRev && adj > 0 -> Text("$adj", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
) {
  var gridN by remember { mutableIntStateOf(4) }

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

  var board by remember(gridN) { mutableStateOf(makeSolvable(gridN)) }
  var moves by remember(gridN) { mutableIntStateOf(0) }
  var solved by remember(gridN) { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      FilterChip(selected = gridN == 4, onClick = { gridN = 4 }, label = { Text("4×4") })
      FilterChip(selected = gridN == 5, onClick = { gridN = 5 }, label = { Text("5×5") })
      Spacer(Modifier.weight(1f))
      Text("MOVES $moves", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { board = makeSolvable(gridN); moves = 0; solved = false }) { Text("New") }
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
                    .background(if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
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
                        val score = (800 - moves * 15).coerceAtLeast(300) + gridN * 80
                        onAwardXp(90, score, "logic_solver")
                      }
                    },
              )
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

package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

// 5. CHROMA FLOOD (Color Territory Flood)
@Composable
fun ChromaFloodGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val n = 10
  val maxMoves = 22
  val palette = listOf(
    Color(0xFF4CE8C8),
    Color(0xFFFF5C7C),
    Color(0xFFFFC857),
    Color(0xFF38BDF8),
    Color(0xFFA78BFA),
    Color(0xFF4ADE80),
  )

  fun randomGrid(): IntArray = IntArray(n * n) { Random.nextInt(palette.size) }

  var grid by remember { mutableStateOf(randomGrid()) }
  var moves by remember { mutableIntStateOf(0) }
  var won by remember { mutableStateOf(false) }

  fun flood(targetColor: Int) {
    val startColor = grid[0]
    if (startColor == targetColor || won || moves >= maxMoves) return
    val next = grid.copyOf()
    val visited = BooleanArray(n * n)
    val q = ArrayDeque<Int>().apply { add(0); visited[0] = true }
    while (q.isNotEmpty()) {
      val cur = q.removeFirst()
      next[cur] = targetColor
      val r = cur / n; val c = cur % n
      listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).forEach { (dr, dc) ->
        val nr = r + dr; val nc = c + dc
        if (nr in 0 until n && nc in 0 until n) {
          val ni = nr * n + nc
          if (!visited[ni] && grid[ni] == startColor) {
            visited[ni] = true
            q.add(ni)
          }
        }
      }
    }
    grid = next
    moves++
    if (next.all { it == targetColor }) {
      won = true
      val pts = 700 + (maxMoves - moves) * 40
      onAwardXp(95, pts, "logic_solver")
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("MOVES $moves / $maxMoves", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { grid = randomGrid(); moves = 0; won = false }) { Text("New Mosaic") }
    }

    Surface(
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(4.dp)) {
        for (r in 0 until n) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (c in 0 until n) {
              Box(modifier = Modifier.weight(1f).fillMaxSize().background(palette[grid[r * n + c]]))
            }
          }
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      palette.forEachIndexed { idx, color ->
        Box(
          modifier =
            Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(color)
              .border(2.dp, if (grid[0] == idx) Color.White else Color.Transparent, CircleShape)
              .clickable { flood(idx) },
        )
      }
    }

    if (won) Text("MOSAIC UNIFIED! +95 XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
    else if (moves >= maxMoves) Text("Out of moves! Tap New Mosaic.", color = MaterialTheme.colorScheme.error)
  }
}

// 6. NONOGRID (5x5 Picross)
@Composable
fun NonoGridGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val patterns = remember {
    listOf(
      "Rocket" to intArrayOf(
        0, 0, 1, 0, 0,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
        1, 1, 0, 1, 1,
        1, 0, 1, 0, 1,
      ),
      "Key" to intArrayOf(
        0, 1, 1, 0, 0,
        1, 0, 0, 1, 0,
        0, 1, 1, 0, 0,
        0, 0, 1, 0, 0,
        0, 0, 1, 1, 0,
      ),
      "Crown" to intArrayOf(
        1, 0, 1, 0, 1,
        1, 1, 1, 1, 1,
        1, 1, 1, 1, 1,
        0, 1, 1, 1, 0,
        0, 1, 1, 1, 0,
      ),
    )
  }

  var stage by remember { mutableIntStateOf(0) }
  val (name, target) = patterns[stage % patterns.size]
  var marks by remember(stage) { mutableStateOf(IntArray(25)) } // 0=empty, 1=fill, 2=X
  var markXMode by remember { mutableStateOf(false) }
  var solved by remember(stage) { mutableStateOf(false) }

  fun clueFor(seq: List<Int>): String {
    val runs = mutableListOf<Int>()
    var cur = 0
    for (v in seq) {
      if (v == 1) cur++ else if (cur > 0) { runs.add(cur); cur = 0 }
    }
    if (cur > 0) runs.add(cur)
    return if (runs.isEmpty()) "0" else runs.joinToString(" ")
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      FilterChip(selected = markXMode, onClick = { markXMode = !markXMode }, label = { Text(if (markXMode) "Mark X" else "Fill Cell") })
      Text("PUZZLE ${stage % patterns.size + 1}/3", style = MaterialTheme.typography.labelMedium)
      OutlinedButton(onClick = { marks = IntArray(25); solved = false }) { Text("Clear") }
    }

    // Column clues + grid
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
          Spacer(Modifier.size(44.dp))
          for (c in 0 until 5) {
            val colClue = clueFor((0 until 5).map { r -> target[r * 5 + c] }).replace(" ", "\n")
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
              Text(colClue, style = MaterialTheme.typography.labelSmall, lineHeight = 13.sp)
            }
          }
        }
        for (r in 0 until 5) {
          val rowClue = clueFor((0 until 5).map { c -> target[r * 5 + c] })
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(40.dp, 46.dp), contentAlignment = Alignment.CenterEnd) {
              Text(rowClue, style = MaterialTheme.typography.labelSmall)
            }
            for (c in 0 until 5) {
              val idx = r * 5 + c
              val state = marks[idx]
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when (state) {
                        1 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                      }
                    )
                    .clickable(enabled = !solved) {
                      val next = marks.copyOf()
                      val targetVal = if (markXMode) 2 else 1
                      next[idx] = if (next[idx] == targetVal) 0 else targetVal
                      marks = next
                      val matches = (0 until 25).all { i -> (next[i] == 1) == (target[i] == 1) }
                      if (matches) {
                        solved = true
                        onAwardXp(90, 850 + stage * 100, "logic_solver")
                      }
                    },
              ) {
                if (state == 2) Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (solved) {
      Button(onClick = { stage++ }) { Text("Revealed \"$name\"! Next Emblem") }
    }
  }
}

// 7. SLIDE-15 (Guaranteed Solvable 4x4 Sliding Puzzle)
@Composable
fun Slide15Game(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  fun createSolvable(): IntArray {
    val arr = IntArray(16) { if (it == 15) 0 else it + 1 }
    var empty = 15
    repeat(90) {
      val r = empty / 4; val c = empty % 4
      val neighbors = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        .map { (dr, dc) -> r + dr to c + dc }
        .filter { (nr, nc) -> nr in 0..3 && nc in 0..3 }
        .map { (nr, nc) -> nr * 4 + nc }
      val pick = neighbors.random()
      arr[empty] = arr[pick]
      arr[pick] = 0
      empty = pick
    }
    return arr
  }

  var tiles by remember { mutableStateOf(createSolvable()) }
  var moves by remember { mutableIntStateOf(0) }
  var won by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("MOVES $moves", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { tiles = createSolvable(); moves = 0; won = false }) { Text("Shuffle") }
    }

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until 4) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (c in 0 until 4) {
              val idx = r * 4 + c
              val v = tiles[idx]
              val inPlace = v != 0 && v == idx + 1
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      when {
                        v == 0 -> Color.Transparent
                        inPlace -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surface
                      }
                    )
                    .clickable(enabled = v != 0 && !won) {
                      val empty = tiles.indexOf(0)
                      val er = empty / 4; val ec = empty % 4
                      if (abs(er - r) + abs(ec - c) == 1) {
                        val next = tiles.copyOf()
                        next[empty] = v
                        next[idx] = 0
                        tiles = next
                        moves++
                        val isSolved = (0 until 15).all { next[it] == it + 1 }
                        if (isSolved) {
                          won = true
                          onAwardXp(100, (1400 - moves * 8).coerceAtLeast(400), "logic_solver")
                        }
                      }
                    },
              ) {
                if (v != 0) {
                  Text(
                    "$v",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (inPlace) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }
    }

    if (won) Text("SEQUENCE RESTORED! +100 XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
  }
}

// 8. TOWER OF HANOI
@Composable
fun HanoiVaultGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var diskCount by remember { mutableIntStateOf(3) }
  var pegs by remember(diskCount) {
    mutableStateOf(listOf((diskCount downTo 1).toList(), emptyList(), emptyList()))
  }
  var selectedPeg by remember(diskCount) { mutableIntStateOf(-1) }
  var moves by remember(diskCount) { mutableIntStateOf(0) }
  var won by remember(diskCount) { mutableStateOf(false) }

  val parMoves = (1 shl diskCount) - 1

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      listOf(3, 4, 5).forEach { count ->
        FilterChip(selected = diskCount == count, onClick = { diskCount = count }, label = { Text("$count Disks") })
      }
      Spacer(Modifier.weight(1f))
      Text("MOVES $moves (PAR $parMoves)", style = MaterialTheme.typography.labelSmall)
    }

    Text("Move the entire stack to Pillar C. Never place a wider disk atop a narrower one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(260.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        for (p in 0 until 3) {
          val isSel = selectedPeg == p
          Column(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface)
                .border(1.5.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable(enabled = !won) {
                  if (selectedPeg == -1) {
                    if (pegs[p].isNotEmpty()) selectedPeg = p
                  } else if (selectedPeg == p) {
                    selectedPeg = -1
                  } else {
                    val src = pegs[selectedPeg]
                    val dst = pegs[p]
                    val disk = src.lastOrNull()
                    val topDst = dst.lastOrNull()
                    if (disk != null && (topDst == null || disk < topDst)) {
                      val next = pegs.mapIndexed { idx, list ->
                        when (idx) {
                          selectedPeg -> list.dropLast(1)
                          p -> list + disk
                          else -> list
                        }
                      }
                      pegs = next
                      moves++
                      selectedPeg = -1
                      if (next[2].size == diskCount) {
                        won = true
                        val pts = 700 + diskCount * 200 - (moves - parMoves).coerceAtLeast(0) * 15
                        onAwardXp(80 + diskCount * 15, pts.coerceAtLeast(350), "logic_solver")
                      }
                    } else {
                      selectedPeg = -1
                    }
                  }
                }
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
          ) {
            pegs[p].reversed().forEach { diskSize ->
              val frac = 0.35f + (diskSize.toFloat() / diskCount.toFloat()) * 0.6f
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth(frac)
                    .height(24.dp)
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f + diskSize * 0.08f)),
              )
            }
            Spacer(Modifier.height(6.dp))
            Text("Pillar ${'A' + p}", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    if (won) {
      Button(onClick = {
        pegs = listOf((diskCount downTo 1).toList(), emptyList(), emptyList())
        moves = 0; selectedPeg = -1; won = false
      }) {
        Text("Stack Transferred! Play Again")
      }
    }
  }
}

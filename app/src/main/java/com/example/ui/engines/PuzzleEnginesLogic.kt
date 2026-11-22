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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import kotlin.math.abs
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

// 5. CHROMA FLOOD (Color Territory Flood)
@Composable
fun ChromaFloodGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
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

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val arr = QuickResumeSerializer.decodeIntArray(obj.optJSONArray("grid")) ?: return@runCatching null
      if (arr.size != n * n) return@runCatching null
      arr to obj.optInt("moves", 0)
    }.getOrNull()
  }

  var grid by remember(savedStateJson) { mutableStateOf(initialSnapshot?.first ?: randomGrid()) }
  var moves by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.second ?: 0) }
  var won by remember { mutableStateOf(false) }

  val latestGrid by rememberUpdatedState(grid)
  val latestMoves by rememberUpdatedState(moves)
  val latestWon by rememberUpdatedState(won)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestWon || latestMoves <= 0 || latestMoves >= maxMoves) null
      else {
        val obj = JSONObject().apply {
          put("grid", QuickResumeSerializer.encodeIntArray(latestGrid))
          put("moves", latestMoves)
        }
        "Moves $latestMoves/$maxMoves" to obj.toString()
      }
    }
    onDispose {}
  }

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
      VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
      val pts = 700 + (maxMoves - moves) * 40
      onAwardXp(95, pts, "logic_solver")
    } else {
      VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
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
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
      color = Color(0xFF0E131B),
      modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        for (r in 0 until n) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
            for (c in 0 until n) {
              val cellCol = palette[grid[r * n + c]]
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(cellCol),
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  drawRect(Color.White.copy(alpha = 0.22f), Offset.Zero, Size(size.width, size.height * 0.16f))
                  drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, size.height * 0.84f), Size(size.width, size.height * 0.16f))
                  if (r == 0 && c == 0) {
                    drawCircle(Color.White, size.minDimension * 0.22f, center, style = Stroke(2f))
                  }
                }
              }
            }
          }
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      palette.forEachIndexed { idx, color ->
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(color)
              .border(2.5.dp, if (grid[0] == idx) Color.White else Color(0x33FFFFFF), CircleShape)
              .clickable { flood(idx) },
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
              Color.White.copy(alpha = 0.45f),
              size.minDimension * 0.15f,
              Offset(size.width * 0.34f, size.height * 0.32f),
            )
          }
        }
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
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
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

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val arr = QuickResumeSerializer.decodeIntArray(obj.optJSONArray("marks")) ?: return@runCatching null
      if (arr.size != 25) return@runCatching null
      Triple(obj.optInt("stage", 0).coerceAtLeast(0), arr, obj.optBoolean("markX", false))
    }.getOrNull()
  }

  var stage by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.first ?: 0) }
  val (name, target) = patterns[stage % patterns.size]
  var marks by remember(savedStateJson) { mutableStateOf(initialSnapshot?.second ?: IntArray(25)) } // 0=empty, 1=fill, 2=X
  var markXMode by remember(savedStateJson) { mutableStateOf(initialSnapshot?.third ?: false) }
  var solved by remember { mutableStateOf(false) }

  fun nextNonoStage(nextStage: Int) {
    stage = nextStage
    marks = IntArray(25)
    solved = false
  }

  val latestStage by rememberUpdatedState(stage)
  val latestMarks by rememberUpdatedState(marks)
  val latestMarkX by rememberUpdatedState(markXMode)
  val latestSolved by rememberUpdatedState(solved)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestSolved || latestMarks.all { it == 0 }) null
      else {
        val obj = JSONObject().apply {
          put("stage", latestStage)
          put("marks", QuickResumeSerializer.encodeIntArray(latestMarks))
          put("markX", latestMarkX)
        }
        "Puzzle ${latestStage % patterns.size + 1}/3" to obj.toString()
      }
    }
    onDispose {}
  }

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
                        else -> Color(0xFF1A2230)
                      }
                    )
                    .border(
                      1.dp,
                      if (state == 1) Color(0x44FFFFFF) else Color(0x2894A3B8),
                      RoundedCornerShape(6.dp),
                    )
                    .clickable(enabled = !solved) {
                      val next = marks.copyOf()
                      val targetVal = if (markXMode) 2 else 1
                      next[idx] = if (next[idx] == targetVal) 0 else targetVal
                      marks = next
                      val matches = (0 until 25).all { i -> (next[i] == 1) == (target[i] == 1) }
                      if (matches) {
                        solved = true
                        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                        onAwardXp(90, 850 + stage * 100, "logic_solver")
                      } else {
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                      }
                    },
              ) {
                if (state == 1) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color.White.copy(alpha = 0.25f), Offset.Zero, Size(size.width, size.height * 0.16f))
                  }
                } else if (state == 2) {
                  Canvas(modifier = Modifier.fillMaxSize(0.5f)) {
                    drawLine(Color(0xFF64748B), Offset.Zero, Offset(size.width, size.height), 3f)
                    drawLine(Color(0xFF64748B), Offset(size.width, 0f), Offset(0f, size.height), 3f)
                  }
                }
              }
            }
          }
        }
      }
    }

    if (solved) {
      Button(onClick = { nextNonoStage(stage + 1) }) { Text("Revealed \"$name\"! Next Emblem") }
    }
  }
}

// 7. SLIDE-15 (Guaranteed Solvable 4x4 Sliding Puzzle)
@Composable
fun Slide15Game(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
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

  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val arr = QuickResumeSerializer.decodeIntArray(obj.optJSONArray("tiles")) ?: return@runCatching null
      if (arr.size != 16) return@runCatching null
      arr to obj.optInt("moves", 0)
    }.getOrNull()
  }

  var tiles by remember(savedStateJson) { mutableStateOf(initialSnapshot?.first ?: createSolvable()) }
  var moves by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.second ?: 0) }
  var won by remember { mutableStateOf(false) }

  val latestTiles by rememberUpdatedState(tiles)
  val latestMoves by rememberUpdatedState(moves)
  val latestWon by rememberUpdatedState(won)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestWon || latestMoves <= 0) null
      else {
        val obj = JSONObject().apply {
          put("tiles", QuickResumeSerializer.encodeIntArray(latestTiles))
          put("moves", latestMoves)
        }
        "Moves $latestMoves" to obj.toString()
      }
    }
    onDispose {}
  }

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
                        v == 0 -> Color(0xFF0C1018)
                        inPlace -> Color(0xFF0D7C6E)
                        else -> Color(0xFF1E293B)
                      }
                    )
                    .border(
                      width = if (inPlace) 1.5.dp else 1.dp,
                      color = when {
                        v == 0 -> Color(0x1494A3B8)
                        inPlace -> Color(0xFF4CE8C8)
                        else -> Color(0x3094A3B8)
                      },
                      shape = RoundedCornerShape(10.dp),
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
                          VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                          onAwardXp(100, (1400 - moves * 8).coerceAtLeast(400), "logic_solver")
                        } else {
                          VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                        }
                      }
                    },
              ) {
                if (v != 0) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(Color.White.copy(alpha = 0.16f), Offset.Zero, Size(size.width, size.height * 0.15f))
                    drawRect(Color.Black.copy(alpha = 0.28f), Offset(0f, size.height * 0.85f), Size(size.width, size.height * 0.15f))
                  }
                  Text(
                    "$v",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (inPlace) Color(0xFFE0FFF9) else Color(0xFFE2E8F0),
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
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  val initialSnapshot = remember(savedStateJson) {
    if (savedStateJson.isNullOrBlank()) null
    else runCatching {
      val obj = JSONObject(savedStateJson)
      val count = obj.optInt("disks", 3).coerceIn(3, 5)
      val pArr = obj.getJSONArray("pegs")
      if (pArr.length() != 3) return@runCatching null
      val pegs = (0 until 3).map { p ->
        val inner = pArr.getJSONArray(p)
        (0 until inner.length()).map { idx -> inner.getInt(idx) }
      }
      if (pegs.sumOf { it.size } != count) return@runCatching null
      Triple(count, pegs, obj.optInt("moves", 0))
    }.getOrNull()
  }

  var diskCount by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.first ?: 3) }
  var pegs by remember(savedStateJson) {
    mutableStateOf(initialSnapshot?.second ?: listOf((diskCount downTo 1).toList(), emptyList(), emptyList()))
  }
  var selectedPeg by remember(savedStateJson) { mutableIntStateOf(-1) }
  var moves by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.third ?: 0) }
  var won by remember(savedStateJson) { mutableStateOf(false) }

  fun startHanoi(count: Int) {
    diskCount = count
    pegs = listOf((count downTo 1).toList(), emptyList(), emptyList())
    selectedPeg = -1
    moves = 0
    won = false
  }

  val latestCount by rememberUpdatedState(diskCount)
  val latestPegs by rememberUpdatedState(pegs)
  val latestMoves by rememberUpdatedState(moves)
  val latestWon by rememberUpdatedState(won)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestWon || latestMoves <= 0) null
      else {
        val obj = JSONObject().apply {
          put("disks", latestCount)
          put("moves", latestMoves)
          val pArr = JSONArray()
          latestPegs.forEach { list ->
            val inner = JSONArray()
            list.forEach { inner.put(it) }
            pArr.put(inner)
          }
          put("pegs", pArr)
        }
        "$latestCount Disks · $latestMoves moves" to obj.toString()
      }
    }
    onDispose {}
  }

  val parMoves = (1 shl diskCount) - 1

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      listOf(3, 4, 5).forEach { count ->
        FilterChip(selected = diskCount == count, onClick = { startHanoi(count) }, label = { Text("$count Disks") })
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
        val diskColors = listOf(
          Color(0xFF4CE8C8),
          Color(0xFF38BDF8),
          Color(0xFFFFC857),
          Color(0xFFA78BFA),
          Color(0xFFFF5C7C),
        )
        for (p in 0 until 3) {
          val isSel = selectedPeg == p
          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color(0xFF121822))
                .border(1.5.dp, if (isSel) MaterialTheme.colorScheme.primary else Color(0x3394A3B8), RoundedCornerShape(14.dp))
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
                        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                        val pts = 700 + diskCount * 200 - (moves - parMoves).coerceAtLeast(0) * 15
                        onAwardXp(80 + diskCount * 15, pts.coerceAtLeast(350), "logic_solver")
                      } else {
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                      }
                    } else {
                      selectedPeg = -1
                    }
                  }
                }
                .padding(10.dp),
          ) {
            // Brushed metallic rod & base pedestal behind stack
            Canvas(modifier = Modifier.fillMaxSize()) {
              val rodW = 10f
              val baseY = size.height - 24.dp.toPx()
              drawRoundRect(
                color = if (isSel) Color(0xFF64748B) else Color(0xFF334155),
                topLeft = Offset((size.width - rodW) * 0.5f, 18f),
                size = Size(rodW, baseY - 18f),
                cornerRadius = CornerRadius(5f, 5f),
              )
              drawRoundRect(
                color = Color(0xFF475569),
                topLeft = Offset(4f, baseY - 4f),
                size = Size(size.width - 8f, 6f),
                cornerRadius = CornerRadius(3f, 3f),
              )
            }
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
            ) {
              pegs[p].reversed().forEach { diskSize ->
                val frac = 0.36f + (diskSize.toFloat() / diskCount.toFloat()) * 0.58f
                val dCol = diskColors[(diskSize - 1) % diskColors.size]
                Box(
                  modifier =
                    Modifier
                      .fillMaxWidth(frac)
                      .height(24.dp)
                      .padding(vertical = 2.dp)
                      .clip(RoundedCornerShape(999.dp))
                      .background(dCol)
                      .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(999.dp)),
                ) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                      Color.White.copy(alpha = 0.35f),
                      Offset(size.width * 0.08f, 2f),
                      Size(size.width * 0.84f, size.height * 0.25f),
                      CornerRadius(999f, 999f),
                    )
                  }
                }
              }
              Spacer(Modifier.height(6.dp))
              Text("Pillar ${'A' + p}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
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

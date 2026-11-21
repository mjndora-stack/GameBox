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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import kotlin.math.abs
import kotlin.random.Random

// 9. CIPHER MASTERMIND (Enthusiast Rank Unlock)
@Composable
fun CipherMastermindGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = listOf(
    Color(0xFF4CE8C8),
    Color(0xFFFF5C7C),
    Color(0xFFFFC857),
    Color(0xFF38BDF8),
    Color(0xFFA78BFA),
    Color(0xFF4ADE80),
  )

  fun makeSecret(): List<Int> = List(4) { Random.nextInt(tokens.size) }

  var secret by remember { mutableStateOf(makeSecret()) }
  val history = remember { mutableStateListOf<Triple<List<Int>, Int, Int>>() } // guess, exact, misplaced
  var draft by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
  var activeSlot by remember { mutableIntStateOf(0) }
  var won by remember { mutableStateOf(false) }

  fun submitGuess() {
    if (won || history.size >= 8) return
    var exact = 0
    val secLeft = mutableListOf<Int>()
    val gusLeft = mutableListOf<Int>()
    for (i in 0 until 4) {
      if (draft[i] == secret[i]) exact++
      else {
        secLeft.add(secret[i])
        gusLeft.add(draft[i])
      }
    }
    var near = 0
    for (g in gusLeft) {
      if (secLeft.remove(g)) near++
    }
    history.add(Triple(draft, exact, near))
    if (exact == 4) {
      won = true
      VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
      val pts = 1000 + (8 - history.size) * 120
      onAwardXp(140, pts, null)
    } else if (history.size >= 8) {
      VaultAudioEngine.playGameSfx(GameSfx.HIT)
    } else {
      VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
    }
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("ATTEMPTS ${history.size}/8", style = MaterialTheme.typography.labelLarge)
      OutlinedButton(onClick = { secret = makeSecret(); history.clear(); won = false }) { Text("New Code") }
    }

    // Guess History
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (history.isEmpty()) {
          Text("Green = exact position · Amber = right color, wrong slot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        history.forEachIndexed { idx, (guess, exact, near) ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("#${idx + 1}", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              guess.forEach { tIdx ->
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(tokens[tIdx]))
              }
            }
            Text("$exact Exact · $near Near", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Draft Slots
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      draft.forEachIndexed { slot, tokenIdx ->
        Box(
          modifier =
            Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(tokens[tokenIdx])
              .border(3.dp, if (activeSlot == slot) Color.White else Color.Transparent, CircleShape)
              .clickable { activeSlot = slot },
        )
      }
    }

    // Token Picker
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      tokens.forEachIndexed { tIdx, color ->
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(color)
              .clickable {
                draft = draft.toMutableList().also { it[activeSlot] = tIdx }
                activeSlot = (activeSlot + 1) % 4
              },
        )
      }
    }

    Button(onClick = { submitGuess() }, enabled = !won && history.size < 8) { Text("Submit Guess") }

    if (won) Text("CIPHER CRACKED! +140 XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
    else if (history.size >= 8) Text("Code locked out! Tap New Code.", color = MaterialTheme.colorScheme.error)
  }
}

// 10. SUDOKU SIX (6x6 with 2x3 boxes — Expert Rank Unlock)
@Composable
fun SudokuSixGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val puzzles = remember {
    listOf(
      intArrayOf(
        0, 2, 0, 4, 0, 6,
        4, 0, 6, 0, 2, 0,
        0, 3, 0, 5, 0, 1,
        5, 0, 1, 0, 3, 0,
        0, 4, 0, 6, 0, 2,
        6, 0, 2, 0, 4, 0,
      ),
      intArrayOf(
        1, 0, 3, 0, 5, 0,
        0, 5, 0, 1, 0, 3,
        0, 1, 0, 3, 0, 5,
        3, 0, 5, 0, 1, 0,
        0, 3, 0, 5, 0, 1,
        5, 0, 1, 0, 3, 0,
      ),
    )
  }

  var level by remember { mutableIntStateOf(0) }
  val fixed = puzzles[level % puzzles.size]
  var board by remember(level) { mutableStateOf(fixed.copyOf()) }
  var selected by remember(level) { mutableIntStateOf(-1) }
  var solved by remember(level) { mutableStateOf(false) }

  fun hasConflict(arr: IntArray, idx: Int): Boolean {
    val v = arr[idx]
    if (v == 0) return false
    val r = idx / 6; val c = idx % 6
    for (i in 0 until 6) {
      if (i != c && arr[r * 6 + i] == v) return true
      if (i != r && arr[i * 6 + c] == v) return true
    }
    val br = (r / 2) * 2; val bc = (c / 3) * 3
    for (dr in 0..1) for (dc in 0..2) {
      val ni = (br + dr) * 6 + (bc + dc)
      if (ni != idx && arr[ni] == v) return true
    }
    return false
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("BOARD ${level % puzzles.size + 1}/2 · 2×3 REGIONS", style = MaterialTheme.typography.labelMedium)
      OutlinedButton(onClick = { board = fixed.copyOf(); selected = -1; solved = false }) { Text("Reset") }
    }

    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0 until 6) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in 0 until 6) {
              val idx = r * 6 + c
              val v = board[idx]
              val isFixed = fixed[idx] != 0
              val isSel = selected == idx
              val conflict = hasConflict(board, idx)
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                      when {
                        conflict -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                        isSel -> MaterialTheme.colorScheme.primaryContainer
                        isFixed -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                      }
                    )
                    .border(
                      width = if (isSel) 2.dp else 0.5.dp,
                      color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                      shape = RoundedCornerShape(4.dp),
                    )
                    .clickable(enabled = !isFixed && !solved) { selected = idx },
              ) {
                if (v != 0) {
                  Text(
                    "$v",
                    fontWeight = if (isFixed) FontWeight.Bold else FontWeight.Medium,
                    color = if (conflict) MaterialTheme.colorScheme.error else if (isFixed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
          if (r == 1 || r == 3) Spacer(Modifier.height(3.dp))
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      (1..6).forEach { digit ->
        Button(
          onClick = {
            if (selected >= 0 && fixed[selected] == 0 && !solved) {
              val next = board.copyOf()
              next[selected] = digit
              board = next
              if (next.none { it == 0 } && next.indices.none { hasConflict(next, it) }) {
                solved = true
                VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                onAwardXp(160, 1400 + level * 200, null)
              } else {
                VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
              }
            }
          },
          modifier = Modifier.size(48.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Text("$digit") }
      }
      OutlinedButton(
        onClick = {
          if (selected >= 0 && fixed[selected] == 0 && !solved) {
            board = board.copyOf().also { it[selected] = 0 }
          }
        },
        modifier = Modifier.height(48.dp),
      ) { Text("Clr") }
    }

    if (solved) {
      Button(onClick = { level++ }) { Text("Sudoku Solved! Next Board") }
    }
  }
}

// 11. CRATE PUSHER (Sokoban — Master Rank Unlock)
@Composable
fun CrateSokobanGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rooms = remember {
    listOf(
      listOf(
        "#######",
        "#.. . #",
        "# $$  #",
        "# .@$ #",
        "#   ###",
        "#######",
      ),
      listOf(
        "########",
        "#   .  #",
        "# $#$  #",
        "#  @ . #",
        "#  $ . #",
        "#  #####",
        "########",
      ),
    )
  }

  var roomIdx by remember { mutableIntStateOf(0) }
  val template = rooms[roomIdx % rooms.size]
  val h = template.size
  val w = template[0].length

  data class SokoState(val px: Int, val py: Int, val crates: Set<Pair<Int, Int>>)

  fun initialState(): SokoState {
    var px = 1; var py = 1
    val c = mutableSetOf<Pair<Int, Int>>()
    for (r in 0 until h) for (col in 0 until w) {
      when (template[r][col]) {
        '@' -> { px = col; py = r }
        '$' -> c.add(col to r)
      }
    }
    return SokoState(px, py, c)
  }

  val goals = remember(roomIdx) {
    buildSet {
      for (r in 0 until h) for (col in 0 until w) {
        if (template[r][col] == '.') add(col to r)
      }
    }
  }

  var state by remember(roomIdx) { mutableStateOf(initialState()) }
  var facingDir by remember(roomIdx) { mutableStateOf(1 to 0) }
  var stepToggle by remember(roomIdx) { mutableStateOf(false) }
  val undoStack = remember(roomIdx) { mutableStateListOf<SokoState>() }
  var won by remember(roomIdx) { mutableStateOf(false) }

  fun step(dx: Int, dy: Int) {
    if (won) return
    facingDir = dx to dy
    stepToggle = !stepToggle
    val nx = state.px + dx
    val ny = state.py + dy
    if (ny !in 0 until h || nx !in 0 until w || template[ny][nx] == '#') return
    if ((nx to ny) in state.crates) {
      val cx = nx + dx
      val cy = ny + dy
      if (cy !in 0 until h || cx !in 0 until w || template[cy][cx] == '#' || (cx to cy) in state.crates) return
      undoStack.add(state)
      val nextCrates = state.crates - (nx to ny) + (cx to cy)
      state = SokoState(nx, ny, nextCrates)
      if (goals.all { it in nextCrates }) {
        won = true
        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
        onAwardXp(190, 1800 + roomIdx * 250, null)
      } else {
        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
      }
    } else {
      undoStack.add(state)
      state = state.copy(px = nx, py = ny)
      VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("WAREHOUSE ${roomIdx % rooms.size + 1}/2", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { if (undoStack.isNotEmpty()) state = undoStack.removeAt(undoStack.lastIndex) }) {
          Icon(Icons.AutoMirrored.Rounded.Undo, null, modifier = Modifier.size(16.dp))
        }
        OutlinedButton(onClick = { state = initialState(); undoStack.clear(); won = false }) { Text("Reset") }
      }
    }

    Surface(
      shape = MaterialTheme.shapes.large,
      color = Color(0xFF11161F),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0 until h) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in 0 until w) {
              val isWall = template[r][c] == '#'
              val isGoal = (c to r) in goals
              val isCrate = (c to r) in state.crates
              val isPlayer = state.px == c && state.py == r
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                      when {
                        isWall -> Color(0xFF334155)
                        isGoal -> Color(0xFF163228)
                        else -> Color(0xFF19202C)
                      }
                    ),
              ) {
                when {
                  isPlayer -> {
                    // Articulated Warehouse Bot Character Model
                    Canvas(modifier = Modifier.fillMaxSize(0.86f)) {
                      val wC = size.width; val hC = size.height
                      val stride = if (stepToggle) 2.5f else -2.5f
                      // Feet
                      drawRoundRect(Color(0xFF0F172A), Offset(wC * 0.24f, hC * 0.78f + stride), Size(wC * 0.20f, hC * 0.16f), CornerRadius(4f))
                      drawRoundRect(Color(0xFF0F172A), Offset(wC * 0.56f, hC * 0.78f - stride), Size(wC * 0.20f, hC * 0.16f), CornerRadius(4f))
                      // Suit Torso & Arms
                      drawRoundRect(Color(0xFF0EA5E9), Offset(wC * 0.20f, hC * 0.42f), Size(wC * 0.60f, hC * 0.40f), CornerRadius(7f))
                      // Hard Hat & Face Visor looking in facingDir
                      drawArc(Color(0xFFFACC15), 180f, 180f, true, Offset(wC * 0.22f, hC * 0.08f), Size(wC * 0.56f, hC * 0.40f))
                      drawRoundRect(Color(0xFF1E293B), Offset(wC * 0.26f, hC * 0.26f), Size(wC * 0.48f, hC * 0.22f), CornerRadius(5f))
                      val eyeShift = facingDir.first * (wC * 0.08f)
                      val eyeY = hC * 0.36f + facingDir.second * (hC * 0.04f)
                      drawCircle(Color(0xFF4CE8C8), wC * 0.06f, Offset(wC * 0.42f + eyeShift, eyeY))
                      drawCircle(Color(0xFF4CE8C8), wC * 0.06f, Offset(wC * 0.58f + eyeShift, eyeY))
                    }
                  }
                  isCrate -> {
                    // Sculpted Cargo Crate Model with X-Bracing
                    Canvas(modifier = Modifier.fillMaxSize(0.86f)) {
                      val base = if (isGoal) Color(0xFF22C55E) else Color(0xFFD97706)
                      val frame = if (isGoal) Color(0xFF15803D) else Color(0xFF92400E)
                      drawRoundRect(base, Offset.Zero, size, CornerRadius(6f))
                      drawRoundRect(frame, Offset.Zero, size, CornerRadius(6f), style = Stroke(width = 3.5f))
                      drawLine(frame, Offset(3f, 3f), Offset(size.width - 3f, size.height - 3f), 3f)
                      drawLine(frame, Offset(size.width - 3f, 3f), Offset(3f, size.height - 3f), 3f)
                    }
                  }
                  isGoal -> Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CE8C8)))
                }
              }
            }
          }
        }
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedButton(onClick = { step(0, -1) }) { Icon(Icons.Rounded.ArrowUpward, "Up") }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { step(-1, 0) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Left") }
        OutlinedButton(onClick = { step(0, 1) }) { Icon(Icons.Rounded.ArrowDownward, "Down") }
        OutlinedButton(onClick = { step(1, 0) }) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Right") }
      }
    }

    if (won) {
      Button(onClick = { roomIdx++ }) { Text("Room Cleared! Next Warehouse") }
    }
  }
}

// 12. QUEENS N-GRID (N-Queens Chess Logic — Legend Rank Unlock)
@Composable
fun QueensGridGame(
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var boardN by remember { mutableIntStateOf(6) }
  var queens by remember(boardN) { mutableStateOf(emptySet<Pair<Int, Int>>()) }
  var won by remember(boardN) { mutableStateOf(false) }

  fun isAttacked(r: Int, c: Int): Boolean =
    queens.any { (qr, qc) ->
      !(qr == r && qc == c) && (qr == r || qc == c || abs(qr - r) == abs(qc - c))
    }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      FilterChip(selected = boardN == 6, onClick = { boardN = 6 }, label = { Text("6×6") })
      FilterChip(selected = boardN == 8, onClick = { boardN = 8 }, label = { Text("8×8") })
      Spacer(Modifier.weight(1f))
      Text("QUEENS ${queens.size}/$boardN", style = MaterialTheme.typography.labelMedium)
      OutlinedButton(onClick = { queens = emptySet(); won = false }) { Text("Clear") }
    }

    Text("Place $boardN Queens so none attack along any row, column, or diagonal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Surface(
      shape = MaterialTheme.shapes.large,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      Column(modifier = Modifier.padding(4.dp)) {
        for (r in 0 until boardN) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (c in 0 until boardN) {
              val hasQ = (r to c) in queens
              val darkSq = (r + c) % 2 == 1
              val clash = hasQ && isAttacked(r, c)
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(
                      when {
                        clash -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                        darkSq -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                      }
                    )
                    .clickable(enabled = !won) {
                      val next = if (hasQ) queens - (r to c) else queens + (r to c)
                      queens = next
                      if (next.size == boardN && next.none { (qr, qc) ->
                          next.any { (or, oc) -> !(or == qr && oc == qc) && (or == qr || oc == qc || abs(or - qr) == abs(oc - qc)) }
                        }
                      ) {
                        won = true
                        VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
                        onAwardXp(240, boardN * 350, null)
                      } else {
                        VaultAudioEngine.playGameSfx(GameSfx.PUZZLE_STEP)
                      }
                    },
              ) {
                if (hasQ) {
                  val crownFill = if (clash) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.primary
                  val crownRim = if (clash) Color.White else MaterialTheme.colorScheme.onPrimary
                  Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
                    val wC = size.width; val hC = size.height
                    val crown = Path().apply {
                      moveTo(wC * 0.16f, hC * 0.80f)
                      lineTo(wC * 0.10f, hC * 0.28f)
                      lineTo(wC * 0.34f, hC * 0.46f)
                      lineTo(wC * 0.50f, hC * 0.14f)
                      lineTo(wC * 0.66f, hC * 0.46f)
                      lineTo(wC * 0.90f, hC * 0.28f)
                      lineTo(wC * 0.84f, hC * 0.80f)
                      close()
                    }
                    drawPath(crown, crownFill)
                    drawPath(crown, crownRim, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(crownRim, wC * 0.06f, Offset(wC * 0.50f, hC * 0.14f))
                  }
                }
              }
            }
          }
        }
      }
    }

    if (won) {
      Button(onClick = { queens = emptySet(); won = false }) {
        Text("$boardN-Queens Solved! +240 XP")
      }
    }
  }
}

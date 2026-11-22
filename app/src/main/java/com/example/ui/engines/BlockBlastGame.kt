package com.example.ui.engines

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BlockShape(
  val id: String,
  val cells: List<Pair<Int, Int>>, // (rowOffset, colOffset)
  val colorHex: Long,
) {
  val rows: Int = (cells.maxOfOrNull { it.first } ?: 0) + 1
  val cols: Int = (cells.maxOfOrNull { it.second } ?: 0) + 1
}

private val BLOCK_SHAPES: List<BlockShape> =
  listOf(
    BlockShape("dot", listOf(0 to 0), 0xFF4CE8C8),
    BlockShape("h2", listOf(0 to 0, 0 to 1), 0xFF38BDF8),
    BlockShape("v2", listOf(0 to 0, 1 to 0), 0xFF38BDF8),
    BlockShape("h3", listOf(0 to 0, 0 to 1, 0 to 2), 0xFFA78BFA),
    BlockShape("v3", listOf(0 to 0, 1 to 0, 2 to 0), 0xFFA78BFA),
    BlockShape("h4", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), 0xFF60A5FA),
    BlockShape("v4", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), 0xFF60A5FA),
    BlockShape("h5", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4), 0xFFFF884D),
    BlockShape("v5", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0), 0xFFFF884D),
    BlockShape("sq2", listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), 0xFFFACC15),
    BlockShape("sq3", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2), 0xFFFF5C7C),
    BlockShape("l2_tl", listOf(0 to 0, 0 to 1, 1 to 0), 0xFF34D399),
    BlockShape("l2_tr", listOf(0 to 0, 0 to 1, 1 to 1), 0xFF34D399),
    BlockShape("l2_bl", listOf(0 to 0, 1 to 0, 1 to 1), 0xFF34D399),
    BlockShape("l2_br", listOf(0 to 1, 1 to 0, 1 to 1), 0xFF34D399),
    BlockShape("big_l1", listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2), 0xFFF472B6),
    BlockShape("big_l2", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0), 0xFFF472B6),
    BlockShape("t_up", listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2), 0xFFC084FC),
    BlockShape("t_down", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1), 0xFFC084FC),
    BlockShape("z_shape", listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2), 0xFFFB7185),
    BlockShape("s_shape", listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1), 0xFF2DD4BF),
  )

object BlockBlastLogic {
  const val GRID_SIZE = 8

  fun emptyGrid(): List<List<Long>> = List(GRID_SIZE) { List(GRID_SIZE) { 0L } }

  fun canPlace(
    grid: List<List<Long>>,
    shape: BlockShape,
    anchorRow: Int,
    anchorCol: Int,
  ): Boolean {
    for ((dr, dc) in shape.cells) {
      val r = anchorRow + dr
      val c = anchorCol + dc
      if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return false
      if (grid[r][c] != 0L) return false
    }
    return true
  }

  fun anyValidPlacement(grid: List<List<Long>>, shape: BlockShape): Boolean {
    for (r in 0 until GRID_SIZE) {
      for (c in 0 until GRID_SIZE) {
        if (canPlace(grid, shape, r, c)) return true
      }
    }
    return false
  }

  fun previewClearedLines(
    grid: List<List<Long>>,
    shape: BlockShape,
    anchorRow: Int,
    anchorCol: Int,
  ): Pair<Set<Int>, Set<Int>> {
    if (!canPlace(grid, shape, anchorRow, anchorCol)) return emptySet<Int>() to emptySet()
    val placed = shape.cells.map { (dr, dc) -> (anchorRow + dr) to (anchorCol + dc) }.toSet()
    val fullRows =
      (0 until GRID_SIZE).filter { r ->
        (0 until GRID_SIZE).all { c -> grid[r][c] != 0L || (r to c) in placed }
      }.toSet()
    val fullCols =
      (0 until GRID_SIZE).filter { c ->
        (0 until GRID_SIZE).all { r -> grid[r][c] != 0L || (r to c) in placed }
      }.toSet()
    return fullRows to fullCols
  }

  data class PlacementResult(
    val nextGrid: List<List<Long>>,
    val clearedRows: Set<Int>,
    val clearedCols: Set<Int>,
    val pointsEarned: Int,
  )

  fun placeAndClear(
    grid: List<List<Long>>,
    shape: BlockShape,
    anchorRow: Int,
    anchorCol: Int,
    comboStreak: Int,
  ): PlacementResult {
    val mutable = grid.map { it.toMutableList() }
    for ((dr, dc) in shape.cells) {
      mutable[anchorRow + dr][anchorCol + dc] = shape.colorHex
    }
    val fullRows = (0 until GRID_SIZE).filter { r -> mutable[r].all { it != 0L } }.toSet()
    val fullCols = (0 until GRID_SIZE).filter { c -> (0 until GRID_SIZE).all { r -> mutable[r][c] != 0L } }.toSet()

    fullRows.forEach { r ->
      for (c in 0 until GRID_SIZE) mutable[r][c] = 0L
    }
    fullCols.forEach { c ->
      for (r in 0 until GRID_SIZE) mutable[r][c] = 0L
    }

    val linesCleared = fullRows.size + fullCols.size
    val placementPts = shape.cells.size * 10
    val blastPts =
      if (linesCleared == 0) 0
      else {
        val baseLine = linesCleared * linesCleared * 90
        val comboBonus = (comboStreak + 1) * 45 * linesCleared
        baseLine + comboBonus
      }

    return PlacementResult(
      nextGrid = mutable.map { it.toList() },
      clearedRows = fullRows,
      clearedCols = fullCols,
      pointsEarned = placementPts + blastPts,
    )
  }

  fun dealHand(grid: List<List<Long>>, rng: Random = Random.Default): List<BlockShape> {
    val candidates = BLOCK_SHAPES.shuffled(rng)
    val chosen = mutableListOf<BlockShape>()
    // Guarantee at least one placeable piece if the board is not near-dead to keep Endless Play snappy
    val firstFit = candidates.firstOrNull { anyValidPlacement(grid, it) } ?: candidates.first()
    chosen += firstFit
    while (chosen.size < 3) {
      val next = BLOCK_SHAPES[rng.nextInt(BLOCK_SHAPES.size)]
      chosen += next
    }
    return chosen.shuffled(rng)
  }

  fun rescueBottomRows(grid: List<List<Long>>): List<List<Long>> {
    val mutable = grid.map { it.toMutableList() }
    for (r in 3..4) {
      for (c in 0 until GRID_SIZE) {
        mutable[r][c] = 0L
      }
    }
    return mutable.map { it.toList() }
  }
}

private data class SavedBlockBlastSnapshot(
  val grid: List<List<Long>>,
  val tray: List<BlockShape?>,
  val score: Int,
  val bestScore: Int,
  val linesTotal: Int,
  val comboStreak: Int,
  val endlessZenMode: Boolean,
)

private fun parseBlockBlastSnapshot(json: String?): SavedBlockBlastSnapshot? {
  if (json.isNullOrBlank()) return null
  return runCatching {
    val obj = JSONObject(json)
    val gridArr = obj.getJSONArray("grid")
    if (gridArr.length() != BlockBlastLogic.GRID_SIZE) return null
    val parsedGrid = List(BlockBlastLogic.GRID_SIZE) { r ->
      val rowArr = gridArr.getJSONArray(r)
      List(BlockBlastLogic.GRID_SIZE) { c -> rowArr.optLong(c, 0L) }
    }
    val trayArr = obj.getJSONArray("tray")
    val parsedTray = (0 until trayArr.length()).map { i ->
      val id = trayArr.optString(i, "")
      if (id.isEmpty()) null else BLOCK_SHAPES.firstOrNull { it.id == id }
    }
    SavedBlockBlastSnapshot(
      grid = parsedGrid,
      tray = if (parsedTray.size == 3 && parsedTray.any { it != null }) parsedTray else BlockBlastLogic.dealHand(parsedGrid),
      score = obj.optInt("score", 0),
      bestScore = obj.optInt("best", 0),
      linesTotal = obj.optInt("lines", 0),
      comboStreak = obj.optInt("combo", 0),
      endlessZenMode = obj.optBoolean("zen", false),
    )
  }.getOrNull()
}

@Composable
fun BlockBlastGame(
  initialBestScore: Int = 0,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  val initialSnapshot = remember(savedStateJson) { parseBlockBlastSnapshot(savedStateJson) }
  var grid by remember(savedStateJson) { mutableStateOf(initialSnapshot?.grid ?: BlockBlastLogic.emptyGrid()) }
  var tray by remember(savedStateJson) { mutableStateOf<List<BlockShape?>>(initialSnapshot?.tray ?: BlockBlastLogic.dealHand(grid)) }
  var selectedSlot by remember(savedStateJson) { mutableStateOf<Int?>(tray.indexOfFirst { it != null }.takeIf { it >= 0 }) }
  var score by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.score ?: 0) }
  var bestScore by remember(savedStateJson) { mutableIntStateOf(maxOf(initialBestScore, initialSnapshot?.bestScore ?: 0)) }
  var linesTotal by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.linesTotal ?: 0) }
  var comboStreak by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.comboStreak ?: 0) }
  var endlessZenMode by remember(savedStateJson) { mutableStateOf(initialSnapshot?.endlessZenMode ?: false) }
  var isGameOver by remember { mutableStateOf(false) }
  var statusBanner by remember(savedStateJson) {
    mutableStateOf(
      if (initialSnapshot != null) "Resumed at ${initialSnapshot.score} pts (${initialSnapshot.linesTotal} lines)!"
      else "Place blocks to clear full rows or columns!"
    )
  }
  var achievementUnlocked by remember { mutableStateOf(false) }

  // Dragging & preview coordinates in root space
  var containerRootPos by remember { mutableStateOf(Offset.Zero) }
  var boardRootPos by remember { mutableStateOf(Offset.Zero) }
  var boardPixelSize by remember { mutableStateOf(Size.Zero) }
  var draggingSlot by remember { mutableStateOf<Int?>(null) }
  var dragRootOffset by remember { mutableStateOf(Offset.Zero) }
  var hoverAnchor by remember { mutableStateOf<Pair<Int, Int>?>(null) }

  // Blast flash animation state
  var lastBlastRows by remember { mutableStateOf<Set<Int>>(emptySet()) }
  var lastBlastCols by remember { mutableStateOf<Set<Int>>(emptySet()) }
  val blastAnim = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val currentGrid by rememberUpdatedState(grid)
  val currentTray by rememberUpdatedState(tray)
  val currentScore by rememberUpdatedState(score)
  val currentBest by rememberUpdatedState(bestScore)
  val currentLines by rememberUpdatedState(linesTotal)
  val currentCombo by rememberUpdatedState(comboStreak)
  val currentZen by rememberUpdatedState(endlessZenMode)
  val currentGameOver by rememberUpdatedState(isGameOver)

  LaunchedEffect(initialBestScore) {
    if (initialBestScore > bestScore) bestScore = initialBestScore
  }

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      val hasProgress = currentScore > 0 || currentGrid.any { row -> row.any { it != 0L } }
      if (currentGameOver || !hasProgress) {
        null
      } else {
        val obj = JSONObject().apply {
          val gridArr = JSONArray()
          currentGrid.forEach { row ->
            val rowArr = JSONArray()
            row.forEach { cell -> rowArr.put(cell) }
            gridArr.put(rowArr)
          }
          put("grid", gridArr)
          val trayArr = JSONArray()
          currentTray.forEach { shape -> trayArr.put(shape?.id ?: "") }
          put("tray", trayArr)
          put("score", currentScore)
          put("best", currentBest)
          put("lines", currentLines)
          put("combo", currentCombo)
          put("zen", currentZen)
        }
        "Score $currentScore · $currentLines lines" to obj.toString()
      }
    }
    onDispose {}
  }

  fun resetRun() {
    val fresh = BlockBlastLogic.emptyGrid()
    grid = fresh
    tray = BlockBlastLogic.dealHand(fresh)
    selectedSlot = 0
    score = 0
    linesTotal = 0
    comboStreak = 0
    isGameOver = false
    statusBanner = "Endless Run started — build combos!"
    lastBlastRows = emptySet()
    lastBlastCols = emptySet()
    VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
  }

  fun commitPlacement(slotIdx: Int, anchorRow: Int, anchorCol: Int): Boolean {
    val shape = currentTray.getOrNull(slotIdx) ?: return false
    if (!BlockBlastLogic.canPlace(currentGrid, shape, anchorRow, anchorCol)) return false

    val prevBest = bestScore
    val res = BlockBlastLogic.placeAndClear(currentGrid, shape, anchorRow, anchorCol, comboStreak)
    grid = res.nextGrid
    score += res.pointsEarned
    val clearedLines = res.clearedRows.size + res.clearedCols.size

    if (clearedLines > 0) {
      comboStreak += 1
      linesTotal += clearedLines
      lastBlastRows = res.clearedRows
      lastBlastCols = res.clearedCols
      scope.launch {
        blastAnim.snapTo(1f)
        blastAnim.animateTo(0f, animationSpec = tween(360))
        lastBlastRows = emptySet()
        lastBlastCols = emptySet()
      }
      statusBanner =
        when {
          clearedLines >= 3 -> "MEGA BLAST! $clearedLines lines (+${res.pointsEarned} PTS)"
          comboStreak >= 2 -> "COMBO x$comboStreak! (+${res.pointsEarned} PTS)"
          else -> "LINE BLAST! (+${res.pointsEarned} PTS)"
        }
      VaultAudioEngine.playGameSfx(if (clearedLines >= 2 || comboStreak >= 2) GameSfx.VICTORY else GameSfx.BLOCK_BLAST)
    } else {
      comboStreak = 0
      statusBanner = "Placed cube cluster (+${res.pointsEarned} PTS)"
      VaultAudioEngine.playGameSfx(GameSfx.BLOCK_PLACE)
    }

    val isNewBest = !endlessZenMode && score > prevBest
    if (!endlessZenMode && score > bestScore) {
      bestScore = score
      if (prevBest > 0 && isNewBest) {
        VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
      }
    }

    // Award progressive XP & update Best Score continuously cleanly
    val xpGain = 8 + clearedLines * 24 + (if (comboStreak >= 2) comboStreak * 6 else 0)
    val achId =
      if (!endlessZenMode && !achievementUnlocked && score >= 1000) {
        achievementUnlocked = true
        "block_blast_master"
      } else null
    onAwardXp(xpGain, if (endlessZenMode) null else score, achId)

    // Remove piece from tray and deal next 3 when empty
    val nextTray = currentTray.toMutableList()
    nextTray[slotIdx] = null
    val refilled =
      if (nextTray.all { it == null }) {
        BlockBlastLogic.dealHand(res.nextGrid)
      } else {
        nextTray
      }
    tray = refilled
    selectedSlot = refilled.indexOfFirst { it != null }.takeIf { it >= 0 }

    // Check remaining valid moves across remaining pieces
    val remaining = refilled.filterNotNull()
    val hasMove = remaining.any { BlockBlastLogic.anyValidPlacement(res.nextGrid, it) }
    if (!hasMove) {
      if (endlessZenMode) {
        // Zen Endless auto-clears center lanes so play never ends
        val rescued = BlockBlastLogic.rescueBottomRows(res.nextGrid)
        grid = rescued
        statusBanner = "Zen Shockwave cleared center lanes — Endless Play continues!"
        VaultAudioEngine.playGameSfx(GameSfx.BLOCK_BLAST)
      } else {
        isGameOver = true
        statusBanner = "No space remaining! Final Score: $score"
        VaultAudioEngine.playGameSfx(if (score >= prevBest && score > 0) GameSfx.VICTORY else GameSfx.HIT)
      }
    }
    return true
  }

  val density = LocalDensity.current
  val dragLiftPx = with(density) { 56.dp.toPx() }

  fun computeHoverAnchor(rootPos: Offset, shape: BlockShape): Pair<Int, Int>? {
    if (boardPixelSize.width <= 0f || boardPixelSize.height <= 0f) return null
    val cellSize = boardPixelSize.width / BlockBlastLogic.GRID_SIZE
    val relX = rootPos.x - boardRootPos.x
    val relY = (rootPos.y - dragLiftPx) - boardRootPos.y
    val anchorCol = ((relX / cellSize) - shape.cols * 0.5f).roundToInt()
    val anchorRow = ((relY / cellSize) - shape.rows * 0.5f).roundToInt()
    return if (BlockBlastLogic.canPlace(grid, shape, anchorRow, anchorCol)) anchorRow to anchorCol else null
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .padding(14.dp)
        .onGloballyPositioned { containerRootPos = it.positionInRoot() },
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Score, Best Score, and Endless Mode Strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("block_blast_score"),
              )
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(14.dp))
                Text("BEST", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
              }
              Text(
                text = "$bestScore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("block_blast_best"),
              )
            }
            Column {
              Text("LINES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("$linesTotal", style = MaterialTheme.typography.titleMedium)
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          FilterChip(
            selected = endlessZenMode,
            onClick = {
              VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
              endlessZenMode = !endlessZenMode
              statusBanner =
                if (endlessZenMode) "Zen Endless ON · Auto-shockwave if jammed"
                else "Classic Endless ON · Official High Score mode"
            },
            leadingIcon = { Icon(Icons.Rounded.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text(if (endlessZenMode) "Zen ∞" else "Endless") },
            modifier = Modifier.testTag("block_blast_endless_toggle"),
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { resetRun() }.testTag("block_blast_restart_btn"),
          ) {
            Icon(
              Icons.Rounded.Refresh,
              contentDescription = "Restart Endless Run",
              modifier = Modifier.padding(10.dp).size(20.dp),
            )
          }
        }
      }

      Text(
        text = statusBanner,
        style = MaterialTheme.typography.labelMedium,
        color = if (comboStreak >= 2) Color(0xFFFFC857) else MaterialTheme.colorScheme.primary,
      )

      // 8x8 Block Blast Board
      BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        val maxSide = minOf(maxWidth, maxHeight)
        val previewShape = draggingSlot?.let { tray.getOrNull(it) }
        val previewAnchor = hoverAnchor
        val previewCells =
          if (previewShape != null && previewAnchor != null) {
            previewShape.cells.map { (dr, dc) -> (previewAnchor.first + dr) to (previewAnchor.second + dc) }.toSet()
          } else {
            emptySet()
          }
        val (previewRows, previewCols) =
          if (previewShape != null && previewAnchor != null) {
            BlockBlastLogic.previewClearedLines(grid, previewShape, previewAnchor.first, previewAnchor.second)
          } else {
            emptySet<Int>() to emptySet()
          }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0D131F),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.size(maxSide),
        ) {
          Canvas(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(8.dp)
                .onGloballyPositioned { coords ->
                  boardRootPos = coords.positionInRoot()
                  boardPixelSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                }
                .pointerInput(selectedSlot, isGameOver) {
                  detectTapGestures { offset ->
                    if (isGameOver) return@detectTapGestures
                    val slot = selectedSlot ?: return@detectTapGestures
                    val shape = currentTray.getOrNull(slot) ?: return@detectTapGestures
                    val cell = size.width / BlockBlastLogic.GRID_SIZE.toFloat()
                    val tappedCol = (offset.x / cell).toInt()
                    val tappedRow = (offset.y / cell).toInt()
                    // Try exact tap or centered shape fallback
                    val placed =
                      commitPlacement(slot, tappedRow, tappedCol) ||
                        commitPlacement(slot, tappedRow - shape.rows / 2, tappedCol - shape.cols / 2)
                    if (!placed) {
                      VaultAudioEngine.playGameSfx(GameSfx.HIT)
                      statusBanner = "Can't fit there — pick an open spot or another block"
                    }
                  }
                }
                .testTag("block_blast_board"),
          ) {
            val cellSize = size.width / BlockBlastLogic.GRID_SIZE
            val gap = cellSize * 0.08f
            val corner = CornerRadius(cellSize * 0.16f)

            for (r in 0 until BlockBlastLogic.GRID_SIZE) {
              for (c in 0 until BlockBlastLogic.GRID_SIZE) {
                val x = c * cellSize + gap * 0.5f
                val y = r * cellSize + gap * 0.5f
                val blockSize = Size(cellSize - gap, cellSize - gap)
                val filledHex = grid[r][c]
                val isPreviewCell = (r to c) in previewCells
                val isWillClear = r in previewRows || c in previewCols
                val isBlastFlash = (r in lastBlastRows || c in lastBlastCols) && blastAnim.value > 0f

                when {
                  isBlastFlash -> {
                    drawRoundRect(
                      color = Color(0xFFFDE047).copy(alpha = blastAnim.value),
                      topLeft = Offset(x, y),
                      size = blockSize,
                      cornerRadius = corner,
                    )
                  }
                  isPreviewCell && previewShape != null -> {
                    drawRoundRect(
                      color = Color(previewShape.colorHex).copy(alpha = 0.65f),
                      topLeft = Offset(x, y),
                      size = blockSize,
                      cornerRadius = corner,
                    )
                    drawRoundRect(
                      color = Color.White.copy(alpha = 0.7f),
                      topLeft = Offset(x, y),
                      size = blockSize,
                      cornerRadius = corner,
                      style = Stroke(2f),
                    )
                  }
                  filledHex != 0L -> {
                    val col = Color(filledHex)
                    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(x, y + 2.5f), blockSize, corner)
                    drawRoundRect(col, Offset(x, y), blockSize, corner)
                    // Glossy top specular catchlight & lower bevel shadow
                    drawRoundRect(
                      Color.White.copy(alpha = 0.34f),
                      Offset(x + 2.5f, y + 2f),
                      Size((blockSize.width - 5f).coerceAtLeast(1f), (blockSize.height * 0.26f).coerceAtLeast(1f)),
                      corner,
                    )
                    drawRoundRect(
                      Color.Black.copy(alpha = 0.22f),
                      Offset(x + 2f, y + blockSize.height * 0.74f),
                      Size((blockSize.width - 4f).coerceAtLeast(1f), (blockSize.height * 0.22f).coerceAtLeast(1f)),
                      corner,
                    )
                    if (isWillClear) {
                      drawRoundRect(Color.White.copy(alpha = 0.78f), Offset(x, y), blockSize, corner, style = Stroke(3.2f))
                    }
                  }
                  else -> {
                    drawRoundRect(
                      color = if ((r + c) % 2 == 0) Color(0xFF172030) else Color(0xFF131B29),
                      topLeft = Offset(x, y),
                      size = blockSize,
                      cornerRadius = corner,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 3-Slot Piece Tray (supports both Drag-and-Drop and Tap-to-Select)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tray.forEachIndexed { idx, shape ->
            val isSelected = selectedSlot == idx && shape != null
            val canFit = shape != null && BlockBlastLogic.anyValidPlacement(grid, shape)
            var slotRootPos by remember { mutableStateOf(Offset.Zero) }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                when {
                  isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                  shape == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                  else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
              border =
                BorderStroke(
                  if (isSelected) 2.dp else 1.dp,
                  if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier
                  .weight(1f)
                  .height(92.dp)
                  .onGloballyPositioned { slotRootPos = it.positionInRoot() }
                  .pointerInput(shape, isGameOver) {
                    if (shape == null || isGameOver) return@pointerInput
                    detectTapGestures {
                      VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
                      selectedSlot = idx
                    }
                  }
                  .pointerInput(shape, isGameOver) {
                    if (shape == null || isGameOver) return@pointerInput
                    detectDragGestures(
                      onDragStart = { localOffset ->
                        VaultAudioEngine.playGameSfx(GameSfx.PICKUP)
                        selectedSlot = idx
                        draggingSlot = idx
                        dragRootOffset = slotRootPos + localOffset
                        hoverAnchor = computeHoverAnchor(dragRootOffset, shape)
                      },
                      onDrag = { change, _ ->
                        change.consume()
                        dragRootOffset = slotRootPos + change.position
                        hoverAnchor = computeHoverAnchor(dragRootOffset, shape)
                      },
                      onDragEnd = {
                        val anchor = hoverAnchor
                        if (anchor != null) {
                          commitPlacement(idx, anchor.first, anchor.second)
                        }
                        draggingSlot = null
                        hoverAnchor = null
                      },
                      onDragCancel = {
                        draggingSlot = null
                        hoverAnchor = null
                      },
                    )
                  }
                  .testTag("block_tray_slot_$idx"),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (shape != null && draggingSlot != idx) {
                  ShapePreviewCanvas(shape = shape, dimmed = !canFit, modifier = Modifier.fillMaxSize())
                }
              }
            }
          }
        }
      }

      Text(
        text = "Drag onto board or tap a shape then tap the 8×8 grid · Clears rows & columns!",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Floating Drag Ghost above thumb
    val activeDragShape = draggingSlot?.let { tray.getOrNull(it) }
    if (activeDragShape != null) {
      val cellPx = if (boardPixelSize.width > 0f) boardPixelSize.width / BlockBlastLogic.GRID_SIZE else 36f
      val wDp = with(density) { (cellPx * activeDragShape.cols).toDp() }
      val hDp = with(density) { (cellPx * activeDragShape.rows).toDp() }
      Canvas(
        modifier =
          Modifier
            .size(wDp, hDp)
            .let { mod ->
              mod.padding(0.dp)
            }
            .then(
              Modifier.graphicsLayerOffset(
                x = dragRootOffset.x - containerRootPos.x - (cellPx * activeDragShape.cols) * 0.5f,
                y = dragRootOffset.y - dragLiftPx - containerRootPos.y - (cellPx * activeDragShape.rows) * 0.5f,
              ),
            ),
      ) {
        val cSize = size.width / activeDragShape.cols
        val gap = cSize * 0.08f
        val col = Color(activeDragShape.colorHex)
        activeDragShape.cells.forEach { (dr, dc) ->
          val bx = dc * cSize + gap * 0.5f
          val by = dr * cSize + gap * 0.5f
          val bw = cSize - gap
          val cr = CornerRadius(cSize * 0.16f)
          drawRoundRect(
            Color(0x55000000),
            Offset(bx + 2f, by + 4f),
            Size(bw, bw),
            cr,
          )
          drawRoundRect(
            col.copy(alpha = 0.92f),
            Offset(bx, by),
            Size(bw, bw),
            cr,
          )
          drawRoundRect(
            Color(0x55FFFFFF),
            Offset(bx + bw * 0.10f, by + bw * 0.08f),
            Size(bw * 0.80f, bw * 0.22f),
            CornerRadius(cSize * 0.08f),
          )
          drawRoundRect(
            Color(0x38000000),
            Offset(bx + bw * 0.06f, by + bw * 0.78f),
            Size(bw * 0.88f, bw * 0.16f),
            CornerRadius(cSize * 0.08f),
          )
        }
      }
    }

    if (isGameOver) {
      Surface(
        color = Color(0xDC080B12),
        modifier = Modifier.fillMaxSize(),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("Endless Run Complete", style = MaterialTheme.typography.headlineSmall)
              Text(
                text = "Score: $score  ·  Best: $bestScore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Cleared $linesTotal lines on the 8×8 Vault grid.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.height(4.dp))
              Button(
                onClick = { resetRun() },
                modifier = Modifier.fillMaxWidth().testTag("block_blast_play_again"),
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("New Endless Run")
              }
            }
          }
        }
      }
    }
  }
}

private fun Modifier.graphicsLayerOffset(x: Float, y: Float): Modifier =
  this.graphicsLayer {
    translationX = x
    translationY = y
  }

@Composable
private fun ShapePreviewCanvas(
  shape: BlockShape,
  dimmed: Boolean,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val maxDim = maxOf(shape.rows, shape.cols).coerceAtLeast(2)
    val cell = minOf(size.width / maxDim, size.height / maxDim) * 0.88f
    val totalW = cell * shape.cols
    val totalH = cell * shape.rows
    val ox = (size.width - totalW) * 0.5f
    val oy = (size.height - totalH) * 0.5f
    val gap = cell * 0.10f
    val color = Color(shape.colorHex).copy(alpha = if (dimmed) 0.35f else 1f)
    val bw = cell - gap
    val cr = CornerRadius(cell * 0.2f)
    shape.cells.forEach { (dr, dc) ->
      val bx = ox + dc * cell + gap * 0.5f
      val by = oy + dr * cell + gap * 0.5f
      if (!dimmed) {
        drawRoundRect(
          color = Color(0x44000000),
          topLeft = Offset(bx, by + cell * 0.06f),
          size = Size(bw, bw),
          cornerRadius = cr,
        )
      }
      drawRoundRect(
        color = color,
        topLeft = Offset(bx, by),
        size = Size(bw, bw),
        cornerRadius = cr,
      )
      drawRoundRect(
        color = Color.White.copy(alpha = if (dimmed) 0.10f else 0.32f),
        topLeft = Offset(bx + bw * 0.10f, by + bw * 0.08f),
        size = Size(bw * 0.80f, bw * 0.20f),
        cornerRadius = CornerRadius(cell * 0.08f),
      )
      drawRoundRect(
        color = Color.Black.copy(alpha = if (dimmed) 0.10f else 0.22f),
        topLeft = Offset(bx + bw * 0.06f, by + bw * 0.78f),
        size = Size(bw * 0.88f, bw * 0.16f),
        cornerRadius = CornerRadius(cell * 0.08f),
      )
    }
  }
}

package com.example.ui.engines

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.GameSfx
import com.example.audio.VaultAudioEngine
import com.example.data.model.PlayerTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class SavedChessSnapshot(
  val friendMode: Boolean,
  val autoFlipBoard: Boolean,
  val selectedElo: Int,
  val state: ChessState,
)

private fun parseChessSnapshot(json: String?): SavedChessSnapshot? {
  if (json.isNullOrBlank()) return null
  return runCatching {
    val obj = JSONObject(json)
    val decoded = QuickResumeSerializer.decodeChessState(obj.optJSONObject("chess")) ?: return null
    val elo = obj.optInt("elo", 100)
    val validElo = if (CHESS_ELO_PRESETS.any { it.elo == elo }) elo else 100
    SavedChessSnapshot(
      friendMode = obj.optBoolean("friend", false),
      autoFlipBoard = obj.optBoolean("flip", false),
      selectedElo = validElo,
      state = decoded,
    )
  }.getOrNull()
}

@Composable
fun VaultChessGame(
  currentTier: PlayerTier,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
  savedStateJson: String? = null,
  onRegisterQuickResumeProvider: ((() -> Pair<String, String>?) -> Unit) = {},
) {
  val initialSnapshot = remember(savedStateJson) { parseChessSnapshot(savedStateJson) }
  var friendMode by remember(savedStateJson) { mutableStateOf(initialSnapshot?.friendMode ?: false) }
  var autoFlipBoard by remember(savedStateJson) { mutableStateOf(initialSnapshot?.autoFlipBoard ?: false) }
  var selectedElo by remember(savedStateJson) { mutableIntStateOf(initialSnapshot?.selectedElo ?: 100) }
  var chessState by remember(savedStateJson) { mutableStateOf(initialSnapshot?.state ?: ChessState.initial()) }
  val history = remember(savedStateJson) { mutableStateListOf<ChessState>() }
  var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var botThinking by remember { mutableStateOf(false) }
  var statusText by remember(savedStateJson) {
    mutableStateOf(
      if (initialSnapshot != null) {
        val side = if (initialSnapshot.state.turn == ChessColor.WHITE) "White" else "Black"
        if (initialSnapshot.friendMode) "Resumed Pass & Play · $side to move"
        else "Resumed vs ${initialSnapshot.selectedElo} Elo · $side to move"
      } else {
        "Your turn (White) · Bot set to 100 Elo"
      }
    )
  }
  var rewardPaidForGame by remember { mutableStateOf(false) }

  val activePreset = CHESS_ELO_PRESETS.first { it.elo == selectedElo }
  val gameStatus = remember(chessState) { ChessRules.evaluateStatus(chessState) }
  val activeTurn = chessState.turn

  val latestFriendMode by rememberUpdatedState(friendMode)
  val latestAutoFlip by rememberUpdatedState(autoFlipBoard)
  val latestElo by rememberUpdatedState(selectedElo)
  val latestState by rememberUpdatedState(chessState)
  val latestStatus by rememberUpdatedState(gameStatus)

  DisposableEffect(onRegisterQuickResumeProvider) {
    onRegisterQuickResumeProvider {
      if (latestStatus != ChessGameStatus.IN_PROGRESS || latestState.lastMove == null) {
        null
      } else {
        val obj = JSONObject().apply {
          put("friend", latestFriendMode)
          put("flip", latestAutoFlip)
          put("elo", latestElo)
          put("chess", QuickResumeSerializer.encodeChessState(latestState))
        }
        val modeTag = if (latestFriendMode) "2P Duel" else "${latestElo} Elo"
        val turnTag = if (latestState.turn == ChessColor.WHITE) "White turn" else "Black turn"
        "$modeTag · $turnTag" to obj.toString()
      }
    }
    onDispose {}
  }

  val legalMovesForActiveSide =
    remember(chessState, friendMode) {
      if (gameStatus == ChessGameStatus.IN_PROGRESS && (friendMode || chessState.turn == ChessColor.WHITE)) {
        ChessRules.legalMoves(chessState, chessState.turn)
      } else {
        emptyList()
      }
    }

  val highlightedTargets =
    remember(selectedSquare, legalMovesForActiveSide) {
      val sel = selectedSquare
      if (sel == null) emptyMap()
      else legalMovesForActiveSide.filter { it.fromRow == sel.first && it.fromCol == sel.second }.associateBy { it.toRow to it.toCol }
    }

  fun resetBoard(isFriend: Boolean = friendMode, elo: Int = selectedElo) {
    chessState = ChessState.initial()
    history.clear()
    selectedSquare = null
    botThinking = false
    rewardPaidForGame = false
    statusText =
      if (isFriend) "Pass & Play · White to move"
      else "Your turn (White) · Bot set to $elo Elo"
  }

  // Handle Game Over Sounds & Rewards
  LaunchedEffect(gameStatus, friendMode) {
    if (gameStatus != ChessGameStatus.IN_PROGRESS && !rewardPaidForGame) {
      rewardPaidForGame = true
      if (friendMode) {
        when (gameStatus) {
          ChessGameStatus.WHITE_WINS_CHECKMATE -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECKMATE)
            delay(380L)
            VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
            statusText = "CHECKMATE! White wins the 2P Friend match! (+90 XP)"
            onAwardXp(90, 450, null)
          }
          ChessGameStatus.BLACK_WINS_CHECKMATE -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECKMATE)
            delay(380L)
            VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
            statusText = "CHECKMATE! Black wins the 2P Friend match! (+90 XP)"
            onAwardXp(90, 450, null)
          }
          else -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECK)
            statusText = "2P Match Draw! (+50 XP)"
            onAwardXp(50, 220, null)
          }
        }
      } else {
        when (gameStatus) {
          ChessGameStatus.WHITE_WINS_CHECKMATE -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECKMATE)
            delay(380L)
            VaultAudioEngine.playGameSfx(GameSfx.VICTORY)
            val xp = 120 + (selectedElo / 2)
            val score = 500 + selectedElo * 2
            val ach = if (selectedElo >= 800) "chess_800_super" else "chess_100_win"
            statusText = "CHECKMATE! You defeated the $selectedElo Elo bot! (+$xp XP)"
            onAwardXp(xp, score, ach)
          }
          ChessGameStatus.BLACK_WINS_CHECKMATE -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECKMATE)
            statusText = "Checkmate — $selectedElo Elo Bot wins. (+25 XP for practice)"
            onAwardXp(25, 100, null)
          }
          else -> {
            VaultAudioEngine.playGameSfx(GameSfx.CHECK)
            statusText = "Draw! (+50 XP)"
            onAwardXp(50, 250 + selectedElo / 2, null)
          }
        }
      }
    }
  }

  // Bot Turn Execution on Background Thread (only in AI Bot mode)
  LaunchedEffect(chessState, gameStatus, selectedElo, friendMode) {
    if (!friendMode && chessState.turn == ChessColor.BLACK && gameStatus == ChessGameStatus.IN_PROGRESS) {
      botThinking = true
      statusText = "$selectedElo Elo Bot thinking..."
      val botMove =
        withContext(Dispatchers.Default) {
          ChessAiEngine.chooseMove(chessState, selectedElo)
        }
      if (botMove != null) {
        val next = ChessRules.applyMove(chessState, botMove)
        chessState = next
        val nextStatus = ChessRules.evaluateStatus(next)
        val inCheck = ChessRules.isInCheck(next, ChessColor.WHITE)
        if (nextStatus == ChessGameStatus.IN_PROGRESS) {
          VaultAudioEngine.playGameSfx(if (inCheck) GameSfx.CHECK else GameSfx.CHESS_MOVE)
        }
        statusText =
          if (inCheck) "Check! Your turn (White)" else "Your turn (White) · $selectedElo Elo"
      }
      botThinking = false
    }
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Mode Switcher: AI Bot vs Play with Friend
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = !friendMode,
        onClick = {
          if (friendMode) {
            friendMode = false
            resetBoard(isFriend = false)
          }
        },
        leadingIcon = { Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("VS AI BOT") },
        modifier = Modifier.weight(1f).testTag("chess_mode_ai"),
      )
      FilterChip(
        selected = friendMode,
        onClick = {
          if (!friendMode) {
            friendMode = true
            resetBoard(isFriend = true)
          }
        },
        leadingIcon = { Icon(Icons.Rounded.Group, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text("PLAY WITH FRIEND") },
        modifier = Modifier.weight(1f).testTag("chess_mode_friend"),
      )
    }

    if (!friendMode) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Text("AI BOT RATING · ${activePreset.label}", style = MaterialTheme.typography.labelLarge)
            }
            Text(
              if (selectedElo >= 800) "SUPER RANK" else activePreset.requiredTier.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CHESS_ELO_PRESETS, key = { it.elo }) { preset ->
              val unlocked = currentTier.levelIndex >= preset.requiredTier.levelIndex
              val isSelected = selectedElo == preset.elo
              FilterChip(
                selected = isSelected,
                onClick = {
                  if (unlocked) {
                    selectedElo = preset.elo
                    statusText =
                      if (chessState.turn == ChessColor.WHITE) "Bot switched to ${preset.elo} Elo"
                      else statusText
                  } else {
                    statusText =
                      "${preset.elo} Elo Bot requires ${preset.requiredTier.displayName} Rank (${preset.requiredTier.minXp} XP)!"
                  }
                },
                leadingIcon = {
                  if (!unlocked) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Locked", modifier = Modifier.size(14.dp))
                  }
                },
                label = {
                  Text(
                    if (unlocked) "${preset.elo}" else "${preset.elo} (${preset.requiredTier.displayName})",
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
                modifier = Modifier.testTag("chess_elo_${preset.elo}"),
              )
            }
          }
          Text(activePreset.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("2P PASS & PLAY · LOCAL DUEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("White & Black alternate moves on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          FilterChip(
            selected = autoFlipBoard,
            onClick = { autoFlipBoard = !autoFlipBoard },
            leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Flip Turn", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.testTag("chess_auto_flip"),
          )
        }
      }
    }

    // Status + Action Row
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        color = if (gameStatus != ChessGameStatus.IN_PROGRESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = {
            if (!botThinking && gameStatus == ChessGameStatus.IN_PROGRESS && history.isNotEmpty()) {
              val prev = history.removeAt(history.lastIndex)
              chessState = prev
              selectedSquare = null
              VaultAudioEngine.playGameSfx(GameSfx.CHESS_MOVE)
              val sideLabel = if (prev.turn == ChessColor.WHITE) "White" else "Black"
              statusText = if (friendMode) "Move undone · $sideLabel to move" else "Move undone · Your turn"
            }
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
        }
        OutlinedButton(
          onClick = { resetBoard() },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.testTag("chess_reset_btn"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = "New Game", modifier = Modifier.size(16.dp))
        }
      }
    }

    // 8x8 Chessboard
    val flipForBlack = friendMode && autoFlipBoard && activeTurn == ChessColor.BLACK
    Surface(
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      val whiteInCheck = ChessRules.isInCheck(chessState, ChessColor.WHITE)
      val blackInCheck = ChessRules.isInCheck(chessState, ChessColor.BLACK)

      val rowIndices = if (flipForBlack) (7 downTo 0) else (0..7)
      val colIndices = if (flipForBlack) (7 downTo 0) else (0..7)

      Column(modifier = Modifier.fillMaxSize()) {
        for (r in rowIndices) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (c in colIndices) {
              val piece = chessState.board[r][c]
              val isDark = (r + c) % 2 == 1
              val isSel = selectedSquare == (r to c)
              val targetMove = highlightedTargets[r to c]
              val isLandingSquare = chessState.lastMove?.let { it.toRow == r && it.toCol == c } == true
              val isLast =
                chessState.lastMove?.let {
                  (it.fromRow == r && it.fromCol == c) || (it.toRow == r && it.toCol == c)
                } == true
              val kingInCheckHere =
                piece?.type == PieceType.KING &&
                  ((piece.color == ChessColor.WHITE && whiteInCheck) ||
                    (piece.color == ChessColor.BLACK && blackInCheck))

              val squareBg =
                when {
                  kingInCheckHere -> Color(0xFFB91C1C)
                  isSel -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                  isLast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                  isDark -> Color(0xFF283444)
                  else -> Color(0xFFCAD4E0)
                }

              val canInteract =
                !botThinking &&
                  gameStatus == ChessGameStatus.IN_PROGRESS &&
                  (friendMode || chessState.turn == ChessColor.WHITE)

              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(squareBg)
                    .clickable(enabled = canInteract) {
                      if (targetMove != null) {
                        history.add(chessState)
                        val next = ChessRules.applyMove(chessState, targetMove)
                        chessState = next
                        selectedSquare = null
                        val nextStatus = ChessRules.evaluateStatus(next)
                        val nextCheck = ChessRules.isInCheck(next, next.turn)
                        if (nextStatus == ChessGameStatus.IN_PROGRESS) {
                          VaultAudioEngine.playGameSfx(if (nextCheck) GameSfx.CHECK else GameSfx.CHESS_MOVE)
                        }
                        if (friendMode && nextStatus == ChessGameStatus.IN_PROGRESS) {
                          val nextLabel = if (next.turn == ChessColor.WHITE) "White" else "Black"
                          statusText = if (nextCheck) "Check! $nextLabel to move" else "Pass & Play · $nextLabel to move"
                        }
                      } else if (piece != null && piece.color == activeTurn) {
                        selectedSquare = if (isSel) null else (r to c)
                      } else {
                        selectedSquare = null
                      }
                    }
                    .testTag("chess_sq_${r}_$c"),
              ) {
                if (piece != null) {
                  val popScale = remember(chessState.lastMove, r, c) { Animatable(if (isLandingSquare) 0.72f else 1f) }
                  LaunchedEffect(chessState.lastMove, r, c) {
                    if (isLandingSquare) {
                      popScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
                    }
                  }
                  ChessPieceVector(
                    piece = piece,
                    modifier = Modifier.fillMaxSize(0.84f).scale(popScale.value),
                  )
                }
                if (targetMove != null) {
                  if (piece == null) {
                    Box(
                      modifier =
                        Modifier
                          .size(12.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    )
                  } else {
                    Box(
                      modifier =
                        Modifier
                          .fillMaxSize()
                          .padding(2.dp)
                          .border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))
  }
}

@Composable
private fun ChessPieceVector(
  piece: ChessPiece,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    drawStauntonPiece(piece)
  }
}

private fun DrawScope.drawStauntonPiece(piece: ChessPiece) {
  val w = size.width
  val h = size.height
  val isWhite = piece.color == ChessColor.WHITE
  val fill = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF151C28)
  val rim = if (isWhite) Color(0xFF334155) else Color(0xFFCBD5E1)
  val accent = if (isWhite) Color(0xFFE2E8F0) else Color(0xFF475569)
  val gleam = if (isWhite) Color.White else Color(0xFF64748B)

  // Drop Shadow + Stepped Pedestal Base shared across all pieces
  drawOval(Color.Black.copy(alpha = 0.28f), Offset(w * 0.15f, h * 0.84f), Size(w * 0.70f, h * 0.11f))
  drawRoundRect(fill, Offset(w * 0.18f, h * 0.76f), Size(w * 0.64f, h * 0.13f), CornerRadius(6f))
  drawRoundRect(gleam.copy(alpha = 0.45f), Offset(w * 0.22f, h * 0.775f), Size(w * 0.56f, h * 0.04f), CornerRadius(3f))
  drawRoundRect(rim, Offset(w * 0.18f, h * 0.76f), Size(w * 0.64f, h * 0.13f), CornerRadius(6f), style = Stroke(width = 2.2f))

  val outline = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  when (piece.type) {
    PieceType.PAWN -> {
      val stem = Path().apply {
        moveTo(w * 0.30f, h * 0.76f)
        quadraticTo(w * 0.40f, h * 0.56f, w * 0.42f, h * 0.44f)
        lineTo(w * 0.58f, h * 0.44f)
        quadraticTo(w * 0.60f, h * 0.56f, w * 0.70f, h * 0.76f)
        close()
      }
      drawPath(stem, fill)
      drawPath(stem, rim, style = outline)
      drawRoundRect(accent, Offset(w * 0.34f, h * 0.40f), Size(w * 0.32f, h * 0.06f), CornerRadius(4f))
      drawCircle(fill, w * 0.18f, Offset(w * 0.50f, h * 0.27f))
      drawCircle(gleam.copy(alpha = 0.55f), w * 0.06f, Offset(w * 0.44f, h * 0.22f))
      drawCircle(rim, w * 0.18f, Offset(w * 0.50f, h * 0.27f), style = outline)
    }
    PieceType.ROOK -> {
      val tower = Path().apply {
        moveTo(w * 0.26f, h * 0.76f)
        lineTo(w * 0.32f, h * 0.34f)
        lineTo(w * 0.24f, h * 0.30f)
        lineTo(w * 0.24f, h * 0.14f)
        lineTo(w * 0.36f, h * 0.14f)
        lineTo(w * 0.36f, h * 0.23f)
        lineTo(w * 0.44f, h * 0.23f)
        lineTo(w * 0.44f, h * 0.14f)
        lineTo(w * 0.56f, h * 0.14f)
        lineTo(w * 0.56f, h * 0.23f)
        lineTo(w * 0.64f, h * 0.23f)
        lineTo(w * 0.64f, h * 0.14f)
        lineTo(w * 0.76f, h * 0.14f)
        lineTo(w * 0.76f, h * 0.30f)
        lineTo(w * 0.68f, h * 0.34f)
        lineTo(w * 0.74f, h * 0.76f)
        close()
      }
      drawPath(tower, fill)
      drawLine(accent, Offset(w * 0.34f, h * 0.35f), Offset(w * 0.66f, h * 0.35f), 2.5f)
      drawPath(tower, rim, style = outline)
    }
    PieceType.KNIGHT -> {
      val knight = Path().apply {
        moveTo(w * 0.26f, h * 0.76f)
        quadraticTo(w * 0.22f, h * 0.45f, w * 0.34f, h * 0.22f)
        lineTo(w * 0.38f, h * 0.10f)
        lineTo(w * 0.48f, h * 0.18f)
        quadraticTo(w * 0.72f, h * 0.22f, w * 0.78f, h * 0.38f)
        lineTo(w * 0.70f, h * 0.46f)
        lineTo(w * 0.54f, h * 0.44f)
        quadraticTo(w * 0.60f, h * 0.60f, w * 0.72f, h * 0.76f)
        close()
      }
      drawPath(knight, fill)
      drawPath(knight, rim, style = outline)
      drawCircle(rim, w * 0.04f, Offset(w * 0.53f, h * 0.29f))
      drawLine(accent, Offset(w * 0.30f, h * 0.38f), Offset(w * 0.32f, h * 0.62f), 2.5f, StrokeCap.Round)
    }
    PieceType.BISHOP -> {
      val bishop = Path().apply {
        moveTo(w * 0.28f, h * 0.76f)
        quadraticTo(w * 0.36f, h * 0.54f, w * 0.34f, h * 0.36f)
        quadraticTo(w * 0.32f, h * 0.20f, w * 0.50f, h * 0.15f)
        quadraticTo(w * 0.68f, h * 0.20f, w * 0.66f, h * 0.36f)
        quadraticTo(w * 0.64f, h * 0.54f, w * 0.72f, h * 0.76f)
        close()
      }
      drawPath(bishop, fill)
      drawPath(bishop, rim, style = outline)
      drawLine(rim, Offset(w * 0.44f, h * 0.29f), Offset(w * 0.58f, h * 0.40f), 2.5f, StrokeCap.Round)
      drawCircle(fill, w * 0.065f, Offset(w * 0.50f, h * 0.11f))
      drawCircle(rim, w * 0.065f, Offset(w * 0.50f, h * 0.11f), style = outline)
    }
    PieceType.QUEEN -> {
      val queen = Path().apply {
        moveTo(w * 0.24f, h * 0.76f)
        lineTo(w * 0.18f, h * 0.30f)
        lineTo(w * 0.33f, h * 0.44f)
        lineTo(w * 0.36f, h * 0.20f)
        lineTo(w * 0.46f, h * 0.42f)
        lineTo(w * 0.50f, h * 0.15f)
        lineTo(w * 0.54f, h * 0.42f)
        lineTo(w * 0.64f, h * 0.20f)
        lineTo(w * 0.67f, h * 0.44f)
        lineTo(w * 0.82f, h * 0.30f)
        lineTo(w * 0.76f, h * 0.76f)
        close()
      }
      drawPath(queen, fill)
      drawPath(queen, rim, style = outline)
      listOf(0.18f to 0.27f, 0.36f to 0.17f, 0.50f to 0.12f, 0.64f to 0.17f, 0.82f to 0.27f).forEach { (cx, cy) ->
        drawCircle(Color(0xFFFFC857), w * 0.048f, Offset(w * cx, h * cy))
        drawCircle(rim, w * 0.048f, Offset(w * cx, h * cy), style = Stroke(width = 1.6f))
      }
    }
    PieceType.KING -> {
      val king = Path().apply {
        moveTo(w * 0.24f, h * 0.76f)
        lineTo(w * 0.22f, h * 0.42f)
        quadraticTo(w * 0.35f, h * 0.26f, w * 0.50f, h * 0.30f)
        quadraticTo(w * 0.65f, h * 0.26f, w * 0.78f, h * 0.42f)
        lineTo(w * 0.76f, h * 0.76f)
        close()
      }
      drawPath(king, fill)
      drawPath(king, rim, style = outline)
      drawLine(Color(0xFFFFC857), Offset(w * 0.50f, h * 0.08f), Offset(w * 0.50f, h * 0.29f), 4f, StrokeCap.Round)
      drawLine(Color(0xFFFFC857), Offset(w * 0.40f, h * 0.16f), Offset(w * 0.60f, h * 0.16f), 4f, StrokeCap.Round)
    }
  }
}

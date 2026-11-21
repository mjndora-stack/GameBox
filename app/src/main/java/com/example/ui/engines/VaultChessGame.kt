package com.example.ui.engines

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VaultChessGame(
  currentTier: PlayerTier,
  onAwardXp: (Int, Int?, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var friendMode by remember { mutableStateOf(false) }
  var autoFlipBoard by remember { mutableStateOf(false) }
  var selectedElo by remember { mutableIntStateOf(100) }
  var chessState by remember { mutableStateOf(ChessState.initial()) }
  val history = remember { mutableStateListOf<ChessState>() }
  var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var botThinking by remember { mutableStateOf(false) }
  var statusText by remember { mutableStateOf("Your turn (White) · Bot set to 100 Elo") }
  var rewardPaidForGame by remember { mutableStateOf(false) }

  val activePreset = CHESS_ELO_PRESETS.first { it.elo == selectedElo }
  val gameStatus = remember(chessState) { ChessRules.evaluateStatus(chessState) }
  val activeTurn = chessState.turn
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

  // Handle Game Over Rewards
  LaunchedEffect(gameStatus, friendMode) {
    if (gameStatus != ChessGameStatus.IN_PROGRESS && !rewardPaidForGame) {
      rewardPaidForGame = true
      if (friendMode) {
        when (gameStatus) {
          ChessGameStatus.WHITE_WINS_CHECKMATE -> {
            statusText = "CHECKMATE! White wins the 2P Friend match! (+90 XP)"
            onAwardXp(90, 450, null)
          }
          ChessGameStatus.BLACK_WINS_CHECKMATE -> {
            statusText = "CHECKMATE! Black wins the 2P Friend match! (+90 XP)"
            onAwardXp(90, 450, null)
          }
          else -> {
            statusText = "2P Match Draw! (+50 XP)"
            onAwardXp(50, 220, null)
          }
        }
      } else {
        when (gameStatus) {
          ChessGameStatus.WHITE_WINS_CHECKMATE -> {
            val xp = 120 + (selectedElo / 2)
            val score = 500 + selectedElo * 2
            val ach = if (selectedElo >= 800) "chess_800_super" else "chess_100_win"
            statusText = "CHECKMATE! You defeated the $selectedElo Elo bot! (+$xp XP)"
            onAwardXp(xp, score, ach)
          }
          ChessGameStatus.BLACK_WINS_CHECKMATE -> {
            statusText = "Checkmate — $selectedElo Elo Bot wins. (+25 XP for practice)"
            onAwardXp(25, 100, null)
          }
          else -> {
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
        val inCheck = ChessRules.isInCheck(next, ChessColor.WHITE)
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
      // Elo Selector Strip (100 - 1000 Elo, 100 unlocked at start, 200-700 lower ranks, 800+ super ranks)
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
                        if (friendMode && ChessRules.evaluateStatus(next) == ChessGameStatus.IN_PROGRESS) {
                          val nextLabel = if (next.turn == ChessColor.WHITE) "White" else "Black"
                          val inCheck = ChessRules.isInCheck(next, next.turn)
                          statusText = if (inCheck) "Check! $nextLabel to move" else "Pass & Play · $nextLabel to move"
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
                  val glyph = if (piece.color == ChessColor.WHITE) piece.type.symbolWhite else piece.type.symbolBlack
                  Text(
                    text = glyph,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (piece.color == ChessColor.WHITE) Color(0xFFFFFFFF) else Color(0xFF0B0E14),
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

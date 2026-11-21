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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
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
  var selectedElo by remember { mutableIntStateOf(100) }
  var chessState by remember { mutableStateOf(ChessState.initial()) }
  val history = remember { mutableStateListOf<ChessState>() }
  var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var botThinking by remember { mutableStateOf(false) }
  var statusText by remember { mutableStateOf("Your turn (White) · Bot set to 100 Elo") }
  var rewardPaidForGame by remember { mutableStateOf(false) }

  val activePreset = CHESS_ELO_PRESETS.first { it.elo == selectedElo }
  val gameStatus = remember(chessState) { ChessRules.evaluateStatus(chessState) }
  val legalForWhite =
    remember(chessState) {
      if (chessState.turn == ChessColor.WHITE && gameStatus == ChessGameStatus.IN_PROGRESS) {
        ChessRules.legalMoves(chessState, ChessColor.WHITE)
      } else {
        emptyList()
      }
    }

  val highlightedTargets =
    remember(selectedSquare, legalForWhite) {
      val sel = selectedSquare
      if (sel == null) emptyMap()
      else legalForWhite.filter { it.fromRow == sel.first && it.fromCol == sel.second }.associateBy { it.toRow to it.toCol }
    }

  // Handle Game Over Rewards
  LaunchedEffect(gameStatus) {
    if (gameStatus != ChessGameStatus.IN_PROGRESS && !rewardPaidForGame) {
      rewardPaidForGame = true
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

  // Bot Turn Execution on Background Thread
  LaunchedEffect(chessState, gameStatus, selectedElo) {
    if (chessState.turn == ChessColor.BLACK && gameStatus == ChessGameStatus.IN_PROGRESS) {
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

  fun resetBoard(elo: Int = selectedElo) {
    chessState = ChessState.initial()
    history.clear()
    selectedSquare = null
    botThinking = false
    rewardPaidForGame = false
    statusText = "Your turn (White) · Bot set to $elo Elo"
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
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
              chessState = history.removeAt(history.lastIndex)
              selectedSquare = null
              statusText = "Move undone · Your turn"
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
    Surface(
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f),
    ) {
      val whiteInCheck = ChessRules.isInCheck(chessState, ChessColor.WHITE)
      val blackInCheck = ChessRules.isInCheck(chessState, ChessColor.BLACK)

      Column(modifier = Modifier.fillMaxSize()) {
        for (r in 0..7) {
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (c in 0..7) {
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

              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(squareBg)
                    .clickable(enabled = !botThinking && gameStatus == ChessGameStatus.IN_PROGRESS && chessState.turn == ChessColor.WHITE) {
                      if (targetMove != null) {
                        history.add(chessState)
                        chessState = ChessRules.applyMove(chessState, targetMove)
                        selectedSquare = null
                      } else if (piece != null && piece.color == ChessColor.WHITE) {
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

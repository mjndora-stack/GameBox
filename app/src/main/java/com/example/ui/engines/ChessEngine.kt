package com.example.ui.engines

import com.example.data.model.PlayerTier
import kotlin.math.abs
import kotlin.random.Random

enum class ChessColor {
  WHITE,
  BLACK;

  fun opposite(): ChessColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val value: Int, val symbolWhite: String, val symbolBlack: String) {
  PAWN(100, "♙", "♟"),
  KNIGHT(320, "♘", "♞"),
  BISHOP(330, "♗", "♝"),
  ROOK(500, "♖", "♜"),
  QUEEN(900, "♕", "♛"),
  KING(20000, "♔", "♚"),
}

data class ChessPiece(val type: PieceType, val color: ChessColor)

data class ChessMove(
  val fromRow: Int,
  val fromCol: Int,
  val toRow: Int,
  val toCol: Int,
  val promotion: PieceType? = null,
  val isCastleKingSide: Boolean = false,
  val isCastleQueenSide: Boolean = false,
  val isEnPassant: Boolean = false,
)

enum class ChessGameStatus {
  IN_PROGRESS,
  WHITE_WINS_CHECKMATE,
  BLACK_WINS_CHECKMATE,
  STALEMATE,
  DRAW_50_MOVE,
}

data class ChessEloPreset(
  val elo: Int,
  val label: String,
  val requiredTier: PlayerTier,
  val description: String,
)

val CHESS_ELO_PRESETS =
  listOf(
    ChessEloPreset(100, "100 Elo", PlayerTier.HOBBYIST, "Novice Bot · Random legal play"),
    ChessEloPreset(200, "200 Elo", PlayerTier.ENTHUSIAST, "Casual Bot · Basic captures"),
    ChessEloPreset(300, "300 Elo", PlayerTier.ENTHUSIAST, "Learner Bot · Avoids simple blunders"),
    ChessEloPreset(400, "400 Elo", PlayerTier.EXPERT, "Club Beginner · 1-ply material"),
    ChessEloPreset(500, "500 Elo", PlayerTier.PRO, "Tactician · Positional 1-ply"),
    ChessEloPreset(600, "600 Elo", PlayerTier.MASTER, "Sharpshooter · 2-ply search"),
    ChessEloPreset(700, "700 Elo", PlayerTier.GRANDMASTER, "Veteran · Clean 2-ply Alpha-Beta"),
    ChessEloPreset(800, "800 Elo", PlayerTier.VIRTUOSO, "Super Rank · Deep tactical 2/3-ply"),
    ChessEloPreset(900, "900 Elo", PlayerTier.LEGEND, "Super Rank · 3-ply Alpha-Beta"),
    ChessEloPreset(1000, "1000 Elo", PlayerTier.MYTHIC, "Super Rank Apex · Full 3-ply Engine"),
  )

data class ChessState(
  val board: Array<Array<ChessPiece?>>,
  val turn: ChessColor = ChessColor.WHITE,
  val whiteCastleK: Boolean = true,
  val whiteCastleQ: Boolean = true,
  val blackCastleK: Boolean = true,
  val blackCastleQ: Boolean = true,
  val enPassantTarget: Pair<Int, Int>? = null,
  val halfMoveClock: Int = 0,
  val lastMove: ChessMove? = null,
) {
  fun copyBoard(): Array<Array<ChessPiece?>> = Array(8) { r -> board[r].copyOf() }

  companion object {
    fun initial(): ChessState {
      val b = Array(8) { arrayOfNulls<ChessPiece>(8) }
      val backRow = listOf(
        PieceType.ROOK,
        PieceType.KNIGHT,
        PieceType.BISHOP,
        PieceType.QUEEN,
        PieceType.KING,
        PieceType.BISHOP,
        PieceType.KNIGHT,
        PieceType.ROOK,
      )
      for (c in 0 until 8) {
        b[0][c] = ChessPiece(backRow[c], ChessColor.BLACK)
        b[1][c] = ChessPiece(PieceType.PAWN, ChessColor.BLACK)
        b[6][c] = ChessPiece(PieceType.PAWN, ChessColor.WHITE)
        b[7][c] = ChessPiece(backRow[c], ChessColor.WHITE)
      }
      return ChessState(board = b)
    }
  }
}

object ChessRules {
  fun isSquareAttacked(state: ChessState, row: Int, col: Int, byColor: ChessColor): Boolean {
    val b = state.board
    // Pawn attacks
    val pawnDir = if (byColor == ChessColor.WHITE) 1 else -1
    for (dc in listOf(-1, 1)) {
      val pr = row + pawnDir
      val pc = col + dc
      if (pr in 0..7 && pc in 0..7) {
        val p = b[pr][pc]
        if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
      }
    }
    // Knight attacks
    val knightOffsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    for ((dr, dc) in knightOffsets) {
      val nr = row + dr; val nc = col + dc
      if (nr in 0..7 && nc in 0..7) {
        val p = b[nr][nc]
        if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
      }
    }
    // King attacks
    for (dr in -1..1) for (dc in -1..1) {
      if (dr == 0 && dc == 0) continue
      val nr = row + dr; val nc = col + dc
      if (nr in 0..7 && nc in 0..7) {
        val p = b[nr][nc]
        if (p != null && p.color == byColor && p.type == PieceType.KING) return true
      }
    }
    // Orthogonal (Rook / Queen)
    for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
      var nr = row + dr; var nc = col + dc
      while (nr in 0..7 && nc in 0..7) {
        val p = b[nr][nc]
        if (p != null) {
          if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
          break
        }
        nr += dr; nc += dc
      }
    }
    // Diagonal (Bishop / Queen)
    for ((dr, dc) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
      var nr = row + dr; var nc = col + dc
      while (nr in 0..7 && nc in 0..7) {
        val p = b[nr][nc]
        if (p != null) {
          if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
          break
        }
        nr += dr; nc += dc
      }
    }
    return false
  }

  fun findKing(state: ChessState, color: ChessColor): Pair<Int, Int>? {
    for (r in 0..7) for (c in 0..7) {
      val p = state.board[r][c]
      if (p != null && p.color == color && p.type == PieceType.KING) return r to c
    }
    return null
  }

  fun isInCheck(state: ChessState, color: ChessColor): Boolean {
    val (kr, kc) = findKing(state, color) ?: return false
    return isSquareAttacked(state, kr, kc, color.opposite())
  }

  fun applyMove(state: ChessState, move: ChessMove): ChessState {
    val nb = state.copyBoard()
    val moving = nb[move.fromRow][move.fromCol] ?: return state
    val captured = nb[move.toRow][move.toCol]

    nb[move.fromRow][move.fromCol] = null
    if (move.isEnPassant) {
      nb[move.fromRow][move.toCol] = null
    }

    val placed = if (move.promotion != null) moving.copy(type = move.promotion) else moving
    nb[move.toRow][move.toCol] = placed

    if (move.isCastleKingSide) {
      nb[move.fromRow][5] = nb[move.fromRow][7]
      nb[move.fromRow][7] = null
    } else if (move.isCastleQueenSide) {
      nb[move.fromRow][3] = nb[move.fromRow][0]
      nb[move.fromRow][0] = null
    }

    var wk = state.whiteCastleK
    var wq = state.whiteCastleQ
    var bk = state.blackCastleK
    var bq = state.blackCastleQ

    if (moving.type == PieceType.KING) {
      if (moving.color == ChessColor.WHITE) { wk = false; wq = false }
      else { bk = false; bq = false }
    }
    if (moving.type == PieceType.ROOK) {
      if (move.fromRow == 7 && move.fromCol == 0) wq = false
      if (move.fromRow == 7 && move.fromCol == 7) wk = false
      if (move.fromRow == 0 && move.fromCol == 0) bq = false
      if (move.fromRow == 0 && move.fromCol == 7) bk = false
    }
    if (captured?.type == PieceType.ROOK) {
      if (move.toRow == 7 && move.toCol == 0) wq = false
      if (move.toRow == 7 && move.toCol == 7) wk = false
      if (move.toRow == 0 && move.toCol == 0) bq = false
      if (move.toRow == 0 && move.toCol == 7) bk = false
    }

    val nextEp =
      if (moving.type == PieceType.PAWN && abs(move.toRow - move.fromRow) == 2) {
        ((move.fromRow + move.toRow) / 2) to move.fromCol
      } else {
        null
      }

    val nextHalfClock =
      if (moving.type == PieceType.PAWN || captured != null || move.isEnPassant) 0
      else state.halfMoveClock + 1

    return ChessState(
      board = nb,
      turn = state.turn.opposite(),
      whiteCastleK = wk,
      whiteCastleQ = wq,
      blackCastleK = bk,
      blackCastleQ = bq,
      enPassantTarget = nextEp,
      halfMoveClock = nextHalfClock,
      lastMove = move,
    )
  }

  fun legalMoves(state: ChessState, color: ChessColor = state.turn): List<ChessMove> {
    val pseudo = mutableListOf<ChessMove>()
    val b = state.board

    for (r in 0..7) {
      for (c in 0..7) {
        val p = b[r][c] ?: continue
        if (p.color != color) continue
        when (p.type) {
          PieceType.PAWN -> {
            val dir = if (color == ChessColor.WHITE) -1 else 1
            val startRow = if (color == ChessColor.WHITE) 6 else 1
            val promoRow = if (color == ChessColor.WHITE) 0 else 7
            val nr = r + dir
            if (nr in 0..7 && b[nr][c] == null) {
              if (nr == promoRow) {
                pseudo.add(ChessMove(r, c, nr, c, promotion = PieceType.QUEEN))
              } else {
                pseudo.add(ChessMove(r, c, nr, c))
                if (r == startRow && b[r + dir * 2][c] == null) {
                  pseudo.add(ChessMove(r, c, r + dir * 2, c))
                }
              }
            }
            for (dc in listOf(-1, 1)) {
              val nc = c + dc
              if (nr in 0..7 && nc in 0..7) {
                val target = b[nr][nc]
                if (target != null && target.color != color) {
                  if (nr == promoRow) pseudo.add(ChessMove(r, c, nr, nc, promotion = PieceType.QUEEN))
                  else pseudo.add(ChessMove(r, c, nr, nc))
                } else if (state.enPassantTarget == (nr to nc)) {
                  pseudo.add(ChessMove(r, c, nr, nc, isEnPassant = true))
                }
              }
            }
          }
          PieceType.KNIGHT -> {
            listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1).forEach { (dr, dc) ->
              val nr = r + dr; val nc = c + dc
              if (nr in 0..7 && nc in 0..7 && b[nr][nc]?.color != color) {
                pseudo.add(ChessMove(r, c, nr, nc))
              }
            }
          }
          PieceType.BISHOP -> addSlidingMoves(b, r, c, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), pseudo)
          PieceType.ROOK -> addSlidingMoves(b, r, c, color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), pseudo)
          PieceType.QUEEN -> addSlidingMoves(
            b, r, c, color,
            listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1),
            pseudo,
          )
          PieceType.KING -> {
            for (dr in -1..1) for (dc in -1..1) {
              if (dr == 0 && dc == 0) continue
              val nr = r + dr; val nc = c + dc
              if (nr in 0..7 && nc in 0..7 && b[nr][nc]?.color != color) {
                pseudo.add(ChessMove(r, c, nr, nc))
              }
            }
            // Castling
            val homeRow = if (color == ChessColor.WHITE) 7 else 0
            val canK = if (color == ChessColor.WHITE) state.whiteCastleK else state.blackCastleK
            val canQ = if (color == ChessColor.WHITE) state.whiteCastleQ else state.blackCastleQ
            if (r == homeRow && c == 4 && !isInCheck(state, color)) {
              if (canK && b[homeRow][5] == null && b[homeRow][6] == null &&
                !isSquareAttacked(state, homeRow, 5, color.opposite()) &&
                !isSquareAttacked(state, homeRow, 6, color.opposite())
              ) {
                pseudo.add(ChessMove(homeRow, 4, homeRow, 6, isCastleKingSide = true))
              }
              if (canQ && b[homeRow][3] == null && b[homeRow][2] == null && b[homeRow][1] == null &&
                !isSquareAttacked(state, homeRow, 3, color.opposite()) &&
                !isSquareAttacked(state, homeRow, 2, color.opposite())
              ) {
                pseudo.add(ChessMove(homeRow, 4, homeRow, 2, isCastleQueenSide = true))
              }
            }
          }
        }
      }
    }

    return pseudo.filter { mv ->
      val next = applyMove(state, mv)
      !isInCheck(next, color)
    }
  }

  private fun addSlidingMoves(
    b: Array<Array<ChessPiece?>>,
    r: Int,
    c: Int,
    color: ChessColor,
    dirs: List<Pair<Int, Int>>,
    out: MutableList<ChessMove>,
  ) {
    for ((dr, dc) in dirs) {
      var nr = r + dr; var nc = c + dc
      while (nr in 0..7 && nc in 0..7) {
        val target = b[nr][nc]
        if (target == null) {
          out.add(ChessMove(r, c, nr, nc))
        } else {
          if (target.color != color) out.add(ChessMove(r, c, nr, nc))
          break
        }
        nr += dr; nc += dc
      }
    }
  }

  fun evaluateStatus(state: ChessState): ChessGameStatus {
    if (state.halfMoveClock >= 100) return ChessGameStatus.DRAW_50_MOVE
    val moves = legalMoves(state, state.turn)
    if (moves.isNotEmpty()) return ChessGameStatus.IN_PROGRESS
    return if (isInCheck(state, state.turn)) {
      if (state.turn == ChessColor.WHITE) ChessGameStatus.BLACK_WINS_CHECKMATE
      else ChessGameStatus.WHITE_WINS_CHECKMATE
    } else {
      ChessGameStatus.STALEMATE
    }
  }
}

object ChessAiEngine {
  private val centerBonus =
    arrayOf(
      intArrayOf(0, 0, 2, 4, 4, 2, 0, 0),
      intArrayOf(0, 4, 6, 8, 8, 6, 4, 0),
      intArrayOf(2, 6, 12, 16, 16, 12, 6, 2),
      intArrayOf(4, 8, 16, 24, 24, 16, 8, 4),
      intArrayOf(4, 8, 16, 24, 24, 16, 8, 4),
      intArrayOf(2, 6, 12, 16, 16, 12, 6, 2),
      intArrayOf(0, 4, 6, 8, 8, 6, 4, 0),
      intArrayOf(0, 0, 2, 4, 4, 2, 0, 0),
    )

  fun chooseMove(state: ChessState, elo: Int): ChessMove? {
    val legal = ChessRules.legalMoves(state, state.turn)
    if (legal.isEmpty()) return null

    // 100 Elo: mostly random legal moves (only 25% chance to take a capture)
    if (elo <= 100) {
      val captures = legal.filter { state.board[it.toRow][it.toCol] != null }
      return if (captures.isNotEmpty() && Random.nextFloat() < 0.25f) captures.random() else legal.random()
    }

    // 200-300 Elo: shallow greedy with significant blunder noise
    if (elo <= 300) {
      val blunderChance = if (elo <= 200) 0.55f else 0.35f
      if (Random.nextFloat() < blunderChance) return legal.random()
      return pickWithNoise(state, legal, depth = 1, noiseRange = 140)
    }

    // 400-500 Elo: 1-ply positional + mild jitter
    if (elo <= 500) {
      val noise = if (elo <= 400) 80 else 35
      return pickWithNoise(state, legal, depth = 1, noiseRange = noise)
    }

    // 600-700 Elo: 2-ply Alpha-Beta
    if (elo <= 700) {
      val noise = if (elo <= 600) 25 else 8
      return pickWithNoise(state, legal, depth = 2, noiseRange = noise)
    }

    // 800-1000 Elo (Super Ranks): 3-ply Alpha-Beta with move ordering
    val noise = when {
      elo <= 800 -> 10
      elo <= 900 -> 4
      else -> 0
    }
    return pickWithNoise(state, legal, depth = 3, noiseRange = noise)
  }

  private fun pickWithNoise(
    state: ChessState,
    legal: List<ChessMove>,
    depth: Int,
    noiseRange: Int,
  ): ChessMove {
    val maximizing = state.turn == ChessColor.WHITE
    val ordered =
      legal.sortedByDescending { mv ->
        val cap = state.board[mv.toRow][mv.toCol]?.type?.value ?: 0
        cap + (if (mv.promotion != null) 800 else 0)
      }

    var bestMove = ordered.first()
    var bestScore = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE

    for (mv in ordered) {
      val next = ChessRules.applyMove(state, mv)
      val baseEval = minimax(next, depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, !maximizing)
      val jitter = if (noiseRange > 0) Random.nextInt(-noiseRange, noiseRange + 1) else 0
      val eval = baseEval + jitter
      if (maximizing) {
        if (eval > bestScore) { bestScore = eval; bestMove = mv }
      } else {
        if (eval < bestScore) { bestScore = eval; bestMove = mv }
      }
    }
    return bestMove
  }

  private fun minimax(
    state: ChessState,
    depth: Int,
    alphaInit: Int,
    betaInit: Int,
    maximizing: Boolean,
  ): Int {
    val legal = ChessRules.legalMoves(state, state.turn)
    if (legal.isEmpty()) {
      return if (ChessRules.isInCheck(state, state.turn)) {
        if (maximizing) -50000 - depth else 50000 + depth
      } else {
        0
      }
    }
    if (depth <= 0) return staticEval(state)

    var alpha = alphaInit
    var beta = betaInit
    val ordered =
      legal.sortedByDescending { mv -> state.board[mv.toRow][mv.toCol]?.type?.value ?: 0 }

    if (maximizing) {
      var best = Int.MIN_VALUE
      for (mv in ordered) {
        val score = minimax(ChessRules.applyMove(state, mv), depth - 1, alpha, beta, false)
        if (score > best) best = score
        if (best > alpha) alpha = best
        if (beta <= alpha) break
      }
      return best
    } else {
      var best = Int.MAX_VALUE
      for (mv in ordered) {
        val score = minimax(ChessRules.applyMove(state, mv), depth - 1, alpha, beta, true)
        if (score < best) best = score
        if (best < beta) beta = best
        if (beta <= alpha) break
      }
      return best
    }
  }

  private fun staticEval(state: ChessState): Int {
    var score = 0
    for (r in 0..7) {
      for (c in 0..7) {
        val p = state.board[r][c] ?: continue
        val sign = if (p.color == ChessColor.WHITE) 1 else -1
        val posBonus =
          when (p.type) {
            PieceType.PAWN -> {
              val advance = if (p.color == ChessColor.WHITE) (6 - r) * 8 else (r - 1) * 8
              advance + centerBonus[r][c] / 2
            }
            PieceType.KNIGHT, PieceType.BISHOP -> centerBonus[r][c]
            PieceType.KING -> if (r == 0 || r == 7) 10 else -15
            else -> centerBonus[r][c] / 3
          }
        score += sign * (p.type.value + posBonus)
      }
    }
    return score
  }
}

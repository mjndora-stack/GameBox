package com.example.ui.engines

import org.json.JSONArray
import org.json.JSONObject

object QuickResumeSerializer {
  fun encodeIntArray(arr: IntArray): JSONArray =
    JSONArray().apply { arr.forEach { put(it) } }

  fun decodeIntArray(arr: JSONArray?): IntArray? {
    if (arr == null) return null
    return IntArray(arr.length()) { i -> arr.optInt(i, 0) }
  }

  fun encodeBooleanArray(arr: BooleanArray): JSONArray =
    JSONArray().apply { arr.forEach { put(it) } }

  fun decodeBooleanArray(arr: JSONArray?): BooleanArray? {
    if (arr == null) return null
    return BooleanArray(arr.length()) { i -> arr.optBoolean(i, false) }
  }

  fun encodeChessState(state: ChessState): JSONObject {
    val obj = JSONObject()
    val rows = JSONArray()
    for (r in 0..7) {
      val rowArr = JSONArray()
      for (c in 0..7) {
        val p = state.board[r][c]
        rowArr.put(if (p == null) "" else "${p.color.name}:${p.type.name}")
      }
      rows.put(rowArr)
    }
    obj.put("board", rows)
    obj.put("turn", state.turn.name)
    obj.put("wK", state.whiteCastleK)
    obj.put("wQ", state.whiteCastleQ)
    obj.put("bK", state.blackCastleK)
    obj.put("bQ", state.blackCastleQ)
    obj.put("halfMove", state.halfMoveClock)
    state.enPassantTarget?.let { (er, ec) ->
      obj.put("epR", er)
      obj.put("epC", ec)
    }
    state.lastMove?.let { m ->
      obj.put(
        "lastMove",
        JSONObject().apply {
          put("fr", m.fromRow)
          put("fc", m.fromCol)
          put("tr", m.toRow)
          put("tc", m.toCol)
          if (m.promotion != null) put("promo", m.promotion.name)
          put("ck", m.isCastleKingSide)
          put("cq", m.isCastleQueenSide)
          put("ep", m.isEnPassant)
        },
      )
    }
    return obj
  }

  fun decodeChessState(obj: JSONObject?): ChessState? {
    if (obj == null) return null
    return runCatching {
      val rows = obj.getJSONArray("board")
      if (rows.length() != 8) return null
      val board = Array(8) { arrayOfNulls<ChessPiece>(8) }
      for (r in 0..7) {
        val rowArr = rows.getJSONArray(r)
        for (c in 0..7) {
          val code = rowArr.optString(c, "")
          if (code.isNotEmpty() && ':' in code) {
            val parts = code.split(':')
            board[r][c] = ChessPiece(
              type = PieceType.valueOf(parts[1]),
              color = ChessColor.valueOf(parts[0]),
            )
          }
        }
      }
      val epTarget =
        if (obj.has("epR") && obj.has("epC")) obj.getInt("epR") to obj.getInt("epC") else null
      val lmObj = obj.optJSONObject("lastMove")
      val lastMove =
        if (lmObj != null) {
          ChessMove(
            fromRow = lmObj.getInt("fr"),
            fromCol = lmObj.getInt("fc"),
            toRow = lmObj.getInt("tr"),
            toCol = lmObj.getInt("tc"),
            promotion = lmObj.optString("promo", "").takeIf { it.isNotEmpty() }?.let { PieceType.valueOf(it) },
            isCastleKingSide = lmObj.optBoolean("ck", false),
            isCastleQueenSide = lmObj.optBoolean("cq", false),
            isEnPassant = lmObj.optBoolean("ep", false),
          )
        } else null
      ChessState(
        board = board,
        turn = ChessColor.valueOf(obj.optString("turn", ChessColor.WHITE.name)),
        whiteCastleK = obj.optBoolean("wK", true),
        whiteCastleQ = obj.optBoolean("wQ", true),
        blackCastleK = obj.optBoolean("bK", true),
        blackCastleQ = obj.optBoolean("bQ", true),
        enPassantTarget = epTarget,
        halfMoveClock = obj.optInt("halfMove", 0),
        lastMove = lastMove,
      )
    }.getOrNull()
  }
}

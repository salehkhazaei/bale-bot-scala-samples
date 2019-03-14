package com.mesr.bot.helpers

import com.bot4s.telegram.models.Message
import com.mesr.bot._
import com.mesr.bot.sdk.StatefulBot

import scala.util.Try

trait StateHelper extends StatefulBot[AftabeState] with Constants {

  def defaultGame(level: Int = 0) = GameState(level, 0, Seq.empty)

  def defaultState(userId: Long) = AftabeState(UserState(userId, startCoin, isEnteringInviteCode = false, None), defaultGame(), None)

  val db = Database(
    IndexedSeq(
      Level("1856114092:-2456185831143174655:1", "رنگین کمان"),
      Level("1856114092:833412221674720768:1", "پیچ گوشتی"),
      Level("1856114092:-6453834187875742207:1", "رباط"),
      Level("1856114092:1363412010635300611:1", "فدات"),
      Level("1856114092:-3205841655052890366:1", "دست انداز"),
      Level("1856114092:141775427338636803::1", "پدال"),
      Level("1856114092:-7914742093562049024:1", "نیرنگ"),
      Level("1856114092:721649063734808066:1", "دومینو"),
      Level("1856114092:-3100974634042323453:1", "ستاره")
    )
  )

  def withCurrentState[S, T](f: (AftabeState, Option[Level]) => T)(implicit msg: Message): T = {
    withChatState { implicit optState =>
      val currentState = optState.getOrElse(defaultState(msg.source))
      val currentLevel = Try(db.levels(currentState.gameState.level)).toOption
      setChatState(currentState)

      f(currentState, currentLevel)
    }
  }

}

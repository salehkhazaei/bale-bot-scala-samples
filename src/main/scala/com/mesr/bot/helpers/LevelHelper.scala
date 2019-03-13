package com.mesr.bot.helpers

import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.Message
import com.mesr.bot.Errors.GameFinished
import com.mesr.bot.Strings.gameFinishedStr
import com.mesr.bot.Constants

trait LevelHelper extends StateHelper with TelegramBot with Constants {

  def nextLevel(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState
        .copy(gameState = defaultGame(currentState.gameState.level + 1))

      setChatState(newState)
    }
  }

  def withCheckFinished[T](f: => T)(implicit msg: Message): T = {
    withCurrentState { (currentState, currentLevel) =>
      if (currentLevel.isDefined) {
        f
      } else {
        request(SendMessage(msg.source, gameFinishedStr))
        throw GameFinished
      }
    }
  }
}

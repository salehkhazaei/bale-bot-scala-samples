package com.mesr.bot.helpers

import akka.actor.ActorSystem
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.Message
import com.mesr.bot.Errors.GameFinished
import com.mesr.bot.Strings._
import com.mesr.bot.sdk.db.RedisExtension
import com.mesr.bot.{Constants, RequestLevel}

import scala.util.Random

trait LevelHelper extends StateHelper with TelegramBot with Constants {
  def nextLevel(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val newState = currentState
        .copy(gameState = defaultGame(currentState.gameState.level + 1))

      setChatState(newState)
    }
  }

  def withCheckFinished[T](f: => T)(implicit msg: Message): T = {
    withCurrentState { (_, currentLevel) =>
      if (currentLevel.isDefined) {
        f
      } else {
        request(SendMessage(msg.source, gameFinishedStr))
        throw GameFinished
      }
    }
  }

  def addingNewLevelRequest()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, sendLevelPhotoStr))
  }

  def addingNewLevelPhoto()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val fileId = msg.photo.get.head.fileId
      val newState = currentState.copy(requestLevelState = Some(RequestLevel(fileId)))

      setChatState(newState)

      request(SendMessage(msg.source, sendLevelResponseStr))
    }
  }

  def isEnteringLevelResponse()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, _) =>
      currentState.requestLevelState.isDefined
    }
  }

  def setLevelText()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val fileId = currentState.requestLevelState.get.fileId
      val response = msg.text.get
      val newState = currentState.copy(requestLevelState = None)

      setChatState(newState)

      redisExt.set(s"request-level-${msg.source}-${Random.nextInt()}", s"$fileId - $response")

      request(SendMessage(msg.source, levelRegisteredStr))
    }
  }
}

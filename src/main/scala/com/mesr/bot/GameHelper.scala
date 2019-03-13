package com.mesr.bot

import akka.actor.ActorSystem
import akka.pattern.after
import com.bot4s.telegram.methods.{SendMessage, SendPhoto}
import com.bot4s.telegram.models.InputFile.FileId
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._

import scala.concurrent.duration._

trait GameHelper extends StateHelper with LevelHelper {

  def sendCurrentGame(wrongGuess: Boolean = false)(implicit system: ActorSystem, msg: Message): Unit = {
    def createResponse(gameState: GameState): String = {
      db.levels(gameState.level).response.zipWithIndex.map { char =>
        if (char._1 == ' ')
          "  "
        else if (char._2 >= gameState.revealedChars.length)
          "-- "
        else if (gameState.revealedChars(char._2))
          s"${char._1} "
        else
          "-- "
      }.mkString
    }

    withCheckFinished {
      withCurrentState { (currentState, currentLevel) =>
        request(SendPhoto(msg.source, FileId(currentLevel.get.fileId)))

        val isShowCharsAllowed = currentState.gameState.revealedChars.forall(_ == false)

        after(500.milliseconds, system.scheduler) {
          val responseText =
            s"""
               |${if (wrongGuess) wrongGuessStr else ""}
               |$noOfCoinsStr *${currentState.userState.coinCount}*
               |$levelStr ${currentState.gameState.level + 1}
               |${createResponse(currentState.gameState)}
               |$enterYourResponseStr
          """.stripMargin

          request(SendMessage(msg.source, responseText, replyMarkup = Some(ReplyKeyboardMarkup(
            Seq(
              if (isShowCharsAllowed)
                Seq(
                  KeyboardButton(showSomeCharsStr),
                  KeyboardButton(showWordStr),
                  KeyboardButton(showHelpStr)
                )
              else
                Seq(
                  KeyboardButton(showWordStr),
                  KeyboardButton(showHelpStr)
                )
            )
          ))))
        }
      }
    }
  }

  def successGuess(implicit msg: Message): Unit = {
    nextLevel

    withCurrentState { (currentState, currentLevel) =>
      val currentCoin = currentState.userState.coinCount

      val newState = currentState
        .copy(userState = currentState.userState.copy(coinCount = currentCoin + wonCoin))

      setChatState(newState)
      request(SendMessage(msg.source, wonStr(wonCoin)))
    }
  }

  def wrongGuess(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val currentGuessCount = currentState.gameState.guessCount

      val newState = currentState
        .copy(gameState = currentState.gameState.copy(guessCount = currentGuessCount + 1))

      setChatState(newState)
    }
  }

}

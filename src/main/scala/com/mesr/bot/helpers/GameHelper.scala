package com.mesr.bot.helpers

import akka.actor.ActorSystem
import com.bot4s.telegram.methods.{SendMessage, SendPhoto}
import com.bot4s.telegram.models.InputFile.FileId
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.GameState
import com.mesr.bot.Strings._

import scala.concurrent.Future

trait GameHelper extends StateHelper with LevelHelper {
  def startGame()(implicit system: ActorSystem, msg: Message): Unit = {
    request(SendMessage(msg.source, helloMessageStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(showInviteCodeStr),
          KeyboardButton(startButtonStr),
          KeyboardButton(buyStr),
          KeyboardButton(addingNewLevelStr),
          KeyboardButton(showHelpStr)
        )
      )))))
  }

  def sendCurrentGame(wrongGuess: Boolean = false)(implicit system: ActorSystem, msg: Message): Future[Unit] = {
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
        for {
          _ <- request(SendPhoto(msg.source, FileId(currentLevel.get.fileId)))
          isShowCharsAllowed = currentState.gameState.revealedChars.forall(_ == false)
          responseText =
          s"""
             |${if (wrongGuess) wrongGuessStr else ""}
             |$levelStr ${currentState.gameState.level + 1}
             |$noOfCoinsStr *${getCoinString(currentState.userState.coinCount)}*
             |${createResponse(currentState.gameState)}
             |$enterYourResponseStr
          """.stripMargin.trim
          _ <- request(SendMessage(msg.source, responseText, replyMarkup = Some(ReplyKeyboardMarkup(
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
        } yield ()
      }
    }
  }

  def successGuess(implicit msg: Message): Future[Unit] = {
    nextLevel

    withCurrentState { (currentState, _) =>
      val currentCoin = currentState.userState.coinCount

      val newState = currentState
        .copy(userState = currentState.userState.copy(coinCount = currentCoin + wonCoin))

      setChatState(newState)
      request(SendMessage(msg.source, wonStr(wonCoin))).map(_ => ())
    }
  }

  def wrongGuess(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val currentGuessCount = currentState.gameState.guessCount

      val newState = currentState
        .copy(gameState = currentState.gameState.copy(guessCount = currentGuessCount + 1))

      setChatState(newState)
    }
  }

}

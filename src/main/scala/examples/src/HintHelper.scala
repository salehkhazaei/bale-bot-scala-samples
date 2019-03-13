package examples.src

import akka.actor.ActorSystem
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import examples.src.Strings.{buyStr, insufficientAmountStr, showSomeCharNotAllowedStr, showWordStr}

import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.duration._
import akka.pattern.after

trait HintHelper extends StateHelper with TelegramBot with Constants with LevelHelper with GameHelper {

  def checkCoins(limit: Int)(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, currentLevel) =>
      currentState.userState.coinCount >= limit
    }
  }

  def subtractCoins(price: Int)(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newCurrentState = currentState
        .copy(userState = currentState.userState.copy(
          coinCount = currentState.userState.coinCount - price
        ))

      setChatState(newCurrentState)
    }
  }

  def revealChars(response: String, revealedArray: IndexedSeq[Boolean], remaining: Int): IndexedSeq[Boolean] = {
    if (remaining == 0) {
      revealedArray
    } else {
      val revealCharIndex = Random.nextInt(revealedArray.size)

      val splitArray = revealedArray.splitAt(revealCharIndex)

      if (response(revealCharIndex) == ' ' || revealedArray(revealCharIndex)) {
        revealChars(response, revealedArray, remaining)
      } else {
        val newArray = splitArray._1 ++ Seq(true) ++ splitArray._2.tail
        revealChars(response, newArray, remaining - 1)
      }
    }
  }

  def calcRevealCount(response: String): Int = {
    if (response.filterNot(_ == ' ').length > 5)
      2
    else
      1
  }

  def showSomeChars(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      if (currentState.gameState.revealedChars.forall(_ == false)) {
        val currentResponse = currentLevel.get.response

        val newCurrentState = currentState
          .copy(gameState = currentState.gameState.copy(
            revealedChars = revealChars(currentResponse, currentResponse.map(_ => false), calcRevealCount(currentResponse))
          ))

        setChatState(newCurrentState)
      } else {
        request(SendMessage(msg.source, showSomeCharNotAllowedStr))
      }
    }
  }

  def showWord(implicit system: ActorSystem, msg: Message): Unit = {
    if (checkCoins(showWordPrice)) {
      subtractCoins(showWordPrice)

      withCurrentState { (currentState, currentLevel) =>
        request(SendMessage(msg.source, showWordStr(currentLevel.get.response)))
      }

      nextLevel

      after(1.second, system.scheduler) {
        Future.successful(sendCurrentGame())
      }
    } else {
      insufficientAmount
    }
  }

  def insufficientAmount(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, insufficientAmountStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        Seq(
          KeyboardButton(buyStr)
        )
      )
    ))))
  }

}

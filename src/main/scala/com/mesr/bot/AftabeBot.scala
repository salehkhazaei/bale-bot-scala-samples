package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.{Polling, RequestHandler, TelegramBot}
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._
import com.mesr.bot.Strings._
import io.circe.parser._
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.util.Try

class AftabeBot(token: String)(implicit _system: ActorSystem)
  extends TelegramBot
    with Polling
    with FixedPolling
    with Commands
    with InviteHelper
    with PaymentHelper
    with HintHelper
    with LevelHelper
    with MessageHandler
    with GameHelper
{
  override val system: ActorSystem = _system
  implicit val mat = ActorMaterializer()

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token,"tapi.bale.ai")

  onCommand("/start") { implicit msg =>
    request(SendMessage(msg.source, helloMessageStr))
  }

  onCommand("/game") { implicit msg =>
    withCheckFinished {
      sendCurrentGame()
    }
  }

  onCommand("/reset") { implicit msg =>
    setChatState(defaultState(msg.source))
  }

  onCommand("/help") { implicit msg =>
    request(SendMessage(msg.source, guidMessageStr))
  }

  onCommand("/buy") { implicit msg =>
    chooseNoOfCoinToBuy()
  }

  onCommand("/setInvite") { implicit msg =>
    enterInviteCode()(system, msg)
  }

  onTextFilter(showSomeCharsStr) { implicit msg =>
    withCheckFinished {
      if (checkCoins(showSomeCharsPrice)) {
        subtractCoins(showSomeCharsPrice)
        showSomeChars(msg)
        sendCurrentGame()
      } else {
        insufficientAmount
      }
    }
  }

  onTextFilter(showHelpStr) { implicit msg =>
    withCheckFinished {
      request(SendMessage(msg.source, guidMessageStr))
    }
  }

  onTextFilter(buyStr) { implicit msg =>
    withCheckFinished {
      chooseNoOfCoinToBuy()
    }
  }

  onTextFilter(returnButtonStr) { implicit msg =>
    withCheckFinished {
      sendCurrentGame()
    }
  }

  onTextFilter(inviteFriendsButtonStr) { implicit msg =>
    withCheckFinished {
      sendInviteCode()
    }
  }

  onTextFilter(showWordStr) { implicit msg =>
    withCheckFinished {
      showWord
    }
  }

  onTextFilter(text => text.startsWith(coinBuyButtonStartStr) && text.endsWith(coinBuyButtonEndStr)) { implicit msg =>
    withCheckFinished {
      val coinCount = msg.text.get.replace(coinBuyButtonStartStr, "").replace(coinBuyButtonEndStr, "").trim.toInt
      buyCoins(coinCount)
    }
  }

  onTextDefaultFilter { implicit msg =>
    withCheckFinished {
      if (isEnteringInviteCode) {
        Try(msg.text.get.toLong).toOption match {
          case Some(inviteCode) if isInviteCodeExists(inviteCode) && canSetInviter =>
            exitEnteringInviteCode
            setInviter(inviteCode)
            request(SendMessage(msg.source, inviteeSuccessStr))
            request(SendMessage(inviteCode, inviterSuccessStr))

          case Some(inviteCode) if !isInviteCodeExists(inviteCode) =>

            request(SendMessage(msg.source, inviteCodeNotFoundErrorStr))
          case Some(_) if !canSetInviter =>
            request(SendMessage(msg.source, alreadyInvitedErrorStr, replyMarkup = Some(ReplyKeyboardMarkup(
              Seq(
                Seq(KeyboardButton(returnButtonStr))
              )
            ))))

            exitEnteringInviteCode
          case None =>

            request(SendMessage(msg.source, inviteCodeNotNumberErrorStr))
        }

      } else {
        withCurrentState { (_, currentLevel) =>
          if (currentLevel.get.response == msg.text.get) {
            successGuess(msg)

            sendCurrentGame()
          } else {
            wrongGuess(msg)

            sendCurrentGame(wrongGuess = true)
          }
        }
      }
    }
  }

  onReceipt { implicit msg =>
    val payment = msg.successfulPayment.get
    val json = parse(payment.invoicePayload)
    val jsMap = json.right.toOption.flatMap(_.asObject).map(_.toMap).getOrElse(Map.empty)

    if (jsMap.get("status").flatMap(_.asString).getOrElse("FAILURE") == "SUCCESS") {
      successfulPayment(payment.totalAmount)
    }
  }
}

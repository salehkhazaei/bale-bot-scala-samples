package com.mesr.bot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{KeyboardButton, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._
import com.mesr.bot.helpers._
import com.mesr.bot.sdk.{BaleAkkaHttpClient, BalePolling, MessageHandler}
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.ExecutionContext

class AftabeBot(token: String)(implicit _system: ActorSystem)
  extends TelegramBot
    with BalePolling
    with Commands
    with InviteHelper
    with PaymentHelper
    with HintHelper
    with LevelHelper
    with MessageHandler
    with GameHelper
{
  override val system: ActorSystem = _system
  override val ec: ExecutionContext = executionContext

  implicit val userStateEncoder: Encoder[UserState] = deriveEncoder[UserState]
  implicit val userStateDecoder: Decoder[UserState] = deriveDecoder[UserState]

  implicit val gameStateEncoder: Encoder[GameState] = deriveEncoder[GameState]
  implicit val gameStateDecoder: Decoder[GameState] = deriveDecoder[GameState]

  implicit val requestLevelEncoder: Encoder[RequestLevel] = deriveEncoder[RequestLevel]
  implicit val requestLevelDecoder: Decoder[RequestLevel] = deriveDecoder[RequestLevel]

  override implicit val encoder: Encoder[AftabeState] = deriveEncoder[AftabeState]
  override implicit val decoder: Decoder[AftabeState] = deriveDecoder[AftabeState]

  implicit val mat: ActorMaterializer = ActorMaterializer()

  initializeState

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client: RequestHandler = new BaleAkkaHttpClient(token,"tapi.bale.ai")

  onCommand("/start") { implicit msg =>
    startGame
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
    help
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
    help
  }

  onTextFilter(buyStr) { implicit msg =>
    withCheckFinished {
      chooseNoOfCoinToBuy()
    }
  }

  onTextFilter(startButtonStr) { implicit msg =>
    withCheckFinished {
      sendCurrentGame()
    }
  }

  onTextFilter(showInviteCodeStr) { implicit msg =>
    withCheckFinished {
      enterInviteCode()(system, msg)
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

  onTextFilter(aftabeGhermezBuyStr) { implicit msg =>
    withCheckFinished {
      buyCoins(1)
    }
  }

  onTextFilter(aftabeHalabiBuyStr) { implicit msg =>
    withCheckFinished {
      buyCoins(2)
    }
  }

  onTextFilter(aftabeLaganBuyStr) { implicit msg =>
    withCheckFinished {
      buyCoins(4)
    }
  }

  onTextFilter(aftabeLagan7DastBuyStr) { implicit msg =>
    withCheckFinished {
      buyCoins(8)
    }
  }

  onTextFilter(addingNewLevelStr) { implicit msg =>
    addingNewLevelRequest()
  }

  onPhotoFilter { implicit msg =>
    addingNewLevelPhoto()
  }

  onTextDefaultFilter { implicit msg =>
    withCheckFinished {
      if (isEnteringLevelResponse) {
        setLevelText()

      } else if (isEnteringInviteCode) {
        handleInviteCode()

      } else {
        withCurrentState { (_, currentLevel) =>
          if (currentLevel.get.response == msg.text.get) {

            for {
              _ <- successGuess(msg)
              _ <- sendCurrentGame()
            } yield ()
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

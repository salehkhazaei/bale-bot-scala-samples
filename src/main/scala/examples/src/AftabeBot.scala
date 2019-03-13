import akka.actor.ActorSystem
import akka.pattern.after
import com.bot4s.telegram.api.Polling
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.InputFile.FileId
import com.bot4s.telegram.models._
import examples.src._

import scala.concurrent.Future
import scala.concurrent.duration._
import io.circe.parser._
import scala.util.{Random, Try}

case object GameFinished extends RuntimeException("Game has finished")

class AftabeBot(_system: ActorSystem, token: String) extends ExampleBot(token)(_system)
  with Polling with FixedPolling with Commands with PerChatState[AftabeState] {

  override val system: ActorSystem = _system
  val config = BotConfig.load()
  val gameCardNo = config.getString("bot.card-no")

  val db = Database(
    IndexedSeq(
      Level("1856114092:-2456185831143174655:1", "رنگین کمان"),
      Level("1856114092:-2456185831143174655:1", "کمان رنگی")
    )
  )
  val showSomeCharsPrice = 10
  val showWordPrice = 50
  val helloMessageStr =
    """
      |*کاربر گرامی* شما از طریق این بازو می توانید با حدس عکس، یک تجربه *سرگرمی هیجان انگیز* در بله داشته باشید. برای شروع این *چالش* روی *شروع* کلیک کنید.
      |[/game](send:/game) - شروع بازی
      |[/buy](send:/buy) - خرید سکه
      |[/setInvite](send:/setInvite) - وارد کردن کد دعوت
      |[/reset](send:/reset) - ریست همه چیز!
      |[/help](send:/help) - راهنما
    """.stripMargin

  val enterYourResponseStr = "کلمه مورد نظر خود را وارد کن:"
  val noOfCoinsStr = "تعداد سکه شما:‌ "
  val levelStr = "مرحله "
  val guidMessageStr = "راهنما"
  val wrongGuessStr = "*حدس شما اشتباه بود*"
  val wonCoin = 50
  val startCoin = 400
  val wonStr = "*حدس شما درست بود. شما برنده " + wonCoin + " سکه شدید.*"
  val insufficientAmountStr = "سکه‌های شما کافی نیست."
  val gameFinishedStr = "شما به پایان بازی رسیدید. منتظر مراحل جدید باشید."
  val showSomeCharNotAllowed = "شما یک‌بار از این قابلیت استفاده کرده‌اید."
  def showWord(response: String): String = "جواب: *" + response + "*"

  val buyStr = "خرید سکه"
  val showSomeCharsStr = "نمایش چند حرف"
  val showWordStr = "نمایش کل کلمه"
  val showHelpStr = "راهنما"
  val returnButtonStr = "بازگشت"
  val inviteFriendsButtonStr = "دعوت دوستان"
  val coinAmounts = Map(50 -> 1, 100 -> 2, 150 -> 3)

  val coinBuyStr =
    """
      |لطفا تعداد سکه‌هایی که می‌خواهید را مشخص کنید.
      |نکته: با معرفی بازو به دوستان خود می توانید به ازای هر دوست 20 سکه بگیرید
    """.stripMargin

  val enterInviteCodeStr = "لطفا کد دعوت دوست خود را وارد کنید."
  val coinBuyButtonStartStr = "خرید "
  val coinBuyButtonEndStr = " سکه"
  def coinBuyButtonStr(coinCount: Int): String = coinBuyButtonStartStr + coinCount + coinBuyButtonEndStr
  def coinBuyLabelStr(coinCount: Int): String = "خرید " + coinCount + " سکه بازی آفتابه"
  def successfulPaymentStr(paidCoinCount: Int, newCoinCount: Int): String =
    s"""
      |شما $paidCoinCount سکه خریده‌اید.
      |تعداد کد سکه‌ها: $newCoinCount
    """.stripMargin

  def coinStr(coinCount: Int) = "تعداد سکه‌های شما: " + coinCount

  def inviteStr(userId: Long): String =
    s"""
      |کد دعوت شما: $userId
      |با وارد کردن این کد توسط دوست شما، 20 سکه به حساب شما و 10 سکه به حساب دوست شما اضافه می شود.
      |
    """.stripMargin

  def defaultGame(level: Int = 0) = GameState(level, 0, Seq.empty)

  def defaultState(userId: Long) = AftabeState(UserState(userId, startCoin, isEnteringInviteCode = false, None), defaultGame())

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

  def withCurrentState[S, T](f: (AftabeState, Option[Level]) => T)(implicit msg: Message): T = {
    withChatState { implicit optState =>
      val currentState = optState.getOrElse(defaultState(msg.source))
      val currentLevel = Try(db.levels(currentState.gameState.level)).toOption
      setChatState(currentState)

      f(currentState, currentLevel)
    }
  }

  def sendInviteCode()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, inviteStr(msg.source)))
  }

  def sendCurrentGame(wrongGuess: Boolean = false)(implicit msg: Message): Unit = {
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
      request(SendMessage(msg.source, wonStr))
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

  def nextLevel(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState
        .copy(gameState = defaultGame(currentState.gameState.level + 1))

      setChatState(newState)
    }
  }

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
    enterInviteCode()
  }

  def enterInviteCode()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      after(1.second, system.scheduler) {
        val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = true))

        Future.successful(setChatState(newState))
      }

      request(SendMessage(msg.source, enterInviteCodeStr))
    }
  }

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
        request(SendMessage(msg.source, showSomeCharNotAllowed))
      }
    }
  }

  def showWord(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newCurrentState = currentState
        .copy(gameState = currentState.gameState.copy(
          revealedChars = currentLevel.get.response.map(_ => true)
        ))

      setChatState(newCurrentState)
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

  def chooseNoOfCoinToBuy()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, coinBuyStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        coinAmounts.map { element =>
          KeyboardButton(coinBuyButtonStr(element._1))
        }.toSeq,
        Seq(
          KeyboardButton(inviteFriendsButtonStr),
          KeyboardButton(returnButtonStr)
        )
      )
    ))))
  }

  def buyCoins(coinCount: Int)(implicit msg: Message): Unit = {
    request(SendInvoice(
      chatId = msg.source,
      title = coinBuyButtonStr(coinCount),
      description = "",
      payload = "buy_" + coinCount,
      providerToken = gameCardNo, startParameter = gameCardNo,
      currency = Currency.YER,
      prices = Array(LabeledPrice(label = coinBuyLabelStr(coinCount), coinAmounts(coinCount)))
    ))
  }

  def successfulPayment(amount: Long)(implicit msg: Message): Unit = {
    val paidCoinCount = coinAmounts.find(_._2 == amount).get._1
    withCurrentState { (currentState, currentLevel) =>
      val newCoinCount = currentState.userState.coinCount + paidCoinCount
      val newState = currentState
        .copy(userState = currentState.userState.copy(coinCount = newCoinCount))

      setChatState(newState)
      request(SendMessage(msg.source, successfulPaymentStr(paidCoinCount, newCoinCount), replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(KeyboardButton(returnButtonStr))
        )
      ))))
    }
  }

  def exitEnteringInviteCode()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = false))

      setChatState(newState)
    }
  }

  def setInviter(inviter: Long)(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState.copy(userState = currentState.userState.copy(invitedBy = Some(inviter)))

      setChatState(newState)
    }
  }

  def isEnteringInviteCode()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, currentLevel) =>
      currentState.userState.isEnteringInviteCode
    }
  }

  def isInviteCodeExists(inviteCode: Long): Boolean = {
    getStateOfUser(inviteCode).isDefined
  }

  def canSetInviter()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, currentLevel) =>
      currentState.userState.invitedBy.isEmpty
    }
  }

  onMessage { implicit msg =>
    withCheckFinished {
      msg.successfulPayment match {
        case Some(payment) =>
          val json = parse(payment.invoicePayload)
          val jsMap = json.right.toOption.flatMap(_.asObject).map(_.toMap).getOrElse(Map.empty)

          if (jsMap.get("status").flatMap(_.asString).getOrElse("FAILURE") == "SUCCESS") {
            successfulPayment(payment.totalAmount)
          }
        case _ =>
      }

      val inviteCodeNotNumberErrorStr = "کد دعوت باید عدد باشد. لطفا مجددا تلاش کنید."
      val alreadyInvitedErrorStr = "شما قبلا دعوت شده‌اید."
      val inviteCodeNotFoundErrorStr = "کد دعوت اشتباه است. لطفا مجددا تلاش کنید."
      val inviterSuccessStr = "شما ۲۰ امتیاز بابت دعوت دوست‌تان گرفتید."
      val inviteeSuccessStr = "شما ۲۰ امتیاز بابت استفاده از کد دعوت دوست‌تان گرفتید."

      msg.text match {
        case Some(inviteCodeStr) if isEnteringInviteCode =>
          Try(inviteCodeStr.toLong).toOption match {
            case Some(inviteCode) if isInviteCodeExists(inviteCode) && canSetInviter =>
              exitEnteringInviteCode
              setInviter(inviteCode)
              request(SendMessage(msg.source, inviteeSuccessStr))
              request(SendMessage(inviteCode, inviterSuccessStr))

            case Some(inviteCode) if !isInviteCodeExists(inviteCode) =>

              request(SendMessage(msg.source, inviteCodeNotFoundErrorStr))
            case Some(inviteCode) if !canSetInviter =>
              request(SendMessage(msg.source, alreadyInvitedErrorStr, replyMarkup = Some(ReplyKeyboardMarkup(
                Seq(
                  Seq(KeyboardButton(returnButtonStr))
                )
              ))))

              exitEnteringInviteCode
            case None =>

              request(SendMessage(msg.source, inviteCodeNotNumberErrorStr))
          }

        case Some(command) if command.startsWith("/") => //ignore commands

        case Some(templateResponse) if templateResponse == showSomeCharsStr =>
          if (checkCoins(showSomeCharsPrice)) {
            subtractCoins(showSomeCharsPrice)
            showSomeChars(msg)
            sendCurrentGame()
          } else {
            insufficientAmount
          }
        case Some(templateResponse) if templateResponse == showWordStr =>
          if (checkCoins(showWordPrice)) {
            subtractCoins(showWordPrice)

            withCurrentState { (currentState, currentLevel) =>
              request(SendMessage(msg.source, showWord(currentLevel.get.response)))
            }

            nextLevel

            after(1.second, system.scheduler) {
              Future.successful(sendCurrentGame())
            }
          } else {
            insufficientAmount
          }

        case Some(templateResponse) if templateResponse == showHelpStr =>
          request(SendMessage(msg.source, guidMessageStr))

        case Some(templateResponse) if templateResponse == buyStr =>
          chooseNoOfCoinToBuy()

        case Some(templateResponse) if templateResponse == returnButtonStr =>
          sendCurrentGame()

        case Some(templateResponse) if templateResponse == inviteFriendsButtonStr =>
          sendInviteCode()

        case Some(templateResponse) if templateResponse.startsWith(coinBuyButtonStartStr) && templateResponse.endsWith(coinBuyButtonEndStr) =>
          val coinCount = templateResponse.replace(coinBuyButtonStartStr, "").replace(coinBuyButtonEndStr, "").trim.toInt
          buyCoins(coinCount)

        case Some(guess) =>
          withCurrentState { (_, currentLevel) =>
            if (currentLevel.get.response == guess) {
              successGuess(msg)

              sendCurrentGame()
            } else {
              wrongGuess(msg)

              sendCurrentGame(wrongGuess = true)
            }
          }
        case _ =>
      }
    }
  }
}

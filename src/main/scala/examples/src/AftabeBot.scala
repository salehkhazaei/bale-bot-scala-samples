import akka.actor.ActorSystem
import com.bot4s.telegram.api.Polling
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.InputFile.FileId
import com.bot4s.telegram.models._
import examples.src._

import scala.concurrent.duration._
import akka.pattern.after

import scala.concurrent.Future
import scala.util.{Random, Try}

class AftabeBot(_system: ActorSystem, token: String) extends ExampleBot(token)(_system)
  with Polling with FixedPolling with Commands with PerChatState[AftabeState] {

  override val system: ActorSystem = _system
  val config = BotConfig.load()

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
      |[/reset](send:/reset) - ریست همه چیز!
      |[/help](send:/help) - راهنما
    """.stripMargin

  val enterYourResponseStr = "کلمه مورد نظر خود را وارد کن:"
  val guidMessageStr = "راهنما"
  val wrongGuessStr = "*حدس شما اشتباه بود*"
  val wonCoin = 50
  val startCoin = 400
  val wonStr = "*حدس شما درست بود. شما برنده " + wonCoin + " سکه شدید.*"
  val insufficientAmountStr = "سکه‌های شما کافی نیست."
  val showSomeCharNotAllowed = "شما یک‌بار از این قابلیت استفاده کرده‌اید."
  def showWord(response: String): String = "جواب: *" + response + "*"

  val buyStr = "خرید سکه"
  val showSomeCharsStr = "نمایش چند حرف"
  val showWordStr = "نمایش کل کلمه"
  val showHelpStr = "راهنما"

  def coinStr(coinCount: Int) = "تعداد سکه‌های شما: " + coinCount

  def defaultGame(level: Int = 0) = GameState(level, 0, Seq.empty)

  def defaultState(userId: Long) = AftabeState(UserState(userId, startCoin), defaultGame())

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

  def withCurrentState[S, T](f: (AftabeState, Level) => T)(implicit msg: Message): T = {
    withChatState { implicit optState =>
      val currentState = optState.getOrElse(defaultState(msg.source))
      val currentLevel = db.levels(currentState.gameState.level)
      setChatState(currentState)

      f(currentState, currentLevel)
    }
  }

  def sendCurrentGame(wrongGuess: Boolean = false)(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      request(SendPhoto(msg.source, FileId(currentLevel.fileId)))

      val isShowCharsAllowed = currentState.gameState.revealedChars.forall(_ == false)

      after(500.milliseconds, system.scheduler) {
        val responseText =
          s"""
             |${if (wrongGuess) wrongGuessStr else ""}
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
    sendCurrentGame()
  }

  onCommand("/reset") { implicit msg =>
    setChatState(defaultState(msg.source))
  }

  onCommand("/help") { implicit msg =>
    request(SendMessage(msg.source, guidMessageStr))
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
        val newCurrentState = currentState
          .copy(gameState = currentState.gameState.copy(
            revealedChars = revealChars(currentLevel.response, currentLevel.response.map(_ => false), calcRevealCount(currentLevel.response))
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
          revealedChars = currentLevel.response.map(_ => true)
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

  onMessage { implicit msg =>
    msg.text match {
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
            request(SendMessage(msg.source, showWord(currentLevel.response)))
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
        request(SendMessage(msg.source, "خرید"))

      case Some(guess) =>
        withCurrentState { (_, currentLevel) =>
          if (currentLevel.response == guess) {
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

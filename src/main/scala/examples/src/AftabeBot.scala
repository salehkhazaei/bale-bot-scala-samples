import akka.actor.ActorSystem
import com.bot4s.telegram.api.Polling
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.InputFile.FileId
import com.bot4s.telegram.models.{KeyboardButton, ReplyKeyboardMarkup}
import examples.src._

class AftabeBot(_system: ActorSystem, token: String) extends ExampleBot(token)(_system)
  with Polling with FixedPolling with Commands with PerChatState[AftabeState] {

  override val system: ActorSystem = _system
  val config = BotConfig.load()

  val db = Database(
    IndexedSeq(
      Level("1856114092:-2456185831143174655:1", "رنگین کمان")
    )
  )

  val helloMessage = "*کاربر گرامی* شما از طریق این بازو می توانید با حدس عکس، یک تجربه *سرگرمی هیجان انگیز* در بله داشته باشید. برای شروع این *چالش* روی *شروع* کلیک کنید."

  def coinStr(coinCount: Int ) = "تعداد سکه‌های شما: " + coinCount

  def defaultState(userId: Long) = AftabeState(UserState(userId, 50, 0), None)

  def defaultGame() = GameState(0, 0, Seq.empty)

  def createResponse(gameState: GameState): String = {
    db.levels(gameState.level).response.zipWithIndex.map { char =>
      if (char._1 == ' ')
        "  "
      else if (char._2 >= gameState.revealedChars.length)
        "-- "
      else if (gameState.revealedChars(char._2))
        s"${char._1}"
      else
        "-- "
    }.mkString
  }

  onCommand("/start") { implicit msg =>
    withChatState { s =>
      val currentState = s.getOrElse(defaultState(msg.source))
      val currentGame = currentState.currentGameState.getOrElse(defaultGame())
      val currentLevel = db.levels(currentGame.level)
      setChatState(currentState)

      request(SendMessage(msg.source, helloMessage + "\n" + coinStr(currentState.userState.coinCount)))
      request(SendMessage(msg.source, helloMessage + "\n" + coinStr(currentState.userState.coinCount), replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(KeyboardButton("salam"), KeyboardButton("salam")),
          Seq(KeyboardButton("salam"), KeyboardButton("salam"))
        )
      ))))

      import com.bot4s.telegram.marshalling.sendPhotoEncoder

      val response = createResponse(currentGame)
      logger.debug("response: {}", response)
      val req = SendPhoto(msg.source, FileId(currentLevel.fileId), Some(response))
      println(com.bot4s.telegram.marshalling.toJson(req))
      request(req)
    }
  }

  onCommand("/game") { implicit msg =>
    withChatState { s =>
      val currentState = s.getOrElse(defaultState(msg.source))
      val currentGame = currentState.currentGameState.getOrElse(defaultGame())
      val currentLevel = db.levels(currentGame.level)
      setChatState(currentState)

      import com.bot4s.telegram.marshalling.sendPhotoEncoder

      val req = SendPhoto(msg.source, FileId(currentLevel.fileId), Some(createResponse(currentGame)))
      println(com.bot4s.telegram.marshalling.toJson(req))
      request(req)
    }
  }
}

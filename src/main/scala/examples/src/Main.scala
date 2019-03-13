import akka.actor.ActorSystem
import examples.src.BotConfig

object Main extends App {
  val config = BotConfig.load()
  implicit val system = ActorSystem("bot", config)
  val bot = new AftabeBot(config.getString("bot.token"))
  bot.run()
}

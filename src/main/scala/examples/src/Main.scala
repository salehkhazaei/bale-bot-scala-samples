import akka.actor.ActorSystem
import examples.src.BotConfig

object Main extends App {
  val config = BotConfig.load()
  val system = ActorSystem("bot", config)
  val bot = new AftabeBot(system, config.getString("bot.token"))
  bot.run()
}

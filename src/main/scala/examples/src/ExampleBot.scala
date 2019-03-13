import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.{RequestHandler, TelegramBot}
import com.bot4s.telegram.clients.{AkkaHttpClient, SttpClient}
import examples.src.BaleAkkaHttpClient
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

/** Quick helper to spawn example bots.
  *
  * Mix Polling or Webhook accordingly.
  *
  * Example:
  * new AftabeBot("123456789:qwertyuiopasdfghjklyxcvbnm123456789").run()
  *
  * @param token Bot's token.
  */
abstract class ExampleBot(token: String)(implicit system: ActorSystem) extends TelegramBot {
  LoggerConfig.factory = PrintLoggerFactory()
  //   set log level, e.g. to TRACE
  LoggerConfig.level = LogLevel.TRACE

  implicit val mat = ActorMaterializer()
  override val client: RequestHandler = new BaleAkkaHttpClient(token,"tapi.bale.ai")
}

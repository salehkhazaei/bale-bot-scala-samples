package examples.src

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.marshalling.AkkaHttpMarshalling
import com.bot4s.telegram.marshalling._
import com.bot4s.telegram.methods.{Request, Response}
import io.circe.{Decoder, Encoder}
import slogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

/** Akka-backed Telegram Bot API client
  * Provide transparent camelCase <-> underscore_case conversions during serialization/deserialization
  *
  * @param token Bot token
  */
class BaleAkkaHttpClient(token: String, telegramHost: String = "api.telegram.org")(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) extends RequestHandler with StrictLogging {
  import AkkaHttpMarshalling._
  private val apiBaseUrl = s"https://$telegramHost/bot$token/"
  private val http = Http()

  override def sendRequest[R, T <: Request[_]](request: T)(implicit encT: Encoder[T], decR: Decoder[R]): Future[R] = {
    Marshal(request).to[RequestEntity]
      .map {
        re =>
          HttpRequest(HttpMethods.POST, Uri(apiBaseUrl + request.methodName), entity = re)
      }
      .flatMap(http.singleRequest(_))
      .flatMap{r =>
        val newEnt = r.entity.transformDataBytes(Flow.fromFunction[ByteString, ByteString]{i =>
          ByteString(i.utf8String.replace("IRR", "THB"))
        })
        Unmarshal(newEnt).to[Response[R]]}
      .map(t => processApiResponse[R](t))
  }
}
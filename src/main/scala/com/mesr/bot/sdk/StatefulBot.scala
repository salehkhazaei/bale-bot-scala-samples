package com.mesr.bot.sdk

import akka.actor.ActorSystem
import com.bot4s.telegram.models.Message
import com.mesr.bot.sdk.db.{ObjectSerializer, RedisExtension}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Simple extension for having stateful Telegram Bots (per chat).
  * The main issue is locking/synchronization, actors (FSM) are a better alternative.
  * This can be easily adapted to handle per-user or per-user+chat state.
  */
trait StatefulBot[S] {
  val system: ActorSystem
  val ec: ExecutionContext
  implicit val encoder: Encoder[S]
  implicit val decoder: Decoder[S]

  private val chatState = collection.mutable.Map[Long, S]()
  lazy val redisExt = RedisExtension(system)

  def initializeState: Future[Seq[Option[S]]] = load.map { chatStateMap =>
    chatStateMap.map { entry =>
      chatState.put(entry._1, entry._2)
    }.toSeq
  }(ec)

  def load: Future[Map[Long, S]] = {
    redisExt.get("chat-state").map { optResp =>
      optResp.flatMap { resp =>
        val json = decode[Map[String, String]](resp)

        json.toOption
      }.map { map =>
        map.map { entry =>
          (entry._1.toLong, ObjectSerializer.deSerialize(entry._2))
        }
      }.getOrElse(Map.empty)
    }(ec)
  }

  def save: Future[Unit] = {
    val jsonString = chatState.toMap
      .mapValues(v => ObjectSerializer.serialize(v))
      .map(v => (v._1.toString, Json.fromString(v._2)))
      .asJson
      .toString()

    redisExt.set("chat-state", jsonString).map(_ => ())(ec)
  }

  def setChatState(value: S)(implicit msg: Message): Unit = atomic {
    chatState.update(msg.chat.id, value)

    save
  }

  def clearChatState(implicit msg: Message): Unit = atomic {
    chatState.remove(msg.chat.id)

    save
  }

  private def atomic[T](f: => T): T = chatState.synchronized {
    f
  }

  def withChatState[T](f: Option[S] => T)(implicit msg: Message): T = f(getChatState)

  def getStateOfUser(chatId: Long): Option[S] = chatState.get(chatId)

  def getChatState(implicit msg: Message): Option[S] = atomic {
    chatState.get(msg.chat.id)
  }
}

package com.mesr.bot.db

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import ObjectSerializer._
import com.mesr.bot.db.entities._
import io.circe.{Decoder, Encoder}

import scala.concurrent.{ExecutionContext, Future}

final class RedisExtensionImpl(system: ActorSystem) extends Extension {
  private implicit val ec: ExecutionContext = system.dispatcher
  private val redisClientActor: ActorRef = system.actorOf(Props(classOf[RedisClientActor]), "RedisClient")

  private implicit val timeout: Timeout = Timeout(10, TimeUnit.SECONDS)

  def hmsetex(key: String, map: Map[Any, Any], expirationTimeInSeconds: Int): Future[Boolean] =
    (redisClientActor ? RedisHMSetEX(key, map, expirationTimeInSeconds)).mapTo[Boolean]

  def psetex(key: String, expirationTimeInSeconds: Long, value: Any): Future[Boolean] = {
    val expirationTimeInMillis = expirationTimeInSeconds * 1000
    (redisClientActor ? RedisPSetEX(key, expirationTimeInMillis, value)).mapTo[Boolean]
  }

  def set(key: String, value: Any): Future[Boolean] =
    (redisClientActor ? RedisSet(key, value)).mapTo[Boolean]

  def delete(key: String): Future[Option[Long]] =
    (redisClientActor ? RedisDelete(key)).mapTo[Option[Long]]

  def keys(key: String): Future[Option[List[Option[String]]]] =
    (redisClientActor ? RedisKeys(key)).mapTo[Option[List[Option[String]]]]

  def hgetall(key: String): Future[Option[Map[String, String]]] =
    (redisClientActor ? RedisHGetAll(key)).mapTo[Option[Map[String, String]]]

  def get(key: String): Future[Option[String]] =
    (redisClientActor ? RedisGet(key)).mapTo[Option[String]]

  def setnx(key: String, value: Any): Future[Boolean] =
    (redisClientActor ? RedisSetNX(key, value)).mapTo[Boolean]

  def increase(key: String): Future[Option[Long]] =
    (redisClientActor ? RedisIncrease(key)).mapTo[Option[Long]]

  def setObj[T](key: String, expirationTimeInSeconds: Long, value: T)(implicit encoder: Encoder[T]): Future[Boolean] = {
    val expirationTimeInMillis = expirationTimeInSeconds * 1000
    (redisClientActor ? RedisPSetEX(key, expirationTimeInMillis, serialize(value))).mapTo[Boolean]
  }

  def getObj[T](key: String)(implicit decoder: Decoder[T]): Future[Option[T]] = {
    (redisClientActor ? RedisGet(key)).mapTo[Option[String]].map { optValue ⇒
      optValue.map { value ⇒
        deSerialize[T](value)
      }
    }
  }

  def phset(key: String, field: String, expirationTimeInSeconds: Int, value: Any): Future[Boolean] =
    (redisClientActor ? RedisPHSetEX(key, field, expirationTimeInSeconds, value)).mapTo[Boolean]
}

object RedisExtension extends ExtensionId[RedisExtensionImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): RedisExtensionImpl = new RedisExtensionImpl(system)

  override def lookup(): ExtensionId[_ <: Extension] = RedisExtension
}

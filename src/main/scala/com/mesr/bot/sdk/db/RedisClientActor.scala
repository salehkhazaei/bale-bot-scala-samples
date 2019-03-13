package com.mesr.bot.sdk.db

import akka.actor.Actor
import com.mesr.bot.sdk.db.entities._
import com.redis.{RedisClient, RedisClientPool}
import com.typesafe.config.Config

class RedisClientActor extends Actor {

  val config: Config = context.system.settings.config

  private lazy val redisClients = new RedisClientPool(
    config.getString("redis.host"),
    config.getInt("redis.port"),
    secret = Some(config.getString("redis.password")),
    maxIdle = 20
  )

  private def getClient: RedisClient = redisClients.withClient[RedisClient] { cli ⇒ cli }

  override def receive: Receive = {
    case RedisPSetEX(key, expiryInMillis, value) ⇒ sender() ! getClient.psetex(key, expiryInMillis, value)
    case RedisPHSetEX(key, field, expiryInSeconds, value) ⇒
      val r1 = getClient.hset(key, field, value)
      val r2 = if (r1) getClient.expire(key, expiryInSeconds) else r1

      sender() ! r2
    case RedisSet(key, value) ⇒ sender() ! getClient.set(key, value)
    case RedisDelete(key)     ⇒ sender() ! getClient.del(key)
    case RedisKeys(key)       ⇒ sender() ! getClient.keys(key)
    case RedisGet(key)        ⇒ sender() ! getClient.get(key)
    case RedisHMSetEX(key, map, expiryInSeconds) ⇒
      val replyTo = sender()
      if (getClient.hmset(key, map)) {
        replyTo ! getClient.expire(key, expiryInSeconds)
      } else {
        replyTo ! false
      }
    case RedisHGetAll(key)      ⇒ sender() ! getClient.hgetall1(key)
    case RedisSetNX(key, value) ⇒ sender() ! getClient.setnx(key, value)
    case RedisIncrease(key)     ⇒ sender() ! getClient.incr(key)
    case msg                    ⇒ context.system.log.error(s"Unsupported message in ${this.getClass}: ${msg.toString}")
  }
}

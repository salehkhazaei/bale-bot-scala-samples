package com.mesr.bot.sdk.db.entities

trait RedisCommands {
  val key: String
}

case class RedisHMSetEX(
  key:             String,
  map:             Map[Any, Any],
  expiryInSeconds: Int
) extends RedisCommands

case class RedisSet(
  key:   String,
  value: Any
) extends RedisCommands

case class RedisDelete(
  key: String
) extends RedisCommands

case class RedisKeys(
  key: String
) extends RedisCommands

case class RedisPSetEX(
  key:            String,
  expiryInMillis: Long,
  value:          Any
) extends RedisCommands

case class RedisPHSetEX(
  key:             String,
  field:           String,
  expiryInSeconds: Int,
  value:           Any
) extends RedisCommands

case class RedisHGetAll(
  key: String
) extends RedisCommands

case class RedisGet(
  key: String
) extends RedisCommands

case class RedisIncrease(
  key: String
) extends RedisCommands

case class RedisSetNX(
  key:   String,
  value: Any
) extends RedisCommands
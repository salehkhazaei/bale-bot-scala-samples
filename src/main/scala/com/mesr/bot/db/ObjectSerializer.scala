package com.mesr.bot.db
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

object ObjectSerializer {

  def serialize[T](obj: T)(implicit encoder: Encoder[T]): String =
    obj.asJson.toString()

  def deSerialize[T](str: String)(implicit decoder: Decoder[T]): T =
    decode[T](str).right.get

}

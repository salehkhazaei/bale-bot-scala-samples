package com.mesr.bot

import com.mesr.bot.sdk.BotConfig
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

trait Constants {
  val conf: Config = BotConfig.load()
  val botConf: Config = conf.getConfig("bot")

  val gameCardNo: String = botConf.getString("card-no")
  val showSomeCharsPrice: Int = botConf.getInt("show-some-chars-price")
  val showWordPrice: Int = botConf.getInt("show-word-price")
  val wonCoin: Int = botConf.getInt("won-coin")
  val startCoin: Int = botConf.getInt("start-coin")
  val inviterGiftCoin: Int = botConf.getInt("inviter-gift-coin")
  val inviteeGiftCoin: Int = botConf.getInt("invitee-gift-coin")
  val finishedFileId: String = botConf.getString("finished-file-id")
  val fileBaseUrl: String = botConf.getString("file-base-url")

  val aftabeGhermezFileId: String = "1856114092:8645451133117009665:1"
  val aftabeHalabiFileId: String = "1856114092:1500138480572435968:1"
  val aftabeLaganFileId: String = "1856114092:-4703077424920719615:1"
  val aftabeLagan7DastFileId: String = "1856114092:-1921612073810129150:1"

  val levels: Seq[Level] = {
    conf.getConfigList("levels").toSeq.map { level =>
      Level(level.getString("file-id"), level.getString("response"))
    }
  }

  val coinAmounts = Map(1 -> 50, 2-> 90, 4 -> 150, 8 -> 200)

  def getCoinPhotoUrl(coinCount: Int): String = fileBaseUrl + getCoinPhoto(coinCount)

  def getCoinPhoto(coinCount: Int): String = coinCount match {
    case 1 => aftabeGhermezFileId
    case 2 => aftabeHalabiFileId
    case 4 => aftabeLaganFileId
    case 8 => aftabeLagan7DastFileId
  }
}

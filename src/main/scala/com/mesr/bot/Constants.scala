package com.mesr.bot

import com.mesr.bot.sdk.BotConfig
import com.typesafe.config.Config

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

  val coinAmounts = Map(1 -> 100, 2-> 200, 4 -> 300, 8 -> 400)
}

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

  val coinAmounts = Map(50 -> 1, 100 -> 2, 150 -> 3)
}

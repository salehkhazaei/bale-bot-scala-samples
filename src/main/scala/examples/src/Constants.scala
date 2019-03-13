package examples.src

trait Constants {
  val conf = BotConfig.load()

  val gameCardNo = conf.getString("bot.card-no")

  val showSomeCharsPrice = 10
  val showWordPrice = 50
  val wonCoin = 50
  val startCoin = 400
  val coinAmounts = Map(50 -> 1, 100 -> 2, 150 -> 3)


}

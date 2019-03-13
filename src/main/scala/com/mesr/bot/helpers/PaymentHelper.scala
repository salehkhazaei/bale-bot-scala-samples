package com.mesr.bot.helpers

import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.{SendInvoice, SendMessage}
import com.bot4s.telegram.models._
import com.mesr.bot.Strings._
import com.mesr.bot.Constants

trait PaymentHelper extends StateHelper with TelegramBot with Constants {

  def chooseNoOfCoinToBuy()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, coinBuyStr, replyMarkup = Some(ReplyKeyboardMarkup(
      Seq(
        coinAmounts.map { element =>
          KeyboardButton(coinBuyButtonStr(element._1))
        }.toSeq,
        Seq(
          KeyboardButton(inviteFriendsButtonStr),
          KeyboardButton(returnButtonStr)
        )
      )
    ))))
  }

  def buyCoins(coinCount: Int)(implicit msg: Message): Unit = {
    request(SendInvoice(
      chatId = msg.source,
      title = coinBuyButtonStr(coinCount),
      description = "",
      payload = "buy_" + coinCount,
      providerToken = gameCardNo, startParameter = gameCardNo,
      currency = Currency.YER,
      prices = Array(LabeledPrice(label = coinBuyLabelStr(coinCount), coinAmounts(coinCount)))
    ))
  }

  def successfulPayment(amount: Long)(implicit msg: Message): Unit = {
    val paidCoinCount = coinAmounts.find(_._2 == amount).get._1
    withCurrentState { (currentState, _) =>
      val newCoinCount = currentState.userState.coinCount + paidCoinCount
      val newState = currentState
        .copy(userState = currentState.userState.copy(coinCount = newCoinCount))

      setChatState(newState)
      request(SendMessage(msg.source, successfulPaymentStr(paidCoinCount, newCoinCount), replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(KeyboardButton(returnButtonStr))
        )
      ))))
    }
  }
}

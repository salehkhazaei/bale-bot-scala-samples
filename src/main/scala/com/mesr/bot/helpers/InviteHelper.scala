package com.mesr.bot.helpers

import akka.actor.ActorSystem
import akka.pattern.after
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{KeyboardButton, Message, ReplyKeyboardMarkup}
import com.mesr.bot.Strings._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

trait InviteHelper extends StateHelper with TelegramBot {
  def sendInviteCode()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, inviteStr(msg.source)))
  }

  def enterInviteCode()(implicit system: ActorSystem, msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      after(1.second, system.scheduler) {
        val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = true))

        Future.successful(setChatState(newState))
      }

      request(SendMessage(msg.source, enterInviteCodeStr, replyMarkup = Some(ReplyKeyboardMarkup(
        Seq(
          Seq(
            KeyboardButton(returnButtonStr)
          )
        )
      ))))
    }
  }

  def exitEnteringInviteCode()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = false))

      setChatState(newState)
    }
  }

  def setInviter(inviter: Long)(implicit msg: Message): Unit = {
    withCurrentState { (currentState, _) =>
      val newCoinCount = currentState.userState.coinCount + inviteeGiftCoin
      val newState = currentState.copy(userState = currentState.userState
        .copy(invitedBy = Some(inviter))
        .copy(coinCount = newCoinCount)
      )

      setChatState(newState)

      increaseInviterCoin(inviter)
    }
  }

  def increaseInviterCoin(inviter: Long): Unit = {
    for {
      currentState <- getStateOfUser(inviter)
    } yield {
      val newCoinCount = currentState.userState.coinCount + inviterGiftCoin
      val newState = currentState.copy(userState = currentState.userState
        .copy(coinCount = newCoinCount)
      )

      setStateOfUser(inviter, newState)
    }
  }

  def isEnteringInviteCode()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, _) =>
      currentState.userState.isEnteringInviteCode
    }
  }

  def isInviteCodeExists(inviteCode: Long): Boolean = {
    getStateOfUser(inviteCode).isDefined
  }

  def canSetInviter()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, _) =>
      currentState.userState.invitedBy.isEmpty
    }
  }

  def handleInviteCode()(implicit msg: Message): Unit = {
    Try(msg.text.get.toLong).toOption match {
      case Some(inviteCode) if isInviteCodeExists(inviteCode) && canSetInviter =>
        exitEnteringInviteCode
        setInviter(inviteCode)

        for {
          _ <- request(SendMessage(msg.source, inviteeSuccessStr))
          _ <- request(SendMessage(inviteCode, inviterSuccessStr))
        } yield ()

      case Some(inviteCode) if !isInviteCodeExists(inviteCode) =>

        request(SendMessage(msg.source, inviteCodeNotFoundErrorStr))
      case Some(_) if !canSetInviter =>
        request(SendMessage(msg.source, alreadyInvitedErrorStr, replyMarkup = Some(ReplyKeyboardMarkup(
          Seq(
            Seq(KeyboardButton(returnButtonStr))
          )
        ))))

        exitEnteringInviteCode
      case None =>

        request(SendMessage(msg.source, inviteCodeNotNumberErrorStr))
    }
  }

}

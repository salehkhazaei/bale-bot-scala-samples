package com.mesr.bot.helpers

import akka.actor.ActorSystem
import akka.pattern.after
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.Message
import com.mesr.bot.Strings.{enterInviteCodeStr, inviteStr}

import scala.concurrent.Future
import scala.concurrent.duration._

trait InviteHelper extends StateHelper with TelegramBot {
  def sendInviteCode()(implicit msg: Message): Unit = {
    request(SendMessage(msg.source, inviteStr(msg.source)))
  }

  def enterInviteCode()(implicit system: ActorSystem, msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      after(1.second, system.scheduler) {
        val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = true))

        Future.successful(setChatState(newState))
      }

      request(SendMessage(msg.source, enterInviteCodeStr))
    }
  }

  def exitEnteringInviteCode()(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState.copy(userState = currentState.userState.copy(isEnteringInviteCode = false))

      setChatState(newState)
    }
  }

  def setInviter(inviter: Long)(implicit msg: Message): Unit = {
    withCurrentState { (currentState, currentLevel) =>
      val newState = currentState.copy(userState = currentState.userState.copy(invitedBy = Some(inviter)))

      setChatState(newState)
    }
  }

  def isEnteringInviteCode()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, currentLevel) =>
      currentState.userState.isEnteringInviteCode
    }
  }

  def isInviteCodeExists(inviteCode: Long): Boolean = {
    getStateOfUser(inviteCode).isDefined
  }

  def canSetInviter()(implicit msg: Message): Boolean = {
    withCurrentState { (currentState, currentLevel) =>
      currentState.userState.invitedBy.isEmpty
    }
  }

}

package ai.bale.bot.bankiaccess

import java.time.Instant

import akka.Done
import im.actor.server.botbankiaccess.BotBankiAccessEvents.{ AccessGranted, AccessRevoked }

private trait BotBankiAccessCommandHandler extends BotBankiAccessHelper {
  this: BotBankiAccessProcessor ⇒

  def grantAccess(botUserId: Int, serviceKey: String): Unit = {
    val replyTo = sender()

    persist(AccessGranted(Instant.now, botUserId, serviceKey)) { evt ⇒
      commit(evt)

      replyTo ! Done
    }
  }

  def revokeAccess(botUserId: Int, serviceKey: String): Unit = {
    val replyTo = sender()

    persist(AccessRevoked(Instant.now, botUserId, serviceKey)) { evt ⇒
      commit(evt)

      replyTo ! Done
    }
  }
}
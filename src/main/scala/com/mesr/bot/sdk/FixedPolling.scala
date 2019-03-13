package com.mesr.bot.sdk

import akka.actor.ActorSystem
import akka.pattern.after
import com.bot4s.telegram.api.Polling
import com.bot4s.telegram.models.Update

import scala.concurrent.Future
import scala.concurrent.duration._

trait FixedPolling extends Polling {
  val system: ActorSystem

  override def pollingGetUpdates(offset: Option[Long]): Future[Seq[Update]] = {
    after(1.second, system.scheduler) {
      super.pollingGetUpdates(offset)
    }
  }
}

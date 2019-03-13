package examples.src

import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.models.Message
import slogging.StrictLogging

import scala.collection.mutable

trait MessageHandler extends TelegramBot with Commands with StrictLogging {
  case class TextFilter(filterFunction: String => Boolean, applyFunction: Message => Unit)

  private val filters = mutable.ArrayBuffer[TextFilter]()
  private var defaultFilter: Option[Message => Unit] = None
  private var filterAll: Option[Message => Unit] = None

  def onTextFilter(filter: String)(f: Message => Unit): Unit = {
    val filterFunction: String => Boolean = { msg =>
      filter == msg
    }

    filters += TextFilter(filterFunction, f)
  }

  def onTextFilter(filter: String => Boolean)(f: Message => Unit): Unit = {
    filters += TextFilter(filter, f)
  }

  def onTextDefaultFilter(f: Message => Unit): Unit = {
    defaultFilter = Some(f)
  }

  def onReceipt(f: Message => Unit): Unit = {
    onMessage { msg =>
      msg.successfulPayment match {
        case Some(_) =>
          f(msg)
        case _ =>
      }
    }
  }

  onMessage { msg =>
    if (msg.text.getOrElse("").startsWith("/")) {
      // ignore commands
    } else {
      val filteringResult = filters.map { filter =>
        msg.text match {
          case Some(text) if filter.filterFunction(text) =>
            filter.applyFunction.apply(msg)

            true

          case _ =>

            false
        }
      }

      if (filteringResult.forall(_ == false) && defaultFilter.isDefined) {
        defaultFilter.get.apply(msg)
      }
    }
  }
}

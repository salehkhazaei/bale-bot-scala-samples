package com.mesr.bot

object Errors {
  case object GameFinished extends RuntimeException("Game has finished")
}

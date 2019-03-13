package examples.src

object Errors {
  case object GameFinished extends RuntimeException("Game has finished")
}

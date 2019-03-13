package examples.src

case class AftabeState(userState: UserState, currentGameState: Option[GameState])

case class UserState(userId: Long, coinCount: Int, currentLevel: Int)
case class GameState(level: Int, guessCount: Int, revealedChars: Seq[Boolean])

case class Level(fileId: String, response: String)

case class Database(levels: IndexedSeq[Level])
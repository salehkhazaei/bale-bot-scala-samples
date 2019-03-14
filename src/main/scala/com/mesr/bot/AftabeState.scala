package com.mesr.bot

case class AftabeState(userState: UserState, gameState: GameState, requestLevelState: Option[RequestLevel])

case class UserState(userId: Long, coinCount: Int, isEnteringInviteCode: Boolean, invitedBy: Option[Long])
case class GameState(level: Int, guessCount: Int, revealedChars: Seq[Boolean])
case class RequestLevel(fileId: String)

case class Level(fileId: String, response: String)
case class Database(levels: IndexedSeq[Level])
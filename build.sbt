name := "bale-bot-samples-scala"

version := "0.1"

scalaVersion := "2.12.8"

// Core with minimal dependencies, enough to spawn your first bot.
libraryDependencies += "com.bot4s" %% "telegram-core" % "4.0.0-RC2"

// Extra goodies: Webhooks, support for games, bindings for actors.
libraryDependencies += "com.bot4s" %% "telegram-akka" % "4.0.0-RC2"

libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.4.0"
package examples.src

import java.nio.file.{Files, Paths}

import com.typesafe.config.{Config, ConfigFactory}

object BotConfig {
  private val configPath = Paths.get("conf/server.conf")
  private val absolutePath =
    if (configPath.isAbsolute) configPath
    else Paths.get(".").toAbsolutePath.normalize().resolve(configPath).toAbsolutePath.normalize

  if (Files.exists(absolutePath)) {
    System.setProperty("config.file", absolutePath.toString)
  }

  def load(defaults: Config = ConfigFactory.empty()): Config = {
    val mainConfig = ConfigFactory.load()
    defaults.withFallback(mainConfig)
      .resolve()
  }
}


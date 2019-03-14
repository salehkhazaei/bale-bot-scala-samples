package ai.bale

object Versioning {

  implicit class RichVersion(versionName: String) {
    def toVersion: String = {
      val buildNumber = getBuildNumber
      if(buildNumber == "") versionName
      else s"$versionName.$buildNumber$getVersionPostfix"
    }
    private def getBuildNumber: String =
      sys.env.get("VERSION_PATCH")
        .orElse(sys.env.get("BUILD_NUMBER"))
        .getOrElse("")

    private def getVersionPostfix: String = sys.env.get("VERSION_POSTFIX").map("-" + _).getOrElse("")
  }

}
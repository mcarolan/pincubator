import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName         = "pincubator"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "org.scala-lang" % "scala-compiler" % "2.10.2"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      scalaVersion := "2.10.2"
    )

}

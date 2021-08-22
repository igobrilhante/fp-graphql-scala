import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import sbt._
import Keys._

object DockerSettings {

  val settings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk11:x86_64-debianslim-jdk-11.0.10_9-slim",
    dockerRepository := Some("igobrilhante"),
    dockerExposedPorts ++= Seq(8080),
    dockerCommands := dockerCommands.value.flatMap {
      case cmd @ Cmd("FROM", _) =>
        List(
          cmd,
          Cmd("RUN", "apt update && apt -y install bash wget")
        )
      case other => List(other)
    }
  )

  val skipPublishSetting = Seq(
    publish / skip := true,
    Docker / publish / skip := true
  )

}

import sbt.Keys._
import sbt._
import sbtrelease.Version

lazy val copyYaml = taskKey[Unit]("Prints 'Hello World'")

name := "hello"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.11.8"
releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse("Error") }
assemblyJarName in assembly := "hello.jar"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.8.4",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.4",
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
  "com.amazonaws" % "aws-lambda-java-events" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-events" % "1.11.54"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")


/** serverless.ymlをsrc/main/resources にコピーするタスク
  *
  */
copyYaml := {
  val path = "src/main/resources"
  val filename = "serverless.yml"

  IO.createDirectory(new File(path))
  IO.copyFile(new File(s"./$filename"), new File(s"$path/$filename"))
}

//  assembly の実行前に copyYamlを実施し、確実に変更したserverless.ymlがjarに内包されるようにする
assembly := { copyYaml.value ; assembly.value }
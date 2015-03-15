name := "Lacquer"

organization := "momijikawa"

version := "0.3.1"

scalaVersion := "2.10.4"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/"

resolvers += "spray repo" at "http://repo.spray.io"

org.scalastyle.sbt.ScalastylePlugin.Settings

ScctPlugin.instrumentSettings

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.13" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-agent" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalaz" %% "scalaz-core" % "7.0.0",
  "org.scalaz" %% "scalaz-effect" % "7.0.0",
  "org.scalaz" %% "scalaz-typelevel" % "7.0.0",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.0.0" % "test",
  "org.pegdown" % "pegdown" % "1.0.2",
  "junit" % "junit" % "latest.integration" % "test",
  "org.mockito" % "mockito-all" % "1.9.5",
  "io.spray" %% "spray-can" % "1.3.1",
  "io.spray" %% "spray-http" % "1.3.1",
  "io.spray" %% "spray-io" % "1.3.1",
  "io.spray" %% "spray-client" % "1.3.1",
  "io.spray" %% "spray-caching" % "1.3.1",
  "io.spray" %% "spray-routing" % "1.3.1",
  "io.spray" %%  "spray-json" % "1.3.0",
  "org.jvnet.mimepull" % "mimepull" % "1.9.4",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "com.wandoulabs.akka" %% "spray-websocket" % "0.1.3"
)

initialCommands := "import momijikawa.lacquer._"

initialCommands in console := "import scalaz._, Scalaz._"

// Specify publish directory with your environment.

publishTo := Some(Resolver.file("lacquer",file(Path.userHome.absolutePath+"/.m2/repository"))(Patterns(true, Resolver.mavenStyleBasePattern)))

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")

parallelExecution in Test := false

fork in run := true // 一度runしても中断してプロンプトに戻れるようにする

testOptions in Test += Tests.Argument("junitxml", "html", "console")

assemblyJarName in assembly := s"lacquer-${version.value}.jar"

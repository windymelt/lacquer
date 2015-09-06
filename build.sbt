// build.sbt
// ScalaのDSLで記述するので、各定義は一行空けること

// プロジェクト名
name := "Lacquer"

// 組織名; パッケージのトップにつく名称
organization := "momijikawa"

// プロジェクトのバージョン
version := "0.3.1"

// 使用するScalaのバージョン
scalaVersion := "2.11.7"

// パッケージの依存関係を解決するのに用いるMavenレポジトリ
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Momijikawa Maven repository on GitHub" at "http://windymelt.github.io/repo/"

resolvers += "spray repo" at "http://repo.spray.io"

// コードフォーマッタプラグインscalariformの設定をロード
// cf. scalariform.sbt
scalariformSettings

// コーディング規約チェッカプラグインscalastyleの設定をロード
org.scalastyle.sbt.ScalastylePlugin.Settings

// カバレッジ測定プラグインscctの設定をロード
ScctPlugin.instrumentSettings

// パッケージの依存性を定義
// %が2つの定義はレポジトリのディレクトリ構造にscalaのバージョンを含むもの
// %を2つ書くことで自動的にscalaのバージョンを補完している
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.6.4" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-agent" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6",
  "commons-codec" % "commons-codec" % "1.9",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.scalaz" %% "scalaz-effect" % "7.1.3",
  "org.scalaz" %% "scalaz-typelevel" % "7.1.3",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.1.3" % "test",
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

lazy val root = project.in(file(".")).dependsOn(barketyRepo)

lazy val barketyRepo = uri("git://github.com/Inkp/Barkety.git#2d7a6244909fa86b8f73339eab03b2c6028da816")

// コンソールを呼んだ時に最初に自動的に実行される文
initialCommands := "import momijikawa.lacquer._"

initialCommands in console := "import scalaz._, Scalaz._"

// publishした時の出力先の指定
publishTo := Some(Resolver.file("lacquer",file(Path.userHome.absolutePath+"/.m2/repository"))(Patterns(true, Resolver.mavenStyleBasePattern)))

// テスト時にJUnitのXMLを出力させる設定
testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")

// Specs2のためのオプション
scalacOptions in Test ++= Seq("-Yrangepos")

// テストを並行に実施しない
parallelExecution in Test := false

// 一度runしても中断してプロンプトに戻れるようにする
fork in run := true

testOptions in Test += Tests.Argument("junitxml", "html", "console")

// 実行可能なJARファイルを出力するsbt-assemblyプラグインの設定
assemblyJarName in assembly := s"lacquer-${version.value}.jar"

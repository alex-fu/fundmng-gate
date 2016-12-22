
scalaVersion in Global := "2.11.8"

val root = project.in(file("."))
  .settings(commonSettings)
  .settings(scalacSettings)
  .settings(resolverSettings)
  .settings(dependencySettings)
  .settings(testDependencySettings)
//  .settings(wartRemoverSettings)
  .settings(coverageSettings)
//  .settings(releaseSettings)

lazy val commonSettings = Seq(
  name := "fundmng-gate",
  organization := "com.heqiying",
  scalaVersion := "2.11.8"
)

val akkaV = "2.4.14"
val akkaHttpV = "10.0.0"
val slickV = "3.1.1"

lazy val dependencySettings = {
  Seq(
    dependencyOverrides ++= Set(
      "org.slf4j" % "slf4j-api" % "1.7.21"
    ),
    libraryDependencies ++= (
      Seq(
        "com.typesafe.akka" %% "akka-actor",
        "com.typesafe.akka" %% "akka-slf4j",
        "com.typesafe.akka" %% "akka-stream"
      ).map(_ % akkaV)
        ++
      Seq(
        "com.typesafe.akka" %% "akka-http" % akkaHttpV
      )
        ++
      Seq(
        "io.prometheus" % "simpleclient",
        "io.prometheus" % "simpleclient_common",
        "io.prometheus" % "simpleclient_hotspot"
      ).map(_ % "0.0.15")
        ++
      Seq(
        "com.typesafe.slick"      %% "slick"                     % slickV,
        "com.typesafe.slick"      %% "slick-hikaricp"            % slickV,
        "org.mariadb.jdbc"        %  "mariadb-java-client"       % "1.3.6",
        "org.postgresql"          %  "postgresql"                % "9.4.1208"
      )
        ++
      Seq(
        "com.heqiying"                  %% "konfig"                 % "0.0.1",
        "ch.qos.logback"                %  "logback-classic"        % "1.1.7",
        "com.typesafe.scala-logging"    %% "scala-logging"          % "3.1.0",
        "org.slf4j"                     %  "jcl-over-slf4j"         % "1.7.12",
        "org.slf4j"                     %  "log4j-over-slf4j"       % "1.7.12",
        "com.github.swagger-akka-http"  %  "swagger-akka-http_2.11" % "0.7.0"
      )
    ).map(_.excludeAll(
      ExclusionRule("commons-logging", "commons-logging"),
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("org.slf4j", "slf4j-log4j12")
    ))
  )
}

lazy val testDependencySettings = {
  libraryDependencies ++= (
    Seq(
      "com.typesafe.akka" %% "akka-testkit",
      "com.typesafe.akka" %% "akka-stream-testkit"
    ).map(_ % akkaV)
      ++
    Seq(
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
    )
      ++
    Seq(
      "org.scalatest"       %% "scalatest"      % "3.0.1" % "test"
    )
  )
}

lazy val resolverSettings = Seq(
  resolvers ++= Seq(
    Resolver.mavenLocal,
    "Taobao Maven" at "http://maven.aliyun.com/nexus/content/groups/public/"
  )
)

lazy val scalacSettings = Seq(
  scalacOptions ++= commonScalacOptions
)

lazy val wartRemoverSettings = Seq(
  wartremoverWarnings ++= Warts.allBut(Wart.Var, Wart.Equals)
)

lazy val commonScalacOptions = Seq(
    "-deprecation"             // Emit warning and location for usages of deprecated APIs
  , "-encoding", "UTF-8"
  , "-feature"                 // Emit warning and location for usages of features that should be imported explicitly
//  , "-unchecked"               // Enable additional warnings where generated code depends on assumptions
//  , "-Xfatal-warnings"         // Fail the compilation if there are any warnings
//  , "-Xfuture"                 // Turn on future language features
//  , "-Xlint"                   // Enable specific warnings (see `scalac -Xlint:help`)
//  , "-Yno-adapted-args"        // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
//  , "-Ywarn-dead-code"         // Warn when dead code is identified
//  , "-Ywarn-inaccessible"      // Warn about inaccessible types in method signatures
//  , "-Ywarn-infer-any"         // Warn when a type argument is inferred to be `Any`
//  , "-Ywarn-nullary-override"  // Warn when non-nullary `def f()' overrides nullary `def f'
//  , "-Ywarn-nullary-unit"      // Warn when nullary methods return Unit
//  , "-Ywarn-numeric-widen"     // Warn when numerics are widened
//  , "-Ywarn-unused"            // Warn when local and private vals, vars, defs, and types are unused
//  , "-Ywarn-unused-import"     // Warn when imports are unused
//  , "-Ywarn-value-discard"     // Warn when non-Unit expression results are unused
)

lazy val coverageSettings = Seq(
  coverageEnabled := true,
  coverageFailOnMinimum := true,
  coverageMinimum := 80,
  coverageOutputCobertura := false,
  coverageOutputXML := false
)


name := "messagingserver"

version := "1.0"

scalaVersion := "2.11.7"

mainClass in Global := Some("messagingserver.Main")

assemblyJarName in assembly := "messagingserver.jar"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.3"
  Seq(
    "org.mindrot"        % "jbcrypt"         % "0.3m",
    "io.spray"          %% "spray-can"       % sprayVersion,
    "io.spray"          %% "spray-routing"   % sprayVersion,
    "io.spray"          %% "spray-json"      % "1.3.2",
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit"    % akkaVersion  % "test",
    "io.spray"          %% "spray-testkit"   % sprayVersion % "test",
    "org.specs2"        %% "specs2"          % "2.3.13"     % "test"
  )
}

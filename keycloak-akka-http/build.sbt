lazy val akkaHttpVersion = "10.1.4"
lazy val akkaVersion    = "2.5.16"
lazy val keycloakVersion = "4.0.0.Final"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "io.scalac",
      scalaVersion    := "2.12.6"
    )),
    name := "keycloak-akka-http",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "org.keycloak"      % "keycloak-core"         % keycloakVersion,
      "org.keycloak"      % "keycloak-adapter-core" % keycloakVersion,
      "org.jboss.logging" % "jboss-logging"         % "3.3.0.Final",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.1",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )

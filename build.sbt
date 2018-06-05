import _root_.wartremover.WartRemover.autoImport._
import com.typesafe.sbt.SbtScalariform._
import org.scalastyle.sbt.ScalastylePlugin._
import com.gilcloud.sbt.gitlab.{GitlabCredentials,GitlabPlugin}

organization := "com.agilogy"

name := "srdb-tx"

version := "1.1"

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.7","2.12.6")

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test"
)

coverageExcludedPackages := "<empty>"

// --> Linters

// See tinyurl.com/sd15lint

// https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

// Execute static analysis via `lint:compile`
val LintTarget = config("lint").extend(Compile)

inConfig(LintTarget) {

  Defaults.compileSettings ++
    Seq(
      sources in LintTarget := {
        val lintSources = (sources in LintTarget).value
        lintSources ++ (sources in Compile).value
      },
      scalacOptions in LintTarget ++= Seq(
        "-Xfatal-warnings",
        "-Ywarn-unused-import",
        "-Ywarn-dead-code",
        "-P:linter:disable:PreferIfToBooleanMatch"
      ),
      wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.MutableDataStructures)
    )
}

scalacOptions in Compile := (scalacOptions in Compile).value filterNot { switch =>
  switch.startsWith("-P:wartremover:") ||
    "^-Xplugin:.*/org[.]brianmckenna/.*wartremover.*[.]jar$".r.pattern.matcher(switch).find ||
    switch.startsWith("-P:linter:") ||
    "^-Xplugin:.*/com[.]foursquare[.]lint/.*linter.*[.]jar$".r.pattern.matcher(switch).find
}

resolvers += "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases"

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")

scalastyleFailOnError := true

// <-- Linters

// --> gitlab

GitlabPlugin.autoImport.gitlabGroupId := None
GitlabPlugin.autoImport.gitlabProjectId := Some(26236490)
GitlabPlugin.autoImport.gitlabDomain := "gitlab.com"

GitlabPlugin.autoImport.gitlabCredentials := {
    val token = sys.env.get("GITLAB_DEPLOY_TOKEN") match {
        case Some(token) => token
        case None =>
            sLog.value.warn(s"Environment variable GITLAB_DEPLOY_TOKEN is undefined, 'publish' will fail.")
            ""
    }
    Some(GitlabCredentials("Deploy-Token", token))
}

// <-- gitlab

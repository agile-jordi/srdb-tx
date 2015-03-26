import _root_.wartremover.WartRemover.autoImport._
import bintray.Keys._
import com.typesafe.sbt.SbtScalariform._
import org.scalastyle.sbt.ScalastylePlugin._

organization := "com.agilogy"

name := "srdb-tx"

version := "1.0.0"

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.4","2.11.6")

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)

ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "<empty>"

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
//  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ywarn-unused-import",     // 2.11 only
  "-P:linter:disable:PreferIfToBooleanMatch"
)

// https://gist.github.com/leifwickland/3e4bf79562ce0a963bc8
wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.DefaultArguments, Wart.MutableDataStructures)

resolvers += "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases"

addCompilerPlugin("com.foursquare.lint" %% "linter" % "0.1.9")

scalastyleFailOnError := true

// <-- Linters

// Reformat at every compile.
// See https://github.com/sbt/sbt-scalariform
scalariformSettings

publishMavenStyle := false

// --> bintray

seq(bintrayPublishSettings:_*)

repository in bintray := "scala"

bintrayOrganization in bintray := Some("agilogy")

packageLabels in bintray := Seq("scala")

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

// <-- bintray

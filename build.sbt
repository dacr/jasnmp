name := "jasnmp"

version := "0.1.1-SNAPSHOT"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.5", "2.11.7")

libraryDependencies ++= Seq(
   "com.typesafe.akka" %% "akka-actor" % "2.3.9",
   "org.snmp4j" % "snmp4j" % "2.3.3" % "compile",
   "org.jsmiparser" % "jsmiparser-api" % "0.13"
)

resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.+" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

resolvers += "snmp4j repository" at "https://oosnmp.net/dist/release/"


initialCommands in console := """
  |import fr.janalyse.snmp._
  |""".stripMargin


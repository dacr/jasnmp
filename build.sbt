name := "jasnmp"

version := "0.1.0"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

libraryDependencies ++= Seq(
   "com.typesafe.akka" %% "akka-actor" % "2.3.9",
   "org.snmp4j" % "snmp4j" % "2.3.3" % "compile",
   "org.jsmiparser" % "jsmiparser-api" % "0.13"
)

resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.+" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

publishTo := Some(
     Resolver.sftp(
         "JAnalyse Repository",
         "www.janalyse.fr",
         "/home/tomcat/webapps-janalyse/repository"
     ) as("tomcat", new File(util.Properties.userHome+"/.ssh/id_rsa"))
)

resolvers += "snmp4j repository" at "https://oosnmp.net/dist/release/"


initialCommands in console := """
  import fr.janalyse.snmp._
"""

name := "jasnmp"

version := "0.0.1"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

libraryDependencies += "org.snmp4j" % "snmp4j" % "2.3.0" % "compile"

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


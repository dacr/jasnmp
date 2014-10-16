name := "jasnmp"

version := "0.0.1"

organization :="fr.janalyse"

organizationHomepage := Some(new URL("http://www.janalyse.fr"))

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.1", "2.9.2")

libraryDependencies += "org.snmp4j" % "snmp4j" % "2.2.2" % "compile"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

publishTo := Some(
     Resolver.sftp(
         "JAnalyse Repository",
         "www.janalyse.fr",
         "/home/tomcat/webapps-janalyse/repository"
     ) as("tomcat", new File(util.Properties.userHome+"/.ssh/id_rsa"))
)

resolvers += "snmp4j repository" at "https://oosnmp.net/dist/release/"


// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Maven Repository" at "http://repo1.maven.org/maven2/"

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

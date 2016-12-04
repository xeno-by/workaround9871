lazy val ScalaVersions = Seq("2.12.0")
lazy val ScalaVersion = ScalaVersions.max
lazy val PluginVersion = "1.0.0"

lazy val root = Project(
  id = "root",
  base = file(".")
) settings (
  sharedSettings,
  packagedArtifacts := Map.empty
) aggregate (
  plugin,
  tests
)

lazy val plugin = Project(
  id   = "workaround9871",
  base = file("plugin")
) settings (
  sharedSettings : _*
) settings (
  description := "Works around SI-9871",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  publishArtifact in Compile := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { x => false },
  pomExtra := (
    <url>https://github.com/xeno-by/workaround9871</url>
    <inceptionYear>2016</inceptionYear>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git://github.com/xeno-by/workaround9871.git</url>
      <connection>scm:git:git://github.com/xeno-by/workaround9871.git</connection>
    </scm>
    <issueManagement>
      <system>GitHub</system>
      <url>https://github.com/xeno-by/workaround9871/issues</url>
    </issueManagement>
    <developers>
      <developer>
        <id>xeno-by</id>
        <name>Eugene Burmako</name>
        <url>http://xeno.by</url>
      </developer>
    </developers>
  ),
  credentials ++= loadCredentials()
)

lazy val tests = Project(
  id   = "tests",
  base = file("tests")
) settings (
  sharedSettings ++ usePluginSettings: _*
) settings (
  exposePaths("tests", Test),
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test"
)

lazy val sharedSettings = Seq(
  scalaVersion := ScalaVersion,
  crossScalaVersions := ScalaVersions,
  crossVersion := CrossVersion.full,
  version := PluginVersion,
  organization := "com.github.xenoby",
  resolvers += Resolver.sonatypeRepo("releases"),
  publishMavenStyle := true,
  publishArtifact in Compile := false,
  publishArtifact in Test := false,
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-unused-import", "-Xfatal-warnings"),
  parallelExecution in Test := true,
  logBuffered := false
)

def loadCredentials(): List[Credentials] = {
  val mavenSettingsFile = System.getProperty("maven.settings.file")
  if (mavenSettingsFile != null) {
    println("Loading Sonatype credentials from " + mavenSettingsFile)
    try {
      import scala.xml._
      val settings = XML.loadFile(mavenSettingsFile)
      def readServerConfig(key: String) = (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
      List(Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        readServerConfig("username"),
        readServerConfig("password")
      ))
    } catch {
      case ex: Exception =>
        println("Failed to load Maven settings from " + mavenSettingsFile + ": " + ex)
        Nil
    }
  } else {
    // println("Sonatype credentials cannot be loaded: -Dmaven.settings.file is not specified.")
    Nil
  }
}

lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar = (Keys.`package` in (plugin, Compile)).value
    System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

def exposePaths(projectName: String, config: Configuration) = {
  def uncapitalize(s: String) = if (s.length == 0) "" else { val chars = s.toCharArray; chars(0) = chars(0).toLower; new String(chars) }
  val prefix = "sbt.paths." + projectName + "." + uncapitalize(config.name) + "."
  Seq(
    sourceDirectory in config := {
      val defaultValue = (sourceDirectory in config).value
      System.setProperty(prefix + "sources", defaultValue.getAbsolutePath)
      defaultValue
    },
    resourceDirectory in config := {
      val defaultValue = (resourceDirectory in config).value
      System.setProperty(prefix + "resources", defaultValue.getAbsolutePath)
      defaultValue
    },
    fullClasspath in config := {
      val defaultValue = (fullClasspath in config).value
      val classpath = defaultValue.files.map(_.getAbsolutePath)
      val scalaLibrary = classpath.map(_.toString).find(_.contains("scala-library")).get
      System.setProperty("sbt.paths.scalalibrary.classes", scalaLibrary)
      System.setProperty(prefix + "classes", classpath.mkString(java.io.File.pathSeparator))
      defaultValue
    }
  )
}

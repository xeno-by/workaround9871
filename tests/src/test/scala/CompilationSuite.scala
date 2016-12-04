package by.xeno.workaround9871

import org.scalatest._
import scala.compat.Platform.EOL
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.{Global, CompilerCommand, Settings}
import scala.tools.nsc.reporters.StoreReporter

class CompilationSuite extends FunSuite {
  test("problematic code doesn't compile without the plugin") {
    val problematic = scala.io.Source.fromFile(sys.props("sbt.paths.tests.test.sources") + "/../main/scala/Problematic.scala").mkString
    val compilationResult = compile(problematic)
    assert(compilationResult === """
      |ERROR ambiguous reference to overloaded definition,
      |both method foo in object Test of type (x: MyFunction1[Int,Int])Nothing
      |and  method foo in object Test of type (x: Int => Int)Nothing
      |match argument types (MyFunction1[Int,Int])
    """.trim.stripMargin)
  }

  test("problematic code compiles with the plugin") {
    // see tests/src/main/scala
  }

  test("normal code compiles with the plugin") {
    val compilationResult = compile("class C")
    assert(compilationResult === "")
  }

  private def compile(code: String): String = {
    def fail(msg: String) = sys.error(s"Compiler initialization failed: $msg")
    val options = "-cp " + System.getProperty("sbt.paths.scalalibrary.classes")
    val args = CommandLineParser.tokenize(options)
    val emptySettings = new Settings(error => fail(s"couldn't apply settings because $error"))
    val command = new CompilerCommand(args, emptySettings)
    val settings = command.settings
    val reporter = new StoreReporter()
    val global = new Global(settings, reporter)
    val run = new global.Run()
    run.compileSources(List(new BatchSourceFile("<snippet>", code)))
    reporter.infos.map(i => s"${i.severity} ${i.msg}").mkString(EOL)
  }
}
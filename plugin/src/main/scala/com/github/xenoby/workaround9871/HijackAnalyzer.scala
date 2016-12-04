package com.github.xenoby.workaround9871

import scala.collection.mutable
import scala.tools.nsc.{Global => NscGlobal, SubComponent}
import com.github.xenoby.workaround9871.{Plugin => Workaround9871Plugin}
import scala.tools.nsc.typechecker.{Analyzer => NscAnalyzer}
import com.github.xenoby.workaround9871.{Analyzer => Workaround9871Analyzer}
import scala.tools.nsc.interpreter.{ReplGlobal => NscReplGlobal}
import scala.tools.nsc.interactive.{Global => NscInteractiveGlobal, InteractiveAnalyzer => NscInteractiveAnalyzer}

trait HijackAnalyzer {
  self: Workaround9871Plugin =>

  def hijackAnalyzer(): (global.analyzer.type, NscAnalyzer) = {
    // NOTE: need to hijack the right `analyzer` field - it's different for batch compilers and repl compilers
    val isRepl = global.isInstanceOf[NscReplGlobal]
    val isInteractive = global.isInstanceOf[NscInteractiveGlobal]
    val oldAnalyzer = global.analyzer
    val newAnalyzer = {
      if (isInteractive) {
        new {
          val global: self.global.type with NscInteractiveGlobal = self.global.asInstanceOf[self.global.type with NscInteractiveGlobal]
        } with Workaround9871Analyzer with NscInteractiveAnalyzer {
          override def newTyper(context: Context) = new Workaround9871Typer(context) with InteractiveTyper
        }
      } else {
        new { val global: self.global.type = self.global } with Workaround9871Analyzer {
          override protected def findMacroClassLoader(): ClassLoader = {
            val loader = super.findMacroClassLoader
            if (isRepl) {
              macroLogVerbose("macro classloader: initializing from a REPL classloader: %s".format(global.classPath.asURLs))
              val virtualDirectory = global.settings.outputDirs.getSingleOutput.get
              new scala.reflect.internal.util.AbstractFileClassLoader(virtualDirectory, loader) {}
            } else {
              loader
            }
          }
        }
      }
    }
    val globalClass: Class[_] = if (isRepl) global.getClass else if (isInteractive) classOf[NscInteractiveGlobal] else classOf[NscGlobal]
    val analyzerField = globalClass.getDeclaredField("analyzer")
    analyzerField.setAccessible(true)
    analyzerField.set(global, newAnalyzer)

    val phasesSetMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesSet")
    val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[mutable.Set[SubComponent]]
    if (phasesSet.exists(_.phaseName == "typer")) { // `scalac -help` doesn't instantiate standard phases
      def subcomponentNamed(name: String) = phasesSet.find(_.phaseName == name).head
      val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) = List(subcomponentNamed("namer"), subcomponentNamed("packageobjects"), subcomponentNamed("typer"))
      val newScs = List(newAnalyzer.namerFactory, newAnalyzer.packageObjects, newAnalyzer.typerFactory)
      def hijackDescription(pt: SubComponent, sc: SubComponent) = {
        val phasesDescMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesDescMap")
        val phasesDescMap = phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
        phasesDescMap(sc) = phasesDescMap(pt)
      }
      oldScs zip newScs foreach { case (pt, sc) => hijackDescription(pt, sc) }
      phasesSet --= oldScs
      phasesSet ++= newScs
    }

    (newAnalyzer.asInstanceOf[global.analyzer.type], oldAnalyzer)
  }
}
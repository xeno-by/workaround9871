package com.github.xenoby.workaround9871

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin => NscPlugin}

class Plugin(val global: Global) extends NscPlugin with HijackAnalyzer {
  val name = "workaround9871"
  val description = "Works around SI-9871"
  val components = Nil

  val (newAnalyzer, oldAnalyzer) = hijackAnalyzer()
  if (global.analyzer ne newAnalyzer) sys.error("failed to hijack analyzer")
}

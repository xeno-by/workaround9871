package by.xeno.workaround9871

import scala.tools.nsc.typechecker.{Analyzer => NscAnalyzer}

trait Analyzer extends NscAnalyzer {
  override def newTyper(context: Context) = new Workaround9871Typer(context)

  class Workaround9871Typer(context0: Context) extends Typer(context0) {
  }
}
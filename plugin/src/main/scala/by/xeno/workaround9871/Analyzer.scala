package by.xeno.workaround9871

import scala.tools.nsc.typechecker.{Analyzer => NscAnalyzer}

trait Analyzer extends NscAnalyzer {
  import global._
  import definitions._

  override def newTyper(context: Context) = new Workaround9871Typer(context)

  class Workaround9871Typer(context0: Context) extends Typer(context0) {
    override def samToFunctionType(tp: Type, sam: Symbol = NoSymbol): Type = {
      val notASamAnnotation = rootMirror.getClassIfDefined("scala.annotation.notASam")
      if (tp.typeSymbol.hasAnnotation(notASamAnnotation)) NoType
      else super.samToFunctionType(tp, sam)
    }

    override val infer = new Inferencer {
      def context = Workaround9871Typer.this.context
      override def isCoercible(tp: Type, pt: Type) = undoLog undo viewExists(tp, pt)

      private def isCompatible(tp: Type, pt: Type): Boolean = {
        def isCompatibleByName(tp: Type, pt: Type): Boolean = (
             isByNameParamType(pt)
          && !isByNameParamType(tp)
          && isCompatible(tp, dropByName(pt))
        )
        def isCompatibleSam(tp: Type, pt: Type): Boolean = (definitions.isFunctionType(tp) || tp.isInstanceOf[MethodType] || tp.isInstanceOf[PolyType]) &&  {
          // val samFun = typer.samToFunctionType(pt)
          val samFun = samToFunctionType(pt)
          (samFun ne NoType) && isCompatible(tp, samFun)
        }

        val tp1 = normalize(tp)

        (    (tp1 weak_<:< pt)
          || isCoercible(tp1, pt)
          || isCompatibleByName(tp, pt)
          || isCompatibleSam(tp, pt)
        )
      }

      override def isCompatibleArgs(tps: List[Type], pts: List[Type]) = (tps corresponds pts)(isCompatible)
    }
  }
}
// This is a minimized reproduction for SI-9871.
// It should fail to compile in Scala 2.12.0 and 2.12.1,
// but should successfully compile with this compiler plugin enabled.

package scala.annotation {
  class notASam extends StaticAnnotation
}

package by.xeno.workaround9871 {
  @scala.annotation.notASam
  abstract class MyFunction1[T, U] extends Function1[T, U]

  object Test {
    def foo(x: Function1[Int, Int]) = "incorrect"
    def foo(x: MyFunction1[Int, Int]) = "correct"

    def test() = {
      val myf: MyFunction1[Int, Int] = null
      foo(myf)
    }
  }
}
### Background on SI-9871

Scala 2.12.0 introduces support for SAM types, which is one of the key features for Java 8 support in Scala.
Unfortunately, this new feature has the potential to break existing code.

For example, the code snippet below compiles just fine in Scala 2.11.8
(because the overload that takes `MyFunction1[Int, Int]`
is more specific that the overload that takes `Function1[Int, Int]`):

```scala
abstract class MyFunction1[T, U] extends Function1[T, U]

object Test {
  def foo(x: Function1[Int, Int]) = ???
  def foo(x: MyFunction1[Int, Int]) = ???

  val myf: MyFunction1[Int, Int] = ???
  foo(myf)
}
```

However, the same code snippet doesn't compile in Scala 2.12.0:

```scala
Test.scala:8: error: ambiguous reference to overloaded definition,
both method foo in object Test of type (x: MyFunction1[Int,Int])Nothing
and  method foo in object Test of type (x: Int => Int)Nothing
match argument types (MyFunction1[Int,Int])
  foo(myf)
  ^
```

We have submitted this issue to the Scala compiler issue tracker
under [SI-9871](https://issues.scala-lang.org/browse/SI-9871).

In the discussion that ensued,
@adriaanm from the Scala team at Lightbend confirmed that this behavior is intentional.
According to the type compatibility rules introduced in Scala 2.12.0,
`Function1[Int, Int]` is compatible with `MyFunction1[Int, Int]` (because `MyFunction1` is a SAM),
which means that none of the two overloads is more specific than the other.

This is an unfortunate issue that blocks our migration from Scala 2.11 to Scala 2.12.

### Working around SI-9871

One idea suggested by @adriaanm to work around the issue is to introduce `@notASam`,
a new annotation that would let programmers opt out from automatic promotion of eligible types to SAM types.

Inspired by this idea, I hacked up a small compiler plugin that implements `@notASam`.
With the compiler plugin enabled, the following code compiles equally well in both Scala 2.11.8 and Scala 2.12.0:

```scala
package scala.annotation {
  class notASam extends StaticAnnotation
}

@scala.annotation.notASam
abstract class MyFunction1[T, U] extends Function1[T, U]

object Test {
  def foo(x: Function1[Int, Int]) = ???
  def foo(x: MyFunction1[Int, Int]) = ???

  val myf: MyFunction1[Int, Int] = ???
  foo(myf)
}
```

Enjoy!

```scala
resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("com.github.xenoby" % "workaround9871" % "1.0.0" cross CrossVersion.full)
```


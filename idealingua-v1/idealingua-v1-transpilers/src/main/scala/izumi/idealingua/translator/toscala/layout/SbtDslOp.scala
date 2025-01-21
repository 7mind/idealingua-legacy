package izumi.idealingua.translator.toscala.layout

sealed trait SbtDslOp

object SbtDslOp {

  final case class Append[T](v: T, scope: List[Scope]) extends SbtDslOp
  object Append {
    def apply[T](v: T, scope: Scope = Scope.ThisBuild): Append[T] = {
      new Append[T](v, List(scope))
    }
  }

  final case class Assign[T](v: T, scope: List[Scope]) extends SbtDslOp
  object Assign {
    def apply[T](v: T, scope: Scope = Scope.ThisBuild): Assign[T] = {
      new Assign[T](v, List(scope))
    }
  }

}

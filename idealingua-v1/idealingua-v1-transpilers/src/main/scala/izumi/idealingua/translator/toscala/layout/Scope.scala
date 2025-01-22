package izumi.idealingua.translator.toscala.layout

sealed trait Scope

object Scope {

  case object ThisBuild extends Scope

  case object Project extends Scope

  case class Custom(scope: String) extends Scope
}

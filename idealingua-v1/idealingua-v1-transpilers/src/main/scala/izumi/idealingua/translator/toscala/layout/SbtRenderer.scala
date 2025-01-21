package izumi.idealingua.translator.toscala.layout

import izumi.idealingua.model.publishing.BuildManifest

class SbtRenderer {
  def renderOp(pair: Tuple2[String, SbtDslOp]): String = {
    val key = pair._1
    val parts = pair._2 match {
      case SbtDslOp.Append(v, scope) =>
        v match {
          case s: Seq[?] =>
            Seq(renderScope(scope, key), "++=", renderValue(s))
          case o =>
            Seq(renderScope(scope, key), "+=", renderValue(o))
        }
      case SbtDslOp.Assign(v, scope) =>
        Seq(renderScope(scope, key), ":=", renderValue(v))
    }
    parts.mkString(" ")
  }

  def renderScope(scopes: List[Scope], key: String): String = {
    if (scopes == List(Scope.ThisBuild)) {
      key
    } else {
      scopes.map {
        case Scope.ThisBuild     => "ThisBuild"
        case Scope.Project       => "ThisProject"
        case Scope.Custom(scope) => scope
      }.appended(key).mkString(" / ")
    }
  }

  def renderValue(v: Any): String = {
    (v: @unchecked) match {
      case s: String =>
        s""""$s""""
      case b: Boolean =>
        b.toString
      case v: Number =>
        v.toString
      case o: Option[?] =>
        o match {
          case Some(value) =>
            s"Some(${renderValue(value)})"
          case None =>
            "None"
        }
      case u: BuildManifest.MFUrl =>
        s"url(${renderValue(u.url)})"
      case l: BuildManifest.License =>
        s"${renderValue(l.name)} -> ${renderValue(l.url)}"
      case s: Seq[?] =>
        s.map(renderValue).mkString("Seq(\n  ", ",\n  ", ")")
      case r: RawExpr =>
        r.e.trim
    }
  }

}

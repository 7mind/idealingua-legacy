package izumi.idealingua.translator.toprotobuf.products

trait RenderableCogenProduct {
  def preamble: String
  def render: List[String]
  def isEmpty: Boolean = render.isEmpty
}

object RenderableCogenProduct {
  def empty: RenderableCogenProduct = new RenderableCogenProduct {
    override def render: List[String] = List.empty
    override def preamble: String = ""
  }
}

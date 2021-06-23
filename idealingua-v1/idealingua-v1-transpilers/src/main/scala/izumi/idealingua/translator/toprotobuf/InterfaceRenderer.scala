package izumi.idealingua.translator.toprotobuf

import izumi.idealingua.model.il.ast.typed.TypeDef.Interface
import izumi.idealingua.translator.toprotobuf.products.CogenProducts
import izumi.idealingua.translator.toprotobuf.types.ProtobufField

final class InterfaceRenderer(ctx: PBTContext) {
  def defns(i: Interface): CogenProducts.Message = {
    val self = ctx.conv.toProtobuf(i.id)
    val fields = i.struct.fields.map {
      field =>
        ProtobufField(field.name, ctx.conv.toProtobuf(field.typeId))
    }
    ctx.ext.extend(i, CogenProducts.Message(self, fields), _.handleInterface)
  }
}

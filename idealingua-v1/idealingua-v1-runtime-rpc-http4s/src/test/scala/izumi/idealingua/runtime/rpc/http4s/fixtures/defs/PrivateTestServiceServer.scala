package izumi.idealingua.runtime.rpc.http4s.fixtures.defs

import _root_.io.circe.syntax.*
import _root_.io.circe.{DecodingFailure as IRTDecodingFailure, Json as IRTJson}
import _root_.izumi.functional.bio.IO2 as IRTIO2
import _root_.izumi.idealingua.runtime.rpc.*

trait PrivateTestServiceServer[Or[+_, +_], C] {
  type Just[+T] = Or[Nothing, T]
  def test(ctx: C, str: String): Just[String]
}

trait PrivateTestServiceClient[Or[+_, +_]] {
  type Just[+T] = Or[Nothing, T]
  def test(str: String): Just[String]
}

class PrivateTestServiceWrappedClient[Or[+_, +_]: IRTIO2](_dispatcher: IRTDispatcher[Or]) extends PrivateTestServiceClient[Or] {
  final val _F: IRTIO2[Or] = implicitly
  import _root_.izumi.idealingua.runtime.rpc.http4s.fixtures.defs.PrivateTestService as _M
  def test(str: String): Just[String] = {
    _F.redeem(_dispatcher.dispatch(IRTMuxRequest(IRTReqBody(new _M.test.Input(str)), _M.test.id)))(
      {
        err => _F.terminate(err)
      },
      {
        case IRTMuxResponse(IRTResBody(v: _M.test.Output), method) if method == _M.test.id =>
          _F.pure(v.value)
        case v =>
          val id       = "PrivateTestService.PrivateTestServiceWrappedClient.test"
          val expected = classOf[_M.test.Input].toString
          _F.terminate(new IRTTypeMismatchException(s"Unexpected type in $id: $v, expected $expected got ${v.getClass}", v, None))
      },
    )
  }
}

object PrivateTestServiceWrappedClient extends IRTWrappedClient {
  val allCodecs: Map[IRTMethodId, IRTCirceMarshaller] = {
    Map(PrivateTestService.test.id -> PrivateTestServiceCodecs.test)
  }
}

class PrivateTestServiceWrappedServer[Or[+_, +_]: IRTIO2, C](_service: PrivateTestServiceServer[Or, C]) extends IRTWrappedService[Or, C] {
  final val _F: IRTIO2[Or]          = implicitly
  final val serviceId: IRTServiceId = PrivateTestService.serviceId
  val allMethods: Map[IRTMethodId, IRTMethodWrapper[Or, C]] = {
    Seq[IRTMethodWrapper[Or, C]](test).map(m => m.signature.id -> m).toMap
  }
  object test extends IRTMethodWrapper[Or, C] {
    import PrivateTestService.test.*
    val signature: PrivateTestService.test.type        = PrivateTestService.test
    val marshaller: PrivateTestServiceCodecs.test.type = PrivateTestServiceCodecs.test
    def invoke(ctx: C, input: Input): Just[Output] = {
      assert(ctx.asInstanceOf[_root_.scala.AnyRef] != null && input.asInstanceOf[_root_.scala.AnyRef] != null)
      _F.map(_service.test(ctx, input.str))(v => new Output(v))
    }
  }
}

object PrivateTestServiceWrappedServer

object PrivateTestService {
  final val serviceId: IRTServiceId = IRTServiceId("PrivateTestService")
  object test extends IRTMethodSignature {
    final val id: IRTMethodId = IRTMethodId(serviceId, IRTMethodName("test"))
    type Input  = TestInput
    type Output = TestOutput
  }
  final case class TestInput(str: String) extends AnyVal
  object TestInput {
    import _root_.io.circe.derivation.{deriveDecoder, deriveEncoder}
    import _root_.io.circe.{Decoder, Encoder}
    implicit val encodeTestInput: Encoder.AsObject[TestInput] = deriveEncoder[TestInput]
    implicit val decodeTestInput: Decoder[TestInput]          = deriveDecoder[TestInput]
  }
  final case class TestOutput(value: String) extends AnyVal
  object TestOutput {
    import _root_.io.circe.*
    import _root_.io.circe.syntax.*
    implicit val encodeUnwrappedTestOutput: Encoder[TestOutput] = Encoder.instance {
      v => v.value.asJson
    }
    implicit val decodeUnwrappedTestOutput: Decoder[TestOutput] = Decoder.instance {
      v => v.as[String].map(d => TestOutput(d))
    }
  }
}

object PrivateTestServiceCodecs {
  object test extends IRTCirceMarshaller {
    import PrivateTestService.test.*
    def encodeRequest: PartialFunction[IRTReqBody, IRTJson] = {
      case IRTReqBody(value: Input) =>
        value.asJson
    }
    def decodeRequest[Or[+_, +_]: IRTIO2]: PartialFunction[IRTJsonBody, Or[IRTDecodingFailure, IRTReqBody]] = {
      case IRTJsonBody(m, packet) if m == id =>
        this.decoded[Or, IRTReqBody](packet.as[Input].map(v => IRTReqBody(v)))
    }
    def encodeResponse: PartialFunction[IRTResBody, IRTJson] = {
      case IRTResBody(value: Output) =>
        value.asJson
    }
    def decodeResponse[Or[+_, +_]: IRTIO2]: PartialFunction[IRTJsonBody, Or[IRTDecodingFailure, IRTResBody]] = {
      case IRTJsonBody(m, packet) if m == id =>
        decoded[Or, IRTResBody](packet.as[Output].map(v => IRTResBody(v)))
    }
  }
}

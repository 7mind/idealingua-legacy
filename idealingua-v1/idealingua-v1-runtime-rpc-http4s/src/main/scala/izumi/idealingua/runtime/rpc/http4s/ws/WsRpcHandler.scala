package izumi.idealingua.runtime.rpc.http4s.ws

import io.circe.Json
import izumi.functional.bio.Exit.Success
import izumi.functional.bio.{Exit, F, IO2}
import izumi.fundamentals.platform.language.Quirks.Discarder
import izumi.idealingua.runtime.rpc.*
import izumi.idealingua.runtime.rpc.http4s.IRTAuthenticator.AuthContext
import izumi.idealingua.runtime.rpc.http4s.IRTServicesContext.{InvokeMethodFailure, InvokeMethodResult}
import izumi.idealingua.runtime.rpc.http4s.IRTServicesContextMultiplexor
import izumi.idealingua.runtime.rpc.http4s.ws.WsRpcHandler.WsResponder
import logstage.LogIO2

abstract class WsRpcHandler[F[+_, +_]: IO2](
  muxer: IRTServicesContextMultiplexor[F],
  responder: WsResponder[F],
  logger: LogIO2[F],
) {

  protected def handlePacket(packet: RpcPacket): F[Throwable, Unit]

  protected def handleAuthRequest(packet: RpcPacket): F[Throwable, Option[RpcPacket]]

  protected def getAuthContext: AuthContext

  protected def handleAuthResponse(ref: RpcPacketId, packet: RpcPacket): F[Throwable, Option[RpcPacket]] = {
    packet.discard()
    responder.responseWith(ref, RawResponse.EmptyRawResponse()).as(None)
  }

  def processRpcMessage(
    message: String
  ): F[Throwable, Option[RpcPacket]] = {
    for {
      packet <- F.fromEither(io.circe.parser.decode[RpcPacket](message))
      _      <- handlePacket(packet)
      response <- packet match {
        // auth
        case RpcPacket(RPCPacketKind.RpcRequest, None, _, _, _, _, _) =>
          handleAuthRequest(packet)

        case RpcPacket(RPCPacketKind.RpcResponse, None, _, Some(ref), _, _, _) =>
          handleAuthResponse(ref, packet)

        // rpc
        case RpcPacket(RPCPacketKind.RpcRequest, Some(data), Some(id), _, Some(service), Some(method), _) =>
          handleWsRequest(data, IRTMethodId(IRTServiceId(service), IRTMethodName(method)))(
            onSuccess = RpcPacket.rpcResponse(id, _),
            onFail    = RpcPacket.rpcFail(Some(id), _),
          )

        case RpcPacket(RPCPacketKind.RpcResponse, Some(data), _, Some(ref), _, _, _) =>
          responder.responseWithData(ref, data).as(None)

        case RpcPacket(RPCPacketKind.RpcFail, data, _, Some(ref), _, _, _) =>
          responder.responseWith(ref, RawResponse.BadRawResponse(data)).as(None)

        // buzzer
        case RpcPacket(RPCPacketKind.BuzzRequest, Some(data), Some(id), _, Some(service), Some(method), _) =>
          handleWsRequest(data, IRTMethodId(IRTServiceId(service), IRTMethodName(method)))(
            onSuccess = RpcPacket.buzzerResponse(id, _),
            onFail    = RpcPacket.buzzerFail(Some(id), _),
          )

        case RpcPacket(RPCPacketKind.BuzzResponse, Some(data), _, Some(ref), _, _, _) =>
          responder.responseWithData(ref, data).as(None)

        case RpcPacket(RPCPacketKind.BuzzFailure, data, _, Some(ref), _, _, _) =>
          responder.responseWith(ref, RawResponse.BadRawResponse(data)).as(None)

        // critical failures
        case RpcPacket(RPCPacketKind.Fail, data, _, Some(ref), _, _, _) =>
          responder.responseWith(ref, RawResponse.BadRawResponse(data)).as(None)

        case RpcPacket(RPCPacketKind.Fail, data, _, None, _, _, _) =>
          logger.error(s"WS request failed: Unknown RPC failure: $data.").as(None)

        // unknown
        case packet =>
          logger
            .error(s"WS request failed: No buzzer client handler for $packet")
            .as(Some(RpcPacket.rpcCritical("No buzzer client handler", packet.ref)))
      }
    } yield response
  }

  protected def handleWsRequest(
    data: Json,
    methodId: IRTMethodId,
  )(onSuccess: Json => RpcPacket,
    onFail: String => RpcPacket,
  ): F[Throwable, Option[RpcPacket]] = {
    muxer
      .invokeMethodWithAuth(methodId)(getAuthContext, data).sandboxExit.flatMap {
        case Success(InvokeMethodResult(_, res)) =>
          F.pure(Some(onSuccess(res)))

        case Exit.Error(_: InvokeMethodFailure.ServiceNotFound, _) =>
          logger.error(s"WS request errored: No service handler for $methodId.").as(Some(onFail("Service not found.")))

        case Exit.Error(_: InvokeMethodFailure.MethodNotFound, _) =>
          logger.error(s"WS request errored: No method handler for $methodId.").as(Some(onFail("Method not found.")))

        case Exit.Error(err: InvokeMethodFailure.AuthFailed, _) =>
          logger.warn(s"WS request errored: unauthorized - ${err.getMessage -> "message"}.").as(Some(onFail("Unauthorized.")))

        case Exit.Termination(exception, allExceptions, trace) =>
          logger.error(s"WS request terminated: $exception, $allExceptions, $trace").as(Some(onFail(exception.getMessage)))

        case Exit.Error(exception, trace) =>
          logger.error(s"WS request failed: $exception $trace").as(Some(onFail(exception.getMessage)))

        case Exit.Interruption(exception, allExceptions, trace) =>
          logger.error(s"WS request interrupted: $exception $allExceptions $trace").as(Some(onFail(exception.getMessage)))
      }
  }
}

object WsRpcHandler {
  trait WsResponder[F[_, _]] {
    def responseWith(id: RpcPacketId, response: RawResponse): F[Throwable, Unit]
    def responseWithData(id: RpcPacketId, data: Json): F[Throwable, Unit]
  }
}

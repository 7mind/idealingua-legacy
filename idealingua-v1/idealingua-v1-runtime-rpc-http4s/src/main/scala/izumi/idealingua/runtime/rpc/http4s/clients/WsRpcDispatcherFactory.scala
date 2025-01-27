package izumi.idealingua.runtime.rpc.http4s.clients

import io.circe.syntax.*
import io.circe.{Json, Printer}
import izumi.functional.bio.{Async2, Entropy1, Entropy2, Exit, F, IO2, Primitives2, Temporal2, UnsafeRun2}
import izumi.functional.lifecycle.Lifecycle
import izumi.fundamentals.platform.functional.Identity
import izumi.fundamentals.platform.language.Quirks.Discarder
import izumi.idealingua.runtime.rpc.*
import izumi.idealingua.runtime.rpc.http4s.HttpServer
import izumi.idealingua.runtime.rpc.http4s.clients.WsRpcDispatcher.IRTDispatcherWs
import izumi.idealingua.runtime.rpc.http4s.clients.WsRpcDispatcherFactory.{ClientWsRpcHandler, WsRpcClientConnection, fromNettyFuture}
import izumi.idealingua.runtime.rpc.http4s.context.WsContextExtractor
import izumi.idealingua.runtime.rpc.http4s.ws.{RawResponse, WsRequestState, WsRpcHandler, WsSessionId}
import izumi.logstage.api.IzLogger
import logstage.LogIO2
import org.asynchttpclient.netty.ws.NettyWebSocket
import org.asynchttpclient.ws.{WebSocket, WebSocketListener, WebSocketUpgradeHandler}
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.http4s.Uri

import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters.*
import scala.util.Try

class WsRpcDispatcherFactory[F[+_, +_]: Async2: Temporal2: Primitives2: UnsafeRun2](
  codec: IRTClientMultiplexor[F],
  printer: Printer,
  logger: LogIO2[F],
  izLogger: IzLogger,
  entropy2: Entropy2[F],
  entropy1: Entropy1[Identity],
) {

  def connect[ServerContext](
    uri: Uri,
    serverMuxer: IRTServerMultiplexor[F, ServerContext],
    wsContextExtractor: WsContextExtractor[ServerContext],
    headers: Map[String, String] = Map.empty,
  ): Lifecycle[F[Throwable, _], WsRpcClientConnection[F]] = {
    for {
      client         <- createAsyncHttpClient()
      wsRequestState <- Lifecycle.liftF(F.syncThrowable(WsRequestState.create[F]))
      listener       <- Lifecycle.liftF(F.syncThrowable(createListener(serverMuxer, wsRequestState, wsContextExtractor, dispatcherLogger(uri, logger))))
      handler        <- Lifecycle.liftF(F.syncThrowable(new WebSocketUpgradeHandler(List(listener).asJava)))
      nettyWebSocket <- Lifecycle.make(
        F.fromFutureJava {
          client
            .prepareGet(uri.toString())
            .setSingleHeaders(headers.asJava)
            .execute(handler).toCompletableFuture
        }
      )(nettyWebSocket => fromNettyFuture(nettyWebSocket.sendCloseFrame()).void)
      sessionId = Option(nettyWebSocket.getUpgradeHeaders.get(HttpServer.`X-Ws-Session-Id`.toString))
        .flatMap(str => Try(WsSessionId(UUID.fromString(str))).toOption)
      // fill promises before closing WS connection, potentially giving a chance to send out an error response before closing
      _ <- Lifecycle.make(F.unit)(_ => wsRequestState.clear())
    } yield {
      new WsRpcClientConnection.Netty(nettyWebSocket, wsRequestState, printer, sessionId, entropy2)
    }
  }

  final def connectSimple(
    uri: Uri,
    serverMuxer: IRTServerMultiplexor[F, Unit],
    headers: Map[String, String] = Map.empty,
  ): Lifecycle[F[Throwable, _], WsRpcClientConnection[F]] = {
    connect(uri, serverMuxer, WsContextExtractor.unit, headers)
  }

  def dispatcher[ServerContext](
    uri: Uri,
    serverMuxer: IRTServerMultiplexor[F, ServerContext],
    wsContextExtractor: WsContextExtractor[ServerContext],
    headers: Map[String, String]         = Map.empty,
    tweakRequest: RpcPacket => RpcPacket = identity,
    timeout: FiniteDuration              = 30.seconds,
  ): Lifecycle[F[Throwable, _], IRTDispatcherWs[F]] = {
    connect(uri, serverMuxer, wsContextExtractor, headers).map {
      new WsRpcDispatcher(_, timeout, codec, dispatcherLogger(uri, logger), entropy2) {
        override protected def buildRequest(rpcPacketId: RpcPacketId, method: IRTMethodId, body: Json): RpcPacket = {
          tweakRequest(super.buildRequest(rpcPacketId, method, body))
        }
      }
    }
  }

  final def dispatcherSimple(
    uri: Uri,
    serverMuxer: IRTServerMultiplexor[F, Unit],
    headers: Map[String, String]         = Map.empty,
    tweakRequest: RpcPacket => RpcPacket = identity,
    timeout: FiniteDuration              = 30.seconds,
  ): Lifecycle[F[Throwable, _], IRTDispatcherWs[F]] = {
    dispatcher(uri, serverMuxer, WsContextExtractor.unit, headers, tweakRequest, timeout)
  }

  protected def wsHandler[ServerContext](
    serverMuxer: IRTServerMultiplexor[F, ServerContext],
    wsRequestState: WsRequestState[F],
    wsContextExtractor: WsContextExtractor[ServerContext],
    logger: LogIO2[F],
  ): WsRpcHandler[F, ServerContext] = {
    new ClientWsRpcHandler(serverMuxer, wsRequestState, wsContextExtractor, logger, entropy1)
  }

  protected def createListener[ServerContext](
    serverMuxer: IRTServerMultiplexor[F, ServerContext],
    wsRequestState: WsRequestState[F],
    wsContextExtractor: WsContextExtractor[ServerContext],
    logger: LogIO2[F],
  ): WebSocketListener = new WebSocketListener() {
    private val handler   = wsHandler(serverMuxer, wsRequestState, wsContextExtractor, logger)
    private val socketRef = new AtomicReference[Option[WebSocket]](None)

    override def onOpen(websocket: WebSocket): Unit = {
      socketRef.set(Some(websocket))
    }

    override def onClose(websocket: WebSocket, code: Int, reason: String): Unit = {
      socketRef.set(None)
      websocket.sendCloseFrame()
      ()
    }

    override def onError(t: Throwable): Unit = {
      socketRef.getAndSet(None).foreach(_.sendCloseFrame())
    }

    override def onPingFrame(payload: Array[Byte]): Unit = {
      socketRef.get().foreach(_.sendPongFrame())
    }

    override def onTextFrame(payload: String, finalFragment: Boolean, rsv: Int): Unit = {
      UnsafeRun2[F].unsafeRunAsync(handler.processRpcMessage(payload)) {
        exit =>
          val maybeResponse: Option[RpcPacket] = exit match {
            case Exit.Success(response)         => response
            case Exit.Error(error, _)           => handleWsError(List(error), "errored")
            case Exit.Termination(error, _, _)  => handleWsError(List(error), "terminated")
            case Exit.Interruption(error, _, _) => handleWsError(List(error), "interrupted")
          }
          maybeResponse.foreach {
            response =>
              socketRef.get().foreach {
                ws => ws.sendTextFrame(printer.print(response.asJson))
              }
          }
      }
    }
  }

  protected def dispatcherLogger(uri: Uri, logger: LogIO2[F]): LogIO2[F] = {
    uri.discard()
    logger
  }

  private def handleWsError(causes: List[Throwable], message: String): Option[RpcPacket] = {
    causes.headOption match {
      case Some(cause) =>
        izLogger.error(s"WS request failed: $message, $cause")
        Some(RpcPacket.rpcCritical(s"$message, cause: $cause", None))
      case None =>
        izLogger.error(s"WS request failed: $message.")
        Some(RpcPacket.rpcCritical(message, None))
    }
  }

  protected def createAsyncHttpClient(): Lifecycle[F[Throwable, _], DefaultAsyncHttpClient] = {
    Lifecycle.fromAutoCloseable(F.syncThrowable {
      new DefaultAsyncHttpClient(
        new DefaultAsyncHttpClientConfig.Builder()
          .setWebSocketMaxBufferSize(64 * 1024 * 1024 * 8) // increase buffer size for 64MB, 128000000 - is default value
          .setWebSocketMaxFrameSize(64 * 1024 * 1024 * 8) // increase frame size for 64MB
          .setKeepAlive(true)
          .setSoKeepAlive(true)
          .setRequestTimeout(Duration.ofSeconds(30)) // 60 seconds is default
          .setPooledConnectionIdleTimeout(Duration.ofSeconds(60)) // 60 seconds is default
          .setConnectTimeout(Duration.ofSeconds(30)) // 5 seconds is default
          .setReadTimeout(Duration.ofSeconds(60)) // 60 seconds is default
          .setShutdownTimeout(Duration.ofSeconds(15)) // 15 seconds is default
          .build()
      )
    })
  }
}

object WsRpcDispatcherFactory {

  class ClientWsRpcHandler[F[+_, +_]: IO2, RequestCtx](
    muxer: IRTServerMultiplexor[F, RequestCtx],
    requestState: WsRequestState[F],
    wsContextExtractor: WsContextExtractor[RequestCtx],
    logger: LogIO2[F],
    entropy1: Entropy1[Identity],
  ) extends WsRpcHandler[F, RequestCtx](muxer, requestState, logger) {
    private val wsSessionId: WsSessionId                   = WsSessionId(entropy1.nextTimeUUID())
    private val requestCtxRef: AtomicReference[RequestCtx] = new AtomicReference()
    override protected def updateRequestCtx(packet: RpcPacket): F[Throwable, RequestCtx] = F.sync {
      val updated = wsContextExtractor.extract(wsSessionId, packet)
      requestCtxRef.updateAndGet {
        case null     => updated
        case previous => wsContextExtractor.merge(previous, updated)
      }
    }
  }

  trait WsRpcClientConnection[F[_, _]] {
    private[clients] def requestAndAwait(id: RpcPacketId, packet: RpcPacket, method: Option[IRTMethodId], timeout: FiniteDuration): F[Throwable, Option[RawResponse]]
    def sessionId: Option[WsSessionId]
    def authorize(headers: Map[String, String], timeout: FiniteDuration = 30.seconds): F[Throwable, Unit]
  }
  object WsRpcClientConnection {
    class Netty[F[+_, +_]: Async2](
      nettyWebSocket: NettyWebSocket,
      requestState: WsRequestState[F],
      printer: Printer,
      val sessionId: Option[WsSessionId],
      entropy2: Entropy2[F],
    ) extends WsRpcClientConnection[F] {

      override def authorize(headers: Map[String, String], timeout: FiniteDuration): F[Throwable, Unit] = {
        RpcPacketId.random(entropy2).flatMap {
          packetId =>
            requestAndAwait(packetId, RpcPacket.auth(packetId, headers), None, timeout).flatMap {
              case Some(_: RawResponse.GoodRawResponse)    => F.unit
              case Some(_: RawResponse.EmptyRawResponse)   => F.unit
              case Some(value: RawResponse.BadRawResponse) => F.fail(new IRTGenericFailure(s"Authorization failed: ${value.error}."))
              case None                                    => F.fail(new IRTGenericFailure("Unable to authorize."))
            }
        }
      }

      override private[clients] def requestAndAwait(
        id: RpcPacketId,
        packet: RpcPacket,
        method: Option[IRTMethodId],
        timeout: FiniteDuration,
      ): F[Throwable, Option[RawResponse]] = {
        requestState.requestAndAwait(id, method, timeout) {
          fromNettyFuture(nettyWebSocket.sendTextFrame(printer.print(packet.asJson)))
        }
      }
    }
  }

  private def fromNettyFuture[F[+_, +_]: Async2, A](mkNettyFuture: => io.netty.util.concurrent.Future[A]): F[Throwable, A] = {
    F.syncThrowable(mkNettyFuture).flatMap {
      nettyFuture =>
        F.asyncCancelable {
          callback =>
            nettyFuture.addListener {
              (completedFuture: io.netty.util.concurrent.Future[A]) =>
                try {
                  if (!completedFuture.isDone) {
                    // shouldn't be possible, future should already be completed
                    completedFuture.await(1000L)
                  }
                  if (completedFuture.isSuccess) {
                    callback(Right(completedFuture.getNow))
                  } else {
                    Option(completedFuture.cause()) match {
                      case Some(error) => callback(Left(error))
                      case None        => callback(Left(new RuntimeException("Awaiting NettyFuture failed, but no exception was available.")))
                    }
                  }
                } catch {
                  case exception: Throwable =>
                    callback(Left(new RuntimeException(s"Awaiting NettyFuture threw an exception=$exception")))
                }
            }
            val canceler = F.sync {
              nettyFuture.cancel(false);
              ()
            }
            canceler
        }
    }
  }

}

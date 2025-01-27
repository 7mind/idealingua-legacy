package izumi.idealingua.runtime.rpc.http4s

import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Queue
import fs2.Stream
import io.circe
import io.circe.syntax.EncoderOps
import io.circe.{Json, Printer}
import izumi.functional.bio.Exit.{Error, Interruption, Success, Termination}
import izumi.functional.bio.{Clock1, Entropy1, Exit, F, IO2, Primitives2, Temporal2, UnsafeRun2}
import izumi.fundamentals.platform.functional.Identity
import izumi.fundamentals.platform.language.Quirks
import izumi.fundamentals.platform.language.Quirks.Discarder
import izumi.idealingua.runtime.rpc.*
import izumi.idealingua.runtime.rpc.http4s.HttpServer.{ServerWsRpcHandler, WsResponseMarker}
import izumi.idealingua.runtime.rpc.http4s.context.{HttpContextExtractor, WsContextExtractor}
import izumi.idealingua.runtime.rpc.http4s.ws.*
import logstage.LogIO2
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.typelevel.ci.CIString
import org.typelevel.vault.Key

import java.time.ZonedDateTime
import java.util.concurrent.RejectedExecutionException
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class HttpServer[F[+_, +_]: IO2: Temporal2: Primitives2: UnsafeRun2, AuthCtx](
  val contextServices: Set[IRTContextServices.AnyContext[F, AuthCtx]],
  val httpContextExtractor: HttpContextExtractor[AuthCtx],
  val wsContextExtractor: WsContextExtractor[AuthCtx],
  val wsSessionsStorage: WsSessionsStorage[F, AuthCtx],
  dsl: Http4sDsl[F[Throwable, _]],
  logger: LogIO2[F],
  printer: Printer,
  entropy1: Entropy1[Identity],
)(implicit val AT: Async[F[Throwable, _]]
) {
  import dsl.*
  // WS Response attribute key, to differ from usual HTTP responses
  protected final val wsAttributeKey = UnsafeRun2[F].unsafeRun(Key.newKey[F[Throwable, _], WsResponseMarker.type])

  protected val serverMuxer: IRTServerMultiplexor[F, AuthCtx]                     = IRTServerMultiplexor.combine(contextServices.map(_.authorizedMuxer))
  protected val wsContextsSessions: Set[WsContextSessions.AnyContext[F, AuthCtx]] = contextServices.map(_.authorizedWsSessions)
  protected val wsHeartbeatTimeout: FiniteDuration                                = 1.minute
  protected val wsHeartbeatInterval: FiniteDuration                               = 10.seconds

  def service(ws: WebSocketBuilder2[F[Throwable, _]]): HttpRoutes[F[Throwable, _]] = {
    val svc = HttpRoutes.of(router(ws))
    loggingMiddle(svc)
  }

  protected def router(ws: WebSocketBuilder2[F[Throwable, _]]): PartialFunction[Request[F[Throwable, _]], F[Throwable, Response[F[Throwable, _]]]] = {
    case request @ GET -> Root / "ws"              => setupWs(request, ws)
    case request @ GET -> Root / service / method  => processHttpRequest(request, service, method)("{}")
    case request @ POST -> Root / service / method => request.decode[String](processHttpRequest(request, service, method))
  }

  protected def setupWs(
    request: Request[F[Throwable, _]],
    ws: WebSocketBuilder2[F[Throwable, _]],
  ): F[Throwable, Response[F[Throwable, _]]] = {
    Quirks.discard(request)
    def pingStream(clientSession: WsClientSession[F, AuthCtx]): Stream[F[Throwable, _], WebSocketFrame] = {
      Stream
        .awakeEvery[F[Throwable, _]](wsHeartbeatInterval)
        .evalMap[F[Throwable, _], WebSocketFrame] {
          _ =>
            for {
              expiration <- F.sync(Clock1.Standard.nowZoned().minusNanos(wsHeartbeatTimeout.toNanos))
              frame <- clientSession.lastHeartbeat().flatMap {
                case Some(lastHeartbeat) if lastHeartbeat.isBefore(expiration) =>
                  logger.warn(s"WS Session: Websocket client heartbeat timeout: ${clientSession.sessionId}, $wsHeartbeatTimeout.") *>
                  F.fromEither(WebSocketFrame.Close(1006, s"Ping-Pong heartbeat timed-out after '$wsHeartbeatTimeout'."))
                case _ =>
                  logger.debug("WS Server: Sending ping frame.").as(WebSocketFrame.Ping())
              }
            } yield frame
        }.takeThrough {
          case _: WebSocketFrame.Close => false
          case _                       => true
        }
    }
    for {
      outQueue     <- Queue.unbounded[F[Throwable, _], WebSocketFrame]
      authContext  <- F.syncThrowable(httpContextExtractor.extract(request))
      clientSession = new WsClientSession.Queued(outQueue, authContext, wsContextsSessions, wsSessionsStorage, wsContextExtractor, logger, printer, entropy1)
      _            <- clientSession.start(onWsConnected)

      outStream = Stream.fromQueueUnterminated(outQueue).merge(pingStream(clientSession))
      inStream = {
        (inputStream: Stream[F[Throwable, _], WebSocketFrame]) =>
          inputStream.evalMap {
            processWsRequest(clientSession, Clock1.Standard.nowZoned())(_).flatMap {
              case Some(v) => outQueue.offer(WebSocketFrame.Text(v))
              case None    => F.unit
            }
          }
      }
      wsSessionIdHeader = Header.Raw(HttpServer.`X-Ws-Session-Id`, clientSession.sessionId.sessionId.toString)

      response <- ws
        .withFilterPingPongs(false)
        .withOnClose(handleWsClose(clientSession))
        .withHeaders(Headers(wsSessionIdHeader))
        .build(outStream, inStream)
    } yield {
      response.withAttribute(wsAttributeKey, WsResponseMarker)
    }
  }

  protected def processWsRequest(
    clientSession: WsClientSession[F, AuthCtx],
    requestTime: ZonedDateTime,
  )(frame: WebSocketFrame
  ): F[Throwable, Option[String]] = {
    (frame match {
      case WebSocketFrame.Text(msg, _) =>
        wsHandler(clientSession).processRpcMessage(msg)
      case WebSocketFrame.Close(_) =>
        F.pure(None)
      case _: WebSocketFrame.Pong =>
        clientSession.heartbeat(requestTime) *>
        onWsHeartbeat(requestTime).as(None)
      case unknownMessage =>
        val message = s"Unsupported WS frame: $unknownMessage."
        logger.error(s"WS request failed: $message.").as(Some(RpcPacket.rpcCritical(message, None)))
    }).map(_.map(p => printer.print(p.asJson)))
  }

  protected def wsHandler(clientSession: WsClientSession[F, AuthCtx]): WsRpcHandler[F, AuthCtx] = {
    new ServerWsRpcHandler(clientSession, serverMuxer, wsContextExtractor, logger)
  }

  protected def handleWsClose(session: WsClientSession[F, AuthCtx]): F[Throwable, Unit] = {
    logger.debug(s"WS Session: Websocket client disconnected ${session.sessionId}.") *>
    session.finish(onWsDisconnected)
  }

  protected def onWsConnected(authContext: AuthCtx): F[Throwable, Unit] = {
    authContext.discard()
    F.unit
  }

  protected def onWsHeartbeat(requestTime: ZonedDateTime): F[Throwable, Unit] = {
    logger.debug(s"WS Session: pong frame at $requestTime")
  }

  protected def onWsDisconnected(authContext: AuthCtx): F[Throwable, Unit] = {
    authContext.discard()
    F.unit
  }

  protected def processHttpRequest(
    request: Request[F[Throwable, _]],
    serviceName: String,
    methodName: String,
  )(body: String
  ): F[Throwable, Response[F[Throwable, _]]] = {
    val methodId = IRTMethodId(IRTServiceId(serviceName), IRTMethodName(methodName))
    (for {
      authContext <- F.syncThrowable(httpContextExtractor.extract(request))
      parsedBody  <- F.fromEither(io.circe.parser.parse(body)).leftMap(err => new IRTDecodingException(s"Can not parse JSON body '$body'.", Some(err)))
      invokeRes   <- serverMuxer.invokeMethod(methodId)(authContext, parsedBody)
    } yield invokeRes).sandboxExit.flatMap(handleHttpResult(request, methodId))
  }

  protected def handleHttpResult(
    request: Request[F[Throwable, _]],
    method: IRTMethodId,
  )(result: Exit[Throwable, Json]
  ): F[Throwable, Response[F[Throwable, _]]] = {
    result match {
      case Success(res) =>
        Ok(printer.print(res))

      case Error(err: IRTMissingHandlerException, _) =>
        logger.warn(s"HTTP Request execution failed - no method handler for $method: $err") *>
        NotFound()

      case Error(error: circe.Error, trace) =>
        logger.warn(s"HTTP Request execution failed - parsing failure while handling $method:\n${error.getMessage -> "error"}\n$trace") *>
        BadRequest()

      case Error(error: IRTDecodingException, trace) =>
        logger.warn(s"HTTP Request execution failed - parsing failure while handling $method:\n$error\n$trace") *>
        BadRequest()

      case Error(error: IRTLimitReachedException, trace) =>
        logger.debug(s"HTTP Request failed - request limit reached $method:\n$error\n$trace") *>
        TooManyRequests()

      case Error(error: IRTUnathorizedRequestContextException, trace) =>
        logger.debug(s"HTTP Request failed - unauthorized $method call:\n$error\n$trace") *>
        F.pure(Response(status = Status.Unauthorized))

      case Error(error, trace) =>
        logger.warn(s"HTTP Request unexpectedly failed while handling $method:\n$error\n$trace") *>
        InternalServerError()

      case Termination(_, (cause: IRTHttpFailureException) :: _, trace) =>
        logger.error(s"HTTP Request rejected - $method, $request:\n$cause\n$trace") *>
        F.pure(Response(status = cause.status))

      case Termination(_, (cause: RejectedExecutionException) :: _, trace) =>
        logger.warn(s"HTTP Request rejected - Not enough capacity to handle $method:\n$cause\n$trace") *>
        TooManyRequests()

      case Termination(cause, _, trace) =>
        logger.error(s"HTTP Request execution failed, termination, $method, $request:\n$cause\n$trace") *>
        InternalServerError()

      case Interruption(cause, _, trace) =>
        logger.error(s"HTTP Request unexpectedly interrupted while handling $method:\n$cause\n$trace") *>
        InternalServerError()
    }
  }

  protected def loggingMiddle(service: HttpRoutes[F[Throwable, _]]): HttpRoutes[F[Throwable, _]] = {
    cats.data.Kleisli {
      (req: Request[F[Throwable, _]]) =>
        OptionT.apply {
          (for {
            _    <- logger.trace(s"${req.method.name -> "method"} ${req.pathInfo -> "path"}: initiated")
            resp <- service(req).value
            _ <- F.traverse(resp) {
              case Status.Successful(resp) =>
                logger.debug(s"${req.method.name -> "method"} ${req.pathInfo -> "path"}: success, ${resp.status.code -> "code"} ${resp.status.reason -> "reason"}")
              case resp if resp.attributes.contains(wsAttributeKey) =>
                logger.debug(s"${req.method.name -> "method"} ${req.pathInfo -> "path"}: websocket request")
              case resp =>
                logger.info(s"${req.method.name -> "method"} ${req.pathInfo -> "uri"}: rejection, ${resp.status.code -> "code"} ${resp.status.reason -> "reason"}")
            }
          } yield resp).tapError {
            case cause: InvalidBodyException => logger.debug(s"${req.method.name -> "method"} ${req.pathInfo -> "path"}: invalid body, $cause")
            case cause                       => logger.error(s"${req.method.name -> "method"} ${req.pathInfo -> "path"}: failure, $cause")
          }
        }
    }
  }
}

object HttpServer {
  val `X-Ws-Session-Id`: CIString = CIString("X-Ws-Session-Id")
  case object WsResponseMarker
  class ServerWsRpcHandler[F[+_, +_]: IO2, AuthCtx](
    clientSession: WsClientSession[F, AuthCtx],
    muxer: IRTServerMultiplexor[F, AuthCtx],
    wsContextExtractor: WsContextExtractor[AuthCtx],
    logger: LogIO2[F],
  ) extends WsRpcHandler[F, AuthCtx](muxer, clientSession, logger) {
    override protected def updateRequestCtx(packet: RpcPacket): F[Throwable, AuthCtx] = {
      clientSession.updateRequestCtx(wsContextExtractor.extract(clientSession.sessionId, packet))
    }
  }
}

package izumi.idealingua.runtime.rpc.http4s

import izumi.functional.bio.{IO2, Monad2}
import izumi.idealingua.runtime.rpc.http4s.ws.WsContextSessions
import izumi.idealingua.runtime.rpc.{IRTServerMiddleware, IRTServerMultiplexor}

trait IRTContextServices[F[+_, +_], AuthCtx, RequestCtx, WsCtx] {
  def authenticator: IRTAuthenticator[F, AuthCtx, RequestCtx]
  def serverMuxer: IRTServerMultiplexor[F, RequestCtx]
  def middlewares: Set[IRTServerMiddleware[F, RequestCtx]]
  def wsSessions: WsContextSessions[F, RequestCtx, WsCtx]

  def authorizedMuxer(implicit io2: IO2[F]): IRTServerMultiplexor[F, AuthCtx] = {
    val withMiddlewares: IRTServerMultiplexor[F, RequestCtx] = middlewares.toList.sortBy(_.priority).foldLeft(serverMuxer) {
      case (muxer, middleware) => muxer.wrap(middleware)
    }
    val authorized: IRTServerMultiplexor[F, AuthCtx] = withMiddlewares.contramap {
      case (authCtx, body) => authenticator.authenticate(authCtx, Some(body))
    }
    authorized
  }
  def authorizedWsSessions(implicit M: Monad2[F]): WsContextSessions[F, AuthCtx, WsCtx] = {
    val authorized: WsContextSessions[F, AuthCtx, WsCtx] = wsSessions.contramap {
      authCtx =>
        authenticator.authenticate(authCtx, None)
    }
    authorized
  }
}

object IRTContextServices {
  type AnyContext[F[+_, +_], AuthCtx] = IRTContextServices[F, AuthCtx, ?, ?]

  def apply[F[+_, +_], AuthCtx, RequestCtx, WsCtx](
    authenticator: IRTAuthenticator[F, AuthCtx, RequestCtx],
    serverMuxer: IRTServerMultiplexor[F, RequestCtx],
    middlewares: Set[IRTServerMiddleware[F, RequestCtx]],
    wsSessions: WsContextSessions[F, RequestCtx, WsCtx],
  ): Default[F, AuthCtx, RequestCtx, WsCtx] = Default(authenticator, serverMuxer, middlewares, wsSessions)

  final case class Default[F[+_, +_], AuthCtx, RequestCtx, WsCtx](
    authenticator: IRTAuthenticator[F, AuthCtx, RequestCtx],
    serverMuxer: IRTServerMultiplexor[F, RequestCtx],
    middlewares: Set[IRTServerMiddleware[F, RequestCtx]],
    wsSessions: WsContextSessions[F, RequestCtx, WsCtx],
  ) extends IRTContextServices[F, AuthCtx, RequestCtx, WsCtx]
}

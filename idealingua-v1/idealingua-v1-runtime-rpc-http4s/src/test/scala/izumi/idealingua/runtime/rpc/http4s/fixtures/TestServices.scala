package izumi.idealingua.runtime.rpc.http4s.fixtures

import io.circe.Json
import izumi.functional.bio.{F, IO2}
import izumi.idealingua.runtime.rpc.*
import izumi.idealingua.runtime.rpc.http4s.IRTAuthenticator.AuthContext
import izumi.idealingua.runtime.rpc.http4s.context.WsIdExtractor
import izumi.idealingua.runtime.rpc.http4s.ws.WsContextSessions.WsContextSessionsImpl
import izumi.idealingua.runtime.rpc.http4s.ws.WsSessionsStorage.WsSessionsStorageImpl
import izumi.idealingua.runtime.rpc.http4s.ws.{WsContextSessions, WsSessionId, WsSessionListener, WsSessionsStorage}
import izumi.idealingua.runtime.rpc.http4s.{IRTAuthenticator, IRTContextServices}
import izumi.r2.idealingua.test.generated.*
import izumi.r2.idealingua.test.impls.AbstractGreeterServer
import logstage.LogIO2
import org.http4s.BasicCredentials
import org.http4s.headers.Authorization

class TestServices[F[+_, +_]: IO2](
  logger: LogIO2[F]
) {

  object Server {
    def userBlacklistMiddleware[C <: TestContext](
      rejectedNames: Set[String]
    ): IRTServerMiddleware[F, C] = new IRTServerMiddleware[F, C] {
      override def priority: Int = 0
      override def prepare(methodId: IRTMethodId)(context: C, parsedBody: Json): F[Throwable, Unit] = {
        F.when(rejectedNames.contains(context.user)) {
          F.fail(new IRTUnathorizedRequestContextException(s"Rejected for users: $rejectedNames."))
        }
      }
    }
    final val wsStorage: WsSessionsStorage[F, AuthContext] = new WsSessionsStorageImpl[F, AuthContext](logger)
    final val globalWsListeners = Set(
      new WsSessionListener[F, Any, Any] {
        override def onSessionOpened(sessionId: WsSessionId, reqCtx: Any, wsCtx: Any): F[Throwable, Unit] = {
          logger.debug(s"WS Session: $sessionId opened $wsCtx on $reqCtx.")
        }
        override def onSessionUpdated(sessionId: WsSessionId, reqCtx: Any, prevStx: Any, newCtx: Any): F[Throwable, Unit] = {
          logger.debug(s"WS Session: $sessionId updated $newCtx from $prevStx on $reqCtx.")
        }
        override def onSessionClosed(sessionId: WsSessionId, wsCtx: Any): F[Throwable, Unit] = {
          logger.debug(s"WS Session: $sessionId closed $wsCtx .")
        }
      }
    )
    // PRIVATE
    final val privateAuth = new IRTAuthenticator[F, AuthContext, PrivateContext] {
      override def authenticate(authContext: AuthContext, body: Option[Json]): F[Nothing, Option[PrivateContext]] = F.sync {
        authContext.headers.get[Authorization].map(_.credentials).collect {
          case BasicCredentials(user, "private") => PrivateContext(user)
        }
      }
    }
    final val privateWsListener: LoggingWsListener[F, PrivateContext, PrivateContext] = new LoggingWsListener[F, PrivateContext, PrivateContext]
    final val privateWsSession: WsContextSessions[F, PrivateContext, PrivateContext] = {
      new WsContextSessionsImpl(wsStorage, globalWsListeners, Set(privateWsListener), WsIdExtractor.id[PrivateContext])
    }
    final val privateService: IRTWrappedService[F, PrivateContext] = new PrivateTestServiceWrappedServer(new PrivateTestServiceServer[F, PrivateContext] {
      def test(ctx: PrivateContext, str: String): Just[String] = F.pure(s"Private: $str")
    })
    final val privateServices: IRTContextServices[F, AuthContext, PrivateContext, PrivateContext] = IRTContextServices(
      authenticator = privateAuth,
      serverMuxer   = new IRTServerMultiplexor.FromServices(Set(privateService)),
      middlewares   = Set.empty,
      wsSessions    = privateWsSession,
    )

    // PROTECTED
    final val protectedAuth = new IRTAuthenticator[F, AuthContext, ProtectedContext] {
      override def authenticate(authContext: AuthContext, body: Option[Json]): F[Nothing, Option[ProtectedContext]] = F.sync {
        authContext.headers.get[Authorization].map(_.credentials).collect {
          case BasicCredentials(user, "protected") => ProtectedContext(user)
        }
      }
    }
    final val protectedWsListener: LoggingWsListener[F, ProtectedContext, ProtectedContext] = new LoggingWsListener[F, ProtectedContext, ProtectedContext]
    final val protectedWsSession: WsContextSessions[F, ProtectedContext, ProtectedContext] = {
      new WsContextSessionsImpl(wsStorage, globalWsListeners, Set(protectedWsListener), WsIdExtractor.id)
    }
    final val protectedService: IRTWrappedService[F, ProtectedContext] = new ProtectedTestServiceWrappedServer(new ProtectedTestServiceServer[F, ProtectedContext] {
      def test(ctx: ProtectedContext, str: String): Just[String] = F.pure(s"Protected: $str")
    })
    final val protectedServices: IRTContextServices[F, AuthContext, ProtectedContext, ProtectedContext] = IRTContextServices(
      authenticator = protectedAuth,
      serverMuxer   = new IRTServerMultiplexor.FromServices(Set(protectedService)),
      middlewares   = Set.empty,
      wsSessions    = protectedWsSession,
    )

    // PUBLIC
    final val publicAuth = new IRTAuthenticator[F, AuthContext, PublicContext] {
      override def authenticate(authContext: AuthContext, body: Option[Json]): F[Nothing, Option[PublicContext]] = F.sync {
        authContext.headers.get[Authorization].map(_.credentials).collect {
          case BasicCredentials(user, _) => PublicContext(user)
        }
      }
    }
    final val publicWsListener: LoggingWsListener[F, PublicContext, PublicContext] = new LoggingWsListener[F, PublicContext, PublicContext]
    final val publicWsSession: WsContextSessions[F, PublicContext, PublicContext] = {
      new WsContextSessionsImpl(wsStorage, globalWsListeners, Set(publicWsListener), WsIdExtractor.id)
    }
    final val publicService: IRTWrappedService[F, PublicContext] = new GreeterServiceServerWrapped(new AbstractGreeterServer.Impl[F, PublicContext])
    final val publicServices: IRTContextServices[F, AuthContext, PublicContext, PublicContext] = IRTContextServices(
      authenticator = publicAuth,
      serverMuxer   = new IRTServerMultiplexor.FromServices(Set(publicService)),
      middlewares   = Set(userBlacklistMiddleware(Set("orc"))),
      wsSessions    = publicWsSession,
    )

    final val contextServices: Set[IRTContextServices[F, AuthContext, ?, ?]] = Set(privateServices, protectedServices, publicServices)
  }

  object Client {
    private val greeterService                               = new AbstractGreeterServer.Impl[F, Unit]
    private val greeterDispatcher                            = new GreeterServiceServerWrapped(greeterService)
    private val dispatchers: Set[IRTWrappedService[F, Unit]] = Set(greeterDispatcher)

    private val clients: Set[IRTWrappedClient] = Set(
      GreeterServiceClientWrapped,
      ProtectedTestServiceWrappedClient,
      PrivateTestServiceWrappedClient,
    )
    val codec: IRTClientMultiplexorImpl[F]               = new IRTClientMultiplexorImpl[F](clients)
    val buzzerMultiplexor: IRTServerMultiplexor[F, Unit] = new IRTServerMultiplexor.FromServices(dispatchers)
  }
}

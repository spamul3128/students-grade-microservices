package com.education.grades.audit

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.education.grades.common.config.*
import com.education.grades.common.infrastructure.db.Database
import com.education.grades.common.auth.{AuthUser, UserId, UserRole}
import com.education.grades.audit.service.{AuditLoggingServiceImpl, AuditLogRepository}
import com.education.grades.audit.http.AuditLoggingRoutes
import fs2.io.net.Network
import java.util.UUID

object AuditLoggingServer extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

  def server[F[_]: Async: Network: cats.effect.std.Console]: Resource[F, Unit] =
    val config = AppConfig(
      server = ServerConfig("0.0.0.0", 8084),
      database = DatabaseConfig(
        host = sys.env.getOrElse("DB_HOST", "localhost"),
        port = sys.env.getOrElse("DB_PORT", "5432").toInt,
        database = sys.env.getOrElse("DB_NAME", "grades_db"),
        user = sys.env.getOrElse("DB_USER", "postgres"),
        password = sys.env.getOrElse("DB_PASSWORD", "postgres"),
        maxConnections = 10
      ),
      redis = RedisConfig(
        host = sys.env.getOrElse("REDIS_HOST", "localhost"),
        port = sys.env.getOrElse("REDIS_PORT", "6379").toInt,
        password = None
      ),
      auth = AuthConfig(
        jwtSecret = sys.env.getOrElse("JWT_SECRET", "secret-key-change-in-production"),
        tokenExpiration = scala.concurrent.duration.Duration(1, "hour")
      )
    )

    // Create stub user for auth middleware
    val stubUser = AuthUser(
      id = UserId(UUID.randomUUID()),
      username = "stub",
      email = "stub@example.com",
      roles = Set(UserRole.Admin),
      studentId = None,
      teacherId = None
    )

    // Auth middleware
    val authMiddleware: org.http4s.AuthedRoutes[AuthUser, F] => org.http4s.HttpRoutes[F] =
      (authedRoutes: org.http4s.AuthedRoutes[AuthUser, F]) =>
        org.http4s.HttpRoutes.of[F] { case req =>
          val contextReq = org.http4s.ContextRequest(stubUser, req)
          authedRoutes.run(contextReq).getOrElse(org.http4s.Response.notFound)
        }

    for
      // Database session pool
      sessionPool <- Database.sessionPool[F](config.database)

      // Placeholder repository
      auditRepo = new AuditLogRepository[F]:
        import com.education.grades.audit.service.{AuditLog, AuditLogId}
        import java.time.Instant
        def create(log: AuditLog): F[AuditLog] = ???
        def findFiltered(
          entityType: Option[String],
          entityId: Option[String],
          userId: Option[UserId],
          from: Option[Instant],
          to: Option[Instant],
          limit: Int
        ): F[List[AuditLog]] = ???

      // Service
      auditService = new AuditLoggingServiceImpl[F](auditRepo)

      // Routes
      routes = new AuditLoggingRoutes[F](auditService)
      httpApp = routes.routes(authMiddleware).orNotFound

      // Add logging
      finalApp = Logger.httpApp(logHeaders = true, logBody = false)(httpApp)

      // Start server
      _ <- EmberServerBuilder
        .default[F]
        .withHost(Host.fromString(config.server.host).get)
        .withPort(Port.fromInt(config.server.port).get)
        .withHttpApp(finalApp)
        .build

      _ <- Resource.eval(Async[F].delay(println(s"Audit Logging Service started on ${config.server.host}:${config.server.port}")))

    yield ()


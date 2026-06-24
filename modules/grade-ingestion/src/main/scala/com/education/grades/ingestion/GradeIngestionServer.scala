package com.education.grades.ingestion

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.education.grades.common.config.*
import com.education.grades.common.infrastructure.db.Database
import com.education.grades.common.infrastructure.messaging.{RedisMessaging, EventPublisher}
import com.education.grades.ingestion.repository.PostgresGradeRepository
import com.education.grades.ingestion.service.{GradeIngestionServiceImpl, AssignmentRepository}
import com.education.grades.ingestion.http.GradeIngestionRoutes
import fs2.io.net.Network

object GradeIngestionServer extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

  def server[F[_]: Async: Network: cats.effect.std.Console]: Resource[F, Unit] =
    import com.education.grades.common.auth.{AuthUser, UserId, UserRole}
    import java.util.UUID

    val config = AppConfig(
      server = ServerConfig("0.0.0.0", 8081),
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

    // Define auth middleware function
    val authMiddleware: org.http4s.AuthedRoutes[AuthUser, F] => org.http4s.HttpRoutes[F] =
      (authedRoutes: org.http4s.AuthedRoutes[AuthUser, F]) =>
        org.http4s.HttpRoutes.of[F] { case req =>
          val contextReq = org.http4s.ContextRequest(stubUser, req)
          authedRoutes.run(contextReq).getOrElse(org.http4s.Response.notFound)
        }

    for
      // Database session pool
      sessionPool <- Database.sessionPool[F](config.database)

      // Messaging
      messaging <- RedisMessaging.create[F](config.redis)
      publisher = messaging._1
      subscriber = messaging._2
      eventPublisher = new EventPublisher[F](publisher)

      // Repositories
      gradeRepo = new PostgresGradeRepository[F](sessionPool)

      // TODO: Implement assignment repository
      assignmentRepo = new AssignmentRepository[F]:
        def findById(id: com.education.grades.common.domain.AssignmentId) = ???

      // Service
      gradeService = new GradeIngestionServiceImpl[F](
        gradeRepo,
        eventPublisher,
        assignmentRepo
      )

      // Routes
      routes = new GradeIngestionRoutes[F](gradeService)

      httpApp = routes.routes(authMiddleware).orNotFound

      // Add logging
      finalApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      // Start server
      _ <- EmberServerBuilder
        .default[F]
        .withHost(Host.fromString(config.server.host).get)
        .withPort(Port.fromInt(config.server.port).get)
        .withHttpApp(finalApp)
        .build

      _ <- Resource.eval(Async[F].delay(println(s"Grade Ingestion Service started on ${config.server.host}:${config.server.port}")))

    yield ()


package com.education.grades.auth

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.education.grades.common.config.*
import com.education.grades.common.infrastructure.db.Database
import com.education.grades.auth.service.{AuthServiceImpl, UserRepository, User}
import com.education.grades.auth.repository.UserRepositoryImpl
import com.education.grades.auth.http.AuthRoutes
import com.education.grades.common.auth.UserId
import fs2.io.net.Network
object AuthServer extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

  def server[F[_]: Async: Network: cats.effect.std.Console]: Resource[F, Unit] =
    val config = AppConfig(
      server = ServerConfig("0.0.0.0", 8080),
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

    for
      // Database session pool
      sessionPool <- Database.sessionPool[F](config.database)

      // Repository
      userRepo = new UserRepositoryImpl[F](sessionPool)

      // Service
      authService = new AuthServiceImpl[F](
        userRepo,
        config.auth.jwtSecret,
        config.auth.tokenExpiration
      )

      // Routes
      routes = new AuthRoutes[F](authService)
      httpApp = routes.allRoutes.orNotFound

      // Add logging
      finalApp = Logger.httpApp(logHeaders = true, logBody = false)(httpApp)

      // Start server
      _ <- EmberServerBuilder
        .default[F]
        .withHost(Host.fromString(config.server.host).get)
        .withPort(Port.fromInt(config.server.port).get)
        .withHttpApp(finalApp)
        .build

      _ <- Resource.eval(Async[F].delay(println(s"Auth Service started on ${config.server.host}:${config.server.port}")))

    yield ()



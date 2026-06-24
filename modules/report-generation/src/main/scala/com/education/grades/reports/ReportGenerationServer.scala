package com.education.grades.reports

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.education.grades.common.config.*
import com.education.grades.common.infrastructure.db.Database
import com.education.grades.common.infrastructure.messaging.{RedisMessaging, EventPublisher}
import com.education.grades.common.auth.{AuthUser, UserId, UserRole}
import com.education.grades.common.domain.*
import com.education.grades.reports.service.{ReportGenerationServiceImpl, StudentRepository, CourseGradeRepository, CourseRepository, TeacherRepository}
import com.education.grades.reports.http.ReportGenerationRoutes
import fs2.io.net.Network
import java.util.UUID

object ReportGenerationServer extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

  def server[F[_]: Async: Network: cats.effect.std.Console]: Resource[F, Unit] =
    val config = AppConfig(
      server = ServerConfig("0.0.0.0", 8083),
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

      // Messaging
      messaging <- RedisMessaging.create[F](config.redis)
      publisher = messaging._1
      eventPublisher = new EventPublisher[F](publisher)

      // Placeholder repositories
      // Placeholder repositories
      studentRepo = new StudentRepository[F]:
        def findById(id: StudentId): F[Option[Student]] = ???

      courseGradeRepo = new CourseGradeRepository[F]:
        def findByStudent(studentId: StudentId): F[List[CourseGrade]] = ???
        def findByStudentAndSemester(studentId: StudentId, semester: String, year: Int): F[List[CourseGrade]] = ???

      courseRepo = new CourseRepository[F]:
        def findById(id: CourseId): F[Option[Course]] = ???

      teacherRepo = new TeacherRepository[F]:
        def findById(id: TeacherId): F[Option[Teacher]] = ???

      // Service
      reportService = new ReportGenerationServiceImpl[F](
        studentRepo,
        courseGradeRepo,
        courseRepo,
        teacherRepo,
        eventPublisher
      )

      // Routes
      routes = new ReportGenerationRoutes[F](reportService)
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

      _ <- Resource.eval(Async[F].delay(println(s"Report Generation Service started on ${config.server.host}:${config.server.port}")))

    yield ()


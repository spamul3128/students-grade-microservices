package com.education.grades.calculation

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.education.grades.common.config.*
import com.education.grades.common.infrastructure.db.Database
import com.education.grades.common.infrastructure.messaging.{RedisMessaging, EventPublisher}
import com.education.grades.common.auth.{AuthUser, UserId, UserRole}
import com.education.grades.common.domain.*
import com.education.grades.calculation.service.{GradeCalculationServiceImpl, GradeRepository, AssignmentRepository, CourseGradeRepository}
import com.education.grades.calculation.http.GradeCalculationRoutes
import fs2.io.net.Network
import java.util.UUID

object GradeCalculationServer extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

  def server[F[_]: Async: Network: cats.effect.std.Console]: Resource[F, Unit] =
    val config = AppConfig(
      server = ServerConfig("0.0.0.0", 8082),
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
      gradeRepo = new GradeRepository[F]:
        def findByStudent(studentId: StudentId): F[List[Grade]] = Async[F].pure(List.empty)
        def findByStudentAndCourse(studentId: StudentId, courseId: CourseId): F[List[Grade]] = Async[F].pure(List.empty)

      assignmentRepo = new AssignmentRepository[F]:
        def findByCourse(courseId: CourseId): F[List[Assignment]] = Async[F].pure(List.empty)

      courseGradeRepo = new CourseGradeRepository[F]:
        def save(courseGrade: CourseGrade): F[CourseGrade] = Async[F].pure(courseGrade)
        def findByStudentAndSemester(studentId: StudentId, semester: String, year: Int): F[List[CourseGrade]] = Async[F].pure(List.empty)
        def findByStudent(studentId: StudentId): F[List[CourseGrade]] = Async[F].pure(List.empty)
        def findByCourse(courseId: CourseId): F[List[CourseGrade]] = Async[F].pure(List.empty)

      // Service
      calculationService = new GradeCalculationServiceImpl[F](gradeRepo, assignmentRepo, courseGradeRepo, eventPublisher)

      // Routes
      routes = new GradeCalculationRoutes[F](calculationService)
      httpApp = routes.routes(authMiddleware).orNotFound
      finalApp = Logger.httpApp(logHeaders = true, logBody = false)(httpApp)

      _ <- EmberServerBuilder
        .default[F]
        .withHost(Host.fromString(config.server.host).get)
        .withPort(Port.fromInt(config.server.port).get)
        .withHttpApp(finalApp)
        .build

      _ <- Resource.eval(Async[F].delay(
        println(s"Grade Calculation Service started on ${config.server.host}:${config.server.port}")
      ))

    yield ()


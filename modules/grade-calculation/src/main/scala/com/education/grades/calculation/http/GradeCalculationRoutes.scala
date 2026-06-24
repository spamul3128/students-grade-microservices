package com.education.grades.calculation.http

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.infrastructure.http.HttpController
import com.education.grades.common.auth.{AuthUser, UserRole}
import com.education.grades.common.domain.*
import com.education.grades.common.json.Codecs.given
import com.education.grades.calculation.service.GradeCalculationService
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{IntVar, UUIDVar}
import java.util.UUID
import io.circe.generic.auto.{*, given}

class GradeCalculationRoutes[F[_]: Concurrent](service: GradeCalculationService[F])
  extends HttpController[F]:

  import dsl.*

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok("Grade Calculation Service is healthy")
  }

  private val teacherRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Calculate course grade for a student
    case POST -> Root / "calculate" / "course" / UUIDVar(studentId) / UUIDVar(courseId) as user
      if isTeacherOrAdmin(user) =>
      for
        result <- service.calculateCourseGrade(StudentId(studentId), CourseId(courseId))
        response <- handleResult(result)
      yield response

    // Get class average
    case GET -> Root / "courses" / UUIDVar(courseId) / "average" as user
      if isTeacherOrAdmin(user) =>
      for
        result <- service.calculateClassAverage(CourseId(courseId))
        response <- handleResult(result)
      yield response
  }

  private val studentRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Calculate cumulative GPA
    case GET -> Root / "students" / UUIDVar(studentId) / "gpa" / "cumulative" as user =>
      if user.studentId.contains(StudentId(studentId)) || isAdmin(user) then
        for
          result <- service.calculateCumulativeGPA(StudentId(studentId))
          response <- handleResult(result)
        yield response
      else
        Forbidden("Access denied")

    // Calculate semester GPA
    case GET -> Root / "students" / UUIDVar(studentId) / "gpa" / semester / IntVar(year) as user =>
      if user.studentId.contains(StudentId(studentId)) || isAdmin(user) then
        for
          result <- service.calculateSemesterGPA(StudentId(studentId), semester, year)
          response <- handleResult(result)
        yield response
      else
        Forbidden("Access denied")
  }

  def routes(authMiddleware: AuthedRoutes[AuthUser, F] => HttpRoutes[F]): HttpRoutes[F] =
    publicRoutes <+> authMiddleware(teacherRoutes) <+> authMiddleware(studentRoutes)

  private def isTeacherOrAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Teacher) || user.roles.contains(UserRole.Admin)

  private def isAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Admin)

object UUIDVar:
  def unapply(str: String): Option[UUID] =
    try Some(UUID.fromString(str))
    catch case _: IllegalArgumentException => None

object IntVar:
  def unapply(str: String): Option[Int] =
    try Some(str.toInt)
    catch case _: NumberFormatException => None


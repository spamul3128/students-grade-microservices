package com.education.grades.ingestion.http

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.UUIDVar
import org.http4s.server.Router
import com.education.grades.common.infrastructure.http.HttpController
import com.education.grades.common.auth.{AuthUser, UserRole}
import com.education.grades.common.domain.*
import com.education.grades.ingestion.service.GradeIngestionService
import com.education.grades.ingestion.model.{SubmitGradeRequest, BulkGradeSubmission, StudentGrade, UpdateGradeRequest}
import com.education.grades.common.json.Codecs.given
import io.circe.generic.auto.{*, given}
import io.circe.{Decoder, Encoder}
import java.util.UUID



class GradeIngestionRoutes[F[_]: Concurrent](service: GradeIngestionService[F])
  extends HttpController[F]:

  import dsl.*

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Health check
    case GET -> Root / "health" =>
      Ok("Grade Ingestion Service is healthy")
  }

  private val teacherRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Submit a single grade
    case req @ POST -> Root / "grades" as user if isTeacherOrAdmin(user) =>
      for
        request <- req.req.as[SubmitGradeRequest]
        teacherId <- getTeacherId(user)
        result <- service.submitGrade(request, teacherId)
        response <- handleResult(result)
      yield response

    // Bulk submit grades
    case req @ POST -> Root / "grades" / "bulk" as user if isTeacherOrAdmin(user) =>
      for
        request <- req.req.as[BulkGradeSubmission]
        teacherId <- getTeacherId(user)
        result <- service.bulkSubmitGrades(request, teacherId)
        response <- handleResult(result)
      yield response

    // Update a grade
    case req @ PUT -> Root / "grades" / UUIDVar(gradeId) as user if isTeacherOrAdmin(user) =>
      for
        body <- req.req.as[UpdateGradeRequest]
        teacherId <- getTeacherId(user)
        result <- service.updateGrade(GradeId(gradeId), body.score, teacherId)
        response <- handleResult(result)
      yield response

    // Get grades for an assignment
    case GET -> Root / "assignments" / UUIDVar(assignmentId) / "grades" as user if isTeacherOrAdmin(user) =>
      for
        result <- service.getGradesByAssignment(AssignmentId(assignmentId))
        response <- handleResult(result)
      yield response
  }

  private val studentRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Get own grades
    case GET -> Root / "students" / UUIDVar(studentId) / "grades" as user =>
      if user.studentId.contains(StudentId(studentId)) || isAdmin(user) then
        for
          result <- service.getGradesByStudent(StudentId(studentId))
          response <- handleResult(result)
        yield response
      else
        Forbidden("You can only view your own grades")
  }

  def routes(authMiddleware: AuthedRoutes[AuthUser, F] => HttpRoutes[F]): HttpRoutes[F] =
    publicRoutes <+> authMiddleware(teacherRoutes) <+> authMiddleware(studentRoutes)

  private def isTeacherOrAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Teacher) || user.roles.contains(UserRole.Admin)

  private def isAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Admin)

  private def getTeacherId(user: AuthUser): F[TeacherId] =
    user.teacherId match
      case Some(tid) => Concurrent[F].pure(tid)
      case None => Concurrent[F].raiseError(new Exception("Teacher ID not found for user"))



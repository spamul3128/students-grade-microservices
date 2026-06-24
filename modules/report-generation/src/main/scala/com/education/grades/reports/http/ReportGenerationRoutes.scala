package com.education.grades.reports.http

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.infrastructure.http.HttpController
import com.education.grades.common.auth.{AuthUser, UserRole}
import com.education.grades.common.domain.*
import com.education.grades.common.json.Codecs.given
import com.education.grades.reports.service.ReportGenerationService
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{UUIDVar, IntVar}
import java.util.UUID
import io.circe.generic.auto.{*, given}

class ReportGenerationRoutes[F[_]: Concurrent](service: ReportGenerationService[F])
  extends HttpController[F]:

  import dsl.*

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok("Report Generation Service is healthy")
  }

  private val studentRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Generate report card
    case GET -> Root / "students" / UUIDVar(studentId) / "report-card" / semester / IntVar(year) as user =>
      if user.studentId.contains(StudentId(studentId)) || isTeacherOrAdmin(user) then
        for
          result <- service.generateReportCard(StudentId(studentId), semester, year)
          response <- handleResult(result)
        yield response
      else
        Forbidden("Access denied")

    // Generate transcript
    case GET -> Root / "students" / UUIDVar(studentId) / "transcript" as user =>
      if user.studentId.contains(StudentId(studentId)) || isTeacherOrAdmin(user) then
        for
          result <- service.generateTranscript(StudentId(studentId))
          response <- handleResult(result)
        yield response
      else
        Forbidden("Access denied")

    // Export report card as PDF
    case POST -> Root / "students" / UUIDVar(studentId) / "report-card" / semester / IntVar(year) / "pdf" as user =>
      if user.studentId.contains(StudentId(studentId)) || isTeacherOrAdmin(user) then
        for
          reportResult <- service.generateReportCard(StudentId(studentId), semester, year)
          response <- reportResult match
            case Right(reportCard) =>
              service.exportReportCardPDF(reportCard).flatMap {
                case Right(pdfBytes) =>
                  Ok(pdfBytes)
                    .map(_.withContentType(org.http4s.headers.`Content-Type`(
                      org.http4s.MediaType.application.pdf
                    )))
                case Left(error) =>
                  errorResponse(error)
              }
            case Left(error) =>
              errorResponse(error)
        yield response
      else
        Forbidden("Access denied")
  }

  def routes(authMiddleware: AuthedRoutes[AuthUser, F] => HttpRoutes[F]): HttpRoutes[F] =
    publicRoutes <+> authMiddleware(studentRoutes)

  private def isTeacherOrAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Teacher) || user.roles.contains(UserRole.Admin)

object UUIDVar:
  def unapply(str: String): Option[UUID] =
    try Some(UUID.fromString(str))
    catch case _: IllegalArgumentException => None

object IntVar:
  def unapply(str: String): Option[Int] =
    try Some(str.toInt)
    catch case _: NumberFormatException => None


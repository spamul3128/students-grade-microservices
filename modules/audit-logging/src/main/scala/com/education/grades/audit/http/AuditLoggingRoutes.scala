package com.education.grades.audit.http

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.infrastructure.http.HttpController
import com.education.grades.common.auth.{AuthUser, UserRole, UserId}
import com.education.grades.common.json.Codecs.given
import com.education.grades.audit.service.{AuditLoggingService, AuditLogId, AuditLog, AuditAction}
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import java.time.Instant
import java.util.UUID
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.{*, given}

// Codecs for audit types
given Decoder[AuditLogId] = Decoder[UUID].map(AuditLogId.apply)
given Encoder[AuditLogId] = Encoder[UUID].contramap(_.value)

given Decoder[AuditAction] = Decoder.decodeString.emap {
  case "Created" => Right(AuditAction.Created)
  case "Updated" => Right(AuditAction.Updated)
  case "Deleted" => Right(AuditAction.Deleted)
  case "Viewed" => Right(AuditAction.Viewed)
  case "Exported" => Right(AuditAction.Exported)
  case other => Left(s"Invalid audit action: $other")
}

given Encoder[AuditAction] = Encoder.encodeString.contramap {
  case AuditAction.Created => "Created"
  case AuditAction.Updated => "Updated"
  case AuditAction.Deleted => "Deleted"
  case AuditAction.Viewed => "Viewed"
  case AuditAction.Exported => "Exported"
}

// UUID QueryParamDecoder
given QueryParamDecoder[UUID] = QueryParamDecoder[String].emap { str =>
  try Right(UUID.fromString(str))
  catch case e: IllegalArgumentException => Left(ParseFailure(s"Invalid UUID: $str", e.getMessage))
}

class AuditLoggingRoutes[F[_]: Concurrent](service: AuditLoggingService[F])
  extends HttpController[F]:

  import dsl.*

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok("Audit Logging Service is healthy")
  }

  private val adminRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {

    // Get audit logs with filters
    case GET -> Root / "audit-logs" :?
      OptionalEntityTypeQueryParamMatcher(entityType) +&
      OptionalEntityIdQueryParamMatcher(entityId) +&
      OptionalUserIdQueryParamMatcher(userId) +&
      OptionalFromQueryParamMatcher(from) +&
      OptionalToQueryParamMatcher(to) +&
      OptionalLimitQueryParamMatcher(limit) as user if isAdmin(user) =>

      for
        result <- service.getAuditLogs(
          entityType,
          entityId,
          userId.map(UserId.apply),
          from.map(Instant.parse),
          to.map(Instant.parse),
          limit.getOrElse(100)
        )
        response <- handleResult(result)
      yield response
  }

  def routes(authMiddleware: AuthedRoutes[AuthUser, F] => HttpRoutes[F]): HttpRoutes[F] =
    publicRoutes <+> authMiddleware(adminRoutes)

  private def isAdmin(user: AuthUser): Boolean =
    user.roles.contains(UserRole.Admin)

// Query parameter matchers
object OptionalEntityTypeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("entityType")
object OptionalEntityIdQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("entityId")
object OptionalUserIdQueryParamMatcher extends OptionalQueryParamDecoderMatcher[UUID]("userId")
object OptionalFromQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("from")
object OptionalToQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("to")
object OptionalLimitQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("limit")


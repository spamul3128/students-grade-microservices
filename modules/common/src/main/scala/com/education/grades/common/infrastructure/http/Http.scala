package com.education.grades.common.infrastructure.http

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.*
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.Challenge
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import com.education.grades.common.errors.*
import com.education.grades.common.auth.{AuthUser, Permission}

// HTTP helpers and middleware
trait HttpController[F[_]: Concurrent]:
  val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

  // JSON encoders/decoders
  given [A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  given [A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  // Error response handling
  def errorResponse(error: AppError): F[Response[F]] =
    import dsl.*
    error match
      case _: ValidationError => BadRequest(error.message)
      case _: NotFoundError => NotFound(error.message)
      case _: UnauthorizedError =>
        Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "grades-api", Map("error" -> error.message))))
      case _: ForbiddenError => Forbidden(error.message)
      case _: DatabaseError => InternalServerError(error.message)
      case _: ExternalServiceError => BadGateway(error.message)
      case _: InternalError => InternalServerError(error.message)

  // Handle Either results
  def handleResult[A: Encoder](result: Either[AppError, A]): F[Response[F]] =
    import dsl.*
    result match
      case Right(value) => Ok(value.asJson)
      case Left(error) => errorResponse(error)

  // Handle Option results
  def handleOption[A: Encoder](opt: Option[A], notFoundMsg: String): F[Response[F]] =
    import dsl.*
    opt match
      case Some(value) => Ok(value.asJson)
      case None => NotFound(notFoundMsg)

// Authentication middleware
trait AuthMiddleware[F[_]]:
  def authenticate(request: Request[F]): F[Either[AppError, AuthUser]]
  def requireAuth(routes: AuthedRoutes[AuthUser, F]): HttpRoutes[F]
  def requirePermission(permission: Permission)(routes: AuthedRoutes[AuthUser, F]): HttpRoutes[F]

// CORS middleware
object CorsMiddleware:
  import org.http4s.server.middleware.CORS
  import cats.effect.kernel.Async

  def apply[F[_]: Async](routes: HttpRoutes[F]): HttpRoutes[F] =
    CORS.policy
      .withAllowOriginAll
      .withAllowMethodsAll
      .withAllowHeadersAll
      .withAllowCredentials(true)
      .apply(routes)


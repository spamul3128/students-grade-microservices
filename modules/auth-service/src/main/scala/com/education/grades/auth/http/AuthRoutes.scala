package com.education.grades.auth.http

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.infrastructure.http.HttpController
import com.education.grades.common.auth.{LoginRequest, UserRole}
import com.education.grades.auth.service.AuthService
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.Challenge
import com.education.grades.common.json.Codecs.given
import io.circe.generic.auto.{*, given}

class AuthRoutes[F[_]: Concurrent](service: AuthService[F])
  extends HttpController[F]:

  import dsl.*

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    // Health check
    case GET -> Root / "health" =>
      Ok("Auth Service is healthy")

    // Login
    case req @ POST -> Root / "login" =>
      for
        loginReq <- req.as[LoginRequest]
        result <- service.login(loginReq.username, loginReq.password)
        response <- handleResult(result)
      yield response

    // Register (simplified - in production would need admin auth)
    case req @ POST -> Root / "register" =>
      for
        regReq <- req.as[RegisterRequest]
        result <- service.register(
          regReq.username,
          regReq.email,
          regReq.password,
          regReq.role.getOrElse(UserRole.Student)
        )
        response <- handleResult(result)
      yield response

    // Validate token
    case req @ GET -> Root / "validate" =>
      req.headers.get[Authorization] match
        case Some(Authorization(credentials)) =>
          credentials match
            case Credentials.Token(AuthScheme.Bearer, token) =>
              service.validateToken(token).flatMap(handleResult)
            case _ =>
              Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "grades-api")))
        case None =>
          Unauthorized(`WWW-Authenticate`(Challenge("Bearer", "grades-api")))
  }

  def allRoutes: HttpRoutes[F] = routes

case class RegisterRequest(
  username: String,
  email: String,
  password: String,
  role: Option[UserRole]
)



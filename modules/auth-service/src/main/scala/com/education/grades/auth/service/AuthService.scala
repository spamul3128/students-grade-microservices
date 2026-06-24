package com.education.grades.auth.service

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.auth.*
import com.education.grades.common.errors.AppError
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import io.circe.syntax.*
import io.circe.parser.decode
import io.circe.generic.auto.{*, given}
import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

trait AuthService[F[_]]:
  def login(username: String, password: String): F[Either[AppError, LoginResponse]]
  def validateToken(token: String): F[Either[AppError, AuthUser]]
  def register(username: String, email: String, password: String, role: UserRole): F[Either[AppError, AuthUser]]

class AuthServiceImpl[F[_]: Sync](
  userRepository: UserRepository[F],
  jwtSecret: String,
  tokenExpiration: FiniteDuration
) extends AuthService[F]:

  import com.education.grades.common.json.Codecs.given

  private val passwordEncoder = new BCryptPasswordEncoder()

  override def login(username: String, password: String): F[Either[AppError, LoginResponse]] =
    (for
      user <- userRepository.findByUsername(username).flatMap {
        case Some(u) => Sync[F].pure(u)
        case None => Sync[F].raiseError(AppError.unauthorized("Invalid credentials"))
      }

      _ <- if passwordEncoder.matches(password, user.passwordHash) then
        Sync[F].unit
      else
        Sync[F].raiseError(AppError.unauthorized("Invalid credentials"))

      authUser = AuthUser(
        id = user.id,
        username = user.username,
        email = user.email,
        roles = user.roles,
        studentId = user.studentId,
        teacherId = user.teacherId
      )

      expiresAt = Instant.now().plusSeconds(tokenExpiration.toSeconds)

      claim = JwtClaim(
        content = authUser.asJson.noSpaces,
        expiration = Some(expiresAt.getEpochSecond),
        issuedAt = Some(Instant.now().getEpochSecond)
      )

      token = JwtCirce.encode(claim, jwtSecret, JwtAlgorithm.HS256)

      response = LoginResponse(
        token = token,
        expiresAt = expiresAt,
        user = authUser
      )

    yield response).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Login failed: ${e.getMessage}")
    })

  override def validateToken(token: String): F[Either[AppError, AuthUser]] =
    Sync[F].delay {
      JwtCirce.decode(token, jwtSecret, Seq(JwtAlgorithm.HS256)).toEither match
        case Right(claim) =>
          decode[AuthUser](claim.content) match
            case Right(user) => Right(user)
            case Left(err) => Left(AppError.unauthorized(s"Invalid token payload: ${err.getMessage}"))
        case Left(err) =>
          Left(AppError.unauthorized(s"Invalid token: ${err.getMessage}"))
    }

  override def register(
    username: String,
    email: String,
    password: String,
    role: UserRole
  ): F[Either[AppError, AuthUser]] =
    (for
      existing <- userRepository.findByUsername(username)

      _ <- existing match
        case Some(_) => Sync[F].raiseError(AppError.validation("Username already exists"))
        case None => Sync[F].unit

      passwordHash <- Sync[F].delay(passwordEncoder.encode(password))

      user = User(
        id = UserId(java.util.UUID.randomUUID()),
        username = username,
        email = email,
        passwordHash = passwordHash,
        roles = Set(role),
        studentId = None,
        teacherId = None
      )

      created <- userRepository.create(user)

      authUser = AuthUser(
        id = created.id,
        username = created.username,
        email = created.email,
        roles = created.roles,
        studentId = created.studentId,
        teacherId = created.teacherId
      )

    yield authUser).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Registration failed: ${e.getMessage}")
    })

case class User(
  id: UserId,
  username: String,
  email: String,
  passwordHash: String,
  roles: Set[UserRole],
  studentId: Option[com.education.grades.common.domain.StudentId],
  teacherId: Option[com.education.grades.common.domain.TeacherId]
)

trait UserRepository[F[_]]:
  def findByUsername(username: String): F[Option[User]]
  def findById(id: UserId): F[Option[User]]
  def create(user: User): F[User]



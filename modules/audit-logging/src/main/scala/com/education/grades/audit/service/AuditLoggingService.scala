package com.education.grades.audit.service

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.domain.*
import com.education.grades.common.auth.{UserId, AuthUser}
import com.education.grades.common.errors.AppError
import java.time.Instant
import java.util.UUID

case class AuditLogId(value: UUID) extends AnyVal

case class AuditLog(
  id: AuditLogId,
  userId: UserId,
  action: AuditAction,
  entityType: String,
  entityId: String,
  oldValue: Option[String],
  newValue: Option[String],
  timestamp: Instant,
  ipAddress: Option[String]
)

enum AuditAction:
  case Created
  case Updated
  case Deleted
  case Viewed
  case Exported

trait AuditLoggingService[F[_]]:
  def log(
    userId: UserId,
    action: AuditAction,
    entityType: String,
    entityId: String,
    oldValue: Option[String] = None,
    newValue: Option[String] = None,
    ipAddress: Option[String] = None
  ): F[Unit]

  def getAuditLogs(
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    userId: Option[UserId] = None,
    from: Option[Instant] = None,
    to: Option[Instant] = None,
    limit: Int = 100
  ): F[Either[AppError, List[AuditLog]]]

class AuditLoggingServiceImpl[F[_]: Async](
  repository: AuditLogRepository[F]
) extends AuditLoggingService[F]:

  override def log(
    userId: UserId,
    action: AuditAction,
    entityType: String,
    entityId: String,
    oldValue: Option[String] = None,
    newValue: Option[String] = None,
    ipAddress: Option[String] = None
  ): F[Unit] =
    val auditLog = AuditLog(
      id = AuditLogId(UUID.randomUUID()),
      userId = userId,
      action = action,
      entityType = entityType,
      entityId = entityId,
      oldValue = oldValue,
      newValue = newValue,
      timestamp = Instant.now(),
      ipAddress = ipAddress
    )

    repository.create(auditLog).void

  override def getAuditLogs(
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    userId: Option[UserId] = None,
    from: Option[Instant] = None,
    to: Option[Instant] = None,
    limit: Int = 100
  ): F[Either[AppError, List[AuditLog]]] =
    repository.findFiltered(entityType, entityId, userId, from, to, limit).attempt.map(_.leftMap { e =>
      AppError.database(s"Failed to fetch audit logs: ${e.getMessage}")
    })

trait AuditLogRepository[F[_]]:
  def create(log: AuditLog): F[AuditLog]
  def findFiltered(
    entityType: Option[String],
    entityId: Option[String],
    userId: Option[UserId],
    from: Option[Instant],
    to: Option[Instant],
    limit: Int
  ): F[List[AuditLog]]


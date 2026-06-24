package com.education.grades.common.auth

import java.util.UUID
import java.time.Instant
import com.education.grades.common.domain.{StudentId, TeacherId}

case class UserId(value: UUID) extends AnyVal

enum UserRole:
  case Student
  case Teacher
  case Admin

case class AuthUser(
  id: UserId,
  username: String,
  email: String,
  roles: Set[UserRole],
  studentId: Option[StudentId],
  teacherId: Option[TeacherId]
)


case class AuthToken(
  token: String,
  userId: UserId,
  expiresAt: Instant
)

case class LoginRequest(
  username: String,
  password: String
)

case class LoginResponse(
  token: String,
  expiresAt: Instant,
  user: AuthUser
)

// Permission-based access control
trait Permission

object Permission:
  case object ViewOwnGrades extends Permission
  case object ViewAllGrades extends Permission
  case object SubmitGrades extends Permission
  case object ModifyGrades extends Permission
  case object GenerateReports extends Permission
  case object ViewAuditLogs extends Permission
  case object ManageUsers extends Permission

  def forRole(role: UserRole): Set[Permission] = role match
    case UserRole.Student => Set(ViewOwnGrades)
    case UserRole.Teacher => Set(
      ViewAllGrades,
      SubmitGrades,
      ModifyGrades,
      GenerateReports
    )
    case UserRole.Admin => Set(
      ViewAllGrades,
      SubmitGrades,
      ModifyGrades,
      GenerateReports,
      ViewAuditLogs,
      ManageUsers
    )


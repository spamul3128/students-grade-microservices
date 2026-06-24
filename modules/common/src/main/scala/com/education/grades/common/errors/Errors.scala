package com.education.grades.common.errors

// Domain errors
sealed trait AppError extends Throwable:
  def message: String
  override def getMessage: String = message

case class ValidationError(message: String) extends AppError
case class NotFoundError(message: String) extends AppError
case class UnauthorizedError(message: String) extends AppError
case class ForbiddenError(message: String) extends AppError
case class DatabaseError(message: String, cause: Option[Throwable] = None) extends AppError
case class ExternalServiceError(message: String) extends AppError
case class InternalError(message: String) extends AppError

object AppError:
  def validation(msg: String): AppError = ValidationError(msg)
  def notFound(msg: String): AppError = NotFoundError(msg)
  def unauthorized(msg: String): AppError = UnauthorizedError(msg)
  def forbidden(msg: String): AppError = ForbiddenError(msg)
  def database(msg: String, cause: Option[Throwable] = None): AppError = DatabaseError(msg, cause)
  def external(msg: String): AppError = ExternalServiceError(msg)
  def internal(msg: String): AppError = InternalError(msg)


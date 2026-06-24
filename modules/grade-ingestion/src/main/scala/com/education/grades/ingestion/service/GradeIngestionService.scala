package com.education.grades.ingestion.service

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.domain.*
import com.education.grades.common.errors.AppError
import com.education.grades.ingestion.repository.GradeRepository
import com.education.grades.common.infrastructure.messaging.EventPublisher
import com.education.grades.ingestion.model.{SubmitGradeRequest, BulkGradeSubmission, StudentGrade}
import java.util.UUID
import java.time.Instant


trait GradeIngestionService[F[_]]:
  def submitGrade(request: SubmitGradeRequest, teacherId: TeacherId): F[Either[AppError, Grade]]
  def bulkSubmitGrades(request: BulkGradeSubmission, teacherId: TeacherId): F[Either[AppError, List[Grade]]]
  def updateGrade(gradeId: GradeId, newScore: Double, teacherId: TeacherId): F[Either[AppError, Grade]]
  def getGradesByStudent(studentId: StudentId): F[Either[AppError, List[Grade]]]
  def getGradesByAssignment(assignmentId: AssignmentId): F[Either[AppError, List[Grade]]]

class GradeIngestionServiceImpl[F[_]: Async](
  repository: GradeRepository[F],
  eventPublisher: EventPublisher[F],
  assignmentRepository: AssignmentRepository[F]
) extends GradeIngestionService[F]:

  import com.education.grades.common.json.Codecs.given

  override def submitGrade(request: SubmitGradeRequest, teacherId: TeacherId): F[Either[AppError, Grade]] =
    (for
      assignment <- assignmentRepository.findById(request.assignmentId).flatMap {
        case Some(a) => Async[F].pure(a)
        case None => Async[F].raiseError(AppError.notFound(s"Assignment ${request.assignmentId} not found"))
      }

      _ <- validateScore(request.score, assignment.maxScore)

      grade = Grade(
        id = GradeId(UUID.randomUUID()),
        studentId = request.studentId,
        assignmentId = request.assignmentId,
        score = request.score,
        submittedAt = Instant.now(),
        gradedAt = Some(Instant.now()),
        gradedBy = Some(teacherId),
        comments = request.comments
      )

      created <- repository.create(grade)

      // Publish event
      event = DomainEvent.GradeSubmitted(
        created.id,
        created.studentId,
        created.assignmentId,
        assignment.courseId,
        created.score,
        created.submittedAt
      )
      _ <- eventPublisher.publishEvent("grades.submitted", event)

    yield created).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to submit grade: ${e.getMessage}")
    })

  private def getAssignment(assignmentId: AssignmentId): F[Assignment] =
    assignmentRepository.findById(assignmentId).flatMap {
      case Some(a) => Async[F].pure(a)
      case None => Async[F].raiseError(AppError.notFound(s"Assignment $assignmentId not found"))
    }

  private def validateScore(score: Double, maxScore: Double): F[Unit] =
    if score < 0 || score > maxScore then
      Async[F].raiseError(AppError.validation(s"Score $score is invalid. Must be between 0 and $maxScore"))
    else
      Async[F].unit

  override def bulkSubmitGrades(
    request: BulkGradeSubmission,
    teacherId: TeacherId
  ): F[Either[AppError, List[Grade]]] =
    (for
      assignment <- getAssignment(request.assignmentId)

      grades <- request.grades.traverse { sg =>
        validateScore(sg.score, assignment.maxScore).as {
          Grade(
            id = GradeId(UUID.randomUUID()),
            studentId = sg.studentId,
            assignmentId = request.assignmentId,
            score = sg.score,
            submittedAt = Instant.now(),
            gradedAt = Some(Instant.now()),
            gradedBy = Some(teacherId),
            comments = sg.comments
          )
        }
      }

      created <- repository.bulkCreate(grades)

      // Publish bulk upload event
      event = DomainEvent.BulkGradesUploaded(
        UUID.randomUUID(),
        teacherId,
        assignment.courseId,
        created.size,
        Instant.now()
      )
      _ <- eventPublisher.publishEvent("grades.bulk_uploaded", event)

    yield created).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to bulk submit grades: ${e.getMessage}")
    })

  override def updateGrade(gradeId: GradeId, newScore: Double, teacherId: TeacherId): F[Either[AppError, Grade]] =
    (for
      existing <- repository.findById(gradeId).flatMap {
        case Some(g) => Async[F].pure(g)
        case None => Async[F].raiseError(AppError.notFound(s"Grade $gradeId not found"))
      }

      assignment <- getAssignment(existing.assignmentId)
      _ <- validateScore(newScore, assignment.maxScore)

      updated = existing.copy(
        score = newScore,
        gradedAt = Some(Instant.now()),
        gradedBy = Some(teacherId)
      )

      result <- repository.update(updated).flatMap {
        case Some(g) => Async[F].pure(g)
        case None => Async[F].raiseError(AppError.internal("Failed to update grade"))
      }

      // Publish update event
      event = DomainEvent.GradeUpdated(
        gradeId,
        existing.score,
        newScore,
        teacherId,
        Instant.now()
      )
      _ <- eventPublisher.publishEvent("grades.updated", event)

    yield result).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to update grade: ${e.getMessage}")
    })

  override def getGradesByStudent(studentId: StudentId): F[Either[AppError, List[Grade]]] =
    repository.findByStudent(studentId).attempt.map(_.leftMap { e =>
      AppError.database(s"Failed to fetch grades for student: ${e.getMessage}")
    })

  override def getGradesByAssignment(assignmentId: AssignmentId): F[Either[AppError, List[Grade]]] =
    repository.findByAssignment(assignmentId).attempt.map(_.leftMap { e =>
      AppError.database(s"Failed to fetch grades for assignment: ${e.getMessage}")
    })

// Placeholder for assignment repository
trait AssignmentRepository[F[_]]:
  def findById(id: AssignmentId): F[Option[Assignment]]




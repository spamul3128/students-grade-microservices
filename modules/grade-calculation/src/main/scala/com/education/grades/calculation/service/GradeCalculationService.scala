package com.education.grades.calculation.service

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.domain.*
import com.education.grades.common.errors.AppError
import com.education.grades.common.infrastructure.messaging.EventPublisher

trait GradeCalculationService[F[_]]:
  def calculateCourseGrade(studentId: StudentId, courseId: CourseId): F[Either[AppError, CourseGrade]]
  def calculateSemesterGPA(studentId: StudentId, semester: String, year: Int): F[Either[AppError, Double]]
  def calculateCumulativeGPA(studentId: StudentId): F[Either[AppError, Double]]
  def calculateClassAverage(courseId: CourseId): F[Either[AppError, Double]]

class GradeCalculationServiceImpl[F[_]: Async](
  gradeRepository: GradeRepository[F],
  assignmentRepository: AssignmentRepository[F],
  courseGradeRepository: CourseGradeRepository[F],
  eventPublisher: EventPublisher[F]
) extends GradeCalculationService[F]:

  import com.education.grades.common.json.Codecs.given
  import java.time.Instant

  override def calculateCourseGrade(studentId: StudentId, courseId: CourseId): F[Either[AppError, CourseGrade]] =
    (for
      // Get all assignments for the course
      assignments <- assignmentRepository.findByCourse(courseId)

      _ <- if assignments.isEmpty then
        Async[F].raiseError(AppError.notFound(s"No assignments found for course $courseId"))
      else
        Async[F].unit

      // Get all grades for the student in this course
      grades <- gradeRepository.findByStudentAndCourse(studentId, courseId)

      // Calculate weighted average
      numericGrade <- calculateWeightedGrade(assignments, grades)

      // Convert to letter grade
      letterGrade = LetterGrade.fromNumericGrade(numericGrade)

      // Create course grade record
      courseGrade = CourseGrade(
        studentId = studentId,
        courseId = courseId,
        numericGrade = numericGrade,
        letterGrade = letterGrade,
        gpa = letterGrade.gpa,
        semester = "Fall", // TODO: Get from course/current date
        academicYear = 2024  // TODO: Get from course/current date
      )

      // Save to database
      saved <- courseGradeRepository.save(courseGrade)

      // Publish event
      event = DomainEvent.CourseGradeCalculated(
        studentId,
        courseId,
        letterGrade,
        numericGrade,
        Instant.now()
      )
      _ <- eventPublisher.publishEvent("course_grades.calculated", event)

    yield saved).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to calculate course grade: ${e.getMessage}")
    })

  private def calculateWeightedGrade(assignments: List[Assignment], grades: List[Grade]): F[Double] =
    Async[F].delay {
      val gradeMap = grades.map(g => g.assignmentId -> g).toMap

      val totalWeight = assignments.map(_.weight).sum

      if totalWeight == 0.0 then
        throw AppError.validation("Total assignment weight cannot be zero")

      val weightedSum = assignments.foldLeft(0.0) { (acc, assignment) =>
        gradeMap.get(assignment.id) match
          case Some(grade) =>
            val percentage = (grade.score / assignment.maxScore) * 100
            acc + (percentage * assignment.weight)
          case None =>
            acc // Missing grades count as 0
      }

      weightedSum / totalWeight
    }

  override def calculateSemesterGPA(studentId: StudentId, semester: String, year: Int): F[Either[AppError, Double]] =
    (for
      courseGrades <- courseGradeRepository.findByStudentAndSemester(studentId, semester, year)

      gpa <- if courseGrades.isEmpty then
        Async[F].raiseError(AppError.notFound(s"No course grades found for student in $semester $year"))
      else
        Async[F].pure {
          val totalGPA = courseGrades.map(_.gpa).sum
          totalGPA / courseGrades.size
        }

    yield gpa).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to calculate semester GPA: ${e.getMessage}")
    })

  override def calculateCumulativeGPA(studentId: StudentId): F[Either[AppError, Double]] =
    (for
      allCourseGrades <- courseGradeRepository.findByStudent(studentId)

      gpa <- if allCourseGrades.isEmpty then
        Async[F].raiseError(AppError.notFound(s"No course grades found for student"))
      else
        Async[F].pure {
          val totalGPA = allCourseGrades.map(_.gpa).sum
          totalGPA / allCourseGrades.size
        }

    yield gpa).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to calculate cumulative GPA: ${e.getMessage}")
    })

  override def calculateClassAverage(courseId: CourseId): F[Either[AppError, Double]] =
    (for
      courseGrades <- courseGradeRepository.findByCourse(courseId)

      average <- if courseGrades.isEmpty then
        Async[F].raiseError(AppError.notFound(s"No grades found for course"))
      else
        Async[F].pure {
          val total = courseGrades.map(_.numericGrade).sum
          total / courseGrades.size
        }

    yield average).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to calculate class average: ${e.getMessage}")
    })

// Repository traits
trait GradeRepository[F[_]]:
  def findByStudentAndCourse(studentId: StudentId, courseId: CourseId): F[List[Grade]]

trait AssignmentRepository[F[_]]:
  def findByCourse(courseId: CourseId): F[List[Assignment]]

trait CourseGradeRepository[F[_]]:
  def save(courseGrade: CourseGrade): F[CourseGrade]
  def findByStudent(studentId: StudentId): F[List[CourseGrade]]
  def findByStudentAndSemester(studentId: StudentId, semester: String, year: Int): F[List[CourseGrade]]
  def findByCourse(courseId: CourseId): F[List[CourseGrade]]


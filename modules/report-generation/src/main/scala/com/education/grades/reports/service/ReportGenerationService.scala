package com.education.grades.reports.service

import cats.effect.*
import cats.syntax.all.*
import com.education.grades.common.domain.*
import com.education.grades.common.errors.AppError
import com.education.grades.common.infrastructure.messaging.EventPublisher
import java.time.Instant

trait ReportGenerationService[F[_]]:
  def generateReportCard(studentId: StudentId, semester: String, year: Int): F[Either[AppError, ReportCard]]
  def generateTranscript(studentId: StudentId): F[Either[AppError, Transcript]]
  def exportReportCardPDF(reportCard: ReportCard): F[Either[AppError, Array[Byte]]]

class ReportGenerationServiceImpl[F[_]: Async](
  studentRepository: StudentRepository[F],
  courseGradeRepository: CourseGradeRepository[F],
  courseRepository: CourseRepository[F],
  teacherRepository: TeacherRepository[F],
  eventPublisher: EventPublisher[F]
) extends ReportGenerationService[F]:

  import com.education.grades.common.json.Codecs.given

  override def generateReportCard(
    studentId: StudentId,
    semester: String,
    year: Int
  ): F[Either[AppError, ReportCard]] =
    (for
      student <- studentRepository.findById(studentId).flatMap {
        case Some(s) => Async[F].pure(s)
        case None => Async[F].raiseError(AppError.notFound(s"Student $studentId not found"))
      }

      courseGrades <- courseGradeRepository.findByStudentAndSemester(studentId, semester, year)

      _ <- if courseGrades.isEmpty then
        Async[F].raiseError(AppError.notFound(s"No grades found for $semester $year"))
      else
        Async[F].unit

      // Fetch course and teacher details
      courseDetails <- courseGrades.traverse { cg =>
        for
          course <- courseRepository.findById(cg.courseId).flatMap {
            case Some(c) => Async[F].pure(c)
            case None => Async[F].raiseError(AppError.notFound(s"Course ${cg.courseId} not found"))
          }
          teacher <- teacherRepository.findById(course.teacherId).flatMap {
            case Some(t) => Async[F].pure(t)
            case None => Async[F].raiseError(AppError.notFound(s"Teacher ${course.teacherId} not found"))
          }
        yield CourseGradeDetails(
          courseName = course.name,
          courseCode = course.code,
          credits = course.credits,
          letterGrade = cg.letterGrade,
          numericGrade = cg.numericGrade,
          teacherName = s"${teacher.firstName} ${teacher.lastName}"
        )
      }

      // Calculate semester GPA
      semesterGPA = courseGrades.map(_.gpa).sum / courseGrades.size

      // Calculate cumulative GPA
      allCourseGrades <- courseGradeRepository.findByStudent(studentId)
      cumulativeGPA = if allCourseGrades.nonEmpty then
        allCourseGrades.map(_.gpa).sum / allCourseGrades.size
      else
        semesterGPA

      reportCard = ReportCard(
        studentId = studentId,
        semester = semester,
        academicYear = year,
        courses = courseDetails,
        semesterGPA = semesterGPA,
        cumulativeGPA = cumulativeGPA,
        generatedAt = Instant.now()
      )

      // Publish event
      event = DomainEvent.ReportCardGenerated(
        studentId,
        semester,
        year,
        Instant.now()
      )
      _ <- eventPublisher.publishEvent("reports.generated", event)

    yield reportCard).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to generate report card: ${e.getMessage}")
    })

  override def generateTranscript(studentId: StudentId): F[Either[AppError, Transcript]] =
    (for
      student <- studentRepository.findById(studentId).flatMap {
        case Some(s) => Async[F].pure(s)
        case None => Async[F].raiseError(AppError.notFound(s"Student $studentId not found"))
      }

      // Get all course grades
      allCourseGrades <- courseGradeRepository.findByStudent(studentId)

      _ <- if allCourseGrades.isEmpty then
        Async[F].raiseError(AppError.notFound("No grades found for student"))
      else
        Async[F].unit

      // Group by semester and year
      groupedGrades = allCourseGrades.groupBy(cg => (cg.semester, cg.academicYear))

      // Generate report card for each semester
      reportCards <- groupedGrades.toList.traverse { case ((semester, year), grades) =>
        generateReportCard(studentId, semester, year).flatMap {
          case Right(rc) => Async[F].pure(rc)
          case Left(err) => Async[F].raiseError(err)
        }
      }

      // Calculate cumulative stats
      cumulativeGPA = allCourseGrades.map(_.gpa).sum / allCourseGrades.size

      totalCredits <- reportCards.traverse { rc =>
        Async[F].pure(rc.courses.map(_.credits).sum)
      }.map(_.sum)

      transcript = Transcript(
        studentId = studentId,
        studentName = s"${student.firstName} ${student.lastName}",
        allReportCards = reportCards.sortBy(rc => (rc.academicYear, rc.semester)),
        cumulativeGPA = cumulativeGPA,
        totalCredits = totalCredits,
        generatedAt = Instant.now()
      )

    yield transcript).attempt.map(_.leftMap {
      case e: AppError => e
      case e => AppError.internal(s"Failed to generate transcript: ${e.getMessage}")
    })

  override def exportReportCardPDF(reportCard: ReportCard): F[Either[AppError, Array[Byte]]] =
    // TODO: Implement PDF generation (could use a library like Apache PDFBox or iText)
    Async[F].delay {
      Left(AppError.internal("PDF export not yet implemented"))
    }

// Repository traits
trait StudentRepository[F[_]]:
  def findById(id: StudentId): F[Option[Student]]

trait CourseGradeRepository[F[_]]:
  def findByStudentAndSemester(studentId: StudentId, semester: String, year: Int): F[List[CourseGrade]]
  def findByStudent(studentId: StudentId): F[List[CourseGrade]]

trait CourseRepository[F[_]]:
  def findById(id: CourseId): F[Option[Course]]

trait TeacherRepository[F[_]]:
  def findById(id: TeacherId): F[Option[Teacher]]


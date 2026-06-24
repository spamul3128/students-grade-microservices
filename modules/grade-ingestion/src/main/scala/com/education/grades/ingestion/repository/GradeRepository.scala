package com.education.grades.ingestion.repository

import cats.effect.*
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import com.education.grades.common.domain.*
import com.education.grades.common.infrastructure.db.Repository
import com.education.grades.common.errors.AppError
import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset}

trait GradeRepository[F[_]] extends Repository[F, GradeId, Grade]:
  def findByStudent(studentId: StudentId): F[List[Grade]]
  def findByAssignment(assignmentId: AssignmentId): F[List[Grade]]
  def findByCourse(courseId: CourseId): F[List[Grade]]
  def bulkCreate(grades: List[Grade]): F[List[Grade]]

class PostgresGradeRepository[F[_]: Async](sessionPool: Resource[F, Session[F]])
  extends GradeRepository[F]:

  // Custom Instant codec
  private val instantCodec: Codec[Instant] =
    timestamp.imap(ldt => ldt.toInstant(ZoneOffset.UTC))(inst => LocalDateTime.ofInstant(inst, ZoneOffset.UTC))

  // Codecs
  private val gradeIdCodec: Codec[GradeId] = uuid.imap(GradeId.apply)(_.value)
  private val studentIdCodec: Codec[StudentId] = uuid.imap(StudentId.apply)(_.value)
  private val assignmentIdCodec: Codec[AssignmentId] = uuid.imap(AssignmentId.apply)(_.value)
  private val teacherIdCodec: Codec[TeacherId] = uuid.imap(TeacherId.apply)(_.value)
  private val courseIdCodec: Codec[CourseId] = uuid.imap(CourseId.apply)(_.value)

  private val gradeCodec: Codec[Grade] =
    (gradeIdCodec *: studentIdCodec *: assignmentIdCodec *: float8 *:
     instantCodec *: instantCodec.opt *: teacherIdCodec.opt *: text.opt).imap {
      case (id, studentId, assignmentId, score, submittedAt, gradedAt, gradedBy, comments) =>
        Grade(id, studentId, assignmentId, score, submittedAt, gradedAt, gradedBy, comments)
    } { grade =>
      (grade.id, grade.studentId, grade.assignmentId, grade.score,
       grade.submittedAt, grade.gradedAt, grade.gradedBy, grade.comments)
    }

  override def findById(id: GradeId): F[Option[Grade]] =
    sessionPool.use { session =>
      val query: Query[GradeId, Grade] =
        sql"""
          SELECT id, student_id, assignment_id, score, submitted_at,
                 graded_at, graded_by, comments
          FROM grades
          WHERE id = $gradeIdCodec
        """.query(gradeCodec)

      session.prepare(query).flatMap(_.option(id))
    }

  override def findAll: F[List[Grade]] =
    sessionPool.use { session =>
      val query: Query[skunk.Void, Grade] =
        sql"""
          SELECT id, student_id, assignment_id, score, submitted_at,
                 graded_at, graded_by, comments
          FROM grades
        """.query(gradeCodec)

      session.execute(query)
    }

  override def create(grade: Grade): F[Grade] =
    sessionPool.use { session =>
      val command: Command[Grade] =
        sql"""
          INSERT INTO grades (id, student_id, assignment_id, score, submitted_at,
                             graded_at, graded_by, comments)
          VALUES ($gradeCodec)
        """.command

      session.prepare(command).flatMap { ps =>
        ps.execute(grade).as(grade)
      }
    }

  override def update(grade: Grade): F[Option[Grade]] =
    sessionPool.use { session =>
      val command: Command[StudentId *: AssignmentId *: Double *: Instant *:
                           Option[Instant] *: Option[TeacherId] *: Option[String] *: GradeId *: EmptyTuple] =
        sql"""
          UPDATE grades
          SET student_id = $studentIdCodec, assignment_id = $assignmentIdCodec,
              score = $float8, submitted_at = $instantCodec,
              graded_at = ${instantCodec.opt}, graded_by = ${teacherIdCodec.opt},
              comments = ${text.opt}
          WHERE id = $gradeIdCodec
        """.command

      session.prepare(command).flatMap { ps =>
        ps.execute(grade.studentId *: grade.assignmentId *: grade.score *: grade.submittedAt *:
                   grade.gradedAt *: grade.gradedBy *: grade.comments *: grade.id *: EmptyTuple).map {
          case skunk.data.Completion.Update(1) => Some(grade)
          case _ => None
        }
      }
    }

  override def delete(id: GradeId): F[Boolean] =
    sessionPool.use { session =>
      val command: Command[GradeId] =
        sql"DELETE FROM grades WHERE id = $gradeIdCodec".command

      session.prepare(command).flatMap { ps =>
        ps.execute(id).map {
          case skunk.data.Completion.Delete(1) => true
          case _ => false
        }
      }
    }

  override def findByStudent(studentId: StudentId): F[List[Grade]] =
    sessionPool.use { session =>
      val query: Query[StudentId, Grade] =
        sql"""
          SELECT id, student_id, assignment_id, score, submitted_at,
                 graded_at, graded_by, comments
          FROM grades
          WHERE student_id = $studentIdCodec
          ORDER BY submitted_at DESC
        """.query(gradeCodec)

      session.prepare(query).flatMap(_.stream(studentId, 1024).compile.toList)
    }

  override def findByAssignment(assignmentId: AssignmentId): F[List[Grade]] =
    sessionPool.use { session =>
      val query: Query[AssignmentId, Grade] =
        sql"""
          SELECT id, student_id, assignment_id, score, submitted_at,
                 graded_at, graded_by, comments
          FROM grades
          WHERE assignment_id = $assignmentIdCodec
          ORDER BY submitted_at DESC
        """.query(gradeCodec)

      session.prepare(query).flatMap(_.stream(assignmentId, 1024).compile.toList)
    }

  override def findByCourse(courseId: CourseId): F[List[Grade]] =
    sessionPool.use { session =>
      val query: Query[CourseId, Grade] =
        sql"""
          SELECT g.id, g.student_id, g.assignment_id, g.score, g.submitted_at,
                 g.graded_at, g.graded_by, g.comments
          FROM grades g
          JOIN assignments a ON g.assignment_id = a.id
          WHERE a.course_id = $courseIdCodec
          ORDER BY g.submitted_at DESC
        """.query(gradeCodec)

      session.prepare(query).flatMap(_.stream(courseId, 1024).compile.toList)
    }

  override def bulkCreate(grades: List[Grade]): F[List[Grade]] =
    sessionPool.use { session =>
      val command: Command[Grade] =
        sql"""
          INSERT INTO grades (id, student_id, assignment_id, score, submitted_at,
                             graded_at, graded_by, comments)
          VALUES ($gradeCodec)
        """.command

      session.prepare(command).flatMap { ps =>
        grades.traverse(grade => ps.execute(grade).as(grade))
      }
    }


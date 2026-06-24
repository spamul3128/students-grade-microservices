package com.education.grades.common.domain

import java.time.Instant
import java.util.UUID

// Core Domain Models

case class StudentId(value: UUID) extends AnyVal
case class TeacherId(value: UUID) extends AnyVal
case class CourseId(value: UUID) extends AnyVal
case class AssignmentId(value: UUID) extends AnyVal
case class GradeId(value: UUID) extends AnyVal

case class Student(
  id: StudentId,
  firstName: String,
  lastName: String,
  email: String,
  enrollmentDate: Instant
)

case class Teacher(
  id: TeacherId,
  firstName: String,
  lastName: String,
  email: String,
  department: String
)

case class Course(
  id: CourseId,
  name: String,
  code: String,
  credits: Int,
  teacherId: TeacherId
)

case class Assignment(
  id: AssignmentId,
  courseId: CourseId,
  name: String,
  maxScore: Double,
  weight: Double, // Weight in final grade (0.0 to 1.0)
  dueDate: Instant,
  assignmentType: AssignmentType
)

enum AssignmentType:
  case Homework
  case Quiz
  case Exam
  case Project
  case Participation

case class Grade(
  id: GradeId,
  studentId: StudentId,
  assignmentId: AssignmentId,
  score: Double,
  submittedAt: Instant,
  gradedAt: Option[Instant],
  gradedBy: Option[TeacherId],
  comments: Option[String]
)

case class CourseGrade(
  studentId: StudentId,
  courseId: CourseId,
  numericGrade: Double,
  letterGrade: LetterGrade,
  gpa: Double,
  semester: String,
  academicYear: Int
)

enum LetterGrade(val gpa: Double):
  case APlus extends LetterGrade(4.0)
  case A extends LetterGrade(4.0)
  case AMinus extends LetterGrade(3.7)
  case BPlus extends LetterGrade(3.3)
  case B extends LetterGrade(3.0)
  case BMinus extends LetterGrade(2.7)
  case CPlus extends LetterGrade(2.3)
  case C extends LetterGrade(2.0)
  case CMinus extends LetterGrade(1.7)
  case DPlus extends LetterGrade(1.3)
  case D extends LetterGrade(1.0)
  case F extends LetterGrade(0.0)

object LetterGrade:
  def fromNumericGrade(score: Double): LetterGrade = score match
    case s if s >= 97 => APlus
    case s if s >= 93 => A
    case s if s >= 90 => AMinus
    case s if s >= 87 => BPlus
    case s if s >= 83 => B
    case s if s >= 80 => BMinus
    case s if s >= 77 => CPlus
    case s if s >= 73 => C
    case s if s >= 70 => CMinus
    case s if s >= 67 => DPlus
    case s if s >= 60 => D
    case _ => F

case class ReportCard(
  studentId: StudentId,
  semester: String,
  academicYear: Int,
  courses: List[CourseGradeDetails],
  semesterGPA: Double,
  cumulativeGPA: Double,
  generatedAt: Instant
)

case class CourseGradeDetails(
  courseName: String,
  courseCode: String,
  credits: Int,
  letterGrade: LetterGrade,
  numericGrade: Double,
  teacherName: String
)

case class Transcript(
  studentId: StudentId,
  studentName: String,
  allReportCards: List[ReportCard],
  cumulativeGPA: Double,
  totalCredits: Int,
  generatedAt: Instant
)


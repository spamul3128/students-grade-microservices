package com.education.grades.common.domain

import java.time.Instant
import java.util.UUID

// Events for inter-service communication

enum DomainEvent:
  case GradeSubmitted(
    gradeId: GradeId,
    studentId: StudentId,
    assignmentId: AssignmentId,
    courseId: CourseId,
    score: Double,
    submittedAt: Instant
  )

  case GradeUpdated(
    gradeId: GradeId,
    oldScore: Double,
    newScore: Double,
    updatedBy: TeacherId,
    updatedAt: Instant
  )

  case CourseGradeCalculated(
    studentId: StudentId,
    courseId: CourseId,
    letterGrade: LetterGrade,
    numericGrade: Double,
    calculatedAt: Instant
  )

  case ReportCardGenerated(
    studentId: StudentId,
    semester: String,
    academicYear: Int,
    generatedAt: Instant
  )

  case BulkGradesUploaded(
    uploadId: UUID,
    teacherId: TeacherId,
    courseId: CourseId,
    numberOfGrades: Int,
    uploadedAt: Instant
  )

object DomainEvent:
  def eventType(event: DomainEvent): String = event match
    case _: GradeSubmitted => "grade.submitted"
    case _: GradeUpdated => "grade.updated"
    case _: CourseGradeCalculated => "course_grade.calculated"
    case _: ReportCardGenerated => "report_card.generated"
    case _: BulkGradesUploaded => "grades.bulk_uploaded"


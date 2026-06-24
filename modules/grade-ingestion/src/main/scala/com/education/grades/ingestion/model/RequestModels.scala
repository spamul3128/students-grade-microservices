package com.education.grades.ingestion.model

import com.education.grades.common.domain.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.{*, given}

// Request models - using auto derivation for Scala 3 compatibility
case class SubmitGradeRequest(
  studentId: StudentId,
  assignmentId: AssignmentId,
  score: Double,
  comments: Option[String]
)

case class StudentGrade(
  studentId: StudentId,
  score: Double,
  comments: Option[String]
)

case class BulkGradeSubmission(
  assignmentId: AssignmentId,
  grades: List[StudentGrade]
)

case class UpdateGradeRequest(score: Double)


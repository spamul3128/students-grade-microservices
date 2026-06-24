package com.education.grades.common.json

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import java.time.Instant
import java.util.UUID
import com.education.grades.common.domain.*
import com.education.grades.common.auth.*

// JSON codecs for domain models
object Codecs:

  // UUID codec
  given Decoder[UUID] = Decoder.decodeString.emap { str =>
    try Right(UUID.fromString(str))
    catch case e: IllegalArgumentException => Left(s"Invalid UUID: $str")
  }

  given Encoder[UUID] = Encoder.encodeString.contramap(_.toString)

  // Instant codec
  given Decoder[Instant] = Decoder.decodeString.emap { str =>
    try Right(Instant.parse(str))
    catch case e: Exception => Left(s"Invalid timestamp: $str")
  }

  given Encoder[Instant] = Encoder.encodeString.contramap(_.toString)

  // Value classes
  given Decoder[StudentId] = Decoder[UUID].map(StudentId.apply)
  given Encoder[StudentId] = Encoder[UUID].contramap(_.value)

  given Decoder[TeacherId] = Decoder[UUID].map(TeacherId.apply)
  given Encoder[TeacherId] = Encoder[UUID].contramap(_.value)

  given Decoder[CourseId] = Decoder[UUID].map(CourseId.apply)
  given Encoder[CourseId] = Encoder[UUID].contramap(_.value)

  given Decoder[AssignmentId] = Decoder[UUID].map(AssignmentId.apply)
  given Encoder[AssignmentId] = Encoder[UUID].contramap(_.value)

  given Decoder[GradeId] = Decoder[UUID].map(GradeId.apply)
  given Encoder[GradeId] = Encoder[UUID].contramap(_.value)

  given Decoder[UserId] = Decoder[UUID].map(UserId.apply)
  given Encoder[UserId] = Encoder[UUID].contramap(_.value)


  // Enums
  given Decoder[AssignmentType] = Decoder.decodeString.emap {
    case "Homework" => Right(AssignmentType.Homework)
    case "Quiz" => Right(AssignmentType.Quiz)
    case "Exam" => Right(AssignmentType.Exam)
    case "Project" => Right(AssignmentType.Project)
    case "Participation" => Right(AssignmentType.Participation)
    case other => Left(s"Invalid assignment type: $other")
  }

  given Encoder[AssignmentType] = Encoder.encodeString.contramap {
    case AssignmentType.Homework => "Homework"
    case AssignmentType.Quiz => "Quiz"
    case AssignmentType.Exam => "Exam"
    case AssignmentType.Project => "Project"
    case AssignmentType.Participation => "Participation"
  }

  given Decoder[LetterGrade] = Decoder.decodeString.emap {
    case "A+" => Right(LetterGrade.APlus)
    case "A" => Right(LetterGrade.A)
    case "A-" => Right(LetterGrade.AMinus)
    case "B+" => Right(LetterGrade.BPlus)
    case "B" => Right(LetterGrade.B)
    case "B-" => Right(LetterGrade.BMinus)
    case "C+" => Right(LetterGrade.CPlus)
    case "C" => Right(LetterGrade.C)
    case "C-" => Right(LetterGrade.CMinus)
    case "D+" => Right(LetterGrade.DPlus)
    case "D" => Right(LetterGrade.D)
    case "F" => Right(LetterGrade.F)
    case other => Left(s"Invalid letter grade: $other")
  }

  given Encoder[LetterGrade] = Encoder.encodeString.contramap {
    case LetterGrade.APlus => "A+"
    case LetterGrade.A => "A"
    case LetterGrade.AMinus => "A-"
    case LetterGrade.BPlus => "B+"
    case LetterGrade.B => "B"
    case LetterGrade.BMinus => "B-"
    case LetterGrade.CPlus => "C+"
    case LetterGrade.C => "C"
    case LetterGrade.CMinus => "C-"
    case LetterGrade.DPlus => "D+"
    case LetterGrade.D => "D"
    case LetterGrade.F => "F"
  }

  given Decoder[UserRole] = Decoder.decodeString.emap {
    case "Student" => Right(UserRole.Student)
    case "Teacher" => Right(UserRole.Teacher)
    case "Admin" => Right(UserRole.Admin)
    case other => Left(s"Invalid user role: $other")
  }

  given Encoder[UserRole] = Encoder.encodeString.contramap {
    case UserRole.Student => "Student"
    case UserRole.Teacher => "Teacher"
    case UserRole.Admin => "Admin"
  }

  // Domain models - using auto derivation for Scala 3 compatibility
  import io.circe.generic.auto.{*, given}

  // The auto derivation will automatically provide codecs for all case classes


  // Events
  given Decoder[DomainEvent] = Decoder.instance { cursor =>
    cursor.downField("type").as[String].flatMap {
      case "grade.submitted" =>
        for
          gradeId <- cursor.downField("gradeId").as[GradeId]
          studentId <- cursor.downField("studentId").as[StudentId]
          assignmentId <- cursor.downField("assignmentId").as[AssignmentId]
          courseId <- cursor.downField("courseId").as[CourseId]
          score <- cursor.downField("score").as[Double]
          submittedAt <- cursor.downField("submittedAt").as[Instant]
        yield DomainEvent.GradeSubmitted(gradeId, studentId, assignmentId, courseId, score, submittedAt)

      case "grade.updated" =>
        for
          gradeId <- cursor.downField("gradeId").as[GradeId]
          oldScore <- cursor.downField("oldScore").as[Double]
          newScore <- cursor.downField("newScore").as[Double]
          updatedBy <- cursor.downField("updatedBy").as[TeacherId]
          updatedAt <- cursor.downField("updatedAt").as[Instant]
        yield DomainEvent.GradeUpdated(gradeId, oldScore, newScore, updatedBy, updatedAt)

      case other => Left(io.circe.DecodingFailure(s"Unknown event type: $other", cursor.history))
    }
  }

  given Encoder[DomainEvent] = Encoder.instance {
    case DomainEvent.GradeSubmitted(gradeId, studentId, assignmentId, courseId, score, submittedAt) =>
      io.circe.Json.obj(
        "type" -> io.circe.Json.fromString("grade.submitted"),
        "gradeId" -> Encoder[GradeId].apply(gradeId),
        "studentId" -> Encoder[StudentId].apply(studentId),
        "assignmentId" -> Encoder[AssignmentId].apply(assignmentId),
        "courseId" -> Encoder[CourseId].apply(courseId),
        "score" -> io.circe.Json.fromDoubleOrNull(score),
        "submittedAt" -> Encoder[Instant].apply(submittedAt)
      )

    case DomainEvent.GradeUpdated(gradeId, oldScore, newScore, updatedBy, updatedAt) =>
      io.circe.Json.obj(
        "type" -> io.circe.Json.fromString("grade.updated"),
        "gradeId" -> Encoder[GradeId].apply(gradeId),
        "oldScore" -> io.circe.Json.fromDoubleOrNull(oldScore),
        "newScore" -> io.circe.Json.fromDoubleOrNull(newScore),
        "updatedBy" -> Encoder[TeacherId].apply(updatedBy),
        "updatedAt" -> Encoder[Instant].apply(updatedAt)
      )

    case other => io.circe.Json.obj("type" -> io.circe.Json.fromString("unknown"))
  }


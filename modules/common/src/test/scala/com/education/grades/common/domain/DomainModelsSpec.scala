package com.education.grades.common.domain

import munit.FunSuite
import java.util.UUID

class DomainModelsSpec extends FunSuite:

  test("StudentId should wrap UUID") {
    val uuid = UUID.randomUUID()
    val studentId = StudentId(uuid)
    assertEquals(studentId.value, uuid)
  }

  test("Assignment should enforce positive max score") {
    val assignment = Assignment(
      id = AssignmentId(UUID.randomUUID()),
      courseId = CourseId(UUID.randomUUID()),
      name = "Test Assignment",
      maxScore = 100.0,
      weight = 0.3,
      dueDate = java.time.Instant.now(),
      assignmentType = AssignmentType.Exam
    )

    assert(assignment.maxScore > 0)
  }

  test("AssignmentType enum should have all expected values") {
    val types = List(
      AssignmentType.Homework,
      AssignmentType.Quiz,
      AssignmentType.Exam,
      AssignmentType.Project,
      AssignmentType.Participation
    )

    assertEquals(types.size, 5)
  }


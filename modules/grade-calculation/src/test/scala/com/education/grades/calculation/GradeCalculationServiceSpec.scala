package com.education.grades.calculation

import cats.effect.IO
import munit.CatsEffectSuite
import com.education.grades.common.domain.*

class GradeCalculationServiceSpec extends CatsEffectSuite:

  test("LetterGrade.fromNumericGrade should convert 95 to A") {
    val grade = LetterGrade.fromNumericGrade(95.0)
    assertEquals(grade, LetterGrade.A)
  }

  test("LetterGrade.fromNumericGrade should convert 87 to B+") {
    val grade = LetterGrade.fromNumericGrade(87.0)
    assertEquals(grade, LetterGrade.BPlus)
  }

  test("LetterGrade.fromNumericGrade should convert 73 to C") {
    val grade = LetterGrade.fromNumericGrade(73.0)
    assertEquals(grade, LetterGrade.C)
  }

  test("LetterGrade.fromNumericGrade should convert 55 to F") {
    val grade = LetterGrade.fromNumericGrade(55.0)
    assertEquals(grade, LetterGrade.F)
  }

  test("Letter grades should have correct GPA values") {
    assertEquals(LetterGrade.APlus.gpa, 4.0)
    assertEquals(LetterGrade.A.gpa, 4.0)
    assertEquals(LetterGrade.AMinus.gpa, 3.7)
    assertEquals(LetterGrade.BPlus.gpa, 3.3)
    assertEquals(LetterGrade.B.gpa, 3.0)
    assertEquals(LetterGrade.F.gpa, 0.0)
  }


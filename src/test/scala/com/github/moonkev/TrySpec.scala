package com.github.moonkev

import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper


class TrySpec extends AnyFlatSpec {

  "TryExamples.simple" should "equal 'The meaning of life is 42'" in {
    TryExamples.simple.success.value shouldBe ("The meaning of life is 42")
  }

  "TryExamples.simple" should "fail with a runtime exception" in {
    TryExamples.bind.failure.exception.getMessage shouldBe ("Better luck next time")
  }

  "TryExamples.composed" should "fail with a runtime exception" in {
    TryExamples.composed.success.value should (be >= 0 and be <= 1998)
  }

  "TryExamples.composedFailure" should "fail with a runtime exception" in {
    TryExamples.composedFailure.failure.exception.getMessage shouldBe ("Better luck next time")
  }

  "TryExamples.patternMatching" should "equal 'The meaning of life is 42'" in {
    TryExamples.patternMatching shouldBe ("The meaning of life is 42")
  }
}

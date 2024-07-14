package com.github.moonkev

import org.scalatest.flatspec.AsyncFlatSpec

class FuturesSpec extends AsyncFlatSpec {

  "FutureExamples.simple" should "equal 'The meaning of life 42'" in {
    FutureExamples.simple map { result => assert(result == "The meaning of life is 42")}
  }

  "FutureExamples.bind" should "equal 'The meaning of life 42'" in {
    FutureExamples.bind map { result => assert(result == "The meaning of life is 42")}
  }

  "FutureExamples.composed" should "be between 0 and 1998 inclusive" in {
    FutureExamples.composed map { result => assert(result >= 0 && result <= 1998)}
  }

  "FutureExamples.simpleLift" should "equal 'The meaning of life 42'" in {
    FutureExamples.simpleLift map { result => assert(result == "The meaning of life is 42")}
  }

  "FutureExamples.callbackLift" should "be between 0 and 1998 inclusive" in {
    FutureExamples.callbackLift map { result => assert(result >= 0 && result <= 1998)}
  }
}

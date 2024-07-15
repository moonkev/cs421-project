package com.github.moonkev

import scala.util.{Failure, Success, Try}

/*
Examples demonstrating the scala.util.Try monad
 */
object TryExamples {

  /*
  Lift execution into a Try.  As we map over the Try, we are passed the values of
  successful computation.  If at any point a Throwable is thrown, the execution is
  short-circuited, and the Throwable is now lifted into the Try
   */
  def simple: Try[String] = {
    val longComputation = Try((1L until 100000L).foldRight(0L)((a, b) => a + b ))
    longComputation
      .map(_ - 4999949958L)
      .map(answer => s"The meaning of life is $answer")
  }

  /*
  Similar to simple, except leveraging flatMap.  Useful if you want to sequence over
  other functions that themselves return a Try, or possibly you want to short-circuit
  to a Failure manually based on some condition
   */
  def bind: Try[String] = {
    val longComputation = Try((1L until 100000L).foldRight(0L)((a, b) => a + b ))
    longComputation
      .flatMap(n => Try(n - 4999949958L))
      .flatMap(_ => MockApi.tryFail)
  }

  /*
  Using a for-comprehension to extract values to construct further Trys
   */
  def composed: Try[Int] = {
    val wrapped1: Try[Int] = Try(MockApi.blockingCall)
    val wrapped2: Try[Int] = Try(MockApi.blockingCall)
    for {
      inputA <- wrapped1
      inputB <- wrapped2
      sum  <- Try(inputA + inputB)
    } yield sum
  }

  /*
  Similar to compose except that  when failure is reached,
  the entire comprehension is short-circuited
   */
  def composedFailure: Try[Int] = {
    val wrapped: Try[Int] = Try(MockApi.blockingCall)
    val failure: Try[Int] = MockApi.tryFail
    for {
      inputA <- wrapped
      inputB <- failure
      sum  <- Try(inputA + inputB)
    } yield sum
  }

  /*
  Success or failure can be gleaned and the successful value or Throwable can
  be extracted using pattern matching
   */
  def patternMatching: String =
    simple match {
      case Success(result) => result
      case Failure(exception) => exception.getLocalizedMessage
    }
}

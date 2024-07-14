package com.github.moonkev

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/*
Examples demonstrating the scala.conncurent.Future monad
 */
object FutureExamples {

  /*
  We lift execution into a Future.  As we map over the Future, we are passed the values of
  successful computation.  If at any point a Throwable is thrown, the execution is
  short-circuited, and the Throwable is now lifted into the Future
   */
  def simple: Future[String] = {
    val longComputation = Future((1L until 100000L).foldRight(0L)((a, b) => a + b ))
    longComputation
      .map(_ - 4999949958L)
      .map(answer => s"The meaning of life is $answer")
  }

  /*
  Similar to simple, except leveraging flatMap.  Useful if you want to sequence over
  other functions that themselves return a Future
   */
  def bind: Future[String] = {
    val longComputation = Future((1L until 100000L).foldRight(0L)((a, b) => a + b ))
    longComputation
      .flatMap(n => Future(n - 4999949958L))
      .flatMap(answer => Future(s"The meaning of life is $answer"))
  }

  /*
  Using a for-comprehension to extract and values to construct further Futures
   */
  def composed: Future[Int] = {
    val wrappedLongExec: Future[Int] = Future(MockApi.blockingCall)
    val nonBlocking: Future[Int] = MockApi.nonBlockingCall
    for {
      inputA <- wrappedLongExec
      inputB <- nonBlocking
      sum  <- MockApi.nonBlockingCallWithInput(inputA, inputB)
    } yield sum
  }

  /*
  This lifts a value directly into a Future, without invoking any concurrent execution.
  Useful when you want to avoid context switching or already know the value to return.
   */
  def simpleLift: Future[String] = {
    Future
      .successful(42)
      .map(answer => s"The meaning of life is $answer")
  }

  /*
  Here we use a help class called Promise.  A promise contains a .success
  method that we can call when we have a value to complete it with.  This
  then completes an internal Future on the promise that can be accessed
  via it's .future member.  This allows us to pass a callback that invokes
  the .success method, but dealing with and collecting the result via
  the promises .future member.
   */
  def callbackLift: Future[Int] = {
    val promise = Promise[Int]()
    MockApi.nonBlockingCallWithCallback(promise.success)
    val callbackFuture: Future[Int] = promise.future
    val nonBlocking: Future[Int] = MockApi.nonBlockingCall
    for {
      inputA <- callbackFuture
      inputB <- nonBlocking
      sum  <- MockApi.nonBlockingCallWithInput(inputA, inputB)
    } yield sum
  }
}

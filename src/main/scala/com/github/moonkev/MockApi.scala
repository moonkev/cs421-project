package com.github.moonkev

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Try}

/*
 The purpose of MockApi is simply to provide a mock of client
 code or APIs you would see in the real world.  The main purpose
 is to simulate long-running blocking or async IO calls, and the
 actual computation of them should be ignored, and can just be
 imagined to be doing some actual meaningful work.
 */
object MockApi {

  private val rng = new Random(System.currentTimeMillis())

  /*
   Supply a fresh single thread executor each time a new
   Future is created in any of the below functions
   The constructor for future will be passed the value from a call
   to this function automatically do the ExecutionContext parameter
   being annotated with the implicit qualifier
   */
  implicit def singleThreadExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())


  /*
   This is to simulate a long-running synchronous API call
   */
  def blockingCall: Int = {
      val rand = rng.nextInt(1000)
      Thread.sleep(rand)
      rand
    }

  /*
   This is to simulate a long-running asynchronous API call
   For example an HTTP client library performing IO on a thread
   that is controlled by the library and not the user
   */
  def nonBlockingCall: Future[Int] =
    Future {
      val rand = rng.nextInt(1000)
      Thread.sleep(rand)
      rand
    }

  /*
   This is to simulate a long-running asynchronous API call
   For example an HTTP client library performing IO on a thread
   that is controlled by the library and not the user
   */
  def nonBlockingCallWithInput(inputA: Int, inputB: Int): Future[Int] =
    Future {
      val sum = inputA + inputB
      Thread.sleep(sum)
      sum
    }

  /*
   This is to simulate a long-running asynchronous API call
   For example an HTTP client library performing IO on a thread
   that is controlled by the library and not the user
   */
  def nonBlockingCallWithCallback(cb: Int => Unit): Unit =
    Future {
      val rand = rng.nextInt(1000)
      Thread.sleep(rand)
      rand
    }.foreach(cb)

  /*
   Simulate a failure in the context of a Try
   */
  lazy val trySucceed: Try[Int] = Try(42)

  /*
   Simulate a failure in the context of a Try
   */
  def tryFail[A]:  Try[A] =
    Failure(new RuntimeException("Better luck next time"))
}

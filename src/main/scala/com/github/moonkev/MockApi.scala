package com.github.moonkev

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
   This is to simulate an API such as a message broker API which will execute a callback upon an event
   such as receiving a message on a channel.
   */
  def nonBlockingCallWithMultiCallback[A](cb: Int => A, n: Int): Future[IndexedSeq[A]] =
    Future.sequence((0 until n).map { _ =>
      Future {
        val rand = rng.nextInt(1000)
        Thread.sleep(rand)
        rand
      }.map(cb)
    })

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

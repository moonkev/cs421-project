package com.github.moonkev

import cats.effect.IO
import cats.effect.std.Queue
import fs2.Stream

object FS2Examples {

  /*
  Simple stream created from an apply (which takes a varargs set of elements).
  The stream adds 100 to each element
   */
  def simpleStream: Stream[IO, Int] =
    Stream(1, 2, 3, 4, 5).map(100.+)

  /*
  Create a stream that just continuously evaluates an IO.  Here the IO
  is lifted from an API call that returns a Future.   It filters the data
  for even numbers and takes the first 5 it sees.
   */
  def fromFutureWithFilter: Stream[IO, Int] =
    Stream
      .repeatEval(IO.fromFuture(IO(MockApi.nonBlockingCall)))
      .filter(n => (n % 2) == 0)
      .take(5)

  /*
  Build a stream using Queue.  In this example, we use this queue
  in a closure that is passed to a callback based API.
  An internal stream is created from the evaluation of an IO,
  and thus we flatten out the surrounding stream to lift the internal one out.
   */
  def queueStream: Stream[IO, Int] =
    Stream.eval {
      for {
        queue <- Queue.unbounded[IO, Option[Int]]
        events <- IO.fromFuture(IO(MockApi.nonBlockingCallWithMultiCallback[IO[Unit]](n => queue.offer(Some(n)), 5)))
        _ <- IO.parSequenceN(5)(events.toList)
        _ <- queue.offer(None)
      } yield Stream.fromQueueNoneTerminated(queue).map(n => n * 2 + 1000)
    }.flatten
}

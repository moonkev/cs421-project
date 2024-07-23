package com.github.moonkev

import cats.effect.{IO, Resource}

import java.io.{BufferedReader, InputStreamReader}
import java.net.InetAddress
import java.util.stream.Collectors
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.ListHasAsScala

object IOExamples {

  /*
  Simulate long-running synchronous API call.  The >> operator in analogous to
  a bind operation which doesn't actually pass the contained result of the
  first IO to the second.
   */
  def simple: IO[String] =
    IO.sleep(1.second) >> IO.pure(42).map(answer => s"The meaning of life is $answer")

  /*
  Lift an async callback based API into an IO
   */
  def asyncCallback: IO[Int] =
    IO.async_(cb => MockApi.nonBlockingCallWithCallback(num => cb(Right(num))))

  /*
  Lift try into an IO
   */
  def fromTry: IO[Int] =
    IO.fromTry(MockApi.trySucceed)

  /*
  Lift future into an IO
   */
  def fromFuture: IO[Int] =
    IO.fromFuture(IO(MockApi.nonBlockingCall))

  /*
  Execute multiple IO in parallel
   */
  def parallel: IO[Seq[Int]] =
    IO.parSequenceN(3)(Seq(asyncCallback, fromTry, fromFuture))

  private def fqdnFileResource: Resource[IO, BufferedReader] =
    for {
      inputStream <- Resource.fromAutoCloseable(IO(getClass.getResourceAsStream("/fqdn_list.txt")))
      inputStreamReader <- Resource.fromAutoCloseable(IO(new InputStreamReader(inputStream)))
      bufferedReader <- Resource.fromAutoCloseable(IO(new BufferedReader(inputStreamReader)))
    } yield bufferedReader

  def readFqdnList: IO[List[String]] =
    fqdnFileResource
      .use { reader => IO(reader.lines().collect(Collectors.toList())) }
      .map(_.asScala.toList)

  def resolveIP(hostname: String): IO[String] =
    IO(InetAddress.getByName(hostname))
      .map(_.getHostAddress)
}

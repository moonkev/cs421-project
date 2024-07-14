package com.github.moonkev

import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxTuple2Parallel

import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import java.net.InetAddress
import java.util.stream.Collectors
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.ListHasAsScala

object IOExamples {

  // This is to simulate a long-running synchronous API call
  def simple: IO[String] =
    IO.sleep(1.second) >> IO(42).map(answer => s"The meaning of life is $answer")

  // Lift an async callback based API into an IO
  def asyncCallback: IO[Int] =
    IO.async_(cb => MockApi.nonBlockingCallWithCallback(num => cb(Right(num))))

  // Lift try into an IO
  def fromTry: IO[Int] =
    IO.fromTry(MockApi.trySucceed)

  // Lift future into an IO
  def fromFuture: IO[Int] =
    IO.fromFuture(IO.pure(MockApi.nonBlockingCall))

  // Execute two IO in parallel
  def parallel: IO[(Int, Int)] =
    (fromTry, fromFuture).parTupled

  private def fqdnFileResource: Resource[IO, BufferedReader] =
    for {
      inputStream <- Resource.fromAutoCloseable(IO(getClass.getResourceAsStream("/fqdn_list.txt")))
      inputStreamReader <- Resource.fromAutoCloseable(IO(new InputStreamReader(inputStream)))
      bufferedReader <- Resource.fromAutoCloseable(IO(new BufferedReader(inputStreamReader)))
    } yield bufferedReader

  def readFqdnList: IO[Seq[String]] =
    fqdnFileResource
      .use { reader => IO(reader.lines().collect(Collectors.toList())) }
      .map(_.asScala.toSeq)

  def resolveIP(hostname: String): IO[String] =
    IO(InetAddress.getByName(hostname))
      .map(_.getHostAddress)

  def resolveAll =
    readFqdnList.flatMap(addrs => IO.parTraverseN(3)(addrs)(resolveIP))
}

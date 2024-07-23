package com.github.moonkev

import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}

object IOSampleApp extends IOApp {

  /*
  Small example of a program written using Cats Effect.  This program reads
  fully qualified domain names from a text file, then resolves the IP address
  of those domain names through DNS.
   */
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      fqdns <- IOExamples.readFqdnList
      fqdnsWithDummy <- IO("dummy.bad" :: fqdns)
      _ <- IO.parTraverseN(fqdnsWithDummy.length)(fqdnsWithDummy)(hostname =>
        IOExamples
          .resolveIP(hostname)
          .map(address => s"Successfully resolved host hostname with ip $address")
          .handleError(error => s"Unable to resolve host ${error.getMessage}")
          .flatMap(Console[IO].println)
      )
    } yield ExitCode.Success
  }
}


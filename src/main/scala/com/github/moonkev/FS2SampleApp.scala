package com.github.moonkev

import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream

import java.net.InetAddress

object FS2SampleApp extends IOApp {

  /*
  Small example of a program written using FS2.  This program reads
  fully qualified domain names from a text file, then resolves the IP address
  of those domain names through DNS.
   */
  override def run(args: List[String]): IO[ExitCode] =
    Stream
      .eval(IOExamples.readFqdnList)
      .flatMap(Stream.emits)
      .append(Stream("dummy.dummy"))
      .parEvalMapUnordered(4) { fqdn =>
        IO(InetAddress.getByName(fqdn))
          .map(Right.apply[Throwable, InetAddress])
          .handleError(Left.apply[Throwable, InetAddress])
      }
      .parEvalMapUnordered(4) {
        case Right(host) =>
          Console[IO].println(s"Successfully resolved host ${host.getHostName} with ip ${host.getHostAddress}")
        case Left(error) =>
          Console[IO].println(s"Unable to resolve host ${error.getMessage}")
      }
      .compile
      .drain
      .as(ExitCode.Success)
}

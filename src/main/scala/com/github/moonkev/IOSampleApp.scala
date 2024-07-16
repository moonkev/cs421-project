package com.github.moonkev

import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.catsSyntaxTuple2Parallel

object IOSampleApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      tupled <- (IOExamples.asyncCallback, IOExamples.fromFuture).parTupled
      _ <- Console[IO].println(s"Async callback returned ${tupled._1}")
      _ <- Console[IO].println(s"Future returned ${tupled._2}")
      fix <- IO.raiseError(new RuntimeException("Boom"))
        .handleError(e => s"Fixed error ${e.getMessage}")
      _ <- Console[IO].println(fix)
      fqdns <- IOExamples.readFqdnList
      fqdnsWithDummy <- IO("dummy.bad" :: fqdns)
      _ <- IO.parTraverseN(fqdnsWithDummy.length)(fqdnsWithDummy)(hostname =>
        IOExamples
          .resolveIP(hostname)
          .map(ip => s"$hostname resolves to IP: $ip")
          .handleError(e => s"Unable to resolve IP for $hostname: ${e.getMessage}")
          .flatMap(Console[IO].println)
      )
    } yield ExitCode.Success
  }
}


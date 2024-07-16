package com.github.moonkev

import zio._
import zio.Console._

object ZIOSampleApp extends ZIOAppDefault {

  def run: ZIO[ZIOAppArgs with Scope, Any, Unit] =
    for {
      _    <- printLine("Hello! What is your name?")
      name <- readLine
      _    <- printLine(s"Hello, ${name}, welcome to ZIO!")
    } yield ()
}
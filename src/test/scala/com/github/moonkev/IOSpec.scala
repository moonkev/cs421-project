package com.github.moonkev

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Inspectors.forAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class IOSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {
  "IOExamples.simple" should "equal 'The meaning of life is 42'" in {
    IOExamples.simple asserting {
      _ shouldBe "The meaning of life is 42"
    }
  }

  "IOExamples.asyncCallback" should "be between 0 and 1998 inclusive" in {
    IOExamples.asyncCallback asserting {
      _ should (be >= 0 and be <= 1998)
    }
  }

  "IOExamples.fromTry" should "be 42" in {
    IOExamples.fromTry asserting {
      _ shouldBe 42
    }
  }

  "IOExamples.fromFuture" should "be between 0 and 1998 inclusive" in {
    IOExamples.fromFuture asserting {
      _ should (be >= 0 and be <= 1998)
    }
  }

  "IOExamples.parallel" should "be a sequence of integer between 0 and 1998 inclusive" in {
    IOExamples.parallel asserting {
      res => forAll(res){ _ should(be >= 0 and be <= 1998) }
    }
  }

  "IOExamples.readFqdnList" should "read a list of fqdns from a file" in {
    IOExamples.readFqdnList asserting {
      _ shouldBe Seq(
        "www.illinois.edu", "www.scala-lang.org", "www.typelevel.org"
      )
    }
  }

  "IOExamples.resolveIP" should "resolve an IP address for www.illinois.edu" in {
    IOExamples.resolveIP("www.illinois.edu") asserting {
      _ should fullyMatch regex """^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"""
    }
  }

  "IOExamples.traverse" should "resolve an IP address for www.illinois.edu" in {
    IOExamples.resolveAll asserting {
      ips => forAll(ips){ _ should fullyMatch regex """^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$""" }
    }
  }
}

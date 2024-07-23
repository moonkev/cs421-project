package com.github.moonkev

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Inspectors.forAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class FS2Spec extends AsyncFlatSpec with AsyncIOSpec with Matchers with CustomMatchers {

  "FS2Examples.simpleStream" should "should provide a constant stream of the integers - 101,102,103,104,105" in {
    FS2Examples.simpleStream.compile.toList asserting { _ shouldBe List(101, 102, 103, 104, 105) }
  }

  "FS2Examples.fromFutureWithFilter" should "should provide a stream of even between 0 and 999 inclusive" in {
    FS2Examples.fromFutureWithFilter.compile.toList asserting { res =>
      forAll(res){ n  => n should(be  >= 0 and be <= 999 and be (even)) }
    }
  }

  "FS2Examples.queueStream" should "should provide a stream of even between 1000 and 2998 inclusive" in {
    FS2Examples.queueStream.compile.toList asserting { res =>
      forAll(res){ n  => n should(be  >= 1000 and be <= 2998) }
    }
  }
}

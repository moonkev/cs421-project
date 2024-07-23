package com.github.moonkev

import org.scalatest.matchers.{BeMatcher, MatchResult}

trait CustomMatchers {
  class EvenMatcher extends BeMatcher[Int] {
    def apply(left: Int): MatchResult =
      MatchResult(left % 2 == 0, left.toString + " was odd", left.toString + " was even")
  }

  val even = new EvenMatcher
}

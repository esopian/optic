package com.seamless.changelog

import org.scalatest.FunSpec

class SetUtilsSpec extends FunSpec {

  import SetUtils._
  it("works for added") {
    assert( (Set(1,2,3) added Set(1,2,3,5, 9)) == Set(5,9) )
  }

  it("works for removed") {
    assert( (Set(1,2, 5) removed Set(5)) == Set(1, 2) )
  }

}

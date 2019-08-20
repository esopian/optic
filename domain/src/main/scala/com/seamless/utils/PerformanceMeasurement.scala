package com.seamless.utils

object PerformanceMeasurement {
  def time[R](event: String)(block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    println(event + ": " + (t1 - t0) + "ms")
    result
  }
}

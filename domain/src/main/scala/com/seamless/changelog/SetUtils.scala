package com.seamless.changelog

object SetUtils {
  implicit class SetWithAddedRemovedUtils[T](s:Set[T]) {

    def removed(otherSet: Set[T]): Set[T] = {
      val intersection = s intersect otherSet
      s -- intersection
    }

    def added(otherSet: Set[T]): Set[T] = {
      val intersection = s intersect otherSet
      otherSet -- intersection
    }

  }
}

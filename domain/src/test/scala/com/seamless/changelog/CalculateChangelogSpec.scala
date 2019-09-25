package com.seamless.changelog

import com.seamless.contexts.rfc.Events
import com.seamless.diff.JsonFileFixture
import com.seamless.serialization.EventSerialization
import org.scalatest.FunSpec

class CalculateChangelogSpec extends FunSpec with JsonFileFixture {

  def fixture(slug: String): Vector[Events.RfcEvent] = {
    val events = eventsFrom(slug)
    EventSerialization.fromJson(events).get
  }

  it("can prepare for changelog generation") {
    val f = fixture("ToDoHistory")
    val input = CalculateChangelog.prepare(f, 20)
    assert(input.headPaths.size == 4 && input.historicalPaths.size == 1)
  }

  it("can calculate added paths") {
    val f = fixture("ToDoHistory")
    val input = CalculateChangelog.prepare(f, 20)
    val addedPaths = CalculateChangelog.computeAddedPaths(input)

    assert(Vector(
      Changelog.AddedRequest("/todos", "GET", "request_R6MSTC8kFn"),
      Changelog.AddedRequest("/todos", "POST", "request_293Su6bI1V"),
      Changelog.AddedRequest("/todos/download", "GET", "request_nkXImtQgiS")
    ).toSet == addedPaths)
  }

  it("can compute request update") {
    val f = fixture("ToDoHistory")
    val input = CalculateChangelog.prepare(f, 30)
    val changes = CalculateChangelog.computeUpdatedPaths(input)

    null
  }


}

package com.seamless.serialization

import com.seamless.contexts.requests.Commands._
import com.seamless.contexts.rfc.Commands._
import com.seamless.contexts.rfc.Events.RfcEvent
import com.seamless.contexts.rfc.{InMemoryQueries, RfcCommandContext, RfcService, RfcServiceJSFacade, RfcState}
import com.seamless.ddd.EventStore
import com.seamless.serialization.CogentSerialization._
import io.circe.Json
import org.scalatest.FunSpec
import io.circe.literal._

class CogentSerializationSpec extends FunSpec {
  case class Wrapper(eventStore: EventStore[RfcEvent], rfcId: String, rfcService: RfcService)
  def fixture(rawEvents: Json) = {
    val events = EventSerialization.fromJson(rawEvents)
    val rfcId: String = "rfc-1"
    val eventStore = RfcServiceJSFacade.makeEventStore()
    eventStore.append(rfcId, events.get)
    val rfcService: RfcService = new RfcService(eventStore)
    Wrapper(eventStore, rfcId, rfcService)
  }

  def commandsFixture(initialCommands: Seq[RfcCommand]) = {
    val rfcId: String = "rfc-1"
    val eventStore = RfcServiceJSFacade.makeEventStore()
    val rfcService: RfcService = new RfcService(eventStore)
    rfcService.handleCommands(rfcId, RfcCommandContext("ccc", "sss", "bbb"), initialCommands: _*)
    Wrapper(eventStore, rfcId, rfcService)
  }


  describe("cogent representation") {
    describe("with an empty spec") {
      it("should be empty") {
        val wrapper = fixture(json"""[]""")
        val queries = new InMemoryQueries(wrapper.eventStore, wrapper.rfcService, wrapper.rfcId)
        val cogentRepresentation = CogentSerialization.asCogentApiArtifactRepresentation(queries)
        assert(cogentRepresentation == IApiArtifactGeneratorData(
          IApiSnapshot(
            Seq.empty
          )
        ))
      }
    }
    describe("with a request but no responses") {
      it("should show a request with no responses") {
        val wrapper = commandsFixture(
          Seq(
            AddRequest("req1", "root", "GET")
          )
        )
        val queries = new InMemoryQueries(wrapper.eventStore, wrapper.rfcService, wrapper.rfcId)
        val cogentRepresentation = CogentSerialization.asCogentApiArtifactRepresentation(queries)
        assert(cogentRepresentation == IApiArtifactGeneratorData(
          IApiSnapshot(
            Seq(
              IApiEndpoint(
                "/",
                "GET",
                IApiRequest(
                  Seq.empty,
                  Seq.empty,
                  Seq.empty,
                  Seq.empty
                ),
                Seq.empty
              )
            )
          )
        ))
      }
    }
  }
}

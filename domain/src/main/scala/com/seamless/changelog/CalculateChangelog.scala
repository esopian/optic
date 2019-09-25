package com.seamless.changelog

import com.seamless.contexts.requests.Commands.{PathComponentId, RequestId, UnsetBodyDescriptor}
import com.seamless.contexts.requests.{PathComponent, RequestsState, Utilities}
import com.seamless.contexts.requests.projections.PathsWithRequestsProjection
import com.seamless.contexts.rfc.Events.RfcEvent
import com.seamless.contexts.rfc.{RfcAggregate, RfcServiceJSFacade, RfcState}
import com.seamless.contexts.shapes.ShapesState
import com.seamless.ddd.{CachedProjection, EventStore, InMemoryEventStore}
import com.seamless.diff.{RequestDiffer, ShapeDiffTargetProvider, ShapeDiffer}
import SetUtils._
import com.seamless.changelog.Changelog.{AddedStatusCode, RemovedStatusCode, ResponseChangelog, ResponsesChangelog}
import com.seamless.diff.ShapeDiffer.resolveBaseObject
import com.seamless.diff.initial.ShapeResolver

object CalculateChangelog {

  def prepare(events: Vector[RfcEvent], since: Int): ChangelogInput = {
    val (history, head) = events.splitAt(since)

    println("Everything after" + events(since).toString)

    val pathsWithRequestsCache = new CachedProjection(PathsWithRequestsProjection, history)

    val pathsWithRequestsHistorical = pathsWithRequestsCache.withEvents(history)
    val pathsWithRequestsHead = pathsWithRequestsCache.withEvents(events)


    val historicalState = history.foldLeft(RfcAggregate.initialState) {
      case (state, event) => RfcAggregate.applyEvent(event, state)
    }

    val headState = head.foldLeft(historicalState) {
      case (state, event) => RfcAggregate.applyEvent(event, state)
    }

    ChangelogInput(
      pathsWithRequestsHistorical, pathsWithRequestsHead,
      historicalState, headState
    )
  }

  def generate(changelogInput: ChangelogInput) = {
    computeAddedPaths(changelogInput)
  }

  def computeAddedPaths(changelogInput: ChangelogInput): Set[Changelog.AddedRequest] = {
    val added = changelogInput.historicalPaths.keySet added changelogInput.headPaths.keySet

    implicit val pathComponents: Map[PathComponentId, PathComponent] = changelogInput.headState.requestsState.pathComponents

    added.map {
      case requestId => {
        //@todo handled is removed
        val method = changelogInput.headState.requestsState.requests(requestId).requestDescriptor.httpMethod
        val absolutePath = Utilities.toAbsolutePath(changelogInput.headPaths(requestId))
        Changelog.AddedRequest(absolutePath, method, requestId)
      }
    }
  }

  def computeUpdatedPaths(changelogInput: ChangelogInput) = {
    //only those that were present in both
    val requestsToCompare = changelogInput.headPaths.keySet intersect changelogInput.historicalPaths.keySet

    requestsToCompare.map {
      case requestId: RequestId => {
        val previous = RequestChangeHelper(requestId, changelogInput.historicalState)
        val current = RequestChangeHelper(requestId, changelogInput.headState)

        val pStatusCodes = previous.responses.values.map(_.statusCode).toSet
        val cStatusCodes = current.responses.values.map(_.statusCode).toSet

        val updated = pStatusCodes intersect cStatusCodes
        val updatedChangelogs = updated.map(status => {
          val (responseId, currentResponse) = current.responses.find(_._2.statusCode == status).get
          val previousResponse = previous.responses(responseId)

          (status, computeUpdatedResponses(previousResponse, currentResponse))
        })

        ResponsesChangelog(
          (pStatusCodes added cStatusCodes).map(i => AddedStatusCode(i, requestId)).toVector,
          (pStatusCodes removed cStatusCodes).map(i => RemovedStatusCode(i, requestId)).toVector,
          updatedChangelogs.filterNot(_._2.noDiff).toMap
        )

      }
    }
  }

  def computeUpdatedResponses(previous: ResponseChangeHelper, current: ResponseChangeHelper): ResponseChangelog = {

    val updatedContentType: Option[String] = {
      if (previous.hasBody && current.hasBody) {
        if (previous.shapedBody.httpContentType == current.shapedBody.httpContentType) {
          None
        } else {
          Some(current.shapedBody.httpContentType)
        }
      } else None
    }

    val addedBody = previous.emptyBody && current.hasBody
    val removedBody = previous.hasBody && current.emptyBody

    if (current.hasBody && previous.hasBody) {
      val currentShape = current.rfcState.shapesState.shapes(current.shapedBody.shapeId)
      val previousShape = previous.rfcState.shapesState.shapes(current.shapedBody.shapeId)

      val hello = ShapeDiffer.diffAll(previousShape, ShapeDiffTargetProvider.fromShapeEntity(Some(currentShape), current.rfcState))(previous.rfcState.shapesState)

      null
    }

    return ResponseChangelog(updatedContentType, addedBody, removedBody)
  }


}

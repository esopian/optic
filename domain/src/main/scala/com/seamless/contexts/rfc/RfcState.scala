package com.seamless.contexts.rfc

import com.seamless.contexts.requests.RequestsState
import com.seamless.contexts.shapes.ShapesState
import com.seamless.utils.PerformanceMeasurement.time

case class RfcState(requestsState: RequestsState, shapesState: ShapesState) {
  def updateShapes(shapesState: ShapesState): RfcState = {
    time("Updating Shapes") {
      this.copy(shapesState = shapesState)
    }
  }
  def updateRequests(requestsState: RequestsState) = {
    time("Updating Requests") {
      this.copy(requestsState = requestsState)
    }
  }
}

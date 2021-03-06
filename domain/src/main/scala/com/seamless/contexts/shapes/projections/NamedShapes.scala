package com.seamless.contexts.shapes.projections

import com.seamless.contexts.rfc.Events.RfcEvent
import com.seamless.contexts.shapes.Commands.ShapeId
import com.seamless.contexts.shapes.Events.{ShapeAdded, ShapeRenamed}
import com.seamless.ddd.Projection

case class NamedShape(shapeId: ShapeId, name: String)

object NamedShapes extends Projection[RfcEvent, Map[ShapeId, NamedShape]] {
  override def fromEvents(events: Vector[RfcEvent]): Map[ShapeId, NamedShape] = {
    withInitialState(Map.empty, events)
  }

  override def withInitialState(initialState: Map[ShapeId, NamedShape], events: Vector[RfcEvent]): Map[ShapeId, NamedShape] = {
    events.foldLeft(initialState) {
      case (shapes: Map[ShapeId, NamedShape], event) => {
        event match {
          case e: ShapeAdded => {
            if (e.name == "") shapes
            else shapes + (e.shapeId -> NamedShape(e.shapeId, e.name))
          }
          case e: ShapeRenamed => {
            val shape = shapes.getOrElse(e.shapeId, NamedShape(e.shapeId, e.name))
            if (e.name == "") shapes - e.shapeId
            else shapes + (e.shapeId -> shape.copy(name = e.name))
          }
          case _ => shapes
        }
      }
    }
  }
}

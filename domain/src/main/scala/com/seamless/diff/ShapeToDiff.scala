package com.seamless.diff

import com.seamless.contexts.rfc.RfcState
import com.seamless.contexts.shapes.Commands.{FieldShapeFromParameter, FieldShapeFromShape, NoProvider, ParameterProvider, ShapeProvider}
import com.seamless.contexts.shapes.ShapesHelper.{BooleanKind, ListKind, NullableKind, NumberKind, ObjectKind, StringKind}
import com.seamless.contexts.shapes.{ShapeEntity, ShapesHelper}
import com.seamless.diff.ShapeDiffer.resolveBaseObject
import io.circe.Json

abstract class ShapeDiffTargetProvider {
  def isEmpty: Boolean
  def id: String

  def isString: Boolean
  def isBoolean: Boolean
  def isNumber: Boolean
  def isNull: Boolean

  def isArray: Boolean
  def items: Vector[ShapeDiffTargetProvider]

  def isObject: Boolean
  def fields: Map[String, ShapeDiffTargetProvider]
  def getField(key: String, id: String): Option[ShapeDiffTargetProvider]

  def asJson: Json
}


object ShapeDiffTargetProvider {
  def fromActualJson(jsonOption: Option[Json]): ShapeDiffTargetProvider = new ShapeDiffTargetProvider {
    def id: String = null
    override def isEmpty: Boolean = jsonOption.isEmpty

    override def isString: Boolean = jsonOption.exists(_.isString)
    override def isBoolean: Boolean = jsonOption.exists(_.isBoolean)
    override def isNumber: Boolean = jsonOption.exists(_.isNumber)
    override def isNull: Boolean = jsonOption.exists(_.isNull)

    override def isArray: Boolean = jsonOption.exists(_.isArray)
    override def items: Vector[ShapeDiffTargetProvider] = jsonOption.get.asArray.get.map(i => fromActualJson(Some(i)))

    override def isObject: Boolean = jsonOption.exists(_.isObject)

    override def fields: Map[String, ShapeDiffTargetProvider] = {
      jsonOption.get.asObject.get.toMap.map {
        case (key, value) => (key, fromActualJson(Some(value)))
      }
    }

    def getField(key: String, id: String): Option[ShapeDiffTargetProvider] = fields.get(key)

    override def asJson: Json = jsonOption.get
  }

  def fromShapeEntity(shapeEntityOption: Option[ShapeEntity], rfcState: RfcState, _id: String = null): ShapeDiffTargetProvider = new ShapeDiffTargetProvider {
    override def isEmpty: Boolean = shapeEntityOption.isEmpty
    def id: String = _id

    private val asCoreShape = ShapesHelper.toCoreShape(shapeEntityOption.get, rfcState.shapesState)

    override def isString: Boolean = asCoreShape == StringKind
    override def isBoolean: Boolean = asCoreShape == BooleanKind
    override def isNumber: Boolean = asCoreShape == NumberKind
    override def isNull: Boolean = asCoreShape == NullableKind


    override def isArray: Boolean = asCoreShape == ListKind
    override def items: Vector[ShapeDiffTargetProvider] = Vector()

    override def isObject: Boolean = asCoreShape == ObjectKind
    override def fields: Map[String, ShapeDiffTargetProvider] = {
      val baseObject = resolveBaseObject(shapeEntityOption.get.shapeId)(rfcState.shapesState)
      baseObject.descriptor.fieldOrdering.map(fieldId => {
        val flattenedField = rfcState.shapesState.flattenedField(fieldId)

        val expectedFieldShape = flattenedField.fieldShapeDescriptor match {
          case fsd: FieldShapeFromShape => {
            Some(rfcState.shapesState.shapes(fsd.shapeId))
          }
          case fsd: FieldShapeFromParameter => {
            flattenedField.bindings(fsd.shapeParameterId) match {
              case Some(value) => value match {
                case p: ParameterProvider => {
                  None
                }
                case p: ShapeProvider => Some(rfcState.shapesState.shapes(p.shapeId))
                case p: NoProvider => None
              }
              case None => None
            }
          }
        }
        (flattenedField.name, fromShapeEntity(expectedFieldShape, rfcState, flattenedField.fieldId))
      }).toMap
    }

    def getField(key: String, id: String): Option[ShapeDiffTargetProvider] = {
      val a = this.fields
      fields.find(_._2.id == id).map(i => i._2)
    }

    override def asJson: Json = null
  }

}
package com.seamless.diff

import com.seamless.contexts.shapes.Commands._
import com.seamless.contexts.shapes.ShapesHelper._
import com.seamless.contexts.shapes.{ShapeEntity, ShapesState}
import io.circe._


object ShapeDiffer {
  sealed trait ShapeDiffResult {}
  case class NoDiff() extends ShapeDiffResult
  case class WeakNoDiff() extends ShapeDiffResult
  case class UnsetShape(actual: Json) extends ShapeDiffResult
  case class UnsetValue(expected: ShapeEntity) extends ShapeDiffResult
  case class NullValue(expected: ShapeEntity) extends ShapeDiffResult
  case class ShapeMismatch(expected: ShapeEntity, actual: Json) extends ShapeDiffResult
  case class UnsetObjectKey(parentObjectShapeId: ShapeId, fieldId: FieldId, key: String, expected: ShapeEntity) extends ShapeDiffResult
  case class NullObjectKey(parentObjectShapeId: ShapeId, fieldId: FieldId, key: String, expected: ShapeEntity) extends ShapeDiffResult
  case class UnexpectedObjectKey(parentObjectShapeId: ShapeId, key: String, expected: ShapeEntity, actual: Json) extends ShapeDiffResult
  case class KeyShapeMismatch(fieldId: FieldId, key: String, expected: ShapeEntity, actual: Json) extends ShapeDiffResult
  case class KeyRenamed(fieldId: FieldId, key: String, newKey: String) extends ShapeDiffResult
  case class MapValueMismatch(key: String, expected: ShapeEntity, actual: Json) extends ShapeDiffResult
  case class MultipleInterpretations(expected: ShapeEntity, actual: Json) extends ShapeDiffResult
  type ParameterBindings = Map[ShapeParameterId, Option[ProviderDescriptor]]

  class ShapeDiffAccumulator {
    private val _diffs = scala.collection.mutable.ListBuffer[ShapeDiffResult]()
    def diffs = _diffs.toVector
    def append(d: ShapeDiffResult*): ShapeDiffAccumulator = {
      println(d)
      _diffs.appendAll(d)
      this
    }
    def head = diffs.headOption.getOrElse(NoDiff())
    def noDiff = this
  }

  def diff(expectedShape: ShapeEntity, actualShape: ShapeDiffTargetProvider)(implicit shapesState: ShapesState, bindings: ParameterBindings = Map.empty): ShapeDiffResult = {
    implicit val accumulator: ShapeDiffAccumulator = new ShapeDiffAccumulator
    diffAll(expectedShape, actualShape)
    accumulator.diffs.headOption.getOrElse(NoDiff())
  }

  //@TODO change bindings to UsageTrail
  def diffAll(expectedShape: ShapeEntity, actualShape: ShapeDiffTargetProvider)(implicit shapesState: ShapesState, bindings: ParameterBindings = Map.empty, accumulator: ShapeDiffAccumulator = new ShapeDiffAccumulator): ShapeDiffAccumulator = {
    val coreShape = toCoreShape(expectedShape, shapesState)
    if (actualShape.isEmpty) {
      val diff = coreShape match {
        case AnyKind => WeakNoDiff()
        case OptionalKind => NoDiff()
        case _ => UnsetValue(expectedShape)
      }
      return accumulator append diff
    }

    coreShape match {
      case AnyKind => {
        accumulator append WeakNoDiff()
      }
      case StringKind => {
        if (actualShape.isString) {
          accumulator noDiff
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case BooleanKind => {
        if (actualShape.isBoolean) {
          accumulator noDiff
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case NumberKind => {
        if (actualShape.isNumber) {
          accumulator noDiff
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case ListKind => {
        if (actualShape.isArray) {
          val itemShape = resolveParameterShape(expectedShape.shapeId, "$listItem")
          if (itemShape.isDefined) {
            actualShape.items.foreach(item => {
              val diff = ShapeDiffer.diffAll(itemShape.get, item)
              diff match {
                case sd: NoDiff =>
                case x => accumulator append(x.diffs:_*)
              }
            })
          }
          accumulator noDiff
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case ObjectKind => {
        if (actualShape.isObject) {
          val baseObject = resolveBaseObject(expectedShape.shapeId)

          val expectedFields = baseObject.descriptor.fieldOrdering
            .map(fieldId => {
              val field = shapesState.fields(fieldId)
              (field.descriptor.name, field)
            })
          val actualFields = actualShape.fields
          val expectedKeys = expectedFields.map(_._1).toSet
          val actualKeys = actualFields.keySet

          val flattenedShape = shapesState.flattenedShape(expectedShape.shapeId)

          // make sure all expected keys match the spec
          val fieldMap = expectedFields.toMap
          expectedKeys.foreach(key => {
            val field = fieldMap(key)
            val flattenedField = shapesState.flattenedField(field.fieldId)
            val expectedFieldShape = flattenedField.fieldShapeDescriptor match {
              case fsd: FieldShapeFromShape => {
                Some(shapesState.shapes(fsd.shapeId))
              }
              case fsd: FieldShapeFromParameter => {
                flattenedField.bindings(fsd.shapeParameterId) match {
                  case Some(value) => value match {
                    case p: ParameterProvider => {
                      None
                    }
                    case p: ShapeProvider => Some(shapesState.shapes(p.shapeId))
                    case p: NoProvider => None
                  }
                  case None => None
                }
              }
            }
            if (expectedFieldShape.isDefined) {
              val actualFieldValue = actualShape.getField(key, flattenedField.fieldId).get

              //check for rename
              val (pName, cName) = actualShape.fields.find(_._2.id == flattenedField.fieldId).map(i => {(key, i._1)}).getOrElse((key, key))
              if (pName != cName) {
                println(pName + " change to  " + cName)
                accumulator append KeyRenamed(flattenedField.fieldId, pName, cName)
              }

              implicit val bindings: ParameterBindings = flattenedField.bindings ++ flattenedShape.bindings
              val diff = ShapeDiffer.diffAll(expectedFieldShape.get, actualFieldValue)(shapesState, bindings, accumulator = new ShapeDiffAccumulator).head
              diff match {
                case d: ShapeMismatch => accumulator append KeyShapeMismatch(field.fieldId, key, expectedFieldShape.get, actualFieldValue.asJson)
                case d: UnsetValue => accumulator append UnsetObjectKey(expectedShape.shapeId, field.fieldId, key, expectedFieldShape.get)
                case d: NullValue => accumulator append NullObjectKey(expectedShape.shapeId, field.fieldId, key, expectedFieldShape.get)
                case x: NoDiff =>
                case x => {
                  accumulator append x
                }
              }
            }
          })

          // detect keys that should not be present
          val extraKeys = actualKeys -- expectedKeys
          if (extraKeys.nonEmpty) {
            extraKeys.map(extra => {
              val actualFieldValue = actualShape.fields(extra)
              accumulator append UnexpectedObjectKey(baseObject.shapeId, extra, baseObject, actualFieldValue.asJson)
            })
          }
          accumulator
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case NullableKind => {
        val referencedShape = resolveParameterShape(expectedShape.shapeId, "$nullableInner")
        if (referencedShape.isDefined) {
          if (actualShape.isNull) {
            accumulator noDiff
          } else {
            accumulator append (ShapeDiffer.diffAll(referencedShape.get, actualShape).diffs:_*)
          }
        } else {
          accumulator append WeakNoDiff()
        }
      }
      case OptionalKind => {
        val referencedShape = resolveParameterShape(expectedShape.shapeId, "$optionalInner")
        if (referencedShape.isDefined) {
          accumulator append (ShapeDiffer.diffAll(referencedShape.get, actualShape).diffs:_*)
        } else {
          accumulator append WeakNoDiff()
        }
      }
      case OneOfKind => {
        // there's only a diff if none of the shapes match
        val shapeParameterIds = expectedShape.descriptor.parameters match {
          case DynamicParameterList(shapeParameterIds) => shapeParameterIds
        }
        val firstMatch = shapeParameterIds.find(shapeParameterId => {
          val referencedShape = resolveParameterShape(expectedShape.shapeId, shapeParameterId)
          if (referencedShape.isDefined) {
            val diff = ShapeDiffer.diffAll(referencedShape.get, actualShape)
            diff == NoDiff()
          } else {
            false
          }
        })
        println(firstMatch)
        if (firstMatch.isDefined) {
          accumulator noDiff
        } else {
          accumulator append MultipleInterpretations(expectedShape, actualShape.asJson)
        }
      }
      case MapKind => {
        if (actualShape.isObject) {
          val o = actualShape.fields
          val referencedShape = resolveParameterShape(expectedShape.shapeId, "$mapValue")
          if (referencedShape.isDefined) {
            o.keys.foreach(key => {
              val v = o(key)
              val diff = ShapeDiffer.diffAll(referencedShape.get, v).head
              diff match {
                case d: ShapeMismatch => return accumulator append MapValueMismatch(key, referencedShape.get, v.asJson)
                case d: UnsetValue => return accumulator append MapValueMismatch(key, referencedShape.get, v.asJson)
                case x: NoDiff =>
                case x => {
                  return return accumulator append x
                }
              }
            })
            accumulator noDiff
          } else {
            accumulator append WeakNoDiff()
          }
        } else {
          accumulator append shapeMismatchOrMissing(expectedShape, actualShape)
        }
      }
      case ReferenceKind => {
        val referencedShape = resolveParameterShape(expectedShape.shapeId, "$referenceInner")
        if (referencedShape.isDefined) {
          //@TODO: referencedShape should be an object. find the first field which is an Identifier<T>. Diff the actualShape against the resolved T
        }
        accumulator append WeakNoDiff()
      }
      case IdentifierKind => {
        val identifierShape = resolveParameterShape(expectedShape.shapeId, "$identifierInner")
        if (identifierShape.isDefined) {
          //@TODO: identifierShape should be a string or number. Diff the actualShape against it.
        }
        accumulator append WeakNoDiff()
      }
    }
  }

  def resolveParameterShape(shapeId: ShapeId, shapeParameterId: ShapeParameterId)(implicit shapesState: ShapesState, bindings: ParameterBindings): Option[ShapeEntity] = {
    val flattenedShape = shapesState.flattenedShape(shapeId)
    // println(bindings, flattenedShape.bindings)
    val itemShape: Option[ShapeEntity] = bindings.getOrElse(shapeParameterId, flattenedShape.bindings(shapeParameterId)) match {
      case Some(value) => value match {
        case ParameterProvider(shapeParameterId) => {
          resolveParameterShape(shapeId, shapeParameterId)
        }
        case ShapeProvider(shapeId) => Some(shapesState.shapes(shapeId))
        case NoProvider() => None
      }
      case None => None
    }
    itemShape
  }

  def resolveBaseObject(objectId: ShapeId)(implicit shapesState: ShapesState): ShapeEntity = {
    val o = shapesState.shapes(objectId)
    if (o.descriptor.baseShapeId == ObjectKind.baseShapeId) {
      o
    } else {
      resolveBaseObject(o.descriptor.baseShapeId)
    }
  }

  def shapeMismatchOrMissing(expectedShape: ShapeEntity, actualShape: ShapeDiffTargetProvider): ShapeDiffResult = {
    if (actualShape.isNull) {
      NullValue(expectedShape)
    } else {
      ShapeMismatch(expectedShape, actualShape.asJson)
    }
  }
}
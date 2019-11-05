package com.seamless.diff.interpreters

import com.seamless.contexts.requests.Commands.{SetRequestBodyShape, SetResponseBodyShape, ShapedBodyDescriptor}
import com.seamless.contexts.shapes.Commands._
import com.seamless.contexts.shapes._
import com.seamless.contexts.shapes.ShapesHelper._
import com.seamless.diff.{DiffInterpretation, FrontEndMetadata, HighlightNestedRequestShape, HighlightNestedResponseShape, HighlightNestedShape, InterpretationContext}
import com.seamless.diff.RequestDiffer._
import com.seamless.diff.ShapeDiffer._

class NullableInterpreter(shapesState: ShapesState) extends Interpreter[RequestDiffResult] {
  override def interpret(diff: RequestDiffResult): Seq[DiffInterpretation] = {
    diff match {
      case d: UnmatchedRequestBodyShape => {
        d.shapeDiff match {
          case sd: NullValue => {
            Seq(
              ChangeShapeToNullable(d, InterpretationContext(None, true), sd)
            )
          }
          case sd: NullObjectKey => {
            Seq(
              ChangeFieldToNullable(sd, InterpretationContext(None, true), HighlightNestedRequestShape(sd.parentObjectShapeId))
            )
          }
          case _ => Seq.empty
        }
      }
      case d: UnmatchedResponseBodyShape => {
        d.shapeDiff match {
          case sd: NullValue => {
            Seq(
              ChangeShapeToNullable(d, InterpretationContext(Some(d.responseId), false), sd)
            )
          }
          case sd: NullObjectKey => {
            Seq(
              ChangeFieldToNullable(sd, InterpretationContext(Some(d.responseId), false), HighlightNestedResponseShape(d.responseStatusCode, sd.parentObjectShapeId))
            )
          }
          case _ => Seq.empty
        }
      }
      case _ => Seq.empty
    }
  }

  def ChangeFieldToNullable(shapeDiff: NullObjectKey, context: InterpretationContext, highlightNestedShape: HighlightNestedShape): DiffInterpretation = {
    val wrapperShapeId = ShapesHelper.newShapeId()
    val field = shapesState.flattenedField(shapeDiff.fieldId)
    val commands = Seq(
      AddShape(wrapperShapeId, NullableKind.baseShapeId, ""),
      SetParameterShape(
        ProviderInShape(
          wrapperShapeId,
          field.fieldShapeDescriptor match {
            case fs: FieldShapeFromShape => ShapeProvider(fs.shapeId)
            case fs: FieldShapeFromParameter => ParameterProvider(fs.shapeParameterId)
          },
          "$nullableInner"
        )
      ),
      SetFieldShape(FieldShapeFromShape(field.fieldId, wrapperShapeId)),
    )

    DiffInterpretation(
      s"Make ${shapeDiff.key} nullable",
//      s"Optic expected to see a value for the key ${shapeDiff.key} and instead saw null. If it is allowed to be null, make it Nullable",
      commands,
      context,
      FrontEndMetadata(affectedIds = Seq(shapeDiff.fieldId), changed = true, highlightNestedShape = Some(highlightNestedShape))
    )
  }

  def ChangeShapeToNullable(requestDiffResult: UnmatchedRequestBodyShape, context: InterpretationContext, shapeDiff: NullValue): DiffInterpretation = {
    val wrapperShapeId = ShapesHelper.newShapeId()
    val commands = Seq(
      AddShape(wrapperShapeId, NullableKind.baseShapeId, ""),
      SetParameterShape(
        ProviderInShape(wrapperShapeId, ShapeProvider(shapeDiff.expected.shapeId), "$nullableInner")
      ),
      SetRequestBodyShape(requestDiffResult.requestId, ShapedBodyDescriptor(requestDiffResult.contentType, wrapperShapeId, isRemoved = false))
    )
    DiffInterpretation(
      "Make Request Body nullable",
//      s"Optic expected to see a value for the request but instead saw null. If it is allowed to be null, make it Nullable",
      commands,
      context,
      FrontEndMetadata(affectedIds = Seq(shapeDiff.expected.shapeId), changed = true)
    )
  }

  def ChangeShapeToNullable(requestDiffResult: UnmatchedResponseBodyShape, context: InterpretationContext, shapeDiff: NullValue): DiffInterpretation = {
    val wrapperShapeId = ShapesHelper.newShapeId()
    val commands = Seq(
      AddShape(wrapperShapeId, NullableKind.baseShapeId, ""),
      SetParameterShape(
        ProviderInShape(wrapperShapeId, ShapeProvider(shapeDiff.expected.shapeId), "$nullableInner")
      ),
      SetResponseBodyShape(requestDiffResult.responseId, ShapedBodyDescriptor(requestDiffResult.contentType, wrapperShapeId, isRemoved = false))
    )
    DiffInterpretation(
      "Makes Response Body nullable",
//      s"Optic expected to see a value for the request but instead saw null. If it is allowed to be null, make it Nullable",
      commands,
      context,
      FrontEndMetadata(affectedIds = Seq(shapeDiff.expected.shapeId), changed = true)
    )
  }
}

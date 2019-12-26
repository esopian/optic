package com.seamless.serialization


import com.seamless.contexts.requests.Commands.RequestId
import com.seamless.contexts.requests.{Commands, Utilities}
import com.seamless.contexts.rfc.InMemoryQueries
import com.seamless.contexts.shapes.ShapesHelper.OptionalKind
import com.seamless.contexts.shapes.projections.{FlatShapeProjection, JsonSchemaHelpers, JsonSchemaProjection}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import io.circe.{Json, JsonObject}
import io.circe.generic.auto._
import io.circe.syntax._

@JSExport
@JSExportAll
object CogentSerialization {

  type ApiPath = String
  type ApiRequestMethod = String
  type ApiContentType = String
  type ApiStatusCode = Int

  case class IApiRequestParameter (
    name: String,
    required: Boolean,
    asJsonSchema: Json
  )
  case class IApiRequestBody (
    contentType: ApiContentType,
    schema: IApiBodySchema,
  )
  case class IApiBodySchema (
    asJsonSchema: Json
  )
  case class IApiResponseBody (
    contentType: ApiContentType,
    schema: IApiBodySchema,
  )
  case class IApiRequest (
    headerParameters: Seq[IApiRequestParameter],
    pathParameters: Seq[IApiRequestParameter],
    queryParameters: Seq[IApiRequestParameter],
    bodies: Seq[IApiRequestBody],
    requestId: RequestId,
    purpose: String
  )
  case class IApiResponse (
    statusCode: ApiStatusCode,
    bodies: Seq[IApiResponseBody]
  )
  case class IApiEndpoint (
    path: ApiPath,
    method: ApiRequestMethod,
    request: IApiRequest,
    responses: Seq[IApiResponse]
  )
  case class IApiSnapshot (
    endpoints: Seq[IApiEndpoint]
  )
  case class IApiArtifactGeneratorData (
    apiSnapshot: IApiSnapshot
  )

  //@TODO: support contributions and examples, etc.
  def asCogentApiArtifactRepresentation(queries: InMemoryQueries): IApiArtifactGeneratorData = {
    val requestsState = queries.requestsState
    val endpoints = requestsState.requests.values
      .map(requestEntity => {
        val path = queries.absolutePath(requestEntity.requestDescriptor.pathComponentId)
        val method = requestEntity.requestDescriptor.httpMethod
        val bodies = requestEntity.requestDescriptor.bodyDescriptor match {
          case Commands.UnsetBodyDescriptor() => Seq.empty
          case body: Commands.ShapedBodyDescriptor => {
            Seq(
              IApiRequestBody(
                body.httpContentType,
                IApiBodySchema(JsonObject.empty.asJson)
              )
            )
          }
        }

        val requestParameters = requestsState.requestParameters
          .filter(requestParameterEntry => {
            val (requestParameterId, requestParameterEntity) = requestParameterEntry
            requestParameterEntity.requestParameterDescriptor.requestId == requestEntity.requestId
          }).values

        val queryParamShape: Option[FlatShapeProjection.FlatShapeResult] = queries.requestsState.requestParameters.filter(x => {
          val (parameterId, parameter) = x
          parameter.requestParameterDescriptor.requestId == requestEntity.requestId && parameter.requestParameterDescriptor.location == "query"
        }).values.headOption.flatMap(query => {
          query.requestParameterDescriptor.shapeDescriptor match {
            case c: Commands.UnsetRequestParameterShapeDescriptor => {
              None
            }
            case c: Commands.ShapedRequestParameterShapeDescriptor => {
              Some(FlatShapeProjection.forShapeId(c.shapeId)(queries.shapesState))
            }
          }
        })

        val queryParamFields = queryParamShape.map(_.root.fields).getOrElse(Seq.empty)
        val queryParameters = queryParamFields.map(i => {
          val innerOption = i.shape.links.get(OptionalKind.innerParam)
          if (innerOption.isDefined) {
            //is optional
            IApiRequestParameter(i.fieldName, false, new JsonSchemaProjection(innerOption.get)(queries.shapesState).asJsonSchema(true))
          } else {
            //is required
            IApiRequestParameter(i.fieldName, true, new JsonSchemaProjection(i.shape.id)(queries.shapesState).asJsonSchema(true))
          }
        })

        val headerParameters = requestParameters
          .filter(x => x.requestParameterDescriptor.location == "header")
          .map(x => IApiRequestParameter(x.requestParameterDescriptor.name, false, JsonSchemaHelpers.defaultStringType))
          .toSeq

        val pathParents = Utilities.parents(requestEntity.requestDescriptor.pathComponentId, requestsState.pathComponents)
        val pathParameters = pathParents
          .filter(x => {
            x match {
              case d: Commands.ParameterizedPathComponentDescriptor => true
              case d: Commands.BasicPathComponentDescriptor => false
            }
          })
          .map(x => IApiRequestParameter(x.name, true, JsonSchemaHelpers.defaultStringType))
        val request = IApiRequest(
          headerParameters,
          pathParameters,
          queryParameters,
          bodies,
          requestEntity.requestId,
          queries.contributions.get(requestEntity.requestId, "purpose").getOrElse("")
        )

        val responses = requestsState
          .responses
          .filter(responseEntry => {
            val (responseId, responseEntity) = responseEntry
            responseEntity.responseDescriptor.requestId == requestEntity.requestId
          })
          .values
          .map(responseEntity => {
            val statusCode = responseEntity.responseDescriptor.httpStatusCode
            val bodies = responseEntity.responseDescriptor.bodyDescriptor match {
              case Commands.UnsetBodyDescriptor() => {
                Seq.empty
              }
              case body: Commands.ShapedBodyDescriptor => {
                Seq(
                  IApiResponseBody(
                    body.httpContentType,
                    IApiBodySchema(JsonObject.empty.asJson)
                  )
                )
              }
            }
            IApiResponse(
              statusCode,
              bodies
            )
          }).toSeq
        IApiEndpoint(
          path,
          method,
          request,
          responses
        )
    }).toSeq
    val apiSnapshot = IApiSnapshot(endpoints)
    IApiArtifactGeneratorData(apiSnapshot)
  }


  def toJs(input: IApiArtifactGeneratorData): js.Any = {
    import io.circe.scalajs.convertJsonToJs

    convertJsonToJs(input.asJson)
  }
}

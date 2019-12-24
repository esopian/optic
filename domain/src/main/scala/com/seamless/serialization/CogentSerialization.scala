package com.seamless.serialization


import com.seamless.contexts.requests.{Commands, Utilities}
import com.seamless.contexts.rfc.InMemoryQueries

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
    name: String
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

        //@TODO: there will only ever be at most 1 query parameter. If it is present, it is an object. each key represents an actual query parameter and its parsed type. should only be either a string or an array of strings
        val queryParameters = requestParameters
          .filter(x => x.requestParameterDescriptor.location == "query")
          .map(x => IApiRequestParameter(x.requestParameterDescriptor.name))
          .toSeq

        val headerParameters = requestParameters
          .filter(x => x.requestParameterDescriptor.location == "header")
          .map(x => IApiRequestParameter(x.requestParameterDescriptor.name))
          .toSeq

        val pathParents = Utilities.parents(requestEntity.requestDescriptor.pathComponentId, requestsState.pathComponents)
        val pathParameters = pathParents
          .filter(x => {
            x match {
              case d: Commands.ParameterizedPathComponentDescriptor => true
              case d: Commands.BasicPathComponentDescriptor => false
            }
          })
          .map(x => IApiRequestParameter(x.name))
        val request = IApiRequest(
          headerParameters,
          pathParameters,
          queryParameters,
          bodies
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

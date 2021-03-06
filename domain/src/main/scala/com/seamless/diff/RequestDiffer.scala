package com.seamless.diff

import com.seamless.contexts.requests.Commands._
import com.seamless.contexts.requests._
import com.seamless.contexts.rfc.RfcState
import com.seamless.contexts.shapes.ShapesState
import com.seamless.diff.ShapeDiffer.ShapeDiffResult
import io.circe._
import io.circe.literal._
import io.circe.generic.auto._
import io.circe.syntax._
import com.seamless.diff.query.{JvmQueryStringParser, QueryStringDiffer, QueryStringParser}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import scala.util.{Failure, Success, Try}

@JSExport
case class ApiRequest(url: String, method: String, queryString: String, contentType: String, body: Option[Json] = None)

@JSExport
case class ApiResponse(statusCode: Int, contentType: String, body: Option[Json] = None)

@JSExport
case class ApiInteraction(apiRequest: ApiRequest, apiResponse: ApiResponse)

@JSExport
@JSExportAll
object JsonHelper {

  import js.JSConverters._

  def fromString(s: String): Json = {
    import io.circe.parser._
    Try {
      parse(s).right.get
    } match {
      case Failure(exception) => {
        println(exception)
        Json.Null
      }
      case Success(value) => value
    }
  }

  def toSome(x: Json): Option[Json] = Some(x)

  def toNone(): Option[Json] = None

  //def fromAny(any: js.Any): Json = any.asJson

  def seqToJsArray(x: Seq[AnyVal]): js.Array[AnyVal] = {
    x.toJSArray
  }

  def iteratorToJsIterator(x: Iterator[AnyVal]): js.Iterator[AnyVal] = {
    x.toJSIterator
  }
}
object PluginRegistryUtilities {
  def defaultPluginRegistry(shapesState: ShapesState) = {
    val parser = new JvmQueryStringParser(JsonObject(("f1", JsonNumber.fromIntegralStringUnsafe("1").asJson)).asJson)
    val differ = new QueryStringDiffer(shapesState, parser)
    PluginRegistry(differ)
  }
}

@JSExport
@JSExportAll
case class PluginRegistry(queryStringDiffer: QueryStringDiffer)

@JSExport
@JSExportAll
object RequestDiffer {

  @JSExportAll
  sealed trait RequestDiffResult {
    def asJs = {
      import io.circe.scalajs.convertJsonToJs
      convertJsonToJs(this.asJson)
    }
  }
  case class NoDiff() extends RequestDiffResult
  case class UnmatchedUrl(interaction: ApiInteraction) extends RequestDiffResult
  case class UnmatchedHttpMethod(pathId: PathComponentId, method: String, interaction: ApiInteraction) extends RequestDiffResult
  case class UnmatchedHttpStatusCode(requestId: RequestId, statusCode: Int, interaction: ApiInteraction) extends RequestDiffResult
  case class UnmatchedResponseContentType(responseId: ResponseId, contentType: String, previousContentType: String, statusCode: Int) extends RequestDiffResult
  case class UnmatchedResponseBodyShape(responseId: ResponseId, contentType: String, responseStatusCode: Int, shapeDiff: ShapeDiffResult) extends RequestDiffResult
  case class UnmatchedRequestBodyShape(requestId: RequestId, contentType: String, shapeDiff: ShapeDiffResult) extends RequestDiffResult
  case class UnmatchedRequestContentType(requestId: RequestId, contentType: String, previousContentType: String) extends RequestDiffResult
  case class UnmatchedQueryParameterShape(requestId: RequestId, parameterId: RequestParameterId, shapeDiff: ShapeDiffResult, actual: Json) extends RequestDiffResult

  case class PipelineItem[T](item: Option[T], results: Iterator[RequestDiffResult])

  def compare(interaction: ApiInteraction, spec: RfcState, plugins: PluginRegistry): Iterator[RequestDiffResult] = {
    val pathPipeline = pathDiff(interaction, spec)
    pathPipeline.item match {
      case None => pathPipeline.results
      case Some(pathId) => {
        val requestPipeline = requestDiff(interaction, spec, pathId)
        requestPipeline.item match {
          case None => pathPipeline.results ++ requestPipeline.results
          case Some(request) => {
            val queryParameterPipeline = queryParameterDiff(interaction, spec, plugins, request)
            val responsePipeline = responseDiff(interaction, spec, request)
            queryParameterPipeline.results ++ requestPipeline.results ++ responsePipeline.results
          }
        }
      }
    }
  }

  def pathDiff(interaction: ApiInteraction, spec: RfcState): PipelineItem[PathComponentId] = {
    val matchedPath = Utilities.resolvePath(interaction.apiRequest.url, spec.requestsState.pathComponents)

    if (matchedPath.isEmpty) {
      return PipelineItem(None, Iterator(UnmatchedUrl(interaction)))
    }
    PipelineItem(matchedPath, Iterator())
  }

  def shouldTrustRequest(interaction: ApiInteraction): Boolean = {
    (200 until 400) contains interaction.apiResponse.statusCode
  }

  def queryParameterDiff(interaction: ApiInteraction, spec: RfcState, plugins: PluginRegistry, request: HttpRequest): PipelineItem[Any] = {
    if (shouldTrustRequest(interaction)) {
      val queryParameterWrapper = spec.requestsState.requestParameters
        .filter(x => {
          val (parameterId, parameter) = x
          println(x)
          parameter.requestParameterDescriptor.requestId == request.requestId && parameter.requestParameterDescriptor.location == "query"
        })
        .values.headOption
      return queryParameterWrapper match {
        case Some(value) => {
          val diff = plugins.queryStringDiffer.diff(value, interaction.apiRequest.queryString)
          PipelineItem(None, diff)
        }
        case None => {
          PipelineItem(None, Iterator.empty)
        }
      }

    }
    PipelineItem(None, Iterator.empty)
  }

  def requestDiff(interaction: ApiInteraction, spec: RfcState, pathId: PathComponentId): PipelineItem[HttpRequest] = {

    val matchedOperation = spec.requestsState.requests.values
      .find(r => r.requestDescriptor.pathComponentId == pathId && r.requestDescriptor.httpMethod == interaction.apiRequest.method)

    if (matchedOperation.isEmpty) {
      return PipelineItem(None, Iterator(UnmatchedHttpMethod(pathId, interaction.apiRequest.method, interaction)))
    }

    val request = matchedOperation.get
    if (shouldTrustRequest(interaction)) {

      // request body
      val requestDiff: Option[Iterator[RequestDiffResult]] = request.requestDescriptor.bodyDescriptor match {
        case d: UnsetBodyDescriptor => {
          if (interaction.apiRequest.body.isEmpty) {
            None
          } else {
            Some(Iterator(UnmatchedRequestBodyShape(request.requestId, interaction.apiRequest.contentType, ShapeDiffer.UnsetShape(interaction.apiRequest.body.get))))
          }
        }
        case d: ShapedBodyDescriptor => {
          if (d.httpContentType == interaction.apiRequest.contentType) {
            val shape = spec.shapesState.shapes(d.shapeId)
            val shapeDiff = ShapeDiffer.diff(shape, interaction.apiRequest.body)(spec.shapesState)
            if (shapeDiff.isEmpty) {
              None
            } else {
              Some(shapeDiff.map(d => UnmatchedRequestBodyShape(request.requestId, interaction.apiRequest.contentType, d)))
            }
          } else {
            Some(Iterator(UnmatchedRequestContentType(request.requestId, interaction.apiRequest.contentType, d.httpContentType)))
          }
        }
      }
      if (requestDiff.isDefined) {
        return PipelineItem(None, requestDiff.get)
      }
    }
    PipelineItem(matchedOperation, Iterator.empty)
  }


  def responseDiff(interaction: ApiInteraction, spec: RfcState, operation: HttpRequest): PipelineItem[HttpResponse] = {

    // check for matching response status
    val matchedResponse = spec.requestsState.responses.values
      .find(r => r.responseDescriptor.requestId == operation.requestId && r.responseDescriptor.httpStatusCode == interaction.apiResponse.statusCode)

    if (matchedResponse.isEmpty) {
      return PipelineItem(None, Iterator(UnmatchedHttpStatusCode(operation.requestId, interaction.apiResponse.statusCode, interaction)))
    }

    val responseId = matchedResponse.get.responseId;
    val responseDiff: Option[Iterator[RequestDiffResult]] = matchedResponse.get.responseDescriptor.bodyDescriptor match {
      case d: UnsetBodyDescriptor => {
        if (interaction.apiResponse.body.isEmpty) {
          None
        } else {
          Some(
            Iterator(
              UnmatchedResponseBodyShape(
                responseId,
                interaction.apiResponse.contentType,
                interaction.apiResponse.statusCode,
                ShapeDiffer.UnsetShape(interaction.apiResponse.body.get)
              )
            )
          )
        }
      }
      case d: ShapedBodyDescriptor => {
        if (d.httpContentType == interaction.apiResponse.contentType) {
          val shape = spec.shapesState.shapes(d.shapeId)
          val shapeDiff = ShapeDiffer.diff(shape, interaction.apiResponse.body)(spec.shapesState)
          if (shapeDiff.isEmpty) {
            None
          } else {
            Some(shapeDiff.map(d => UnmatchedResponseBodyShape(responseId, interaction.apiResponse.contentType, interaction.apiResponse.statusCode, d)))
          }
        } else {
          Some(Iterator(UnmatchedResponseContentType(matchedResponse.get.responseId, interaction.apiResponse.contentType, d.httpContentType, interaction.apiResponse.statusCode)))
        }
      }
    }

    if (responseDiff.isDefined) {
      return PipelineItem(matchedResponse, responseDiff.get)
    }

    PipelineItem(matchedResponse, Iterator.empty)
  }
}



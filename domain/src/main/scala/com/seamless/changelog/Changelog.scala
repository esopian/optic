package com.seamless.changelog

import com.seamless.changelog.Changelog.{AddedRequest, ResponsesChangelog}
import com.seamless.contexts.requests.Commands.RequestId

object Changelog {
  case class AddedRequest(path: String, method: String, requestId: RequestId)
  case class AddedStatusCode(statusCode: Int, requestId: RequestId)
  case class RemovedStatusCode(statusCode: Int, requestId: RequestId)

  case class ResponsesChangelog(addedStatusCodes: Vector[AddedStatusCode],
                                removedStatusCodes: Vector[RemovedStatusCode],
                                updatedStatusCodes: Map[Int, ResponseChangelog])

  case class ResponseChangelog(
                                updatedContentType: Option[String] = None,
                                bodyAdded: Boolean = false,
                                bodyRemoved: Boolean = false,
                                bodyShapeDiff: Vector[String] = Vector()
                              ) {
    def noDiff: Boolean = updatedContentType.isEmpty && !bodyAdded && !bodyRemoved && bodyShapeDiff.isEmpty

  }

}
case class Changelog(addedRequest: Vector[AddedRequest], responseChangeLog: ResponsesChangelog)

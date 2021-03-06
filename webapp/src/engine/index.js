import * as opticEngine from 'optic-domain'

export const ShapesCommands = opticEngine.com.seamless.contexts.shapes.Commands
export const ShapesHelper = opticEngine.com.seamless.contexts.shapes.ShapesHelper()
export const RequestsHelper = opticEngine.com.seamless.contexts.requests.RequestsServiceHelper()
export const ContentTypesHelper = opticEngine.com.seamless.contexts.requests.ContentTypes()
export const NaiveSummary = opticEngine.com.seamless.diff.NaiveSummary()

export const RfcCommands = opticEngine.com.seamless.contexts.rfc.Commands
export const RequestsCommands = opticEngine.com.seamless.contexts.requests.Commands
export const RfcCommandContext = opticEngine.com.seamless.contexts.rfc.RfcCommandContext
export const ScalaJSHelpers = opticEngine.ScalaJSHelpers

export const Facade = opticEngine.com.seamless.contexts.rfc.RfcServiceJSFacade()
export const Queries = (eventStore, service, aggregateId) => new opticEngine.Queries(eventStore, service, aggregateId)


export function commandsToJson(commands) {
    return commands.map(x => JSON.parse(opticEngine.CommandSerialization.toJsonString(x)))
}
export function commandsFromJson(commands) {
    return opticEngine.CommandSerialization.fromJsonString(JSON.stringify(commands))
}
export function commandsToJs(commandSequence) {
    return opticEngine.CommandSerialization.toJs(commandSequence)
}
export function commandToJs(command) {
    return opticEngine.CommandSerialization.toJs(command)
}

export const JsonHelper = opticEngine.com.seamless.diff.JsonHelper()
function fromJs(x) {
    if (typeof x === 'undefined') {
        return JsonHelper.toNone()
    }
    return JsonHelper.toSome(JsonHelper.fromString(JSON.stringify(x)))
}

export const mapScala = (collection) => (handler) => {
    return ScalaJSHelpers.toJsArray(collection).map(handler)
}

export const everyScala = (collection) => (handler) => {
    return ScalaJSHelpers.toJsArray(collection).every(handler)
}
export const lengthScala = (collection) => {
    return ScalaJSHelpers.toJsArray(collection).length
}

const { ApiInteraction, ApiRequest, ApiResponse } = opticEngine.com.seamless.diff;
export function toInteraction(sample) {
    return ApiInteraction(
        ApiRequest(sample.request.url, sample.request.method, sample.queryString || '', sample.request.headers['content-type'] || '*/*', fromJs(sample.request.body)),
        ApiResponse(sample.response.statusCode, sample.response.headers['content-type'] || '*/*', fromJs(sample.response.body))
    )
}
export const InteractionDiffer = opticEngine.com.seamless.diff.InteractionDiffer;
export const RequestDiffer = opticEngine.com.seamless.diff.RequestDiffer()
export const Interpreters = opticEngine.com.seamless.diff.interpreters
export const PluginRegistry = opticEngine.com.seamless.diff.PluginRegistry
export const QueryStringDiffer = opticEngine.com.seamless.diff.query.QueryStringDiffer
export const { JsQueryStringParser } = opticEngine
console.log(opticEngine)
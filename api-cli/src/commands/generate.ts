import Command from '@oclif/command'
import { getPaths, IPathMapping } from '../Paths'
import * as fs from 'fs-extra'
// @ts-ignore
import * as opticEngine from '../../provided/domain.js'
import { ApiArtifactGenerator } from '@useoptic/cogent'
import { IApiArtifactGeneratorProps } from '@useoptic/cogent/dist/src/api-artifact-generators'
import { ICogentComponentRegistry } from '@useoptic/cogent/dist/src/component-registries'
import { FileSystemReconciler } from '@useoptic/cogent/dist/src/react-fs/file-system-reconciler'
import { JavascriptApiClient } from '@useoptic/cogent/dist/src/api-artifact-generators/javascript-api-client'
import * as util from 'util';
import { IFileSystemRendererFolder } from '@useoptic/cogent/dist/src/react-fs/file-system-renderer'

export default class Generate extends Command {

    //@TODO: add optional args for hosts?
    static args = [
    ]
    static flags = {
    }

    async makeOpticApiRepresentation(paths: IPathMapping) {
        const { specStorePath } = paths
        const specStoreExists = await fs.pathExists(specStorePath)
        const eventsAsString = specStoreExists ? (await fs.readFile(specStorePath)).toString() : '[]'
        const Facade = opticEngine.com.seamless.contexts.rfc.RfcServiceJSFacade()
        const RfcCommandContext = opticEngine.com.seamless.contexts.rfc.RfcCommandContext
        const rfcId = 'testRfcId'
        const clientId = 'cli-readonly'
        const sessionId = 'api-generate'
        const batchId = 'api-generate-init'
        const commandContext = new RfcCommandContext(clientId, sessionId, batchId)

        const eventStore = Facade.makeEventStore()
        eventStore.bulkAdd(rfcId, eventsAsString)
        const rfcService = Facade.fromJsonCommands(eventStore, rfcId, '[]', commandContext)
        const Queries = (eventStore: any, service: any, aggregateId: string) => new opticEngine.Queries(eventStore, service, aggregateId)
        const queries = Queries(eventStore, rfcService, rfcId)
        return {
            queries
        }
    }

    async makeCogentApiArtifactGeneratorRepresentation(paths: IPathMapping) {
        const opticApiRepresentation = await this.makeOpticApiRepresentation(paths)
        const CogentSerialization = opticEngine.com.seamless.serialization.CogentSerialization()
        const cogentApiArtifactGeneratorRepresentation = CogentSerialization.asCogentApiArtifactRepresentation(opticApiRepresentation.queries.queries())
        const representation = CogentSerialization.toJs(cogentApiArtifactGeneratorRepresentation)
        console.log(util.inspect(representation, { colors: true, depth: 100 }))
        return representation
    }

    async generateArtifacts(artifacts: any[], representation: any) {
        const registry = new ApiArtifactRegistry()
        const generator = new ApiArtifactGenerator()
        const output: IFileSystemRendererFolder = await new Promise((resolve, reject) => {
            generator.run({
                data: representation,
                callback: (err, result) => {
                    if (err) {
                        return reject(err)
                    }
                    if (result) {
                        return resolve(result)
                    }
                    reject(new Error('something went wrong'))
                },
                metadata: {
                    api: {
                        id: 'my-api',
                        version: '0.1.0'
                    },
                    artifact: {
                        id: 'my-artifact',
                        version: '0.1.0'
                    }
                }
            }, registry)
        })
        console.log(util.inspect(output, { colors: true, depth: 100 }))
        const reconciler = new FileSystemReconciler();
        reconciler.emit(output, { outputDirectory: './generated' })
    }

    async run() {
        const paths = await getPaths()
        const representation = await this.makeCogentApiArtifactGeneratorRepresentation(paths)
        await this.generateArtifacts([
            { name: 'javascript-api-client', config: {} }
        ], representation)
    }
}

class ApiArtifactRegistry implements ICogentComponentRegistry<IApiArtifactGeneratorProps> {
    resolve(id: string): React.ComponentType<IApiArtifactGeneratorProps> | null {
        return JavascriptApiClient
    }
}
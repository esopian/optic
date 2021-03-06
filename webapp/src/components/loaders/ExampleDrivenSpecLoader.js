import React, { useState, useEffect } from 'react';
import { LocalDiffRfcStore, withRfcContext } from '../../contexts/RfcContext';
import { notificationAreaComponent, shareButtonComponent } from './SharedLoader.js';
import { basePaths } from '../../RouterPaths';
import CompareArrowsIcon from '@material-ui/icons/CompareArrows';
import QueueIcon from '@material-ui/icons/Queue';
import FiberNewIcon from '@material-ui/icons/FiberNew';
import {
  TextField,
  Button,
  Paper,
  Select,
  Grid,
  Typography,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@material-ui/core';
import useForm from 'react-hook-form';
import { STATUS_CODES } from 'http';
import { InteractionDiffer, toInteraction } from '../../engine';
import Dialog from '@material-ui/core/Dialog';
import DialogContentText from '@material-ui/core/DialogContentText';
import { DiffDocGrid } from '../requests/DocGrid';
import { DocSubGroup } from '../requests/DocSubGroup';
import JsonTextarea from '../shared/JsonTextarea';
import { DocDivider } from '../requests/DocConstants';
import Chip from '@material-ui/core/Chip';
import jsonic from 'jsonic';
import { queryStringDiffer } from '../diff/DiffUtilities';
import IconButton from '@material-ui/core/IconButton';
import { LightTooltip } from '../tooltips/LightTooltip';
import { LoaderFactory, SpecServiceContext } from './LoaderFactory';

export const basePath = basePaths.exampleDrivenSpecBasePath

function parseLoosely(nonEmptyBodyString) {
  if (!nonEmptyBodyString) {
    return [true];
  }
  try {
    return [true, JSON.parse(nonEmptyBodyString)];
  } catch (e) {
    try {
      const result = jsonic(nonEmptyBodyString);
      console.log({ result });
      if (result) {
        return [true, result];
      }
      return [false];
    } catch (e) {
      return [false];
    }
  }
}

export function DialogWrapper(props) {
  const [showExampleBuilder, setShowExampleBuilder] = useState(false);
  const { specService, onSampleAdded, children } = props;
  function handleSampleAdded(sample) {
    onSampleAdded(sample)
    setShowExampleBuilder(false)
  }
  return (
    <>
      <LightTooltip title="Manually Add Example">
        <IconButton color="primary" size="small" onClick={() => setShowExampleBuilder(true)}>
          <QueueIcon />
        </IconButton>
      </LightTooltip>
      <Dialog
        fullWidth={true}
        maxWidth={'xl'}
        fullScreen={true}
        open={showExampleBuilder}
        onClose={() => setShowExampleBuilder(false)}
      >
        <ExampleBuilder onSampleAdded={handleSampleAdded} specService={specService} />
      </Dialog>
      {children}
    </>
  )
}

function ExampleBuilderBase(props) {

  const { register, handleSubmit, setValue, getValues, watch } = useForm({
    defaultValues: {
      request: {
        method: 'GET',
        url: '/'
      },
      response: {
        statusCode: 200
      }
    },
  });


  // need watch() to update getValues() on change
  const watchAll = watch();
  const formValues = getValues({ nest: true });

  const parseFormState = state => {
    console.log({ state });
    const [parsedRequestBodySuccess, parsedRequestBody] = parseLoosely(state.request.body);
    const request = {
      method: state.request.method,
      queryParameters: {},
      url: state.request.url || '/',
      headers: {}
    };
    if (parsedRequestBodySuccess && parsedRequestBody) {
      request.headers['content-type'] = 'application/json';
      request.body = parsedRequestBody;
    }


    const [parsedResponseBodySuccess, parsedResponseBody] = parseLoosely(state.response.body);
    const response = {
      statusCode: parseInt(state.response.statusCode, 10),
      headers: {},
    };
    if (parsedResponseBodySuccess && parsedResponseBody) {
      response.headers['content-type'] = 'application/json';
      response.body = parsedResponseBody;
    }


    const sample = {
      request,
      response
    };


    const { rfcService, rfcId } = props;
    const rfcState = rfcService.currentState(rfcId);
    const interactionDiffer = new InteractionDiffer(rfcState);
    const interaction = toInteraction(sample);
    const hasUnrecognizedPath = interactionDiffer.hasUnrecognizedPath(interaction);
    const hasDiff = interactionDiffer.hasDiff(interaction, queryStringDiffer(rfcState.shapesState, sample));
    const result = {
      state,
      sample,
      hasUnrecognizedPath,
      hasDiff,
      parsedRequestBodySuccess,
      parsedResponseBodySuccess,
    };
    console.log(result);
    return result;
  };
  const onSubmit = data => {
    const { onSampleAdded } = props;
    const { sample } = parseFormState(data);
    onSampleAdded(sample);
  };
  const {
    hasDiff, hasUnrecognizedPath, parsedRequestBodySuccess, parsedResponseBodySuccess
  } = parseFormState(formValues);

  useEffect(() => {
    register({ name: "request.body" });
    register({ name: "response.body" });
  }, []);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <DialogTitle>
        Add Example
          </DialogTitle>
      <DialogContent>
        <DialogContentText>
          Design your API by example. Optic will use the examples to guide you through the development of your API
          specification.
            </DialogContentText>

        <DiffDocGrid
          colMaxWidth={'100%'}
          style={{ paddingBottom: 200 }}
          left={(
            <>
              <Typography variant="h4" color="primary">Request</Typography>
              <DocSubGroup title="URL">
                <div style={{ display: 'flex', marginTop: 11 }}>
                  <Select native label="Method" name="request.method" inputRef={register}>
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                  </Select>

                  <TextField fullWidth label="URL" name="request.url" inputRef={register} autoFocus
                    autoComplete="off" style={{ marginLeft: 25 }} />

                </div>
              </DocSubGroup>

              <DocSubGroup title="Request Body">
                <JsonTextarea
                  onChange={(value) => {
                    setValue('request.body', value)
                  }}
                  value={formValues.request.body}
                />
              </DocSubGroup>
            </>
          )}
          right={(
            <>
              <Typography variant="h4" color="primary">Response</Typography>
              <DocSubGroup title="Status Code">
                <div style={{ marginTop: 22 }}>
                  <Select native label="Status Code" name="response.statusCode" inputRef={register}>
                    {Object.entries(STATUS_CODES).map(entry => {
                      const [code, message] = entry;
                      return (
                        <option value={code}>{code}: {message}</option>
                      );
                    })}
                  </Select>
                </div>
              </DocSubGroup>
              <DocSubGroup title="Response Body">
                <JsonTextarea
                  onChange={(value) => {
                    setValue('response.body', value)
                  }}
                  value={formValues.response.body}
                />
              </DocSubGroup>
            </>
          )}
        />
        <DialogActions style={{ zIndex: 1000, display: 'flex', flexDirection: 'row', position: 'fixed', left: 0, width: '100%', bottom: 0, backgroundColor: 'rgb(250, 250, 250)' }}>
          <DocDivider style={{ marginTop: 22 }} />
          <div>
            {hasUnrecognizedPath ? <Chip
              style={{ margin: 6 }}
              color="secondary"
              icon={<FiberNewIcon />}
              label="This URL Path is new."
            /> : null}
            {hasDiff ? <Chip
              style={{ margin: 6 }}
              color="secondary"
              icon={<CompareArrowsIcon />}
              label="This example produces a diff"
            /> : null}

          </div>

          <div style={{ flex: 1 }} />
          <Button
            color="primary"
            variant="contained"
            type="submit"
            disabled={!parsedRequestBodySuccess || !parsedResponseBodySuccess}>Add
                  Example</Button>
        </DialogActions>
      </DialogContent>
    </form>
  );
}

const ExampleBuilder = withRfcContext(ExampleBuilderBase);

const specServiceTask = () => {
  const sessionId = 'manual-session';
  let eventsString = '[]'
  let session = {
    samples: []
  }
  const examples = {}
  const specService = {
    loadSession: (sessionId) => {
      return Promise.resolve({
        diffStateResponse: {
          diffState: {}
        },
        sessionResponse: {
          session
        }
      });
    },
    listEvents() {
      return Promise.resolve(eventsString);
    },
    listSessions() {
      return Promise.resolve({ sessions: [sessionId] });
    },
    saveEvents: (eventStore, rfcId) => {
      const events = eventStore.serializeEvents(rfcId);
      eventsString = events;
    },
    listExamples: (requestId) => {
      return Promise.resolve({ examples: examples[requestId] || [] });
    },
    saveExample: (interaction, requestId) => {
      const requestExamples = examples[requestId] || [];
      requestExamples.push(interaction);
      examples[requestId] = requestExamples;
    },
    saveDiffState: () => {
    },
    handleSampleAdded(sample) {
      session = {
        ...session,
        samples: [...session.samples, sample]
      }
    }
  };
  return Promise.resolve(specService)
}

const addExampleComponent = (
  <SpecServiceContext.Consumer>
    {(context) => {
      const { specService } = context;

      return (
        <DialogWrapper
          specService={specService}
          onSampleAdded={(sample) => {
            specService.handleSampleAdded(sample)
          }} />
      )
    }}
  </SpecServiceContext.Consumer>

)

const {
  Routes: ExampleDrivenSpecLoaderRoutes
} = LoaderFactory.build({
  specServiceTask,
  notificationAreaComponent,
  addExampleComponent,
  shareButtonComponent,
  basePath,
  RfcStoreImpl: LocalDiffRfcStore
})

export default ExampleDrivenSpecLoaderRoutes;

/**
 * This file contains all redux actions that can be executed on events
 */

// Constants of actions types for fetching events from server
export const LOAD_EVENTS_IN_PROGRESS = 'LOAD_EVENTS_IN_PROGRESS';
export const LOAD_EVENTS_SUCCESS = 'LOAD_EVENTS_SUCCESS';
export const LOAD_EVENTS_FAILURE = 'LOAD_EVENTS_FAILURE';

// Constants of actions types affecting UI
export const SHOW_ACTIONS = 'SHOW_ACTIONS';

// Constants of action types affecting fetching of event metadata from server
export const LOAD_EVENT_METADATA_IN_PROGRESS = 'LOAD_EVENT_METADATA_IN_PROGRESS';
export const LOAD_EVENT_METADATA_SUCCESS = 'LOAD_EVENT_METADATA_SUCCESS';
export const LOAD_EVENT_METADATA_FAILURE = 'LOAD_EVENT_METADATA_FAILURE';

// Actions affecting fetching of events from server

export const loadEventsInProgress = () => ({
    type: LOAD_EVENTS_IN_PROGRESS
});

export const loadEventsSuccess = events => ({
    type: LOAD_EVENTS_SUCCESS,
    payload: { events }
});

export const loadEventsFailure = () => ({
    type: LOAD_EVENTS_FAILURE
});

// Actions affecting UI

export const showActions = isShowing => ({
    type: SHOW_ACTIONS,
    payload: { isShowing }
});

// Actions affecting fetching of event metadata from server

export const loadEventMetadataInProgress = () => ({
    type: LOAD_EVENT_METADATA_IN_PROGRESS
});

export const loadEventMetadataSuccess = metadata => ({
    type: LOAD_EVENT_METADATA_SUCCESS,
    payload: { metadata }
});

export const loadEventMetadataFailure = () => ({
    type: LOAD_EVENT_METADATA_FAILURE
});

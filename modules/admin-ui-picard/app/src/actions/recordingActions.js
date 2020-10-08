/**
 * This file contains all redux actions that can be executed on recordings
 */

// Constants of action types for fetching recordings from server
export const LOAD_RECORDINGS_IN_PROGRESS = 'LOAD_RECORDINGS_IN_PROGRESS';
export const LOAD_RECORDINGS_SUCCESS = 'LOAD_RECORDINGS_SUCCESS';
export const LOAD_RECORDINGS_FAILURE = 'LOAD_RECORDINGS_FAILURE';

// Actions affecting fetching recordings from server

export const loadRecordingsInProgress = () => ({
    type: LOAD_RECORDINGS_IN_PROGRESS
});

export const loadRecordingsSuccess = recordings => ({
    type: LOAD_RECORDINGS_SUCCESS,
    payload: { recordings }
});

export const loadRecordingsFailure = () => ({
    type: LOAD_RECORDINGS_FAILURE,
});

/**
 * This file contains all redux actions that can be executed on a certain recording/capture agent
 */

// Constants of action types for fetching details of a certain recording/capture agent from server
export const LOAD_RECORDING_DETAILS_IN_PROGRESS = 'LOAD_RECORDING_DETAILS_IN_PROGRESS';
export const LOAD_RECORDING_DETAILS_SUCCESS = 'LOAD_RECORDING_DETAILS_SUCCESS';
export const LOAD_RECORDING_DETAILS_FAILURE = 'LOAD_RECORDING_DETAILS_FAILURE';

// Actions affecting fetching details of a certain recording/capture agent from server

export const loadRecordingDetailsInProgress = () => ({
    type: LOAD_RECORDING_DETAILS_IN_PROGRESS
});

export const loadRecordingDetailsSuccess = recordingDetails => ({
    type: LOAD_RECORDING_DETAILS_SUCCESS,
    payload: { recordingDetails }
});

export const loadRecordingDetailsFailure = () => ({
    type: LOAD_RECORDING_DETAILS_FAILURE
});

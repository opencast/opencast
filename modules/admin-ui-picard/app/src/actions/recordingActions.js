/**
 * This file contains all redux actions that can be executed on recordings
 */

// Constants of action types for fetching recordings from server
export const LOAD_RECORDINGS_IN_PROGRESS = "LOAD_RECORDINGS_IN_PROGRESS";
export const LOAD_RECORDINGS_SUCCESS = "LOAD_RECORDINGS_SUCCESS";
export const LOAD_RECORDINGS_FAILURE = "LOAD_RECORDINGS_FAILURE";

// Constants of action types affecting UI
export const SET_RECORDINGS_COLUMNS = "SET_RECORDINGS_COLUMNS";

// Actions affecting fetching recordings from server

export const loadRecordingsInProgress = () => ({
	type: LOAD_RECORDINGS_IN_PROGRESS,
});

export const loadRecordingsSuccess = (recordings) => ({
	type: LOAD_RECORDINGS_SUCCESS,
	payload: { recordings },
});

export const loadRecordingsFailure = () => ({
	type: LOAD_RECORDINGS_FAILURE,
});

// Actions affecting UI

export const setRecordingsColumns = (updatedColumns) => ({
	type: SET_RECORDINGS_COLUMNS,
	payload: { updatedColumns },
});

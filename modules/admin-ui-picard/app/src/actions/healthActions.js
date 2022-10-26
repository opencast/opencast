/**
 * This file contains all redux actions concerning the health status shown in header
 */

// Constants of action types for fetching health status from server and loading it into state
export const LOAD_HEALTH_STATUS = "LOAD_HEALTH_STATUS";
export const LOAD_STATUS_IN_PROGRESS = "LOAD_STATUS_IN_PROGRESS";
export const LOAD_STATUS_FAILURE = "LOAD_STATUS_FAILURE";

// Constants of action types for setting information about errors
export const SET_ERROR = "SET_ERROR";
export const ADD_NUM_ERROR = "ADD_NUM_ERROR";
export const RESET_NUM_ERROR = "RESET_NUM_ERROR";

// Actions affecting fetching health status from server and loading it into state

export const loadHealthStatus = (healthStatus) => ({
	type: LOAD_HEALTH_STATUS,
	payload: { healthStatus },
});

export const loadStatusInProgress = () => ({
	type: LOAD_STATUS_IN_PROGRESS,
});

export const loadStatusFailure = () => ({
	type: LOAD_STATUS_FAILURE,
});

// Actions affecting setting information about errors

export const setError = (isError) => ({
	type: SET_ERROR,
	payload: { isError },
});

export const addNumError = (numError) => ({
	type: ADD_NUM_ERROR,
	payload: { numError },
});

export const resetNumError = () => ({
	type: RESET_NUM_ERROR,
});

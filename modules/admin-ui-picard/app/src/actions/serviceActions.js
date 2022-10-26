/**
 * This file contains all redux actions that can be executed on services
 */

// Constants of action types for fetching services from server
export const LOAD_SERVICES_IN_PROGRESS = "LOAD_SERVICES_IN_PROGRESS";
export const LOAD_SERVICES_SUCCESS = "LOAD_SERVICES_SUCCESS";
export const LOAD_SERVICES_FAILURE = "LOAD_SERVICES_FAILURE";

// Constants of action types affecting UI
export const SET_SERVICES_COLUMNS = "SET_SERVICES_COLUMNS";

// Actions affecting fetching services from server

export const loadServicesInProgress = () => ({
	type: LOAD_SERVICES_IN_PROGRESS,
});

export const loadServicesSuccess = (services) => ({
	type: LOAD_SERVICES_SUCCESS,
	payload: { services },
});

export const loadServicesFailure = () => ({
	type: LOAD_SERVICES_FAILURE,
});

// Actions affecting UI

export const setServicesColumns = (updatedColumns) => ({
	type: SET_SERVICES_COLUMNS,
	payload: { updatedColumns },
});

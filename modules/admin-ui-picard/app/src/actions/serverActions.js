/**
 * This file contains all redux actions that can be executed on servers
 */

// Constants of action types for fetching servers from server
export const LOAD_SERVERS_IN_PROGRESS = "LOAD_SERVERS_IN_PROGRESS";
export const LOAD_SERVERS_SUCCESS = "LOAD_SERVERS_SUCCESS";
export const LOAD_SERVERS_FAILURE = "LOAD_SERVERS_FAILURE";

// Constants of action types affecting UI
export const SET_SERVER_COLUMNS = "SET_SERVER_COLUMNS";

// Actions affecting fetching servers from server

export const loadServersInProgress = () => ({
	type: LOAD_SERVERS_IN_PROGRESS,
});

export const loadServersSuccess = (servers) => ({
	type: LOAD_SERVERS_SUCCESS,
	payload: { servers },
});

export const loadServersFailure = () => ({
	type: LOAD_SERVERS_FAILURE,
});

// Actions affecting UI

export const setServerColumns = (updatedColumns) => ({
	type: SET_SERVER_COLUMNS,
	payload: { updatedColumns },
});

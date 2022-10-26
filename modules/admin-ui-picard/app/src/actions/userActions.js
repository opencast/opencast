/**
 * This file contains all redux actions that can be executed on users
 */

// Constants of action types for fetching users from server
export const LOAD_USERS_IN_PROGRESS = "LOAD_USERS_IN_PROGRESS";
export const LOAD_USERS_SUCCESS = "LOAD_USERS_SUCCESS";
export const LOAD_USERS_FAILURE = "LOAD_USERS_FAILURE";

// Constants of action types affecting UI
export const SET_USER_COLUMNS = "SET_USER_COLUMNS";

// Actions affecting fetching users from server

export const loadUsersInProgress = () => ({
	type: LOAD_USERS_IN_PROGRESS,
});

export const loadUsersSuccess = (users) => ({
	type: LOAD_USERS_SUCCESS,
	payload: { users },
});

export const loadUsersFailure = () => ({
	type: LOAD_USERS_FAILURE,
});

// Action affecting UI

export const setUserColumns = (updatedColumns) => ({
	type: SET_USER_COLUMNS,
	payload: { updatedColumns },
});

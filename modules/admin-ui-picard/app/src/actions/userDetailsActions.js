/**
 * This file contains all redux actions that can be executed on details of a user
 */

// Constants of action types for fetching details of a certain user from server
export const LOAD_USER_DETAILS_IN_PROGRESS = "LOAD_USER_DETAILS_IN_PROGRESS";
export const LOAD_USER_DETAILS_SUCCESS = "LOAD_USER_DETAILS_SUCCESS";
export const LOAD_USER_DETAILS_FAILURE = "LOAD_USER_DETAILS_FAILURE";

// Actions affecting fetching details of a certain user from server

export const loadUserDetailsInProgress = () => ({
	type: LOAD_USER_DETAILS_IN_PROGRESS,
});

export const loadUserDetailsSuccess = (userDetails) => ({
	type: LOAD_USER_DETAILS_SUCCESS,
	payload: { userDetails },
});

export const loadUserDetailsFailure = () => ({
	type: LOAD_USER_DETAILS_FAILURE,
});

/**
 * This file contains all redux actions that can be executed on information about the current user
 */

// Constants of action types for fetching information about the current user from server
export const LOAD_USER_INFO_IN_PROGRESS = "LOAD_USER_INFO_IN_PROGRESS";
export const LOAD_USER_INFO_SUCCESS = "LOAD_USER_INFO_SUCCESS";
export const LOAD_USER_INFO_FAILURE = "LOAD_USER_INFO_FAILURE";

// Constants of action types for fetching opencast version from server
export const LOAD_OC_VERSION_IN_PROGRESS = "LOAD_OC_VERSION_IN_PROGRESS";
export const LOAD_OC_VERSION_SUCCESS = "LOAD_OC_VERSION_SUCCESS";
export const LOAD_OC_VERSION_FAILURE = "LOAD_OC_VERSION_FAILURE";

// Actions affecting fetching information about the current user from server

export const loadUserInfoInProgress = () => ({
	type: LOAD_USER_INFO_IN_PROGRESS,
});

export const loadUserInfoSuccess = (userInfo) => ({
	type: LOAD_USER_INFO_SUCCESS,
	payload: { userInfo },
});

export const loadUserInfoFailure = () => ({
	type: LOAD_USER_INFO_FAILURE,
});

export const loadOcVersionInProgress = () => ({
	type: LOAD_OC_VERSION_IN_PROGRESS,
});

export const loadOcVersionSuccess = (ocVersion) => ({
	type: LOAD_OC_VERSION_SUCCESS,
	payload: { ocVersion },
});

export const loadOcVersionFailure = () => ({
	type: LOAD_OC_VERSION_FAILURE,
});

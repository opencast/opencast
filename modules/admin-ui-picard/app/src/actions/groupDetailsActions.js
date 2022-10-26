/**
 * This file contains all redux actions that can be executed on details of a group
 */

// Constants of action types for fetching details of a certain group from server
export const LOAD_GROUP_DETAILS_IN_PROGRESS = "LOAD_GROUP_DETAILS_IN_PROGRESS";
export const LOAD_GROUP_DETAILS_SUCCESS = "LOAD_GROUP_DETAILS_SUCCESS";
export const LOAD_GROUP_DETAILS_FAILURE = "LOAD_GROUP_DETAILS_FAILURE";

// Actions affecting fetching details of a certain group from server

export const loadGroupDetailsInProgress = () => ({
	type: LOAD_GROUP_DETAILS_IN_PROGRESS,
});

export const loadGroupDetailsSuccess = (groupDetails) => ({
	type: LOAD_GROUP_DETAILS_SUCCESS,
	payload: { groupDetails },
});

export const loadGroupDetailsFailure = () => ({
	type: LOAD_GROUP_DETAILS_FAILURE,
});

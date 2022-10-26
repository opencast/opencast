/**
 * This file contains all redux actions that can be executed on groups
 */

// Constants of action types for fetching groups from server
export const LOAD_GROUPS_IN_PROGRESS = "LOAD_GROUPS_IN_PROGRESS";
export const LOAD_GROUPS_SUCCESS = "LOAD_GROUPS_SUCCESS";
export const LOAD_GROUPS_FAILURE = "LOAD_GROUPS_FAILURE";

// Constants of action types affecting UI
export const SET_GROUP_COLUMNS = "SET_GROUP_COLUMNS";

// Actions affecting fetching groups from server

export const loadGroupsInProgress = () => ({
	type: LOAD_GROUPS_IN_PROGRESS,
});

export const loadGroupsSuccess = (groups) => ({
	type: LOAD_GROUPS_SUCCESS,
	payload: { groups },
});

export const loadGroupsFailure = () => ({
	type: LOAD_GROUPS_FAILURE,
});

// Actions affecting UI

export const setGroupColumns = (updatedColumns) => ({
	type: SET_GROUP_COLUMNS,
	payload: { updatedColumns },
});

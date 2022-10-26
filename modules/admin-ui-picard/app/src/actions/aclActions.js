/**
 * This file contains all redux actions that can be executed on acls
 */

// Constants of action types for fetching acls from server
export const LOAD_ACLS_IN_PROGRESS = "LOAD_ACLS_IN_PROGRESS";
export const LOAD_ACLS_SUCCESS = "LOAD_ACLS_SUCCESS";
export const LOAD_ACLS_FAILURE = "LOAD_ACLS_FAILURE";

// Constants of action types affecting UI
export const SET_ACL_COLUMNS = "SET_ACL_COLUMNS";

// Actions affecting fetching acls from server

export const loadAclsInProgress = () => ({
	type: LOAD_ACLS_IN_PROGRESS,
});

export const loadAclsSuccess = (acls) => ({
	type: LOAD_ACLS_SUCCESS,
	payload: { acls },
});

export const loadAclsFailure = () => ({
	type: LOAD_ACLS_FAILURE,
});

// Actions affecting UI

export const setACLColumns = (updatedColumns) => ({
	type: SET_ACL_COLUMNS,
	payload: { updatedColumns },
});

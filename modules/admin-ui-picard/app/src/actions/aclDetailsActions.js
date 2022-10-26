/**
 * This file contains all redux actions that can be executed on details of an ACL
 */

// Constants of action types for fetching details of a certain ACL from server
export const LOAD_ACL_DETAILS_IN_PROGRESS = "LOAD_ACL_DETAILS_IN_PROGRESS";
export const LOAD_ACL_DETAILS_SUCCESS = "LOAD_ACL_DETAILS_SUCCESS";
export const LOAD_ACL_DETAILS_FAILURE = "LOAD_ACL_DETAILS_FAILURE";

// Actions affecting fetching details of a certain ACL from server

export const loadAclDetailsInProgress = () => ({
	type: LOAD_ACL_DETAILS_IN_PROGRESS,
});

export const loadAclDetailsSuccess = (aclDetails) => ({
	type: LOAD_ACL_DETAILS_SUCCESS,
	payload: { aclDetails },
});

export const loadAclDetailsFailure = () => ({
	type: LOAD_ACL_DETAILS_FAILURE,
});

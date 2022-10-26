/**
 * This file contains all redux actions that can be executed on workflows
 */

// Constants of actions types for fetching workflow definitions from server
export const LOAD_WORKFLOW_DEF_IN_PROGRESS = "LOAD_WORKFLOW_DEF_IN_PROGRESS";
export const LOAD_WORKFLOW_DEF_SUCCESS = "LOAD_WORKFLOW_DEF_SUCCESS";
export const LOAD_WORKFLOW_DEF_FAILURE = "LOAD_WORKFLOW_DEF_FAILURE";

// Actions affecting fetching of workflow definitions from server

export const loadWorkflowDefInProgress = () => ({
	type: LOAD_WORKFLOW_DEF_IN_PROGRESS,
});

export const loadWorkflowDefSuccess = (workflowDef) => ({
	type: LOAD_WORKFLOW_DEF_SUCCESS,
	payload: { workflowDef },
});

export const loadWorkflowDefFailure = () => ({
	type: LOAD_WORKFLOW_DEF_FAILURE,
});

import {
	LOAD_WORKFLOW_DEF_FAILURE,
	LOAD_WORKFLOW_DEF_IN_PROGRESS,
	LOAD_WORKFLOW_DEF_SUCCESS,
} from "../actions/workflowActions";

/**
 * This file contains redux reducer for actions affecting the state of workflows
 */

// Initial state of workflows in redux store
const initialState = {
	isLoading: false,
	defaultWorkflowId: "",
	workflows: [],
};

// Reducer for workflows
const workflows = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_WORKFLOW_DEF_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_WORKFLOW_DEF_SUCCESS: {
			const { workflowDef } = payload;
			return {
				...state,
				isLoading: false,
				defaultWorkflowId: workflowDef.defaultWorkflowId,
				workflows: workflowDef.workflows,
			};
		}
		case LOAD_WORKFLOW_DEF_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		default:
			return state;
	}
};

export default workflows;

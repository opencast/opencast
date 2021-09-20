import {
    LOAD_EVENT_POLICIES_IN_PROGRESS,
    LOAD_EVENT_POLICIES_SUCCESS,
    LOAD_EVENT_POLICIES_FAILURE,
    LOAD_EVENT_COMMENTS_IN_PROGRESS,
    LOAD_EVENT_COMMENTS_SUCCESS,
    LOAD_EVENT_COMMENTS_FAILURE,
    SAVE_COMMENT_IN_PROGRESS,
    SAVE_COMMENT_DONE,
    SAVE_COMMENT_REPLY_IN_PROGRESS,
    SAVE_COMMENT_REPLY_DONE,
    LOAD_EVENT_WORKFLOWS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOWS_SUCCESS,
    LOAD_EVENT_WORKFLOWS_FAILURE,
    SET_EVENT_WORKFLOW_DEFINITIONS,
    SET_EVENT_WORKFLOW,
    SET_EVENT_WORKFLOW_CONFIGURATION,
    DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS,
    DO_EVENT_WORKFLOW_ACTION_SUCCESS,
    DO_EVENT_WORKFLOW_ACTION_FAILURE,
    DELETE_EVENT_WORKFLOW_IN_PROGRESS,
    DELETE_EVENT_WORKFLOW_SUCCESS,
    DELETE_EVENT_WORKFLOW_FAILURE
} from '../actions/eventDetailsActions';

// Initial state of event details in redux store
const initialState = {
    eventId: "",
    policies: [],
    fetchingPoliciesInProgress: false,
    savingCommentReplyInProgress: false,
    savingCommentInProgress: false,
    fetchingCommentsInProgress: false,
    comments: [],
    commentReasons: [],
    fetchingWorkflowsInProgress: false,
    workflows: {
        scheduling: false,
        entries: [],
        workflow: {
            workflowId: "",
            description: ""
        }
    },
    workflowConfiguration: {
        workflowId: "",
        description: ""
    },
    workflowDefinitions: [],
    baseWorkflow: {},
    workflowActionInProgress: false,
    deleteWorkflowInProgress: false
}

// Reducer for event details
const eventDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_EVENT_POLICIES_IN_PROGRESS: {
            return {
                ...state,
                fetchingPoliciesInProgress: true,
            };
        }
        case LOAD_EVENT_POLICIES_SUCCESS: {
            const { policies } = payload;
            return {
                ...state,
                fetchingPoliciesInProgress: false,
                policies: policies
            };
        }
        case LOAD_EVENT_POLICIES_FAILURE: {
            return {
                ...state,
                fetchingPoliciesInProgress: false,
            };
        }
        case LOAD_EVENT_COMMENTS_IN_PROGRESS: {
            return {
                ...state,
                fetchingCommentsInProgress: true
            };
        }
        case LOAD_EVENT_COMMENTS_SUCCESS: {
            const { comments, commentReasons } = payload;
            return {
                ...state,
                fetchingCommentsInProgress: false,
                comments: comments,
                commentReasons: commentReasons
            };
        }
        case LOAD_EVENT_COMMENTS_FAILURE: {
            return {
                ...state,
                fetchingCommentsInProgress: false
            };
        }
        case SAVE_COMMENT_IN_PROGRESS: {
            return {
                ...state,
                savingCommentInProgress: true
            };
        }
        case SAVE_COMMENT_DONE: {
            return {
                ...state,
                savingCommentInProgress: false
            };
        }
        case SAVE_COMMENT_REPLY_IN_PROGRESS: {
            return {
                ...state,
                savingCommentReplyInProgress: true
            };
        }
        case SAVE_COMMENT_REPLY_DONE: {
            return {
                ...state,
                savingCommentReplyInProgress: false
            };
        }
        case LOAD_EVENT_WORKFLOWS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOWS_SUCCESS: {
            const { workflows } = payload;
            return {
                ...state,
                workflows: workflows,
                fetchingWorkflowsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOWS_FAILURE: {
            return {
                ...state,
                fetchingWorkflowsInProgress: false
            };
        }
        case SET_EVENT_WORKFLOW_DEFINITIONS: {
            const { workflows, workflowDefinitions } = payload;
            return {
                ...state,
                baseWorkflow: {...workflows.workflow},
                workflows: workflows,
                workflowDefinitions: workflowDefinitions
            };
        }
        case SET_EVENT_WORKFLOW: {
            const { workflow } = payload;
            return {
                ...state,
                workflows: {
                    ...state.workflows,
                    workflow: workflow
                }
            }
        }
        case SET_EVENT_WORKFLOW_CONFIGURATION: {
            const { workflow_configuration } = payload;
            return {
                ...state,
                workflowConfiguration: workflow_configuration
            }
        }
        case DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS: {
            return {
                ...state,
                workflowActionInProgress: true
            }
        }
        case DO_EVENT_WORKFLOW_ACTION_SUCCESS: {
            return {
                ...state,
                workflowActionInProgress: false
            }
        }
        case DO_EVENT_WORKFLOW_ACTION_FAILURE: {
            return {
                ...state,
                workflowActionInProgress: false
            }
        }
        case DELETE_EVENT_WORKFLOW_IN_PROGRESS: {
            return {
                ...state,
                deleteWorkflowInProgress: true
            }
        }
        case DELETE_EVENT_WORKFLOW_SUCCESS: {
            const {} = payload;
            return {
                ...state,
                deleteWorkflowInProgress: false
            }
        }
        case DELETE_EVENT_WORKFLOW_FAILURE: {
            return {
                ...state,
                deleteWorkflowInProgress: false
            }
        }
        default:
            return state;
    }
};

export default eventDetails;
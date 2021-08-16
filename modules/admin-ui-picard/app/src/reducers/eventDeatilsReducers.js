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
    LOAD_EVENT_WORKFLOWS_FAILURE, SET_EVENT_WORKFLOW_DEFINITIONS, SET_EVENT_WORKFLOW
} from '../actions/eventDetailsActions';

// Initial state of event details in redux store
const initialState = {
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
        workflow: {description: ""}
    },
    workflowConfiguration: {
        id: "",
        description: "",
        configuration_panel: ""
    },
    workflowDefinitions: [],
    baseWorkflow: {},
    workflow: {},
    eventId: ""
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
            const { baseWorkflow, workflowDefinitions } = payload;
            return {
                ...state,
                baseWorkflow: baseWorkflow,
                workflow: {
                    ...state.workflow,
                    id: baseWorkflow.workflowId
                },
                workflowDefinitions: workflowDefinitions
            };
        }
        case SET_EVENT_WORKFLOW: {
            const { workflow } = payload;
            return {
                ...state,
                workflow: workflow
            }
        }
        default:
            return state;
    }
};

export default eventDetails;
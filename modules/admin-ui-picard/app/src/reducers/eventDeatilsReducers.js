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
    LOAD_EVENT_WORKFLOW_DETAILS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOW_DETAILS_SUCCESS,
    LOAD_EVENT_WORKFLOW_DETAILS_FAILURE,
    SET_EVENT_WORKFLOW_DEFINITIONS,
    SET_EVENT_WORKFLOW,
    SET_EVENT_WORKFLOW_CONFIGURATION,
    DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS,
    DO_EVENT_WORKFLOW_ACTION_SUCCESS,
    DO_EVENT_WORKFLOW_ACTION_FAILURE,
    DELETE_EVENT_WORKFLOW_IN_PROGRESS,
    DELETE_EVENT_WORKFLOW_SUCCESS,
    DELETE_EVENT_WORKFLOW_FAILURE,
    LOAD_EVENT_WORKFLOW_OPERATIONS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOW_OPERATIONS_SUCCESS,
    LOAD_EVENT_WORKFLOW_OPERATIONS_FAILURE,
    LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_SUCCESS,
    LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_FAILURE,
    LOAD_EVENT_WORKFLOW_ERRORS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOW_ERRORS_SUCCESS,
    LOAD_EVENT_WORKFLOW_ERRORS_FAILURE,
    LOAD_EVENT_WORKFLOW_ERROR_DETAILS_IN_PROGRESS,
    LOAD_EVENT_WORKFLOW_ERROR_DETAILS_SUCCESS,
    LOAD_EVENT_WORKFLOW_ERROR_DETAILS_FAILURE,
    LOAD_EVENT_PUBLICATIONS_SUCCESS,
    LOAD_EVENT_PUBLICATIONS_IN_PROGRESS,
    LOAD_EVENT_PUBLICATIONS_FAILURE,
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
    fetchingWorkflowDetailsInProgress: false,
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
    deleteWorkflowInProgress: false,
    fetchingWorkflowOperationsInProgress: false,
    workflowOperations: {},
    fetchingWorkflowOperationDetailsInProgress: false,
    workflowOperationDetails: {},
    fetchingWorkflowErrorsInProgress: false,
    workflowErrors: {},
    fetchingWorkflowErrorDetailsInProgress: false,
    workflowErrorDetails: {},
    loadingPublications: false,
    publications: []
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
        case LOAD_EVENT_PUBLICATIONS_SUCCESS: {
            const { publications } = payload;
            return {
                ...state,
                loadingPublications: false,
                publications: publications
            }
        }
        case LOAD_EVENT_PUBLICATIONS_IN_PROGRESS: {
            return {
                ...state,
                loadingPublications: true
            }
        }
        case LOAD_EVENT_PUBLICATIONS_FAILURE: {
            return {
                ...state,
                loadingPublications: false
            }
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
        case LOAD_EVENT_WORKFLOW_DETAILS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowDetailsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOW_DETAILS_SUCCESS: {
            const { workflowDetails } = payload;
            return {
                ...state,
                workflows: {
                    ...state.workflows,
                    workflow: workflowDetails
                },
                fetchingWorkflowDetailsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_DETAILS_FAILURE: {
            const emptyWorkflowData = {
                creator: {
                    name: "",
                    email: ""
                },
                title: "",
                description: "",
                submittedAt: "",
                state: "",
                executionTime: "",
                wiid: "",
                wdid: "",
                configuration: {}
            }

            return {
                ...state,
                workflows: {
                    ...state.workflows,
                    workflow: emptyWorkflowData
                },
                fetchingWorkflowDetailsInProgress: false
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
            const { workflowsEntries } = payload;
            return {
                ...state,
                workflows: {
                    ...state.workflows,
                    entries: workflowsEntries
                },
                deleteWorkflowInProgress: false
            }
        }
        case DELETE_EVENT_WORKFLOW_FAILURE: {
            return {
                ...state,
                deleteWorkflowInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_OPERATIONS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowOperationsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOW_OPERATIONS_SUCCESS: {
            const { workflowOperations } = payload;
            return {
                ...state,
                workflowOperations: workflowOperations,
                fetchingWorkflowOperationsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_OPERATIONS_FAILURE: {
            return {
                ...state,
                workflowOperations: {entries: []},
                fetchingWorkflowOperationsInProgress: false
            };
        }
        case LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowOperationDetailsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_SUCCESS: {
            const { workflowOperationDetails } = payload;
            return {
                ...state,
                workflowOperationDetails: workflowOperationDetails,
                fetchingWorkflowOperationDetailsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_FAILURE: {
            const emptyOperationDetails = {
                name: "",
                description: "",
                state: "",
                execution_host: "",
                job: "",
                time_in_queue: "",
                started: "",
                completed: "",
                retry_strategy: "",
                failed_attempts: "",
                max_attempts: "",
                exception_handler_workflow: "",
                fail_on_error: ""
            };

            return {
                ...state,
                workflowOperationDetails: emptyOperationDetails,
                fetchingWorkflowOperationDetailsInProgress: false
            };
        }
        case LOAD_EVENT_WORKFLOW_ERRORS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowErrorsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOW_ERRORS_SUCCESS: {
            const { workflowErrors } = payload;
            return {
                ...state,
                workflowErrors: workflowErrors,
                fetchingWorkflowErrorsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_ERRORS_FAILURE: {
            return {
                ...state,
                workflowErrors: {entries: []},
                fetchingWorkflowErrorsInProgress: false
            };
        }
        case LOAD_EVENT_WORKFLOW_ERROR_DETAILS_IN_PROGRESS: {
            return {
                ...state,
                fetchingWorkflowErrorDetailsInProgress: true
            };
        }
        case LOAD_EVENT_WORKFLOW_ERROR_DETAILS_SUCCESS: {
            const { workflowErrorDetails } = payload;
            return {
                ...state,
                workflowErrorDetails: workflowErrorDetails,
                fetchingWorkflowErrorDetailsInProgress: false
            }
        }
        case LOAD_EVENT_WORKFLOW_ERROR_DETAILS_FAILURE: {
            const emptyErrorDetails = {
            };

            return {
                ...state,
                workflowErrorDetails: emptyErrorDetails,
                fetchingWorkflowErrorDetailsInProgress: false
            };
        }
        default:
            return state;
    }
};



export default eventDetails;

/**
 * This file contains all redux actions that can be executed on event details
 */

// Constant of actions types affecting the access policies tab tab
export const LOAD_EVENT_POLICIES_SUCCESS = 'LOAD_EVENT_POLICIES_SUCCESS';
export const LOAD_EVENT_POLICIES_FAILURE = 'LOAD_EVENT_POLICIES_FAILURE';
export const LOAD_EVENT_POLICIES_IN_PROGRESS = 'LOAD_EVENT_POLICIES_IN_PROGRESS';

// Constant of actions types affecting the comments tab
export const LOAD_EVENT_COMMENTS_SUCCESS = 'LOAD_EVENT_COMMENTS_SUCCESS';
export const LOAD_EVENT_COMMENTS_FAILURE = 'LOAD_EVENT_COMMENTS_FAILURE';
export const LOAD_EVENT_COMMENTS_IN_PROGRESS = 'LOAD_EVENT_COMMENTS_IN_PROGRESS';
export const SAVE_COMMENT_DONE = 'SAVE_COMMENT_DONE';
export const SAVE_COMMENT_IN_PROGRESS = 'SAVE_COMMENT_IN_PROGRESS';
export const SAVE_COMMENT_REPLY_DONE = 'SAVE_COMMENT_REPLY_DONE';
export const SAVE_COMMENT_REPLY_IN_PROGRESS = 'SAVE_COMMENT_REPLY_IN_PROGRESS';

// Constant of actions types affecting the workflows tab
export const LOAD_EVENT_WORKFLOWS_IN_PROGRESS = 'LOAD_EVENT_WORKFLOWS_IN_PROGRESS';
export const LOAD_EVENT_WORKFLOWS_SUCCESS = 'LOAD_EVENT_WORKFLOWS_SUCCESS';
export const LOAD_EVENT_WORKFLOWS_FAILURE = 'LOAD_EVENT_WORKFLOWS_FAILURE';
export const SET_EVENT_WORKFLOW_DEFINITIONS = 'SET_EVENT_WORKFLOW_DEFINITIONS';
export const SET_EVENT_WORKFLOW = 'SET_EVENT_WORKFLOW';


// actions for access policies
export const loadEventPoliciesInProgress = () => ({
    type: LOAD_EVENT_POLICIES_IN_PROGRESS
});

export const loadEventPoliciesSuccess = ( policies ) => ({
    type: LOAD_EVENT_POLICIES_SUCCESS,
    payload: {
        policies
    }
});

export const loadEventPoliciesFailure = () => ({
    type: LOAD_EVENT_POLICIES_FAILURE
});

// actions for comments
export const loadEventCommentsInProgress = () => ({
    type: LOAD_EVENT_COMMENTS_IN_PROGRESS
});

export const loadEventCommentsSuccess = ( comments, commentReasons ) => ({
    type: LOAD_EVENT_COMMENTS_SUCCESS,
    payload: {
        comments,
        commentReasons
    }
});

export const loadEventCommentsFailure = () => ({
    type: LOAD_EVENT_COMMENTS_FAILURE
});

export const saveCommentInProgress = ( ) => ({
    type: SAVE_COMMENT_IN_PROGRESS
});

export const saveCommentDone = ( ) => ({
    type: SAVE_COMMENT_DONE
});

export const saveCommentReplyInProgress = ( ) => ({
    type: SAVE_COMMENT_REPLY_IN_PROGRESS
});

export const saveCommentReplyDone = ( ) => ({
    type: SAVE_COMMENT_REPLY_DONE
});

// actions for workflows
export const loadEventWorkflowsInProgress = () => ({
    type: LOAD_EVENT_WORKFLOWS_IN_PROGRESS
});

export const loadEventWorkflowsSuccess = ( workflows ) => ({
    type: LOAD_EVENT_WORKFLOWS_SUCCESS,
    payload: {
        workflows
    }
});

export const loadEventWorkflowsFailure = () => ({
    type: LOAD_EVENT_WORKFLOWS_FAILURE
});

export const setEventWorkflowDefinitions = (baseWorkflow, workflowDefinitions) => ({
    type: SET_EVENT_WORKFLOW_DEFINITIONS,
    payload: {
        baseWorkflow,
        workflowDefinitions
    }
});

export const setEventWorkflow = (workflow) => ({
    type: SET_EVENT_WORKFLOW,
    payload: {
        workflow
    }
});
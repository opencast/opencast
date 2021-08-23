/* selectors for policies */
export const getPolicies = state => state.eventDetails.policies;

/* selectors for comments */
export const getComments = state => state.eventDetails.comments;
export const getCommentReasons = state => state.eventDetails.commentReasons;
export const isFetchingComments = state => state.eventDetails.fetchingCommentsInProgress;
export const isSavingComment = state => state.eventDetails.savingCommentInProgress;
export const isSavingCommentReply = state => state.eventDetails.savingCommentReplyInProgress;

/* selectors for workflows */
export const getWorkflows = state => state.eventDetails.workflows;
export const isFetchingWorkflows = state => state.eventDetails.fetchingWorkflowsInProgress;
export const getWorkflowDefinitions = state => state.eventDetails.workflowDefinitions;
export const getWorkflowConfiguration = state => state.eventDetails.workflowConfiguration;
export const getWorkflow = state => state.eventDetails.workflows.workflow;
export const getBaseWorkflow = state => state.eventDetails.baseWorkflow;
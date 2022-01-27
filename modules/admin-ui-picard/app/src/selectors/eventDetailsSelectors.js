/* selectors for metadata */
export const getMetadata = state => state.eventDetails.metadata;
export const isFetchingMetadata = state => state.eventDetails.fetchingMetadataInProgress;

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
export const isFetchingWorkflowDetails = state => state.eventDetails.fetchingWorkflowDetailsInProgress;
export const getBaseWorkflow = state => state.eventDetails.baseWorkflow;
export const performingWorkflowAction = state => state.eventDetails.workflowActionInProgress;
export const deletingWorkflow = state => state.eventDetails.deleteWorkflowInProgress;
export const getWorkflowOperations = state => state.eventDetails.workflowOperations;
export const isFetchingWorkflowOperations = state => state.eventDetails.fetchingWorkflowOperationsInProgress;
export const getWorkflowOperationDetails = state => state.eventDetails.workflowOperationDetails;
export const isFetchingWorkflowOperationDetails = state => state.eventDetails.fetchingWorkflowOperationDetailsInProgress;
export const getWorkflowErrors = state => state.eventDetails.workflowErrors;
export const isFetchingWorkflowErrors = state => state.eventDetails.fetchingWorkflowErrorsInProgress;
export const getWorkflowErrorDetails = state => state.eventDetails.workflowErrorDetails;
export const isFetchingWorkflowErrorDetails = state => state.eventDetails.fetchingWorkflowErrorDetailsInProgress;

/* selectors for publications */
export const getPublications = state => state.eventDetails.publications;

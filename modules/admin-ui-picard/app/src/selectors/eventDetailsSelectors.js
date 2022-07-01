/* selectors for metadata */
export const getMetadata = state => state.eventDetails.metadata;
export const getExtendedMetadata = state => state.eventDetails.extendedMetadata;
export const isFetchingMetadata = state => state.eventDetails.fetchingMetadataInProgress;

/* selectors for assets */
export const getAssets = state => state.eventDetails.assets;
export const isFetchingAssets = state => state.eventDetails.fetchingAssetsInProgress;
export const isTransactionReadOnly = state => state.eventDetails.transactionsReadOnly;
export const getUploadAssetOptions = state => state.eventDetails.uploadAssetOptions;
export const getAssetAttachments = state => state.eventDetails.assetAttachments;
export const getAssetAttachmentDetails = state => state.eventDetails.assetAttachmentDetails;
export const getAssetCatalogs = state => state.eventDetails.assetCatalogs;
export const getAssetCatalogDetails = state => state.eventDetails.assetCatalogDetails;
export const getAssetMedia = state => state.eventDetails.assetMedia;
export const getAssetMediaDetails = state => state.eventDetails.assetMediaDetails;
export const getAssetPublications = state => state.eventDetails.assetPublications;
export const getAssetPublicationDetails = state => state.eventDetails.assetPublicationDetails;

/* selectors for policies */
export const getPolicies = state => state.eventDetails.policies;

/* selectors for comments */
export const getComments = state => state.eventDetails.comments;
export const getCommentReasons = state => state.eventDetails.commentReasons;
export const isFetchingComments = state => state.eventDetails.fetchingCommentsInProgress;
export const isSavingComment = state => state.eventDetails.savingCommentInProgress;
export const isSavingCommentReply = state => state.eventDetails.savingCommentReplyInProgress;

/* selectors for scheduling */
export const getSchedulingProperties = state => state.eventDetails.scheduling.hasProperties;
export const isFetchingScheduling = state => state.eventDetails.fetchingSchedulingInProgress;
export const isSavingScheduling = state => state.eventDetails.savingSchedulingInProgress;
export const getSchedulingSource = state => state.eventDetails.schedulingSource;
export const getSchedulingConflicts = state => state.eventDetails.schedulingConflicts;
export const isCheckingConflicts = state => state.eventDetails.checkingConflicts;

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

/* selectors for statistics */
export const hasStatistics = state => state.eventDetails.statistics.length > 0;
export const getStatistics = state => state.eventDetails.statistics;
export const hasStatisticsError = state => state.eventDetails.hasStatisticsError;
export const isFetchingStatistics = state => state.eventDetails.fetchingStatisticsInProgress;
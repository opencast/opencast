/**
 * This file contains all redux actions that can be executed on event details
 */

// Constants of actions types affecting the metadata tab
export const LOAD_EVENT_METADATA_SUCCESS = "LOAD_EVENT_METADATA_SUCCESS";
export const LOAD_EVENT_METADATA_FAILURE = "LOAD_EVENT_METADATA_FAILURE";
export const LOAD_EVENT_METADATA_IN_PROGRESS =
	"LOAD_EVENT_METADATA_IN_PROGRESS";
export const SET_EVENT_METADATA = "SET_EVENT_METADATA";
export const SET_EXTENDED_EVENT_METADATA = "SET_EXTENDED_EVENT_METADATA";

// Constants of actions types affecting the assets tab
export const LOAD_EVENT_ASSETS_IN_PROGRESS = "LOAD_EVENT_ASSETS_IN_PROGRESS";
export const LOAD_EVENT_ASSETS_SUCCESS = "LOAD_EVENT_ASSETS_SUCCESS";
export const LOAD_EVENT_ASSETS_FAILURE = "LOAD_EVENT_ASSETS_FAILURE";
export const LOAD_EVENT_ASSET_ATTACHMENTS_SUCCESS =
	"LOAD_EVENT_ASSET_ATTACHMENTS_SUCCESS";
export const LOAD_EVENT_ASSET_ATTACHMENTS_FAILURE =
	"LOAD_EVENT_ASSET_ATTACHMENTS_FAILURE";
export const LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_SUCCESS =
	"LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_SUCCESS";
export const LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_FAILURE =
	"LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_FAILURE";
export const LOAD_EVENT_ASSET_CATALOGS_SUCCESS =
	"LOAD_EVENT_ASSET_CATALOGS_SUCCESS";
export const LOAD_EVENT_ASSET_CATALOGS_FAILURE =
	"LOAD_EVENT_ASSET_CATALOGS_FAILURE";
export const LOAD_EVENT_ASSET_CATALOG_DETAILS_SUCCESS =
	"LOAD_EVENT_ASSET_CATALOG_DETAILS_SUCCESS";
export const LOAD_EVENT_ASSET_CATALOG_DETAILS_FAILURE =
	"LOAD_EVENT_ASSET_CATALOG_DETAILS_FAILURE";
export const LOAD_EVENT_ASSET_MEDIA_SUCCESS = "LOAD_EVENT_ASSET_MEDIA_SUCCESS";
export const LOAD_EVENT_ASSET_MEDIA_FAILURE = "LOAD_EVENT_ASSET_MEDIA_FAILURE";
export const LOAD_EVENT_ASSET_MEDIA_DETAILS_SUCCESS =
	"LOAD_EVENT_ASSET_MEDIA_DETAILS_SUCCESS";
export const LOAD_EVENT_ASSET_MEDIA_DETAILS_FAILURE =
	"LOAD_EVENT_ASSET_MEDIA_DETAILS_FAILURE";
export const LOAD_EVENT_ASSET_PUBLICATIONS_SUCCESS =
	"LOAD_EVENT_ASSET_PUBLICATIONS_SUCCESS";
export const LOAD_EVENT_ASSET_PUBLICATIONS_FAILURE =
	"LOAD_EVENT_ASSET_PUBLICATIONS_FAILURE";
export const LOAD_EVENT_ASSET_PUBLICATION_DETAILS_SUCCESS =
	"LOAD_EVENT_ASSET_PUBLICATION_DETAILS_SUCCESS";
export const LOAD_EVENT_ASSET_PUBLICATION_DETAILS_FAILURE =
	"LOAD_EVENT_ASSET_PUBLICATION_DETAILS_FAILURE";

// Constants of actions types affecting the access policies tab
export const LOAD_EVENT_POLICIES_SUCCESS = "LOAD_EVENT_POLICIES_SUCCESS";
export const LOAD_EVENT_POLICIES_FAILURE = "LOAD_EVENT_POLICIES_FAILURE";
export const LOAD_EVENT_POLICIES_IN_PROGRESS =
	"LOAD_EVENT_POLICIES_IN_PROGRESS";

// Constants of actions types affecting the comments tab
export const LOAD_EVENT_COMMENTS_SUCCESS = "LOAD_EVENT_COMMENTS_SUCCESS";
export const LOAD_EVENT_COMMENTS_FAILURE = "LOAD_EVENT_COMMENTS_FAILURE";
export const LOAD_EVENT_COMMENTS_IN_PROGRESS =
	"LOAD_EVENT_COMMENTS_IN_PROGRESS";
export const SAVE_COMMENT_DONE = "SAVE_COMMENT_DONE";
export const SAVE_COMMENT_IN_PROGRESS = "SAVE_COMMENT_IN_PROGRESS";
export const SAVE_COMMENT_REPLY_DONE = "SAVE_COMMENT_REPLY_DONE";
export const SAVE_COMMENT_REPLY_IN_PROGRESS = "SAVE_COMMENT_REPLY_IN_PROGRESS";

// Constants of actions types affecting the publications tab
export const LOAD_EVENT_PUBLICATIONS_IN_PROGRESS =
	"LOAD_EVENT_PUBLICATIONS_IN_PROGRESS";
export const LOAD_EVENT_PUBLICATIONS_SUCCESS =
	"LOAD_EVENT_PUBLICATIONS_SUCCESS";
export const LOAD_EVENT_PUBLICATIONS_FAILURE =
	"LOAD_EVENT_PUBLICATIONS_FAILURE";

// Constants of actions types affecting the scheduling tab
export const LOAD_EVENT_SCHEDULING_IN_PROGRESS =
	"LOAD_EVENT_SCHEDULING_IN_PROGRESS";
export const LOAD_EVENT_SCHEDULING_SUCCESS = "LOAD_EVENT_SCHEDULING_SUCCESS";
export const LOAD_EVENT_SCHEDULING_FAILURE = "LOAD_EVENT_SCHEDULING_FAILURE";
export const CHECK_CONFLICTS_IN_PROGRESS = "CHECK_CONFLICTS_IN_PROGRESS";
export const CHECK_CONFLICTS_SUCCESS = "CHECK_CONFLICTS_SUCCESS";
export const CHECK_CONFLICTS_FAILURE = "CHECK_CONFLICTS_FAILURE";
export const SAVE_EVENT_SCHEDULING_IN_PROGRESS =
	"SAVE_EVENT_SCHEDULING_IN_PROGRESS";
export const SAVE_EVENT_SCHEDULING_SUCCESS = "SAVE_EVENT_SCHEDULING_SUCCESS";
export const SAVE_EVENT_SCHEDULING_FAILURE = "SAVE_EVENT_SCHEDULING_FAILURE";

// Constants of actions types affecting the workflows tab
export const LOAD_EVENT_WORKFLOWS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOWS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOWS_SUCCESS = "LOAD_EVENT_WORKFLOWS_SUCCESS";
export const LOAD_EVENT_WORKFLOWS_FAILURE = "LOAD_EVENT_WORKFLOWS_FAILURE";
export const LOAD_EVENT_WORKFLOW_DETAILS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOW_DETAILS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOW_DETAILS_SUCCESS =
	"LOAD_EVENT_WORKFLOW_DETAILS_SUCCESS";
export const LOAD_EVENT_WORKFLOW_DETAILS_FAILURE =
	"LOAD_EVENT_WORKFLOW_DETAILS_FAILURE";
export const SET_EVENT_WORKFLOW_DEFINITIONS = "SET_EVENT_WORKFLOW_DEFINITIONS";
export const SET_EVENT_WORKFLOW = "SET_EVENT_WORKFLOW";
export const SET_EVENT_WORKFLOW_CONFIGURATION =
	"SET_EVENT_WORKFLOW_CONFIGURATION";
export const DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS =
	"DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS";
export const DO_EVENT_WORKFLOW_ACTION_SUCCESS =
	"DO_EVENT_WORKFLOW_ACTION_SUCCESS";
export const DO_EVENT_WORKFLOW_ACTION_FAILURE =
	"DO_EVENT_WORKFLOW_ACTION_FAILURE";
export const DELETE_EVENT_WORKFLOW_IN_PROGRESS =
	"DELETE_EVENT_WORKFLOW_IN_PROGRESS";
export const DELETE_EVENT_WORKFLOW_SUCCESS = "DELETE_EVENT_WORKFLOW_SUCCESS";
export const DELETE_EVENT_WORKFLOW_FAILURE = "DELETE_EVENT_WORKFLOW_FAILURE";
export const LOAD_EVENT_WORKFLOW_OPERATIONS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOW_OPERATIONS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOW_OPERATIONS_SUCCESS =
	"LOAD_EVENT_WORKFLOW_OPERATIONS_SUCCESS";
export const LOAD_EVENT_WORKFLOW_OPERATIONS_FAILURE =
	"LOAD_EVENT_WORKFLOW_OPERATIONS_FAILURE";
export const LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_SUCCESS =
	"LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_SUCCESS";
export const LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_FAILURE =
	"LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_FAILURE";
export const LOAD_EVENT_WORKFLOW_ERRORS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOW_ERRORS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOW_ERRORS_SUCCESS =
	"LOAD_EVENT_WORKFLOW_ERRORS_SUCCESS";
export const LOAD_EVENT_WORKFLOW_ERRORS_FAILURE =
	"LOAD_EVENT_WORKFLOW_ERRORS_FAILURE";
export const LOAD_EVENT_WORKFLOW_ERROR_DETAILS_IN_PROGRESS =
	"LOAD_EVENT_WORKFLOW_ERROR_DETAILS_IN_PROGRESS";
export const LOAD_EVENT_WORKFLOW_ERROR_DETAILS_SUCCESS =
	"LOAD_EVENT_WORKFLOW_ERROR_DETAILS_SUCCESS";
export const LOAD_EVENT_WORKFLOW_ERROR_DETAILS_FAILURE =
	"LOAD_EVENT_WORKFLOW_ERROR_DETAILS_FAILURE";

// Constants of actions types affecting the statistics tab
export const LOAD_EVENT_STATISTICS_IN_PROGRESS =
	"LOAD_EVENT_STATISTICS_IN_PROGRESS";
export const LOAD_EVENT_STATISTICS_SUCCESS = "LOAD_EVENT_STATISTICS_SUCCESS";
export const LOAD_EVENT_STATISTICS_FAILURE = "LOAD_EVENT_STATISTICS_FAILURE";
export const UPDATE_EVENT_STATISTICS_SUCCESS = "LOAD_EVENT_STATISTICS_SUCCESS";
export const UPDATE_EVENT_STATISTICS_FAILURE = "LOAD_EVENT_STATISTICS_FAILURE";

// Actions affecting fetching of event details from server

// actions for metadata
export const loadEventMetadataInProgress = () => ({
	type: LOAD_EVENT_METADATA_IN_PROGRESS,
});

export const loadEventMetadataSuccess = (metadata, extendedMetadata) => ({
	type: LOAD_EVENT_METADATA_SUCCESS,
	payload: {
		metadata,
		extendedMetadata,
	},
});

export const loadEventMetadataFailure = () => ({
	type: LOAD_EVENT_METADATA_FAILURE,
});

export const setEventMetadata = (metadata) => ({
	type: SET_EVENT_METADATA,
	payload: {
		metadata,
	},
});

export const setExtendedEventMetadata = (metadata) => ({
	type: SET_EXTENDED_EVENT_METADATA,
	payload: {
		metadata,
	},
});

// actions for assets
export const loadEventAssetsInProgress = () => ({
	type: LOAD_EVENT_ASSETS_IN_PROGRESS,
});

export const loadEventAssetsSuccess = (
	assets,
	transactionsReadOnly,
	uploadAssetOptions
) => ({
	type: LOAD_EVENT_ASSETS_SUCCESS,
	payload: {
		assets,
		transactionsReadOnly,
		uploadAssetOptions,
	},
});

export const loadEventAssetsFailure = () => ({
	type: LOAD_EVENT_ASSETS_FAILURE,
});

export const loadEventAssetAttachmentsSuccess = (attachments) => ({
	type: LOAD_EVENT_ASSET_ATTACHMENTS_SUCCESS,
	payload: {
		attachments,
	},
});

export const loadEventAssetAttachmentsFailure = () => ({
	type: LOAD_EVENT_ASSET_ATTACHMENTS_FAILURE,
});

export const loadEventAssetAttachmentDetailsSuccess = (attachmentDetails) => ({
	type: LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_SUCCESS,
	payload: {
		attachmentDetails,
	},
});

export const loadEventAssetAttachmentDetailsFailure = () => ({
	type: LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_FAILURE,
});

export const loadEventAssetCatalogsSuccess = (catalogs) => ({
	type: LOAD_EVENT_ASSET_CATALOGS_SUCCESS,
	payload: {
		catalogs,
	},
});

export const loadEventAssetCatalogsFailure = () => ({
	type: LOAD_EVENT_ASSET_CATALOGS_FAILURE,
});

export const loadEventAssetCatalogDetailsSuccess = (catalogDetails) => ({
	type: LOAD_EVENT_ASSET_CATALOG_DETAILS_SUCCESS,
	payload: {
		catalogDetails,
	},
});

export const loadEventAssetCatalogDetailsFailure = () => ({
	type: LOAD_EVENT_ASSET_CATALOG_DETAILS_FAILURE,
});

export const loadEventAssetMediaSuccess = (media) => ({
	type: LOAD_EVENT_ASSET_MEDIA_SUCCESS,
	payload: {
		media,
	},
});

export const loadEventAssetMediaFailure = () => ({
	type: LOAD_EVENT_ASSET_MEDIA_FAILURE,
});

export const loadEventAssetMediaDetailsSuccess = (mediaDetails) => ({
	type: LOAD_EVENT_ASSET_MEDIA_DETAILS_SUCCESS,
	payload: {
		mediaDetails,
	},
});

export const loadEventAssetMediaDetailsFailure = () => ({
	type: LOAD_EVENT_ASSET_MEDIA_DETAILS_FAILURE,
});

export const loadEventAssetPublicationsSuccess = (publications) => ({
	type: LOAD_EVENT_ASSET_PUBLICATIONS_SUCCESS,
	payload: {
		publications,
	},
});

export const loadEventAssetPublicationsFailure = () => ({
	type: LOAD_EVENT_ASSET_PUBLICATIONS_FAILURE,
});

export const loadEventAssetPublicationDetailsSuccess = (
	publicationDetails
) => ({
	type: LOAD_EVENT_ASSET_PUBLICATION_DETAILS_SUCCESS,
	payload: {
		publicationDetails,
	},
});

export const loadEventAssetPublicationDetailsFailure = () => ({
	type: LOAD_EVENT_ASSET_PUBLICATION_DETAILS_FAILURE,
});

// actions for access policies
export const loadEventPoliciesInProgress = () => ({
	type: LOAD_EVENT_POLICIES_IN_PROGRESS,
});

export const loadEventPoliciesSuccess = (policies) => ({
	type: LOAD_EVENT_POLICIES_SUCCESS,
	payload: {
		policies,
	},
});

export const loadEventPoliciesFailure = () => ({
	type: LOAD_EVENT_POLICIES_FAILURE,
});

// actions for comments
export const loadEventCommentsInProgress = () => ({
	type: LOAD_EVENT_COMMENTS_IN_PROGRESS,
});

export const loadEventCommentsSuccess = (comments, commentReasons) => ({
	type: LOAD_EVENT_COMMENTS_SUCCESS,
	payload: {
		comments,
		commentReasons,
	},
});

export const loadEventCommentsFailure = () => ({
	type: LOAD_EVENT_COMMENTS_FAILURE,
});

export const saveCommentInProgress = () => ({
	type: SAVE_COMMENT_IN_PROGRESS,
});

export const saveCommentDone = () => ({
	type: SAVE_COMMENT_DONE,
});

export const saveCommentReplyInProgress = () => ({
	type: SAVE_COMMENT_REPLY_IN_PROGRESS,
});

export const saveCommentReplyDone = () => ({
	type: SAVE_COMMENT_REPLY_DONE,
});

// actions for publications
export const loadEventPublicationsInProgress = () => ({
	type: LOAD_EVENT_PUBLICATIONS_IN_PROGRESS,
});

export const loadEventPublicationsSuccess = (publications) => ({
	type: LOAD_EVENT_PUBLICATIONS_SUCCESS,
	payload: {
		publications,
	},
});

export const loadEventPublicationsFailure = () => ({
	type: LOAD_EVENT_PUBLICATIONS_FAILURE,
});

// actions for scheduling
export const loadEventSchedulingInProgress = () => ({
	type: LOAD_EVENT_SCHEDULING_IN_PROGRESS,
});

export const loadEventSchedulingSuccess = (source) => ({
	type: LOAD_EVENT_SCHEDULING_SUCCESS,
	payload: {
		source,
	},
});

export const loadEventSchedulingFailure = () => ({
	type: LOAD_EVENT_SCHEDULING_FAILURE,
});

export const checkConflictsInProgress = () => ({
	type: CHECK_CONFLICTS_IN_PROGRESS,
});

export const checkConflictsSuccess = (conflicts) => ({
	type: CHECK_CONFLICTS_SUCCESS,
	payload: {
		conflicts,
	},
});

export const checkConflictsFailure = () => ({
	type: CHECK_CONFLICTS_FAILURE,
});
export const saveEventSchedulingInProgress = () => ({
	type: SAVE_EVENT_SCHEDULING_IN_PROGRESS,
});

export const saveEventSchedulingSuccess = (source) => ({
	type: SAVE_EVENT_SCHEDULING_SUCCESS,
	payload: {
		source,
	},
});

export const saveEventSchedulingFailure = () => ({
	type: SAVE_EVENT_SCHEDULING_FAILURE,
});

// actions for workflows
export const loadEventWorkflowsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOWS_IN_PROGRESS,
});

export const loadEventWorkflowsSuccess = (workflows) => ({
	type: LOAD_EVENT_WORKFLOWS_SUCCESS,
	payload: {
		workflows,
	},
});

export const loadEventWorkflowsFailure = () => ({
	type: LOAD_EVENT_WORKFLOWS_FAILURE,
});

export const loadEventWorkflowDetailsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOW_DETAILS_IN_PROGRESS,
});

export const loadEventWorkflowDetailsSuccess = (workflowDetails) => ({
	type: LOAD_EVENT_WORKFLOW_DETAILS_SUCCESS,
	payload: {
		workflowDetails,
	},
});

export const loadEventWorkflowDetailsFailure = () => ({
	type: LOAD_EVENT_WORKFLOW_DETAILS_FAILURE,
});

export const loadEventWorkflowOperationsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOW_OPERATIONS_IN_PROGRESS,
});

export const loadEventWorkflowOperationsSuccess = (workflowOperations) => ({
	type: LOAD_EVENT_WORKFLOW_OPERATIONS_SUCCESS,
	payload: {
		workflowOperations,
	},
});

export const loadEventWorkflowOperationsFailure = () => ({
	type: LOAD_EVENT_WORKFLOW_OPERATIONS_FAILURE,
});

export const loadEventWorkflowOperationDetailsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_IN_PROGRESS,
});

export const loadEventWorkflowOperationDetailsSuccess = (
	workflowOperationDetails
) => ({
	type: LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_SUCCESS,
	payload: {
		workflowOperationDetails,
	},
});

export const loadEventWorkflowOperationDetailsFailure = () => ({
	type: LOAD_EVENT_WORKFLOW_OPERATION_DETAILS_FAILURE,
});

export const loadEventWorkflowErrorsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOW_ERRORS_IN_PROGRESS,
});

export const loadEventWorkflowErrorsSuccess = (workflowErrors) => ({
	type: LOAD_EVENT_WORKFLOW_ERRORS_SUCCESS,
	payload: {
		workflowErrors,
	},
});

export const loadEventWorkflowErrorsFailure = () => ({
	type: LOAD_EVENT_WORKFLOW_ERRORS_FAILURE,
});

export const loadEventWorkflowErrorDetailsInProgress = () => ({
	type: LOAD_EVENT_WORKFLOW_ERROR_DETAILS_IN_PROGRESS,
});

export const loadEventWorkflowErrorDetailsSuccess = (workflowErrorDetails) => ({
	type: LOAD_EVENT_WORKFLOW_ERROR_DETAILS_SUCCESS,
	payload: {
		workflowErrorDetails,
	},
});

export const loadEventWorkflowErrorDetailsFailure = () => ({
	type: LOAD_EVENT_WORKFLOW_ERROR_DETAILS_FAILURE,
});

export const setEventWorkflowDefinitions = (
	workflows,
	workflowDefinitions
) => ({
	type: SET_EVENT_WORKFLOW_DEFINITIONS,
	payload: {
		workflows,
		workflowDefinitions,
	},
});

export const setEventWorkflow = (workflow) => ({
	type: SET_EVENT_WORKFLOW,
	payload: {
		workflow,
	},
});

export const setEventWorkflowConfiguration = (workflow_configuration) => ({
	type: SET_EVENT_WORKFLOW_CONFIGURATION,
	payload: {
		workflow_configuration,
	},
});

export const doEventWorkflowActionInProgress = () => ({
	type: DO_EVENT_WORKFLOW_ACTION_IN_PROGRESS,
});

export const doEventWorkflowActionSuccess = () => ({
	type: DO_EVENT_WORKFLOW_ACTION_SUCCESS,
});

export const doEventWorkflowActionFailure = () => ({
	type: DO_EVENT_WORKFLOW_ACTION_FAILURE,
});

export const deleteEventWorkflowInProgress = () => ({
	type: DELETE_EVENT_WORKFLOW_IN_PROGRESS,
});

export const deleteEventWorkflowSuccess = (workflowsEntries) => ({
	type: DELETE_EVENT_WORKFLOW_SUCCESS,
	payload: {
		workflowsEntries,
	},
});

export const deleteEventWorkflowFailure = () => ({
	type: DELETE_EVENT_WORKFLOW_FAILURE,
});

// actions for statistics
export const loadEventStatisticsInProgress = () => ({
	type: LOAD_EVENT_STATISTICS_IN_PROGRESS,
});

export const loadEventStatisticsSuccess = (statistics, hasError) => ({
	type: LOAD_EVENT_STATISTICS_SUCCESS,
	payload: {
		statistics,
		hasError,
	},
});

export const loadEventStatisticsFailure = (hasError) => ({
	type: LOAD_EVENT_STATISTICS_FAILURE,
	payload: {
		hasError,
	},
});

export const updateEventStatisticsSuccess = (statistics) => ({
	type: UPDATE_EVENT_STATISTICS_SUCCESS,
	payload: {
		statistics,
	},
});

export const updateEventStatisticsFailure = () => ({
	type: UPDATE_EVENT_STATISTICS_FAILURE,
});

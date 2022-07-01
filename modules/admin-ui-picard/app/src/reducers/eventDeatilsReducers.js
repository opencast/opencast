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
    LOAD_EVENT_METADATA_IN_PROGRESS,
    LOAD_EVENT_METADATA_SUCCESS,
    LOAD_EVENT_METADATA_FAILURE,
    SET_EVENT_METADATA,
    SET_EXTENDED_EVENT_METADATA,
    LOAD_EVENT_ASSETS_IN_PROGRESS,
    LOAD_EVENT_ASSETS_SUCCESS,
    LOAD_EVENT_ASSETS_FAILURE,
    LOAD_EVENT_ASSET_ATTACHMENTS_SUCCESS,
    LOAD_EVENT_ASSET_ATTACHMENTS_FAILURE,
    LOAD_EVENT_ASSET_CATALOGS_SUCCESS,
    LOAD_EVENT_ASSET_CATALOGS_FAILURE,
    LOAD_EVENT_ASSET_MEDIA_SUCCESS,
    LOAD_EVENT_ASSET_MEDIA_FAILURE,
    LOAD_EVENT_ASSET_PUBLICATIONS_SUCCESS,
    LOAD_EVENT_ASSET_PUBLICATIONS_FAILURE,
    LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_SUCCESS,
    LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_FAILURE,
    LOAD_EVENT_ASSET_PUBLICATION_DETAILS_FAILURE,
    LOAD_EVENT_ASSET_PUBLICATION_DETAILS_SUCCESS,
    LOAD_EVENT_ASSET_MEDIA_DETAILS_FAILURE,
    LOAD_EVENT_ASSET_MEDIA_DETAILS_SUCCESS,
    LOAD_EVENT_ASSET_CATALOG_DETAILS_FAILURE,
    LOAD_EVENT_ASSET_CATALOG_DETAILS_SUCCESS,
    LOAD_EVENT_SCHEDULING_IN_PROGRESS,
    LOAD_EVENT_SCHEDULING_SUCCESS,
    LOAD_EVENT_SCHEDULING_FAILURE,
    CHECK_CONFLICTS_FAILURE,
    CHECK_CONFLICTS_SUCCESS,
    CHECK_CONFLICTS_IN_PROGRESS,
    SAVE_EVENT_SCHEDULING_IN_PROGRESS,
    SAVE_EVENT_SCHEDULING_SUCCESS,
    SAVE_EVENT_SCHEDULING_FAILURE,
    LOAD_EVENT_STATISTICS_IN_PROGRESS,
    LOAD_EVENT_STATISTICS_SUCCESS,
    LOAD_EVENT_STATISTICS_FAILURE,
    UPDATE_EVENT_STATISTICS_SUCCESS,
    UPDATE_EVENT_STATISTICS_FAILURE,
} from '../actions/eventDetailsActions';

// Initial state of event details in redux store
const initialState = {
    eventId: "",
    metadata: {},
    extendedMetadata: [],
    fetchingMetadataInProgress: false,
    assets: {
        attachments: null,
        catalogs: null,
        media: null,
        publications: null
    },
    transactionsReadOnly: false,
    uploadAssetOptions: [],
    assetAttachments: [],
    assetAttachmentDetails: {
        id: "",
        type: "",
        mimetype: "",
        size: null,
        checksum: null,
        reference: "",
        tags: [],
        url: ""
    },
    assetCatalogs: [],
    assetCatalogDetails: {
        id: "",
        type: "",
        mimetype: "",
        size: null,
        checksum: null,
        reference: "",
        tags: [],
        url: ""
    },
    assetMedia: [],
    assetMediaDetails: {
        id: "",
        type: "",
        mimetype: "",
        tags: [],
        duration: null,
        size: null,
        url: "",
        streams: {
            audio: [],
            video: []
        },
        video: ""
    },
    assetPublications: [],
    assetPublicationDetails: {
        id: "",
        type: "",
        mimetype: "",
        size: null,
        channel: "",
        reference: "",
        tags: [],
        url: ""
    },
    fetchingAssetsInProgress: false,
    policies: [],
    fetchingPoliciesInProgress: false,
    savingCommentReplyInProgress: false,
    savingCommentInProgress: false,
    fetchingCommentsInProgress: false,
    comments: [],
    commentReasons: [],
    fetchingSchedulingInProgress: false,
    savingSchedulingInProgress: false,
    scheduling: {
        hasProperties: false
    },
    schedulingSource: {
        start: {
            date: "",
            hour: null,
            minute: null
        },
        duration: {
            hour: null,
            minute: null
        },
        end: {
            date: "",
            hour: null,
            minute: null
        },
        device: {
            name: "",
            inputs: [],
            inputMethods: []
        }
    },
    checkingConflicts: false,
    schedulingConflicts: [],
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
    publications: [],
    fetchingStatisticsInProgress: false,
    statistics: [],
    hasStatisticsError: false
}

// Reducer for event details
const eventDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_EVENT_METADATA_IN_PROGRESS: {
            return {
                ...state,
                fetchingMetadataInProgress: true,
            };
        }
        case LOAD_EVENT_METADATA_SUCCESS: {
            const { metadata, extendedMetadata } = payload;
            return {
                ...state,
                fetchingMetadataInProgress: false,
                metadata: metadata,
                extendedMetadata: extendedMetadata
            };
        }
        case LOAD_EVENT_METADATA_FAILURE: {
            return {
                ...state,
                fetchingMetadataInProgress: false,
                metadata: {},
                extendedMetadata: []
            };
        }
        case SET_EVENT_METADATA: {
            const { metadata } = payload;
            return {
                ...state,
                metadata: metadata
            };
        }
        case SET_EXTENDED_EVENT_METADATA: {
            const { metadata } = payload;

            return {
                ...state,
                extendedMetadata: metadata
            };
        }
        case LOAD_EVENT_ASSETS_IN_PROGRESS: {
            return {
                ...state,
                fetchingAssetsInProgress: true
            };
        }
        case LOAD_EVENT_ASSETS_SUCCESS: {
            const { assets, transactionsReadOnly, uploadAssetOptions } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assets: assets,
                transactionsReadOnly: transactionsReadOnly,
                uploadAssetOptions: uploadAssetOptions
            };
        }
        case LOAD_EVENT_ASSETS_FAILURE: {
            const emptyAssets = {
                attachments: null,
                catalogs: null,
                media: null,
                publications: null
            };

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assets: emptyAssets,
                transactionsReadOnly: false,
                uploadAssetOptions: []
            };
        }
        case LOAD_EVENT_ASSET_ATTACHMENTS_SUCCESS: {
            const { attachments } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetAttachments: attachments
            };
        }
        case LOAD_EVENT_ASSET_ATTACHMENTS_FAILURE: {
            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetAttachments: []
            };
        }
        case LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_SUCCESS: {
            const { attachmentDetails } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetAttachmentDetails: attachmentDetails
            };
        }
        case LOAD_EVENT_ASSET_ATTACHMENT_DETAILS_FAILURE: {
            const emptyAssetAttachmentDetails = {
                id: "",
                type: "",
                mimetype: "",
                size: null,
                checksum: null,
                reference: "",
                tags: [],
                url: ""
            };

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetAttachmentDetails: emptyAssetAttachmentDetails
            };
        }
        case LOAD_EVENT_ASSET_CATALOGS_SUCCESS: {
            const { catalogs } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetCatalogs: catalogs
            };
        }
        case LOAD_EVENT_ASSET_CATALOGS_FAILURE: {
            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetCatalogs: []
            };
        }
        case LOAD_EVENT_ASSET_CATALOG_DETAILS_SUCCESS: {
            const { catalogDetails } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetCatalogDetails: catalogDetails
            };
        }
        case LOAD_EVENT_ASSET_CATALOG_DETAILS_FAILURE: {
            const emptyAssetCatalogDetails = {
                id: "",
                type: "",
                mimetype: "",
                size: null,
                checksum: null,
                reference: "",
                tags: [],
                url: ""
            };

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetCatalogDetails: emptyAssetCatalogDetails
            };
        }
        case LOAD_EVENT_ASSET_MEDIA_SUCCESS: {
            const { media } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetMedia: media
            };
        }
        case LOAD_EVENT_ASSET_MEDIA_FAILURE: {
            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetMedia: []
            };
        }
        case LOAD_EVENT_ASSET_MEDIA_DETAILS_SUCCESS: {
            const { mediaDetails } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetMediaDetails: mediaDetails
            };
        }
        case LOAD_EVENT_ASSET_MEDIA_DETAILS_FAILURE: {
            const emptyAssetMediaDetails = {
                id: "",
                type: "",
                mimetype: "",
                tags: [],
                duration: null,
                size: null,
                url: "",
                streams: {
                    audio: [],
                    video: []
                },
                video: ""
            };

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetMediaDetails: emptyAssetMediaDetails
            };
        }
        case LOAD_EVENT_ASSET_PUBLICATIONS_SUCCESS: {
            const { publications } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetPublications: publications
            };
        }
        case LOAD_EVENT_ASSET_PUBLICATIONS_FAILURE: {
            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetPublications: []
            };
        }
        case LOAD_EVENT_ASSET_PUBLICATION_DETAILS_SUCCESS: {
            const { publicationDetails } = payload;

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetPublicationDetails: publicationDetails
            };
        }
        case LOAD_EVENT_ASSET_PUBLICATION_DETAILS_FAILURE: {
            const emptyAssetPublicationDetails = {
                id: "",
                type: "",
                mimetype: "",
                size: null,
                channel: "",
                reference: "",
                tags: [],
                url: ""
            }

            return {
                ...state,
                fetchingAssetsInProgress: false,
                assetPublicationDetails: emptyAssetPublicationDetails
            };
        }
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
        case LOAD_EVENT_SCHEDULING_IN_PROGRESS: {
            return {
                ...state,
                fetchingSchedulingInProgress: true
            };
        }
        case LOAD_EVENT_SCHEDULING_SUCCESS: {
            const { source } = payload;
            return {
                ...state,
                fetchingSchedulingInProgress: false,
                schedulingSource: source,
                scheduling: {
                    ...state.scheduling,
                    hasProperties: true
                }
            };
        }
        case LOAD_EVENT_SCHEDULING_FAILURE: {
            const emptySchedulingSource = {
                start: {
                    date: "",
                    hour: null,
                    minute: null
                },
                duration: {
                    hour: null,
                    minute: null
                },
                end: {
                    date: "",
                    hour: null,
                    minute: null
                },
                device: {
                    name: "",
                    inputs: [],
                    inputMethods: []
                }
            };

            return {
                ...state,
                fetchingSchedulingInProgress: false,
                schedulingSource: emptySchedulingSource,
                scheduling: {
                    ...state.scheduling,
                    hasProperties: false
                }
            };
        }
        case SAVE_EVENT_SCHEDULING_IN_PROGRESS: {
            return {
                ...state,
                savingSchedulingInProgress: true
            };
        }
        case SAVE_EVENT_SCHEDULING_SUCCESS: {
            const { source } = payload;
            return {
                ...state,
                savingSchedulingInProgress: false,
                schedulingSource: source
            };
        }
        case SAVE_EVENT_SCHEDULING_FAILURE: {
            return {
                ...state,
                savingSchedulingInProgress: false,
            };
        }
        case CHECK_CONFLICTS_IN_PROGRESS: {
            return {
                ...state,
                checkingConflicts: true
            };
        }
        case CHECK_CONFLICTS_SUCCESS: {
            const { conflicts } = payload;
            return {
                ...state,
                checkingConflicts: false,
                schedulingConflicts: conflicts
            };
        }
        case CHECK_CONFLICTS_FAILURE: {
            return {
                ...state,
                checkingConflicts: false,
                schedulingConflicts: []
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
        case LOAD_EVENT_STATISTICS_IN_PROGRESS: {
            return {
                ...state,
                fetchingStatisticsInProgress: true,
            };
        }
        case LOAD_EVENT_STATISTICS_SUCCESS: {
            const { statistics, hasError } = payload;
            return {
                ...state,
                fetchingStatisticsInProgress: false,
                statistics: statistics,
                hasStatisticsError: hasError
            };
        }
        case LOAD_EVENT_STATISTICS_FAILURE: {
            const { hasError } = payload;
            return {
                ...state,
                fetchingStatisticsInProgress: false,
                statistics: [],
                hasStatisticsError: hasError
            };
        }
        case UPDATE_EVENT_STATISTICS_SUCCESS: {
            const { statistics } = payload;
            return {
                ...state,
                statistics: statistics
            };
        }
        case UPDATE_EVENT_STATISTICS_FAILURE: {
            return {
                ...state
            };
        }
        default:
            return state;
    }
};



export default eventDetails;

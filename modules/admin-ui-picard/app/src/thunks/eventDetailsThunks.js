import axios from "axios";
import {
    loadEventPoliciesInProgress,
    loadEventPoliciesSuccess,
    loadEventPoliciesFailure,
    loadEventCommentsInProgress,
    loadEventCommentsSuccess,
    loadEventCommentsFailure,
    saveCommentInProgress,
    saveCommentDone,
    saveCommentReplyInProgress,
    saveCommentReplyDone,
    loadEventWorkflowsInProgress,
    loadEventWorkflowsSuccess,
    loadEventWorkflowsFailure,
    setEventWorkflowDefinitions,
    setEventWorkflow,
    setEventWorkflowConfiguration,
    doEventWorkflowActionInProgress,
    doEventWorkflowActionSuccess,
    doEventWorkflowActionFailure,
    deleteEventWorkflowInProgress,
    deleteEventWorkflowFailure,
    deleteEventWorkflowSuccess,
    loadEventPublicationsInProgress,
    loadEventPublicationsSuccess,
    loadEventPublicationsFailure,
    loadEventWorkflowDetailsInProgress,
    loadEventWorkflowDetailsSuccess,
    loadEventWorkflowDetailsFailure,
    loadEventWorkflowOperationsInProgress,
    loadEventWorkflowOperationsSuccess,
    loadEventWorkflowOperationsFailure,
    loadEventWorkflowOperationDetailsFailure,
    loadEventWorkflowOperationDetailsSuccess,
    loadEventWorkflowOperationDetailsInProgress,
    loadEventWorkflowErrorsInProgress,
    loadEventWorkflowErrorsSuccess,
    loadEventWorkflowErrorsFailure,
    loadEventWorkflowErrorDetailsInProgress,
    loadEventWorkflowErrorDetailsSuccess,
    loadEventWorkflowErrorDetailsFailure,
} from '../actions/eventDetailsActions';
import {addNotification} from "./notificationThunks";
import {createPolicy} from "../utils/resourceUtils";
import {NOTIFICATION_CONTEXT} from "../configs/modalConfig";
import {getBaseWorkflow, getWorkflow, getWorkflowDefinitions, getWorkflows} from "../selectors/eventDetailsSelectors";
import {fetchWorkflowDef} from "./workflowThunks";
import {getWorkflowDef} from "../selectors/workflowSelectors";
import {logger} from "../utils/logger";

// prepare http headers for posting to resources
const getHttpHeaders = () => {
    return {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
    };
}


// thunks for access policies
export const saveAccessPolicies = (eventId, policies) => async (dispatch) => {

    let headers = getHttpHeaders();

    let data = new URLSearchParams();
    data.append("acl", JSON.stringify(policies));
    data.append("override", true);

    return axios.post(`admin-ng/event/${eventId}/access`, data.toString(), headers)
        .then(response => {
            logger.info(response);
            dispatch(addNotification('info', 'SAVED_ACL_RULES', -1, null, NOTIFICATION_CONTEXT));
            return true;
        })
        .catch(response => {
            logger.error(response);
            dispatch(addNotification('error', 'ACL_NOT_SAVED', -1, null, NOTIFICATION_CONTEXT));
            return false;
        });
}

export const fetchAccessPolicies = (eventId) => async (dispatch) => {
    try {
        dispatch(loadEventPoliciesInProgress());

        const policyData = await axios.get(`admin-ng/event/${eventId}/access.json`);
        let accessPolicies = await policyData.data;

        let policies = [];
        if(!!accessPolicies.episode_access){
            const json = JSON.parse(accessPolicies.episode_access.acl).acl.ace;
            let newPolicies = {};
            let policyRoles = [];
            for(let i = 0; i < json.length; i++){
                const policy = json[i];
                if(!newPolicies[policy.role]){
                    newPolicies[policy.role] = createPolicy(policy.role);
                    policyRoles.push(policy.role);
                }
                if (policy.action === 'read' || policy.action === 'write') {
                    newPolicies[policy.role][policy.action] = policy.allow;
                } else if (policy.allow === true || policy.allow === 'true'){
                    newPolicies[policy.role].actions.push(policy.action);
                }
            }
            policies = policyRoles.map(role => newPolicies[role]);
        }

        dispatch(loadEventPoliciesSuccess(policies));
    } catch (e) {
        logger.error(e);
        dispatch(loadEventPoliciesFailure());
    }
}

export const fetchHasActiveTransactions = (eventId) => async () => {
    try {
        const transactionsData = await axios.get(`admin-ng/event/${eventId}/hasActiveTransaction`);
        const hasActiveTransactions = await transactionsData.data;
        return hasActiveTransactions;
    } catch (e) {
        logger.error(e);
    }
}


// thunks for comments

export const fetchComments = (eventId) => async (dispatch) => {
    try {
        dispatch(loadEventCommentsInProgress());

        const commentsData = await axios.get(`admin-ng/event/${eventId}/comments`);
        const comments = await commentsData.data;

        const commentReasonsData = await axios.get(`admin-ng/resources/components.json`);
        const commentReasons = (await commentReasonsData.data).eventCommentReasons;

        dispatch(loadEventCommentsSuccess(comments, commentReasons));
    } catch (e) {
        dispatch(loadEventCommentsFailure());
        logger.error(e);
    }
}

export const saveComment = (eventId, commentText, commentReason) => async (dispatch) => {
    try {
        dispatch(saveCommentInProgress());

        let headers = getHttpHeaders();

        let data = new URLSearchParams();
        data.append("text", commentText);
        data.append("reason", commentReason);

        const commentSaved = await axios.post(`admin-ng/event/${eventId}/comment`,
            data.toString(), headers );
        await commentSaved.data;

        dispatch(saveCommentDone());
        return true;
    } catch (e) {
        dispatch(saveCommentDone());
        logger.error(e);
        return false;
    }
}

export const deleteComment = (eventId, commentId) => async () => {
    try {
        const commentDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}`);
        await commentDeleted.data;
        return true;
    } catch (e) {
        logger.error(e);
        return false;
    }
}

export const saveCommentReply = (eventId, commentId, replyText, commentResolved) => async (dispatch) => {
    try {
        dispatch(saveCommentReplyInProgress());

        let headers = getHttpHeaders();

        let data = new URLSearchParams();
        data.append("text", replyText);
        data.append("resolved", commentResolved);

        const commentReply = await axios.post(`admin-ng/event/${eventId}/comment/${commentId}/reply`,
            data.toString(), headers );

        await commentReply.data;

        dispatch(saveCommentReplyDone());
        return true;
    } catch (e) {
        dispatch(saveCommentReplyDone());
        logger.error(e);
        return false;
    }
}

export const deleteCommentReply = (eventId, commentId, replyId) => async () => {
    try {
        const commentReplyDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}/${replyId}`);
        await commentReplyDeleted.data;

        return true;
    } catch (e) {
        logger.error(e);
        return false;
    }
}


// thunks for workflows

export const fetchWorkflows = (eventId) => async (dispatch, getState) => {
    try {
        dispatch(loadEventWorkflowsInProgress());

        // todo: show notification if there are active transactions
        // dispatch(addNotification('warning', 'ACTIVE_TRANSACTION', -1, null, NOTIFICATION_CONTEXT));

        const data = await axios.get(`admin-ng/event/${eventId}/workflows.json`);
        const workflowsData = await data.data;

        if(!!workflowsData.results){
            const workflows = {
                entries: workflowsData.results,
                scheduling: false,
                workflow: {
                    id: "",
                    description: ""
                }
            };

            dispatch(loadEventWorkflowsSuccess(workflows));
        } else {
            const workflows = {
                workflow: workflowsData,
                scheduling: true,
                entries: []
            };

            await dispatch(fetchWorkflowDef('event-details'));

            const state = getState();

            const workflowDefinitions = getWorkflowDef(state);

            dispatch(setEventWorkflowDefinitions(workflows, workflowDefinitions));
            dispatch(changeWorkflow(false));

            dispatch(loadEventWorkflowsSuccess(workflows));
        }
    } catch (e) {
        dispatch(loadEventWorkflowsFailure());
        logger.error(e);
    }
}

export const fetchWorkflowDetails = (eventId, workflowId) => async (dispatch) => {

    try {
        dispatch(loadEventWorkflowDetailsInProgress());

        const data = await axios.get(`/admin-ng/event/${eventId}/workflows/${workflowId}.json`);
        const workflowData = await data.data;
        dispatch(loadEventWorkflowDetailsSuccess(workflowData));
    } catch (e) {
        dispatch(loadEventWorkflowDetailsFailure())
        // todo: probably needs a Notification to the user
        logger.error(e);
    }
}

const changeWorkflow = (saveWorkflow) => async (dispatch, getState) => {
    const state = getState();
    const workflow = getWorkflow(state);

    if(!!workflow.workflowId){
        dispatch(setEventWorkflowConfiguration(workflow));
    } else {
        dispatch(setEventWorkflowConfiguration(getBaseWorkflow(state)));
    }
    if(saveWorkflow){
        saveWorkflowConfig();
    }
}

export const updateWorkflow = (saveWorkflow, workflowId) => async (dispatch, getState) => {
    const state = getState();
    const workflowDefinitions = getWorkflowDefinitions(state);
    const workflowDef = workflowDefinitions.find(def => def.id === workflowId);
    await dispatch(setEventWorkflow({
        workflowId: workflowId,
        description: workflowDef.description,
        configuration: workflowDef.configuration
    }));
    dispatch(changeWorkflow(saveWorkflow));
}

const saveWorkflowConfig = () => {
    //todo
}

export const performWorkflowAction = (eventId, workflowId, action, close) => async (dispatch) => {
    dispatch(doEventWorkflowActionInProgress());

    let headers = {headers: {
        'Content-Type': 'application/json;charset=utf-8'
    }};

    let data = {
        "action": action,
        "id": eventId,
        "wfId": workflowId
    };

    axios.put(`admin-ng/event/${eventId}/workflows/${workflowId}/action/${action}`, data, headers)
        .then( response => {
            dispatch(addNotification('success', 'EVENTS_PROCESSING_ACTION_' + action, -1, null, NOTIFICATION_CONTEXT));
            close();
            dispatch(doEventWorkflowActionSuccess());
        })
        .catch( response => {
            dispatch(addNotification('error', 'EVENTS_PROCESSING_ACTION_NOT_' + action, -1, null, NOTIFICATION_CONTEXT));
            dispatch(doEventWorkflowActionFailure());
        });
}

export const deleteWorkflow = (eventId, workflowId) => async (dispatch, getState) => {
    dispatch(deleteEventWorkflowInProgress());

    axios.delete(`/admin-ng/event/${eventId}/workflows/${workflowId}`)
        .then( response => {
            dispatch(addNotification('success', 'EVENTS_PROCESSING_DELETE_WORKFLOW', -1, null, NOTIFICATION_CONTEXT));

            const state = getState();
            const workflows = getWorkflows(state);

            if(!!workflows.entries){
                dispatch(deleteEventWorkflowSuccess(workflows.entries.filter( wf => wf.id !== workflowId)));
            } else {
                dispatch(deleteEventWorkflowSuccess(workflows.entries));
            }
        })
        .catch( response => {
            dispatch(addNotification('error', 'EVENTS_PROCESSING_DELETE_WORKFLOW_FAILED', -1, null, NOTIFICATION_CONTEXT));
            dispatch(deleteEventWorkflowFailure());
        });
}

export const fetchWorkflowOperations = (eventId, workflowId) => async (dispatch) => {
    try {
        dispatch(loadEventWorkflowOperationsInProgress());

        const data = await axios.get(`/admin-ng/event/${eventId}/workflows/${workflowId}/operations.json`);
        const workflowOperationsData = await data.data;
        const workflowOperations = {entries: workflowOperationsData}
        dispatch(loadEventWorkflowOperationsSuccess(workflowOperations));
    } catch (e) {
        dispatch(loadEventWorkflowOperationsFailure())
        // todo: probably needs a Notification to the user
        logger.error(e);
    }
}

export const fetchWorkflowOperationDetails = (eventId, workflowId, operationId) => async (dispatch) => {
    try {
        dispatch(loadEventWorkflowOperationDetailsInProgress());

        const data = await axios.get(`/admin-ng/event/${eventId}/workflows/${workflowId}/operations/${operationId}`);
        const workflowOperationDetails = await data.data;
        dispatch(loadEventWorkflowOperationDetailsSuccess(workflowOperationDetails));
    } catch (e) {
        dispatch(loadEventWorkflowOperationDetailsFailure())
        // todo: probably needs a Notification to the user
        logger.error(e);
    }
}

export const fetchWorkflowErrors = (eventId, workflowId) => async (dispatch) => {
    try {
        dispatch(loadEventWorkflowErrorsInProgress());

        const data = await axios.get(`/admin-ng/event/${eventId}/workflows/${workflowId}/errors.json`);
        const workflowErrorsData = await data.data;
        const workflowErrors = {entries: workflowErrorsData}
        dispatch(loadEventWorkflowErrorsSuccess(workflowErrors));
    } catch (e) {
        dispatch(loadEventWorkflowErrorsFailure())
        // todo: probably needs a Notification to the user
        logger.error(e);
    }
}

export const fetchWorkflowErrorDetails = (eventId, workflowId, errorId) => async (dispatch) => {
    try {
        dispatch(loadEventWorkflowErrorDetailsInProgress());

        const data = await axios.get(`/admin-ng/event/${eventId}/workflows/${workflowId}/errors/${errorId}.json`);
        const workflowErrorDetails = await data.data;
        dispatch(loadEventWorkflowErrorDetailsSuccess(workflowErrorDetails));
    } catch (e) {
        dispatch(loadEventWorkflowErrorDetailsFailure())
        // todo: probably needs a Notification to the user
        logger.error(e);
    }
}


// thunks for publications

export const fetchEventPublications = eventId => async dispatch => {
    try {
        dispatch(loadEventPublicationsInProgress());

        let data = await axios.get(`admin-ng/event/${eventId}/publications.json`);

        let publications = (await data.data);

        // get information about possible publication channels
        data = await axios.get('admin-ng/resources/PUBLICATION.CHANNELS.json');

        let publicationChannels = await data.data;

        let now = new Date();

        // fill publication objects with additional information
        publications.publications.forEach(publication => {

            publication.enabled =
                !(publication.id === 'engage-live' &&
                    (now < new Date(publications['start-date']) || now > new Date(publications['end-date'])));

            if (publicationChannels[publication.id]) {
                let channel = JSON.parse(publicationChannels[publication.id]);

                if (channel.label) {
                    publication.label = channel.label
                }
                if (channel.icon) {
                    publication.icon = channel.icon;
                }
                if (channel.hide) {
                    publication.hide = channel.hide;
                }
                if (channel.description) {
                    publication.description = channel.description;
                }
                if (channel.order) {
                    publication.order = channel.order;
                }
            }
        });

        dispatch(loadEventPublicationsSuccess(publications.publications));

    } catch (e) {
        dispatch(loadEventPublicationsFailure());
        logger.error(e);
    }
}

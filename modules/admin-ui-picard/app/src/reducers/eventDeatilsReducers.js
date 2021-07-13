import {
    LOAD_EVENT_POLICIES_SUCCESS,
    LOAD_EVENT_COMMENTS_IN_PROGRESS,
    LOAD_EVENT_COMMENTS_SUCCESS,
    LOAD_EVENT_COMMENTS_FAILURE,
    SAVE_COMMENT_IN_PROGRESS,
    SAVE_COMMENT_DONE,
    SAVE_COMMENT_REPLY_IN_PROGRESS,
    SAVE_COMMENT_REPLY_DONE,
} from '../actions/eventDetailsActions';

// Initial state of event details in redux store
const initialState = {
    policies: [],
    savingCommentReplyInProgress: false,
    savingCommentInProgress: false,
    fetchingCommentsInProgress: false,
    comments: [],
    commentReasons: [],
    eventId: ""
}

const createPolicy = (role) => {
    return {
        role: role,
        read: false,
        write: false,
        actions: []
    };
};

// Reducer for event details
const eventDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_EVENT_POLICIES_SUCCESS: {
            const { accessPolicies } = payload;
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
            return {
                ...state,
                fetchingPoliciesInProgress: false,
                policies: policies
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
        default:
            return state;
    }
};

export default eventDetails;
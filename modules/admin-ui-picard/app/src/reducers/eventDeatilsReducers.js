import {
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
    savingCommentReplyInProgress: false,
    savingCommentInProgress: false,
    fetchingCommentsInProgress: false,
    comments: [],
    commentReasons: [],
    eventId: ""
}

// Reducer for event details
const eventDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
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
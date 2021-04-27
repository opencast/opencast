export const LOAD_EVENT_COMMENTS_SUCCESS = 'LOAD_EVENT_COMMENTS_SUCCESS';
export const LOAD_EVENT_COMMENTS_FAILURE = 'LOAD_EVENT_COMMENTS_FAILURE';
export const LOAD_EVENT_COMMENTS_IN_PROGRESS = 'LOAD_EVENT_COMMENTS_IN_PROGRESS';
export const SAVE_COMMENT_DONE = 'SAVE_COMMENT_DONE';
export const SAVE_COMMENT_IN_PROGRESS = 'SAVE_COMMENT_IN_PROGRESS';
export const SAVE_COMMENT_REPLY_DONE = 'SAVE_COMMENT_REPLY_DONE';
export const SAVE_COMMENT_REPLY_IN_PROGRESS = 'SAVE_COMMENT_REPLY_IN_PROGRESS';

// Actions affecting fetching of event details from server

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

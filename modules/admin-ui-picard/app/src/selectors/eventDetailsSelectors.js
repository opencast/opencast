export const getPolicies = state => state.eventDetails.policies;
export const getComments = state => state.eventDetails.comments;
export const getCommentReasons = state => state.eventDetails.commentReasons;
export const isFetchingComments = state => state.eventDetails.fetchingCommentsInProgress;
export const isSavingComment = state => state.eventDetails.savingCommentInProgress;
export const isSavingCommentReply = state => state.eventDetails.savingCommentReplyInProgress;
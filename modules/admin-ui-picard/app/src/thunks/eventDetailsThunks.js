import {
    loadEventCommentsInProgress,
    loadEventCommentsSuccess,
    loadEventCommentsFailure,
    saveCommentInProgress,
    saveCommentDone,
    saveCommentReplyInProgress,
    saveCommentReplyDone,
} from '../actions/eventDetailsActions';
import axios from "axios";

export const fetchAccessPolicies = (eventId) => async (dispatch) => {
    try {
        const getData = async () => {
            const policyData = await axios.get(`admin-ng/event/${eventId}/access.json`);
            const policies = await policyData.data;
            return policies;
        }
        return getData();
    } catch (e) {
        console.log(e);
    }
}

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
        console.log(e);
    }
}

export const saveComment = (eventId, commentText, commentReason) => async (dispatch) => {
    try {
        dispatch(saveCommentInProgress());

        const commentSaved = await axios.post(`admin-ng/event/${eventId}/comment`,
            `text=${commentText}&reason=${commentReason}`);
        await commentSaved.data;

        dispatch(saveCommentDone());
        return true;
    } catch (e) {
        dispatch(saveCommentDone());
        console.log(e);
        return false;
    }
}

export const deleteComment = (eventId, commentId) => async (dispatch) => {
    try {
        const commentDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}`);
        await commentDeleted.data;
        return true;
    } catch (e) {
        console.log(e);
        return false;
    }
}

export const saveCommentReply = (eventId, commentId, replyText, commentResolved) => async (dispatch) => {
    try {
        dispatch(saveCommentReplyInProgress());

        const commentReply = await axios.post(`admin-ng/event/${eventId}/comment/${commentId}/reply`,
            `text=${replyText}&resolved=${commentResolved}`);

        await commentReply.data;

        dispatch(saveCommentReplyDone());
        return true;
    } catch (e) {
        dispatch(saveCommentReplyDone());
        console.log(e);
        return false;
    }
}

export const deleteCommentReply = (eventId, commentId, replyId) => async (dispatch) => {
    try {
        const commentReplyDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}/${replyId}`);
        await commentReplyDeleted.data;

        return true;
    } catch (e) {
        console.log(e);
        return false;
    }
}
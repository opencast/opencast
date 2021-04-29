import {
    loadEventCommentsInProgress,
    loadEventCommentsSuccess,
    loadEventCommentsFailure,
    saveCommentInProgress,
    saveCommentDone,
    saveCommentReplyInProgress,
    saveCommentReplyDone,
} from '../actions/eventDetailsActions';
import {
    getURLParams
} from "../utils/resourceUtils";
import axios from "axios";

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

export const saveComment = (eventId, commentText, commentReason) => async (dispatch, getState) => {
    try {
        dispatch(saveCommentInProgress());

        const state = getState();
        let params = getURLParams(state);

        const commentSaved = await axios.post(`admin-ng/event/${eventId}/comment`,
            `text=${commentText}&reason=${commentReason}`, { params: params });
        await commentSaved.data;

        dispatch(saveCommentDone());
        return true;
    } catch (e) {
        dispatch(saveCommentDone());
        console.log(e);
        return false;
    }
}

export const deleteComment = (eventId, commentId) => async (getState) => {
    try {
        const state = getState();
        let params = getURLParams(state);

        const commentDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}`,
            { params: params });
        await commentDeleted.data;
        return true;
    } catch (e) {
        console.log(e);
        return false;
    }
}

export const saveCommentReply = (eventId, commentId, replyText, commentResolved) => async (dispatch, getState) => {
    try {
        dispatch(saveCommentReplyInProgress());

        const state = getState();
        let params = getURLParams(state);

        const commentReply = await axios.post(`admin-ng/event/${eventId}/comment/${commentId}/reply`,
            `text=${replyText}&resolved=${commentResolved}`, { params: params });

        await commentReply.data;

        dispatch(saveCommentReplyDone());
        return true;
    } catch (e) {
        dispatch(saveCommentReplyDone());
        console.log(e);
        return false;
    }
}

export const deleteCommentReply = (eventId, commentId, replyId) => async (getState) => {
    try {
        const state = getState();
        let params = getURLParams(state);

        const commentReplyDeleted = await axios.delete(`admin-ng/event/${eventId}/comment/${commentId}/${replyId}`,
            { params: params });
        await commentReplyDeleted.data;

        return true;
    } catch (e) {
        console.log(e);
        return false;
    }
}
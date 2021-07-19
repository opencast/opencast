import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import {
    fetchComments,
    saveComment,
    saveCommentReply,
    deleteComment,
    deleteCommentReply
} from "../../../../thunks/eventDetailsThunks";
import {
    getComments,
    getCommentReasons,
    isFetchingComments,
    isSavingComment,
    isSavingCommentReply,
} from "../../../../selectors/eventDetailsSelectors";
import Notifications from "../../../shared/Notifications";

/**
 * This component manages the comment tab of the event details modal
 */
const EventDetailsCommentsTab = ({ eventId, header, t,
                                     loadComments, saveNewComment, saveNewCommentReply, deleteOneComment, deleteCommentReply,
                                     comments, isSavingComment, isSavingCommentReply, commentReasons }) => {

    useEffect( () => {
        loadComments(eventId).then(r => console.log(r));
    }, []);

    const [ replyToComment, setReplyToComment ] = useState(false);
    const [ replyCommentId, setReplyCommentId ] = useState(null);
    const [ originalComment, setOriginalComment ] = useState(null);
    const [ commentReplyText, setCommentReplyText ] = useState("");
    const [ commentReplyIsResolved, setCommentReplyIsResolved ] = useState(false);

    const [ newCommentText, setNewCommentText ] = useState("");
    const [ commentReason, setCommentReason ] = useState("");

    const saveComment = (commentText, commentReason) => {
        saveNewComment(eventId, commentText, commentReason).then((successful) => {
            if(successful){
                loadComments(eventId);
                setNewCommentText("");
                setCommentReason("");
            }
        });
    }

    const replyTo = (comment, key) => {
        setReplyToComment(true);
        setReplyCommentId(key);
        setOriginalComment(comment);
    };

    const exitReplyMode = () => {
        setReplyToComment(false);
        setReplyCommentId(null);
        setOriginalComment(null);
        setCommentReplyText("");
        setCommentReplyIsResolved(false);
    };

    const saveReply = (originalComment, reply, isResolved) => {
        saveNewCommentReply(eventId, originalComment.id, reply, isResolved).then((success) => {
            if(success){
                loadComments(eventId);
                exitReplyMode();
            }
        });
    }

    const deleteComment = (comment) => {
        deleteOneComment(eventId, comment.id).then((success) => {
            if(success){
                loadComments(eventId);
            }
        });
    }

    const deleteReply = (comment, reply) => {
        deleteCommentReply(eventId, comment.id, reply.id).then((success) => {
            if(success){
                loadComments(eventId);
            }
        });
    }

    // todo: add user and role management
    return (
        <div className="modal-content">
            <div className="modal-body">
                <Notifications context="not-corner"/>
                <div className="full-col">
                    <div className="obj comments">
                        <header>{t(header)}</header>
                        <div className="obj-container">
                            <div className="comment-container">
                                {
                                    comments.map( (comment, key) => (

                                        <div className={`comment ${(replyCommentId === key) ? 'active' : ''}`} key={key}>
                                            <hr/>
                                            <div className="date">{t('dateFormats.dateTime.short', {dateTime: new Date(comment.creationDate)}) || ''}</div>
                                            <h4>{ comment.author.name }</h4>
                                            <span className="category">
                                                <strong>{t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")}</strong>:
                                                { " " + t(comment.reason) || '' }
                                            </span>
                                            <p>{ comment.text }</p>
                                            <a onClick={() => deleteComment(comment)}
                                               className="delete">
                                                {t('EVENTS.EVENTS.DETAILS.COMMENTS.DELETE')}
                                            </a>
                                            <a onClick={() => replyTo(comment, key)}
                                               className="reply">{/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_REPLY" */}
                                                {t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY')}
                                            </a>
                                            <span className="resolve" ng-class="{ resolved : comment.resolvedStatus }">
                                                { t('EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED') }
                                            </span>

                                            {
                                                comment.replies.map( (reply, replyKey) => (
                                                    <div className="comment is-reply" key={replyKey}>
                                                        <hr/>
                                                        <div className="date">
                                                            {t('dateFormats.dateTime.short', {dateTime: new Date(reply.creationDate)}) || ''}
                                                        </div>
                                                        <h4>{ reply.author.name }</h4>
                                                        <span className="category">
                                                            <strong>{ t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")}</strong>:
                                                            { " " + t(comment.reason) || '' }
                                                        </span>
                                                        <p>
                                                            <span>@{ comment.author.name }</span> { reply.text }
                                                        </p>
                                                        <a onClick={ () => deleteReply(comment, reply, replyKey)}
                                                           className="delete">{/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE" */}
                                                            <i className="fa fa-times-circle"/>
                                                            { t('EVENTS.EVENTS.DETAILS.COMMENTS.DELETE') }
                                                        </a>
                                                    </div>
                                                ))
                                            }


                                        </div>

                                    ) )
                                }
                            </div>
                        </div>

                        {
                            replyToComment || (<form
                                className="add-comment">{/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_CREATE">*/}
                                <textarea
                                    value={newCommentText}
                                    onChange={ (comment) => setNewCommentText(comment.target.value)}
                                    placeholder={t('EVENTS.EVENTS.DETAILS.COMMENTS.PLACEHOLDER')}>
                                </textarea>
                                <div className="chosen-container chosen-container-single">
                                    <select className="chosen-single chosen-default"
                                            chosen
                                            value={commentReason}
                                            onChange={(newReason) =>
                                                setCommentReason(newReason.target.value)}
                                            pre-select-from="components.eventCommentReasons"
                                            data-width="'200px'"
                                    >
                                        <option value="" disabled selected hidden>
                                            {t('EVENTS.EVENTS.DETAILS.COMMENTS.SELECTPLACEHOLDER')}
                                        </option>
                                        {Object.entries(commentReasons).map( (reason, key) =>
                                            <option value={reason[0]} key={key}>{t(reason[1])}</option>
                                        )}
                                    </select>
                                </div>
                                <button
                                    disabled={ !!(!newCommentText.length || newCommentText.length <= 0 ||
                                        !commentReason.length || commentReason.length <= 0 ||
                                        isSavingComment)}
                                    className={`save green  ${(!newCommentText.length || newCommentText.length <= 0 ||
                                        !commentReason.length || commentReason.length <= 0 ||
                                        isSavingComment ) ?
                                        "disabled" : "false"}`}
                                    onClick={() => saveComment(newCommentText, commentReason)}
                                >
                                    {t("SUBMIT") /* Submit */}
                                </button>
                            </form>)
                        }

                        {
                            replyToComment && (<form className="add-comment reply">
                                <textarea
                                    value={commentReplyText}
                                    onChange={ (reply) => setCommentReplyText(reply.target.value)}
                                    placeholder={ t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY_TO') + "@" +  originalComment.author.name }>
                                </textarea>
                                <button
                                    disabled={ !!(!commentReplyText.length || commentReplyText.length <= 0 ||
                                        isSavingCommentReply)}
                                    className={`save green  ${(!commentReplyText.length || commentReplyText.length <= 0  ||
                                        isSavingCommentReply ) ?
                                        "disabled" : "false"}`}
                                    onClick={ () =>
                                        saveReply(originalComment, commentReplyText, commentReplyIsResolved)}
                                >
                                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY") /* Reply */}
                                </button>
                                <button className="red" onClick={() => exitReplyMode()} >
                                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.CANCEL_REPLY") /* Cancel */}
                                </button>
                                <input type="checkbox"
                                       id="resolved-checkbox"
                                       className="ios"
                                       onChange={ () =>
                                           setCommentReplyIsResolved(!commentReplyIsResolved)}
                                />{/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" */}
                                <label> {/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" */}
                                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED") /* Resolved */}
                                </label>
                            </form>)
                        }
                    </div>
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    comments: getComments(state),
    commentReasons: getCommentReasons(state),
    isFetchingComments: isFetchingComments(state),
    isSavingComment: isSavingComment(state),
    isSavingCommentReply: isSavingCommentReply(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadComments: eventId => dispatch(fetchComments(eventId)),
    saveNewComment: (eventId, commentText, commentReason) => dispatch(saveComment(eventId, commentText, commentReason)),
    saveNewCommentReply: (eventId, commentId, replyText, commentResolved) => dispatch(saveCommentReply(eventId, commentId, replyText, commentResolved)),
    deleteOneComment: (eventId, commentId) => dispatch(deleteComment(eventId, commentId)),
    deleteCommentReply: (eventId, commentId, replyId) => dispatch(deleteCommentReply(eventId, commentId, replyId))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsCommentsTab);
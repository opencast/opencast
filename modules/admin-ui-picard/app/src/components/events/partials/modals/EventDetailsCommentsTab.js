import React, {useState} from "react";
import {connect} from "react-redux";

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const EventDetailsCommentsTab = ({ header, t }) => {

    const replyReasons = [
        "Some reason",
        "Some other reason",
        "Very good reason"
    ]

    const reply1 = {
        creationDate: "2021-04-01T13:05",
        author: {
            name: "Author Of Reply"
        },
        text: "This is an example for a comment reply"
    };

    const reply2 = {
        creationDate: "2021-04-01T13:00",
        author: {
            name: "Author Of Reply2"
        },
        text: "This is a 2nd example for a comment reply"
    };

    const comment = {
        creationDate: "2021-04-01T12:00",
        author: {
            name: "Author Of Comment"
        },
        reason: "test reason",
        text: "This is an example for a comment",
        resolvedStatus: "resolved",
        replies: [
            reply1,
            reply2
        ]
    };

    const originalCommentInit = {
        creationDate: "2021-04-01T11:00",
        author: {
            name: "Author Of Some Other Comment"
        },
        reason: "test",
        text: "This is an example for an original comment",
        resolvedStatus: "open",
        replies: []
    };

    const comments = [comment, originalCommentInit];

    const localizeDate = false;

    const [ replyToComment, setReplyToComment ] = useState(false);
    const [ replyCommentId, setReplyCommentId ] = useState(null);
    const [ originalComment, setOriginalComment ] = useState(null);
    const [ commentReason, setCommentReason ] = useState("");
    const [ commentReplyText, setCommentReplyText ] = useState("");
    const [ commentReplyIsResolved, setCommentReplyIsResolved ] = useState(false);
    const [ newCommentText, setNewCommentText ] = useState("");

    const saveComment = (commentText, commentReason) => {
        // todo: save comment
        console.log(`Should save new comment here!\nText: "${commentText}" with reason "${commentReason}"`)
        setNewCommentText("");
        setCommentReason("");
    }

    const replyTo = (comment, key) => {
        setReplyToComment(true);
        setReplyCommentId(key)
        setOriginalComment(comment);
    };

    const exitReplyMode = () => {
        setReplyToComment(false);
        setReplyCommentId(null)
        setOriginalComment(null);
        setCommentReplyText("");
        setCommentReplyIsResolved(false);
    };

    const saveReply = (originalComment, reply, isResolved) => {
        // todo: save the reply
        console.log(`Should save reply to a comment here!\nText: "${reply}" reply to comment of author
         "${originalComment.author.name}", which is ${isResolved? 'now resolved' : 'still unresolved'}.`);
        exitReplyMode();
    }

    const deleteComment = (comment, key) => {
        // todo: delete the comment
        /*deleteComment(comment.id)*/
        console.log(`Delete comment nr ${key} here (author: ${comment.author.name}).`)
    }

    const deleteReply = (comment, reply, replyKey) => {
        // todo: delete the reply
        console.log(`Delete reply nr ${replyKey} (author: ${reply.author.name}) to comment of author ${comment.author.name} here`) /*deleteCommentReply(comment.id, reply.id)*/
    }

    return (
        <div className="modal-content">
            <div className="modal-body">
                <div data-admin-ng-notifications="" context="events-access"></div>
                <div className="full-col">
                    <div className="obj comments">
                        <header className="no-expand">{t(header)}</header>
                        <div className="obj-container">
                            <div className="comment-container">
                                {
                                    comments.map( (comment, key) => (

                                        <div className={`comment ${(replyCommentId === key) ? 'active' : ''}`} key={key}>
                                            <hr/>
                                            <div className="date">{ comment.creationDate | localizeDate }</div> {/*: 'dateTime' : 'short'*/}
                                            <h4>{ comment.author.name }</h4>
                                            <span className="category">
                                                <strong> {t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")} </strong>:
                                                { comment.reason /*| translate*/ }
                                            </span>
                                            <p>{ comment.text }</p>
                                            <a onClick={() => deleteComment(comment, key)}
                                               className="delete">
                                                {t('EVENTS.EVENTS.DETAILS.COMMENTS.DELETE')}
                                            </a>
                                            <a onClick={() => replyTo(comment, key)}
                                               className="reply" with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_REPLY">
                                                {t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY')}
                                            </a>
                                            <span className="resolve" ng-class="{ resolved : comment.resolvedStatus }">
                                                { t('EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED') }
                                            </span>

                                            {
                                                comment.replies.map( (reply, replyKey) => (
                                                    <div className="comment is-reply" key={replyKey}>
                                                        <hr/>
                                                        <div className="date">{ reply.creationDate | localizeDate }</div> {/*: 'dateTime' : 'short'*/}
                                                        <h4>{ reply.author.name }</h4>
                                                        <span className="category">
                                                            <strong> { t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")} </strong>:
                                                            { comment.reason /*| translate */}
                                                        </span>
                                                        <p>
                                                            <span>@{ comment.author.name }</span> { reply.text }
                                                        </p>
                                                        <a onClick={ () => deleteReply(comment, reply, replyKey)}
                                                           className="delete"
                                                           with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE">
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
                                className="add-comment"> {/* with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_CREATE">*/}
                                <textarea
                                    value={newCommentText}
                                    onChange={ (comment) => setNewCommentText(comment.target.value)}
                                    placeholder={t('EVENTS.EVENTS.DETAILS.COMMENTS.PLACEHOLDER')}>{/*ng-model="myComment.text"*/}
                                </textarea>
                                <div className="chosen-container chosen-container-single">
                                    <select className="chosen-single chosen-default"
                                            chosen
                                            value={commentReason}
                                            onChange={(newReason) =>
                                                setCommentReason(newReason.target.value)}
                                            pre-select-from="components.eventCommentReasons"
                                            data-width="'200px'"
                                    >{/*ng-model="myComment.reason"*/}
                                        {/*ng-options="value | translate for (id, value) in components.eventCommentReasons"*/}
                                        <option value="" disabled selected hidden>
                                            {t('EVENTS.EVENTS.DETAILS.COMMENTS.SELECTPLACEHOLDER')}</option>
                                        {replyReasons.map( (reason, key) => (
                                            <option value={reason} key={key}>{reason}</option>
                                        ))}
                                    </select>
                                </div>
                                <button
                                    ng-class="{ disabled: !myComment.text.length || !myComment.reason.length || myComment.saving }"
                                    className="save green"
                                    onClick={() => saveComment(newCommentText, commentReason)}>
                                    {t("SUBMIT") /* Submit */}
                                </button>
                            </form>)
                        }

                        {
                            replyToComment && (<form className="add-comment reply">
                                <textarea
                                    value={commentReplyText}
                                    onChange={ (reply) => setCommentReplyText(reply.target.value)}
                                    placeholder={ t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY_TO') + "@" +  originalComment.author.name }>{/*ng-model="myComment.text"*/}
                                </textarea>
                                <button
                                    className="save green"
                                    onClick={ () =>
                                        saveReply(originalComment, commentReplyText, commentReplyIsResolved)}>
                                    {/*ng-class="{ disabled: !myComment.text.length || myComment.saving }"*/}
                                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY") /* Reply */}
                                </button>
                                <button className="red" onClick={() => exitReplyMode()} >
                                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.CANCEL_REPLY") /* Cancel */}
                                </button>
                                <input with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" type="checkbox"
                                       id="resolved-checkbox" className="ios"
                                       onChange={ () =>
                                           setCommentReplyIsResolved(!commentReplyIsResolved)} />
                                           {/*ng-model="myComment.resolved"*/}
                                <label with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" >
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
});



export default connect(mapStateToProps)(EventDetailsCommentsTab);
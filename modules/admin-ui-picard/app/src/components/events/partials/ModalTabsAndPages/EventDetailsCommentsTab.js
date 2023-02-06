import React, { useState, useEffect } from "react";
import { connect } from "react-redux";
import {
	fetchComments,
	saveComment,
	saveCommentReply,
	deleteComment,
	deleteCommentReply,
} from "../../../../thunks/eventDetailsThunks";
import {
	getComments,
	getCommentReasons,
	isFetchingComments,
	isSavingComment,
	isSavingCommentReply,
} from "../../../../selectors/eventDetailsSelectors";
import Notifications from "../../../shared/Notifications";
import { logger } from "../../../../utils/logger";
import { getUserInformation } from "../../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../../utils/utils";
import DropDown from "../../../shared/DropDown";

/**
 * This component manages the comment tab of the event details modal
 */
const EventDetailsCommentsTab = ({
	eventId,
	header,
	t,
	loadComments,
	saveNewComment,
	saveNewCommentReply,
	deleteOneComment,
	deleteCommentReply,
	comments,
	isSavingComment,
	isSavingCommentReply,
	commentReasons,
	user,
}) => {
	useEffect(() => {
		loadComments(eventId).then((r) => logger.info(r));
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	const [replyToComment, setReplyToComment] = useState(false);
	const [replyCommentId, setReplyCommentId] = useState(null);
	const [originalComment, setOriginalComment] = useState(null);
	const [commentReplyText, setCommentReplyText] = useState("");
	const [commentReplyIsResolved, setCommentReplyIsResolved] = useState(false);

	const [newCommentText, setNewCommentText] = useState("");
	const [commentReason, setCommentReason] = useState("");

	const saveComment = (commentText, commentReason) => {
		saveNewComment(eventId, commentText, commentReason).then((successful) => {
			if (successful) {
				loadComments(eventId);
				setNewCommentText("");
				setCommentReason("");
			}
		});
	};

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
		saveNewCommentReply(eventId, originalComment.id, reply, isResolved).then(
			(success) => {
				if (success) {
					loadComments(eventId);
					exitReplyMode();
				}
			}
		);
	};

	const deleteComment = (comment) => {
		deleteOneComment(eventId, comment.id).then((success) => {
			if (success) {
				loadComments(eventId);
			}
		});
	};

	const deleteReply = (comment, reply) => {
		deleteCommentReply(eventId, comment.id, reply.id).then((success) => {
			if (success) {
				loadComments(eventId);
			}
		});
	};

	// todo: add user and role management
	return (
		<div className="modal-content">
			<div className="modal-body">
				<Notifications context="not-corner" />
				<div className="full-col">
					<div className="obj comments">
						<header>{t(header)}</header>
						<div className="obj-container">
							<div className="comment-container">
								{
									/* all comments listed below each other */
									comments.map((comment, key) => (
										/* one comment */
										<div
											className={`comment ${
												replyCommentId === key ? "active" : ""
											}`}
											key={key}
										>
											<hr />

											{/* details about the comment */}
											<div className="date">
												{t("dateFormats.dateTime.short", {
													dateTime: new Date(comment.creationDate),
												}) || ""}
											</div>
											<h4>{comment.author.name}</h4>
											<span className="category">
												<strong>
													{t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")}
												</strong>
												:{" " + t(comment.reason) || ""}
											</span>

											{/* comment text */}
											<p>{comment.text}</p>

											{/* links with performable actions for the comment */}
											{hasAccess(
												"ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE",
												user
											) && (
												<a
													onClick={() => deleteComment(comment)}
													className="delete"
												>
													{t("EVENTS.EVENTS.DETAILS.COMMENTS.DELETE")}
												</a>
											)}
											{hasAccess(
												"ROLE_UI_EVENTS_DETAILS_COMMENTS_REPLY",
												user
											) && (
												<a
													onClick={
														() => replyTo(comment, key) /* enters reply mode */
													}
													className="reply"
												>
													{t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY")}
												</a>
											)}
											<span
												className="resolve"
												ng-class="{ resolved : comment.resolvedStatus }"
											>
												{t("EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED")}
											</span>

											{
												/* all replies to this comment listed below each other */
												comment.replies.map((reply, replyKey) => (
													<div className="comment is-reply" key={replyKey}>
														<hr />

														{/* details about the reply and reply text */}
														<div className="date">
															{t("dateFormats.dateTime.short", {
																dateTime: new Date(reply.creationDate),
															}) || ""}
														</div>
														<h4>{reply.author.name}</h4>
														<span className="category">
															<strong>
																{t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")}
															</strong>
															:{" " + t(comment.reason) || ""}
														</span>
														<p>
															<span>@{comment.author.name}</span> {reply.text}
														</p>

														{/* link for deleting the reply */}
														{hasAccess(
															"ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE",
															user
														) && (
															<a
																onClick={() =>
																	deleteReply(comment, reply, replyKey)
																}
																className="delete"
															>
																<i className="fa fa-times-circle" />
																{t("EVENTS.EVENTS.DETAILS.COMMENTS.DELETE")}
															</a>
														)}
													</div>
												))
											}
										</div>
									))
								}
							</div>
						</div>

						{
							/* form for writing a comment (not shown, while replying to a comment is active) */
							replyToComment ||
								(hasAccess("ROLE_UI_EVENTS_DETAILS_COMMENTS_CREATE", user) && (
									<form className="add-comment">
										{/* text field */}
										<textarea
											value={newCommentText}
											onChange={(comment) =>
												setNewCommentText(comment.target.value)
											}
											placeholder={t(
												"EVENTS.EVENTS.DETAILS.COMMENTS.PLACEHOLDER"
											)}
										></textarea>

										{/* drop-down for selecting a reason for the comment */}
										<div className="editable">
											<DropDown
												value={commentReason}
												text={t(commentReason)}
												options={Object.entries(commentReasons)}
												type={"comment"}
												required={true}
												handleChange={(element) =>
													setCommentReason(element.value)
												}
												placeholder={t(
													"EVENTS.EVENTS.DETAILS.COMMENTS.SELECTPLACEHOLDER"
												)}
												tabIndex={"5"}
											/>
										</div>

										{/* submit button for comment (only active, if text has been written and a reason has been selected) */}
										<button
											disabled={
												!!(
													!newCommentText.length ||
													newCommentText.length <= 0 ||
													!commentReason.length ||
													commentReason.length <= 0 ||
													isSavingComment
												)
											}
											className={`save green  ${
												!newCommentText.length ||
												newCommentText.length <= 0 ||
												!commentReason.length ||
												commentReason.length <= 0 ||
												isSavingComment
													? "disabled"
													: "false"
											}`}
											onClick={() => saveComment(newCommentText, commentReason)}
										>
											{t("SUBMIT") /* Submit */}
										</button>
									</form>
								))
						}

						{
							/* form for writing a reply to a comment (only shown, while replying to a comment is active) */
							replyToComment && (
								<form className="add-comment reply">
									{/* text field */}
									<textarea
										value={commentReplyText}
										onChange={(reply) =>
											setCommentReplyText(reply.target.value)
										}
										placeholder={
											t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY_TO") +
											"@" +
											originalComment.author.name
										}
									></textarea>

									{/* submit button for comment reply (only active, if text has been written) */}
									<button
										disabled={
											!!(
												!commentReplyText.length ||
												commentReplyText.length <= 0 ||
												isSavingCommentReply
											)
										}
										className={`save green  ${
											!commentReplyText.length ||
											commentReplyText.length <= 0 ||
											isSavingCommentReply
												? "disabled"
												: "false"
										}`}
										onClick={() =>
											saveReply(
												originalComment,
												commentReplyText,
												commentReplyIsResolved
											)
										}
									>
										{t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY") /* Reply */}
									</button>

									{/* cancel button (exits reply mode) */}
									<button className="red" onClick={() => exitReplyMode()}>
										{
											t(
												"EVENTS.EVENTS.DETAILS.COMMENTS.CANCEL_REPLY"
											) /* Cancel */
										}
									</button>

									{/* 'resolved' checkbox */}
									{hasAccess(
										"ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE",
										user
									) && (
										<>
											<input
												type="checkbox"
												id="resolved-checkbox"
												className="ios"
												onChange={() =>
													setCommentReplyIsResolved(!commentReplyIsResolved)
												}
											/>
											<label>
												{
													t(
														"EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED"
													) /* Resolved */
												}
											</label>
										</>
									)}
								</form>
							)
						}
					</div>
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	comments: getComments(state),
	commentReasons: getCommentReasons(state),
	isFetchingComments: isFetchingComments(state),
	isSavingComment: isSavingComment(state),
	isSavingCommentReply: isSavingCommentReply(state),
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadComments: (eventId) => dispatch(fetchComments(eventId)),
	saveNewComment: (eventId, commentText, commentReason) =>
		dispatch(saveComment(eventId, commentText, commentReason)),
	saveNewCommentReply: (eventId, commentId, replyText, commentResolved) =>
		dispatch(saveCommentReply(eventId, commentId, replyText, commentResolved)),
	deleteOneComment: (eventId, commentId) =>
		dispatch(deleteComment(eventId, commentId)),
	deleteCommentReply: (eventId, commentId, replyId) =>
		dispatch(deleteCommentReply(eventId, commentId, replyId)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsCommentsTab);

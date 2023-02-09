import { connect } from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
	getWorkflow,
	getWorkflowErrors,
	isFetchingWorkflowErrors,
} from "../../../../selectors/eventDetailsSelectors";
import { fetchWorkflowErrorDetails } from "../../../../thunks/eventDetailsThunks";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";

/**
 * This component manages the workflow errors for the workflows tab of the event details modal
 */
const EventDetailsWorkflowErrors = ({
	eventId,
	t,
	setHierarchy,
	workflowId,
	errors,
	isFetching,
	fetchErrorDetails,
}) => {
	const severityColor = (severity) => {
		switch (severity.toUpperCase()) {
			case "FAILURE":
				return "red";
			case "INFO":
				return "green";
			case "WARNING":
				return "yellow";
			default:
				return "red";
		}
	};

	const openSubTab = (tabType, errorId = null) => {
		removeNotificationWizardForm();
		setHierarchy(tabType);
		if (tabType === "workflow-error-details") {
			fetchErrorDetails(eventId, workflowId, errorId).then((r) => {});
		}
	};

	return (
		<div className="modal-content">
			{/* Hierarchy navigation */}
			<EventDetailsTabHierarchyNavigation
				openSubTab={openSubTab}
				hierarchyDepth={1}
				translationKey0={"EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE"}
				subTabArgument0={"workflow-details"}
				translationKey1={"EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE"}
				subTabArgument1={"errors-and-warnings"}
			/>

			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* 'Errors & Warnings' table */}
				<div className="full-col">
					<div className="obj tbl-container">
						<header>
							{
								t(
									"EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.HEADER"
								) /* Errors & Warnings */
							}
						</header>

						<div className="obj-container">
							<table className="main-tbl">
								{isFetching || (
									<>
										<thead>
											<tr>
												<th className="small" />
												<th>
													{
														t(
															"EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DATE"
														) /* Date */
													}
													<i />
												</th>
												<th>
													{
														t(
															"EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE"
														) /* Errors & Warnings */
													}
													<i />
												</th>
												<th className="medium" />
											</tr>
										</thead>
										<tbody>
											{
												/* error details */
												errors.entries.map((item, key) => (
													<tr key={key}>
														<td>
															{!!item.severity && (
																<div
																	className={`circle ${severityColor(
																		item.severity
																	)}`}
																/>
															)}
														</td>
														<td>
															{t("dateFormats.dateTime.medium", {
																dateTime: new Date(item.timestamp),
															})}
														</td>
														<td>{item.title}</td>

														{/* link to 'Error Details'  sub-Tab */}
														<td>
															<a
																className="details-link"
																onClick={() =>
																	openSubTab("workflow-error-details", item.id)
																}
															>
																{
																	t(
																		"EVENTS.EVENTS.DETAILS.MEDIA.DETAILS"
																	) /*  Details */
																}
															</a>
														</td>
													</tr>
												))
											}
											{
												/* No errors message */
												errors.entries.length === 0 && (
													<tr>
														<td colSpan="4">
															{
																t(
																	"EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.EMPTY"
																) /* No errors found. */
															}
														</td>
													</tr>
												)
											}
										</tbody>
									</>
								)}
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	workflowId: getWorkflow(state).wiid,
	errors: getWorkflowErrors(state),
	isFetching: isFetchingWorkflowErrors(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	fetchErrorDetails: (eventId, workflowId, operationId) =>
		dispatch(fetchWorkflowErrorDetails(eventId, workflowId, operationId)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsWorkflowErrors);

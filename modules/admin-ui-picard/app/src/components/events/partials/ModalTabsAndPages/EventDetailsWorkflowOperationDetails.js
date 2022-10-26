import { connect } from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
	getWorkflow,
	getWorkflowOperationDetails,
	getWorkflowOperations,
	isFetchingWorkflowOperationDetails,
} from "../../../../selectors/eventDetailsSelectors";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";

/**
 * This component manages the workflow operation details for the workflows tab of the event details modal
 */
const EventDetailsWorkflowOperationDetails = ({
	eventId,
	t,
	setHierarchy,
	operationDetails,
	isFetching,
}) => {
	const openSubTab = (tabType) => {
		removeNotificationWizardForm();
		setHierarchy(tabType);
	};

	return (
		<div className="modal-content">
			{/* Hierarchy navigation */}
			<EventDetailsTabHierarchyNavigation
				openSubTab={openSubTab}
				hierarchyDepth={2}
				translationKey0={"EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE"}
				subTabArgument0={"workflow-details"}
				translationKey1={"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE"}
				subTabArgument1={"workflow-operations"}
				translationKey2={"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TITLE"}
				subTabArgument2={"workflow-operation-details"}
			/>

			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* 'Operation Details' table */}
				<div className="full-col">
					<div className="obj tbl-details">
						<header>
							{
								t(
									"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TITLE"
								) /* Operation Details */
							}
						</header>
						<div className="obj-container">
							<table className="main-tbl">
								{isFetching || (
									<tbody>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.TITLE"
													) /* Title */
												}
											</td>
											<td>{operationDetails.name}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.DESCRIPTION"
													) /* Description */
												}
											</td>
											<td>{operationDetails.description}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.STATE"
													) /* State */
												}
											</td>
											<td>{t(operationDetails.state)}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.EXECUTION_HOST"
													) /* Execution Host */
												}
											</td>
											<td>{operationDetails.execution_host}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.JOB"
													) /* Job */
												}
											</td>
											<td>{operationDetails.job}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.TIME_IN_QUEUE"
													) /* Time in Queue */
												}
											</td>
											<td>{operationDetails.time_in_queue}ms</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.STARTED"
													) /* Started */
												}
											</td>
											<td>
												{t("dateFormats.dateTime.medium", {
													dateTime: new Date(operationDetails.started),
												})}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FINISHED"
													) /* Finished */
												}
											</td>
											<td>
												{t("dateFormats.dateTime.medium", {
													dateTime: new Date(operationDetails.completed),
												})}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.RETRY_STRATEGY"
													) /* Retry Strategy */
												}
											</td>
											<td>{operationDetails.retry_strategy}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FAILED_ATTEMPTS"
													) /* Failed Attempts */
												}
											</td>
											<td>{operationDetails.failed_attempts}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.MAX_ATTEMPTS"
													) /* Max */
												}
											</td>
											<td>{operationDetails.max_attempts}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.EXCEPTION_HANDLER_WORKFLOW"
													) /* Exception Handler Workflow */
												}
											</td>
											<td>{operationDetails.exception_handler_workflow}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FAIL_ON_ERROR"
													) /* Fail on Error */
												}
											</td>
											<td>{operationDetails.fail_on_error}</td>
										</tr>
									</tbody>
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
	operations: getWorkflowOperations(state),
	isFetching: isFetchingWorkflowOperationDetails(state),
	operationDetails: getWorkflowOperationDetails(state),
});

export default connect(mapStateToProps)(EventDetailsWorkflowOperationDetails);

import { connect } from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
	getWorkflow,
	getWorkflowOperations,
	isFetchingWorkflowOperations,
} from "../../../../selectors/eventDetailsSelectors";
import { fetchWorkflowOperationDetails } from "../../../../thunks/eventDetailsThunks";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";

/**
 * This component manages the workflow operations for the workflows tab of the event details modal
 */
const EventDetailsWorkflowOperations = ({
	eventId,
	t,
	setHierarchy,
	workflowId,
	operations,
	isFetching,
	fetchOperationDetails,
}) => {
	const openSubTab = (tabType, operationId = null) => {
		removeNotificationWizardForm();
		setHierarchy(tabType);
		if (tabType === "workflow-operation-details") {
			fetchOperationDetails(eventId, workflowId, operationId).then((r) => {});
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
				translationKey1={"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE"}
				subTabArgument1={"workflow-operations"}
			/>

			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* 'Workflow Operations' table */}
				<div className="full-col">
					<div className="obj tbl-container">
						<header>
							{
								t(
									"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE"
								) /* Workflow Operations */
							}
						</header>
						<div className="obj-container">
							<table className="main-tbl">
								{isFetching || (
									<>
										<thead>
											<tr>
												<th>
													{
														t(
															"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.STATUS"
														) /* Status */
													}
												</th>
												<th>
													{
														t(
															"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.TITLE"
														) /* Title */
													}
													<i />
												</th>
												<th>
													{
														t(
															"EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.DESCRIPTION"
														) /* Description */
													}
													<i />
												</th>
												<th className="medium" />
											</tr>
										</thead>
										<tbody>
											{/* workflow operation details */}
											{operations.entries.map((item, key) => (
												<tr key={key}>
													<td>{t(item.status)}</td>
													<td>{item.title}</td>
													<td>{item.description}</td>

													{/* link to 'Operation Details'  sub-Tab */}
													<td>
														<a
															className="details-link"
															onClick={() =>
																openSubTab("workflow-operation-details", key)
															}
														>
															{
																t(
																	"EVENTS.EVENTS.DETAILS.MEDIA.DETAILS"
																) /* Details */
															}
														</a>
													</td>
												</tr>
											))}
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
	operations: getWorkflowOperations(state),
	isFetching: isFetchingWorkflowOperations(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	fetchOperationDetails: (eventId, workflowId, operationId) =>
		dispatch(fetchWorkflowOperationDetails(eventId, workflowId, operationId)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsWorkflowOperations);

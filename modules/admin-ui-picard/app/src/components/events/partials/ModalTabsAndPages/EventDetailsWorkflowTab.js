import React, { useEffect } from "react";
import { connect } from "react-redux";
import { Formik } from "formik";
import {
	deleteWorkflow,
	fetchWorkflowDetails,
	fetchWorkflows,
	performWorkflowAction,
	saveWorkflowConfig,
	updateWorkflow,
} from "../../../../thunks/eventDetailsThunks";
import {
	deletingWorkflow,
	getBaseWorkflow,
	getWorkflow,
	getWorkflowConfiguration,
	getWorkflowDefinitions,
	getWorkflows,
	isFetchingWorkflows,
	performingWorkflowAction,
} from "../../../../selectors/eventDetailsSelectors";
import Notifications from "../../../shared/Notifications";
import RenderWorkflowConfig from "../wizards/RenderWorkflowConfig";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";
import { getUserInformation } from "../../../../selectors/userInfoSelectors";
import { hasAccess, parseBooleanInObject } from "../../../../utils/utils";
import { setDefaultConfig } from "../../../../utils/workflowPanelUtils";
import DropDown from "../../../shared/DropDown";

/**
 * This component manages the workflows tab of the event details modal
 */
const EventDetailsWorkflowTab = ({
	eventId,
	t,
	close,
	setHierarchy,
	baseWorkflow,
	saveWorkflowConfig,
	removeNotificationWizardForm,
	workflow,
	workflows,
	isLoading,
	workflowDefinitions,
	workflowConfiguration,
	performingWorkflowAction,
	deletingWorkflow,
	loadWorkflows,
	updateWorkflow,
	loadWorkflowDetails,
	performWorkflowAction,
	deleteWf,
	user,
}) => {
	const isRoleWorkflowEdit = hasAccess(
		"ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT",
		user
	);
	const isRoleWorkflowDelete = hasAccess(
		"ROLE_UI_EVENTS_DETAILS_WORKFLOWS_DELETE",
		user
	);

	useEffect(() => {
		removeNotificationWizardForm();
		loadWorkflows(eventId).then((r) => {});
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	const isCurrentWorkflow = (workflowId) => {
		let currentWorkflow = workflows.entries[workflows.entries.length - 1];
		return currentWorkflow.id === workflowId;
	};

	const workflowAction = (workflowId, action) => {
		if (!performingWorkflowAction) {
			performWorkflowAction(eventId, workflowId, action, close);
		}
	};

	const deleteWorkflow = (workflowId) => {
		if (!deletingWorkflow) {
			deleteWf(eventId, workflowId);
		}
	};

	const openSubTab = (tabType, workflowId) => {
		loadWorkflowDetails(eventId, workflowId).then((r) => {});
		setHierarchy(tabType);
		removeNotificationWizardForm();
	};

	const hasCurrentAgentAccess = () => {
		//todo
		return true;
	};

	const changeWorkflow = (value, changeFormikValue) => {
		let currentConfiguration = {};

		if (value === baseWorkflow.workflowId) {
			currentConfiguration = parseBooleanInObject(baseWorkflow.configuration);
		} else {
			currentConfiguration = setDefaultConfig(workflowDefinitions, value);
		}

		changeFormikValue("configuration", currentConfiguration);
		changeFormikValue("workflowDefinition", value);
		updateWorkflow(value);
	};

	const setInitialValues = () => {
		let initialConfig = parseBooleanInObject(baseWorkflow.configuration);

		return {
			workflowDefinition: !!workflow.workflowId
				? workflow.workflowId
				: baseWorkflow.workflowId,
			configuration: initialConfig,
		};
	};

	const handleSubmit = (values) => {
		saveWorkflowConfig(values, eventId);
	};

	return (
		<div className="modal-content" data-modal-tab-content="workflows">
			<div className="modal-body">
				<div className="full-col">
					{/* Notifications */}
					<Notifications context="not_corner" />

					<ul>
						<li>
							{workflows.scheduling || (
								<div className="obj tbl-container">
									<header>
										{
											t(
												"EVENTS.EVENTS.DETAILS.WORKFLOW_INSTANCES.TITLE"
											) /* Workflow instances */
										}
									</header>
									<div className="obj-container">
										<table className="main-tbl">
											<thead>
												<tr>
													<th>
														{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.ID") /* ID */}
													</th>
													<th>
														{
															t(
																"EVENTS.EVENTS.DETAILS.WORKFLOWS.TITLE"
															) /* Title */
														}
													</th>
													<th>
														{
															t(
																"EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTER"
															) /* Submitter */
														}
													</th>
													<th>
														{
															t(
																"EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTED"
															) /* Submitted */
														}
													</th>
													<th>
														{
															t(
																"EVENTS.EVENTS.DETAILS.WORKFLOWS.STATUS"
															) /* Status */
														}
													</th>
													{isRoleWorkflowEdit && (
														<th className="fit">
															{
																t(
																	"EVENTS.EVENTS.DETAILS.WORKFLOWS.ACTIONS"
																) /* Actions */
															}
														</th>
													)}
													<th className="medium" />
												</tr>
											</thead>
											<tbody>
												{isLoading ||
													workflows.entries.map((
														item,
														key /*orderBy:'submitted':true track by $index"*/
													) => (
														<tr key={key}>
															<td>{item.id}</td>
															<td>{item.title}</td>
															<td>{item.submitter}</td>
															<td>
																{t("dateFormats.dateTime.medium", {
																	dateTime: new Date(item.submitted),
																})}
															</td>
															<td>{t(item.status)}</td>
															{isRoleWorkflowEdit && (
																<td>
																	{item.status ===
																		"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.RUNNING" && (
																		<a
																			onClick={() =>
																				workflowAction(item.id, "STOP")
																			}
																			className="stop fa-fw"
																			title={t(
																				"EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.STOP"
																			)}
																		>
																			{/* STOP */}
																		</a>
																	)}
																	{item.status ===
																		"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.PAUSED" && (
																		<a
																			onClick={() =>
																				workflowAction(item.id, "NONE")
																			}
																			className="fa fa-hand-stop-o fa-fw"
																			style={{ color: "red" }}
																			title={t(
																				"EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.ABORT"
																			)}
																		>
																			{/* Abort */}
																		</a>
																	)}
																	{item.status ===
																		"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.PAUSED" && (
																		<a
																			onClick={() =>
																				workflowAction(item.id, "RETRY")
																			}
																			className="fa fa-refresh fa-fw"
																			title={t(
																				"EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.RETRY"
																			)}
																		>
																			{/* Retry */}
																		</a>
																	)}
																	{(item.status ===
																		"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.SUCCEEDED" ||
																		item.status ===
																			"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.FAILED" ||
																		item.status ===
																			"EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.STOPPED") &&
																		!isCurrentWorkflow(item.id) &&
																		isRoleWorkflowDelete && (
																			<a
																				onClick={() => deleteWorkflow(item.id)}
																				className="remove fa-fw"
																				title={t(
																					"EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.DELETE"
																				)}
																			>
																				{/* DELETE */}
																			</a>
																		)}
																</td>
															)}
															<td>
																<a
																	className="details-link"
																	onClick={() =>
																		openSubTab("workflow-details", item.id)
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
										</table>
									</div>
								</div>
							)}

							{workflows.scheduling &&
								(isLoading || (
									<Formik
										initialValues={setInitialValues()}
										enableReinitialize
										onSubmit={(values) => handleSubmit(values)}
									>
										{(formik) => (
											<div className="obj list-obj">
												<header>
													{
														t(
															"EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.CONFIGURATION"
														) /* Workflow configuration */
													}
												</header>
												<div className="obj-container">
													<div className="obj list-obj quick-actions">
														<table className="main-tbl">
															<thead>
																<tr>
																	<th>
																		{
																			t(
																				"EVENTS.EVENTS.DETAILS.WORKFLOWS.WORKFLOW"
																			) /*Select Workflow*/
																		}
																	</th>
																</tr>
															</thead>

															<tbody>
																<tr>
																	<td>
																		<div className="obj-container padded">
																			<div className="editable">
																				<DropDown
																					value={
																						formik.values.workflowDefinition
																					}
																					text={
																						workflowDefinitions.find(
																							(workflowDef) =>
																								workflowDef.id ===
																								formik.values.workflowDefinition
																						).title
																					}
																					options={
																						!!workflowDefinitions &&
																						workflowDefinitions.length > 0
																							? workflowDefinitions /*w.id as w.title for w in workflowDefinitions | orderBy: 'displayOrder':true*/
																							: []
																					}
																					type={"workflow"}
																					required={true}
																					handleChange={(element) =>
																						changeWorkflow(
																							element.value,
																							formik.setFieldValue
																						)
																					}
																					placeholder={
																						!!workflowDefinitions &&
																						workflowDefinitions.length > 0
																							? t(
																									"EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW"
																							  )
																							: t(
																									"EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW_EMPTY"
																							  )
																					}
																					tabIndex={"5"}
																					disabled={
																						!hasCurrentAgentAccess() ||
																						!isRoleWorkflowEdit
																					}
																				/>
																				{/*pre-select-from="workflowDefinitionIds"*/}
																			</div>
																			<div className="obj-container padded">
																				{workflow.description}
																			</div>
																		</div>
																	</td>
																</tr>
															</tbody>
														</table>
													</div>

													<div className="obj list-obj quick-actions">
														<table className="main-tbl">
															<thead>
																<tr>
																	<th>
																		{
																			t(
																				"EVENTS.EVENTS.DETAILS.WORKFLOWS.CONFIGURATION"
																			) /* Configuration */
																		}
																	</th>
																</tr>
															</thead>

															<tbody>
																<tr>
																	<td>
																		<div className="obj-container padded">
																			{hasCurrentAgentAccess() &&
																				isRoleWorkflowEdit &&
																				!!workflowConfiguration &&
																				!!workflowConfiguration.workflowId && (
																					<div
																						id="event-workflow-configuration"
																						className="checkbox-container obj-container"
																					>
																						<RenderWorkflowConfig
																							workflowId={
																								workflowConfiguration.workflowId
																							}
																							formik={formik}
																						/>
																					</div>
																				)}
																			{(!!workflowConfiguration &&
																				!!workflowConfiguration.workflowId) || (
																				<div>
																					{
																						t(
																							"EVENTS.EVENTS.DETAILS.WORKFLOWS.NO_CONFIGURATION"
																						) /* No config */
																					}
																				</div>
																			)}
																		</div>
																	</td>
																</tr>
															</tbody>
														</table>
													</div>
												</div>

												{/* Save and cancel buttons */}
												{hasCurrentAgentAccess() &&
													isRoleWorkflowEdit &&
													!!workflowConfiguration &&
													!!workflowConfiguration.workflowId &&
													formik.dirty && (
														<footer style={{ padding: "15px" }}>
															<div className="pull-left">
																<button
																	type="reset"
																	onClick={() => {
																		changeWorkflow(
																			baseWorkflow.workflowId,
																			formik.setFieldValue
																		);
																		formik.resetForm();
																	}}
																	disabled={!formik.isValid}
																	className={`cancel  ${
																		!formik.isValid ? "disabled" : ""
																	}`}
																>
																	{t("CANCEL") /* Cancel */}
																</button>
															</div>
															<div className="pull-right">
																<button
																	onClick={() => formik.handleSubmit()}
																	disabled={!(formik.dirty && formik.isValid)}
																	className={`save green  ${
																		!(formik.dirty && formik.isValid)
																			? "disabled"
																			: ""
																	}`}
																>
																	{t("SAVE") /* Save */}
																</button>
															</div>
														</footer>
													)}
											</div>
										)}
									</Formik>
								))}
						</li>
					</ul>
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	workflow: getWorkflow(state),
	workflows: getWorkflows(state),
	baseWorkflow: getBaseWorkflow(state),
	isLoading: isFetchingWorkflows(state),
	workflowDefinitions: getWorkflowDefinitions(state),
	workflowConfiguration: getWorkflowConfiguration(state),
	performingWorkflowAction: performingWorkflowAction(state),
	deletingWorkflow: deletingWorkflow(state),
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadWorkflows: (eventId) => dispatch(fetchWorkflows(eventId)),
	updateWorkflow: (workflow) => dispatch(updateWorkflow(workflow)),
	loadWorkflowDetails: (eventId, workflowId) =>
		dispatch(fetchWorkflowDetails(eventId, workflowId)),
	performWorkflowAction: (eventId, workflowId, action, close) =>
		dispatch(performWorkflowAction(eventId, workflowId, action, close)),
	deleteWf: (eventId, workflowId) =>
		dispatch(deleteWorkflow(eventId, workflowId)),
	saveWorkflowConfig: (values, eventId) =>
		dispatch(saveWorkflowConfig(values, eventId)),
	removeNotificationWizardForm: () => dispatch(removeNotificationWizardForm()),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsWorkflowTab);

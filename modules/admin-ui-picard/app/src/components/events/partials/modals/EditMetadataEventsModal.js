import React, { useEffect, useState } from "react";
import { Formik, Field } from "formik";
import { useTranslation } from "react-i18next";
import { getSelectedRows } from "../../../../selectors/tableSelectors";
import { connect } from "react-redux";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {
	getCurrentLanguageInformation,
	hasAccess,
} from "../../../../utils/utils";
import cn from "classnames";
import RenderField from "../../../shared/wizard/RenderField";
import {
	postEditMetadata,
	updateBulkMetadata,
} from "../../../../thunks/eventThunks";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import { logger } from "../../../../utils/logger";
import { getUserInformation } from "../../../../selectors/userInfoSelectors";

// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manges the edit metadata bulk action
 */
const EditMetadataEventsModal = ({
	close,
	selectedRows,
	updateBulkMetadata,
	user,
}) => {
	const { t } = useTranslation();

	const [selectedEvents, setSelectedEvents] = useState(selectedRows);
	const [metadataFields, setMetadataFields] = useState({});
	const [loading, setLoading] = useState(true);
	const [fatalError, setFatalError] = useState({});
	const [fetchedValues, setFetchedValues] = useState(null);

	useEffect(() => {
		async function fetchData() {
			setLoading(true);

			let eventIds = [];
			selectedEvents.forEach((event) => eventIds.push(event.id));

			// Get merged metadata from backend
			const responseMetadataFields = await postEditMetadata(eventIds);

			// Set fatal error if response contains error
			if (!!responseMetadataFields.fatalError) {
				setFatalError(responseMetadataFields);
			} else {
				// Set initial values and save metadata field infos in state
				let initialValues = getInitialValues(responseMetadataFields);
				setFetchedValues(initialValues);
				setMetadataFields(responseMetadataFields);
			}
			setLoading(false);
		}
		fetchData();
	}, []);

	const handleSubmit = (values) => {
		const response = updateBulkMetadata(metadataFields, values);
		logger.info(response);
		close();
	};

	const onChangeSelected = (e, fieldId) => {
		let selected = e.target.checked;
		let fields = metadataFields;
		fields.mergedMetadata = metadataFields.mergedMetadata.map((field) => {
			if (field.id === fieldId) {
				return {
					...field,
					selected: selected,
				};
			} else {
				return field;
			}
		});

		setMetadataFields(fields);
	};

	// Check if value of metadata field is changed
	const isTouchedOrSelected = (field, formikValues) => {
		if (field.selected) {
			return true;
		}

		if (fetchedValues[field.id] !== formikValues[field.id]) {
			let fields = metadataFields;
			fields.mergedMetadata = metadataFields.mergedMetadata.map((f) => {
				if (f.id === field.id) {
					return {
						...f,
						selected: true,
					};
				} else {
					return f;
				}
			});

			setMetadataFields(fields);

			return true;
		}
		return false;
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section className="modal wizard modal-animation">
				<header>
					<a className="fa fa-times close-modal" onClick={() => close()} />
					<h2>{t("BULK_ACTIONS.EDIT_EVENTS_METADATA.CAPTION")}</h2>
				</header>

				{/* Loading spinner */}
				{loading && (
					<div className="modal-content">
						<div className="modal-body">
							<div className="loading">
								<i className="fa fa-spinner fa-spin fa-2x fa-fw" />
							</div>
						</div>
					</div>
				)}

				{/* Fatal error view */}
				{!!fatalError.fatalError && (
					<div className="modal-content">
						<div className="modal-body">
							<div className="row">
								<div className="alert sticky error">
									<p>
										{t("BULK_ACTIONS.EDIT_EVENTS_METADATA.FATAL_ERROR", {
											fatalError: fatalError.fatalError,
										})}
									</p>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* todo: Request Errors View and Update Errors View (not quite sure what this is used for) */}

				{!loading && fatalError.fatalError === undefined && (
					<MuiPickersUtilsProvider
						utils={DateFnsUtils}
						locale={currentLanguage.dateLocale}
					>
						<Formik
							initialValues={fetchedValues}
							onSubmit={(values) => handleSubmit(values)}
						>
							{(formik) => (
								<>
									<div className="modal-content">
										<div className="modal-body">
											<div className="full-col">
												<div className="obj header-description">
													<span>
														{t(
															"BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.DESCRIPTION"
														)}
													</span>
												</div>
												<div className="obj tbl-details">
													<header>
														<span>
															{t(
																"BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.CAPTION"
															)}
														</span>
													</header>
													<div className="obj-container">
														<table className="main-tbl">
															<thead>
																<tr>
																	<th className="small" />
																	<th>
																		{t(
																			"BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.FIELDS"
																		)}
																	</th>
																	<th>
																		{t(
																			"BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.VALUES"
																		)}
																	</th>
																</tr>
															</thead>
															<tbody>
																{metadataFields.mergedMetadata.map(
																	(metadata, key) =>
																		!metadata.readOnly && (
																			<tr
																				key={key}
																				className={cn({
																					info: metadata.differentValues,
																				})}
																			>
																				<td>
																					<input
																						type="checkbox"
																						name="changes"
																						checked={isTouchedOrSelected(
																							metadata,
																							formik.values
																						)}
																						disabled={
																							(!metadata.differentValues &&
																								!metadata.selected) ||
																							(metadata.required &&
																								!metadata.selected)
																						}
																						onChange={(e) =>
																							onChangeSelected(e, metadata.id)
																						}
																						className="child-cbox"
																					/>
																				</td>
																				<td>
																					<span>{t(metadata.label)}</span>
																					{metadata.required && (
																						<i className="required">*</i>
																					)}
																				</td>
																				<td className="editable ng-isolated-scope">
																					{/* Render single value or multi value input */}
																					{metadata.type === "mixed_text" &&
																					!!metadata.collection &&
																					metadata.collection.length !== 0 ? (
																						<Field
																							name={metadata.id}
																							fieldInfo={metadata}
																							showCheck
																							component={RenderMultiField}
																						/>
																					) : (
																						<Field
																							name={metadata.id}
																							metadataField={metadata}
																							showCheck
																							component={RenderField}
																						/>
																					)}
																				</td>
																			</tr>
																		)
																)}
															</tbody>
														</table>
													</div>
												</div>
											</div>
										</div>
									</div>

									{/* Buttons for cancel and submit */}
									<footer>
										<button
											type="submit"
											onClick={() => formik.handleSubmit()}
											disabled={!(formik.dirty && formik.isValid)}
											className={cn("submit", {
												active:
													formik.dirty &&
													formik.isValid &&
													hasAccess(
														"ROLE_UI_EVENTS_DETAILS_METADATA_EDIT",
														user
													),
												inactive: !(
													formik.dirty &&
													formik.isValid &&
													hasAccess(
														"ROLE_UI_EVENTS_DETAILS_METADATA_EDIT",
														user
													)
												),
											})}
										>
											{t("WIZARD.UPDATE")}
										</button>
										<button onClick={() => close()} className="cancel">
											{t("CLOSE")}
										</button>
									</footer>

									<div className="btm-spacer" />
								</>
							)}
						</Formik>
					</MuiPickersUtilsProvider>
				)}
			</section>
		</>
	);
};

const getInitialValues = (metadataFields) => {
	// Transform metadata fields provided by backend (saved in redux)
	let initialValues = {};
	metadataFields.mergedMetadata.forEach((field) => {
		initialValues[field.id] = field.value;
	});

	return initialValues;
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	selectedRows: getSelectedRows(state),
	user: getUserInformation(state),
});

const mapDispatchToProps = (dispatch) => ({
	updateBulkMetadata: (metadataFields, values) =>
		dispatch(updateBulkMetadata(metadataFields, values)),
});
export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EditMetadataEventsModal);

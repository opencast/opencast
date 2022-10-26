import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Formik } from "formik";
import TermsOfUsePage from "./modals/TermsOfUsePage";
import { countries, states } from "../../configs/adopterRegistrationConfig";
import cn from "classnames";
import { AdopterRegistrationSchema } from "../../utils/validate";
import {
	deleteAdopterRegistration,
	fetchAdopterRegistration,
	postRegistration,
} from "../../utils/adopterRegistrationUtils";

/**
 * This component renders the adopter registration modal. This modal has various states.
 */
const RegistrationModal = ({ close }) => {
	const { t } = useTranslation();

	// current state of the modal that is shown
	const [state, setState] = useState("form");
	// initial values for Formik
	const [initialValues, setInitialValues] = useState({});

	const handleClose = () => {
		close();
	};

	useEffect(() => {
		fetchRegistrationInfos().then((r) => console.log(r));
	}, []);

	const onClickContinue = async () => {
		// if state is delete_submit then delete infos about adaptor else show next state
		if (state === "delete_submit") {
			await resetRegistrationData();
		} else {
			setState(states[state].nextState[1]);
		}
	};

	const fetchRegistrationInfos = async () => {
		let registrationInfo = await fetchAdopterRegistration();

		// set response as initial values for formik
		setInitialValues(registrationInfo);
	};

	const handleSubmit = (values) => {
		// post request for adopter information
		postRegistration(values)
			.then(() => {
				// show thank you state
				return setState(states[state].nextState[0]);
			})
			.catch(() => {
				// show error state
				return setState(states[state].nextState[1]);
			});
	};

	const resetRegistrationData = () => {
		// delete adopter information
		deleteAdopterRegistration()
			.then(() => {
				// show thank you state
				return setState(states[state].nextState[0]);
			})
			.catch(() => {
				// show error state
				return setState(states[state].nextState[1]);
			});
	};

	// style of label when input has content
	const styleWithContent = {
		fontSize: "70%",
		fontWeight: "700",
		transform: "translate3d(0, -35%, 0)",
		opacity: 1,
	};

	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section
				id="registration-modal"
				className="modal active modal-open modal-animation"
			>
				<header>
					<a
						onClick={() => handleClose()}
						className="fa fa-times close-modal"
					/>
					<h2>{t("ADOPTER_REGISTRATION.MODAL.CAPTION")}</h2>
				</header>

				{/* shows information about the registration*/}
				{state === "information" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div
								className="registration-header"
								style={{ padding: "5px 0 15px 0" }}
							>
								<h2>
									{t("ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.HEADER")}
								</h2>
							</div>
							<div>
								<div className="row">
									<p>
										{t(
											"ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.INFORMATION_PARAGRAPH_1"
										)}
									</p>
									<br />
									<p>
										{t(
											"ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.INFORMATION_PARAGRAPH_2"
										)}
									</p>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows terms of use */}
				{state === "legal_info" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div>
								<div className="row">
									<div className="scrollbox">
										<TermsOfUsePage />
									</div>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows state after skipping the registration */}
				{state === "skip" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div className="registration-header">
								<h2>{t("ADOPTER_REGISTRATION.MODAL.SKIP_STATE.HEADER")}</h2>
							</div>
							<div>
								<div className="row">
									<p>
										<span>
											{t("ADOPTER_REGISTRATION.MODAL.SKIP_STATE.TEXT")}
										</span>
										<br />
									</p>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows delete confirmation */}
				{state === "delete_submit" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<p>
								<span>
									{t("ADOPTER_REGISTRATION.MODAL.DELETE_SUBMIT_STATE.TEXT")}
								</span>
							</p>
						</div>
					</div>
				)}

				{/* shows spinner while API requests are processed */}
				{(state === "save" || state === "delete" || state === "update") && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div>
								<div className="row spinner-container">
									<i className="fa fa-spinner fa-spin fa-4x fa-fw" />
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows thank you after registration */}
				{state === "thank_you" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div className="registration-header">
								<h2>
									{t("ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.HEADER")}
								</h2>
							</div>
							<div>
								<div>
									<p>
										<span>
											{t(
												"ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.TEXT_LEADING_TO_PATH"
											)}
										</span>
										<b>
											(<span>{t("HELP.HELP")}</span>)
											<span className="fa fa-question-circle" />>
											<span>{t("HELP.ADOPTER_REGISTRATION")}</span>
										</b>
										<span>
											{" "}
											{t(
												"ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.TEXT_LEADING_AFTER_PATH"
											)}
										</span>
									</p>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows error */}
				{state === "error" && (
					<div className="modal-content" style={{ display: "block" }}>
						<div className="modal-body">
							<div className="registration-header">
								<h2>{t("ADOPTER_REGISTRATION.MODAL.ERROR.HEADER")}</h2>
							</div>
							<div>
								<div className="row">
									<p>
										<span>{t("ADOPTER_REGISTRATION.MODAL.ERROR.TEXT")}</span>
									</p>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* shows registration form containing adaptor information */}
				<Formik
					initialValues={initialValues}
					enableReinitialize
					validationSchema={AdopterRegistrationSchema}
					onSubmit={(values) => handleSubmit(values)}
				>
					{(formik) => (
						<>
							{state === "form" && (
								<div className="modal-content" style={{ display: "block" }}>
									<div className="modal-body">
										<div>
											<fieldset>
												<legend>
													{t(
														"ADOPTER_REGISTRATION.MODAL.FORM_STATE.ORGANISATION"
													)}
												</legend>
												<div className="row">
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="organisationName"
																id="adopter_organisation"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_organisation"
																style={
																	formik.values.organisationName
																		? styleWithContent
																		: {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.ORGANISATION"
																)}
															</label>
														</div>
													</div>
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="departmentName"
																id="adopter_department"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_department"
																style={
																	formik.values.departmentName
																		? styleWithContent
																		: {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.DEPARTMENT"
																)}
															</label>
														</div>
													</div>
												</div>
												<div className="row">
													<div className="col">
														<div className="form-group">
															<Field
																style={{ color: "#666", fontWeight: "600" }}
																id="adopter_country"
																name="country"
																as="select"
																className="form-control"
															>
																<option value="" />
																{countries.map((country, key) => (
																	<option key={key} value={country.code}>
																		{country.name}
																	</option>
																))}
															</Field>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_country"
																style={
																	formik.values.country ? styleWithContent : {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.COUNTRY"
																)}
															</label>
														</div>
													</div>
													<div className="col">
														<div className="form-group-pair">
															<div className="form-group">
																<Field
																	type="text"
																	name="postalCode"
																	id="adopter_postalcode"
																	className="form-control"
																/>
																<label
																	className="form-control-placeholder"
																	htmlFor="adopter_postalcode"
																	style={
																		formik.values.postalCode
																			? styleWithContent
																			: {}
																	}
																>
																	{t(
																		"ADOPTER_REGISTRATION.MODAL.FORM_STATE.POSTAL_CODE"
																	)}
																</label>
															</div>
															<div className="form-group">
																<Field
																	type="text"
																	name="city"
																	id="adopter_city"
																	className="form-control"
																/>
																<label
																	className="form-control-placeholder"
																	htmlFor="adopter_city"
																	style={
																		formik.values.city ? styleWithContent : {}
																	}
																>
																	{t(
																		"ADOPTER_REGISTRATION.MODAL.FORM_STATE.CITY"
																	)}
																</label>
															</div>
														</div>
													</div>
												</div>
											</fieldset>
											<fieldset>
												<legend>
													{t(
														"ADOPTER_REGISTRATION.MODAL.FORM_STATE.CONTACT_INFO"
													)}
												</legend>
												<div className="row">
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="firstName"
																id="adopter_firstname"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_firstname"
																style={
																	formik.values.firstName
																		? styleWithContent
																		: {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.FIRST_NAME"
																)}
															</label>
														</div>
													</div>
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="lastName"
																id="adopter_lastname"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_lastname"
																style={
																	formik.values.lastName ? styleWithContent : {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.LAST_NAME"
																)}
															</label>
														</div>
													</div>
												</div>
												<div className="row">
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="street"
																id="adopter_street"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_street"
																style={
																	formik.values.street ? styleWithContent : {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.STREET"
																)}
															</label>
														</div>
													</div>
													<div className="col">
														<div className="form-group">
															<Field
																type="text"
																name="streetNo"
																id="adopter_streetnumber"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_streetnumber"
																style={
																	formik.values.streetNo ? styleWithContent : {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.NUMBER"
																)}
															</label>
														</div>
													</div>
												</div>
												<div className="row">
													<div className="col">
														<div className="form-group">
															<Field
																id="adopter_emailadr"
																name="email"
																type="email"
																className="form-control"
															/>
															<label
																className="form-control-placeholder"
																htmlFor="adopter_emailadr"
																style={
																	formik.values.email ? styleWithContent : {}
																}
															>
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.MAIL"
																)}
															</label>
														</div>
													</div>
													<div className="col">
														<div className="form-group form-group-checkbox">
															<Field
																type="checkbox"
																name="contactme"
																id="adopter_contactme"
																className="form-control"
															/>
															<label htmlFor="adopter_contactme">
																{t(
																	"ADOPTER_REGISTRATION.MODAL.FORM_STATE.CONTACT_ME"
																)}
															</label>
														</div>
													</div>
												</div>
											</fieldset>
											<fieldset>
												<legend>
													{t(
														"ADOPTER_REGISTRATION.MODAL.FORM_STATE.WHICH_DATA_TO_SHARE"
													)}
												</legend>
												<div className="form-group form-group-checkbox">
													<Field
														type="checkbox"
														name="allowsStatistics"
														id="adopter_allows_statistics"
														className="form-control"
													/>
													<label htmlFor="adopter_allows_statistics">
														{t(
															"ADOPTER_REGISTRATION.MODAL.FORM_STATE.USAGE_STATISTICS"
														)}
													</label>
												</div>
												<div className="form-group form-group-checkbox">
													<Field
														type="checkbox"
														name="allowsErrorReports"
														id="adopter_allows_err_reports"
														className="form-control"
													/>
													<label htmlFor="adopter_allows_err_reports">
														{t(
															"ADOPTER_REGISTRATION.MODAL.FORM_STATE.ERROR_REPORTS"
														)}
													</label>
												</div>
											</fieldset>
											<fieldset>
												<legend>
													{t(
														"ADOPTER_REGISTRATION.MODAL.FORM_STATE.POLICY_HEADLINE"
													)}
												</legend>
												<div className="form-group form-group-checkbox">
													<Field
														type="checkbox"
														name="agreedToPolicy"
														id="agreedToPolicy"
														className="form-control"
													/>
													<label htmlFor="agreedToPolicy">
														<span>
															{t(
																"ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_BEFORE"
															)}
														</span>
														<span
															className="link"
															onClick={() =>
																setState(states[state].nextState[2])
															}
														>
															{t(
																"ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_LINK"
															)}
														</span>
														<span>
															{t(
																"ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_AFTER"
															)}
														</span>
													</label>
												</div>
											</fieldset>
										</div>
									</div>
								</div>
							)}

							{/* navigation buttons depending on state of modal */}
							<footer>
								{states[state].buttons.submit && (
									<div className="pull-right">
										{/* submit of form content */}
										{state === "form" ? (
											<button
												disabled={
													!(formik.isValid && formik.values.agreedToPolicy)
												}
												onClick={() => formik.handleSubmit()}
												className={cn("submit", {
													active:
														formik.isValid && formik.values.agreedToPolicy,
													inactive: !(
														formik.isValid && formik.values.agreedToPolicy
													),
												})}
											>
												{t(states[state].buttons.submitButtonText)}
											</button>
										) : (
											// continue button or confirm button (depending on state)
											<button
												className="continue-registration"
												onClick={() => onClickContinue()}
											>
												{t(states[state].buttons.submitButtonText)}
											</button>
										)}
									</div>
								)}

								{/* back, delete or cancel button depending on state */}
								<div className="pull-left">
									{state !== "form" && states[state].buttons.back && (
										<button
											className="cancel"
											onClick={() => setState(states[state].nextState[5])}
										>
											{t("ADOPTER_REGISTRATION.MODAL.BACK")}
										</button>
									)}
									{state === "form" && formik.values.registered && (
										<button
											className="danger"
											onClick={() => setState(states[state].nextState[4])}
										>
											{t("WIZARD.DELETE")}
										</button>
									)}
									{states[state].buttons.skip && (
										<button
											className="cancel"
											onClick={() => setState(states[state].nextState[2])}
										>
											{t("ADOPTER_REGISTRATION.MODAL.SKIP")}
										</button>
									)}
								</div>
							</footer>
						</>
					)}
				</Formik>
			</section>
		</>
	);
};

export default RegistrationModal;

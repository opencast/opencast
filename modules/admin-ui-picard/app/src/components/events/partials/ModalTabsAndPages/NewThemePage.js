import React from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import { Field } from "formik";
import { getSeriesThemes } from "../../../../selectors/seriesSeletctor";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import DropDown from "../../../shared/DropDown";

/**
 * This component renders the theme page for new series in the new series wizard.
 */
const NewThemePage = ({ formik, nextPage, previousPage, seriesThemes }) => {
	const { t } = useTranslation();

	const getDescription = (id) => {
		const theme = seriesThemes.find((theme) => theme.id === id);

		return theme.description;
	};

	return (
		<>
			<div className="modal-content">
				<div className="modal-body">
					<div className="full-col">
						<div className="obj quick-actions">
							<header className="no-expand">
								{t("EVENTS.SERIES.NEW.THEME.TITLE")}
							</header>
							<div className="obj-container padded">
								<ul>
									<li>
										<p>{t("EVENTS.SERIES.NEW.THEME.DESCRIPTION.TEXT")}</p>
										{seriesThemes.length > 0 ? (
											<>
												<p>
													<div className="editable">
														<DropDown
															value={formik.values.theme}
															text={
																!!seriesThemes.find(
																	(theme) => formik.values.theme === theme.id
																)
																	? seriesThemes.find(
																			(theme) =>
																				formik.values.theme === theme.id
																	  ).name
																	: ""
															}
															options={seriesThemes}
															type={"newTheme"}
															required={false}
															handleChange={(element) =>
																formik.setFieldValue("theme", element.value)
															}
															placeholder={t("EVENTS.SERIES.NEW.THEME.LABEL")}
															tabIndex={"1"}
														/>
													</div>
												</p>
												{!!formik.values.theme && (
													<p>{getDescription(formik.values.theme)}</p>
												)}
											</>
										) : (
											<p>{t("EVENTS.SERIES.NEW.THEME.EMPTY")}</p>
										)}
									</li>
								</ul>
							</div>
						</div>
					</div>
				</div>
			</div>

			{/* Button for navigation to next page and previous page */}
			<WizardNavigationButtons
				formik={formik}
				nextPage={nextPage}
				previousPage={previousPage}
			/>
		</>
	);
};

const mapStateToProps = (state) => ({
	seriesThemes: getSeriesThemes(state),
});

export default connect(mapStateToProps)(NewThemePage);

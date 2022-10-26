import React from "react";
import { useTranslation } from "react-i18next";
import { Field } from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import FileUpload from "../../../shared/wizard/FileUpload";

/**
 * This component renders the title slide page for new themes in the new theme wizard and for themes in themes details modal.
 */
const TitleSlidePage = ({ formik, nextPage, previousPage, isEdit }) => {
	const { t } = useTranslation();

	return (
		<>
			<div className="modal-content">
				<div className="modal-body">
					<div className="full-col">
						<p className="tab-description">
							{t("CONFIGURATION.THEMES.DETAILS.TITLE.DESCRIPTION")}
						</p>
					</div>
					{/*todo: Notification*/}
					<div className="obj">
						<header>{t("CONFIGURATION.THEMES.DETAILS.TITLE.ACTIVE")}</header>
						<div className="obj-container content-list padded">
							{/* Checkbox for activation of title slide*/}
							<div className="list-row">
								<div className="header-column">
									<label className="large">
										{t("CONFIGURATION.THEMES.DETAILS.TITLE.ENABLE")}
									</label>
								</div>
								{/* Checkbox for activating title slide */}
								<div className="content-column">
									<div className="content-container">
										<Field
											id="titleSlide-toggle"
											type="checkbox"
											name="titleSlideActive"
										/>
									</div>
								</div>
							</div>
						</div>
					</div>

					{/* Radio buttons for choosing between extraction of title slide or uploading file */}
					{formik.values.titleSlideActive && (
						<div className="obj">
							<header>
								{t("CONFIGURATION.THEMES.DETAILS.TITLE.BACKGROUND")}
							</header>
							<div className="obj-container padded">
								<div className="file-upload">
									<div className="form-container">
										{/* Radio button for choosing title slide mode*/}
										<div className="row">
											<Field
												type="radio"
												value="extract"
												name="titleSlideMode"
												id="background-extract"
											/>
											<label>
												{t("CONFIGURATION.THEMES.DETAILS.TITLE.EXTRACT")}
											</label>
										</div>
										<div className="row">
											<Field
												type="radio"
												value="upload"
												name="titleSlideMode"
												id="background-upload"
											/>
											<label>
												{t("CONFIGURATION.THEMES.DETAILS.TITLE.UPLOAD")}
											</label>
										</div>
									</div>
								</div>
								{/*If title slide mode upload is chosen, use component for file upload */}
								{formik.values.titleSlideMode === "upload" && (
									<FileUpload
										acceptableTypes="image/*"
										fileId="titleSlideBackground"
										fileName="titleSlideBackgroundName"
										formik={formik}
										labelKey="CONFIGURATION.THEMES.DETAILS.TITLE.UPLOAD_LABEL"
										buttonKey="CONFIGURATION.THEMES.DETAILS.TITLE.UPLOAD_BUTTON"
										isEdit={isEdit}
									/>
								)}
							</div>
						</div>
					)}
				</div>
			</div>
			{/* Show navigation buttons only if page is used for a new theme*/}
			{!isEdit && (
				//Button for navigation to next page
				<WizardNavigationButtons
					formik={formik}
					previousPage={previousPage}
					nextPage={nextPage}
				/>
			)}
		</>
	);
};

export default TitleSlidePage;

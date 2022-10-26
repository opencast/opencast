import React from "react";
import { useTranslation } from "react-i18next";
import Notifications from "../../../shared/Notifications";
import { Field } from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

/**
 * This component renders the general page for new themes in the new themes wizard
 * and for themes in the themes details modal.
 * Here, additional information, like name, for themes can be provided.
 */
const GeneralPage = ({ formik, nextPage, isEdit }) => {
	const { t } = useTranslation();

	// Style used in themes details modal
	const editStyle = {
		color: "#666666",
		fontSize: "14px",
	};

	return (
		<>
			{/* Fields for name and description */}
			<div className="modal-content">
				<div className="modal-body">
					<div className="full-col">
						<div className="form-container">
							<div className="row">
								<Notifications />
								<label className="required" style={isEdit && editStyle}>
									{t("CONFIGURATION.THEMES.DETAILS.GENERAL.NAME")}
								</label>
								<Field
									name="name"
									type="text"
									autoFocus={!isEdit}
									placeholder={
										t("CONFIGURATION.THEMES.DETAILS.GENERAL.NAME") + "..."
									}
								/>
							</div>
							<div className="row">
								<label style={isEdit && editStyle}>
									{t("CONFIGURATION.THEMES.DETAILS.GENERAL.DESCRIPTION")}
								</label>
								<Field
									name="description"
									as="textarea"
									placeholder={
										t("CONFIGURATION.THEMES.DETAILS.GENERAL.DESCRIPTION") +
										"..."
									}
								/>
							</div>
						</div>
					</div>
				</div>
			</div>

			{/* Show navigation buttons only if page is used for a new theme*/}
			{!isEdit && (
				//Button for navigation to next page
				<WizardNavigationButtons isFirst formik={formik} nextPage={nextPage} />
			)}
		</>
	);
};

export default GeneralPage;

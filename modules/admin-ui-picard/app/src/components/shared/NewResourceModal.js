import React from "react";
import { useTranslation } from "react-i18next";
import NewEventWizard from "../events/partials/wizards/NewEventWizard";
import NewSeriesWizard from "../events/partials/wizards/NewSeriesWizard";
import NewThemeWizard from "../configuration/partials/wizard/NewThemeWizard";
import NewAclWizard from "../users/partials/wizard/NewAclWizard";
import NewGroupWizard from "../users/partials/wizard/NewGroupWizard";
import NewUserWizard from "../users/partials/wizard/NewUserWizard";

/**
 * This component renders the modal for adding new resources
 */
const NewResourceModal = ({ handleClose, showModal, resource }) => {
	const { t } = useTranslation();

	const close = () => {
		handleClose();
	};

	return (
		// todo: add hotkeys
		showModal && (
			<>
				<div className="modal-animation modal-overlay" />
				<section
					tabIndex="1"
					className="modal wizard modal-animation"
					id="add-event-modal"
				>
					<header>
						<a className="fa fa-times close-modal" onClick={() => close()} />
						{resource === "events" && <h2>{t("EVENTS.EVENTS.NEW.CAPTION")}</h2>}
						{resource === "series" && <h2>{t("EVENTS.SERIES.NEW.CAPTION")}</h2>}
						{resource === "themes" && (
							<h2>{t("CONFIGURATION.THEMES.DETAILS.NEWCAPTION")}</h2>
						)}
						{resource === "acl" && <h2>{t("USERS.ACLS.NEW.CAPTION")}</h2>}
						{resource === "group" && <h2>{t("USERS.GROUPS.NEW.CAPTION")}</h2>}
						{resource === "user" && (
							<h2>{t("USERS.USERS.DETAILS.NEWCAPTION")}</h2>
						)}
					</header>
					{resource === "events" && (
						//New Event Wizard
						<NewEventWizard close={close} />
					)}
					{resource === "series" && (
						// New Series Wizard
						<NewSeriesWizard close={close} />
					)}
					{resource === "themes" && (
						// New Theme Wizard
						<NewThemeWizard close={close} />
					)}
					{resource === "acl" && (
						// New ACL Wizard
						<NewAclWizard close={close} />
					)}
					{resource === "group" && (
						// New Group Wizard
						<NewGroupWizard close={close} />
					)}
					{resource === "user" && (
						// New User Wizard
						<NewUserWizard close={close} />
					)}
				</section>
			</>
		)
	);
};

export default NewResourceModal;

import React from "react";
import { useTranslation } from "react-i18next";
import EventDetails from "./EventDetails";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";

/**
 * This component renders the modal for displaying event details
 */
const EventDetailsModal = ({
	handleClose,
	showModal,
	tabIndex,
	eventTitle,
	eventId,
}) => {
	const { t } = useTranslation();

	const close = () => {
		removeNotificationWizardForm();
		handleClose();
	};

	return (
		// todo: add hotkeys
		showModal && (
			<>
				<div className="modal-animation modal-overlay" />
				<section
					id="event-details-modal"
					tabIndex={tabIndex}
					className="modal wizard modal-animation"
				>
					<header>
						<a className="fa fa-times close-modal" onClick={() => close()} />
						<h2>
							{
								t("EVENTS.EVENTS.DETAILS.HEADER", {
									resourceId: eventTitle,
								}) /*Event details - {resourceTitle}*/
							}
						</h2>
					</header>

					<EventDetails tabIndex={tabIndex} eventId={eventId} />
				</section>
			</>
		)
	);
};

export default EventDetailsModal;

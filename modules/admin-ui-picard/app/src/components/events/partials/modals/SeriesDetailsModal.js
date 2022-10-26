import React from "react";
import { useTranslation } from "react-i18next";
import SeriesDetails from "./SeriesDetails";

/**
 * This component renders the modal for displaying series details
 */
const SeriesDetailsModal = ({ handleClose, seriesTitle, seriesId }) => {
	const { t } = useTranslation();

	const close = () => {
		handleClose();
	};

	// todo: add hotkeys
	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section className="modal modal-animation" id="series-details-modal">
				<header>
					<a className="fa fa-times close-modal" onClick={() => close()} />
					<h2>
						{t("EVENTS.SERIES.DETAILS.HEADER", { resourceId: seriesTitle })}
					</h2>
				</header>

				<SeriesDetails seriesId={seriesId} />
			</section>
		</>
	);
};

export default SeriesDetailsModal;

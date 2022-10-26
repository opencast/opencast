import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import { deleteEvent } from "../../../thunks/eventThunks";
import { connect } from "react-redux";
import EventDetailsModal from "./modals/EventDetailsModal";
import EmbeddingCodeModal from "./modals/EmbeddingCodeModal";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";
import SeriesDetailsModal from "./modals/SeriesDetailsModal";
import {
	fetchNamesOfPossibleThemes,
	fetchSeriesDetailsAcls,
	fetchSeriesDetailsFeeds,
	fetchSeriesDetailsMetadata,
	fetchSeriesDetailsTheme,
} from "../../../thunks/seriesDetailsThunks";

/**
 * This component renders the action cells of events in the table view
 */
const EventActionCell = ({
	row,
	deleteEvent,
	fetchSeriesDetailsMetadata,
	fetchSeriesDetailsAcls,
	fetchSeriesDetailsFeeds,
	fetchSeriesDetailsTheme,
	fetchSeriesDetailsThemeNames,
	user,
}) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displayEventDetailsModal, setEventDetailsModal] = useState(false);
	const [displaySeriesDetailsModal, setSeriesDetailsModal] = useState(false);
	const [eventDetailsTabIndex, setEventDetailsTabIndex] = useState(0);
	const [displayEmbeddingCodeModal, setEmbeddingCodeModal] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const deletingEvent = (id) => {
		deleteEvent(id);
	};

	const hideEmbeddingCodeModal = () => {
		setEmbeddingCodeModal(false);
	};

	const showEmbeddingCodeModal = () => {
		setEmbeddingCodeModal(true);
	};

	const showEventDetailsModal = () => {
		setEventDetailsModal(true);
	};

	const hideEventDetailsModal = () => {
		setEventDetailsModal(false);
	};

	const showSeriesDetailsModal = () => {
		setSeriesDetailsModal(true);
	};

	const hideSeriesDetailsModal = () => {
		setSeriesDetailsModal(false);
	};

	const onClickSeriesDetails = async () => {
		await fetchSeriesDetailsMetadata(row.series.id);
		await fetchSeriesDetailsAcls(row.series.id);
		await fetchSeriesDetailsFeeds(row.series.id);
		await fetchSeriesDetailsTheme(row.series.id);
		await fetchSeriesDetailsThemeNames();

		showSeriesDetailsModal();
	};

	const onClickEventDetails = () => {
		setEventDetailsTabIndex(0);
		showEventDetailsModal();
	};

	const onClickComments = () => {
		setEventDetailsTabIndex(7);
		showEventDetailsModal();
	};

	const onClickWorkflow = () => {
		setEventDetailsTabIndex(5);
		showEventDetailsModal();
	};

	const onClickAssets = () => {
		setEventDetailsTabIndex(3);
		showEventDetailsModal();
	};

	return (
		<>
			{/* Display modal for editing table view if table edit button is clicked */}
			<EventDetailsModal
				showModal={displayEventDetailsModal}
				handleClose={hideEventDetailsModal}
				tabIndex={eventDetailsTabIndex}
				eventTitle={row.title}
				eventId={row.id}
			/>

			{displaySeriesDetailsModal && (
				<SeriesDetailsModal
					handleClose={hideSeriesDetailsModal}
					seriesId={row.series.id}
					seriesTitle={row.series.title}
				/>
			)}

			{/* Open event details */}
			{hasAccess("ROLE_UI_EVENTS_DETAILS_VIEW", user) && (
				<a
					onClick={() => onClickEventDetails()}
					className="more"
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{/* If event belongs to a series then the corresponding series details can be opened */}
			{!!row.series && hasAccess("ROLE_UI_SERIES_DETAILS_VIEW", user) && (
				<a
					onClick={() => onClickSeriesDetails()}
					className="more-series"
					title={t("EVENTS.SERIES.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{/* Delete an event */}
			{/*TODO: needs to be checked if event is published */}
			{hasAccess("ROLE_UI_EVENTS_DELETE", user) && (
				<a
					onClick={() => setDeleteConfirmation(true)}
					className="remove"
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.DELETE")}
				/>
			)}

			{/* Confirmation for deleting an event*/}
			{displayDeleteConfirmation && (
				<ConfirmModal
					close={hideDeleteConfirmation}
					resourceName={row.title}
					resourceType="EVENT"
					resourceId={row.id}
					deleteMethod={deletingEvent}
				/>
			)}

			{/* If the event has an preview then the editor can be opened and status if it needs to be cut is shown */}
			{!!row.has_preview && hasAccess("ROLE_UI_EVENTS_EDITOR_VIEW", user) && (
				<a
					href={`/editor-ui/index.html?id=${row.id}`}
					className="cut"
					title={
						row.needs_cutting
							? t("EVENTS.EVENTS.TABLE.TOOLTIP.EDITOR_NEEDS_CUTTING")
							: t("EVENTS.EVENTS.TABLE.TOOLTIP.EDITOR")
					}
				>
					{row.needs_cutting && <span id="badge" className="badge" />}
				</a>
			)}

			{/* If the event has comments and no open comments then the comment tab of event details can be opened directly */}
			{row.has_comments && !row.has_open_comments && (
				<a
					onClick={() => onClickComments()}
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS")}
					className="comments"
				/>
			)}

			{/* If the event has comments and open comments then the comment tab of event details can be opened directly */}
			{row.has_comments && row.has_open_comments && (
				<a
					onClick={() => onClickComments()}
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS")}
					className="comments-open"
				/>
			)}

			{/*If the event is in in a paused workflow state then a warning icon is shown and workflow tab of event
                details can be opened directly */}
			{row.workflow_state === "PAUSED" &&
				hasAccess("ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT", user) && (
					<a
						title={t("EVENTS.EVENTS.TABLE.TOOLTIP.PAUSED_WORKFLOW")}
						onClick={() => onClickWorkflow()}
						className="fa fa-warning"
					/>
				)}

			{/* Open assets tab of event details directly*/}
			{hasAccess("ROLE_UI_EVENTS_DETAILS_ASSETS_VIEW", user) && (
				<a
					onClick={() => onClickAssets()}
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.ASSETS")}
					className="fa fa-folder-open"
				/>
			)}
			{/* Open dialog for embedded code*/}
			{hasAccess("ROLE_UI_EVENTS_EMBEDDING_CODE_VIEW", user) && (
				<a
					onClick={() => showEmbeddingCodeModal()}
					title={t("EVENTS.EVENTS.TABLE.TOOLTIP.EMBEDDING_CODE")}
					className="fa fa-link"
				/>
			)}

			{displayEmbeddingCodeModal && (
				<EmbeddingCodeModal close={hideEmbeddingCodeModal} eventId={row.id} />
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	deleteEvent: (id) => dispatch(deleteEvent(id)),
	fetchSeriesDetailsMetadata: (id) => dispatch(fetchSeriesDetailsMetadata(id)),
	fetchSeriesDetailsAcls: (id) => dispatch(fetchSeriesDetailsAcls(id)),
	fetchSeriesDetailsFeeds: (id) => dispatch(fetchSeriesDetailsFeeds(id)),
	fetchSeriesDetailsTheme: (id) => dispatch(fetchSeriesDetailsTheme(id)),
	fetchSeriesDetailsThemeNames: () => dispatch(fetchNamesOfPossibleThemes()),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventActionCell);

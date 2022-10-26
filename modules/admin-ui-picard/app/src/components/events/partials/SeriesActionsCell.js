import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {
	checkForEventsDeleteSeriesModal,
	deleteSeries,
} from "../../../thunks/seriesThunks";
import { connect } from "react-redux";
import SeriesDetailsModal from "./modals/SeriesDetailsModal";
import {
	fetchNamesOfPossibleThemes,
	fetchSeriesDetailsAcls,
	fetchSeriesDetailsFeeds,
	fetchSeriesDetailsMetadata,
	fetchSeriesDetailsTheme,
} from "../../../thunks/seriesDetailsThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";
import {
	getSeriesHasEvents,
	isSeriesDeleteAllowed,
} from "../../../selectors/seriesSeletctor";

/**
 * This component renders the action cells of series in the table view
 */
const SeriesActionsCell = ({
	row,
	deleteSeries,
	fetchSeriesDetailsMetadata,
	fetchSeriesDetailsAcls,
	checkDeleteAllowed,
	fetchSeriesDetailsFeeds,
	fetchSeriesDetailsTheme,
	fetchSeriesDetailsThemeNames,
	user,
	deleteAllowed,
	hasEvents,
}) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displaySeriesDetailsModal, setSeriesDetailsModal] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const showDeleteConfirmation = async () => {
		await checkDeleteAllowed(row.id);

		setDeleteConfirmation(true);
	};

	const deletingSeries = (id) => {
		deleteSeries(id);
	};

	const hideSeriesDetailsModal = () => {
		setSeriesDetailsModal(false);
	};

	const showSeriesDetailsModal = async () => {
		await fetchSeriesDetailsMetadata(row.id);
		await fetchSeriesDetailsAcls(row.id);
		await fetchSeriesDetailsFeeds(row.id);
		await fetchSeriesDetailsTheme(row.id);
		await fetchSeriesDetailsThemeNames();

		setSeriesDetailsModal(true);
	};

	return (
		<>
			{/* series details */}
			{hasAccess("ROLE_UI_SERIES_DETAILS_VIEW", user) && (
				<a
					onClick={() => showSeriesDetailsModal()}
					className="more-series"
					title={t("EVENTS.SERIES.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{displaySeriesDetailsModal && (
				<SeriesDetailsModal
					handleClose={hideSeriesDetailsModal}
					seriesId={row.id}
					seriesTitle={row.title}
				/>
			)}

			{/* delete series */}
			{hasAccess("ROLE_UI_SERIES_DELETE", user) && (
				<a
					onClick={() => showDeleteConfirmation()}
					className="remove"
					title={t("EVENTS.SERIES.TABLE.TOOLTIP.DELETE")}
				/>
			)}

			{displayDeleteConfirmation && (
				<ConfirmModal
					close={hideDeleteConfirmation}
					resourceName={row.title}
					resourceType="SERIES"
					resourceId={row.id}
					deleteMethod={deletingSeries}
					deleteAllowed={deleteAllowed}
					showCautionMessage={hasEvents}
					deleteNotAllowedMessage={
						"CONFIRMATIONS.ERRORS.SERIES_HAS_EVENTS"
					} /* The highlighted series cannot be deleted as they still contain events */
					deleteWithCautionMessage={
						"CONFIRMATIONS.WARNINGS.SERIES_HAS_EVENTS"
					} /* This series does contain events. Deleting the series will not delete the events. */
				/>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
	deleteAllowed: isSeriesDeleteAllowed(state),
	hasEvents: getSeriesHasEvents(state),
});

const mapDispatchToProps = (dispatch) => ({
	deleteSeries: (id) => dispatch(deleteSeries(id)),
	fetchSeriesDetailsMetadata: (id) => dispatch(fetchSeriesDetailsMetadata(id)),
	fetchSeriesDetailsAcls: (id) => dispatch(fetchSeriesDetailsAcls(id)),
	fetchSeriesDetailsFeeds: (id) => dispatch(fetchSeriesDetailsFeeds(id)),
	fetchSeriesDetailsTheme: (id) => dispatch(fetchSeriesDetailsTheme(id)),
	fetchSeriesDetailsThemeNames: () => dispatch(fetchNamesOfPossibleThemes()),
	checkDeleteAllowed: (id) => dispatch(checkForEventsDeleteSeriesModal(id)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SeriesActionsCell);

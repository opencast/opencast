import React from "react";
import { useTranslation } from "react-i18next";
import { getFilters } from "../../../selectors/tableFilterSelectors";
import { editFilterValue } from "../../../actions/tableFilterActions";
import { connect } from "react-redux";
import { fetchEvents } from "../../../thunks/eventThunks";
import { loadEventsIntoTable } from "../../../thunks/tableThunks";

/**
 * This component renders the series cells of events in the table view
 */
const EventsSeriesCell = ({
	row,
	filterMap,
	editFilterValue,
	loadEvents,
	loadEventsIntoTable,
}) => {
	const { t } = useTranslation();

	// Filter with value of current cell
	const addFilter = async (series) => {
		let filter = filterMap.find(({ name }) => name === "series");
		if (!!filter) {
			await editFilterValue(filter.name, series.id);
			await loadEvents();
			loadEventsIntoTable();
		}
	};

	return (
		!!row.series && (
			// Link template for series of event
			<a
				className="crosslink"
				title={t("EVENTS.EVENTS.TABLE.TOOLTIP.SERIES")}
				onClick={() => addFilter(row.series)}
			>
				{row.series.title}
			</a>
		)
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	filterMap: getFilters(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	editFilterValue: (filterName, value) =>
		dispatch(editFilterValue(filterName, value)),
	loadEvents: () => dispatch(fetchEvents()),
	loadEventsIntoTable: () => dispatch(loadEventsIntoTable()),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventsSeriesCell);

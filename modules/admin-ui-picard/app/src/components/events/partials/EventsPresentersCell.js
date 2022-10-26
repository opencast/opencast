import React from "react";
import { useTranslation } from "react-i18next";
import { getFilters } from "../../../selectors/tableFilterSelectors";
import { editFilterValue } from "../../../actions/tableFilterActions";
import { connect } from "react-redux";
import { fetchEvents } from "../../../thunks/eventThunks";
import { loadEventsIntoTable } from "../../../thunks/tableThunks";

/**
 * This component renders the presenters cells of events in the table view
 */
const EventsPresentersCell = ({
	row,
	filterMap,
	editFilterValue,
	loadEvents,
	loadEventsIntoTable,
}) => {
	const { t } = useTranslation();

	// Filter with value of current cell
	const addFilter = async (presenter) => {
		let filter = filterMap.find(
			({ name }) => name === "presentersBibliographic"
		);
		if (!!filter) {
			await editFilterValue(filter.name, presenter);
			await loadEvents();
			loadEventsIntoTable();
		}
	};

	return (
		// Link template for presenter of event
		// Repeat for each presenter
		row.presenters.map((presenter, key) => (
			<a
				className="metadata-entry"
				key={key}
				title={t("EVENTS.EVENTS.TABLE.TOOLTIP.PRESENTER")}
				onClick={() => addFilter(presenter)}
			>
				{presenter}
			</a>
		))
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

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventsPresentersCell);

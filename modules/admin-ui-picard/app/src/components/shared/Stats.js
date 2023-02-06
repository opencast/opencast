import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { getFilters, getStats } from "../../selectors/tableFilterSelectors";
import {
	editFilterValue,
	resetFilterValues,
} from "../../actions/tableFilterActions";
import { connect } from "react-redux";
import { fetchEvents } from "../../thunks/eventThunks";
import { loadEventsIntoTable } from "../../thunks/tableThunks";
import { fetchStats } from "../../thunks/tableFilterThunks";
import { logger } from "../../utils/logger";

/**
 * This component renders the status bar of the event view and filters depending on these
 */
const Stats = ({
	loadingStats,
	stats,
	filterMap,
	editFilterValue,
	loadEvents,
	loadEventsIntoTable,
	resetFilterMap,
}) => {
	const { t } = useTranslation();

	// Filter with value of clicked status
	const showStatsFilter = async (stats) => {
		resetFilterMap();
		let filterValue;
		await stats.filters.forEach((f) => {
			let filter = filterMap.find(({ name }) => name === f.name);
			filterValue = f.value;
			if (!!filter) {
				editFilterValue(filter.name, filterValue);
			}
		});
		await loadEvents();
		loadEventsIntoTable();
	};

	const loadStats = async () => {
		// Fetching stats from server
		await loadingStats();
	};

	useEffect(() => {
		// Load stats on mount
		loadStats().then((r) => logger.info(r));
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	return (
		<>
			<div className="main-stats">
				{/* Show one counter for each status */}
				{stats.map((st, key) => (
					<div className="col" key={key}>
						<div
							className="stat"
							onClick={() => showStatsFilter(st)}
							title={t(st.description)}
						>
							<h1>{st.count}</h1>
							{/* Show the description of the status, if defined,
                            else show name of filter and its value*/}
							{!!st.description ? (
								<span>{t(st.description)}</span>
							) : (
								st.filters.map((filter, key) => (
									<span key={key}>
										{t(filter.filter)}: {t(filter.value)}
									</span>
								))
							)}
						</div>
					</div>
				))}
			</div>
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	filterMap: getFilters(state),
	stats: getStats(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadingStats: () => dispatch(fetchStats()),
	editFilterValue: (filterName, value) =>
		dispatch(editFilterValue(filterName, value)),
	loadEvents: () => dispatch(fetchEvents()),
	loadEventsIntoTable: () => dispatch(loadEventsIntoTable()),
	resetFilterMap: () => dispatch(resetFilterValues()),
});

export default connect(mapStateToProps, mapDispatchToProps)(Stats);

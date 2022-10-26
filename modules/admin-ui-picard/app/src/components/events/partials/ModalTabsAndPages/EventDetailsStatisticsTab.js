import React from "react";
import {
	getStatistics,
	hasStatisticsError,
} from "../../../../selectors/eventDetailsSelectors";
import { connect } from "react-redux";
import TimeSeriesStatistics from "../../../shared/TimeSeriesStatistics";
import { fetchEventStatisticsValueUpdate } from "../../../../thunks/eventDetailsThunks";

/**
 * This component manages the statistics tab of the event details modal
 */
const EventDetailsStatisticsTab = ({
	eventId,
	header,
	t,
	statistics,
	hasError,
	recalculateStatistics,
}) => {
	/* generates file name for download-link for a statistic */
	const statisticsCsvFileName = (statsTitle) => {
		const sanitizedStatsTitle = statsTitle
			.replace(/[^0-9a-z]/gi, "_")
			.toLowerCase();
		return "export_event_" + eventId + "_" + sanitizedStatsTitle + ".csv";
	};

	return (
		<div className="modal-content">
			<div className="modal-body">
				<div className="full-col">
					{hasError ? (
						/* error message */
						<div className="obj">
							<header>{t(header) /* Statistics */}</header>
							<div className="modal-alert danger">
								{t("STATISTICS.NOT_AVAILABLE")}
							</div>
						</div>
					) : (
						/* iterates over the different available statistics */
						statistics.map((stat, key) => (
							<div className="obj" key={key}>
								{/* title of statistic */}
								<header className="no-expand">{t(stat.title)}</header>

								{stat.providerType === "timeSeries" ? (
									/* visualization of statistic for time series data */
									<div className="obj-container">
										<TimeSeriesStatistics
											t={t}
											resourceId={eventId}
											statTitle={t(stat.title)}
											providerId={stat.providerId}
											fromDate={stat.from}
											toDate={stat.to}
											timeMode={stat.timeMode}
											dataResolution={stat.dataResolution}
											statDescription={stat.description}
											onChange={recalculateStatistics}
											exportUrl={stat.csvUrl}
											exportFileName={statisticsCsvFileName}
											totalValue={stat.totalValue}
											sourceData={stat.values}
											chartLabels={stat.labels}
											chartOptions={stat.options}
										/>
									</div>
								) : (
									/* unsupported type message */
									<div className="modal-alert danger">
										{t("STATISTICS.UNSUPPORTED_TYPE")}
									</div>
								)}
							</div>
						))
					)}
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	statistics: getStatistics(state),
	hasError: hasStatisticsError(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	recalculateStatistics: (
		eventId,
		providerId,
		from,
		to,
		dataResolution,
		timeMode
	) =>
		dispatch(
			fetchEventStatisticsValueUpdate(
				eventId,
				providerId,
				from,
				to,
				dataResolution,
				timeMode
			)
		),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsStatisticsTab);

import React from "react";
import {getStatistics, hasStatisticsError} from "../../../../selectors/eventDetailsSelectors";
import {connect} from "react-redux";
import StatisticsGraph from "../../../shared/StatisticsGraph";

const EventDetailsStatisticsTab = ({ eventId, header, t,
                                       statistics, hasError,
                                       recalculateStatistics}) => {

    const statisticsCsvFileName = (statsTitle) => {
      const sanitizedStatsTitle = statsTitle.replace(/[^0-9a-z]/gi, '_').toLowerCase();
      return 'export_event_' + eventId + '_' + sanitizedStatsTitle + '.csv';
    };

    return (
        <div className="modal-content">
            <div className="modal-body">
                <div className="full-col">
                    <div className="obj">
                        <header>{t(header)}</header>
                        <div className="obj-container">
                            {hasError? (
                                <div className="modal-alert danger">
                                    {t("STATISTICS.NOT_AVAILABLE")}
                                </div>
                            ):(
                                statistics.map((stat, key) => (
                                    <div className="obj">
                                        <header className="no-expand">
                                            {t(stat.title) /* Statistics */}
                                        </header>
                                        {(stat.providerType === 'timeSeries')? (
                                            <div className="obj-container">
                                                <StatisticsGraph
                                                    chartLabels={stat.labels}
                                                    chartOptions={stat.options}
                                                    initialDataResolution={stat.dataResolution}
                                                    providerId={stat.providerId}
                                                    fromDate={stat.from}
                                                    onChange={recalculateStatistics}
                                                    exportUrl={stat.csvUrl}
                                                    exportFileName={statisticsCsvFileName(t(stat.title))}
                                                    sourceData={stat.values}
                                                    timeChooseMode={stat.timeChooseMode}
                                                    toDate={stat.to}
                                                    totalValue={stat.totalValue}
                                                    statDescription={stat.description}
                                                />
                                            </div>
                                        ):(
                                            <div className="modal-alert danger">
                                                {t('STATISTICS.UNSUPPORTED_TYPE')}
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

const mapStateToProps = state => ({
    statistics: getStatistics(state),
    hasError: hasStatisticsError(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    recalculateStatistics: (provider, from, to, dataResolution, timeChooseMode) => dispatch(/*statReusable.recalculate*/),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsStatisticsTab);
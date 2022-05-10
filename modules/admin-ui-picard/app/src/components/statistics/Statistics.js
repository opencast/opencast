import React, {useEffect, useState} from "react";
import {connect} from "react-redux";
import {Link} from "react-router-dom";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import Header from "../Header";
import Footer from "../Footer";
import MainNav from "../shared/MainNav";
import TimeSeriesStatistics from "../shared/TimeSeriesStatistics";
import {
    getOrganizationId,
    getStatistics,
    hasStatistics,
    hasStatisticsError,
    isFetchingStatistics
} from "../../selectors/statisticsSelectors";
import {
    fetchStatisticsPageStatistics,
    fetchStatisticsPageStatisticsValueUpdate
} from "../../thunks/statisticsThunks";
import {hasAccess} from "../../utils/utils";
import {getUserInformation} from "../../selectors/userInfoSelectors";
import {styleNavClosed, styleNavOpen} from "../../utils/componentsUtils";

const Statistics = ({ organizationId, statistics, isLoadingStatistics, hasStatistics, hasError, user,
                    loadStatistics, recalculateStatistics }) => {
    const { t } = useTranslation();

    const [displayNavigation, setNavigation] = useState(false);

    useEffect(() => {
        loadStatistics().then();
    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    };

    /* generates file name for download-link for a statistic */
    const statisticsCsvFileName = (statsTitle) => {
        const sanitizedStatsTitle = statsTitle.replace(/[^0-9a-z]/gi, '_').toLowerCase();
        return 'export_organization?_' + organizationId + '_' + sanitizedStatsTitle + '.csv';
    };

    return (
        <span>
            <Header />
            <section className="action-nav-bar">

                {/* Include Burger-button menu */}
                <MainNav  isOpen={displayNavigation}
                          toggleMenu={toggleNavigation}/>

                <nav>
                    {hasAccess("ROLE_UI_STATISTICS_ORGANIZATION_VIEW", user) && (
                        <Link to="/statistics/organization"
                              className={cn({active: true})}
                              onClick={ () => {} }>
                            {t('STATISTICS.NAVIGATION.ORGANIZATION')}
                        </Link>
                    )}
                </nav>
            </section>

            {/* main view of this page, displays statistics */}
            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                <div className="obj statistics">

                    {/* heading */}
                    <div className="controls-container">
                        <h1> {t("STATISTICS.NAVIGATION.ORGANIZATION")  /* Organisation */} </h1>
                    </div>

                    {!isLoadingStatistics && (

                        (hasError || !hasStatistics)? (
                            /* error message */
                            <div className="obj">
                                <div className="modal-alert danger">
                                    {t("STATISTICS.NOT_AVAILABLE")}
                                </div>
                            </div>
                        ):(

                            /* iterates over the different available statistics */
                            statistics.map((stat, key) => (
                                <div className="obj" key={key}>

                                    {/* title of statistic */}
                                    <header className="no-expand">
                                        {t(stat.title)}
                                    </header>

                                    {(stat.providerType === 'timeSeries')? (
                                        /* visualization of statistic for time series data */
                                        <div className="obj-container">
                                            <TimeSeriesStatistics
                                                t={t}
                                                resourceId={organizationId}
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
                                    ):(
                                        /* unsupported type message */
                                        <div className="modal-alert danger">
                                            {t('STATISTICS.UNSUPPORTED_TYPE')}
                                        </div>
                                    )}
                                </div>
                            ))
                        )
                    )}
                </div>
            </div>
            <Footer />
        </span>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    organizationId: getOrganizationId(state),
    hasStatistics: hasStatistics(state),
    isLoadingStatistics: isFetchingStatistics(state),
    statistics: getStatistics(state),
    hasError: hasStatisticsError(state),
    user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadStatistics: () => dispatch(fetchStatisticsPageStatistics()),
    recalculateStatistics: (organizationId, providerId, from, to, dataResolution, timeMode) => dispatch(fetchStatisticsPageStatisticsValueUpdate(organizationId, providerId, from, to, dataResolution, timeMode)),
});

export default connect(mapStateToProps, mapDispatchToProps)(Statistics);

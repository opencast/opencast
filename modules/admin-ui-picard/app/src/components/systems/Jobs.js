import React, {useEffect, useState} from "react";
import { Link } from "react-router-dom";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import cn from 'classnames';
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import MainNav from "../shared/MainNav";
import Notifications from "../shared/Notifications";
import {jobsTemplateMap} from "../../configs/tableConfigs/jobsTableConfig";
import {getTotalJobs} from "../../selectors/jobSelectors";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchJobs} from "../../thunks/jobThunks";
import {loadJobsIntoTable, loadServersIntoTable, loadServicesIntoTable} from "../../thunks/tableThunks";
import {fetchServers} from "../../thunks/serverThunks";
import {fetchServices} from "../../thunks/serviceThunks";
import {editTextFilter} from "../../actions/tableFilterActions";
import {setOffset} from "../../actions/tableActions";
import {styleNavClosed, styleNavOpen} from "../../utils/componentsUtils";
import {logger} from "../../utils/logger";
import Header from "../Header";
import Footer from "../Footer";
import {getUserInformation} from "../../selectors/userInfoSelectors";
import {hasAccess} from "../../utils/utils";

/**
 * This component renders the table view of jobs
 */
const Jobs = ({ loadingJobs, loadingJobsIntoTable, jobs, loadingFilters,
                  loadingServers, loadingServersIntoTable, loadingServices,
                  loadingServicesIntoTable, resetTextFilter, resetOffset, user }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadJobs = async () => {
        // Fetching jobs from server
        await loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
    }

    const loadServers = () => {
        loadingFilters("servers");

        // Reset the current page to first page
        resetOffset();

        // Fetching servers from server
        loadingServers()

        // Load servers into table
        loadingServersIntoTable()
    }

    const loadServices = () => {
        loadingFilters("services");

        // Reset the current page to first page
        resetOffset();

        // Fetching services from server
        loadingServices();

        // Load services into table
        loadingServicesIntoTable()
    }

    useEffect(() => {
        resetTextFilter();

        // Load jobs on mount
        loadJobs().then(r => logger.info(r));

        // Fetch jobs every minute
        let fetchJobInterval = setInterval(loadJobs, 5000);

        return () => clearInterval(fetchJobInterval);

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    }

    return (
        <>
            <Header />
            <section className="action-nav-bar">

                {/* Include Burger-button menu*/}
                <MainNav isOpen={displayNavigation}
                         toggleMenu={toggleNavigation} />

                <nav>
                    {hasAccess("ROLE_UI_JOBS_VIEW", user) && (
                        <Link to="/systems/jobs"
                              className={cn({active: true})}
                              onClick={() => {
                                  loadingFilters("jobs");
                                  loadJobs().then();
                              }}>
                            {t('SYSTEMS.NAVIGATION.JOBS')}
                        </Link>
                    )}
                    {hasAccess("ROLE_UI_SERVERS_VIEW", user) && (
                        <Link to="/systems/servers"
                              className={cn({active: false})}
                              onClick={() => loadServers()}>
                            {t('SYSTEMS.NAVIGATION.SERVERS')}
                        </Link>
                    )}
                    {hasAccess("ROLE_UI_SERVICES_VIEW", user) && (
                        <Link to="/systems/services"
                              className={cn({active: false})}
                              onClick={() => loadServices()}>
                            {t('SYSTEMS.NAVIGATION.SERVICES')}
                        </Link>
                    )}
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/* Include notifications component */}
                <Notifications />

                <div  className="controls-container">
                    {/* Include filters component */}
                    <TableFilters loadResource={loadingJobs}
                                  loadResourceIntoTable={loadingJobsIntoTable}
                                  resource={'jobs'}/>
                    <h1>{t('SYSTEMS.JOBS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: jobs })}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={jobsTemplateMap} />
            </div>
            <Footer />
        </>
    )

}

// Getting state data out of redux store
const mapStateToProps = state => ({
    jobs: getTotalJobs(state),
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
    loadingServers: () => dispatch(fetchServers()),
    loadingServersIntoTable: () => dispatch(loadServersIntoTable()),
    loadingServices: () => dispatch(fetchServices()),
    loadingServicesIntoTable: () => dispatch(loadServicesIntoTable()),
    resetTextFilter: () => dispatch(editTextFilter('')),
    resetOffset: () => dispatch(setOffset(0))
});

export default connect(mapStateToProps, mapDispatchToProps)(Jobs);

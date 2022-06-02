import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import { Link } from "react-router-dom";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {connect} from "react-redux";
import Notifications from "../shared/Notifications";
import {serversTemplateMap} from "../../configs/tableConfigs/serversTableConfig";
import {getTotalServers} from "../../selectors/serverSelectors";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchServers} from "../../thunks/serverThunks";
import {loadJobsIntoTable, loadServersIntoTable, loadServicesIntoTable} from "../../thunks/tableThunks";
import {fetchJobs} from "../../thunks/jobThunks";
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
 * This component renders the table view of servers
 */
const Servers = ({ loadingServers, loadingServersIntoTable, servers, loadingFilters,
                     loadingJobs, loadingJobsIntoTable, loadingServices,
                     loadingServicesIntoTable, resetTextFilter, resetOffset, user }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadServers = async () => {
        // Fetching servers from server
        await loadingServers();

        // Load servers into table
        loadingServersIntoTable();
    }

    const loadJobs = () => {
        loadingFilters("jobs");

        // Reset the current page to first page
        resetOffset();

        // Fetching jobs from server
        loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
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

        // Load servers on mount
        loadServers().then(r => logger.info(r));

        // Fetch servers every minute
        let fetchServersInterval = setInterval(loadServers, 5000);

        return () => clearInterval(fetchServersInterval);

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
                              className={cn({active: false})}
                              onClick={() => loadJobs()}>
                            {t('SYSTEMS.NAVIGATION.JOBS')}
                        </Link>
                    )}
                    {hasAccess("ROLE_UI_SERVERS_VIEW", user) && (
                        <Link to="/systems/servers"
                              className={cn({active: true})}
                              onClick={() => {
                                  loadingFilters("servers");
                                  loadServers().then();
                              }}>
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
                    <TableFilters loadResource={loadingServers}
                                  loadResourceIntoTable={loadingServersIntoTable}
                                  resource={'servers'}/>
                    <h1>{t('SYSTEMS.SERVERS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: servers })}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={serversTemplateMap} />
            </div>
            <Footer />
        </>
    )
}

// Getting state data out of redux store
const mapStateToProps = state =>({
    servers: getTotalServers(state),
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingServers: () => dispatch(fetchServers()),
    loadingServersIntoTable: () => dispatch(loadServersIntoTable()),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
    loadingServices: () => dispatch(fetchServices()),
    loadingServicesIntoTable: () => dispatch(loadServicesIntoTable()),
    resetTextFilter: () => dispatch(editTextFilter('')),
    resetOffset: () => dispatch(setOffset(0))
});

export default connect(mapStateToProps, mapDispatchToProps)(Servers);

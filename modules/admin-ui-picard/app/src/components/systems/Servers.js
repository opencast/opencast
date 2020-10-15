import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {serversTemplateMap} from "../../configs/tableConfigs/serversTableConfig";
import {getServers} from "../../selectors/serverSelectors";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchServers} from "../../thunks/serverThunks";
import {loadJobsIntoTable, loadServersIntoTable, loadServicesIntoTable} from "../../thunks/tableThunks";
import {fetchJobs} from "../../thunks/jobThunks";
import {fetchServices} from "../../thunks/serviceThunks";

const Servers = ({ loadingServers, loadingServersIntoTable, servers, loadingFilters,
                     loadingJobs, loadingJobsIntoTable, loadingServices,
                     loadingServicesIntoTable }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadServers = async () => {
        // Fetching jobs from server
        await loadingServers();

        // Load jobs into table
        loadingServersIntoTable();
    }

    const loadJobs = () => {
        loadingJobs();

        loadingJobsIntoTable();
    }

    const loadServices = () => {
        // Fetching services from server
        loadingServices();

        // Load services into table
        loadingServicesIntoTable()
    }

    useEffect(() => {
        // Load jobs on mount
        loadServers().then(r => console.log(r));

        // Load filters
        loadingFilters('servers');

    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    }

    const styleNavOpen = {
        marginLeft: '130px',
    };
    const styleNavClosed = {
        marginLeft: '20px',
    };

    return (
        <>
            <section className="action-nav-bar">

                {/* Include Burger-button menu*/}
                <MainNav isOpen={displayNavigation}
                         toggleMenu={toggleNavigation} />

                <nav>
                    {/*todo: with role*/}
                    <Link to="/systems/jobs"
                          className={cn({active: false})}
                          onClick={() => loadJobs()}>
                        {t('SYSTEMS.NAVIGATION.JOBS')}
                    </Link>
                    <Link to="/systems/servers"
                          className={cn({active: true})}
                          onClick={() => loadServers()}>
                        {t('SYSTEMS.NAVIGATION.SERVERS')}
                    </Link>
                    <Link to="/systems/services"
                          className={cn({active: false})}
                          onClick={() => loadServices()}>
                        {t('SYSTEMS.NAVIGATION.SERVICES')}
                    </Link>
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/*Todo: What is data-admin-ng-notifications?*/}

                <div  className="controls-container">
                    {/* Include filters component */}
                    <TableFilters loadResource={loadingServers}
                                  loadResourceIntoTable={loadingServersIntoTable}
                                  resource={'servers'}/>
                    <h1>{t('SYSTEMS.SERVERS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: servers.length})}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={serversTemplateMap} />
            </div>
        </>
    )
}

const mapStateToProps = state =>({
    servers: getServers(state)
});

const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingServers: () => dispatch(fetchServers()),
    loadingServersIntoTable: () => dispatch(loadServersIntoTable()),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
    loadingServices: () => dispatch(fetchServices()),
    loadingServicesIntoTable: () => dispatch(loadServicesIntoTable())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Servers));

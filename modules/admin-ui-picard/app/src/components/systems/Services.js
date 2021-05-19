import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchJobs} from "../../thunks/jobThunks";
import {loadJobsIntoTable, loadServersIntoTable, loadServicesIntoTable} from "../../thunks/tableThunks";
import {fetchServers} from "../../thunks/serverThunks";
import {servicesTemplateMap} from "../../configs/tableConfigs/servicesTableConfig";
import {getTotalServices} from "../../selectors/serviceSelector";
import {fetchServices} from "../../thunks/serviceThunks";
import Notifications from "../shared/Notifications";
import {editTextFilter} from "../../actions/tableFilterActions";

/**
 * This component renders the table view of services
 */
const Services = ({ loadingServices, loadingServicesIntoTable, services, loadingFilters,
                      loadingJobs, loadingJobsIntoTable, loadingServers,
                      loadingServersIntoTable, resetTextFilter }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadServices = async () => {
        // Fetching services from server
        await loadingServices();

        // Load services into table
        loadingServicesIntoTable()
    }

    const loadJobs = () => {
        // Fetching jobs from server
        loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
    }

    const loadServers = () => {
        // Fetching servers from server
        loadingServers()

        // Load servers into table
        loadingServersIntoTable()
    }

    useEffect(() => {
        resetTextFilter();

        // Load services on mount
        loadServices().then(r => console.log(r));

        // Load filters
        loadingFilters('services');

        // Fetch services every minute
        let fetchServicesInterval = setInterval(loadServices, 100000);

        return () => clearInterval(fetchServicesInterval);

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
                          className={cn({active: false})}
                          onClick={() => loadServers()}>
                        {t('SYSTEMS.NAVIGATION.SERVERS')}
                    </Link>
                    <Link to="/systems/services"
                          className={cn({active: true})}
                          onClick={() => loadServices()}>
                        {t('SYSTEMS.NAVIGATION.SERVICES')}
                    </Link>
                </nav>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/* Include notifications component */}
                <Notifications />

                <div  className="controls-container">
                    {/* Include filters component */}
                    <TableFilters loadResource={loadingServices}
                                  loadResourceIntoTable={loadingServicesIntoTable}
                                  resource={'services'}/>
                    <h1>{t('SYSTEMS.SERVICES.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: services })}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={servicesTemplateMap} />
            </div>
        </>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    services: getTotalServices(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingServices: () => dispatch(fetchServices()),
    loadingServicesIntoTable: () => dispatch(loadServicesIntoTable()),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
    loadingServers: () => dispatch(fetchServers()),
    loadingServersIntoTable: () => dispatch(loadServersIntoTable()),
    resetTextFilter: () => dispatch(editTextFilter(''))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Services));

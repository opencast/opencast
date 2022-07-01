import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import { Link } from "react-router-dom";
import {connect} from "react-redux";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import MainNav from "../shared/MainNav";
import Notifications from "../shared/Notifications";
import {servicesTemplateMap} from "../../configs/tableConfigs/servicesTableConfig";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {fetchJobs} from "../../thunks/jobThunks";
import {loadJobsIntoTable, loadServersIntoTable, loadServicesIntoTable} from "../../thunks/tableThunks";
import {fetchServices} from "../../thunks/serviceThunks";
import {fetchServers} from "../../thunks/serverThunks";
import {getTotalServices} from "../../selectors/serviceSelector";
import {editTextFilter} from "../../actions/tableFilterActions";
import {setOffset} from "../../actions/tableActions";
import {styleNavClosed, styleNavOpen} from "../../utils/componentsUtils";
import {logger} from "../../utils/logger";
import Header from "../Header";
import Footer from "../Footer";
import {getUserInformation} from "../../selectors/userInfoSelectors";
import {hasAccess} from "../../utils/utils";
import { getCurrentFilterResource } from '../../selectors/tableFilterSelectors';

/**
 * This component renders the table view of services
 */
const Services = ({ loadingServices, loadingServicesIntoTable, services, loadingFilters,
                      loadingJobs, loadingJobsIntoTable, loadingServers,
                      loadingServersIntoTable, resetTextFilter, resetOffset, user, currentFilterType }) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadServices = async () => {
        // Fetching services from server
        await loadingServices();

        // Load services into table
        loadingServicesIntoTable()
    }

    const loadJobs = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching jobs from server
        loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
    }

    const loadServers = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching servers from server
        loadingServers()

        // Load servers into table
        loadingServersIntoTable()
    }

    useEffect(() => {
        if ("services" !== currentFilterType) {
            loadingFilters("services");
        }

        resetTextFilter();

        // Load services on mount
        loadServices().then(r => logger.info(r));

        // Fetch services every minute
        let fetchServicesInterval = setInterval(loadServices, 5000);

        return () => clearInterval(fetchServicesInterval);

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
                              className={cn({active: false})}
                              onClick={() => loadServers()}>
                            {t('SYSTEMS.NAVIGATION.SERVERS')}
                        </Link>
                    )}
                    {hasAccess("ROLE_UI_SERVICES_VIEW", user) && (
                        <Link to="/systems/services"
                              className={cn({active: true})}
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
                    <TableFilters loadResource={loadingServices}
                                  loadResourceIntoTable={loadingServicesIntoTable}
                                  resource={'services'}/>
                    <h1>{t('SYSTEMS.SERVICES.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: services })}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={servicesTemplateMap} />
            </div>
            <Footer />
        </>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    services: getTotalServices(state),
    user: getUserInformation(state),
    currentFilterType: getCurrentFilterResource(state)
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
    resetTextFilter: () => dispatch(editTextFilter('')),
    resetOffset: () => dispatch(setOffset(0))
});

export default connect(mapStateToProps, mapDispatchToProps)(Services);

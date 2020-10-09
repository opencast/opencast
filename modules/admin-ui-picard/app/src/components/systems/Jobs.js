import React, {useEffect, useState} from "react";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";
import {withRouter} from "react-router-dom";
import {connect} from "react-redux";
import {fetchFilters} from "../../thunks/tableFilterThunks";
import {jobsTemplateMap} from "../../configs/tableConfigs/jobsTableConfig";
import {fetchJobs} from "../../thunks/jobThunks";
import {loadJobsIntoTable} from "../../thunks/tableThunks";
import {getJobs} from "../../selectors/jobSelectors";


const Jobs = ({ loadingJobs, loadingJobsIntoTable, jobs, loadingFilters}) => {
    const { t } = useTranslation();
    const [displayNavigation, setNavigation] = useState(false);

    const loadJobs = async () => {
        // Fetching jobs from server
        await loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
    }

    // TODO: implement
    const loadServers = () => {
        console.log('placeholder');
    }

    // TODO: implement
    const loadServices = () => {
        console.log('placeholder');
    }

    useEffect(() => {
        // Load jobs on mount
        loadJobs().then(r => console.log(r));

        // Load filters
        loadingFilters('jobs');

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
                          className={cn({active: true})}
                          onClick={() => loadJobs()}>
                        {t('SYSTEMS.NAVIGATION.JOBS')}
                    </Link>
                    <Link to="/systems/servers"
                          className={cn({active: false})}
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
                    <TableFilters loadResource={loadingJobs}
                                  loadResourceIntoTable={loadingJobsIntoTable}
                                  resource={'jobs'}/>
                    <h1>{t('SYSTEMS.JOBS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: jobs.length})}</h4>
                </div>
                {/* Include table component */}
                <Table templateMap={jobsTemplateMap} />
            </div>
        </>
    )

}

const mapStateToProps = state => ({
    jobs: getJobs(state)
});

const mapDispatchToProps = dispatch => ({
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Jobs));

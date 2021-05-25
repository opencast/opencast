import React from "react";
import {useTranslation} from "react-i18next";
import {Link} from "react-router-dom";
import {
    loadEventsIntoTable,
    loadJobsIntoTable,
    loadRecordingsIntoTable,
    loadThemesIntoTable,
    loadUsersIntoTable
} from "../../thunks/tableThunks";
import {fetchEvents} from "../../thunks/eventThunks";
import {fetchRecordings} from "../../thunks/recordingThunks";
import {fetchJobs} from "../../thunks/jobThunks";
import {fetchUsers} from "../../thunks/userThunks";
import {fetchThemes} from "../../thunks/themeThunks";
import {connect} from "react-redux";
import {fetchStats} from "../../thunks/tableFilterThunks";
import {setOffset} from "../../actions/tableActions";

/**
 * This component renders the main navigation that opens when the burger button is clicked
 */
const MainNav = ({ isOpen, toggleMenu, loadingEvents, loadingEventsIntoTable, loadingStats, loadingRecordings,
                     loadingRecordingsIntoTable, loadingJobs, loadingJobsIntoTable, loadingUsers, loadingUsersIntoTable,
                     loadingThemes, loadingThemesIntoTable, resetOffset }) => {
    const { t } = useTranslation();

    const loadEvents = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching stats from server
        loadingStats();

        // Fetching events from server
        loadingEvents();

        // Load events into table
        loadingEventsIntoTable();
    }

    const loadRecordings = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching recordings from server
        loadingRecordings();

        // Load recordings into table
        loadingRecordingsIntoTable();
    }

    const loadJobs = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching jobs from server
        loadingJobs();

        // Load jobs into table
        loadingJobsIntoTable();
    }

    const loadUsers = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching users from server
        loadingUsers();

        // Load users into table
        loadingUsersIntoTable()
    }

    const loadThemes = () => {
        // Reset the current page to first page
        resetOffset();

        // Fetching themes from server
        loadingThemes();

        // Load themes into table
        loadingThemesIntoTable();
    }

    return (
        <>
            <div className="menu-top" onClick={() => toggleMenu()}>
                {isOpen && (
                    <nav id="roll-up-menu">
                        <div id="nav-container">
                            {/* Todo: add role management (see MainNav in admin-ui-frontend)*/}
                            {/* todo: more than one href? how? (see MainNav admin-ui-frontend)*/}
                            <Link to="/events/events" onClick={() => loadEvents()}>
                                <i className="events" title={t('NAV.EVENTS.TITLE')}/>
                            </Link>
                            <Link to="/recordings/recordings" onClick={() => loadRecordings()}>
                                <i className="recordings" title={t('NAV.CAPTUREAGENTS.TITLE')}/>
                            </Link>
                            <Link to="/systems/jobs" onClick={() => loadJobs()}>
                                <i className="systems" title={t('NAV.SYSTEMS.TITLE')}/>
                            </Link>
                            <Link to="/users/users" onClick={() => loadUsers()}>
                                <i className="users" title={t('NAV.USERS.TITLE')}/>
                            </Link>
                            <Link to="/configuration/themes" onClick={() => loadThemes()}>
                                <i className="configuration" title={t('NAV.CONFIGURATION.TITLE')}/>
                            </Link>
                            <Link to="/statistics/organization">
                                <i className="statistics" title={t('NAV.STATISTICS.TITLE')}/>
                            </Link>
                        </div>
                    </nav>
                )}

            </div>
        </>
    );
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingEvents: () => dispatch(fetchEvents()),
    loadingEventsIntoTable: () => dispatch(loadEventsIntoTable()),
    loadingStats: () => dispatch(fetchStats()),
    loadingRecordings: () => dispatch(fetchRecordings()),
    loadingRecordingsIntoTable: () => dispatch(loadRecordingsIntoTable()),
    loadingJobs: () => dispatch(fetchJobs()),
    loadingJobsIntoTable: () => dispatch(loadJobsIntoTable()),
    loadingUsers: () => dispatch(fetchUsers()),
    loadingUsersIntoTable: () => dispatch(loadUsersIntoTable()),
    loadingThemes: () => dispatch(fetchThemes()),
    loadingThemesIntoTable: () => dispatch(loadThemesIntoTable()),
    resetOffset: () => dispatch(setOffset(0))
});

export default connect(null, mapDispatchToProps)(MainNav);

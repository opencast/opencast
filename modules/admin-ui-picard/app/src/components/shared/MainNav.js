import React from "react";
import {useTranslation, withTranslation} from "react-i18next";

import eventIcon from '../../img/events.png';
import recordingIcon from '../../img/recordings.png';
import systemsIcon from '../../img/servers.png';
import userIcon from '../../img/user-group.png';
import configIcon from '../../img/configuration.png';
import statisticsIcon from '../../img/activity.png';
import {Link} from "react-router-dom";
import {loadEventsIntoTable} from "../../thunks/tableThunks";
import {fetchEvents} from "../../thunks/eventThunks";
import {connect} from "react-redux";

/**
 * This component renders the main navigation that opens when the burger button is clicked
 */
const MainNav = ({ isOpen, toggleMenu, loadingEvents, loadingEventsIntoTable}) => {
    const { t } = useTranslation();

    const loadEvents = () => {
        // Fetching events from server
        loadingEvents(false, false);

        // Load events into table
        loadingEventsIntoTable();
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
                                <i className="events" title={t('NAV.EVENTS.TITLE')}>
                                    <img src={eventIcon}/>
                                </i>
                            </Link>
                            <Link to="/recordings/recordings">
                                <i className="recordings" title={t('NAV.CAPTUREAGENTS.TITLE')}>
                                    <img src={recordingIcon}/>
                                </i>
                            </Link>
                            <Link to="/systems/jobs">
                                <i className="systems" title={t('NAV.SYSTEMS.TITLE')}>
                                    <img src={systemsIcon}/>
                                </i>
                            </Link>
                            <Link to="/users/users">
                                <i className="users" title={t('NAV.USERS.TITLE')}>
                                    <img src={userIcon}/>
                                </i>
                            </Link>
                            <Link to="/configuration/themes">
                                <i className="configuration" title={t('NAV.CONFIGURATION.TITLE')}>
                                    <img src={configIcon}/>
                                </i>
                            </Link>
                            <Link to="/statistics/organization">
                                <i className="statistics" title={t('NAV.STATISTICS.TITLE')}>
                                    <img src={statisticsIcon}/>
                                </i>
                            </Link>
                        </div>
                    </nav>
                )}

            </div>
        </>
    );
}


const mapDispatchToProps = dispatch => ({
    loadingEvents: (filter, sort) => dispatch(fetchEvents()),
    loadingEventsIntoTable: () => dispatch(loadEventsIntoTable())
});

export default connect(null, mapDispatchToProps)(MainNav);

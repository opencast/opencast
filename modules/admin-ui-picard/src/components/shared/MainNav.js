import React from "react";
import {withTranslation} from "react-i18next";

import eventIcon from '../../img/events.png';
import recordingIcon from '../../img/recordings.png';
import systemsIcon from '../../img/servers.png';
import userIcon from '../../img/user-group.png';
import configIcon from '../../img/configuration.png';
import statisticsIcon from '../../img/activity.png';

/**
 * This component renders the main navigation that opens when the burger button is clicked
 */
const MainNav = ({t, isOpen, toggleMenu}) => (
    <>
        <div className="menu-top" onClick={() => toggleMenu()}>
            {isOpen && (
                <nav id="roll-up-menu">
                    <div id="nav-container">
                        {/* Todo: add role management (see MainNav in admin-ui-frontend)*/}
                        {/* todo: more than one href? how? (see MainNav admin-ui-frontend)*/}
                        <a href="#!/events/events">
                            <i className="events" title={t('NAV.EVENTS.TITLE')}>
                                <img src={eventIcon}/>
                            </i>
                        </a>
                        <a href="#!/recordings/recordings">
                            <i className="recordings" title={t('NAV.CAPTUREAGENTS.TITLE')}>
                                <img src={recordingIcon} />
                            </i>
                        </a>
                        <a href="#!/systems/jobs">
                            <i className="systems" title={t('NAV.SYSTEMS.TITLE')}>
                                <img src={systemsIcon} />
                            </i>
                        </a>
                        <a href="#!/users/users">
                            <i className="users" title={t('NAV.USERS.TITLE')}>
                                <img src={userIcon} />
                            </i>
                        </a>
                        <a href="#!/configuration/themes">
                            <i className="configuration" title={t('NAV.CONFIGURATION.TITLE')}>
                                <img src={configIcon} />
                            </i>
                        </a>
                        <a href="#!/statistics/organization">
                            <i className="statistics" title={t('NAV.STATISTICS.TITLE')}>
                                <img src={statisticsIcon} />
                            </i>
                        </a>
                    </div>
                </nav>
            )}

        </div>
    </>
);


export default withTranslation()(MainNav);

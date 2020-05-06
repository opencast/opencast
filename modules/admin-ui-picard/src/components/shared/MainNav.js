import React, {Component} from "react";
import {withTranslation} from "react-i18next";

import eventIcon from '../../img/events.png';
import recordingIcon from '../../img/recordings.png';
import systemsIcon from '../../img/servers.png';
import userIcon from '../../img/user-group.png';
import configIcon from '../../img/configuration.png';
import statisticsIcon from '../../img/activity.png';

class MainNav extends Component {
    constructor(props) {
        super(props);


    }


    render() {
        const { t, isOpen } = this.props;
        return(
            <>
                <div className="menu-top" onClick={() => this.props.toggleMenu()}>
                    {isOpen && (
                        <nav id="roll-up-menu">
                            <div id="nav-container">
                                <a>
                                    <i className="events" title={t('NAV.EVENTS.TITLE')}>
                                        <img src={eventIcon}/>
                                    </i>
                                </a>
                                <a>
                                    <i className="recordings" title={t('NAV.CAPTUREAGENTS.TITLE')}>
                                        <img src={recordingIcon} />
                                    </i>
                                </a>
                                <a>
                                    <i className="systems" title={t('NAV.SYSTEMS.TITLE')}>
                                        <img src={systemsIcon} />
                                    </i>
                                </a>
                                <a>
                                    <i className="users" title={t('NAV.USERS.TITLE')}>
                                        <img src={userIcon} />
                                    </i>
                                </a>
                                <a>
                                    <i className="configuration" title={t('NAV.CONFIGURATION.TITLE')}>
                                        <img src={configIcon} />
                                    </i>
                                </a>
                                <a>
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
    }
}

export default withTranslation()(MainNav);

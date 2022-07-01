import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import { Link } from "react-router-dom";
import i18n from "../i18n/i18n";
import languages from "../i18n/languages";
import opencastLogo from '../img/opencast-white.svg';
import {fetchHealthStatus} from "../thunks/healthThunks";
import { getErrorCount, getHealthStatus } from '../selectors/healthSelectors';
import {getCurrentLanguageInformation, hasAccess} from "../utils/utils";
import {logger} from "../utils/logger";
import {getOrgProperties, getUserInformation} from "../selectors/userInfoSelectors";
import axios from "axios";
import RegistrationModal from "./shared/RegistrationModal";
import { loadServicesIntoTable} from "../thunks/tableThunks";
import {studioURL} from "../configs/generalConfig";
import HotKeyCheatSheet from "./shared/HotKeyCheatSheet";
import {GlobalHotKeys} from "react-hotkeys";
import {availableHotkeys} from "../configs/hotkeysConfig";
import { setSpecificServiceFilter } from '../thunks/tableFilterThunks';


// Get code, flag and name of the current language
const currentLanguage = getCurrentLanguageInformation();



// References for detecting a click outside of the container of the dropdown menus
const containerLang = React.createRef();
const containerHelp = React.createRef();
const containerUser = React.createRef();
const containerNotify = React.createRef();

function changeLanguage(code) {
    // Load json-file of the language with provided code
    i18n.changeLanguage(code);
    // Reload window for updating the flag of the language dropdown menu
    window.location.reload();
}

function logout() {
    axios.get('/j_spring_security_logout')
        .then(response => {
            logger.info(response);
            window.location.reload();
        })
        .catch(response => {
            logger.error(response);
        });
}

/**
 * Component that renders the header and the navigation in the upper right corner.
 */
const Header = ({ loadingHealthStatus, healthStatus, errorCounter, user, orgProperties,
    setSpecificServiceFilter, loadingServicesIntoTable }) => {
    const { t } = useTranslation();
    // State for opening (true) and closing (false) the dropdown menus for language, notification, help and user
    const [displayMenuLang, setMenuLang] = useState(false);
    const [displayMenuUser, setMenuUser] = useState(false);
    const [displayMenuNotify, setMenuNotify] = useState(false);
    const [displayMenuHelp, setMenuHelp] = useState(false);
    const [displayRegistrationModal, setRegistrationModal] = useState(false);
    const [displayHotKeyCheatSheet, setHotKeyCheatSheet] = useState(false);

    const loadHealthStatus = async () => {
        await loadingHealthStatus();
    }

    const hideMenuHelp = () => {
        setMenuHelp(false);
    }

    const showRegistrationModal = () => {
        setRegistrationModal(true);
    }

    const hideRegistrationModal = () => {
        setRegistrationModal(false);
    }

    const redirectToServices = async () => {
        // Load services into table
        await loadingServicesIntoTable();

        // set the action filter value of services to true
        await setSpecificServiceFilter('actions', 'true');
    }

    const showHotKeyCheatSheet = () => {
        setHotKeyCheatSheet(true);
    }

    const hideHotKeyCheatSheet = () => {
        setHotKeyCheatSheet(false);
    }

    const hotKeyHandlers = {
        HOTKEY_CHEATSHEET: showHotKeyCheatSheet
    }

    useEffect(() => {
        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerLang.current && !containerLang.current.contains(e.target)) {
                setMenuLang(false);
            }

            if (containerHelp.current && !containerHelp.current.contains(e.target)) {
                setMenuHelp(false);
            }

            if (containerUser.current && !containerUser.current.contains(e.target)) {
                setMenuUser(false);
            }

            if (containerNotify.current && !containerNotify.current.contains(e.target)) {
                setMenuNotify(false);
            }
        }

        // Fetching health status information at mount
        loadHealthStatus().then(r => logger.info(r));
        // Fetch health status every minute
        setInterval(loadingHealthStatus, 5000);

        // Event listener for handle a click outside of dropdown menu
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }
    }, []);

    return(
        <>
            <GlobalHotKeys keyMap={availableHotkeys.general} handlers={hotKeyHandlers} />
            <header className="primary-header">
                {/* Opencast logo in upper left corner */}
                <div className="header-branding">
                    <a href="/" target="_self" className="logo">
                        <img src={opencastLogo} alt="Opencast Logo"/>
                    </a>
                </div>

                {/* Navigation with icons and dropdown menus in upper right corner */}
                <nav className="header-nav nav-dd-container" id="nav-dd-container">
                    {/* Select language */}
                    <div className="nav-dd lang-dd" id="lang-dd" ref={containerLang}>
                        <div className="lang" title={t('LANGUAGE')}  onClick={() => setMenuLang(!displayMenuLang)}>
                            <img src={currentLanguage.flag} alt={currentLanguage.code} />
                        </div>
                        {/* Click on the flag icon, a dropdown menu with all available languages opens */}
                        { displayMenuLang && (
                            <MenuLang />
                        )}
                    </div>

                    {/* Media Module */}
                    {/* Show icon only if mediaModuleUrl is set*/}
                    {/* The seperated if clauses are intentional because on start up orgProperties are not filled yet,
                    otherwise the app crashes */}
                    {!!orgProperties && (
                        !!orgProperties["org.opencastproject.admin.mediamodule.url"] && (
                            <div className="nav-dd" title={t('MEDIAMODULE')}>
                                <a href={orgProperties["org.opencastproject.admin.mediamodule.url"]}>
                                    <span className="fa fa-play-circle"/>
                                </a>
                            </div>
                        )
                    )}

                {/* Opencast Studio */}
                {hasAccess("ROLE_STUDIO", user) && (
                    <div className="nav-dd" title="Studio">
                        <a href={studioURL}>
                            <span className="fa fa-video-camera"/>
                        </a>
                    </div>
                )}

                {/* System warnings and notifications */}
                {hasAccess("ROLE_ADMIN", user) && (
                    <div className="nav-dd info-dd" id="info-dd" title={t('SYSTEM_NOTIFICATIONS')} ref={containerNotify}>
                        <div onClick={() => setMenuNotify(!displayMenuNotify)}>
                            <i className="fa fa-bell" aria-hidden="true"/>
                            {errorCounter !== 0 &&(
                              <span id="error-count" className="badge" >{errorCounter}</span>
                            )}
                            {/* Click on the bell icon, a dropdown menu with all services in serviceList and their status opens */}
                            {displayMenuNotify && (
                                <MenuNotify healthStatus={healthStatus}
                                            redirectToServices={redirectToServices}/>
                            )}
                        </div>
                    </div>
                )}


                    {/* Help */}
                    {/* Show only if documentationUrl or restdocsUrl is set */}
                    {/* The seperated if clauses are intentional because on start up orgProperties are not filled yet,
                    otherwise the app crashes */}
                    {!!orgProperties && (
                        (!!orgProperties["org.opencastproject.admin.help.documentation.url"] || !!orgProperties["org.opencastproject.admin.help.restdocs.url"]) && (
                            <div title="Help" className="nav-dd" id="help-dd" ref={containerHelp} >
                                <div className="fa fa-question-circle" onClick={() => setMenuHelp(!displayMenuHelp)}/>
                                {/* Click on the help icon, a dropdown menu with documentation, REST-docs and shortcuts (if available) opens */}
                                {displayMenuHelp && (
                                    <MenuHelp hideMenuHelp={hideMenuHelp}
                                              showRegistrationModal={showRegistrationModal}
                                              showHotKeyCheatSheet={showHotKeyCheatSheet}
                                              orgProperties={orgProperties}
                                              user={user}/>
                                )}
                            </div>
                        )
                    )}

                {/* Username */}
                    <div className="nav-dd user-dd" id="user-dd" ref={containerUser}>
                    <div className="h-nav" onClick={() => setMenuUser(!displayMenuUser)}>{user.user.name || user.user.username}<span className="dropdown-icon"/></div>
                {/* Click on username, a dropdown menu with the option to logout opens */}
                {displayMenuUser && (
                    <MenuUser />
                    )}
                    </div>
                </nav>
            </header>

            {/* Adopters Registration Modal */}
            {displayRegistrationModal && (
                <RegistrationModal close={hideRegistrationModal}/>
            )}

            {/* Hotkey Cheat Sheet */}
            {displayHotKeyCheatSheet && (
                <HotKeyCheatSheet close={hideHotKeyCheatSheet}/>
            )}
        </>

    );
};

const MenuLang = () => {
    return (
        <ul className="dropdown-ul">
            {/* one list item for each available language */}
            {languages.map((language, key) => (
                <li key={key}>
                    <a onClick={() => changeLanguage(language.code)}>
                        <img className="lang-flag"
                             src={language.flag}
                             alt={language.code}/>
                        {language.long}
                    </a>
                </li>
            ))}

        </ul>
    )
};

const MenuNotify = ({ healthStatus, redirectToServices }) => {

    return (
        <ul className="dropdown-ul">
            {/* For each service in the serviceList (ActiveMQ and Background Services) one list item */}
            {healthStatus.map((service, key) => (
                <li key={key}>
                    {!!service.status && (
                        <Link to="/systems/services" onClick={async () => await redirectToServices()}>
                            <span> {service.name} </span>
                            {service.error ? (
                                <span className="ng-multi-value ng-multi-value-red">{service.status}</span>
                            ) : (
                                <span className="ng-multi-value ng-multi-value-green">{service.status}</span>
                            )}
                        </Link>
                    )}
                </li>
            ))}

        </ul>
    )
};



const MenuHelp = ({ hideMenuHelp, showRegistrationModal, showHotKeyCheatSheet, user, orgProperties }) => {

    const { t } = useTranslation();

    // show Adopter Registration Modal and hide drop down
    const showAdoptersRegistrationModal = () => {
        showRegistrationModal();
        hideMenuHelp();
    }

    // show Hotkeys Cheat Sheet and hide drop down
    const showHotKeys = () => {
        showHotKeyCheatSheet();
        hideMenuHelp();
    }

    return (
        <>
            <ul className="dropdown-ul">
                {/* Show only if documentationUrl is set */}
                {!!orgProperties["org.opencastproject.admin.help.documentation.url"] && (
                    <li>
                        <a href={orgProperties["org.opencastproject.admin.help.documentation.url"]}>
                            <span>{t('HELP.DOCUMENTATION')}</span>
                        </a>
                    </li>
                )}
                {/* Show only if restUrl is set */}
                {(!!orgProperties["org.opencastproject.admin.help.restdocs.url"] && hasAccess("ROLE_ADMIN", user)) && (
                    <li>
                        <a target="_self" href={orgProperties["org.opencastproject.admin.help.restdocs.url"]}>
                            <span>{t('HELP.REST_DOC')}</span>
                        </a>
                    </li>
                )}
                <li>
                    <a onClick={() => showHotKeys()}>
                        <span>{t('HELP.HOTKEY_CHEAT_SHEET')}</span>
                    </a>
                </li>
                {/* Adoter registration Modal */}
                {hasAccess("ROLE_ADMIN", user) && (
                    <li>
                        <a onClick={() => showAdoptersRegistrationModal()}>
                            <span>{t('HELP.ADOPTER_REGISTRATION')}</span>
                        </a>
                    </li>
                )}
            </ul>
        </>
    )

};

const MenuUser = () => {
    const { t } = useTranslation();
    return (
        <ul className="dropdown-ul">
            <li>
                <a onClick={() => logout()}>
                <span className="logout-icon">
                    {t('LOGOUT')}
                </span>
                </a>
            </li>
        </ul>
    )
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    healthStatus: getHealthStatus(state),
    errorCounter: getErrorCount(state),
    user: getUserInformation(state),
    orgProperties: getOrgProperties(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingHealthStatus: () => dispatch(fetchHealthStatus()),
    loadingServicesIntoTable: () => dispatch(loadServicesIntoTable()),
    setSpecificServiceFilter: (filter, filterValue) => dispatch(setSpecificServiceFilter(filter, filterValue))
});

export default connect(mapStateToProps, mapDispatchToProps)(Header);

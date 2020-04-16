// React imports
import React, {Component} from "react";
import {withTranslation} from "react-i18next";

// service and library imports
import i18n from "../i18n/i18n";
import languages from "../i18n/languages";
import services from "../services/restServiceMonitor";


// image and icon imports
import opencastLogo from '../img/opencast-white.svg';

import {FaPlayCircle, FaVideo, FaBell, FaQuestionCircle, FaChevronDown, FaPowerOff} from "react-icons/fa";



// Todo: Find suitable place to define them and get these links out of config-file or whatever
const mediaModuleUrl = "http://localhost:8080/engage/ui/index.html";
const studio = "https://opencast.org/";
const documentationUrl = "https://opencast.org/";
const restUrl = "https://opencast.org/";

// Get status of background services and activeMQ for notification and system warnings (bell icon)
const serviceList = Object.keys(services.service).map(key => {
    let service = services.service[key];
    service.name = key;
    return service;
});

/**
 * Component that renders the header and the navigation in the upper right corner.
 */
class Header extends Component {
    // References for detecting a click outside of the container of the dropdown menus
    containerLang = React.createRef();
    containerHelp = React.createRef();
    containerUser = React.createRef();
    containerNotify = React.createRef();

    // Get code, flag and name of the current language
    currentLanguage = languages.find(({ code }) => code === i18n.language);



    constructor(props) {
        super(props);
        this.state = {
            // State for opening (true) and closing (false) the dropdown menus for language, notification, help and user
            displayMenuLang: false,
            displayMenuUser: false,
            displayMenuHelp: false,
            displayMenuNotify: false

        };

        // Binding of methods so one can use 'this'
        // Necessary for correct working of the code
        this.handleMenuLang = this.handleMenuLang.bind(this);
        this.handleMenuUser = this.handleMenuUser.bind(this);
        this.handleMenuHelp = this.handleMenuHelp.bind(this);
        this.handleMenuNotify = this.handleMenuNotify.bind(this);
        this.handleClickOutside = this.handleClickOutside.bind(this);
        this.changeLanguage = this.changeLanguage.bind(this);
        this.showHotkeyCheatSheet = this.showHotkeyCheatSheet.bind(this);
        this.logout = this.logout.bind(this);

        console.log(serviceList);
    }

    componentDidMount() {
        // Event listener for handle a click outside of dropdown menu
        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }


    showHotkeyCheatSheet() {
        //todo: Implement method
        console.log('Show Hot Keys');
    };

    //Todo: implement logout-method
    logout() {
        console.log('logout');
    };

    handleMenuLang() {
        this.setState(state => ({
            displayMenuLang: !state.displayMenuLang
        }));
        console.log('State Changed ' + this.state.displayMenuLang);

    }

    handleMenuUser(e) {
        e.preventDefault();
        this.setState(state => ({
            displayMenuUser: !state.displayMenuUser
        }));
        console.log('user state changed ' + this.state.displayMenuUser);
    }

    handleMenuHelp(e) {
        e.preventDefault();
        this.setState(state => ({
            displayMenuHelp: !state.displayMenuHelp
        }));
        console.log('Help state changed ' + this.state.displayMenuHelp);
    }

    handleMenuNotify(e) {
        e.preventDefault();
        this.setState(state => ({
            displayMenuNotify: !state.displayMenuNotify
        }));
        console.log('notify state changed ' + this.state.displayMenuUser);
    }

    handleClickOutside(e) {
        if (this.containerLang.current && !this.containerLang.current.contains(e.target)) {
            this.setState({
                displayMenuLang: false
            });
        }

        if (this.containerHelp.current && !this.containerHelp.current.contains(e.target)) {
            this.setState({
                displayMenuHelp: false
            });
        }

        if (this.containerUser.current && !this.containerUser.current.contains(e.target)) {
            this.setState({
                displayMenuUser: false
            });
        }

        if (this.containerNotify.current && !this.containerNotify.current.contains(e.target)) {
            this.setState({
                displayMenuNotify: false
            });
        }
    }

    changeLanguage(code) {
        // Load json-file of the language with provided code
        i18n.changeLanguage(code);
        // Reload window for updating the flag of the language dropdown menu
        window.location.reload();
    }

    render() {
        // Get t-function of i18next for translation
        const {t} = this.props;

        console.log('Languages: ' + this.currentLanguage);
        console.log('language: ' + i18n.language);

        return (
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
                    <div className="nav-dd lang-dd" id="lang-dd" ref={this.containerLang}>
                        <div className="lang" title={t('LANGUAGE')}  onClick={this.handleMenuLang}>
                            <img src={this.currentLanguage.flag} alt={this.currentLanguage.code} />
                        </div>
                        {/* Click on the flag icon, a dropdown menu with all available languages opens */}
                        { this.state.displayMenuLang && (
                            <ul className="dropdown-ul">
                                {/* one list item for each available language */}
                                {languages.map((language, key) => (
                                    <li key={key}>
                                        <a onClick={() => this.changeLanguage(language.code)}>
                                            <img className="lang-flag"
                                                 src={language.flag}
                                                 alt={language.code}/>
                                            {language.long}
                                        </a>
                                    </li>
                                ))}

                            </ul>
                        )}
                    </div>

                    {/* Media Module */}
                    {/* Show icon only if mediaModuleUrl is set*/}
                    {!!mediaModuleUrl && (
                        <div className="nav-dd" title={t('MEDIAMODULE')}>
                            <a href={mediaModuleUrl}>
                                <FaPlayCircle className="fa fa-play-circle"/>
                            </a>
                        </div>
                    )}

                    {/* Opencast Studio */}
                    {/* Todo: before with 'with Role="ROLE_STUDIO": What is this? implement React equivalent */}
                    <div className="nav-dd" title="Studio">
                        <a href={studio}>
                            <FaVideo className="fa fa-video-circle"/>
                        </a>
                    </div>

                    {/* System warnings and notifications */}
                    {/* Todo: before with 'with Role="ROLE_ADMIN": What is this? implement React equivalent */}
                    <div className="nav-dd info-dd" id="info-dd" title={t('SYSTEM_NOTIFICATIONS')} ref={this.containerNotify}>
                        <div onClick={this.handleMenuNotify}>
                        <FaBell className="fa fa-bell" aria-hidden="true"/>
                        <span id="error-count" className="badge" >{services.numErr}</span>
                        {/* Click on the bell icon, a dropdown menu with all services in serviceList and their status opens */}
                        {this.state.displayMenuNotify && (
                            <ul className="dropdown-ul">
                                {/* For each service in the serviceList (ActiveMQ and Background Services) one list item */}
                                {serviceList.map((service, key) => (
                                    <li key={key}>
                                        <a>
                                            <span> {service.name} </span>
                                            {service.error ? (
                                                <span className="ng-multi-value ng-multi-value-red">{service.status}</span>
                                            ) : (
                                                <span className="ng-multi-value ng-multi-value-green">{service.status}</span>
                                            )}
                                        </a>
                                    </li>
                                ))}

                            </ul>
                        )}
                        </div>
                    </div>

                    {/* Help */}
                    {/* Show only if documentationUrl or restdocsUrl is set */}
                    {(!!documentationUrl || !!restUrl) && (
                        <div title="Help" className="nav-dd" id="help-dd" ref={this.containerHelp} >
                            <FaQuestionCircle className="fa fa-question-circle" onClick={this.handleMenuHelp}/>
                            {/* Click on the help icon, a dropdown menu with documentation, REST-docs and shortcuts (if available) opens */}
                            {this.state.displayMenuHelp && (
                                <ul className="dropdown-ul">
                                    {/* Show only if documentationUrl is set */}
                                    {!!documentationUrl && (
                                        <li>
                                            <a href={documentationUrl}>
                                                <span>{t('HELP.DOCUMENTATION')}</span>
                                            </a>
                                        </li>
                                    )}
                                    {/* Todo: only if restUrl is there and with-role="ROLE_ADMIN */}
                                    {/* Show only if restUrl is set */}
                                    {!!restUrl && (
                                        <li>
                                            <a target="_self" href={restUrl}>
                                                <span>{t('HELP.REST_DOC')}</span>
                                            </a>
                                        </li>
                                    )}
                                    <li>
                                        <a onClick={this.showHotkeyCheatSheet}>
                                            <span>{t('HELP.HOTKEY_CHEAT_SHEET')}</span>
                                        </a>
                                    </li>
                                </ul>
                            )}
                        </div>
                    )}

                    {/* Username */}
                    <div className="nav-dd user-dd" id="user-dd" ref={this.containerUser}>
                        {/* Todo: User name of currently logged in user*/}
                        <div className="h-nav" onClick={this.handleMenuUser}>Here is space for a name <FaChevronDown className="dropdown-icon"/></div>
                        {/* Click on username, a dropdown menu with the option to logout opens */}
                        {this.state.displayMenuUser && (
                            <ul className="dropdown-ul">
                            <li>
                                <a onClick={this.logout} >
                                    <span>
                                        <FaPowerOff className="logout-icon" />
                                        {t('LOGOUT')}
                                    </span>
                                </a>
                            </li>
                        </ul>
                        )}
                    </div>

                </nav>
            </header>
        );
    }
}

export default withTranslation()(Header);

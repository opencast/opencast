import React, {Component} from 'react';
import {withTranslation} from "react-i18next";
import TableFilters from "../components/shared/TableFilters";
import MainNav from "../components/shared/MainNav";


class Recordings extends Component {
    containerAction = React.createRef();

    constructor(props) {
        super(props);
        this.state = {
            displayActionMenu: false,
            displayNavigation: false
        };

        this.handleActionMenu = this.handleActionMenu.bind(this);
        this.handleClickOutside = this.handleClickOutside.bind(this);

        this.toggleNavigation = this.toggleNavigation.bind(this);

    }

    toggleNavigation() {
        this.setState(state => ({
            displayNavigation: !state.displayNavigation
        }));
        console.log("menu toggled");
    }

    componentDidMount() {
        document.addEventListener("mousedown", this.handleClickOutside);
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    handleActionMenu(e) {
        e.preventDefault();
        this.setState(state => ({
            displayActionMenu: !state.displayActionMenu
        }));
    }

    handleClickOutside(e) {
        if (this.containerAction.current && !this.containerAction.current.contains(e.target)) {
            this.setState({
                displayActionMenu: false
            });
        }
    }

    render() {
        const { t } =  this.props;
        const styleNavOpen = {
            marginLeft: '130px',
        };
        const styleNavClosed = {
            marginLeft: '20px',
        };
        return (
            <>
                <section className="action-nav-bar">
                    {/*TODO: include Components containing the suitable buttons for the current view */}
                    {/*Todo: Include Burger-button menu */}
                    <MainNav  isOpen={this.state.displayNavigation}
                              toggleMenu={this.toggleNavigation}/>

                    <nav>
                        {/*Todo: Show only if user has ROLE_UI_EVENTS_VIEW*/}
                        <a href="!/events/events">{t('EVENTS.EVENTS.NAVIGATION.EVENTS')}</a>
                        <a href="!/events/series">{t('EVENTS.EVENTS.NAVIGATION.SERIES')}</a>
                    </nav>
                    {/*Todo: show only if table resource is events and with Role ROLE_UI_EVENTS_COUNTERS_VIEW */}
                    <div className="stats-container">
                        {/*Todo: include status bar component*/}
                    </div>
                </section>

                {/*TODO: HIER STYLE EINFÜGEN, vorher welcher style soll genutzt werden*/}
                <div className="main-view" style={this.state.displayNavigation ? styleNavOpen : styleNavClosed}>
                    {/*todo: include notification component*/}

                    <div className="controls-container">
                        <div className="filters-container">
                            <div className="drop-down-container" onClick={this.handleActionMenu} ref={this.containerAction}>
                                <span>{t('BULK_ACTIONS.CAPTION')}</span>
                                {/* show dropdown if actions is clicked*/}
                                { this.state.displayActionMenu && (
                                    <ul className="dropdown-ul">
                                        {/*todo: show only if user has right to delete resource (with-role ROLE_UI_{{ table.resource }}_DELETE*/}
                                        <li>
                                            {/*todo: open overlay for deletion and change EVENTS to table.resource.toUpperCase() */}
                                            <a>{t('BULK_ACTIONS.DELETE.EVENTS.CAPTION')}</a>
                                        </li>
                                        {/*todo: show only if table resource is events and with-Role ROLE_UI_TASKS_CREATE*/}
                                        <li>
                                            {/*todo: open overlay for schedule task */}
                                            <a>{t('BULK_ACTIONS.SCHEDULE_TASK.CAPTION')}</a>
                                        </li>
                                        {/*todo: show only if table resource is events and user is admin with roles
                                    ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT and ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT */}
                                        <li>
                                            {/*todo: open overlay for edit events */}
                                            <a>{t('BULK_ACTIONS.EDIT_EVENTS.CAPTION')}</a>
                                        </li>
                                        {/*todo: show only if table resource is events and user is admin with roles
                                    ROLE_UI_EVENTS_DETAILS_METADATA_EDIT*/}
                                        <li>
                                            {/*todo: open overlay for edit metadata of events */}
                                            <a>{t('BULK_ACTIONS.EDIT_EVENTS_METADATA.CAPTION')}</a>
                                        </li>
                                    </ul>
                                )}

                            </div>
                            {/*todo: include filters component*/}
                            <TableFilters />
                        </div>

                        {/*todo: instead of events table.caption*/}
                        <h1>{t('EVENTS.EVENTS.TABLE.CAPTION')}</h1>
                        {/*todo: instead of 4 the numberOfRows (table.pagination.totalItems)*/}
                        <h4>{t('TABLE_SUMMARY') + " " + "4"}</h4>
                    </div>
                    {/*todo: include table component*/}
                </div>
            </>
        );
    }
}

export default withTranslation()(Recordings);

import React, {useEffect, useState} from 'react';
import {useTranslation} from "react-i18next";
import TableFilters from "../components/shared/TableFilters";
import MainNav from "../components/shared/MainNav";
import Stats from "../components/shared/Stats";
import Table from "../components/shared/Table";
import * as et from "../thunks/eventThunks";
import * as tt from "../thunks/tableThunks";
import * as es from "../selectors/eventSelectors";
import {connect} from "react-redux";
import {eventsTemplateMap} from "../configs/tableConfigs/eventsTableConfig";

// References for detecting a click outside of the container of the dropdown menu
const containerAction = React.createRef();

const Events = ({loadingEvents, loadingEventsIntoTable, events}) => {
    const { t } = useTranslation();
    const [displayActionMenu, setActionMenu] = useState(false);
    const [displayNavigation, setNavigation] = useState(false);

    useEffect(() => {

        // Fetching events from server
        loadingEvents();

        // Load events into table
        loadingEventsIntoTable();

        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerAction.current && !containerAction.current.contains(e.target)) {
               setActionMenu(false);
            }
        }

        // Event listener for handle a click outside of dropdown menu
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }


    })

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
        console.log("menu toggled");
    };

    const handleActionMenu = (e) => {
        e.preventDefault();
        setActionMenu(!displayActionMenu);
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
                {/*TODO: include Components containing the suitable buttons for the current view */}
                {/*Todo: Include Burger-button menu */}
                <MainNav  isOpen={displayNavigation}
                          toggleMenu={toggleNavigation}/>

                <nav>
                    {/*Todo: Show only if user has ROLE_UI_EVENTS_VIEW*/}
                    <a href="#!/events/events">{t('EVENTS.EVENTS.NAVIGATION.EVENTS')}</a>
                    <a href="#!/events/series">{t('EVENTS.EVENTS.NAVIGATION.SERIES')}</a>
                </nav>
                {/*Todo: show only if table resource is events and with Role ROLE_UI_EVENTS_COUNTERS_VIEW */}
                <div className="stats-container">
                    {/* Include status bar component*/}
                    <Stats />
                </div>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/*todo: include notification component*/}

                <div className="controls-container">
                    <div className="filters-container">
                        <div className="drop-down-container" onClick={e => handleActionMenu(e)} ref={containerAction}>
                            <span>{t('BULK_ACTIONS.CAPTION')}</span>
                            {/* show dropdown if actions is clicked*/}
                            { displayActionMenu && (
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
                        {/* Include filters component*/}
                        <TableFilters />
                    </div>

                    {/*todo: instead of events table.caption*/}
                    <h1>{t('EVENTS.EVENTS.TABLE.CAPTION')}</h1>
                    {/*todo: instead of 4 the numberOfRows (table.pagination.totalItems)*/}
                    <h4>{events.length + " " + t('TABLE_SUMMARY')}</h4>
                </div>
                {/*todo: include table component*/}
                <Table templateMap={eventsTemplateMap}/>
            </div>
        </>
    );
};

const mapStateToProps = state => ({
    events: es.getEvents(state)
});


const mapDispatchToProps = dispatch => ({
    loadingEvents: () => dispatch(et.fetchEvents()),
    loadingEventsIntoTable: () => dispatch(tt.loadEventsIntoTable())
});

export default connect(mapStateToProps, mapDispatchToProps)(Events);

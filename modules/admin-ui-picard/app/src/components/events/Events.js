import React, {useEffect, useState} from 'react';
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import TableFilters from "../shared/TableFilters";
import MainNav from "../shared/MainNav";
import Stats from "../shared/Stats";
import Table from "../shared/Table";
import {fetchEventMetadata, fetchEvents} from "../../thunks/eventThunks";
import {loadEventsIntoTable, loadSeriesIntoTable} from "../../thunks/tableThunks";
import {getTotalEvents, isLoading, isShowActions} from "../../selectors/eventSelectors";
import {connect} from "react-redux";
import {eventsTemplateMap} from "../../configs/tableConfigs/eventsTableConfig";
import Link from "react-router-dom/Link";
import {withRouter} from "react-router-dom";
import {fetchSeries} from "../../thunks/seriesThunks";
import {fetchFilters, fetchStats} from "../../thunks/tableFilterThunks";
import Notifications from "../shared/Notifications";
import NewResourceModal from "../shared/NewResourceModal";
import {editTextFilter} from "../../actions/tableFilterActions";
import {setOffset} from "../../actions/tableActions";
import DeleteEventsModal from "./partials/DeleteEventsModal";
import StartTaskModal from "./partials/StartTaskModal";
import EditScheduledEventsModal from "./partials/EditScheduledEventsModal";
import EditMetadataEventsModal from "./partials/EditMetadataEventsModal";

// References for detecting a click outside of the container of the dropdown menu
const containerAction = React.createRef();

/**
 * This component renders the table view of events
 */
const Events = ({loadingEvents, loadingEventsIntoTable, events, showActions, loadingSeries,
                        loadingSeriesIntoTable, loadingFilters, loadingStats, loadingEventMetadata, resetTextFilter,
                    resetOffset }) => {
    const { t } = useTranslation();
    const [displayActionMenu, setActionMenu] = useState(false);
    const [displayNavigation, setNavigation] = useState(false);
    const [displayNewEventModal, setNewEventModal] = useState(false);
    const [displayDeleteModal, setDeleteModal] = useState(false);
    const [displayStartTaskModal, setStartTaskModal] = useState(false);
    const [displayEditScheduledEventsModal, setEditScheduledEventsModal] = useState(false);
    const [displayEditMetadataEventsModal, setEditMetadataEventsModal] = useState(false);

    const loadEvents = async () => {
        // Fetching stats from server
        loadingStats();

        // Fetching events from server
        await loadingEvents();

        // Load events into table
        loadingEventsIntoTable();

    };

    const loadSeries = () => {
        // Reset the current page to first page
        resetOffset();

        //fetching series from server
        loadingSeries();

        //load series into table
        loadingSeriesIntoTable();
    }

    useEffect(() => {
        resetTextFilter();

        // Load events on mount
        loadEvents().then(r => console.log(r));

        // Load event filters
        loadingFilters("events");

        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerAction.current && !containerAction.current.contains(e.target)) {
               setActionMenu(false);
            }
        }

        // Fetch events every minute
        let fetchEventsInterval = setInterval(loadEvents, 100000);

        // Event listener for handle a click outside of dropdown menu
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
            clearInterval(fetchEventsInterval);
        }


    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    };

    const handleActionMenu = e => {
        e.preventDefault();
        setActionMenu(!displayActionMenu);
    }

    const showNewEventModal = async () => {
        await loadingEventMetadata();

        setNewEventModal(true);
    }

    const hideNewEventModal = () => {
        setNewEventModal(false);
    };

    const hideDeleteModal = () => {
        setDeleteModal(false);
    };

    const hideStartTaskModal = () => {
        setStartTaskModal(false);
    };

    const hideEditScheduledEventsModal = () => {
        setEditScheduledEventsModal(false);
    };

    const hideEditMetadataEventsModal = () => {
        setEditMetadataEventsModal(false);
    };

    const styleNavOpen = {
        marginLeft: '130px',
    };
    const styleNavClosed = {
        marginLeft: '20px',
    };
    return (
        <>
            <section className="action-nav-bar">
                {/*TODO: include with role things */}
                <div className="btn-group">
                    <button className="add" onClick={() => showNewEventModal()}>
                        <i className="fa fa-plus" />
                        <span>{t('EVENTS.EVENTS.ADD_EVENT')}</span>
                    </button>
                </div>

                {/* Display modal for new event if add event button is clicked */}
                <NewResourceModal showModal={displayNewEventModal}
                                  handleClose={hideNewEventModal}
                                  resource={"events"} />

                {/* Display bulk actions modal if one is chosen from dropdown */}
                {displayDeleteModal && (
                    <DeleteEventsModal close={hideDeleteModal}/>
                )}

                {displayStartTaskModal && (
                    <StartTaskModal close={hideStartTaskModal}/>
                )}

                {displayEditScheduledEventsModal && (
                    <EditScheduledEventsModal  close={hideEditScheduledEventsModal}/>
                )}

                {displayEditMetadataEventsModal && (
                    <EditMetadataEventsModal close={hideEditMetadataEventsModal}/>
                )}

                {/* Include Burger-button menu */}
                <MainNav  isOpen={displayNavigation}
                          toggleMenu={toggleNavigation}/>

                <nav>
                    {/*Todo: Show only if user has ROLE_UI_EVENTS_VIEW*/}
                    <Link to="/events/events"
                          className={cn({active: true})}
                          onClick={() => loadEvents()}>
                        {t('EVENTS.EVENTS.NAVIGATION.EVENTS')}
                    </Link>
                    <Link to="/events/series"
                          className={cn({active: false})}
                          onClick={() => loadSeries()}>
                        {t('EVENTS.EVENTS.NAVIGATION.SERIES')}
                    </Link>
                </nav>

                <div className="stats-container">
                    {/* Include status bar component*/}
                    <Stats />
                </div>
            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
                {/* Include notifications component */}
                <Notifications />

                <div className="controls-container">
                    <div className="filters-container">
                        <div className={cn("drop-down-container", {disabled: !showActions})}
                             onClick={e => handleActionMenu(e)}
                             ref={containerAction} >
                            <span>{t('BULK_ACTIONS.CAPTION')}</span>
                            {/* show dropdown if actions is clicked*/}
                            { displayActionMenu && (
                                <ul className="dropdown-ul">
                                    {/*todo: show only if user has right to delete resource (with-role ROLE_UI_{{ table.resource }}_DELETE*/}
                                    <li>
                                        <a onClick={() => setDeleteModal(true)}>
                                            {t('BULK_ACTIONS.DELETE.EVENTS.CAPTION')}
                                        </a>
                                    </li>
                                    {/*todo: show only  with-Role ROLE_UI_TASKS_CREATE*/}
                                    <li>
                                        <a onClick={() => setStartTaskModal(true)}>
                                            {t('BULK_ACTIONS.SCHEDULE_TASK.CAPTION')}
                                        </a>
                                    </li>
                                    {/*todo: show only if  user is admin with roles
                                    ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT and ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT */}
                                    <li>
                                        <a onClick={() => setEditScheduledEventsModal(true)}>
                                            {t('BULK_ACTIONS.EDIT_EVENTS.CAPTION')}
                                        </a>
                                    </li>
                                    {/*todo: show  user is admin with roles ROLE_UI_EVENTS_DETAILS_METADATA_EDIT*/}
                                    <li>
                                        <a onClick={() => setEditMetadataEventsModal(true)}>
                                            {t('BULK_ACTIONS.EDIT_EVENTS_METADATA.CAPTION')}
                                        </a>
                                    </li>
                                </ul>
                            )}
                        </div>

                        {/* Include filters component*/}
                        <TableFilters loadResource={loadingEvents}
                                      loadResourceIntoTable={loadingEventsIntoTable}
                                      resource={'events'}/>
                    </div>
                    <h1>{t('EVENTS.EVENTS.TABLE.CAPTION')}</h1>
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: events })}</h4>
                </div>
                {/*Include table component*/}
                <Table templateMap={eventsTemplateMap} resourceType="events"/>
            </div>
        </>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    events: getTotalEvents(state),
    showActions: isShowActions(state),
    isLoadingEvents: isLoading(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingEvents: () => dispatch(fetchEvents()),
    loadingEventsIntoTable: () => dispatch(loadEventsIntoTable()),
    loadingSeries: () => dispatch(fetchSeries()),
    loadingSeriesIntoTable: () => dispatch(loadSeriesIntoTable()),
    loadingFilters: resource => dispatch(fetchFilters(resource)),
    loadingStats: () => dispatch(fetchStats()),
    loadingEventMetadata: () => dispatch(fetchEventMetadata()),
    resetTextFilter: () => dispatch(editTextFilter('')),
    resetOffset: () => dispatch(setOffset(0))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Events));

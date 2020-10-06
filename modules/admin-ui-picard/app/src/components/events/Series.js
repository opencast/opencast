import React, {useEffect, useState} from "react";
import MainNav from "../shared/MainNav";
import Link from "react-router-dom/Link";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import TableFilters from "../shared/TableFilters";
import Table from "../shared/Table";

import { fetchSeries } from"../../thunks/seriesThunks";
import {loadEventsIntoTable, loadSeriesIntoTable} from "../../thunks/tableThunks";
import {seriesTemplateMap} from "../../configs/tableConfigs/seriesTableConfig";
import {connect} from "react-redux";
import {withRouter} from "react-router-dom";
import {fetchEvents} from "../../thunks/eventThunks";
import {getSeries, isShowActions} from "../../selectors/seriesSeletctor";


// References for detecting a click outside of the container of the dropdown menu
const containerAction = React.createRef();


const Series = ({ showActions, loadingSeries, loadingSeriesIntoTable, loadingEvents, loadingEventsIntoTable,
                    series }) => {
    const { t } = useTranslation();
    const [displayActionMenu, setActionMenu] = useState(false);
    const [displayNavigation, setNavigation] = useState(false);

    const loadEvents = () => {
        // Fetching events from server
        loadingEvents();

        // Load events into table
        loadingEventsIntoTable();
    };

    const loadSeries = async () => {
        //fetching series from server
        await loadingSeries();

        //load series into table
        loadingSeriesIntoTable();
    }

    useEffect( () => {

        // Load Series on mount
        loadSeries().then(r => console.log(r));

        // Function for handling clicks outside of an dropdown menu
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
    }, []);

    const toggleNavigation = () => {
        setNavigation(!displayNavigation);
    };

    const handleActionMenu = e => {
        e.preventDefault();
        setActionMenu(!displayActionMenu);
    }

    const placeholder = () => {
        console.log("To be implemented");
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
                {/*TODO: include with role ROLE_UI_SERIES_CREATE */}
                <div className="btn-group">
                    <button className="add" onClick={() => placeholder()}>
                        <i className="fa fa-plus" />
                        <span>{t('EVENTS.EVENTS.ADD_SERIES')}</span>
                    </button>
                </div>

                {/* Include Burger-button menu */}
                <MainNav  isOpen={displayNavigation}
                          toggleMenu={toggleNavigation}/>

                <nav>
                    {/*Todo: Show only if user has ROLE_UI_EVENTS_VIEW*/}
                    <Link to="/events/events" onClick={() => loadEvents()}>{t('EVENTS.EVENTS.NAVIGATION.EVENTS')}</Link>
                    <Link to="/events/series" onClick={() => loadSeries()}>{t('EVENTS.EVENTS.NAVIGATION.SERIES')}</Link>
                </nav>

            </section>

            <div className="main-view" style={displayNavigation ? styleNavOpen : styleNavClosed}>
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
                                        {/*todo: open overlay for deletion */}
                                        <a>{t('BULK_ACTIONS.DELETE.SERIES.CAPTION')}</a>
                                    </li>
                                </ul>
                            )}
                        </div>
                        {/* Include filters component */}
                        {/* Todo: fetch actual filters for series */}
                        <TableFilters loadResource={loadingSeries}
                                      loadResourceIntoTable={loadingSeriesIntoTable}
                                      resource={'series'} />

                    </div>
                    <h1>{t('EVENTS.SERIES.TABLE.CAPTION')}</h1>
                    {/* Include table view */}
                    <h4>{t('TABLE_SUMMARY', { numberOfRows: series.length})}</h4>
                </div>
                <Table templateMap={seriesTemplateMap} />
            </div>
        </>
    )
};

// todo: Selectors for this
const mapStateToProps = state => ({
    series: getSeries(state),
    showActions: isShowActions(state),
});

const mapDispatchToProps = dispatch => ({
    loadingSeries: () => dispatch(fetchSeries()),
    loadingSeriesIntoTable: () => dispatch(loadSeriesIntoTable()),
    loadingEvents: () => dispatch(fetchEvents()),
    loadingEventsIntoTable: () => dispatch(loadEventsIntoTable())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Series));

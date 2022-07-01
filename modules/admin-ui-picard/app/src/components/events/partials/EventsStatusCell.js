import React from 'react';
import {useTranslation} from "react-i18next";
import {getFilters} from "../../../selectors/tableFilterSelectors";
import {editFilterValue} from "../../../actions/tableFilterActions";
import {connect} from "react-redux";
import {fetchEvents} from "../../../thunks/eventThunks";
import {loadEventsIntoTable} from "../../../thunks/tableThunks";

/**
 * This component renders the status cells of events in the table view
 */
const EventsStatusCell = ({ row, filterMap, editFilterValue, loadEvents, loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = async status => {
        let filter = filterMap.find(({ name }) => name === "status");
        if (!!filter) {
            await editFilterValue(filter.name, status);
            await loadEvents();
            loadEventsIntoTable();
        }
    };

    return (
        <a className="crosslink"
           onClick={() => addFilter(row.event_status)}
           title={t('EVENTS.EVENTS.TABLE.TOOLTIP.STATUS')}>
            {t(row.displayable_status)}
        </a>
    );
};


// Getting state data out of redux store
const mapStateToProps = state => ({
    filterMap: getFilters(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    editFilterValue: (filterName, value) => dispatch(editFilterValue(filterName, value)),
    loadEvents: () => dispatch(fetchEvents()),
    loadEventsIntoTable: () => dispatch(loadEventsIntoTable())
});


export default connect(mapStateToProps, mapDispatchToProps)(EventsStatusCell);

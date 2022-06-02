import React from 'react';
import {useTranslation} from "react-i18next";
import {editFilterValue} from "../../../actions/tableFilterActions";
import {fetchEvents} from "../../../thunks/eventThunks";
import {loadEventsIntoTable} from "../../../thunks/tableThunks";
import {getFilters} from "../../../selectors/tableFilterSelectors";
import {connect} from "react-redux";

/**
 * This component renders the start date cells of events in the table view
 */
const EventsDateCell = ({ row, filterMap, editFilterValue, loadEvents,
                            loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = async date => {

        let filter = filterMap.find(({ name }) => name === "startDate");
        if (!!filter) {
            await editFilterValue(filter.name, date + '/' + date);
            await loadEvents();
            loadEventsIntoTable();
        }

    };

    return (
        // Link template for start date of event
        <a  className="crosslink"
            title={t('EVENTS.EVENTS.TABLE.TOOLTIP.START')} onClick={() => addFilter(row.date)}>
            {t('dateFormats.date.short', { date: new Date(row.date) })}
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

export default connect(mapStateToProps, mapDispatchToProps)(EventsDateCell);

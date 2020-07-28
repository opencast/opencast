import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfa from "../../../actions/tableFilterActions";
import * as et from "../../../thunks/eventThunks";
import * as tt from "../../../thunks/tableThunks";
import * as tfs from "../../../selectors/tableFilterSelectors";
import {connect} from "react-redux";

/**
 * This component renders the start date cells of events in the table view
 */
const EventsDateCell = ({ row, filterMap, setStartDate, setEndDate, editFilterValue, loadEvents,
                            loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = date => {
        setStartDate(date);
        setEndDate(date);

        let filter = filterMap.find(({ name }) => name === "startDate");
        if (!!filter) {
            // Todo: Currently only startDate considered
            editFilterValue(filter.name, date);
            loadEvents(true, false);
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
    filterMap: tfs.getFilters(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    setStartDate: date => dispatch(tfa.setStartDate(date)),
    setEndDate: date => dispatch(tfa.setEndDate(date)),
    editFilterValue: (filterName, value) => dispatch(tfa.editFilterValue(filterName, value)),
    loadEvents: (filter, sort) => dispatch(et.fetchEvents(filter, sort)),
    loadEventsIntoTable: () => dispatch(tt.loadEventsIntoTable())
});

export default connect(mapStateToProps, mapDispatchToProps)(EventsDateCell);

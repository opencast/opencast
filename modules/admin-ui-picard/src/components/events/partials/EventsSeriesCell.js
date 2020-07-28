import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfs from "../../../selectors/tableFilterSelectors";
import * as tfa from "../../../actions/tableFilterActions";
import {connect} from "react-redux";
import * as et from "../../../thunks/eventThunks";
import * as tt from "../../../thunks/tableThunks";

/**
 * This component renders the series cells of events in the table view
 */
const EventsSeriesCell = ({ row, filterMap, editFilterValue, loadEvents, loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = series => {
        let filter = filterMap.find(({ name }) => name === "series");
        if (!!filter) {
            editFilterValue(filter.name, series.title);
            loadEvents(true, false);
            loadEventsIntoTable();
        }
    };

    return (
        !!row.series && (
            // Link template for series of event
            <a className="crosslink"
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.SERIES')}
               onClick={() => addFilter(row.series)}>
                {row.series.title}
            </a>
        )
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    filterMap: tfs.getFilters(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    editFilterValue: (filterName, value) => dispatch(tfa.editFilterValue(filterName, value)),
    loadEvents: (filter, sort) => dispatch(et.fetchEvents(filter, sort)),
    loadEventsIntoTable: () => dispatch(tt.loadEventsIntoTable())
});


export default connect(mapStateToProps, mapDispatchToProps)(EventsSeriesCell);

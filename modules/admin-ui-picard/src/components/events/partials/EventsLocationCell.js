import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfs from "../../../selectors/tableFilterSelectors";
import * as tfa from "../../../actions/tableFilterActions";
import {connect} from "react-redux";
import * as et from "../../../thunks/eventThunks";
import * as tt from "../../../thunks/tableThunks";

/**
 * This component renders the location cells of events in the table view
 */
const EventsLocationCell = ({ row, filterMap, editFilterValue, loadEvents, loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = location => {
        let filter = filterMap.find(({ name }) => name === "location");
        if (!!filter) {
            editFilterValue(filter.name, location);
            loadEvents(true, false);
            loadEventsIntoTable();
        }
    };

    return (
        // Link template for location of event
        <a className="crosslink"
           title={t('EVENTS.EVENTS.TABLE.TOOLTIP.LOCATION')}
           onClick={() => addFilter(row.location)}>
            {row.location}
        </a>
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


export default connect(mapStateToProps, mapDispatchToProps)(EventsLocationCell);

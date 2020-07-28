import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfs from "../../../selectors/tableFilterSelectors";
import * as tfa from "../../../actions/tableFilterActions";
import {connect} from "react-redux";
import * as et from "../../../thunks/eventThunks";
import * as tt from "../../../thunks/tableThunks";

/**
 * This component renders the presenters cells of events in the table view
 */
const EventsPresentersCell = ({ row, filterMap, editFilterValue, loadEvents, loadEventsIntoTable })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = presenter => {
        let filter = filterMap.find(({ name }) => name === "presentersBibliographic");
        if (!!filter) {
            editFilterValue(filter.name, presenter);
            loadEvents(true, false);
            loadEventsIntoTable();
        }
    };

    return (
        // Link template for presenter of event
        // Repeat for each presenter
        row.presenters.map((presenter, key) => (
            <a className="metadata-entry"
               key={key}
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.PRESENTER')} onClick={() => addFilter(presenter)}>
                {presenter}
            </a>
        ))
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

export default connect(mapStateToProps, mapDispatchToProps)(EventsPresentersCell);

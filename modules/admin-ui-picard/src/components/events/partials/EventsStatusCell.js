import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfs from "../../../selectors/tableFilterSelectors";
import * as tfa from "../../../actions/tableFilterActions";
import {connect} from "react-redux";

/**
 * This component renders the status cells of events in the table view
 */
const EventsStatusCell = ({ row, filterMap, editFilterValue })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = status => {
        let filter = filterMap.find(({ name }) => name === "status");
        if (!!filter) {
            editFilterValue(filter.name, status);
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
    filterMap: tfs.getFilters(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    editFilterValue: (filterName, value) => dispatch(tfa.editFilterValue(filterName, value))
});


export default connect(mapStateToProps, mapDispatchToProps)(EventsStatusCell);

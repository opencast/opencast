import React from 'react';
import { useTranslation } from "react-i18next";
import * as tfs from "../../../selectors/tableFilterSelectors";
import * as tfa from "../../../actions/tableFilterActions";
import {connect} from "react-redux";

/**
 * This component renders the presenters cells of events in the table view
 */
const EventsPresentersCell = ({ row, filterMap, editFilterValue })  => {
    const { t } = useTranslation();

    // Filter with value of current cell
    const addFilter = presenter => {
        let filter = filterMap.find(({ name }) => name === "presentersBibliographic");
        if (!!filter) {
            editFilterValue(filter.name, presenter);
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
    editFilterValue: (filterName, value) => dispatch(tfa.editFilterValue(filterName, value))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventsPresentersCell);

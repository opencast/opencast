import React from 'react';
import {useTranslation} from "react-i18next";

import stats from "../../mocks/statsService";
import * as tfs from "../../selectors/tableFilterSelectors";
import * as tfa from "../../actions/tableFilterActions";
import {connect} from "react-redux";
import * as et from "../../thunks/eventThunks";
import * as tt from "../../thunks/tableThunks";



/**
 * This component renders the status bar of the event view and filters depending on these
 */
const Stats = ({ filterMap, editFilterValue, loadEvents, loadEventsIntoTable }) => {
    const { t } = useTranslation();

    // Filter with value of clicked status
    const showStatsFilter = (name) => {
        let filter = filterMap.find(({ name }) => name === "status");
        if (!!filter) {
            editFilterValue(filter.name, name);
            loadEvents(true, false);
            loadEventsIntoTable();
        }
    }

    return (
        <>
            <div className="main-stats">
                {/* Show one counter for each status */}
                {stats.map((st, key) => (
                    <div className="col" key={key}>
                        <div className="stat" onClick={() => showStatsFilter(st.description)} title={t(st.description)}>
                            <h1>{st.counter}</h1>
                            {/* Show the description of the status, if defined,
                            else show name of filter and its value*/}
                            {!!st.description ? (
                                <span>{t(st.description)}</span>
                            ) : (st.filters.map((filter, key) => (
                                    <span>{t(filter.filter)}: {t(filter.value)}</span>
                                ))
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </>
    )
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

export default connect(mapStateToProps, mapDispatchToProps)(Stats);

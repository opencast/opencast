import React, {Component} from 'react';
import {withTranslation} from "react-i18next";

import stats from "../../mocks/statsService";

// todo: implement filter function
function showStatsFilter(name) {
    console.log("Show stats filter");
}

/**
 * This component renders the status bar of the event view and filters depending on these
 */
const Stats = ({ t }) => (
        <>
            <div className="main-stats">
                {/* Show one counter for each status */}
                {stats.map( (st, key) => (
                    <div className="col" key={key}>
                        <div className="stat" onClick={() => showStatsFilter(st.name)} title={t(st.description)}>
                            <h1>{st.counter}</h1>
                            {/* Show the description of the status, if defined,
                            else show name of filter and its value*/}
                            {!!st.description ? (
                                <span>{t(st.description)}</span>
                            ):(st.filters.map((filter, key) => (
                                        <span>{t(filter.filter)}: {t(filter.value)}</span>
                                ))
                            )}
                        </div>
                    </div>
                    ))}
            </div>
        </>
);

export default withTranslation()(Stats);

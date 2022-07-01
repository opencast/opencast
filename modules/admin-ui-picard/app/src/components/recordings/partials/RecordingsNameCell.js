import React from "react";
import {useTranslation} from "react-i18next";
import { Link }  from 'react-router-dom';
import { setSpecificEventFilter } from '../../../thunks/tableFilterThunks';
import { loadEventsIntoTable } from '../../../thunks/tableThunks';
import { connect } from 'react-redux';

/**
 * This component renders the name cells of recordings in the table view
 */
const RecordingsNameCell = ({ row, loadingEventsIntoTable, setSpecificEventFilter }) => {
    const { t } = useTranslation();

    const redirectToEvents = async locationName => {
        // redirect to tables
        await loadingEventsIntoTable();

        // set the location filter value of events to location name
        await setSpecificEventFilter('location', locationName);
    }

    return (
        <Link to="/events/events"
              className="crosslink"
              onClick={async () => await redirectToEvents(row.Name)}
              title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.NAME')}>
            {row.name}
        </Link>
    )
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingEventsIntoTable: () => dispatch(loadEventsIntoTable()),
    setSpecificEventFilter: (filter, filterValue) => dispatch(setSpecificEventFilter(filter, filterValue))
});

export default connect(null, mapDispatchToProps)(RecordingsNameCell);

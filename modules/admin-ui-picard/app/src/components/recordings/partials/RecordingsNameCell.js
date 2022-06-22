import React from "react";
import {useTranslation} from "react-i18next";
import Link  from 'react-router-dom/Link';
import { setSpecificEventFilter } from '../../../thunks/tableFilterThunks';
import { loadEventsIntoTable } from '../../../thunks/tableThunks';
import { editTextFilter } from '../../../actions/tableFilterActions';
import { connect } from 'react-redux';

/**
 * This component renders the name cells of recordings in the table view
 */
const RecordingsNameCell = ({ row, loadingEventsIntoTable, setSpecificEventFilter }) => {
    const { t } = useTranslation();

    const redirectToEvents = locationName => {
        // set the location filter value of events to location name
        setSpecificEventFilter('location', locationName);

        // redirect to tables
        loadingEventsIntoTable();
    }

    return (
        <Link to="/events/events"
              className="crosslink"
              onClick={() => redirectToEvents(row.name)}
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

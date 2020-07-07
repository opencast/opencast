import React from 'react';
import { useTranslation } from "react-i18next";

/**
 * This component renders the location cells of events in the table view
 */
const EventsLocationCell = ({ row })  => {
    const { t } = useTranslation();
    return (
        // Link template for location of event
        <a className="crosslink"
           title={t('EVENTS.EVENTS.TABLE.TOOLTIP.LOCATION')}
           onClick={() => addFilter()}>
            {row.location}
        </a>
    );
};

// todo: if location is clicked, table should be filtered accordingly
const addFilter = () => {
    console.log("needs to be implemented!");
};

export default EventsLocationCell;

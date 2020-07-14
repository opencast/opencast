import React from 'react';
import { useTranslation } from "react-i18next";

/**
 * This component renders the start date cells of events in the table view
 */
const EventsDateCell = ({ row })  => {
    const { t } = useTranslation();

    return (
        // Link template for start date of event
        <a  className="crosslink"
            title={t('EVENTS.EVENTS.TABLE.TOOLTIP.START')} onClick={() => addFilter()}>
            {row.date}
        </a>
    );
};

// todo: if start date is clicked, table should be filtered accordingly
const addFilter = () => {
    console.log("needs to be implemented!");
};

export default EventsDateCell;

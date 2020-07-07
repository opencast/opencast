import React from 'react';
import { useTranslation } from "react-i18next";

/**
 * This component renders the status cells of events in the table view
 */
const EventsStatusCell = ({ row })  => {
    const { t } = useTranslation();
    return (
        <a className="crosslink"
           onClick={() => addFilter()}
           title={t('EVENTS.EVENTS.TABLE.TOOLTIP.STATUS')}>
            {t(row.displayable_status)}
        </a>
    );
};

// todo: if status is clicked, table should be filtered accordingly
const addFilter = () => {
    console.log("needs to be implemented!");
};

export default EventsStatusCell;

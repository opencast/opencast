import React from 'react';
import {useTranslation} from "react-i18next";

/**
 * This component renders the technical date cells of events in the table view
 */
const EventsTechnicalDateCell = ({ row })  => {
    const { t } = useTranslation();
    return (
        // Link template for technical date of event
        <a  className="crosslink"
            title={t('EVENTS.EVENTS.TABLE.TOOLTIP.START')} onClick={() => addFilter()}>
            {row.technical_start}
        </a>
    );
};

// todo: if technical date is clicked, table should be filtered accordingly
const addFilter = () => {
    console.log("needs to be implemented!");
};

export default EventsTechnicalDateCell;

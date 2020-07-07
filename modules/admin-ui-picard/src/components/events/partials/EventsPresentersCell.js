import React from 'react';
import { useTranslation } from "react-i18next";

/**
 * This component renders the presenters cells of events in the table view
 */
const EventsPresentersCell = ({ row })  => {
    const { t } = useTranslation();
    return (
        // Link template for presenter of event
        // Repeat for each presenter
        row.presenters.map((presenter, key) => (
            <a className="metadata-entry"
               key={key}
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.PRESENTER')} onClick={() => addFilter()}>
                {presenter}
            </a>
        ))
    );
};

// todo: if a presenter is clicked, table should be filtered accordingly
const addFilter = () => {
    console.log("needs to be implemented!");
};

export default EventsPresentersCell;

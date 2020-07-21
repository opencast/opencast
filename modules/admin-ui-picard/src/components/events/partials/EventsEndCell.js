import {useTranslation} from "react-i18next";
import React from "react";

const EventsEndCell = ({row}) => {
    const {t} = useTranslation();

    return (
        // Link template for start date of event
        <span>
            {t('dateFormats.time.short', {time: new Date(row.end_date)})}
        </span>
    );
};
export default EventsEndCell;

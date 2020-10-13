import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the sarted date cells of jobs in the table view
 */
const JobsStartedCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <span>
            {t('dateFormats.dateTime.short', { dateTime: new Date(row.started)})}
        </span>
    )

}

export default JobsStartedCell;

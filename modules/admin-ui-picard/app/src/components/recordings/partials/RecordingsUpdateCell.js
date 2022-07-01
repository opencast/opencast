import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the updated cells of recordings in the table view
 */
const RecordingsUpdateCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <span>
            {t('dateFormats.dateTime.short', { dateTime: new Date(row.update)})}
        </span>
    )

}

export default RecordingsUpdateCell;

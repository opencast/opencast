import React from "react";
import {useTranslation} from "react-i18next";

const RecordingsUpdateCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <span>
            {t('dateFormats.dateTime.short', { dateTime: new Date(row.Update)})}
        </span>
    )

}

export default RecordingsUpdateCell;

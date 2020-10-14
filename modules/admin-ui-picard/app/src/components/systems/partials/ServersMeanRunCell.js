import React from "react";
import {useTranslation} from "react-i18next";


const ServersMeanRunCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <span>
            {t('dateFormats.time.medium', { time: new Date(row.meanRunTime)})}
        </span>
    );
}

export default ServersMeanRunCell;

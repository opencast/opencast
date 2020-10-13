import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the status cells of recordings in the table view
 */
const RecordingsStatusCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <span data-status={row.Status}>
            {t(row.Status)}
        </span>
    )

}

export default RecordingsStatusCell;

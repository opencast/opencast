import React from "react";
import {useTranslation} from "react-i18next";


const RecordingsNameCell = ({ row, filterMap, editFilterValue }) => {
    const { t } = useTranslation();


    const placeholder = recordingsName => {
        console.log('To be implemented')
    }

    // TODO: When click on name, open events with this location
    return (
        <a className="crosslink"
           onClick={() => placeholder()}
           title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.NAME')}>
            {row.Name}
        </a>
    )
}

export default RecordingsNameCell;

import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the title cells of series in the table view
 */
const SeriesTitleCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <a className="crosslink"
           title={t('EVENTS.SERIES.TABLE.TOOLTIP.SERIES')}
           onClick={() => onClickPlaceholder()}>
            {row.title}
        </a>
    )
}

//todo: remove if not needed anymore
const onClickPlaceholder = row => {
    console.log("In the Future here opens an other component, which is not implemented yet");
};

export default SeriesTitleCell;

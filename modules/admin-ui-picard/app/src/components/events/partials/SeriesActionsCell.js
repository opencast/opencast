import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the action cells of series in the table view
 */
const SeriesActionsCell = ({ row }) => {
    const { t } = useTranslation();
    return (
        <>
            {/*TODO: When series details are implemented, remove placeholder
            {/*TODO: with-Role ROLE_UI_SERIES_DETAILS_VIEW*/}
            <a onClick={() => onClickPlaceholder(row)}
               className="more-series"
               title={t('EVENTS.SERIES.TABLE.TOOLTIP.DETAILS')}/>

            {/*TODO: When series action for deleting a series is implemented, remove placeholder */}
            {/*TODO: with-Role ROLE_UI_SERIES_DELETE*/}
            <a onClick={() => onClickPlaceholder(row)}
               className="remove"
               title={t('EVENTS.SERIES.TABLE.TOOLTIP.DELETE')}/>
        </>
    )
}


//todo: remove if not needed anymore
const onClickPlaceholder = row => {
    console.log("In the Future here opens an other component, which is not implemented yet");
};

export default SeriesActionsCell;

import React from "react";
import {useTranslation} from "react-i18next";
import styled from "styled-components";

import moreSeriesIcon from '../../../img/more-series-icon.png';
import removeIcon from '../../../img/remove-icon.png';

const SeriesDetailsLink = styled.a`
    background-image: url(${moreSeriesIcon});
    top: auto;
    left: auto;
    width: 19px;
    height: 15px;
`;

const RemoveLink = styled.a`
    background-image: url(${removeIcon});
    top: auto;
    left: auto;
    width: 17px;
    height: 17px;
`;

/**
 * This component renders the action cells of series in the table view
 */
const SeriesActionsCell = ({ row }) => {
    const { t } = useTranslation();
    return (
        <>
            {/*TODO: When series details are implemented, remove placeholder
            {/*TODO: with-Role ROLE_UI_SERIES_DETAILS_VIEW*/}
            <SeriesDetailsLink onClick={() => onClickPlaceholder(row)}
                               title={t('EVENTS.SERIES.TABLE.TOOLTIP.DETAILS')}/>

            {/*TODO: When series action for deleting a series is implemented, remove placeholder */}
            {/*TODO: with-Role ROLE_UI_SERIES_DELETE*/}
            <RemoveLink onClick={() => onClickPlaceholder(row)}
                        title={t('EVENTS.SERIES.TABLE.TOOLTIP.DELETE')}/>
        </>
    )
}


//todo: remove if not needed anymore
const onClickPlaceholder = row => {
    console.log("In the Future here opens an other component, which is not implemented yet");
};

export default SeriesActionsCell;

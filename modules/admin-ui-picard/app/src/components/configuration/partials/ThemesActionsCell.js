import React from 'react';
import {useTranslation} from "react-i18next";

/**
 * This component renders the action cells of themes in the table view
 */
const ThemesActionsCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <>
            {/*TODO: When theme details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('CONFIGURATION.THEMES.TABLE.TOOLTIP.DETAILS')}/>

            {/*// TODO: When theme action for deleting a theme is implemented, remove placeholder*/}
            {/*// TODO: with-Role*/}
            <a onClick={() => onClickPlaceholder()}
               className="remove ng-scope ng-isolate-scope"
               title={t('CONFIGURATION.THEMES.TABLE.TOOLTIP.DELETE')}/>

        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

export default ThemesActionsCell;

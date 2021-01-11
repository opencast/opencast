import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the action cells of services in the table view
 */
const ServicesActionCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        row.status !== 'SYSTEMS.SERVICES.STATUS.NORMAL' ? (
            // todo: with-role
            // TODO: When action for sanitize is implemented, remove placeholder
            <a className="sanitize fa fa-undo"
               onChange={() => onClickPlaceholder()}
               title={t('SYSTEMS.SERVICES.TABLE.SANITIZE')}/>
        ) : null
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

export default ServicesActionCell;

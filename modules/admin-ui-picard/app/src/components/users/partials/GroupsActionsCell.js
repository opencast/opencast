import React from 'react';
import {useTranslation} from "react-i18next";

/**
 * This component renders the action cells of groups in the table view
 */
const GroupsActionsCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <>
            {/*TODO: When group details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

            {/*// TODO: When group action for deleting is implemented, remove placeholder*/}
            {/*// TODO: with-Role*/}
            <a onClick={() => onClickPlaceholder()}
               className="remove"
               title={t('USERS.GROUPS.TABLE.TOOLTIP.DETAILS')}/>

        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

export default GroupsActionsCell;

import React from 'react';
import {useTranslation} from "react-i18next";


const UsersActionCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <>
            {/*TODO: When user details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.USERS.TABLE.TOOLTIP.DETAILS')}/>

            {row.manageable ? (
                // TODO: When user action for deleting an event is implemented, remove placeholder
                // TODO: with-Role
                <a onClick={() => onClickPlaceholder()}
                   className="remove"
                   title={t('USERS.USERS.TABLE.TOOLTIP.DETAILS')}/>
            ) : null }
        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

export default UsersActionCell;

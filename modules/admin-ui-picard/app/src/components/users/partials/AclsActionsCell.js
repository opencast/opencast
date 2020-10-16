import React from 'react';
import {useTranslation} from "react-i18next";


const AclsActionsCell = ({ row }) => {
    const { t } = useTranslation();

    return (
        <>
            {/*TODO: When acl details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('USERS.ACLS.TABLE.TOOLTIP.DETAILS')}/>

            {/*// TODO: When acl action for deleting an event is implemented, remove placeholder*/}
            {/*// TODO: with-Role*/}
            <a onClick={() => onClickPlaceholder()}
               className="remove"
               title={t('USERS.ACLS.TABLE.TOOLTIP.DETAILS')}/>

        </>
    );
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

export default AclsActionsCell;

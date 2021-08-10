import React from "react";
import {useTranslation} from "react-i18next";
import AclDetails from "./AclDetails";

/**
 * This component renders the modal for displaying acl details
 */
const AclDetailsModal = ({ close, aclId }) => {
    const { t } = useTranslation();

    const handleClose = () => {
        close();
    };

    return (
        // todo: add hotkeys
        <>
            <div className="modal-animation modal-overlay"/>
            <section>
                <header>
                    <a className="fa fa-times close-modal" onClick={() => handleClose()}/>
                    <h2>
                        {t('', { resourceId: aclId })}
                    </h2>
                </header>

                {/* component that manages tabs of acl details modal*/}
                <AclDetails />
            </section>
        </>
    );
}

export default AclDetailsModal;

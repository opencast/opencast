import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the modal for displaying group details
 */
const GroupDetailsModal = ({ close, groupname}) => {
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
                        {t('', { resourceId: groupname })}
                    </h2>
                </header>

                {/* component that manages tabs of group details modal*/}
                <GroupDetailsModal />
            </section>
        </>
    );
}

export default GroupDetailsModal;

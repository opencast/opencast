import React from "react";
import {useTranslation} from "react-i18next";

const ConfirmModal = ({ close, resourceType, resourceName, resourceId, deleteMethod }) => {
    const { t } = useTranslation();

    const handleClose = () => {
        close();
    };

    const handleConfirmation = () => {
        deleteMethod(resourceId);
        close();
    };

    return (
        <section className="modal modal-animation" id="confirm-modal" style={{fontSize: "14px"}}>
            <header>
                <a className="fa fa-times close-modal" onClick={() => handleClose()}/>
                <h2>{t('CONFIRMATIONS.ACTIONS.CONFIRMATION')}</h2>
            </header>

            <div>
                <p><span>{t('CONFIRMATIONS.METADATA.NOTICE.' + resourceType)}</span></p>
                <p className="delete">{resourceName}</p>
            </div>
            {resourceType === 'EVENT' && (
                <p className="warning">{t('CONFIRMATIONS.WARNINGS.EVENT_WILL_BE_GONE')}</p>
            )}
            <p>{t('CONFIRMATIONS.CONTINUE_ACTION')}</p>

            <div className="btn-container">
                <button className="cancel-btn close-modal" onClick={() => handleClose()}><i>{t('CANCEL')}</i></button>
                <button className="danger-btn" onClick={() => handleConfirmation()}><i>{t('CONFIRM')}</i></button>
            </div>
        </section>
    );
};

export default ConfirmModal;

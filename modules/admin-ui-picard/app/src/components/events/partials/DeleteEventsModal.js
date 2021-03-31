import React from "react";
import {useTranslation} from "react-i18next";

const DeleteEventsModal = ({ close }) => {
    const { t } = useTranslation();

    return (
        <>
            <section className="modal active modal-open"
                     id="delete-events-status-modal"
                     style={{display: "block"}}>
                <header>
                    <a onClick={close}
                       className="fa fa-times close-modal"/>
                   <h2>{t('BULK_ACTIONS.DELETE.EVENTS.CAPTION')}</h2>
                </header>
            </section>
        </>
    );
};

export default DeleteEventsModal;

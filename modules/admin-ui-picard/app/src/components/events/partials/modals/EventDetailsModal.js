import React from "react";
import {useTranslation} from "react-i18next";
import EventDetailsWizard from "../wizards/EventDetailsWizard";


/**
 * This component renders the modal for adding new resources
 */
const EventDetailsModal = ({ handleClose, showModal, tabIndex }) => {
    const { t } = useTranslation();

    const close = () => {
        handleClose();
    }

    return (
        // todo: add hotkeys
        showModal && (
            <section id="event-details-modal" tabIndex={tabIndex} className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>{t('EVENTS.EVENTS.DETAILS.HEADER',
                        { resourceId: "Beispiel Event ID" /*todo: find real value*/} ) /*Event details - {{resourceId}}*/} </h2>
                </header>

                <EventDetailsWizard tabIndex={tabIndex}
                                    close={close}/>
            </section>
        )
    )
}

export default EventDetailsModal;
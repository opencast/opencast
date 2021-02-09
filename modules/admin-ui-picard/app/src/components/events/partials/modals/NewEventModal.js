import React from "react";
import {useTranslation} from "react-i18next";
import NewEventWizard from "../wizards/NewEventWizard";

/**
 * This component renders the modal for adding new events
 */
const NewEventModal = ({ handleClose, showModal }) => {
    const { t } = useTranslation();

    const close = () => {
        handleClose();
    }

    return (
        // todo: add hotkeys
        showModal && (
            <section id="add-event-modal" tabIndex="1" className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>{t('EVENTS.EVENTS.NEW.CAPTION')}</h2>
                </header>
                {/*New Event Wizard*/}
                <NewEventWizard close={close}/>
            </section>
        )
    )
}

export default NewEventModal;

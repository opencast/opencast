import React from "react";
import {useTranslation} from "react-i18next";
import NewEventWizard from "../events/partials/wizards/NewEventWizard";
import NewSeriesWizard from "../events/partials/wizards/NewSeriesWizard";


/**
 * This component renders the modal for adding new resources
 */
const NewResourceModal = ({ handleClose, showModal, resource }) => {
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
                    {resource === 'events' && (
                        <h2>{t('EVENTS.EVENTS.NEW.CAPTION')}</h2>
                    )}
                    {resource === 'series' && (
                        <h2>{t('EVENTS.SERIES.NEW.CAPTION')}</h2>
                    )}
                </header>
                {resource === 'events' && (
                    //New Event Wizard
                    <NewEventWizard close={close}/>
                )}
                {resource === 'series' && (
                    // New Series Wizard
                    <NewSeriesWizard close={close}/>
                )}

            </section>
        )
    )
}

export default NewResourceModal;

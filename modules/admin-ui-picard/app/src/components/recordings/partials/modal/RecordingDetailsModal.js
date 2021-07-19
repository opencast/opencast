import React from "react";
import {useTranslation} from "react-i18next";
import RecordingsDetails from "./RecordingsDetails";

/**
 * This component renders the modal for displaying recording details
 */
const RecordingDetailsModal = ({ close, recordingId }) => {
    const { t } = useTranslation();

    const handleClose = () => {
        close();
    };

    return (
        // todo: add hotkeys
        <>
            <div className="modal-animation modal-overlay"/>
            <section id="capture-agent-details-modal" className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => handleClose()}/>
                    <h2>
                        {t('RECORDINGS.RECORDINGS.DETAILS.HEADER', { resourceId: recordingId })}
                    </h2>
                </header>

                {/* component that manages tabs of recording details modal*/}
                <RecordingsDetails />

            </section>
        </>
    );
}

export default RecordingDetailsModal;

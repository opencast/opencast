import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteRecording} from "../../../thunks/recordingThunks";
import {connect} from "react-redux";

/**
 * This component renders the action cells of recordings in the table view
 */
const RecordingsActionCell = ({ row, deleteRecording }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingRecording = id => {
       deleteRecording(id);
    };

    return (
        <>
            {/*TODO: When recording details are implemented, remove placeholder */}
            {/*TODO: with-Role */}
            <a className="more"
               title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.DETAILS')}
               onClick={() => onClickPlaceholder()}/>

            {/*TODO: with-Role */}
            <a className="remove"
               title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.DELETE')}
               onClick={() => setDeleteConfirmation(true)}/>

            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.Name}
                              resourceType="LOCATION"
                              resourceId={row.Name}
                              deleteMethod={deletingRecording}/>
            )}
        </>
    )
}

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteRecording: (id) => dispatch(deleteRecording(id))
});

export default connect(null, mapDispatchToProps)(RecordingsActionCell);

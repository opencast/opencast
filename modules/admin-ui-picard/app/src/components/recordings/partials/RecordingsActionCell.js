import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteRecording} from "../../../thunks/recordingThunks";
import {connect} from "react-redux";
import RecordingDetailsModal from "./modal/RecordingDetailsModal";
import {fetchRecordingDetails} from "../../../thunks/recordingDetailsThunks";

/**
 * This component renders the action cells of recordings in the table view
 */
const RecordingsActionCell = ({ row, deleteRecording, fetchRecordingDetails }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displayRecordingDetails, setRecordingDetails] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const hideRecordingDetails = () => {
        setRecordingDetails(false);
    };

    const showRecordingDetails = async () => {
        await fetchRecordingDetails(row.Name);

        setRecordingDetails(true);
    };

    const deletingRecording = id => {
       deleteRecording(id);
    };

    return (
        <>
            {/*TODO: with-Role */}
            <a className="more"
               title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.DETAILS')}
               onClick={() => showRecordingDetails()}/>

            {displayRecordingDetails && (
                <RecordingDetailsModal close={hideRecordingDetails}
                                       recordingId={row.Name} />
            )}

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

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteRecording: (id) => dispatch(deleteRecording(id)),
    fetchRecordingDetails: name => dispatch(fetchRecordingDetails(name))
});

export default connect(null, mapDispatchToProps)(RecordingsActionCell);

import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import RecordingDetailsModal from "./modal/RecordingDetailsModal";
import {deleteRecording} from "../../../thunks/recordingThunks";
import {fetchRecordingDetails} from "../../../thunks/recordingDetailsThunks";
import {getUserInformation} from "../../../selectors/userInfoSelectors";
import {hasAccess} from "../../../utils/utils";

/**
 * This component renders the action cells of recordings in the table view
 */
const RecordingsActionCell = ({ row, deleteRecording, fetchRecordingDetails, user }) => {
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
        await fetchRecordingDetails(row.name);

        setRecordingDetails(true);
    };

    const deletingRecording = id => {
       deleteRecording(id);
    };

    return (
        <>
            {/* view details location/recording */}
            {hasAccess("ROLE_UI_LOCATIONS_DETAILS_VIEW", user) && (
                <a className="more"
                   title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.DETAILS')}
                   onClick={() => showRecordingDetails()}/>
            )}

            {displayRecordingDetails && (
                <RecordingDetailsModal close={hideRecordingDetails}
                                       recordingId={row.name} />
            )}

            {/* delete location/recording */}
            {hasAccess("ROLE_UI_LOCATIONS_DELETE", user) && (
                <a className="remove"
                   title={t('RECORDINGS.RECORDINGS.TABLE.TOOLTIP.DELETE')}
                   onClick={() => setDeleteConfirmation(true)}/>
            )}

            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.name}
                              resourceType="LOCATION"
                              resourceId={row.name}
                              deleteMethod={deletingRecording}/>
            )}
        </>
    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteRecording: (id) => dispatch(deleteRecording(id)),
    fetchRecordingDetails: name => dispatch(fetchRecordingDetails(name))
});

export default connect(mapStateToProps, mapDispatchToProps)(RecordingsActionCell);

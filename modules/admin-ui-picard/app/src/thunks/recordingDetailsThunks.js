import {
    loadRecordingDetailsFailure,
    loadRecordingDetailsInProgress,
    loadRecordingDetailsSuccess
} from "../actions/recordingDetailsActions";
import axios from "axios";

// fetch details of certain recording from server
export const fetchRecordingDetails = name => async dispatch => {
    try {
        dispatch(loadRecordingDetailsInProgress());

        // fetch recording details
        let data = await axios.get(`/admin-ng/capture-agents/${name}`);

        const recordingDetails = await data.data;

        dispatch(loadRecordingDetailsSuccess(recordingDetails));

    } catch (e) {
        dispatch(loadRecordingDetailsFailure());
    }
}

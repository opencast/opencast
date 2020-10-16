import {loadRecordingsFailure, loadRecordingsInProgress, loadRecordingsSuccess} from "../actions/recordingActions";
import {getURLParams} from "../utils/resourceUtils";

// fetch recordings from server
export const fetchRecordings = () => async (dispatch, getState) => {
    try {
        dispatch(loadRecordingsInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /agents.json?filter={filter}&limit=100&offset=0&inputs=false&sort={sort}
        let data = await fetch('admin-ng/capture-agents/agents.json?' + params);


        const recordings = await data.json();
        dispatch(loadRecordingsSuccess(recordings));

    } catch (e) {
        dispatch(loadRecordingsFailure());
        console.log(e);
    }
}

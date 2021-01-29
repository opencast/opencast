import {loadRecordingsFailure, loadRecordingsInProgress, loadRecordingsSuccess} from "../actions/recordingActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

// fetch recordings from server
export const fetchRecordings = flag => async (dispatch, getState) => {
    try {
        dispatch(loadRecordingsInProgress());

        let data;

        if (flag === "inputs") {
            data = await axios.get('admin-ng/capture-agents/agents.json?inputs=true');
        } else {
            const state = getState();
            let params = getURLParams(state);

            // /agents.json?filter={filter}&limit=100&offset=0&inputs=false&sort={sort}
            data = await axios.get('admin-ng/capture-agents/agents.json', { params: params });
        }

        const recordings = await data.data;
        dispatch(loadRecordingsSuccess(recordings));

    } catch (e) {
        dispatch(loadRecordingsFailure());
        console.log(e);
    }
}

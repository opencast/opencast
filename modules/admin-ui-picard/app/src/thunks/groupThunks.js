import {loadGroupsFailure, loadGroupsInProgress, loadGroupsSuccess} from "../actions/groupActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

// fetch groups from server
export const fetchGroups = () => async (dispatch, getState) => {
    try {
        dispatch(loadGroupsInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /groups.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await axios.get('admin-ng/groups/groups.json', { params: params });

        const groups = await data.data;
        dispatch(loadGroupsSuccess(groups));

    } catch (e) {
        dispatch(loadGroupsFailure());
    }
};

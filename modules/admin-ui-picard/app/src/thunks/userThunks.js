import {loadUsersFailure, loadUsersInProgress, loadUsersSuccess} from "../actions/userActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

// fetch users from server
export const fetchUsers = () => async (dispatch, getState) => {
    try {
        dispatch(loadUsersInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /users.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await axios.get('admin-ng/users/users.json', { params: params });

        const users = await data.data;
        dispatch(loadUsersSuccess(users));

    } catch (e) {
        dispatch(loadUsersFailure());
    }
};

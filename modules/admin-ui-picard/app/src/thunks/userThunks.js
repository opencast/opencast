import axios from "axios";
import {loadUsersFailure, loadUsersInProgress, loadUsersSuccess} from "../actions/userActions";
import {getURLParams} from "../utils/resourceUtils";
import {transformToIdValueArray} from "../utils/utils";
import {addNotification} from "./notificationThunks";

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

// get users and their user names
export const fetchUsersAndUsernames = async () => {

    let data = await axios.get('/admin-ng/resources/USERS.NAME.AND.USERNAME.json');

    const response = await data.data;

    return transformToIdValueArray(response);
};

// new user to backend
export const postNewUser = values => async dispatch => {
    let data = new URLSearchParams();
    // fill form data with user inputs
    data.append('username', values.username);
    data.append('name', values.name);
    data.append('email', values.email);
    data.append('password', values.password);
    data.append('roles', JSON.stringify(values.roles));

    // POST request
    axios.post('/admin-ng/users', data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => {
        console.log(response);
        dispatch(addNotification('success', 'USER_ADDED'));
    }).catch(response => {
        console.log(response);
        dispatch(addNotification('error', 'USER_NOT_SAVED'));
    });
};

// delete user with provided id
export const deleteUser = id => async dispatch => {
    // API call for deleting an user
    axios.delete(`/admin-ng/users/${id}.json`).then(res => {
        console.log(res);
        // add success notification
        dispatch(addNotification('success', 'USER_DELETED'));
    }).catch(res => {
        console.log(res);
        // add error notification
        dispatch(addNotification('error', 'USER_NOT_DELETED'));
    });
};

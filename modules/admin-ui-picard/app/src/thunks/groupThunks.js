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

// post new group to backend
export const postNewGroup = async values => {
    let roles = [], users = [];

    // fill form data depending on user inputs
    let data = new FormData();
    data.append('name', values.name);
    data.append('description', values.description);

    for(let i = 0 ; i < values.roles.length; i++) {
        roles.push(values.roles[i].name);
    }
    for(let i = 0 ; i < values.users.length; i++) {
        users.push(values.users[i].name);
    }
    data.append('roles', roles.join(','));
    data.append('users', users.join(','));

    // POST request
    // todo: notification
    axios.post('/admin-ng/groups', data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => console.log(response)).catch(response => console.log(response));
};

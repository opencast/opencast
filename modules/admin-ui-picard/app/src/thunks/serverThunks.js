import {loadServersFailure, loadServersInProgress, loadServersSuccess} from "../actions/serverActions";
import {getURLParams} from "../utils/resourceUtils";

// fetch servers from server
export const fetchServers = () => async (dispatch, getState) => {
    try {
        dispatch(loadServersInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /servers.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await fetch('admin-ng/server/servers.json?' + params);

        const servers = await data.json();
        console.log(servers);

        dispatch(loadServersSuccess(servers));
    } catch (e) {
       dispatch(loadServersFailure());
    }
}

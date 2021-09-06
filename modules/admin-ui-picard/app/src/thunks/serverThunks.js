import axios from "axios";
import {loadServersFailure, loadServersInProgress, loadServersSuccess} from "../actions/serverActions";
import {getURLParams} from "../utils/resourceUtils";
import {logger} from "../utils/logger";

// fetch servers from server
export const fetchServers = () => async (dispatch, getState) => {
    try {
        dispatch(loadServersInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /servers.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await axios.get('admin-ng/server/servers.json', { params: params });

        const servers = await data.data;
        logger.info(servers);

        dispatch(loadServersSuccess(servers));
    } catch (e) {
        logger.error(e);
       dispatch(loadServersFailure());
    }
}

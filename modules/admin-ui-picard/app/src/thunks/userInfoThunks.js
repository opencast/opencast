import {loadUserInfoFailure, loadUserInfoInProgress, loadUserInfoSuccess} from "../actions/userInfoActions";
import axios from "axios";
import {logger} from "../utils/logger";

export const fetchUserInfo = () => async dispatch => {
    try {
        dispatch(loadUserInfoInProgress());

        let data = await axios.get('/info/me.json');

        const userInfo = await (data.data);

        dispatch(loadUserInfoSuccess(userInfo));
    } catch (e) {
        logger.error(e);
        dispatch(loadUserInfoFailure());
    }
}

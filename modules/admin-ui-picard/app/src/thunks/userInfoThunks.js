import {loadUserInfoFailure, loadUserInfoInProgress, loadUserInfoSuccess} from "../actions/userInfoActions";
import axios from "axios";
import {logger} from "../utils/logger";
import {getUserInformation} from "../selectors/userInfoSelectors";

export const fetchUserInfo = () => async dispatch => {
    try {
        dispatch(loadUserInfoInProgress());

        let data = await axios.get('/info/me.json');

        let userInfo = await (data.data);

        // add direct information about user being an admin
        userInfo = {
            isAdmin: userInfo.roles.includes("ROLE_ADMIN"),
            isOrgAdmin: userInfo.roles.includes(userInfo.org.adminRole),
            ...userInfo
        };

        dispatch(loadUserInfoSuccess(userInfo));
    } catch (e) {
        logger.error(e);
        dispatch(loadUserInfoFailure());
    }
}

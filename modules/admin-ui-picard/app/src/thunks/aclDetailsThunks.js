import {loadAclDetailsFailure, loadAclDetailsInProgress, loadAclDetailsSuccess} from "../actions/aclDetailsActions";
import axios from "axios";

// fetch details about certain acl from server
export const fetchAclDetails = aclId => async dispatch => {
    try {
        dispatch(loadAclDetailsInProgress());

        let data = await axios.get(`admin-ng/acl/${aclId}`);

        const aclDetails = await data.data;

        dispatch(loadAclDetailsSuccess(aclDetails));
    } catch (e) {
        dispatch(loadAclDetailsFailure());
    }
}

export const updateAclDetails = (values, aclId) => async dispatch => {

}
